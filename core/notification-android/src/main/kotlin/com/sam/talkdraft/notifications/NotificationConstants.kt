package com.sam.talkdraft.notifications

object NotificationConstants {

    const val LOCAL_DB_SYNC_WORKER_NOTIFICATION_ID = 12
    const val DOWNLOAD_STT_MODEL_WORKER_NOTIFICATION_ID = 13

    const val DB_SYNC_WORKER_CHANNEL_ID = "db_sync_worker"
    internal const val DB_SYNC_WORKER_CHANNEL_NAME = "Model Sync Worker"
    internal const val DB_SYNC_WORKER_CHANNEL_DESCRIPTION = "Represent a background sync with the remote database"

    const val DOWNLOAD_MODEL_WORKER_CHANNEL_ID = "download_model_worker"
    internal const val DOWNLOAD_MODEL_WORKER_CHANNEL_NAME = "Download speech model"
    internal const val DOWNLOAD_MODEL_WORKER_CHANNEL_DESCRIPTION = "Manages speech model downloads"
}
