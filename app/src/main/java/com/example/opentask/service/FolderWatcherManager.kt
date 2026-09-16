package com.example.opentask.service

import android.provider.DocumentsContract
import com.example.opentask.util.KWatchChannel
import com.example.opentask.util.KWatchChannelMode
import com.example.opentask.util.KWatchEvent
import com.example.opentask.util.KWatchEventKind
import java.io.File
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.net.toUri

class FolderWatcherManager(
    private val debugManager: DebugManager,
    private val onFileEvent: (filename: String, kind: KWatchEventKind) -> Unit,
) {
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

    fun isPaused(filename: String): Boolean {
        return pausedFiles.contains(filename)
    }

    private fun getFileFromUriString(uriString: String): File? {
        try {
            val uri = uriString.toUri()
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
            debugManager.log(
                "FolderWatcherManager",
                "Error resolving file path: ${e.message}"
            )
        }
        return null
    }

    fun setupWatcher(folderUriString: String?) {
        stop()
        if (folderUriString == null) return

        try {
            val folder = getFileFromUriString(folderUriString)
            if (folder == null) {
                debugManager.log(
                    "FolderWatcherManager",
                    "WatchService could not resolve local path for URI: $folderUriString"
                )
                return
            }

            val channel = KWatchChannel(
                folder,
                KWatchChannelMode.SINGLE_DIRECTORY,
                CoroutineScope(Dispatchers.IO + SupervisorJob()),
                debugManager
            )
            watchChannel = channel
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    for (event in channel) {
                        if (filenameRegex.matches(event.file.name)) {
                            debounceFileEvent(event)
                        }
                    }
                } catch (e: Exception) {
                    debugManager.log(
                        "FolderWatcherManager",
                        "WatchService channel error: ${e.message}"
                    )
                }
            }
            debugManager.log("FolderWatcherManager", "Watching: ${folder.absolutePath}")

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
            delay(100.milliseconds)
            val finalKind = pendingEventKinds.remove(name) ?: return@launch
            pendingEventJobs.remove(name)
            
            // Re-check pause status after delay settlement
            if (pausedFiles.contains(name)) return@launch

            debugManager.log("FolderWatcherManager", "WatchService Event: $finalKind -> $name")
            onFileEvent(name, finalKind)
        }
        pendingEventJobs[name] = job
    }

    fun reset() {
        debugManager.log("FolderWatcherManager", "Watcher reset.")
        stop()
    }

    fun stop() {
        pendingEventJobs.values.forEach { it.cancel() }
        pendingEventJobs.clear()
        pendingEventKinds.clear()
        watchChannel?.close()
        watchChannel = null
        pausedFiles.clear()
    }
}
