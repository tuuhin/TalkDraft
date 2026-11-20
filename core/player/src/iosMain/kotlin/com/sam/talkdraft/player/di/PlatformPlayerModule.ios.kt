package com.sam.talkdraft.player.di

import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import platform.AVFoundation.AVQueuePlayer

@Module
internal actual class PlatformPlayerModule {

    @Singleton
    fun player(): AVQueuePlayer = AVQueuePlayer()
}
