package com.sam.talkdraft.recorder.data

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import org.koin.core.annotation.Factory

@Factory(binds = [IRecordPermissionChecker::class])
internal actual class RecordPermissionChecker(
    private val context: Context,
) : IRecordPermissionChecker {

    actual override fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PermissionChecker.PERMISSION_GRANTED
    }
}
