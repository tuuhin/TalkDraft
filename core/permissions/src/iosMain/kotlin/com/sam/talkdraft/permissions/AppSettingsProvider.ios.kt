package com.sam.talkdraft.permissions

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Factory(binds = [IAppSettingsProvider::class])
internal actual class AppSettingsProvider(
    private val dispatcher: IPlatformCoroutineDispatchers,
) : IAppSettingsProvider {

    actual override suspend fun openSettings() {
        withContext(dispatcher.mainImmediate) {
            val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
            if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
                UIApplication.sharedApplication.openURL(
                    url = settingsUrl,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null,
                )
            }
        }
    }
}
