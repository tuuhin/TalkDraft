package com.sam.talkdraft.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.sam.talkdraft.designsystem.annotations.PreviewAppTheme
import com.sam.talkdraft.designsystem.utils.LocalSnackBarState

@Composable
internal fun OnBoardingScreen(modifier: Modifier = Modifier) {

    val snackbarHostState = LocalSnackBarState.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {},
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(modifier = modifier.padding(padding)) {
        }
    }
}


@Preview
@Composable
@PreviewWrapper(PreviewAppTheme::class)
private fun OnBoardingScreenPreview() {
    OnBoardingScreen()
}
