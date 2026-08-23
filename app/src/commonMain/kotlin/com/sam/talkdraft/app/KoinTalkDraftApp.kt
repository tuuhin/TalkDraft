package com.sam.talkdraft.app

import com.sam.talkdraft.app.di.CoreModule
import com.sam.talkdraft.app.di.FeatureModule
import com.sam.talkdraft.app.di.PlatformModule
import kotlin.native.HiddenFromObjC
import org.koin.core.annotation.KoinApplication

@KoinApplication(
    modules = [
        CoreModule::class,
        FeatureModule::class,
        PlatformModule::class,
    ],
)
@HiddenFromObjC
class KoinTalkDraftApp
