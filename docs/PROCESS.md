# open-flow build process (hard)

Follow Superpowers + these rules. No exceptions.

## Per feature (every time)

1. **Web search** — APIs + Android security for that feature.
2. **Quick plan** — save under `docs/superpowers/plans/YYYY-MM-DD-<feature>.md`.
3. **Worktree** — `git worktree add .worktrees/<feature> -b feat/<feature>`  
   (`.worktrees/` must be gitignored.)
4. **TDD** — failing test first → green → refactor.
5. **Security** — no new risky perm without plan note; no INTERNET by default; cleartext off.
6. **Commit** — one focused commit on that feature branch (author Mitun only).
7. **Merge to main** when feature green (fast-forward or merge commit).

## Skills order

1. using-superpowers (check skills)
2. using-git-worktrees (isolate)
3. writing-plans (plan file)
4. test-driven-development (code)
5. verification-before-completion (prove)

## Feature map (order)

| ID | Branch | What |
|----|--------|------|
| F0 | feat/00-bootstrap | git, LICENSE, SECURITY, process |
| F1 | feat/01-scaffold | Gradle/Compose shell, builds debug APK |
| F2 | feat/02-privacy | PrivacyDefaults + report UI |
| F3 | feat/03-search-export | TranscriptSearch + TranscriptExporter |
| F4 | feat/04-room | Room sessions + FTS |
| F5 | feat/05-stt | On-device SpeechRecognizer engine |
| F6 | feat/06-ime | Voice IME |
| F7 | feat/07-recorder | Record + save session |
| F8 | feat/08-timeline-ui | Compose timeline + search |
| F9 | feat/09-export-ui | Share/export files |

## Test on device

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
cd .worktrees/<feature>   # or main after merge
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
