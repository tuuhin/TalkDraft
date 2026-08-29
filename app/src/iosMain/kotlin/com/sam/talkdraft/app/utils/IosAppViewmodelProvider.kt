package com.sam.talkdraft.app.utils

import com.sam.talkdraft.app.viewmodel.AppCommonViewmodel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object IosAppViewmodelProvider : KoinComponent {

    private val viewModel by inject<AppCommonViewmodel>()

    val showNewVersionRequiredDialog: Boolean
        get() = viewModel.showAppUpdateRequiredDialog

    suspend fun loadRemoteConfig() = viewModel.loadRemoteConfig()
}
