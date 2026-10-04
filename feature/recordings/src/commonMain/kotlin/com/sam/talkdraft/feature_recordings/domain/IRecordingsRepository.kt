package com.sam.talkdraft.feature_recordings.domain

import com.sam.talkdraft.common.utils.Resource
import com.sam.talkdraft.feature_recordings.domain.model.RecordingModel
import kotlinx.coroutines.flow.Flow

interface IRecordingsRepository {

    fun getAllRecordings(): Flow<Resource<List<RecordingModel>, Exception>>

    fun saveNewRecording(): Result<RecordingModel>
}
