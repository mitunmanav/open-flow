# Open Flow vs other open voice apps

**Date:** 2026-08-13. Public sources: F-Droid package pages, project READMEs, Android `SpeechRecognizer` docs, Play a11y policy notes.

**This is not marketing.** If you need audio to stay on the phone, **do not pick Open Flow first.**

---

## What Open Flow actually is

- Floating **bubble**. You keep your keyboard.
- Uses Android **Accessibility** to type into the focused field.
- Speech comes from the system **`SpeechRecognizer`**.
- **INTERNET** is declared; Open Flow makes no network requests until the user opts into net ear/brain or model download.
- Cleanup is **hand-written rules**, not a language model.
- English only. MIT. No account.

**The hard truth about speech:**  
Android’s own docs say the default recognizer is likely to stream audio to a remote service. `createOnDeviceSpeechRecognizer()` exists, and Open Flow prefers it, but many phones still fall back to Google’s engine. **Unused-by-default INTERNET in our app does not mean your voice never leaves the device.**

If that sentence bothers you, use a Whisper/Vosk app below.

---

## How to read the table

- **Offline audio** = recognition can run with no network *after* you have a local model (or a proven on-device engine).
- **Keep keyboard** = not an IME. You do not switch away from Gboard/HeliBoard.
- **OSI FOSS** = MIT/Apache/GPL on F-Droid or GitHub, not a “source first / pay if you’re a company” license.

| | Open Flow | Phone Whisper | FUTO Voice | Sayboard | Whisper IME | Kõnele | Wispr Flow |
|---|---|---|---|---|---|---|---|
| Keep your keyboard | **Yes** (bubble) | **Yes** (overlay) | No (voice IME / its keyboard) | No (it *is* a voice keyboard) | No (IME) | No (IME / provider) | Yes (bubble) |
| Speech engine | System SpeechRecognizer | Local sherpa-onnx **or** OpenAI | Local Whisper (whisper.cpp) | Local Vosk | Local Whisper TFLite | Kaldi **server** (public default) | Cloud AI |
| Audio can leave phone | **Yes, often** (Google STT) | Only if you turn on cloud | No (after model download) | No (after model download) | No (after model download) | **Yes** on default server (even unencrypted) | **Yes** (that is the product) |
| INTERNET in the app | **Declared; off until opt-in** | For models / optional API | For model download | For model download (can revoke) | For model download | Yes (server) | Yes |
| Cleanup | Rule lists | Optional OpenAI | In-model / app | Weak / none | Weak / none | Server-side | Cloud LLM |
| Languages | **en-US only** | Several (model-dependent) | Many | 20+ Vosk models | Many | Depends on server | Many |
| License | MIT | “Do whatever” personal project | **Not OSI** (FUTO Source First) | GPL-3.0 | FOSS (F-Droid) | Apache-2.0 | Closed |
| On F-Droid | **No** | No | FUTO’s own repo | **Yes** | **Yes** | **Yes** | No |
| History / search | Yes (on phone) | Limited | Keyboard-centric | No real timeline | No | No | Yes (their cloud) |
| Dictionary / snippets | Yes | Keywords in some modes | Weak | No | No | No | Yes |
| Maturity | Early (0.1.x, debug-signed) | Small GitHub project | Polished, funded | Stable, slower updates | Actively updated | Older, niche | Commercial |
| Accessibility / bank scare | **Yes** (bubble) | **Yes** | Lower (IME path) | Lower | Lower | Lower | **Yes** |
| You pay | No | Optional sponsor; cloud = your API key | Optional paid unlock | No | No | No (unless you host) | Subscription |

Wispr is **not** open source. It is in the table only because people will compare us to it.

---

## Brutal verdicts

### If your goal is “voice never leaves this phone”

**Winners:** FUTO Voice Input, Sayboard, Whisper IME (woheller69), Phone Whisper **in local mode**.

**Open Flow loses.** We wrap the system recognizer. That is convenient and small. It is not a local Whisper stack. Claiming “fully offline speech” would be a lie.

### If your goal is “do not switch keyboards”

**Open Flow and Phone Whisper** (and commercial Wispr) share this shape.

FUTO / Sayboard / Whisper IME **replace or add a voice keyboard**. That is a different habit. Some people hate it. Some people prefer it (no Accessibility overlay).

### If your goal is “best words on the page”

**Wispr wins** (cloud model).  
Among open-ish apps: **FUTO or Whisper IME or Phone Whisper+cleanup**, not us.

Our Medium/High cleanup is regex and phrase rules. It will miss jokes, names, and messy speech. High can be too aggressive. We do not have an on-device LLM.

### If your goal is “MIT, no account, no ads, inspectable, keep Gboard”

**Open Flow is in a small set.** Phone Whisper is the closest open overlay cousin, and it has **better STT options** (real local models). We have more **product** around history, retention, dictionary, snippets, and a first-run walkthrough.

That is the honest split: **they transcribe better; we are a small MIT product shell around the system engine.**

### If your goal is F-Droid tomorrow

**We are not there.** Sayboard, Whisper IME, Kõnele are. We ship a **debug-signed** GitHub APK. Play Protect will nag. Restricted Settings will bite on Android 13+.

### Kõnele warning

Default Kõnele can send audio **and** device metadata to a public Kaldi server **without TLS**. Fine if you self-host. Reckless as a default for private speech. Do not treat “it’s on F-Droid” as “it’s private.”

### FUTO warning

Good offline Whisper. **License is not OSI FOSS.** Fine for many users. Not “pure F-Droid mainline MIT/GPL.”

---

## What Open Flow will not catch up on without new work

1. A real on-device model (Whisper / Vosk / Parakeet). Until then, privacy of **audio** is not under our control.
2. A release keystore and F-Droid metadata.
3. Languages other than English.
4. Accuracy that matches cloud Wispr.

Until those exist, do not market this as “the private Wispr.” It is a **FOSS bubble + local history** that still leans on the phone vendor’s STT.

---

## Sources

- Android `SpeechRecognizer` / `createOnDeviceSpeechRecognizer` documentation
- [Sayboard on F-Droid](https://f-droid.org/packages/com.elishaazaria.sayboard/)
- [Whisper IME (woheller69) on F-Droid](https://f-droid.org/en/packages/org.woheller69.whisper/)
- [Kõnele on F-Droid](https://f-droid.org/en/packages/ee.ioc.phon.android.speak/) + project privacy notes (default server)
- [FUTO Voice Input](https://gitlab.futo.org/keyboard/voiceinput) / Play listing (Source First license, offline Whisper)
- [Phone Whisper](https://github.com/kafkasl/phone-whisper) (overlay + sherpa-onnx / optional OpenAI)
- Fluence-Android public README (overlay + SenseVoice / Groq) — small, newer project
- This repo: `docs/ARCHITECTURE.md`, `AndroidManifest.xml` (INTERNET declared, unused by default)

We did not run timed accuracy tests on one phone against all of these. Accuracy ranks above are **engine class**, not a lab bake-off.
