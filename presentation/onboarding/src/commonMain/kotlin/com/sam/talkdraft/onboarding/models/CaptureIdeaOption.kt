package com.sam.talkdraft.onboarding.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import talkdraft.presentation.onboarding.generated.resources.Res
import talkdraft.presentation.onboarding.generated.resources.capture_option_brainstorming_text
import talkdraft.presentation.onboarding.generated.resources.capture_option_brainstorming_title
import talkdraft.presentation.onboarding.generated.resources.capture_option_idea_text
import talkdraft.presentation.onboarding.generated.resources.capture_option_idea_title
import talkdraft.presentation.onboarding.generated.resources.capture_option_meetings_text
import talkdraft.presentation.onboarding.generated.resources.capture_option_meetings_title
import talkdraft.presentation.onboarding.generated.resources.capture_option_personal_note_text
import talkdraft.presentation.onboarding.generated.resources.capture_option_personal_note_title
import talkdraft.presentation.onboarding.generated.resources.ic_brain_filled
import talkdraft.presentation.onboarding.generated.resources.ic_brain_outlined
import talkdraft.presentation.onboarding.generated.resources.ic_idea_filled
import talkdraft.presentation.onboarding.generated.resources.ic_idea_outlined
import talkdraft.presentation.onboarding.generated.resources.ic_metting_filled
import talkdraft.presentation.onboarding.generated.resources.ic_metting_outlined
import talkdraft.presentation.onboarding.generated.resources.ic_note_filled
import talkdraft.presentation.onboarding.generated.resources.ic_note_outlined

internal enum class CaptureIdeaOption {
    IDEAS,
    PERSONAL_NOTE,
    MEETINGS,
    BRAIN_STORMING;

    val title: String
        @Composable
        get() = when (this) {
            IDEAS -> stringResource(Res.string.capture_option_idea_title)
            PERSONAL_NOTE -> stringResource(Res.string.capture_option_personal_note_title)
            MEETINGS -> stringResource(Res.string.capture_option_meetings_title)
            BRAIN_STORMING -> stringResource(Res.string.capture_option_brainstorming_title)
        }

    val text: String
        @Composable
        get() = when (this) {
            IDEAS -> stringResource(Res.string.capture_option_idea_text)
            PERSONAL_NOTE -> stringResource(Res.string.capture_option_personal_note_text)
            MEETINGS -> stringResource(Res.string.capture_option_meetings_text)
            BRAIN_STORMING -> stringResource(Res.string.capture_option_brainstorming_text)
        }

    val painterOutlined: Painter
        @Composable
        get() = when (this) {
            IDEAS -> painterResource(Res.drawable.ic_idea_outlined)
            PERSONAL_NOTE -> painterResource(Res.drawable.ic_note_outlined)
            MEETINGS -> painterResource(Res.drawable.ic_metting_outlined)
            BRAIN_STORMING -> painterResource(Res.drawable.ic_brain_outlined)
        }

    val painterFilled: Painter
        @Composable
        get() = when (this) {
            IDEAS -> painterResource(Res.drawable.ic_idea_filled)
            PERSONAL_NOTE -> painterResource(Res.drawable.ic_note_filled)
            MEETINGS -> painterResource(Res.drawable.ic_metting_filled)
            BRAIN_STORMING -> painterResource(Res.drawable.ic_brain_filled)
        }
}
