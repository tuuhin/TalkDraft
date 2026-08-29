package com.sam.talkdraft.app.di

import com.sam.talkdraft.designsystem.di.UIDesignModule
import com.sam.talkdraft.home.di.HomeUIModule
import com.sam.talkdraft.onboarding.di.OnboardingUIModule
import kotlin.native.HiddenFromObjC
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(
    includes = [
        UIDesignModule::class,
        OnboardingUIModule::class,
        HomeUIModule::class,
    ],
)
@HiddenFromObjC
@ComponentScan("com.sam.talkdraft.app.viewmodel")
internal class PlatformUIModule
