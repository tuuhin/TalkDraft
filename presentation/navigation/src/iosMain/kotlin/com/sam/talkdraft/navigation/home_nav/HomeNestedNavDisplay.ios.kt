package com.sam.talkdraft.navigation.home_nav

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.sam.talkdraft.designsystem.theme.TalkDraftTheme
import platform.Foundation.NSTimeInterval
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UITabBarControllerDelegateProtocol
import platform.UIKit.UITabBarControllerModeTabSidebar
import platform.UIKit.UITabBarItem
import platform.UIKit.UITransitionContextFromViewKey
import platform.UIKit.UITransitionContextToViewKey
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerAnimatedTransitioningProtocol
import platform.UIKit.UIViewControllerContextTransitioningProtocol
import platform.UIKit.secondaryLabelColor
import platform.UIKit.tabBarItem
import platform.darwin.NSObject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun HomeNestedNavDisplay(
    homeScreen: @Composable (() -> Unit),
    recordingScreen: @Composable (() -> Unit),
    settingsScreen: @Composable (() -> Unit),
    modifier: Modifier,
) {

    val primary = MaterialTheme.colorScheme.primary

    UIKitViewController(
        factory = {
            val homeNavigationController = UINavigationController(
                rootViewController = ComposeUIViewController {
                    TalkDraftTheme { homeScreen() }
                },
            ).apply {
                tabBarItem = UITabBarItem(
                    title = "Home",
                    image = UIImage.systemImageNamed("house"),
                    selectedImage = UIImage.systemImageNamed("house.fill"),
                )
            }

            val recordingNavController = UINavigationController(
                rootViewController = ComposeUIViewController {
                    TalkDraftTheme { recordingScreen() }
                },
            ).apply {
                tabBarItem = UITabBarItem(
                    title = "Recordings",
                    image = UIImage.systemImageNamed("waveform"),
                    selectedImage = UIImage.systemImageNamed("waveform.path.badge.plus"),
                )
            }


            val settingsNavController = UINavigationController(
                rootViewController = ComposeUIViewController {
                    TalkDraftTheme { settingsScreen() }
                },
            ).apply {
                tabBarItem = UITabBarItem(
                    title = "Settings",
                    image = UIImage.systemImageNamed("gearshape"),
                    selectedImage = UIImage.systemImageNamed("gearshape.fill"),
                )
            }
            AnimatedTabBarController(primary).apply {
                setViewControllers(
                    viewControllers = listOf(
                        homeNavigationController,
                        recordingNavController,
                        settingsNavController,
                    ),
                    animated = true,
                )
                mode = UITabBarControllerModeTabSidebar
            }
        },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.Cooperative(),
            isNativeAccessibilityEnabled = true,
            placedAsOverlay = true,
        ),
        modifier = modifier,
    )
}


private class AnimatedTabBarController : UITabBarController,
    UITabBarControllerDelegateProtocol {

    constructor(selectedColor: Color) : super(nibName = null, bundle = null) {
        delegate = this
        tabBar.tintColor = selectedColor.toUIColor()
        tabBar.unselectedItemTintColor = UIColor.secondaryLabelColor
    }

    override fun tabBarController(
        tabBarController: UITabBarController,
        animationControllerForTransitionFromViewController: UIViewController,
        toViewController: UIViewController,
    ): UIViewControllerAnimatedTransitioningProtocol {

        return object : NSObject(), UIViewControllerAnimatedTransitioningProtocol {

            override fun transitionDuration(transitionContext: UIViewControllerContextTransitioningProtocol?): NSTimeInterval =
                0.28

            override fun animateTransition(transitionContext: UIViewControllerContextTransitioningProtocol) {
                val fromView = transitionContext.viewForKey(UITransitionContextFromViewKey) ?: run {
                    transitionContext.completeTransition(false)
                    return
                }

                val toView = transitionContext.viewForKey(UITransitionContextToViewKey) ?: run {
                    transitionContext.completeTransition(false)
                    return
                }

                val container = transitionContext.containerView

                toView.alpha = 0.0
                container.addSubview(toView)

                UIView.animateWithDuration(
                    duration = transitionDuration(transitionContext),
                    animations = {
                        fromView.alpha = 0.0
                        toView.alpha = 1.0
                    },
                    completion = { finished ->
                        fromView.alpha = 1.0
                        toView.alpha = 1.0

                        transitionContext.completeTransition(
                            finished && !transitionContext.transitionWasCancelled(),
                        )
                    },
                )
            }
        }
    }
}

private fun Color.toUIColor(): UIColor {
    return UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}
