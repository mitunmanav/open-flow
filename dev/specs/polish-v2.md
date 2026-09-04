# Spec: Polish v2 — Bubble + Insights + Setup + Home scale

Status: draft 2026-08-27. Beat Wispr Flow where they are weak, keep what we do well.
Sources: wispr-teardown.md, wispr-ui-teardown.md, testing.md, play-store-readiness.md

## Objective

- Fix floating bubble so it feels Wispr-class (or better) on `of_win` 16KB.
- Make Insights actually useful (aggregation, not just tiles).
- Make Setup 2-min and honest (a11y + mic + battery).
- Keep Home history fast when 1000+ rows (paging + search).

No new permissions. No new native deps. Keep MIT.

## Tech Stack

- Kotlin + Compose, `object` policies, Truth tests, `scripts/qa/gate.sh` + `play-check.sh`
- Device: `of_win` Windows `-gpu host`, wrap-adb, `android screen capture -a` for visuals
- Visual guard: Paparazzi/Roborazzi later, for now `android screen` + `layout` dumps + manual compare

## Commands

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease
bash scripts/qa/play-check.sh
bash scripts/qa/gate.sh --quick
bash scripts/qa/functional-check.sh
android screen capture -a -o /tmp/bubble-*.png
android layout --pretty
```

## Project Structure

```
app/src/main/java/app/openflow/bubble/  FlowAccessibilityService, BubbleVisualPainter, BubbleGeometry, etc.
app/src/main/java/app/openflow/ui/home/ HomeFeed, HistoryScreen, HomeHonestyFooter
app/src/main/java/app/openflow/ui/insights/ InsightsScreen, InsightsAggregatePolicy
app/src/main/java/app/openflow/ui/setup/ SetupWizard, FirstRunPolicy
app/src/main/res/layout/flow_bubble.xml
scripts/qa/  gate, play-check, functional-check, crash-scan
docs/specs/polish-v2.md  this
docs/testing.md  gate + E2E
```

## Code Style

Keep `object` policies pure, small. One `Truth` test per policy. No `Thread.sleep` in tests, use `waitUntil`.

Example:

```kotlin
object BubbleVisibility {
  fun shouldShow(...): Boolean = ...
}
```

## Testing Strategy

- Keep unit Truth tests for every policy (Bubble* , Visibility, etc.)
- Device: `gate.sh --quick` + `functional-check.sh` 6/6 on `of_win`
- Visual: capture `android screen -a` for bubble idle/listen/chips, home header, insights heatmap, setup steps; store in `.scratch/qa/<stamp>/` and compare manually until Paparazzi lands
- Docs staleness: `QaLoopScanTest` + new `DocsStaleScanTest` pin versionName/targetSdk/NDK/docs links

## Boundaries

Always:
- Verify on `of_win` before merge; no blind refactor of bubble state machine
- Keep `INTERNET` honest, `allowBackup false`, no keystore in git
- Use `prefs.bubbleHidden` for tile hide, not opacity

Ask first:
- New `uses-permission`, new SDK, DB migration, changing `targetSdk`

Never:
- Commit `*.png` baselines without review, edit vendor `third_party`, add Co-Authored-By

## Success Criteria

1. Bubble: idle pill at (32,220) BOTTOM END, drag saves X/Y, snap to edge, park above IME, tap start/stop, long-press PTT 420ms, drag to top snooze 10m, shake unsnooze, language chip cycles, no grey veil, no tap-thief (GONE not alpha 0), recovery posts nudge after 3 fails. Verified via `dumpsys window` + `screen -a` + `functional`.
2. Insights: Usage tab shows Words/Sessions/WPM/Streak/Cleaned + heatmap 12w + topApp + share text/card; Voice tab shows peakHour/topWord/mostCorrected/topApp + unlock progress + BYOK refresh; perf <50ms for 1000 sessions on JVM test.
3. Setup: 3 steps (welcome, a11y+mic, battery) from `FirstRunPolicy`, progress dots, skip clear, battery dialog plain language before `REQUEST_IGNORE`, TalkBack order, not blocked by overlay.
4. Home history: LazyColumn with `Paging` or `items` + `stickyHeader` per day, search debounced 300ms, filter via `HubListPolicy`, 1000 rows <16ms frame, footer 88dp not clipped, empty/loading/error states, tests for `HistorySearchPolicy`.
5. Docs: every `docs/*.md` link + version pin checked by `DocsStaleScanTest`; `CHANGELOG.md` has `## versionName`; `docs/testing.md` gate docs match scripts.
6. Repo: `tasks/todo-play.md` + this spec green, `git log --oneline` no Co-Authored-By, `ci.yml` green (unit+lint+bundle+play-check).

## Open Questions

- Bubble inside own app: keep hidden (anti-tap-thief) or show for History search? Current is hidden unless listening.
- Insights BYOK refresh: which brain model for voice flavor? Keep current `currentBrain()`.
- Home paging: Room Paging3 or manual limit+offset? Keep simple limit 200 + load-more for now.
