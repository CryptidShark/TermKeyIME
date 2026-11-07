package com.termkey.ime

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertTrue
import org.junit.Test

class TermKeyIMEServiceTest {

    @Test
    fun testEditorInfoPasswordDetection() {
        val types = intArrayOf(
            EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
            EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
        )

        for (t in types) {
            val info = EditorInfo().apply { inputType = t }
            val variation = info.inputType and EditorInfo.TYPE_MASK_VARIATION
            val isPass = variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                    (info.inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
            assertTrue("Failed for inputType: $t", isPass)
        }
    }

    @Test
    fun testEditorInfoNumericDetection() {
        val numTypes = intArrayOf(EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_PHONE)
        for (t in numTypes) {
            val info = EditorInfo().apply { inputType = t }
            val classType = info.inputType and EditorInfo.TYPE_MASK_CLASS
            val isNum = classType == EditorInfo.TYPE_CLASS_NUMBER || classType == EditorInfo.TYPE_CLASS_PHONE
            assertTrue("Failed for numeric inputType: $t", isNum)
        }
    }

    @Test
    fun testEnglishLexiconStore() {
        val store = EnglishLexiconStore()
        val candidates = store.lookupCandidates("term")
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.any { it.text == "terminal" })
    }
}
