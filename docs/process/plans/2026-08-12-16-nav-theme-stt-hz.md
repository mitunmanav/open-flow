# F16 Nav + theme visibility + STT speed + 60–144 Hz

**Goal:** Fix confused back nav, light/dark readability, better layout editor, faster STT knobs, adaptive refresh.

## Honest Wispr parity

| Wispr | Open Flow |
|-------|-----------|
| Bubble + a11y insert | yes |
| Dict / snippets / styles | yes (local) |
| Cleanup levels | yes (rules, no cloud AI) |
| Cancel/Done listen bar | yes |
| Clipboard fallback | yes (this change) |
| Cloud AI rewrite / Command Mode | **no** — FOSS local only |
| Account sync | **no** |

## Ship

1. NavStack real back stack
2. AppShell theme colors (dark readable)
3. BrutalLight/Dark contrast
4. STT profiles fast/balanced/accurate + snappier defaults
5. Display refresh 60/90/120/144 via preferredDisplayModeId
6. Layout editor numbered + reset
7. Clipboard if SET_TEXT fails

**Security:** no new permissions / no INTERNET.
