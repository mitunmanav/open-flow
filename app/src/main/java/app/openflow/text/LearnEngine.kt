package app.openflow.text

data class LearnPair(val from: String, val to: String)

data class LearnConfirm(val persisted: Boolean, val pendingHits: Int)

data class LearnSides(
    val sides: Map<String, Set<String>> = emptyMap(),
    val auto: Set<String> = emptySet(),
    val manual: Set<String> = emptySet()
)

/**
 * On-device word learn from a user fix after dictation.
 * No cloud. No new STT model.
 * Short close pairs stay shy: rewrite only bare/same-bag hits.
 */
object LearnEngine {

    internal val COMMON: Set<String> = setOf(
        "the", "a", "an", "to", "of", "and", "or", "is", "it", "in", "on",
        "for", "with", "at", "as", "be", "this", "that", "you", "we", "they",
        "i", "my", "me", "was", "are", "have", "has", "not", "but", "if",
        "so", "just", "then", "than", "from", "by", "about", "into", "over",
        "after", "before", "your", "our"
    )

    private const val MANUAL_MARK = "*"
    private val SUFFIXES = listOf("'s", "s", "ed", "ing", "er", "est")
    private val ROMAN = setOf("II", "III", "IV")

    @Volatile
    private var store: LearnSides = LearnSides()

    private val pending = LinkedHashMap<String, PendingLearn>()

    @Volatile
    var persistHook: ((String) -> Unit)? = null

    @Volatile
    var pendingHook: ((String) -> Unit)? = null

    fun resetLearn() {
        store = LearnSides()
        pending.clear()
        persistHook = null
        pendingHook = null
    }

    fun sideBags(): Map<String, Set<String>> = store.sides

    fun autoKeys(): Set<String> = store.auto

    fun isAuto(from: String): Boolean = from.lowercase() in store.auto

    fun putAuto(from: String, bag: Set<String>) {
        val k = from.lowercase()
        store = store.copy(
            sides = store.sides + (k to bag),
            auto = store.auto + k,
            manual = store.manual - k
        )
        persist()
    }

    fun putManual(from: String) {
        val k = from.lowercase()
        store = store.copy(
            sides = store.sides - k,
            auto = store.auto - k,
            manual = store.manual + k
        )
        persist()
    }

    fun drop(from: String) {
        val k = from.lowercase()
        store = store.copy(
            sides = store.sides - k,
            auto = store.auto - k,
            manual = store.manual - k
        )
        pending.entries.removeAll { it.value.from == k || it.value.to == k }
        persist()
        persistPending()
    }

    fun clearAll() {
        store = LearnSides()
        pending.clear()
        persist()
        persistPending()
    }

    fun loadSides(raw: String) {
        store = decodeSides(raw)
    }

    fun loadPending(raw: String) {
        pending.clear()
        if (raw.isBlank()) return
        for (line in raw.split('\n')) {
            val t = line.trim()
            if (t.isEmpty() || '=' !in t || '|' !in t) continue
            val from = t.substringBefore('=').trim().lowercase()
            val rest = t.substringAfter('=')
            val parts = rest.split('|')
            if (parts.size < 2 || from.isEmpty()) continue
            val to = parts[0].trim().lowercase()
            val hits = parts[1].trim().toIntOrNull() ?: continue
            val bag = if (parts.size < 3 || parts[2].isBlank()) {
                emptySet()
            } else {
                parts[2].split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
            }
            if (to.isEmpty()) continue
            pending[pendingKey(from, to, bag)] = PendingLearn(from, to, bag, hits)
        }
    }

    fun encodePending(): String =
        pending.values.joinToString("\n") { p ->
            val bag = p.bag.sorted().joinToString(",")
            "${p.from}=${p.to}|${p.hits}|$bag"
        }

    fun noteAutoCandidate(from: String, to: String, bag: Set<String>): LearnConfirm {
        val f = from.trim().lowercase()
        val t = to.trim().lowercase()
        val b = bag.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        if (f.isEmpty() || t.isEmpty() || f == t) return LearnConfirm(false, 0)
        if (t in store.auto) {
            drop(t)
            return LearnConfirm(false, 0)
        }
        val key = pendingKey(f, t, b)
        val hits = (pending[key]?.hits ?: 0) + 1
        if (hits < 2) {
            pending[key] = PendingLearn(f, t, b, hits)
            persistPending()
            return LearnConfirm(false, hits)
        }
        putAuto(from, b)
        pending.remove(key)
        persistPending()
        return LearnConfirm(true, hits)
    }

    fun encodeSides(): String = encodeSides(store.sides, store.auto, store.manual)

    fun encodeSides(
        sides: Map<String, Set<String>>,
        auto: Set<String>,
        manual: Set<String>
    ): String {
        val lines = LinkedHashSet<String>()
        for (m in manual.map { it.lowercase() }.sorted()) {
            lines.add("$m=$MANUAL_MARK")
        }
        for (a in auto.map { it.lowercase() }.sorted()) {
            if (a in manual.map { it.lowercase() }.toSet()) continue
            val bag = sides[a].orEmpty().map { it.lowercase() }.sorted().joinToString(",")
            lines.add("$a=$bag")
        }
        return lines.joinToString("\n")
    }

    fun decodeSides(raw: String): LearnSides {
        if (raw.isBlank()) return LearnSides()
        val sides = LinkedHashMap<String, Set<String>>()
        val auto = LinkedHashSet<String>()
        val manual = LinkedHashSet<String>()
        for (line in raw.split('\n')) {
            val t = line.trim()
            if (t.isEmpty() || '=' !in t) continue
            val key = t.substringBefore('=').trim().lowercase()
            val rhs = t.substringAfter('=').trim()
            if (key.isEmpty()) continue
            if (rhs == MANUAL_MARK) {
                manual.add(key)
                continue
            }
            auto.add(key)
            sides[key] = if (rhs.isEmpty()) {
                emptySet()
            } else {
                rhs.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
            }
        }
        return LearnSides(sides = sides, auto = auto, manual = manual)
    }

    fun pairsFromEdit(inserted: String, edited: String): List<LearnPair> {
        val a = inserted.trim()
        val b = edited.trim()
        if (a == b || a.isEmpty() || b.isEmpty()) return emptyList()

        val fromWords = splitWords(a)
        val toWords = splitWords(b)
        if (fromWords.isEmpty() || toWords.isEmpty()) return emptyList()
        if (fromWords.size == 1 && toWords.size != 1) return emptyList()
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
        if (splitWords(f).size != 1 || splitWords(t).size != 1) return false
        return true
    }

    fun isAmbiguous(from: String, to: String): Boolean {
        val a = lettersOf(from)
        val b = lettersOf(to)
        if (a.isEmpty() || b.isEmpty()) return false
        return minOf(a.length, b.length) <= 4 && levenshtein(a, b) <= 2
    }

    fun sideBag(sentence: String, fromHit: String): Set<String> {
        val tokens = splitTokens(sentence)
        val fromToks = splitTokens(fromHit).filter { isWordToken(it) }
        val start = tokens.indices.firstOrNull { matchesAt(tokens, it, fromToks) }
            ?: return sideWords(tokens, emptySet())
        return sideFromTokens(tokens, start, fromToks.size)
    }

    fun reverseKey(from: String, to: String, existing: Map<String, String>): String? =
        existing.entries.find {
            it.key.equals(to, ignoreCase = true) && it.value.equals(from, ignoreCase = true)
        }?.key

    fun wouldCycle(from: String, to: String, existing: Map<String, String>): Boolean =
        reverseKey(from, to, existing) != null

    fun rewriteMap(pairs: List<LearnPair>): Map<String, String> =
        pairs.filter { shouldLearn(it.from, it.to) }.associate { it.from to it.to }

    fun shouldWatch(nowMs: Long, insertedAtMs: Long, windowMs: Long = 45_000): Boolean {
        if (insertedAtMs <= 0) return false
        val delta = nowMs - insertedAtMs
        return delta > 0 && delta <= windowMs
    }

    fun isOwnSet(edited: String, lastInserted: String): Boolean =
        edited.trim() == lastInserted.trim()

    fun applyPairs(
        text: String,
        replacements: Map<String, String>,
        sides: Map<String, Set<String>> = emptyMap(),
        autoKeys: Set<String> = emptySet()
    ): String {
        if (replacements.isEmpty() || text.isEmpty()) return text
        val tokens = splitTokens(text).toMutableList()
        if (tokens.isEmpty()) return text
        val auto = autoKeys.map { it.lowercase() }.toSet()
        val entries = replacements.entries
            .filter { it.key.isNotBlank() }
            .sortedByDescending { it.key.length }
        for ((from, to) in entries) {
            val fromToks = splitTokens(from).filter { isWordToken(it) }
            if (fromToks.isEmpty()) continue
            val fromLow = from.lowercase()
            val shy = fromLow in auto || fromToks.first().lowercase() in auto
            val amb = isAmbiguous(from, to)
            var i = 0
            while (i < tokens.size) {
                var matchedExact = matchesAt(tokens, i, fromToks)
                var suffixMatch: Pair<String, String>? = null

                if (!matchedExact) {
                    if (fromToks.size == 1 && isWordToken(tokens[i])) {
                        suffixMatch = matchWithSuffix(tokens[i], fromToks[0])
                    }
                    if (suffixMatch == null) {
                        i++
                        continue
                    }
                }

                val next = wordAfter(tokens, i, fromToks.size)
                if (next != null && isTitleCaseWord(next)) {
                    i++
                    continue
                }
                if (shy && next != null && isRomanNumeral(next)) {
                    i++
                    continue
                }
                val apply = when {
                    !shy -> true
                    !amb -> true
                    else -> {
                        val side = sideFromTokens(tokens, i, fromToks.size)
                        val stored = sides[fromLow]
                            ?: sides[fromToks.first().lowercase()]
                            ?: emptySet()
                        side.isEmpty() || side == stored
                    }
                }
                if (apply) {
                    if (suffixMatch != null) {
                        val (_, suffix) = suffixMatch
                        val cased = transferCase(tokens[i].removeSuffix(suffix), to)
                        tokens[i] = cased + suffix
                        i++
                    } else {
                        val replacement = transferCase(tokens[i], to)
                        i = replaceSpan(tokens, i, fromToks.size, replacement)
                    }
                    continue
                }
                i++
            }
        }
        return tokens.joinToString("")
    }

    fun levenshtein(left: String, right: String): Int {
        val s = left.lowercase()
        val t = right.lowercase()
        if (s == t) return 0
        if (s.isEmpty()) return t.length
        if (t.isEmpty()) return s.length
        val prev = IntArray(t.length + 1) { it }
        val cur = IntArray(t.length + 1)
        for (i in 1..s.length) {
            cur[0] = i
            for (j in 1..t.length) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            for (j in prev.indices) prev[j] = cur[j]
        }
        return prev[t.length]
    }

    private fun persist() {
        persistHook?.invoke(encodeSides())
    }

    private fun persistPending() {
        pendingHook?.invoke(encodePending())
    }

    private fun pendingKey(from: String, to: String, bag: Set<String>): String =
        "$from>$to|${bag.sorted().joinToString(",")}"

    private data class PendingLearn(
        val from: String,
        val to: String,
        val bag: Set<String>,
        val hits: Int,
    )

    private fun isRomanNumeral(tok: String): Boolean = tok.uppercase() in ROMAN

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

    private fun lettersOf(token: String): String =
        token.lowercase().filter { it.isLetter() }

    internal fun splitTokens(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val out = ArrayList<String>()
        val r = Regex("[A-Za-z0-9]+(?:'[A-Za-z0-9]+)?|[^A-Za-z0-9]+")
        r.findAll(text).forEach { out.add(it.value) }
        return out
    }

    internal fun isWordToken(tok: String): Boolean =
        tok.firstOrNull()?.isLetterOrDigit() == true

    internal fun isTitleCaseWord(tok: String): Boolean {
        if (tok.length < 2) return false
        if (tok[0] !in 'A'..'Z') return false
        return tok.drop(1).all { it.isLetter() }
    }

    private fun matchesAt(tokens: List<String>, start: Int, fromToks: List<String>): Boolean {
        if (fromToks.isEmpty() || start !in tokens.indices) return false
        var i = start
        var n = 0
        while (i < tokens.size && n < fromToks.size) {
            if (!isWordToken(tokens[i])) {
                if (n == 0) return false
                i++
                continue
            }
            if (!tokens[i].equals(fromToks[n], ignoreCase = true)) return false
            n++
            i++
        }
        return n == fromToks.size
    }

    private fun wordAfter(tokens: List<String>, start: Int, fromWordCount: Int): String? {
        var seen = 0
        var i = start
        while (i < tokens.size) {
            if (isWordToken(tokens[i])) {
                seen++
                if (seen > fromWordCount) return tokens[i]
            }
            i++
        }
        return null
    }

    private fun sideFromTokens(tokens: List<String>, hitStart: Int, fromWordCount: Int): Set<String> {
        val skip = hitWordIndices(tokens, hitStart, fromWordCount)
        return sideWords(tokens, skip)
    }

    private fun hitWordIndices(tokens: List<String>, hitStart: Int, fromWordCount: Int): Set<Int> {
        val skip = LinkedHashSet<Int>()
        var seen = 0
        var i = hitStart
        while (i < tokens.size && seen < fromWordCount) {
            if (isWordToken(tokens[i])) {
                skip.add(i)
                seen++
            }
            i++
        }
        return skip
    }

    private fun sideWords(tokens: List<String>, skip: Set<Int>): Set<String> {
        val bag = LinkedHashSet<String>()
        for (i in tokens.indices) {
            if (i in skip) continue
            if (!isWordToken(tokens[i])) continue
            val low = tokens[i].lowercase()
            if (low in COMMON) continue
            bag.add(low)
        }
        return bag
    }

    private fun replaceSpan(
        tokens: MutableList<String>,
        start: Int,
        fromWordCount: Int,
        to: String
    ): Int {
        tokens[start] = to
        if (fromWordCount <= 1) return start + 1
        var seen = 1
        var i = start + 1
        while (i < tokens.size && seen < fromWordCount) {
            if (isWordToken(tokens[i])) {
                tokens.removeAt(i)
                if (i < tokens.size && !isWordToken(tokens[i])) {
                    tokens.removeAt(i)
                }
                seen++
            } else {
                tokens.removeAt(i)
            }
        }
        return start + 1
    }

    internal fun transferCase(source: String, replacement: String): String {
        if (source.isEmpty() || replacement.isEmpty()) return replacement
        // Never re-case multi-word replacements (e.g. "openflow" → "Open Flow")
        if (' ' in replacement) return replacement
        // Only transfer case when source and replacement are close variants of the same word
        // (e.g. "mitton"→"Mitun"). For genuinely different replacements preserve map casing.
        val srcLetters = source.lowercase().filter { it.isLetter() }
        val repLetters = replacement.lowercase().filter { it.isLetter() }
        if (srcLetters.isEmpty() || repLetters.isEmpty()) return replacement
        if (levenshtein(srcLetters, repLetters) > srcLetters.length / 2) return replacement
        // all uppercase
        if (source.all { !it.isLetter() || it.isUpperCase() }) return replacement.uppercase()
        // all lowercase
        if (source.all { !it.isLetter() || it.isLowerCase() }) return replacement.lowercase()
        // title case or mixed — keep replacement as-is
        return replacement
    }

    internal fun matchWithSuffix(token: String, fromWord: String): Pair<String, String>? {
        for (sfx in SUFFIXES) {
            if (token.endsWith(sfx, ignoreCase = true) && !fromWord.endsWith(sfx, ignoreCase = true)) {
                val stem = token.dropLast(sfx.length)
                if (stem.equals(fromWord, ignoreCase = true)) {
                    return stem to sfx
                }
            }
        }
        return null
    }
}
