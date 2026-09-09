package com.sam.talkdraft.onboarding.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sam.talkdraft.common.model.PlatformTarget
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.onboarding.composables.scenes.FirstRecordingScene
import com.sam.talkdraft.onboarding.composables.scenes.HowDoesItWorkContainer
import com.sam.talkdraft.onboarding.composables.scenes.LocalAndPrivacyScene
import com.sam.talkdraft.onboarding.composables.scenes.LocalTranscriptionModelScene
import com.sam.talkdraft.onboarding.composables.scenes.PermissionsScene
import com.sam.talkdraft.onboarding.composables.scenes.WelcomeContainer
import com.sam.talkdraft.onboarding.composables.scenes.WhatWillYouCaptureScene
import com.sam.talkdraft.onboarding.models.CaptureIdeaOption
import com.sam.talkdraft.onboarding.models.OnboardingScene
import com.sam.talkdraft.permissions.model.IosPermissionStatus
import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun OnBoardingScenes(
    page: Int,
    onAction: () -> Unit,
    onNavigateToModelDownload: () -> Unit,
    onNavigateToRecorder: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateCaptureIdea: (CaptureIdeaOption) -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    recommendModel: TranscriptionModel? = null,
    permissions: ImmutableMap<Permissions, PermissionState> = persistentMapOf(),
    capturedIdeas: ImmutableSet<CaptureIdeaOption> = persistentSetOf(),
    contentPadding: PaddingValues = PaddingValues.Zero,
    platform: PlatformTarget = PlatformTarget.UNKNOWN,
) {

    val hasAudioRecordPermission by remember(permissions) {
        derivedStateOf {
            if (permissions.isEmpty()) return@derivedStateOf false
            val per = permissions.getOrElse(Permissions.RECORD_AUDIO) { return@derivedStateOf false }
            when (per) {
                is PermissionState.AndroidPermissionState -> per.isGranted
                is PermissionState.IosPermissionState -> per.status == IosPermissionStatus.GRANTED
            }
        }
    }

    Box(
        modifier = modifier
            .animateContentSize()
            .padding(contentPadding),
        contentAlignment = Alignment.BottomCenter,
    ) {
        when (page) {
            OnboardingScene.WELCOME_SCREEN.index -> WelcomeContainer(onAction = onAction)
            OnboardingScene.HOW_DOES_IT_WORK_SCREEN.index -> HowDoesItWorkContainer(onAction = onAction)
            OnboardingScene.FIRST_RECORDING_SCREEN.index -> FirstRecordingScene(
                onContinueToHome = onNavigateToHome,
                isRecordingEnabled = hasAudioRecordPermission,
                onTryRecording = onNavigateToRecorder,
            )

            OnboardingScene.VOICE_MODEL_SETUP.index -> LocalTranscriptionModelScene(
                onAction = onAction,
                recommendModel = recommendModel,
                onNavigateToModelDownload = onNavigateToModelDownload,
            )

            OnboardingScene.WHAT_WILL_YOU_CAPTURE_SCREEN.index -> WhatWillYouCaptureScene(
                onAction = onAction,
                capturedItems = capturedIdeas,
                onSelectIdea = onUpdateCaptureIdea,
            )

            OnboardingScene.LOCAL_FIRST_AND_PRIVACY.index -> LocalAndPrivacyScene(onAction = onAction)

            OnboardingScene.PERMISSIONS.index -> PermissionsScene(
                platform = platform,
                permissions = permissions,
                onAction = onAction,
                openAppSettings = onOpenAppSettings,
                onRequestPermissions = onRequestPermissions,
            )

            else -> {}
        }
    }
}

