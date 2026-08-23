package com.sam.talkdraft.common.platform

import java.security.MessageDigest
import org.koin.core.annotation.Factory

@Factory
actual class PlatformHashGenerator {

    private val digest by lazy { MessageDigest.getInstance("SHA-256") }

    actual fun hash(input: String): String {
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.toHexString(format = HexFormat.Default)
    }
}
