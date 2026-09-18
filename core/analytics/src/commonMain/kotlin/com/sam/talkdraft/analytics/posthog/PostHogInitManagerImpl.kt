package com.sam.talkdraft.analytics.posthog

import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import com.sam.talkdraft.commons.AppSecretProperties
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPostHogInitManager::class])
internal class PostHogInitManagerImpl(
    private val provider: IPostHogContext,
) : IPostHogInitManager {

    override fun setup() = PostHog.setup(
        config = PostHogConfig(apiKey = AppSecretProperties.POST_HOG_API_KEY),
        context = provider.readContext(),
    )

    override fun turnOffDataCollection() = PostHog.optOut()
    override fun turnOnDataCollection() = PostHog.optIn()

}
