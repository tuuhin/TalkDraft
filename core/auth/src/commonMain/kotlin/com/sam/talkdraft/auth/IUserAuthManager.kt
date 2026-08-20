package com.sam.talkdraft.auth

import com.sam.talkdraft.auth.models.AuthUserModel
import com.sam.talkdraft.auth.models.OAuthProviders
import kotlinx.coroutines.flow.Flow

interface IUserAuthManager {

	suspend fun getCurrentUser(): AuthUserModel

	val userFlow: Flow<AuthUserModel?>

	suspend fun signInWithOAuth(provider: OAuthProviders = OAuthProviders.GOOGLE): Result<AuthUserModel>

	suspend fun signOut(): Result<Unit>
}