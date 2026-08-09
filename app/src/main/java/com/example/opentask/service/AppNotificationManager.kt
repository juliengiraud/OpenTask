package com.example.opentask.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import androidx.core.app.NotificationCompat
import com.example.opentask.R
import com.example.opentask.ui.PopupActivity

class AppNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val channelId = "task_channel_v7"
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
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun getForegroundNotification(taskNames: List<String>): Notification {
        val popupIntent = Intent(context, PopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val taskCount = taskNames.size
        val joinedNames = taskNames.joinToString(" / ")
        val titleText = "[$taskCount] $joinedNames"
        
        val spannableTitle = SpannableString(titleText).apply {
            // Standard title is ~16sp, aiming for ~14sp (roughly 2sp/dp smaller)
            setSpan(AbsoluteSizeSpan(14, true), 0, titleText.length, 0)
        }

        return NotificationCompat.Builder(context, channelId)
            .setSubText("Tâches pour aujourd'hui")
            .setContentTitle(spannableTitle)
            .setContentText(joinedNames)
            .setSmallIcon(R.drawable.ic_settings)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun updateForegroundNotification(taskNames: List<String>) {
        notificationManager.notify(foregroundId, getForegroundNotification(taskNames))
    }

    fun showPushNotification(title: String, text: String) {
        val pushNotification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_settings)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(text.hashCode(), pushNotification)
    }

    fun getForegroundId() = foregroundId
}
