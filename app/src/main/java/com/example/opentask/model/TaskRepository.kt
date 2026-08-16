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
            val oldTask = _tasks[index]
            // newContent is expected to be raw content
            _tasks[index] = Task.fromRaw(oldTask.filename, newContent).copy(
                id = oldTask.id,
                lastUpdate = java.time.LocalDateTime.now()
            )
        }
    }

    fun getTaskTitles(): List<String> = _tasks.map { it.title }

    fun createEmptyTask(): Task {
        val now = java.time.LocalDateTime.now()
        val dateStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val filename = "$dateStr.md"
        val defaultYaml = "---\ncreation_date: $dateStr\n---"
        
        val newTask = Task(
            title = "",
            textContent = "",
            filename = filename,
            createdAt = now,
            lastUpdate = now,
            yaml = defaultYaml
        )
        _tasks.add(0, newTask) // Add to the top of the list
        return newTask
    }
}
