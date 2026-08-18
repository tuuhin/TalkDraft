package com.sam.talkdraft.di

import com.sam.talkdraft.analytics.di.AnalyticsModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(modules = [AnalyticsModule::class])
internal class KoinTalkDraftApp