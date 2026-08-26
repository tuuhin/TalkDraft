package com.sam.talkdraft.player

import com.sam.talkdraft.player.model.AudioSource
import com.sam.talkdraft.player.model.PlayerPlayBackSpeed
import com.sam.talkdraft.player.model.PlayerPlayItemMetadata
import com.sam.talkdraft.player.model.PlayerTimeline
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory(binds = [IAudioPlayer::class])
internal expect class PlatformAudioPlayerImpl : IAudioPlayer {

    override val playerState: Flow<PlayerPlayItemMetadata>
    override val timeline: Flow<PlayerTimeline>
    override val errorFlow: Flow<Throwable>

    override suspend fun prepare(source: AudioSource): Result<Boolean>
    override suspend fun play()
    override suspend fun pause()
    override suspend fun stop()
    override suspend fun seekBy(delta: Duration, rewind: Boolean)
    override suspend fun onMuteDevice()
    override suspend fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed)
    override suspend fun setPlayLooping(loop: Boolean)
    override suspend fun release()
}
