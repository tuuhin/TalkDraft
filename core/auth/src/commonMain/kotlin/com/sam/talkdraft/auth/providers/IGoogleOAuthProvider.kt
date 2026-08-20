package com.sam.talkdraft.auth.providers

import com.sam.talkdraft.auth.models.GoogleOAuthToken

internal interface IGoogleOAuthProvider {
	suspend fun signInWithGoogle(nonce: String): Result<GoogleOAuthToken>
	suspend fun clearCredentials()
}