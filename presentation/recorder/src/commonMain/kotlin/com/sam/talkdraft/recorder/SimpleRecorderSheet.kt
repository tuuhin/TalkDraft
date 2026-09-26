package com.sam.talkdraft.recorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.designsystem.components.SheetTitleBar
import com.sam.talkdraft.designsystem.utils.Dimensions
import com.sam.talkdraft.recorder.composable.RealtimeTranscriptionText
import com.sam.talkdraft.recorder.composable.RecorderDynamicVisualizer
import com.sam.talkdraft.recorder.composable.RecorderSheetActions
import com.sam.talkdraft.recorder.composable.RecorderSheetContent
import com.sam.talkdraft.recorder.composable.RecorderTimerText
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.events.RecordingScreenEvent
import com.sam.talkdraft.recorder.model.RecorderScreenState
import kotlin.time.Duration

@Composable
internal fun SimpleRecorderSheet(
    recordingDuration: () -> Duration,
    audioWaveForm: () -> ReadOnlyFloatBuffer,
    screenState: RecorderScreenState = RecorderScreenState(),
    onAction: (RecordingScreenEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val isRecording by remember(screenState) {
        derivedStateOf { screenState.recorderState == RecorderState.RECORDING }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SheetTitleBar(
            title = "Capture your thoughts",
            onDismissSheet = onDismiss,
        )
        RecorderSheetContent(
            state = screenState,
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = Dimensions.MODAL_SHEET_MIN_HEIGHT),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecorderTimerText(
                    duration = recordingDuration,
                    modifier = Modifier.fillMaxWidth(),
                )
                RecorderDynamicVisualizer(
                    audioWaveForm = audioWaveForm,
                    modifier = Modifier.widthIn(max = 380.dp)
                        .fillMaxWidth(.8f)
                        .height(120.dp),
                )
                // transcription text
                RealtimeTranscriptionText(
                    state = screenState.transcriptions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        RecorderSheetActions(
            isRecording = isRecording,
            onRecording = { onAction(RecordingScreenEvent.StartRecording) },
            onCancel = { onAction(RecordingScreenEvent.OnCancelRecording) },
            onStopRecording = { onAction(RecordingScreenEvent.StopRecording) },
            modifier = Modifier.align(Alignment.End),
        )
    }
}
