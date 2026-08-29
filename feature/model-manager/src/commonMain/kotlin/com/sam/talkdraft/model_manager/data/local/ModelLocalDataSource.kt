package com.sam.talkdraft.model_manager.data.local

import com.sam.talkdraft.database.dao.LocalTranscriptionEntityDao
import com.sam.talkdraft.database.enums.ModelDownloadStatus
import com.sam.talkdraft.model_manager.data.mapper.toDomain
import com.sam.talkdraft.model_manager.data.mapper.toEntity
import com.sam.talkdraft.model_manager.domain.exceptions.LocalDataSourceException
import com.sam.talkdraft.model_manager.domain.local.IModelLocalDataSource
import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.LocalModelStatus
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import org.koin.core.annotation.Factory

@Factory(binds = [IModelLocalDataSource::class])
internal class ModelLocalDataSource(
    private val dao: LocalTranscriptionEntityDao,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : IModelLocalDataSource {

    override suspend fun deleteModel(models: List<LocalTranscriptionModel>) {
        val entities = models.map { it.toEntity(timeZone) }
        dao.deleteTranscriptionsModels(entities)
    }

    override fun getModelAsFlow(id: Uuid): Flow<LocalTranscriptionModel> {
        return dao.getModelFlow(id).filterNotNull()
            .map { it.toDomain(timeZone) }
    }

    override suspend fun getModelById(uuid: Uuid): LocalTranscriptionModel {
        return dao.getModel(uuid)?.toDomain(timeZone)
            ?: throw LocalDataSourceException.InvalidSourceException()
    }

    override suspend fun getAllModels(): List<LocalTranscriptionModel> {
        return dao.readAllModels().map { it.toDomain(timeZone) }
    }

    override suspend fun readSmallestModel(maxSizeInBytes: Long): LocalTranscriptionModel {
        return dao.readSmallestModel(maxSizeInBytes)?.toDomain(timeZone)
            ?: throw LocalDataSourceException.InvalidSourceException()
    }

    override fun readAllModelsAsFlow(): Flow<List<LocalTranscriptionModel>> {
        return dao.observeModels().map { models ->
            models.map { relation -> relation.toDomain(timeZone) }
        }
    }

    override suspend fun hasLocalData(): Boolean {
        return dao.isNotEmpty()
    }

    override suspend fun updateModelStatus(
        modelId: Uuid,
        status: LocalModelStatus,
    ): LocalTranscriptionModel? {
        val dbStatus = when (status) {
            LocalModelStatus.NOT_INSTALLED -> ModelDownloadStatus.UNKNOWN
            LocalModelStatus.DOWNLOADING -> ModelDownloadStatus.DOWNLOADING
            LocalModelStatus.INSTALLED -> ModelDownloadStatus.DOWNLOADED
        }
        dao.updateDownloadModelStatus(modelId, dbStatus)
        return dao.getModel(modelId)?.toDomain(timeZone)
    }

    override suspend fun updateModelPath(modelId: Uuid, path: String?): LocalTranscriptionModel? {
        dao.updateDownloadModelPath(modelId, path)
        return dao.getModel(modelId)?.toDomain(timeZone)
    }

    override suspend fun upsertModels(models: List<LocalTranscriptionModel>) {
        val entities = models.map { it.toEntity(timeZone) }
        dao.upsertTranscriptionModels(entities)
    }


}
