package com.example.androidtaskapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.androidtaskapp.service.TaskService
import com.example.androidtaskapp.ui.theme.AndroidTaskAppTheme

class PopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Increment task count via service
        val incrementIntent = Intent(this, TaskService::class.java).apply {
            action = TaskService.ACTION_INCREMENT
        }
        startService(incrementIntent)

        setContent {
            AndroidTaskAppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { finish() }, // Close when clicking outside
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.5f)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .clickable(enabled = false) { }, // Prevent closing when clicking inside
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Popup Title",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
