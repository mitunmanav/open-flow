package app.openflow.ui.privacy

import app.openflow.stt.OnDeviceSpeechPolicy

/** Honest launch copy. Brain enhance can POST this utterance — never claim it never leaves. */
object PrivacyHonesty {
    const val HOME_FOOTER =
        "Phone speech may send audio. A brain you pick can POST this line. History stays on this phone unless you share it."

    const val SETTINGS_BODY =
        "History stays on this phone. Cloud speech stays off until you pick it. A cloud brain you pick can POST this line."

    const val KEEP_FOREVER =
        "History is stored on this phone (not encrypted). A cloud brain you pick can still POST this line."

    const val INSIGHTS_VOICE =
        "Usage stays on this phone. Refresh Voice sends counts and top words to the brain you pick — not full dictations."

    const val SETUP_MIC =
        "Phone speech may send audio. Open Flow has no server. A rewrite you pick can POST this line."

    const val WALKTHROUGH =
        "Phone speech may use Google. A rewrite you pick can POST this line. History stays here. Wipe anytime. No Open Flow server."

    const val ON_DEVICE_OFF = OnDeviceSpeechPolicy.HONESTY_OFF

    const val ON_DEVICE_ON = OnDeviceSpeechPolicy.HONESTY_ON
}
