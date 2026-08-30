package com.sam.talkdraft.crashlytics

import com.sam.talkdraft.commons.AppSecretProperties
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.annotation.Singleton
import platform.Foundation.NSNumber
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
            apiUrl = "https://ingest.measure.sh",
        )

        // copied from swift
        val config = BaseMeasureConfig(
            enableLogging = false,
            autoStart = true,
            requestHeadersProvider = null,
            maxDiskUsageInMb = NSNumber(50),
            enableFullCollectionMode = false,
            enableDiagnosticMode = true,
            enableDiagnosticModeGesture = false,
        )

        Measure.initializeWith(clientInfo, config)
    }
}
