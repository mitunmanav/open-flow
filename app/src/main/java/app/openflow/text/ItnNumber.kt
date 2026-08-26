package app.openflow.text

/**
 * Spoken cardinals → digits. Unambiguous shapes only (NeMo-style conservatism):
 * - cardinals with hundred/thousand folding (comma-grouped >= 1000)
 * - decade-pair years ("twenty twenty six" → 2026, gated to 1100..2999)
 * - "point" decimals with single digit words ("three point one four" → 3.14)
 * - "N percent" → "N percent"
 * - million/billion stay words unless more number words follow ("7 billion" kept,
 *   "two million five hundred" → 2,000,500)
 * Ordinal words are never touched here.
 */
object ItnNumber {

    internal val units = linkedMapOf(
        "zero" to 0L, "one" to 1L, "two" to 2L, "three" to 3L, "four" to 4L,
        "five" to 5L, "six" to 6L, "seven" to 7L, "eight" to 8L, "nine" to 9L,
        "ten" to 10L, "eleven" to 11L, "twelve" to 12L, "thirteen" to 13L,
        "fourteen" to 14L, "fifteen" to 15L, "sixteen" to 16L, "seventeen" to 17L,
        "eighteen" to 18L, "nineteen" to 19L,
    )
    internal val tens = linkedMapOf(
        "twenty" to 20L, "thirty" to 30L, "forty" to 40L, "fourscore" to 80L,
        "fifty" to 50L, "sixty" to 60L, "seventy" to 70L, "eighty" to 80L,
        "ninety" to 90L,
    )
    private const val YEAR_MIN = 1100L
    private const val YEAR_MAX = 2999L

    /** Parse a maximal cardinal starting at index i. Returns null when not a number. */
    internal fun cardinal(tokens: List<String>, i: Int): Triple<Long, Int, Boolean>? {
        var idx = i
        var total = 0L
        var current = 0L
        var seen = false
        var thousandThenHundred = false
        fun key(j: Int): String =
            if (j in tokens.indices) tokens[j].lowercase().trim(',', '.', '!', '?', ';', ':')
            else ""
        while (idx < tokens.size) {
            val tok = key(idx)
            val unit = units[tok]
            val ten = tens[tok]
            when {
                unit != null -> {
                    current += unit
                    seen = true
                    idx++
                }
                ten != null -> {
                    // A second small decade without a scale between ends the run
                    // ("fifty fifty" is not one hundred); round hundreds continue.
                    if (current >= 20 && total == 0L && current % 100 != 0L) break
                    val nextUnit = units[key(idx + 1)]
                    current += if (nextUnit != null && nextUnit in 1..9) {
                        idx++
                        ten + nextUnit
                    } else {
                        ten
                    }
                    seen = true
                    idx++
                }
                tok == "hundred" && current > 0 -> {
                    if (total > 0L) thousandThenHundred = true
                    current *= 100
                    idx++
                }
                tok == "thousand" && current > 0 -> {
                    total += current * 1000
                    current = 0
                    idx++
                }
                (tok == "million" || tok == "billion") && current > 0 -> {
                    // Fold only when more number words follow; else leave word.
                    val moreFollows = ((idx + 1) until tokens.size)
                        .any { isNumberWord(key(it)) }
                    if (!moreFollows) break
                    total += current * if (tok == "million") 1_000_000L else 1_000_000_000L
                    current = 0
                    idx++
                }
                tok == "and" && seen && idx > i -> {
                    // "one hundred and twenty three" — only between number words
                    val nextIsNum = isNumberWord(key(idx + 1))
                    if (!nextIsNum) break
                    idx++
                }
                else -> break
            }
        }
        if (!seen) return null
        return Triple(total + current, idx - i, thousandThenHundred)
    }

    internal val scaleWords = setOf("hundred", "thousand", "million", "billion")

    internal fun isNumberWord(tok: String): Boolean =
        tok in units || tok in tens || tok in scaleWords

    private fun isDigitWord(tok: String): Boolean {
        val v = units[tok] ?: return false
        return v in 0..9
    }

    private fun group(value: Long, grouped: Boolean): String {
        if (!grouped && value < 10_000) return value.toString()
        return java.text.DecimalFormat("#,###").format(value)
    }

    fun apply(text: String): String {
        if (!text.any { it.isLetter() }) return text
        val tokens = text.split(" ")
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            val clean = tokens[i].lowercase().trim(',', '.', '!', '?', ';', ':', '"')
            // Decade-pair year: "twenty twenty six", "nineteen ninety nine"
            val year = yearPair(tokens, i)
            if (year != null) {
                out.add(year.second.toString())
                i += year.first
                continue
            }
            if (!isNumberWord(clean)) {
                out.add(tokens[i])
                i++
                continue
            }
            val parsed = cardinal(tokens, i)
            if (parsed == null) {
                out.add(tokens[i])
                i++
                continue
            }
            val (value, count, grouped) = parsed
            var consumed = count
            var rendered = group(value, grouped)
            // Decimal tail: "point" + single digit words
            if (i + consumed < tokens.size &&
                tokens[i + consumed].lowercase() == "point" &&
                i + consumed + 1 < tokens.size &&
                isDigitWord(tokens[i + consumed + 1].lowercase())
            ) {
                val frac = StringBuilder()
                var j = i + consumed + 1
                while (j < tokens.size && isDigitWord(tokens[j].lowercase())) {
                    frac.append(units[tokens[j].lowercase()])
                    j++
                }
                rendered += "." + frac
                consumed = j - i
            }
            // Small standalone numbers ("five options") stay words; scales,
            // decimals, multi-word runs and bracketed digits convert.
            val nextIsScale = i + 1 < tokens.size &&
                tokens[i + 1].lowercase() in scaleWords
            val bracketed =
                (i > 0 && tokens[i - 1].lastOrNull()?.let { it == '(' || it == '[' } == true) ||
                    (i + 1 < tokens.size && tokens[i + 1].firstOrNull()?.let { it == ')' || it == ']' } == true)
            if (consumed == 1 && value < 10 && !nextIsScale && !bracketed) {
                out.add(tokens[i])
                i++
                continue
            }
            out.add(rendered)
            i += consumed
        }
        return out.joinToString(" ")
    }

    /** "twenty twenty" / "twenty twenty six" / "nineteen ninety nine" → year digits. */
    internal fun yearPair(tokens: List<String>, i: Int): Pair<Int, Long>? {
        val first = tokens.getOrNull(i)?.lowercase() ?: return null
        val a = (tens[first] ?: units[first]?.takeIf { it in 10..19 }) ?: return null
        val second = tokens.getOrNull(i + 1)?.lowercase() ?: return null
        val b = tens[second] ?: return null
        val third = tokens.getOrNull(i + 2)?.lowercase()
        val c = if (third != null) units[third]?.takeIf { it in 1..9 } else null
        return if (c != null) {
            val v = a * 100 + b + c
            if (v in YEAR_MIN..YEAR_MAX) 3 to v else null
        } else {
            val v = a * 100 + b
            if (v in YEAR_MIN..YEAR_MAX) 2 to v else null
        }
    }
}
