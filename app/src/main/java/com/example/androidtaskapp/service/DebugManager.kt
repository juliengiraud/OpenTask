package com.example.androidtaskapp.service

import android.content.Context
import android.content.Intent
import android.util.Log

class DebugManager(private val context: Context) {

    fun log(tag: String, message: String) {
        Log.d(tag, message)
        val intent = Intent(MainService.ACTION_DEBUG_LOG).apply {
            setPackage(context.packageName)
            putExtra(MainService.EXTRA_LOG_MESSAGE, "[$tag] $message")
        }
        context.sendBroadcast(intent)
    }
}
