package com.sam.talkdraft.background_jobs.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.sam.talkdraft.background_jobs.IRemoteDbBackgroundSyncRegistrar
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.workers.workers.PeriodicRemoteDataSyncWorker
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.koin.core.annotation.Singleton

@Singleton(binds = [IRemoteDbBackgroundSyncRegistrar::class])
internal actual class RemoteDbBackgroundSyncRegistrar(
    private val context: Context,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IRemoteDbBackgroundSyncRegistrar {

    private val workManager by lazy { WorkManager.getInstance(context.applicationContext) }

    actual override fun setupBackgroundTask() {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<PeriodicRemoteDataSyncWorker>(repeatInterval = 6.hours.toJavaDuration())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30.minutes.toJavaDuration(),
                )
                .setInitialDelay(10.seconds.toJavaDuration())
                .setConstraints(constraints)
                .build()

        Logger.d(tag = TAG) { "UNIQUE PERIODIC WORKER ENQUEUED, POLICY UPDATE" }

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORKER_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )

    }

    companion object {
        private const val TAG = "START_UP_WORKER_REGISTRAR"
        private const val PERIODIC_SYNC_WORKER_NAME = "periodic_model_db_sync_worker"
    }
}
