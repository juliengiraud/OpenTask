package com.example.opentask.model

import com.example.opentask.util.generateConflict
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

data class Task(
    val title: String = "",
    val textContent: String = "",
    val filename: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDateTime? = null,
    val isDone: Boolean = false,
    val extraYaml: List<String> = emptyList(),
    val lastUpdateFs: Long = 0
) {
    /**
     * Converts the task to its raw Obsidian-compatible Markdown format.
     *
     * The format follows these spacing rules:
     * 1. YAML frontmatter delimited by '---'.
     * 2. Exactly one empty line after the YAML block.
     * 3. A Markdown H1 title ('# Title'), always present even if empty.
     * 4. Exactly one empty line after the title line.
     * 5. The body content (textContent) follows.
     * 6. Ensures the output ends with a newline if the content is not empty.
     */
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
            sb.append("due_date: ").append(it.toLocalDate().toString()).append("\n")
        }
        
        // Add any other existing YAML properties that weren't handled above
        extraYaml.forEach { line ->
            sb.append(line).append("\n")
        }
        sb.append("---\n\n")

        // Always include title row, even if empty, with exactly 1 empty line below
        sb.append("# ").append(title).append("\n\n")
        sb.append(textContent)
        
        return sb.toString()
    }

    fun isEmpty(): Boolean {
        return title.isBlank() && textContent.isBlank()
    }

    companion object {
        private val filenameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

        private val MANAGED_KEYS = setOf(
            "done",
            "due_date",
            "last_update",
            "creation_date"
        )

        fun create(dueDate: LocalDateTime? = null): Task {
            val now = LocalDateTime.now()
            val dateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val filename = "$dateStr.md"

            return Task(
                title = "",
                textContent = "",
                filename = filename,
                createdAt = now,
                lastUpdate = now,
                dueDate = dueDate
            )
        }

        fun fromRaw(filename: String, rawContent: String, lastUpdateFs: Long = 0L): Task {
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
                                "done" -> isDone = value.toBoolean()
                                "due_date" -> {
                                    try {
                                        dueDate = LocalDateTime.parse(value)
                                    } catch (e: Exception) {
                                        try {
                                            dueDate = java.time.LocalDate.parse(value).atStartOfDay()
                                        } catch (e2: Exception) {}
                                    }
                                }
                                "last_update" -> {
                                    try {
                                        lastUpdate = LocalDateTime.parse(value, filenameFormatter)
                                    } catch (e: Exception) {
                                        try {
                                            lastUpdate = LocalDateTime.parse(value)
                                        } catch (e2: Exception) {}
                                    }
                                }
                                "creation_date" -> {
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
                        title = lines[current].substring(2)
                        // If there is an empty line immediately after the title, skip it
                        // to keep textContent clean for toRaw's mandatory spacing.
                        bodyStartLine = if (current + 1 < lines.size && lines[current + 1].isBlank()) {
                            current + 2
                        } else {
                            current + 1
                        }
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
                    // Handle cases with or without a blank line after the title
                    bodyStartLine = if (current + 1 < lines.size && lines[current + 1].isBlank()) {
                        current + 2
                    } else {
                        current + 1
                    }
                }
            }

            val body = lines.drop(bodyStartLine).joinToString("\n")

            // If title is empty, use the first non-empty line from the body
            if (title.isBlank()) {
                title = body.lineSequence().firstOrNull { it.trim().isNotEmpty() } ?: ""
            }

            return Task(
                title = title,
                textContent = body,
                filename = filename,
                createdAt = createdAt,
                lastUpdate = lastUpdate,
                extraYaml = extraYaml,
                isDone = isDone,
                dueDate = dueDate,
                lastUpdateFs = lastUpdateFs
            )
        }

        fun merge(base: Task, local: Task, remote: Task): Task { // todo: simplify 3 -> 2
            // YAML from remote (filesystem) always wins
            val mergedCreatedAt = remote.createdAt
            val mergedLastUpdate = remote.lastUpdate
            val mergedDueDate = remote.dueDate
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
                generateConflict(local.title, remote.title, separator = " ")
            }

            // Body merge
            val mergedBody = if (local.textContent == remote.textContent) {
                local.textContent
            } else if (local.textContent == base.textContent) {
                remote.textContent
            } else if (remote.textContent == base.textContent) {
                local.textContent
            } else {
                generateConflict(local.textContent, remote.textContent, separator = "\n")
            }

            // LastUpdateFs merge
            val mergedLastUpdateFs = if (local.lastUpdateFs > remote.lastUpdateFs) {
                local.lastUpdateFs // should be impossible
            } else {
                remote.lastUpdateFs // 100% cases
            }

            return remote.copy(
                title = mergedTitle,
                textContent = mergedBody,
                createdAt = mergedCreatedAt,
                lastUpdate = mergedLastUpdate,
                dueDate = mergedDueDate,
                isDone = mergedIsDone,
                extraYaml = mergedExtraYaml,
                lastUpdateFs = mergedLastUpdateFs
            )
        }
    }
}
