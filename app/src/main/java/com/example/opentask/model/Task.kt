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
            try {
                // Remove .md extension if present for parsing
                val datePart = filename.removeSuffix(".md")
                createdAt = LocalDateTime.parse(datePart, filenameFormatter)
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

            val body = lines.drop(actualBodyStart).joinToString("\n")

            return Task(
                id = filename,
                title = title,
                textContent = body,
                filename = filename,
                createdAt = createdAt,
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
        if (yaml.isNotEmpty()) {
            sb.append(yaml).append("\n\n")
        }
        // Always include title row, even if empty, with 1 empty line below
        sb.append("# ").append(title).append("\n\n")
        // Ensure the content ends with at least one empty line
        sb.append(textContent.trimEnd()).append("\n\n")
        return sb.toString()
    }
}

data class TaskDuration(
    val days: Int = 0,
    val hours: Int = 0,
    val minutes: Int = 0
)
