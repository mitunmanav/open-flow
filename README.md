# Open Flow

**v0.1.0** — FOSS Android dictation + private history.

- **Not a keyboard.** Floating **Flow Bubble** + Accessibility (Wispr Android style)
- Keep Gboard / your normal keyboard
- Local-first, no account, no ads, **no INTERNET permission**
- On-device STT preferred (`SpeechRecognizer`)
- MIT license

## Install (GitHub release)

1. Download **`OpenFlow-0.1.0.apk`** from [Releases](../../releases)
2. Allow install from unknown sources for your browser/Files app
3. Open the APK → Install
4. Open **Open Flow**
5. **Allow microphone**
6. **Enable Flow Bubble** → Accessibility → turn on **Open Flow**
7. Focus any text field → tap the floating bubble → speak → tap again

> **Note:** Release APK is **debug-signed** (FOSS sideload). Not for Play Store.  
> System STT may still use network on some devices; Open Flow itself never uploads audio or transcripts.

Optional: `OpenFlow-0.1.0-debug.apk` installs as `app.openflow.debug` (side-by-side with release).

Checksums: `*.sha256` on the same release.

## Features

| Area | What you get |
|------|----------------|
| Dictation | Bubble → polish once → insert into focused field |
| History | Search, copy, share, Markdown export |
| Dictionary | Personal replacements |
| Snippets | Trigger → expansion |
| Privacy | Keep / wipe 24h / never store history |

## Build from source

```bash
export JAVA_HOME=$HOME/.local/jdk   # or your JDK 17
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew :app:testDebugUnitTest :app:assembleRelease :app:assembleDebug
```

Outputs:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/apk/debug/app-debug.apk`

## License

MIT — see `LICENSE`.

## Security

See `SECURITY.md`. Hard defaults: no INTERNET, no cleartext, backup off.
