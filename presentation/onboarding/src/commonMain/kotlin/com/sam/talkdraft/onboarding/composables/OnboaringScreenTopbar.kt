package com.sam.talkdraft.onboarding.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.RichTooltipColors
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.app_name
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OnBoardingScreenTopBar(
    onSkipFullTour: () -> Unit,
    onPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier,
    showPrevious: Boolean = false,
    showSkipTourButton: Boolean = false,
    scrollBehaviour: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(CommonResources.string.app_name),
                style = MaterialTheme.typography.titleLargeEmphasized,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        actions = {
            AnimatedTopAppbarButtons(
                show = showSkipTourButton,
                tooltipTitle = "Skip tour",
                tooltipText = "Skip this whole tour and get start with ",
            ) {
                Button(
                    onClick = onSkipFullTour,
                    enabled = showSkipTourButton,
                    shapes = ButtonDefaults.shapes(
                        shape = MaterialTheme.shapes.extraLarge,
                        pressedShape = ButtonDefaults.pressedShape,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                ) {
                    Text(
                        text = "Skip tour",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        fontSize = 14.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
        navigationIcon = {
            AnimatedTopAppbarButtons(
                show = showPrevious,
                tooltipTitle = "Previous",
                tooltipText = "Check the previous screen about what we offer",
                modifier = Modifier.offset(x = 2.dp),
            ) {
                OutlinedButton(
                    onClick = onPreviousScreen,
                    enabled = showPrevious,
                    shapes = ButtonDefaults.shapes(
                        shape = MaterialTheme.shapes.extraLarge,
                        pressedShape = ButtonDefaults.pressedShape,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                ) {
                    Text(
                        text = "Previous",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehaviour,
        colors = colors,
    )
}


@Composable
private fun AnimatedTopAppbarButtons(
    tooltipTitle: String,
    tooltipText: String,
    show: Boolean,
    modifier: Modifier = Modifier,
    tooltipColors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = show,
        enter = scaleIn(
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            initialScale = .4f,
        ) + fadeIn(
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
            initialAlpha = .2f,
        ),
        exit = shrinkOut(
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Center,
        ) + fadeOut(
            animationSpec = tween(durationMillis = 80, easing = EaseOut),
        ),
        modifier = modifier,
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Below,
                spacingBetweenTooltipAndAnchor = 4.dp,
            ),
            enableUserInput = false,
            tooltip = {
                RichTooltip(
                    title = { Text(text = tooltipTitle) },
                    text = { Text(text = tooltipText) },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = tooltipColors,
                )
            },

            state = rememberTooltipState(),
            content = content,
        )
    }
}
