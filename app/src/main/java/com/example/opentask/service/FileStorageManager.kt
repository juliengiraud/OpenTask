package com.example.opentask.service

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object FileStorageManager {

    fun readFileContent(context: Context, fileUri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun saveFileContent(context: Context, folderUri: Uri, filename: String, content: String): Boolean {
        return try {
            val rootFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
            var file = rootFolder.findFile(filename)
            if (file == null) {
                file = rootFolder.createFile("text/markdown", filename)
            }
            file?.let { f ->
                context.contentResolver.openOutputStream(f.uri, "wt")?.use { output ->
                    output.write(content.toByteArray())
                }
                return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFile(context: Context, folderUri: Uri, filename: String): Boolean {
        return try {
            val rootFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
            val file = rootFolder.findFile(filename)
            file?.delete() ?: false
        } catch (e: Exception) {
            false
        }
    }
}
