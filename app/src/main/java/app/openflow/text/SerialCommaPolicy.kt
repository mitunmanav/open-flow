package app.openflow.text

/**
 * Conservative spoken serial comma: "milk eggs and bread" → "milk, eggs, and bread".
 * Three content words, last joined by and/or, no existing comma. Verbs/pronouns skip.
 */
object SerialCommaPolicy {

    private val skip = setOf(
        "i", "we", "you", "they", "he", "she", "it",
        "me", "him", "her", "us", "them",
        "my", "your", "our", "their", "his",
        "the", "a", "an", "this", "that", "these", "those",
        "is", "are", "was", "were", "be", "been", "am",
        "to", "of", "in", "on", "at", "for", "with", "from",
        "if", "when", "because", "so", "but", "or",
        "have", "has", "had", "do", "did", "does", "will", "would",
        "can", "could", "should", "may", "might",
        "go", "goes", "went", "going", "come", "came",
        "see", "saw", "make", "made", "get", "got",
        "take", "took", "want", "wanted", "think", "thought",
        "know", "knew", "said", "say", "tell", "told",
        "give", "gave", "left", "leave", "start", "started",
        "try", "tried", "use", "used", "need", "needs", "needed",
        "and",
        "one", "two", "three", "four", "five", "six",
        "seven", "eight", "nine", "ten", "eleven", "twelve",
        "first", "second", "third", "fourth", "fifth",
    )

    private val tripleAnd = Regex(
        """(?i)\b([A-Za-z][A-Za-z'-]*)\s+([A-Za-z][A-Za-z'-]*)\s+(and|or)\s+([A-Za-z][A-Za-z'-]*)\b"""
    )

    fun apply(t: String): String {
        if (t.isBlank() || t.contains(',')) return t
        return tripleAnd.replace(t) { m ->
            val a = m.groupValues[1]
            val b = m.groupValues[2]
            val join = m.groupValues[3]
            val c = m.groupValues[4]
            if (a.lowercase() in skip || b.lowercase() in skip || c.lowercase() in skip) {
                m.value
            } else {
                "$a, $b, $join $c"
            }
        }
    }
}
