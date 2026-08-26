package app.openflow.text

/**
 * Spoken money → written forms. Dollars get "$", pounds "£"; euros and bucks
 * become digits without symbol; cents alone → "$0.NN".
 * Splice design: only matched money phrases are replaced; all other text
 * (including decimal candidates like "three point one four") passes through
 * byte-exact for later stages.
 */
object ItnMoney {

    private val impliedPriceCue = setOf("total", "price", "cost", "fee")

    private fun clean(tok: String): String = tok.lowercase().trim(',', '.', '!', '?', ';', ':')

    fun apply(text: String): String {
        if (!text.any { it.isLetter() }) return text
        val tokens = text.split(" ")
        val starts = IntArray(tokens.size)
        var pos = 0
        for (k in tokens.indices) {
            starts[k] = pos
            pos += tokens[k].length + 1
        }

        // (startToken, endTokenExclusive, replacement)
        val reps = ArrayList<Triple<Int, Int, String>>()
        var i = 0
        while (i < tokens.size) {
            val low = clean(tokens[i])

            // Implied-dollar price ("total is nine ninety nine") must run BEFORE
            // generic cardinal merge — "nine ninety nine" is $9.99, not 99.
            val prev = tokens.getOrNull(i - 1)?.let(::clean)
            if (i >= 2 && prev == "is" && clean(tokens[i - 2]) in impliedPriceCue &&
                ItnNumber.units[low] in 1L..9L
            ) {
                val frac = ItnNumber.cardinal(tokens, i + 1)
                if (frac != null && frac.second == 2 && frac.first in 10..99) {
                    val endTok = i + 1 + frac.second
                    reps += Triple(i, endTok, "$${ItnNumber.units[low]}.${frac.first}")
                    i = endTok
                    continue
                }
            }

            if (!ItnNumber.isNumberWord(low)) { i++; continue }
            val card = ItnNumber.cardinal(tokens, i)
            if (card == null) { i++; continue }
            val (value, count, _) = card
            val after = tokens.getOrNull(i + count)?.let(::clean)

            when {
                after == "dollars" || after == "dollar" -> {
                    val base = i + count + 1
                    val tail = if (tokens.getOrNull(base)?.let(::clean) == "and") {
                        ItnNumber.cardinal(tokens, base + 1)
                    } else null
                    val centsOk = tail != null &&
                        tokens.getOrNull(base + 1 + tail.second)?.let(::clean) == "cents"
                    if (centsOk) {
                        val endTok = base + 1 + tail.second + 1
                        reps += Triple(
                            i, endTok,
                            "$${group(value)}." + tail.first.toString().padStart(2, '0')
                        )
                        i = endTok
                    } else {
                        reps += Triple(i, i + count + 1, "$${group(value)}")
                        i += count + 1
                    }
                }
                after == "pounds" || after == "pound" -> {
                    reps += Triple(i, i + count + 1, "£${group(value)}")
                    i += count + 1
                }
                after == "euros" || after == "euro" || after == "bucks" || after == "buck" -> {
                    reps += Triple(i, i + count + 1, "${group(value)} ${tokens[i + count]}")
                    i += count + 1
                }
                after == "cents" -> {
                    reps += Triple(i, i + count + 1, "$0." + value.toString().padStart(2, '0'))
                    i += count + 1
                }
                else -> i += count
            }
        }

        var out = text
        for ((s, e, rep) in reps.sortedByDescending { it.first }) {
            val charStart = starts[s]
            val charEnd = starts[e - 1] + tokens[e - 1].length
            out = out.substring(0, charStart) + rep + out.substring(charEnd)
        }
        return out
    }

    private fun group(v: Long): String = java.text.DecimalFormat("#,###").format(v)
}
