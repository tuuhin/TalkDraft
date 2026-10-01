package com.sam.talkdraft.model_manager.domain.repository

import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType

fun interface IRecommendedModelProvider {

    suspend fun recommendedModel(type: TranscriptionType): Result<TranscriptionModel>
}
