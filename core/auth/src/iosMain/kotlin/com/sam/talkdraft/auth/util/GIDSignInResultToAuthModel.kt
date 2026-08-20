package com.sam.talkdraft.auth.util

import com.sam.talkdraft.auth.models.GoogleOAuthToken
import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.TalkDraft.core.core.auth.GIDGoogleUser

@OptIn(ExperimentalForeignApi::class)
internal fun GIDGoogleUser.toAuthModel() = GoogleOAuthToken(
	idToken = idToken?.tokenString ?: "",
)