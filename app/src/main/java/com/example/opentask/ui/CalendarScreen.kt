package com.example.opentask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.opentask.util.DateUtils

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppConfig.DefaultBackgroundColor)
            .padding(8.dp)
    ) {
        SubTopPanel {
            Text(
                text = DateUtils.getCurrentMonthYear(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
        }
    }
}
