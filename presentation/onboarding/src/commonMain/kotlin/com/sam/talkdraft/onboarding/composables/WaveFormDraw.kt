package com.sam.talkdraft.onboarding.composables

import androidx.compose.animation.core.EaseInBounce
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.sam.talkdraft.onboarding.models.OnboardingScene
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
internal fun WaveFormDraw(
    screen: OnboardingScene,
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary,
) {
    val numberOfPoints = 200

    val randomSet = remember {
        ShortArray(numberOfPoints) { idx ->
            if (idx < 10) Random.nextInt(1, 5).toShort()
            else Random.nextInt(1, 10).toShort()
        }
    }

    val patternSet = remember {
        FloatArray(numberOfPoints) { idx ->
            val normalizedX = idx.toFloat() / numberOfPoints
            val envelope = sin(normalizedX * PI.toFloat())
            val speechPulse = sin(normalizedX * 8 * PI.toFloat()) * 3f + 5f
            (envelope * speechPulse).coerceIn(1f, 10f)
        }
    }

    val baseTransition = updateTransition(targetState = screen, label = "OnboardingTransition")

    val waveFormTranslateFraction by baseTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 240, easing = EaseInBounce)
        },
        label = "WaveFormTranslate",
    ) { scene ->
        if (scene == OnboardingScene.WELCOME_SCREEN) 0.4f else 0f
    }

    val blurRadius by baseTransition.animateDp(
        transitionSpec = { tween(durationMillis = 350) },
        label = "WaveFormBlur",
    ) { targetScene ->
        if (targetScene == OnboardingScene.WHAT_WILL_YOU_CAPTURE_SCREEN) 8.dp
        else if (baseTransition.currentState != targetScene) 14.dp else 0.dp
    }

    val scaleEffect by baseTransition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "WaveFormScale",
    ) { targetScene ->
        if (baseTransition.currentState != targetScene) 0.15f else 0f
    }

    val morphProgress by baseTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "WaveFormMorph",
    ) { scene ->
        val targetIndex = OnboardingScene.FIRST_RECORDING_SCREEN.index.toFloat()
        (scene.index / targetIndex).coerceIn(0f, 1f)
    }

    val pointsMultiplierFraction by baseTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
        label = "WaveFormPointCount",
    ) { scene ->
        val progress = (scene.index / OnboardingScene.FIRST_TRANSCRIPT_SCREEN.index.toFloat()).coerceIn(0f, 1f)
        lerp(1.0f, 0.4f, progress)
    }

    val waveStartOpacity by baseTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
        label = "wave_form_start_opacity",
    ) { scene ->
        if (scene == OnboardingScene.WELCOME_SCREEN) 0.3f
        else .1f
    }


    Spacer(
        modifier = modifier
            .blur(blurRadius)
            .graphicsLayer {
                scaleY = 1 + scaleEffect
                scaleX = 1 + scaleEffect
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .drawWithCache {

                val maxNoOfPoints = (numberOfPoints * 0.8f).fastRoundToInt()
                val maxRandomSize = randomSet.max()
                val randomMultiplier = size.height * 0.08f / maxRandomSize
                val waveYAxisAnchor = .35f
                val waveWidth = size.width

                onDrawBehind {

                    val yAxisAnchor = size.height * waveYAxisAnchor
                    val currentStartOffset = size.width * waveFormTranslateFraction
                    val currentNoOfPoints = (maxNoOfPoints * pointsMultiplierFraction).fastRoundToInt()
                    val widthToWorkWith = waveWidth / (1.35f * currentNoOfPoints + 1)
                    val cornerRadius = CornerRadius(widthToWorkWith * 0.5f, widthToWorkWith * 0.5f)

                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                waveColor.copy(alpha = waveStartOpacity),
                            ),
                        ),
                        start = Offset(0f, yAxisAnchor),
                        end = Offset(currentStartOffset, yAxisAnchor),
                        strokeWidth = widthToWorkWith,
                        cap = StrokeCap.Round,
                    )

                    translate(left = currentStartOffset) {
                        for (idx in 0 until currentNoOfPoints) {
                            if (idx % 2 == 0) continue

                            val initialHeight = randomSet[idx].toFloat()
                            val targetHeight = patternSet[idx]
                            val currentBarHeight = lerp(initialHeight, targetHeight, morphProgress) * randomMultiplier

                            val alpha =
                                waveStartOpacity + ((idx.toFloat() / (currentNoOfPoints * 2)) * (1 - waveStartOpacity))

                            drawRoundRect(
                                color = waveColor,
                                topLeft = Offset(
                                    x = widthToWorkWith * idx * 1.35f,
                                    y = yAxisAnchor - currentBarHeight,
                                ),
                                size = Size(widthToWorkWith, currentBarHeight * 2f),
                                cornerRadius = cornerRadius,
                                alpha = alpha,
                            )
                        }
                    }
                }
            },
    )
}
