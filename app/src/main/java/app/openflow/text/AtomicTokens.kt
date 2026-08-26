package app.openflow.text

/**
 * Protect spans that downstream regex stages must not touch (URLs, emails,
 * paths, IPs, domains): swap for opaque sentinels, restore byte-exact later.
 */
object AtomicTokens {

    private val patterns = listOf(
        // email
        Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""),
        // URL with scheme or www
        Regex("""(?:https?://|www\.)\S+"""),
        // absolute-ish path or domain/path
        Regex("""(?:/[A-Za-z0-9._-]+){2,}|\b[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+(/[A-Za-z0-9._/-]*)?"""),
    )

    data class Protected(val text: String, val spans: List<String>)

    fun protect(input: String): Protected {
        var t = input
        val found = ArrayList<String>()
        for (p in patterns) {
            p.findAll(t).toList().reversed().forEach { m ->
                var value = m.value
                var lastIdx = m.range.last
                // A trailing sentence dot belongs to the sentence, not the span.
                if (value.endsWith('.') &&
                    (lastIdx + 1 >= t.length || t[lastIdx + 1].isWhitespace() || t[lastIdx + 1] == '.')
                ) {
                    value = value.dropLast(1)
                    lastIdx -= 1
                }
                if (lastIdx < m.range.first || t.substring(m.range.first, lastIdx + 1).contains('\uE000')) {
                    return@forEach
                }
                found.add(value)
                val sentinel = "\uE000${found.size - 1}\uE001"
                t = t.substring(0, m.range.first) + sentinel +
                    t.substring(lastIdx + 1)
            }
        }
        return Protected(t, found)
    }

    fun restore(text: String, spans: List<String>): String {
        if (spans.isEmpty() || !text.contains('\uE000')) return text
        var t = text
        spans.forEachIndexed { i, s ->
            t = t.replace("\uE000${i}\uE001", s)
        }
        return t
    }
}
