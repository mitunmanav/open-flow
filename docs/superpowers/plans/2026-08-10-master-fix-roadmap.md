# Open Flow — Master Fix + Feature Roadmap

> **For agentic workers:** Superpowers TDD + worktree per feature + max 5 sub-agents, no shared files.  
> **Product:** Wispr Android bubble (not IME) + local memory. MIT. No INTERNET default.

**Goal:** Ordered plan to fix real bugs, speed, UI, then ship next features.

**Architecture:** Bubble + Accessibility insert; SttEngine continuous restart; Room memory.

**Tech Stack:** Kotlin, Compose, SpeechRecognizer, AccessibilityService, Room

---

## Where we are (done)

| ID | Status |
|----|--------|
| F0 bootstrap | done |
| F1 scaffold | done |
| F10 Flow Bubble (not IME) | done |
| F11 continuous restart + UI chips | done |
| F12-rel reliability | done (`5584152`) |
| F13 Wispr parity core | done (`eafe209`) |
| ponytail dual-stack cut | done |
| F12-ux UI foundation | done (`37a4a1f`) |

**Canonical history:** `docs/BASELINE.md`  
**Working today:** install APK → a11y + mic → bubble → talk → text in fields.

---

## Known bugs / gaps (priority)

### P0 — Reliability (user feels “broken”)

| # | Issue | Fix |
|---|--------|-----|
| P0.1 | STT fails if mic not granted when bubble taps | Check RECORD_AUDIO; bubble label “Allow mic”; deep-link settings |
| P0.2 | Stale focus node → insert fails in other apps | Re-find focused editable right before ACTION_SET_TEXT |
| P0.3 | ERROR_RECOGNIZER_BUSY / double start | cancel() before restart; serialize starts |
| P0.4 | Restart beeps (OEM) | Soft mute STREAM_MUSIC during startListening when possible |
| P0.5 | Partial never hits field (only finals) | Keep finals only for insert (correct); show partial on bubble only |

### P1 — Speed / feel

| # | Issue | Fix |
|---|--------|-----|
| P1.1 | Gap between sessions | Keep ~50–80ms restart; recreate recognizer only when needed |
| P1.2 | UI refresh of bubble status | Handler tick every 1s while listening |
| P1.3 | MainActivity lifecycle deprecation | Use `androidx.lifecycle.compose.LocalLifecycleOwner` |

### P2 — Product gaps

| # | Feature | Notes |
|---|---------|--------|
| P2.1 | Memo recorder (F12) | Audio + live STT save to Room (mic exclusive design) |
| P2.2 | Export from UI | Share .txt/.md of session |
| P2.3 | Language picker | Locale for STT |
| P2.4 | Dictation history | Optional save each bubble session to timeline |

### P3 — Later

Whisper opt-in, sync, Wear, widgets, diarization.

---

## Execution order (one worktree each)

1. **feat/12-reliability** — P0.1–P0.4 + P1.1–P1.3  
2. **feat/13-recorder** — P2.1  
3. **feat/14-export-ui** — P2.2  
4. **feat/15-language** — P2.3  
5. **feat/16-dictation-history** — P2.4  

After each: unit tests + assembleDebug + Desktop APK copy + commit + merge main.

---

## Task 1: Reliability (this sprint)

**Files (single worktree, no parallel edit same file):**
- `app/.../stt/SttEngine.kt`
- `app/.../bubble/FlowAccessibilityService.kt`
- `app/.../bubble/FieldPolicy.kt` + tests if needed
- `app/.../ui/MainActivity.kt`
- `app/.../res/values/strings.xml`

**Done when:**
- Bubble shows clear mic error
- Insert works more often (fresh focus)
- Fewer busy errors
- APK on Desktop
- Commit `fix: dictation reliability…`

---

## Security

No INTERNET. A11y insert-only. Skip password. No new dangerous perms.

---

## How Mitun tests after reliability

1. Install new Desktop APK  
2. Deny mic → tap bubble → must say allow mic  
3. Grant mic → long talk with pauses → text keeps coming  
4. WhatsApp/Notes field → insert works  
5. Password field → no insert / no force  
