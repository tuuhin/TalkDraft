package com.sam.talkdraft.crashlytics

import com.sam.talkdraft.commons.AppSecretProperties
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.annotation.Singleton
import swiftPMImport.TalkDraft.core.core.crashlytics.BaseMeasureConfig
import swiftPMImport.TalkDraft.core.core.crashlytics.ClientInfo
import swiftPMImport.TalkDraft.core.core.crashlytics.Measure
import swiftPMImport.TalkDraft.core.core.crashlytics.initializeWith

@Singleton(binds = [MeasureSetupManager::class])
@OptIn(ExperimentalForeignApi::class)
internal actual class MeasureSetupImpl : MeasureSetupManager {

	actual override fun setup() {
		val clientInfo = ClientInfo(
			apiKey = AppSecretProperties.MEASURE_IOS_KEY,
			apiUrl = AppSecretProperties.POST_HOG_API_KEY
		)

		val config = BaseMeasureConfig()
		Measure.initializeWith(clientInfo, config)
	}
}