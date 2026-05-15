package com.example.androidtaskapp.service

import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugManager(private val context: Context) {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun log(tag: String, message: String) {
        val timestamp = timeFormat.format(Date())
        val formattedMessage = "[$timestamp] [$tag] $message"
        
        Log.d(tag, formattedMessage)
        val intent = Intent(MainService.ACTION_DEBUG_LOG).apply {
            setPackage(context.packageName)
            putExtra(MainService.EXTRA_LOG_MESSAGE, formattedMessage)
        }
        context.sendBroadcast(intent)
    }
}
