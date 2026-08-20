package com.sam.talkdraft.auth.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

suspend fun getTopViewController(controller: UIViewController? = null): UIViewController? {
	return withContext(Dispatchers.Main) {

		val activeScene = UIApplication.sharedApplication.connectedScenes
			.filterIsInstance<UIWindowScene>()
			.firstOrNull { it.activationState == UISceneActivationStateForegroundActive }

		val keyWindow = activeScene?.windows
			?.filterIsInstance<UIWindow>()
			?.firstOrNull { it.isKeyWindow() }
			?: UIApplication.sharedApplication.keyWindow

		var topController = controller ?: keyWindow?.rootViewController

		// 2. Traverse down the hierarchy until we hit the absolute leaf node
		while (topController != null && currentCoroutineContext().isActive) {
			// ensures the coroutine is active
			currentCoroutineContext().ensureActive()
			topController = when {
				topController.presentedViewController != null -> topController.presentedViewController
				topController is UINavigationController -> topController.visibleViewController
				topController is UITabBarController -> topController.selectedViewController
				else -> break
			}

		}
		topController
	}
}