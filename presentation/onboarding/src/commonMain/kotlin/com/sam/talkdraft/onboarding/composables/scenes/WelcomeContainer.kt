package com.sam.talkdraft.onboarding.composables.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.talkdraft.onboarding.composables.OnboardingContextAction

@Composable
internal fun WelcomeContainer(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.displayMediumEmphasized,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            CompositionLocalProvider(
                LocalTextStyle provides titleStyle.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold, lineHeight = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(text = "Speak")
                Text(text = "Capture")
                Text(text = "Understand")
            }
        }
        Text(
            text = "Turn your thoughts into text, simply by speaking",
            style = MaterialTheme.typography.bodyLargeEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OnboardingContextAction(title = "Get Started", onClick = onAction)
        TextButton(
            onClick = {},
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = false,
        ) {
            Text(
                text = "Privacy policy",
                letterSpacing = 1.2.sp,
                style = MaterialTheme.typography.labelLargeEmphasized,
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
