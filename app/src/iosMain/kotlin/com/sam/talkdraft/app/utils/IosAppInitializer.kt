package com.sam.talkdraft.app.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.OSLogWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.XcodeSeverityWriter
import com.sam.talkdraft.analytics.posthog.IPostHogInitManager
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.crashlytics.MeasureSetupManager
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object IosAppInitializer : KoinComponent {

    private val dispatchers by inject<IPlatformCoroutineDispatchers>()
    private val posthogInit by inject<IPostHogInitManager>()
    private val measure by inject<MeasureSetupManager>()

    suspend fun setup() = coroutineScope {
        val op0 = async(dispatchers.io) {
            posthogInit.setup()
            posthogInit.turnOffDataCollection()
        }
        val op1 = async(dispatchers.io) { measure.setup() }
        awaitAll(op0, op1)
        Unit
    }

    @OptIn(ExperimentalNativeApi::class)
    fun setupLogging() {
        val isDebug = Platform.isDebugBinary
        Logger.setLogWriters(
            if (isDebug) XcodeSeverityWriter()
            else OSLogWriter(subsystem = "com.sam.talkdraft", category = "TalkDraft", publicLogging = true),
        )
        if (isDebug) Logger.setMinSeverity(Severity.Verbose)
        else Logger.setMinSeverity(Severity.Info)
    }
}
