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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.AVFAudio.setPreferredOutputNumberOfChannels
import platform.AVFAudio.setPreferredSampleRate

private const val TAG = "AUDIO_PCM_READER"

@Factory(binds = [IAudioPCMReader::class])
@OptIn(ExperimentalAtomicApi::class, ExperimentalForeignApi::class)
internal actual class AudioPCMReader(
    private val permissions: IRecordPermissionChecker,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IAudioPCMReader {

    @Volatile
    private var _engine: AVAudioEngine? = null

    private val _engineReady = AtomicBoolean(false)

    actual override fun initReader() {
        if (!permissions.hasPermission()) {
            Logger.d(tag = TAG) { "RECORD AUDIO PERMISSION MISSING" }
            return
        }

        if (_engine != null) {
            Logger.d(tag = TAG) { "RECORDER ALREADY INITIATED PLEASE RELEASE IT FIRST" }
            return
        }

        try {
            val audioSession = AVAudioSession.sharedInstance()
            val options = AVAudioSessionCategoryOptionAllowBluetooth or AVAudioSessionCategoryOptionDefaultToSpeaker

            val categorySuccess = audioSession.setCategory(
                category = AVAudioSessionCategoryPlayAndRecord,
                withOptions = options,
                null,
            )
            if (!categorySuccess) {
                Logger.e(tag = TAG) { "FAILED TO SET AUDIO SESSION CATEGORY" }
                return
            }
            audioSession.setMode(AVAudioSessionModeMeasurement, error = null)
            audioSession.setActive(active = true, error = null)
            audioSession.setPreferredSampleRate(16_000.0, null)
            audioSession.setPreferredOutputNumberOfChannels(1L, null)
            audioSession.setPrefersNoInterruptionsFromSystemAlerts(true, null)

            val engine = AVAudioEngine()

            _engine = engine
            _engineReady.compareAndSet(expectedValue = false, newValue = true)

            Logger.d(tag = TAG) { "IOS AUDIO ENGINE INIT SUCCESSFUL" }
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "INVALID ARGUMENTS FOR IOS AUDIO ENGINE" }
            releaseReader()
        }
    }

    actual override fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer> {
        return channelFlow {
            if (!state.canReadAmplitudes) {
                Logger.d(tag = TAG) { "INVALID STATE TO EXPOSE DATA: $state" }
                send(ReadOnlyShortBuffer.empty())
                return@channelFlow
            }

            val input = _engine?.inputNode() ?: run {
                Logger.w(tag = TAG) { "Audio recorder is not initialized" }
                throw RecorderInitMissingException()
            }
            val inputFormat = input.outputFormatForBus(0u)
            val bufferSize = 1024u

            Logger.d(tag = TAG) { "STARTING READING AMPLITUDE DATA VIA AVINPUTNODE TAP" }
            // Remove existing tap if present
            input.removeTapOnBus(0u)

            // Install Audio Tap to stream PCM buffers from hardware mic
            input.installTapOnBus(0u, bufferSize, inputFormat) { buffer: AVAudioPCMBuffer?, _ ->
                if (buffer == null || state != RecorderState.RECORDING) return@installTapOnBus

                val frameLength = buffer.frameLength.toInt()
                if (frameLength <= 0) return@installTapOnBus
                // Extract Int16 raw PCM channels from iOS AVAudioPCMBuffer C-pointer
                val int16Data = buffer.int16ChannelData ?: return@installTapOnBus
                val channelPointer = int16Data[0] ?: return@installTapOnBus
                val pcmArray = ShortArray(frameLength) { index -> channelPointer[index] }
                trySend(ReadOnlyShortBuffer.wrap(pcmArray, frameLength))
            }
            awaitClose {
                Logger.d(tag = TAG) { "REMOVING MULTINODE TAP ON FLOW CLOSE" }
                input.removeTapOnBus(0u)
            }
        }.catch { err ->
            if (err is CancellationException) Logger.d(tag = TAG) { "FLOW READ IS CANCELLED" }
        }.flowOn(dispatchers.io)
    }


    actual override fun start() {
        val engine = _engine ?: run {
            Logger.w(tag = TAG) { "AUDIO RECORDER CANNOT BE NULL FOR START" }
            throw RecorderInitMissingException()
        }

        if (engine.isRunning()) {
            Logger.w(tag = TAG) { "RECORDER STATE RECORDING CANNOT START AGAIN" }
            return
        }

        try {
            engine.prepare()
            val started = engine.startAndReturnError(null)
            if (!started) Logger.e(tag = TAG) { "FAILED TO START AUDIO ENGINE" }
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO START RECORDING" }
        }
    }

    actual override fun stop() {
        val engine = _engine ?: return

        if (!engine.isRunning()) {
            Logger.w(tag = TAG) { "RECORDER STATE STOPPED RECORDING CANNOT STOP AGAIN" }
            return
        }

        try {
            _engine?.inputNode()?.removeTapOnBus(0u)
            engine.stop()
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO STOP RECORDING" }
        }
    }

    actual override fun releaseReader() {
        try {
            _engine?.inputNode?.removeTapOnBus(0u)
            if (_engine?.isRunning() == true) {
                _engine?.stop()
            }
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "UNABLE TO STOP RECORDING AUDIO" }
        }

        try {
            AVAudioSession.sharedInstance().setActive(false, error = null)
            _engine = null
            _engineReady.compareAndSet(expectedValue = true, false)
            Logger.d(tag = TAG) { "RECORDER RELEASED" }
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "FAILED TO RELEASE THE RECORDER" }
        }
    }
}
