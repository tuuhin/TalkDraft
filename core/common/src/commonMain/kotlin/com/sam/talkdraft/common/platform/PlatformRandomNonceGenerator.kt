package com.sam.talkdraft.common.platform

import kotlin.io.encoding.Base64
import org.koin.core.annotation.Factory
import org.kotlincrypto.random.CryptoRand

@Factory
class PlatformRandomNonceGenerator {

    fun generateNonce(size: Int = 30): String {
        val bytes = ByteArray(size)
        CryptoRand.nextBytes(bytes)
        return Base64.encode(bytes)
    }
}
