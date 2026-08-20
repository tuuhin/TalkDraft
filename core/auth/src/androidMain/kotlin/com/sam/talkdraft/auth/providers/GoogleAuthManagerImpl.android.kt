package com.sam.talkdraft.auth.providers

import android.content.Context
import android.content.MutableContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sam.talkdraft.auth.exceptions.GoogleSignInException
import com.sam.talkdraft.auth.models.GoogleOAuthToken
import com.sam.talkdraft.auth.utils.HashGenerator
import com.sam.talkdraft.auth.utils.credentialMessage
import com.sam.talkdraft.commons.AppSecretProperties
import io.ktor.utils.io.CancellationException
import org.koin.core.annotation.Singleton

private const val TAG = "GOOGLE_AUTH_MANAGER"

@Singleton(binds = [IGoogleOAuthProvider::class])
internal actual class GoogleAuthManagerImpl(
	private val context: Context,
	private val hashGenerator: HashGenerator,
) : IGoogleOAuthProvider {

	private val manager by lazy { CredentialManager.create(context) }

	val idOptionBuilder: GetGoogleIdOption.Builder
		get() = GetGoogleIdOption.Builder()
			.setFilterByAuthorizedAccounts(false)
			.setServerClientId(AppSecretProperties.GOOGLE_SIGN_IN_WEB_CLIENT_ID)
			.setAutoSelectEnabled(true)


	actual override suspend fun signInWithGoogle(nonce: String): Result<GoogleOAuthToken> {

		val idOptions = idOptionBuilder.setNonce(hashGenerator.hash(nonce))
			.build()

		val request = GetCredentialRequest.Builder()
			.addCredentialOption(idOptions)
			.build()

		val mutableCtx = MutableContextWrapper(context)
		return runCatching {
			val result = manager.getCredential(request = request, context = mutableCtx)
			val creds = GoogleIdTokenCredential.createFrom(result.credential.data)
			GoogleOAuthToken(creds.idToken)
		}.onFailure { err ->
			if (err is GetCredentialException) {
				val message = err.credentialMessage ?: return@onFailure
				return Result.failure(GoogleSignInException(message))
			}
			if (err is CancellationException) throw err
		}
	}

	actual override suspend fun clearCredentials() {
		try {
			val request = ClearCredentialStateRequest()
			manager.clearCredentialState(request)
		} catch (e: Exception) {
			if (e is CancellationException) throw e
		}
	}

}