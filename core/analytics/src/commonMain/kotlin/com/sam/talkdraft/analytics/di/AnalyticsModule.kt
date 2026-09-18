package com.sam.talkdraft.analytics.di

import com.sam.talkdraft.analytics.posthog.IPostHogContext
import com.sam.talkdraft.analytics.posthog.IPostHogInitManager
import com.sam.talkdraft.analytics.posthog.PostHogInitManagerImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.sam.talkdraft.analytics")
class AnalyticsModule {

    // [IosAppInitializer] was failing to read in the manager
    // view [ComponentScan] thus needed to be invoked via property
    @Singleton
    internal fun postHogProvider(context: IPostHogContext): IPostHogInitManager = PostHogInitManagerImpl(context)
}
