package app.openflow.bubble

import app.openflow.text.WritingStyle

/**
 * Local per-app category style (Wispr Android Style tab, no cloud).
 * Unknown packages keep the user's global fallback.
 */
object AppStylePolicy {

    fun category(packageName: String?): String {
        val p = packageName.orEmpty().lowercase()
        if (p.isBlank()) return "other"
        if (personal.any { p.contains(it) }) return "personal"
        if (email.any { p.contains(it) }) return "email"
        if (work.any { p.contains(it) }) return "work"
        return "other"
    }

    fun styleFor(packageName: String?, fallback: WritingStyle): WritingStyle =
        when (category(packageName)) {
            "personal" -> WritingStyle.CASUAL
            "email", "work" -> WritingStyle.FORMAL
            else -> fallback
        }

    private val personal = listOf(
        "whatsapp", "telegram", "org.telegram", "org.thoughtcrime.securesms",
        "instagram", "sms", "messaging", "facebook.orca", "discord"
    )
    private val email = listOf(
        "android.gm", "gmail", "k9", "outlook", "mail.android", "yahoo.mobile.client.android.mail"
    )
    private val work = listOf(
        "slack", "teams", "linkedin", "skype", "webex"
    )
}
