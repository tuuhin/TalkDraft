package com.sam.talkdraft.workers.utils

import org.koin.core.annotation.Factory

@Factory(binds = [IWorkerEnvironment::class])
internal class AppWorkerEnvironment : IWorkerEnvironment {

    override val isProd: Boolean
        get() = true
}
