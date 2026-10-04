package com.sam.talkdraft.recorder.composable

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.talkdraft.designsystem.theme.googleSansFlexFont
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.recorder.generated.resources.Res
import talkdraft.presentation.recorder.generated.resources.ic_transcription_completed

@Composable
internal fun CapturedTranscriptionContent(
    transcriptionText: String?,
    modifier: Modifier = Modifier,
) {
    val quoteFontFamily = googleSansFlexFont(
        settings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.grade(5),
            FontVariation.slant(-10f),
            FontVariation.width(125f),
            FontVariation.Setting("ROUD", 500f),
        ),
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_transcription_completed),
            contentDescription = "Transcription completed",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = if (transcriptionText != null) "Your thought is captured" else "Failed to capture your voice",
            style = MaterialTheme.typography.titleMediumEmphasized,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Crossfade(
            targetState = transcriptionText != null,
            modifier = Modifier.heightIn(min = 80.dp),
        ) { isPresent ->
            if (isPresent && transcriptionText != null)
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontFamily = quoteFontFamily,
                                fontSize = 40.sp,
                                baselineShift = BaselineShift(-0.3f),
                            ),
                        ) {
                            append("\u201C")
                        }

                        append(" $transcriptionText ")
                        withStyle(
                            style = SpanStyle(
                                fontFamily = quoteFontFamily,
                                fontSize = 40.sp,
                                baselineShift = BaselineShift(-2.3f),
                            ),
                        ) {
                            append("\u201D")
                        }
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            else Text(
                text = "Sorry can you try again, unable to read your voice", textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
