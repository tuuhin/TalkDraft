package com.sam.talkdraft.designsystem.utils.toast

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGAffineTransformScale
import platform.CoreGraphics.CGSizeMake
import platform.QuartzCore.kCACornerCurveContinuous
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIApplication
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightMedium
import platform.UIKit.UIGlassEffect
import platform.UIKit.UIGlassEffectStyle
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UILabel
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode
import platform.UIKit.UIVisualEffect
import platform.UIKit.UIVisualEffectView
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.labelColor
import platform.UIKit.systemBackgroundColor
import platform.UIKit.systemBlueColor

@OptIn(ExperimentalForeignApi::class)
@Singleton
internal actual class PlatformToastProvider(private val dispatchers: IPlatformCoroutineDispatchers) {

    actual suspend fun showToastMessage(message: String, mode: ToastDuration) {
        val displayDuration = if (mode == ToastDuration.SHORT) 2.seconds else 3.5.seconds

        withContext(dispatchers.mainImmediate) {
            val window = getKeyWindow() ?: return@withContext

            val topInset = window.safeAreaInsets.useContents { top }
            val hasDynamicIsland = topInset >= DYNAMIC_ISLAND_TOP_INSET
            val useGlass = isIOS26OrLater()

            val toastView = UIView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                layer.cornerRadius = CORNER_RADIUS
                layer.masksToBounds = false
                if (!useGlass) {
                    layer.shadowColor = UIColor.blackColor.CGColor
                    layer.shadowOpacity = 0.15f
                    layer.shadowOffset = CGSizeMake(0.0, 6.0)
                    layer.shadowRadius = 12.0
                }
            }
            val visualEffect: UIVisualEffect = if (useGlass) {
                UIGlassEffect.effectWithStyle(UIGlassEffectStyle.UIGlassEffectStyleRegular).apply {
                    tintColor = UIColor.systemBackgroundColor.colorWithAlphaComponent(GLASS_TINT_ALPHA)
                }
            } else {
                UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleSystemMaterial)
            }

            val blurView = UIVisualEffectView(
                effect = if (useGlass) null else visualEffect,
            ).apply {
                translatesAutoresizingMaskIntoConstraints = false
                layer.cornerRadius = CORNER_RADIUS
                if (useGlass) {
                    layer.cornerCurve = kCACornerCurveContinuous
                    clipsToBounds = false
                } else {
                    clipsToBounds = true
                }
            }

            toastView.addSubview(blurView)

            val imageView = UIImageView(
                image = UIImage.systemImageNamed("info.circle.fill"),
            ).apply {
                tintColor = UIColor.systemBlueColor
                translatesAutoresizingMaskIntoConstraints = false
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
            }

            val label = UILabel().apply {
                text = message
                textColor = UIColor.labelColor
                font = UIFont.systemFontOfSize(14.0, weight = UIFontWeightMedium)
                numberOfLines = 0
                translatesAutoresizingMaskIntoConstraints = false
            }

            blurView.contentView.addSubview(imageView)
            blurView.contentView.addSubview(label)

            NSLayoutConstraint.activateConstraints(
                listOf(
                    blurView.topAnchor.constraintEqualToAnchor(toastView.topAnchor),
                    blurView.bottomAnchor.constraintEqualToAnchor(toastView.bottomAnchor),
                    blurView.leadingAnchor.constraintEqualToAnchor(toastView.leadingAnchor),
                    blurView.trailingAnchor.constraintEqualToAnchor(toastView.trailingAnchor),

                    imageView.leadingAnchor.constraintEqualToAnchor(
                        blurView.contentView.leadingAnchor,
                        constant = 16.0,
                    ),
                    imageView.centerYAnchor.constraintEqualToAnchor(blurView.contentView.centerYAnchor),
                    imageView.widthAnchor.constraintEqualToConstant(20.0),
                    imageView.heightAnchor.constraintEqualToConstant(20.0),

                    label.leadingAnchor.constraintEqualToAnchor(imageView.trailingAnchor, constant = 10.0),
                    label.trailingAnchor.constraintEqualToAnchor(
                        blurView.contentView.trailingAnchor,
                        constant = -18.0,
                    ),
                    label.topAnchor.constraintEqualToAnchor(blurView.contentView.topAnchor, constant = 12.0),
                    label.bottomAnchor.constraintEqualToAnchor(
                        blurView.contentView.bottomAnchor,
                        constant = -12.0,
                    ),
                ),
            )

            window.addSubview(toastView)

            val centerXConstraint = toastView.centerXAnchor.constraintEqualToAnchor(window.centerXAnchor)
            val widthConstraint =
                toastView.widthAnchor.constraintLessThanOrEqualToAnchor(window.widthAnchor, multiplier = 0.85)

            val hiddenTransform = {
                if (hasDynamicIsland) {
                    CGAffineTransformScale(CGAffineTransformMakeTranslation(0.0, -20.0), 0.2, 0.2)
                } else {
                    CGAffineTransformMakeScale(0.85, 0.85)
                }
            }

            if (hasDynamicIsland) {
                val topConstraint = toastView.topAnchor.constraintEqualToAnchor(
                    window.safeAreaLayoutGuide.topAnchor,
                    constant = 8.0,
                )
                NSLayoutConstraint.activateConstraints(listOf(topConstraint, centerXConstraint, widthConstraint))
            } else {
                val bottomConstraint = toastView.bottomAnchor.constraintEqualToAnchor(
                    window.safeAreaLayoutGuide.bottomAnchor,
                    constant = -30.0,
                )
                NSLayoutConstraint.activateConstraints(listOf(bottomConstraint, centerXConstraint, widthConstraint))
            }

            toastView.alpha = 0.0
            toastView.transform = hiddenTransform()

            window.layoutIfNeeded()

            try {
                UIView.animateWithDuration(
                    duration = 0.5,
                    delay = 0.0,
                    usingSpringWithDamping = 0.7,
                    initialSpringVelocity = 0.6,
                    options = 0u,
                    animations = {
                        toastView.alpha = 1.0
                        toastView.transform = CGAffineTransformIdentity.readValue()
                        if (useGlass) blurView.effect = visualEffect
                        window.layoutIfNeeded()
                    },
                    completion = null,
                )
                delay(displayDuration)
            } finally {
                UIView.animateWithDuration(
                    duration = if (hasDynamicIsland) 0.35 else 0.25,
                    delay = 0.0,
                    options = 0u,
                    animations = {
                        toastView.alpha = 0.0
                        toastView.transform = hiddenTransform()
                        if (useGlass) blurView.effect = null // glass dematerializes
                    },
                    completion = { _ ->
                        toastView.removeFromSuperview()
                    },
                )
            }
        }
    }

    private fun getKeyWindow(): UIWindow? {
        val sharedApp = UIApplication.sharedApplication
        val connectedScenes = sharedApp.connectedScenes
        for (scene in connectedScenes) {
            val windowScene = scene as? UIWindowScene ?: continue
            if (windowScene.activationState == UISceneActivationStateForegroundActive) {
                val window = windowScene.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() }
                if (window != null) return window
            }
        }
        return sharedApp.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() }
            ?: sharedApp.keyWindow
    }

    private fun isIOS26OrLater(): Boolean {
        val majorVersion = UIDevice.currentDevice.systemVersion
            .substringBefore(".")
            .toIntOrNull() ?: return false

        return majorVersion >= 26
    }

    companion object {
        private const val CORNER_RADIUS = 24.0
        private const val DYNAMIC_ISLAND_TOP_INSET = 50.0
        private const val GLASS_TINT_ALPHA = 0.18
    }
}
