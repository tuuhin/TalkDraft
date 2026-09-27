package com.sam.talkdraft.designsystem.annotations

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import com.sam.talkdraft.designsystem.theme.TalkDraftTheme

internal class ThemedPreviewWrapper : PreviewWrapperProvider {

    @Composable
    override fun Wrap(content: @Composable (() -> Unit)) {
        TalkDraftTheme(content = content)
    }
}
