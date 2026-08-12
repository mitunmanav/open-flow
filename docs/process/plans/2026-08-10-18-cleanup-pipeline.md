# F18 — Cleanup pipeline core (English MVP)

**Branch:** `feat/18-cleanup-pipeline`  
**Worktree:** `.worktrees/f18-cleanup-pipeline`  
**Date:** 2026-08-10

## Goal

Prove core promise (English first):

> Speak → Android STT → local cleanup + self-correct → insert clean text → keep raw recoverable.

Align with product prompt without rebuild-from-zero.

## Android docs (searched)

- `SpeechRecognizer` — may stream audio to remote; not always offline. Prefer `createOnDeviceSpeechRecognizer` when available; `EXTRA_PREFER_OFFLINE`.
- `RecognizerIntent` — `EXTRA_LANGUAGE`, silence extras, `EXTRA_ENABLE_FORMATTING` (API 33).
- `AccessibilityNodeInfo.ACTION_SET_TEXT` — field insert.
- Do **not** claim full offline for system STT.

## In scope

1. `Correction` model + analyzer (marker / original / replacement / confidence).
2. Staged `CleanupPipeline` (normalize → fillers → reps → false starts → corrections → punct → caps).
3. Stronger course-correct: amount `430 actually 530`, dates, `no, send to X`.
4. `SpeechEngine` interface + `AndroidSpeechEngine` (wrap existing `SttEngine`).
5. `TextAIProvider` + `NoAI` (MVP path).
6. Dual store: `rawText` + `cleanText` on dictations.
7. No automatic clipboard overwrite; last session held for in-app Copy.
8. Honest privacy dashboard / strings (system STT may leave device).
9. Unit tests for pipeline examples.

## Out of scope (later)

- Hindi UI strings (architecture only)
- Local LLM / cloud AI
- Full Keystore field encryption (note for F19)
- Meeting recorder / export
- Custom STT model

## TDD

Failing tests first on:

- `"The amount is 430, actually 530."` → `530` only
- `"I, uh, like, um, pizza."` → `"I like pizza."` (keep real *like*)
- `"Send it to John. No, send it to James."` → James
- `"The deadline is August 12th, no, August 15th."` → 15th
- Raw preserved in pipeline result

## Files

- `text/Correction.kt`, `text/CleanupPipeline.kt`, enhance `CourseCorrector.kt` / `TextPostProcessor.kt`
- `stt/SpeechEngine.kt`, `stt/AndroidSpeechEngine.kt`
- `ai/TextAIProvider.kt`
- `data/DictationEntities.kt`, `DictationRepository.kt`
- `bubble/FlowAccessibilityService.kt` (save raw+clean, no auto-clip)
- `privacy/PrivacyDefaults.kt`, `res/values/strings.xml`
- `ui/MainActivity.kt` (history raw/clean, last result)
- tests under `app/src/test/...`

## Security

- No INTERNET permission.
- No new permissions.
- Honest STT privacy copy only.

## Device check

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
# phone: dictate "amount is 430 actually 530" → field shows 530; History shows raw
```

## Merge

Commit on feature branch when green; merge main after Mitun OK.
