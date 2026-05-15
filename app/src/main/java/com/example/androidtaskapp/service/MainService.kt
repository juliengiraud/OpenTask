package com.example.androidtaskapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.androidtaskapp.R
import com.example.androidtaskapp.ui.PopupActivity

class MainService : Service() {

    private var taskCount = 0
    private val totalTasks = 5
    private val channelId = "task_channel_v5"
    private val notificationId = 1
    
    private lateinit var folderManager: FolderManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        folderManager = FolderManager(this, channelId) { updateNotification() }
        Log.d("MainService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INCREMENT -> {
                taskCount++
                updateNotification()
                Log.d("MainService", "Incremented taskCount to $taskCount")
            }
            ACTION_UPDATE_WATCHED_FOLDER -> {
                val folderUri = intent.getStringExtra(EXTRA_FOLDER_URI)
                folderManager.setupWatcher(folderUri)
            }
            ACTION_RESET_WATCHER -> {
                folderManager.reset()
            }
            else -> {
                val folderUri = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("watched_folder", null)
                folderManager.setupWatcher(folderUri)
                startForeground(notificationId, createNotification())
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        folderManager.stop()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Task Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Shows task progress"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
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
            .setContentText("Done: $taskCount/$totalTasks | ${folderManager.lastEventInfo}")
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
        const val ACTION_INCREMENT = "com.example.androidtaskapp.service.MainService.ACTION_INCREMENT"
        const val ACTION_UPDATE_WATCHED_FOLDER = "com.example.androidtaskapp.service.MainService.ACTION_UPDATE_WATCHED_FOLDER"
        const val ACTION_RESET_WATCHER = "com.example.androidtaskapp.service.MainService.ACTION_RESET_WATCHER"
        const val EXTRA_FOLDER_URI = "com.example.androidtaskapp.service.MainService.EXTRA_FOLDER_URI"
        const val ACTION_DEBUG_LOG = "com.example.androidtaskapp.service.MainService.ACTION_DEBUG_LOG"
        const val EXTRA_LOG_MESSAGE = "com.example.androidtaskapp.service.MainService.EXTRA_LOG_MESSAGE"
    }
}
