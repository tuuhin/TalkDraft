package com.sam.talkdraft.analytics.posthog

import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogConfig
import com.sam.talkdraft.commons.AppSecretProperties
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPostHogInitManager::class])
internal class PostHogInitManager(
	val provider: PostHogContextProvider
) : IPostHogInitManager {

	override fun setup() {
		PostHog.setup(
			config = PostHogConfig(apiKey = AppSecretProperties.POST_HOG_API_KEY),
			context = provider.readContext()
		)
	}
}