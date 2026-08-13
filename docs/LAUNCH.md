# Open Flow — launch status (2026-08-13)

**Not new features.** Truth of what we have, what to polish, what is missing for a honest launch.

**Phone now:** `0.1.1-debug` on A059 (`00161352H004300`). F22 Wispr-feel is a *separate* worktree and stays running.

Product lock: Bubble + a11y insert. Not IME. en-US. Local FOSS. No APK INTERNET.

---

## UX map (how it works today)

```
Cold start
  → MainActivity
  → Home (F21) / Setup wizard (F22, not on main yet)
  → Enable Accessibility (system settings)
  → Allow mic
  → Optional battery settings
  → Ready

Any app text field (not password / phone / bank)
  → Focus + IME
  → Flow Bubble (TYPE_ACCESSIBILITY_OVERLAY)
  → Tap = start  |  tap again = insert  |  hold = PTT
  → Cancel = drop  |  Done = insert
  → Drag = move  |  drag to bottom = snooze 10m  |  shake = unsnooze
  → STT (on-device prefer) → PhraseMap / commands → Cleanup → Style → dict/snippets
  → ACTION_SET_TEXT (clipboard fallback)
  → Room history (unless never_store)
```

Bottom tabs: Home · History · Dict · Settings.

---

## Launch gate

| Gate | Status | Note |
|------|--------|------|
| Installs, launches | **have** | 0.1.1 on device |
| A11y + mic path | **have** | Home chips; F22 makes it linear |
| Bubble insert | **have** | F21 tap-again |
| No INTERNET in APK | **have** | Manifest omit |
| Backup off | **have** | `allowBackup=false` + extraction exclude |
| Cleartext blocked | **have** | NSC |
| Unit tests | **have** | 210 on F21 main |
| First-run wizard | **F22** | Not on main yet |
| Search-field shrink | **F22** | In flight |
| History by day | **F22** | In flight |
| Device QA matrix | **missing** | Need Mitun pass on A059 |
| Release signing / F-Droid | **partial** | debug-signed localRelease |
| Room migrations | **risk** | `fallbackToDestructiveMigration` wipes history |
| Encrypted at rest | **gap** | SECURITY.md wants Keystore; Room/prefs plaintext in app sandbox |
| REQUEST_IGNORE_BATTERY | **skip** | New dangerous perm. Keep settings intent only |

**Launch = honest 0.1.x sideload**, not Play store / 100-lang / cloud clone.

---

## What to polish (existing only)

1. First-run order (F22) — Wispr: a11y → mic → skippable battery
2. Home repair / snooze banners (F22)
3. History day groups (F22)
4. Search-field bubble shrink (F22)
5. Motion: use `Motion.TAB_SWITCH_MS` (F22)
6. Room: stop silent wipe on version bump (F23 tests + later real migrations)
7. Battery copy: OEM steps on the existing battery chip (no new perm)
8. Device pass: tap / hold / cancel / field skip / snooze / theme

## What is missing (do **not** build this slice)

Account · overlay perm · Command Mode · Notetaker · extra languages · F19 bias · tiny model · EncryptedFile (until we store audio) · Play listing · Maestro CI

## Features working (main 0.1.1)

See `docs/FEATURES.md`. Core dictation path works. Weak: auto-punct (rules), High cleanup ≠ Wispr AI, export = share.

---

## Security (frontend + “backend”)

There is **no server**. Backend = on-device Room + STT process.

| Check | Now |
|-------|-----|
| INTERNET | Not declared |
| Cleartext | Blocked |
| Backup / transfer | Excluded |
| Secrets in repo | None expected (`local.properties` gitignored) |
| A11y service | `BIND_ACCESSIBILITY_SERVICE`; events limited to focus/window |
| A11y `exported=true` | Required for system bind; permission guards it |
| Mic | Runtime only |
| Password / bank skip | FieldPolicy + PackagePolicy |
| Clipboard | Fallback insert + copy chip (user action) |
| Logs of transcripts | Must stay off in release |
| Room wipe | **Launch risk** |

Do not add INTERNET “for launch.” System STT may still leave the device — Privacy report already says that.

---

## Storage

- Room `openflow.db` v4, app-private
- Prefs: cleanup, style, bubble, retention
- Retention: keep / wipe_24h (save + F21 launch purge) / never_store
- No audio files yet
- **Polish:** write real Room migrations before next schema bump

## Battery

- Home “Battery settings” → ignore-battery screen
- F20 keep-screen-on while listening
- Wispr: OEM Unrestricted / Never sleeping (Nothing A059 = treat like Pixel + OEM extras)
- **Do not** add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` this slice

## UI

- Cream/charcoal. **No redesign**
- Known leftover feel: Home module soup (F22 banners help), nav was fixed F16
- F22 wizard = launch UX

---

## AI-agent Android mistakes (web 2025–2026)

Do **not** repeat:

| Mistake | Source | Our lock |
|---------|--------|----------|
| One-shot whole app; skip validation / a11y | DEV 2026 | Feature slices + TDD |
| Guess APIs / old Gradle | Medium AI-Android | android-cli docs |
| Add INTERNET “just in case” | Common agent habit | Manifest omit |
| Hardcode secrets | Cybernews 2026 AI apps | No keys |
| `fallbackToDestructiveMigration` as forever | Android Room docs | Document + stop next bump |
| Restyle every turn (purple / Material soup) | DEV “AI not great at design” | No redesign |
| Treat a11y as scrape-all | Android a11y caution | Insert + focus only |
| Overlay + IME as product | Wispr Android is bubble | Not IME |
| Claim PASS without `gradlew` | Our gate | Proof required |
| Wipe user data / wrong tree | Adversa 2026 agent incidents | Tree jail |

---

## Team (this worktree)

Max 5. Different files. No F22 paths (`MainActivity`, `FieldPolicy`, `FlowAccessibilityService`, setup/home policies F22 owns).

1. `docs/LAUNCH.md` (this file — keep true)
2. Security regression tests (manifest / NSC / backup)
3. Storage: Room wipe risk test + note
4. Battery copy policy (OEM text, no new perm)
5. Pickup: `docs/HANDOFF.md` launch section only if that file is free

## Mitun launch check (phone)

1. A11y on → mic on → field → tap → speak → tap again
2. Cancel drops
3. Hold-to-talk
4. Password field: no insert
5. Battery chip opens settings
6. Dark/light readable
7. Force-stop → a11y off (honest) → repair
