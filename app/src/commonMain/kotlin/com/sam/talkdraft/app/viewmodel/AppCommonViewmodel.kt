package com.sam.talkdraft.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.sam.talkdraft.common.model.PlatformTarget
import com.sam.talkdraft.common.platform.IPlatformTargetProvider
import com.sam.talkdraft.remote_config.IRemoteConfigProvider
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AppCommonViewmodel(
    private val remoteConfig: IRemoteConfigProvider,
    currentTarget: IPlatformTargetProvider,
) : ViewModel() {

    private val versionCode by mutableStateOf(currentTarget.platformVersionCode)
    private val remoteMinAndroidVersion by mutableStateOf(remoteConfig.minAndroidVersion().value ?: 0)
    private val remoteMinIosVersion by mutableStateOf(remoteConfig.minIosVersion().value ?: 0)

    val showAppUpdateRequiredDialog = when (currentTarget.target()) {
        PlatformTarget.ANDROID -> versionCode < remoteMinAndroidVersion
        PlatformTarget.IOS -> versionCode < remoteMinIosVersion
        PlatformTarget.UNKNOWN -> false
    }

    val isAndroid = currentTarget.target() == PlatformTarget.ANDROID
    val isIos = currentTarget.target() == PlatformTarget.IOS

    suspend fun loadRemoteConfig() = remoteConfig.loadFlags()

}
