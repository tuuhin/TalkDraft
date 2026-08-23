package com.sam.talkdraft.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp

val AppTypographyCustom: Typography
    @Composable
    get() {
        // --- Font Configurations ---
        val manropeSemiBold = manrope(FontVariation.Settings(FontVariation.weight(600)))
        val manropeBold = manrope(FontVariation.Settings(FontVariation.weight(700)))
        val manropeExtraBold = manrope(FontVariation.Settings(FontVariation.weight(800)))

        val uiFont = googleSansFlexFont(FontVariation.Settings(FontVariation.weight(400)))
        val uiMediumFont = googleSansFlexFont(FontVariation.Settings(FontVariation.weight(500)))
        val uiStrongFont = googleSansFlexFont(FontVariation.Settings(FontVariation.weight(600)))
        val uiBoldFont = googleSansFlexFont(FontVariation.Settings(FontVariation.weight(700)))

        return Typography(
            // --- Standard Display Styles ---
            displayLarge = TextStyle(
                fontFamily = manropeBold,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.5).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = manropeBold,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = (-0.25).sp,
            ),
            displaySmall = TextStyle(
                fontFamily = manropeBold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),

            // --- Standard Headline Styles ---
            headlineLarge = TextStyle(
                fontFamily = manropeSemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.15).sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = manropeSemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
            headlineSmall = TextStyle(
                fontFamily = manropeSemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),

            // --- Standard Title Styles ---
            titleLarge = TextStyle(
                fontFamily = manropeSemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = uiMediumFont,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = uiMediumFont,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),

            // --- Standard Body Styles ---
            bodyLarge = TextStyle(
                fontFamily = uiFont,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = uiFont,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = uiFont,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
            ),

            // --- Standard Label Styles ---
            labelLarge = TextStyle(
                fontFamily = uiStrongFont,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = uiMediumFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = uiMediumFont,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),

            // --- Material 3 Expressive Emphasized Styles ---
            displayLargeEmphasized = TextStyle(
                fontFamily = manropeExtraBold,
                fontSize = 64.sp,
                lineHeight = 72.sp,
                letterSpacing = (-0.75).sp,
            ),
            displayMediumEmphasized = TextStyle(
                fontFamily = manropeExtraBold,
                fontSize = 50.sp,
                lineHeight = 58.sp,
                letterSpacing = (-0.5).sp,
            ),
            headlineLargeEmphasized = TextStyle(
                fontFamily = manropeBold,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                letterSpacing = (-0.15).sp,
            ),
            titleLargeEmphasized = TextStyle(
                fontFamily = manropeBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
            titleMediumEmphasized = TextStyle(
                fontFamily = uiStrongFont,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
            titleSmallEmphasized = TextStyle(
                fontFamily = uiStrongFont,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            bodyLargeEmphasized = TextStyle(
                fontFamily = uiStrongFont,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
            bodyMediumEmphasized = TextStyle(
                fontFamily = uiStrongFont,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
            bodySmallEmphasized = TextStyle(
                fontFamily = uiStrongFont,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
            ),
            labelLargeEmphasized = TextStyle(
                fontFamily = uiBoldFont,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            labelMediumEmphasized = TextStyle(
                fontFamily = uiStrongFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
            labelSmallEmphasized = TextStyle(
                fontFamily = uiStrongFont,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        )
    }
