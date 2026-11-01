package com.termkey.ime

data class SpanishCandidate(
    val text: String,
    val score: Int = 100
)

class SpanishLexiconStore {
    private val spanishWords = listOf(
        "hola", "gracias", "por", "favor", "buenos", "dias", "como", "estas", "bien", "tambien",
        "computadora", "teclado", "terminal", "archivo", "directorio", "ejecutar", "servidor",
        "conexion", "ayuda", "configurar", "sistema", "programa", "codigo", "desarrollo",
        "proyecto", "comando", "prueba", "error", "actualizar", "instalar", "saludos", "adios",
        "si", "no", "tal", "vez", "entonces", "porque", "cuando", "donde", "quien", "cual",
        "este", "esta", "estos", "estas", "aqui", "alli", "ahora", "despues", "antes", "todo",
        "nada", "algo", "alguien", "nadie", "cada", "otro", "otra", "mismo", "tanto", "cuanto",
        "poco", "mucho", "bastante", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete",
        "ocho", "nueve", "diez", "listar", "estado", "limpiar", "red", "puerto", "usuario",
        "clave", "contrasena", "version", "fecha", "hora", "proceso", "memoria", "disco",
        "ejecutar", "comando", "ayuda", "listar", "estado", "limpiar"
    )

    fun lookupCandidates(prefix: String): List<SpanishCandidate> {
        val clean = prefix.lowercase().trim()
        if (clean.isEmpty()) return emptyList()
        return spanishWords
            .filter { it.startsWith(clean) || levenshteinDistance(it, clean) <= 1 }
            .sortedBy { word ->
                if (word.startsWith(clean)) 0 else 1
            }
            .take(8)
            .map { SpanishCandidate(it) }
    }

    fun correctSpelling(word: String): String {
        val clean = word.lowercase().trim()
        if (clean.isEmpty() || spanishWords.contains(clean)) return word
        return spanishWords.minByOrNull { levenshteinDistance(it, clean) } ?: word
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                newCost[j] = minOf(
                    cost[j] + 1,
                    newCost[j - 1] + 1,
                    cost[j - 1] + match
                )
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}
