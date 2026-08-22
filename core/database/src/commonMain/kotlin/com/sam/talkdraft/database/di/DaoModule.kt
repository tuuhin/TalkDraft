package com.sam.talkdraft.database.di

import com.sam.talkdraft.database.TalkDraftDB
import com.sam.talkdraft.database.dao.GeneratedNotesDao
import com.sam.talkdraft.database.dao.LocalTranscriptionEntityDao
import com.sam.talkdraft.database.dao.ProcessingJobDao
import com.sam.talkdraft.database.dao.RecordingDao
import com.sam.talkdraft.database.dao.RecordingsDetailsDao
import com.sam.talkdraft.database.dao.TranscriptModelDownloadEntityDao
import com.sam.talkdraft.database.dao.TranscriptSegmentsDao
import com.sam.talkdraft.database.dao.TranscriptsDao
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
internal class DaoModule {

    @Singleton
    fun generatedNotesDao(db: TalkDraftDB): GeneratedNotesDao = db.generatedNotesDao()

    // recordings and transcripts
    @Singleton
    fun transcriptsDao(db: TalkDraftDB): TranscriptsDao = db.transcriptsDao()

    @Singleton
    fun recordingsDao(db: TalkDraftDB): RecordingDao = db.recordingsDao()

    @Singleton
    fun recordingsDetailsDao(db: TalkDraftDB): RecordingsDetailsDao = db.recordingsDetailsDao()

    @Singleton
    fun transcriptsSegmentDao(db: TalkDraftDB): TranscriptSegmentsDao = db.transcriptsSegmentDao()

    // processing audit
    @Singleton
    fun processingDao(db: TalkDraftDB): ProcessingJobDao = db.processingDao()

    // local transcriptions model list
    @Singleton
    fun transcriptionModelDao(db: TalkDraftDB): LocalTranscriptionEntityDao = db.transcriptionModelDao()

    @Singleton
    fun transcriptsModelDao(db: TalkDraftDB): TranscriptModelDownloadEntityDao = db.downloadEntityDao()
}
