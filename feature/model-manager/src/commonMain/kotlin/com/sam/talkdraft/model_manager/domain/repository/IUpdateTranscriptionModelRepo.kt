package com.sam.talkdraft.model_manager.domain.repository

interface IUpdateTranscriptionModelRepo {

    suspend fun syncLocalData(): Result<Unit>
}
