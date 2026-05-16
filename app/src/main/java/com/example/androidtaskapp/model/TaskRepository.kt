package com.example.androidtaskapp.model

import java.time.LocalDateTime

object TaskRepository {
    val mockTasks = listOf(
        Task(title = "Courses", textContent = "Courses", dueDate = LocalDateTime.of(2026, 5, 14, 0, 0)),
        Task(title = "Plantes", textContent = "Plantes", dueDate = LocalDateTime.of(2026, 5, 14, 0, 0)),
        Task(title = "Sortie Magda", textContent = "Sortie Magda", dueDate = LocalDateTime.of(2026, 5, 14, 0, 0))
    )

    fun getTaskTitles(): List<String> = mockTasks.map { it.title }
}
