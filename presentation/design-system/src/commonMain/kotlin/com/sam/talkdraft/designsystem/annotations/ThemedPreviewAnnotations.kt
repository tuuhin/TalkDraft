package com.sam.talkdraft.designsystem.annotations

import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Preview(name = "Themed_Light_Mode", apiLevel = 31)
@PreviewWrapper(ThemedPreviewWrapper::class)
annotation class LightThemedPreview

@Preview(name = "Themed_Dark_Mode", uiMode = UI_MODE_NIGHT_YES, apiLevel = 31)
@PreviewWrapper(ThemedPreviewWrapper::class)
annotation class DarkThemedPreview
