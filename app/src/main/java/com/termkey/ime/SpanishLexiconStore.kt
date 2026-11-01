package com.termkey.ime

data class SpanishCandidate(
    val text: String,
    val score: Int = 100,
)

class SpanishLexiconStore {
    private val spanishWords = listOf(
        // Saludos y conversación
        "hola", "gracias", "por", "favor", "buenos", "dias", "tardes", "noches", "como", "estas",
        "bien", "tambien", "adios", "saludos", "hasta", "luego", "por favor", "disculpa", "perdon",
        // Pronombres y determinantes
        "yo", "tu", "el", "ella", "nosotros", "vosotros", "ellos", "ellas", "mi", "tu", "su",
        "este", "esta", "estos", "estas", "ese", "esa", "esos", "esas", "aquel", "aquella",
        "que", "cual", "quien", "cuyo", "cuanto", "todo", "nada", "algo", "alguien", "nadie",
        // Verbos frecuentes
        "ser", "estar", "tener", "hacer", "ir", "decir", "ver", "dar", "saber", "querer",
        "llegar", "pasar", "deber", "poner", "parecer", "quedar", "creer", "hablar", "llevar",
        "dejar", "seguir", "encontrar", "llamar", "venir", "pensar", "salir", "volver", "tomar",
        "conocer", "vivir", "sentir", "tratar", "mirar", "contar", "empezar", "esperar", "buscar",
        "existir", "entrar", "trabajar", "escribir", "perder", "producir", "ocurrir", "entender",
        "pedir", "recibir", "recordar", "terminar", "permitir", "aparecer", "conseguir", "comenzar",
        // Adverbios, conjunciones y preposiciones
        "si", "no", "tal", "vez", "entonces", "porque", "cuando", "donde", "aqui", "alli",
        "ahora", "despues", "antes", "mas", "menos", "muy", "tan", "como", "pero", "aunque",
        "sin", "con", "para", "desde", "hasta", "contra", "hacia", "sobre", "bajo", "entre",
        // Términos técnicos, de terminal y desarrollo
        "computadora", "teclado", "terminal", "archivo", "directorio", "ejecutar", "servidor",
        "conexion", "ayuda", "configurar", "sistema", "programa", "codigo", "desarrollo",
        "proyecto", "comando", "prueba", "error", "actualizar", "instalar", "red", "puerto",
        "usuario", "clave", "contrasena", "version", "fecha", "hora", "proceso", "memoria",
        "disco", "git", "commit", "push", "pull", "branch", "build", "gradle", "kotlin", "java",
        "android", "studio", "smartphone", "pantalla", "tactil", "deslizar", "prediccion",
    )

    fun lookupCandidates(prefix: String): List<SpanishCandidate> {
        val clean = prefix.lowercase().trim()
        if (clean.isEmpty()) return emptyList()
        return spanishWords.asSequence()
            .filter { it.startsWith(clean) || (levenshteinDistance(it, clean) <= 1) }
            .sortedBy { word ->
                when {
                    word == clean -> 0
                    word.startsWith(clean) -> 1
                    else -> 2
                }
            }
            .take(10)
            .map { SpanishCandidate(it) }
            .toList()
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
