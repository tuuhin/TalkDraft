package com.sam.talkdraft.feature_recordings.domain

import okio.Path

internal interface IRecordingFileManager {

    val readRecordingPath: Path

    suspend fun saveFileContent(
        fileName: String,
        contentFilePath: Path,
        deleteContentFile: Boolean = true,
    ): Result<Path>
}
