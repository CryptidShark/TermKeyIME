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

    @Test
    fun testSwipePrefixMatching() {
        val candidates = store.lookupCandidates("term")
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.any { it.text == "terminal" })
    }

    @Test
    fun testSwipeFuzzyCorrection() {
        val corrected = store.correctSpelling("gracias")
        assertEquals("gracias", corrected)
        val correctedTypos = store.correctSpelling("gacias")
        assertTrue(correctedTypos == "gracias" || store.lookupCandidates("gacias").isNotEmpty())
    }
}
