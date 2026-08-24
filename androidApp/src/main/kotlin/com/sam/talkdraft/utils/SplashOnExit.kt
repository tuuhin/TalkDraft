package com.sam.talkdraft.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.splashscreen.SplashScreen


internal fun SplashScreen.animateOnExit(
    screenViewDuration: Long = 200L,
    onAnimationStart: () -> Unit = {},
    onAnimationEnd: () -> Unit = {},
) = setOnExitAnimationListener { screenView ->

    val icon = try {
        screenView.iconView
    } catch (_: NullPointerException) {
        null
    }

    val viewScaleXAnimation = ObjectAnimator
        .ofFloat(screenView.view, View.SCALE_X, 1.0f, 1.25f)
        .apply {
            duration = screenViewDuration
            interpolator = AccelerateInterpolator()
        }

    val viewScaleYAnimation = ObjectAnimator
        .ofFloat(screenView.view, View.SCALE_Y, 1.0f, 1.25f)
        .apply {
            duration = screenViewDuration
            interpolator = AccelerateInterpolator()
        }

    val viewFadeAnimation = ObjectAnimator
        .ofFloat(screenView.view, View.ALPHA, 1.0f, 0.0f)
        .apply {
            duration = screenViewDuration
            interpolator = DecelerateInterpolator()
        }

    val viewAnimatorSet = AnimatorSet().apply {
        playTogether(viewScaleXAnimation, viewScaleYAnimation, viewFadeAnimation)
        doOnEnd {
            screenView.remove()
            onAnimationEnd()
        }
    }

    if (icon == null) {
        onAnimationStart()
        viewAnimatorSet.start()
        return@setOnExitAnimationListener
    }

    val iconAnimDuration = screenView.iconAnimationDurationMillis.takeIf { it > 0 } ?: screenViewDuration

    val iconScaleXAnimation = ObjectAnimator
        .ofFloat(icon, View.SCALE_X, 1.0f, 0.65f)
        .apply {
            duration = iconAnimDuration
            interpolator = DecelerateInterpolator()
        }

    val iconScaleYAnimation = ObjectAnimator
        .ofFloat(icon, View.SCALE_Y, 1.0f, 0.65f)
        .apply {
            duration = iconAnimDuration
            interpolator = DecelerateInterpolator()
        }

    val iconAnimatorSet = AnimatorSet().apply {
        playTogether(iconScaleXAnimation, iconScaleYAnimation)
        doOnStart { onAnimationStart() }
        doOnEnd {
            viewAnimatorSet.start()
        }
    }

    iconAnimatorSet.start()
}
