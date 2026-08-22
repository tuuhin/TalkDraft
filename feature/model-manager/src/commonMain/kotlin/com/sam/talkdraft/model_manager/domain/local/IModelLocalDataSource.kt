package com.sam.talkdraft.model_manager.domain.local

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

internal interface IModelLocalDataSource {

    fun observeModels(): Flow<List<LocalTranscriptionModel>>
    suspend fun getModel(id: Uuid): LocalTranscriptionModel

    suspend fun hasLocalData(): Boolean

    suspend fun upsertModels(models: List<LocalTranscriptionModel>)
    suspend fun deleteModel(models: List<LocalTranscriptionModel>)
}
