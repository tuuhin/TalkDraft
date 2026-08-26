package com.sam.talkdraft.common.platform

import com.sam.talkdraft.common.model.PlatformTarget

fun interface IPlatformTargetProvider {

    fun target(): PlatformTarget
}
