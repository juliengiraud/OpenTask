package com.example.opentask.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class TaskTest {

    @Test
    fun `test full task parsing and reconstruction`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = """---
creation_date: 2023-10-27_10-30-00
last_update: 2023-10-27_11-00-00
done: true
due_date: 2023-10-28
hastime: false
custom_prop: value
# comment line
---

# My Test Title

This is the content of the task.
It has multiple lines.
"""
        val task = Task.fromRaw(filename, raw)
        
        assertEquals("My Test Title", task.title)
        assertEquals("This is the content of the task.\nIt has multiple lines.", task.textContent)
        assertTrue(task.isDone)
        assertEquals(LocalDateTime.of(2023, 10, 28, 0, 0), task.dueDate)
        assertFalse(task.hasTime)
        
        // Check extra YAML preservation
        assertTrue(task.extraYaml.contains("custom_prop: value"))
        assertTrue(task.extraYaml.contains("# comment line"))

        // Round trip
        val reconstructed = task.toRaw()
        assertTrue(reconstructed.contains("custom_prop: value"))
        assertTrue(reconstructed.contains("# comment line"))
        assertTrue(reconstructed.contains("# My Test Title"))
        assertTrue(reconstructed.endsWith("\n\n# My Test Title\n\nThis is the content of the task.\nIt has multiple lines.\n"))
    }

    @Test
    fun `test parsing without YAML`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = """
# Simple Title

Just content.
"""
        val task = Task.fromRaw(filename, raw)
        assertEquals("Simple Title", task.title)
        assertEquals("Just content.", task.textContent)
        assertEquals(LocalDateTime.of(2023, 10, 27, 10, 30), task.createdAt)
    }

    @Test
    fun `test parsing content only`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = "No title, just text."
        val task = Task.fromRaw(filename, raw)
        assertEquals("", task.title)
        assertEquals("No title, just text.", task.textContent)
    }

    @Test
    fun `test parsing with YAML variants`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = """---
completed: true
deadline: 2023-12-25T15:00:00
updated: 2023-11-01_12-00-00
---
"""
        val task = Task.fromRaw(filename, raw)
        assertTrue(task.isDone)
        assertEquals(LocalDateTime.of(2023, 12, 25, 15, 0), task.dueDate)
        assertTrue(task.hasTime)
        assertEquals(LocalDateTime.of(2023, 11, 1, 12, 0), task.lastUpdate)
    }

    @Test
    fun `test empty file`() {
        val task = Task.fromRaw("2023-10-27_10-30-00.md", "")
        assertEquals("", task.title)
        assertEquals("", task.textContent)
        assertFalse(task.isDone)
    }

    @Test
    fun `test whitespace handling`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = """---
done: false
---


# Title with spaces  


Content with trailing spaces   
"""
        val task = Task.fromRaw(filename, raw)
        assertEquals("Title with spaces", task.title)
        assertEquals("Content with trailing spaces", task.textContent)
        
        val reconstructed = task.toRaw()
        // Should enforce standard spacing: 1 empty line after title, 1 at end
        assertTrue(reconstructed.contains("# Title with spaces\n\nContent with trailing spaces\n"))
    }

    @Test
    fun `test extra YAML preservation order and content`() {
        val raw = """---
tags: [todo, urgent]
creation_date: 2023-01-01_10-00-00
priority: high
---
# Test
"""
        val task = Task.fromRaw("test.md", raw)
        assertEquals(2, task.extraYaml.size)
        assertEquals("tags: [todo, urgent]", task.extraYaml[0])
        assertEquals("priority: high", task.extraYaml[1])
        
        val reconstructed = task.toRaw()
        assertTrue(reconstructed.contains("tags: [todo, urgent]"))
        assertTrue(reconstructed.contains("priority: high"))
        // Managed key should be written by toRaw
        assertTrue(reconstructed.contains("creation_date: 2023-01-01_10-00-00"))
    }

    @Test
    fun `test YAML with colon in value`() {
        val raw = """---
description: "This is a test: with a colon"
---
# Title
"""
        val task = Task.fromRaw("test.md", raw)
        assertEquals(1, task.extraYaml.size)
        assertEquals("description: \"This is a test: with a colon\"", task.extraYaml[0])
    }

    @Test
    fun `test multiple titles`() {
        val raw = """
# Title 1

# Title 2

Content
"""
        val task = Task.fromRaw("test.md", raw)
        assertEquals("Title 1", task.title)
        assertEquals("# Title 2\n\nContent", task.textContent)
    }

    @Test
    fun `test empty YAML section`() {
        val raw = """---
---
# Title
Content
"""
        val task = Task.fromRaw("test.md", raw)
        assertEquals("Title", task.title)
        assertEquals("Content", task.textContent)
        assertTrue(task.extraYaml.isEmpty())
    }

    @Test
    fun `test malformed YAML delimiter`() {
        val raw = """---
done: true
--
# Title
"""
        // Should not treat as YAML because second delimiter is --
        val task = Task.fromRaw("test.md", raw)
        assertEquals("", task.title)
        assertTrue(task.textContent.contains("done: true"))
    }
}
