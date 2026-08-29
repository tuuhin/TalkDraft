package com.sam.talkdraft.background_jobs.data

import com.sam.talkdraft.background_jobs.IStartupWorkerRegistrar

internal expect class StartupWorkRegistrar : IStartupWorkerRegistrar {
    override fun enqueueWorkers()
}
