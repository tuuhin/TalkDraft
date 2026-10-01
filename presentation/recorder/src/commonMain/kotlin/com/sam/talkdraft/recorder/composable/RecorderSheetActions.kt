package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.ic_cancel
import com.sam.talkdraft.designs.ic_reset
import com.sam.talkdraft.designsystem.annotations.LightThemedPreview
import com.sam.talkdraft.designsystem.utils.LocalAnimatedContentScope
import com.sam.talkdraft.designsystem.utils.LocalSharedTransitionScope
import com.sam.talkdraft.designsystem.utils.sharedBoundsWrapper
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.recorder.generated.resources.Res
import talkdraft.presentation.recorder.generated.resources.ic_mic_variant_1
import talkdraft.presentation.recorder.generated.resources.ic_recording_stop

@Composable
internal fun RecorderSheetActions(
    onRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onResetRecording: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    isRecordingCompleted: Boolean = false,
    isRecording: Boolean = false,
    isSavingRecording: Boolean = false,
) {

    val type by remember(isRecordingCompleted, isRecording) {
        derivedStateOf {
            when {
                isRecordingCompleted -> ActionType.SaveOrResetRecording
                isRecording -> ActionType.StopOrCancelRecording
                else -> ActionType.StartRecording
            }
        }
    }

    val motionScheme = MaterialTheme.motionScheme

    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            AnimatedContent(
                targetState = type,
                transitionSpec = {
                    val enterTransition = fadeIn(
                        animationSpec = motionScheme.fastSpatialSpec(),
                    )
                    val exitTransition = fadeOut(
                        animationSpec = motionScheme.fastEffectsSpec(),
                    )

                    enterTransition togetherWith exitTransition using SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ -> motionScheme.defaultSpatialSpec() },
                    )

                },
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) { actionType ->
                CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                    when (actionType) {
                        ActionType.StartRecording -> StartRecordingUI(
                            onRecording = onRecording,
                            isRecording = isRecording,
                        )

                        ActionType.StopOrCancelRecording -> StopOrCancelRecordingUI(
                            onCancelRecording = onCancelRecording,
                            onStopRecording = onStopRecording,
                            isRecording = isRecording,
                        )

                        ActionType.SaveOrResetRecording -> SaveOrResetRecording(
                            onSaveRecording = onSave,
                            onResetRecording = onResetRecording,
                            isRecordingCompleted = isRecordingCompleted,
                            isSaving = isSavingRecording,
                        )

                    }
                }
            }
        }
    }
}

@Composable
private fun SaveOrResetRecording(
    onResetRecording: () -> Unit,
    onSaveRecording: () -> Unit,
    isSaving: Boolean = false,
    isRecordingCompleted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onResetRecording,
            enabled = !isSaving,
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            shapes = ButtonDefaults.shapes(shape = ButtonDefaults.filledTonalShape),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.sharedBoundsWrapper(
                key = SharedTransitionKeys.SECONDARY_ACTION_BUTTON,
                enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = scaleOut(),
            ),
        ) {
            Icon(
                painter = painterResource(CommonResources.drawable.ic_reset),
                contentDescription = "Cancel recording",
            )
        }
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp,
            ),
            enableUserInput = isRecordingCompleted,
            tooltip = {
                RichTooltip(
                    title = { Text(text = "Stop Capture") },
                    text = { Text(text = "Stop ongoing recorder") },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TooltipDefaults.richTooltipColors(),
                )
            },
            state = rememberTooltipState(),
            modifier = Modifier.weight(1f),
        ) {
            Button(
                onClick = onSaveRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shapes = ButtonDefaults.shapes(
                    shape = MaterialTheme.shapes.extraLarge,
                    pressedShape = MaterialTheme.shapes.large,
                ),
                enabled = !isSaving,
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight, true),
                modifier = Modifier
                    .sharedBoundsWrapper(
                        key = SharedTransitionKeys.MAIN_ACTION_BUTTON,
                        clipShape = MaterialTheme.shapes.extraLarge,
                    )
                    .height(ButtonDefaults.MediumContainerHeight)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Save Recording",
                    fontWeight = FontWeight.Bold,
                    style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                )
            }
        }
    }
}

@Composable
private fun StopOrCancelRecordingUI(
    onCancelRecording: () -> Unit,
    onStopRecording: () -> Unit,
    isRecording: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onCancelRecording,
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            shapes = ButtonDefaults.shapes(shape = ButtonDefaults.filledTonalShape),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            modifier = Modifier.sharedBoundsWrapper(
                key = SharedTransitionKeys.SECONDARY_ACTION_BUTTON,
                enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = scaleOut(),
            ),
        ) {
            Icon(
                painter = painterResource(CommonResources.drawable.ic_cancel),
                contentDescription = "Cancel recording",
            )
        }
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp,
            ),
            enableUserInput = isRecording,
            tooltip = {
                RichTooltip(
                    title = { Text(text = "Stop Capture") },
                    text = { Text(text = "Stop ongoing recorder") },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TooltipDefaults.richTooltipColors(),
                )
            },
            state = rememberTooltipState(),
            modifier = Modifier.weight(1f),
        ) {
            Button(
                onClick = onStopRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                shapes = ButtonDefaults.shapes(
                    shape = MaterialTheme.shapes.extraLarge,
                    pressedShape = MaterialTheme.shapes.large,
                ),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight, true),
                modifier = Modifier
                    .sharedBoundsWrapper(
                        key = SharedTransitionKeys.MAIN_ACTION_BUTTON,
                        clipShape = MaterialTheme.shapes.extraLarge,
                    )
                    .height(ButtonDefaults.MediumContainerHeight)
                    .fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_recording_stop),
                    contentDescription = "Stop button",
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                Text(
                    text = "Stop",
                    fontWeight = FontWeight.Bold,
                    style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                )
            }
        }
    }
}

@Composable
private fun StartRecordingUI(
    onRecording: () -> Unit,
    isRecording: Boolean = false,
    modifier: Modifier = Modifier,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
            spacingBetweenTooltipAndAnchor = 4.dp,
        ),
        enableUserInput = !isRecording,
        tooltip = {
            RichTooltip(
                title = { Text(text = "Start Recording") },
                text = { Text(text = "Start recording") },
                shape = MaterialTheme.shapes.extraLarge,
                colors = TooltipDefaults.richTooltipColors(titleContentColor = MaterialTheme.colorScheme.error),
            )
        },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        Button(
            onClick = onRecording,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            shapes = ButtonDefaults.shapes(
                shape = MaterialTheme.shapes.extraLarge,
                pressedShape = MaterialTheme.shapes.large,
            ),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight, true),
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight)
                .sharedBoundsWrapper(
                    key = SharedTransitionKeys.MAIN_ACTION_BUTTON,
                    clipShape = MaterialTheme.shapes.extraLarge,
                    enter = fadeIn(MaterialTheme.motionScheme.slowSpatialSpec()) + scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = fadeOut() + scaleOut(),
                )
                .fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_mic_variant_1),
                contentDescription = "Stop button",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
            Text(
                text = "Start Recording",
                fontWeight = FontWeight.Bold,
                style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
            )
        }
    }
}

private object SharedTransitionKeys {
    const val MAIN_ACTION_BUTTON = "main_action_button"
    const val SECONDARY_ACTION_BUTTON = "secondary_action_button"
}

private enum class ActionType {
    StartRecording,
    StopOrCancelRecording,
    SaveOrResetRecording,
}

@Composable
@LightThemedPreview
private fun RecorderActionSheetPreview() {

    var isRecordingCompleted by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    Column {
        RecorderSheetActions(
            onRecording = {},
            onCancelRecording = {},
            onStopRecording = {},
            onResetRecording = {},
            onSave = {},
            isRecording = isRecording,
            isRecordingCompleted = isRecordingCompleted,
        )
        Row {
            ToggleButton(checked = isRecording, onCheckedChange = { isRecording = it }) {
                Text("recording")
            }
            ToggleButton(checked = isRecordingCompleted, onCheckedChange = { isRecordingCompleted = it }) {
                Text("is recording complterd")
            }
        }
    }
}
