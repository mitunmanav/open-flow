# Design: Research drop + dual skins (M3 · subtle brutal)

**Date:** 2026-08-10  
**Status:** LOCKED (IA + skins + feature set)  
**Product:** Open Flow (`/home/mitun/open-flow`)  
**Out of scope:** F16 memo recorder · cloud STT · account · Command Mode AI

---

## 1. Information architecture (no duplicates)

| Zone | Items | Rule |
|------|--------|------|
| **Bottom bar** | Home · Dictionary · Snippets · **Style** | Primary destinations only (≤4) |
| **Home body** | Setup (if needed) · KEY ACTIONS chips · stats · recent | Actions, not twin nav links |
| **Navbar (drawer)** | **Settings** · History · Customize | Extras only — never Home/Dict/Snips/Style |
| **Overlay** | Flow Bubble | Not in Compose tree; a11y View |

**Never duplicate:** bottom tabs or Home chips into drawer.

### Home KEY ACTIONS (chips)
- Cleanup level (None / Light / Medium / High)
- Language tag
- Copy last
- Export (share sheet entry)
- Test field (module, optional)

### Settings hub (from drawer)
Appearance (includes **skin: M3 | Subtle brutal**) · Bubble · Cleanup · Privacy/retention · Sounds & haptics · Language packs · Home modules · Menu visibility (drawer extras only)

### Customize (drawer)
Home module order/visibility · Drawer item visibility (Settings always; History/Customize optional)

---

## 2. Two visual versions (two worktrees)

| Version | Branch / worktree | Skin |
|---------|-------------------|------|
| **A · M3** | `feat/product-m3` · `.worktrees/product-m3` | Material 3 soft (tonal, purple seed, rounded) |
| **B · Subtle brutal** | `feat/product-brutal` · `.worktrees/product-brutal` | Cream · charcoal · ink accent · 2–3dp hard borders · offset shadow 0 blur |

Same IA and same feature code goals. Theme tokens + shape differ. Prefer shared logic; theme packages diverge.

Android refs:
- [M3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)
- [Navigation drawer](https://developer.android.com/develop/ui/compose/components/drawer)
- [Settings pattern](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- [Accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility) — 48dp · contrast

---

## 3. Bubble product lock

- Control chrome only: **no live speech text** (default off; optional later pref)
- Shapes: circle (default) · pill · square · dot
- Listen: waveform / pulse (RMS if available)
- Edge snap · haptics · optional sounds
- Shrink in search-like fields
- Copy affordance after stop (optional chip / toast action)
- Insert still: polish once → field (session model)

---

## 4. Features from deep research (1–15, no 16)

| # | Feature | Primary files (guide) |
|---|---------|------------------------|
| 1 | Bubble chrome rewrite | `FlowAccessibilityService`, bubble layout/drawable, prefs |
| 2 | Edge snap + haptics | service + prefs |
| 3 | Real waveform | STT RMS / level → bubble view |
| 4 | Copy after stop | service + clipboard |
| 5 | Shrink in search fields | `FieldPolicy` |
| 6 | Cleanup levels | `TextPostProcessor` + prefs + UI |
| 7 | Style by app type | package map + style prefs |
| 8 | Export/share history | exporter + History UI |
| 9 | History search + flag | Room + History UI |
| 10 | Raw + polished + undo | Room cols + UI |
| 11 | Star dictionary + sort | Room + Dictionary UI |
| 12 | Retention policy | prefs + purge job |
| 13 | Service notifications | optional notif channel |
| 14 | Lang pack hints | Settings / Home chip |
| 15 | Sounds toggle | prefs + service |

---

## 5. Worktree map (parallel-safe)

| Worktree | Branch | Owns | Depends |
|----------|--------|------|---------|
| `product-m3` | `feat/product-m3` | Full product default skin M3 | main |
| `product-brutal` | `feat/product-brutal` | Full product default skin brutal | main (or merge m3 base later) |

**Internal feature slices inside each product branch (ordered commits):**

1. `nav-ia` — bottom 4 + drawer extras only + Home keys  
2. `theme` — skin tokens  
3. `bubble-chrome` — 1–5  
4. `speech-data` — 6–7, 11  
5. `history-privacy` — 8–10, 12  
6. `system` — 13–15  

Agents must not edit the same file in two live worktrees without merge plan. Prefer complete slice on `product-m3` then port theme-only delta to `product-brutal`, **or** implement both themes in each worktree with default skin flipped.

**Recommended implementation strategy (less thrash):**  
- Build all features once on `feat/product-m3` with **both** skins selectable (default M3).  
- `feat/product-brutal` flips default to subtle brutal + any brand copy deltas.  
- Merge m3 → main, then brutal → main (or ship two APK flavors later).

---

## 6. Testing / done

- Unit tests for pure logic (cleanup levels, FieldPolicy search, retention, CourseCorrector)  
- `:app:testDebugUnitTest` + `:app:assembleDebug` green per worktree  
- Manual: drawer has no Dict/Style; bottom has Style; bubble shows no transcript  

---

## 7. Authorship

Commits: **Mitun only**. No Co-Authored-By.
