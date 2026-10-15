package com.sam.talkdraft.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation

/**
 * Google Sans Flex
 * Used for body text, labels, buttons, navigation, and other UI elements,
 * prioritizing readability and a clean, modern appearance.
 */
@Composable
expect fun googleSansFlexFont(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily

/**
 * DynaPuff
 * Intended for expressive branding or playful UI elements rather than
 * the primary reading experience.
 */
@Composable
expect fun dynaPuff(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily

/**
 * Fraunces
 * Used for prominent headings and display text to give TalkDraft
 * a distinctive, editorial character.
 */
@Composable
expect fun fraunces(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily

/**
 *  Geist Mono
 * Used for timestamps, recording durations, and technical metadata
 * where consistent character widths improve alignment.
 */
@Composable
expect fun geistMono(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily

/**
 * Bricolage Grotesque
 * Used for screen titles, document names, and card titles to add
 * personality while maintaining a modern interface.
 */
@Composable
expect fun bicolageGrotesque(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily
