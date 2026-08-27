package com.sam.talkdraft.recorder.domain

internal fun interface IRecordPermissionChecker {

    fun hasPermission(): Boolean
}
