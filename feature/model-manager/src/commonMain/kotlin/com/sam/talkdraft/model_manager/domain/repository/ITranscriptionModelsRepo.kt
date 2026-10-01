package com.sam.talkdraft.model_manager.domain.repository

import com.sam.talkdraft.common.utils.Resource
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing and retrieving [TranscriptionModel] entities.
 *
 * Provides reactive data streams and suspending operations for reading, filtering,
 * and updating available transcription models.
 */
interface ITranscriptionModelsRepo {

    /**
     * Observes continuous updates to the full list of transcription models.
     *
     * @return A [Flow] emitting a [Resource] that wraps the list of [TranscriptionModel]
     * instances or an [Exception] if a stream error occurs.
     */
    fun readAllModelsFlow(): Flow<Resource<List<TranscriptionModel>, Exception>>

    /**
     * Fetches a specific transcription model by its unique identifier, and observe it for changes
     *
     * @param uuid The unique [Uuid] of the model to retrieve.
     * @return A [Flow] of matching [TranscriptionModel] otherwise null,
     */
    fun readModelAsFlow(uuid: Uuid): Flow<TranscriptionModel?>

    /**
     * Fetches all available transcription models once.
     *
     * @return A [Result] containing the list of [TranscriptionModel] instances on success,
     * or the failure exception.
     */
    suspend fun readAllModels(): Result<List<TranscriptionModel>>

    /**
     * Fetches all available transcription models based on the given type
     * @param type Type of transcription we requested , can be streaming or batched
     * @return A [Result] containing the list of [TranscriptionModel] instances on success,
     * or the failure exception.
     * @see [TranscriptionType]
     */
    suspend fun readAllModelByType(type: TranscriptionType): Result<List<TranscriptionModel>>

    /**
     * Retrieves the smallest transcription model whose size does not exceed [maxModelSize].
     *
     * @param maxModelSize The maximum allowed size in bytes for the model.
     * @param type Reads based on the transcription type , if null then type is not taken care of
     * @return A [Result] containing the matching [TranscriptionModel] on success,
     * or an error if no suitable model is found or the operation fails.
     */
    suspend fun readSmallestModel(maxModelSize: Long, type: TranscriptionType? = null): Result<TranscriptionModel>

    /**
     * Fetches a specific transcription model by its unique identifier, and observe it as a flow
     *
     * @param uuid The unique [Uuid] of the model to retrieve.
     * @return A [Result] of matching [TranscriptionModel] otherwise,an error if the model
     * does not exist or the operation fails.
     */
    suspend fun readModel(uuid: Uuid): Result<TranscriptionModel>

    /**
     * Updates the state or metadata of an existing transcription model.
     *
     * @param modelId The [TranscriptionModel]'s id whose status need to be updated
     * @param status The status to update to
     * @return A [Result] of matching [TranscriptionModel] otherwise,an error if the model
     * does not exist or the operation fails.
     */
    suspend fun updateModelStatus(
        modelId: Uuid,
        status: ModelInstallStatus = ModelInstallStatus.NOT_INSTALLED,
    ): Result<TranscriptionModel>

    /**
     * Updates the state or metadata of an existing transcription model.
     *
     * @param modelId The [TranscriptionModel]'s id whose status need to be updated
     * @param path New model path to update to
     * @return A [Result] of matching [TranscriptionModel] otherwise,an error if the model
     * does not exist or the operation fails.
     */
    suspend fun updateModelPath(modelId: Uuid, path: String? = null): Result<TranscriptionModel>
}
