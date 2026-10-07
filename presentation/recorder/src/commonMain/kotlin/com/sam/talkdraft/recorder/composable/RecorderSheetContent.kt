package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.model.RecorderFailedReason
import com.sam.talkdraft.recorder.model.RecorderSheetState
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.recorder.generated.resources.Res
import talkdraft.presentation.recorder.generated.resources.ic_no_record_audio_permission
import talkdraft.presentation.recorder.generated.resources.ic_record_audio
import talkdraft.presentation.recorder.generated.resources.ic_transcription_completed

@Composable
internal fun RecorderSheetContent(
    modifier: Modifier = Modifier,
    state: RecorderSheetState = RecorderSheetState(),
    recordingUI: @Composable () -> Unit,
    completedUI: @Composable () -> Unit,
) {

    val motionScheme = MaterialTheme.motionScheme

    val screenState by remember(state) {
        derivedStateOf {
            when (state.failedReason) {
                is RecorderFailedReason.GenericError -> SheetContentState.CaptureOrSetupFailed(state.failedReason.errorMessage)
                RecorderFailedReason.MissingPermission -> SheetContentState.MissingPermission
                RecorderFailedReason.None if state.isModelSetupRunning -> SheetContentState.PreparingForRecording
                else -> when (state.recorderState) {
                    RecorderState.IDLE, RecorderState.PREPARING -> SheetContentState.GetStarted
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
            targetState = screenState,
            transitionSpec = {
                scaleIn(initialScale = 0.97f, animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) togetherWith
                    scaleOut(
                        targetScale = 0.97f, animationSpec = motionScheme.defaultSpatialSpec(),
                    ) + fadeOut(animationSpec = motionScheme.defaultEffectsSpec()) using SizeTransform(
                    clip = false,
                    sizeAnimationSpec = { _, _ -> motionScheme.defaultSpatialSpec() },
                )
            },
            label = "transition_between_states",
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) { contentState ->
            when (contentState) {
                SheetContentState.GetStarted -> OneImageTwoTextLayout(
                    painter = painterResource(Res.drawable.ic_record_audio),
                    title = "What’s on your mind?",
                    text = "Say it out loud and see your thoughts turn into text",
                )

                is SheetContentState.CaptureOrSetupFailed -> OneImageTwoTextLayout(
                    painter = painterResource(Res.drawable.ic_transcription_completed),
                    title = "Missing access to mic",
                    text = "Cannot access the device microphone please make sure you got permissions",
                )

                SheetContentState.MissingPermission -> OneImageTwoTextLayout(
                    painter = painterResource(Res.drawable.ic_no_record_audio_permission),
                    title = "Missing access to mic",
                    text = "Cannot access the device microphone please make sure you got permissions",
                )

                SheetContentState.PreparingForRecording -> Column(
                    modifier = modifier.wrapContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LoadingIndicator()
                    Text(text = "Preparing models and recorder")
                }

                is SheetContentState.CaptureCompleted -> completedUI()
                SheetContentState.OngoingRecording -> recordingUI()
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
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            fontWeight = FontWeight.Medium,
            color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(.8f),
        )
    }
}

private sealed class SheetContentState(val order: Int) {
    data object GetStarted : SheetContentState(0)
    data object OngoingRecording : SheetContentState(3)
    data object MissingPermission : SheetContentState(1)
    data object PreparingForRecording : SheetContentState(2)

    data class CaptureCompleted(val transcript: String? = null) : SheetContentState(5)
    data class CaptureOrSetupFailed(val message: String? = null) : SheetContentState(5)
}
