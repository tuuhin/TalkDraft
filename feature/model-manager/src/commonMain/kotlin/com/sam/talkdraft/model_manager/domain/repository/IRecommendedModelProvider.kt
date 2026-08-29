package com.sam.talkdraft.model_manager.domain.repository

import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel

fun interface IRecommendedModelProvider {

    suspend fun recommendedModel(): Result<TranscriptionModel>
}
