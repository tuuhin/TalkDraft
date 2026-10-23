package com.sam.talkdraft.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.presentation.settings.events.SettingsScreenEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class SettingsViewmodel : ViewModel() {

    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow()

    fun onEvent(events: SettingsScreenEvents) {
        when (events) {
            SettingsScreenEvents.ShowToast -> viewModelScope.launch {
                uiEvents.emit(UIEvents.ShowToast("Hello"))
            }
        }
    }
}
