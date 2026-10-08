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
class NativePunctuationWriterTest : KoinTest {

    @get:Rule
    val tempDirectory = TemporaryFolder()

    private lateinit var fileProvider: AssetsToFileConvertor
    private lateinit var punctuationWriter: NativePunctuationWriter

    private val context by inject<Context>()

    @get:Rule
    val koinRule = KoinTestRule.create {
        module<TestPlatformModule>()
    }

    @BeforeTest
    fun setup() {
        punctuationWriter = NativePunctuationWriter()
        fileProvider = AssetsToFileConvertor(context.assets)
    }

    @AfterTest
    fun tearDown() {
        if (::punctuationWriter.isInitialized) {
            punctuationWriter.close()
        }
        tempDirectory.delete()
    }

    @Test
    fun test_isInitialized_returns_false_initially() {
        assertThat(punctuationWriter.isInitialized).isFalse()
    }

    @Test
    fun test_processText_when_uninitialized_throws_illegal_state_exception() {
        assertFailure {
            punctuationWriter.processText("hello world")
        }.isInstanceOf(IllegalStateException::class)
    }

    @Test
    fun test_initialize_with_invalid_model_path_throws_illegal_state_exception() {
        assertFailure {
            punctuationWriter.initialize(
                "/invalid/path/to/non_existent_model.onnx",
                vocalPath = "/invalid/path/to/vocalbs.vocab",
            )
        }.isInstanceOf(IllegalStateException::class)
        assertThat(punctuationWriter.isInitialized).isFalse()
    }

    @Test
    fun test_initialize_with_valid_model_returns_true_and_sets_isInitialized() = runTest {
        val modelFiles = preparePunctuationModelFiles()
        val result = punctuationWriter.initWithFiles(modelFiles)
        assertThat(result).isTrue()
        assertThat(punctuationWriter.isInitialized).isTrue()
    }


    @Test
    fun test_processText_with_empty_string_returns_null() = runTest {
        val modelFiles = preparePunctuationModelFiles()
        punctuationWriter.initWithFiles(modelFiles)
        val result = punctuationWriter.processText("")
        assertThat(result).isNull()
    }

    @Test
    fun test_processText_with_valid_input_returns_punctuated_text() = runTest {
        val modelFiles = preparePunctuationModelFiles()
        punctuationWriter.initWithFiles(modelFiles)
        val result = punctuationWriter.processText("how are you doing today")
        assertThat(result).isNotNull()
        assertThat(result?.isNotEmpty() == true).isTrue()
    }

    @Test
    fun test_processText_multiple_calls_succeed() = runTest {
        val modelFiles = preparePunctuationModelFiles()
        punctuationWriter.initWithFiles(modelFiles)

        val firstResult = punctuationWriter.processText("hello world")
        val secondResult = punctuationWriter.processText("what time is it")

        assertThat(firstResult).isNotNull()
        assertThat(secondResult).isNotNull()
    }

    @Test
    fun test_close_resets_isInitialized_and_subsequent_processText_fails() = runTest {
        val modelFiles = preparePunctuationModelFiles()
        punctuationWriter.initWithFiles(modelFiles)
        assertThat(punctuationWriter.isInitialized).isTrue()

        punctuationWriter.close()

        assertThat(punctuationWriter.isInitialized).isFalse()
        assertFailure {
            punctuationWriter.processText("hello world")
        }.isInstanceOf(IllegalStateException::class)
            .hasMessage("Punctuations writer is not ready initialize it to continue")
    }

    @Test
    fun test_close_is_idempotent() = runTest {
        punctuationWriter.close()
        punctuationWriter.close()

        val modelFiles = preparePunctuationModelFiles()
        punctuationWriter.initWithFiles(modelFiles)

        punctuationWriter.close()
        punctuationWriter.close()

        assertThat(punctuationWriter.isInitialized).isFalse()
    }

    @Test
    fun test_can_reinitialize_after_close() = runTest {
        val modelFiles = preparePunctuationModelFiles()
        punctuationWriter.initWithFiles(modelFiles)
        punctuationWriter.close()
        assertThat(punctuationWriter.isInitialized).isFalse()

        val reinitResult =
            punctuationWriter.initWithFiles(modelFiles)
        assertThat(reinitResult).isTrue()
        assertThat(punctuationWriter.isInitialized).isTrue()

        val result = punctuationWriter.processText("testing reinitialization")
        assertThat(result).isNotNull()
    }

    @Test
    fun test_use_block_automatically_closes_instance() = runTest {
        val modelFiles = preparePunctuationModelFiles()
        lateinit var writerRef: NativePunctuationWriter

        NativePunctuationWriter().use { writer ->
            writerRef = writer
            writer.initWithFiles(modelFiles)
            assertThat(writer.isInitialized).isTrue()

            val result = writer.processText("inside use block")
            assertThat(result).isNotNull()
        }

        assertThat(writerRef.isInitialized).isFalse()
    }

    @Test
    fun test_multiple_instances_can_exist_and_operate_independently() = runTest {
        val modelFiles = preparePunctuationModelFiles()

        NativePunctuationWriter().use { writer1 ->
            NativePunctuationWriter().use { writer2 ->
                val init1 = writer1.initWithFiles(modelFiles)
                val init2 = writer2.initWithFiles(modelFiles)

                assertThat(init1).isTrue()
                assertThat(init2).isTrue()

                val res1 = writer1.processText("first instance text")
                val res2 = writer2.processText("second instance text")

                assertThat(res1).isNotNull()
                assertThat(res2).isNotNull()
            }
        }
    }

    private data class PunctuationModelFiles(
        val modelPath: String,
        val vocabPath: String,
    )

    private fun NativePunctuationWriter.initWithFiles(files: PunctuationModelFiles): Boolean {
        return initialize(files.modelPath, files.vocabPath)
    }

    private suspend fun preparePunctuationModelFiles(): PunctuationModelFiles = coroutineScope {
        val outDir = tempDirectory.newFolder()
        val modelOp = async(Dispatchers.IO) {
            fileProvider.convertToFile("punctuation/model.int8.onnx", outDir).absolutePath
        }
        val vocabOp = async(Dispatchers.IO) {
            fileProvider.convertToFile("punctuation/bpe.vocab", outDir).absolutePath
        }
        PunctuationModelFiles(
            modelPath = modelOp.await(),
            vocabPath = vocabOp.await(),
        )
    }
}
