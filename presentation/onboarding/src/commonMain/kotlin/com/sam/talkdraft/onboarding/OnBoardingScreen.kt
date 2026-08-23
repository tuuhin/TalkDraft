package com.sam.talkdraft.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.sam.talkdraft.designsystem.annotations.PreviewAppTheme

@Composable
fun OnBoardingScreen(modifier: Modifier = Modifier) {
    Scaffold { insertsPadding ->
        Column(modifier = modifier.padding(insertsPadding)) {
        }
    }
}


@Preview
@Composable
@PreviewWrapper(PreviewAppTheme::class)
private fun OnBoardingScreenPreview() {
    OnBoardingScreen()
}
