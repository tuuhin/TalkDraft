package com.sam.talkdraft.player.model

enum class PlayerPlaybackState {
    IDLE,
    PLAYER_READY,
    BUFFERING,
    COMPLETED;

    val canAdvertiseCurrentPosition: Boolean
        get() = this == PLAYER_READY
}
