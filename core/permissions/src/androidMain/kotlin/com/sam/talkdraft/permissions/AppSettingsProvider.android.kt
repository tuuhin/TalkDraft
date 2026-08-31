package com.sam.talkdraft.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [IAppSettingsProvider::class])
internal actual class AppSettingsProvider(
    private val context: Context,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IAppSettingsProvider {

    actual override suspend fun openSettings() {
        withContext(dispatchers.mainImmediate) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
