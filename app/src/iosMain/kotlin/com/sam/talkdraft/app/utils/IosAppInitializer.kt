package com.sam.talkdraft.app.utils

import com.sam.talkdraft.analytics.posthog.IPostHogInitManager
import com.sam.talkdraft.crashlytics.MeasureSetupManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal object IosAppInitializer : KoinComponent {

    private val posthogInit by inject<IPostHogInitManager>()
    private val measure by inject<MeasureSetupManager>()

    fun setup() {
        posthogInit.setup()
        posthogInit.turnOffDataCollection()
        measure.setup()
    }
}
