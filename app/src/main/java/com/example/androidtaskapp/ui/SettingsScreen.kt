package com.example.androidtaskapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onSelectFolder: () -> Unit,
    onResetWatcher: () -> Unit,
    watchedFolder: String?,
    debugLogs: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        FolderSelectionSection(
            onSelectFolder = onSelectFolder,
            onResetWatcher = onResetWatcher,
            watchedFolder = watchedFolder
        )

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        DebugPanel(
            debugLogs = debugLogs,
            modifier = Modifier.weight(1f)
        )
    }
}
