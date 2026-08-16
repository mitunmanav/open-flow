package app.openflow.bubble

/**
 * After insert, Wispr-style send. API 30+ IME enter (AOSP `ACTION_IME_ENTER` = 0x00400000).
 */
object InsertSubmitPolicy {
    enum class How { NONE, IME_ENTER, CLICK }

    const val IME_ENTER_MIN_API = 30

    /** Same int as AccessibilityNodeInfo.ACTION_IME_ENTER (API 30). */
    const val ACTION_IME_ENTER = 0x00400000

    fun how(submit: Boolean, api: Int): How {
        if (!submit) return How.NONE
        return if (api >= IME_ENTER_MIN_API) How.IME_ENTER else How.CLICK
    }

    fun actionId(how: How): Int? = when (how) {
        How.NONE -> null
        How.IME_ENTER -> ACTION_IME_ENTER
        How.CLICK -> 0x00000010 // AccessibilityNodeInfo.ACTION_CLICK
    }
}
