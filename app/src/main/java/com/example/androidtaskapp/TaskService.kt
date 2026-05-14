package com.example.androidtaskapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TaskService : Service() {

    private var taskCount = 0
    private val totalTasks = 5
    private val channelId = "task_channel"
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows task progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val incrementIntent = Intent(this, TaskService::class.java).apply {
            action = ACTION_INCREMENT
        }
        
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            incrementIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("My task app title")
            .setContentText("Current tasks done $taskCount/$totalTasks")
            .setSmallIcon(R.drawable.ic_home) // Using existing icon
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, createNotification())
    }

    companion object {
        const val ACTION_INCREMENT = "com.example.androidtaskapp.ACTION_INCREMENT"
    }
}
