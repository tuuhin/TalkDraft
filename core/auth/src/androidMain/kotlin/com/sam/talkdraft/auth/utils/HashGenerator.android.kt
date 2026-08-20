package com.sam.talkdraft.auth.utils

import org.koin.core.annotation.Factory
import java.security.MessageDigest

@Factory
internal actual class HashGenerator {

	private val digest by lazy { MessageDigest.getInstance("SHA-256") }

	actual fun hash(input: String): String {
		val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
		return hashBytes.toHexString(format = HexFormat.Default)
	}
}