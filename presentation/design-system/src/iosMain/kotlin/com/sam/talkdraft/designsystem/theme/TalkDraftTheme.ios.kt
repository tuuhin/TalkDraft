package com.sam.talkdraft.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
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
    val lightColorScheme = rememberDynamicMaterialThemeState(
        isDark = false,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        primary = PrimaryLight,
        secondary = SecondaryLight,
        tertiary = TertiaryLight,
        error = ErrorLight,
        neutral = NeutralLight,
        neutralVariant = NeutralVariantLight,
    )

    val darkColorScheme = rememberDynamicMaterialThemeState(
        isDark = true,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        primary = PrimaryDark,
        secondary = SecondaryDark,
        tertiary = TertiaryDark,
        error = ErrorDark,
        neutral = NeutralDark,
        neutralVariant = NeutralVariantDark,
    )

    val colorScheme = when {
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

