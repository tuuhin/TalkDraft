package com.sam.talkdraft.crashlytics

import org.koin.core.annotation.Singleton

@Singleton(binds = [MeasureSetupManager::class])
internal expect class MeasureSetupImpl : MeasureSetupManager {
	override fun setup()
}