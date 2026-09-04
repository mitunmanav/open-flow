# Wispr Flow Android — UI Teardown & Parity Matrix

Date: 2026-08-25 · Scope: bubble/overlay UX only (engine covered in wispr-teardown.md)
Sources: TechCrunch 2026-02-23, HotHardware 2026-02-23, wisprflow.ai/whats-new (v1.4.1→v2.2.4),
zackproser.com review, Computerworld/MUO Typeless pieces, Android Authority (Gboard Rambler).

## Their Android launch facts
- Launched 2026-02-23. Floating bubble over apps. Hold-to-dictate (PTT) or tap-start/tap-close-stop.
- Android 13+ only. Free unlimited during early access; Pro $12/mo planned.
- At launch they LACKED: learning dictionary, snippets, per-app punctuation/caps, uncommon-name
  context (HotHardware). Dictionary landed v1.4.1 (Mar 31) + context awareness same release.

## Their changelog highlights (Android pills)
| Ver | Date | Feature |
|-----|------|---------|
| 1.7.3 | Apr | Bubble auto-shrink after 5s idle (icon or tiny dot); auto-minimize in search fields; removed persistent notification; post-dictation copy button (10s auto-dismiss); opacity slider 20–100% |
| 1.88 | Jun | Shake-to-unsnooze active only while snoozed (accelerometer off otherwise); back-button behavior |
| 2.0.9 | Jul | Bubble self-recovery + system nudge; accessibility-permission recovery; multi-account isolation; Xiaomi/OnePlus battery/overlay fixes |
| 1.4.1 | Mar 31 | Dictionary; field-context continuation; banking pause 50+ apps; keep-screen-on during dictation; 5-min session warn + autosave; retry failed (audio preserved); home "service not running" card w/ one-tap fix |
| 2.2.x | Aug | Sign-in fix saga ends; report form keeps sending |

Rivals: Gboard Rambler (Gemini cleanup in keyboard, free, summer rollout) — keyboard-only, no
bubble, no learned dictionary, no RAW control. Typeless — keyboard app, 4k words/wk free,
copy-dialog outside fields, no cross-app overlay. Nothing Essential Dictate — device-bound.

## Parity matrix (open-flow verified refs)
| Their feature | Ours | Verdict |
|---|---|---|
| Floating bubble over apps | FlowAccessibilityService overlay | PARITY |
| Hold-to-dictate PTT | pushToTalk path (service:112/465/599) | PARITY |
| Tap start / stop | BubbleTapPolicy | PARITY |
| Auto-shrink idle → dot, search-field minimize | prefs.bubbleShrinkIdle/Dot/Search (BubbleSettings.kt:78–80) | PARITY (beat: 3 knobs) |
| Keep screen on while listening | win.view.keepScreenOn + FLAG (service:1833/1838) | PARITY |
| Opacity slider | prefs.bubbleOpacity | PARITY |
| Snooze gesture + shake-off accelerometer | drag-top snooze + PulseSchedule idle gate | BEAT (drag target vs their menu) |
| Banking pause | bank-package bubble hide | PARITY |
| Learning dictionary | DictationRepository.dictionary + SttBias at listen | BEAT (biasing feeds engine) |
| Field-context continuation | fieldPrefix capture + AppContextEngine category | PARITY (they read more text; we bias + tone) |
| Session warn + autosave | warn discs + save=true on fatal | PARITY |
| Copy after dictation | idle-bubble COPY action + copy-last broadcast | PARITY (persistent vs 10s chip) |
| RAW mode | rules-only routing / cleanup level | BEAT (per-insert choice) |
| Hinglish model | Sarvam codemix prewired + suggestion chip | PARITY (theirs native-tuned; ours API) |
| Retry failed w/ audio | raw text saved to history; NO audio-preserving retry | GAP-1 |
| Home "service dead" one-tap card | mic repair card only | GAP-2 |
| Bubble auto-recovery after OEM kill | boot receiver only; no runtime resurrection | GAP-3 |
| Language quick-switch on bubble | Settings-only | GAP-4 |

## Ranked UI attack plan (match-or-beat)
1. GAP-2 Home service-card — cheap, high trust value (their #1 support complaint).
   Pure policy + test: ServiceHealthCard.visible(serviceEnabled, batteryKilledAt?) → copy CTA.
2. GAP-4 language quick-switch in bubble long-press menu — pure LangMenu policy + wiring;
   beats them (they only have it on desktop bar).
3. GAP-1 retry-from-history needs stored audio — CaptureCap WAV already exists; persist last-N
   retry WAVs under keep/wipe rule, History row retry button. Medium lift, big "never lose words" story.
4. GAP-3 runtime resurrection — re-add overlay on a11y onServiceConnected after OEM kill +
  notification nudge if overlay refused. Needs careful loop-guarding.
5. Leapfrog (no one has): cleanup-level cycle chip on the bubble (RAW ↔ LIGHT ↔ FULL) —
  instant tone control without leaving the field.

## Sources
techcrunch.com/2026/02/23/wispr-flow-launches-an-android-app · hothardware.com/news/
wispr-flow-ai-dictation-app-android · wisprflow.ai/whats-new · zackproser.com/blog/
wisprflow-android-app-review · computerworld.com/article/4122901 · makeuseof.com/typeless-ai-
voice-typing-android · androidauthority.com/gboard-rambler-gemini-intelligence-3665653
