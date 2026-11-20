package com.sam.talkdraft.designsystem.theme

import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val colorScheme = remember(context, dynamicColor, isDarkTheme) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                isDarkTheme -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        } else null
    }

    if (colorScheme != null) {
        DynamicMaterialExpressiveTheme(
            seedColor = colorScheme.primary,
            motionScheme = MotionScheme.expressive(),
            isDark = isDarkTheme,
            secondary = colorScheme.secondary,
            tertiary = colorScheme.tertiary,
            error = colorScheme.error,
            style = PaletteStyle.Vibrant,
            animate = true,
            animationSpec = tween(durationMillis = 500),
            content = content,
        )
    } else {
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
}
