package com.sam.talkdraft.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import com.sam.talkdraft.designs.BricolageGrotesque_Font
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.DynaPuff_VariableFont
import com.sam.talkdraft.designs.Fraunces_Italic_VariableFont
import com.sam.talkdraft.designs.Fraunces_VariableFont
import com.sam.talkdraft.designs.GeistMono_Italic_VariableFont
import com.sam.talkdraft.designs.GeistMono_VariableFont
import com.sam.talkdraft.designs.GoogleSansFlex
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
actual fun dynaPuff(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            resource = CommonResources.font.DynaPuff_VariableFont,
            variationSettings = settings,
        ),
    )
}

@Composable
actual fun fraunces(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            resource = CommonResources.font.Fraunces_VariableFont,
            variationSettings = settings,
        ),
        Font(
            resource = CommonResources.font.Fraunces_Italic_VariableFont,
            variationSettings = settings,
            style = FontStyle.Italic,
        ),
    )
}

@Composable
actual fun geistMono(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            resource = CommonResources.font.GeistMono_VariableFont,
            variationSettings = settings,
        ),
        Font(
            resource = CommonResources.font.GeistMono_Italic_VariableFont,
            variationSettings = settings,
            style = FontStyle.Italic,
        ),
    )
}

@Composable
actual fun bicolageGrotesque(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            resource = CommonResources.font.BricolageGrotesque_Font,
            variationSettings = settings,
        ),
    )
}
