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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RichTooltip
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
import com.sam.talkdraft.designs.ic_app_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OnBoardingScreenTopBar(
    onSkipFullTour: () -> Unit,
    onPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier,
    showPrevious: Boolean = false,
    scrollBehaviour: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(CommonResources.drawable.ic_app_logo),
                    contentDescription = "App logo",
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(CommonResources.string.app_name),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Below,
                    spacingBetweenTooltipAndAnchor = 4.dp,
                ),
                tooltip = {
                    RichTooltip(
                        title = { Text("Skip tour") },
                        text = { Text(text = "Skip this whole tour and get start with ") },
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                },
                state = rememberTooltipState(),
            ) {
                Button(
                    onClick = onSkipFullTour,
                    shapes = ButtonDefaults.shapes(
                        shape = MaterialTheme.shapes.extraLarge,
                        pressedShape = ButtonDefaults.pressedShape,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                ) {
                    Text(text = "Skip tour", style = MaterialTheme.typography.titleMediumEmphasized, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
        navigationIcon = {
            AnimatedVisibility(
                showPrevious,
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
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        positioning = TooltipAnchorPosition.Below,
                        spacingBetweenTooltipAndAnchor = 4.dp,
                    ),
                    tooltip = {
                        RichTooltip(
                            title = { Text("Previous") },
                            text = { Text(text = "Check the previous screen about what we offer") },
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                    },
                    state = rememberTooltipState(),
                ) {
                    OutlinedButton(
                        onClick = onPreviousScreen,
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
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehaviour,
        colors = colors,
    )
}
