package com.sam.talkdraft.model_manager.domain.remote

internal fun interface IModelRemoteDataSource {

    suspend fun readRemoteSource(): Result<List<RemoteTranscriptionModel>>
}
