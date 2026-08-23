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
private val ibmPlexMono = GoogleFont("IBMPlexMono")
private val manrope = GoogleFont("Manrope")

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
actual fun ibmPlexMono(
    settings: FontVariation.Settings,
): FontFamily = FontFamily(
    Font(
        ibmPlexMono,
        fontProvider = provider,
        variationSettings = settings,
    ),
)

@Composable
actual fun manrope(settings: FontVariation.Settings): FontFamily {
    return FontFamily(
        Font(
            manrope,
            fontProvider = provider,
            variationSettings = settings,
        ),
    )
}
