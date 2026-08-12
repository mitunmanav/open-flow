package app.openflow.bubble

/**
 * Hide Flow Bubble on banking / auth apps (Wispr-style privacy).
 * Package name only — no screen content scrape.
 *
 * Guide: a11y insert is powerful; bank apps distrust overlays —
 * we hide ourselves rather than fight their defenses.
 */
object PackagePolicy {
    private val exact = setOf(
        "com.google.android.apps.authenticator2",
        "com.azure.authenticator",
        "com.authy.authy",
        "com.chase.sig.android",
        "com.wf.wellsfargomobile",
        "com.bankofamerica.cashpro",
        "com.usbank.mobilebanking",
        "com.infonow.bofa",
        "com.paypal.android.p2pmobile",
        "com.venmo",
        "com.square.cash",
        "com.coinbase.android",
        "com.binance.dev",
    )

    private val tokens = listOf(
        ".bank", "bank.", "banking", "wallet", "fintech",
        "creditunion", "credit_union", "paypal", "stripe",
        "auth0", "okta", "1password", "lastpass", "bitwarden",
        "keepersecurity", "authenticator"
    )

    fun shouldHideBubble(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val p = packageName.lowercase()
        if (p in exact) return true
        return tokens.any { p.contains(it) }
    }
}
