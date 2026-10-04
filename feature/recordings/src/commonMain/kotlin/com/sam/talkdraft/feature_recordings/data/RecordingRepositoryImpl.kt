package com.sam.talkdraft.feature_recordings.data

import com.sam.talkdraft.common.utils.Resource
import com.sam.talkdraft.feature_recordings.domain.IRecordingsRepository
import com.sam.talkdraft.feature_recordings.domain.model.RecordingModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory(binds = [IRecordingsRepository::class])
internal class RecordingRepositoryImpl : IRecordingsRepository {

    override fun getAllRecordings(): Flow<Resource<List<RecordingModel>, Exception>> {
        TODO("Not yet implemented")
    }

    override fun saveNewRecording(): Result<RecordingModel> {
        TODO("Not yet implemented")
    }
}
