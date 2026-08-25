package com.sam.talkdraft.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.GoogleSansFlex
import com.sam.talkdraft.designs.IBMPlexMono
import com.sam.talkdraft.designs.Manrope
import org.jetbrains.compose.resources.Font

@Composable
actual fun googleSansFlexFont(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            resource = CommonResources.font.GoogleSansFlex,
            variationSettings = settings,
        ),
    )
}

@Composable
actual fun ibmPlexMono(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            resource = CommonResources.font.IBMPlexMono,
            variationSettings = settings,
        ),
    )
}

@Composable
actual fun manrope(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            resource = CommonResources.font.Manrope,
            variationSettings = settings,
        ),
    )
}
