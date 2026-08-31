# Plan: Offline Smart (No-AI Track)

Spec: `docs/specs/offline-smart.md`. Combined from 20+ research sources (see spec).

## Architecture

```
system STT (unmodifiable)
  └─ N-best + confidence + span alternatives        [P1: HypothesisPick]
       └─ dictionary/context re-rank + span apply   [P1]
            └─ raw transcript
                 ├─ Stage 0 protect: atomic tokens (URL/email/number/code) [P2]
                 ├─ Stage 1 vocabulary: dictionary pairs, longest-first    [P2]
                 ├─ Stage 2 disfluency: fillers (guarded), stutters, reps  [P2]
                 ├─ Stage 3 self-correction: triggers + rough-copy align   [P2]
                 ├─ Stage 4 voice commands: table from voice_commands.json [P2]
                 ├─ Stage 5 ITN: numbers/dates/times/money/electronic      [P2]
                 ├─ Stage 6 homophones: conservative context rules         [P2]
                 ├─ Stage 7 format: context-aware caps/lists/trailing      [P4]
                 ├─ Stage 8 restore protected spans                        [P2]
                 └─ Invariant gate I1–I4; fail → fall back to light pass   [P3]
LearnEngine v2 (post-insertion): correction-vs-edit classifier → pairs     [P3]
```

Order of stages matters (parla-clean): vocabulary before fillers, protect/restore wrap everything.

## Phases (dependency order)

**P0 — Benchmark foundation (test-only, no behavior change)**
Golden corpus + Truth runner + per-category P/R/F1 + `baseline.json` pinning today's pipeline.
This is the measuring stick; nothing else is allowed before it.

**P1 — N-best smart pick (stt/)**
Harvest `RESULTS_RECOGNITION` + `CONFIDENCE_SCORES` + `RESULTS_ALTERNATIVES` in
`AndroidSpeechEngine`. New `HypothesisPick` policy: score hypotheses by dictionary +
field-context match (whole-phrase only, edit-distance <= 2 or phonetic-ish similarity,
PRISM-style no-prefix-match rule). Apply span alternatives when a dictionary term matches.
Pure object, Truth-tested. Golden hypothesis-list fixtures.

**P2 — Cleanup pipeline v2 (text/)**
Rework `CleanupPipeline` into staged, rules-as-data objects:
- `AtomicTokens` protect/restore (URL, email, number-ish, code-ish).
- `FillerTable` data-driven with context guards ("like" kept when content use;
  guarded "you know", "I mean", "well"); keep-when-emphasis rules.
- `StutterCollapse` (1–2 char x3+ → 1), function-word doubles, any-word triples,
  number doubles; keep 2x content-word emphasis; keep punctuation-separated repeats.
- `CourseCorrector v2`: expanded triggers (actually, no wait, sorry, scratch that,
  make that, rather, I said) + natural-restatement via rough-copy alignment
  (word edit distance between adjacent spans; keep repair side).
- `Itn*` semiotic classes (NeMo taxonomy): cardinals, ordinals, decimals, money,
  time, dates, phone-ish, email/URL spoken forms ("at"/"dot" only inside
  email/URL-ish spans). English v1.
- `HomophoneGuard`: its/it's, your/you're, their/there/they're, to/too/two,
  loose/lose — apply only on deterministic context rules; otherwise untouched.
- `VoiceCommands` table expansion to industry consensus set (assets JSON already close).
- Invariant gate: I1 word-origin check + I2 + I3 + I4; on violation fall back to
  current light pass and log (never crash).

**P3 — LearnEngine v2 + safety glue**
Correction-vs-edit classifier: time-since-insertion, edit size, word-overlap /
letter similarity (phone-ish), use of alternatives list. True corrections become
dictionary pairs (feeds P1 + P2 vocabulary stage) with confirmation threshold
(learn after N consistent hits, existing pending/auto model). Filter out edits
(Google FL lesson: learning all edits diverges). Timeout + fallback wiring in service.

**P4 — Context-aware formatting**
Extend existing AppContextEngine/TrailingPeriodPolicy: mid-sentence continuation
(lowercase join when field text precedes), messaging vs email trailing punct,
list detection ("one, ... two, ..." and "first ... second ..."), line-break capitalization.

**P5 — Hallucination guard (whisper path only)**
Repeated 4-gram loop collapse + signature-phrase flag list for the on-device whisper
ear (system STT mostly immune). Flag-and-keep-safe, never delete silently.

## Risks / Mitigations

- Over-aggressive rules destroy words → I1 gate + `notouch` FP=0 gate + precision-over-recall.
- ITN ambiguity ("may third" date vs name) → conservative grammars; convert only unambiguous shapes.
- Android OEMs return no alternatives/confidence → P1 degrades to 1-best + post-rules (P2 owns quality).
- Hinglish users → zero-AI Hindi rides system STT as-is; English ITN only; no regression promised.
- Perf on long text → pipeline is linear regex/table passes; 50ms budget enforced by test.
- Pinned scan tests (BubbleLayoutScanTest/UiSourceScan) → update file targets when extracting.

## Verification checkpoints

- After P0: baseline report committed; current pipeline measured.
- After P1: N-best golden set green.
- After P2: golden corpus F1 >= baseline + 15, notouch FP=0, property tests green.
- After P3: correction classifier >= 90% precision on synthetic set.
- After P4/P5: full verify command green + device smoke (bubble flow unchanged).
