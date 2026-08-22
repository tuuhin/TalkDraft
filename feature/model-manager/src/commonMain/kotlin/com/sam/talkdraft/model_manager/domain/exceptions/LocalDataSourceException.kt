package com.sam.talkdraft.model_manager.domain.exceptions

sealed class LocalDataSourceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class InvalidSourceException : LocalDataSourceException(message = "Cannot find the local source by id")
}

