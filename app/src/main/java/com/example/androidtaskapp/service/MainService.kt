package com.example.androidtaskapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MainService : Service() {

    private var taskCount = 0
    private val totalTasks = 5
    
    private lateinit var folderWatcherManager: FolderWatcherManager
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var debugManager: DebugManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        debugManager = DebugManager(this)
        notificationManager = AppNotificationManager(this)
        folderWatcherManager = FolderWatcherManager(this, notificationManager, debugManager) { updateNotification() }
        
        debugManager.log("MainService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INCREMENT -> {
                taskCount++
                updateNotification()
                debugManager.log("MainService", "Incremented taskCount to $taskCount")
            }
            ACTION_UPDATE_WATCHED_FOLDER -> {
                val folderUri = intent.getStringExtra(EXTRA_FOLDER_URI)
                folderWatcherManager.setupWatcher(folderUri)
            }
            ACTION_RESET_WATCHER -> {
                folderWatcherManager.reset()
            }
            else -> {
                val folderUri = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("watched_folder", null)
                folderWatcherManager.setupWatcher(folderUri)
                
                startForeground(
                    notificationManager.getForegroundId(),
                    notificationManager.getForegroundNotification(
                        taskCount,
                        totalTasks,
                        folderWatcherManager.lastEventInfo,
                    ),
                )
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        folderWatcherManager.stop()
    }

    private fun updateNotification() {
        notificationManager.updateForegroundNotification(taskCount, totalTasks, folderWatcherManager.lastEventInfo)
    }

    companion object {
        const val ACTION_INCREMENT = "com.example.androidtaskapp.service.MainService.ACTION_INCREMENT"
        const val ACTION_UPDATE_WATCHED_FOLDER = "com.example.androidtaskapp.service.MainService.ACTION_UPDATE_WATCHED_FOLDER"
        const val ACTION_RESET_WATCHER = "com.example.androidtaskapp.service.MainService.ACTION_RESET_WATCHER"
        const val EXTRA_FOLDER_URI = "com.example.androidtaskapp.service.MainService.EXTRA_FOLDER_URI"
        const val ACTION_DEBUG_LOG = "com.example.androidtaskapp.service.MainService.ACTION_DEBUG_LOG"
        const val EXTRA_LOG_MESSAGE = "com.example.androidtaskapp.service.MainService.EXTRA_LOG_MESSAGE"
    }
}
