package com.sam.talkdraft.feature_recordings.domain.exception

internal class EmptyFileException : Exception("Associated file path has no data, cannot copy the file content")
