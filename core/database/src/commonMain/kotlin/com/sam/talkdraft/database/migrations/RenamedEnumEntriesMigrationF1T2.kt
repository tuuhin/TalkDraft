package com.sam.talkdraft.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.sam.talkdraft.database.utils.DBConstants

internal class RenamedEnumEntriesMigrationF1T2 : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            UPDATE ${DBConstants.DOWNLOADED_MODEL_TABLE}
            SET status = CASE
                WHEN status = '0' OR status = 0 THEN 'UNKNOWN'
                WHEN status = '1' OR status = 1 THEN 'DOWNLOADING'
                WHEN status = '2' OR status = 2 THEN 'DOWNLOADED'
                ELSE 'UNKNOWN'
            END
            """.trimIndent(),
        )

        connection.execSQL(
            """
            UPDATE ${DBConstants.PROCESSING_ENTRIES_TABLE_NAME}
            SET status = CASE
                WHEN status = '0' OR status = 0 THEN 'UNKNOWN'
                WHEN status = '1' OR status = 1 THEN 'RUNNING'
                WHEN status = '2' OR status = 2 THEN 'COMPLETED'
                ELSE 'UNKNOWN'
            END
            """.trimIndent(),
        )

        connection.execSQL(
            """
            UPDATE ${DBConstants.PROCESSING_ENTRIES_TABLE_NAME}
            SET type = CASE
                WHEN type = '0' OR type = 0 THEN 'ON_DEVICE_TRANSCRIPTION'
                WHEN type = '1' OR type = 1 THEN 'CLOUD_TRANSFORMATION'
                ELSE 'ON_DEVICE_TRANSCRIPTION'
            END
            """.trimIndent(),
        )

        connection.execSQL(
            """
            UPDATE ${DBConstants.TRANSCRIPTION_MODEL_TABLE}
            SET model_status = CASE
                WHEN model_status = '0' OR model_status = 0 THEN 'ACTIVE'
                WHEN model_status = '1' OR model_status = 1 THEN 'DEPRECATED'
                WHEN model_status = '2' OR model_status = 2 THEN 'INACTIVE'
                ELSE 'ACTIVE'
            END
            """.trimIndent(),
        )
    }
}
