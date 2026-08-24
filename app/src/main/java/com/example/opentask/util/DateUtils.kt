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
        return formatDate(LocalDate.now())
    }

    /**
     * Returns the date in the format: {3 first letters of the day}. {number of day in month} {name of the month}
     * Example: "Wed. 17 July"
     */
    fun formatDate(date: LocalDate): String {
        val locale = Locale.getDefault()
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val dayAbbr = dayName.take(3)
        val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
        return "$dayAbbr. ${date.dayOfMonth} $monthName"
    }

    /**
     * Returns the month and year of the given date in localized format.
     * Example: "Octobre 2023"
     */
    fun formatMonthYear(date: LocalDate): String {
        val locale = Locale.getDefault()
        val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
        return "$monthName ${date.year}"
    }

    /**
     * Returns the current month and year in localized format.
     * Example: "Octobre 2023"
     */
    fun getCurrentMonthYear(): String {
        return formatMonthYear(LocalDate.now())
    }
}
