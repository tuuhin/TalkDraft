package com.sam.talkdraft.transcription_android

import android.content.Context
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import com.sam.talkdraft.testing.di.TestPlatformModule
import com.sam.talkdraft.transcription_android.assets.AssetsToFileConvertor
import com.sam.talkdraft.transcription_android.models.ProcessingState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.plugin.module.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

@RunWithPlatform
class NativeWhisperTest : KoinTest {

    private lateinit var whisper: NativeWhisper
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
        whisper = NativeWhisper()
        fileProvider = AssetsToFileConvertor(context.assets)
    }

    @AfterTest
    fun tearDown() {
        if (::whisper.isInitialized) {
            whisper.close()
        }
        tempDirectory.delete()
    }


    @Test
    fun test_whisper_init_and_close_step() = runTest {
        val modelPath = prepareWhisperModelFile()

        val isInitialized = whisper.initialize(
            modelPath = modelPath,
            language = "en",
            useGpu = false,
        )

        assertThat(isInitialized).isTrue()
    }


    @Test
    fun test_whisper_reinitialization_throws_illegal_state_exception() = runTest {
        val modelPath = prepareWhisperModelFile()

        whisper.initialize(modelPath = modelPath)

        assertFailure { whisper.initialize(modelPath = modelPath) }.isInstanceOf(
            IllegalStateException::class,
        )
            .hasMessage("A instance of whisper is already running")
    }

    @Test
    fun test_processSamples_when_uninitialized_throws_exception() {
        val sampleAudio = ShortArray(16_000) { 0 }

        assertFailure {
            whisper.processSamples(sampleAudio)
        }.isInstanceOf(IllegalStateException::class)
            .hasMessage("Whisper is not instantiated")
    }

    @Test
    fun test_processSamples_with_empty_array_returns_buffering_state() = runTest {
        val modelPath = prepareWhisperModelFile()
        whisper.initialize(modelPath = modelPath)

        val state = whisper.processSamples(shortArrayOf())
        assertThat(state).isEqualTo(ProcessingState.Buffering)
    }

    @Test
    fun test_processSamples_under_minimum_samples_returns_buffering_state() = runTest {
        val modelPath = prepareWhisperModelFile()
        whisper.initialize(modelPath = modelPath)

        // Feed fewer samples than MIN_INFERENCE_SAMPLES (16,000 samples)
        val shortFrame = ShortArray(8_000) { 0 }
        val state = whisper.processSamples(shortFrame)

        assertThat(state).isEqualTo(ProcessingState.Buffering)
    }

    @Test
    fun test_processSamples_with_wav_file_returns_success_and_valid_state() = runTest {
        val modelPath = prepareWhisperModelFile()
        whisper.initialize(modelPath = modelPath, language = "auto", useGpu = false)

        val tempFolder = tempDirectory.newFolder()
        val audioFile = fileProvider.convertToFile("test_wavs/0.wav", tempFolder)

        // Read WAV audio payload (skipping the 44-byte header) into ShortArray PCM
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

        // Send 16,000 sample (1 second) frames sequentially to trigger inference
        val chunkSize = 16_000
        var offset = 0
        var lastState: ProcessingState = ProcessingState.Buffering

        while (offset < pcmShorts.size) {
            val end = minOf(offset + chunkSize, pcmShorts.size)
            val frame = pcmShorts.copyOfRange(offset, end)

            lastState = whisper.processSamples(frame)
            offset += chunkSize
        }

        assertThat(lastState).isEqualTo(ProcessingState.Success)

        val whisperState = whisper.readState()
        assertThat(whisperState).isNotNull()
        assertThat(whisperState?.text).isNotNull().isNotEmpty()
    }

    @Test
    fun test_readState_and_readError_return_null_when_uninitialized() {
        assertThat(whisper.readState()).isNull()
        assertThat(whisper.readError()).isNull()
    }

    @Test
    fun test_multiple_instances_can_exist_independently() = runTest {
        val modelPath = prepareWhisperModelFile()

        NativeWhisper().use { whisper1 ->
            NativeWhisper().use { whisper2 ->
                val init1 = whisper1.initialize(modelPath = modelPath, useGpu = false)
                val init2 = whisper2.initialize(modelPath = modelPath, useGpu = false)

                assertThat(init1).isTrue()
                assertThat(init2).isTrue()
            }
        }
    }

    @Test
    fun test_multiple_instances_can_process_independently() = runTest {
        val modelPath = prepareWhisperModelFile()

        NativeWhisper().use { whisper1 ->
            NativeWhisper().use { whisper2 ->

                assertThat(
                    whisper1.initialize(modelPath, "en", false),
                ).isTrue()

                assertThat(
                    whisper2.initialize(modelPath, "en", false),
                ).isTrue()

                val audio = ShortArray(16_000)

                assertThat(whisper1.processSamples(audio))
                    .isEqualTo(ProcessingState.Success)

                assertThat(whisper2.processSamples(audio))
                    .isEqualTo(ProcessingState.Success)
            }
        }
    }

    @Test
    fun test_whisper_returns_valid_segment_timing() = runTest {
        val modelPath = prepareWhisperModelFile()

        whisper.initialize(modelPath = modelPath, language = "auto", useGpu = false)

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

        val chunkSize = 16_000
        var offset = 0
        var lastState: ProcessingState = ProcessingState.Buffering

        while (offset < pcmShorts.size) {
            val end = minOf(offset + chunkSize, pcmShorts.size)
            val frame = pcmShorts.copyOfRange(offset, end)

            lastState = whisper.processSamples(frame)
            offset += chunkSize
        }

        assertThat(lastState).isEqualTo(ProcessingState.Success)
        val segment = whisper.readState()

        assertThat(segment).isNotNull()
        assertThat(segment?.text).isNotNull().isNotEmpty()
        assertThat(segment?.startTime).isNotNull().isGreaterThanOrEqualTo(0.seconds)
        assertThat(segment?.endTime).isNotNull().isGreaterThan(segment?.startTime ?: 0.seconds)
    }

    private suspend fun prepareWhisperModelFile(): String = coroutineScope {
        val outDir = tempDirectory.newFolder()
        val op = async(Dispatchers.IO) {
            fileProvider.convertToFile("whisper/tiny.bin", outDir).absolutePath
        }
        op.await()
    }
}
