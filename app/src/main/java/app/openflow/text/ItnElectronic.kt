package app.openflow.text

/**
 * Spoken electronic forms: emails ("john at gmail dot com" → john@gmail.com),
 * URLs/domains, IP addresses, phone digit runs, and slash paths.
 * Runs before generic cardinals so digit words are still words.
 */
object ItnElectronic {

    private val tlds = setOf("com", "net", "org", "io", "edu", "gov", "co", "dev", "app", "ai")

    private fun isDigitWord(t: String): Boolean {
        val v = ItnNumber.units[t.lowercase()] ?: return false
        return v in 0..9
    }

    private fun simpleWord(t: String): Boolean =
        t.isNotEmpty() && t.all { it.isLetterOrDigit() } && t.length <= 63

    /** Line-local so inserted newlines (voice commands) survive re-passes. */
    fun apply(text: String): String =
        if (text.contains('\n')) {
            text.split("\n").joinToString("\n") { line ->
                applyPhones(applyEmailUrlIpPath(line))
            }
        } else {
            applyPhones(applyEmailUrlIpPath(text))
        }

    internal fun applyEmailUrlIpPath(text: String): String {
        val tokens = text.split(" ")
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            val low = tokens[i].lowercase()

            // email: W at W (dot W)+ ending in TLD
            if (simpleWord(low) && tokens.getOrNull(i + 1)?.lowercase() == "at") {
                val host0 = tokens.getOrNull(i + 2)?.lowercase()
                if (!host0.isNullOrBlank() && host0 != "at" && simpleWord(host0)) {
                    val parts = ArrayList<String>()
                    parts.add(host0)
                    var j = i + 3
                    while (j + 1 < tokens.size &&
                        tokens[j].lowercase() == "dot" &&
                        simpleWord(tokens[j + 1])
                    ) {
                        parts.add(tokens[j + 1].lowercase())
                        j += 2
                    }
                    if (parts.size >= 2 && parts.last() in tlds) {
                        out.add(low + "@" + parts.joinToString("."))
                        i = j
                        continue
                    }
                }
            }

            // dotted run (domain / www / IP), optionally followed by slash segments
            val dotted = ArrayList<String>()
            var j = i
            while (j + 2 < tokens.size &&
                tokens[j + 1].lowercase() == "dot" &&
                simpleWord(tokens[j + 2])
            ) {
                if (dotted.isEmpty()) dotted.add(low)
                dotted.add(tokens[j + 2].lowercase())
                j += 2
            }
            if (dotted.size >= 2) {
                val asDigits = dotted.map { s -> ItnNumber.units[s]?.toString() ?: s }
                val looksIp = asDigits.size == 4 && asDigits.all { s ->
                    s.all { it.isDigit() } && s.length in 1..3 && s.toInt() <= 255
                }
                val looksDomain = dotted.first() == "www" ||
                    dotted.drop(1).any { it in tlds }
                if (looksIp || looksDomain) {
                    val sb = StringBuilder(asDigits.joinToString("."))
                    var end = j
                    while (end + 2 < tokens.size && tokens[end + 1].lowercase() == "slash") {
                        sb.append('/').append(tokens[end + 2].lowercase())
                        end += 2
                    }
                    out.add(sb.toString())
                    i = end + 1
                    continue
                }
            }

            // path: W (slash|dot W)+ — leading "/" only for multi-slash paths
            if (i + 2 < tokens.size && simpleWord(low)) {
                val segs = ArrayList<String>()
                segs.add(low)
                var slashes = 0
                var k = i
                while (k + 2 < tokens.size) {
                    val joiner = tokens[k + 1].lowercase()
                    val nxt = tokens[k + 2].lowercase()
                    val okNext = simpleWord(nxt) || (joiner == "dot" && simpleWord(nxt))
                    if (joiner == "slash" && simpleWord(nxt)) {
                        segs.add(nxt); slashes++; k += 2
                    } else if (joiner == "dot" && simpleWord(nxt)) {
                        segs.add(".$nxt"); k += 2
                    } else break
                }
                if (slashes >= 1 && segs.size >= 3) {
                    val sb = StringBuilder(if (slashes >= 2) "/" else "")
                    segs.forEachIndexed { idx, s ->
                        sb.append(if (idx > 0 && !s.startsWith('.')) "/$s" else s)
                    }
                    out.add(sb.toString())
                    i = k + 1
                    continue
                }
            }

            out.add(tokens[i])
            i++
        }
        return out.joinToString(" ")
    }

    private val phoneCues = setOf("number", "phone", "call", "dial", "reach", "tel", "fax", "mobile", "cell")

    /** Digit-word runs → grouped phone numbers (10→3-3-4, 7→3-4, else plain).
     *  Only near an explicit phone cue; lone digit words stay for other stages. */
    internal fun applyPhones(text: String): String {
        val tokens = text.split(" ")
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            if (!isDigitWord(tokens[i])) {
                out.add(tokens[i]); i++; continue
            }
            var j = i
            val digits = StringBuilder()
            var words = 0
            while (j < tokens.size && isDigitWord(tokens[j])) {
                digits.append(ItnNumber.units[tokens[j].lowercase()])
                j++; words++
            }
            val hasCue = (maxOf(0, i - 3) until i)
                .any { tokens[it].lowercase().trim(',', '.', ':') in phoneCues }
            out.add(if (words >= 3 && hasCue) group(digits.toString()) else tokens.slice(i until j).joinToString(" "))
            i = j
        }
        return out.joinToString(" ")
    }

    private fun group(d: String): String = when (d.length) {
        10 -> "${d.slice(0..2)}-${d.slice(3..5)}-${d.slice(6..9)}"
        7 -> "${d.slice(0..2)}-${d.slice(3..6)}"
        else -> d
    }
}
