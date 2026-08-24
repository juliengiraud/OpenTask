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
    val yaml: String = ""
) {
    companion object {
        private val filenameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

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
            var yaml = ""
            var title = ""
            var bodyStartLine = 0

            var isDone = false
            var dueDate: LocalDateTime? = null
            var hasTime = false
            var duration: TaskDuration? = null

            if (lines.isNotEmpty() && lines[0] == "---") {
                val closingIndex = lines.drop(1).indexOf("---")
                if (closingIndex != -1) {
                    val yamlLines = lines.slice(0..closingIndex + 1)
                    yaml = yamlLines.joinToString("\n")
                    
                    // Basic YAML property parsing
                    yamlLines.forEach { line ->
                        val parts = line.split(":", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim().lowercase()
                            val value = parts[1].trim()
                            when (key) {
                                "done", "completed" -> isDone = value.toBoolean()
                                "due", "deadline" -> {
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
                                "last_update" -> {
                                    try {
                                        lastUpdate = LocalDateTime.parse(value, filenameFormatter)
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }

                    var current = closingIndex + 2
                    while (current < lines.size && lines[current].isBlank()) {
                        current++
                    }
                    
                    if (current < lines.size && lines[current].startsWith("# ")) {
                        title = lines[current].substring(2)
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
                    title = lines[current].substring(2)
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
                yaml = yaml,
                isDone = isDone,
                dueDate = dueDate,
                hasTime = hasTime,
                duration = duration
            )
        }
    }

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
        
        // Add any other existing YAML properties that aren't handled above
        if (yaml.isNotEmpty()) {
            val lines = yaml.lines()
            // We want to skip the first and last lines (the --- delimiters)
            // and also skip properties we've already handled
            val propertyLines = if (lines.firstOrNull() == "---") {
                val closingIndex = lines.drop(1).indexOf("---")
                if (closingIndex != -1) {
                    lines.subList(1, closingIndex + 1)
                } else emptyList()
            } else emptyList()

            propertyLines.forEach { line ->
                val key = line.split(":", limit = 2).firstOrNull()?.trim()?.lowercase()
                if (key != null && key.isNotEmpty() && key !in listOf("creation_date", "last_update", "done", "completed", "due", "deadline", "hastime", "due_date")) {
                    sb.append(line).append("\n")
                }
            }
        }
        sb.append("---\n\n")

        // Always include title row, even if empty, with 1 empty line below
        sb.append("# ").append(title).append("\n\n")
        // Ensure the content ends with exactly one empty line
        sb.append(textContent.trimEnd()).append("\n")
        return sb.toString()
    }
}

data class TaskDuration(
    val days: Int = 0,
    val hours: Int = 0,
    val minutes: Int = 0
)
