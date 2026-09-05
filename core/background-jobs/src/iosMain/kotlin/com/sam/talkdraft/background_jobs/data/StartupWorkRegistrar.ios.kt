package com.sam.talkdraft.background_jobs.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.analytics.AnalyticsEvent
import com.sam.talkdraft.analytics.IAnalyticsProvider
import com.sam.talkdraft.background_jobs.IRemoteDbBackgroundSyncRegistrar
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.model_manager.domain.repository.IUpdateTranscriptionModelRepo
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton
import platform.BackgroundTasks.BGAppRefreshTask
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.BackgroundTasks.BGTaskSchedulerErrorDomain
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.darwin.dispatch_get_main_queue

private const val TAG = "DB_SYNC_TASK_SCHEDULAR"

@Singleton(binds = [IRemoteDbBackgroundSyncRegistrar::class])
internal actual class RemoteDbBackgroundSyncRegistrar private constructor(
    private val repo: IUpdateTranscriptionModelRepo,
    private val analytics: IAnalyticsProvider,
    dispatchers: IPlatformCoroutineDispatchers,
) : IRemoteDbBackgroundSyncRegistrar {

    private val scope = CoroutineScope(dispatchers.io + SupervisorJob())
    private var syncJob: Job? = null

    actual override fun setupBackgroundTask() {
        val req = BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = DB_SYNC_IDENTIFIER,
            usingQueue = dispatch_get_main_queue(),
        ) { task ->
            if (task !is BGAppRefreshTask) return@registerForTaskWithIdentifier
            handleTaskExecution(task)
        }

        Logger.i(tag = TAG) { "BACKGROUND TASK REGISTRATION IS_SUCCESSFUL = $req" }
        scheduleNextTask(delayInSeconds = 10.seconds.inWholeMilliseconds.toDouble())
    }

    private suspend fun performSync() {
        val result = repo.syncLocalData()
        result.fold(
            onSuccess = {
                analytics.track(
                    AnalyticsEvent.ModelRemoteSyncSuccess,
                    mapOf(DB_MODEL_SYNC_KEY to DB_MODEL_SYNC_SUCCESS),
                )
            },
            onFailure = { err ->
                if (err is CancellationException) throw err
                val message = err.message ?: "SOME ERROR OCCURRED"
                analytics.track(
                    AnalyticsEvent.ModelRemoteSyncFailed,
                    mapOf(
                        DB_MODEL_SYNC_KEY to DB_MODEL_SYNC_FAILED,
                        DB_MODEL_SYNC_FAILED_REASON to message,
                    ),
                )
            },
        )
    }

    private fun handleTaskExecution(task: BGAppRefreshTask) {
        // reschedule for next half day
        val halfDay = 12.hours.inWholeSeconds
        scheduleNextTask(delayInSeconds = halfDay.toDouble())

        syncJob = scope.launch {
            try {
                performSync()
                Logger.d(tag = TAG) { "DB Sync Completed Successfully" }
                task.setTaskCompletedWithSuccess(true)
            } catch (_: CancellationException) {
                Logger.d(tag = TAG) { "TASK HAS BEEN CANCELLED VIA CANCELLATION" }
                task.setTaskCompletedWithSuccess(false)
            } catch (e: Exception) {
                Logger.e(tag = TAG, throwable = e) { "TASK FAILED" }
                task.setTaskCompletedWithSuccess(false)
            }
        }
        task.expirationHandler = {
            Logger.d(tag = TAG) { "IOS SYNC JOB CANCELLING " }
            syncJob?.cancel()
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun scheduleNextTask(delayInSeconds: Double = 30.0) {
        BGTaskScheduler.sharedScheduler.getPendingTaskRequestsWithCompletionHandler { requests ->
            val pendingList = requests?.filterIsInstance<BGTaskRequest>() ?: emptyList()
            val isAlreadyScheduled = pendingList.any { it.identifier == DB_SYNC_IDENTIFIER }

            if (isAlreadyScheduled) {
                Logger.d(tag = TAG) { "Task IDENTIFIER:$DB_SYNC_IDENTIFIER IS PENDING" }
            } else {
                Logger.d(tag = TAG) { "TASK IS NOT SCHEDULE NEED TO ASSIGN IT" }
                val request = BGAppRefreshTaskRequest(DB_SYNC_IDENTIFIER).apply {
                    earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(delayInSeconds)
                }

                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val success = BGTaskScheduler.sharedScheduler.submitTaskRequest(request, errorPtr.ptr)

                    if (success) {
                        Logger.d(tag = TAG) { "TASK SCHEDULED SUCCESSFULLY WILL RUN AFTER NOW+$delayInSeconds sec" }
                        return@memScoped
                    }

                    val err = errorPtr.value ?: return@memScoped
                    if (err.domain == BGTaskSchedulerErrorDomain && err.code == 1L) {
                        Logger.d(tag = TAG) { "Task schedular unavailable (expected on iOS Simulator / Low Power Mode): ${err.localizedDescription}" }
                    } else {
                        Logger.w(tag = TAG) { "FAILED TO SUBMIT TASK: Code ${err.code}, Domain: ${err.domain}, Description: ${err.localizedDescription}" }
                    }
                }
            }
        }
    }

    private fun cancelPendingSyncTask() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(DB_SYNC_IDENTIFIER)
        Logger.d(tag = TAG) { "TASK: $DB_SYNC_IDENTIFIER CANCELLED" }
    }


    companion object {
        private const val DB_MODEL_SYNC_KEY = "db_model_sync"
        private const val DB_MODEL_SYNC_SUCCESS = "db_model_sync_success"
        private const val DB_MODEL_SYNC_FAILED = "db_model_sync_failed"
        private const val DB_MODEL_SYNC_FAILED_REASON = "db_model_sync_failed_reason"

        // sync identifier dont change
        const val DB_SYNC_IDENTIFIER = "com.sam.talkdraft.background_jobs.sync_db"

    }
}
