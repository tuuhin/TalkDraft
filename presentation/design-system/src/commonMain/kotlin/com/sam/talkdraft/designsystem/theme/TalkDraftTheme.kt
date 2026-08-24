package com.sam.talkdraft.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

@Composable
expect fun TalkDraftTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    customTypography: Typography? = AppTypographyCustom,
    content: @Composable () -> Unit,
)
