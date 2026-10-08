package com.sam.talkdraft.auth.models

internal data class AppleOAuthToken(
    val token: String,
    val fullname: String?,
    val givenName: String?,
    val familyName: String?,
)
