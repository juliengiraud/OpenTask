package com.example.opentask.ui

import com.example.opentask.R

enum class AppTabs(
    val label: String,
    val icon: Int,
) {
    HOME("Notes", R.drawable.ic_note),
    CALENDAR("Calendrier", R.drawable.ic_event),
    SETTINGS("Settings", R.drawable.ic_settings),
}
