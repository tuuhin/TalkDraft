package com.sam.talkdraft.auth.providers

import com.sam.talkdraft.auth.models.AppleOAuthToken

internal interface IAppleOAuthProvider {

    suspend fun readToken(): AppleOAuthToken
}
