# Mitun leftover asks — honest audit

Worktree `feat/24-feel-fix`. Not main.

| Ask | Status | Note |
|-----|--------|------|
| Walkthrough align | **DONE** | Center + status bar (`eb0c7c0`) |
| Pill live vs settings | **DONE** | Idle pill overlay is wide (`3b0660c`). Settings preview is a small chip, not the live overlay. |
| Copy UX + timeout | **DONE** | Chip 6s default; UI 3 / 6 / 10; service uses `prefs.copyChipSec` |
| Haptics custom | **DONE** | Off / Light / Full. Light = CLOCK_TICK. Full = CONFIRM / REJECT / CONTEXT_CLICK |
| Color custom | **DONE** | Charcoal / Cream / Ink / Stone. Presets only — not a free color picker |
| Nav back | **DONE** | Dict tab back → Home (`ae1da21`) |
| Home layout | **DONE** | Already on this branch |
| 120 Hz | **DONE** | Pref + policy include 120. Device may clamp. |
| STT fast/balanced/accurate | **DONE** | Appearance chips + `SttTuning`. Applies on next listen |
| Mic status language | **DONE** | Bubble: Tap / Hearing… / Hearing 3s / Mic off. No checkmark |
| Own identity (not Wispr) | **DONE** | Own words + cream/charcoal mark (`6efe497`). No Wispr name in UI |
| Cleanup light+medium | **DONE** | Tests + pipeline (`1ae65a2`) |
| Dark readability | **DONE** | BrutalDark `onSurfaceVariant` bumped `#D8D3C8` |
| Export | **DONE** | History export + empty-list tests. Share label stays |
| Logo vector | **DONE** | Cream/charcoal vector (`6efe497`) |
| Bank hide | **PARTIAL** | India banks hide bubble (`1ae65a2`). **CANNOT** silence the system bank / a11y warning |

**CANNOT**

- Silence bank / Accessibility restricted-app warning. OS owns that. We only hide the bubble.

**PARTIAL on purpose**

- Colors = 4 presets, not RGB picker.
- 120 Hz = ask the display. Phone may stay at 90/60.
- Settings pill preview ≠ live overlay size (live is wide).
