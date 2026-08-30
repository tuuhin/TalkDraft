package com.sam.talkdraft.app.utils

import com.sam.talkdraft.analytics.posthog.IPostHogInitManager
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.crashlytics.MeasureSetupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object IosAppInitializer : KoinComponent {

    private val dispatchers by inject<IPlatformCoroutineDispatchers>()
    private val posthogInit by inject<IPostHogInitManager>()
    private val measure by inject<MeasureSetupManager>()

    suspend fun setup() {
        withContext(Dispatchers.IO) {
            val op0 = async(dispatchers.io) {
                posthogInit.setup()
                posthogInit.turnOffDataCollection()

            }
            val op1 = async(dispatchers.main) { measure.setup() }
            awaitAll(op0, op1)
        }
    }
}
