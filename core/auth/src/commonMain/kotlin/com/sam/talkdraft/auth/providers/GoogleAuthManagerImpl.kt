package com.sam.talkdraft.auth.providers

import com.sam.talkdraft.auth.models.GoogleOAuthToken
import org.koin.core.annotation.Singleton

@Singleton(binds = [IGoogleOAuthProvider::class])
internal expect class GoogleAuthManagerImpl : IGoogleOAuthProvider {
	override suspend fun signInWithGoogle(nonce: String): Result<GoogleOAuthToken>
	override suspend fun clearCredentials()
}