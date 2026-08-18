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
- Speech language catalog in Settings (default en-US). MIT. No account.

**The hard truth about speech:**  
Android’s own docs say the default recognizer is likely to stream audio to a remote service. Open Flow uses that default path. `createOnDeviceSpeechRecognizer()` is optional, not the default. **Declared-but-unused INTERNET does not mean your voice never leaves the device.**

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
| INTERNET in the app | **Declared; unused until opt-in** | For models / optional API | For model download | For model download (can revoke) | For model download | Yes (server) | Yes |
| Cleanup | Rule lists | Optional OpenAI | In-model / app | Weak / none | Weak / none | Server-side | Cloud LLM |
| Languages | Catalog (en-US default) | Several (model-dependent) | Many | 20+ Vosk models | Many | Depends on server | Many |
| License | **MIT** | Personal / permissive | Non-OSI (commercial restrictions) | GPL-3.0 | GPL-3.0 | Apache-2.0 | Closed |
| On F-Droid | **No** | No | Their repo | **Yes** | **Yes** | **Yes** | No |
| History + dict/snippets | **Yes** | Limited | Weak | No | No | No | Yes (their cloud) |
| Maturity | Early 0.1.x, debug-signed | Small project | Polished | Stable | Active | Older | Commercial |

---

## Sources

- Android `SpeechRecognizer` / `createOnDeviceSpeechRecognizer` documentation
- [Sayboard on F-Droid](https://f-droid.org/packages/com.elishaazaria.sayboard/)
- [Whisper IME (woheller69) on F-Droid](https://f-droid.org/en/packages/org.woheller69.whisper/)
- [Kõnele on F-Droid](https://f-droid.org/en/packages/ee.ioc.phon.android.speak/) + project privacy notes (default server)
- [FUTO Voice Input](https://gitlab.futo.org/keyboard/voiceinput) / Play listing (Source First license, offline Whisper)
- [Phone Whisper](https://github.com/kafkasl/phone-whisper) (overlay + sherpa-onnx / optional OpenAI)
- This repo: [architecture page](https://mitunmanav.github.io/open-flow/architecture.html), `AndroidManifest.xml` (INTERNET declared; unused until opt-in)

We did not run timed accuracy tests on one phone against all of these. Accuracy ranks above are **engine class**, not a lab bake-off.
