package com.sam.talkdraft.recorder.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.recorder.domain.IAudioBytesDataProvider
import com.sam.talkdraft.recorder.domain.IAudioFormatDataExtractor
import com.sam.talkdraft.recorder.domain.IAudioPCMReader
import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import com.sam.talkdraft.recorder.domain.IVoiceRecorder
import com.sam.talkdraft.recorder.domain.exception.RecorderInvalidConfigurationException
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.models.RecordingFormats
import com.sam.talkdraft.recorder.domain.stopwatch.RecorderStopWatch
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeSpokenAudio
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.Foundation.NSURL

private const val TAG = "IOS-VOICE_RECORDER"

@OptIn(ExperimentalForeignApi::class)
@Factory(binds = [IVoiceRecorder::class, IAudioBytesDataProvider::class])
internal actual class VoiceRecorderImpl(
    @InjectedParam private val stopWatch: RecorderStopWatch,
    private val files: IPlatformFilePathProvider,
    private val permissions: IRecordPermissionChecker,
    private val formatsReader: IAudioFormatDataExtractor,
    private val pcmReader: IAudioPCMReader,
) : IVoiceRecorder, IAudioBytesDataProvider {

    private var recorder: AVAudioRecorder? = null
    private var recordingPath: Path? = null

    private val recordingCachePath by lazy { files.providesCachesDirPath() / "recording_caches" }
    private val lock = Mutex()
    private val fs = FileSystem.SYSTEM

    actual override val state: StateFlow<RecorderState>
        get() = stopWatch.recorderState
    actual override val elapsedTime: StateFlow<Duration>
        get() = stopWatch.elapsedTime

    actual override val stream: Flow<ReadOnlyShortBuffer>
        get() = stopWatch.recorderState.flatMapLatest(pcmReader::readRecorderRawBytes)

    actual override suspend fun start() {
        lock.withLock(this) {
            if (!permissions.hasPermission()) {
                Logger.w(tag = TAG) { "No permission to record audio." }
                return@withLock
            }

            if (recordingPath != null) {
                Logger.w(tag = TAG) { "RECORDING IS ALREADY ACTIVE" }
                return@withLock
            }

            try {
                val session = AVAudioSession.sharedInstance()
                val options = AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionAllowBluetooth
                session.setCategory(
                    category = AVAudioSessionCategoryPlayAndRecord,
                    mode = AVAudioSessionModeSpokenAudio,
                    options = options,
                    error = null,
                )
                session.setActive(true, error = null)

                val extension = formatsReader.getFileExtension(RecordingFormats.FORMAT_M4A)
                val encoder = formatsReader.getEncoder(RecordingFormats.FORMAT_M4A)

                val newPath = recordingCachePath / "${Uuid.random()}.$extension"
                recordingPath = newPath

                val fileUrl = NSURL.fileURLWithPath(newPath.toString())
                val settings: Map<Any?, Any> = mapOf(
                    AVFormatIDKey to encoder,
                    AVSampleRateKey to 44100.0,
                    AVNumberOfChannelsKey to 1,
                    AVEncoderBitRateKey to 128000,
                )

                val audioRecorder = AVAudioRecorder(uRL = fileUrl, settings = settings, null)
                audioRecorder.meteringEnabled = true

                if (!audioRecorder.prepareToRecord()) {
                    Logger.e(tag = TAG) { "Failed to prepare AVAudioRecorder" }
                    cleanupRecorderInternal()
                    return@withLock
                }
                recorder = audioRecorder
                pcmReader.start()
                if (audioRecorder.record()) {
                    stopWatch.startOrResume()
                    Logger.d(tag = TAG) { "iOS Recorder started successfully" }
                } else {
                    Logger.e(tag = TAG) { "AVAudioRecorder.record() returned false" }
                    cleanupRecorderInternal()
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(tag = TAG, throwable = e) { "Error starting iOS recorder" }
                cleanupRecorderInternal()
            }
        }
    }

    actual override suspend fun resume() {
        lock.withLock(this) {
            val rec = recorder ?: return@withLock
            if (!rec.isRecording()) {
                rec.record()
                stopWatch.startOrResume()
                Logger.d(tag = TAG) { "iOS Recorder resumed" }
            }
        }
    }

    actual override suspend fun pause() {
        lock.withLock(this) {
            val rec = recorder ?: return@withLock
            if (rec.isRecording()) {
                rec.pause()
                stopWatch.pause()
                Logger.d(tag = TAG) { "iOS Recorder paused" }
            }
        }
    }

    actual override suspend fun stop(): Result<Path> {
        return lock.withLock(this) {
            val file = recordingPath ?: return Result.failure(RecorderInvalidConfigurationException())

            stopWatch.stop()
            recorder?.stop()
            recorder = null
            recordingPath = null

            AVAudioSession.sharedInstance().setActive(false, error = null)
            Logger.d(tag = TAG) { "iOS Recorder stopped" }
            Result.success(file)
        }
    }

    actual override suspend fun cancel() {
        lock.withLock(this) {
            stopWatch.cancel()
            recorder?.stop()
            recorder = null

            val pathToDelete = recordingPath
            recordingPath = null

            if (pathToDelete != null) {
                withContext(NonCancellable) {
                    runCatching { fs.delete(pathToDelete) }
                }
            }
            AVAudioSession.sharedInstance().setActive(false, error = null)
            Logger.d(tag = TAG) { "iOS Recording cancelled and file removed" }
        }
    }

    actual override fun release() {
        pcmReader.releaseReader()
        recorder?.stop()
        recorder = null
        recordingPath = null
        stopWatch.reset()
        AVAudioSession.sharedInstance().setActive(false, error = null)
        Logger.d(tag = TAG) { "iOS Recorder released" }
    }


    private fun cleanupRecorderInternal() {
        recorder?.stop()
        recorder = null
        recordingPath = null
        stopWatch.reset()
        AVAudioSession.sharedInstance().setActive(false, error = null)
    }
}
