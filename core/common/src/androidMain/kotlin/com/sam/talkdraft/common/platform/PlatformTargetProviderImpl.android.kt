package com.sam.talkdraft.common.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.sam.talkdraft.common.model.PlatformTarget
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformTargetProvider::class])
internal actual class PlatformTargetProviderImpl(
    private val context: Context,
) : IPlatformTargetProvider {

    actual override fun target(): PlatformTarget = PlatformTarget.ANDROID

    actual override val platformVersionCode: Long
        get() {
            val packageManager = context.packageManager
            val packageName = context.packageName
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }
                return packageInfo.longVersionCode
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                return 0L
            }
        }
}
