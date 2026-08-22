package com.sam.talkdraft.model_manager.data.local

import com.sam.talkdraft.database.dao.LocalTranscriptionEntityDao
import com.sam.talkdraft.model_manager.data.mapper.toDomain
import com.sam.talkdraft.model_manager.data.mapper.toEntity
import com.sam.talkdraft.model_manager.domain.exceptions.LocalDataSourceException
import com.sam.talkdraft.model_manager.domain.local.IModelLocalDataSource
import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
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

    override suspend fun getModel(id: Uuid): LocalTranscriptionModel {
        return dao.getModel(id)?.toDomain(timeZone)
            ?: throw LocalDataSourceException.InvalidSourceException()
    }

    override fun observeModels(): Flow<List<LocalTranscriptionModel>> {
        return dao.observeModels().map { models ->
            models.map { relation -> relation.toDomain(timeZone) }
        }
    }

    override suspend fun hasLocalData(): Boolean {
        return dao.isNotEmpty()
    }

    override suspend fun upsertModels(models: List<LocalTranscriptionModel>) {
        val entities = models.map { it.toEntity(timeZone) }
        dao.upsertTranscriptionModels(entities)
    }


}
