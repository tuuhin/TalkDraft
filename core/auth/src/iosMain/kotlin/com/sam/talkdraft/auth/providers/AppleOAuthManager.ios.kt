@file:OptIn(BetaInteropApi::class)

package com.sam.talkdraft.auth.providers

import co.touchlab.kermit.Logger
import com.sam.talkdraft.auth.models.AppleOAuthToken
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.annotation.Single
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

@Single(binds = [IAppleOAuthProvider::class])
internal actual class AppleOAuthManager : IAppleOAuthProvider {

    actual override suspend fun readToken(): AppleOAuthToken {
        return suspendCancellableCoroutine { cont ->

            val delegate = object : NSObject(), ASAuthorizationControllerDelegateProtocol,
                ASAuthorizationControllerPresentationContextProvidingProtocol {

                override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
                    val activeScene = UIApplication.sharedApplication.connectedScenes
                        .filterIsInstance<UIWindowScene>()
                        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }

                    val keyWindow = activeScene?.windows
                        ?.filterIsInstance<UIWindow>()
                        ?.firstOrNull { it.isKeyWindow() }
                        ?: UIApplication.sharedApplication.keyWindow

                    return keyWindow ?: ASPresentationAnchor()
                }

                override fun authorizationController(
                    controller: ASAuthorizationController,
                    didCompleteWithAuthorization: ASAuthorization,
                ) {
                    Logger.d(tag = TAG) { "APPLE CREDENTIALS FOUND" }
                    val credentials = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential

                    if (credentials == null) {
                        if (cont.isActive) cont.resumeWith(Result.failure(IllegalStateException("Invalid credential type received.")))
                        return
                    }

                    val userIdentity = credentials.identityToken
                    if (userIdentity == null) {
                        if (cont.isActive) cont.resumeWith(Result.failure(IllegalStateException("Identity token missing.")))
                        return
                    }

                    val tokenString = NSString.create(data = userIdentity, encoding = NSUTF8StringEncoding)?.toString()
                    if (tokenString == null) {
                        if (cont.isActive) cont.resumeWith(Result.failure(IllegalStateException("Failed to decode token string.")))
                        return
                    }

                    val fullName = credentials.fullName()
                    val fullNameString = buildString {
                        fullName?.givenName()?.let { givenName -> append("$givenName ") }
                        fullName?.middleName()?.let { middleName -> append("$middleName ") }
                        fullName?.familyName()?.let { familyName -> append("$familyName ") }
                    }.trimEnd()

                    val authToken = AppleOAuthToken(
                        tokenString,
                        fullname = fullNameString,
                        givenName = fullName?.givenName(),
                        familyName = fullName?.familyName(),
                    )

                    if (cont.isActive) cont.resumeWith(Result.success(authToken))
                }

                override fun authorizationController(
                    controller: ASAuthorizationController,
                    didCompleteWithError: NSError,
                ) {
                    Logger.w(tag = TAG) { "UNABLE TO AUTHORIZE WITH APPLE ID" }
                    val errorMessage = didCompleteWithError.localizedDescription
                    if (cont.isActive) cont.resumeWith(Result.failure(IllegalStateException(errorMessage)))
                }
            }

            // this part will request the popup
            val provider = ASAuthorizationAppleIDProvider()
            val request = provider.createRequest().apply {
                requestedScopes = listOf(ASAuthorizationScopeEmail, ASAuthorizationScopeFullName)
            }
            // assign delegates
            val controller = ASAuthorizationController(authorizationRequests = listOf(request))
            controller.delegate = delegate
            controller.presentationContextProvider = delegate

            cont.invokeOnCancellation {
                controller.delegate = null
                controller.presentationContextProvider = null
                Logger.d(tag = TAG) { "CLEARING DELEGATES" }
            }

            Logger.i(tag = TAG) { "REQUESTING APPLE SIGN IN" }
            controller.performRequests()
        }
    }

    companion object {
        private const val TAG = "APPLE_OATH_MANAGER"
    }
}
