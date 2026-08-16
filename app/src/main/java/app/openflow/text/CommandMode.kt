package app.openflow.text

import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import java.util.Locale

/**
 * Voice edit & semantic transformation engine.
 * Supports 100% offline local rule transforms and optional AI enhancement.
 */
object CommandMode {

    private val bulletTriggers = listOf(
        "organize into bullets", "organise into bullets",
        "make a list", "make bullets", "make bullet points", "bullet points", "bullet list",
        "format as bullets", "make a bullet list"
    )

    private val numberTriggers = listOf(
        "numbered list", "number this", "number list", "format as numbered list", "make numbered list", "make a numbered list"
    )

    private val allCapsTriggers = listOf(
        "all caps", "uppercase", "in all caps", "all uppercase"
    )

    private val lowercaseTriggers = listOf(
        "all lower", "lowercase", "all lowercase", "make lowercase"
    )

    private val titleCaseTriggers = listOf(
        "title case", "capitalize words", "capitalize each word", "in title case"
    )

    private val quoteTriggers = listOf(
        "add quotes", "in quotes", "put in quotes", "quote this", "wrap in quotes"
    )

    private val camelCaseTriggers = listOf(
        "camel case", "camelcase", "format as camel case"
    )

    private val snakeCaseTriggers = listOf(
        "snake case", "snakecase", "format as snake case"
    )

    fun applyLocal(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return text
        val lower = trimmed.lowercase(Locale.ROOT)

        // Bullet lists
        for (trigger in bulletTriggers) {
            if (lower.startsWith(trigger)) {
                val rest = trimmed.substring(trigger.length).trim().removePrefix(":").removePrefix(",")
                return formatBullets(rest)
            }
        }

        // Numbered lists
        for (trigger in numberTriggers) {
            if (lower.startsWith(trigger)) {
                val rest = trimmed.substring(trigger.length).trim().removePrefix(":").removePrefix(",")
                return formatNumberedList(rest)
            }
        }

        // ALL CAPS
        for (trigger in allCapsTriggers) {
            if (lower.startsWith(trigger)) {
                val rest = trimmed.substring(trigger.length).trim().removePrefix(":").removePrefix(",")
                return rest.uppercase(Locale.ROOT)
            }
        }

        // Lowercase
        for (trigger in lowercaseTriggers) {
            if (lower.startsWith(trigger)) {
                val rest = trimmed.substring(trigger.length).trim().removePrefix(":").removePrefix(",")
                return rest.lowercase(Locale.ROOT)
            }
        }

        // Title Case
        for (trigger in titleCaseTriggers) {
            if (lower.startsWith(trigger)) {
                val rest = trimmed.substring(trigger.length).trim().removePrefix(":").removePrefix(",")
                return toTitleCase(rest)
            }
        }

        // Quotes
        for (trigger in quoteTriggers) {
            if (lower.startsWith(trigger)) {
                val rest = trimmed.substring(trigger.length).trim().removePrefix(":").removePrefix(",")
                return "\"$rest\""
            }
        }

        // Camel Case
        for (trigger in camelCaseTriggers) {
            if (lower.startsWith(trigger)) {
                val rest = trimmed.substring(trigger.length).trim().removePrefix(":").removePrefix(",")
                return toCamelCase(rest)
            }
        }

        // Snake Case
        for (trigger in snakeCaseTriggers) {
            if (lower.startsWith(trigger)) {
                val rest = trimmed.substring(trigger.length).trim().removePrefix(":").removePrefix(",")
                return toSnakeCase(rest)
            }
        }

        return text
    }

    suspend fun apply(
        text: String,
        brainCommand: Boolean,
        brain: TextAIProvider = NoAI,
    ): String {
        val local = applyLocal(text)
        if (local != text) return local
        if (!brainCommand || brain == NoAI) return text
        return brain.enhance(text, "command")
    }

    private fun formatBullets(raw: String): String {
        val items = splitListItems(raw)
        return items.filter { it.isNotBlank() }.joinToString("\n") { "• " + capitalizeFirst(it.trim()) }
    }

    private fun formatNumberedList(raw: String): String {
        val items = splitListItems(raw)
        return items.filter { it.isNotBlank() }.mapIndexed { i, it -> "${i + 1}. " + capitalizeFirst(it.trim()) }.joinToString("\n")
    }

    private fun splitListItems(raw: String): List<String> {
        val normalized = raw
            .replace(Regex("(?i)\\bcomma\\b"), ",")
            .replace(Regex("(?i)\\band then\\b"), ",")
            .replace(Regex("(?i)\\bnext\\b"), ",")
        return normalized.split(Regex("[,;\\n]+|\\.\\s+"))
    }

    private fun capitalizeFirst(s: String): String {
        if (s.isEmpty()) return s
        return s.substring(0, 1).uppercase(Locale.ROOT) + s.substring(1)
    }

    private fun toTitleCase(s: String): String {
        return s.split(Regex("\\s+")).joinToString(" ") { capitalizeFirst(it.lowercase(Locale.ROOT)) }
    }

    private fun toCamelCase(s: String): String {
        val words = s.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return s
        val first = words.first().lowercase(Locale.ROOT)
        val rest = words.drop(1).joinToString("") { capitalizeFirst(it.lowercase(Locale.ROOT)) }
        return first + rest
    }

    private fun toSnakeCase(s: String): String {
        val words = s.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        return words.joinToString("_") { it.lowercase(Locale.ROOT) }
    }
}
