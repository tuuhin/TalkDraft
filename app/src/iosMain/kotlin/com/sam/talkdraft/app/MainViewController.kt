package com.sam.talkdraft.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.talkdraft.app.utils.IosDeepLinkRouter

object MainViewController {

    fun viewController(
        showContentForIos: Boolean = true,
        router: IosDeepLinkRouter = IosDeepLinkRouter(),
    ) = ComposeUIViewController {

        val incomingLinks by router.incomingLink.collectAsStateWithLifecycle()

        App(
            showContentForIos = showContentForIos,
            deeplinkURL = { incomingLinks },
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(WindowInsets.navigationBars),
        )
    }
}
