# F15b Theme + Wispr bubble UX

**Goal:** Light/dark actually works; bubble feels closer to Wispr Flow Android.

**Wispr reference (docs + device):**
- Floating pill over text fields
- Tap dictate / long-press hold-to-dictate
- Recording: waveform + Cancel + Done
- Drag reposition; bottom snooze 10 min
- Overlay + a11y + mic

**Done:**
1. Theme-aware components (MaterialTheme.colorScheme) — SecUi, OpenCard/Chip/Button/TextField/EmptyState
2. Status bar light/dark icons
3. BubbleVisibility pure rules + tests
4. Idle orb; listen bar Cancel | status | Done
5. Smooth press + edge snap spring
6. Show on field focus; hide when no field (keep while listening)

**Security:** No new permissions. Still TYPE_ACCESSIBILITY_OVERLAY (not SYSTEM_ALERT_WINDOW).
