package com.sam.talkdraft.common.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformCoroutineDispatchers::class])
internal actual class PlatformCoroutinesDispatchers :
    IPlatformCoroutineDispatchers {
    actual override val default: CoroutineDispatcher
        get() = Dispatchers.Default
    actual override val io: CoroutineDispatcher
        get() = Dispatchers.IO
    actual override val main: CoroutineDispatcher
        get() = Dispatchers.Main
    actual override val mainImmediate: CoroutineDispatcher
        get() = Dispatchers.Main.immediate
    actual override val unconfined: CoroutineDispatcher
        get() = Dispatchers.Unconfined
}
