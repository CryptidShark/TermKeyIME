package com.termkey.ime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserDictionaryStoreTest {

    @Test
    fun testRecordWordThreeTimes() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val store = UserDictionaryStore(ctx)
        store.clearDictionary()
        repeat(3) { store.recordWordUsage("customterm") }
        assertTrue(store.getUserWords().contains("customterm"))
    }

    @Test
    fun testWordNotLearnedBeforeThreeUses() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val store = UserDictionaryStore(ctx)
        store.clearDictionary()
        repeat(2) { store.recordWordUsage("twiceword") }
        assertTrue(!store.getUserWords().contains("twiceword"))
    }
}
