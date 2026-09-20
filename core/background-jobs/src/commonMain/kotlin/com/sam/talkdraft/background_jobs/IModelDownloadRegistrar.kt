package com.sam.talkdraft.background_jobs

import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/**
 * Manages and monitoring the download lifecycle of transcription models.
 */
interface IModelDownloadRegistrar {

    /**
     * Initiates a background download for the specified [model].
     *
     * @param model The [TranscriptionModel] to be downloaded.
     * @return A unique [Uuid] tracking this specific download request.
     */
    fun startModelDownload(model: TranscriptionModel): Uuid

    /**
     * Observes the [ModelDownloadStatus] and status for a specific download request.
     *
     * @param uuid The unique identifier returned by [startModelDownload].
     * @return A [Flow] emitting updates on the [ModelDownloadStatus].
     */
    fun observerDownloadStatus(uuid: Uuid): Flow<ModelDownloadStatus>

    /**
     * Observes the download status associated with a given [model].
     *
     * @param model The [TranscriptionModel] to observe.
     * @return A [Flow] emitting a pair containing the request's [Uuid] and its current
     * [ModelDownloadStatus], or `null` if no status is currently registered.
     */
    fun observerDownloadStatus(model: TranscriptionModel): Flow<Pair<Uuid, ModelDownloadStatus>>

    /**
     * Cancels an ongoing download associated with the given [uuid].
     *
     * @param uuid The unique identifier of the download request to cancel.
     */
    fun cancelDownload(uuid: Uuid)
}
