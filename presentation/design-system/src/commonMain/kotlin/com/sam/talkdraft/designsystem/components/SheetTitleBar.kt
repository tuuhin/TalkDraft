package com.sam.talkdraft.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.ic_cancel
import org.jetbrains.compose.resources.painterResource

@Composable
fun SheetTitleBar(
    title: String,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmallEmphasized,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = titleStyle,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            modifier = Modifier.weight(1f),
        )
        FilledIconButton(
            onClick = onDismissSheet,
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
                painter = painterResource(CommonResources.drawable.ic_cancel),
                contentDescription = "Cancel Action",
            )
        }
    }
}
