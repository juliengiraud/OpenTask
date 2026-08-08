package com.example.opentask.ui

import com.example.opentask.R

enum class AppTabs(
    val label: String,
    val icon: Int,
) {
    HOME("Notes", R.drawable.ic_event),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    SETTINGS("Settings", R.drawable.ic_settings),
}
