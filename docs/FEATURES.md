# Open Flow vs Wispr Flow Android (feature map)

**Research:** 2026-08-10  
**Baseline truth:** `docs/BASELINE.md` (full history + what shipped)  
**Sources:** Wispr docs, Play listing, launch notes, Help Center + live Open Flow code.

**Wispr Android = cloud STT + account.**  
**Open Flow = on-device first, no account required.**

---

## Status legend

| Tag | Meaning |
|-----|---------|
| **have** | In main, testable |
| **F14+** | Planned next |
| **skip** | Never / N/A for FOSS local |
| **partial** | Some coverage only |

---

## A–Z Wispr Flow Android vs Open Flow

| # | Feature | Wispr Android | Open Flow | Status |
|---|---------|---------------|-----------|--------|
| A1 | Accessibility insert into any app | Yes | Yes | **have** |
| A2 | Auto punctuation | Cloud | Local rules + API33 format | **have** |
| A4 | Self / course correction | Cloud AI | Local CourseCorrector on stop | **have** |
| A5 | No raw chunk dump into field | Polish then insert | Session accumulate → insert once on stop | **have** |
| A3 | Account / login / billing | Yes | Never required | **skip** |
| B1 | Bubble drag reposition | Yes | Yes | **have** |
| B2 | Bubble size | Yes | Settings 0.7–1.15× | **have** |
| B3 | Bubble opacity | Yes | Settings 20–100% | **have** |
| B4 | Bubble shrink idle / dot | Yes | — | **have** |
| B5 | Battery optimization setup | Yes | Setup card (basic) | **partial** |
| C1 | Cancel while dictating | Yes | Stop tap | **have** |
| C2 | Continuous / unlimited dictation | Yes | Restart loop | **have** |
| C3 | Copy last transcript | Yes | Yes | **have** |
| D1 | Dictionary custom words | Yes | Room + apply | **have** |
| D2 | Dictionary edit/delete | Yes | UI | **have** |
| D3 | Display over other apps | Yes (a11y) | a11y overlay | **have** |
| F1 | Filler removal | Yes | Local filter | **have** |
| F2 | Numbered list format | Yes | Local rules | **have** |
| H1 | History by day | Yes | Room timeline | **have** |
| H2 | History copy/delete | Yes | UI | **have** |
| H3 | Hold-to-talk | Yes | Long-press PTT | **have** |
| H4 | Home setup permission cards | Yes | Chips + buttons | **have** |
| L1 | Language select | Yes | **en-US only** (locked) | **have** |
| L2 | 100+ languages cloud | Yes | Deferred — not product now | **skip** |
| M1 | Mic permission flow | Yes | Reliability labels | **have** |
| N1 | Notifications | Yes | — | **F15** |
| O1 | Offline dictation | **No** | **Yes (ours)** | **have** |
| P1 | Privacy local store | Toggle | Always default | **have** |
| P2 | Private cloud sync | Opt-in | Later | later |
| P3 | Password/phone field hide | Yes | FieldPolicy | **have** |
| P4 | Banking app hide | Yes | Package denylist | **have** |
| R1 | Report issue | Yes | GitHub later | **skip** |
| S1 | Snippets voice shortcuts | Yes | Room + expand | **have** |
| S2 | Snooze bubble 10 min | Yes | Yes | **have** |
| S3 | Shake to unsnooze | Yes | Sensor | **have** |
| S4 | Stats (words, streak) | Yes | Local counters | **have** |
| S5 | Writing styles | Yes | Formal/Casual/Very casual/Excited/Custom (local rules) | **have** |
| S5b | Style per app category | Yes | Global styles only | **partial** |
| C0 | Cleanup levels None/Light/Medium/High | Yes (AI High) | Local rules stages; High ≠ LLM | **have** |
| C0b | Spoken punct / backspace / new line | Yes | PhraseMap + VoiceCommands | **have** |
| S6 | Settings nav | Drawer | Bottom nav 5 tabs | **have** |
| T1 | Tap bubble toggle | Yes | Yes | **have** |
| T2 | Transcript retry cloud | Yes | N/A local | **skip** |
| W1 | Waveform while recording | Yes | Basic bubble status | **have** |
| W2 | Any text field (WhatsApp…) | Yes | Yes | **have** |
| UI | Dark mode theme | — | system/light/dark | **have** |
| R0 | Memo audio recorder | Notes limited | Not built | **F16** |
| E0 | Export share history | — | Not built | **F15** |

**Not on Wispr Android (skip or later):** Scratchpad/Notes depth, IAP, Notetaker meetings.

---

## Open Flow ship order (current)

1. **Done:** bubble · pipeline (no AI) · modern brutal UI · en-US lock  
2. **F15** export/share polish + optional notifs (export already partial)  
3. **F16** memo recorder (audio + transcript)  
4. **Later** optional on-device Whisper · sync  

Privacy forever: no forced account, no cloud STT default.

Research: `docs/research/` · small-features map: `docs/WISPR-SMALL-FEATURES-MAP.md`  
Baseline: `docs/BASELINE.md`
