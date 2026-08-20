package com.sam.talkdraft.auth.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import org.koin.core.annotation.Factory
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@Factory
@OptIn(ExperimentalForeignApi::class)
internal actual class HashGenerator {

	actual fun hash(input: String): String {
		val inputData = input.encodeToByteArray()
		val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)

		inputData.usePinned { inputPinned ->
			digest.usePinned { digestPinned ->
				CC_SHA256(
					inputPinned.addressOf(0),
					inputData.size.toUInt(),
					digestPinned.addressOf(0).reinterpret()
				)
			}
		}
		return digest.toHexString(HexFormat.Default)
	}
}