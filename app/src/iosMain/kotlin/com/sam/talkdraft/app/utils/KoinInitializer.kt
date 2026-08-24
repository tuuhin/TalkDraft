package com.sam.talkdraft.app.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import com.sam.talkdraft.app.KoinTalkDraftApp
import org.koin.plugin.module.dsl.startKoin

object KoinInitializer {

	fun initKoin() {
		startKoin<KoinTalkDraftApp> {
			logger(KermitKoinLogger(logger = Logger.withTag("TalkDraft-iOS")))
		}
		// setup post hog and measure
		IosAppInitializer.setup()
	}
}
