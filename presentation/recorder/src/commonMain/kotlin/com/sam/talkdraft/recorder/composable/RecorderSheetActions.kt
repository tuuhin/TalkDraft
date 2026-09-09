package com.sam.talkdraft.recorder.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RecorderSheetActions(
    onRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp,
            ),
            enableUserInput = isRecording,
            tooltip = {
                RichTooltip(
                    title = { Text(text = "Cancel recording") },
                    text = { Text(text = "Cancels recording for now try it later") },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TooltipDefaults.richTooltipColors(titleContentColor = MaterialTheme.colorScheme.error),
                )
            },
            state = rememberTooltipState(),
        ) {
            Button(
                onClick = onCancel,
                enabled = isRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                shapes = ButtonDefaults.shapes(
                    shape = MaterialTheme.shapes.extraLarge,
                    pressedShape = MaterialTheme.shapes.large,
                ),
                contentPadding = ButtonDefaults.MediumContentPadding,
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                )
            }
        }
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp,
            ),
            enableUserInput = isRecording,
            tooltip = {
                RichTooltip(
                    title = { Text(text = if (isRecording) "Stop Capture" else "Start Capture") },
                    text = { Text(text = if (isRecording) "Stop ongoing recorder" else "Start capturing your thought") },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TooltipDefaults.richTooltipColors(),
                )
            },
            state = rememberTooltipState(),
        ) {
            Button(
                onClick = if (isRecording) onStopRecording else onRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isRecording) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (!isRecording) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                ),
                shapes = ButtonDefaults.shapes(
                    shape = MaterialTheme.shapes.extraLarge,
                    pressedShape = MaterialTheme.shapes.large,
                ),
                contentPadding = ButtonDefaults.MediumContentPadding,
            ) {
                Text(
                    text = if (!isRecording) "Capture" else "Stop",
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                )
            }
        }
    }
}
