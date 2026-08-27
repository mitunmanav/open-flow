# Data safety — draft for Play Console

> Fill this in Play Console under Policy → App content → Data safety.
> This file is the git source of truth; the form must match it.
> Sources: [Declare data use](https://developer.android.com/privacy-and-security/declare-data-use) · [Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469)

## Summary (what the reviewer sees)

- **No ads. No analytics. No tracking. No SDK that collects.**
- **Default (no cloud pick)**: app makes **zero** network requests. `INTERNET` declared but unused. Audio goes to **system SpeechRecognizer** (device or Google, per phone) — that is the OS, not Open Flow's server. History stays on device.
- **If user picks a cloud ear/brain**: the text/audio the user chose is `POST`ed to the vendor they picked (OpenAI / xAI / Gemini / Anthropic / Deepgram / AssemblyAI) or laptop URL they entered. See `docs/PRIVACY.md` vendor list.
- **Encryption in transit**: Yes (HTTPS only; NSC `cleartextTrafficPermitted=false` except `localhost`/`10.0.2.2` literals).
- **Data deletion**: No account; local data deleted via in-app Wipe/History delete + App info → Clear data. Privacy policy describes this.

## Play form answers (copy-paste)

**Does your app collect or share any of the required user data types?**
- Yes → see breakdown below. (If default-only build with no cloud pick, the *app binary* still declares `RECORD_AUDIO` and may transmit via system STT — answer truthfully.)

**What is collected:**

- **Audio** — *Collected* when you tap bubble to dictate.
  - Purpose: App functionality (speech-to-text → insert).
  - Ephemeral: not stored as audio unless you enable Whisper on-phone memos (then encrypted at rest per `SECURITY.md`).
  - Optional? Yes — user can deny `RECORD_AUDIO`; dictation disabled.
  - **Sharing**: By default, audio is handed to `SpeechRecognizer` (system). The system may send to Google — disclosure in privacy policy. If user picks a cloud ear (Deepgram/AssemblyAI), audio is sent to that vendor. If local Whisper, audio stays on device.

- **App activity → In-app search history** (dictation transcripts)
  - Purpose: User history feature (search, share, export).
  - Stored on device only (Room + FTS). Not transmitted unless user taps Share/Export or picks cloud brain.
  - Sharing: Not shared by default. Shared only when user taps Share/Save-file/POST to brain they picked.

- **Contacts / other types**: **Not collected.**

**Is this data encrypted in transit?**
- Yes. HTTPS only. Cleartext allowlist is only `localhost`/`10.0.2.2`/RFC1918 for laptop dev; Play form still answers Yes.

**Can users request deletion?**
- Yes. In-app: History → Delete one / Clear all learned, Settings → Wipe history, System → App info → Clear data removes all. No server account to delete. Privacy policy states this.

**SDKs:**
- None of OkHttp, Room, Compose, Coroutines collect/share. If you add analytics/crash SDK later, update this file + form.

## Traceability (manifest → code → policy)

| Manifest permission | What code does | Data safety | Privacy policy |
|---|---|---|---|
| `RECORD_AUDIO` | `FlowAccessibilityService` + `AndroidSpeechEngine` start listen | Audio, collected, app functionality | `docs/PRIVACY.md` “Microphone only while dictating” |
| `INTERNET` | `HostUrl` okHttp only when user picks net ear/brain or `10.0.2.2` model download | Audio/text shared only after user pick | `docs/PRIVACY.md` “INTERNET declared, unused until pick” |
| `POST_NOTIFICATIONS` | Bubble foreground + copy chip | No data collection | — |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Battery dialog → `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | No data | `docs/store/SENSITIVE_PERMS.md` |
| `ACCESS_NETWORK_STATE` | Check online before brain POST | No data | — |

## Keep consistent

- `app/src/main/AndroidManifest.xml` perms ↔ this file ↔ `docs/PRIVACY.md` ↔ in-app `Legal` screens.
- `scripts/qa/play-check.sh` checks `PRIVACY` + `PERMS` + `NSC` + `BACKUP` as proxies.
- If you change vendors, `OkHttp` hosts, or add SDKs, update here **before** Play submission.

## Checklist before Play upload

- [ ] Privacy policy URL reachable (both Play field + in-app `Legal`).
- [ ] Form says Yes to encryption in transit.
- [ ] Form lists Audio + locally stored transcripts with correct sharing rule.
- [ ] No “No data collected” claim if you keep `RECORD_AUDIO` + system STT path.
