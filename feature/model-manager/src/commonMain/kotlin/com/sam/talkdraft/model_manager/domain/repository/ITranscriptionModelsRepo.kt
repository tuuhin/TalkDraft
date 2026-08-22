package com.sam.talkdraft.model_manager.domain.repository

import com.sam.talkdraft.common.utils.Resource
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

interface ITranscriptionModelsRepo {

    fun readAllModels(): Flow<Resource<List<TranscriptionModel>, Exception>>
    suspend fun readModel(uuid: Uuid): Result<TranscriptionModel>
    suspend fun refreshModels(): Result<Unit>
}
