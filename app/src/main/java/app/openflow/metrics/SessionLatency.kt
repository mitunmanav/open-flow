package app.openflow.metrics

/**
 * End-to-end latency marks for one dictation session.
 * Clock injected — pure JVM testable. First mark per stage wins.
 */
class SessionLatency(private val now: () -> Long) {
    private val marks = linkedMapOf<String, Long>()

    fun mark(stage: String) {
        marks.putIfAbsent(stage, now())
    }

    fun elapsedMs(from: String, to: String): Long? {
        val a = marks[from] ?: return null
        val b = marks[to] ?: return null
        return (b - a).coerceAtLeast(0L)
    }

    /** One line: gap between each consecutive mark. Blank when unmarked. */
    fun summary(): String {
        val entries = marks.entries.toList()
        if (entries.isEmpty()) return ""
        var prev = entries.first().value
        return buildString {
            for ((name, t) in entries) {
                if (isNotEmpty()) append(' ')
                append(name).append('=').append(t - prev).append("ms")
                prev = t
            }
        }
    }
}
