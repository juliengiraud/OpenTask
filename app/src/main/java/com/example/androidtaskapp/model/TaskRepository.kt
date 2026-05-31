package com.example.androidtaskapp.model

import androidx.compose.runtime.mutableStateListOf

object TaskRepository {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> get() = _tasks

    fun setTasks(newTasks: List<Task>) {
        _tasks.clear()
        _tasks.addAll(newTasks)
    }

    fun getTaskTitles(): List<String> = _tasks.map { it.title }
}
