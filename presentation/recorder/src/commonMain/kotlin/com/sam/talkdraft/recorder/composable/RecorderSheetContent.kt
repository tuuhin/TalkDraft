package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.model.RecorderScreenState
import com.sam.talkdraft.recorder.model.RecorderSetupFailedReason
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.recorder.generated.resources.Res
import talkdraft.presentation.recorder.generated.resources.ic_no_record_audio_permission
import talkdraft.presentation.recorder.generated.resources.ic_record_audio
import talkdraft.presentation.recorder.generated.resources.ic_transcription_completed

@Composable
internal fun RecorderSheetContent(
    state: RecorderScreenState = RecorderScreenState(),
    modifier: Modifier = Modifier,
    recordingUI: @Composable () -> Unit,
) {

    val semiState by remember(state) {
        derivedStateOf {
            if (!state.isLoaded) return@derivedStateOf SheetContentState.GetStarted
            when (state.failedReason) {
                is RecorderSetupFailedReason.FailedTranscriptionModelSetup -> SheetContentState.FailedModel(state.failedReason.errorMessage)
                RecorderSetupFailedReason.MissingPermission -> SheetContentState.MissingPermission
                RecorderSetupFailedReason.None -> when (state.recorderState) {
                    RecorderState.IDLE, RecorderState.PREPARING, RecorderState.CANCELLED -> SheetContentState.GetStarted
                    RecorderState.RECORDING, RecorderState.PAUSED -> SheetContentState.OngoingRecording
                    RecorderState.COMPLETED -> SheetContentState.CaptureCompleted()
                }
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = semiState,
            transitionSpec = {
                fadeIn(animationSpec = tween(150, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(150)) togetherWith
                    fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing)) using
                    SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ -> tween(durationMillis = 200, easing = FastOutSlowInEasing) },
                    )
            },
            label = "transition_between_states",
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.Center,
        ) { contentState ->
            when (contentState) {
                SheetContentState.GetStarted -> OneImageTwoTextLayout(
                    painter = painterResource(Res.drawable.ic_record_audio),
                    title = "What’s on your mind?",
                    text = "Say it out loud and see your thoughts turn into text",
                )

                is SheetContentState.CaptureCompleted ->
                    OneImageTwoTextLayout(
                        painter = painterResource(Res.drawable.ic_transcription_completed),
                        title = "Your thought is captured",
                        text = contentState.transcript ?: "Your words are saved and ready to revisit",
                        isFailed = contentState.transcript != null,
                    )

                SheetContentState.OngoingRecording -> recordingUI()
                is SheetContentState.FailedModel -> OneImageTwoTextLayout(
                    painter = painterResource(Res.drawable.ic_transcription_completed),
                    title = "Missing access to mic",
                    text = "Cannot access the device microphone please make sure you got permissions",
                )

                SheetContentState.MissingPermission -> OneImageTwoTextLayout(
                    painter = painterResource(Res.drawable.ic_no_record_audio_permission),
                    title = "Missing access to mic",
                    text = "Cannot access the device microphone please make sure you got permissions",
                )
            }
        }
    }
}


@Composable
private fun OneImageTwoTextLayout(
    painter: Painter,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    isFailed: Boolean = false,
) {
    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painter,
            contentDescription = "Content for $title",
            modifier = Modifier.size(80.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(.8f),
        )
    }
}

private sealed class SheetContentState {
    data object GetStarted : SheetContentState()
    data object OngoingRecording : SheetContentState()
    data class CaptureCompleted(val transcript: String? = null) : SheetContentState()
    data class FailedModel(val message: String? = null) : SheetContentState()
    data object MissingPermission : SheetContentState()
}
