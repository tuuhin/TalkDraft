package com.sam.talkdraft.auth.utils

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException

val GetCredentialException.credentialMessage: String?
	get() = when (this) {
		is GetCredentialCancellationException -> null
		is NoCredentialException -> "No matching credentials found. Please ensure you are signed into Google on this device."
		is GetCredentialInterruptedException -> "Sign-in was interrupted. Please try again."
		is GetCredentialProviderConfigurationException -> "Authentication configuration error. Please contact support."
		is GetCredentialUnsupportedException -> "Google Sign-In is not supported on this device version."
		else -> "Sign-in failed. Please try again."
	}
