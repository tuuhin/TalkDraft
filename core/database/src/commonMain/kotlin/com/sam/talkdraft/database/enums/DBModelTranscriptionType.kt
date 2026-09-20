package com.sam.talkdraft.database.enums

enum class DBModelTranscriptionType(val code: String) {
    STREAMING("STREAMING"),
    BATCHED("BATCHED");

    companion object {
        internal fun fromCode(code: String?): DBModelTranscriptionType {
            return DBModelTranscriptionType.entries.find { it.code == code } ?: BATCHED
        }
    }
}
