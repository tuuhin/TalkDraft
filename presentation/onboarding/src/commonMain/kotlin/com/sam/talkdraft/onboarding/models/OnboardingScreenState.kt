package com.sam.talkdraft.onboarding.models

import androidx.compose.runtime.Immutable
import com.sam.talkdraft.common.model.PlatformTarget
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class OnboardingScreenState(
    val recommended: TranscriptionModel? = null,
    val capturedIdeas: ImmutableSet<CaptureIdeaOption> = persistentSetOf(CaptureIdeaOption.IDEAS),
    val platform: PlatformTarget = PlatformTarget.UNKNOWN,
    val permissionsState: ImmutableMap<Permissions, PermissionState> = persistentMapOf(),
)
