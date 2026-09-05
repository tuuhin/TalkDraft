package com.sam.talkdraft.background_jobs.data

import com.sam.talkdraft.background_jobs.IRemoteDbBackgroundSyncRegistrar

internal expect class RemoteDbBackgroundSyncRegistrar : IRemoteDbBackgroundSyncRegistrar {
    override fun setupBackgroundTask()
}
