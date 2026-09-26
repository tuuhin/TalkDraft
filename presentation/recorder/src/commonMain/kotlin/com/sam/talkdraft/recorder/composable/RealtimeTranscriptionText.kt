package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.ic_error_simple
import com.sam.talkdraft.designs.ic_listener
import com.sam.talkdraft.transcription.domain.model.TranscriptionError
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun RealtimeTranscriptionText(
    state: TranscriptionState,
    modifier: Modifier = Modifier,
) {
    val uiState by remember(state) {
        derivedStateOf {
            when (state) {
                is TranscriptionState.Failed -> {
                    val errorMessage = when (state.error) {
                        TranscriptionError.AudioNotFound -> "Audio not found"
                        TranscriptionError.TranscriptionFailed -> "Transcription Failed"
                        TranscriptionError.UnsupportedAudioFormat -> "UnSupported format"
                    }
                    UIRealtimeTranscriptionMode.Error(state.message ?: errorMessage)
                }

                TranscriptionState.Idle -> UIRealtimeTranscriptionMode.Idle
                TranscriptionState.Preparing -> UIRealtimeTranscriptionMode.Preparing
                TranscriptionState.Ready -> UIRealtimeTranscriptionMode.Ready
                is TranscriptionState.Success -> {
                    val id = state.segments.lastOrNull()?.segmentId ?: -1
                    UIRealtimeTranscriptionMode.Success(segmentId = id)
                }
            }
        }
    }

    Box(
        modifier = modifier.heightIn(min = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                if (targetState is UIRealtimeTranscriptionMode.Success) {
                    val enter = fadeIn() + slideInVertically { height -> height / 2 } + expandVertically()
                    val exit = fadeOut() + shrinkVertically() + shrinkVertically()
                    enter togetherWith exit using SizeTransform(clip = false)
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            },
            contentAlignment = Alignment.Center,
        ) { targetState ->
            when (targetState) {
                is UIRealtimeTranscriptionMode.Preparing -> PreparingEngineContainer()
                is UIRealtimeTranscriptionMode.Ready -> EngineReadyIndicator()
                is UIRealtimeTranscriptionMode.Success -> {
                    val transcription = state as? TranscriptionState.Success
                    GrowingTranscriptionText(segmentModel = transcription?.segments?.lastOrNull())
                }

                is UIRealtimeTranscriptionMode.Error -> TranscriptionEngineFailed(error = targetState.message)
                else -> {}
            }
        }
    }
}

private sealed class UIRealtimeTranscriptionMode {
    data object Idle : UIRealtimeTranscriptionMode()
    data object Preparing : UIRealtimeTranscriptionMode()
    data object Ready : UIRealtimeTranscriptionMode()

    // segment id differentiate a segment not the message
    data class Success(val segmentId: Long) : UIRealtimeTranscriptionMode()
    data class Error(val message: String) : UIRealtimeTranscriptionMode()
}

@Composable
private fun GrowingTranscriptionText(
    segmentModel: TranscriptionSegmentModel?,
    modifier: Modifier = Modifier,
) {
    if (segmentModel == null) return

    val words = segmentModel.text.trim().split("\\s+".toRegex())


    FlowRow(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = 8,
    ) {
        words.forEachIndexed { index, word ->
            key("${segmentModel.segmentId}_${index}_$word") {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(durationMillis = 120))
                        + expandIn(initialSize = { IntSize.Zero }),
                ) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

}

@Composable
private fun TranscriptionEngineFailed(modifier: Modifier = Modifier, error: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(CommonResources.drawable.ic_error_simple),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = error ?: "Transcription failed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EngineReadyIndicator(modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = "Listening...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier,
            )
        },
        icon = {
            Icon(painter = painterResource(CommonResources.drawable.ic_listener), contentDescription = "Listener")
        },
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .8f),
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    )
}

@Composable
private fun PreparingEngineContainer(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = "Initializing engine...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
