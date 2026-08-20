package com.sam.talkdraft.auth.utils

import org.koin.core.annotation.Factory

@Factory
internal expect class HashGenerator {

	fun hash(input: String): String
}