package com.sam.talkdraft.app.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun AndroidAppUpdateRequiredDialog(
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        confirmButton = {
            Button(
                onClick = onUpdate,
                shapes = ButtonDefaults.shapes(
                    shape = ButtonDefaults.elevatedShape,
                    pressedShape = ButtonDefaults.pressedShape,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text(text = "Update", style = MaterialTheme.typography.titleMediumEmphasized)
            }
        },
        title = { Text(text = "App update required") },
        text = { Text(text = "App is running on a lower version which is lesser minimum version required version please update your app") },
    )
}
