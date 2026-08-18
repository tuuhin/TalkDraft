package com.sam.talkdraft.crashlytics

import android.content.Context
import org.koin.core.annotation.Singleton
import sh.measure.android.Measure
import sh.measure.android.config.MeasureConfig

@Singleton(binds = [MeasureSetupManager::class])
internal actual class MeasureSetupImpl(private val context: Context) : MeasureSetupManager {
	actual override fun setup() {
		val config = MeasureConfig()
		Measure.init(context, config)
	}
}