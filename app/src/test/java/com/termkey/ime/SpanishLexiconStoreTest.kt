package com.termkey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpanishLexiconStoreTest {

    private val store = SpanishLexiconStore()

    @Test
    fun testLookupCandidates() {
        val results = store.lookupCandidates("hol")
        assertTrue(results.isNotEmpty())
        assertEquals("hola", results[0].text)
    }

    @Test
    fun testCorrectSpelling() {
        val corrected = store.correctSpelling("gracias")
        assertEquals("gracias", corrected)
    }
}
