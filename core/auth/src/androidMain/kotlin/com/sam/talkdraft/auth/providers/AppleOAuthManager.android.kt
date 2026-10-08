package com.sam.talkdraft.auth.providers

import com.sam.talkdraft.auth.models.AppleOAuthToken
import org.koin.core.annotation.Single

@Single(binds = [IAppleOAuthProvider::class])
internal actual class AppleOAuthManager : IAppleOAuthProvider {

    actual override suspend fun readToken(): AppleOAuthToken =
        throw IllegalStateException("Android cannot sign in with apple")
}
