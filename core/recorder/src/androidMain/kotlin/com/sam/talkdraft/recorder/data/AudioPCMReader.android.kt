package com.sam.talkdraft.recorder.data

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.recorder.domain.IAudioPCMReader
import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import com.sam.talkdraft.recorder.domain.exception.RecorderInitMissingException
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.koin.core.annotation.Factory

private const val TAG = "AudioPCMReader"

@SuppressLint("MissingPermission")
@Factory(binds = [IAudioPCMReader::class])
internal actual class AudioPCMReader(
    private val permissions: IRecordPermissionChecker,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IAudioPCMReader {

    @Volatile
    private var _recorder: AudioRecord? = null

    @Volatile
    private var _pcmBufferSize: Int = 0

    private val errorCodes =
        arrayOf(AudioRecord.ERROR_INVALID_OPERATION, AudioRecord.ERROR_BAD_VALUE, AudioRecord.ERROR)

    actual override fun initReader() {
        if (!permissions.hasPermission()) {
            Logger.d(tag = TAG) { "RECORD AUDIO PERMISSION MISSING" }
            return
        }

        if (_recorder != null) {
            Logger.d(tag = TAG) { "RECORDER ALREADY INITIATED PLEASE RELEASE IT FIRST" }
            return
        }

        val sampleRate = 16_000
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val channelCount = 1
        val bytesPerSample = 2

        try {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Logger.w(tag = TAG) { "CANNOT INITIATE BUFFER SIZE BUFFER SIZ" }
                return
            }

            _pcmBufferSize = bufferSize / (bytesPerSample * channelCount)
            Logger.d(tag = TAG) { "GRANTED A BUFFER SIZE OF :$_pcmBufferSize" }

            _recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2,
            )

            Logger.d(tag = TAG) { "RECORDER READY" }

            if (_recorder?.state != AudioRecord.STATE_INITIALIZED) {
                Logger.e(tag = TAG) { "AUDIO INIT FAILED RELEASING RECORDER" }
                releaseReader()
                return
            }
        } catch (_: IllegalArgumentException) {
            Logger.e(tag = TAG) { "INVALID ARGUMENTS IN FOR RECORDER" }
        }

    }

    actual override fun start() {
        if (_recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            Logger.w(tag = TAG) { "RECORDER STATE RECORDING CANNOT START AGAIN" }
            return
        }
        if (_recorder == null) {
            Logger.w(tag = TAG) { "AUDIO RECORDER CANNOT BE NULL FOR START" }
            throw RecorderInitMissingException()
        }
        try {
            _recorder?.startRecording()
        } catch (e: IllegalStateException) {
            Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO START RECORDING" }
        }
    }

    actual override fun stop() {
        if (_recorder?.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
            Logger.w(tag = TAG) { "RECORDER STATE STOPPED RECORDING CANNOT STOP AGAIN" }
            return
        }
        try {
            _recorder?.stop()
        } catch (e: IllegalStateException) {
            Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO STOP RECORDING" }
        }
    }

    actual override fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer> {
        return flow {
            // reset the state based on the state
            if (!state.canReadAmplitudes) {
                Logger.d(tag = TAG) { "INVALID STATE TO EXPOSE DATA: $state" }
                emit(ReadOnlyShortBuffer.empty())
                return@flow
            }
            val recorder = _recorder ?: run {
                Logger.w(tag = TAG) { "Audio recorder is not initialized" }
                throw RecorderInitMissingException()
            }

            try {
                val pcmBuffer = ShortArray(_pcmBufferSize)
                var shortsRead: Int

                Logger.d(tag = TAG) { "STARING READING AMPLITUDE DATA" }

                while (state == RecorderState.RECORDING && currentCoroutineContext().isActive) {
                    // ensure the current coroutine is active otherwise
                    shortsRead = _recorder?.read(pcmBuffer, 0, pcmBuffer.size) ?: break
                    if (shortsRead in errorCodes || shortsRead == 0) break
                    val frameSnapshot = pcmBuffer.copyOf(shortsRead)

                    emit(ReadOnlyShortBuffer.wrap(frameSnapshot, shortsRead))
                }
            } catch (e: Exception) {
                if (e is CancellationException)
                    Logger.d(tag = TAG) { "AMPLITIDE DATA COLLECTION HAS BEEN CANCELLED" }
                throw e
            }
        }.flowOn(dispatchers.io)
    }

    actual override fun releaseReader() {
        try {
            if (_recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                Logger.d(tag = TAG) { "AUDIO RECORDING STOPPED" }
                _recorder?.stop()
            }
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "UNABLE TO STOP RECORDING AUDIO" }
        }

        try {
            _recorder?.release()
            _recorder = null
            _pcmBufferSize = 0
            Logger.d(tag = TAG) { "RECORDER RELEASED" }
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "FAILED TO RELEASE THE RECORDER" }
        }
    }
}
