package com.sam.talkdraft.common.platform

import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformCoroutineDispatchers::class])
internal expect class PlatformCoroutinesDispatchers : IPlatformCoroutineDispatchers {
    override val default: CoroutineDispatcher
    override val io: CoroutineDispatcher
    override val main: CoroutineDispatcher
    override val mainImmediate: CoroutineDispatcher
    override val unconfined: CoroutineDispatcher
}
