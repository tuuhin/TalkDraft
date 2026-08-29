package com.sam.talkdraft.common.platform

import com.sam.talkdraft.common.model.PlatformTarget

interface IPlatformTargetProvider {

    fun target(): PlatformTarget

    val platformVersionCode: Long
}
