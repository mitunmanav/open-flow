# Open Flow — Agent Rules

Wispr-Flow-style Android dictation app (floating bubble, Kotlin/Compose, accessibility service).

## Comms (strict)

- Caveman style. Short lines. Easy words.
- Bullet points only. Brief.
- Report format: DID / PASS-FAIL / NEXT / SUGGEST / ASK.

## Android workflow (mandatory)

- Always use the `android-cli` skill for device, emulator, SDK, layout and doc-search work.
- Web search before non-trivial implementation decisions; cite sources in the report.
- Load matching installed skills (compose/tv/wear/testing/profiler etc.) when the task touches their domain.
- Verify on a device or emulator when runtime behavior changes; no blind refactors of state machines.

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

- CI (`.github/workflows/ci.yml`): unit tests + lint + debug APK on every push/PR. Commits by github-actions[bot] only.
- Dependabot (`.github/dependabot.yml`): weekly gradle + actions bumps via dependabot[bot].
