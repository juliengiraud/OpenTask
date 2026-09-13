package com.example.opentask.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppConfig {
    var showWeekNumber by mutableStateOf(true)

    val DefaultBackgroundColor = Color(0xFFEEEEEE)
    val AddNoteButtonBackgroundColor = Color(0xFF4CAF50) // A nice green
    val AddNoteButtonIconColor = Color.White
    val TopPanelBackgroundColor = Color(0xFFD6D6D6)
    val SubPanelBackgroundColor = Color(0xFFE0E0E0)

    val NotesListBottomPadding = 64.dp

    val CalendarWeekdayColor = Color.Black
    val CalendarSaturdayColor = Color.Blue
    val CalendarSundayColor = Color.Red
    
    val CalendarGridLineColor = Color.DarkGray
    val CalendarOutOfMonthColor = Color(0xFFF5F5F5) // Lighter than DefaultBackgroundColor
    val CalendarCurrentDayBorderColor = Color(0xFF64B5F6)
    val CalendarCurrentDayBorderWidth = 2.5.dp

    val EditorFocusBorderColor = Color(0xFFFFA500) // Orange
}
