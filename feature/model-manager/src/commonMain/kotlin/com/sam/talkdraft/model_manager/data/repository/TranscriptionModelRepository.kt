package com.sam.talkdraft.model_manager.data.repository

import com.sam.talkdraft.common.utils.Resource
import com.sam.talkdraft.model_manager.data.mapper.toDomainModel
import com.sam.talkdraft.model_manager.data.mapper.toLocal
import com.sam.talkdraft.model_manager.domain.local.IModelLocalDataSource

import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.remote.IModelRemoteDataSource
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.TimeZone
import org.koin.core.annotation.Factory

@Factory(binds = [ITranscriptionModelsRepo::class])
internal class TranscriptionModelRepository(
    private val localDataSource: IModelLocalDataSource,
    private val remoteDataSource: IModelRemoteDataSource,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ITranscriptionModelsRepo {

    override fun readAllModelsFlow(): Flow<Resource<List<TranscriptionModel>, Exception>> {
        return localDataSource.readAllModelsAsFlow()
            .map { data ->
                Resource.Success<List<TranscriptionModel>, Exception>(data.map { it.toDomainModel() })
                    as Resource<List<TranscriptionModel>, Exception>
            }
            .onStart {
                emit(Resource.Loading)
                if (!localDataSource.hasLocalData()) {
                    runCatching {
                        refreshModels().getOrThrow()
                    }.onFailure { err ->
                        emit(
                            Resource.Error(
                                err as? Exception ?: Exception(err),
                                "Failed to load initial models from remote",
                            ),
                        )
                    }
                }
            }
            .catch { err ->
                val error = Resource.Error<List<TranscriptionModel>, Exception>(
                    error = err as? Exception ?: Exception(err),
                    message = "Failed to read the models",
                )
                emit(error)
            }
    }

    override fun readModelAsFlow(uuid: Uuid): Flow<TranscriptionModel?> {
        return localDataSource.getModelAsFlow(uuid).map { it.toDomainModel() }
    }


    override suspend fun readAllModels(): Result<List<TranscriptionModel>> {
        return runCatching {
            if (!localDataSource.hasLocalData()) refreshModels().getOrThrow()

            localDataSource.getAllModels().map { it.toDomainModel() }
        }
    }

    override suspend fun readSmallestModel(maxModelSize: Long): Result<TranscriptionModel> {
        return runCatching {
            if (!localDataSource.hasLocalData()) refreshModels().getOrThrow()
            localDataSource.readSmallestModel(maxModelSize).toDomainModel()
        }
    }

    override suspend fun readModel(uuid: Uuid): Result<TranscriptionModel> {
        return runCatching {
            if (!localDataSource.hasLocalData()) refreshModels().getOrThrow()
            localDataSource.getModelById(uuid).toDomainModel()
        }
    }

    override suspend fun updateModel(model: TranscriptionModel): Result<Unit> {
        return runCatching {
            localDataSource.upsertModels(listOf(model.toLocal()))
        }
    }

    private suspend fun refreshModels(): Result<Unit> {
        return runCatching {
            val remoteModels = remoteDataSource.readRemoteSource().getOrThrow()
            val localModels = remoteModels.map { remote -> remote.toLocal(timeZone) }
            localDataSource.upsertModels(localModels)
        }
    }
}
