package com.example.androidtaskapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import com.example.androidtaskapp.R
import com.example.androidtaskapp.ui.PopupActivity

class TaskService : Service() {

    private var taskCount = 0
    private val totalTasks = 5
    private val channelId = "task_channel_v5"
    private val notificationId = 1
    
    private var fileMetadataMap = mutableMapOf<String, Long>()
    private var lastEventInfo = "No changes yet"
    
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sendDebugLog("Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INCREMENT -> {
                taskCount++
                updateNotification()
                sendDebugLog("Incremented taskCount to $taskCount")
            }
            ACTION_UPDATE_WATCHED_FOLDER -> {
                val folderUri = intent.getStringExtra(EXTRA_FOLDER_URI)
                setupWatcher(folderUri)
            }
            ACTION_RESET_WATCHER -> {
                sendDebugLog("Watcher reset.")
                setupWatcher(null)
                fileMetadataMap.clear()
                lastEventInfo = "Watcher reset"
                updateNotification()
            }
            else -> {
                val folderUri = getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getString("watched_folder", null)
                setupWatcher(folderUri)
                startForeground(notificationId, createNotification())
            }
        }
        return START_STICKY
    }

    private fun setupWatcher(folderUriString: String?) {
        folderContentObserver?.let { contentResolver.unregisterContentObserver(it) }
        folderContentObserver = null
        pollingHandler.removeCallbacks(pollRunnable)
        currentWatchedUri = null

        if (folderUriString == null) return

        val uri = Uri.parse(folderUriString)
        currentWatchedUri = uri
        sendDebugLog("Monitoring: $uri")

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, updatedUri: Uri?) {
                sendDebugLog("System event triggered scan")
                scanFolder(uri, showPush = true)
            }
        }
        contentResolver.registerContentObserver(uri, true, observer)
        folderContentObserver = observer
        
        scanFolder(uri, showPush = false) // Initial state
        pollingHandler.postDelayed(pollRunnable, 5000)
    }

    private fun scanFolder(treeUri: Uri, showPush: Boolean) {
        try {
            val folder = DocumentFile.fromTreeUri(this, treeUri)
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
                fileMetadataMap.clear()
                fileMetadataMap.putAll(newMetadata)
                updateNotification()
            }
            
        } catch (e: Exception) {
            sendDebugLog("Scan Error: ${e.message}")
        }
    }

    private fun showPushNotification(title: String, text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val pushNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_home)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 1, pushNotification)
    }

    private fun sendDebugLog(message: String) {
        Log.d("TaskService", message)
        val intent = Intent(ACTION_DEBUG_LOG).apply {
            setPackage(packageName)
            putExtra(EXTRA_LOG_MESSAGE, message)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingHandler.removeCallbacks(pollRunnable)
        folderContentObserver?.let { contentResolver.unregisterContentObserver(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows task progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val popupIntent = Intent(this, PopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("My task app title")
            .setContentText("Done: $taskCount/$totalTasks | $lastEventInfo")
            .setSmallIcon(R.drawable.ic_home)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, createNotification())
    }

    companion object {
        const val ACTION_INCREMENT = "com.example.androidtaskapp.service.ACTION_INCREMENT"
        const val ACTION_UPDATE_WATCHED_FOLDER = "com.example.androidtaskapp.service.ACTION_UPDATE_WATCHED_FOLDER"
        const val ACTION_RESET_WATCHER = "com.example.androidtaskapp.service.ACTION_RESET_WATCHER"
        const val EXTRA_FOLDER_URI = "com.example.androidtaskapp.service.EXTRA_FOLDER_URI"
        const val ACTION_DEBUG_LOG = "com.example.androidtaskapp.service.ACTION_DEBUG_LOG"
        const val EXTRA_LOG_MESSAGE = "com.example.androidtaskapp.service.EXTRA_LOG_MESSAGE"
    }
}
