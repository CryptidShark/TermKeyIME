package com.termkey.ime

data class EnglishCandidate(
    val text: String,
    val score: Int = 100,
)

class EnglishLexiconStore {
    private val englishWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "terminal", "keyboard", "code", "run", "test", "build", "app", "android", "kotlin",
        "java", "git", "commit", "push", "pull", "branch", "file", "directory", "server",
        "system", "config", "debug", "error", "success", "update", "install", "device",
    )

    fun lookupCandidates(prefix: String): List<EnglishCandidate> {
        val clean = prefix.lowercase().trim()
        if (clean.isEmpty()) return emptyList()
        return englishWords.asSequence()
            .filter { it.startsWith(clean) || (levenshteinDistance(it, clean) <= 1) }
            .sortedBy { word ->
                when {
                    word == clean -> 0
                    word.startsWith(clean) -> 1
                    else -> 2
                }
            }
            .take(10)
            .map { EnglishCandidate(it) }
            .toList()
    }

    @Suppress("unused")
    fun correctSpelling(word: String): String {
        val clean = word.lowercase().trim()
        if (clean.isEmpty() || englishWords.contains(clean)) return word
        return englishWords.minByOrNull { levenshteinDistance(it, clean) } ?: word
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                newCost[j] = minOf(
                    cost[j] + 1,
                    newCost[j - 1] + 1,
                    cost[j - 1] + match,
                )
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}
