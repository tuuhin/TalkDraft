package com.sam.talkdraft.auth.utils

import org.koin.core.annotation.Factory
import org.kotlincrypto.random.CryptoRand
import kotlin.io.encoding.Base64

@Factory
internal class RandomNonceGenerator {

	fun generateNonce(size: Int = 30): String {
		val bytes = ByteArray(size)
		CryptoRand.nextBytes(bytes)
		return Base64.encode(bytes)
	}
}