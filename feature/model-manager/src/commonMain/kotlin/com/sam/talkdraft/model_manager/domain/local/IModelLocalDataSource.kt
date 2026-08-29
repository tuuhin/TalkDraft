package com.sam.talkdraft.model_manager.domain.local

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

internal interface IModelLocalDataSource {

    fun readAllModelsAsFlow(): Flow<List<LocalTranscriptionModel>>
    suspend fun getAllModels(): List<LocalTranscriptionModel>

    fun getModelAsFlow(id: Uuid): Flow<LocalTranscriptionModel>
    suspend fun getModelById(uuid: Uuid): LocalTranscriptionModel

    suspend fun hasLocalData(): Boolean

    suspend fun upsertModels(models: List<LocalTranscriptionModel>)
    suspend fun deleteModel(models: List<LocalTranscriptionModel>)
    suspend fun readSmallestModel(maxSizeInBytes: Long): LocalTranscriptionModel
}
