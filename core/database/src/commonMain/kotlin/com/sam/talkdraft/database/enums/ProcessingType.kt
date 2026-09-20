package com.sam.talkdraft.database.enums

enum class ProcessingType(val code: String) {
    ON_DEVICE_TRANSCRIPTION("ON_DEVICE_TRANSCRIPTION"),
    CLOUD_TRANSFORMATION("CLOUD_TRANSFORMATION");

    companion object {
        internal fun fromCode(code: String?): ProcessingType {
            return entries.find { it.code == code } ?: ON_DEVICE_TRANSCRIPTION
        }
    }
}
