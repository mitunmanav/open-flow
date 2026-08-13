package app.openflow.text

data class LearnPair(val from: String, val to: String)

/**
 * On-device word learn from a user fix after dictation.
 * No cloud. No new STT model.
 */
object LearnEngine {

    internal val COMMON: Set<String> = setOf(
        "the", "a", "an", "to", "of", "and", "or", "is", "it", "in", "on",
        "for", "with", "at", "as", "be", "this", "that", "you", "we", "they",
        "i", "my", "me", "was", "are", "have", "has", "not", "but", "if",
        "so", "just", "then", "than", "from", "by", "about", "into", "over",
        "after", "before", "your", "our"
    )

    fun pairsFromEdit(inserted: String, edited: String): List<LearnPair> {
        val a = inserted.trim()
        val b = edited.trim()
        if (a == b || a.isEmpty() || b.isEmpty()) return emptyList()

        val fromWords = splitWords(a)
        val toWords = splitWords(b)
        if (fromWords.isEmpty() || toWords.isEmpty()) return emptyList()
        if (isHugeRewrite(fromWords, toWords)) return emptyList()

        val pairs = LinkedHashSet<LearnPair>()
        val close = kotlin.math.abs(fromWords.size - toWords.size) <= 1
        if (close) {
            val n = minOf(fromWords.size, toWords.size)
            for (i in 0 until n) {
                val f = fromWords[i]
                val t = toWords[i]
                if (shouldLearn(f, t)) pairs.add(LearnPair(f, t))
            }
        }

        val fromLower = fromWords.map { it.lowercase() }.toSet()
        val toLower = toWords.map { it.lowercase() }.toSet()
        val onlyFrom = fromWords.filter { it.lowercase() !in toLower }
        val onlyTo = toWords.filter { it.lowercase() !in fromLower }
        if (onlyFrom.size == onlyTo.size) {
            for (i in onlyFrom.indices) {
                if (shouldLearn(onlyFrom[i], onlyTo[i])) {
                    pairs.add(LearnPair(onlyFrom[i], onlyTo[i]))
                }
            }
        }

        return pairs.toList()
    }

    fun shouldLearn(from: String, to: String): Boolean {
        val f = from.trim()
        val t = to.trim()
        if (f.equals(t, ignoreCase = true)) return false
        if (f.length < 2 || f.length > 40) return false
        if (t.length < 2 || t.length > 40) return false
        if (f.lowercase() in COMMON || t.lowercase() in COMMON) return false
        return true
    }

    fun rewriteMap(pairs: List<LearnPair>): Map<String, String> =
        pairs.filter { shouldLearn(it.from, it.to) }.associate { it.from to it.to }

    fun shouldWatch(nowMs: Long, insertedAtMs: Long, windowMs: Long = 45_000): Boolean {
        if (insertedAtMs <= 0) return false
        val delta = nowMs - insertedAtMs
        return delta > 0 && delta <= windowMs
    }

    fun isOwnSet(edited: String, lastInserted: String): Boolean =
        edited.trim() == lastInserted.trim()

    private fun splitWords(text: String): List<String> =
        text.split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun isHugeRewrite(from: List<String>, to: List<String>): Boolean {
        val n = maxOf(from.size, to.size)
        if (n == 0) return false
        val m = minOf(from.size, to.size)
        var differ = kotlin.math.abs(from.size - to.size)
        for (i in 0 until m) {
            if (!from[i].equals(to[i], ignoreCase = true)) differ++
        }
        return differ > 3 && differ * 2 > n
    }
}
