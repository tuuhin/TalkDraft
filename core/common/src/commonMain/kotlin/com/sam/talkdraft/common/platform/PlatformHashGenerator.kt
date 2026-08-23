package com.sam.talkdraft.common.platform

import org.koin.core.annotation.Factory

@Factory
expect class PlatformHashGenerator {

    fun hash(input: String): String
}
