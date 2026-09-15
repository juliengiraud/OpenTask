package com.example.opentask.service

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.nio.file.*
import java.util.Collections
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class FolderWatcherManager(
    private val context: Context,
    private val debugManager: DebugManager,
    private val onFileEvent: (filename: String, kind: KWatchEventKind) -> Unit,
) {
    var lastEventInfo: String = "No changes yet"
        private set

    private var watchChannel: KWatchChannel? = null
    private var pendingEventJobs = mutableMapOf<String, Job>()
    private var pendingEventKinds = mutableMapOf<String, KWatchEventKind>()
    
    // Thread-safe set of filenames to ignore during application writes to break infinite loops
    private val pausedFiles = Collections.synchronizedSet(mutableSetOf<String>())

    fun pauseWatching(filename: String) {
        pausedFiles.add(filename)
    }

    fun resumeWatching(filename: String) {
        pausedFiles.remove(filename)
    }

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
        pausedFiles.clear()

        if (folderUriString == null) {
            lastEventInfo = "No folder selected"
            return
        }

        try {
            val file = getFileFromUriString(folderUriString)
            if (file != null) {
                val channel = KWatchChannel(file, KWatchChannelMode.SINGLE_DIRECTORY, CoroutineScope(Dispatchers.IO + SupervisorJob()), debugManager)
                watchChannel = channel
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        for (event in channel) {
                            if (filenameRegex.matches(event.file.name)) {
                                debounceFileEvent(event)
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
        } catch (e: Exception) {
            debugManager.log("FolderWatcherManager", "Setup Error: ${e.message}")
        }
    }

    private val filenameRegex = Regex("""\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.md""")

    private fun debounceFileEvent(event: KWatchEvent) {
        val name = event.file.name
        
        // Skip entirely if file watching is paused for this filename
        if (pausedFiles.contains(name)) {
            return
        }

        pendingEventJobs[name]?.cancel()
        pendingEventKinds[name] = event.kind

        val job = CoroutineScope(Dispatchers.Main).launch {
            // Apply 100ms debounce delay per file to prevent long spam
            delay(100)
            val finalKind = pendingEventKinds.remove(name) ?: return@launch
            pendingEventJobs.remove(name)
            
            // Re-check pause status after delay settlement
            if (pausedFiles.contains(name)) return@launch

            lastEventInfo = "$finalKind: $name"
            debugManager.log("FolderWatcherManager", "WatchService Event: $finalKind -> $name")
            onFileEvent(name, finalKind)
        }
        pendingEventJobs[name] = job
    }

    fun reset() {
        debugManager.log("FolderWatcherManager", "Watcher reset.")
        setupWatcher(null)
        lastEventInfo = "Watcher reset"
    }

    fun stop() {
        pendingEventJobs.values.forEach { it.cancel() }
        pendingEventJobs.clear()
        pendingEventKinds.clear()
        watchChannel?.close()
        watchChannel = null
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
                debugManager.log("ChannelWatcher", "Init => ClosedWatchServiceException")
            } catch (e: Exception) {
                debugManager.log("ChannelWatcher", "Init => Unknown Exception")
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
            debugManager.log("ChannelWatcher", "registerDirectory => Unknown Exception")
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
            debugManager.log("ChannelWatcher", "registerRecursive => Unknown Exception")
        }
    }

    override fun close(cause: Throwable?): Boolean {
        watchJob?.cancel()
        try {
            watchService.close()
        } catch (e: Exception) {
            debugManager.log("ChannelWatcher", "close => Unknown Exception")
        }
        registeredKeys.clear()
        return channel.close(cause)
    }
}
