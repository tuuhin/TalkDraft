package com.sam.talkdraft.transcription_android

import android.content.Context
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import com.sam.talkdraft.testing.di.TestPlatformModule
import com.sam.talkdraft.transcription_android.assets.AssetsToFileConvertor
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
    fun test_if_zip_former_is_initialized() {
        assertThat(zipFormer.isInitialized).isFalse()
    }

    @Test
    fun test_zip_former_init_and_close_step() = runBlocking {
        val outDir = tempDirectory.newFolder()

        val op1 = async {
            fileProvider.convertToFile("zip_former/encoder.onnx", outDir).absolutePath
        }
        val op2 = async {
            fileProvider.convertToFile("zip_former/decoder.onnx", outDir).absolutePath
        }
        val op3 = async {
            fileProvider.convertToFile("zip_former/tokens.txt", outDir).absolutePath
        }
        val op4 = async {
            fileProvider.convertToFile("zip_former/joiner.onnx", outDir).absolutePath
        }
        val isOke = zipFormer.initialize(op1.await(), op2.await(), op4.await(), op3.await())
        assertThat(isOke).isTrue()
        assertThat(zipFormer.isInitialized).isTrue()
    }
}
