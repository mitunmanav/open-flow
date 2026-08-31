# Tasks: Offline Smart (No-AI Track)

Plan: `tasks/plan.md`. Spec: `docs/specs/offline-smart.md`.

## P0 — Benchmark foundation

- [x] T1: Golden corpus fixtures
  - Acceptance: >= 200 raw→expected pairs across 12 categories in test assets; every line categorized.
  - Verify: fixture loader test green; counts per category printed.
  - Files: app/src/test/java/app/openflow/text/golden/ (new fixtures + loader)
- [x] T2: Scoring runner + baseline pin
  - Acceptance: Truth test runs current CleanupPipeline over corpus, emits per-category word P/R/F1, writes `baseline.json`.
  - Verify: `./gradlew :app:testDebugUnitTest --tests '*GoldenCorpus*'`; baseline committed.
  - Files: app/src/test/java/app/openflow/text/GoldenCorpusTest.kt (new)

## P1 — N-best smart pick

- [x] T3: HypothesisPick policy
  - Acceptance: pure object; scores N-best by dictionary + field context; whole-phrase-only match; edit-distance gated; no dictionary → picks 1-best.
  - Verify: Truth tests incl. spurious-prefix cases.
  - Files: app/src/main/java/app/openflow/stt/HypothesisPick.kt (new), tests
- [x] T4: Harvest N-best + alternatives in AndroidSpeechEngine
  - Acceptance: onResults/onPartialResults capture RESULTS_RECOGNITION, CONFIDENCE_SCORES, RESULTS_ALTERNATIVES (guarded API level); feed HypothesisPick.
  - Verify: unit tests for bundle parsing via seam; debug build green.
  - Files: stt/AndroidSpeechEngine.kt, stt/HypothesisPick.kt

## P2 — Cleanup pipeline v2

- [x] T5: AtomicTokens protect/restore
  - Acceptance: URL/email/number/code spans survive all stages byte-exact.
  - Verify: Truth tests; I3 on random strings with URLs.
  - Files: text/AtomicTokens.kt (new)
- [x] T6: FillerTable guards (lived in CleanupPipeline.stripFillers, no new file)
  - Acceptance: filler list = data records with context guards; "like" content use kept; emphasis kept.
  - Verify: Truth tests incl. guard cases; notouch FP=0 on golden.
  - Files: text/CleanupPipeline.kt refactor, text/FillerTable.kt (new)
- [x] T7: Stutter/repetition policies (text/StutterCollapse.kt)
  - Acceptance: stutter collapse (1–2 char x3+), function-word doubles, any-word triples; keeps 2x emphasis + punct-separated repeats.
  - Verify: Truth tests.
  - Files: text/StutterCollapse.kt (new) or extend CollapseRepetitions
- [x] T8: CourseCorrector v2 (bare no / no-wait triggers; wait/actually/i-mean bare = open)
  - Acceptance: expanded trigger set + rough-copy natural restatement; keeps repair side; never deletes unique content (false-start caution from literature).
  - Verify: Truth tests incl. "I actually enjoyed the movie" preserved case.
  - Files: text/CourseCorrector.kt
- [x] T9: ITN semiotic classes (ItnNumber/ItnDateTime/ItnMoney/ItnElectronic + Itn facade)
  - Acceptance: cardinals, ordinals, decimals, money, time, dates, phone-ish, spoken email/URL; unambiguous shapes only; atomic-token aware.
  - Verify: Truth tests per class; golden itn_* categories improve.
  - Files: text/ItnNumber.kt, ItnDateTime.kt, ItnMoney.kt, ItnElectronic.kt (new)
- [ ] T10: HomophoneGuard (deferred: homophone golden already 1.000 without it)
  - Acceptance: conservative context rules for its/it's, your/you're, their/there/they're, to/too/two, loose/lose; default = untouched.
  - Verify: Truth tests; notouch golden FP=0.
  - Files: text/HomophoneGuard.kt (new)
- [x] T11: Invariant gate I1–I4 + fallback
  - Acceptance: runtime word-origin check; violation → light-pass fallback, never crash; I4 idempotence.
  - Verify: property tests (seeded PRNG, 1k inputs); Truth tests.
  - Files: text/InvariantGate.kt (new), CleanupPipeline wiring
- [x] T12: VoiceCommands consensus expansion (brackets -> []; quote spacing)
  - Acceptance: assets table covers Windows/Deepgram/Nabla/TongueType consensus; longest-first; word boundaries.
  - Verify: Truth tests; assets JSON review.
  - Files: app/src/main/assets/voice_commands.json, text/VoiceCommands.kt

## P3 — LearnEngine v2 + safety

- [x] T13: Correction-vs-edit classifier
  - Acceptance: pure policy; inputs = time since insertion, edit distance, word overlap, alternatives used; precision >= 90% on synthetic set.
  - Verify: Truth tests + synthetic eval test.
  - Files: text/CorrectionClassifier.kt (new) or extend LearnEngine
- [x] T14: Learned pairs feed pipeline + pick
  - Acceptance: confirmed pairs reach vocabulary stage (P2) and HypothesisPick bias (P1); N-hit confirmation kept.
  - Verify: integration Truth tests; existing LearnEngineTest green.
  - Files: text/LearnEngine.kt, prefs wiring
- [x] T15: Timeout + fallback in service
  - Acceptance: cleanup bounded; on anomaly bubble inserts raw/light text with honest notice; critical path never blocked.
  - Verify: Truth tests on policy seam; debug build.
  - Files: bubble/FlowAccessibilityService.kt wiring, text/TextPostProcessor.kt

## P4 — Context-aware formatting

- [x] T16: Continuation + trailing punct + lists
  - Acceptance: mid-sentence lowercase join; messaging/email trailing rules; numbered/first-second list detection.
  - Verify: Truth tests; golden context category improves.
  - Files: text/ policies + bubble/AppContextEngine.kt

## P5 — Hallucination guard

- [x] T17: Loop + signature guard for whisper path
  - Acceptance: repeated 4-gram collapse; signature phrase flag (no silent delete).
  - Verify: Truth tests.
  - Files: whisper/TranscriptParts.kt or text/ (new policy)

## Done = spec success criteria 1–6 green.

## P2 leftover pressure items (P3 fuel)

- Bare `wait` / `actually` / `i mean` triggers stay **open** (T8). Only `no` / `no wait` / `wait no` fire unpunctuated.
- ~~"it costs 5 bucks actually 7" -> money splice glues "7bucks"~~ RESOLVED (T11).
- ~~Emphasis triples ("no no no i insist")~~ RESOLVED: keep-when-emphatic.
- ~~Letter-stutter "w w why" -> "Why?"~~ RESOLVED: bare why is not a question.
- ~~Serial comma insertion ("milk eggs and bread")~~ RESOLVED (P4).
- ~~Mid-sentence sentence-split ("noon it was fun")~~ RESOLVED (P4).

## T11 follow-ups (not blocking)

- ~~AI brain rewrite path (`brain.enhance`) bypasses InvariantGate~~ RESOLVED: gated, falls back to local.
- Convergence doubles pipeline sweeps for unstable inputs (max 3); latency measured in CleanupBudgetTest (500 words < 50ms).
- Golden corpus: 2 expectations re-pinned to idempotent fixpoints ("so like we need...", "5 bucks actually 7").
