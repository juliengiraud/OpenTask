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
        
        // Exact reconstruction check
        val reconstructed = task.toRaw()
        val expected = """---
creation_date: 2023-10-27_10-30-00
last_update: 2023-10-27_11-00-00
done: true
due_date: 2023-10-28
custom_prop: value
# comment line
---

# My Test Title

This is the content of the task.
It has multiple lines.
"""
        assertEquals(expected, reconstructed)
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
        
        val expected = "---\n" +
                "creation_date: 2023-10-27_10-30-00\n" +
                "last_update: 2023-10-27_10-30-00\n" +
                "---\n\n" +
                "# Simple Title\n\n" +
                "Just content.\n"
        assertEquals(expected, task.toRaw())
    }

    @Test
    fun `test parsing content only`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = "No title, just text."
        val task = Task.fromRaw(filename, raw)
        assertEquals("", task.title)
        assertEquals("No title, just text.", task.textContent)
        
        val expected = "---\n" +
                "creation_date: 2023-10-27_10-30-00\n" +
                "last_update: 2023-10-27_10-30-00\n" +
                "---\n\n" +
                "# \n\n" +
                "No title, just text.\n"
        assertEquals(expected, task.toRaw())
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
        
        val expected = "---\n" +
                "creation_date: 2023-10-27_10-30-00\n" +
                "last_update: 2023-11-01_12-00-00\n" +
                "done: true\n" +
                "due_date: 2023-12-25T15:00\n" +
                "hastime: true\n" +
                "---\n\n" +
                "# \n\n\n"
        assertEquals(expected, task.toRaw())
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
        val expected = """---
creation_date: 2023-10-27_10-30-00
last_update: 2023-10-27_10-30-00
---

# Title with spaces

Content with trailing spaces
"""
        assertEquals(expected, reconstructed)
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
        val task = Task.fromRaw("2023-01-01_10-00-00.md", raw)
        
        val reconstructed = task.toRaw()
        val expected = "---\n" +
                "creation_date: 2023-01-01_10-00-00\n" +
                "last_update: 2023-01-01_10-00-00\n" +
                "tags: [todo, urgent]\n" +
                "priority: high\n" +
                "---\n\n" +
                "# Test\n\n\n"
        assertEquals(expected, reconstructed)
    }

    @Test
    fun `test YAML with colon in value`() {
        val raw = """---
description: "This is a test: with a colon"
---
# Title
"""
        val task = Task.fromRaw("2023-01-01_10-00-00.md", raw)
        val reconstructed = task.toRaw()
        val expected = "---\n" +
                "creation_date: 2023-01-01_10-00-00\n" +
                "last_update: 2023-01-01_10-00-00\n" +
                "description: \"This is a test: with a colon\"\n" +
                "---\n\n" +
                "# Title\n\n\n"
        assertEquals(expected, reconstructed)
    }

    @Test
    fun `test multiple titles`() {
        val raw = """
# Title 1

# Title 2

Content
"""
        val task = Task.fromRaw("2023-01-01_10-00-00.md", raw)
        assertEquals("Title 1", task.title)
        assertEquals("# Title 2\n\nContent", task.textContent)
        
        val expected = "---\n" +
                "creation_date: 2023-01-01_10-00-00\n" +
                "last_update: 2023-01-01_10-00-00\n" +
                "---\n\n" +
                "# Title 1\n\n" +
                "# Title 2\n\nContent\n"
        assertEquals(expected, task.toRaw())
    }

    @Test
    fun `test empty YAML section`() {
        val raw = """---
---
# Title
Content
"""
        val task = Task.fromRaw("2023-01-01_10-00-00.md", raw)
        val reconstructed = task.toRaw()
        val expected = """---
creation_date: 2023-01-01_10-00-00
last_update: 2023-01-01_10-00-00
---

# Title

Content
"""
        assertEquals(expected, reconstructed)
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

    @Test
    fun `test trivial merge - local change only`() {
        val base = Task(
            filename = "2023-01-01_10-00-00.md",
            createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
            lastUpdate = LocalDateTime.of(2023, 1, 1, 10, 0),
            title = "Base Title", 
            textContent = "Base Content"
        )
        val local = base.copy(textContent = "Local Content")
        val remote = base.copy()
        
        val merged = Task.merge(base, local, remote)
        val expected = "---\n" +
                "creation_date: 2023-01-01_10-00-00\n" +
                "last_update: 2023-01-01_10-00-00\n" +
                "---\n\n" +
                "# Base Title\n\n" +
                "Local Content\n"
        assertEquals(expected, merged.toRaw())
    }

    @Test
    fun `test trivial merge - remote change only`() {
        val base = Task(
            filename = "2023-01-01_10-00-00.md",
            createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
            lastUpdate = LocalDateTime.of(2023, 1, 1, 10, 0),
            title = "Base Title", 
            textContent = "Base Content"
        )
        val local = base.copy()
        val remote = base.copy(
            title = "Remote Title",
            lastUpdate = LocalDateTime.of(2023, 1, 1, 11, 0)
        )
        
        val merged = Task.merge(base, local, remote)
        val expected = "---\n" +
                "creation_date: 2023-01-01_10-00-00\n" +
                "last_update: 2023-01-01_11-00-00\n" +
                "---\n\n" +
                "# Remote Title\n\n" +
                "Base Content\n"
        assertEquals(expected, merged.toRaw())
    }

    @Test
    fun `test identical changes merge`() {
        val base = Task(
            filename = "2023-01-01_10-00-00.md",
            createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
            lastUpdate = LocalDateTime.of(2023, 1, 1, 10, 0),
            title = "Base", 
            textContent = "Base"
        )
        val local = base.copy(title = "New", textContent = "New")
        val remote = base.copy(
            title = "New", 
            textContent = "New",
            lastUpdate = LocalDateTime.of(2023, 1, 1, 11, 0)
        )
        
        val merged = Task.merge(base, local, remote)
        val expected = "---\n" +
                "creation_date: 2023-01-01_10-00-00\n" +
                "last_update: 2023-01-01_11-00-00\n" +
                "---\n\n" +
                "# New\n\n" +
                "New\n"
        assertEquals(expected, merged.toRaw())
    }

    @Test
    fun `test conflict in title and body`() {
        val base = Task(
            filename = "2023-01-01_10-00-00.md",
            createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
            lastUpdate = LocalDateTime.of(2023, 1, 1, 10, 0),
            title = "Base", 
            textContent = "Base content"
        )
        val local = base.copy(title = "Local Title", textContent = "Local content")
        val remote = base.copy(
            title = "Remote Title", 
            textContent = "Remote content",
            lastUpdate = LocalDateTime.of(2023, 1, 1, 11, 0)
        )
        
        val merged = Task.merge(base, local, remote)
        val expected = "---\n" +
                "creation_date: 2023-01-01_10-00-00\n" +
                "last_update: 2023-01-01_11-00-00\n" +
                "---\n\n" +
                "# CONFLICT: Local(Local Title) vs Remote(Remote Title)\n\n" +
                "<<<<<<< External\n" +
                "Remote content\n" +
                "=======\n" +
                "Local content\n" +
                ">>>>>>> Local\n"
        assertEquals(expected, merged.toRaw())
    }

    @Test
    fun `test YAML conflict - remote always wins`() {
        val base = Task(
            filename = "2023-01-01_10-00-00.md",
            createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
            lastUpdate = LocalDateTime.of(2023, 1, 1, 10, 0),
            isDone = false
        )
        val local = base.copy(isDone = true)
        val remote = base.copy(
            isDone = false, 
            extraYaml = listOf("tags: [remote]"),
            lastUpdate = LocalDateTime.of(2023, 1, 1, 11, 0)
        )
        
        val merged = Task.merge(base, local, remote)
        val expected = "---\n" +
                "creation_date: 2023-01-01_10-00-00\n" +
                "last_update: 2023-01-01_11-00-00\n" +
                "tags: [remote]\n" +
                "---\n\n" +
                "# \n\n\n"
        assertEquals(expected, merged.toRaw())
    }
}
