package com.example.androidtaskapp.service

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

class FolderWatcherManager(
    private val context: Context,
    private val notificationManager: AppNotificationManager,
    private val debugManager: DebugManager,
    private val onStatusChanged: () -> Unit,
) {
    var lastEventInfo: String = "No changes yet"
        private set

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

            for (file in currentFiles) {
                val name = file.name ?: "Unknown"
                val lastModified = file.lastModified()
                newMetadata[name] = lastModified

                if (!fileMetadataMap.containsKey(name)) {
                    if (showPush && fileMetadataMap.isNotEmpty()) {
                        lastEventInfo = "Created: $name"
                        debugManager.log("FolderWatcherManager", lastEventInfo)
                        notificationManager.showPushNotification("File Created", name)
                        changeDetected = true
                    }
                } else if (fileMetadataMap[name]!! < lastModified) {
                    if (showPush) {
                        lastEventInfo = "Updated: $name"
                        debugManager.log("FolderWatcherManager", lastEventInfo)
                        notificationManager.showPushNotification("File Updated", name)
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
                        notificationManager.showPushNotification("File Deleted", oldName)
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
                onStatusChanged()
            }

        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Scan Error: ${e.message}")
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
