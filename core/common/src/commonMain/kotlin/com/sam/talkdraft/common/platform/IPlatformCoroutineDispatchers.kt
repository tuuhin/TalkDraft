package com.sam.talkdraft.common.platform

import kotlinx.coroutines.CoroutineDispatcher

interface IPlatformCoroutineDispatchers {
    val main: CoroutineDispatcher
    val mainImmediate: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}
