# F11 Continuous dictation + speed + UI polish

**Goal:** Fix “how long can I speak?” — restart loop for OS STT timeouts. Faster restart. Clearer bubble/home UI.

**Why:** Android `SpeechRecognizer` ends sessions on short silence (~few sec). Official API not built for unlimited continuous listen. Fix = auto-restart while user wants listening.

**Architecture:**
- Pure `ContinuousPolicy` (testable): when to restart after error/result
- `SttEngine` continuous mode: recreate recognizer when needed, longer silence extras, debounced restart
- Bubble shows listening time + partial text; stop is obvious
- Main UI: setup checklist, status chips

**Security:** no new permissions; still on-device prefer; no INTERNET

**Order in this feature:**
1. ContinuousPolicy + tests
2. SttEngine continuous
3. FlowAccessibilityService wire-up
4. Bubble + MainActivity UI
5. Build APK, commit
