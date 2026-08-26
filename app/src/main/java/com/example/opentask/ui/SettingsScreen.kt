package com.example.opentask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onSelectFolder: () -> Unit,
    onResetWatcher: () -> Unit,
    onToggleWeekNumber: (Boolean) -> Unit,
    onToggleLogPeriodicScan: (Boolean) -> Unit,
    watchedFolder: String?,
    debugLogs: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(AppConfig.DefaultBackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        FolderSelectionSection(
            onSelectFolder = onSelectFolder,
            onResetWatcher = onResetWatcher,
            watchedFolder = watchedFolder
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Calendar", style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Display week number", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = AppConfig.showWeekNumber,
                onCheckedChange = onToggleWeekNumber
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Debug Informations", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Log periodic scan", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = AppConfig.logPeriodicScan,
                onCheckedChange = onToggleLogPeriodicScan
            )
        }

        // Logs section (part of the global scroll)
        Spacer(modifier = Modifier.height(16.dp))
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        DebugPanel(
            debugLogs = debugLogs,
            modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight * 0.5f)
        )
    }
}
