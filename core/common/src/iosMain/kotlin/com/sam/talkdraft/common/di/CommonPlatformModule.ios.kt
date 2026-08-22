package com.sam.talkdraft.common.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.annotation.Module

@Module
internal actual class CommonPlatformModule {

    fun ktorIosEngine(): HttpClientEngine = Darwin.create()
}
