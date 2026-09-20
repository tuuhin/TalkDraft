package com.sam.talkdraft.database.enums

enum class DBModelDownloadStatus(val code: String) {
    UNKNOWN("UNKNOWN"),
    DOWNLOADING("DOWNLOADING"),
    DOWNLOADED("DOWNLOADED");

    companion object {
        internal fun fromCode(code: String?): DBModelDownloadStatus {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}
