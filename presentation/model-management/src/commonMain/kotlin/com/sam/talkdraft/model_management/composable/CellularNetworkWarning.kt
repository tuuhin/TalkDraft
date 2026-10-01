package com.sam.talkdraft.model_management.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInElastic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.model_management.generated.resources.Res
import talkdraft.presentation.model_management.generated.resources.ic_cellular

@Composable
internal fun CellularNetworkWarning(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 200, easing = EaseInElastic),
        ) + expandVertically(
            expandFrom = Alignment.CenterVertically,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        ),
        exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = EaseOutExpo)) + shrinkVertically(
            shrinkTowards = Alignment.CenterVertically,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        ),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = MaterialTheme.shapes.largeIncreased,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = MaterialShapes.Slanted.toShape(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_cellular),
                        contentDescription = "Cellular Connectivity",
                        tint = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text(
                    text = "Device connected to Cellular network download may take some time.",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
