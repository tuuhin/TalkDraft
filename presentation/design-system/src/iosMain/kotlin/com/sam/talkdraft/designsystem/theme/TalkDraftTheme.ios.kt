package com.sam.talkdraft.designsystem.theme

import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun TalkDraftTheme(
    isDarkTheme: Boolean,
    dynamicColor: Boolean,
    customTypography: Typography?,
    content: @Composable (() -> Unit),
) {
    DynamicMaterialExpressiveTheme(
        seedColor = SeedColor,
        motionScheme = MotionScheme.expressive(),
        isDark = isDarkTheme,
        secondary = Secondary,
        tertiary = Tertiary,
        error = Error,
        style = PaletteStyle.Vibrant,
        animate = true,
        animationSpec = tween(durationMillis = 500),
        content = content,
    )
}
