package com.sam.talkdraft.app.di

import com.sam.talkdraft.notifications.di.AndroidNotificationsModule
import com.sam.talkdraft.workers.di.AndroidWorkersModule
import org.koin.core.annotation.Module

@Module(
    includes = [
        AndroidNotificationsModule::class,
        AndroidWorkersModule::class,
    ],
)
internal actual class PlatformModule
