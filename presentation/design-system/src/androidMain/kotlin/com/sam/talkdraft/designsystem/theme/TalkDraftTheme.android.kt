package com.sam.talkdraft.designsystem.theme

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun TalkDraftTheme(
    isDarkTheme: Boolean,
    dynamicColor: Boolean,
    useSystemFonts: Boolean,
    customTypography: Typography?,
    content: @Composable (() -> Unit),
) {
    val context = LocalContext.current
    val style = PaletteStyle.TonalSpot
    val specVersion = ColorSpec.SpecVersion.SPEC_2025

    val lightColorScheme = rememberDynamicMaterialThemeState(
        isDark = false,
        style = style,
        specVersion = specVersion,
        primary = PrimaryLight,
        secondary = SecondaryLight,
        tertiary = TertiaryLight,
        error = ErrorLight,
        neutral = NeutralLight,
        neutralVariant = NeutralVariantLight,
    )

    val darkColorScheme = rememberDynamicMaterialThemeState(
        isDark = true,
        style = style,
        specVersion = specVersion,
        primary = PrimaryDark,
        secondary = SecondaryDark,
        tertiary = TertiaryDark,
        error = ErrorDark,
        neutral = NeutralDark,
        neutralVariant = NeutralVariantDark,
    )

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val colorScheme = if (isDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)

            rememberDynamicMaterialThemeState(
                isDark = true,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
                seedColor = colorScheme.primary,
            )
        }

        isDarkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    DynamicMaterialExpressiveTheme(
        state = colorScheme,
        motionScheme = MotionScheme.expressive(),
        animate = true,
        content = content,
    )
}
