package com.sam.talkdraft.app.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IosDeepLinkRouter {

    val incomingLink: StateFlow<String?>
        field = MutableStateFlow(null)

    fun handleLink(url: String?) {
        incomingLink.tryEmit(url)
    }
}
