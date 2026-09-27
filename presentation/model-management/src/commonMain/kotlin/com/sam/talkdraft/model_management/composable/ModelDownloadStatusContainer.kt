package com.sam.talkdraft.model_management.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.ic_model_absent
import com.sam.talkdraft.designs.ic_model_present
import com.sam.talkdraft.model_management.model.UIModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ModelDownloadStatusContainer(
    status: ModelInstallStatus = ModelInstallStatus.INSTALLED,
    uiDownloadStatus: UIModelDownloadStatus = UIModelDownloadStatus.Idle,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    val indicatorProgress: () -> Float = remember(uiDownloadStatus) {
        when (uiDownloadStatus) {
            is UIModelDownloadStatus.Downloading -> uiDownloadStatus.progress
            else -> {
                { 0f }
            }
        }
    }

    val artWorkState = remember(status, uiDownloadStatus) {
        when (status) {
            ModelInstallStatus.NOT_INSTALLED -> DownloadArtWorkState.NotInstalledOrNotStarted
            ModelInstallStatus.INSTALLED -> DownloadArtWorkState.ModelInstalledAndReady
            ModelInstallStatus.DOWNLOADING -> when (uiDownloadStatus) {
                UIModelDownloadStatus.Idle -> DownloadArtWorkState.NotInstalledOrNotStarted
                is UIModelDownloadStatus.Downloading -> DownloadArtWorkState.InstallDownloadProgressive
                is UIModelDownloadStatus.Failed -> DownloadArtWorkState.ModelInstallationFailed(
                    uiDownloadStatus.message.ifBlank { "Failed to download model" },
                )

                UIModelDownloadStatus.Extracting -> DownloadArtWorkState.Extracting
                UIModelDownloadStatus.Starting -> DownloadArtWorkState.InstallStarted
                UIModelDownloadStatus.Success -> DownloadArtWorkState.ModelInstalledAndReady
                UIModelDownloadStatus.Verifying -> DownloadArtWorkState.InstallVerifying
            }
        }
    }

    AnimatedContent(
        targetState = artWorkState,
        modifier = modifier,
        transitionSpec = {
            scaleIn(
                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
                transformOrigin = TransformOrigin.Center,
                initialScale = 0.85f,
            ) + fadeIn(animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)) togetherWith
                scaleOut(
                    animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
                    transformOrigin = TransformOrigin.Center,
                    targetScale = 1.05f,
                ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 150,
                    easing = FastOutLinearInEasing,
                ),
            ) using SizeTransform(
                clip = false,
                sizeAnimationSpec = { _, _ ->
                    tween(durationMillis = 200, easing = FastOutSlowInEasing)
                },
            )
        },
        contentAlignment = Alignment.Center,
    ) { state ->
        when (state) {
            DownloadArtWorkState.NotInstalledOrNotStarted -> Image(
                painter = painterResource(CommonResources.drawable.ic_model_absent),
                contentDescription = "Model absent",
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
            )

            DownloadArtWorkState.ModelInstalledAndReady -> Image(
                painter = painterResource(CommonResources.drawable.ic_model_present),
                contentDescription = "Model Present",
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            )


            DownloadArtWorkState.InstallStarted -> {
                Text(
                    text = "Starting",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
            }

            DownloadArtWorkState.InstallVerifying -> {
                Text(
                    text = "Verifying",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
            }

            is DownloadArtWorkState.ModelInstallationFailed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(CommonResources.drawable.ic_model_absent),
                    contentDescription = "Model absent",
                    modifier = Modifier.size(120.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
                )
                Text(
                    text = state.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            DownloadArtWorkState.Extracting -> Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.matchParentSize(),
                    stroke = with(density) { Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round) },
                    trackStroke = with(density) { Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round) },
                    gapSize = 6.dp,
                    wavelength = 40.dp,
                    waveSpeed = 32.dp,
                )
                Text(text = "Extracting")
            }

            DownloadArtWorkState.InstallDownloadProgressive -> Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator(
                    progress = indicatorProgress,
                    modifier = Modifier.matchParentSize(),
                    stroke = with(density) { Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round) },
                    trackStroke = with(density) { Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round) },
                    gapSize = 6.dp,
                    amplitude = { with(density) { 3.dp.toPx() } },
                    wavelength = 40.dp,
                    waveSpeed = 32.dp,
                )
                AnimatedNumberCounter(
                    number = { (indicatorProgress() * 100).fastRoundToInt() },
                    range = 0..100,
                )
            }
        }
    }
}

private sealed class DownloadArtWorkState {
    data object NotInstalledOrNotStarted : DownloadArtWorkState()
    data object InstallStarted : DownloadArtWorkState()
    data object InstallDownloadProgressive : DownloadArtWorkState()
    data object InstallVerifying : DownloadArtWorkState()
    data class ModelInstallationFailed(val reason: String) : DownloadArtWorkState()
    data object ModelInstalledAndReady : DownloadArtWorkState()
    data object Extracting : DownloadArtWorkState()
}


