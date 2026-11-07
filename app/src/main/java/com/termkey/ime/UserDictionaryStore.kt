package com.termkey.ime

import android.content.Context
import androidx.preference.PreferenceManager

class UserDictionaryStore(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun recordWordUsage(word: String) {
        val clean = word.lowercase().trim()
        if (clean.length < 2) return
        val countKey = "word_count_$clean"
        val count = prefs.getInt(countKey, 0) + 1
        prefs.edit().putInt(countKey, count).apply()

        if (count >= 3) {
            val words = prefs.getStringSet("user_words", mutableSetOf()) ?: mutableSetOf()
            if (!words.contains(clean)) {
                val newWords = words.toMutableSet()
                newWords.add(clean)
                prefs.edit().putStringSet("user_words", newWords).apply()
            }
        }
    }

    fun getUserWords(): Set<String> {
        return prefs.getStringSet("user_words", emptySet()) ?: emptySet()
    }

    fun lookupCandidates(prefix: String): List<String> {
        val clean = prefix.lowercase().trim()
        if (clean.isEmpty()) return emptyList()
        return getUserWords()
            .filter { it.startsWith(clean) }
            .toList()
    }

    fun clearDictionary() {
        val editor = prefs.edit()
        val words = getUserWords()
        for (word in words) {
            editor.remove("word_count_$word")
        }
        editor.remove("user_words")
        editor.apply()
    }
}
