package com.sam.talkdraft.model_management.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designsystem.utils.LocalBytesConvertor
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel

@Composable
internal fun RecommendModelCard(
    model: TranscriptionModel,
    modifier: Modifier = Modifier,
) {
    val formatter = LocalBytesConvertor.current

    val modelSize = remember(formatter) { formatter.formatToString(model.sizeInBytes) }
    val supportedLocales = remember(model) {
        val containAsterisks = model.supportedLanguages.contains("*")
        if (containAsterisks) listOf("English")
        else model.supportedLanguages.filter { it != "*" }
    }


    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InfoBadge(text = modelSize, color = MaterialTheme.colorScheme.secondary)
        Text(
            text = model.displayName,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "Fast, lightweight on-device transcription.",
            style = MaterialTheme.typography.bodyLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            supportedLocales.forEachIndexed { idx, locale ->
                Text(
                    text = locale,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                )
                if (idx + 1 != supportedLocales.size) {
                    Box(
                        modifier = Modifier.size(12.dp)
                            .clip(MaterialShapes.Flower.toShape())
                            .background(color = MaterialTheme.colorScheme.primary),
                    )
                }

            }
        }
    }
}


@Composable
private fun InfoBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    shape: Shape = MaterialTheme.shapes.extraLarge,
) {
    Surface(
        shape = shape,
        color = color,
        contentColor = contentColorFor(color),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMediumEmphasized,
            )
        }
    }
}
