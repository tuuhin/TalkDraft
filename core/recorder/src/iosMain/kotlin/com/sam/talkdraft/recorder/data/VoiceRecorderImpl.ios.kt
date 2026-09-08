package com.sam.talkdraft.recorder.data

import co.touchlab.kermit.Logger
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
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetoothA2DP
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetoothHFP
import platform.AVFAudio.AVAudioSessionCategoryOptionBluetoothHighQualityRecording
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeSpokenAudio
import platform.AVFAudio.AVAudioSessionPortBuiltInMic
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.availableInputs
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSURL

private const val TAG = "IOS-VOICE_RECORDER"

@OptIn(
    ExperimentalForeignApi::class,
    BetaInteropApi::class,
)
@Factory(binds = [IVoiceRecorderWithByteReader::class])
internal actual class VoiceRecorderImpl(
    @InjectedParam private val scope: CoroutineScope,
    private val files: IPlatformFilePathProvider,
    private val permissions: IRecordPermissionChecker,
    private val formatsReader: IAudioFormatDataExtractor,
    private val pcmReader: IAudioPCMReader,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IVoiceRecorderWithByteReader {

    private var _recorder: AVAudioRecorder? = null
    private var _recordingPath: Path? = null

    private val recordingCachePath by lazy { files.providesCachesDirPath() / "recording_caches" }
    private val lock = Mutex()
    private val fs = FileSystem.SYSTEM

    private val stopWatch = RecorderStopWatch(scope = scope, delayTime = RecorderConstants.STOPWATCH_DELAY_RATE)

    actual override val state: StateFlow<RecorderState>
        get() = stopWatch.recorderState

    actual override val elapsedTime: StateFlow<Duration>
        get() = stopWatch.elapsedTime

    actual override val stream: Flow<ReadOnlyShortBuffer> = stopWatch.recorderState
        .flatMapLatest(pcmReader::readRecorderRawBytes)
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.Lazily, 0)

    actual override suspend fun start() = lock.withLock(this) {
        if (!permissions.hasPermission()) {
            Logger.w(tag = TAG) { "No permission to record audio." }
            return@withLock
        }

        if (_recordingPath != null) {
            Logger.w(tag = TAG) { "RECORDING IS ALREADY ACTIVE" }
            return@withLock
        }

        try {
            withContext(dispatchers.io) {
                if (!fs.exists(recordingCachePath)) {
                    Logger.d(tag = TAG) { "FILE PATH WAS MISSING CREATING FILE PATH" }
                    fs.createDirectories(recordingCachePath)
                }

                val extension = formatsReader.getFileExtension(RecordingFormats.FORMAT_M4A)
                val encoder = formatsReader.getEncoder(RecordingFormats.FORMAT_M4A)
                val newPath = recordingCachePath / "${Uuid.random()}.$extension"

                configureAudioSessionInternal()

                // Init PCM tap reader first
                pcmReader.initReader()
                pcmReader.start()

                val fileUrl = NSURL.fileURLWithPath(newPath.toString())
                val settings: Map<Any?, Any> = mapOf(
                    AVFormatIDKey to NSNumber(unsignedInt = encoder.toUInt()),
                    AVSampleRateKey to NSNumber(double = 48_000.0),
                    AVNumberOfChannelsKey to NSNumber(int = 1),
                    AVEncoderBitRateKey to NSNumber(int = 128_000),
                )

                val audioRecorder = memScoped {
                    val nsError = alloc<ObjCObjectVar<NSError?>>()
                    val recorder = AVAudioRecorder(uRL = fileUrl, settings = settings, nsError.ptr)
                    val nsErrorValue = nsError.value
                    if (nsErrorValue != null) {
                        Logger.w(tag = TAG) { "FAILED TO CREATE RECORDER INSTANCE: ${nsErrorValue.localizedDescription}" }
                        return@memScoped null
                    }
                    recorder
                } ?: run {
                    cleanupRecorderInternal()
                    return@withContext
                }

                audioRecorder.meteringEnabled = true

                if (!audioRecorder.prepareToRecord()) {
                    Logger.e(tag = TAG) { "Failed to prepare AVAudioRecorder" }
                    cleanupRecorderInternal()
                    return@withContext
                }

                if (audioRecorder.record()) {
                    _recorder = audioRecorder
                    _recordingPath = newPath
                    stopWatch.startOrResume()
                    Logger.d(tag = TAG) { "iOS Recorder started successfully" }
                } else {
                    Logger.e(tag = TAG) { "AVAudioRecorder.record() returned false" }
                    cleanupRecorderInternal()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "Error starting iOS recorder" }
            cleanupRecorderInternal()
        }
    }

    actual override suspend fun resume() = lock.withLock(this) {
        val rec = _recorder ?: return@withLock
        if (rec.isRecording()) {
            Logger.w(tag = TAG) { "RECORDER IS RECORDING CANNOT RESUME" }
            return@withLock
        }
        withContext(dispatchers.io) {
            rec.record()
            stopWatch.startOrResume()
            Logger.d(tag = TAG) { "RECORDER RESUMED" }
        }
    }

    actual override suspend fun pause() = lock.withLock(this) {
        val rec = _recorder ?: return@withLock
        if (!rec.isRecording()) {
            Logger.w(tag = TAG) { "RECORDER IS NOT RECORDING CANNOT PAUSE" }
            return@withLock
        }
        withContext(dispatchers.io) {
            rec.pause()
            pcmReader.stop()
            stopWatch.pause()
            Logger.d(tag = TAG) { "iOS Recorder paused" }
        }
    }

    actual override suspend fun stop(): Result<Path> = lock.withLock(this) {
        val file = _recordingPath ?: return Result.failure(RecorderInvalidConfigurationException())

        withContext(dispatchers.io) {
            stopWatch.stop()
            pcmReader.stop()
            _recorder?.stop()
            _recorder = null
            _recordingPath = null

            deactivateAudioSessionInternal()
            Logger.d(tag = TAG) { "IOS RECORDER STOPPED" }
        }
        Result.success(file)
    }

    actual override suspend fun cancel() = lock.withLock(this) {
        withContext(dispatchers.io + NonCancellable) {
            stopWatch.cancel()
            pcmReader.stop()
            _recorder?.stop()
            _recorder = null

            val pathToDelete = _recordingPath
            _recordingPath = null

            if (pathToDelete != null) {
                runCatching { fs.delete(pathToDelete) }
            }
            deactivateAudioSessionInternal()
            Logger.d(tag = TAG) { "iOS Recording cancelled and file removed" }
        }
    }

    actual override fun release() {
        pcmReader.releaseReader()
        _recorder?.stop()
        _recorder = null
        _recordingPath = null
        stopWatch.reset()
        deactivateAudioSessionInternal()
        Logger.d(tag = TAG) { "iOS Recorder released" }
    }

    private fun cleanupRecorderInternal() {
        pcmReader.releaseReader()
        _recorder?.stop()
        _recorder = null
        _recordingPath = null
        stopWatch.reset()
        deactivateAudioSessionInternal()
    }

    private fun deactivateAudioSessionInternal() = memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        val session = AVAudioSession.sharedInstance()
        val success = session.setActive(
            active = false,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = errorPtr.ptr,
        )
        if (!success) {
            Logger.w(tag = TAG) { "Failed to deactivate audio session: ${errorPtr.value?.localizedDescription}" }
        }
    }

    private fun configureAudioSessionInternal() = memScoped {
        val session = AVAudioSession.sharedInstance()
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()

        val options = AVAudioSessionCategoryOptionDefaultToSpeaker or
            AVAudioSessionCategoryOptionAllowBluetoothHFP or
            AVAudioSessionCategoryOptionAllowBluetoothA2DP or
            AVAudioSessionCategoryOptionBluetoothHighQualityRecording

        val categorySuccess = session.setCategory(
            category = AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeSpokenAudio,
            options = options,
            error = errorPtr.ptr,
        )

        if (!categorySuccess) {
            Logger.e(tag = TAG) { "FAILED TO SET CATEGORY: ${errorPtr.value?.localizedDescription}" }
        }

        val activeSuccess = session.setActive(true, error = errorPtr.ptr)
        if (!activeSuccess) {
            Logger.e(tag = TAG) { "Failed to activate AVAudioSession: ${errorPtr.value?.localizedDescription}" }
        }

        val availableInputs = session.availableInputs()
            ?.filterIsInstance<AVAudioSessionPortDescription>()
            ?: emptyList()

        val builtInMic = availableInputs.firstOrNull { it.portType == AVAudioSessionPortBuiltInMic }
        if (builtInMic != null) {
            session.setPreferredInput(builtInMic, error = errorPtr.ptr)
        }
    }
}
