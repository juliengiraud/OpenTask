package com.example.opentask.util

import org.junit.Test
import org.junit.Assert.*

class MergeWordsTest {

    @Test
    fun testIdenticalSentences() {
        val f1 = "The quick brown fox"
        val f2 = "The quick brown fox"
        
        val result = generateConflict(f1, f2, separator = " ")
        assertEquals(f1, result)
    }

    @Test
    fun testSingleWordDifference() {
        val f1 = "The quick brown fox"
        val f2 = "The fast brown fox"
        
        // joinToString(" ") adds spaces around markers
        val expected = "The <<<<<<< APP quick ======= fast >>>>>>> DISK brown fox"
        
        val result = generateConflict(f1, f2, separator = " ")
        assertEquals(expected, result)
    }

    @Test
    fun testWordAdditionAtEnd() {
        val f1 = "Hello world"
        val f2 = "Hello world today"
        
        val expected = "Hello world <<<<<<< APP ======= today >>>>>>> DISK"
        
        val result = generateConflict(f1, f2, separator = " ")
        assertEquals(expected, result)
    }

    @Test
    fun testWordDeletionInMiddle() {
        val f1 = "I love coding in Kotlin"
        val f2 = "I love Kotlin"
        
        // "coding in" vs empty
        val expected = "I love <<<<<<< APP coding in ======= >>>>>>> DISK Kotlin"
        
        val result = generateConflict(f1, f2, separator = " ")
        assertEquals(expected, result)
    }

    @Test
    fun testMultipleWordChanges() {
        val f1 = "The cat sat on the mat"
        val f2 = "A dog lay on the mat"
        
        val expected = "<<<<<<< APP The cat sat ======= A dog lay >>>>>>> DISK on the mat"
        
        val result = generateConflict(f1, f2, separator = " ")
        assertEquals(expected, result)
    }
}
