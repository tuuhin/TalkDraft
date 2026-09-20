package com.sam.talkdraft.database.enums

enum class DBRemoteModelStatus(val code: String) {
    ACTIVE("ACTIVE"),
    DEPRECATED("DEPRECATED"),
    INACTIVE("INACTIVE");

    companion object {
        internal fun fromCode(code: String?): DBRemoteModelStatus {
            return entries.find { it.code == code } ?: ACTIVE
        }
    }
}
