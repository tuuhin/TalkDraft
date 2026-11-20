package com.sam.talkdraft.common.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
internal actual class CommonPlatformModule {

    @Singleton
    fun ktorIosEngine(): HttpClientEngine = Darwin.create()
}
