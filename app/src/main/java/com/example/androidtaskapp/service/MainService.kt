package com.example.androidtaskapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.androidtaskapp.model.TaskRepository

class MainService : Service() {

    private lateinit var folderWatcherManager: FolderWatcherManager
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var debugManager: DebugManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        debugManager = DebugManager(this)
        notificationManager = AppNotificationManager(this)
        folderWatcherManager = FolderWatcherManager(this, debugManager) { updateNotification() }
        
        debugManager.log("MainService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INCREMENT -> {
                debugManager.log("MainService", "ACTION_INCREMENT no longer supported with file watcher")
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
                    notificationManager.getForegroundNotification(TaskRepository.getTaskTitles()),
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
        notificationManager.updateForegroundNotification(TaskRepository.getTaskTitles())
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
