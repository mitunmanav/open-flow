# ScreenshotTest placeholder (CPST scaffold)

This source set is NOT compiled until AGP 9.0 + `android.experimental.enableScreenshotTest=true`.

See `docs/specs/visual-baseline.md` for setup steps.

When ready, add `app/src/screenshotTest/kotlin/app/openflow/screenshots/PreviewScreenshots.kt` with `@PreviewTest` composables for HomeFeed / Insights / SetupWizard / Bubble.

Until then, use `scripts/qa/visual-capture.sh` for manual visual evidence per gate.

Do NOT add `screenshotTestImplementation` deps until AGP 9 upgrade (build will break on AGP 8).
