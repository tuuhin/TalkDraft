package com.sam.talkdraft.auth.providers

import com.sam.talkdraft.auth.models.AppleOAuthToken
import org.koin.core.annotation.Single

@Single(binds = [IAppleOAuthProvider::class])
internal expect class AppleOAuthManager : IAppleOAuthProvider {
    override suspend fun readToken(): AppleOAuthToken
}
