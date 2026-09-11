package com.sam.talkdraft.transcription_android.assets

import android.content.res.AssetManager
import java.io.File
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetsToFileConvertor(private val manager: AssetManager) {

    suspend fun convertToFile(assetPath: String, directory: File): File {
        return withContext(Dispatchers.IO) {
            val extension = assetPath.substringAfterLast('.', "")
            val fileName = if (extension.isNotEmpty()) "${Uuid.random()}.$extension" else Uuid.random().toString()

            val tempFile = File(directory, fileName)

            manager.open(assetPath).use { inStream ->
                tempFile.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            tempFile
        }
    }
}
