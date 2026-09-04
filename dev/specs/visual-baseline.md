# Visual baseline — next step (scaffold)

## Why

Manual `scripts/qa/visual-capture.sh` captures 4 PNGs + layout JSON per gate. It is cheap and honest until automated goldens land. Suggested next was Paparazzi / Compose Preview Screenshot Testing (CPST).

## Decision (deferred full install)

**Source:** `kb://android/studio/preview/compose-screenshot-testing` — Requirements:
- CPST 0.0.1-alpha15 needs **AGP 9.0+** + `gradle.properties android.experimental.enableScreenshotTest=true` + new `screenshotTest` source set.
- Project is on **Gradle 8.13 + AGP 8.x** (see `./gradlew --version`). Upgrading to AGP 9 is a separate tracked migration (see `agp-9-upgrade` skill) — not done in this slice to keep CI green.

So full CPST not installed now. Scaffold only — keep manual harness as gate, add spec + placeholder for AGP 9.

## What exists now (evidence per gate)

- `scripts/qa/visual-capture.sh` → `.scratch/visual/<stamp>/` 4 PNGs (home/insights/history/bubble) + 4 layout JSON + 4 xml + `bubble.window.txt`.
- `docs/testing.md` documents gate stages + visual step.
- `.scratch/` gitignored `.gitignore:63`, not committed.

Verify after any UI change:

```
bash scripts/qa/visual-capture.sh
ls .scratch/visual/<stamp>/   # 4 PNGs + json
android screen capture -a -o .scratch/visual/<stamp>/home.png
android layout --pretty > .scratch/visual/<stamp>/home.layout.json
```

## Scaffold for AGP 9 (when ready)

1. Bump AGP to 9.0-rc03 + Kotlin 2.2.10 + JDK 17 (already JDK 17).
2. Add to `gradle.properties`:

```
android.experimental.enableScreenshotTest=true
android.compose.screenshot.maxHeapSize=4g
```

3. Add to version catalog (when added) or `app/build.gradle.kts`:

```
plugins { alias(libs.plugins.screenshot) } // com.android.compose.screenshot 0.0.1-alpha15
android { experimentalProperties["android.experimental.enableScreenshotTest"] = true }
dependencies {
  screenshotTestImplementation(libs.screenshot.validation.api)
  screenshotTestImplementation(libs.androidx.ui.tooling)
}
```

4. Create source set `app/src/screenshotTest/kotlin/app/openflow/screenshots/` with:

```kotlin
@PreviewTest @Preview(showBackground = true)
@Composable fun HomeFeedPreview() { OpenFlowTheme { HomeFeed(...) } }
@PreviewTest @Preview(showBackground = true)
@Composable fun InsightsPreview() { OpenFlowTheme { InsightsScreen(...) } }
@PreviewTest @Preview(showBackground = true)
@Composable fun SetupWizardPreview() { OpenFlowTheme { SetupWizard(...) } }
@PreviewTest @Preview(showBackground = true)
@Composable fun BubbleIdlePreview() { OpenFlowTheme { BubbleVisualPainter idle } }
```

5. Generate refs: `./gradlew updateDebugScreenshotTest` → `app/src/screenshotTestDebug/reference/`, commit refs.
6. Validate: `./gradlew validateDebugScreenshotTest` → `app/build/reports/screenshotTest/preview/debug/index.html`.
7. Keep manual `visual-capture.sh` as fallback until CPST stable; document in `docs/testing.md`.

## Acceptance for this scaffold

- This doc exists and `DocsStaleScanTest` does not require it yet — manual harness still PASS.
- No build break: `app/build.gradle.kts` unchanged, no new source set compiled until AGP 9.
- Placeholder dir `app/src/screenshotTest/` has README only, not compiled.

## Reference

- Compose Preview Screenshot Testing setup: https://developer.android.com/studio/preview/compose-screenshot-testing
- Paparazzi AGP 9 upgrade note: kb://android/agents/skills/build/agp/agp-9-upgrade/references/paparazzi-gradle-9
