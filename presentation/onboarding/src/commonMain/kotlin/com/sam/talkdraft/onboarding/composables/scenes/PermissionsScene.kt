package com.sam.talkdraft.onboarding.composables.scenes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.mohamedrejeb.calf.permissions.ExperimentalPermissionsApi
import com.mohamedrejeb.calf.permissions.Notification
import com.mohamedrejeb.calf.permissions.Permission
import com.mohamedrejeb.calf.permissions.PermissionStatus
import com.mohamedrejeb.calf.permissions.RecordAudio
import com.mohamedrejeb.calf.permissions.isDenied
import com.mohamedrejeb.calf.permissions.rememberMultiplePermissionsState
import com.sam.talkdraft.common.model.PlatformTarget
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.app_name
import com.sam.talkdraft.designs.ic_settings
import com.sam.talkdraft.onboarding.composables.OnboardingContextAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import talkdraft.presentation.onboarding.generated.resources.Res
import talkdraft.presentation.onboarding.generated.resources.ic_mic_filled
import talkdraft.presentation.onboarding.generated.resources.ic_notification_bell
import talkdraft.presentation.onboarding.generated.resources.permission_notificaiton_text
import talkdraft.presentation.onboarding.generated.resources.permission_record_audio_text

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun PermissionsScene(
    onAction: () -> Unit,
    openAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.displaySmallEmphasized,
    contentPadding: PaddingValues = PaddingValues.Zero,
    platform: PlatformTarget = PlatformTarget.UNKNOWN,
) {
    val permissions = rememberMultiplePermissionsState(listOf(Permission.Notification, Permission.RecordAudio))

    val isPermanentlyDenied by remember(permissions, platform) {
        derivedStateOf {
            if (permissions.allPermissionsGranted) return@derivedStateOf false

            when (platform) {
                PlatformTarget.IOS -> permissions.revokedPermissions.any { permissionState -> permissionState.status.isDenied }


                else -> {
                    permissions.revokedPermissions.isNotEmpty() &&
                        !permissions.shouldShowRationale &&
                        permissions.revokedPermissions.any {
                            (it.status as? PermissionStatus.Denied)?.shouldShowRationale == false
                        }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "A few things to get started",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "1. Permissions",
                style = titleStyle,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(CommonResources.string.app_name) + " needs access to the features that make recording and transcription work smoothly",
            style = MaterialTheme.typography.titleSmallEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        AppPermissions.entries.fastForEach {
            PermissionMarker(it, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(IntrinsicSize.Max),
        ) {
            val buttonTitle = when {
                permissions.allPermissionsGranted -> "Permissions Granted"
                isPermanentlyDenied -> "Open Settings"
                else -> "Grant Permissions"
            }
            OnboardingContextAction(
                title = buttonTitle,
                onClick = {
                    if (isPermanentlyDenied) openAppSettings()
                    else permissions.launchMultiplePermissionRequest()
                },
                enabled = !permissions.allPermissionsGranted,
                modifier = Modifier.weight(1f)
                    .animateContentSize(MaterialTheme.motionScheme.defaultEffectsSpec()),
            )

            AnimatedVisibility(
                visible = isPermanentlyDenied,
                enter = expandHorizontally(MaterialTheme.motionScheme.defaultEffectsSpec()),
                modifier = Modifier.fillMaxHeight(),
            ) {
                FilledIconButton(
                    onClick = openAppSettings,
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                    shape = IconButtonDefaults.extraLargeRoundShape,
                ) {
                    Icon(
                        painter = painterResource(CommonResources.drawable.ic_settings),
                        "Settings",
                        modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                    )
                }
            }
        }

        OnboardingContextAction(
            title = if (!permissions.allPermissionsGranted) "Skip for Now" else "Continue",
            onClick = onAction,
        )
    }
}


private enum class AppPermissions {
    RECORD_AUDIO,
    NOTIFICATIONS;

    val title: String
        @Composable
        get() = when (this) {
            RECORD_AUDIO -> stringResource(Res.string.permission_record_audio_text)
            NOTIFICATIONS -> stringResource(Res.string.permission_notificaiton_text)
        }

    val painter: Painter
        @Composable
        get() = when (this) {
            RECORD_AUDIO -> painterResource(Res.drawable.ic_mic_filled)
            NOTIFICATIONS -> painterResource(Res.drawable.ic_notification_bell)
        }
}

@Composable
private fun PermissionMarker(
    permissions: AppPermissions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = permissions.painter,
            contentDescription = "permission :${permissions.name}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = permissions.title,
            style = MaterialTheme.typography.bodySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
