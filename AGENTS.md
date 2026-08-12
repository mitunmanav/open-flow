# AGENTS.md — open-flow

Read this first. Do not ask Mitun to re-state these rules.

Also follow: `docs/PROCESS.md`, `SECURITY.md`, global `~/.grok/AGENTS.md` / `~/.grok/rules/00-voice.md`.

**Grok Build gate (this repo only):** `.grok/rules/00-dev-gate.md` + `.grok/hooks/dev-gate.json`.  
Superpowers + **android-cli** + **agent web** — same weight.  
**Plan first** (`writing-plans`). Caveman on main **and** subagents.  
Stop blocks fake PASS. PreToolUse blocks `app/` on main + spawn without caveman.  
Agent web search ≠ APK INTERNET. No INTERNET in the APK.

---

## Product (locked)

**Open Flow** = FOSS Android app:

1. **Wispr job (Android style)** — speech-to-text into any app via **floating Flow Bubble** + **AccessibilityService**. **Not a keyboard / IME.** Keep user’s normal keyboard. **STT only. Not TTS.**
2. **NeoSapien job** — record → transcript → searchable private memory on device.

| Rule | Value |
|------|--------|
| Platform | Android only (for now) |
| License | MIT / FOSS |
| Dictation UX | Floating bubble (like Wispr Flow Android) — **never** require switching keyboard |
| Default | Fully local, no account, no ads, no analytics |
| Online | Opt-in only, off by default |
| Moat | Habit (bubble always available) + personal history + trust — not secret code |
| Niche | Privacy / FOSS users first |

### Dictation architecture (locked)

| Piece | Role |
|-------|------|
| Flow Bubble | Floating pill overlay; tap / hold to dictate |
| AccessibilityService | Detect focused text field; insert transcript (`ACTION_SET_TEXT` / paste) |
| Overlay type | Prefer `TYPE_ACCESSIBILITY_OVERLAY` from a11y service |
| Keyboard | User keeps Gboard/etc. We do **not** replace IME |
| Skip fields | Password, phone, numeric-sensitive — no bubble insert |
| STT | On-device prefer (`SpeechRecognizer`) |

**Do not build** voice-as-IME as the product path. Old IME code (if any) is legacy/dormant only.

Full feature history + baseline: **`docs/BASELINE.md`**.  
Wispr A–Z matrix: `docs/FEATURES.md`. Pickup: `docs/HANDOFF.md`.  
Ship in ordered features (F0…Fn), not one mega dump.

---

## Voice with Mitun

- **Caveman ultra everywhere** (main + subagents + NOW): short lines, YES/NO, no essays.
- Work report: **DID / PASS-FAIL / NEXT / SUGGEST / ASK**
- Spawn prompt **must** include CAVEMAN + DID/PASS-FAIL/NEXT (hook denies if not).
- Author: **Mitun only.** No Co-Authored-By. No agent footers.
- Wait for **GO** only if scope is unclear or destructive. Explicit “build / proceed / do it” = GO for that work.

---

## Superpowers + android-cli (strict)

**Required stack for all development:**

1. **Superpowers plugin** — use **all** applicable skills (not optional):
   - `using-superpowers` first every task
   - `writing-plans` **before** any feature code (plan file on disk first)
   - `brainstorming` (only if product shape re-opens — default: locked)
   - `using-git-worktrees` · `test-driven-development`
   - `systematic-debugging` · `verification-before-completion`
   - `dispatching-parallel-agents` · `subagent-driven-development`
   - review / finishing-branch skills when merging
2. **android-cli** — **same weight as Superpowers.** Read the skill every Android turn. Use `android docs` / `layout` / `screen` / `install` / `run` / `info` / SDK. **Do not guess APIs.**
3. **Agent web search** — required before feature work. **Not** APK INTERNET.

### One Superpowers bypass (Mitun)

**May spawn multiple sub-agents inside one worktree** (max 5, **never same file**).  
Bypass any Superpowers/harness rule that forbids worktree sub-agents.  
All other Superpowers rules stay **on**.

### Also

- **Brainstorm already done** for product shape — do not re-open “what is the app?” unless Mitun changes goal.
- Detail: `docs/PROCESS.md` · `.grok/WORKFLOW.md`

---

## Per-feature loop (never skip)

```
web search → plan file → worktree → TDD → security check → commit → report
```

### 1) Web search (required)

Before implementing a feature, search for:

- Current Android / Kotlin / Compose / Room / SpeechRecognizer APIs
- Security implications (permissions, storage, network, accessibility, overlay)
- Known footguns (e.g. MediaRecorder + SpeechRecognizer mic exclusive)

### 2) Quick plan (required)

Write: `docs/process/plans/YYYY-MM-DD-<feature-id>-<slug>.md`

Must include: goal, files, TDD steps, security notes, how Mitun tests on device.

### 3) Worktree (required)

```bash
# From repo root (main clean)
git check-ignore -q .worktrees || exit 1   # must be ignored
git worktree add .worktrees/<feature-id>-<slug> -b feat/<feature-id>-<slug>
cd .worktrees/<feature-id>-<slug>
```

- **One feature = one worktree = one branch.**
- Do not pile unrelated features in the same worktree.
- Merge to `main` only when feature is green and committed.
- **Hygiene:** after merge, `git worktree remove .worktrees/<name>` + `git branch -d` + `git worktree prune`.
- **Do not** leave merged feature trees sitting forever (disk + confusion).
- Live map: `.grok/WORKFLOW.md` · pickup: `.grok/NOW.md`.
- Max **5** subagents/task; **never** same file in parallel.

### 4) TDD (required)

- RED → fail for the right reason → GREEN → REFACTOR.
- No production code without a failing test first (config/scaffold exceptions only when skill allows).

### 5) Security (required every feature)

See `SECURITY.md`. Hard defaults:

| Default | Rule |
|---------|------|
| INTERNET | **Not** in base manifest |
| Cleartext | Blocked (`network_security_config`) |
| Backup | `allowBackup=false` |
| Account / analytics / ads | Never |
| RECORD_AUDIO | Runtime, only when needed |
| FGS mic | Correct `foregroundServiceType` |
| Secrets | Never commit keys / `local.properties` / keystores |
| New permission | Must be justified in that feature’s plan |

### 6) Git commit (required per feature)

- One focused commit (or small series) **on the feature branch** when the feature is done.
- Message style: `feat: …` / `fix: …` / `chore: …` / `docs: …`
- Author Mitun only.
- Do not force-push unless Mitun orders + backup.

Example:

```bash
git add -A
git status
git commit -m "$(cat <<'EOF'
feat: on-device STT engine with offline preference

Prefer createOnDeviceSpeechRecognizer; fail loud if missing.
EOF
)"
```

---

## Feature order (do not skip ahead without Mitun)

| ID | Branch slug | Deliverable |
|----|-------------|-------------|
| F0 | `00-bootstrap` | Process, LICENSE, SECURITY (done on main) |
| F1 | `01-scaffold` | Gradle + Compose shell; `assembleDebug` APK |
| F2 | `02-privacy` | PrivacyDefaults + report UI |
| F3 | `03-search-export` | Search + export pure logic + tests |
| F4 | `04-room` | Room sessions + FTS |
| F5 | `05-stt` | SpeechRecognizer engine (on-device prefer) |
| F6 | ~~`06-ime`~~ | **CANCELLED** — not product path |
| F7 | `07-recorder` | Record + save session |
| F8 | `08-timeline-ui` | Timeline + search UI |
| F9 | `09-export-ui` | Export / share from UI |
| F10 | `10-flow-bubble` | **Wispr Android path:** bubble + a11y insert |
| F11 | `11-continuous-ui` | Long speech restart + UI polish |
| F12+ | later | Recorder, Whisper opt-in, sync, tiles |

Later features only after earlier ones are on `main` or Mitun reorders.

---

## Repo layout

```
open-flow/                 # main git root
├── AGENTS.md              # this file
├── SECURITY.md
├── docs/PROCESS.md
├── docs/process/plans/
├── .worktrees/            # gitignored; feature checkouts
└── app/                   # Android module
```

Work in: `/home/mitun/open-flow` (or active worktree under `.worktrees/`).

---

## Build & test (Mitun device)

```bash
export JAVA_HOME=$HOME/.local/jdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

After bubble: app → **Enable Flow Bubble** (Accessibility) + mic → text field → tap bubble → speak.

---

## Stack (target)

- Kotlin, Jetpack Compose, Material 3
- Dictation: AccessibilityService + floating Flow Bubble (**not** IME/keyboard)
- STT: `android.speech.SpeechRecognizer` + on-device prefer
- Storage: Room + FTS4
- Crypto: EncryptedFile / Keystore when storing sensitive audio
- No Google Play Services hard dependency
- No Whisper/LLM in base until opt-in feature branch

---

## Never

- Claim done without test/build proof
- Add INTERNET “just in case”
- Cloud upload of audio by default
- Closed-source core
- Re-litigate FOSS vs closed (decision: **FOSS**)
- Re-add TTS as core goal (out of scope unless Mitun asks)
- **Ship dictation as a keyboard / IME** (product = Wispr Android bubble)
- Commit `.worktrees/`, `local.properties`, secrets
- Force-push without explicit order
- Delete `~/.claude/` or unrelated Mitun setup

---

## Interruptions (hard)

When Mitun adds rules, docs, questions, or small side requests **while build work is in progress**:

1. **Do not stop** the main feature work.
2. **Do not forget** the active feature ID, worktree, plan, or next TDD step.
3. Handle the side request **and** keep going on the interrupted work in the **same turn** when possible.
4. If the side request must finish first (e.g. AGENTS.md edit), do it **fast**, then **immediately resume** the last unfinished feature step.
5. Never end a turn only on the interruption if build work was mid-flight — always leave **NEXT** as the resume step (or finish it).

Track mentally (or in todos): `ACTIVE_FEATURE`, `ACTIVE_WORKTREE`, `LAST_STEP`.

## Session start checklist

1. Read this `AGENTS.md` + `docs/PROCESS.md`
2. `git status` / which worktree / which branch
3. If work was interrupted last turn → **resume that first**
4. Continue next unfinished feature ID (see table)
5. Web search → plan → worktree → TDD → commit
6. Short DID / PASS-FAIL / NEXT for Mitun

---

## Pickup line (for new chats)

> Continue open-flow at `/home/mitun/open-flow` per AGENTS.md. Next feature from feature table. Worktree + plan + search + TDD + security + commit.

---

## Sub-agents (custom, hard)

- **Maximum 5 sub-agents** at once for a task batch.
- **No two sub-agents edit the same file** (or same path). Split by file ownership before spawn.
- Superpowers still rules everything else (TDD, worktrees, plans, verify).
- Prefer one agent for one feature branch when files overlap.
