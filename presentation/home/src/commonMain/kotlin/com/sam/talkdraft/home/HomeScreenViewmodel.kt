package com.sam.talkdraft.home

import androidx.lifecycle.ViewModel
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.feature_recordings.domain.IRecordingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class HomeScreenViewmodel(
    private val recordingsRepo: IRecordingsRepository,
) : ViewModel() {

    val recordings = recordingsRepo.getAllRecordings()

    val uiEvents: SharedFlow<UIEvents>
        get() = MutableSharedFlow()
}
