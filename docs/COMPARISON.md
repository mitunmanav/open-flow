# Open Flow vs Wispr Flow

Honest side-by-side. Last updated 2026-09-03.

## Headline

| Axis | Open Flow | Wispr Flow |
|---|---|---|
| Model | FOSS, AGPL-style licensed, auditable | Closed source, SOC 2 / ISO 27001 / HIPAA |
| Distribution | Play + F-Droid (planned) + sideload | Play + App Store + sideload (paid) |
| Free tier | Always free, no word cap | ~1000-2000 words/week, then paid |
| On-device | Yes (system STT + on-device whisper.cpp path) | No, cloud only |
| Offline | Yes (system STT, offline whisper model) | No |
| RAM idle | Background AccessibilityService (no eager model) | ~800 MB idle (per How-To Geek, Apr 2026) |
| Cold start | Already-running service | 8-10 s (per How-To Geek, Apr 2026) |
| Languages | System STT (device-dependent) + whisper multilingual | 100+ cloud languages |
| Bubble UI | `TYPE_ACCESSIBILITY_OVERLAY`. Keep your keyboard. | Floating bubble. Keep your keyboard (per Android Police). |
| Per-app style | `text/StyleResolvePolicy.kt` (local, rule-based) | Cloud proprietary |
| Snippets | Speak a trigger, expand to text | Speak a trigger, expand to text |
| Personal dictionary | Yes (`text/LearnEngine.kt`) | Yes |
| Cleanup stage | 8-stage local pipeline (AtomicTokens → Restore) + InvariantGate fallback | Proprietary post-processing |
| Code-aware dictation | No (planned) | Yes (camelCase/snake_case, IDE files) |
| Account / sign-in | None | Email + paid tiers + Teams/Enterprise SSO |
| Data sold | Never (no collection at all) | Never (per privacy page; user opts in for model training) |

## Where Wispr Flow wins (today)

- Polished zero-edit rate (90% per their benchmark)
- IDE / code-aware dictation in Cursor + Windsurf
- Notetaker product (separate)
- Cross-platform: Mac, Windows, iOS, Android
- Team / Enterprise tier with admin + SSO

## Where Open Flow wins (today)

- Fully offline, no internet required
- No account, no email, no billing
- Local-only processing; transcripts/audio never leave the device unless user opts in
- FOSS; reproducible-build target (planned)
- No word cap, no tier gate, no time-out
- Hard-edge Compose UI; no Google services dependency

## What we still need to close the gap

- IDE / code-aware dictation
- IDE / Cursor-style file tagging
- Cross-platform (today: Android only)
- Zero-edit rate at 90%+ on par with Wispr (currently measured by golden corpus; F1 gap to close)
- Per-language polishing for non-English flows

## Sources

- Wispr Flow product page: https://wisprflow.ai/
- Wispr Flow why-flow page: https://wisprflow.ai/why-flow
- Wispr Flow privacy page: https://wisprflow.ai/privacy
- Wikipedia: https://en.wikipedia.org/wiki/Wispr_Flow
- Android Police review (Feb 2026): https://www.androidpolice.com/wispr-flow-app-android-voice-typing-experience/
- How-To Geek competitive review (Apr 2026): https://www.howtogeek.com/i-tried-7-voice-typing-apps-on-windows-and-speechify-stood-out-for-an-important-reason/
