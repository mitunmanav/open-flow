# Open Flow — Baseline + full feature history

**Saved:** 2026-08-10  
**Repo:** `/home/mitun/open-flow`  
**Branch:** `main`  
**Git tip when written:** `F14-merged` (see git log)  
**Sources:** prior Grok sessions (`019feb59`, `019febaa`, `019febd3`), `AGENTS.md`, plans, git log, live code.

**Rule:** This file is the durable map of *what was decided* and *what shipped*.  
Pickup for daily work still starts at `docs/HANDOFF.md`. Product law still at `AGENTS.md`.

---

## 1. Product lock (do not re-open)

| Item | Locked value |
|------|----------------|
| Name / path | **open-flow** (`/home/mitun/open-flow`) |
| License | MIT FOSS |
| Jobs | (1) **Wispr-style** dictation (2) **NeoSapien-style** local memory |
| Dictation UX | **Floating Flow Bubble + AccessibilityService** — **NOT a keyboard/IME** |
| Keyboard | User keeps Gboard / etc. |
| STT vs TTS | **STT only** (no TTS/read-back) |
| Network | **No INTERNET** in base app; online opt-in later only |
| Account / ads / analytics | **Never** required / never |
| STT engine (default) | Android `SpeechRecognizer`, prefer on-device |
| Platform | Android only (for now) |
| Build process | Superpowers: plan → worktree → TDD → security → commit |
| Sub-agents | Max **5**; never edit same file in parallel |
| MVP philosophy | Full feature surface ordered by phases — not permanent thin cut |

**Rejected path:** Voice-as-IME / custom keyboard as product. Old IME scaffold was wrong UX; user confirmed bubble works on device.

**One-liner:** Speak to type anywhere (bubble) + private on-device dictation history / later memos.

---

## 2. Architecture (live code)

```
Bubble (WindowManager overlay from a11y)
    → SttEngine (SpeechRecognizer + continuous restart)
    → TextPostProcessor (filler / punct / lists / style)
    → dict + snippets apply
    → Accessibility ACTION_SET_TEXT into focused field
    → DictationRepository save (on stop)

MainActivity (Compose)
    Home · Dictionary · Snippets · Style · Settings
    Dark mode via FlowPrefs + OpenFlowTheme
```

| Layer | Key files |
|-------|-----------|
| App | `OpenFlowApp.kt` |
| Bubble / a11y | `bubble/FlowAccessibilityService.kt`, `FieldPolicy.kt` |
| STT | `stt/SttEngine.kt`, `ContinuousPolicy.kt` |
| Text | `text/TextPostProcessor.kt` |
| Data | `data/DictationDao|Entities|Repository`, `OpenFlowDatabase` |
| Prefs | `prefs/FlowPrefs.kt` (+ `PrefsStore` / `MemoryPrefsStore` for tests) |
| Privacy | `privacy/PrivacyDefaults.kt` |
| UI | `ui/MainActivity.kt`, `ui/components/*`, `ui/theme/*`, `ui/a11y/*` |

**Deleted on purpose (ponytail — do not resurrect without real wire):**  
`Session*` dual stack, `TranscriptSearch`, `TranscriptExporter`, empty `RecordingService`, `SttConfig`, `FocusResolver` extract, nav-compose / viewmodel-compose / security-crypto deps, Robolectric for simple prefs tests.

**Manifest:** only `RECORD_AUDIO` + AccessibilityService. No FGS until real recorder.

---

## 3. Feature ID timeline (what agents actually shipped)

IDs drifted across chats. **Canonical order below** is what git + sessions support.

| ID | Name | Commit(s) | Status | Notes |
|----|------|-----------|--------|-------|
| F0 | Bootstrap process / license / security | `dcb65e1` | **done** | AGENTS, PROCESS, SECURITY |
| F1 | Android Compose scaffold + security defaults | `54227af` | **done** | Room shell, no INTERNET |
| F10 | Flow Bubble (not IME) | `021b066` | **done** | Overlay + a11y insert; user validated on phone |
| F11 | Continuous dictation + light UI | `a51b3e7` | **done** | Auto-restart on timeout/no-match/busy; chips |
| F12-rel | Dictation reliability | `5584152` | **done** | Mic gate, re-focus, serialize STT, soft-mute beeps |
| map | Wispr A–Z research map | `8d38c57` | **done** | `docs/FEATURES.md` |
| F13 | Wispr parity core (local) | `eafe209` | **done** | See §4 |
| cut | Ponytail dead dual-stack | `0867d3d` → merge `e5d0137` | **done** | Dictations-only store |
| F12-ux | UI foundation | `37a4a1f` | **done** | Theme, Open* components, dark mode |
| F14 | Polish bank/modes/shake/pulse | see git | **done** | PackagePolicy, ShakeDetector, bubbleMode |
| docs | HANDOFF truth | `c13c195` | **done** | Single main tip |

### ID collision note (agents messed this up)

| Label used | Actually means |
|------------|----------------|
| “F12 reliability” | `5584152` reliability fix |
| “F12 UX foundation” | UI kit `37a4a1f` (plans under `12-ux-foundation*`) |
| Early “F7 recorder” | Still **not built** — now tracked as **F16** |
| Early “F6 IME polish” | **Dropped** (not IME product) |

**Going forward use only:** F14 polish · F15 export · F16 recorder · F17 lang polish · later Whisper/sync.

---

## 4. Shipped product surface (testable)

### Dictation / bubble

- [x] Floating bubble overlay from AccessibilityService  
- [x] Tap toggle listen / stop  
- [x] Long-press push-to-talk  
- [x] Drag reposition  
- [x] Drag-to-bottom snooze ~10 min + end snooze in Settings  
- [x] Bubble size 0.7–1.15× + opacity 20–100%  
- [x] Continuous listen: restart on OS silence / timeout / busy  
- [x] Recreate `SpeechRecognizer` every N sessions (stuck fix)  
- [x] Partial text on bubble; finals insert to field  
- [x] Skip password / sensitive fields (`FieldPolicy`)  
- [x] Re-resolve focus before insert  
- [x] Mic denied → clear “Allow mic in app”  
- [x] Soft-mute music stream on STT start (OEM beep soften)  
- [x] Language tag pref for STT (`en-US`, `hi-IN`, …)  
- [x] Copy last transcript chunk  

### Text post-process (local)

- [x] Filler strip (um/uh/like style)  
- [x] Light auto punctuation / caps / questions  
- [x] Numbered list light formatting  
- [x] Style presets (casual etc.) via `TextPostProcessor`  
- [x] Dictionary replace on insert  
- [x] Snippet expand (whole-utterance trigger match)  

### In-app UI

- [x] Bottom nav: Home · Dictionary · Snippets · Style · Settings  
- [x] Setup: enable a11y + grant mic chips  
- [x] Home history list + delete + stats (words / sessions / streak)  
- [x] Test field for in-app dictation check  
- [x] Privacy copy (local-first)  
- [x] Dark mode: system / light / dark  
- [x] OpenFlow theme + shared components (`OpenButton`, `OpenCard`, `OpenChip`, …)  

### Data / privacy

- [x] Room **dictations** store (single path)  
- [x] Dictionary + snippets tables  
- [x] Stats counters  
- [x] No INTERNET permission  
- [x] `allowBackup=false`, cleartext blocked  

### Explicitly not shipped

- [ ] Real memo **recorder** (audio file + FGS)  
- [ ] Export/share UI (.txt/.md)  
- [ ] Bank-app package denylist  
- [ ] Bubble shrink-to-dot / idle modes  
- [ ] Shake to unsnooze  
- [ ] Waveform polish  
- [ ] Multi-language pack install UX  
- [ ] Whisper / local LLM opt-in  
- [ ] Cloud sync  
- [ ] Wear / tiles / call record  
- [ ] Account / billing (never)  

---

## 5. Original full catalog (from first Grok plan — ordered later)

Early session locked a large catalog (phases A–I). **Still valid as backlog**, not as “already built.”

| Bucket | Intent | Status |
|--------|--------|--------|
| A STT / insert | Dictation into any app | **Bubble path done** (not IME) |
| B recorder / memory | Audio + transcript memos | **Not built** (F16) |
| C search / second brain | FTS recall | Deferred (dual FTS cut; rebuild when needed) |
| D on-device AI | Whisper / tiny LLM | Later opt-in |
| E export | Share files | F15 |
| F privacy defaults | No net, no account | **Defaults done** |
| G opt-in online | WebDAV / user-key STT | Later |
| H UI / system | Settings, theme, a11y cards | **Core + F12-ux done** |
| I never-list | No forced cloud/account/ads | **Law** |

Mic exclusive trap (session research): cannot reliably run `MediaRecorder` + `SpeechRecognizer` same time → recorder design must pick one pipeline.

---

## 6. Wispr Android parity (summary)

Full A–Z table: `docs/FEATURES.md` (refresh dates with this baseline).

**Have today (local FOSS):** a11y insert, continuous dictate, bubble drag/size/opacity/snooze/PTT, dictionary, snippets, styles, history, stats, copy, offline STT, sensitive field skip, dark mode UI.

**Next parity:** shrink modes, bank hide, shake, waveform, export, recorder depth.

**Never clone:** Wispr account / cloud STT / billing.

---

## 7. Baseline proof (this save)

| Check | Result |
|-------|--------|
| Branch | `main` only (no leftover feature worktrees) |
| Unit tests | **25** (`FieldPolicy` 6 + `Continuous` 8 + `TextPost` 5 + `DarkMode` 5 + `Privacy` 1) |
| Build | `:app:testDebugUnitTest` + `:app:assembleDebug` **PASS** offline (~8s) |
| APK | `dist/open-flow-debug.apk` (gitignored body; rebuild anytime) |
| Dead dual-stack | **Gone** |
| Robolectric | **Not** in deps (avoids huge jar download) |

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$PATH"
cd /home/mitun/open-flow
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest :app:assembleDebug --offline
```

**Phone path:** mic → Accessibility ON Open Flow → focus field → bubble → speak → stop → history.

---

## 8. NEXT (ordered — one worktree each)

| ID | Work |
|----|------|
| **F14** | Polish: bubble shrink modes, bank-app denylist, shake unsnooze, better waveform |
| **F15** | Export/share history (.txt/.md) — rebuild exporter when wiring UI |
| **F16** | Memo recorder (audio + transcript) — FGS + real service only when used |
| **F17** | Multi-language UI / offline pack hints |
| later | Whisper opt-in, opt-in sync, Wear/tiles |

---

## 9. Agent thrash lessons (saved so we don’t repeat)

1. **One truth branch.** Do not leave ponytail cut unmerged while starting UX on fat main.  
2. **HANDOFF must match `git log -1`.**  
3. **No Robolectric** for prefs unless Mitun OK — downloads huge jars, looks like mystery install.  
4. **Do not re-add** empty Session*/export/recorder shells “for later.”  
5. **Feature IDs:** write them in this file when shipping; stop reusing F12 for two things.  
6. Max 5 sub-agents; never two writers on one file.  
7. Interrupt rule: side request OK, do not drop primary stream unless “stop.”  

---

## 10. Competitive / strategy (session research — short)

- Wispr = cloud + account; we = local FOSS wedge.  
- FOSS peers: FUTO / Whisper keyboards, Notely Voice memos — gap is **combo** bubble + memory + trust.  
- Moat: habit + personal history + trust — not secret code.  
- FAIL = “another Whisper keyboard.” PASS = daily bubble + local memory stickiness.

---

## 11. Related docs

| File | Role |
|------|------|
| `AGENTS.md` | Law for every agent |
| `docs/HANDOFF.md` | Live pickup (WHERE / NEXT) |
| `docs/FEATURES.md` | Wispr A–Z matrix |
| `docs/PROCESS.md` | Process |
| `SECURITY.md` | Security defaults |
| `docs/superpowers/plans/*` | Per-feature plans |
| **This file** | Full baseline + history |

---

## Paste for new chat

```
Open Flow baseline: /home/mitun/open-flow docs/BASELINE.md + HANDOFF.md + AGENTS.md.
Product: Flow Bubble + a11y (NOT IME), local STT, MIT, no INTERNET default.
main tip at save: c13c195. Tests+APK green offline. Dictations-only (ponytail cut).
NEXT: F14 polish or F16 recorder — ask Mitun.
Do not resurrect Session dual-stack, empty RecordingService, or Robolectric for prefs.
```
