package com.sam.talkdraft.player.utils

import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import co.touchlab.kermit.Logger
import com.sam.talkdraft.player.model.PlayerPlayBackSpeed
import com.sam.talkdraft.player.model.PlayerPlayItemMetadata
import com.sam.talkdraft.player.model.PlayerPlaybackState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update


private const val TAG = "AUDIO_PLAYER_LISTENER"

internal class AudioPlayerListener(private val player: Player) : Player.Listener {

    private val _playbackState = MutableStateFlow(PlayerPlaybackState.IDLE)
    private val _playBackSpeed = MutableStateFlow<PlayerPlayBackSpeed>(PlayerPlayBackSpeed.Normal)
    private val _isLooping = MutableStateFlow(false)
    private val _isStreamMuted = MutableStateFlow(false)

    val playerMetaDataFlow: Flow<PlayerPlayItemMetadata> = combine(
        _playbackState,
        _isLooping,
        _playBackSpeed,
        _isStreamMuted,
    ) { state, repeat, speed, muted ->
        PlayerPlayItemMetadata(
            playerState = state,
            isRepeating = repeat,
            playBackSpeed = speed,
            isMuted = muted,
        )
    }

    private val _errorsFlow = Channel<PlaybackException>(capacity = Channel.CONFLATED)
    val errorFlow: Flow<PlaybackException> = _errorsFlow.receiveAsFlow()

    override fun onPlaybackStateChanged(playbackState: Int) {
        val newState = when (playbackState) {
            Player.STATE_IDLE -> PlayerPlaybackState.IDLE
            Player.STATE_BUFFERING -> PlayerPlaybackState.BUFFERING
            Player.STATE_READY -> PlayerPlaybackState.PLAYER_READY
            Player.STATE_ENDED -> PlayerPlaybackState.COMPLETED
            else -> return
        }
        _playbackState.update { newState }
        Logger.d(tag = TAG) { "PLAYBACK STATE CHANGED: $newState" }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        val isLooping = repeatMode == Player.REPEAT_MODE_ONE
        _isLooping.update { isLooping }
        Logger.d(tag = TAG) { "PLAYER REPEATING: $isLooping" }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        val playerSpeed = playbackParameters.speed
        val speed = PlayerPlayBackSpeed.fromInt(playerSpeed) ?: return
        _playBackSpeed.update { speed }
        Logger.d(tag = TAG) { "PLAYER SPEED: $playerSpeed" }
    }

    override fun onVolumeChanged(volume: Float) {
        super.onVolumeChanged(volume)
        _isStreamMuted.update { volume == 0f }
        Logger.d(tag = TAG) { "DEVICE VOLUME CHANGED: $volume" }
    }

    override fun onPlayerError(error: PlaybackException) {
        _errorsFlow.trySend(error)
        Logger.e(tag = TAG, throwable = error) { error.message ?: "PLAYER_ERROR" }
    }

    fun updateStateFromCurrentPlayerConfig() {
        Logger.d(tag = TAG) { "UPDATING PLAYER CONFIG" }
        _isLooping.update { player.repeatMode == Player.REPEAT_MODE_ONE }

        _playBackSpeed.update { current -> PlayerPlayBackSpeed.fromInt(player.playbackParameters.speed) ?: current }

        _playbackState.update {
            when (player.playbackState) {
                Player.STATE_IDLE -> PlayerPlaybackState.IDLE
                Player.STATE_ENDED -> PlayerPlaybackState.COMPLETED
                Player.STATE_READY -> PlayerPlaybackState.PLAYER_READY
                else -> it
            }
        }

        _isStreamMuted.update { player.volume == 0f }
    }
}
