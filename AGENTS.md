# Open Flow — Agent Rules

Wispr-Flow-style Android dictation app (floating bubble, Kotlin/Compose, accessibility service).

## Comms (strict)

- Caveman style. Short lines. Easy words.
- Bullet points only. Brief.
- Report format: DID / PASS-FAIL / NEXT / SUGGEST / ASK.

## Android workflow (mandatory)

- Always use the `android-cli` skill for device, SDK, layout and doc-search work.
- **Do not** start the WSL AVD with `android emulator start`. See **Emulator** below.
- Device QA: `docs/testing.md` + `scripts/qa/gate.sh`. Wrap SDK adb + adb-bridge (`scripts/qa/wrap-adb.sh`).
- Web search before non-trivial implementation decisions; cite sources in the report.
- Load matching installed skills (compose/tv/wear/testing/profiler etc.) when the task touches their domain.
- Verify on a device or emulator when runtime behavior changes; no blind refactors of state machines.

## Emulator (this laptop — WSL2 + Windows)

WSL has **no `/dev/dri`**. `-gpu host` in WSL **crashes**. SwiftShader **boots but is CPU-slow**.

**Run the Windows AVD with Intel Arc host GPU.** Keep that. Never "fix" it by switching to SwiftShader.

| Piece | Use |
|-------|-----|
| Start | `of-emu` → `scripts/qa/emu-up.sh up` |
| AVD | `of_win` on the **Windows** SDK |
| GPU | `-gpu host` (Intel Arc) |
| Boot | Quick Boot. Do **not** pass `-no-snapshot-load`. Reuse if already up. Never `pkill qemu`. |
| adb | Windows `adb.exe`. WSL SDK adb must be the wrap (`scripts/qa/wrap-adb.sh`). |
| Sideload | `./gradlew :app:assembleDebug` then `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Ready | `adb devices` shows `emulator-5554` and `getprop sys.boot_completed` = `1` |
| Gate | `scripts/qa/gate.sh` (or `--quick` after CI-like unit/lint) |

**Never**

- `android emulator start of_test` (WSL AVD)
- WSL `emulator -gpu swiftshader` / `lavapipe` for daily test
- A second qemu in WSL while `of_win` is up (fights adb port 5554)
- Linux SDK `adb` daemon (empty devices; can steal Windows `:5037`)

**Why**

- Windows emulator: WHPX + Arc host GLES. Fast. Window on Windows.
- WSL `of_test`: no host GL. Crash or SwiftShader crawl.
- Snapshots need hardware GL.
- Sources: [GPU modes](https://developer.android.com/studio/run/emulator-acceleration#command-gpu) · [Snapshots](https://developer.android.com/studio/run/emulator-snapshots) · [Test CLI](https://developer.android.com/studio/test/command-line)

**RAM (do not "fix" WSL 12 GB cap)**

- WSL apps ~2 GB. Cap 12 GB is headroom; Windows `vmmemWSL` can look fat when **cache** fills, then reclaim dumps it.
- `of_win` guest 4 GB → Windows qemu ~5–6 GB. That is the real extra RAM.
- Do not drop to software GPU to save RAM.

**After start**

```
of-emu
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Cold first boot can take ~1–2 min. Later starts = Quick Boot. Overlay + Accessibility still need granting on the emu.

`android emulator list` only sees WSL `of_test`. Ignore it. After wrap-adb, `android layout` / `android screen` / `android install` talk to `of_win`.

`of_win` is a **16 KB page** image. Native must stay 16 KB ELF-aligned (NDK r28 + `graphics-path:1.1.0`). Source: [16 KB page sizes](https://developer.android.com/guide/practices/page-sizes). Do not “fix” the compat dialog by tapping Don’t Show Again.

## Verify before done

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Release builds additionally need `:app:assembleRelease` green.

## Conventions

- Pure logic goes in small `object` policy classes under `bubble/`, `ui/`, `stt/`, each with a Truth unit test.
- Source-scan tests (`BubbleLayoutScanTest`, `UiSourceScan`) pin architecture — update their file targets when extracting code.
- Overlay/window failures must never crash the service; retry or notify honestly.
- Never touch opacity for visibility state — use `prefs.bubbleHidden`.
- Commits: explicit paths only, no `git add -A`. Author = Mitun only. No Co-Authored-By footers, ever — including CI and bot configs.

## Skills

Use the global skill set in `~/.config/opencode/skills/` when it applies:
spec-driven-development → planning-and-task-breakdown → incremental-implementation +
test-driven-development → code-review-and-quality → code-simplification →
git-workflow-and-versioning → shipping-and-launch.

## Automation

- CI (`.github/workflows/ci.yml`): unit tests + lint + debug APK + secret scan + authorship gate on every push/PR.
- Pages (`.github/workflows/pages.yml`): deploys `docs/` to GitHub Pages.
- No Dependabot. No bot commits. Author **Mitun only**. No Co-Authored-By. Dep bumps are Mitun commits.
