package com.sam.talkdraft.file_archive.domain

import com.sam.talkdraft.file_archive.domain.models.ArchiveFormats
import okio.Path

fun interface IArchiveExtractor {

    suspend fun extract(src: Path, dest: Path, formats: ArchiveFormats): Result<Unit>
}
