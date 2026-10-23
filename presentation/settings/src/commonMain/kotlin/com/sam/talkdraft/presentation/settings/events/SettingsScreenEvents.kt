package com.sam.talkdraft.presentation.settings.events

internal sealed interface SettingsScreenEvents {
    data object ShowToast : SettingsScreenEvents
}
