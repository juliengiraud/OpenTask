package com.example.androidtaskapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.androidtaskapp.model.Task
import java.time.LocalDateTime

@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    val mockTasks = remember {
        val dueDate = LocalDateTime.of(2026, 5, 14, 0, 0)
        listOf(
            Task(title = "Courses", textContent = "Courses", dueDate = dueDate),
            Task(title = "Plantes", textContent = "Plantes", dueDate = dueDate),
            Task(title = "Sortie Magda", textContent = "Sortie Magda", dueDate = dueDate)
        )
    }

    Column(
        modifier = modifier
            .background(Color(0xFFEEEEEE)) // Light grey background
            .padding(8.dp)
    ) {
        // Filters panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0)) // More grey than background
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Filters", style = MaterialTheme.typography.titleMedium, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Task list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        ) {
            items(mockTasks) { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White) // Background for each task row
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                    Checkbox(checked = task.isDone, onCheckedChange = { })
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
