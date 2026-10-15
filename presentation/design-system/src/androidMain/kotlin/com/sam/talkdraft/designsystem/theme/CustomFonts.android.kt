package com.sam.talkdraft.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.sam.talkdraft.designsystem.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val googleSansFlex = GoogleFont("Google Sans Flex")
private val dynaPuff = GoogleFont("DynaPuff")
private val fraunces = GoogleFont("Fraunces")
private val geistMono = GoogleFont("Geist Mono")
private val bricolageGrotesque = GoogleFont("Bricolage Grotesque")

@Composable
actual fun googleSansFlexFont(
    settings: FontVariation.Settings,
): FontFamily = FontFamily(
    Font(
        googleSansFlex,
        fontProvider = provider,
        variationSettings = settings,
    ),
)

@Composable
actual fun dynaPuff(
    settings: FontVariation.Settings,
): FontFamily = FontFamily(
    Font(
        dynaPuff,
        fontProvider = provider,
        variationSettings = settings,
    ),
)

@Composable
actual fun fraunces(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            fraunces,
            fontProvider = provider,
            variationSettings = settings,
        ),
    )
}

@Composable
actual fun geistMono(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            geistMono,
            fontProvider = provider,
            variationSettings = settings,
        ),
    )
}

@Composable
actual fun bicolageGrotesque(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            bricolageGrotesque,
            fontProvider = provider,
            variationSettings = settings,
        ),
    )
}
