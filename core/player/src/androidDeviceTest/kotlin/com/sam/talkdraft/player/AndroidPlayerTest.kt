package com.sam.talkdraft.player

import android.content.Context
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isIn
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import com.sam.talkdraft.common.di.CommonModule
import com.sam.talkdraft.player.di.PlayerModule
import com.sam.talkdraft.player.model.AudioSource
import com.sam.talkdraft.player.model.PlayerPlayBackSpeed
import com.sam.talkdraft.player.model.PlayerPlaybackState
import com.sam.talkdraft.testing.R
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import com.sam.talkdraft.testing.di.TestPlatformModule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.plugin.module.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

@OptIn(ExperimentalCoroutinesApi::class)
@RunWithPlatform
class AndroidPlayerTest : KoinTest {

    private val context by inject<Context>()
    private val player by inject<IAudioPlayer>()

    lateinit var source: AudioSource

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        allowOverride(true)
        module<CommonModule>()
        module<TestPlatformModule>()
        module<PlayerModule>()
    }

    @get:Rule
    val tempFolder = TemporaryFolder()

    @BeforeTest
    fun setup() {
        val file = tempFolder.newFile("example_mp3")
        context.resources.openRawResource(R.raw.example_audio)
            .use { stream ->
                file.outputStream().use { outStream -> stream.copyTo(outStream) }
            }
        source = AudioSource(Uuid.random(), file.absolutePath)

    }

    @AfterTest
    fun cleanup() {
        runBlocking {
            player.release()
        }
        tempFolder.delete()
    }

    @Test
    fun prepare_validAudioSource_returnsSuccessResult() = runTest {
        val result = player.prepare(source)
        advanceUntilIdle()
        assertThat(result).isSuccess()
    }

    @Test
    fun prepare_updatesPlayerStateMetadata() = runTest {
        player.playerState.test {
            player.prepare(source)
            val item = awaitItem()
            advanceUntilIdle()
            assertThat(item).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun play_pause_and_stop_emitsCorrectPlayStates() = runTest {
        player.playerState.test {
            player.prepare(source)
            // Skip initial preparation emissions if needed
            skipItems(1)

            player.play()
            val stateAfterPlay = awaitItem()
            advanceUntilIdle()
            assertThat(stateAfterPlay.playerState)
                .isIn(PlayerPlaybackState.PLAYER_READY, PlayerPlaybackState.BUFFERING)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun seekBy_modifiesTimelinePosition() = runTest {

        player.prepare(source)
        player.play()
        delay(10.milliseconds)
        advanceUntilIdle()

        player.timeline.test {
            // Initial position check
            val initialTimeline = awaitItem()

            // Seek 2 seconds forward
            player.seekBy(delta = 2.seconds, rewind = false)

            val updatedTimeline = awaitItem()
            advanceUntilIdle()

            assertThat(updatedTimeline.current).isGreaterThan(initialTimeline.current)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setPlaybackSpeed_emitsUpdatedSpeedInState() = runTest {
        player.prepare(source)
        player.playerState.test {

            val firstState = awaitItem()
            advanceUntilIdle()
            assertThat(firstState.playBackSpeed)
                .isEqualTo(PlayerPlayBackSpeed.Normal)

            player.setPlayBackSpeed(PlayerPlayBackSpeed.Fast)
            advanceUntilIdle()

            val item = awaitItem()
            advanceUntilIdle()

            assertThat(item.playBackSpeed)
                .isEqualTo(PlayerPlayBackSpeed.Fast)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setPlayLooping_emitsUpdatedLoopingFlag() = runTest {
        player.prepare(source)
        player.playerState.test {

            val firstState = awaitItem()
            advanceUntilIdle()
            assertThat(firstState.isRepeating)
                .isFalse()

            player.setPlayLooping(true)
            advanceUntilIdle()

            val item = awaitItem()
            advanceUntilIdle()

            assertThat(item.isRepeating).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun prepare_invalidFileUri_emitsErrorInErrorFlow() = runTest {
        val invalidSource = AudioSource(Uuid.random(), "/invalid/path/to/non_existent_audio.mp3")

        player.errorFlow.test {
            player.prepare(invalidSource)

            val throwable = awaitItem()
            advanceUntilIdle()
            assertThat(throwable).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
