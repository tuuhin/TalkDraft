package com.sam.talkdraft.background_jobs.bridge

import com.sam.talkdraft.background_jobs.IRemoteDbBackgroundSyncRegistrar
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object RemoteDbSyncRegistrarBridge : KoinComponent {

    private val registrar by inject<IRemoteDbBackgroundSyncRegistrar>()

    fun invoke() = registrar.setupBackgroundTask()
}
