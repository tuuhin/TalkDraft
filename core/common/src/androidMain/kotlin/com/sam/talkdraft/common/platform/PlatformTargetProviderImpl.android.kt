package com.sam.talkdraft.common.platform

import com.sam.talkdraft.common.model.PlatformTarget
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformTargetProvider::class])
internal actual class PlatformTargetProviderImpl : IPlatformTargetProvider {

    actual override fun target(): PlatformTarget = PlatformTarget.ANDROID
}
