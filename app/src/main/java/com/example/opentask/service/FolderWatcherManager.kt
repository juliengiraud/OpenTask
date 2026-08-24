package com.example.opentask.service

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.example.opentask.model.Task
import com.example.opentask.model.TaskRepository
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
    private var taskCache = mutableMapOf<String, Task>()
    private var folderContentObserver: ContentObserver? = null
    private val pollingHandler = Handler(Looper.getMainLooper())
    private var currentWatchedUri: Uri? = null
    private var currentChildrenUri: Uri? = null

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
        taskCache.clear()

        if (folderUriString == null) {
            lastEventInfo = "No folder selected"
            onStatusChanged()
            return
        }

        try {
            val uri = folderUriString.toUri()
            currentWatchedUri = uri
            val documentId = DocumentsContract.getTreeDocumentId(uri)
            currentChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)
            
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
        val childrenUri = currentChildrenUri ?: return
        try {
            val cursor = context.contentResolver.query(childrenUri, PROJECTION, null, null, null)
            val newMetadata = mutableMapOf<String, Long>()
            var changeDetected = false
            val loadedTasks = mutableListOf<Task>()

            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val lastModIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

                if (nameIndex != -1 && lastModIndex != -1 && idIndex != -1) {
                    while (c.moveToNext()) {
                        val name = c.getString(nameIndex) ?: continue
                        val lastModified = c.getLong(lastModIndex)
                        val docId = c.getString(idIndex)
                        
                        newMetadata[name] = lastModified

                        if (name.endsWith(".md")) {
                            val cachedTask = taskCache[name]
                            if (cachedTask != null && fileMetadataMap[name] == lastModified) {
                                loadedTasks.add(cachedTask)
                            } else {
                                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                loadTaskFromUri(fileUri, name, lastModified)?.let { task ->
                                    taskCache[name] = task
                                    loadedTasks.add(task)
                                }
                            }
                        }

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
                        } else if (fileMetadataMap[name]!! > lastModified) {
                            // This can happen if file system clock is slightly different or file was replaced with an older version
                            if (showPush) {
                                lastEventInfo = "Externally Replaced: $name"
                                debugManager.log("FolderWatcherManager", lastEventInfo)
                                changeDetected = true
                            }
                        }
                    }
                }
            }

            // Detect Deletions
            for (oldName in fileMetadataMap.keys) {
                if (!newMetadata.containsKey(oldName)) {
                    taskCache.remove(oldName)
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

    private fun loadTaskFromUri(uri: Uri, name: String, lastModified: Long): Task? {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return null

            Task.fromRaw(name, content).copy(
                lastUpdate = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastModified),
                    ZoneId.systemDefault()
                )
            )
        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Error reading $name: ${e.message}")
            null
        }
    }

    fun updateCache(name: String, lastModified: Long, task: Task) {
        debugManager.log("FolderWatcherManager", "Cache synced for $name")
        fileMetadataMap[name] = lastModified
        taskCache[name] = task
    }

    fun removeFromCache(name: String) {
        fileMetadataMap.remove(name)
        taskCache.remove(name)
    }

    fun reset() {
        debugManager.log("FolderWatcherManager", "Watcher reset.")
        setupWatcher(null)
        fileMetadataMap.clear()
        taskCache.clear()
        lastEventInfo = "Watcher reset"
        onStatusChanged()
    }

    fun stop() {
        pollingHandler.removeCallbacks(pollRunnable)
        folderContentObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        folderContentObserver = null
        currentChildrenUri = null
    }

    companion object {
        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        )
    }
}
