package com.sam.talkdraft.transcription_android

import android.content.Context
import android.content.res.AssetManager
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isLessThan
import assertk.assertions.isTrue
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import com.sam.talkdraft.testing.di.TestPlatformModule
import com.sam.talkdraft.transcription_android.assets.AssetsToFileConvertor
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
    private lateinit var assertsManager: AssetManager
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
        assertsManager = context.assets
        fileProvider = AssetsToFileConvertor(assertsManager)
    }

    @AfterTest
    fun tearDown() {
        if (::vad.isInitialized) vad.close()
        tempDirectory.delete()
    }

    @Test
    fun test_vad_init_should_be_false() {
        assertThat(vad.isInitialized).isFalse()
    }

    @Test
    fun test_vad_setup_with_asserts_manager() {
        val isOke = vad.initialize(assertsManager, NativeVoiceActivityDetector.MODEL_NAME)
        assertThat(isOke).isTrue()

        assertThat(vad.isInitialized).isTrue()
    }

    @Test
    fun test_vad_init_with_model_path() = runTest {
        val tempFile = tempDirectory.newFolder()
        val finalPath = fileProvider.convertToFile(NativeVoiceActivityDetector.MODEL_NAME, tempFile)

        val isOke = vad.initialize(finalPath.absolutePath)
        assertThat(isOke).isTrue()
        assertThat(vad.isInitialized).isTrue()
    }

    @Test
    fun test_processFrame_with_short_array_returns_valid_Result() {
        vad.initialize(assertsManager, NativeVoiceActivityDetector.MODEL_NAME)

        // 512 samples of PCM 16-bit silence
        val silenceShorts = ShortArray(512) { 0 }
        val result = vad.processFrame(silenceShorts)

        assertThat(result.probability).isLessThan(0.1f)
        assertThat(result.isSpeech).isFalse()

        vad.close()
    }

    @Test
    fun test_multiple_instances_can_exist_independently() {
        val vad1 = NativeVoiceActivityDetector()
        val vad2 = NativeVoiceActivityDetector()

        assertThat(vad1.initialize(assertsManager, NativeVoiceActivityDetector.MODEL_NAME)).isTrue()
        assertThat(vad2.initialize(assertsManager, NativeVoiceActivityDetector.MODEL_NAME)).isTrue()

        assertThat(vad1.isInitialized).isTrue()
        assertThat(vad2.isInitialized).isTrue()

        vad1.close()
        // vad2 should still remain valid and initialized
        assertThat(vad2.isInitialized).isTrue()
        vad2.close()
    }

}
