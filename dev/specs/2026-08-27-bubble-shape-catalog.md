# Spec — Bubble shape catalog (single wire)

Date: 2026-08-27
Status: implementing

## Goal

One catalog owns floating bubble **shapes**. Settings, prefs normalize, geometry size, corner radius, and painter all read the catalog. Add a shape → it appears in UI and paints correctly. Tints own light/dark readable fill+on.

## Catalog fields (per shape)

| Field | Meaning |
|-------|---------|
| id | prefs string |
| label | settings chip |
| idleWDp / idleHDp | idle overlay size |
| oval | true → GradientDrawable.OVAL |
| cornerBaseDp | rect corner at roundPct=0 (ignored if oval) |
| cornerMaxDp | rect corner at roundPct=100 |

## Shapes

| id | label | size | form |
|----|-------|------|------|
| pill | Pill | 96×48 | rect, soft pill corners |
| slim | Slim | 112×36 | thin bar pill |
| chunk | Chunk | 80×56 | short thick pill |
| stadium | Stadium | 96×48 | rect, always near-full round |
| circle | Circle | 48×48 | oval |
| square | Squircle | 48×48 | rect, small corners |
| dot | Dot | 28×28 | oval |

Legacy unknown → `pill`.

## Colors

- User tint via `BubbleTint` (charcoal/cream/…).
- `BubbleTint.argb` / `onArgb` already contrast.
- Painter uses prefs palette (idle/listen/text), not hardcoded cream fill.
- Pulse ring alpha from **on** color (not fixed cream).
- App UI light/dark is separate; overlay contrast is tint-driven so it stays readable on any host app.

## Non-goals (this slice)

- Full repo delete / third_party wipe
- Adaptive NavigationSuiteScaffold
- Visual QA (after code green)

## Verify

- Unit: catalog normalize, sizes, corners, settings scan uses catalog
- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- Install 5554 only; visual later
