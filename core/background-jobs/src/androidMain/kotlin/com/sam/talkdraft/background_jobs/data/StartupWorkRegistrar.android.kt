package com.sam.talkdraft.background_jobs.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.sam.talkdraft.background_jobs.IStartupWorkerRegistrar
import com.sam.talkdraft.workers.workers.PeriodicRemoteDataSyncWorker
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.koin.core.annotation.Singleton

private const val TAG = "START_UP_WORKER_REGISTRAR"

@Singleton(binds = [IStartupWorkerRegistrar::class])
internal actual class StartupWorkRegistrar(private val context: Context) : IStartupWorkerRegistrar {

    actual override fun enqueueWorkers() {
        startPeriodicSyncWorker()
    }

    private fun startPeriodicSyncWorker() {
        val constrains = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<PeriodicRemoteDataSyncWorker>(repeatInterval = 1.days.toJavaDuration())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10.minutes.toJavaDuration())
                .setInitialDelay(10.seconds.toJavaDuration())
                .setConstraints(constrains)
                .build()

        Logger.d(tag = TAG) { "UNIQUE PERIODIC WORKER ENQUEUED " }
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORKER_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )

    }

    companion object {
        private const val PERIODIC_SYNC_WORKER_NAME = "periodic_model_db_sync_worker"
    }
}
