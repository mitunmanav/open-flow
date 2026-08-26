# Spec: Offline Smart (No-AI Track)

Status: approved intent 2026-08-25. Research: `docs/research/` (this spec supersedes the multi-model direction — see "Rejected" below).

## Objective

Make Open Flow's offline dictation feel Wispr-class with **zero AI models** in the cleanup path.
System STT (unmodifiable) or future single on-device ASR feeds text; all "smartness" comes from
deterministic engineering + learning from the user's own corrections.

Goal: reach >= 75% of cloud-LLM (Claude) cleanup quality on a scored stress benchmark, on every
phone, with no download and no network.

User stories:

- User dictates into any app; text lands cleaned (fillers, stutters, false starts, spoken numbers)
  with their words preserved.
- User's dictionary and field context fix names/jargon the recognizer misheard (N-best re-ranking).
- User fixes a word once; the app learns it and stops repeating the mistake.
- Nothing is invented. When in doubt, don't touch.

## Tech Stack

- Kotlin, pure `object` policy classes under `app/src/test`-coverable packages (`text/`, `stt/`, `bubble/`).
- No new native libraries. No ONNX. No llama.cpp. Regex + tables + alignment algorithms only.
- Android `SpeechRecognizer` N-best APIs: `RESULTS_RECOGNITION`, `CONFIDENCE_SCORES`,
  `RESULTS_ALTERNATIVES` (span alternatives, API 33+).

## Commands

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Release gate: `:app:assembleRelease`.

## Project Structure

```
app/src/main/java/app/openflow/text/    cleanup policies (objects) + pipeline
app/src/main/java/app/openflow/stt/     N-best pick policy (HypothesisPick)
app/src/main/java/app/openflow/data/    LearnEngine persistence (existing)
app/src/main/assets/                    voice_commands.json, golden corpus (tests read via test assets)
app/src/test/java/app/openflow/text/    Truth tests, golden corpus runner
```

## Code Style

Existing convention: small `object` policy, pure functions, Truth test per object.

```kotlin
object ItnMoney {
    fun apply(t: String): String = MONEY.replace(t) { m -> writeMoney(m) }
}
```

Rules are data: filler/homophone/command tables are lists of records, tuned without touching logic.

## Testing Strategy

- Truth unit tests per policy object (repo convention).
- **Golden corpus** (`app/src/test/.../golden/`): raw→expected pairs, categorized
  (filler, stutter, selfcorrect, itn_number, itn_date, itn_time, itn_money, itn_electronic,
  list, notouch, context, commands). Runner reports per-category word-level P/R/F1.
  Baseline pinned in `baseline.json`; CI fails on regression below baseline; `notouch` must keep FP = 0.
- Property tests (seeded PRNG): invariants I1–I4 below. No fuzzing deps.

## Boundaries

Always:
- Preserve invariants I1–I4 on every pipeline run.
- Keep raw transcript untouched in DB (`rawText`); clean is derived.
- Longest-match-first, word-boundary matching for all phrase rules.
- Atomic tokens: URLs, emails, numbers, code-ish spans are never rewritten inside.

Ask first:
- New dependencies, DB schema changes, manifest changes.
- Changing cloud brain paths (out of scope here).

Never:
- Insert words the user did not say (I1).
- Touch `third_party/` vendored code.
- Delete failing tests to pass CI.

## Invariants (the contract)

- **I1 No invention** — every output word is a substring of an input word, a deterministic
  transform of one (number/date/symbol rendering), or a user-dictionary term.
- **I2 Terminal punctuation survives** — input ending in `.!?` ends with terminal punctuation
  unless the last words were a removed filler/declarative tag.
- **I3 Totality** — every input produces output; pipeline never throws on user text.
- **I4 Idempotence** — `clean(clean(x)) == clean(x)`.

## Success Criteria

1. Golden corpus: aggregate word F1 >= baseline + 15 points; `notouch` FP = 0.
2. Property tests for I1–I4 pass on 1k seeded random + adversarial inputs.
3. Cleanup latency < 50ms for a 500-word input on the JVM test runtime.
4. N-best picker: on a golden hypothesis-list set, dictionary-term recall >= 90% with
   zero substitutions where no dictionary term is phonetically close.
5. LearnEngine v2: correction-vs-edit classification precision >= 90% on synthetic pairs.
6. Full verify command green: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## Rejected direction (do not re-propose without Mitun)

- Multiple on-device models (whisper + VAD + normalizer). Mitun: max ONE model, later phase.
- LLM cleanup model on phone. Cleanup stays code.

## Open Questions

- Hinglish (hi-IN) zero-AI path: system STT handles Hindi; rule ITN stays English-only for now. OK?
- Golden corpus text will be hand-written synthetic sets (no user data). OK?

## Research Sources (key)

- parla-clean (github.com/Maayk/parla-clean): invariants I1–I4, golden corpus CI gate, rules-as-data,
  precision-over-recall, atomic tokens, fixed pass order.
- EnviousWispr (enviouswispr.com/blog/on-device-dictation-polishing-small-models): deterministic
  pre/post around model, anti-instruction framing, timeout fallback, 20-behavior stress benchmark.
- GoodTurn (goodturn.ai): mechanical no-rephrase verifier (token diff + allowlisted deletes).
- NeMo ITN / Sparrowhawk (Interspeech 2021 zhang21ga): WFST ITN as production standard; semiotic
  classes CARDINAL/ORDINAL/DATE/TIME/MONEY/ELECTRONIC/WHITELIST; low tolerance for unrecoverable errors.
- Android SpeechRecognizer docs: RESULTS_RECOGNITION, CONFIDENCE_SCORES, RESULTS_ALTERNATIVES,
  AlternativeSpan ("re-rank/apply ... before powering dictation features").
- Disfluency literature (Liu et al. 2003; ACL N04-4040; D18-1490): interruption-point + rule onset,
  rough-copy alignment for repairs, repetition-pattern detection, lexical rules suffice without prosody.
- Microsoft 2004 (unsupervised learning from user corrections) + US8280733: correction-vs-edit
  inference from time-to-edit, edit size, phonetic similarity; learn only true corrections.
- Google FL corrections paper (arxiv 2310.00141): training on all edits diverges; filter to likely
  misrecognitions first.
- PRISM (EMNLP 2023): whole-phrase-only matching with backoff avoids spurious dictionary matches.
- whisper-cleanup (github.com/tinyproof/whisper-cleanup): hallucination phrase list, repeated
  4-gram loop detection, flag-don't-delete.
- Handy PR #589: stutter collapse (1–2 char word repeated 3+ times → 1), filler list, VAD threshold.
- Spoken-punctuation consensus tables: Windows SR, Deepgram dictation, Nabla, WhisperTyping,
  TongueType, Ottex (all converge on the same command set).
- gladia-normalization: 3-stage fixed-order deterministic pipeline, protect/restore pairs.
