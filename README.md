# Open Flow

FOSS Android app: **speech-to-text anywhere** (Wispr Flow **Android** style) + **private voice memory** (NeoSapien-style).

- **Not a keyboard.** Floating **Flow Bubble** + Accessibility (like Wispr on Android)
- Keep your normal keyboard (Gboard, etc.)
- Local-first, no account, no ads
- Open source (MIT)
- On-device STT preferred (Android `SpeechRecognizer`)
- Opt-in online later only

## Status

Work in progress.  
**Baseline + all features:** `docs/BASELINE.md`  
Pickup: `docs/HANDOFF.md` · Process: `docs/PROCESS.md` · Agents: `AGENTS.md`

## Build

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# or: dist/open-flow-debug.apk / Desktop copy
```

## Test on phone (dictation)

1. Install debug APK  
2. Open **Open Flow** → grant **microphone**  
3. Tap **Enable Flow Bubble** → turn on **Open Flow** in Accessibility  
4. Open WhatsApp / Notes → tap a text field  
5. Tap the **floating mic bubble** → speak → text inserts  
6. Timeline / search still in the app for memos  

## License

MIT — see `LICENSE`.
