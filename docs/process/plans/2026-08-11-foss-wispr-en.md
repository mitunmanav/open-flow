# FOSS Wispr EN — Implementation Plan

> **For agentic workers:** TDD required. Work only in this worktree. No commit unless Mitun asks.

**Goal:** Close Wispr-parity gaps for en-US local polish: numbered-list rewrite, voice paren/colon/quote cmds, history markdown/txt export helper, lock `SttTuning.DEFAULT_LANGUAGE = en-US`.

**Architecture:** Pure Kotlin text pipeline (`TextPostProcessor`) + pure export helper. No network. No INTERNET permission.

**Tech stack:** Kotlin, JUnit + Truth, existing Room entities for export shape.

---

## Feature matrix (Wispr vs Open Flow)

| Feature | Wispr Android | Open Flow (this branch) | Better? |
|---------|---------------|-------------------------|---------|
| Floating bubble + a11y insert | Yes | Yes (F10) | = |
| Tap / hold-PTT | Yes | Yes | = |
| Auto punct + filler strip | Cloud | Local `TextPostProcessor` | FOSS/local |
| Self-correct | Cloud | `CourseCorrector` | FOSS/local |
| Numbered lists `1. A 2. B` | Yes | **This plan** | FOSS/local |
| Voice paren/colon/quote | Yes | **This plan** | FOSS/local |
| 100+ langs | Cloud STT | **Blocked — en-US only** (`LanguagePolicy`) | Privacy + focus |
| History export md/txt | Share | Pure `HistoryExport` | FOSS/local |
| Dictionary / snippets | Yes | Room + applyDictionary/expandSnippets | = |
| Clipboard fallback | Yes | Existing path | = |
| History export md/txt | Share | **This plan** pure helper | FOSS/local |
| Needs internet for STT | Yes | No (on-device prefer) | **OF wins** |
| Privacy / no account | Opt-in modes | Default local, no ads/analytics | **OF wins** |

---

## File map

| File | Role |
|------|------|
| `app/.../text/TextPostProcessor.kt` | Numbered-list + paren/colon/quote voice cmds |
| `app/.../text/TextPostProcessorTest.kt` | RED→GREEN tests |
| `app/.../export/HistoryExport.kt` | Pure md/txt formatter for dictation rows |
| `app/.../export/HistoryExportTest.kt` | TDD |
| `app/.../stt/SttTuning.kt` | Confirm `DEFAULT_LANGUAGE = "en-US"` |
| `app/.../stt/SttTuningDefaultsTest.kt` | Lock default lang in test |

---

## Security

| Check | Rule |
|-------|------|
| INTERNET | Not added |
| Export | Local string only; no upload |
| Mic | Unchanged; runtime only |
| Cleartext | Unchanged blocked |

---

## TDD tasks

### Task 1 — Numbered list + voice cmds (TextPostProcessor)

- [ ] Write failing tests for:
  - `"1. Apples 2. Bananas 3. Oranges"` → multiline `1. Apples\n2. Bananas\n3. Oranges`
  - Spoken digits `"1 apples 2 bananas 3 oranges"` → same shape
  - Voice: `open paren` / `close paren` / `colon` / `quote` / `open quote` / `close quote`
- [ ] Run tests → RED
- [ ] Implement minimal `applyListHints` + `applyVoiceCommands` changes
- [ ] Run tests → GREEN

### Task 2 — History export helper

- [ ] Failing tests: markdown header+bullets; plain txt lines; empty list → empty/minimal
- [ ] Implement `HistoryExport.toMarkdown` / `toPlainText`
- [ ] GREEN

### Task 3 — en-US lock

- [ ] Test `SttTuning.DEFAULT_LANGUAGE == "en-US"`
- [ ] Confirm constant already set (no change if already locked)

### Task 4 — Verify

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

### Device smoke (Mitun)

1. Install debug APK
2. Enable Flow Bubble a11y + mic
3. Dictate: `1. Apples 2. Bananas 3. Oranges` → see newlines
4. Dictate: `hello open paren world close paren colon quote hi`

---

## Out of scope

- Cloud STT / 100+ langs
- Commit / merge to main
- Touching other worktrees
