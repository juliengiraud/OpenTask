package com.example.opentask.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.opentask.model.TaskRepository
import com.example.opentask.service.MainService
import com.example.opentask.ui.theme.OpenTaskTheme
import com.example.opentask.util.DateUtils

class PopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Increment task count via service
        val incrementIntent = Intent(this, MainService::class.java).apply {
            action = MainService.ACTION_INCREMENT
        }
        startService(incrementIntent)

        val formattedDate = DateUtils.getTodayFormattedDate()

        setContent {
            OpenTaskTheme {
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
                            .background(AppConfig.DefaultBackgroundColor, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .clickable(enabled = false) { }, // Prevent closing when clicking inside
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        NotesList(
                            tasks = TaskRepository.tasks,
                            onTaskClick = { /* Maybe open MainActivity or just ignore for now */ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
