package com.example.opentask.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TaskRepository {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> get() = _tasks
    
    // Index for fast lookup by date (due_date if set, otherwise creation_date)
    private val _tasksByDate = mutableStateMapOf<LocalDate, SnapshotStateList<Task>>()

    // In-memory mutation callback hooks to decouple filesystem saving from repository operations
    var onTaskChangedInMemory: ((Task, isDeleted: Boolean, oldTask: Task?) -> Unit)? = null

    // Kept temporary properties to avoid breaking MainService / UI listeners before Step 5 complete integration
    var onTaskSaved: ((String, Task) -> Unit)? = null
    var onTaskDeleted: ((String, Task) -> Unit)? = null

    private fun rebuildIndex() {
        _tasksByDate.clear()
        _tasks.forEach { addToIndex(it) }
    }

    private fun addToIndex(task: Task) {
        val date = task.dueDate?.toLocalDate() ?: return
        _tasksByDate.getOrPut(date) { mutableStateListOf() }.add(task)
    }

    private fun removeFromIndex(task: Task) {
        val date = task.dueDate?.toLocalDate() ?: return
        _tasksByDate[date]?.removeIf { it.id == task.id }
        if (_tasksByDate[date]?.isEmpty() == true) {
            _tasksByDate.remove(date)
        }
    }

    fun setTasks(newTasks: List<Task>) {
        _tasks.clear()
        _tasks.addAll(newTasks)
        rebuildIndex()
    }

    fun updateTask(taskId: String, newRawContent: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        val filename = if (index != -1) _tasks[index].filename else taskId
        val now = LocalDateTime.now().withNano(0)
        
        val newTask = Task.fromRaw(filename, newRawContent).copy(
            id = taskId,
            lastUpdate = now
        )

        val isEmpty = newTask.title.isBlank() && newTask.textContent.isBlank()

        if (index != -1) {
            val oldTask = _tasks[index]
            if (isEmpty) {
                removeFromIndex(oldTask)
                _tasks.removeAt(index)
                onTaskChangedInMemory?.invoke(oldTask, true, oldTask)
                onTaskDeleted?.invoke(oldTask.filename, oldTask)
            } else {
                removeFromIndex(oldTask)
                _tasks[index] = newTask
                addToIndex(newTask)
                onTaskChangedInMemory?.invoke(newTask, false, oldTask)
                onTaskSaved?.invoke(newTask.filename, newTask)
            }
        } else if (!isEmpty) {
            _tasks.add(0, newTask)
            addToIndex(newTask)
            onTaskChangedInMemory?.invoke(newTask, false, null)
            onTaskSaved?.invoke(newTask.filename, newTask)
        }
    }

    fun deleteTask(taskId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = _tasks[index]
            removeFromIndex(task)
            _tasks.removeAt(index)
            onTaskChangedInMemory?.invoke(task, true, task)
            onTaskDeleted?.invoke(task.filename, task)
        }
    }

    fun exists(filename: String): Boolean {
        return _tasks.any { it.filename == filename }
    }

    fun delete(filename: String): Boolean {
        val index = _tasks.indexOfFirst { it.filename == filename }
        if (index != -1) {
            val task = _tasks[index]
            removeFromIndex(task)
            _tasks.removeAt(index)
            onTaskChangedInMemory?.invoke(task, true, task)
            onTaskDeleted?.invoke(task.filename, task)
            return true
        }
        return false
    }

    fun upsert(newTask: Task): Boolean {
        val index = _tasks.indexOfFirst { it.filename == newTask.filename }
        if (index != -1) {
            val oldTask = _tasks[index]

            removeFromIndex(oldTask)
            _tasks[index] = newTask
            addToIndex(newTask)

            if (oldTask.toRaw() != newTask.toRaw()) {
                onTaskChangedInMemory?.invoke(newTask, false, oldTask)
                return true
            } else {
                return false
            }
        } else {
            _tasks.add(0, newTask)
            addToIndex(newTask)
            onTaskChangedInMemory?.invoke(newTask, false, null)
            return true
        }
    }

    fun getTaskTitles(): List<String> = _tasks.map { it.title }

    fun getTodaysTaskTitles(): List<String> {
        val today = LocalDate.now()
        return _tasksByDate[today]
            ?.filter { !it.isDone && it.title.isNotBlank() }
            ?.map { it.title }
            ?: emptyList()
    }

    fun getTodaysTasks(): List<Task> {
        return getTasksForDate(LocalDate.now())
    }

    fun getTasksForDate(date: LocalDate): List<Task> {
        return _tasksByDate[date]
            ?.filter { !it.isDone }
            ?: emptyList()
    }

    fun createEmptyTask(dueDate: LocalDateTime? = null): Task {
        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
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
