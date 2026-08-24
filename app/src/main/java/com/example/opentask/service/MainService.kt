package com.example.opentask.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.opentask.model.TaskRepository

class MainService : Service() {

    private lateinit var folderWatcherManager: FolderWatcherManager
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var debugManager: DebugManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        debugManager = DebugManager(this)
        notificationManager = AppNotificationManager(this)
        folderWatcherManager = FolderWatcherManager(this, debugManager) { changedTasks ->
            val today = java.time.LocalDate.now()
            if (changedTasks.isEmpty() || changedTasks.any { it.dueDate?.toLocalDate() == today }) {
                updateNotification()
            }
        }
        
        TaskRepository.onTaskSaved = { name, lastModified, task ->
            folderWatcherManager.updateCache(name, lastModified, task)
            if (task.dueDate?.toLocalDate() == java.time.LocalDate.now()) {
                updateNotification()
            }
        }
        TaskRepository.onTaskDeleted = { name, task ->
            folderWatcherManager.removeFromCache(name)
            if (task.dueDate?.toLocalDate() == java.time.LocalDate.now()) {
                updateNotification()
            }
        }

        debugManager.log("MainService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
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
                    notificationManager.getForegroundNotification(TaskRepository.getTodaysTaskTitles()),
                )
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        folderWatcherManager.stop()
        TaskRepository.onTaskSaved = null
        TaskRepository.onTaskDeleted = null
    }

    private fun updateNotification() {
        notificationManager.updateForegroundNotification(TaskRepository.getTodaysTaskTitles())
    }

    companion object {
        const val ACTION_UPDATE_WATCHED_FOLDER = "com.example.opentask.service.MainService.ACTION_UPDATE_WATCHED_FOLDER"
        const val ACTION_RESET_WATCHER = "com.example.opentask.service.MainService.ACTION_RESET_WATCHER"
        const val EXTRA_FOLDER_URI = "com.example.opentask.service.MainService.EXTRA_FOLDER_URI"
        const val ACTION_DEBUG_LOG = "com.example.opentask.service.MainService.ACTION_DEBUG_LOG"
        const val EXTRA_LOG_MESSAGE = "com.example.opentask.service.MainService.EXTRA_LOG_MESSAGE"
    }
}
