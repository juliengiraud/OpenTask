package com.example.androidtaskapp.model

import java.time.LocalDateTime
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val textContent: String,
    val filename: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDateTime? = null,
    val hasTime: Boolean = false,
    val duration: TaskDuration? = null,
    val isDone: Boolean = false
)

data class TaskDuration(
    val days: Int = 0,
    val hours: Int = 0,
    val minutes: Int = 0
)
