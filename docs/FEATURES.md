# Open Flow vs Wispr Flow Android (feature map)

Research date: 2026-08-10. Sources: Wispr docs, Play listing, launch notes, Help Center.

**Wispr Android = cloud STT.** Open Flow = **on-device first**, no account required.

---

## A–Z Wispr Flow Android (today)

| # | Feature | Wispr Android | Open Flow | Phase |
|---|---------|---------------|-----------|-------|
| A1 | Accessibility insert into any app | Yes | **Have** | done |
| A2 | Auto punctuation | Yes (cloud) | Local rules | **F13** |
| A3 | Account / login / billing | Yes | **Never required** | skip |
| B1 | Bubble drag reposition | Yes | Partial | **F13** |
| B2 | Bubble size (0.7–1.15×) | Yes | Settings | **F13** |
| B3 | Bubble opacity (20–100%) | Yes | Settings | **F13** |
| B4 | Bubble shrink idle / dot / search | Yes | Settings | **F14** |
| B5 | Battery optimization setup card | Yes | Setup card | **F13** |
| C1 | Cancel while dictating | Yes | Stop tap | have |
| C2 | Continuous / unlimited dictation | Yes | Restart loop | have |
| C3 | Copy last transcript | Yes | Action | **F13** |
| C4 | Copy button after dictate | Yes | **F13** |
| D1 | Dictionary custom words | Yes | Room + apply | **F13** |
| D2 | Dictionary search/edit/delete | Yes | UI | **F13** |
| D3 | Display over other apps | Yes (a11y overlay) | a11y overlay | have |
| F1 | Filler removal (um/uh/like) | Yes | Local filter | **F13** |
| F2 | Formatting numbered lists | Yes | Local rules | **F13** |
| H1 | History by day | Yes | Room timeline | **F13** |
| H2 | History copy/delete | Yes | UI | **F13** |
| H3 | Hold-to-talk (long press) | Yes | **F13** |
| H4 | Home setup permission cards | Yes | Improve | **F13** |
| L1 | Languages multi-select | Yes | STT locale picker | **F13** |
| L2 | 100+ languages (cloud) | Yes | OS packs offline | partial |
| M1 | Mic permission flow | Yes | Have + reliability | have |
| N1 | Notifications | Yes | Optional later | F15 |
| N2 | Numbered list auto format | Yes | Local | **F13** |
| O1 | Offline dictation | **No** | **Yes (ours)** | have |
| P1 | Privacy mode (local store) | Toggle | Default always | have |
| P2 | Private cloud sync | Opt-in | Opt-in later | F16 |
| P3 | Password/phone field hide | Yes | FieldPolicy | have |
| P4 | Banking app hide | Yes | Package denylist | **F14** |
| R1 | Report issue | Yes | Skip / GitHub | skip |
| S1 | Snippets voice shortcuts | Yes | Room + expand | **F13** |
| S2 | Snooze bubble 10 min | Yes | **F13** |
| S3 | Shake to unsnooze | Yes | Sensor | **F14** |
| S4 | Stats (words, streak, WPM) | Yes | Local counters | **F13** |
| S5 | Style per app category | Yes | Local styles | **F14** |
| S6 | Side drawer settings | Yes | Nav | **F13** |
| T1 | Tap bubble toggle listen | Yes | Have | done |
| T2 | Transcript retry (cloud) | Yes | N/A local | skip |
| W1 | Waveform while recording | Yes | Bubble anim | **F13** |
| W2 | WhatsApp/etc any text field | Yes | Have | done |

**Not on Wispr Android (skip or later):** Scratchpad/Notes (Wispr says not on Android yet), in-app IAP, Notetaker meetings.

---

## Open Flow ship order

1. **F13 wispr-parity-core** — post-process text, dictionary, snippets, history, long-press, bubble prefs, snooze, stats, bottom nav  
2. **F14 polish** — style, bank denylist, shrink modes, shake  
3. **F15 notifications / export share**  
4. **F16 opt-in sync**  

Privacy forever: no forced account, no cloud STT default.
