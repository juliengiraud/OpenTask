package com.example.opentask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun TopPanel(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(elevation = 4.dp)
            .zIndex(1f)
            .background(Color(0xFFD6D6D6)) // Lighter grey
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp, top = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = "Hello $title",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
