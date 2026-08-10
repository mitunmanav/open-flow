# Open Flow

FOSS Android app: **speech-to-text anywhere** (Wispr-style) + **private voice memory** (NeoSapien-style).

- Local-first, no account, no ads
- Open source (MIT)
- On-device STT preferred (Android `SpeechRecognizer`)
- Opt-in online later only

## Status

Work in progress. Process: `docs/PROCESS.md`.

## Build (when SDK ready)

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Test on phone

1. Install debug APK  
2. Grant mic when asked  
3. **Settings → System → Keyboard → Manage keyboards → Open Flow Voice**  
4. Open app → Record / search timeline  

## License

MIT — see `LICENSE`.
