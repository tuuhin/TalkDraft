package com.sam.talkdraft.common.platform

import com.sam.talkdraft.common.model.PlatformTarget
import org.koin.core.annotation.Factory
import platform.Foundation.NSBundle

@Factory(binds = [IPlatformTargetProvider::class])
internal actual class PlatformTargetProviderImpl : IPlatformTargetProvider {
    actual override fun target(): PlatformTarget = PlatformTarget.IOS

    actual override val platformVersionCode: Long
        get() = (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String?)?.toLongOrNull() ?: 0L
}
