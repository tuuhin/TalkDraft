package com.sam.talkdraft.model_manager.data.repository

import com.sam.talkdraft.model_manager.data.mapper.toLocal
import com.sam.talkdraft.model_manager.domain.local.IModelLocalDataSource
import com.sam.talkdraft.model_manager.domain.remote.IModelRemoteDataSource
import com.sam.talkdraft.model_manager.domain.repository.IUpdateTranscriptionModelRepo
import io.ktor.utils.io.CancellationException
import kotlinx.datetime.TimeZone
import org.koin.core.annotation.Singleton

@Singleton(binds = [IUpdateTranscriptionModelRepo::class])
internal class UpdateTranscriptionModelRepository(
    private val localDataSource: IModelLocalDataSource,
    private val remoteDataSource: IModelRemoteDataSource,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : IUpdateTranscriptionModelRepo {

    override suspend fun syncLocalData(): Result<Unit> {
        return runCatching {
            val remoteModels = remoteDataSource.readRemoteSource().getOrThrow()
            val localModels = remoteModels.map { remote -> remote.toLocal(timeZone) }
            localDataSource.upsertModels(localModels)
        }.onFailure { err ->
            if (err is CancellationException) throw err
        }
    }
}
