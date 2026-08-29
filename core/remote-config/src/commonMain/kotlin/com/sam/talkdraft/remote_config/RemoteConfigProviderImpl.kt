package com.sam.talkdraft.remote_config

import co.touchlab.kermit.Logger
import com.posthog.kmp.PostHog
import com.sam.talkdraft.remote_config.model.RemoteConfigData
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.annotation.Factory

private const val TAG = "REMOTE_CONFIGS"

@Factory(binds = [IRemoteConfigProvider::class])
internal class RemoteConfigProviderImpl : IRemoteConfigProvider {

    override suspend fun loadFlags() {
        suspendCancellableCoroutine { cont ->
            PostHog.reloadFeatureFlags {
                Logger.d(tag = TAG) { "LOADING FLAGS SUCCESSFULLY" }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    override fun minAndroidVersion(): RemoteConfigData<Long> {
        return fetchConfigData("min_android_version")
    }

    override fun minIosVersion(): RemoteConfigData<Long> {
        return fetchConfigData("min_ios_version")
    }

    private fun fetchConfigData(key: String): RemoteConfigData<Long> {
        val result = PostHog.getFeatureFlagResult(key)
        return RemoteConfigData(
            key = key,
            value = result?.payload?.toString()?.toLongOrNull() ?: 0L,
            isFlagPresent = result?.enabled ?: false,
        )
    }
}
