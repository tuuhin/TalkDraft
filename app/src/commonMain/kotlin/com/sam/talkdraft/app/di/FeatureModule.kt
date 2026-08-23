package com.sam.talkdraft.app.di

import com.sam.talkdraft.feature_onboarding.di.FeatureOnboardingModule
import com.sam.talkdraft.model_downloader.di.ModelDownloaderModule
import com.sam.talkdraft.model_manager.di.ModelManagerModule
import kotlin.native.HiddenFromObjC
import org.koin.core.annotation.Module

@Module(
    includes = [
        ModelManagerModule::class,
        ModelDownloaderModule::class,
        FeatureOnboardingModule::class,
    ],
)
@HiddenFromObjC
internal class FeatureModule
