package com.sam.talkdraft.workers.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.android.annotation.KoinWorker

@KoinWorker
class UnzipModelFileWorker internal constructor(
    context: Context,
    private val params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        TODO("Not yet implemented")
    }
}
