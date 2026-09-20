package com.sam.talkdraft.player.utils

import com.sam.talkdraft.platform.kvo.NSKeyValueObservingProtocol
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal class PlayerKVOObserver(
    private val onUpdate: (keyPath: String?) -> Unit,
) : NSObject(), NSKeyValueObservingProtocol {

    override fun observeValueForKeyPath(
        keyPath: String?,
        ofObject: Any?,
        change: Map<Any?, *>?,
        context: COpaquePointer?,
    ) {
        onUpdate(keyPath)
    }
}
