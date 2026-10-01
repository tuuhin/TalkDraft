package com.sam.talkdraft.model_management

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.connectivity.models.ConnectivityState
import com.sam.talkdraft.designsystem.annotations.DarkThemedPreview
import com.sam.talkdraft.designsystem.annotations.LightThemedPreview
import com.sam.talkdraft.designsystem.components.SheetTitleBar
import com.sam.talkdraft.designsystem.utils.Dimensions
import com.sam.talkdraft.model_management.composable.CellularNetworkWarning
import com.sam.talkdraft.model_management.composable.ModelDownloadActions
import com.sam.talkdraft.model_management.composable.ModelDownloadStatusContainer
import com.sam.talkdraft.model_management.composable.RecommendModelCard
import com.sam.talkdraft.model_management.events.SelectedModelScreenEvent
import com.sam.talkdraft.model_management.model.SelectedModelScreenState
import com.sam.talkdraft.model_management.model.UIModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus

@Composable
internal fun RecommendedModelDownloadSheet(
    onDismiss: () -> Unit,
    onAction: (SelectedModelScreenEvent) -> Unit,
    state: SelectedModelScreenState = SelectedModelScreenState(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetTitleBar(
            title = "Recommended Model",
            onDismissSheet = onDismiss,
        )
        Crossfade(
            targetState = state.model != null && state.isModelLoaded,
            label = "recommended_model_found_suggestions",
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = Dimensions.MODAL_SHEET_MIN_HEIGHT),
        ) { isReady ->
            if (isReady && state.model != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CellularNetworkWarning(
                        isVisible = state.networkState == ConnectivityState.CELLULAR && state.downloadStatus == UIModelDownloadStatus.Idle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ModelDownloadStatusContainer(
                            status = state.model.status,
                            uiDownloadStatus = state.downloadStatus,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    RecommendModelCard(model = state.model)
                }
            } else Box(
                modifier = Modifier.heightIn(120.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = "Finding the best model for your device",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = state.isModelLoaded,
            enter = slideInVertically(),
            exit = slideOutVertically(),
        ) {
            ModelDownloadActions(
                isDownloading = state.isModelSetupRunning,
                isModelAlreadyPresent = state.isModelPresent,
                isUserOffline = state.networkState == ConnectivityState.OFFLINE,
                onCancelDownload = { onAction(SelectedModelScreenEvent.CancelDownload) },
                onStartDownload = { onAction(SelectedModelScreenEvent.StartDownload) },
                modifier = Modifier.fillMaxWidth(.8f)
                    .padding(top = 12.dp),
            )
        }
    }
}

private class SelectedModelScreenStatePreviewProvider : PreviewParameterProvider<SelectedModelScreenState> {
    override val values: Sequence<SelectedModelScreenState>
        get() = sequenceOf(
            SelectedModelScreenState(
                model = null,
                downloadStatus = UIModelDownloadStatus.Idle,
                isModelLoaded = false,
                networkState = ConnectivityState.OFFLINE,
            ),

            SelectedModelScreenState(
                model = PreviewFakes.FAKE_STREAMING_MODEL.copy(status = ModelInstallStatus.NOT_INSTALLED),
                downloadStatus = UIModelDownloadStatus.Idle,
                isModelLoaded = true,
                networkState = ConnectivityState.WIFI,
            ),
            SelectedModelScreenState(
                model = PreviewFakes.FAKE_STREAMING_MODEL.copy(status = ModelInstallStatus.DOWNLOADING),
                downloadStatus = UIModelDownloadStatus.Downloading(progress = { .45f }),
                isModelLoaded = true,
                networkState = ConnectivityState.CELLULAR,
            ),
            SelectedModelScreenState(
                model = PreviewFakes.FAKE_STREAMING_MODEL,
                downloadStatus = UIModelDownloadStatus.Failed("Failed to connect to server"),
                isModelLoaded = true,
                networkState = ConnectivityState.WIFI,
            ),
            SelectedModelScreenState(
                model = PreviewFakes.FAKE_STREAMING_MODEL.copy(status = ModelInstallStatus.INSTALLED),
                downloadStatus = UIModelDownloadStatus.Idle,
                isModelLoaded = true,
                networkState = ConnectivityState.OFFLINE,
            ),
            SelectedModelScreenState(
                model = PreviewFakes.FAKE_STREAMING_MODEL.copy(status = ModelInstallStatus.INSTALLED),
                downloadStatus = UIModelDownloadStatus.Success,
                isModelLoaded = true,
                networkState = ConnectivityState.WIFI,
            ),
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@LightThemedPreview
@DarkThemedPreview
@Composable
private fun SimpleRecorderSheetPreview(
    @PreviewParameter(SelectedModelScreenStatePreviewProvider::class)
    state: SelectedModelScreenState,
) = Surface(
    shape = BottomSheetDefaults.ExpandedShape,
    color = BottomSheetDefaults.ContainerColor,
) {
    RecommendedModelDownloadSheet(
        state = state,
        onAction = {},
        onDismiss = {},
        modifier = Modifier.padding(Dimensions.MODAL_BOTTOM_SHEET_CONTENT_PADDING),
    )
}
