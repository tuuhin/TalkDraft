package com.sam.talkdraft.auth.models

import kotlin.jvm.JvmInline

@JvmInline
internal value class GoogleOAuthToken(val idToken: String)