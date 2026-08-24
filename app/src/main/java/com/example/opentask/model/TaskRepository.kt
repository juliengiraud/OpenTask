package com.example.opentask.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

object TaskRepository {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> get() = _tasks

    var onTaskSaved: ((String, Long, Task) -> Unit)? = null
    var onTaskDeleted: ((String) -> Unit)? = null

    fun setTasks(newTasks: List<Task>) {
        _tasks.clear()
        _tasks.addAll(newTasks)
    }

    fun updateTask(context: Context, taskId: String, newRawContent: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        
        // Use taskId as filename for new tasks (since createEmptyTask uses it)
        val filename = if (index != -1) _tasks[index].filename else taskId
        
        // Use second-level precision for lastUpdate to match YAML format and avoid false external updates
        val now = java.time.LocalDateTime.now().withNano(0)
        
        val newTask = Task.fromRaw(filename, newRawContent).copy(
            id = taskId,
            lastUpdate = now
        )

        val isEmpty = newTask.title.isBlank() && newTask.textContent.isBlank()

        if (index != -1) {
            val oldTask = _tasks[index]
            if (isEmpty) {
                if (context is com.example.opentask.ui.MainActivity) {
                    context.addDebugLog("Memory: Deleting empty task ${oldTask.filename}")
                }
                _tasks.removeAt(index)
                deleteTaskFile(context, oldTask.filename)
            } else {
                // Optimization: Don't save if content hasn't changed (including YAML properties)
                if (oldTask.toRaw() == newRawContent) return
                
                if (context is com.example.opentask.ui.MainActivity) {
                    context.addDebugLog("Memory: Updating task ${newTask.filename}")
                }
                _tasks[index] = newTask
                saveTaskToFile(context, newTask)
            }
        } else if (!isEmpty) {
            // New task and not empty: add and save
            if (context is com.example.opentask.ui.MainActivity) {
                context.addDebugLog("Memory: Creating new task ${newTask.filename}")
            }
            _tasks.add(0, newTask)
            saveTaskToFile(context, newTask)
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
        val isNewFile = file == null
        if (file == null) {
            file = rootFolder.createFile("text/markdown", task.filename)
        }
        
        file?.let { f ->
            context.contentResolver.openOutputStream(f.uri, "wt")?.use { output ->
                output.write(task.toRaw().toByteArray())
            }
            if (context is com.example.opentask.ui.MainActivity) {
                val action = if (isNewFile) "Created" else "Updated"
                context.addDebugLog("File: $action ${task.filename}")
            }
            onTaskSaved?.invoke(task.filename, f.lastModified(), task)
        }
    }

    private fun deleteTaskFile(context: Context, filename: String) {
        val folderUriString = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("watched_folder", null) ?: return
        val folderUri = folderUriString.toUri()
        val rootFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return
        
        rootFolder.findFile(filename)?.delete()
        if (context is com.example.opentask.ui.MainActivity) {
            context.addDebugLog("File: Deleted $filename")
        }
        onTaskDeleted?.invoke(filename)
    }

    fun getTaskTitles(): List<String> = _tasks.map { it.title }

    fun createEmptyTask(dueDate: java.time.LocalDateTime? = null): Task {
        val now = java.time.LocalDateTime.now()
        val dateStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val filename = "$dateStr.md"
        
        return Task(
            id = filename,
            title = "",
            textContent = "",
            filename = filename,
            createdAt = now,
            lastUpdate = now,
            dueDate = dueDate
        )
    }
}
