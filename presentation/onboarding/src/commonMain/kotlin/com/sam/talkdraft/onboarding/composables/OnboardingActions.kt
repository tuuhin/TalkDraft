package com.sam.talkdraft.onboarding.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun OnboardingContextAction(
    title: String,
    modifier: Modifier = Modifier,
    subTitle: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor),
        ),
        modifier = modifier.widthIn(min = 480.dp, max = 840.dp),
        enabled = enabled,
        shapes = ButtonDefaults.shapes(shape = ButtonDefaults.shape, pressedShape = MaterialTheme.shapes.large),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
            subTitle?.let {
                Text(
                    text = subTitle,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }
        }
    }
}
