# Wispr Flow teardown → open-flow attack plan

Research dossier. Sources listed at the end. Facts verified Aug 2026.

## How Wispr Flow works

```
Mic → 16kHz PCM16 mono → streaming WebSocket STT
    → fine-tuned Llama cleanup model (multi-step chain,
      Baseten + TensorRT-LLM on AWS dedicated deployments)
    → polished text inserted at cursor via AccessibilityService
```

- **Cleanup brain**: fine-tuned Llama, self-hosted. p99 end-to-end <700ms;
  Llama generates 100+ tokens in <250ms (TensorRT-LLM).
- **Context awareness**: reads foreground app + screen content to pick tone/format.
  On desktop this included screenshots — caused a viral privacy backlash.
- **Android app** (launched Feb 23, 2026): floating bubble + AccessibilityService
  insert. Not a keyboard. Min Android 13. Account sign-in required before the
  bubble appears.
- **Bubble**: appears only when a text field focuses. Tap = start/stop,
  long-press = push-to-talk. Sizes 0.70/0.85/1.00/1.15x, opacity presets,
  shrink-idle / shrink-dot / shrink-in-search.
- **Snooze**: drag to bottom-edge drop target = 10-min snooze; shake unsnoozes.
- **Survival**: auto-restart at boot/unlock/app-update; persistent non-dismissible
  notification; per-OEM battery-exemption wizard (Samsung never-sleep list,
  Xiaomi autostart).
- **Sessions**: time-capped to protect battery.
- **Languages**: 100+; dedicated Hinglish model (Latin-script Hindi-English mix).
- **Offline**: none. Cloud only.

## Their verified weaknesses

1. 75+ outages in 6 months (StatusGator); one ~6-day capacity incident (June 2026).
2. Trustpilot 2.7/5 — "day-two drop": trial great, paid degrades.
3. Forced rewrite — no true RAW mode.
4. Noise fragility — "built for silent offices."
5. Privacy scars — screenshots, idle phoning-home, training defaults.
6. Free tier walls: 2,000 words/wk desktop. Pro $12/mo annual.
7. Desktop client bloat: ~800MB RAM idle, 8% CPU, 8–10s startup.
8. $81M raised, ~$700M valuation — they must grow fast; we can stay lean.

## open-flow vs Wispr

| Capability | Wispr | open-flow |
|---|---|---|
| Works offline | NO | YES (system ear + whisper.cpp) |
| Survives vendor outage | NO | YES (SttRouter fallback chain) |
| RAW / no-rewrite mode | NO | YES (RAW/LIGHT/HIGH) |
| Account required | YES | NO |
| Word limits | 2,000/wk free | None, MIT |
| Course correction, lists, fillers, dictionary+auto-learn, snippets, backtrack, snooze+shake, shrink modes, push-to-talk | YES | YES (parity) |
| Cleanup intelligence | Fine-tuned Llama + screen context | Rules engine + BYO cloud brain |
| Hinglish | Dedicated model | Partial (hi-IN + Sarvam ear) |
| Latency | p99 <700ms measured | Instrumented as of v0.1.7 (`OpenFlow.Latency` log) |
| Battery discipline | Session caps | PulseSchedule adaptive tick + SessionGuard caps |

## Attack plan (ranked)

1. DONE — latency instrumentation: `SessionLatency` marks listen → ear_ready →
   first_partial → stop → inserted; log tag `OpenFlow.Latency`. Compare vs their
   p99 700ms bar. Next step: aggregate into Insights.
2. DONE — context-aware cleanup: `AppCategory.promptGuideline` now feeds the
   brain prompt via `promptHint` (was hardcoded null). Chat gets casual, mail
   formal, dev preserves camelCase, search strips pleasantries.
3. MOSTLY DONE — Hinglish: Sarvam Saaras v3 `codemix` mode already wired
   (EnginePrefs accepts transcribe/translate/verbatim/translit/codemix).
   Remaining: ship default suggestion + bias lists for Indian names.
4. TODO — outage-proof messaging — auto-fallback toast "cloud slow → phone ear,
   0 words lost". Their status page is our ad.
5. UX parity: drag-bubble-to-bottom-edge snooze target; inline notification-reply
   bubble coverage; browser address-bar list check.
6. Later: Whisper Mode (quiet speech) — mic gain / VAD threshold work.
7. Later: Hindi/Hinglish filler set (matlab, yaani) once real samples collected.

Sarvam intel (2026): Saaras v3 = 22 Indic languages + English, native
code-mixing, streaming WS <150ms TTFT, Hindi WER <8%, code-mixed WER <12%.
Modes include codemix + translit (Roman script) — exactly Wispr's Hinglish
play, but rentable per-minute instead of building a model.

## Sources

- Baseten customer story: baseten.co/resources/customers/wispr-flow (CTO quotes, p99 numbers)
- TechCrunch Android launch: techcrunch.com/2026/02/23/wispr-flow-launches-an-android-app (Hinglish, $81M)
- Official docs: docs.wisprflow.ai (setup, bubble sizes, snooze, battery OEM steps)
- api-docs.wisprflow.ai/introduction (positioning)
- Reliability log: getvoibe.com/resources/is-wispr-flow-reliable (75+ outages, Trustpilot 2.7, complaint themes)
- switchmytool.com/blog/wispr-flow-vs-apple-dictation (Reddit complaint synthesis, offline stance)
- eesel.ai/blog/wispr-flow-review (privacy red flags)
