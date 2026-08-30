package com.sam.talkdraft.onboarding.composables.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.talkdraft.onboarding.composables.OnboardingContextAction

@Composable
internal fun HowDoesItWorkContainer(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.displaySmallEmphasized,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Column(
        modifier = modifier
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            CompositionLocalProvider(
                LocalTextStyle provides titleStyle.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Turn ")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append("thoughts")
                        }
                    },
                )
                Text(text = "into")
                Text(
                    text = buildAnnotatedString {
                        append("something ")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append("clear")
                        }
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Speak naturally. Your words become text as you think",
            style = MaterialTheme.typography.bodyLargeEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OnboardingContextAction(title = "Continue", onClick = onAction)
    }
}

