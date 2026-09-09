package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designsystem.theme.spaceGrotesk
import kotlin.time.Duration

@Composable
internal fun RecorderTimerText(
    duration: () -> Duration,
    modifier: Modifier = Modifier,
    clockTextStyle: TextStyle = MaterialTheme.typography.displayMediumEmphasized,
    separatorStyle: TextStyle = MaterialTheme.typography.displaySmallEmphasized,
    separatorColor: Color = MaterialTheme.colorScheme.tertiary,
    hourColor: Color = MaterialTheme.colorScheme.onSurface,
    minuteColor: Color = MaterialTheme.colorScheme.secondary,
    secondsColor: Color = MaterialTheme.colorScheme.primary,
    fontFamily: FontFamily = spaceGrotesk(),
) {
    val tabularFeature = "tnum"
    val baseClockStyle = clockTextStyle.copy(fontFamily = fontFamily, fontFeatureSettings = tabularFeature)
    val baseSeparatorStyle = separatorStyle.copy(fontFamily = fontFamily, fontFeatureSettings = tabularFeature)

    val timeComponents by remember {
        derivedStateOf {
            duration().toComponents { hours, minutes, seconds, nanoseconds ->
                val hh = if (hours > 0) hours.toString().padStart(2, '0') else null
                val mm = minutes.toString().padStart(2, '0')
                val ss = seconds.toString().padStart(2, '0')
                val sf = (nanoseconds / 10_000_000).toString().padStart(2, '0')

                DurationComponents(
                    hours = hh,
                    minutes = mm,
                    seconds = ss,
                    subSeconds = sf,
                )
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            timeComponents.hours?.let { hh ->
                AnimatedDigitText(
                    text = hh,
                    style = baseClockStyle,
                    color = hourColor,
                )
                Text(
                    text = ":",
                    style = separatorStyle.copy(fontFamily = fontFamily),
                    color = separatorColor,
                    fontWeight = FontWeight.Black,
                )
            }

            AnimatedDigitText(
                text = timeComponents.minutes,
                style = baseClockStyle,
                color = minuteColor,
            )

            Text(
                text = ":",
                style = baseSeparatorStyle,
                color = separatorColor,
                fontWeight = FontWeight.Black,
            )

            Text(
                text = timeComponents.seconds,
                style = baseClockStyle,
                color = secondsColor,
            )

            Text(
                text = ".",
                style = baseSeparatorStyle,
                color = separatorColor,
                fontWeight = FontWeight.Black,
            )

            if (timeComponents.hours == null) {
                Text(
                    text = timeComponents.subSeconds,
                    style = baseClockStyle,
                    color = secondsColor,
                )
            }
        }
    }
}

@Composable
private fun AnimatedDigitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                (slideOutVertically { height -> height } + fadeOut()) using
                SizeTransform(clip = false)
        },
        label = "digit_number_animation",
        modifier = modifier,
    ) { targetText ->
        Text(
            text = targetText,
            style = style,
            color = color,
        )
    }
}

private data class DurationComponents(
    val hours: String?,
    val minutes: String,
    val seconds: String,
    val subSeconds: String,
)
