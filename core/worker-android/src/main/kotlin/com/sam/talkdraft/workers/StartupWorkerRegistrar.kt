package com.sam.talkdraft.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.sam.talkdraft.workers.workers.PeriodicRemoteDataSyncWorker
import java.time.Duration
import org.koin.core.annotation.Singleton

private const val TAG = "START_UP_WORKER_REGISTRAR"

@Singleton(binds = [IStartupWorkerRegistrar::class])
internal class StartupWorkerRegistrar(private val context: Context) : IStartupWorkerRegistrar {

    override fun enqueueWorkers() {
        startPeriodicSyncWorker()
    }

    private fun startPeriodicSyncWorker() {
        val constrains = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<PeriodicRemoteDataSyncWorker>(repeatInterval = Duration.ofDays(1))
                .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofMinutes(10))
                .setInitialDelay(Duration.ofSeconds(10))
                .setConstraints(constrains)
                .build()

        Logger.d(tag = TAG) { "UNIQUE PERIODIC WORKER ENQUEUED " }
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORKER_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest,
        )

    }

    companion object {
        private const val PERIODIC_SYNC_WORKER_NAME = "periodic_model_db_sync_worker"
    }
}
