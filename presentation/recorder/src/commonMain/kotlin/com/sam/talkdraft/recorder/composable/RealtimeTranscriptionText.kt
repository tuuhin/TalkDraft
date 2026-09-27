package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.ic_error_simple
import com.sam.talkdraft.designs.ic_listener
import com.sam.talkdraft.designsystem.theme.montserrat
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
                is TranscriptionState.Failed -> UIRealtimeTranscriptionMode.Error
                TranscriptionState.Idle -> UIRealtimeTranscriptionMode.Idle
                TranscriptionState.Preparing -> UIRealtimeTranscriptionMode.Preparing
                TranscriptionState.Ready -> UIRealtimeTranscriptionMode.Ready
                is TranscriptionState.Success -> {
                    val id = state.segments.lastOrNull()?.segmentId
                        ?: return@derivedStateOf UIRealtimeTranscriptionMode.Ready
                    UIRealtimeTranscriptionMode.Success(segmentId = id)
                }
            }
        }
    }

    Box(
        modifier = modifier.heightIn(min = 42.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                if (targetState is UIRealtimeTranscriptionMode.Success) {
                    val enter = fadeIn() + slideInVertically { height -> height / 2 } + expandVertically()
                    val exit = fadeOut() + shrinkVertically()
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
                    GrowingTranscriptionText(
                        segmentModel = transcription?.segments?.lastOrNull(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is UIRealtimeTranscriptionMode.Error -> {
                    val failed = state as? TranscriptionState.Failed
                    val failedMessage = failed?.message ?: failed?.error?.uiMessage ?: "Unable to process"
                    TranscriptionEngineFailed(error = failedMessage)
                }

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
    data object Error : UIRealtimeTranscriptionMode()
}

@Composable
private fun GrowingTranscriptionText(
    segmentModel: TranscriptionSegmentModel?,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = montserrat(),
) {
    val words = segmentModel?.text?.trim()?.split("\\s+".toRegex()) ?: emptyList()
    val segmentId = segmentModel?.segmentId ?: -1

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = 8,
    ) {
        words.forEachIndexed { index, word ->
            key("$segmentId\"_${index}_$word") {
                Text(
                    text = word,
                    fontFamily = fontFamily,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
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
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = error ?: "Transcription failed",
            style = MaterialTheme.typography.titleMediumEmphasized,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun EngineReadyIndicator(modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = "Listening.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = modifier,
                fontWeight = FontWeight.SemiBold,
            )
        },
        border = null,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            Icon(
                painter = painterResource(CommonResources.drawable.ic_listener),
                contentDescription = "Listener",
            )
        },
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .8f),
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Composable
private fun PreparingEngineContainer(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        LoadingIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = "Initializing engine...",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}
