package com.example.opentask.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val textContent: String = "",
    val filename: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDateTime? = null,
    val hasTime: Boolean = false,
    val duration: TaskDuration? = null,
    val isDone: Boolean = false,
    val extraYaml: List<String> = emptyList()
) {
    fun toRaw(): String {
        val sb = StringBuilder()
        
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val createdAtStr = createdAt.format(formatter)
        val lastUpdateStr = lastUpdate.format(formatter)
        
        sb.append("---\n")
        sb.append("creation_date: ").append(createdAtStr).append("\n")
        sb.append("last_update: ").append(lastUpdateStr).append("\n")
        if (isDone) sb.append("done: true\n")
        dueDate?.let {
            if (hasTime) {
                sb.append("due_date: ").append(it.toString()).append("\n")
            } else {
                sb.append("due_date: ").append(it.toLocalDate().toString()).append("\n")
            }
        }
        if (hasTime) sb.append("hastime: true\n")
        
        // Add any other existing YAML properties that weren't handled above
        extraYaml.forEach { line ->
            sb.append(line).append("\n")
        }
        sb.append("---\n\n")

        // Always include title row, even if empty, with 1 empty line below
        sb.append("# ").append(title).append("\n\n")
        // Ensure the content ends with exactly one empty line
        sb.append(textContent.trimEnd()).append("\n")
        return sb.toString()
    }

    companion object {
        private val filenameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

        private val MANAGED_KEYS = setOf(
            "done", "completed",
            "due", "deadline", "due_date",
            "hastime",
            "last_update", "updated",
            "creation_date", "created"
        )

        fun fromRaw(filename: String, rawContent: String): Task {
            var createdAt = LocalDateTime.now()
            var lastUpdate = LocalDateTime.now()
            try {
                // Remove .md extension if present for parsing
                val datePart = filename.removeSuffix(".md")
                createdAt = LocalDateTime.parse(datePart, filenameFormatter)
                lastUpdate = createdAt // Default to creation time if not in YAML
            } catch (e: DateTimeParseException) {
                // Keep default now()
            }

            val lines = rawContent.lines()
            val extraYaml = mutableListOf<String>()
            var title = ""
            var bodyStartLine = 0

            var isDone = false
            var dueDate: LocalDateTime? = null
            var hasTime = false
            var duration: TaskDuration? = null

            if (lines.isNotEmpty() && lines[0] == "---") {
                val closingIndex = lines.drop(1).indexOf("---")
                if (closingIndex != -1) {
                    // Parse YAML lines between delimiters
                    for (i in 1 until (closingIndex + 1)) {
                        val line = lines[i]
                        val parts = line.split(":", limit = 2)
                        val key = if (parts.size == 2) parts[0].trim().lowercase() else null
                        
                        if (key != null && key in MANAGED_KEYS) {
                            val value = parts[1].trim()
                            when (key) {
                                "done", "completed" -> isDone = value.toBoolean()
                                "due", "deadline", "due_date" -> {
                                    try {
                                        dueDate = LocalDateTime.parse(value)
                                        hasTime = true
                                    } catch (e: Exception) {
                                        try {
                                            dueDate = java.time.LocalDate.parse(value).atStartOfDay()
                                            hasTime = false
                                        } catch (e2: Exception) {}
                                    }
                                }
                                "hastime" -> hasTime = value.toBoolean()
                                "last_update", "updated" -> {
                                    try {
                                        lastUpdate = LocalDateTime.parse(value, filenameFormatter)
                                    } catch (e: Exception) {
                                        try {
                                            lastUpdate = LocalDateTime.parse(value)
                                        } catch (e2: Exception) {}
                                    }
                                }
                                "creation_date", "created" -> {
                                    try {
                                        createdAt = LocalDateTime.parse(value, filenameFormatter)
                                    } catch (e: Exception) {
                                        try {
                                            createdAt = LocalDateTime.parse(value)
                                        } catch (e2: Exception) {}
                                    }
                                }
                            }
                        } else {
                            extraYaml.add(line)
                        }
                    }

                    var current = closingIndex + 2
                    while (current < lines.size && lines[current].isBlank()) {
                        current++
                    }
                    
                    if (current < lines.size && lines[current].startsWith("# ")) {
                        title = lines[current].substring(2).trim()
                        bodyStartLine = current + 1
                    } else {
                        bodyStartLine = closingIndex + 2
                    }
                }
            } else {
                var current = 0
                while (current < lines.size && lines[current].isBlank()) {
                    current++
                }
                if (current < lines.size && lines[current].startsWith("# ")) {
                    title = lines[current].substring(2).trim()
                    bodyStartLine = current + 1
                }
            }

            // Skip leading empty lines in body
            var actualBodyStart = bodyStartLine
            while (actualBodyStart < lines.size && lines[actualBodyStart].isBlank()) {
                actualBodyStart++
            }

            val body = lines.drop(actualBodyStart).joinToString("\n").trimEnd()

            return Task(
                id = filename,
                title = title,
                textContent = body,
                filename = filename,
                createdAt = createdAt,
                lastUpdate = lastUpdate,
                extraYaml = extraYaml,
                isDone = isDone,
                dueDate = dueDate,
                hasTime = hasTime,
                duration = duration
            )
        }

        fun merge(base: Task, local: Task, remote: Task): Task {
            // YAML from remote (filesystem) always wins
            val mergedCreatedAt = remote.createdAt
            val mergedLastUpdate = remote.lastUpdate
            val mergedDueDate = remote.dueDate
            val mergedHasTime = remote.hasTime
            val mergedIsDone = remote.isDone
            val mergedExtraYaml = remote.extraYaml

            // Title merge
            val mergedTitle = if (local.title == remote.title) {
                local.title
            } else if (local.title == base.title) {
                remote.title
            } else if (remote.title == base.title) {
                local.title
            } else {
                "CONFLICT: Local(${local.title}) vs Remote(${remote.title})"
            }

            // Body merge
            val mergedBody = if (local.textContent == remote.textContent) {
                local.textContent
            } else if (local.textContent == base.textContent) {
                remote.textContent
            } else if (remote.textContent == base.textContent) {
                local.textContent
            } else {
                // Both changed to different things: Use markers as specified in AGENTS.md
                "<<<<<<< External\n${remote.textContent}\n=======\n${local.textContent}\n>>>>>>> Local"
            }

            return remote.copy(
                title = mergedTitle,
                textContent = mergedBody,
                createdAt = mergedCreatedAt,
                lastUpdate = mergedLastUpdate,
                dueDate = mergedDueDate,
                hasTime = mergedHasTime,
                isDone = mergedIsDone,
                extraYaml = mergedExtraYaml
            )
        }
    }
}

data class TaskDuration(
    val days: Int = 0,
    val hours: Int = 0,
    val minutes: Int = 0
)
