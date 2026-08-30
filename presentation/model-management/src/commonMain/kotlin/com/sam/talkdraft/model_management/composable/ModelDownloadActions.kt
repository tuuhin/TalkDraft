package com.sam.talkdraft.model_management.composable

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
internal fun ModelDownloadActions(
    onCancelDownload: () -> Unit,
    onStartDownload: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloadEnabled: Boolean = true,
    isCancelEnabled: Boolean = true,
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
            enableUserInput = isCancelEnabled,
            tooltip = {
                RichTooltip(
                    title = { Text(text = "Cancel download") },
                    text = { Text(text = "Cancels the ongoing model download") },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TooltipDefaults.richTooltipColors(titleContentColor = MaterialTheme.colorScheme.error),
                )
            },
            state = rememberTooltipState(),
        ) {
            Button(
                onClick = onCancelDownload,
                enabled = isCancelEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
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
            enableUserInput = isDownloadEnabled,
            tooltip = {
                RichTooltip(
                    title = { Text(text = "Cancel download") },
                    text = { Text(text = "Cancels the ongoing model download") },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TooltipDefaults.richTooltipColors(),
                )
            },
            state = rememberTooltipState(),
        ) {
            Button(
                onClick = onStartDownload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                enabled = isDownloadEnabled,
                shapes = ButtonDefaults.shapes(
                    shape = MaterialTheme.shapes.extraLarge,
                    pressedShape = MaterialTheme.shapes.large,
                ),
                contentPadding = ButtonDefaults.MediumContentPadding,
            ) {
                Text(text = "Download", style = MaterialTheme.typography.bodyLargeEmphasized)
            }
        }
    }
}
