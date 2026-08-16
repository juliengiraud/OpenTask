package com.example.opentask.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

object TaskRepository {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> get() = _tasks

    fun setTasks(newTasks: List<Task>) {
        _tasks.clear()
        _tasks.addAll(newTasks)
    }

    fun updateTask(context: Context, taskId: String, newRawContent: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val oldTask = _tasks[index]
            
            // Optimization: Don't save if content hasn't changed
            if (oldTask.toRaw() == newRawContent) {
                return
            }

            val newTask = Task.fromRaw(oldTask.filename, newRawContent).copy(
                id = oldTask.id,
                lastUpdate = java.time.LocalDateTime.now()
            )
            
            if (newTask.title.isBlank() && newTask.textContent.isBlank()) {
                _tasks.removeAt(index)
                deleteTaskFile(context, oldTask.filename)
            } else {
                _tasks[index] = newTask
                saveTaskToFile(context, newTask)
            }
        }
    }

    fun deleteTask(context: Context, taskId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = _tasks[index]
            _tasks.removeAt(index)
            deleteTaskFile(context, task.filename)
        }
    }

    private fun saveTaskToFile(context: Context, task: Task) {
        val folderUriString = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("watched_folder", null) ?: return
        val folderUri = folderUriString.toUri()
        val rootFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return
        
        var file = rootFolder.findFile(task.filename)
        if (file == null) {
            file = rootFolder.createFile("text/markdown", task.filename)
        }
        
        file?.let {
            context.contentResolver.openOutputStream(it.uri, "wt")?.use { output ->
                output.write(task.toRaw().toByteArray())
            }
        }
    }

    private fun deleteTaskFile(context: Context, filename: String) {
        val folderUriString = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("watched_folder", null) ?: return
        val folderUri = folderUriString.toUri()
        val rootFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return
        
        rootFolder.findFile(filename)?.delete()
    }

    fun getTaskTitles(): List<String> = _tasks.map { it.title }

    fun createEmptyTask(): Task {
        val now = java.time.LocalDateTime.now()
        val dateStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val filename = "$dateStr.md"
        
        val newTask = Task(
            id = filename,
            title = "",
            textContent = "",
            filename = filename,
            createdAt = now,
            lastUpdate = now
        )
        _tasks.add(0, newTask) // Add to the top of the list
        return newTask
    }
}
