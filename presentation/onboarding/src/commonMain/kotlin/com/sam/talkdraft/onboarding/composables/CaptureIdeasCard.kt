package com.sam.talkdraft.onboarding.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.onboarding.models.CaptureIdeaOption

@Composable
internal fun CaptureIdeaCard(
    idea: CaptureIdeaOption,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {


    val colorAnimation by animateColorAsState(
        label = "container_color_animation",
        targetValue = if (isSelected) selectedContainerColor else selectedContainerColor.copy(
            alpha = .2f,
        ),
    )

    Surface(
        onClick = onSelect,
        modifier = modifier,
        shape = if (isSelected) MaterialTheme.shapes.extraExtraLarge else MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
        color = colorAnimation,
        contentColor = contentColorFor(selectedContainerColor),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = idea.title, style = MaterialTheme.typography.bodyLargeEmphasized)
                Text(
                    text = idea.text,
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    val enterAnimation = fadeIn() + scaleIn(initialScale = 0.4f)
                    val exitAnimation = fadeOut() + scaleOut(targetScale = 0.4f)
                    enterAnimation togetherWith exitAnimation
                },
                label = "fade_scale_transition",
            ) { isFilled ->
                if (isFilled) {
                    Icon(
                        painter = idea.painterFilled,
                        contentDescription = "Filled $idea",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        painter = idea.painterOutlined,
                        contentDescription = "Outlined :$idea",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
