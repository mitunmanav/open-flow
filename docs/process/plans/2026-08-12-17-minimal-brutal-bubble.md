# F17 — Minimal brutal UI + bubble chrome

## Goal
- App skin: **minimal brutal** (cream/charcoal, hard edge, thin offset — not chunky).
- Floating bubble: match product (not soft zinc + purple). Hard edge, high contrast, readable light/dark screens.

## Visual findings (device screenshots 2026-08-12)
- App Style/Settings: thick 3px + 4dp offset cards — brutal but heavy.
- Bubble on Brave/Google: soft dark **circle** orb, low contrast on dark pages, generic Material mic — **not** brutal, not Wispr-polished.
- Purple/indigo listen stroke clashes with cream brutal app.

## Files
- `bubble/BubbleChrome.kt` + tests (colors, corners, listen bar)
- `bubble/FlowAccessibilityService.kt` — use chrome
- `res/drawable/bg_flow_bubble.xml` — hard rect
- `ui/components/OpenCard.kt` — 2dp border + 2dp offset
- `ui/MainActivity.kt` — SecUi borders 2dp
- `BubbleGeometry.kt` — square corners 2dp (minimal)

## TDD
1. RED: BubbleChrome idle/listen colors + square corner 2dp
2. GREEN: implement
3. Visual: reinstall, screenshot text field + dark web page

## Security
- Overlay only; no new perms; no network.

## Mitun test
1. Enable a11y + mic
2. Dark page + light page → orb visible, hard edge
3. Tap → listen bar Cancel | status | Done hard chrome
4. App cards thinner offset, still cream/charcoal
