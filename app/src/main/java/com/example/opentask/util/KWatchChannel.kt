package com.example.opentask.util

import com.example.opentask.service.DebugManager
import java.io.File
import java.nio.file.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

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
                debugManager.log("ChannelWatcher", "Init => ClosedWatchServiceException: ${e.message}")
            } catch (e: Exception) {
                debugManager.log("ChannelWatcher", "Init => Unknown Exception: ${e.message}")
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
