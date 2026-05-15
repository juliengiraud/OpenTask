package com.example.androidtaskapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.androidtaskapp.R
import com.example.androidtaskapp.ui.PopupActivity

class AppNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val channelId = "task_channel_v5"
    private val foregroundId = 1

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Task Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Shows task progress"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun getForegroundNotification(taskCount: Int, totalTasks: Int, lastEventInfo: String): Notification {
        val popupIntent = Intent(context, PopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("My task app title")
            .setContentText("Done: $taskCount/$totalTasks | $lastEventInfo")
            .setSmallIcon(R.drawable.ic_home)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    fun updateForegroundNotification(taskCount: Int, totalTasks: Int, lastEventInfo: String) {
        notificationManager.notify(foregroundId, getForegroundNotification(taskCount, totalTasks, lastEventInfo))
    }

    fun showPushNotification(title: String, text: String) {
        val pushNotification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_home)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(text.hashCode(), pushNotification)
    }

    fun getForegroundId() = foregroundId
}
