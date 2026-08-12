# Open Flow — Real UI Fix + Accurate Live Feature Proof Plan

> **For agentic workers:** superpowers:executing-plans / subagent-driven-development.  
> **Skills:** using-superpowers · systematic-debugging · TDD · verification-before-completion · using-git-worktrees · android-cli.  
> **Repo:** `/home/mitun/open-flow`  
> **Desktop evidence:** `C:\Users\Mitun Manav G Y\Desktop\Open-Flow\{apk,qa,docs,logs}`  
> **Device link:** USB only via `~/.local/bin/adb` → Windows `adb.exe` (no usbipd).

**Goal:** Fix real UI bugs and prove every feature with **industry-grade** device testing (not text-dump cosplay).

**Architecture:** One Activity + Compose routes; bubble + a11y STT; Room + FlowPrefs.

**Tech:** Kotlin, Compose M3, AccessibilityService, SpeechRecognizer, Room, adb / android-cli, optional Maestro later.

---

## 0. Postmortem (why prior “testing” was invalid)

| Bad practice we used | Industry rule we violated |
|----------------------|---------------------------|
| Dump UI and look for labels | **Assert after act** — presence ≠ feature works |
| Fixed `sleep()` between taps | Prefer **wait-until** element/condition (UI Automator / poll) |
| Tap by English text only | Prefer **stable ids / testTag / content-desc**; text is flaky |
| Left app via system Back / Settings | **Package guard** every step; abort if focus wrong |
| Vanity “43 PASS” | One PASS = action + **durable state** + evidence |
| Almost no fixes | FAIL → root cause → fix → retest same case |

**Locked PASS rule:**

```
ARRANGE (known state)
ACT     (user action)
ASSERT  layer 1: UI hierarchy / screenshot
ASSERT  layer 2: prefs (run-as) and/or Room (sqlite)
ASSERT  layer 3: logcat no FATAL for package
EVIDENCE → Desktop\Open-Flow\qa\<case>\
```

If any ASSERT fails → case FAIL → bug → fix → re-run **that case only** then smoke.

---

## 1. Web-confirmed accurate testing stack (ADB / Android)

Sources: Android UI Automator docs, ADB E2E guides, SharedPreferences/Room debug via `run-as`, Maestro/Appium comparisons, a11y testing notes.

### 1.1 Pyramid (use all three)

| Layer | Tool | What it proves |
|-------|------|----------------|
| L0 Unit | `./gradlew :app:testDebugUnitTest` | CourseCorrector, LayoutPrefs, FieldPolicy, export pure logic |
| L1 Instrumented (preferred for in-app UI) | Espresso / Compose UI Test / UI Automator in `androidTest` | Click Save Word → list shows row (no human) |
| L2 Device black-box | adb + uiautomator dump + run-as + logcat | Real phone, real a11y, real OEM quirks |
| L2b Optional | **Maestro** YAML flows | Less flake than raw sleep-scripts; good for regression later |
| L2c Optional | Appium | Overkill for single Android FOSS app now |

**Decision for this project:**  
- **Now:** harden **L2 adb harness** (phone already on USB) + **L0 unit** for pure bugs.  
- **Same sprint if time:** add **L1 Compose/UI Automator tests** for Dict/Snippet/History so “add word” is CI-proof.  
- **Not now:** full Appium stack.

### 1.2 Device prep (anti-flake — do first every session)

```bash
# Disable animations (UI Automator / industry standard)
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# Stay awake while charging
adb shell settings put global stay_on_while_plugged_in 3

# One package for QA day
PKG=app.openflow.debug
# uninstall other if confusing: adb uninstall app.openflow
```

### 1.3 Wait, don’t sleep (pattern)

```text
for i in 1..30:
  dump hierarchy
  if predicate(text/id/desc): break
  sleep 0.2s
else: FAIL timeout
```

Never fixed 2s sleep as the only sync. (UI Automator `Until.hasObject` is the library form of this.)

### 1.4 Triple assert (accurate feature proof)

| Feature | UI assert | Data assert | Side assert |
|---------|-----------|-------------|-------------|
| Save Word | row text on Dict | `run-as` Room/dictionary or UI after kill/relaunch | logcat clean |
| Cleanup chip | selected chip (if exposed) | `shared_prefs` cleanup_level | — |
| Privacy wipe | card selected | pref retention=wipe_24h | — |
| History search | non-match hidden | — | — |
| Dictate insert | field text | last session prefs + Room row | logcat STT |

**Debug-only data probe:**

```bash
adb shell run-as app.openflow.debug ls shared_prefs
adb shell run-as app.openflow.debug cat shared_prefs/<file>.xml
adb shell run-as app.openflow.debug ls databases
adb shell run-as app.openflow.debug sh -c "sqlite3 databases/openflow.db 'SELECT text FROM dictations LIMIT 5;'"
```

(Release non-debuggable cannot `run-as` — **QA on debug APK**.)

### 1.5 Locators (make app testable)

Industry: **resource-id / testTag > content-desc > text**.

Compose today is mostly text-only → flaky. Plan adds:

```kotlin
// Example on primary actions
Modifier = Modifier.testTag("dict_save_word")
// contentDescription already on some icons
```

Tags for: bottom tabs, Save Word, Add Snippet, Export, practice field, cleanup chips, privacy options, bubble shapes.

### 1.6 Accessibility / bubble testing (honest limits)

- Physical device required (a11y services).  
- Enabling service: `settings put secure enabled_accessibility_services` (OEM may block).  
- **Cannot fully fake human speech via adb** for system SpeechRecognizer quality.  
- Dictation E2E: automate up to “listening”; **Mitun speaks once**; harness asserts field/history/log.  
- Optional later: debug BroadcastReceiver to inject “fake final STT” for CI-only (not replace human once).

### 1.7 Concurrent observation

| Stream | Purpose |
|--------|---------|
| logcat filtered package | crashes, STT errors |
| screencap before/after | visual UI bugs |
| hierarchy dump after act | structural assert |
| scrcpy (optional, Mitun screen) | human watches live |

### 1.8 Harness exit code

Script must `exit 1` if any case FAIL. No “mostly pass” summary without listing fails first.

---

## 2. Bug inventory (fix these)

### P0
| ID | Bug | Fix |
|----|-----|-----|
| P0.1 | Dual packages confuse QA | QA only `app.openflow.debug`; uninstall release or label Home “DEBUG” |
| P0.2 | Setup/mic/a11y chips wrong package | isRunning + mic for **this** applicationId |
| P0.3 | Dictation E2E unproven | Matrix G + fix if insert/prefix fails |
| P0.4 | Mic not granted on fresh debug | install script `pm grant` + in-app request |

### P1 UI
| ID | Bug | Fix |
|----|-----|-----|
| P1.1 | Double titles (TopBar + body H1) | Single title policy |
| P1.2 | “Drawer extras” copy | Rename Menu visibility |
| P1.3 | Home module order | setup → test → keys → stats → recent |
| P1.4 | Missing testTags | Add tags for harness |
| P1.5 | Enable bubble → system Settings no guidance | Toast / copy “come back when enabled” |
| P1.6 | Back left app | Done (`BackHandler` 1873450) — re-verify in suite |

### P2
Touch targets, strings.xml, full MainActivity split — after P0/P1 green.

---

## 3. Live feature matrix (action tests)

### A Shell
A1 cold start · A2 tabs · A3 Back stays in app · A4 no FATAL

### B Home
B1 type practice field (UI shows text) · B2 cleanup chip → pref · B3 open in-app bubble settings · B4 chips match real a11y+mic

### C Dictionary (mutate)
C1 add word → row + persist kill/relaunch · C2 delete → gone

### D Snippets
D1 add · D2 delete

### E History
E1 row exists (dictate or debug seed) · E2 search filters · E3 copy tap · E4 export sheet · E5 delete

### F Settings mutate
F1 privacy pref · F2 bubble shape pref · F3 appearance pref · F4 home layout hide module

### G Dictation
G1 a11y enabled · G2 mic · G3 overlay/service · G4 listen · G5 insert (human speech) · G6 history · G7 logcat

---

## 4. Phases / worktrees

### Phase 0 — Session baseline (no product code)
1. Animations off; stay-on; adb devices.  
2. Install only debug APK → `Open-Flow\apk\`.  
3. Grant mic/notifications; enable a11y for debug service.  
4. Start logcat → `Open-Flow\logs\session.log`.  
5. Create empty matrix checkboxes → `Open-Flow\docs\LIVE-MATRIX.md`.

### Phase 1 — worktree `fix/ui-titles-tags`
**Files:** `AppShell.kt`, `MainActivity.kt`  
1. Kill double titles.  
2. Fix Drawer → Menu copy.  
3. Add `testTag`s on critical controls.  
4. Device: screenshot each main screen once.  
5. Commit `fix(ui): single titles, menu copy, testTags`.

### Phase 2 — worktree `fix/home-setup-state`
**Files:** HomeHub / service running check  
1. Accurate setup chips for this package.  
2. Default module order.  
3. B1–B4 pass.  
4. Commit `fix(ui): home setup truth`.

### Phase 3 — worktree `test/live-harness` (or `scripts/`)
**Create:** `scripts/live_feature_test.py`  
Implements:
- package focus guard  
- wait-until poll  
- AAA per case  
- prefs/Room assert  
- shots to `Open-Flow\qa\<id>\`  
- nonzero exit on fail  
Run full C–F matrix; paste results to `LIVE-MATRIX.md`.

### Phase 4 — worktree `fix/dictation-e2e` (only if G fails)
**Files:** `FlowAccessibilityService.kt`, `SttEngine.kt`  
Root-cause only; human speak once; re-run G.

### Phase 5 — Ship
assembleDebug → Desktop apk; HANDOFF truth table; optional release APK.

**Parallelism:** max 1 agent on MainActivity; never parallel same file.

---

## 5. Harness skeleton (must implement)

```python
# scripts/live_feature_test.py — conceptual
# for each case:
#   assert_focus(PKG)
#   arrange()
#   shot(f"{id}-before")
#   act()
#   wait_until(predicate, timeout=8)
#   shot(f"{id}-after")
#   assert_ui(predicate)
#   assert_prefs(key, value)   # run-as
#   assert_no_fatal()
#   record PASS/FAIL
# sys.exit(1 if any FAIL else 0)
```

**adb input notes:**
- `input text` is limited (no spaces well → use `%s`); prefer UI Automator `setText` when possible or clipboard + paste for long strings.  
- For Compose fields: tap field → `adb shell input text` or `cmd clipboard` + paste keyevent.

---

## 6. Success criteria

- [ ] Animations disabled on device for suite.  
- [ ] Single QA package: debug.  
- [ ] Zero double titles on tabs + settings children.  
- [ ] testTags on Save Word, Add Snippet, tabs, Export, practice field.  
- [ ] Dict add/delete: UI + persist after process death.  
- [ ] Snippet add: UI proof.  
- [ ] Prefs mutations: run-as XML proof.  
- [ ] History search/copy: proof.  
- [ ] Dictation G: PASS or explicit BLOCKED with log.  
- [ ] Harness exit code fails CI-style on any FAIL.  
- [ ] `Desktop\Open-Flow\docs\LIVE-MATRIX.md` complete with evidence paths.  
- [ ] Unit tests still green.

---

## 7. Out of scope

Emulator · usbipd · Appium farm · full redesign · F16 recorder · Play signing.

---

## 8. Sources (research)

- Android Developers — UI Automator (predicate wait, macrobenchmark)  
- UI Automator guides — no sleep; disable animations; prefer resource ids  
- ADB E2E (uiautomator dump /dev/tty + xpath)  
- SharedPreferences / Room inspect via `adb shell run-as` (debug)  
- Maestro vs Appium — Maestro lower flake for YAML flows (optional upgrade)  
- A11y service — physical device; limited pure-adb control of speech

---

## 9. GO order

1. Phase 0 baseline  
2. Phase 1 UI titles + testTags (biggest “UI bugs everywhere” win)  
3. Phase 2 home truth  
4. Phase 3 live harness + run matrix  
5. Phase 4 dictation only if fail  
6. Phase 5 ship to Desktop\Open-Flow  

**When Mitun says GO:** execute this plan; no vanity pass counts.
