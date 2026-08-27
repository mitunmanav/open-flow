# Testing loop (internal)

Setup only. Not the public site. Not a claim that every flow is tested.

## Goal

One **fast, reliable emulator**, then a **risk-based gate** around it.

Do **not** invent dozens of AVDs. Do **not** mark skipped work as PASS.

## Emulator (this laptop)

WSL has **no `/dev/dri`**. WSL `-gpu host` crashes. SwiftShader is CPU-slow.

**Use Windows AVD `of_win` + Intel Arc `-gpu host`.**

| Piece | Use |
|-------|-----|
| Start | `of-emu` or `scripts/qa/emu-up.sh up` |
| AVD | `of_win` on the **Windows** SDK |
| GPU | `-gpu host` (Intel Arc) |
| Boot | Quick Boot snapshot (default). Do **not** pass `-no-snapshot-load` |
| adb | Windows `adb.exe` (WSL SDK adb is a wrapper — `scripts/qa/wrap-adb.sh`) |
| android CLI | needs `scripts/qa/adb-bridge.sh` (WSL `:5037` → Windows `adb -a`) |
| Ready | `adb devices` → `emulator-5554` and `getprop sys.boot_completed` = `1` |

**Never**

- `android emulator start of_test` (WSL AVD)
- SwiftShader / lavapipe for daily test
- `pkill qemu` to “restart”
- A second qemu in WSL while `of_win` is up (fights adb 5554)
- Linux SDK `adb` as a real daemon (empty device list; can steal Windows `:5037`)

**Why**

- Windows: WHPX + Arc host GLES. Fast.
- WSL `of_test`: no host GL. Crash or crawl.
- Snapshots need hardware GL. Software GL makes them flaky.
- Sources:
  - [GPU modes](https://developer.android.com/studio/run/emulator-acceleration#command-gpu)
  - [Snapshots / Quick Boot](https://developer.android.com/studio/run/emulator-snapshots)
  - [Emulator CLI](https://developer.android.com/studio/run/emulator-commandline)
  - [Test CLI](https://developer.android.com/studio/test/command-line)

`android emulator list` only sees the WSL AVD. Ignore it. Device commands (`android install`, `android run`, `android layout`, `android screen`) work **after** wrap-adb + of-emu.

`of_win` uses 16 KB pages (`getconf PAGE_SIZE` = 16384). Native .so files must LOAD-align `2**14`. NDK **28.2.13676358** + `graphics-path:1.1.0`. Source: [16 KB page sizes](https://developer.android.com/guide/practices/page-sizes).

```
of-emu                 # reuse if up; else start + wait
of-emu status
of-emu stop            # adb emu kill
scripts/qa/prep.sh     # anim=0, stay awake
```

Cold first boot ~1–2 min. Next starts = Quick Boot. Overlay + Accessibility still need granting.

## What already exists

| Layer | Where | Gate |
|-------|--------|------|
| Unit (Truth, JVM) | `app/src/test/` | CI + `./gradlew :app:testDebugUnitTest` |
| Lint | `:app:lintDebug` | CI |
| Debug APK | `:app:assembleDebug` | CI |
| Golden STT/cleanup | `GoldenCorpusTest` | unit |
| Compose launch smoke | `androidTest` `AppLaunchTest` | device gate |
| Room/prefs smoke | `androidTest` `DataSmokeTest` | device gate |
| testTags | `UiPathTest` pins tags | unit |

**Not in repo yet (later, risk order):** overlay bubble E2E, a11y enable, live STT, visual baselines, perf traces, extra API AVDs.

**Won’t add just to look busy:** Hilt, Robolectric, Jacoco, Dropshots, extra AVDs. Stack stays JUnit4 + Truth + Compose UI + Espresso **3.7.0** + `am instrument`.

Espresso 3.7.0 is required on `of_win` (API 37): older Espresso reflects `InputManager.getInstance`, which is gone. Source: [Espresso 3.7.0](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0).

## Gate

```
scripts/qa/wrap-adb.sh          # once / after SDK platform-tools bump
scripts/qa/gate.sh              # unit + lint + debug apk + install + instrument + crash
scripts/qa/gate.sh --quick      # skip unit/lint (CI already ran them)
scripts/qa/gate.sh --release    # also :app:assembleRelease + bundleRelease + play-check
bash scripts/qa/play-check.sh   # 17 static Play checks (target36, 16KB, AAB, store)
bash scripts/qa/functional-check.sh # 6 deep-link + footer + nav checks (post-gate)
```

Reports: `.scratch/qa/<stamp>/report.txt` (gitignored).

Required stages: WRAP-ADB, EMU, UNIT (unless --quick), LINT (unless --quick), BUILD-DEBUG, BUILD-RELEASE+ BUNDLE + PLAY-CHECK (only --release), APK-INFO, INSTALL, INSTRUMENT, CRASH.

Skipped on purpose (printed, not PASS): functional core, visual, a11y audit, perf, extra matrix.

Instrumented tests use **Windows adb** `am instrument`, not `connectedDebugAndroidTest` (that would use Linux adb unless wrap is in place; wrap makes both work).

## E2E — what to test (2026, risk-based)

**Pyramid** — unit fastest (500–2000, 5–30s), integration (50–200, 1–5m), E2E top (10–30 journeys, 10–30m). Keep E2E <30m; move lower-value tests down the pyramid. [Mobile E2E guide](https://mobileapp.wiki/en/testing/e2e-testing-guide) · [Pie 2026 checklists](https://pie.inc/blog/mobile-app-testing-guide/)

**Framework map for this repo** — Espresso in-process (fast, sync with UI thread; `IdlingResource` for custom async; no `sleep`); Compose `createAndroidComposeRule` + `onNodeWithTag` + shared `testTag` constants; UI Automator `uiAutomator { onElement { } }` for cross-app/system dialogs; MAESTRO/Journeys YAML intent-based planned for smoke — vision finds by label not `R.id`. Assign explicit `testTag` in Compose; disable animations on test devices; use `waitUntil` not `Thread.sleep`. [Espresso](https://developer.android.com/training/testing/espresso) · [UI Automator 2.4](https://developer.android.com/training/testing/other-components/ui-automator) · [E2E with Compose 2026](https://itnext.io/e2e-testing-for-android-with-jetpack-compose-a-practical-guide-47a152aff956) · [Espresso vs UI Automator 2026](https://johal.in/testing-android-apps-with-espresso-and-ui-automator)

**Pick 5–8 critical journeys** (by revenue + user impact; test those well on real devices each release). [User journey testing](https://www.drizz.dev/post/user-journey-testing) · [Enacton master checklist](https://www.enacton.com/blog/mobile-app-testing-checklist/)

For **Open Flow** (wispr-flow dictation, bubble + a11y + snippets):

1. Install → first launch → walkthrough → Setup wizard (a11y + mic + battery seen) — fresh/upgrade/abandoned states.
2. Permission grant flows — overlay `SYSTEM_ALERT_WINDOW`, `RECORD_AUDIO`, accessibility enable/disable, revoke-later.
3. Bubble lifecycle — show/hide, drag/park above IME, visibility gates (`insideOwnApp`, bank packages).
4. Dictation loop — STT → polish → insert into editable field (prefix capture, `insertDictation`); deep link `open_history` cold + warm.
5. Persistence — history search/filter/export (MD/Plain/JSON/raw), dict word add/search, snippet trigger expansion, style per-category save/discard.
6. Settings hub — all 9 sub-screens in gate; prefs persist across `force-stop` + rotation + `ON_RESUME`.
7. Lifecycle & interrupts — `background/foreground` 5m, `force-stop`→wizard re-entry, incoming call/notification/permission dialog mid-flow.
8. Compatibility — landscape/portrait, dark/light/system theme, font-scale 130%–200%, gesture vs 3-button nav.

**Pre-release checklist (shrink to risk map); evidence per item: screenshots + `ui-dump` + logcat.** [NextPage 2026 checklist](https://www.nextpageit.com/blog/mobile-app-testing-checklist) · [qa::checklist mobile](https://qa-checklist.dev/qa-guides/mobile-app-testing-checklist) · [Functional checklist](https://www.nextpageit.com/blog/functional-testing-checklist-web-mobile-apps)

- Build/install, onboarding, login/auth (if any), permissions allowed/denied/revoked, search/filter, forms/validation, empty/loading/error states.
- Deep link cold vs warm (`--ez open_history true`), push/notification tap, app-to-web handoff.
- Offline/slow/airplane — queued action + sync on return; network WI-FI↔LTE handoff mid-flow.
- Background/foreground, rotation, multi-window/PIP if supported.
- Accessibility — TalkBack traversal, `contentDescription`, min 48dp touch, contrast WCAG 2.2, font-scale no clip. [ComposeProof a11y](https://composeproof.dev/)
- Performance — cold/warm start, LazyColumn scroll jank, memory stable over 10m, battery (no `keepScreenOn` leak). Profile with `Macrobenchmark`/`Baseline Profile` via UI Automator. [UI Automator perf](https://developer.android.com/training/testing/other-components/ui-automator)
- Security — tokens in EncryptedSharedPrefs/Keystore, no `log` of `text`, API `https` + `network_security_config`, fileprovider `grantUriPermissions`.
- Analytics/crash — event once + consent, crash payload symbolicated + no PII.
- Store readiness — versionCode, screenshots, privacy labels, review notes, staged rollout + rollback plan.

**Data hermetics** — named test accounts with reset via API; or fresh account per journey; never shared pool without cleanup (drift looks like bug). Reset `openflow_prefs.xml` + DB between suites.

**Anti-flake** — disable animations (`prep.sh` `anim 0`), retry 1× then quarantine >1.4% flake (Espresso avg 1.4%; UI Automator 3.2% — [AndroidDocs bench 2026](https://androiddocs.com/best-android-testing-frameworks/)), sharding, ordered rules, hermetic `MockWebServer` + Page Object (`LoginScreen.enterUsername` vs raw `onNodeWithTag`), `waitUntilExists` not sleep.

## UI/UX feedback per phase (not after launch)

Every gate phase captures UI/UX evidence and feeds **next** phase — see [Pie metrics MTTD/MTTR](https://pie.inc/blog/mobile-app-testing-guide/) + [VLM-Fuzz on complex layouts](https://link.springer.com/article/10.1007/s10664-026-10816-4).

| Gate phase | Capture | UX signal → next fix |
|---|---|---|
| `prep.sh` + `gate.sh` | `adb screen capture`, `android layout --pretty` dumps, `logcat -b crash` | clipped fine-print behind bottom bar (Home `padding` bug), touch target <48dp, missing `contentDescription` |
| Instrumented smoke | `createAndroidComposeRule` + `onNodeWithTag` + `waitUntil` | walkthrough vs home race, search-filter debounce, empty state copy |
| Functional core (bubble/a11y loop) | bubble position dump + `dumpsys accessibility` | drag parks above IME, `isRunning` poll 2s stale, battery dialog copy |
| Visual | Paparazzi/Roborazzi golden + animated `Touch Robot` / `paparazzi.gif()` | brutalism spacing (`GAP` 12, `GAP_LG` 16 vs `88dp` bottom clearance), dark-mode `luminance` legibility |
| A11y audit | `accessibility-checker` (touch 48dp, contrast) + TalkBack traversal + 175% font-scale | address trigger overlap RTL, font-scale clip on snippet card |
| Perf | Macrobenchmark (TTFD, frame timing) + Profiler memory/battery | `applyRmsPulse` scale leak `keepScreenOn`, Baseline Profile CUJs |
| Matrix/stress (later) | network drop/battery low/locale RTL/rotation simulation × emulator | offline queue lost, `Lifecycle` observer missing `ON_RESUME` |

**How to act on signal** — triage by severity (Blocker→backlog; 4 levels with owner). File with screenshot + layout JSON + repro steps (intent extra, prefs dump, `adb shell run-as … cat shared_prefs/...`). Verify fix on same `of_win` device before next gate — no blind refactors of state machines (AGENTS.md).

**Tooling that already exists for the loop** — `android screen capture`, `android layout`, `scripts/qa/gate.sh` reports `.scratch/qa/<stamp>/`, `android docs search/fetch`, CI lint+unit + `bundleRelease` + `play-check.sh` (17 PASS). `scripts/qa/functional-check.sh` now 6/6 PASS (deep-link cold/warm + footer 88dp + Home/nav).

## Risk order (now → next)

1. Crashes / ANR (logcat + crash buffer) — gate PASS.
2. Core dictation (needs a11y + overlay + recognizer) — `open_history` warm fixed `singleTop`.
3. Accessibility service bind — PASS (poll + `ON_RESUME` refresh).
4. Text insert — `prefix` fix, `autoLearn` guard.
5. Speech recognition — golden corpus F1 unchanged.
6. Bubble UI — `BUBBLE` + `INSIDE_OWN_APP` gates; bottom-bar `88dp` anti-clip fix.
7. Data loss (prefs/db) — smoke PASS; needs rotation stress.
8. Visual regression — add Paparazzi goldens + animated diff.
9. Performance — add Baseline Profile + cold start gate.
10. Edge cases / env stress — VLM-guided fuzz later.

Cleanup/STT scoring stays **golden corpus F1**. Do not invent a second score.

## Matrix

**Now:** one AVD — `of_win`, API 37 Play, 1080×2400 @ 420, x86_64.

**Later if a bug is API/size specific:** one more AVD or a real phone. Not a farm.

## Release decision

A build is PASS only when required gate stages PASS.

CI on GitHub = unit + lint + debug APK + authorship + secret scan. **No emulator on ubuntu-latest.** Device gate is this laptop.

## Commands (android-cli)

```
of-emu
android info
android layout --pretty
android screen capture -o .scratch/qa/screen.png
android install --device emulator-5554 --apks app/build/outputs/apk/debug/app-debug.apk
```
