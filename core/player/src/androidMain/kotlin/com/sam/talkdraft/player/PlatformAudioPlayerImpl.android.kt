package com.sam.talkdraft.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.ext.tryWithLock
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.player.exceptions.PlayerCommandNotFoundException
import com.sam.talkdraft.player.model.AudioSource
import com.sam.talkdraft.player.model.PlayerPlayBackSpeed
import com.sam.talkdraft.player.model.PlayerPlayItemMetadata
import com.sam.talkdraft.player.model.PlayerTimeline
import com.sam.talkdraft.player.utils.AudioPlayerListener
import com.sam.talkdraft.player.utils.computePlayerTrackData
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

private const val TAG = "ANDROID_AUDIO_PLAYER"

@Factory(binds = [IAudioPlayer::class])
internal actual class PlatformAudioPlayerImpl(
    private val player: Player,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IAudioPlayer {

    private val _listener by lazy { AudioPlayerListener(player) }
    private val _lock = Mutex()

    actual override val playerState: Flow<PlayerPlayItemMetadata>
        get() = _listener.playerMetaDataFlow

    actual override val timeline: Flow<PlayerTimeline>
        get() = player.computePlayerTrackData()

    actual override val errorFlow: Flow<Throwable>
        get() = _listener.errorFlow

    actual override suspend fun prepare(source: AudioSource): Result<Boolean> = withContext(dispatchers.main) {
        val command = player.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM)
        if (!command) return@withContext Result.failure(PlayerCommandNotFoundException(Player.COMMAND_SET_MEDIA_ITEM))

        _lock.tryWithLock(this) {
            try {
                player.addListener(_listener)
                Logger.d(tag = TAG) { "PLAYER LISTENER SET CORRECTLY" }
                val audioId = source.sourceId.toHexString()
                // current mediaId is same as the file audioId
                val areMediaIdSame = player.currentMediaItem?.mediaId == audioId

                if (areMediaIdSame) {
                    Logger.d(tag = TAG) { "PLAYER ID ALREADY EXISTS CAN DEFER TI" }
                    Logger.d(tag = TAG) { "UPDATING PLAYER PARAMETERS" }
                    // player media item is set so need to update the parameters
                    _listener.updateStateFromCurrentPlayerConfig()
                } else {
                    Logger.d(tag = TAG) { "SETTING PLAYER WITH ID" }
                    // normally adding the audio file to the player
                    addAudioItemToPlayer(source)
                }
                // prepare the player if the state is idle
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                    Logger.d(tag = TAG) { "PLAYER IS READY TO PLAY" }
                }
                player.playbackState == Player.STATE_READY
            } catch (e: IllegalStateException) {
                Logger.e(tag = TAG, throwable = e) { "PLAYER IS NOT CONFIGURED PROPERLY" }
                return@withContext Result.failure(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
    }

    actual override suspend fun play() = withContext(dispatchers.main) {
        val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
        if (!command) {
            Logger.w(tag = TAG) { "PLAYER CANNOT BE PLAYED COMMAND NOT FOUND" }
            return@withContext
        }
        _lock.tryWithLock(this) {
            try {
                player.play()
                Logger.i(tag = TAG) { "PLAYER PAUSED" }
            } catch (e: IllegalStateException) {
                Logger.w(tag = TAG, throwable = e) { "CANNOT PAUSE THE PLAYER TRY AGAIN" }
            }
        }
        Unit
    }

    actual override suspend fun pause() = withContext(dispatchers.main) {
        val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
        if (!command) {
            Logger.w(tag = TAG) { "PLAYER CANNOT BE PAUSED COMMAND NOT FOUND" }
            return@withContext
        }
        _lock.tryWithLock(this) {
            try {
                player.pause()
                Logger.i(tag = TAG) { "PLAYER PAUSED" }
            } catch (e: IllegalStateException) {
                Logger.w(tag = TAG, throwable = e) { "CANNOT PAUSE THE PLAYER TRY AGAIN" }
            }
        }
        Unit
    }

    actual override suspend fun stop() = withContext(dispatchers.main) {
        val command = player.isCommandAvailable(Player.COMMAND_STOP)
        if (!command) {
            Logger.w(tag = TAG) { "PLAYER CANNOT BE PAUSED COMMAND NOT FOUND" }
            return@withContext
        }
        _lock.tryWithLock(this) {
            try {
                player.stop()
                Logger.i(tag = TAG) { "PLAYER PAUSED" }
            } catch (e: IllegalStateException) {
                Logger.w(tag = TAG, throwable = e) { "CANNOT PAUSE THE PLAYER TRY AGAIN" }
            }
        }
        Unit
    }


    actual override suspend fun seekBy(delta: Duration, rewind: Boolean) = withContext(dispatchers.main) {
        val command = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        if (!command) {
            Logger.w(tag = TAG) { "CANNOT SEEK IN THE CLIP COMMAND NOT FOUND" }
            return@withContext
        }
        val amount = delta.inWholeMilliseconds.run {
            if (rewind) unaryMinus() else this
        }
        val seekPosition = (player.currentPosition + amount)
            .coerceIn(0..player.duration)

        when {
            seekPosition >= player.duration -> player.seekTo(player.duration)
            seekPosition < 0 -> player.seekTo(0)
            else -> player.seekTo(seekPosition)
        }
        Logger.i(tag = TAG) { "PLAYER SEEKER TO :$seekPosition" }
    }


    actual override suspend fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed) = withContext(dispatchers.main) {
        val command = player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)
        if (!command) {
            Logger.w(tag = TAG) { "CANNOT SET PLAYBACK SPEED" }
            return@withContext
        }
        player.setPlaybackSpeed(playBackSpeed.speed)
    }

    actual override suspend fun setPlayLooping(loop: Boolean) = withContext(dispatchers.main) {
        val command = player.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)
        if (!command) {
            Logger.w(tag = TAG) { "CANNOT SET PLAYER INTO LOOPING" }
            return@withContext
        }
        val repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.repeatMode = repeatMode
    }

    actual override suspend fun onMuteDevice() = withContext(dispatchers.main) {
        val command = player.isCommandAvailable(Player.COMMAND_SET_VOLUME)
        if (!command) {
            Logger.w(tag = TAG) { "CANNOT MUTE THE CURRENT PLAYBACK" }
            return@withContext
        }
        if (player.volume == .0f) player.unmute() else player.mute()
    }

    actual override suspend fun release() = withContext(dispatchers.main) {
        player.removeListener(_listener)
        Logger.d(tag = TAG) { "REMOVING PLAYER" }
    }


    private fun addAudioItemToPlayer(audio: AudioSource) {
        val mediaItem = MediaItem.Builder()
            .setMediaId(audio.sourceId.toHexString())
            .setUri(audio.source)
            .build()
        // set this current media item
        player.apply {
            // set repeat mode off
            repeatMode = Player.REPEAT_MODE_OFF
            // set speed to 1f
            setPlaybackSpeed(1f)
            // volume normal
            volume = 1f
            // clear and set item
            clearMediaItems()
            setMediaItem(mediaItem)
        }
        Logger.i(tag = TAG) { "MEDIA ITEM ADDED MEDIA COUNT:${player.mediaItemCount}" }
        if (player.playbackState != Player.STATE_IDLE) {
            Logger.i(tag = TAG) { "STOPPING CURRENT PLAYER" }
            player.stop()
        }
    }
}
