package com.example.androidtaskapp.service

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.androidtaskapp.model.Task
import com.example.androidtaskapp.model.TaskRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class FolderWatcherManager(
    private val context: Context,
    private val debugManager: DebugManager,
    private val onStatusChanged: () -> Unit,
) {
    var lastEventInfo: String = "No changes yet"
        private set

    val activeTasks: List<String>
        get() = fileMetadataMap.keys.toList()

    private var fileMetadataMap = mutableMapOf<String, Long>()
    private var folderContentObserver: ContentObserver? = null
    private val pollingHandler = Handler(Looper.getMainLooper())
    private var currentWatchedUri: Uri? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            currentWatchedUri?.let {
                scanFolder(it, showPush = true)
            }
            pollingHandler.postDelayed(this, 5000)
        }
    }

    fun setupWatcher(folderUriString: String?) {
        stop()
        currentWatchedUri = null

        if (folderUriString == null) {
            lastEventInfo = "No folder selected"
            onStatusChanged()
            return
        }

        try {
            val uri = folderUriString.toUri()
            currentWatchedUri = uri
            debugManager.log("FolderWatcherManager", "Monitoring: $uri")

            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, updatedUri: Uri?) {
                    debugManager.log("FolderWatcherManager", "System event triggered scan")
                    scanFolder(uri, showPush = true)
                }
            }
            context.contentResolver.registerContentObserver(uri, true, observer)
            folderContentObserver = observer

            scanFolder(uri, showPush = false) // Initial state
            pollingHandler.postDelayed(pollRunnable, 5000)
        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Setup Error: ${e.message}")
        }
    }

    private fun scanFolder(treeUri: Uri, showPush: Boolean) {
        try {
            val folder = DocumentFile.fromTreeUri(context, treeUri)
            val currentFiles = folder?.listFiles() ?: emptyArray()

            val newMetadata = mutableMapOf<String, Long>()
            var changeDetected = false
            val loadedTasks = mutableListOf<Task>()

            for (file in currentFiles) {
                if (file.name?.endsWith(".md") == true) {
                    loadTaskFromFile(file)?.let { loadedTasks.add(it) }
                }

                val name = file.name ?: "Unknown"
                val lastModified = file.lastModified()
                newMetadata[name] = lastModified

                if (!fileMetadataMap.containsKey(name)) {
                    if (showPush && fileMetadataMap.isNotEmpty()) {
                        lastEventInfo = "Created: $name"
                        debugManager.log("FolderWatcherManager", lastEventInfo)
                        changeDetected = true
                    }
                } else if (fileMetadataMap[name]!! < lastModified) {
                    if (showPush) {
                        lastEventInfo = "Updated: $name"
                        debugManager.log("FolderWatcherManager", lastEventInfo)
                        changeDetected = true
                    }
                }
            }

            // Detect Deletions
            for (oldName in fileMetadataMap.keys) {
                if (!newMetadata.containsKey(oldName)) {
                    if (showPush) {
                        lastEventInfo = "Deleted: $oldName"
                        debugManager.log("FolderWatcherManager", lastEventInfo)
                        changeDetected = true
                    }
                }
            }

            if (changeDetected || fileMetadataMap.isEmpty()) {
                if (fileMetadataMap.isEmpty() && newMetadata.isEmpty()) {
                    lastEventInfo = "Folder empty"
                }
                fileMetadataMap.clear()
                fileMetadataMap.putAll(newMetadata)
                TaskRepository.setTasks(loadedTasks)
                onStatusChanged()
            }

        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Scan Error: ${e.message}")
        }
    }

    private fun loadTaskFromFile(file: DocumentFile): Task? {
        return try {
            val content = context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return null

            val lines = content.lines()
            val titleRegex = Regex("^#+ *")
            val title = lines.find { it.trim().startsWith("#") }
                ?.let { it.trim().replace(titleRegex, "") }
                ?: file.name?.removeSuffix(".md") ?: "Unknown"

            Task(
                title = title,
                textContent = content,
                filename = file.name ?: "Unknown",
                lastUpdate = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(file.lastModified()),
                    ZoneId.systemDefault()
                )
            )
        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Error reading ${file.name}: ${e.message}")
            null
        }
    }

    fun reset() {
        debugManager.log("FolderWatcherManager", "Watcher reset.")
        setupWatcher(null)
        fileMetadataMap.clear()
        lastEventInfo = "Watcher reset"
        onStatusChanged()
    }

    fun stop() {
        pollingHandler.removeCallbacks(pollRunnable)
        folderContentObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        folderContentObserver = null
    }
}
