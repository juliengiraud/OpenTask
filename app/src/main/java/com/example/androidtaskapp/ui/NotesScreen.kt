package com.example.androidtaskapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
        SubTopPanel {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        NotesList(
            tasks = mockTasks,
            modifier = Modifier.weight(1f)
        )
    }
}
