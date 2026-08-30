package com.sam.talkdraft.model_management

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designsystem.components.SheetTitleBar
import com.sam.talkdraft.model_management.composable.ModelDownloadActions
import com.sam.talkdraft.model_management.composable.ModelDownloadStatusContainer
import com.sam.talkdraft.model_management.composable.RecommendModelCard
import com.sam.talkdraft.model_management.events.SelectedModelScreenEvent
import com.sam.talkdraft.model_management.model.SelectedModelScreenState

@Composable
internal fun RecommendedModelDownloadSheet(
    onDismiss: () -> Unit,
    onAction: (SelectedModelScreenEvent) -> Unit,
    state: SelectedModelScreenState = SelectedModelScreenState(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SheetTitleBar(
            title = "Recommended Model",
            onDismissSheet = onDismiss,
        )
        AnimatedContent(
            targetState = state.model != null && state.isModelLoaded,
            transitionSpec = {
                fadeIn(animationSpec = tween(150, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(150)) togetherWith
                    fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing)) using
                    SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ -> tween(durationMillis = 200, easing = FastOutSlowInEasing) },
                    )
            },
            label = "recommended_model_found_suggestions",
            modifier = Modifier.fillMaxWidth(),
        ) { isReady ->
            if (isReady && state.model != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = 200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ModelDownloadStatusContainer(
                            status = state.model.modelStatus,
                            uiDownloadStatus = state.downloadStatus,
                        )
                    }
                    RecommendModelCard(model = state.model)
                }
            } else Box(
                modifier = Modifier.heightIn(120.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = "Finding the best model that's fits your device",
                        style = MaterialTheme.typography.bodySmallEmphasized,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        // some content
        Spacer(modifier = Modifier.size(12.dp))
        AnimatedVisibility(
            visible = !state.isModelPresent,
            enter = slideInVertically(),
            exit = slideOutVertically(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ModelDownloadActions(
                isDownloadEnabled = !state.isModelSetupRunning,
                isCancelEnabled = state.isModelSetupRunning,
                onCancelDownload = { onAction(SelectedModelScreenEvent.CancelDownload) },
                onStartDownload = { onAction(SelectedModelScreenEvent.StartDownload) },
            )
        }
    }
}
