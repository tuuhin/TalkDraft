package com.sam.talkdraft.workers.util

import com.sam.talkdraft.workers.utils.IWorkerEnvironment
import org.koin.core.annotation.Factory

@Factory(binds = [IWorkerEnvironment::class])
internal class TestWorkerEnvironment : IWorkerEnvironment {

    override val isProd: Boolean
        get() = true
}
