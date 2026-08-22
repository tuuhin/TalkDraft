package com.sam.talkdraft.model_manager.domain.remote

internal interface IModelRemoteDataSource {

    suspend fun readRemoteSource(): Result<List<RemoteTranscriptionModel>>
    suspend fun getRemoteCatalogVersion(): Result<Int>
}
