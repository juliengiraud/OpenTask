package com.example.opentask.service

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.example.opentask.model.Task
import com.example.opentask.model.TaskRepository
import java.io.File
import java.nio.file.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class FolderWatcherManager(
    private val context: Context,
    private val debugManager: DebugManager,
    private val onStatusChanged: (List<Task>) -> Unit,
) {
    var lastEventInfo: String = "No changes yet"
        private set

    val activeTasks: List<String>
        get() = fileMetadataMap.keys.toList()

    private var fileMetadataMap = mutableMapOf<String, Long>()
    private var taskCache = mutableMapOf<String, Task>()
    private var watchChannel: KWatchChannel? = null
    private var currentWatchedUri: Uri? = null
    private var currentChildrenUri: Uri? = null
    private var pendingEventJobs = mutableMapOf<String, Job>()
    private var pendingEventKinds = mutableMapOf<String, KWatchEventKind>()

    private fun getFileFromUriString(uriString: String?): File? {
        if (uriString == null) return null
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                return uri.path?.let { File(it) }
            }
            if (uri.scheme == "content") {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                if (docId != null && docId.startsWith("primary:")) {
                    val relativePath = docId.substringAfter("primary:")
                    return File("/storage/emulated/0", relativePath)
                }
            }
        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Error resolving file path: ${e.message}")
        }
        return null
    }

    fun setupWatcher(folderUriString: String?) {
        stop()
        currentWatchedUri = null
        taskCache.clear()

        if (folderUriString == null) {
            lastEventInfo = "No folder selected"
            onStatusChanged(emptyList())
            return
        }

        try {
            val uri = folderUriString.toUri()
            currentWatchedUri = uri
            val documentId = DocumentsContract.getTreeDocumentId(uri)
            currentChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)

            val file = getFileFromUriString(folderUriString)

            if (file != null) {
                val channel = KWatchChannel(file, KWatchChannelMode.SINGLE_DIRECTORY, CoroutineScope(Dispatchers.IO + SupervisorJob()), debugManager)
                watchChannel = channel
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        for (event in channel) {
                            if (filenameRegex.matches(event.file.name)) {
                                debounceFileEvent(uri, event)
                            }
                        }
                    } catch (e: Exception) {
                        debugManager.log("FolderWatcherManager", "WatchService channel error: ${e.message}")
                    }
                }
                debugManager.log("FolderWatcherManager", "Watching: ${file.absolutePath}")
            } else {
                debugManager.log("FolderWatcherManager", "WatchService could not resolve local path for URI: $folderUriString")
            }

            scanFolder(uri, showPush = false) // Initial state
        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Setup Error: ${e.message}")
        }
    }

    private val filenameRegex = Regex("""\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.md""")

    private fun debounceFileEvent(treeUri: Uri, event: KWatchEvent) {
        val name = event.file.name
        pendingEventJobs[name]?.cancel()
        pendingEventKinds[name] = event.kind

        val job = CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            val finalKind = pendingEventKinds.remove(name) ?: return@launch
            pendingEventJobs.remove(name)
            
            debugManager.log("FolderWatcherManager", "WatchService Event: $finalKind -> ${event.file.name} (${event.file.absolutePath})")
            handleSingleFileEvent(treeUri, event.copy(kind = finalKind))
        }
        pendingEventJobs[name] = job
    }

    private fun handleSingleFileEvent(treeUri: Uri, event: KWatchEvent) {
        val name = event.file.name
        val childrenUri = currentChildrenUri ?: return

        try {
            if (event.kind == KWatchEventKind.DELETED) {
                val removedTask = taskCache[name]
                fileMetadataMap.remove(name)
                taskCache.remove(name)

                lastEventInfo = "Deleted: $name"
                debugManager.log("FolderWatcherManager", lastEventInfo)

                val updatedList = taskCache.values.toList()
                TaskRepository.setTasks(updatedList)
                onStatusChanged(if (removedTask != null) listOf(removedTask) else emptyList())
                return
            }

            // For CREATED and MODIFIED, look up just this file via the provider using a filtered query or cursor scanning
            val cursor = context.contentResolver.query(childrenUri, PROJECTION, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val lastModIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

                if (nameIndex != -1 && lastModIndex != -1 && idIndex != -1) {
                    while (c.moveToNext()) {
                        val currentName = c.getString(nameIndex) ?: continue
                        if (currentName == name) {
                            val lastModified = c.getLong(lastModIndex)
                            val docId = c.getString(idIndex)

                            val isNew = !fileMetadataMap.containsKey(name)
                            val isChanged = fileMetadataMap[name] != lastModified

                            if (isNew || isChanged) {
                                fileMetadataMap[name] = lastModified
                                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                loadTaskFromUri(fileUri, name, lastModified)?.let { task ->
                                    taskCache[name] = task
                                    lastEventInfo = if (isNew) "Created: $name" else "Updated: $name"
                                    debugManager.log("FolderWatcherManager", lastEventInfo)

                                    val updatedList = taskCache.values.toList()
                                    TaskRepository.setTasks(updatedList)
                                    onStatusChanged(listOf(task))
                                }
                            }
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Single File Event Error: ${e.message}")
        }
    }

    private fun scanFolder(treeUri: Uri, showPush: Boolean) {
        val childrenUri = currentChildrenUri ?: return
        val startTime = System.currentTimeMillis()
        val isInitialScan = fileMetadataMap.isEmpty()
        try {
            val cursor = context.contentResolver.query(childrenUri, PROJECTION, null, null, null)
            val queryDuration = System.currentTimeMillis() - startTime
            val newMetadata = mutableMapOf<String, Long>()
            var changeDetected = false
            val loadedTasks = mutableListOf<Task>()
            val changedTasks = mutableListOf<Task>()

            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val lastModIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

                if (nameIndex != -1 && lastModIndex != -1 && idIndex != -1) {
                    while (c.moveToNext()) {
                        val name = c.getString(nameIndex) ?: continue
                        val lastModified = c.getLong(lastModIndex)
                        val docId = c.getString(idIndex)
                        
                        if (filenameRegex.matches(name)) {
                            newMetadata[name] = lastModified

                            val cachedTask = taskCache[name]
                            if (cachedTask != null && fileMetadataMap[name] == lastModified) {
                                loadedTasks.add(cachedTask)
                            } else {
                                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                loadTaskFromUri(fileUri, name, lastModified)?.let { task ->
                                    taskCache[name] = task
                                    loadedTasks.add(task)
                                    changedTasks.add(task)
                                }
                            }

                            if (!fileMetadataMap.containsKey(name)) {
                                if (showPush && fileMetadataMap.isNotEmpty()) {
                                    lastEventInfo = "Created: $name"
                                    debugManager.log("FolderWatcherManager", lastEventInfo)
                                    changeDetected = true
                                }
                            } else if (fileMetadataMap[name]!! != lastModified) {
                                if (showPush) {
                                    lastEventInfo = if (fileMetadataMap[name]!! < lastModified) "Updated: $name" else "Externally Replaced: $name"
                                    debugManager.log("FolderWatcherManager", lastEventInfo)
                                    changeDetected = true
                                }
                            }
                        }
                    }
                }
            }

            // Detect Deletions
            for (oldName in fileMetadataMap.keys) {
                if (!newMetadata.containsKey(oldName)) {
                    val removedTask = taskCache[oldName]
                    if (removedTask != null) changedTasks.add(removedTask)
                    taskCache.remove(oldName)
                    if (showPush) {
                        lastEventInfo = "Deleted: $oldName"
                        debugManager.log("FolderWatcherManager", lastEventInfo)
                        changeDetected = true
                    }
                }
            }

            val totalDuration = System.currentTimeMillis() - startTime
            val typePrefix = if (isInitialScan) "Initial exploration" else "Full scan"
            debugManager.log("FolderWatcherManager", "$typePrefix: Found ${newMetadata.size} files in ${totalDuration}ms (query: ${queryDuration}ms)")

            if (changeDetected || fileMetadataMap.isEmpty()) {
                if (fileMetadataMap.isEmpty() && newMetadata.isEmpty()) {
                    lastEventInfo = "Folder empty"
                }
                fileMetadataMap.clear()
                fileMetadataMap.putAll(newMetadata)
                TaskRepository.setTasks(loadedTasks)
                onStatusChanged(if (fileMetadataMap.size == newMetadata.size && !changeDetected) loadedTasks else changedTasks)
            }

        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Scan Error: ${e.message}")
        }
    }

    private fun loadTaskFromUri(uri: Uri, name: String, lastModified: Long): Task? {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return null

            Task.fromRaw(name, content).copy(
                lastUpdate = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastModified),
                    ZoneId.systemDefault()
                )
            )
        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Error reading $name: ${e.message}")
            null
        }
    }

    fun updateCache(name: String, lastModified: Long, task: Task) {
        debugManager.log("FolderWatcherManager", "Cache synced for $name")
        fileMetadataMap[name] = lastModified
        taskCache[name] = task
    }

    fun removeFromCache(name: String) {
        fileMetadataMap.remove(name)
        taskCache.remove(name)
    }

    fun reset() {
        debugManager.log("FolderWatcherManager", "Watcher reset.")
        setupWatcher(null)
        fileMetadataMap.clear()
        taskCache.clear()
        lastEventInfo = "Watcher reset"
        onStatusChanged(emptyList())
    }

    fun stop() {
        pendingEventJobs.values.forEach { it.cancel() }
        pendingEventJobs.clear()
        pendingEventKinds.clear()
        watchChannel?.close()
        watchChannel = null
        currentChildrenUri = null
    }

    companion object {
        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        )
    }
}

enum class KWatchEventKind {
    CREATED, MODIFIED, DELETED
}

data class KWatchEvent(
    val file: File,
    val kind: KWatchEventKind
)

enum class KWatchChannelMode {
    SINGLE_FILE, SINGLE_DIRECTORY, RECURSIVE
}

class KWatchChannel(
    val file: File,
    val mode: KWatchChannelMode = KWatchChannelMode.RECURSIVE,
    val scope: CoroutineScope = GlobalScope,
    val debugManager: DebugManager,
    private val channel: Channel<KWatchEvent> = Channel(),
) : Channel<KWatchEvent> by channel {

    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val registeredKeys = mutableMapOf<WatchKey, Path>()
    private var watchJob: Job? = null

    init {
        if (file.isDirectory) {
            if (mode == KWatchChannelMode.RECURSIVE) {
                registerRecursive(file.toPath())
            } else {
                registerDirectory(file.toPath())
            }
        } else {
            file.parentFile?.toPath()?.let { registerDirectory(it) }
        }

        watchJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val key = watchService.take() ?: break
                    val dirPath = registeredKeys[key] ?: continue

                    for (event in key.pollEvents()) {
                        val kind = event.kind()
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue

                        val context = event.context() as? Path ?: continue
                        val resolvedPath = dirPath.resolve(context)
                        val affectedFile = resolvedPath.toFile()

                        if (mode == KWatchChannelMode.SINGLE_FILE && affectedFile.absolutePath != file.absolutePath) {
                            continue
                        }

                        val watchEventKind = when (kind) {
                            StandardWatchEventKinds.ENTRY_CREATE -> KWatchEventKind.CREATED
                            StandardWatchEventKinds.ENTRY_MODIFY -> KWatchEventKind.MODIFIED
                            StandardWatchEventKinds.ENTRY_DELETE -> KWatchEventKind.DELETED
                            else -> continue
                        }

                        if (mode == KWatchChannelMode.RECURSIVE && watchEventKind == KWatchEventKind.CREATED && affectedFile.isDirectory) {
                            registerRecursive(resolvedPath)
                        }

                        channel.send(KWatchEvent(affectedFile, watchEventKind))
                    }

                    if (!key.reset()) {
                        registeredKeys.remove(key)
                        if (registeredKeys.isEmpty()) break
                    }
                }
            } catch (e: ClosedWatchServiceException) {
                // Ignore
                debugManager.log("FolderWatcherManager", "error 1")
            } catch (e: Exception) {
                // Ignore
                debugManager.log("FolderWatcherManager", "error 2")
            }
        }
    }

    private fun registerDirectory(path: Path) {
        try {
            val key = path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )
            registeredKeys[key] = path
        } catch (e: Exception) {
            // Ignore
            debugManager.log("FolderWatcherManager", "error 3")
        }
    }

    private fun registerRecursive(root: Path) {
        try {
            Files.walk(root).forEach { path ->
                if (Files.isDirectory(path)) {
                    registerDirectory(path)
                }
            }
        } catch (e: Exception) {
            // Ignore
            debugManager.log("FolderWatcherManager", "error 4")
        }
    }

    override fun close(cause: Throwable?): Boolean {
        watchJob?.cancel()
        try {
            watchService.close()
        } catch (e: Exception) {
            // Ignore
            debugManager.log("FolderWatcherManager", "error 5")
        }
        registeredKeys.clear()
        return channel.close(cause)
    }
}
