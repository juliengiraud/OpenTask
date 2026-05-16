package com.example.androidtaskapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FolderSelectionSection(
    onSelectFolder: () -> Unit,
    onResetWatcher: () -> Unit,
    watchedFolder: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "Watched Folder:", style = MaterialTheme.typography.titleMedium)
        Text(text = watchedFolder ?: "Not selected", style = MaterialTheme.typography.bodyMedium)
        
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = onSelectFolder, modifier = Modifier.fillMaxWidth()) {
                Text("Select Folder")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onResetWatcher, modifier = Modifier.fillMaxWidth()) {
                Text("Reset Watcher")
            }
        }
    }
}
