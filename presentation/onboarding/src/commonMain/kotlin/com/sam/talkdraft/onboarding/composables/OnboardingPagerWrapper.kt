package com.sam.talkdraft.onboarding.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sam.talkdraft.onboarding.composables.scenes.HowDoesItWorkContainer
import com.sam.talkdraft.onboarding.composables.scenes.LocalAndPrivacyScene
import com.sam.talkdraft.onboarding.composables.scenes.PermissionsScene
import com.sam.talkdraft.onboarding.composables.scenes.WelcomeContainer
import com.sam.talkdraft.onboarding.composables.scenes.WhatWillYouCaptureScene
import com.sam.talkdraft.onboarding.models.CaptureIdeaOption
import com.sam.talkdraft.onboarding.models.OnboardingScene
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun OnBoardingScreens(
    page: Int,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateCaptureIdea: (CaptureIdeaOption) -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    capturedIdeas: ImmutableSet<CaptureIdeaOption> = persistentSetOf(),
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Box(
        modifier = modifier
            .animateContentSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        when (page) {
            OnboardingScene.WELCOME_SCREEN.index -> WelcomeContainer(
                onAction = onAction,
                contentPadding = contentPadding,
            )

            OnboardingScene.HOW_DOES_IT_WORK_SCREEN.index -> HowDoesItWorkContainer(
                onAction = onAction,
                contentPadding = contentPadding,
            )

            OnboardingScene.WHAT_WILL_YOU_CAPTURE_SCREEN.index -> WhatWillYouCaptureScene(
                onAction = onAction,
                capturedItems = capturedIdeas,
                onSelectIdea = onUpdateCaptureIdea,
                contentPadding = contentPadding,
            )

            OnboardingScene.LOCAL_FIRST_AND_PRIVACY.index -> LocalAndPrivacyScene(
                onAction = onAction,
                contentPadding = contentPadding,
            )

            OnboardingScene.PERMISSIONS.index -> PermissionsScene(
                onAction = onAction,
                openAppSettings = onOpenAppSettings,
                contentPadding = contentPadding,
            )

            else -> {}
        }
    }
}

