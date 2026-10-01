package com.sam.talkdraft.model_manager.domain.local

import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

internal interface IModelLocalDataSource {

    fun readAllModelsAsFlow(): Flow<List<LocalTranscriptionModel>>
    suspend fun getAllModels(): List<LocalTranscriptionModel>
    suspend fun getAllModelsByType(type: TranscriptionType): List<LocalTranscriptionModel>

    fun getModelAsFlow(id: Uuid): Flow<LocalTranscriptionModel>
    suspend fun getModelById(uuid: Uuid): LocalTranscriptionModel

    suspend fun hasLocalData(): Boolean

    suspend fun updateModelStatus(modelId: Uuid, status: ModelInstallStatus = ModelInstallStatus.NOT_INSTALLED)
        : LocalTranscriptionModel?

    suspend fun updateModelPath(modelId: Uuid, path: String?): LocalTranscriptionModel?

    suspend fun insertOrUpdateModel(models: List<LocalTranscriptionModel>)
    suspend fun deleteModel(models: List<LocalTranscriptionModel>)
    suspend fun readSmallestModelByType(maxSizeInBytes: Long, type: TranscriptionType): LocalTranscriptionModel
    suspend fun readSmallestModel(maxSizeInBytes: Long): LocalTranscriptionModel
}
