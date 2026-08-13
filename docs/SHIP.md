# Open Flow — what to keep in mind before you ship

Deep pass 2026-08-13. Sources: Play a11y policy, F-Droid inclusion, Android 13+ restricted settings, Google developer verification 2026–27, app-signing / R8, privacy templates for on-device a11y + mic.

**This ship = honest sideload 0.1.x.** Not Play day-one. Not Wispr clone identity.

---

## 1. What you are shipping

- Android only. Bubble + AccessibilityService. Not a keyboard.
- MIT / FOSS. No account. No ads. No INTERNET in the APK.
- English only. Local polish (rules). Phone STT may still talk to Google.
- Banks: we hide. We cannot silence their warning.

If that sentence is not true on the APK, do not ship.

---

## 2. Sideload (what users hit first)

Android 13+ **restricted settings** on non-Play installs:

1. User tries Accessibility → grey / “restricted”.
2. App info → **⋮ → Allow restricted settings** → PIN.
3. Then enable Accessibility. Then mic.

Play Protect will say “unknown app”. Normal. “Install anyway”.

Samsung: Auto Blocker can block sideload. Battery Unrestricted.
Xiaomi / Oppo / Vivo: autostart + no-sleep or the bubble dies.

**ADB install does not teach this.** If you only ever `adb install`, you never see Restricted Settings. Test once by installing the APK from Files.

Updates can re-lock restricted settings. Say that in the how-to.

---

## 3. Do not lie about speech

Open Flow has **no INTERNET permission**.  
`SpeechRecognizer` is still the **phone’s** engine. It may send audio to Google.

Privacy report + walkthrough must keep that line. Play Data Safety: you do not collect; the **system STT** might.

---

## 4. Play Store (later — hard)

You are **not** an accessibility tool for Play (`isAccessibilityTool=true`) unless the **primary** purpose is disability access and the listing says so. Voice dictation for everyone = **not** that flag.

Then Play requires:

- In-app **prominent disclosure** (not only Settings / privacy page).
- Affirmative tap (Accept).
- Separate from other legal text.
- Play Console **AccessibilityService declaration** + **video**: open app → disclosure → accept → enable a11y → dictate.
- Same video must show **refuse** path.
- RECORD_AUDIO on the Permissions form.
- Data Safety form (no off-device collection if true).
- Public **privacy policy URL** (GitHub Pages is fine). Link in app + listing.
- 2026: no “autonomous plan-and-act” a11y agents. Our tap → insert is rule-based. OK.

Expect weeks. Rejection is common. **Sideload / F-Droid first.**

Do **not** set `isAccessibilityTool=true` just to skip disclosure. That is a policy lie.

---

## 5. F-Droid (best FOSS path)

- Public git + LICENSE (MIT — have).
- Tag = versionName (`v0.1.5`).
- No GMS / Firebase / Crashlytics (we have none).
- Fastlane metadata: `fastlane/metadata/android/en-US/` title, 80-char summary, description, changelog, 512 icon, screenshots.
- Release **signed** APK on GitHub Releases.
- Reproducible: `dependenciesInfo { includeInApk = false }` + pinned JDK/SDK.
- MR to fdroiddata. Review is slow. Mark Anti-Features only if true (we should have none if no net).

---

## 6. Signing (you cannot skip)

Debug APK (`0.1.4-debug`) is **not** a real ship.

Need:

- One PKCS12 keystore. Backup offline. Lose it = cannot update.
- `assembleRelease` signed. `debuggable=false`.
- Test **release** on device (R8 can break AccessibilityService). Keep rule if needed: `* extends AccessibilityService`.
- Never commit keystore / passwords. `local.properties` only.

Play later: Play App Signing. F-Droid: your cert SHA-256 in metadata.

---

## 7. Google developer verification (2026–27)

Certified phones (most stock Androids):

- **Sep 2026** some countries, **2027** more: unsigned/unverified packages get blocked unless user does a long “advanced” sideload flow.
- ADB stays exempt for now.
- F-Droid / GitHub APKs on Pixel/Samsung will get harder.

Keep in mind: sideload today is easy; 2027 may need you (or F-Droid) registered. Watch keepandroidopen.org. Do not panic-register tonight. Do not ignore it.

De-Googled ROMs (Graphene, etc.) less affected.

---

## 8. Legal / trust (even for sideload)

Minimum for an honest launch:

| Item | Why |
|------|-----|
| Privacy page in-app + URL | A11y + mic. People will ask. |
| What a11y can do / cannot do | Insert text. Not scrape banks. Hide on banks. |
| Honest STT line | Phone engine may leave device. |
| LICENSE MIT visible | FOSS. |
| No analytics | Already. Keep it. |
| Contact email | Bugs. |

Not a lawyer. If you ever take money / EU users at scale, get a human to read GDPR. On-device + no account is the easy story **only if it stays true**.

---

## 9. Product truth before you post the APK

- Walkthrough + Setup + Restricted Settings path works on **Files install**, not only adb.
- Bubble pill/circle/dot match settings.
- Back from Dict = app Home.
- Cleanup Light ≠ Medium (tests exist).
- Fast / Balanced / Accurate different (tests exist).
- Hide on bank/wallet. Bank may still scream. Say so.
- History copy/share is how you copy. **No bubble copy chip.**
- Force-stop turns a11y off. Not a virus.
- Battery optional. OEM Unrestricted if bubble dies.
- English only. Say it.
- Release APK, not debug, for anyone who is not you.

---

## 10. How to actually put it in people’s hands (order)

1. **You + friends:** signed release APK on Desktop / GitHub Release. How-to with Restricted Settings screenshots.
2. **Privacy URL** live (one HTML page).
3. **F-Droid** metadata + MR (months).
4. **Play** only if you want the fight (declaration + video + wait). Optional.
5. Watch 2026–27 verification if you care about stock phones long-term.

Do not launch on Play first. A11y + sideload story is F-Droid / GitHub.

---

## 11. Do not

- Add INTERNET “for updates”.
- Claim “100% offline speech” unless you ship an on-device model.
- Claim “not malware” at banks — they will still warn.
- Ship debug-signed APK as 1.0.
- Set `isAccessibilityTool=true` unless product is disability-first.
- Force-push or lose the keystore.

---

## Sources (2026)

- https://support.google.com/googleplay/android-developer/answer/10964491
- https://support.google.com/googleplay/android-developer/answer/16558241
- https://developer.android.com/developer-verification
- https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://f-droid.org/en/docs/Reproducible_Builds/
- https://keepandroidopen.org/
- Android 13+ restricted settings (sideload a11y)
