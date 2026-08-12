# open-flow build process (hard)

**Stack:** Superpowers plugin + **android-cli**. No freestyle agent process.

## Superpowers — use all of it

Before real work, load and follow applicable Superpowers skills:

| Skill | When |
|-------|------|
| `using-superpowers` | Session start / any task |
| `brainstorming` | New creative / feature shape (product already locked — don't re-open "what is the app?") |
| `writing-plans` | **Required before feature code** |
| `using-git-worktrees` | Feature isolation |
| `test-driven-development` | Logic / pipeline / pure Kotlin |
| `systematic-debugging` | Bugs before random fixes |
| `verification-before-completion` | Before claim done / merge |
| `dispatching-parallel-agents` | Independent parallel work |
| `subagent-driven-development` | Plan execution with agents |
| `requesting-code-review` / `receiving-code-review` | Pre-merge quality |
| `finishing-a-development-branch` | After green, merge options |
| `executing-plans` | Multi-PR plan runs |

### One Superpowers bypass (Mitun law)

**Allowed:** spawn **multiple sub-agents inside one worktree** for parallel slices  
(different files only; max 5).  

Bypass any Superpowers / harness rule that says *not* to run sub-agents in a worktree.  
Everything else Superpowers remains **on**.

## android-cli (same weight as Superpowers)

Use `android` CLI every Android turn. Not optional. Commands:

- `android info` — SDK / device
- `android docs search|fetch` — API truth (not guesses)
- `android layout` / `android screen capture` — UI verify
- `android install` / `android run` — deploy
- SDK: `android sdk …` when missing packages

Prefer `android` over raw inventing device flows. `adb` still OK for deep inject/logs.

## Per feature (every time)

1. Superpowers skill check  
2. **Web search** + **android docs** for APIs/security  
3. **Plan** → `docs/process/plans/YYYY-MM-DD-<feature>.md`  
4. **Worktree** → `.worktrees/<feature>` + branch `feat/<feature>`  
5. **TDD** → red → green → refactor  
6. **Security** → no INTERNET default; no new risky perm without plan  
7. **Verify** → unit tests + compile; device when UI/bubble  
8. **Commit** on feature branch (author **Mitun only**)  
9. **Merge main** when green → **remove worktree**

Hooks: caveman spawn; max 5/tree; **tree jail**; no fake PASS.  
This repo only. Small fix = no worktree. Large = worktree + merge.  
Agent web ≠ APK INTERNET.

```bash
git check-ignore -q .worktrees || exit 1
git worktree add .worktrees/<slug> -b feat/<slug>
cd .worktrees/<slug>
# … build …
git worktree remove .worktrees/<slug>   # after merge
git worktree prune
```

## Product locks

- Bubble + AccessibilityService — **not** IME  
- en-US only  
- Local FOSS; no cloud AI default  
- Cleanup High = **rules**, not LLM rewrite  
- Default UI: **modern brutal** (`VisualSkin.BRUTAL`)

## Local polish pipeline

```
STT → PhraseMap/VoiceCommands → Cleanup level → Style → Dict/Snippets → insert
```

## Test on device

```bash
export JAVA_HOME=${JAVA_HOME:-$HOME/.local/jdk}
export ANDROID_HOME=${ANDROID_HOME:-$HOME/Android/Sdk}
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew :app:assembleDebug :app:testDebugUnitTest
android install --apks app/build/outputs/apk/debug/app-debug.apk
# or: adb install -r …
android layout -p   # inspect UI
```

## Voice with Mitun

Caveman ultra. DID / PASS-FAIL / NEXT / SUGGEST / ASK.
