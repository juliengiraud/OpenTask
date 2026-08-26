package com.example.opentask.util

import org.junit.Test
import org.junit.Assert.*

class MergeRowsTest {

    @Test
    fun testIdenticalContent() {
        val f1 = """
            Line 1
            Line 2
            Line 3
        """.trimIndent()
        
        val f2 = """
            Line 1
            Line 2
            Line 3
        """.trimIndent()
        
        val result = generateConflict(f1, f2)
        assertEquals(f1, result)
    }

    @Test
    fun testSimpleChange() {
        val f1 = """
            Line 1
            Line 2 Original
            Line 3
        """.trimIndent()
        
        val f2 = """
            Line 1
            Line 2 Modified
            Line 3
        """.trimIndent()
        
        val expected = """
            Line 1
            <<<<<<< APP
            Line 2 Original
            =======
            Line 2 Modified
            >>>>>>> DISK
            Line 3
        """.trimIndent()
        
        val result = generateConflict(f1, f2)
        assertEquals(expected, result)
    }

    @Test
    fun testAdditionAtEnd() {
        val f1 = """
            Line 1
            Line 2
        """.trimIndent()
        
        val f2 = """
            Line 1
            Line 2
            Line 3
        """.trimIndent()
        
        val expected = """
            Line 1
            Line 2
            <<<<<<< APP
            =======
            Line 3
            >>>>>>> DISK
        """.trimIndent()
        
        val result = generateConflict(f1, f2)
        assertEquals(expected, result)
    }

    @Test
    fun testConflictMultipleLines() {
        val f1 = """
            Common Start
            Change A1
            Change A2
            Common End
        """.trimIndent()
        
        val f2 = """
            Common Start
            Change B1
            Change B2
            Common End
        """.trimIndent()
        
        val expected = """
            Common Start
            <<<<<<< APP
            Change A1
            Change A2
            =======
            Change B1
            Change B2
            >>>>>>> DISK
            Common End
        """.trimIndent()
        
        val result = generateConflict(f1, f2)
        assertEquals(expected, result)
    }

    @Test
    fun testTotalDivergence() {
        val f1 = """
            Apple
            Banana
        """.trimIndent()
        
        val f2 = """
            Cherry
            Date
        """.trimIndent()
        
        val expected = """
            <<<<<<< APP
            Apple
            Banana
            =======
            Cherry
            Date
            >>>>>>> DISK
        """.trimIndent()
        
        val result = generateConflict(f1, f2)
        assertEquals(expected, result)
    }
}
