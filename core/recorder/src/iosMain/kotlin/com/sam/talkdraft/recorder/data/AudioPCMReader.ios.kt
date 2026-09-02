package com.sam.talkdraft.recorder.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.recorder.domain.IAudioPCMReader
import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import com.sam.talkdraft.recorder.domain.exception.RecorderInitMissingException
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeSpokenAudio
import platform.AVFAudio.setActive
import platform.Foundation.NSError

private const val TAG = "AUDIO_PCM_READER"

@OptIn(
    ExperimentalAtomicApi::class,
    ExperimentalForeignApi::class,
    BetaInteropApi::class,
)
@Factory(binds = [IAudioPCMReader::class])
internal actual class AudioPCMReaderImpl(
    private val permissions: IRecordPermissionChecker,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IAudioPCMReader {

    @Volatile
    private var _engine: AVAudioEngine? = null

    private val _engineReady = AtomicBoolean(false)

    private val pcmBufferSharedFlow = MutableSharedFlow<ReadOnlyShortBuffer>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    actual override suspend fun initReader() = memScoped {
        if (!permissions.hasPermission()) {
            Logger.d(tag = TAG) { "RECORD AUDIO PERMISSION MISSING" }
            return@memScoped
        }

        if (_engine != null) {
            Logger.d(tag = TAG) { "RECORDER ALREADY INITIATED PLEASE RELEASE IT FIRST" }
            return@memScoped
        }

        try {
            withContext(dispatchers.io) {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val audioSession = AVAudioSession.sharedInstance()
                val options = AVAudioSessionCategoryOptionAllowBluetooth or AVAudioSessionCategoryOptionDefaultToSpeaker

                val categorySuccess = audioSession.setCategory(
                    category = AVAudioSessionCategoryPlayAndRecord,
                    mode = AVAudioSessionModeSpokenAudio,
                    options = options,
                    error = errorPtr.ptr,
                )
                if (!categorySuccess) {
                    Logger.e(tag = TAG) { "FAILED TO SET AUDIO SESSION CATEGORY: ${errorPtr.value?.localizedDescription}" }
                    return@withContext
                }

                val isActive = audioSession.setActive(active = true, error = errorPtr.ptr)
                if (!isActive) {
                    Logger.e(tag = TAG) { "FAILED TO SET AUDIO ACTIVE: ${errorPtr.value?.localizedDescription}" }
                    return@withContext
                }

                val engine = AVAudioEngine()
                _engine = engine
                _engineReady.compareAndSet(expectedValue = false, newValue = true)
            }
            Logger.d(tag = TAG) { "IOS AUDIO ENGINE INIT SUCCESSFUL" }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "INVALID ARGUMENTS FOR IOS AUDIO ENGINE" }
            releaseReader()
        }
    }

    actual override fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer> {
        return pcmBufferSharedFlow.asSharedFlow()
            .catch { err ->
                if (err is CancellationException) Logger.d(tag = TAG) { "FLOW READ IS CANCELLED" }
            }
            .flowOn(dispatchers.io)
    }

    actual override suspend fun start() = memScoped {
        val engine = _engine ?: run {
            Logger.w(tag = TAG) { "AUDIO RECORDER CANNOT BE NULL FOR START" }
            throw RecorderInitMissingException()
        }

        if (engine.isRunning()) {
            Logger.w(tag = TAG) { "RECORDER STATE RECORDING CANNOT START AGAIN" }
            return@memScoped
        }

        val error = alloc<ObjCObjectVar<NSError?>>()
        try {
            withContext(dispatchers.io) {
                // CRITICAL FIX: Access inputNode BEFORE calling prepare() or start()
                // Accessing inputNode attaches input hardware node to the engine graph.
                val inputNode = engine.inputNode
                val bus = 0uL
                val format = inputNode.inputFormatForBus(bus)

                inputNode.removeTapOnBus(bus)
                inputNode.installTapOnBus(
                    bus = bus,
                    bufferSize = 1024u,
                    format = format,
                ) { buffer: AVAudioPCMBuffer?, _ ->
                    if (buffer == null) return@installTapOnBus

                    val frameLength = buffer.frameLength.toInt()
                    if (frameLength <= 0) return@installTapOnBus

                    val int16Data = buffer.int16ChannelData ?: return@installTapOnBus
                    val channelPointer = int16Data[0] ?: return@installTapOnBus
                    val pcmArray = ShortArray(frameLength) { index -> channelPointer[index] }

                    pcmBufferSharedFlow.tryEmit(ReadOnlyShortBuffer.wrap(pcmArray, frameLength))
                }

                engine.prepare()
                val isGood = engine.startAndReturnError(error.ptr)
                if (!isGood) {
                    Logger.d(tag = TAG) { "FAILED TO START RECORDING: ${error.value?.localizedDescription}" }
                } else {
                    Logger.d(tag = TAG) { "AUDIO ENGINE STARTED SUCCESSFULLY" }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO START RECORDING" }
        }
    }

    actual override suspend fun stop() {
        val engine = _engine ?: return

        if (!engine.isRunning()) {
            Logger.w(tag = TAG) { "RECORDER STATE STOPPED RECORDING CANNOT STOP AGAIN" }
            return
        }

        try {
            withContext(dispatchers.io) {
                engine.inputNode.removeTapOnBus(0u)
                engine.stop()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO STOP RECORDING" }
        }
    }

    actual override fun releaseReader() = memScoped {
        try {
            _engine?.inputNode?.removeTapOnBus(0u)
            if (_engine?.isRunning() == true) {
                _engine?.stop()
            }
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "UNABLE TO STOP RECORDING AUDIO" }
        }

        try {
            val session = AVAudioSession.sharedInstance()
            val error = alloc<ObjCObjectVar<NSError?>>()
            val isActive = session.setActive(false, error = error.ptr)
            if (!isActive) {
                Logger.d(tag = TAG) { "FAILED TO DEACTIVATE AUDIO SESSION: ${error.value?.localizedDescription}" }
            }

            Logger.d(tag = TAG) { "RECORDER RELEASED" }
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "FAILED TO RELEASE THE RECORDER" }
        } finally {
            _engine = null
            _engineReady.compareAndSet(expectedValue = true, false)
        }
    }
}
