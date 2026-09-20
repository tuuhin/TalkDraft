package com.sam.talkdraft.database.enums

enum class DBModelArtifactType(val code: String) {
    BINARY("BIN"),
    PACKAGED("PKG"),
    UNKNOWN("UNK");

    companion object {
        internal fun fromCode(code: String?): DBModelArtifactType {
            return DBModelArtifactType.entries.find { it.code == code } ?: UNKNOWN
        }
    }
}
