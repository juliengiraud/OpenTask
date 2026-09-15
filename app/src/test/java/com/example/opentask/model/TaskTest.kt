package com.example.opentask.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
custom_prop: value
# comment line
---

# My Test Title

This is the content of the task.
It has multiple lines.
"""
        val task = Task.fromRaw(filename, raw)
        
        assertEquals("My Test Title", task.title)
        assertEquals("This is the content of the task.\nIt has multiple lines.\n", task.textContent)
        assertTrue(task.isDone)
        assertEquals(LocalDateTime.of(2023, 10, 28, 0, 0), task.dueDate)
        
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
        assertEquals("Just content.\n", task.textContent)
        assertEquals(LocalDateTime.of(2023, 10, 27, 10, 30), task.createdAt)

        val expected = """---
creation_date: 2023-10-27_10-30-00
last_update: 2023-10-27_10-30-00
---

# Simple Title

Just content.
"""
        assertEquals(expected, task.toRaw())
    }

    @Test
    fun `test parsing content without title marker uses first line as title`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = "No title, just text.\n"
        val task = Task.fromRaw(filename, raw)
        // Title should be inferred from the first non-empty line
        assertEquals("No title, just text.", task.title)
        assertEquals("No title, just text.\n", task.textContent)
        
        // Reconstruction should now use the inferred title
        val expected = "---\n" +
                "creation_date: 2023-10-27_10-30-00\n" +
                "last_update: 2023-10-27_10-30-00\n" +
                "---\n\n" +
                "# No title, just text.\n\n" +
                "No title, just text.\n"
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
    fun `test title trimming and body whitespace preservation`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = """---
done: false
---


# Title with spaces  


Content with trailing spaces   
"""
        val task = Task.fromRaw(filename, raw)
        // Title should be trimmed
        assertEquals("Title with spaces", task.title)
        // Body should preserve leading and trailing whitespace after the mandatory 1-line skip
        assertEquals("\nContent with trailing spaces   \n", task.textContent)
        
        val reconstructed = task.toRaw()
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
                "# Test\n\n"
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
                "# Title\n\n"
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
        assertEquals("# Title 2\n\nContent\n", task.textContent)
        
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
        // Consequently, the first line "---" is treated as part of the body
        // and becomes the inferred title.
        val task = Task.fromRaw("test.md", raw)
        assertEquals("---", task.title)
        assertTrue(task.textContent.contains("done: true"))
    }

    @Test
    fun `test trivial merge - local change only`() {
        val base = Task(
            filename = "2023-01-01_10-00-00.md",
            createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
            lastUpdate = LocalDateTime.of(2023, 1, 1, 10, 0),
            title = "Base Title", 
            textContent = "Base Content\n"
        )
        val local = base.copy(textContent = "Local Content\n")
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
            textContent = "Base Content\n"
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
            textContent = "Base\n"
        )
        val local = base.copy(title = "New", textContent = "New\n")
        val remote = base.copy(
            title = "New", 
            textContent = "New\n",
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
            textContent = "Base content\n"
        )
        val local = base.copy(title = "Local Title", textContent = "Local content\n")
        val remote = base.copy(
            title = "Remote Title", 
            textContent = "Remote content\n",
            lastUpdate = LocalDateTime.of(2023, 1, 1, 11, 0)
        )
        
        val merged = Task.merge(base, local, remote)

        val expected = """---
creation_date: 2023-01-01_10-00-00
last_update: 2023-01-01_11-00-00
---

# <<<<<<< APP Local ======= Remote >>>>>>> DISK Title

<<<<<<< APP
Local content
=======
Remote content
>>>>>>> DISK
"""
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
                "# \n\n"
        assertEquals(expected, merged.toRaw())
    }

    @Test
    fun `test auto-title inference from body`() {
        val filename = "2023-10-27_10-30-00.md"
        val raw = """---
done: false
---

# 

   
  First non-empty line  
  Second line
"""
        val task = Task.fromRaw(filename, raw)
        // Should ignore leading blank lines and trim the result
        assertEquals("First non-empty line", task.title)
        
        val reconstructed = task.toRaw()
        assertTrue(reconstructed.contains("# First non-empty line\n"))
    }

    @Test
    fun `test parsing title without trailing blank line`() {
        val filename = "2023-10-27_10-30-00.md"
        // No empty line between title and content
        val raw = """---
done: false
---
# My Title
Content starts here
"""
        val task = Task.fromRaw(filename, raw)
        assertEquals("My Title", task.title)
        assertEquals("Content starts here\n", task.textContent)

        val reconstructed = task.toRaw()
        // Reconstruction should fix the format by adding the missing blank line
        val expected = """---
creation_date: 2023-10-27_10-30-00
last_update: 2023-10-27_10-30-00
---

# My Title

Content starts here
"""
        assertEquals(expected, reconstructed)
    }

    @Test
    fun `test trailing newline enforcement`() {
        val filename = "2023-10-27_10-30-00.md"
        
        // Case 1: Body without trailing newline
        val task1 = Task(filename = filename, title = "T", textContent = "Content")
        val raw1 = task1.toRaw()
        assertTrue(raw1.endsWith("Content\n"))
        
        // Case 2: Body with trailing spaces but no newline
        val task2 = Task(filename = filename, title = "T", textContent = "Content  ")
        val raw2 = task2.toRaw()
        assertTrue(raw2.endsWith("Content  \n"))
        
        // Case 3: Body already ends with newline
        val task3 = Task(filename = filename, title = "T", textContent = "Content\n")
        val raw3 = task3.toRaw()
        assertEquals("Content\n", raw3.takeLast(8))
        assertFalse(raw3.endsWith("Content\n\n")) // Should not add extra newline
    }
}
