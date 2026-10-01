package com.sam.talkdraft.model_management.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.model_management.generated.resources.Res
import talkdraft.presentation.model_management.generated.resources.ic_cancel
import talkdraft.presentation.model_management.generated.resources.ic_download
import talkdraft.presentation.model_management.generated.resources.ic_filled_checked
import talkdraft.presentation.model_management.generated.resources.ic_offline


@Composable
internal fun ModelDownloadActions(
    isDownloading: Boolean,
    isModelAlreadyPresent: Boolean,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckModel: () -> Unit = {},
    isUserOffline: Boolean = false,
) {
    val motionScheme = MaterialTheme.motionScheme

    val downloadState by remember(isDownloading, isModelAlreadyPresent) {
        derivedStateOf {
            when {
                isModelAlreadyPresent -> ModelDownloadState.ModelPresent
                isDownloading -> ModelDownloadState.Downloading
                else -> ModelDownloadState.Downloadable
            }
        }
    }

    AnimatedContent(
        targetState = downloadState,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val enterTransition = fadeIn(
                animationSpec = motionScheme.fastSpatialSpec(),
            ) + scaleIn(
                initialScale = 0.85f,
                animationSpec = motionScheme.fastSpatialSpec(),
            )

            val exitTransition = fadeOut(
                animationSpec = motionScheme.fastEffectsSpec(),
            ) + scaleOut(
                targetScale = 0.85f,
                animationSpec = motionScheme.fastEffectsSpec(),
            )

            enterTransition togetherWith exitTransition using (SizeTransform(
                clip = false,
                sizeAnimationSpec = { _, _ -> motionScheme.defaultEffectsSpec() },
            ))
        },
        modifier = modifier,
        label = "DownloadActionsTransition",
    ) { state ->
        when (state) {
            ModelDownloadState.Downloadable -> {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        positioning = TooltipAnchorPosition.Above,
                        spacingBetweenTooltipAndAnchor = 4.dp,
                    ),
                    enableUserInput = !isUserOffline,
                    tooltip = {
                        RichTooltip(
                            title = { Text(text = "Download Model") },
                            text = { Text(text = "Download model to enable offline transcription") },
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = TooltipDefaults.richTooltipColors(),
                        )
                    },
                    state = rememberTooltipState(),
                ) {
                    Button(
                        onClick = onStartDownload,
                        enabled = !isUserOffline,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        shapes = ButtonDefaults.shapes(
                            shape = MaterialTheme.shapes.extraLarge,
                            pressedShape = MaterialTheme.shapes.large,
                        ),
                        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight, true),
                        modifier = Modifier.height(ButtonDefaults.MediumContainerHeight).fillMaxWidth(),
                    ) {
                        AnimatedContent(isUserOffline, label = "DownloadIconTransition") { isOffline ->
                            if (isOffline) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_offline),
                                    contentDescription = "User offline",
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_download),
                                    contentDescription = "Start download",
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                        Text(
                            text = if (isUserOffline) "Offline" else "Start Download",
                            fontWeight = FontWeight.Bold,
                            style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                        )
                    }
                }
            }

            ModelDownloadState.Downloading -> {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        positioning = TooltipAnchorPosition.Above,
                        spacingBetweenTooltipAndAnchor = 4.dp,
                    ),
                    enableUserInput = true,
                    tooltip = {
                        RichTooltip(
                            title = { Text(text = "Cancel Download") },
                            text = { Text(text = "Stop downloading model") },
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = TooltipDefaults.richTooltipColors(titleContentColor = MaterialTheme.colorScheme.error),
                        )
                    },
                    state = rememberTooltipState(),
                ) {
                    Button(
                        onClick = onCancelDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        shapes = ButtonDefaults.shapes(
                            shape = MaterialTheme.shapes.extraLarge,
                            pressedShape = MaterialTheme.shapes.large,
                        ),
                        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight, true),
                        modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight).fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_cancel),
                            contentDescription = "Cancel download",
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)),
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                        Text(
                            text = "Cancel Download",
                            fontWeight = FontWeight.Bold,
                            style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                        )
                    }
                }
            }

            ModelDownloadState.ModelPresent -> {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        positioning = TooltipAnchorPosition.Above,
                        spacingBetweenTooltipAndAnchor = 4.dp,
                    ),
                    enableUserInput = true,
                    tooltip = {
                        RichTooltip(
                            title = { Text(text = "Model Ready") },
                            text = { Text(text = "Model is installed and ready to use") },
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = TooltipDefaults.richTooltipColors(),
                        )
                    },
                    state = rememberTooltipState(),
                ) {
                    Button(
                        onClick = onCheckModel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shapes = ButtonDefaults.shapes(
                            shape = MaterialTheme.shapes.extraLarge,
                            pressedShape = MaterialTheme.shapes.large,
                        ),
                        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight, true),
                        modifier = Modifier.height(ButtonDefaults.MediumContainerHeight).fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_filled_checked),
                            contentDescription = "Model downloaded",
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)),
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                        Text(
                            text = "Ready to Use",
                            fontWeight = FontWeight.Bold,
                            style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                        )
                    }
                }
            }
        }
    }
}

enum class ModelDownloadState {
    Downloadable, Downloading, ModelPresent,
}
