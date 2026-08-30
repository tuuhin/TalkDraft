package com.sam.talkdraft.onboarding.composables.scenes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import com.mohamedrejeb.calf.permissions.isGranted
import com.mohamedrejeb.calf.permissions.rememberMultiplePermissionsState
import com.mohamedrejeb.calf.permissions.shouldShowRationale
import com.sam.talkdraft.common.model.PlatformTarget
import com.sam.talkdraft.designs.CommonResources
import com.sam.talkdraft.designs.app_name
import com.sam.talkdraft.designs.ic_settings
import com.sam.talkdraft.onboarding.composables.OnboardingContextAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import talkdraft.presentation.onboarding.generated.resources.Res
import talkdraft.presentation.onboarding.generated.resources.ic_mic_filled
import talkdraft.presentation.onboarding.generated.resources.ic_no_mic
import talkdraft.presentation.onboarding.generated.resources.ic_no_notification
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
    val permissions = rememberMultiplePermissionsState(
        listOf(Permission.Notification, Permission.RecordAudio),
    )

    val isPermanentlyDenied by remember {
        derivedStateOf {
            if (permissions.allPermissionsGranted) return@derivedStateOf false
            when (platform) {
                PlatformTarget.IOS -> permissions.revokedPermissions
                    .any { permissionState -> permissionState.status is PermissionStatus.Denied }

                PlatformTarget.ANDROID -> permissions.revokedPermissions.isNotEmpty() && permissions.revokedPermissions.any { state ->
                    state.status.isDenied && state.status.shouldShowRationale
                }

                else -> true
            }
        }
    }
    val recorderStatus =
        if (permissions.permissions.any { it.permission == Permission.RecordAudio && it.status.isGranted })
            AppPermissionStatus.GRANTED
        else
            AppPermissionStatus.DENIED


    val notificationStatus =
        if (permissions.permissions.any { it.permission == Permission.Notification && it.status.isGranted })
            AppPermissionStatus.GRANTED
        else AppPermissionStatus.DENIED


    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Permissions",
            style = titleStyle,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(CommonResources.string.app_name) +
                " needs access to the features that make recording and transcription work smoothly",
            style = MaterialTheme.typography.titleSmallEmphasized,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        AppPermissions.entries.fastForEach { permission ->
            PermissionMarker(
                permission = permission,
                status = when (permission) {
                    AppPermissions.RECORD_AUDIO -> recorderStatus
                    AppPermissions.NOTIFICATIONS -> notificationStatus
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(IntrinsicSize.Max),
        ) {
            val buttonTitle = when {
                permissions.allPermissionsGranted -> "Permissions Granted"
                isPermanentlyDenied -> "Permission Denied"
                else -> "Grant Permissions"
            }
            OnboardingContextAction(
                title = buttonTitle,
                onClick = {
                    if (isPermanentlyDenied) {
                        openAppSettings()
                    } else {
                        permissions.launchMultiplePermissionRequest()
                    }
                },
                enabled = !permissions.allPermissionsGranted,
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(MaterialTheme.motionScheme.defaultEffectsSpec()),
            )

            AnimatedVisibility(
                visible = isPermanentlyDenied,
                enter = expandHorizontally(MaterialTheme.motionScheme.defaultEffectsSpec()),
                modifier = Modifier.fillMaxHeight(),
            ) {
                FilledIconButton(
                    onClick = openAppSettings,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                    shape = IconButtonDefaults.extraLargeRoundShape,
                ) {
                    Icon(
                        painter = painterResource(CommonResources.drawable.ic_settings),
                        contentDescription = "Settings",
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

    val grantedPainter: Painter
        @Composable
        get() = when (this) {
            RECORD_AUDIO -> painterResource(Res.drawable.ic_mic_filled)
            NOTIFICATIONS -> painterResource(Res.drawable.ic_notification_bell)
        }

    val notGrantedPainter: Painter
        @Composable
        get() = when (this) {
            RECORD_AUDIO -> painterResource(Res.drawable.ic_no_mic)
            NOTIFICATIONS -> painterResource(Res.drawable.ic_no_notification)
        }
}

private enum class AppPermissionStatus {
    GRANTED, DENIED
}

@Composable
private fun PermissionMarker(
    permission: AppPermissions,
    modifier: Modifier = Modifier,
    status: AppPermissionStatus = AppPermissionStatus.DENIED,
) {
    val colorAnimation by animateColorAsState(
        targetValue = if (status == AppPermissionStatus.GRANTED) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        label = "app_label_color",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedContent(
            targetState = status == AppPermissionStatus.GRANTED,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.4f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.4f))
            },
            label = "fade_scale_transition",
        ) { isGranted ->
            Icon(
                painter = if (isGranted) permission.grantedPainter else permission.notGrantedPainter,
                contentDescription = if (isGranted) "Granted: $permission" else "Restricted: $permission",
                tint = colorAnimation,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = permission.title,
            style = MaterialTheme.typography.bodySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
