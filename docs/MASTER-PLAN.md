# open-flow — MASTER PLAN (everything from 4 Grok sessions)

**When:** 2026-08-10 (compiles the 4 Grok sessions + every .kt in `app/src/main/java`)
**Repo:** `/home/mitun/open-flow` (WSL Ubuntu-26.04) · main tip `ba78aeb`
**Sessions read:** `019feb59` (Voco strategic, 2786 lines) · `019febaa` (ponytail cut, 175) · `019febd3` (agent mess-up triage, 1143) · `019fec01` (active design, 798) = **4902 lines, all on disk**
**Code read:** all 36 .kt in `app/src/main/java` · `app/build.gradle.kts` · `AndroidManifest.xml`

---

## 0. ONE LINE

> **open-flow = free, FOSS, local-first Android app. Speak to type anywhere (Wispr job) + record memos, search them privately (NeoSapien job). No account, no cloud default, MIT. Bubble + accessibility, NOT a keyboard IME.**

---

## 1. PRODUCT — locked per session 019fec01 (Aug 10 IA lock, lines 632–646) + session 019feb59 + Voco §A/B/C

### 1.1 M3 IA (locked)
| Zone | Items | Source line |
|------|-------|-------------|
| **Bottom bar** (4 tabs, fixed) | Home · Dict · Snips · Style | 019fec01:643 + 1245 |
| **Drawer** (3 items, fixed) | Settings · History · Customize | 019fec01:617 |
| **Home body** (reorderable) | Setup · Stats · Key actions · Test field · Recent | 019fec01:635 + 314 |
| **Bubble overlay** | Transcribe → 1 insert on stop, no raw dump | 019febd3:907 + BASELINE §2 |

**Not in navbar** (019fec01:619): "Home · Dict · Snips · Cleanup · Copy last"

### 1.2 Two jobs (locked 019feb59 + AGENTS.md)
| Job | Source product | Implementation |
|-----|----------------|----------------|
| **1. Voice-to-text in any app** | Wispr Flow | Bubble + AccessibilityService → ACTION_SET_TEXT |
| **2. Private searchable voice memory** | NeoSapien.ai (without ₹12,999 hardware) | Recorder + Transcript + FTS search (when built) |

### 1.3 Hard product locks (NEVER re-open without Mitun OK)
- Platform: **Android only** (for now)
- License: **MIT FOSS** (picked at 019feb59:1786; never closed-source)
- Keyboard: **NOT a custom IME** — bubble overlay only (019feb59:1245, 019febd3:2345)
- Online features: **opt-in only**, INTERNET blocked via NetworkSecurityConfig
- Account / ads / analytics: **NEVER**
- STT: **Android `SpeechRecognizer`** with `EXTRA_PREFER_OFFLINE`
- Build process: per AGENTS.md + superpowers (worktree → search → plan → TDD → commit)

---

## 2. COMPETITORS — every one mentioned, with 2026 status

Source: 019feb59:1325–1413 (full FOSS map) + 019fec01:165 (Wispr desktop feature list)

| App | Job | Platform | FOSS / Status | 2026 Reality | Source |
|-----|-----|----------|---------------|--------------|--------|
| **Wispr Flow** | STT | Cloud (Mac/Win/iPhone/Android) | Closed · $12/mo Pro | Industry standard. Privacy scare (screenshots 2023). 40% MoM growth. 375k Android waitlist | 019feb59:1667, 019fec01:165–250 |
| **NeoSapien.ai** | Memory | Hardware pendant ₹12,999 + app | Closed | Paid hardware. Phone-only = open-flow wedge | 019feb59:455–468 |
| **FUTO Keyboard** | STT IME | Android | Source-available | 4.6★ / 5k+ reviews. Best polish. Swipe weaker than Gboard. Voice "a bit slow" | 019feb59:1338 |
| **Whisper (woheller69)** | Voice IME + Whisper | Android | MIT, F-Droid active | v3.7. Active. Slow on weak phones. Model download pain | 019feb59:1339 |
| **Whisper+** | Faster Whisper fork | Android | GPL-3, F-Droid | ~381★ GH. Snappier. Same "old model" critique | 019feb59:1340 |
| **Voxscribe** | PTT mic | Android | FOSS | New mid-2026. Fresh. Small user base | 019feb59:1341 |
| **Sayboard** | Vosk IME | Android | GPL-3, F-Droid | Last big update ~2024. Barebones. Manual models | 019feb59:1342 |
| **Kõnele** | Speech UI/IME glue | Android | Apache-2 | Old. Often needs server (Estonian). Not pure local | 019feb59:1343 |
| **Offline Voice Input (notune)** | Mic popup | Android | FOSS | Active. Good for Joplin. Not full memo | 019feb59:1344 |
| **Notely Voice** | Record → Whisper → notes | Android | F-Droid GPL free / Play paid | Closest to open-flow memos. Accuracy good. Play users mad about sub | 019feb59:1357 |
| **Scrib** | Pick audio → Whisper text | All | FOSS | Early ~29★. File transcribe only | 019feb59:1358 |
| **Fossify Voice Recorder** | Record only | Android | GPL | No STT. People pair with Notely | 019feb59:1359 |
| **Otter.ai / Notta** | Meeting transcription | Cloud | Closed | $100+/yr. Cloud fear. Real privacy pain for journalists/lawyers | 019feb59:400–403 |
| **FreeFlow** | STT | macOS | MIT | 2.4k stars. Released 1.2.0. No mobile | 019feb59:354–435 |
| **Voquill** | Cross-platform STT | All, 997★ | FOSS | Mobile in progress, half-baked Android | 019feb59:354–435 |
| **Yap, Muesli, QSpeak, Mumbli, Jarvis** | STT | macOS/Desktop | FOSS | Low stars, no mobile | 019feb59:354–435 |
| **Dyad, OpenHands, Agent Orchestrator, etc.** | AI coding agents | Desktop | FOSS | Cited as "close to your idea" but no mobile voice | 019fec01:BG (CHATGPT.txt) |

### Competitive verdict (019feb59:1391–1406)
- **Keyboard alone**: don't build, FUTO wins
- **Memo alone**: Notely exists
- **Both + recall + private FOSS + one app**: **the gap** = open-flow's real wedge
- **10× simpler than the 3-app stack** (FUTO + Notely + Fossify + hope)

---

## 3. FEATURE BACKLOG — every feature ID across all 4 sessions

Legend: ✅ shipped in main · ⏳ partial · ❌ missing · 🚫 skip/defer · 🟢 MVP · 🔵 V1 · 🟡 V2/V3

### 3.1 Session 019feb59 (Voco) — 54 features cataloged (Aug 10 morning)

#### A. STT / Wispr layer (19 features)
| # | Feature | Status | LOC est | Notes |
|---|---------|--------|---------|-------|
| 1 | Voice IME w/ mic | 🚫 **rejected** — not product path, replaced by bubble | n/a | line 940 audit confirms `createOnDeviceSpeechRecognizer()` API doesn't exist |
| 2 | Live STT into focused field | ⏳ partial — bubble does it, IME doesn't | ~30 | Bubble path works; needs polish |
| 3 | Floating bubble/overlay dictation (Wispr-Android style) | ✅ shipped F10 (`021b066`) | working | |
| 4 | Prefer on-device STT | ✅ shipped (`EXTRA_PREFER_OFFLINE`) | n/a | |
| 5 | Online STT only if user opts in | ✅ default (no INTERNET in code path) | n/a | |
| 6 | Language picker + auto | ⏳ partial — bubble has lang tag, no UI picker | ~100 | |
| 7 | OS language packs (as many as device has) | ✅ shipped (delegated to OS) | 0 | |
| 8 | Continuous / long dictation | ✅ F11 (`a51b3e7`) | ~50 | |
| 9 | Pause / resume dictation | ❌ missing | ~30 | |
| 10 | Partial results (words appear while speaking) | ⏳ partial — gated by `bubbleShowText` pref | ~30 | user's "still shows text" complaint |
| 11 | Punctuation / auto capitals | ✅ shipped (`TextPostProcessor`) | ~50 | |
| 12 | Profanity filter toggle | ❌ missing | ~30 | |
| 13 | Custom vocabulary (names, jargon) | ⏳ partial — table exists, no per-app bias | ~100 | Android STT biasing API limited |
| 14 | Voice commands ("new paragraph", "delete last") | ⏳ partial — voice commands in TextPostProcessor (newline/period) | ~50 | "delete last" NOT supported (019feb59:556) |
| 15 | Filler-word cleanup | ✅ shipped (TextPostProcessor.stripFillers) | ~30 | |
| 16 | Quick switch 2-3 languages | ❌ missing | ~50 | |
| 17 | Works in WhatsApp/mail/browser | ✅ shipped (Accessibility INSERT) | n/a | |
| 18 | Share-to / system speech intent provider | ❌ missing | ~50 | |
| 19 | Fallback UI when offline engine missing | ❌ missing | ~30 | |

#### B. Recorder / NeoSapien layer (27 features) — **MOSTLY MISSING, ALL DEFERRED**
| # | Feature | Status | LOC est |
|---|---------|--------|---------|
| 20 | Tap-to-record activity | ❌ missing — recorder/ package doesn't exist | ~400 |
| 21 | Pause / resume | ❌ missing | ~80 |
| 22 | Background recording (FGS, mic type) | ❌ missing — manifest has NO FGS permission | ~300 |
| 23 | Lock-screen / notification controls | ❌ missing | ~150 |
| 24 | Quick Settings tile | ❌ missing | ~200 |
| 25 | Home-screen widget | ❌ missing | ~200 |
| 26 | Live transcript while recording | ❌ missing (mic-fight trap from line 1258) | ~250 |
| 27 | Auto-save session (audio + transcript) | ❌ missing | ~80 |
| 28 | Playback synced with transcript | ❌ missing | ~300 |
| 29 | Waveform scrubber | ❌ missing (G found RMS not wired) | ~250 |
| 30 | Speed 0.5x-3x + pitch correct | ❌ missing | ~150 |
| 31 | EQ / quality presets | ❌ missing | ~200 |
| 32 | Trim / cut | ❌ missing | ~200 |
| 33 | Concatenate recordings | ❌ missing | ~150 |
| 34 | Bookmarks / highlights | ❌ missing | ~80 |
| 35 | Notes per recording | ❌ missing | ~50 |
| 36 | Tags + smart folders | ❌ missing | ~120 |
| 37 | Voice memo vs long-form modes | ❌ missing | ~30 |
| 38 | Import existing audio → transcribe | ❌ missing | ~150 |
| 39 | Bluetooth / wired headset support | ⏳ partial — Android handles | ~50 |
| 40 | Sample rate / format options | ❌ missing | ~80 |
| 41 | Auto-delete after N days (opt-in) | ⏳ partial — `retentionPolicy` pref exists, no auto-purge | ~30 |
| 42 | Phone-call record (jurisdiction gate) | 🚫 skip (line 1294 "legal mess") | n/a |
| 43 | Multi-source mic + system audio | 🚫 skip (line 1294 OEM blocks) | n/a |
| 44 | Noise suppression (RNNoise) | ❌ missing | ~250 |
| 45 | Auto-chapters (silence detection) | ❌ missing | ~80 |
| 46 | Wear OS quick-record | 🚫 skip (line 549) | n/a |

#### C. Search / second brain (10 features)
| # | Feature | Status | LOC est |
|---|---------|--------|---------|
| 47 | Timeline of all sessions | ⏳ partial — Home shows recent only | ~50 |
| 48 | FTS search across all transcripts | ❌ missing — cut in ponytail | ~200 |
| 49 | Natural date filters ("yesterday", "Tuesday") | ❌ missing | ~100 |
| 50 | Search in tags / notes / bookmarks | ❌ missing | ~50 |
| 51 | "What did I say about X" keyword + date | ❌ missing | ~50 |
| 52 | Local RAG over past transcripts | 🚫 defer (LLM work) | ~500 |
| 53 | Daily / weekly summary (on-device LLM) | 🚫 defer (Qwen, ~400 MB) | ~300 |
| 54 | Action-items extraction | 🚫 defer | ~150 |
| 55 | Bullet notes mode from transcript | 🚫 defer | ~100 |
| 56 | Markdown editor w/ insert from transcript | 🚫 defer | ~400 |

#### D. On-device AI opt-in (8 features)
| # | Feature | Status | Notes |
|---|---------|--------|-------|
| 57 | Whisper-tiny (better langs) | 🚫 defer (line 549 NDK yak-shaving) | 75 MB |
| 58 | Whisper-small (noisy / accents) | 🚫 defer V3 | 500 MB |
| 59 | Speaker labels / diarization | 🚫 defer (line 544 "no MIT model on Android") | |
| 60 | Qwen 0.5B summary | 🚫 defer V2 | 400 MB, but Apache-2.0 restrictions (line 960) |
| 61 | Larger summary (1.5B / Phi) | 🚫 defer V3 | 1+ GB |
| 62 | Translate transcript | 🚫 defer | |
| 63 | Custom vocab boost with local models | 🚫 skip (line 549 no API) | |
| 64 | Auto-redact PII (on-device NER) | 🚫 defer V3 | |

#### E. Export / share (8 features)
| # | Feature | Status | LOC est |
|---|---------|--------|---------|
| 65 | Export .txt | ❌ missing (cut in ponytail) | ~50 |
| 66 | Export .md (timestamps) | ❌ missing | ~80 |
| 67 | Export .srt / .vtt | ❌ missing | ~120 |
| 68 | Export .json | ❌ missing | ~100 |
| 69 | PDF export | ❌ missing | ~200 |
| 70 | Zip: audio + transcript | ❌ missing | ~120 |
| 71 | Share sheet (text / file) | ❌ missing | ~80 |
| 72 | Subtitle pack for video editors | 🚫 defer late | |

#### F. Privacy / security defaults (11 features) — MOST DONE
| # | Feature | Status |
|---|---------|--------|
| 73 | No account required | ✅ shipped (default) |
| 74 | No analytics / ads / trackers | ✅ shipped (default) |
| 75 | No INTERNET until opt-in | ✅ NetworkSecurityConfig blocks |
| 76 | AES encrypt at rest (Keystore) | 🚫 skip in MVP — only recorder needs it, recorder is V2 |
| 77 | Optional biometric lock | 🚫 defer V2 |
| 78 | Decoy mode | 🚫 defer late |
| 79 | Auto-wipe after failed biometric | 🚫 defer late |
| 80 | Privacy report (bytes sent) | ⏳ partial — static report text only (`PrivacyDefaults.kt`) |
| 81 | Reproducible F-Droid builds | ⏳ partial — `local.properties` not committed, but no Google deps |
| 82 | Clear permission UX | ✅ shipped |
| 83 | Network security config / opt-in crash | ⏳ partial — crash never wired |

#### G. Opt-in online (8 features, ALL OFF BY DEFAULT)
| # | Feature | Status |
|---|---------|--------|
| 84 | WebDAV / Nextcloud E2E sync | 🚫 defer V1.5 |
| 85 | Self-host / paid relay E2E sync | 🚫 defer V1.5 |
| 86 | User-config cloud STT | 🚫 defer |
| 87 | User-config cloud LLM | 🚫 defer |
| 88 | User-config translate API | 🚫 defer |
| 89 | Encrypted share link (self-destruct) | 🚫 defer |
| 90 | Opt-in Sentry / crash | 🚫 defer |
| 91 | Beta update channel | 🚫 defer |

#### H. UI / system (7 features)
| # | Feature | Status | LOC est |
|---|---------|--------|---------|
| 92 | Material You + dark mode | ✅ shipped (theme + dark mode flow) | working |
| 93 | Multi-language UI | ❌ missing | ~300 |
| 94 | Onboarding < 2 min | ⏳ partial — chips exist, no guided flow | ~200 |
| 95 | Settings: engines, langs, privacy toggles | ✅ shipped (drawer Settings hub) | working |
| 96 | Live caption floating bubble | 🟢 same as #3 (bubble = caption chrome) | |
| 97 | Calendar / meeting detect | 🚫 defer late | |
| 98 | Voice reminders → system alarm | 🚫 defer late (line 1062 hard) | |

### 3.2 Session 019fec01 — Gap list (1-15, Aug 10 latest)

| # | Want | Status in code | Next |
|---|------|----------------|------|
| 1 | Bubble chrome rewrite (no speech text default) | ✅ done (default `bubbleShowText=false`) | surface in Home |
| 2 | Edge snap + haptics | ⏳ snap pref exists, never applied | implement in `setupTouch` |
| 3 | Real waveform (RMS) | ❌ `onRmsChanged {}` empty | wire scaleY 0.95-1.05 |
| 4 | Copy chip after stop | ⏳ Home has Copy-last, but no post-stop toast | add toast with text |
| 5 | Shrink in search fields | ❌ | FieldPolicy extension |
| 6 | Auto cleanup levels None/Light/Med/High | ✅ shipped (high = medium in code, "ponytail") | rename or note |
| 7 | Style by app type | ❌ global only | per-package DB map |
| 8 | Export/share history | ❌ (cut in ponytail) | F15 wire FileProvider |
| 9 | History search + flag | ❌ | Room query |
| 10 | Raw + polished · undo polish | ❌ Room only has `text` | add `raw` col, DB v4 |
| 11 | Star dictionary · sort | ❌ Room sorts by word alpha only | add `priority` |
| 12 | Retention: keep / 24h wipe / never store | ⏳ pref exists, no auto-purge | wire WorkManager |
| 13 | Optional notif: service dead · copy last | ❌ | NotificationCompat |
| 14 | Lang pack hint UI | ❌ | link to Play Store offline langs |
| 15 | Sounds toggle start/stop | ⏳ pref exists, no sound | ToneGenerator |

**NEVER (per 019fec01:398 + 019feb59:2090-2098):** Command Mode · Transforms AI · Notetaker · MCP · cloud STT · account · team · sync · billing · true cloud self-correct · Wear · Tile · home widget · phone-call record

### 3.3 Session 019febd3 — feature timeline (mapped from `AGENTS.md` + commit hashes)

| ID | Feature | Commit | Status |
|----|---------|--------|--------|
| F0 | Bootstrap (license, security, process) | `dcb65e1` | ✅ |
| F1 | Android Compose scaffold | `54227af` | ✅ |
| F10 | Flow Bubble (NOT IME) | `021b066` | ✅ user validated on phone |
| F11 | Continuous dictation + light UI | `a51b3e7` | ✅ |
| F12-rel | Dictation reliability | `5584152` | ✅ |
| F12-ux | UI foundation (theme, Open*, dark mode) | `37a4a1f` | ✅ |
| F13 | Wispr parity core (local) | `eafe209` | ✅ |
| F14 | Polish (bank hide, bubble modes, shake, pulse) | `1e32bc3` | ✅ |
| F15 | Export/share history | (planned next per HANDOFF) | ❌ |
| F16 | Memo recorder | (planned per HANDOFF) | ❌ |
| F17 | Language picker UI polish | (planned) | ❌ |

---

## 4. PRIVACY / SECURITY MODEL — locked

### 4.1 Defaults (all ON by default, all OFF requires user toggle)
- No account
- No analytics SDK
- No telemetry
- No crash reports (opt-in only)
- `INTERNET` permission declared in manifest but **blocked by `network_security_config`** until user toggles a cloud feature
- Voice-typing audio stays in RAM (never written to disk by open-flow)
- Recorder audio would be encrypted, but recorder not built yet
- All audio stays on phone

### 4.2 Permissions (manifest, all lazy)
| Permission | When | Status |
|-----------|------|--------|
| `RECORD_AUDIO` | always (for STT) | ✅ declared |
| `POST_NOTIFICATIONS` | Android 13+ foreground | not declared yet (no FGS yet) |
| `FOREGROUND_SERVICE` + `microphone` type | recorder (V2) | not declared |
| `INTERNET` | only if cloud feature on | declared + NSC-blocked |
| `WAKE_LOCK` | recorder background | not declared |
| `BIOMETRIC` | optional vault (V2) | not declared |
| `READ_MEDIA_AUDIO` | import audio (V2) | not declared |

**Forbidden:** `QUERY_ALL_PACKAGES`, `READ_CONTACTS`, `READ_SMS`, location

### 4.3 License compatibility matrix (verified 019feb59:594-604)
| Component | License | OK? |
|-----------|---------|-----|
| open-flow source | MIT | ✓ |
| AndroidX | Apache-2.0 | ✓ |
| EncryptedFile (Jetpack Security) | Apache-2.0 | ✓ (but unused) |
| whisper.cpp (opt-in) | MIT | ✓ (deferred) |
| llama.cpp (opt-in) | MIT | ✓ (deferred) |
| Qwen weights | Apache-2.0 + restrictions | ⚠️ NOTICE required if bundled |
| Phi-3 weights | MIT | ✓ |

---

## 5. USER RULES from chats (lock in AGENTS.md if not already)

| # | Rule | Lock? |
|---|------|-------|
| 1 | Max **5 sub-agents**, **never edit same file in parallel** (019febd3:1163) | ✅ AGENTS.md |
| 2 | One git commit per feature (line 1143) | ✅ AGENTS.md |
| 3 | Web search before every build (line 1130) | ✅ PROCESS.md |
| 4 | Plan doc per feature (line 1132) | ✅ PROCESS.md |
| 5 | Interrupt rule: side request ≠ stop primary (line 1151) | ✅ AGENTS.md |
| 6 | Always place APK at `~/Desktop/` (line 1157) | ⚠️ Not in AGENTS.md — add |
| 7 | "Don't be a yes-man, check it" | ⚠️ Implicit, not in docs |
| 8 | FOSS, MIT or pick later, no closed-source | ✅ AGENTS.md |
| 9 | Verify build before claiming done | ✅ PROCESS.md |
| 10 | Voice style: caveman terse, "DID / PASS / NEXT" report shape | ✅ AGENTS.md |
| 11 | "Actually 4:30 wait 5:30" → only 5:30 (course-correct works) | ✅ shipped |
| 12 | STT speed matters, pin en-US, offline prefer | ✅ shipped |

---

## 6. REAL BUGS (verified by reading code, in audit-2026-08-10.md)

| # | Bug | File | Severity | Fix size |
|---|-----|------|----------|----------|
| A | `fieldPrefix` capture + `FieldPolicy.mergeSession` is dead code — `ACTION_SET_TEXT` already replaces whole field | `FlowAccessibilityService.kt:64,433,541,619` | medium | ~10 |
| B | SttEngine listener leak — new `setListener(obj)` overwrites but old `onFinal` callbacks could fire on stale `sessionBuffer` | `FlowAccessibilityService.kt:443-516` | high | ~2 |
| C | `ContinuousPolicy.ERROR_*` = dupe of `SpeechRecognizer.ERROR_*` | `ContinuousPolicy.kt:40-50` | low | delete companion |
| D | `bumpStats` is non-atomic read-modify-write → lost updates | `DictationRepository.kt:70-87` | medium | ~5 |
| E | `bubbleShowText` toggle buried — user said "text still shows in pill" needs 1-tap from Home | `MainActivity.kt:920-933` + add Home chip | user-visible | ~8 |
| F | `bubbleEdgeSnap` pref exists, never snaps | `FlowAccessibilityService.kt:280-343` | medium | ~15 |
| G | `onRmsChanged {}` empty — no waveform | `SttEngine.kt:174` | medium | ~8 |
| H | No raw+polished in Room (`text` only) | `DictationEntities.kt` + DB v4 | medium | ~15 |
| I | No post-stop toast | `FlowAccessibilityService.stopListening` | low | ~5 |
| J | MainActivity 985 lines (god file) | `MainActivity.kt` | low | defer |
| K | `BubbleLabelFormatter.idle()` always returns text, ignores `bubbleShowText` | `BubbleLabelFormatter.kt:8` | low | confirm runtime, defer |

---

## 7. NEXT-DROP PUNCH LIST (recommend Drop 4)

Tier 0 bugs A–D (~30 lines) + Tier 1 user-asked E–I (~50 lines)

1. A: drop `fieldPrefix` merge — `ACTION_SET_TEXT` replaces cleanly
2. B: `stt.setListener(null)` in `stopListening` before re-attach
3. C: delete `ContinuousPolicy.ERROR_*` companion (use `SpeechRecognizer.ERROR_*`)
4. D: wrap `bumpStats` in `withTransaction`
5. E: add `bubbleShowText` chip to Home `keys` card
6. F: implement edge snap in `setupTouch` ACTION_UP
7. G: wire `onRmsChanged` to bubble scaleY
8. H: add `raw` column to `dictations`, DB v4 migration, save raw on stop
9. I: toast after stop with "Polished X inserted (or copied)"
10. Tests: `CourseCorrector.apply("meet tuesday wait friday")` → "meet friday"
11. Tests: `FieldPolicy.mergeSession("Hello ", "world")` → "Hello world"
12. Tests: `LayoutPrefs.parseModules("!history,customize")` → history hidden, customize visible

**Branch:** `.worktrees/feat-drop-4-bubble-fix` (per AGENTS.md superpowers)
**Commit:** `fix: drop 4 — bubble chrome 1-tap, edge snap, RMS, raw+polished, post-stop toast`
**Ship:** copy APK to `~/Desktop/open-flow-debug.apk`

---

## 8. FUTURE / DEFERRED — explicit "do not build until asked"

Saved per `019feb59:2127` (V2/V3 feature dump, ~40 features) + `019fec01:1-15` (Jan 15 gap list)

### 8.1 Recorder + Search (V1 next, per HANDOFF)
- F15 Export/share history (txt/md/srt/json/PDF)
- F16 Memo recorder (audio + transcript, foreground mic service)
- FTS timeline rebuilt properly (when recorder ships, not standalone)
- Auto-purge WorkManager (retention: keep / 24h wipe / never store)

### 8.2 Polish (V1.5)
- #13 notif (service dead · copy last)
- #14 lang pack hint UI
- #15 sounds actually play (ToneGenerator)

### 8.3 On-device AI (V2 explicit opt-in)
- Whisper-tiny / -small JNI .so (line 549: bundle prebuilt from official releases, verify SHA)
- Qwen 0.5B Q4 download-on-demand with license NOTICE
- Per-app style overrides
- Daily summary (on-device LLM, opt-in)

### 8.4 Hidden (V3, only if users ask)
- Wear OS / Tile / Home widget
- Phone-call record (legal-jurisdiction gated)
- Multi-source mic + system audio
- Whisper.cpp medium (1.5 GB, battery killer)
- E2E sync (WebDAV / own server)
- PII auto-redact (on-device NER)
- Subtitle export for video editors
- iOS / Desktop Electron port

### 8.5 NEVER (locks)
- Cloud STT / LLM as default
- Account / billing / team features
- Closed-source core
- Vendor lock-in
- Telemetry
- Play Services hard dependency (microG OK)
- "Always-on pendant" clone (battery hate)
- Voice commands ("delete that") — Android STT doesn't return commands (verified 019feb59:556)
- Speaker diarization — "no MIT model on Android, pyannote needs PyTorch+GPU" (verified 019feb59:544)
- Custom dictionary auto-learn — no Android API (verified 019feb59:549)

---

## 9. MOAT analysis (per 019feb59:1462-1718)

| Moat type | Possible? | Action |
|-----------|-----------|--------|
| Secret code / patent | NO | skip |
| Cloud data network | NO | skip |
| AI model alone | NO | skip |
| **Switching cost (history)** | YES | protect: don't lose user dictations |
| **Habit / default (keyboard)** | YES-ish (bubble = less sticky than keyboard default) | polish bubble to one-tap |
| **Trust brand** | YES | reproducible F-Droid build, no surprise net |
| **Community + distribution** | YES | "the" F-Droid pick |
| **Paid sync/support (Red Hat style)** | YES (soft) | never paywall core features |

**Verdict:** soft moat possible = habit + personal data gravity + trust. Not secret tech.

---

## 10. SHIP ORDER (boring = wins, per 019feb59:1582-1602)

```
Build order for stickiness:
Week 1  → best local STT into any app (Wispr job)
Week 2  → best searchable private memory (NeoSapien job)
Week 3+ → export, polish, opt-in sync
NEVER  → TTS, always-on pendant, Wear, call record, LLM, Whisper bundle, 40-feature dump
```

---

## 11. REPO LAYOUT (current state vs target)

```
/home/mitun/open-flow/
├── .git/                  # git, main only, tip ba78aeb
├── app/
│   └── src/main/java/app/openflow/
│       ├── bubble/        # ✅ shipped (FlowAccessibilityService, FieldPolicy, BubbleLabelFormatter, PackagePolicy, ShakeDetector)
│       ├── data/          # ✅ shipped (single DB: dictations + dict + snippets + stats)
│       ├── prefs/         # ✅ shipped (FlowPrefs + LayoutPrefs)
│       ├── privacy/       # ✅ shipped (PrivacyDefaults)
│       ├── stt/           # ✅ shipped (SttEngine + ContinuousPolicy + SttTuning)
│       ├── text/          # ✅ shipped (CourseCorrector + TextPostProcessor)
│       ├── ui/            # ✅ shipped (MainActivity 985 lines + theme + components + shell)
│       └── OpenFlowApp.kt # ✅ shipped
├── info/sessions/          # grok chat dumps + brainstorm HTML mockups (gitignored lately)
├── docs/
│   ├── AGENTS.md
│   ├── BASELINE.md
│   ├── HANDOFF.md          # says main tip = 946be80 (Drop 3) — STALE by 1 commit
│   ├── FEATURES.md         # full Wispr A-Z
│   ├── PROCESS.md
│   ├── SECURITY.md
│   ├── AUDIT-2026-08-10.md # bug list
│   ├── MASTER-PLAN.md      # this file
│   └── superpowers/plans/  # per-feature plans
├── dist/                   # APK output (gitignored)
├── gradle/                 # wrapper
├── .worktrees/             # git worktrees (gitignored)
└── open-flow-debug.apk     # 17MB
```

**MISSING packages** (per Voco plan, deferred):
- `recorder/` (F16) — package doesn't exist
- `export/` (F15) — package doesn't exist
- `transcript/` — merged into `data/`
- `search/` — merged into `data/`
- `ime/` — abandoned (pivot to bubble)

---

## 12. OPEN QUESTIONS / DECISIONS NEEDED

1. **Drop 4 GO?** (the punch list above) — yes/no
2. **HANDOFF.md stale** — it's 1 commit behind main. Update or leave (user looks at old APK sometimes per BASELINE §11).
3. **APK location convention** — both `~/Desktop/open-flow-debug.apk` (17MB) and `~/open-flow/open-flow-debug.apk` exist. Which is canonical?
4. **`bubbleShowText` default** — `false` is shipped. User complained "still shows text in pill". Is complaint from (a) old APK, (b) toggled on and forgot, or (c) stale chat? Drop 4 path adds 1-tap to verify.
5. **F16 recorder** — start next, or wait for F15 export first?
6. **Lock GROK nav order: Home / Dict / Snips in bottom, Settings in drawer, Style in bottom?** (Confirmed in 019fec01 + shipped in `ba78aeb` — verified. Drop 4 doesn't touch this.)

---

## 13. BUILD PHASES (Voco canonical ordering, 019feb59:2102-2117)

Phase order from `019feb59:2102` — full app, ordered so it stays usable:

| Phase | Scope | Open-Flow status |
|-------|-------|------------------|
| **P0** | Project skeleton, themes, Room, encrypt, permissions, privacy defaults | ✅ done (F0 + F1, commits `093fbbc` → `54227af`) |
| **P1** | Voice IME + live STT + language picker | ❌ not as IME. **Pivot to bubble** (F10 = `021b066`). Fulfilled via a11y. |
| **P2** | Recorder + transcript + save + list | ❌ missing — recorder/ package not built. F16 next. |
| **P3** | FTS search + timeline + export | ❌ missing — F15 export, FTS rebuilt when recorder ships |
| **P4** | Playback scrubber, bookmarks, tags, background service, tile | ❌ all deferred (V3+) |
| **P5** | Whisper opt-in, better offline, import audio | ❌ deferred V2 |
| **P6** | On-device summary LLM opt-in, chapters, vocab | ❌ deferred V2 |
| **P7** | Opt-in sync + privacy report + polish | ❌ deferred V1.5 |
| **P8** | Wear, call-record gate, diarization, advanced export, plugins | ❌ all skipped permanently (Voco audit line 544-556) |

**Rule:** "Full feature list = target. Phases = order of working delivery, not MVP cut forever."

---

## 14. THE HARD TECH TRAP — mic fight (019feb59:1258)

The single hardest constraint of the whole product:

> **Live STT + save audio at the same time is hard.** Mic is often exclusive. `MediaRecorder` + `SpeechRecognizer` together = classic fail (SO + Chromium issues).

**3 workarounds (pick one):**
1. Record only → transcribe file after (easy, not "live")
2. One `AudioRecord` pipeline → file + STT chunks (harder, correct)
3. **STT live only, no full audio file (keyboard/bubble path)** ← **this is what open-flow shipped**

The architecture commit (019febd3:2345) explicitly dropped the IME/keyboard path in favor of (3). The recorder when it ships (F16) will likely use (1) for V1 → (2) for V2. Don't ship (3) with recorder "live" — it breaks.

**Implication for future devs:** if anyone proposes "let me add real-time transcription overlay during recording", **stop them**. Either defer or accept the tradeoff.

---

## 15. WISPR FLOW DESKTOP feature inventory (019fec01:165-360)

Wispr Flow Desktop (Mac + Windows) features per official docs. Used to map what open-flow can ship locally vs. need cloud for.

### 1. Dictation control (Flow Bar)
- Floating Flow Bar (bottom default; drag to bottom / left / right)  → **open-flow: bubble + drag (F10)**
- Position saved; vertical when side-docked  → **open-flow: ⏳ partial**
- Center bubble: start · live waveform · Cancel / Stop  → **open-flow: G fix needed**
- Push-to-talk (hold) + hands-free (double-tap)  → **open-flow: PTT yes, hands-free yes**
- Default hotkeys: Mac fn · Win Ctrl+Win  → **open-flow: not applicable**
- Mouse Flow: extra mouse buttons  → **skip**
- Language picker on bar  → **open-flow: ⏳ partial (lang in bubble settings)**
- Right-click menu: hide 1h · Settings · Mic · History · Paste last  → **open-flow: ⏳ settings in drawer**
- Always show bar  → **open-flow: ⏳ snooze (10min) instead**

### 2. AI polish (while / after speak)
- Filler strip · auto punct · lists · backtrack / self-correct  → **open-flow: ✅ local rules**
- Whisper / quiet speech support  → **n/a (different engine)**
- Auto Cleanup levels None · Light · Medium · High  → **open-flow: ✅ shipped**
- Undo AI edit from history (raw vs cleaned)  → **H fix needed**
- Session length up to ~20 min (warn near end)  → **open-flow: unlimited via F11 restart**
- No offline  → **open-flow: ✅ has offline (inverse)**
- **CLIFF:** Context-aware names / jargon  → **NEVER match without cloud AI**

### 3-15. (full inventory in 019fec01:200-360)
- Hub app: Home · Notifications · Dictionary · Snippets · Style · Scratchpad · Insights · Transforms
- Personalization: Dictionary, Snippets, Styles (per-app-context), 100+ langs, auto-detect
- Command Mode + Transforms (Pro paid Experimental)
- Vibe coding / IDE tagging
- Scratchpad (β, Mac/Win)
- Notetaker (β, Mac now, Win later)
- History + audio playback (last 14 days)
- System tray / Mac menu bar
- Settings map (General · System · Notetaker · Vibe · Experimental · Account · Data & Privacy)
- Privacy: Privacy Mode, Private Cloud Sync, SOC 2 · ISO 27001 · HIPAA
- Team / Enterprise
- Plans: Free Basic / Pro / Team / Enterprise
- Free desktop: ~2k words/week

### What desktop has that mobile (and open-flow) lacks
- Flow Bar + hotkeys + Mouse Flow  → bubble equivalent OK
- **Command Mode** + **Transforms** AI  → **NEVER (online AI)**
- Scratchpad rich + version history  → V2+ if asked
- Notetaker (Mac) · MCP · Connectors  → **NEVER (account + cloud)**
- Insights / Voice profile / Leaderboard  → **NEVER**
- Audio playback in history  → defer (V3+)
- Mic ranking / clamshell  → **n/a mobile**
- Vibe coding / IDE tagging  → **NEVER**
- Full Settings depth · team admin  → **NEVER (no team feature)**

### Hard limits on desktop that open-flow turns around
- Always online for STT  → **open-flow inverse: works offline**
- No word-by-word live paste (by design)  → **open-flow inverse: live chrome, no raw dump**
- Styles / Auto Cleanup quality = cloud AI  → **open-flow: local rules weaker but private**

---

## 16. WHAT REAL PEOPLE PRAISE vs HATE (019feb59:1364-1378)

### Praise (FOSS voice apps)
- Offline = real
- No account / no ads
- FUTO / Notely accuracy "good enough"
- F-Droid free path matters

### Hate
- Slow on mid/old phones  → **open-flow bane: tuned 0.9s silence**
- Big model downloads  → **open-flow WINS: zero model download**
- Weaker than Gboard for daily typing  → **open-flow: this is acceptable, target ≠ Gboard**
- Play paywall (Notely paid fork)  → **open-flow WINS: always free, MIT**
- Setup friction (IME enable, models)  → **open-flow: bubble + a11y 2-step enable**
- **No single app that does keyboard + searchable encrypted memos well**  → **open-flow TARGETS this**

---

## 17. REPO LAYOUT (current state, post-organize, 2026-08-10)

```
/home/mitun/open-flow/   (WSL Ubuntu-26.04)
├── .git/                  → main only, tip `ba78aeb`
├── AGENTS.md              → locked (law)
├── LICENSE                → MIT
├── README.md
├── SECURITY.md            → locked
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/            → drawable / layout / values / xml
│       └── java/app/openflow/
│           ├── bubble/        (5 files)  ✅ shipped F10
│           ├── data/          (4 files)  ✅ shipped
│           ├── prefs/         (2 files)  ✅ shipped (incl. LayoutPrefs Drop 2)
│           ├── privacy/       (1 file)   ✅ shipped
│           ├── stt/           (3 files)  ✅ shipped (incl. continuous F11)
│           ├── text/          (2 files)  ✅ shipped (course-correct + polish)
│           ├── ui/            (10 files) ✅ shipped (incl. theme, components, shell)
│           └── OpenFlowApp.kt
├── gradle/                 → wrapper
├── gradlew
├── gradle.properties
├── settings.gradle.kts
├── build.gradle.kts
├── local.properties        → gitignored (1-line SDK path)
└── docs/
    ├── INDEX.md            → 2026-08-10 (this reorganise)
    ├── AGENTS.md           → note: reorg keeps AGENTS.md at repo root per workflow rule
    ├── HANDOFF.md          → STALE: says `946be80`, real tip = `ba78aeb`
    ├── BASELINE.md         → product law + feature history
    ├── MASTER-PLAN.md      → this file
    ├── AUDIT-2026-08-10.md → bug list A-K + plan
    ├── FEATURES.md         → Wispr A-Z vs open-flow status matrix
    ├── PROCESS.md          → per-feature workflow
    ├── SECURITY.md         → privacy + permission defaults
    ├── README.md
    ├── sessions/           → raw 4 Grok transcripts (canonical, ~160KB)
    │   ├── README.md       → session index + reading order
    │   ├── 2026-08-10-strategic-voco.txt
    │   ├── 2026-08-10-ponytail-cut.txt
    │   ├── 2026-08-10-agent-messup-triage.txt
    │   └── 2026-08-10-design-customisability.txt
    ├── mockups/            → brainstorm HTMLs (archived)
    │   └── 2026-08-10-brainstorm/  (14 HTMLs + server state)
    └── superpowers/        → legacy per-feature plans + specs
        ├── plans/          (10 files)
        └── specs/          (2 files)

gitignored at root:
├── .gradle/ .kotlin/ .idea/  → build artefacts
├── .worktrees/                → feature checkouts
├── dist/                      → APK output (kept single copy + .gitkeep)
├── *.apk *.aab                → caught by gitignore
├── open-flow-debug.apk        → gitignored, build produces fresh
└── local.properties           → 1-line SDK path, never commit
```

**Removed during this organize:**
- ~~`info/sessions/`~~ → moved to `info/sessions/` + `info/mockups/2026-08-10-brainstorm/`
- ~~`.worktrees/12-ux-foundation/`~~ (stale empty dir, gradle leftover)
- ~~`.worktrees/chore-ponytail-cut/`~~ (stale empty dir)
- ~~`.worktrees/product-brutal/`~~ (worktree for parked brutal branch — deleted, branch dropped)
- ~~`.worktrees/product-m3/`~~ (worktree for merged M3 branch — deleted, branch dropped)
- ~~`open-flow-debug.apk`~~ (duplicate in repo root, kept only `dist/`)

---

## 18. SUCCESS METRICS (from 019feb59:1426-1633)

### Real moat checklist (operational definition of "people use it")
- [ ] First open → dictating in WhatsApp in <2 min
- [ ] Search hits *their* old words in 1 sec
- [ ] F-Droid "recommended private voice"
- [ ] Zero surprise network
- [ ] Export anytime (trust → stay longer)

### PASS / FAIL for "people will use it"
- **PASS:** keyboard + search memory ship solid
- **FAIL:** feature zoo before daily dictation feels better than FUTO

### Demand focus (pick 1 first)
**Best first market:** privacy + de-Google + FOSS Android
- Already install FUTO/Notely, want **one app**
- Review, star, share on Graphene / F-Droid / Reddit
- Students / lawyers = later paid story. Win nerds first = install base.

---

## 19. CUSTOM RULES (user-locked, all in chat)

| # | Rule | Source session line |
|---|------|---------------------|
| 1 | Max 5 sub-agents, never edit same file in parallel | 019febd3:1163 |
| 2 | One git commit per feature | line 1143 |
| 3 | Web search before every build | line 1130 |
| 4 | Plan doc per feature (in `docs/process/plans/`) | line 1132 |
| 5 | Interrupt rule: side request ≠ stop primary | line 1151 |
| 6 | Always place APK at `~/Desktop/` | line 1157 |
| 7 | "Don't be a yes-man, check it" — verify before claiming done | BASELINE §11 |
| 8 | Caveman terse voice + DID/PASS/NEXT report shape | AGENTS.md |
| 9 | "the app is fucking working" — real-world run matters | line 1165 |
| 10 | "verify it, don't be a yes-man" | HANDOFF + multiple sessions |
| 11 | MIT, no closed-source | line 1784 |
| 12 | Verify build (assembleDebug + unit tests) before "done" | PROCESS.md |

---

## 20. PHASE P0 actual shipped list (per commit timeline 019feb59:2200-2400)

| Tip | Date | What shipped |
|-----|------|--------------|
| `093fbbc` | 2026-08-10 | AGENTS.md created |
| `54227af` | 2026-08-10 | F1 Android Compose scaffold (gradle, build, debug APK) |
| `eafe209` | 2026-08-10 | F13 Wispr Android parity core |
| `5584152` | 2026-08-10 | F12 reliability (mic, focus, restart) |
| `a51b3e7` | 2026-08-10 | F11 continuous dictation |
| `021b066` | 2026-08-10 | **F10 Flow Bubble (NOT IME) — pivotal pivot** |
| `1e32bc3` | 2026-08-10 | F14 polish (bank hide, bubble modes, shake, pulse) |
| `37a4a1f` | 2026-08-10 | F12 UX foundation |
| `e5d0137` | 2026-08-10 | merge ponytail cut |
| `c13c195` | 2026-08-10 | HANDOFF truth after F12 UI + ponytail merge |
| `7d7d039` | 2026-08-10 | UI Drop 2 (Home layout, Menu items, bubble pulse) |
| `85560e9` | 2026-08-10 | UI Drop 1 (drawer hub + bubble text) |
| `8b6af31` | 2026-08-10 | spec for UI redesign Drop 1 |
| `162dcae` | 2026-08-10 | F14 merge doc |
| `946be80` | 2026-08-10 | Drop 3 STT perf + Wispr session insert (no raw dump) |
| **`ba78aeb`** | 2026-08-10 | **M3 product shell — bottom nav IA, dual skin tokens, research prefs** |

---

## 21. WHY open-flow wins the gap (019feb59:1393-1406, recap)

| Wedge winner | Why |
|--------------|-----|
| **FUTO Keyboard** | best polish, privacy loved; swipe/predict weaker than Gboard; voice "a bit slow" |
| **Notely Voice** | closest memo alt; F-Droid free but Play paywall angers users |
| **Our edge** | one app = bubble + memos + history + private FOSS. Beats stack of FUTO+Notely+Fossify+hope. |
| **Risk** | "another Whisper keyboard" if surface is just one job. Must win BOTH jobs. |
