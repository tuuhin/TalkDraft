package com.sam.talkdraft.testing.platform

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.koin.core.annotation.Singleton

@Singleton
class StandardTestDispatcherProvider(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : IPlatformCoroutineDispatchers {

    override val default: CoroutineDispatcher
        get() = testDispatcher
    override val io: CoroutineDispatcher
        get() = testDispatcher
    override val main: CoroutineDispatcher
        get() = testDispatcher
    override val mainImmediate: CoroutineDispatcher
        get() = testDispatcher

    override val unconfined: CoroutineDispatcher
        get() = testDispatcher
}
