# F12 — UI/UX Foundation

**Date:** 2026-08-10
**Feature:** UI/UX design system foundation for all 5 screens
**Scope:** Theme, color, components, motion, a11y primitives. NOT screen-level polish (that is F13).

---

## Goal

Polish existing 5 tabs (Home, Dictionary, Snippets, Style, Settings) by building a reusable design system foundation: theme tokens, component library, motion primitives, and WCAG AAA accessibility primitives. All screens in Spec 2 use these components.

---

## Decisions

| Question | Answer |
|---|---|
| Visual mood | Calm pro — Wispr-clean + Material 3, professional |
| Architecture | Follow current (no ViewModels, no Hilt) |
| Accessibility | WCAG AAA + manual dark toggle in Settings |
| Motion | Subtle pro — 150ms fades, ripple, haptics, respects `prefers-reduced-motion` |
| Tests | Compose UI tests per screen (Spec 2) |
| Split | Foundation (this spec) → screen polish (Spec 2 = F13) |

---

## Design System

### Theme & Color (`ui/theme/OpenFlowColors.kt` + update `Theme.kt`)

**Palette:**
- Primary: `#1565C0` (calm blue, WCAG AAA ≥7:1 on white)
- Secondary: `#546E7A` (blue-grey)
- Surface light: `#FAFAFA`, dark: `#121212`
- Error: `#B00020`
- Semantic: `onBubbleOn`, `onBubbleOff` for chip states

**Dark mode:** Toggle in Settings (`prefs.darkMode: "system" | "light" | "dark"`). `OpenFlowTheme` reads via StateFlow.

**Typography:** Material 3 default scale, `bodySmall` min 12sp.

**Shape:** `RoundedCornerShape(12.dp)` cards, `8.dp` buttons.

### Reusable Components (`ui/components/`)

| Component | States | Notes |
|---|---|---|
| `OpenCard` | default, selected, disabled | Shape + elevation tokens |
| `OpenChip` | on, off, loading | Bubble/Mic status chips |
| `OpenTextField` | default, error, disabled | Supporting text, label animation |
| `OpenButton` | filled, outlined, text | Ripple + optional haptics |
| `OpenListItem` | default, with actions | History rows, dict words |
| `EmptyState` | icon + title + subtitle | Empty screens |
| `LoadingState` | indeterminate | Async operations |
| `ErrorState` | icon + message + retry | DB errors |

All: `contentDescription` for TalkBack, touch targets ≥ 48dp, text scale up to 200%.

### Motion (`ui/theme/Motion.kt`)

- Tab switch: `AnimatedContent` fade 150ms
- Card list: `animateItemPlacement()`
- Chip color: `animateColorAsState()` 200ms
- `prefers-reduced-motion`: check `configuration.isScreenWideColorGamut` before animating
- Haptics: `HapticFeedbackConstants.CONFIRM` on copy, `REJECT` on delete

### Accessibility Primitives (`ui/a11y/`)

- `OpenIcons.kt`: semantic icons with `contentDescription`
- `Dimen.kt`: `TOUCH_TARGET = 48.dp`
- `OpenStrings.kt`: all strings extracted to `strings.xml` with IDs

---

## Files

```
ui/theme/OpenFlowColors.kt      # new
ui/theme/Motion.kt              # new
ui/theme/Theme.kt               # update (dark mode, motion)
ui/components/OpenCard.kt      # new
ui/components/OpenChip.kt       # new
ui/components/OpenTextField.kt  # new
ui/components/OpenButton.kt     # new
ui/components/OpenListItem.kt   # new
ui/components/EmptyState.kt    # new
ui/components/LoadingState.kt  # new
ui/components/ErrorState.kt     # new
ui/a11y/OpenIcons.kt           # new
ui/a11y/Dimen.kt              # new
ui/a11y/OpenStrings.kt         # new
MainActivity.kt                # update (components, dark toggle)
res/values/strings.xml         # update (extract strings)
res/values/themes.xml          # update
FlowPrefs.kt                   # update (darkMode pref)
```

---

## Security

No new permissions. No INTERNET. No behavior change to bubble/STT.

---

## How Mitun tests

1. Manual dark/light/system toggle in Settings — verify colors correct
2. TalkBack walk all 5 tabs — all elements readable
3. Text scaling to 200% — no overflow/clipping
4. Bubble/mic chips animate correctly, reduced-motion disables them
5. Copy-to-clipboard (history) produces haptic
6. `assembleDebug` + install

---

## Next

Spec 2 (F13): Apply components to all 5 screens (Home, Dict, Snippets, Style, Settings).
