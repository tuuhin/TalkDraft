package com.sam.talkdraft.recorder.data

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.recorder.domain.IAudioPCMReader
import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import com.sam.talkdraft.recorder.domain.exception.RecorderInitMissingException
import com.sam.talkdraft.recorder.domain.models.RecorderState
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

private const val TAG = "AudioPCMReader"

@SuppressLint("MissingPermission")
@Factory(binds = [IAudioPCMReader::class])
internal actual class AudioPCMReaderImpl(
    private val permissions: IRecordPermissionChecker,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IAudioPCMReader {

    @Volatile
    private var _recorder: AudioRecord? = null

    @Volatile
    private var _pcmBufferSize: Int = 0

    actual override suspend fun initReader() {
        if (!permissions.hasPermission()) {
            Logger.d(tag = TAG) { "RECORD AUDIO PERMISSION MISSING" }
            return
        }

        if (_recorder != null) {
            Logger.d(tag = TAG) { "RECORDER ALREADY INITIATED PLEASE RELEASE IT FIRST" }
            return
        }

        withContext(dispatchers.io) {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    return@withContext
                }

                // we are doing 4 times of the buffer size provided
                // a larger buffers help to accumulate the n amount of data points
                // required for transcription data
                val internalBufferSize = (minBufferSize * 4)
                    .coerceAtLeast(16000 * BYTES_PER_SAMPLE)

                // but the preferable chunk sample size is 1600
                _pcmBufferSize = CHUNK_SAMPLE_COUNT
                Logger.d(tag = TAG) { "GRANTED A BUFFER SIZE OF :$_pcmBufferSize" }

                _recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    internalBufferSize,
                )

                Logger.d(tag = TAG) { "RECORDER READY" }

                if (_recorder?.state != AudioRecord.STATE_INITIALIZED) {
                    Logger.e(tag = TAG) { "AUDIO INIT FAILED RELEASING RECORDER" }
                    releaseReader()
                    return@withContext
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: IllegalArgumentException) {
                Logger.e(tag = TAG) { "INVALID ARGUMENTS IN FOR RECORDER" }
            }
        }
    }

    actual override suspend fun start() {
        withContext(dispatchers.io) {
            if (_recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                Logger.w(tag = TAG) { "RECORDER STATE RECORDING CANNOT START AGAIN" }
                return@withContext
            }
            if (_recorder == null) {
                Logger.w(tag = TAG) { "AUDIO RECORDER CANNOT BE NULL FOR START" }
                throw RecorderInitMissingException()
            }
            try {
                Logger.i(tag = TAG) { "STARTING AUDIO RECORD" }
                _recorder?.startRecording()
                Logger.i(tag = TAG) { "AUDIO RECORD STARTED RECORDING" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO START RECORDING" }
            }
        }
    }

    actual override suspend fun stop() {
        withContext(dispatchers.io) {
            if (_recorder?.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                Logger.w(tag = TAG) { "RECORDER STATE STOPPED RECORDING CANNOT STOP AGAIN" }
                return@withContext
            }
            try {
                _recorder?.stop()
                Logger.d(tag = TAG) { "AUDIO RECORD HAS BEEN STOPPED" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                Logger.e(tag = TAG, throwable = e) { "WRONG STATE TO STOP RECORDING" }
            }
        }
    }

    actual override fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer> {
        return flow {
            // reset the state based on the state
            if (!state.canReadAmplitudes || state != RecorderState.RECORDING) {
                Logger.d(tag = TAG) { "INVALID STATE TO EXPOSE DATA: $state" }
                emit(ReadOnlyShortBuffer.empty())
                return@flow
            }
            val recorder = _recorder ?: run {
                Logger.w(tag = TAG) { "Audio recorder is not initialized" }
                throw RecorderInitMissingException()
            }

            try {
                val bufferSize = if (_pcmBufferSize > 0) _pcmBufferSize else CHUNK_SAMPLE_COUNT
                val pcmBuffer = ShortArray(bufferSize)

                Logger.i(tag = TAG) { "WAITING FOR THE RECORDER TO MOVE TO RECORDING STATE" }
                // a bit of delay
                while (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING && currentCoroutineContext().isActive) {
                    delay(10.milliseconds)
                }

                Logger.i(tag = TAG) { "STARTING READING AMPLITUDE DATA" }

                while (currentCoroutineContext().isActive && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val shortsRead = recorder.read(pcmBuffer, 0, pcmBuffer.size, AudioRecord.READ_NON_BLOCKING)
                    when {
                        // in-case we get an empty shorts read
                        shortsRead == 0 -> delay(10.milliseconds)
                        // if the error codes are empty
                        shortsRead in errorCodes -> {
                            Logger.w(tag = TAG) { "AudioRecord read returned error code: $shortsRead" }
                            if (shortsRead == AudioRecord.ERROR_INVALID_OPERATION && recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                                break
                            }
                            delay(10.milliseconds)
                        }
                        // if we have the data emit it
                        shortsRead > 0 -> {
                            val frameSnapshot = pcmBuffer.copyOf(shortsRead)
                            emit(ReadOnlyShortBuffer.wrap(frameSnapshot, shortsRead))
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException)
                    Logger.d(tag = TAG) { "AMPLITUDE DATA COLLECTION HAS BEEN CANCELLED" }
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

    companion object {
        private val errorCodes = arrayOf(
            AudioRecord.ERROR_INVALID_OPERATION,
            AudioRecord.ERROR_BAD_VALUE,
            AudioRecord.ERROR,
        )

        // DO_NOT CHANGE SAMPLE RATE
        private const val SAMPLE_RATE = 16_000
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val BYTES_PER_SAMPLE = 2
        private const val CHUNK_SAMPLE_COUNT = 1600
    }
}
