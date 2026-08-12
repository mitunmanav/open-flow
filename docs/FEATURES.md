# Open Flow vs Wispr Flow Android (audit 2026-08-12)

**Product lock:** Bubble + a11y. **Not** IME. en-US only. Local FOSS. No APK INTERNET. No tiny model this slice.

**Wispr Android = cloud STT + account.**  
**Open Flow = on-device SpeechRecognizer + local rules.**

## Legend

| Tag | Meaning |
|-----|---------|
| **have** | On main, device-checked or unit-tested |
| **partial** | Exists, weaker than Wispr Android |
| **add** | Local, we will ship in F20 |
| **skip** | Never / not product |
| **later** | After F20 (F19 STT extras, optional model) |

## Skip (do not build)

Account · billing · cloud retry · Command Mode / Transforms · Notetaker · Scratchpad (Wispr Android has **no** Notes) · 100 languages · cloud sync · team / Insights · tiny on-phone LLM · F19 `BIASING_STRINGS` (next after this)

## Map

| # | Feature | Status | Truth |
|---|---------|--------|--------|
| A1 | Insert any field | **have** | a11y SET_TEXT + clipboard fallback |
| A2 | Auto punct | **partial** | API33 format + rules. Not cloud. |
| A4 | Course correct | **partial** | Marker rules Medium+. Not AI. |
| A5 | One insert on stop | **have** | SessionText + mergeSession |
| B1 | Drag | **have** | |
| B2 | Size | **have** | 0.7–1.15× + full/compact/dot pref |
| B3 | Opacity | **have** | |
| B4 | Idle shrink 5s | **have** | F20 visual compact after 5s |
| B5 | Battery setup | **have** | F20 Home Battery chip |
| C1 | Cancel | **have** | |
| C2 | Continuous | **have** | Restart loop |
| C3 | Copy last | **partial** | History + home. No bubble chip / notif action |
| D1–D2 | Dictionary | **have** | Room + UI. No import file |
| D3 | Overlay | **have** | TYPE_ACCESSIBILITY_OVERLAY |
| F1–F2 | Filler / lists | **have** | Rules |
| H1–H2 | History | **have** | + share exists |
| H3 | Hold-to-talk | **have** | |
| H4 | Setup cards | **partial** | a11y + mic. No battery |
| L1 | en-US | **have** | Locked. Ignore other langs |
| M1 | Mic flow | **have** | |
| N1 | Notifications | **have** | F20 saved + Copy last + service-stopped |
| O1 | Offline | **have** | Ours |
| P1 | Local privacy | **have** | |
| P3–P4 | Hide password / bank | **have** | |
| S1 | Snippets | **have** | |
| S2–S3 | Snooze + shake | **have** | |
| S4 | Stats | **have** | |
| S5 | Styles | **have** | Global only |
| S5b | Style per app | **have** | F20 AppStylePolicy personal/work/email |
| C0 | Cleanup levels | **have** | High ≠ Wispr AI High |
| C0b | Spoken cmds | **have** | |
| T1 | Tap bubble | **have** | |
| W1 | Waveform | **have** | F20 RMS 4-bar |
| W2 | Any field | **have** | |
| F18 | IME park + small listen | **have** | Device 2026-08-12 |
| E0 | Export | **partial** | Share from history |
| Keep screen on | **have** | F20 while listening |
| Session time warn | **have** | F20 4:30 warn / 5:00 stop |
| Copy chip after stop | **have** | F20 10s Copy |
| Service-died notif | **have** | F20 |

## F20 ship (this slice)

1. Idle shrink after 5s (visual compact; tap restores)
2. Copy chip 10s after insert + Copy last on notif
3. Keep screen on while listening
4. Session warn 4:30 / stop 5:00
5. RMS waveform bars
6. Battery setup chip
7. Per-app style (personal/work/email → local style)
8. Service-stopped notification

Then **F19** STT bias. Model still later.
