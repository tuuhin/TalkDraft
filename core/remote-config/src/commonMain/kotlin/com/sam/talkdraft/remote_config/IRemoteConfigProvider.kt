package com.sam.talkdraft.remote_config

import com.sam.talkdraft.remote_config.model.RemoteConfigData

interface IRemoteConfigProvider {

    suspend fun loadFlags()
    fun minAndroidVersion(): RemoteConfigData<Long>
    fun minIosVersion(): RemoteConfigData<Long>
}
