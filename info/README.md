# info/ — extracted reference material

This folder is the **deep audit trail** for open-flow. Everything here is
append-only reference — never imported into production code, never read at
runtime. Edit by **adding** files, never rewrite history.

**Why split out of `docs/`:** docs/ holds the **law** for running the project
(AGENTS, PROCESS, SECURITY) plus the **live state** of features (HANDOFF,
MASTER-PLAN, AUDIT). info/ holds **why we got here** — the raw transcripts,
brainstorm archive, and feature ID → source-line traceback.

## Folders

| Subfolder | What's inside | How to read |
|----------|---------------|-------------|
| `info/sessions/` | Raw 4 Grok chat transcripts (~160KB total) | `info/sessions/README.md` indexes them. Read top-down for full project history, or grep a topic. |
| `info/mockups/` | HTML mockups from brainstorming (archive) | Open in browser. Mostly pre-M3 designs that didn't ship. |
| `info/research/` | Web-research dumps (when run) | 1-table-per-item, dates in filename. Verify before citing. |

## What does NOT belong here

- Active plans / specs → `docs/process/`
- Bug list / current state → `docs/AUDIT-2026-08-10.md`
- Product law → repo-root `AGENTS.md`
- Live pickup pointer → `docs/HANDOFF.md`

Rule: **new info → `info/`.** New law → root or `docs/`.
