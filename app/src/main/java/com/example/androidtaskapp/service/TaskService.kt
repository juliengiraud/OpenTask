package com.example.androidtaskapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.androidtaskapp.R
import com.example.androidtaskapp.ui.PopupActivity

class TaskService : Service() {

    private var taskCount = 0
    private val totalTasks = 5
    private val channelId = "task_channel_v2"
    private val notificationId = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_INCREMENT) {
            taskCount++
            updateNotification()
        } else {
            startForeground(notificationId, createNotification())
        }
        return START_STICKY
    }
    private fun sendDebugLog(message: String) {
        val intent = Intent(ACTION_DEBUG_LOG).apply {
            putExtra(EXTRA_LOG_MESSAGE, message)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        sendDebugLog("Service destroyed, stopped watching")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows task progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            sendDebugLog("Notification channel created")
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
            .setContentText("Current tasks done $taskCount/$totalTasks")
            .setSmallIcon(R.drawable.ic_home)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, createNotification())
    }

    companion object {
        const val ACTION_INCREMENT = "com.example.androidtaskapp.service.ACTION_INCREMENT"
        const val ACTION_DEBUG_LOG = "com.example.androidtaskapp.service.ACTION_DEBUG_LOG"
    }
}
