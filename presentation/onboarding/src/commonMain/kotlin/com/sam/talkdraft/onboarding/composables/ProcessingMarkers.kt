package com.sam.talkdraft.onboarding.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.onboarding.generated.resources.Res
import talkdraft.presentation.onboarding.generated.resources.ic_cloud_ai
import talkdraft.presentation.onboarding.generated.resources.ic_local_ai

@Composable
internal fun ProcessingMarkers(
    show: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = show,
        enter = expandVertically(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        ) + scaleIn(
            initialScale = 0.2f,
            transformOrigin = TransformOrigin(0.5f, 0f),
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        ),
        exit = shrinkHorizontally(
            animationSpec = tween(durationMillis = 60, easing = EaseOutBack),
        ) + scaleOut(
            targetScale = 0.2f,
            transformOrigin = TransformOrigin(0.5f, 0f),
            animationSpec = tween(durationMillis = 60, easing = EaseOutBack),
        ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_local_ai),
                    contentDescription = "Local Ai",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(100.dp),
                )
                Text(
                    text = "Local AI",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_cloud_ai),
                    contentDescription = "Local Ai",
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = "Cloud AI",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}
