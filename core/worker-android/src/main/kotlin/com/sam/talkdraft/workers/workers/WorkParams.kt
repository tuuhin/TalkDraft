package com.sam.talkdraft.workers.workers

internal object WorkParams {
    // Associated with remote db sync
    const val DB_MODEL_SYNC_KEY = "db_model_sync"
    const val DB_MODEL_SYNC_SUCCESS = "db_model_sync_success"
    const val DB_MODEL_SYNC_FAILED = "db_model_sync_failed"
    const val DB_MODEL_SYNC_FAILED_REASON = "db_model_sync_failed_reason"

    // associated with model download
    const val TRANSCRIPTION_MODEL_DOWNLOAD_KEY = "transcription_model_download"
    const val TRANSCRIPTION_INPUT_MODEL_ID = "transcription_model_id"
    const val TRANSCRIPTION_MODEL_DOWNLOAD_SUCCESS = "transcription_model_download_success"
    const val TRANSCRIPTION_MODEL_DOWNLOAD_FAILED = "transcription_model_download_failed"
    const val TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_INVALID_ID = "invalid_model_id_provided"
    const val TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_MESSAGE = "trancription_model_download_failed_message"
    const val TRANSCRIPTION_MODEL_DOWNLOAD_SAVE_STATUS = "transcription_model_download_save_status"

    const val TRANSCRIPTION_STATUS_KEY = "transcription_status"
    const val TRANSCRIPTION_STATUS_STARTING_DOWNLOAD = "transcription_status_download_started"
    const val TRANSCRIPTOON_STATUS_VERIFYING_DOWNLOAD = "transcription_status_verifying"
    const val TRANCRIPTION_STATUS_DOWNLOAD_FAILED = "transcription_status_download_failed"
    const val TRANSCRIPTION_STATUS_DOWNLOADING = "transcription_status_downloading"
    const val TRANSCRIPTION_STATUS_VERIFYING = "transcription_status_verifying"
    const val TRANSCRIPTION_STATUS_DOWNLOAD_SUCCESS = "transcription_status_download_success"
    const val TRANSCRIPTION_STATUS_DOWNLOAD_PERCENTAGE = "download_percentage"

}
