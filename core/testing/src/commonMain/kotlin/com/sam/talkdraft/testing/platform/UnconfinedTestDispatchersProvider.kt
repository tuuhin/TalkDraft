package com.sam.talkdraft.testing.platform

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Named("unconfined")
@Singleton
class UnconfinedTestDispatchersProvider(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
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
