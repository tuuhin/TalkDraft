package com.sam.talkdraft.transcription_android

import android.content.Context
import android.content.res.AssetManager
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.isTrue
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import com.sam.talkdraft.testing.di.TestPlatformModule
import com.sam.talkdraft.transcription_android.assets.AssetsToFileConvertor
import com.sam.talkdraft.transcription_android.models.VoiceDetectionProbability
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.plugin.module.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

@RunWithPlatform
class NativeVADTest : KoinTest {

    private lateinit var vad: NativeVoiceActivityDetector
    private lateinit var assetManager: AssetManager
    private lateinit var fileProvider: AssetsToFileConvertor

    private val context by inject<Context>()

    @get:Rule
    val koinRule = KoinTestRule.create {
        module<TestPlatformModule>()
    }

    @get:Rule
    val tempDirectory = TemporaryFolder()

    @BeforeTest
    fun setup() {
        vad = NativeVoiceActivityDetector()
        assetManager = context.assets
        fileProvider = AssetsToFileConvertor(assetManager)
    }

    @AfterTest
    fun tearDown() {
        if (::vad.isInitialized) {
            vad.close()
        }
        tempDirectory.delete()
    }

    @Test
    fun test_vad_init_should_be_false_by_default() {
        assertThat(vad.isInitialized).isFalse()
    }

    @Test
    fun test_vad_setup_with_asset_manager() {
        val isInitialized = vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)

        assertThat(isInitialized).isTrue()
        assertThat(vad.isInitialized).isTrue()
    }

    @Test
    fun test_vad_init_with_model_path() = runTest {
        val tempFolder = tempDirectory.newFolder()
        val modelFile = fileProvider.convertToFile(NativeVoiceActivityDetector.MODEL_NAME, tempFolder)

        val isInitialized = vad.initialize(modelFile.absolutePath)

        assertThat(isInitialized).isTrue()
        assertThat(vad.isInitialized).isTrue()
    }

    @Test
    fun test_vad_initialize_twice_throws_illegal_state_exception() {
        vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)

        assertFailure {
            vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)
        }.isInstanceOf(IllegalStateException::class)
            .hasMessage("Voice detector is already initialized close it to continue")
    }

    @Test
    fun test_vad_initialize_unsupported_sample_rate_throws_exception() {
        assertFailure {
            vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME, sampleRate = 44100)
        }.isInstanceOf(IllegalArgumentException::class)
            .hasMessage("VAD requires 8kHz or 16kHz sample rate.")
    }

    @Test
    fun test_processFrame_when_uninitialized_throws_exception() {
        val silenceShorts = ShortArray(640) { 0 }

        assertFailure {
            vad.processFrame(silenceShorts)
        }.isInstanceOf(IllegalStateException::class)
            .hasMessage("Voice recorder is not initialized make sure its done first")
    }

    @Test
    fun test_processFrame_with_empty_array_returns_zero_probability() {
        vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)

        val emptyFrame = ShortArray(0)
        val result = vad.processFrame(emptyFrame)

        assertThat(result.probability).isEqualTo(0f)
    }

    @Test
    fun test_processFrame_with_silence_returns_low_probability() {
        vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)

        // Pass 640 samples (SILERO_SAMPLE_COUNT) to trigger inference
        val silenceFrame = ShortArray(640) { 0 }
        val result = vad.processFrame(silenceFrame)

        assertThat(result.probability).isLessThan(0.1f)
        assertThat(result.isSpeech).isFalse()
    }

    @Test
    fun test_process_frame_streaming_entire_wav_file() = runTest {
        vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)

        val tempFolder = tempDirectory.newFolder()
        val audioFile = fileProvider.convertToFile("test_wavs/0.wav", tempFolder)

        val pcmShorts = audioFile.inputStream().use { stream ->
            val fileBytes = stream.readBytes()
            val headerSize = 44
            val audioBytes = fileBytes.copyOfRange(headerSize, fileBytes.size)

            ShortArray(audioBytes.size / 2) { i ->
                val low = audioBytes[i * 2].toInt() and 0xFF
                val high = audioBytes[i * 2 + 1].toInt()
                ((high shl 8) or low).toShort()
            }
        }

        val windowSize = 640
        var probability = VoiceDetectionProbability(0.0f)
        var offset = 0

        while (offset + windowSize <= pcmShorts.size) {
            val frame = pcmShorts.copyOfRange(offset, offset + windowSize)
            val result = vad.processFrame(frame)
            if (result.probability > probability.probability) {
                probability = result
            }
            offset += windowSize
        }

        assertThat(probability.probability).isGreaterThan(0.7f)
        assertThat(probability.isSpeech).isTrue()
    }

    @Test
    fun test_resetState_clears_pending_buffers_without_error() {
        vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)

        // Push partial frame (< 640 samples)
        vad.processFrame(ShortArray(300) { 100 })

        // Reset state should clear partial samples
        vad.resetState()

        val freshFrameResult = vad.processFrame(ShortArray(640) { 0 })
        assertThat(freshFrameResult.probability).isLessThan(0.1f)
    }

    @Test
    fun test_pending_sample_buffer_overflow_throws_exception() {
        vad.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)

        // Overflow buffer (> 32,000 samples)
        val massiveChunk = ShortArray(35_000) { 0 }

        assertFailure {
            vad.processFrame(massiveChunk)
        }.isInstanceOf(IllegalStateException::class)
    }

    @Test
    fun test_multiple_instances_can_exist_independently() {
        NativeVoiceActivityDetector().use { vad1 ->
            NativeVoiceActivityDetector().use { vad2 ->
                assertThat(vad1.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)).isTrue()
                assertThat(vad2.initialize(assetManager, NativeVoiceActivityDetector.MODEL_NAME)).isTrue()

                assertThat(vad1.isInitialized).isTrue()
                assertThat(vad2.isInitialized).isTrue()

                vad1.close()

                // vad2 should remain valid and initialized after vad1 is closed
                assertThat(vad1.isInitialized).isFalse()
                assertThat(vad2.isInitialized).isTrue()
            }
        }
    }
}
