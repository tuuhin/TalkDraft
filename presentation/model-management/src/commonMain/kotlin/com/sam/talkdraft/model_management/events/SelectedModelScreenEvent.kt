package com.sam.talkdraft.model_management.events

internal sealed interface SelectedModelScreenEvent {
    data object StartDownload : SelectedModelScreenEvent
    data object CancelDownload : SelectedModelScreenEvent
    data object DeleteModel : SelectedModelScreenEvent
}
