# Grok chat transcripts (full audit trail)

Raw 4-session Grok web chat where open-flow was planned + built. **Append-only.** When a new strategic chat closes, drop it here as the next number.

## Index

### `01-strategic-voco.txt` — original product ideation (2786 lines, 93 KB)

Product birth + 3 name changes (Voco → open-spruce → open-flow). 54-feature catalog (9 buckets A-I), 8 build phases P0-P8, moat analysis, FOSS vs closed-source verdict.

**Read this if:** you need the WHY for every product lock. Why is it a bubble and not an IME? Why MIT and not Apache? Why "FUTO wins keyboard, we win combo"?

**Skip if:** you only want current state. (Use `docs/MASTER-PLAN.md` for that.)

| Lines | Topic |
|-------|-------|
| 1-200 | Initial 6-feature MVP (Voco = voice recorder + on-device transcription) |
| 201-345 | "All features, fully local + online opt-in" → 54-feature catalog split A-I |
| 346-449 | "Check FOSS competitors" → FreeFlow / Voquill / WhisperType → verdict: desktop saturated, Android gap is polished all-in-one |
| 450-473 | "Combine Wispr + NeoSapien" → features table (no pendant hardware) |
| 474-636 | Final spec, audit (real-vs-plan), simplify, drop IME, keep bubble |
| 637-917 | "open-flow" named, privacy spec, runtime `INTERNET` removal (false claim, fixed by NSC) |
| 918-1133 | "Simplify" iterations, AGENTS.md created, **drop voice IME, ship bubble** |
| 1134-1531 | Naming (Voco → open-flow), Superpowers use-skill, decisions log, moat analysis (closed-source vs FOSS) |
| 1532-1718 | Moat analysis: habit + history + trust. Real demand ($81M Wispr, Notely/FUTO exist as alt). |
| 1719-2400 | Master plan 98 features (P0-P8), F1 shipped, **decision to abandon voice IME**, build bubble F10 |
| 2401-2786 | Drop 3 STT perf, F14 polish, "test on phone" instructions |

### `02-ponytail-cut.txt` — dead-code cut (175 lines, 6 KB)

`/ponytail full` + `/ponytail-audit` session. 16 specific cuts applied in one diff (-569 / +56 lines), merged as commit `e5d0137`.

**Read if:** you don't want to re-add what was already cut (Session dual-stack, TranscriptSearch/Exporter, empty RecordingService, SttConfig, FocusResolver, double polish path, unused deps).

### `03-agent-messup-triage.txt` — agent thrash recovery (1143 lines, 31 KB)

User came back angry ("other coding agents messed up"). Agent did full triage, locked answers via 6 questions, shipped Drop 1 → 2 → 3 in 24 hours.

**Read if:** you need the 4-stop incremental design history (triage → Drop 1 → Drop 2 → Drop 3) and the Wispr-Android research that drove Drop 3 (silence wait 2.0→0.9s, course-correct "4:30 actually 5:30").

| Lines | Topic |
|-------|-------|
| 1-100 | User anger, "fix everything", request to read previous session |
| 200-380 | Triage: 2 app shapes, 3 worktrees, HANDOFF wrong 3 ways |
| 400-500 | 6 questions answered → Calm Pro + full IA + everything tweakable + side drawer + Settings in hub |
| 500-650 | Drop 1 design (App shell, Home hub, Settings tree) |
| 700-870 | Drop 1 shipped `85560e9`, Drop 2 built `7d7d039` (Home layout, Menu items, Listen pulse) |
| 880-1070 | Wispr research, STT silence tuning, Drop 3 spec |
| 1080-1143 | Drop 3 shipped `946be80` — course-correct + no raw dump + faster STT + Home settings |

### `04-design-customisability.txt` — M3 IA lock (798 lines, 28 KB)

Latest session. User wanted: **everything** (Wispr desktop A1-A15 + full app customisability + bubble chrome fix hard).

**Read if:** you need the M3 IA lock and WHY Settings lives in drawer (not bottom), or the gap-list 1-15 that became Drop 4 work order.

| Lines | Topic |
|-------|-------|
| 1-110 | User anger, "everything", plan: P0 chrome / P1 STT speed / P2 polish / P3 customize / P4 motion |
| 110-360 | Wispr Flow **DESKTOP** feature inventory (15 sections, A1-A15): Flow Bar, AI polish, Hub app, Personalization, Command Mode, Vibe coding, Scratchpad, Notetaker, History, Tray, Settings, Privacy, Team, Plans |
| 360-500 | 6 rejected brand directions (Calm Pro / Field Mic / Ribbon Ink / High-Vis / Swiss Grid / Brutal Catalog / Dense Channel) — most killed as "AI soup" |
| 500-660 | Locked IA: bottom = Home·Dict·Snips·Style, drawer = Settings·History·Customize only. **Settings lives in drawer, not bottom.** |
| 660-798 | M3 + subtle brutal as 2 themes → "STOP. M3 only. Ship." → `feat/product-m3` → main `ba78aeb`. Brutal parked. |

---

## How to read efficiently

| Need | Read |
|------|------|
| New chat pickup | `docs/HANDOFF.md` only (1KB) |
| Full project history | `01-strategic-voco.txt` + `03-agent-messup-triage.txt` (3.6K lines, 124KB) |
| Current state of features | `docs/MASTER-PLAN.md` + `docs/AUDIT-2026-08-10.md` |
| Specific decision | grep by tag/ID (`F12`, `A-1`, `Gap 4`, `Ponytail cut`, etc.) |
| What was cut / not to re-add | `02-ponytail-cut.txt` |

**Total:** ~4902 lines, ~160 KB. Don't read all 4 in one session — your context will hurt.
