package com.sam.talkdraft.model_manager.data.remote

import co.touchlab.kermit.Logger
import com.sam.talkdraft.model_manager.data.mapper.toDomainModels
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelArtifactDTO
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelMetadataDTO
import com.sam.talkdraft.model_manager.domain.exceptions.RemoteDatasourceException
import com.sam.talkdraft.model_manager.domain.remote.IModelRemoteDataSource
import com.sam.talkdraft.model_manager.domain.remote.RemoteTranscriptionModel
import com.sam.talkdraft.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Factory


@Factory(binds = [IModelRemoteDataSource::class])
internal class ModelRemoteDataSource(val supabase: SupabaseProvider) : IModelRemoteDataSource {

    private val supabaseClient by lazy { supabase.providesSupabase() }

    override suspend fun readRemoteSource(): Result<List<RemoteTranscriptionModel>> {
        return runCatching {
            coroutineScope {
                val metaData = async { fetchMetadata() }
                val artifact = async { fetchArtifacts() }
                metaData.await().toDomainModels(artifact.await())
            }
        }.onFailure { err ->
            if (err is CancellationException) throw err
        }
    }


    private suspend fun fetchMetadata(): List<RemoteModelMetadataDTO> {
        return try {
            supabaseClient
                .from(SupabaseTableName.REMOTE_MODEL_METADATA_TABLE)
                .select()
                .decodeList<RemoteModelMetadataDTO>()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.d(tag = TAG, throwable = e) { "FAILED TO READ METADATA" }
            throw RemoteDatasourceException.MetadataFetchFailed(e)
        }
    }

    private suspend fun fetchArtifacts(): List<RemoteModelArtifactDTO> {
        return try {
            supabaseClient
                .from(SupabaseTableName.REMOTE_MODEL_ARTIFACT_TABLE)
                .select()
                .decodeList<RemoteModelArtifactDTO>()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.d(tag = TAG, throwable = e) { "FAILED TO READ ARTIFACTS" }
            throw RemoteDatasourceException.ArtifactFetchFailed(e)
        }
    }

    companion object {
        private const val TAG = "REMOTE_MODEL_DATASOURCE"
    }
}
