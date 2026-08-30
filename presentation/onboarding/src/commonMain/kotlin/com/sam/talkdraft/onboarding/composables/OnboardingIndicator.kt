package com.sam.talkdraft.onboarding.composables

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
internal fun OnboardingIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
    onChangePage: (Int) -> Unit,
    tapToChangePageEnable: Boolean = false,
    trackColor: Color = MaterialTheme.colorScheme.secondary,
) {
    require(pageCount > 0) { "Cannot work with empty pages" }
    require(currentPage < pageCount) { "Current page cannot exceed total pages" }

    val currentOnChangePage by rememberUpdatedState(onChangePage)

    val animatedProgress by animateFloatAsState(
        targetValue = currentPage.toFloat(),
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "page_indicator_progress",
    )

    Spacer(
        modifier = modifier
            .defaultMinSize(minWidth = 320.dp, minHeight = 12.dp)
            .pointerInput(tapToChangePageEnable) {
                val basicX = size.width.toFloat() / pageCount
                detectTapGestures { tapPos ->
                    if (!tapToChangePageEnable) return@detectTapGestures
                    repeat(pageCount) { page ->
                        val startOffset = basicX * page
                        val endOffset = basicX * (page + 1)
                        if (tapPos.x in startOffset..endOffset)
                            currentOnChangePage(page)
                    }
                }
            }
            .drawWithCache {

                val thickness = minOf(size.height, 8.dp.toPx())
                val pathSpace = 6.dp.toPx()
                val gapCount = maxOf(0, pageCount - 1)

                val yOffset = (size.height - thickness) / 2f
                val halfThickness = thickness / 2f
                val totalGapWidth = gapCount * pathSpace
                val segmentWidth = (size.width - totalGapWidth) / pageCount

                val cornerRadius = CornerRadius(halfThickness, halfThickness)

                onDrawBehind {
                    repeat(pageCount) { idx ->
                        drawRoundRect(
                            color = trackColor.copy(alpha = 0.2f),
                            topLeft = Offset((segmentWidth + pathSpace) * idx, yOffset),
                            size = Size(segmentWidth, thickness),
                            cornerRadius = cornerRadius,
                        )
                    }

                    repeat(pageCount) { idx ->
                        val segmentProgress = (animatedProgress - idx + 1f).coerceIn(0f, 1f)
                        if (segmentProgress > 0f) {
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset((segmentWidth + pathSpace) * idx, yOffset),
                                size = Size(segmentWidth * segmentProgress, thickness),
                                cornerRadius = cornerRadius,
                            )
                        }
                    }
                }
            },
    )
}
