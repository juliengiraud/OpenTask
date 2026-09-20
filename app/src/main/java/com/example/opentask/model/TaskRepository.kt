package com.example.opentask.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import java.time.LocalDate
import androidx.core.database.sqlite.transaction

class TaskRepository private constructor(context: Context) {

    // In-memory mutation callback hooks to decouple filesystem saving from repository operations
    var onTaskChangedInMemory: ((Task, isDeleted: Boolean) -> Unit)? = null

    // Kept temporary properties to avoid breaking MainService / UI listeners before Step 5 complete integration
    var onTaskSaved: ((String, Task) -> Unit)? = null
    var onTaskDeleted: ((String, Task) -> Unit)? = null

    private val dbHelper = TaskDbHelper(context.applicationContext)
    
    private var mutationTrigger by mutableIntStateOf(0)

    fun cursorToTask(cursor: Cursor): Task? {
        try {
            val filenameIdx = cursor.getColumnIndex("filename")
            val rawContentIdx = cursor.getColumnIndex("raw_content")
            val lastUpdateFsIdx = cursor.getColumnIndex("last_update_fs")

            val filename = cursor.getString(filenameIdx) ?: ""
            val rawContent = cursor.getString(rawContentIdx) ?: ""
            val lastUpdateFs = if (lastUpdateFsIdx != -1) cursor.getLong(lastUpdateFsIdx) else 0L
            
            val task = Task.fromRaw(filename, rawContent, lastUpdateFs)
            return task
        } catch (_: Exception) {
            return null
        }
    }

    fun taskToContentValues(task: Task): ContentValues {
        return ContentValues().apply {
            put("filename", task.filename)
            put("title", task.title)
            put("raw_content", task.toRaw())
            put("created_at", task.createdAt.toString())
            put("updated_at", task.lastUpdate.toString())
            put("due_date", task.dueDate?.toString())
            put("is_done", if (task.isDone) 1 else 0)
            put("last_update_fs", task.lastUpdateFs)
        }
    }

    fun getAllTasks(): List<Task> {
        mutationTrigger
        val db = dbHelper.readableDatabase
        val cursor = db.query("tasks", null, null, null, null, null, "updated_at DESC")
        val loadedTasks = mutableListOf<Task>()
        cursor.use { c ->
            while (c.moveToNext()) {
                val task = cursorToTask(c)
                if (task != null) {
                    loadedTasks.add(task)
                }
            }
        }
        return loadedTasks
    }

    fun getTaskById(taskId: String): Task? {
        mutationTrigger
        val db = dbHelper.readableDatabase
        val cursor = db.query("tasks", null, "filename = ?", arrayOf(taskId), null, null, null)
        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToTask(c)
            }
            return null
        }
    }

    fun clear() {
        val db = dbHelper.writableDatabase
        db.delete("tasks", null, null)
        mutationTrigger++
    }

    fun update(taskId: String, newRawContent: String): Boolean {
        val oldTask = getTaskById(taskId) ?: return false
        val newTask = Task.fromRaw(taskId, newRawContent, oldTask.lastUpdateFs)

        if (newTask.isEmpty()) return delete(oldTask)

        if (newTask.toRaw() == oldTask.toRaw()) return false

        val db = dbHelper.writableDatabase
        db.replace("tasks", null, taskToContentValues(newTask))
        onTaskChangedInMemory?.invoke(newTask, false)
        onTaskSaved?.invoke(newTask.filename, newTask)
        mutationTrigger++
        return true
    }

    fun update(task: Task): Boolean {
        return update(task.filename, task.toRaw())
    }

    fun delete(task: Task): Boolean {
        val db = dbHelper.writableDatabase
        val deleted = db.delete("tasks", "filename = ?", arrayOf(task.filename))
        if (deleted == 1) {
            onTaskChangedInMemory?.invoke(task, true)
            onTaskDeleted?.invoke(task.filename, task)
            mutationTrigger++
            return true
        }
        return false
    }

    fun delete(taskId: String): Boolean {
        val task = getTaskById(taskId)
        if (task != null) {
            return delete(task)
        }
        return false
    }

    fun delete(taskIds: List<String>) {
        taskIds.forEach {
            val task = getTaskById(it)
            if (task != null) {
                delete(task)
            }
        }
    }

    fun exists(filename: String): Boolean {
        return getTaskById(filename) != null
    }

    fun exists(task: Task): Boolean {
        return exists(task.filename)
    }

    fun create(task: Task) {
        val db = dbHelper.writableDatabase
        db.insert("tasks", null, taskToContentValues(task))
        mutationTrigger++
        onTaskChangedInMemory?.invoke(task, false)
    }

    fun upsert(newTask: Task): Boolean {
        if (!exists(newTask)) {
            create(newTask)
            return true
        }

        return update(newTask)
    }

    fun upsert(tasks: List<Task>) {
        val db = dbHelper.writableDatabase
        db.transaction {
            try {
                tasks.forEach { task ->
                    insert("tasks", null, taskToContentValues(task))
                }
            } finally {
            }
        }
        mutationTrigger++
    }

    fun getTodayTasks(): List<Task> {
        val today = LocalDate.now()
        return getTasksForDate(today)
    }

    fun getTasksForDate(date: LocalDate): List<Task> {
        mutationTrigger
        val db = dbHelper.readableDatabase
        val dateString = date.toString() // ISO-8601 format

        // Search for dates starting with the specified date string
        val selection = "due_date LIKE ?"
        val selectionArgs = arrayOf("$dateString%")

        val cursor = db.query("tasks", null, selection, selectionArgs, null, null, "updated_at DESC")
        val loadedTasks = mutableListOf<Task>()
        cursor.use { c ->
            while (c.moveToNext()) {
                val task = cursorToTask(c)
                if (task != null) loadedTasks.add(task)
            }
        }
        return loadedTasks
    }

    companion object {
        private var instance: TaskRepository? = null

        fun getInstance(context: Context): TaskRepository {
            return instance ?: synchronized(this) {
                instance ?: TaskRepository(context).also { instance = it }
            }
        }
    }
}

class TaskDbHelper(context: Context) : SQLiteOpenHelper(context, "tasks.db", null, 4) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE tasks (
                filename TEXT,
                title TEXT,
                raw_content TEXT,
                created_at TEXT,
                updated_at TEXT,
                due_date TEXT,
                is_done INTEGER,
                last_update_fs INTEGER,
                PRIMARY KEY (filename)
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX idx_tasks_created_at ON tasks(created_at)")
        db.execSQL("CREATE INDEX idx_tasks_updated_at ON tasks(updated_at)")
        db.execSQL("CREATE INDEX idx_tasks_due_date ON tasks(due_date)")
        db.execSQL("CREATE INDEX idx_tasks_last_update_fs ON tasks(last_update_fs)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS tasks")
        onCreate(db)
    }
}
