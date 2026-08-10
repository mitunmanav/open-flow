# Open Flow — UI redesign design (Drop 1 + roadmap Drop 2)

**Date:** 2026-08-10  
**Status:** Approved by Mitun (chat) — pending final read of this file  
**Repo:** `/home/mitun/open-flow`  
**Product lock:** Flow Bubble + AccessibilityService (NOT IME); local STT; MIT; no INTERNET default  

---

## 1. Goal

Redesign the **in-app UI** to a calm-pro, drawer-first hub with room for full customisability, and **fix bubble live transcript** so spoken text shows on the floating bubble while dictating.

Product behavior (a11y insert, continuous STT, dictionary/snippets, etc.) stays. This is **IA + visual + prefs surface**, not a new product.

---

## 2. Decisions (locked)

| Topic | Choice |
|-------|--------|
| Feel | **Calm pro** — soft surfaces, clean blue, quiet trust |
| Scope | **Full IA change** + path to **full customisability** |
| Nav | **Side drawer + Home hub** (no bottom nav as primary) |
| Customize UX | **Settings with sub-pages** (not one mega screen) |
| Ship | **Two drops** (this spec’s implementable slice = Drop 1) |
| Stack | **Compose shell + FlowPrefs** (no Navigation-Compose unless Drop 2 forces it) |
| Bubble product | Unchanged: `TYPE_ACCESSIBILITY_OVERLAY` from AccessibilityService |

---

## 3. Drop 1 — in scope

### 3.1 App shell

- Top app bar: menu (open drawer) + title + optional status chip  
- **Modal / permanent-friendly navigation drawer** listing destinations  
- Content area hosts one destination at a time (state in Activity or small shell state holder — no Hilt)

**Drawer items (Drop 1 defaults — all on):**

| Item | Content |
|------|---------|
| Home | Hub (below) |
| History | Full dictation list (Home may show only recent) |
| Dictionary | Existing dictionary UI, restyled |
| Snippets | Existing snippets UI, restyled |
| Style | Existing style presets, restyled |
| Settings | Settings hub → sub-pages |

Home is always available. Drawer replaces bottom `NavigationBar`.

### 3.2 Home hub

Ordered sections:

1. **Setup** — Bubble ON / Mic ON chips + enable a11y + grant mic actions  
2. **Stats** — words · sessions · streak  
3. **Recent history** — last N items with copy/delete  
4. **Test field** — local dictation practice  

Use restyled `OpenCard` / `OpenChip` / `OpenListItem` / `EmptyState`.

### 3.3 Appearance (Settings → Appearance)

| Pref | Values | Default |
|------|--------|---------|
| `darkMode` | system / light / dark | system (existing) |
| `density` (optional if cheap) | comfortable / compact | comfortable |

Accent color customization can wait for Drop 2 if timeboxed; primary remains calm blue `#1565C0`.

### 3.4 Visual system (calm pro)

| Token | Light | Dark |
|-------|-------|------|
| Primary | `#1565C0` | lighter blue for contrast |
| Surface | `#FAFAFA` | `#121212` |
| Background | `#FFFFFF` | `#1E1E1E` |
| Cards | 12dp corners, low elevation | same |
| Type | Material 3 scale; body ≥ 12sp | same |
| Motion | 150ms fades; respect reduced motion where we already gate | |

Reuse / restyle existing components under `ui/components/` and `ui/theme/`. Prefer split files over one 500+ line `MainActivity`.

### 3.5 P0 — Bubble live transcript (bug fix, same drop)

**User report:** recorded text does not show on the floating bubble.

**Root cause (code):**

- `onFinal` sets label to `"Listening Xs · stop"` — **does not show final phrase**  
- `onPartial` truncates to **22** characters  
- Layout: `maxLines=1`, narrow `maxWidth`  
- `onReady` can overwrite partials with timer-only text  

**Required behavior while listening:**

1. Show **live partial** text on the bubble (prefer full phrase; wrap up to ~3 lines; widen bubble)  
2. On **final** chunk: show that phrase briefly (or “✓ phrase”), append to session as today, keep continuous listen  
3. Idle: calm “Tap to talk” (or mode-appropriate idle)  
4. **Dot mode:** while listening, expand to mini-pill that still shows text (not icon-only during speech)  
5. Errors / need-mic: unchanged clear short labels  

**Files likely touched:**

- `app/.../bubble/FlowAccessibilityService.kt` (listener label updates, session display)  
- `app/.../res/layout/flow_bubble.xml` (maxLines, maxWidth, padding)  
- Optional pure helper for preview string (unit-testable truncate rules if we keep a soft max for very long lines)

**Not in this fix:** changing insert pipeline, STT engine restart policy (unless label race requires main-thread guard).

### 3.6 Settings placeholders for Drop 2

Settings hub lists:

- Appearance — **live**  
- Home modules — “Coming” or disabled row  
- Bubble — “Coming” or move existing bubble sliders here if cheap (size/opacity/mode already in Settings today may stay until Drop 2 restructure)  
- Nav visibility — “Coming”  

If moving bubble sliders into Settings → Bubble in Drop 1 is small and reduces orphan UI, prefer that over leaving them only on a flat Settings page.

---

## 4. Drop 2 — out of Drop 1 implementation, designed later

| Settings page | Knobs |
|---------------|--------|
| **Home** | Toggle + reorder modules: setup, stats, recent, test field |
| **Nav** | Which drawer items visible (Home always on) |
| **Bubble** | full/compact/dot, size, opacity, snooze, language, pulse on/off |

Full customisability = Appearance + Home modules + Bubble knobs + Nav visibility. Defaults match current feature surface.

---

## 5. Out of scope (both drops unless Mitun expands)

- Memo recorder (F16), export (F15), Whisper/sync  
- Re-adding Navigation-Compose / Hilt / dual Session stack / Robolectric  
- Redesigning as IME  
- INTERNET permission  
- Visual companion mockups (skipped by Mitun “proceed”)

---

## 6. Architecture

```
MainActivity
  └─ OpenFlowTheme(darkMode)
       └─ AppShell(
            drawerItems,
            currentRoute,
            onNavigate,
            content = { when(route) Home / History / … / Settings }
          )

ui/
  shell/AppShell.kt
  home/HomeScreen.kt
  history/HistoryScreen.kt
  dictionary/DictionaryScreen.kt
  snippets/SnippetsScreen.kt
  style/StyleScreen.kt
  settings/SettingsHub.kt
  settings/AppearanceSettings.kt
  components/*   (existing, restyle)
  theme/*        (existing, refine)
```

**State:** `var route` + `FlowPrefs` StateFlows. No new DI framework.

**Data:** existing `OpenFlowApp.dictations` / `prefs` only.

---

## 7. Security / privacy

- No new permissions for redesign  
- Drawer/settings are local UI only  
- Bubble still skips password fields (`FieldPolicy`) and bank packages (`PackagePolicy`)  
- Accessibility still insert-only  

---

## 8. Testing

| Layer | What |
|-------|------|
| Unit | Prefs normalize helpers; optional bubble preview truncate helper |
| Unit | Existing FieldPolicy / ContinuousPolicy / TextPost / PackagePolicy / Shake stay green |
| Manual | Drawer open/close; each destination; theme toggle; bubble shows partial + final words while speaking; insert still works |

Proof before claim done:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --offline
# APK to Desktop
```

---

## 9. Success criteria (Drop 1)

1. Bottom nav gone; drawer + Home hub in use  
2. Calm pro look consistent on all destinations  
3. Appearance theme works (system/light/dark)  
4. **Bubble shows spoken text (partial + final) while listening**  
5. Dictation insert + history still work  
6. Unit tests + debug APK green  
7. Docs: HANDOFF + BASELINE note Drop 1 UI redesign  

---

## 10. Implementation order (for writing-plans next)

1. Bubble transcript P0 (fix user-visible dictation feedback)  
2. Theme token pass  
3. AppShell + drawer routing; extract screens from MainActivity  
4. Home hub layout  
5. Settings hub + Appearance  
6. Polish empty states / strings  
7. Verify + Desktop APK  

---

## 11. Spec self-review

| Check | Result |
|-------|--------|
| Placeholders | None intentional (Drop 2 marked out of Drop 1) |
| Contradictions | Bottom nav removed vs Drop 2 nav visibility — Drop 2 only hides drawer items, does not restore bottom nav as primary |
| Scope | Single drop implementable; Drop 2 separate plan later |
| Ambiguity | Bubble must show words — explicit; truncate only for extreme length with wrap first |

---

## 12. Approval trail

- Feel A, IA C + full customisability C, nav B, customize B, ship B  
- Approach: Compose shell + prefs  
- Design sections 1–3 approved YES (2026-08-10)  
- Bubble transcript bug called out and included in Drop 1  

**Next after Mitun reviews this file:** invoke **writing-plans** → `docs/superpowers/plans/2026-08-10-ui-redesign-drop1.md` → implement.
