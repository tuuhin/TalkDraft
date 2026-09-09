package com.sam.talkdraft.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation

@Composable
expect fun googleSansFlexFont(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily

@Composable
expect fun ibmPlexMono(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily

@Composable
expect fun manrope(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily

@Composable
expect fun spaceGrotesk(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily
