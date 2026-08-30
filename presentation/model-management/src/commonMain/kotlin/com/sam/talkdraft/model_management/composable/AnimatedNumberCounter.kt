package com.sam.talkdraft.model_management.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun AnimatedNumberCounter(
    number: () -> Int,
    range: ClosedRange<Int> = 0..100,
    modifier: Modifier = Modifier,
    spec: AnimationSpec<Float> = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
) {
    require(range.endInclusive < 1_000) { "Number too large" }

    val lifecycle = LocalLifecycleOwner.current

    val textMeasurer = rememberTextMeasurer(cacheSize = 100)
    val currentNumberLambda by rememberUpdatedState(number)

    val percentageStyle = MaterialTheme.typography.titleMedium
    val percentageColor = MaterialTheme.colorScheme.secondary

    val numberStyle = MaterialTheme.typography.headlineLarge
    val numberColor = MaterialTheme.colorScheme.onSurface

    val hundredStyle = MaterialTheme.typography.displayLarge
    val hundredColor = MaterialTheme.colorScheme.primary

    val hundredsAnim = remember { Animatable(0f) }
    val tensAnim = remember { Animatable(0f) }
    val onesAnim = remember { Animatable(0f) }

    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snapshotFlow { currentNumberLambda().coerceIn(range) }
                .collect { value ->

                    val hundreds = value / 100
                    val tens = (value % 100) / 10
                    val ones = value % 10

                    coroutineScope {
                        launch { hundredsAnim.animateTo(targetValue = hundreds.toFloat(), animationSpec = spec) }
                        launch { tensAnim.animateTo(targetValue = tens.toFloat(), animationSpec = spec) }
                        launch { onesAnim.animateTo(targetValue = ones.toFloat(), animationSpec = spec) }
                    }
                }
        }
    }

    Spacer(
        modifier = modifier
            .defaultMinSize(minWidth = 120.dp, minHeight = 80.dp)
            .drawWithCache {

                val hundredDigitLayouts = (0..9).map {
                    textMeasurer.measure(
                        text = "$it",
                        style = hundredStyle.copy(color = hundredColor),
                    )
                }

                val numberDigitLayouts = (0..9).map {
                    textMeasurer.measure(
                        text = "$it",
                        style = numberStyle.copy(color = numberColor, fontStyle = FontStyle.Italic),
                    )
                }

                val percentLayout = textMeasurer.measure(
                    text = "%",
                    style = percentageStyle.copy(color = percentageColor),
                )


                val hundredDigitWidth = hundredDigitLayouts.maxOf { it.size.width }.toFloat()
                val numberDigitWidth = numberDigitLayouts.maxOf { it.size.width }.toFloat()

                val space = 2.dp.toPx()
                val hundredsGap = 4.dp.toPx()

                val widthOfBlock = hundredDigitWidth + hundredsGap +
                    (2f * numberDigitWidth) +
                    space +
                    percentLayout.size.width


                val maxAscent = maxOf(
                    hundredDigitLayouts.maxOf { it.firstBaseline },
                    numberDigitLayouts.maxOf { it.firstBaseline },
                    percentLayout.firstBaseline,
                )

                val maxDescent = maxOf(
                    hundredDigitLayouts.maxOf { it.size.height - it.firstBaseline },
                    numberDigitLayouts.maxOf { it.size.height - it.firstBaseline },
                    percentLayout.size.height - percentLayout.firstBaseline,
                )

                val blockHeight = maxAscent + maxDescent
                val offXStart = (size.width - widthOfBlock) / 2f
                val baseline = (size.height - blockHeight) / 2f + maxAscent

                onDrawBehind {

                    val valHundreds = hundredsAnim.value
                    val valTens = tensAnim.value
                    val valOnes = onesAnim.value

                    translate(left = offXStart, top = 0f) {

                        drawFadingScalingDigitColumn(
                            value = valHundreds,
                            digitWidth = hundredDigitWidth,
                            baseline = baseline,
                            xOffset = 0f,
                            digitLayouts = hundredDigitLayouts,
                        )

                        drawFadingScalingDigitColumn(
                            value = valTens,
                            digitWidth = numberDigitWidth,
                            baseline = baseline,
                            xOffset = hundredDigitWidth + hundredsGap,
                            digitLayouts = numberDigitLayouts,
                        )

                        drawFadingScalingDigitColumn(
                            value = valOnes,
                            digitWidth = numberDigitWidth,
                            baseline = baseline,
                            xOffset =
                                hundredDigitWidth +
                                    hundredsGap +
                                    numberDigitWidth,
                            digitLayouts = numberDigitLayouts,
                        )

                        val percentX = hundredDigitWidth + hundredsGap +
                            (2f * numberDigitWidth) + space

                        val percentY = baseline - percentLayout.firstBaseline

                        drawText(
                            textLayoutResult = percentLayout,
                            topLeft = Offset(x = percentX, y = percentY),
                        )
                    }
                }
            },
    )
}


private fun DrawScope.drawFadingScalingDigitColumn(
    value: Float,
    digitWidth: Float,
    baseline: Float,
    xOffset: Float,
    digitLayouts: List<TextLayoutResult>,
) {
    if (digitLayouts.isEmpty()) return

    val baselineStep =
        digitLayouts[0].size.height.toFloat()

    for (i in 0..9) {
        val distanceFromCurrent = i - value
        if (distanceFromCurrent > -1.5f && distanceFromCurrent < 1.5f) {

            val fadeDistance = abs(distanceFromCurrent).coerceIn(0f, 1f)
            val scale = 1f - (fadeDistance * 0.6f)
            val alpha = 1f - fadeDistance
            val layout = digitLayouts[i]
            val digitBaseline = baseline + (distanceFromCurrent * baselineStep)
            val topY = digitBaseline - layout.firstBaseline
            val pivotX = xOffset + (digitWidth / 2f)

            withTransform(
                transformBlock = {
                    scale(scaleX = scale, scaleY = scale, pivot = Offset(x = pivotX, y = digitBaseline))
                },
            ) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x = xOffset, y = topY),
                    alpha = alpha,
                )
            }
        }
    }
}
