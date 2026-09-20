package com.example.opentask.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import androidx.core.net.toUri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.opentask.model.Task
import com.example.opentask.model.TaskRepository
import com.example.opentask.util.KWatchEventKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainService : Service() {

    private var folderWatcherManager: FolderWatcherManager? = null
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var debugManager: DebugManager
    private lateinit var fileStorageManager: FileStorageManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: TaskRepository

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
        repository = TaskRepository.getInstance(this)
        debugManager = DebugManager(this)
        notificationManager = AppNotificationManager(this)
        fileStorageManager = FileStorageManager(this)

        // Scenario 3: Create/modify from the app
        repository.onTaskChangedInMemory = { task, isDeleted ->
            val folderUriString = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("watched_folder", null)
            
            // If the watcher is already pausing this file, it means the change is coming from the filesystem.
            // We skip the redundant write to disk.
            val isExternalSync = folderWatcherManager?.isPaused(task.filename) == true

            if (folderUriString != null && !isExternalSync) {
                val folderUri = folderUriString.toUri()
                
                // 1. Pause watching on corresponding file to avoid infinite self-trigger loops
                folderWatcherManager?.pauseWatching(task.filename)
                
                try {
                    if (isDeleted) {
                        debugManager.log("MainService", "File Action: Deleting file ${task.filename}")
                        fileStorageManager.deleteFile(folderUri, task.filename)
                    } else {
                        debugManager.log("MainService", "File Action: Writing file ${task.filename}")
                        fileStorageManager.saveFileContent(folderUri, task.filename, task.toRaw())
                    }
                } finally {
                    // 2. Re-enable watching on corresponding file once I/O finishes
                    folderWatcherManager?.resumeWatching(task.filename)
                }
            }

            updateNotification()
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(dateChangeReceiver, filter)

        debugManager.log("MainService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debugManager.log("MainService", "Repository status: ${repository.getAllTasks().size} notes in database")
        val folderUri = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("watched_folder", null)

        when (intent?.action) {
            ACTION_UPDATE_WATCHED_FOLDER -> {
                val newFolderUri = intent.getStringExtra(EXTRA_FOLDER_URI)
                serviceScope.launch {
                    ensureWatcherInitialized()
                    folderWatcherManager?.setupWatcher(newFolderUri)
                    scanAndLoadFolderInternal(newFolderUri)
                }
            }
            ACTION_RESET_WATCHER -> {
                ensureWatcherInitialized()
                folderWatcherManager?.reset()
                repository.setTasks(emptyList())
                updateNotification()
            }
            else -> {
                if (folderWatcherManager == null) {
                    serviceScope.launch {
                        ensureWatcherInitialized()
                        folderWatcherManager?.setupWatcher(folderUri)
                        scanAndLoadFolderInternal(folderUri)
                        
                        withContext(Dispatchers.Main) {
                            notificationManager.start(this@MainService)
                        }
                    }
                } else {
                    debugManager.log("MainService", "Service already running")
                    folderWatcherManager?.logStatus()
                    updateNotification()
                }
            }
        }
        return START_STICKY
    }

    private fun ensureWatcherInitialized() {
        if (folderWatcherManager == null) {
            // Scenario 2: External Changes detected from the FolderWatcher
            folderWatcherManager = FolderWatcherManager(debugManager) { filename, kind ->
                handleExternalFileEvent(filename, kind)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterReceiver(dateChangeReceiver)
        folderWatcherManager?.stop()
        repository.onTaskChangedInMemory = null
    }

    inner class DocFile(val name: String, val uri: Uri, val lastUpdate: Long) {
        fun getContent(): String? = fileStorageManager.readFileContent(uri)
    }

    private fun listFiles(folderUriString: String, nameRegex: Regex? = null, updatedAfter: Long? = null): List<DocFile> {
        val treeUri = folderUriString.toUri()
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val files = mutableListOf<DocFile>()

        val cursor = contentResolver.query(childrenUri, projection, null, null, null)
        cursor?.use { c ->
            val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val lastUpdateIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            if (nameIndex != -1 && idIndex != -1) {
                while (c.moveToNext()) {
                    val docId = c.getString(idIndex)
                    val name = c.getString(nameIndex) ?: continue
                    val lastUpdate = c.getLong(lastUpdateIndex)

                    if (updatedAfter != null && lastUpdate < updatedAfter) continue

                    if (nameRegex != null && !nameRegex.matches(name)) continue

                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    files.add(DocFile(name, fileUri, lastUpdate))
                }
            }
        }

        return files
    }

    private fun getFile(folderUriString: String, filename: String): DocFile? {
        val treeUri = folderUriString.toUri()
        val rootFolder = DocumentFile.fromTreeUri(this, treeUri) ?: return null
        val file = rootFolder.findFile(filename) ?: return null
        return DocFile(filename, file.uri, file.lastModified())
    }

    fun getFolderPath(folderUriString: String): String {
        return folderUriString.toUri().path?.split(":")?.getOrNull(1) ?: ""
    }

    // Scenario 1: Select folder, list files, and initialize repository
    private suspend fun scanAndLoadFolderInternal(folderUriString: String?) {
        if (folderUriString == null) return
        val startTime = System.currentTimeMillis()
        // todo run batches to read files in another thread?
        // => tmp notes with only filename
        // => add fs last updated
        // => put notes in database, add columns to duplicate properties from raw_content
        // 100 is good size for repo/ui commit
        // handle ui for loading states
        // todo? static loading time to allow some smart content load first
        try {
            val files = listFiles(folderUriString, filenameRegex)
            val queryDuration = System.currentTimeMillis() - startTime

            val loadedTasks = mutableListOf<Task>()
            for (file in files) {
                file.getContent()?.let { content ->
                    loadedTasks.add(Task.fromRaw(file.name, content, file.lastUpdate))
                }
            }

            val totalDuration = System.currentTimeMillis() - startTime
            
            withContext(Dispatchers.Main) {
                debugManager.log("MainService", "Folder scan complete: Loaded ${loadedTasks.size} notes in ${totalDuration}ms (query: ${queryDuration}ms)")
                repository.setTasks(loadedTasks)
                updateNotification()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                debugManager.log("MainService", "Scanning failed: ${e.message}")
            }
        }
    }

    private fun handleExternalFileEvent(filename: String, kind: KWatchEventKind) {
        val folderUriString = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("watched_folder", null) ?: return

        // Signal to the repository listener that this is an external sync
        folderWatcherManager?.pauseWatching(filename)
        try {
            if (kind == KWatchEventKind.DELETED) {
                if (repository.delete(filename)) {
                    debugManager.log("MainService", "External Change: Deleted note $filename from memory")
                }
                return
            }

            // For CREATED and MODIFIED, find and re-read the file
            val file = getFile(folderUriString, filename) ?: return
            val content = file.getContent()
            if (content != null) {
                val externalTask = Task.fromRaw(filename, content, file.lastUpdate)
                if (repository.upsert(externalTask)) {
                    debugManager.log("MainService", "External Change: Synchronized note $filename with memory")
                }
            }
        } catch (e: Exception) {
            debugManager.log("MainService", "External sync error: ${e.message}")
        } finally {
            folderWatcherManager?.resumeWatching(filename)
        }
    }

    private fun updateNotification() {
        notificationManager.updateForegroundNotification(repository.getTodayTasks())
    }

    companion object {
        const val ACTION_UPDATE_WATCHED_FOLDER = "com.example.opentask.service.MainService.ACTION_UPDATE_WATCHED_FOLDER"
        const val ACTION_RESET_WATCHER = "com.example.opentask.service.MainService.ACTION_RESET_WATCHER"
        const val EXTRA_FOLDER_URI = "com.example.opentask.service.MainService.EXTRA_FOLDER_URI"
        const val ACTION_DEBUG_LOG = "com.example.opentask.service.MainService.ACTION_DEBUG_LOG"
        const val EXTRA_LOG_MESSAGE = "com.example.opentask.service.MainService.EXTRA_LOG_MESSAGE"
    }
}
