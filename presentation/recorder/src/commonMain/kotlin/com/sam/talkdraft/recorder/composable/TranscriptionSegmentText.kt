package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designsystem.annotations.DarkThemedPreview
import com.sam.talkdraft.designsystem.annotations.LightThemedPreview
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel

@Composable
internal fun TranscriptionSegmentText(
    segmentModel: TranscriptionSegmentModel?,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default,
) {
    val whitespaceRegex = remember { Regex("\\s+") }

    val words = segmentModel?.text?.trim()?.split(whitespaceRegex)
        ?.filterNot { it.isBlank() }
        ?: emptyList()

    val segmentId by remember(segmentModel) {
        derivedStateOf { segmentModel?.segmentId ?: -1 }
    }

    LookaheadScope {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Bottom,
            maxItemsInEachRow = 8,
        ) {
            words.forEachIndexed { index, word ->
                key(segmentId, index) {
                    val isLatestWord = index == words.lastIndex

                    val color by animateColorAsState(
                        targetValue = if (isLatestWord) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                        label = "wordColor",
                    )

                    BasicText(
                        text = word,
                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = { color },
                        modifier = Modifier.animateBounds(
                            lookaheadScope = this@LookaheadScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessHigh,
                                )
                            },
                        ).padding(horizontal = 2.dp),
                    )
                }
            }
        }
    }
}

@LightThemedPreview
@DarkThemedPreview
@Composable
fun TranscriptionSegmentTextPreview() = Surface {
    TranscriptionSegmentText(
        segmentModel = TranscriptionSegmentModel(
            -1L,
            "Some segment line",
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
