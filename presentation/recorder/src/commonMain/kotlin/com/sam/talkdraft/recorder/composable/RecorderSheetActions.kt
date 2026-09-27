package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.recorder.generated.resources.Res
import talkdraft.presentation.recorder.generated.resources.ic_mic_variant_1
import talkdraft.presentation.recorder.generated.resources.ic_recording_stop

@Composable
internal fun RecorderSheetActions(
    onRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
) {
    val motionScheme = MaterialTheme.motionScheme

    AnimatedContent(
        targetState = isRecording,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val enterTransition = fadeIn(
                animationSpec = motionScheme.fastSpatialSpec(),
            ) + scaleIn(
                initialScale = 0.85f,
                animationSpec = motionScheme.fastSpatialSpec(),
            )

            val exitTransition = fadeOut(
                animationSpec = motionScheme.fastEffectsSpec(),
            ) + scaleOut(
                targetScale = 0.85f,
                animationSpec = motionScheme.fastEffectsSpec(),
            )

            enterTransition togetherWith exitTransition using (
                SizeTransform(
                    clip = false,
                    sizeAnimationSpec = { _, _ -> motionScheme.defaultEffectsSpec() },
                )
                )
        },
        modifier = modifier,
    ) { isCapturing ->
        if (isCapturing) {
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
                    modifier = Modifier.height(ButtonDefaults.MediumContainerHeight).fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_recording_stop),
                        contentDescription = "Stop button",
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)),
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                    Text(
                        text = "Stop Recording",
                        fontWeight = FontWeight.Bold,
                        style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                    )
                }
            }
        } else {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                    spacingBetweenTooltipAndAnchor = 4.dp,
                ),
                enableUserInput = isRecording,
                tooltip = {
                    RichTooltip(
                        title = { Text(text = "Start Recording") },
                        text = { Text(text = "Start recording") },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = TooltipDefaults.richTooltipColors(titleContentColor = MaterialTheme.colorScheme.error),
                    )
                },
                state = rememberTooltipState(),
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
    }
}
