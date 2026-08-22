package com.sam.talkdraft.model_manager.domain.exceptions

sealed class RemoteDatasourceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class MetadataFetchFailed(cause: Throwable) : RemoteDatasourceException(
        message = "Failed to fetch model metadata.",
        cause = cause,
    )

    class ArtifactFetchFailed(cause: Throwable) : RemoteDatasourceException(
        message = "Failed to fetch model artifacts.",
        cause = cause,
    )

}
