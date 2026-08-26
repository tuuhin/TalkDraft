package com.sam.talkdraft.player.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [PlatformPlayerModule::class])
@ComponentScan("com.sam.talkdraft.player")
class PlayerModule
