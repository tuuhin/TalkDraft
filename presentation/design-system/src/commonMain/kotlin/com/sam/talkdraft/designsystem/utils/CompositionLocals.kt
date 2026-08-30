package com.sam.talkdraft.designsystem.utils

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.core.layout.WindowSizeClass
import com.sam.talkdraft.designsystem.utils.formatter.BytesFormatterStyle
import com.sam.talkdraft.designsystem.utils.formatter.IBytesSizeFormatter

val LocalSnackBarState = staticCompositionLocalOf { SnackbarHostState() }

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedContentScope = staticCompositionLocalOf<AnimatedContentScope?> { null }

val LocalWindowSizeInfo = compositionLocalOf { WindowSizeClass(400, 400) }
val LocalPostureInfo = compositionLocalOf { Posture() }

// customs
val LocalBytesConvertor = staticCompositionLocalOf<IBytesSizeFormatter> {
    object : IBytesSizeFormatter {
        override fun formatToString(bytes: Long, style: BytesFormatterStyle): String {
            return "$bytes bytes"
        }
    }
}
