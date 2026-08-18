package com.sam.talkdraft

import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import com.sam.talkdraft.app.KoinTalkDraftApp
import org.koin.core.component.KoinComponent
import org.koin.plugin.module.dsl.startKoin

object KoinInitializer : KoinComponent {

	fun initKoin() {
		startKoin<KoinTalkDraftApp> {
			logger(KermitKoinLogger(logger = Logger.withTag("TalkDraft-iOS")))
		}
		// setup posthog and measure
		IosAppInitializer.setup()
	}
}