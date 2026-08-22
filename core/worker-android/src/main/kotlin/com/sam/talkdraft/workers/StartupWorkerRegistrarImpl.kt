package com.sam.talkdraft.workers

import android.app.Application
import com.sam.talkdraft.workers.workers.PeriodicRemoteDataSyncWorker
import org.koin.core.annotation.Singleton

@Singleton(binds = [IStartupWorkerRegistrar::class])
internal class StartupWorkerRegistrarImpl(private val app: Application) : IStartupWorkerRegistrar {

    override fun enqueueWorkers() {
        // just start the workers here
        PeriodicRemoteDataSyncWorker.startRepeatWorker(app.applicationContext)
    }
}
