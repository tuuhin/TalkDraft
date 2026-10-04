package com.sam.talkdraft.recorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.designsystem.annotations.LightThemedPreview
import com.sam.talkdraft.designsystem.components.SheetTitleBar
import com.sam.talkdraft.designsystem.utils.Dimensions
import com.sam.talkdraft.recorder.composable.CapturedTranscriptionContent
import com.sam.talkdraft.recorder.composable.RealtimeTranscriptionText
import com.sam.talkdraft.recorder.composable.RecorderDynamicVisualizer
import com.sam.talkdraft.recorder.composable.RecorderSheetActions
import com.sam.talkdraft.recorder.composable.RecorderSheetContent
import com.sam.talkdraft.recorder.composable.RecorderTimerText
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.events.RecordingScreenEvent
import com.sam.talkdraft.recorder.model.RecorderSheetState
import com.sam.talkdraft.recorder.model.RecorderUIState
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun SimpleRecorderSheet(
    recordingDuration: () -> Duration,
    audioWaveForm: () -> ReadOnlyFloatBuffer,
    screenState: RecorderSheetState,
    onAction: (RecordingScreenEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetTitleBar(
            title = "Capture your thoughts",
            onDismissSheet = onDismiss,
        )
        Spacer(modifier = Modifier.height(20.dp))
        RecorderSheetContent(
            state = screenState,
            modifier = Modifier.heightIn(min = Dimensions.MODAL_SHEET_MIN_HEIGHT),
            recordingUI = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RecorderTimerText(
                        duration = recordingDuration,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    RecorderDynamicVisualizer(
                        audioWaveForm = audioWaveForm,
                        modifier = Modifier.widthIn(max = 380.dp)
                            .fillMaxWidth(.8f)
                            .height(100.dp),
                    )
                    // transcription text
                    RealtimeTranscriptionText(
                        state = screenState.transcriptionResult,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            completedUI = {
                CapturedTranscriptionContent(
                    transcriptionText = screenState.finalizedTranscriptionText,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
        // actions will be updated later
        RecorderSheetActions(
            isRecording = screenState.recorderState == RecorderState.RECORDING,
            isRecordingCompleted = screenState.recorderState == RecorderState.COMPLETED,
            isSavingRecording = screenState.isSavingRecording && screenState.finalizedTranscriptionText != null,
            isAllButtonDisabled = screenState.isModelSetupRunning,
            onCancelRecording = { onAction(RecordingScreenEvent.OnCancelRecording) },
            onSave = { onAction(RecordingScreenEvent.OnSaveTranscription) },
            onResetRecording = { onAction(RecordingScreenEvent.OnResetRecording) },
            onRecording = { onAction(RecordingScreenEvent.StartRecording) },
            onStopRecording = { onAction(RecordingScreenEvent.StopRecording) },
            modifier = Modifier.fillMaxWidth(.85f),
        )
    }
}


private class SimpleRecorderPreviewScreenState : PreviewParameterProvider<RecorderSheetState> {
    override val values: Sequence<RecorderSheetState>
        get() = sequenceOf(
            RecorderSheetState(
                state = RecorderUIState(
                    recorderState = RecorderState.RECORDING,
                    transcriptions = TranscriptionResult.Success(
                        TranscriptionSegmentModel(segmentId = 1, "Hello how are you doing"),
                    ),
                ),
            ),
            RecorderSheetState(
                state = RecorderUIState(
                    recorderState = RecorderState.IDLE,
                    transcriptions = TranscriptionResult.Idle,
                ),
            ),
            RecorderSheetState(
                state = RecorderUIState(
                    recorderState = RecorderState.IDLE,
                    transcriptions = TranscriptionResult.Idle,
                ),
                isModelSetupRunning = true,
            ),
            RecorderSheetState(
                finalizedTranscriptionText = "Hello how are you doing these days".repeat(3),
                state = RecorderUIState(
                    recorderState = RecorderState.COMPLETED,
                    transcriptions = TranscriptionResult.Success(
                        TranscriptionSegmentModel(segmentId = 1, "Hello how are you doing"),
                    ),
                ),
            ),
            RecorderSheetState(
                finalizedTranscriptionText = null,
                state = RecorderUIState(
                    recorderState = RecorderState.COMPLETED,
                    transcriptions = TranscriptionResult.Success(
                        TranscriptionSegmentModel(segmentId = 1, "Hello how are you doing"),
                    ),
                ),
            ),
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@LightThemedPreview
//@DarkThemedPreview
@Composable
private fun SimpleRecorderSheetPreview(
    @PreviewParameter(SimpleRecorderPreviewScreenState::class)
    state: RecorderSheetState,
) = Surface(
    shape = BottomSheetDefaults.ExpandedShape,
    color = BottomSheetDefaults.ContainerColor,
) {
    SimpleRecorderSheet(
        recordingDuration = { 12.seconds },
        audioWaveForm = {
            val floatArray = FloatArray(36) { Random.nextFloat() }
            ReadOnlyFloatBuffer.wrap(floatArray, floatArray.size)
        },
        screenState = state,
        onAction = {},
        onDismiss = {},
        modifier = Modifier.padding(Dimensions.MODAL_BOTTOM_SHEET_CONTENT_PADDING),
    )
}
