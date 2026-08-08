package com.example.opentask.ui

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AddNoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = AppConfig.AddNoteButtonBackgroundColor,
        contentColor = AppConfig.AddNoteButtonIconColor,
        modifier = modifier
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
