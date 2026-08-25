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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.sam.talkdraft.onboarding.composables.CaptureIdeaCard
import com.sam.talkdraft.onboarding.composables.OnboardingContextAction
import com.sam.talkdraft.onboarding.models.CaptureIdeaOption
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun WhatWillYouCaptureScene(
    capturedItems: ImmutableSet<CaptureIdeaOption>,
    onSelectIdea: (CaptureIdeaOption) -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.displaySmallEmphasized,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {

    Column(
        modifier = modifier.fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.Start) {
            CompositionLocalProvider(
                LocalTextStyle provides titleStyle.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(text = "What do you want")
                Text(text = "to Capture?", fontStyle = FontStyle.Italic)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Choose what matters to you. You can change this anytime",
            style = MaterialTheme.typography.titleSmallEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "There’s no wrong choice",
            style = MaterialTheme.typography.titleSmallEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        CaptureIdeaOption.entries.fastForEach { idea ->
            val isSelected = idea in capturedItems
            CaptureIdeaCard(
                idea = idea,
                onSelect = { onSelectIdea(idea) },
                isSelected = isSelected,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        OnboardingContextAction(title = "Continue", onClick = onAction)
    }
}
