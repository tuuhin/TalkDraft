package com.sam.talkdraft.app

import com.sam.talkdraft.app.di.CoreModule
import com.sam.talkdraft.app.di.FeatureModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(
    modules = [
        CoreModule::class,
        FeatureModule::class,
    ],
)
class KoinTalkDraftApp
