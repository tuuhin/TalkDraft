package com.sam.talkdraft.presentation.recordings

import androidx.lifecycle.ViewModel
import com.sam.talkdraft.designsystem.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class RecordingsListViewmodel : ViewModel() {

    val uiEvents: SharedFlow<UIEvents>
        get() = MutableSharedFlow()
}
