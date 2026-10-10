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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.ic_cancel
import com.sam.talkdraft.designs.ic_reset
import com.sam.talkdraft.designsystem.utils.LocalAnimatedContentScope
import com.sam.talkdraft.designsystem.utils.LocalSharedTransitionScope
import com.sam.talkdraft.designsystem.utils.sharedBoundsWrapper
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.events.RecordingScreenEvent
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.recorder.generated.resources.Res
import talkdraft.presentation.recorder.generated.resources.ic_mic_variant_1
import talkdraft.presentation.recorder.generated.resources.ic_recording_stop

@Composable
internal fun RecorderSheetActions(
    recorderState: RecorderState,
    isSavable: Boolean,
    isSavingRecording: Boolean,
    isModelSetupRunning: Boolean,
    onAction: (RecordingScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionType = remember(recorderState) {
        when (recorderState) {
            RecorderState.COMPLETED -> ActionType.SaveOrResetRecording
            RecorderState.RECORDING -> ActionType.StopOrCancelRecording
            else -> ActionType.StartRecording
        }
    }

    val motionScheme = MaterialTheme.motionScheme
    val isActionEnabled = !isModelSetupRunning

    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            AnimatedContent(
                targetState = actionType,
                transitionSpec = {
                    val enterTransition = fadeIn(animationSpec = motionScheme.fastSpatialSpec())
                    val exitTransition = fadeOut(animationSpec = motionScheme.fastEffectsSpec())

                    enterTransition togetherWith exitTransition using SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ -> motionScheme.defaultSpatialSpec() },
                    )
                },
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) { targetType ->
                CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                    when (targetType) {
                        ActionType.StartRecording -> StartRecordingUI(
                            onStart = { onAction(RecordingScreenEvent.StartRecording) },
                            isEnabled = isActionEnabled,
                            isRecording = recorderState == RecorderState.RECORDING,
                        )

                        ActionType.StopOrCancelRecording -> DualActionButtonLayout(
                            secondaryColors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                            onSecondaryClick = { onAction(RecordingScreenEvent.OnCancelRecording) },
                            isSecondaryEnabled = isActionEnabled,
                            primaryColors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            onPrimaryClick = { onAction(RecordingScreenEvent.StopRecording) },
                            isPrimaryEnabled = isActionEnabled,
                            enableTooltip = recorderState == RecorderState.RECORDING,
                            tooltipTitle = "Stop Capture",
                            tooltipText = "Stop ongoing recorder",
                            secondaryContent = {
                                Icon(
                                    painter = painterResource(CommonResources.drawable.ic_cancel),
                                    contentDescription = "Cancel recording",
                                )
                            },
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

                        ActionType.SaveOrResetRecording -> DualActionButtonLayout(
                            secondaryColors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            onSecondaryClick = { onAction(RecordingScreenEvent.OnResetRecording) },
                            isSecondaryEnabled = !isSavingRecording && isActionEnabled,

                            primaryColors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            onPrimaryClick = { onAction(RecordingScreenEvent.OnSaveTranscription) },
                            isPrimaryEnabled = !isSavingRecording && isActionEnabled && isSavable,
                            enableTooltip = recorderState == RecorderState.COMPLETED,
                            tooltipTitle = "Stop Capture",
                            tooltipText = "Stop ongoing recorder",
                            secondaryContent = {
                                Icon(
                                    painter = painterResource(CommonResources.drawable.ic_reset),
                                    contentDescription = "Reset recording",
                                )
                            },
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
        }
    }
}

@Composable
private fun DualActionButtonLayout(
    onSecondaryClick: () -> Unit,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondaryEnabled: Boolean = true,
    primaryColors: ButtonColors = ButtonDefaults.buttonColors(),
    isPrimaryEnabled: Boolean = true,
    enableTooltip: Boolean = true,
    tooltipTitle: String = "Action title",
    tooltipText: String = "Action title description",
    secondaryColors: ButtonColors = ButtonDefaults.buttonColors(),
    secondaryContent: @Composable RowScope.() -> Unit,
    primaryContent: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onSecondaryClick,
            enabled = isSecondaryEnabled,
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            shapes = ButtonDefaults.shapes(shape = ButtonDefaults.filledTonalShape),
            colors = secondaryColors,
            modifier = Modifier.sharedBoundsWrapper(
                key = SharedTransitionKeys.SECONDARY_ACTION_BUTTON,
                enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = scaleOut(),
            ),
            content = secondaryContent,
        )

        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp,
            ),
            enableUserInput = enableTooltip,
            tooltip = {
                RichTooltip(
                    title = { Text(text = tooltipTitle) },
                    text = { Text(text = tooltipText) },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TooltipDefaults.richTooltipColors(),
                )
            },
            state = rememberTooltipState(),
            modifier = Modifier.weight(1f),
        ) {
            Button(
                onClick = onPrimaryClick,
                colors = primaryColors,
                shapes = ButtonDefaults.shapes(
                    shape = MaterialTheme.shapes.extraLarge,
                    pressedShape = MaterialTheme.shapes.large,
                ),
                enabled = isPrimaryEnabled,
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight, true),
                modifier = Modifier
                    .sharedBoundsWrapper(
                        key = SharedTransitionKeys.MAIN_ACTION_BUTTON,
                        clipShape = MaterialTheme.shapes.extraLarge,
                    )
                    .height(ButtonDefaults.MediumContainerHeight)
                    .fillMaxWidth(),
                content = primaryContent,
            )
        }
    }
}

@Composable
private fun StartRecordingUI(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isRecording: Boolean = false,
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
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            shapes = ButtonDefaults.shapes(
                shape = MaterialTheme.shapes.extraLarge,
                pressedShape = MaterialTheme.shapes.large,
            ),
            enabled = isEnabled,
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight, true),
            modifier = Modifier
                .heightIn(ButtonDefaults.MediumContainerHeight)
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
                contentDescription = "Start button",
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
