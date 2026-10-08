package com.sam.talkdraft.auth.models

data class AuthUserModel(
    val authId: String,
    val displayName: String? = null,
    val avatar: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val isAnonymous: Boolean = false,
)
