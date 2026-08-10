# open-flow — Docs index

This is the **single landing page** for every doc in `docs/`. Pick by role + recency. Start here.

**Repo:** `/home/mitun/open-flow` (WSL Ubuntu-26.04) · **main tip:** `ba78aeb`
**Last audit:** `2026-08-10` · **License:** MIT FOSS
**Built:** pointer → `AGENTS.md` (law) → `PROCESS.md` (how) → `SECURITY.md` (what's safe) → this `INDEX.md`

---

## How the docs relate

```
AGENTS.md          ─→ law every agent obeys (product locks + voice + workflow)
  │
  └─→ docs/
        ├─ INDEX.md                 ← you are here
        ├─ HANDOFF.md               ← live pickup: WHERE / NEXT (maintain daily)
        ├─ BASELINE.md             ← feature history + locked product law
        ├─ MASTER-PLAN.md          ← every feature, status, owner, source session
        ├─ AUDIT-2026-08-10.md      ← bug punch list + code-review findings (latest)
        ├─ FEATURES.md             ← Wispr A–Z vs open-flow status matrix
        ├─ PROCESS.md              ← per-feature workflow (worktree / plan / TDD / commit)
        ├─ SECURITY.md             ← privacy + permission defaults
        │
        ├─ sessions/               ← raw 4 Grok chats (190 KB, full audit trail)
        │   ├─ 2026-08-10-strategic-voco.txt          (product ideation → locked)
        │   ├─ 2026-08-10-ponytail-cut.txt            (dead-code cut, merged)
        │   ├─ 2026-08-10-agent-messup-triage.txt     (post-triage of agent thrash)
        │   └─ 2026-08-10-design-customisability.txt  (M3 IA + 15-feature gap list)
        │
        ├─ mockups/                 ← brainstorm HTMLs (kept for reference)
        │   └─ 2026-08-10-brainstorm/  (14 HTMLs + server state)
        │
        └─ superpowers/             ← legacy per-feature plan + spec docs
            ├─ plans/               (10 files: 00-bootstrap … master-fix-roadmap)
            └─ specs/               (2 files: dual-skin + ui-redesign)
```

---

## What each doc is FOR

| Doc | Read when | Purpose | Maintained by |
|-----|-----------|---------|---------------|
| `AGENTS.md` (repo root) | every chat start | Product + workflow + voice rules. Non-negotiable. | locked |
| `docs/INDEX.md` | (this file) | Find the right doc by purpose | when reorganising |
| `docs/HANDOFF.md` | resuming mid-task | Current `WHERE / NEXT` pointer. Tiny. | end of each drop |
| `docs/BASELINE.md` | asking "what shipped" | Locked product law + feature history | once per feature |
| `docs/MASTER-PLAN.md` | scoping next drop | Master backlog (98 features, status per ID) | end of each drop |
| `docs/AUDIT-2026-08-10.md` | fixing bugs | Bug list A→K with file + line + fix size | per audit |
| `docs/FEATURES.md` | competing / comparing | Wispr A-Z vs open-flow status | per feature |
| `docs/PROCESS.md` | starting a new feature | Web search → plan → worktree → TDD → security → commit | locked |
| `docs/SECURITY.md` | touching permissions / privacy | Privacy defaults + permission truth | per perm change |
| `info/sessions/` | deep audit of decisions | Raw 4-session Grok chat transcripts | append-only |
| `info/mockups/` | reviewing design experiments | Pre-M3 HTML mockups from brainstorming | archive |
| `docs/process/plans/` | understanding feature detail | Per-feature plan docs (legacy F0-F14) | frozen |

---

## Reading order for a new chat

| Your role | Order |
|-----------|-------|
| **Ship the next drop** | `HANDOFF.md` → `MASTER-PLAN.md` § next drop → repo health check → worktree |
| **Audit / fix bugs** | `AUDIT-2026-08-10.md` → `MASTER-PLAN.md` § 6 (real bugs) → relevant `.kt` |
| **Add feature** | `MASTER-PLAN.md` → `FEATURES.md` → `sessions/2026-08-10-strategic-voco.txt` → plan in `superpowers/plans/` → worktree |
| **New chat pickup** | `HANDOFF.md` only (paste into new chat) |

---

## Recency (last touched)

| Path | When |
|------|------|
| `INDEX.md` | 2026-08-10 (reorganise) |
| `AUDIT-2026-08-10.md` | 2026-08-10 (full + Voco delta) |
| `MASTER-PLAN.md` | 2026-08-10 (initial + AUDIT delta) |
| `HANDOFF.md` | 2026-08-10 (Drop 3 STT perf — 1 commit behind main tip) |
| `BASELINE.md` | 2026-08-10 (F-merged) |
| `FEATURES.md` | 2026-08-10 (post F14) |
| `PROCESS.md` | locked |
| `SECURITY.md` | locked |
| `AGENTS.md` (root) | locked |
| `sessions/` | appended 2026-08-10 |
| `mockups/` | archived 2026-08-10 |

---

## DROP map (from git log)

| Tip | Date | What |
|-----|------|------|
| `093fbbc` | 2026-08-10 | bootstrap process |
| `54227af` | 2026-08-10 | F1 Android Compose scaffold |
| `eafe209` | 2026-08-10 | F13 Wispr Android parity core (local) |
| `5584152` | 2026-08-10 | F12 dictation reliability |
| `a51b3e7` | 2026-08-10 | F11 continuous dictation + light UI |
| `021b066` | 2026-08-10 | F10 Flow Bubble (NOT IME) |
| `021b066`→`1e32bc3` | 2026-08-10 | F14 polish (bank hide, bubble modes, shake, pulse) |
| `37a4a1f` | 2026-08-10 | F12 UX foundation |
| `e5d0137` | 2026-08-10 | merge ponytail cut |
| `c13c195` | 2026-08-10 | HANDOFF truth after F12 UI + ponytail merge |
| `7d7d039` | 2026-08-10 | UI Drop 2 (Home layout, Menu items, bubble pulse) |
| `85560e9` | 2026-08-10 | UI Drop 1 (drawer hub + bubble text) |
| `946be80` | 2026-08-10 | Drop 3 STT perf + Wispr session insert (no raw dump) |
| `ba78aeb` | 2026-08-10 | **main tip** M3 product shell — bottom nav IA, dual skin tokens, research prefs |

---

## Quick warnings

- **HANDOFF.md is 1 commit behind main** (says tip=`946be80`, real=`ba78aeb`). Update next before claiming done.
- **2 APK paths existed before clean:** repo-root + `dist/`. Kept only `dist/open-flow-debug.apk` per BASELINE convention.
- **Old APK on user's Desktop** (`~/Desktop/open-flow-debug.apk`) is the LAST-built one. If user reports "still broken", check `stat` first — may be stale.
- **2 stale worktrees** were removed (M3 + brutal, both already merged or parked). `.worktrees/12-ux-foundation/` and `.chore-ponytail-cut/` empty dirs cleaned.
- **Repo before this clean:** had `info/sessions/` with 4 grok dumps + 14 HTML mockups. Now canonicalised to `info/sessions/` + `info/mockups/`.
