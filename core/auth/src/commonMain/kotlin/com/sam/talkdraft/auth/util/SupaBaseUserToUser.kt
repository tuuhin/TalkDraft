package com.sam.talkdraft.auth.util

import com.sam.talkdraft.auth.models.AuthUserModel
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.jsonPrimitive

fun UserInfo.toAuthModel(): AuthUserModel {

	val displayName = userMetadata?.get("full_name")?.jsonPrimitive?.content
		?: userMetadata?.get("name")?.jsonPrimitive?.content
	val avatarUrl = userMetadata?.get("avatar_url")?.jsonPrimitive?.content
		?: userMetadata?.get("picture")?.jsonPrimitive?.content

	val phoneNumber = userMetadata?.get("phone_number")?.jsonPrimitive?.content

	return AuthUserModel(
		authId = id,
		email = email,
		displayName = displayName,
		avatar = avatarUrl,
		phoneNumber = phoneNumber
	)
}