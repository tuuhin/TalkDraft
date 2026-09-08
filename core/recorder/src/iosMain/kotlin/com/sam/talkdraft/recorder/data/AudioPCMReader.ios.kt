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
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import platform.AVFAudio.AVAudioConverter
import platform.AVFAudio.AVAudioConverterInputStatus_HaveData
import platform.AVFAudio.AVAudioConverterInputStatus_NoDataNow
import platform.AVFAudio.AVAudioConverterOutputStatus_Error
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatInt16
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetoothHFP
import platform.AVFAudio.AVAudioSessionCategoryOptionBluetoothHighQualityRecording
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionPortBuiltInMic
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.availableInputs
import platform.AVFAudio.inputNumberOfChannels
import platform.AVFAudio.sampleRate
import platform.AVFAudio.setActive
import platform.AVFAudio.setPreferredIOBufferDuration
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

    actual override suspend fun initReader() {
        if (!permissions.hasPermission()) {
            Logger.d(tag = TAG) { "RECORD AUDIO PERMISSION MISSING" }
            return
        }

        if (_engine != null) {
            Logger.d(tag = TAG) { "RECORDER ALREADY INITIATED PLEASE RELEASE IT FIRST" }
            return
        }

        try {
            withContext(dispatchers.io) {
                configureAudioSession()

                val engine = AVAudioEngine()
                _engine = engine
                _engineReady.compareAndSet(expectedValue = false, newValue = true)
            }

            Logger.d(tag = TAG) { "IOS AUDIO ENGINE INIT SUCCESSFUL" }
        } catch (e: CancellationException) {
            withContext(NonCancellable + dispatchers.io) {
                releaseReaderInternal()
            }
            throw e
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "INVALID ARGUMENTS FOR IOS AUDIO ENGINE" }

            withContext(NonCancellable + dispatchers.io) {
                releaseReaderInternal()
            }
        }
    }

    actual override fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer> {
        return pcmBufferSharedFlow.asSharedFlow()
    }

    actual override suspend fun start() {
        val engine = _engine ?: run {
            Logger.w(tag = TAG) { "AUDIO RECORDER CANNOT BE NULL FOR START" }
            throw RecorderInitMissingException()
        }

        if (engine.isRunning()) {
            Logger.w(tag = TAG) { "RECORDER STATE RECORDING CANNOT START AGAIN" }
            return
        }

        try {
            withContext(dispatchers.io) {
                configureAudioSession()

                val inputNode = engine.inputNode
                val bus = 0uL

                inputNode.removeTapOnBus(bus)
                val inputFormat = inputNode.outputFormatForBus(bus)
                Logger.d(tag = TAG) { "INPUT AUDIO FORMAT: ${inputFormat.sampleRate}Hz CHANNEL:${inputFormat.channelCount}" }


                val outputFormat = AVAudioFormat(
                    commonFormat = AVAudioPCMFormatInt16,
                    sampleRate = SAMPLE_RATE,
                    channels = CHANNEL_COUNT.toUInt(),
                    interleaved = false,
                )

                val converter = AVAudioConverter(inputFormat, outputFormat)


                inputNode.installTapOnBus(
                    bus = bus,
                    bufferSize = BUFFER_SIZE,
                    format = inputFormat,
                ) { inputBuffer: AVAudioPCMBuffer?, _ ->
                    if (inputBuffer == null || inputBuffer.frameLength == 0u) return@installTapOnBus

                    val ratio = outputFormat.sampleRate / inputFormat.sampleRate
                    val outCapacity = ((inputBuffer.frameLength.toDouble() * ratio).toUInt()) + 16u

                    val convertedBuffer = AVAudioPCMBuffer(pCMFormat = outputFormat, frameCapacity = outCapacity)

                    var suppliedInput = false

                    memScoped {
                        val error = alloc<ObjCObjectVar<NSError?>>()

                        val status = converter.convertToBuffer(convertedBuffer, error = error.ptr) { _, outStatusPtr ->
                            if (suppliedInput) {
                                outStatusPtr?.pointed?.value = AVAudioConverterInputStatus_NoDataNow
                                null
                            } else {
                                suppliedInput = true
                                outStatusPtr?.pointed?.value = AVAudioConverterInputStatus_HaveData
                                inputBuffer
                            }
                        }

                        if (status == AVAudioConverterOutputStatus_Error) {
                            Logger.w(tag = TAG) { "PCM CONVERSION FAILED: ${error.value?.localizedDescription}" }
                            return@installTapOnBus
                        }
                    }

                    val convertedFrameLength = convertedBuffer.frameLength.toInt()
                    if (convertedFrameLength <= 0) return@installTapOnBus

                    val int16Data = convertedBuffer.int16ChannelData ?: return@installTapOnBus
                    val channelPointer = int16Data[0] ?: return@installTapOnBus

                    val pcmArray = ShortArray(convertedFrameLength)
                    repeat(convertedFrameLength) { idx ->
                        pcmArray[idx] = channelPointer[idx]
                    }

                    pcmBufferSharedFlow.tryEmit(ReadOnlyShortBuffer.wrap(pcmArray, convertedFrameLength))
                }

                engine.prepare()
                memScoped {
                    val error = alloc<ObjCObjectVar<NSError?>>()
                    val isGood = engine.startAndReturnError(error.ptr)

                    if (!isGood) {
                        inputNode.removeTapOnBus(bus)
                        Logger.e(tag = TAG) { "FAILED TO START RECORDING: ${error.value?.localizedDescription}" }

                        throw IllegalStateException(
                            error.value?.localizedDescription ?: "FAILED TO START AUDIO ENGINE",
                        )
                    }

                    Logger.d(tag = TAG) { "AUDIO ENGINE STARTED SUCCESSFULLY" }
                }
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable + dispatchers.io) {
                stopEngineInternal(engine)
            }
            throw e
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO START RECORDING" }

            withContext(NonCancellable + dispatchers.io) {
                stopEngineInternal(engine)
            }
        }
    }


    actual override suspend fun stop() {
        val engine = _engine ?: return

        withContext(NonCancellable) {
            withContext(dispatchers.io) {
                stopEngineInternal(engine)
            }
        }
    }

    private fun stopEngineInternal(engine: AVAudioEngine) {
        _engineReady.compareAndSet(expectedValue = true, newValue = false)
        try {
            engine.inputNode.removeTapOnBus(0u)
            if (engine.isRunning()) engine.stop()
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "UNABLE TO STOP RECORDING AUDIO" }
        }
    }

    actual override fun releaseReader() = releaseReaderInternal()


    private fun releaseReaderInternal() {
        try {
            _engine?.let { engine ->
                stopEngineInternal(engine)
            }
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) {
                "UNABLE TO RELEASE RECORDING AUDIO"
            }
        } finally {
            _engine = null
            _engineReady.compareAndSet(expectedValue = true, newValue = false)
            deactivateAudioSession()
            Logger.d(tag = TAG) { "RECORDER RELEASED" }
        }
    }

    private fun deactivateAudioSession() = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val session = AVAudioSession.sharedInstance()

        val success = session.setActive(
            active = false,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = error.ptr,
        )

        if (!success)
            Logger.w(tag = TAG) { "FAILED TO DEACTIVATE AUDIO SESSION: ${error.value?.localizedDescription}" }
    }

    private fun configureAudioSession() = memScoped {
        val session = AVAudioSession.sharedInstance()
        val error = alloc<ObjCObjectVar<NSError?>>()

        val options = AVAudioSessionCategoryOptionDefaultToSpeaker or
            AVAudioSessionCategoryOptionAllowBluetooth or
            AVAudioSessionCategoryOptionAllowBluetoothHFP or
            AVAudioSessionCategoryOptionBluetoothHighQualityRecording

        val categorySuccess = session.setCategory(
            category = AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeMeasurement,
            options = options,
            error = error.ptr,
        )

        if (!categorySuccess) {
            Logger.e(tag = TAG) { "FAILED TO SET AUDIO SESSION CATEGORY: ${error.value?.localizedDescription}" }
        }


        val bufferDurationSuccess = session.setPreferredIOBufferDuration(
            duration = BUFFER_SIZE.toDouble() / SAMPLE_RATE,
            error = error.ptr,
        )

        if (!bufferDurationSuccess)
            Logger.w(tag = TAG) { "FAILED TO SET PREFERRED IO BUFFER DURATION: ${error.value?.localizedDescription}" }

        val activeSuccess = session.setActive(active = true, error = error.ptr)

        if (!activeSuccess)
            Logger.e(tag = TAG) { "FAILED TO ACTIVATE AUDIO SESSION: ${error.value?.localizedDescription}" }


        val availableInputs = session.availableInputs()
            ?.filterIsInstance<AVAudioSessionPortDescription>()
            ?: emptyList()

        val builtInMic = availableInputs.firstOrNull {
            it.portType == AVAudioSessionPortBuiltInMic
        }

        if (builtInMic != null) {
            val inputSuccess = session.setPreferredInput(builtInMic, error = error.ptr)
            if (!inputSuccess)
                Logger.w(tag = TAG) { "FAILED TO SET PREFERRED INPUT: ${error.value?.localizedDescription}" }
        }
        Logger.d(tag = TAG) { "AUDIO SESSION CONFIGURED SAMPLE_RATE=${session.sampleRate} CHANEL:${session.inputNumberOfChannels}" }
    }

    companion object {
        private const val SAMPLE_RATE = 16000.0
        private const val CHANNEL_COUNT = 1
        private const val BUFFER_SIZE = 1024u
    }
}
