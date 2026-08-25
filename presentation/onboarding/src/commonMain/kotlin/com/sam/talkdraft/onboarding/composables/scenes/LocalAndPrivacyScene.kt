package com.sam.talkdraft.onboarding.composables.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.talkdraft.onboarding.composables.OnboardingContextAction

@Composable
internal fun LocalAndPrivacyScene(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.displaySmallEmphasized,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Column(
        modifier = modifier
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            CompositionLocalProvider(
                LocalTextStyle provides titleStyle.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(text = "Your voice stays on your device")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Transcribe with AI running locally, even when you're offline",
            style = MaterialTheme.typography.bodyMediumEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "More powerful AI when you need it",
            style = MaterialTheme.typography.bodySmallEmphasized,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = "Use optional cloud AI for advanced processing",
            style = MaterialTheme.typography.bodySmallEmphasized,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OnboardingContextAction(title = "Continue", onClick = onAction)
    }

}
