package com.sam.talkdraft.recorder

import app.cash.turbine.test
import com.sam.talkdraft.recorder.di.TestRecorderModule
import com.sam.talkdraft.recorder.domain.IAudioPCMReader
import com.sam.talkdraft.recorder.domain.exception.RecorderInitMissingException
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.utils.GrantOrRequestPermissionRule
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
@RunWithPlatform
class AudioPCMReaderTest : KoinComponent {

    private val pcmReader by inject<IAudioPCMReader>()
    private val grantRule by inject<GrantOrRequestPermissionRule>()

    @BeforeTest
    fun setup() {
        startKoin {
            module<TestRecorderModule>()
        }
        grantRule.grantAudioPermission()
    }

    @AfterTest
    fun cleanup() {
        stopKoin()
        pcmReader.releaseReader()
    }

    @Test
    fun test_start_without_init_throws_exception() {
        assertFailsWith<RecorderInitMissingException> { pcmReader.start() }
    }

    @Test
    fun test_read_without_init_throws_exception() = runTest {
        assertFailsWith<RecorderInitMissingException> {
            pcmReader.readRecorderRawBytes(RecorderState.RECORDING).first()
            advanceUntilIdle()
        }
    }

    @Test
    fun test_multiple_init_calls_are_handled_safely() {
        pcmReader.initReader()
        pcmReader.initReader()
        pcmReader.start()
        pcmReader.stop()
    }

    @Test
    fun test_reinitialization_after_release_works() = runTest {
        pcmReader.initReader()
        pcmReader.start()
        pcmReader.stop()
        pcmReader.releaseReader()

        pcmReader.initReader()
        pcmReader.start()

        pcmReader.readRecorderRawBytes(RecorderState.RECORDING).test {
            val buffer = awaitItem()
            advanceUntilIdle()
            assertTrue("Buffer must contain samples after re-initialization") { buffer.size > 0 }
            cancelAndIgnoreRemainingEvents()
        }
        pcmReader.stop()
    }


    @Test
    fun test_double_start_is_safe_noop() {
        pcmReader.initReader()
        pcmReader.start()
        pcmReader.start()
        pcmReader.stop()
    }

    @Test
    fun test_double_stop_is_safe_noop() {
        pcmReader.initReader()
        pcmReader.start()
        pcmReader.stop()
        pcmReader.stop()
    }


    @Test
    fun test_real_mic_recording_on_real_hardware() = runTest {
        pcmReader.initReader()
        pcmReader.start()

        pcmReader.readRecorderRawBytes(RecorderState.RECORDING).test {
            val buffer = awaitItem()
            advanceUntilIdle()

            assertFalse("Buffer must not be empty") { buffer.isEmpty }
            assertTrue("Buffer size must be greater than zero") { buffer.size > 0 }

            cancelAndIgnoreRemainingEvents()
        }

        pcmReader.stop()
    }

    @Test
    fun test_readRecorderRawBytes_emits_empty_buffer_when_state_cannot_read() = runTest {
        pcmReader.initReader()
        pcmReader.start()

        val nonReadableState = RecorderState.IDLE
        pcmReader.readRecorderRawBytes(nonReadableState).test {
            val buffer = awaitItem()
            advanceUntilIdle()
            assertTrue("Should immediately emit empty buffer for unreadable state") { buffer.isEmpty }
            awaitComplete()
        }
        pcmReader.stop()
    }

    @Test
    fun test_flow_cancellation_stops_reading_gracefully() = runTest {
        pcmReader.initReader()
        pcmReader.start()

        val flow = pcmReader.readRecorderRawBytes(RecorderState.RECORDING)

        flow.test {
            awaitItem() // First frame received
            awaitItem() // Second frame received
            // Cancel subscription while recorder is still actively reading
            cancelAndIgnoreRemainingEvents()
        }
        pcmReader.stop()
    }
}
