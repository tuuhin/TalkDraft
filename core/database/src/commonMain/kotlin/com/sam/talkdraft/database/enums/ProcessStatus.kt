package com.sam.talkdraft.database.enums

enum class ProcessStatus(val code: String) {
    UNKNOWN("UNKNOWN"),
    RUNNING("RUNNING"),
    COMPLETED("COMPLETED");

    companion object {
        internal fun fromCode(code: String?): ProcessStatus {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}
