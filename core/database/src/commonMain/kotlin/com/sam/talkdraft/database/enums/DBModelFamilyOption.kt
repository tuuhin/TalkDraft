package com.sam.talkdraft.database.enums

enum class DBModelFamilyOption(val family: String) {
    WHISPER("WHISPER"),
    ZIP_FORMER("ZIP_FORMER"),
    UNKNOWN("UNKNOWN");

    companion object {
        internal fun fromCode(family: String?): DBModelFamilyOption {
            return DBModelFamilyOption.entries.find { it.family.equals(family, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
