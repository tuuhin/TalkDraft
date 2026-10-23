package com.sam.talkdraft.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.ic_back_arrow
import org.jetbrains.compose.resources.painterResource

@Composable
fun NavBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip {
                Text(text = "Back Arrow")
            }
        },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = iconColor,
                contentColor = contentColorFor(iconColor),
            ),
            modifier = Modifier.minimumInteractiveComponentSize()
                .size(IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
            shapes = IconButtonDefaults.shapes(
                shape = IconButtonDefaults.smallRoundShape,
                pressedShape = IconButtonDefaults.smallPressedShape,
            ),
        ) {
            Icon(
                painter = painterResource(CommonResources.drawable.ic_back_arrow),
                contentDescription = "Back Arrow",
            )
        }
    }
}

