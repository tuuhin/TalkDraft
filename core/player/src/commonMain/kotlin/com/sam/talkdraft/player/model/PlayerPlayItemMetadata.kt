package com.sam.talkdraft.player.model

data class PlayerPlayItemMetadata(
    val playerState: PlayerPlaybackState = PlayerPlaybackState.IDLE,
    val playBackSpeed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.Normal,
    val isRepeating: Boolean = false,
    val isMuted: Boolean = false,
)
