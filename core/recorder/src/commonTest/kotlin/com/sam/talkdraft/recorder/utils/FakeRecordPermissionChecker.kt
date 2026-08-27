package com.sam.talkdraft.recorder.utils

import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker

class FakeRecordPermissionChecker : IRecordPermissionChecker {
    override fun hasPermission(): Boolean {
        return true
    }
}
