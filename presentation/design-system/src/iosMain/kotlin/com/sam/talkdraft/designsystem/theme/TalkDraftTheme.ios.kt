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
    customTypography: Typography?,
    content: @Composable (() -> Unit),
) {
    val style = PaletteStyle.TonalSpot
    val specVersion = ColorSpec.SpecVersion.SPEC_2025

    val primary = if (isDarkTheme) PrimaryDark else PrimaryLight
    val secondary = if (isDarkTheme) SecondaryDark else SecondaryLight
    val tertiary = if (isDarkTheme) TertiaryDark else TertiaryLight
    val error = if (isDarkTheme) ErrorDark else ErrorLight
    val neutral = if (isDarkTheme) NeutralDark else NeutralLight
    val neutralVariant = if (isDarkTheme) NeutralVariantDark else NeutralVariantLight

    val colorScheme = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
        style = style,
        specVersion = specVersion,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        error = error,
        neutral = neutral,
        neutralVariant = neutralVariant,
    )

    DynamicMaterialExpressiveTheme(
        state = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = customTypography ?: Typography(),
        animate = true,
        content = content,
    )
}

