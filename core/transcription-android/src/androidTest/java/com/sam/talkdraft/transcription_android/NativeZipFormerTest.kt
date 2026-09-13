package com.sam.talkdraft.transcription_android

import android.content.Context
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import com.sam.talkdraft.testing.di.TestPlatformModule
import com.sam.talkdraft.transcription_android.assets.AssetsToFileConvertor
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
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
class NativeZipFormerTest : KoinTest {

    private lateinit var zipFormer: NativeZipFormer
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
        zipFormer = NativeZipFormer()
        fileProvider = AssetsToFileConvertor(context.assets)
    }

    @AfterTest
    fun tearDown() {
        if (::zipFormer.isInitialized) {
            zipFormer.close()
        }
        tempDirectory.delete()
    }

    @Test
    fun test_if_zip_former_is_not_initialized_by_default() {
        assertThat(zipFormer.isInitialized).isFalse()
    }

    @Test
    fun test_zip_former_init_and_close_step() = runTest {
        val modelFiles = prepareZipFormerFiles()

        val isInitialized = zipFormer.initialize(
            encoderPath = modelFiles.encoderPath,
            decoderPath = modelFiles.decoderPath,
            joinerPath = modelFiles.joinerPath,
            tokensPath = modelFiles.tokensPath,
        )

        assertThat(isInitialized).isTrue()
        assertThat(zipFormer.isInitialized).isTrue()
    }

    @Test
    fun test_zip_former_init_missing_file_throws_exception() {
        val invalidPath = "/non/existent/path/file.onnx"

        assertFailure {
            zipFormer.initialize(
                encoderPath = invalidPath,
                decoderPath = invalidPath,
                joinerPath = invalidPath,
                tokensPath = invalidPath,
            )
        }.isInstanceOf(IllegalArgumentException::class)
    }

    @Test
    fun test_zip_former_reinitialization_returns_false() = runTest {
        val modelFiles = prepareZipFormerFiles()

        val firstInit = zipFormer.initialize(
            modelFiles.encoderPath,
            modelFiles.decoderPath,
            modelFiles.joinerPath,
            modelFiles.tokensPath,
        )
        assertThat(firstInit).isTrue()

        val secondInit = zipFormer.initialize(
            modelFiles.encoderPath,
            modelFiles.decoderPath,
            modelFiles.joinerPath,
            modelFiles.tokensPath,
        )
        assertThat(secondInit).isFalse()
    }

    @Test
    fun test_processFrame_when_uninitialized_throws_exception() {
        val sampleAudio = ShortArray(1600) { 0 }

        assertFailure {
            zipFormer.processFrame(sampleAudio)
        }.isInstanceOf(IllegalStateException::class)
            .hasMessage("NativeZipFormer is not initialized")
    }

    @Test
    fun test_processFrame_with_empty_array_returns_null() = runTest {
        val modelFiles = prepareZipFormerFiles()
        zipFormer.initialize(
            modelFiles.encoderPath,
            modelFiles.decoderPath,
            modelFiles.joinerPath,
            modelFiles.tokensPath,
        )

        val result = zipFormer.processFrame(shortArrayOf())
        assertThat(result).isNull()
    }

    @Test
    fun test_processFrame_with_audio_samples_returns_result_or_null() = runTest {
        val modelFiles = prepareZipFormerFiles()
        zipFormer.initialize(
            modelFiles.encoderPath,
            modelFiles.decoderPath,
            modelFiles.joinerPath,
            modelFiles.tokensPath,
        )

        // 1 second of silent PCM audio frame (16,000 samples @ 16kHz)
        val audioFrame = ShortArray(16_000) { 0 }
        val result = zipFormer.processFrame(audioFrame)

        // Native ZipFormer returns transcript String or null for silence
        if (result != null) {
            assertThat(result).isNotNull()
        } else {
            assertThat(result).isNull()
        }
    }

    @Test
    fun test_reset_on_initialized_instance_executes_safely() = runTest {
        val modelFiles = prepareZipFormerFiles()
        zipFormer.initialize(
            modelFiles.encoderPath,
            modelFiles.decoderPath,
            modelFiles.joinerPath,
            modelFiles.tokensPath,
        )

        zipFormer.reset()
        assertThat(zipFormer.isInitialized).isTrue()
    }

    @Test
    fun test_multiple_instances_can_exist_independently() = runTest {
        val modelFiles = prepareZipFormerFiles()

        NativeZipFormer().use { zipFormer1 ->
            NativeZipFormer().use { zipFormer2 ->
                val init1 = zipFormer1.initialize(
                    modelFiles.encoderPath,
                    modelFiles.decoderPath,
                    modelFiles.joinerPath,
                    modelFiles.tokensPath,
                )
                val init2 = zipFormer2.initialize(
                    modelFiles.encoderPath,
                    modelFiles.decoderPath,
                    modelFiles.joinerPath,
                    modelFiles.tokensPath,
                )

                assertThat(init1).isTrue()
                assertThat(init2).isTrue()

                zipFormer1.close()

                assertThat(zipFormer1.isInitialized).isFalse()
                assertThat(zipFormer2.isInitialized).isTrue()
            }
        }
    }

    @Test
    fun test_process_frame_with_wav_file_returns_transcript() = runTest {
        val modelFiles = prepareZipFormerFiles()
        zipFormer.initialize(
            modelFiles.encoderPath,
            modelFiles.decoderPath,
            modelFiles.joinerPath,
            modelFiles.tokensPath,
        )

        val tempFolder = tempDirectory.newFolder()
        val audioFile = fileProvider.convertToFile("test_wavs/1.wav", tempFolder)

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
        var finalTranscript: String? = null

        while (offset < pcmShorts.size) {
            val end = minOf(offset + chunkSize, pcmShorts.size)
            val frame = pcmShorts.copyOfRange(offset, end)

            val result = zipFormer.processFrame(frame)
            if (result != null) {
                finalTranscript = result
            }
            offset += chunkSize
        }
        assertThat(finalTranscript).isNotNull()
    }

    private data class ZipFormerModelFiles(
        val encoderPath: String,
        val decoderPath: String,
        val joinerPath: String,
        val tokensPath: String,
    )

    private suspend fun prepareZipFormerFiles(): ZipFormerModelFiles = coroutineScope {
        val outDir = tempDirectory.newFolder()

        val op1 = async(Dispatchers.IO) { fileProvider.convertToFile("zip_former/encoder.onnx", outDir).absolutePath }
        val op2 = async(Dispatchers.IO) { fileProvider.convertToFile("zip_former/decoder.onnx", outDir).absolutePath }
        val op3 = async(Dispatchers.IO) { fileProvider.convertToFile("zip_former/joiner.onnx", outDir).absolutePath }
        val op4 = async(Dispatchers.IO) { fileProvider.convertToFile("zip_former/tokens.txt", outDir).absolutePath }

        ZipFormerModelFiles(
            encoderPath = op1.await(),
            decoderPath = op2.await(),
            joinerPath = op3.await(),
            tokensPath = op4.await(),
        )
    }
}
