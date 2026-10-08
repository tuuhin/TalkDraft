package com.sam.talkdraft.auth

import co.touchlab.kermit.Logger
import com.sam.talkdraft.analytics.IAnalyticsProvider
import com.sam.talkdraft.auth.exceptions.UserSessionNotFoundException
import com.sam.talkdraft.auth.models.AuthUserModel
import com.sam.talkdraft.auth.models.OAuthProviders
import com.sam.talkdraft.auth.providers.IAppleOAuthProvider
import com.sam.talkdraft.auth.providers.IGoogleOAuthProvider
import com.sam.talkdraft.auth.util.toAuthModel
import com.sam.talkdraft.common.model.PlatformTarget
import com.sam.talkdraft.common.platform.IPlatformTargetProvider
import com.sam.talkdraft.common.platform.PlatformRandomNonceGenerator
import com.sam.talkdraft.supabase.SupabaseProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.annotation.Factory

@Factory(binds = [IUserAuthManager::class])
internal class UserAuthManagerImpl(
    private val analytics: IAnalyticsProvider,
    private val googleProvider: IGoogleOAuthProvider,
    private val appleProvider: IAppleOAuthProvider,
    private val supabaseProvider: SupabaseProvider,
    private val nonceGenerator: PlatformRandomNonceGenerator,
    private val platformTarget: IPlatformTargetProvider,
) : IUserAuthManager {

    val client: SupabaseClient by lazy { supabaseProvider.providesSupabase() }

    override val supportedOAuthProviders: List<OAuthProviders>
        get() = when (platformTarget.target()) {
            PlatformTarget.ANDROID -> listOf(OAuthProviders.GOOGLE)
            PlatformTarget.IOS -> listOf(OAuthProviders.GOOGLE, OAuthProviders.APPLE)
            PlatformTarget.UNKNOWN -> throw Exception("Invalid target only supported targets are android and ios")
        }

    override val userFlow: Flow<AuthUserModel?>
        get() = client.auth.sessionStatus.filterNot { status -> status is SessionStatus.Initializing }
            .onEach { status ->
                val simpleName = when (status) {
                    is SessionStatus.Authenticated -> "AUTHENTICATED SOURCE:${status.source}"
                    SessionStatus.Initializing -> "UNKNOWN"
                    is SessionStatus.NotAuthenticated -> "NOT_AUTHENTICATED IS_LOGOUT:${status.isSignOut}"
                    is SessionStatus.RefreshFailure -> "REFRESHING SESSION REASON"
                }
                Logger.d(tag = TAG) { "CURRENT USER STATE :$simpleName" }
            }.map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> status.session.user?.toAuthModel()
                    else -> null
                }
            }


    override suspend fun getCurrentUser(): AuthUserModel {
        val status = client.auth.sessionStatus.first { status -> status !is SessionStatus.Initializing }

        Logger.d(tag = TAG) { "CURRENT USER STATUS :$status" }

        if (status is SessionStatus.Authenticated) {
            val user = status.session.user ?: throw UserSessionNotFoundException()
            return user.toAuthModel()
        }

        Logger.d(tag = TAG) { "USER IS NOT AUTHENTICATED" }
        throw UserSessionNotFoundException()
    }

    override suspend fun signInWithOAuth(provider: OAuthProviders): Result<AuthUserModel> {
        return runCatching {
            // try to get credentials from Google
            when (provider) {
                OAuthProviders.GOOGLE -> {
                    val nonce = nonceGenerator.generateNonce()
                    val credentials = googleProvider.signInWithGoogle(nonce).getOrThrow()
                    // sign in the user with the
                    client.auth.signInWith(IDToken) {
                        this.idToken = credentials.idToken
                        this.nonce = nonce
                        this.provider = Google

                    }
                    Logger.d(tag = TAG) { "SUCCESSFULLY LOGGED IN WITH GOOGLE" }
                }

                OAuthProviders.APPLE -> {
                    val token = appleProvider.readToken()
                    client.auth.signInWith(IDToken) {
                        this.idToken = token.token
                        this.provider = Apple
                    }
                    Logger.d(tag = TAG) { "SUCCESSFULLY LOGGED IN WITH APPLE" }
                    // first timers
                    if (token.fullname != null) {
                        Logger.d(tag = TAG) { "UPDATING THE USER FULL NAME AS APPLE ONLY GIVES THIS A SINGLE TIME" }
                        client.auth.updateUser {
                            data = buildJsonObject {
                                put("full_name", token.fullname)
                                token.givenName?.let { put("given_name", it) }
                                token.familyName?.let { put("family_name", it) }
                            }
                        }
                        Logger.i(tag = TAG) { "USER NAME SET FOR APPLE USER SUCCESS" }
                    }
                }
            }
            // return the current user
            getCurrentUser()
        }.onSuccess {
            analytics.identify(userId = it.authId)
        }.onFailure { err ->
            if (err is CancellationException) throw err
            Logger.e(tag = TAG, throwable = err) { "FAILED TO SIGN IN USER WITH :${provider.name}" }
        }
    }

    override suspend fun signInAnonymously(): Result<AuthUserModel> {
        return runCatching {
            client.auth.signInAnonymously()
            // return the current user
            getCurrentUser()
        }.onSuccess {
            analytics.identify(userId = it.authId)
        }.onFailure { err ->
            if (err is CancellationException) throw err
            Logger.e(tag = TAG, throwable = err) { "FAILED TO SIGN IN USER" }
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            // sign out the user
            client.auth.signOut()
            // clear identity for the user
            analytics.reset()
            Logger.d(tag = TAG) { "USER SUCCESSFULLY SIGNED OUT" }
        }.onFailure { err ->
            if (err is CancellationException) throw err
            Logger.e(tag = TAG, throwable = err) { "FAILED TO SIGN OUT USER" }
        }
    }

    companion object {
        private const val TAG = "USER_AUTH_MANAGER"
    }
}
