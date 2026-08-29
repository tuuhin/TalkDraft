package com.sam.talkdraft.background_jobs

fun interface IStartupWorkerRegistrar {

    fun enqueueWorkers()
}
