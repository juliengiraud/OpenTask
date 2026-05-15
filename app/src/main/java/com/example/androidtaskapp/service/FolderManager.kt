package com.example.androidtaskapp.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.androidtaskapp.R

class FolderManager(
    private val context: Context,
    private val channelId: String,
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
            sendDebugLog("Monitoring: $uri")

            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, updatedUri: Uri?) {
                    sendDebugLog("System event triggered scan")
                    scanFolder(uri, showPush = true)
                }
            }
            context.contentResolver.registerContentObserver(uri, true, observer)
            folderContentObserver = observer

            scanFolder(uri, showPush = false) // Initial state
            pollingHandler.postDelayed(pollRunnable, 5000)
        } catch (e: Exception) {
            sendDebugLog("Setup Error: ${e.message}")
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
                        sendDebugLog(lastEventInfo)
                        showPushNotification("File Created", name)
                        changeDetected = true
                    }
                } else if (fileMetadataMap[name]!! < lastModified) {
                    if (showPush) {
                        lastEventInfo = "Updated: $name"
                        sendDebugLog(lastEventInfo)
                        showPushNotification("File Updated", name)
                        changeDetected = true
                    }
                }
            }

            // Detect Deletions
            for (oldName in fileMetadataMap.keys) {
                if (!newMetadata.containsKey(oldName)) {
                    if (showPush) {
                        lastEventInfo = "Deleted: $oldName"
                        sendDebugLog(lastEventInfo)
                        showPushNotification("File Deleted", oldName)
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
            sendDebugLog("Scan Error: ${e.message}")
        }
    }

    fun reset() {
        sendDebugLog("Watcher reset.")
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

    private fun showPushNotification(title: String, text: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val pushNotification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_home)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        // Use a unique ID based on message content to avoid collisions while allowing updates
        notificationManager.notify(text.hashCode(), pushNotification)
    }

    private fun sendDebugLog(message: String) {
        Log.d("FolderManager", message)
        val intent = Intent(MainService.ACTION_DEBUG_LOG).apply {
            setPackage(context.packageName)
            putExtra(MainService.EXTRA_LOG_MESSAGE, message)
        }
        context.sendBroadcast(intent)
    }
}
