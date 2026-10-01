package com.sam.talkdraft.onboarding.composables.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.onboarding.composables.OnboardingContextAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocalTranscriptionModelScene(
    onAction: () -> Unit,
    onNavigateToModelDownload: () -> Unit,
    modifier: Modifier = Modifier,
    recommendModel: TranscriptionModel? = null,
    titleStyle: TextStyle = MaterialTheme.typography.displaySmallEmphasized,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {

    val recommendedModelExists by remember(recommendModel) {
        derivedStateOf { recommendModel != null }
    }

    val isModelInstalled by remember(recommendModel) {
        derivedStateOf {
            recommendModel?.status == ModelInstallStatus.INSTALLED
                && recommendModel.modelPath != null
        }
    }

    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Get your offline voice model",
            style = titleStyle,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Download the model once to transcribe your recordings on your device, even without internet",
            style = MaterialTheme.typography.titleSmallEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OnboardingContextAction(
            title = if (isModelInstalled) "Model Already Present" else "Get Models Now",
            enabled = recommendedModelExists,
            onClick = onNavigateToModelDownload,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
        OnboardingContextAction(
            title = if (isModelInstalled) "Continue" else "Skip",
            onClick = onAction,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
    }
}
