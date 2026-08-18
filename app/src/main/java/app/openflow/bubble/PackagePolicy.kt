package app.openflow.bubble

/**
 * Hide Flow Bubble on banking / auth / wallet apps (Wispr-style privacy).
 * Package name only — no screen content scrape.
 *
 * We hide *our* overlay. We do not evade a bank's own
 * "malicious a11y" banner — that is their UI; we cannot delete it.
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
        // India wallets / banks (token miss: names lack token)
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",
        "com.snapwork.hdfc",
        "com.snapwork.hdfcbank",
        "com.hdfcbank.android.now",
        "com.sbi.lotusintouch",
        "com.sbi.upi",
        "com.csam.icici.bank.imobile",
        "com.msf.kbank.mobile",
        "com.olive.kotak.upi",
        "com.axis.mobile",
        "in.org.npci.upiapp",
        "com.dreamplug.androidapp",
    )

    private val tokens = listOf(
        ".bank", "bank.", "banking", "wallet", "fintech",
        "creditunion", "credit_union", "paypal", "stripe",
        "auth0", "okta", "1password", "lastpass", "bitwarden",
        "keepersecurity", "authenticator",
        // India-common
        "phonepe", "paytm", "paisa", "hdfc", "yono",
        "icici", "kotak", "bhim", "cred",
    )

    fun shouldHideBubble(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val p = packageName.lowercase()
        if (p in exact) return true
        return tokens.any { p.contains(it) }
    }

    fun isOwnApp(packageName: String?): Boolean {
        val p = packageName.orEmpty()
        return p == "app.openflow" || p.startsWith("app.openflow.")
    }
}
