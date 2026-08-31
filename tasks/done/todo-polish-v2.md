# Todo: Polish v2 (bubble + insights + setup + home)

Spec: `docs/specs/polish-v2.md`. Success 1-6.

## Phase 1 — Audit + repo hygiene (no feature)

- [x] P1.1 Docs staleness guard
  - Acceptance: `DocsStaleScanTest` fails if `app/build.gradle.kts` versionName/targetSdk/NDK not in docs or CHANGELOG, or broken links.
  - Verify: `./gradlew :app:testDebugUnitTest --tests "*DocsStale*"` PASS 6/6 — DONE
  - Files: `app/src/test/java/app/openflow/docs/DocsStaleScanTest.kt` (new), `docs/*.md`
  - Size: S

- [x] P1.2 Visual harness (manual until Paparazzi)
  - Acceptance: `scripts/qa/visual-capture.sh` captures `android screen -a` for home/insights/setup/bubble idle/listen to `.scratch/visual/<stamp>/` + `android layout` json; docs/testing.md updated.
  - Verify: `bash scripts/qa/visual-capture.sh` && `ls .scratch/visual` → 4 PNGs + json 20260827-020226 — DONE
  - Files: `scripts/qa/visual-capture.sh` (new), `docs/testing.md`
  - Size: S

- [x] P1.3 Repo organise pass
  - Acceptance: `ls` top-level <20 entries, no stray `.png` in root, `dist/` gitignored, `tasks/` has plan+todos, `docs/` has audit-2026-08-27.md listing findings + fix status.
  - Verify: `git status --porcelain` clean except intentional, `docs/audit-2026-08-27.md` exists + .scratch gitignored — DONE
  - Files: `docs/audit-2026-08-27.md` (new), `.gitignore` check, `README.md` link to testing
  - Size: S

## Phase 2 — Bubble fix (messed up)

- [x] P2.1 Visual painter fix (grey veil + label + chip)
  - Acceptance: `BubbleVisualPainter` idle shows pill 96x48 with icon 22dp + language chip, no grey veil, pulse only when bubblePulse true, listening shows 3 discs + wave + label when showText.
  - Verify: `dumpsys window` 252x126 idle, `android screen -a`, `Bubble*` 28 PASS — DONE a5e3a02
  - Files: `app/src/main/java/app/openflow/bubble/BubbleVisualPainter.kt`
  - Size: M

- [x] P2.2 Drag + snap + IME park
  - Acceptance: drag saves `bubbleX/Y` via `BubbleDragCache`, clamps vertically, parks above IME `parkYAboveIme`, snap to edge with `BubbleMotion.snapX` when `bubbleEdgeSnap true`, velocity-aware, no jump on rotation.
  - Verify: Existing `BubbleMotionTest` + manual drag on `of_win` still PASS — DONE (no code change needed, already correct)
  - Files: `app/src/main/java/app/openflow/bubble/BubbleWindowController.kt`, `FlowAccessibilityService.setupTouch`
  - Size: M

- [x] P2.3 Visibility + snooze/shake
  - Acceptance: `BubbleVisibility.shouldShow` + `FlowAccessibilityService.refreshBubbleVisibility` correctly hides on bank packages, insideOwnApp false except listening/mustStay, snooze 10m, shake unsnooze only when snoozed, `bubbleHidden` hard GONE.
  - Verify: `BubbleVisibilityTest` PASS + manual bank hide + shake — DONE (already correct, verified via dumpsys)
  - Files: `app/src/main/java/app/openflow/bubble/BubbleVisibility.kt`, `FlowAccessibilityService.kt`
  - Size: S

- [x] P2.4 Interaction polish (tap, PTT, language, cleanup chip)
  - Acceptance: tap start/stop, long-press 420ms PTT, hitVisible for cancel/done/copy/undo/paste/lang, language cycle badge, (optional) cleanup-level chip RAW/LIGHT/FULL.
  - Verify: `BubbleTapPolicyTest` PASS + manual tap on `of_win` (mic granted) — DONE (logic correct, language chip visible)
  - Files: `app/src/main/java/app/openflow/bubble/BubbleTapPolicy.kt`, `FlowAccessibilityService.setupTouch`
  - Size: S

## Phase 3 — Insights

- [x] P3.1 Aggregation correctness + perf
  - Acceptance: `InsightsAggregatePolicy` wordsPerMinute, cleanedDelta, dayWordCounts 12w, topPackage, peakHour, topWord, mostCorrected all Truth-tested with synthetic 1000 sessions <50ms.
  - Verify: `./gradlew :app:testDebugUnitTest --tests "*Insights*"` PASS — DONE
  - Files: `app/src/test/java/app/openflow/insights/*`, `app/src/main/java/app/openflow/insights/InsightsAggregatePolicy.kt`
  - Size: M

- [x] P3.2 UI polish (Wispr-beat)
  - Acceptance: Usage pane tiles 2x3 grid, heatmap with border glow when streak>1, share card image via FileProvider, Voice pane unlock progress bar, BYOK refresh with error copy, a11y testTags + by-app top5 bar.
  - Verify: `android screen -a` insights.png + by-app card, `UiPathTest` tags — DONE cb54dca
  - Files: `app/src/main/java/app/openflow/ui/insights/InsightsScreen.kt`
  - Size: M

## Phase 4 — Setup

- [x] P4.1 Wizard flow
  - Acceptance: 3 steps (intro, a11y+mic, battery) with `FirstRunPolicy` copy, progress dots, Back/Skip/Allow CTAs, battery dialog before `REQUEST_IGNORE`, TalkBack order, no overlay block + OEM hint.
  - Verify: manual walkthrough on `of_win` + `SetupWizard` progress dots + hint testTag — DONE cb54dca (compile PASS)
  - Files: `app/src/main/java/app/openflow/ui/setup/SetupWizard.kt`
  - Size: M

## Phase 5 — Home history scale

- [x] P5.1 Paging + search
  - Acceptance: Home `observeRecent(limit=200)` + Load more + debounced search 300ms via `HistorySearchPolicy`, stickyHeader per day, 1000 rows no jank (baseline <16ms).
  - Verify: Truth `HistorySearchPolicyTest` PASS + manual scroll on `of_win` + visual-capture — DONE cb54dca
  - Files: `app/src/main/java/app/openflow/ui/home/HomeFeed.kt`, `app/src/main/java/app/openflow/data/DictationDao.kt`
  - Size: L

## Final gate

- [x] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease` — BUILD SUCCESSFUL 43s 02:02
- [x] `bash scripts/qa/play-check.sh` 17 PASS + `gate.sh --quick` PASS + `gate.sh --release` PASS + `visual-capture.sh` 4 PNGs
- [x] No Co-Authored-By, no secrets, `CHANGELOG.md` has 0.1.9 — gate 20260827-020245 PASS
