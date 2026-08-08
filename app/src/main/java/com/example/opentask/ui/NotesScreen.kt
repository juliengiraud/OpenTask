package com.example.opentask.ui

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
import com.example.opentask.model.Task
import com.example.opentask.model.TaskRepository

@Composable
fun NotesScreen(
    modifier: Modifier = Modifier
) {
    val tasks = TaskRepository.tasks

    Column(
        modifier = modifier
            .background(AppConfig.DefaultBackgroundColor)
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
            tasks = tasks,
            modifier = Modifier.weight(1f)
        )
    }
}
