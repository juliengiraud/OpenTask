package com.example.opentask.service

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.opentask.util.KWatchChannel
import com.example.opentask.util.KWatchChannelMode
import com.example.opentask.util.KWatchEvent
import com.example.opentask.util.KWatchEventKind
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
