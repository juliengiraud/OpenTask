package com.example.opentask.util

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    /**
     * Returns today's date in the format: {3 first letters of the day}. {number of day in month} {name of the month}
     * Example: "Wed. 17 July"
     */
    fun getTodayFormattedDate(): String {
        val today = LocalDate.now()
        val locale = Locale.getDefault()
        val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val dayAbbr = dayName.take(3)
        val monthName = today.month.getDisplayName(TextStyle.FULL, locale)
        return "$dayAbbr. ${today.dayOfMonth} $monthName"
    }
}
