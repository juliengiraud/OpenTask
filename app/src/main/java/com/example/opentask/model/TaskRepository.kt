package com.example.opentask.model

import androidx.compose.runtime.mutableStateListOf

object TaskRepository {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> get() = _tasks

    fun setTasks(newTasks: List<Task>) {
        _tasks.clear()
        _tasks.addAll(newTasks)
    }

    fun updateTask(taskId: String, newContent: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = _tasks[index]
            _tasks[index] = task.copy(textContent = newContent, lastUpdate = java.time.LocalDateTime.now())
        }
    }

    fun getTaskTitles(): List<String> = _tasks.map { it.title }
}
