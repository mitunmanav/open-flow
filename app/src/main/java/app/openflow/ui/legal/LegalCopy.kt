package app.openflow.ui.legal

object LegalCopy {
    const val privacyTitle = "Privacy policy"
    const val termsTitle = "Terms of use"

    val privacyBody: String = """
Last updated: 18 August 2026.

Open Flow is a free, local-first dictation app. No account. No ads. No analytics. No Open Flow server. We do not see your voice or text.

What this app uses
• Accessibility — only to see if a text field is focused and to insert the words you just spoke. We do not scrape other apps for ads. We hide the bubble in bank, wallet, and authenticator apps.
• Microphone — only while you are dictating after you tap the bubble.
• Storage — transcripts you keep, dictionary, snippets, settings, and any API key you paste. App-private.
• Internet — the app declares INTERNET so you can download an on-phone model, talk to a laptop URL, or call a key you pasted. Unused until you pick one of those.

Honest about speech
Open Flow does not upload audio on the default path. Android SpeechRecognizer is the phone’s engine. On some phones it may still use Google or the network. That is the phone, not an Open Flow server. A cloud brain you pick can POST this line.

If you pick a cloud ear or brain, that vendor gets what that pick needs. We never send dictation history, dictionary, snippets, or other apps to an Open Flow server (there is none). Keys stay on the phone. Uninstall deletes them.

Your choices
Stay on default phone speech and local rules. Revoke mic. Turn off Accessibility. Settings: keep, wipe in 24 hours, or never store. Not directed at children under 13.

Contact
GitHub Discussions for questions. GitHub Issues for bugs. Do not send personal data. No email.
""".trimIndent()

    val termsBody: String = """
Last updated: 18 August 2026.

By using Open Flow you agree to these terms.

License
Open Flow is free and open source under the MIT License. The software is provided “AS IS”, without warranty of any kind.

What the app does
Open Flow is not a keyboard. You keep your keyboard. A floating bubble inserts speech as text into the field you are using.

Permissions
You turn on Accessibility and Microphone yourself. Accessibility is only for focused fields and insert. You may turn either off at any time.

Optional cloud
Default is local. If you paste a vendor key or a laptop URL, that service’s own terms apply. Open Flow does not run those services.

No account
We do not bill, track, or host your history. History stays on this phone unless you share it.

Children
Not directed at children under 13.

Sideload
Installing outside Play is at your own risk. Some phones hide Accessibility until you allow restricted settings.

Liability
To the maximum extent the MIT License allows, the authors are not liable for data loss, vendor use of audio or text you send, or other apps reacting to Accessibility.

Contact
GitHub Discussions for questions. GitHub Issues for bugs. Do not send personal data. No email.
""".trimIndent()
}
