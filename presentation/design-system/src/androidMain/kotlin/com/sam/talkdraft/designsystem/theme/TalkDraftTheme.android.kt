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
    customTypography: Typography?,
    content: @Composable (() -> Unit),
) {
    val context = LocalContext.current
    val style = PaletteStyle.TonalSpot
    val specVersion = ColorSpec.SpecVersion.SPEC_2025


    val colorSchemeState =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val dynamicScheme = if (isDarkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)

                rememberDynamicMaterialThemeState(
                    isDark = isDarkTheme,
                    style = style,
                    specVersion = specVersion,
                    primary = dynamicScheme.primary,
                    secondary = dynamicScheme.secondary,
                    tertiary = dynamicScheme.tertiary,
                    error = dynamicScheme.error,
                    neutral = dynamicScheme.surface,
                    neutralVariant = dynamicScheme.surfaceVariant,
                )
            }

            else -> {
                val primary = if (isDarkTheme) PrimaryDark else PrimaryLight
                val secondary = if (isDarkTheme) SecondaryDark else SecondaryLight
                val tertiary = if (isDarkTheme) TertiaryDark else TertiaryLight
                val error = if (isDarkTheme) ErrorDark else ErrorLight
                val neutral = if (isDarkTheme) NeutralDark else NeutralLight
                val neutralVariant = if (isDarkTheme) NeutralVariantDark else NeutralVariantLight

                rememberDynamicMaterialThemeState(
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
            }
        }

    DynamicMaterialExpressiveTheme(
        state = colorSchemeState,
        motionScheme = MotionScheme.expressive(),
        animate = true,
        typography = customTypography ?: Typography(),
        content = content,
    )
}
