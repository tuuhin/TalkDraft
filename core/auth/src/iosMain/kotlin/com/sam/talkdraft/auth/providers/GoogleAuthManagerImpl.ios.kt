package com.sam.talkdraft.auth.providers

import co.touchlab.kermit.Logger
import com.sam.talkdraft.auth.exceptions.GoogleSignInException
import com.sam.talkdraft.auth.models.GoogleOAuthToken
import com.sam.talkdraft.auth.util.toAuthModel
import com.sam.talkdraft.common.platform.PlatformHashGenerator
import com.sam.talkdraft.commons.AppSecretProperties
import io.ktor.utils.io.CancellationException
import kotlin.coroutines.resumeWithException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.annotation.Singleton
import swiftPMImport.TalkDraft.core.core.auth.GIDConfiguration
import swiftPMImport.TalkDraft.core.core.auth.GIDSignIn

private const val TAG = "GOOGLE_SIGN_IN_MANAGER"

@OptIn(ExperimentalForeignApi::class)
@Singleton(binds = [IGoogleOAuthProvider::class])
internal actual class GoogleAuthManagerImpl(
    private val hashGenerator: PlatformHashGenerator,
) : IGoogleOAuthProvider {

    actual override suspend fun signInWithGoogle(nonce: String): Result<GoogleOAuthToken> {
        return runCatching {
            val uiController =
                getTopViewController() ?: throw IllegalStateException("UI controller not found")

            val config = GIDConfiguration(
                clientID = AppSecretProperties.GOOGLE_IOS_SIGN_IN_CLIENT_ID,
                serverClientID = AppSecretProperties.GOOGLE_SIGN_IN_WEB_CLIENT_ID,
            )

            GIDSignIn.sharedInstance.configuration = config
            val hash = hashGenerator.hash(nonce)

            Logger.d(tag = TAG) { "REQUESTING GOOGLE SIGN IN" }
            suspendCancellableCoroutine { cont ->
                GIDSignIn.sharedInstance.signInWithPresentingViewController(
                    presentingViewController = uiController,
                    nonce = hash,
                    hint = null,
                    additionalScopes = null,
                ) { result, error ->
                    Logger.d(tag = TAG) { "SOME RESPONSE FOUND :${error?.localizedDescription} ${result?.user?.idToken}" }
                    when {
                        error != null -> if (cont.isActive)
                            cont.resumeWithException(GoogleSignInException(error.localizedDescription))

                        result == null -> if (cont.isActive)
                            cont.resumeWithException(IllegalStateException("Invalid state"))

                        else -> if (cont.isActive) {
                            cont.resumeWith(result = Result.success(result.user.toAuthModel()))
                        }
                    }
                }
            }
        }
    }

    actual override suspend fun clearCredentials() {
        try {
            GIDSignIn.sharedInstance.signOut()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(e, tag = TAG) { "FAILED TO CLEAR CREDENTIALS" }
        }
    }
}
