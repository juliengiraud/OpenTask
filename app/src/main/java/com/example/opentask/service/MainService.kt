package com.example.opentask.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.net.toUri
import android.provider.DocumentsContract
import com.example.opentask.model.Task
import com.example.opentask.model.TaskRepository
import java.time.LocalDate

class MainService : Service() {

    private lateinit var folderWatcherManager: FolderWatcherManager
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var debugManager: DebugManager

    private val filenameRegex = Regex("""\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.md""")

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_DATE_CHANGED || 
                intent?.action == Intent.ACTION_TIMEZONE_CHANGED) {
                updateNotification()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        debugManager = DebugManager(this)
        notificationManager = AppNotificationManager(this)

        // Scenario 2: External Changes detected from the FolderWatcher
        folderWatcherManager = FolderWatcherManager(this, debugManager) { filename, kind ->
            handleExternalFileEvent(filename, kind)
        }
        
        // Scenario 3: Create/modify from the app
        TaskRepository.onTaskChangedInMemory = { task, isDeleted, oldTask ->
            val folderUriString = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("watched_folder", null)
            
            if (folderUriString != null) {
                val folderUri = folderUriString.toUri()
                
                // 1. Pause watching on corresponding file to avoid infinite self-trigger loops
                folderWatcherManager.pauseWatching(task.filename)
                
                try {
                    if (isDeleted) {
                        debugManager.log("MainService", "File Action: Deleting file ${task.filename}")
                        FileStorageManager.deleteFile(this, folderUri, task.filename)
                    } else {
                        debugManager.log("MainService", "File Action: Writing file ${task.filename}")
                        FileStorageManager.saveFileContent(this, folderUri, task.filename, task.toRaw())
                    }
                } finally {
                    // 2. Re-enable watching on corresponding file once I/O finishes
                    folderWatcherManager.resumeWatching(task.filename)
                }
            }

            val today = LocalDate.now()
            if (task.dueDate?.toLocalDate() == today || oldTask?.dueDate?.toLocalDate() == today || 
                task.createdAt.toLocalDate() == today || oldTask?.createdAt?.toLocalDate() == today) {
                updateNotification()
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(dateChangeReceiver, filter)

        debugManager.log("MainService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val folderUri = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("watched_folder", null)

        when (intent?.action) {
            ACTION_UPDATE_WATCHED_FOLDER -> {
                val newFolderUri = intent.getStringExtra(EXTRA_FOLDER_URI)
                folderWatcherManager.setupWatcher(newFolderUri)
                scanAndLoadFolder(newFolderUri)
            }
            ACTION_RESET_WATCHER -> {
                folderWatcherManager.reset()
                TaskRepository.setTasks(emptyList())
                updateNotification()
            }
            else -> {
                folderWatcherManager.setupWatcher(folderUri)
                scanAndLoadFolder(folderUri)
                
                startForeground(
                    notificationManager.getForegroundId(),
                    notificationManager.getForegroundNotification(TaskRepository.getTodaysTaskTitles()),
                )
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dateChangeReceiver)
        folderWatcherManager.stop()
        TaskRepository.onTaskChangedInMemory = null
    }

    // Scenario 1: Select folder, list files, and initialize repository
    private fun scanAndLoadFolder(folderUriString: String?) {
        if (folderUriString == null) return
        val startTime = System.currentTimeMillis()
        try {
            val treeUri = folderUriString.toUri()
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            )

            val cursor = contentResolver.query(childrenUri, projection, null, null, null)
            val queryDuration = System.currentTimeMillis() - startTime
            val loadedTasks = mutableListOf<Task>()

            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

                if (nameIndex != -1 && idIndex != -1) {
                    while (c.moveToNext()) {
                        val name = c.getString(nameIndex) ?: continue
                        val docId = c.getString(idIndex)
                        
                        if (!filenameRegex.matches(name)) continue
                        
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val fileContent = FileStorageManager.readFileContent(this, fileUri)
                        if (fileContent != null) {
                            loadedTasks.add(Task.fromRaw(name, fileContent))
                        }
                    }
                }
            }

            val totalDuration = System.currentTimeMillis() - startTime
            debugManager.log("MainService", "Folder scan initialized: Loaded ${loadedTasks.size} notes in ${totalDuration}ms (query: ${queryDuration}ms)")
            TaskRepository.setTasks(loadedTasks)
            updateNotification()
        } catch (e: Exception) {
            debugManager.log("MainService", "Scanning failed: ${e.message}")
        }
    }

    private fun handleExternalFileEvent(filename: String, kind: KWatchEventKind) {
        val folderUriString = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("watched_folder", null) ?: return
        val treeUri = folderUriString.toUri()

        try {
            if (kind == KWatchEventKind.DELETED) {
                // Fine-grained repository external update
                val existingList = TaskRepository.tasks.toMutableList()
                val targetIndex = existingList.indexOfFirst { it.filename == filename }
                if (targetIndex != -1) {
                    existingList.removeAt(targetIndex)
                    TaskRepository.setTasks(existingList)
                    updateNotification()
                    debugManager.log("MainService", "External Change: Deleted note $filename from memory")
                }
                return
            }

            // For CREATED and MODIFIED, find and re-read the file
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            )

            val cursor = contentResolver.query(childrenUri, projection, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

                if (nameIndex != -1 && idIndex != -1) {
                    while (c.moveToNext()) {
                        val currentName = c.getString(nameIndex) ?: continue
                        if (currentName == filename) {
                            val docId = c.getString(idIndex)
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val content = FileStorageManager.readFileContent(this, fileUri)
                            
                            if (content != null) {
                                val externalTask = Task.fromRaw(filename, content)
                                val existingList = TaskRepository.tasks.toMutableList()
                                val targetIndex = existingList.indexOfFirst { it.filename == filename }
                                
                                if (targetIndex != -1) {
                                    // Merge or update with filesystem precision
                                    existingList[targetIndex] = externalTask
                                } else {
                                    existingList.add(0, externalTask)
                                }
                                
                                TaskRepository.setTasks(existingList)
                                updateNotification()
                                debugManager.log("MainService", "External Change: Synchronized note $filename with memory")
                            }
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            debugManager.log("MainService", "External sync error: ${e.message}")
        }
    }

    private fun updateNotification() {
        notificationManager.updateForegroundNotification(TaskRepository.getTodaysTaskTitles())
    }

    companion object {
        const val ACTION_UPDATE_WATCHED_FOLDER = "com.example.opentask.service.MainService.ACTION_UPDATE_WATCHED_FOLDER"
        const val ACTION_RESET_WATCHER = "com.example.opentask.service.MainService.ACTION_RESET_WATCHER"
        const val EXTRA_FOLDER_URI = "com.example.opentask.service.MainService.EXTRA_FOLDER_URI"
        const val ACTION_DEBUG_LOG = "com.example.opentask.service.MainService.ACTION_DEBUG_LOG"
        const val EXTRA_LOG_MESSAGE = "com.example.opentask.service.MainService.EXTRA_LOG_MESSAGE"
    }
}
