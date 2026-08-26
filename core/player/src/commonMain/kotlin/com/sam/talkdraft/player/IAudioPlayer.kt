package com.sam.talkdraft.player

import com.sam.talkdraft.player.model.AudioSource
import com.sam.talkdraft.player.model.PlayerPlayBackSpeed
import com.sam.talkdraft.player.model.PlayerPlayItemMetadata
import com.sam.talkdraft.player.model.PlayerTimeline
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

interface IAudioPlayer {

    val playerState: Flow<PlayerPlayItemMetadata>
    val timeline: Flow<PlayerTimeline>
    val errorFlow: Flow<Throwable>

    suspend fun prepare(source: AudioSource): Result<Boolean>
    suspend fun play()
    suspend fun pause()
    suspend fun stop()

    suspend fun seekBy(delta: Duration, rewind: Boolean)
    suspend fun setPlayLooping(loop: Boolean)
    suspend fun onMuteDevice()
    suspend fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed)

    suspend fun release()
}
