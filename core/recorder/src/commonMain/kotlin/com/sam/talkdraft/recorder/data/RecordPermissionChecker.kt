package com.sam.talkdraft.recorder.data

import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import org.koin.core.annotation.Factory

@Factory(binds = [IRecordPermissionChecker::class])
internal expect class RecordPermissionChecker : IRecordPermissionChecker {
    override fun hasPermission(): Boolean
}
