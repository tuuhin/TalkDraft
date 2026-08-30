package com.sam.talkdraft.onboarding.composables.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.talkdraft.onboarding.composables.OnboardingContextAction

@Composable
internal fun FirstRecordingScene(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.displaySmallEmphasized,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "2. Get your offline voice model",
            style = titleStyle,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Download the model once to transcribe your recordings on your device, even without internet",
            style = MaterialTheme.typography.titleSmallEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OnboardingContextAction(
            title = "Try y",
            onClick = {

            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
        OnboardingContextAction(
            title = "Continue to Home",
            onClick = onAction,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
    }
}
