package com.sam.talkdraft.model_downloader.domain

import com.sam.talkdraft.model_downloader.domain.exceptions.ModelFileAlreadyExistsException
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlinx.coroutines.CancellationException
import okio.Path

/**
 * Interface responsible for managing local storage and lifecycle operations of model files,
 * including persistence from temporary cache locations and cleanup.
 */
internal interface IModelFileManager {

    /**
     * Promotes a cached model file to permanent storage under the appropriate model path directory structure.
     *
     * In case of thread/coroutine cancellation mid-operation, any partially written or copied file
     * at the destination is guaranteed to be cleaned up.
     *
     * @param model The [TranscriptionModel] metadata defining the model target destination.
     * @param cachedPath The [Path] to the temporary or cached source file to be persisted.
     * @param overwrite If `false` and the model already points to an existing file location,
     * the operation fails without modifying existing files. Defaults to `true`.
     *
     * @return A [Result] containing [Unit] on success, or encapsulating an exception on failure:
     * - [ModelFileAlreadyExistsException] if [overwrite] is `false` and the model target already exists.
     * - [okio.IOException] or file system exception if path creation, file copy, or deletion fails.
     *
     * @throws CancellationException Re-thrown if the calling coroutine context is cancelled.
     */
    suspend fun saveModel(
        model: TranscriptionModel,
        cachedPath: Path,
        overwrite: Boolean = true,
    ): Result<Unit>

    /**
     * Deletes the local file associated with the given model, as well as its parent directory
     * if left empty after deletion.
     *
     * Deletion of files and empty directories is executed within a non-cancellable block to ensure
     * storage is not left in an inconsistent state.
     *
     * @param model The [TranscriptionModel] to delete from disk.
     *
     * @return A [Result] containing `true` if deletion completes cleanly or if the target file did not exist,
     * or encapsulating an exception on failure:
     * - [okio.IOException] or underlying file system exception if file/directory deletion fails.
     *
     * @throws CancellationException Re-thrown if the calling coroutine context is cancelled.
     */
    suspend fun deleteModelFile(model: TranscriptionModel): Result<Boolean>
}
