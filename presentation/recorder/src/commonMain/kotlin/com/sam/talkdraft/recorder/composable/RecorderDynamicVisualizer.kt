package com.sam.talkdraft.recorder.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer

@Composable
internal fun RecorderDynamicVisualizer(
    audioWaveForm: () -> ReadOnlyFloatBuffer,
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    Spacer(
        modifier = modifier.sizeIn(minHeight = 40.dp, minWidth = 280.dp)
            .drawWithCache {

                val barSpacingPx = 4.dp.toPx()
                val cornerRadiusPx = 20.dp.toPx()
                val minRatio = .12f

                onDrawBehind {
                    val pointsBuffer = audioWaveForm()
                    val sampleCount = pointsBuffer.size

                    val areAllZero = pointsBuffer.toArray().all { it == .0f }
                    if (areAllZero) return@onDrawBehind

                    if (sampleCount <= 0) return@onDrawBehind

                    val stepPx = size.width / sampleCount
                    val barWidthPx = (stepPx - barSpacingPx).coerceAtLeast(1f)

                    for (index in 0 until sampleCount) {
                        val amplitude = pointsBuffer[index].coerceIn(0f, 1f)
                        val heightRatio = minRatio + (1f - minRatio) * amplitude
                        val barHeight = size.height * heightRatio
                        val x = index * stepPx + barSpacingPx / 2f
                        val y = (size.height - barHeight) / 2f

                        drawRoundRect(
                            color = waveColor,
                            topLeft = Offset(x, y),
                            size = Size(width = barWidthPx, height = barHeight),
                            cornerRadius = CornerRadius(x = cornerRadiusPx, y = cornerRadiusPx),
                        )
                    }
                }
            },
    )

}
