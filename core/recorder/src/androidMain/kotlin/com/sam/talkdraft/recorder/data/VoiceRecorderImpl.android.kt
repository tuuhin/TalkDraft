package com.sam.talkdraft.recorder.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.text.format.Formatter
import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.ext.tryWithLock
import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.recorder.RecorderConstants
import com.sam.talkdraft.recorder.domain.IAudioFormatDataExtractor
import com.sam.talkdraft.recorder.domain.IAudioPCMReader
import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import com.sam.talkdraft.recorder.domain.IVoiceRecorderWithByteReader
import com.sam.talkdraft.recorder.domain.exception.RecorderInvalidConfigurationException
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.models.RecordingFormats
import com.sam.talkdraft.recorder.domain.stopwatch.RecorderStopWatch
import java.io.IOException
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

private const val TAG = "VOICE_RECORDER"

@Factory(binds = [IVoiceRecorderWithByteReader::class])
internal actual class VoiceRecorderImpl(
    @InjectedParam private val scope: CoroutineScope,
    private val context: Context,
    private val files: IPlatformFilePathProvider,
    private val permissions: IRecordPermissionChecker,
    private val formatsReader: IAudioFormatDataExtractor,
    private val pcmReader: IAudioPCMReader,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IVoiceRecorderWithByteReader {

    @Volatile
    private var _recorder: MediaRecorder? = null

    @Volatile
    private var _recordingPath: Path? = null

    private val _lock = Mutex()
    private val fs = FileSystem.SYSTEM

    private val _recordingCachePath by lazy { files.providesCachesDirPath() / "recordings" }
    private val _stopWatch = RecorderStopWatch(scope = scope, delayTime = RecorderConstants.STOPWATCH_DELAY_RATE)

    actual override val state: StateFlow<RecorderState> = _stopWatch.recorderState
    actual override val elapsedTime: StateFlow<Duration> = _stopWatch.elapsedTime

    @OptIn(ExperimentalCoroutinesApi::class)
    actual override val stream: Flow<ReadOnlyShortBuffer> = _stopWatch.recorderState
        .flatMapLatest(pcmReader::readRecorderRawBytes)
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.Lazily, 0)

    actual override suspend fun start() {
        _lock.tryWithLock(this) {
            // current uri is already set cannot set it again
            if (_recordingPath != null) {
                Logger.w(tag = TAG) { "RECORDING FILE URI CANNOT BE EMPTY" }
                return@tryWithLock
            }
            Logger.d(tag = TAG) { "PREPARING FILES FOR RECORDING" }
            initRecorder(format = RecordingFormats.FORMAT_M4A)

            Logger.d(tag = TAG) { "PREPARING RECORDER STOP WATCH" }
            _stopWatch.prepare()
            // prepare the recorder
            _recorder?.prepare()
            Logger.d(tag = TAG) { "RECORDER READY" }
            // initiate the amplitude reader
            _stopWatch.startOrResume()
            pcmReader.start()
            _recorder?.start()
            Logger.d(tag = TAG) { "RECORDER STARTED" }
        }
    }

    actual override suspend fun resume() {
        _lock.tryWithLock(this) {
            try {
                Logger.d(tag = TAG) { "STOP WATCH RESUMED" }
                _stopWatch.startOrResume()
                //pause recorder
                Logger.d(tag = TAG) { "RECORDER IS PAUSED" }
                _recorder?.resume()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    actual override suspend fun pause() {
        _lock.tryWithLock(this) {
            try {
                Logger.d(tag = TAG) { "STOP WATCH PAUSED" }
                _stopWatch.pause()
                //pause recorder
                Logger.d(tag = TAG) { "RECORDER IS PAUSED" }
                _recorder?.pause()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    actual override suspend fun stop(): Result<Path> {
        return _lock.withLock(this) {
            val file = _recordingPath ?: return Result.failure(RecorderInvalidConfigurationException())
            // reset the timer
            Logger.d(tag = TAG) { "STOPWATCH STOPPED" }
            _stopWatch.stop()
            //stop the ongoing recording
            try {
                _recorder?.stop()
                Logger.d(tag = TAG) { "RECORDER STOPPED" }
            } catch (e: RuntimeException) {
                Logger.e(tag = TAG, throwable = e) { "FAILED TO STOP RECORDER" }
            }
            Result.success(file)
        }
    }

    actual override suspend fun cancel() {
        _lock.tryWithLock(this) {
            try {
                // cancel the timer watch
                Logger.d(tag = TAG) { "STOPPING STOPWATCH" }
                _stopWatch.cancel()
                try {
                    _recorder?.stop()
                    Logger.d(tag = TAG) { "RECORDER STOPPED" }
                } catch (e: RuntimeException) {
                    Logger.e(tag = TAG, throwable = e) { "FAILED TO STOP RECORDER" }
                }
                // delete the current recording
                withContext(NonCancellable) {
                    try {
                        deleteRecordingPath(checkSize = false)
                    } finally {
                        _recordingPath = null
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    actual override fun release() {
        //set buffer reader to null
        pcmReader.releaseReader()
        // clear the recorder resources
        Logger.d(tag = TAG) { "RELEASING THE RECORDER" }
        try {
            if (_stopWatch.recorderState.value == RecorderState.RECORDING)
                _recorder?.stop()
            Logger.d(tag = TAG) { "RECORDER STOPPED" }
        } catch (e: RuntimeException) {
            Logger.e(tag = TAG, throwable = e) { "FAILED TO STOP RECORDER" }
        }
        _recorder?.release()
        _recorder = null
        // reset path
        if (_recordingPath != null) runBlocking {
            try {
                withContext(NonCancellable) {
                    deleteRecordingPath()
                }
            } finally {
                _recordingPath = null
            }
        }
        // resetting the stopwatch
        Logger.d(tag = TAG) { "RESETTING STOPWATCH" }
        _stopWatch.reset()
    }


    @Suppress("DEPRECATION")
    private fun createRecorder(): Boolean {
        if (!permissions.hasPermission()) {
            Logger.d(tag = TAG) { "NO AUDIO RECORD PERMISSION FOUND" }
            return false
        }
        if (_recorder != null) {
            Logger.d(tag = TAG) { "RECORDER IS ALREADY READY" }
            return false
        }
        // set recorder
        _recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context)
        else MediaRecorder()

        _recorder?.setOnErrorListener { _, what, extra ->
            if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) release()
            Logger.w(tag = TAG) { "SOME ERROR OCCURRED :$what CODE:$extra" }
        }
        Logger.d(tag = TAG) { "CREATED RECORDER SUCCESSFULLY" }
        return true
    }

    private suspend fun initRecorder(format: RecordingFormats) = coroutineScope {
        if (_recorder == null) {
            val isSuccess = createRecorder()
            if (!isSuccess) return@coroutineScope
        }

        val outFormat = formatsReader.getOutputFormat(format)
        val encoder = formatsReader.getEncoder(format)
        val extension = formatsReader.getFileExtension(format)
        val channelCount = 1

        // recorder should be ready by now
        val recorder = _recorder ?: return@coroutineScope

        // ensures the file is being created in a different coroutine
        val fileDeferred = async(dispatchers.io) {
            val newPath = _recordingCachePath / "${Uuid.random()}.$extension"
            if (!fs.exists(_recordingCachePath)) {
                Logger.d(tag = TAG) { "FILE PATH MISSING CREATED PATH" }
                fs.createDirectory(_recordingCachePath)
            }
            Logger.d(tag = TAG) { "SAVING PATH :$newPath" }
            _recordingPath = newPath
            newPath.toFile()
        }

        pcmReader.initReader()

        recorder.apply {
            setOutputFile(fileDeferred.await())
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(outFormat)
            setAudioEncoder(encoder)
            setAudioChannels(channelCount)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
        }
        recorder.logMetrics()
    }

    private suspend fun deleteRecordingPath(checkSize: Boolean = true) {
        withContext(dispatchers.io) {
            val path = _recordingPath ?: return@withContext
            val size = fs.metadataOrNull(path)?.size ?: 0L

            if (checkSize && size == 0L) {
                fs.delete(path)
                Logger.i(tag = TAG) { "DELETING THE EMPTY FILE" }
            } else if (!checkSize) {
                val fileSize = Formatter.formatFileSize(context, size)
                Logger.w(tag = TAG) { "NEED TO REMOVE THE FILE SIZE:$fileSize" }
                fs.delete(path)
            }
        }
    }

    private fun MediaRecorder.logMetrics() {
        val currentMetrics = metrics ?: return
        val bitrate = currentMetrics.getInt(MediaRecorder.MetricsConstants.AUDIO_BITRATE)
        val sampleRte = currentMetrics.getInt(MediaRecorder.MetricsConstants.AUDIO_SAMPLERATE)
        val channel = currentMetrics.getInt(MediaRecorder.MetricsConstants.AUDIO_CHANNELS)
        Logger.i(tag = TAG) { "RECORDER METRICS AFTER CONFIGURATION" }
        Logger.i(tag = TAG) { "SAMPLING RATE : $sampleRte , BIT_RATE:$bitrate CHANNEL:$channel" }
    }
}
