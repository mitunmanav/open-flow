# How the acceptance gate is run and recorded

Type: grilling
Status: open
Blocked by: none

## Question

The destination defines "shipped" precisely: three device classes (Pixel-like, Samsung-class, Xiaomi-class) × ten text-entry scenarios, each going start → record → transcribe → clean → insert → recover cleanly. That is 30 runs, and it is the single thing standing between this project and its goal.

What is undecided is the *protocol*, and getting it wrong makes the gate worthless in a specific way — a gate that is tedious enough to be skipped is not a gate.

- **Scripted harness or manual checklist?** The scenarios are dictation runs into third-party apps (Gmail, WhatsApp, Slack, a notes app). You cannot script the *content*, because the point is inserting into apps you do not control. So the honest options are a scripted setup with a manual pass/fail, or a fully manual matrix. What does the project actually want to maintain?
- **How is a run recorded?** Per-device, per-scenario pass/fail with a short note is enough to be useful. The temptation is a full metrics database, which is a system to maintain and defend against rot.
- **What counts as "recover cleanly"?** The destination says it, and it is the least objective phrase in it. Recoverable errors resurface in History per ticket 14 — so a recovery test is exercising a real path, not an edge case, and deserves its own definition.
- **Whose devices?** This is the practical blocker. Three named OEM classes means physical hardware someone owns and can keep charged. Whether that is the owner, a lab, or CI-hosted real devices (Firebase Test Lab) changes the budget and the timeline, and it decides whether the gate is repeatable or anecdotal.
- **Does a failure block the release tag?** A gate that reports without gating is a report. `release.yml` currently triggers on `v*` and runs unit tests only — decide whether it must also consult the recorded gate result before publishing.

Decide the protocol, not the results. The results do not exist yet, and this ticket is what makes producing them a repeatable act rather than a heroic weekend.