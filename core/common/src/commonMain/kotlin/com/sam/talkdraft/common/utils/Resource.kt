package com.sam.talkdraft.common.utils

sealed interface Resource<out S, out E : Throwable> {
    data object Loading : Resource<Nothing, Nothing>
    data class Success<S, E : Throwable>(val data: S, val error: E? = null, val message: String? = null) :
        Resource<S, E>

    data class Error<S, E : Throwable>(val error: Throwable?, val message: String) : Resource<S, E>
}
