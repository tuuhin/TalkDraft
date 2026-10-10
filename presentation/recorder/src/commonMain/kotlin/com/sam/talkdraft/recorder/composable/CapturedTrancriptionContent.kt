package com.sam.talkdraft.recorder.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.talkdraft.designsystem.annotations.LightThemedPreview
import com.sam.talkdraft.designsystem.theme.googleSansFlexFont
import kotlin.random.Random
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.painterResource
import talkdraft.presentation.recorder.generated.resources.Res
import talkdraft.presentation.recorder.generated.resources.ic_transcription_completed

@Composable
internal fun CapturedTranscriptionContent(
    transcriptionText: ImmutableList<String>,
    modifier: Modifier = Modifier,
    maxItemToDisplay: Int = 5,
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

    val isResultEmpty by remember(transcriptionText) {
        derivedStateOf { transcriptionText.isEmpty() }
    }

    val wrappedResult by remember(transcriptionText, maxItemToDisplay) {
        derivedStateOf {
            val texts = if (transcriptionText.size > maxItemToDisplay) transcriptionText.take(maxItemToDisplay)
            else transcriptionText

            val random = Random.nextInt(5, 10)

            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontFamily = quoteFontFamily,
                        fontSize = 20.sp,
                        baselineShift = BaselineShift(-0.3f),
                    ),
                ) {
                    append("\u201C")
                }
                for ((idx, text) in texts.withIndex()) {
                    append(".".repeat(random))
                    append(text)
                    append(".".random())
                    if (idx + 1 == texts.size) append("......")
                    else append(".".random())
                }
                withStyle(
                    style = SpanStyle(
                        fontFamily = quoteFontFamily,
                        fontSize = 20.sp,
                        baselineShift = BaselineShift(-0.3f),
                    ),
                ) {
                    append("\u201D")
                }
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_transcription_completed),
            contentDescription = "Transcription completed",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        if (isResultEmpty) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = quoteFontFamily,
                            fontSize = 20.sp,
                            baselineShift = BaselineShift(-0.3f),
                        ),
                    ) {
                        append("\u201C")
                    }

                    append(" Sorry we failed to capture your voice ")
                    withStyle(
                        style = SpanStyle(
                            fontFamily = quoteFontFamily,
                            fontSize = 20.sp,
                            baselineShift = BaselineShift(-0.3f),
                        ),
                    ) {
                        append("\u201D")
                    }
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            return
        }
        Text(
            text = wrappedResult,
            maxLines = 8,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private class CapturedTranscriptionContentPreviewParams : PreviewParameterProvider<ImmutableList<String>> {
    override val values: Sequence<ImmutableList<String>>
        get() = sequence {
            yield(persistentListOf("Hello", "is this a text"))
            yield(persistentListOf())
            yield(List(20) { "Some tex t:$it" }.toPersistentList())
        }
}

@Composable
@LightThemedPreview
private fun CaptureTranscriptionContentPreview(
    @PreviewParameter(CapturedTranscriptionContentPreviewParams::class)
    results: ImmutableList<String>,
) = Surface {
    CapturedTranscriptionContent(transcriptionText = results, modifier = Modifier.fillMaxWidth())
}
