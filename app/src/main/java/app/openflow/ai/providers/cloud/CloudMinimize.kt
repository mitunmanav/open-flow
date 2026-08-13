package app.openflow.ai.providers.cloud

/** Strip email + phone-shaped tokens before a cloud brain POST. Not perfect. */
internal object CloudMinimize {
    private val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val phone = Regex("""\d{10,}""")

    fun forBrain(text: String): String =
        text.replace(email, "").replace(phone, "").replace(Regex("""\s+"""), " ").trim()
}
