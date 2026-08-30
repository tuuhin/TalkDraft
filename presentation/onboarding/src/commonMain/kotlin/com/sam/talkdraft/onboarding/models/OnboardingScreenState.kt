package com.sam.talkdraft.onboarding.models

import androidx.compose.runtime.Immutable
import com.sam.talkdraft.common.model.PlatformTarget
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class OnboardingScreenState(
    val recommended: TranscriptionModel? = null,
    val capturedIdeas: ImmutableSet<CaptureIdeaOption> = persistentSetOf(CaptureIdeaOption.IDEAS),
    val platform: PlatformTarget = PlatformTarget.UNKNOWN,
)
