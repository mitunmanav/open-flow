# Where does MODEL_MISSING put its download prompt?

Type: grilling
Status: resolved
Blocked by: none

## Question

[Where the first-launch model download sits in the onboarding flow](47-first-launch-download-and-onboarding-order.md)
settled the first-run screen and, as a consequence nobody had asked about, revealed a
second entry point to the same download surface.

[ADR-0001](../../../docs/adr/0001-speech-provider-contract.md) is explicit that
`OfflineModelMissing` is *recoverable* and that the "download the speech model" prompt
**"belongs on the user's screen only once every candidate has failed"**. On first launch,
with one real provider, that condition is satisfied immediately — which is fine, because
first run *is* the prompt. The question is what happens on every launch **after** it.

Concretely: a user completes setup, later deletes OpenFlow's data, or the model's files
are removed by the system under storage pressure. `health()` returns `MODEL_MISSING`
(`isModelPresent()` is null — the ModelStore's null *is* the signal). The router finds no
eligible provider and records why. And then what does the user see?

Three things are genuinely undecided:

- **Is it the same surface?** [ADR-0013](../../../docs/adr/0013-first-launch-order.md) built one
  download surface and pointed two flows at it, which is the obvious answer and may be
  the wrong one. A first-run screen explains why the model is being fetched before the
  user has ever used the app; a post-setup prompt has to explain it to someone who
  already knows, and re-running onboarding is a worse answer than it looks — it discards
  a consent the user already gave and re-asks a question they have answered.
- **Does it block?** First run blocks because there is nothing to look at. Afterwards
  there is: History, the dictionary, snippets. So the same "cannot" (dictation is
  impossible) is paired with a *false* "must not" (the app is perfectly usable), which is
  the same argument ticket 47 used to choose blocking — inverted. This is the substance
  of the decision and it has not been made.
- **Where is `MODEL_MISSING` allowed to surface at all?** It is a `ProviderHealth` value
  the router reads at `PREPARING`; nothing says the user must ever see it. The opposite
  option is real: dictate simply fails, with the failure surfaced in History per ticket
  14's recovery model, and the fix lives in Settings. That is a smaller surface and it may
  be the right one — but it means the most common post-setup breakage of a
  download-on-first-launch product is a silent dead button.

Constraints this has to respect: `POST_NOTIFICATIONS` is not on either screen; the
permanent cost footer (about 128 MB downloaded / about 45 MB installed / about 175 MB
temporarily needed / pinned source / integrity check) is not
optional on any surface that fetches; and [Acceptance Gate](../../../docs/quality/acceptance-gate.md)
scenario coverage for the first-run fetch is a separate question this does not settle.

Also worth settling in the same breath: **does removing the model put the app in a state
it can recover from without a reinstall**, and is that state reachable by anything other
than the user deleting files — which is what makes this a decision about a *normal*
situation rather than a support edge case.

## Comments

### 2026-10-04 — Facts checked before discussion

`isModelPresent()` returns a Boolean; `modelDirectory()` returns null when required files
are absent. The question's nullable `isModelPresent()` premise is a naming error.

The Model Store uses `noBackupFilesDir`, not a cache directory. Android documents
low-storage eviction for cache files, so routine cache eviction is not evidence that
this model will disappear. Missing required files can be recovered through `ensureModel()`;
this does not require reinstalling the app. Backup exclusions also matter when restoring
app data to another device. Full app-data clearing is distinct from model-only loss: it
also removes setup state and local data, so it must not be described as preserved setup.

Sources: [Android app-specific storage](https://developer.android.com/training/data-storage/app-specific)
and [Android Auto Backup](https://developer.android.com/identity/data/autobackup).

Post-setup UX remains awaiting the owner's decision.


### 2026-10-04 — First round accepted

The owner accepted the three recommendations:

- Reuse the download controls with post-setup recovery wording, preserving existing setup
  and consent rather than replaying onboarding.
- Missing/downloading model blocks dictation only; History, dictionary, snippets and
  settings remain accessible.
- Settings exposes a Voice model recovery action. When eligible providers are exhausted
  because the model is missing, the Bubble explains the problem and offers that action.
  Fetching starts only after an explicit tap. Every fetching surface retains size, peak
  space, source and integrity information.

Completion and navigation behaviour remain the next round; this ticket is unresolved.


## Answer

Resolved through two live rounds on 2026-10-04: the owner accepted all recommendations.

### Post-setup surface and access

Reuse the first-run download controls with post-setup recovery wording, such as
“Download the voice model to resume dictation.” Do not replay Welcome or accessibility
consent merely because the model is missing. Existing setup, permissions and local data
are preserved. Full app-data clearing is different: it removes local setup as well, so
it follows first-run setup rather than pretending the previous setup survived.

Model absence or an active download blocks dictation only. History, dictionary, snippets
and settings remain accessible. Settings provides the Voice model recovery action and
shows the current download state. When a Dictation exhausts eligible providers because
of a missing model, the Bubble explains the problem and offers that action. A missing
model in an unused adapter does not interrupt a Dictation another eligible adapter can
serve. Keep the router's exclusion reason distinct from permission, language, network or
other failures; those failures must not be presented as a missing-model problem.

Downloading requires an explicit tap. The fetching surface retains the settled permanent
footer in idle, running, failed and ready states: download size, peak free space, pinned
source and integrity check. Retain one progress indicator, named phases, meaningful
failure actions and the metered-connection choice. POST_NOTIFICATIONS is not requested
on this screen.

### Completion and lifetime

Verification and successful model preparation lead to “Ready to dictate”. The microphone
stays closed and the user must make a fresh Bubble tap, with fresh target and safety
checks. Never restart the Dictation that opened recovery automatically.

Navigating to History or settings while the app remains open does not cancel the download.
Settings retains access to its state and an explicit Cancel action. If the app process
ends, offer Retry on reopening; reopening alone never starts another fetch. This promises
no unattended background completion or byte-range resume. The existing Model Store
fetch/verify/extract path can recover missing required files without reinstalling.

### Facts corrected

`isModelPresent()` is Boolean; `modelDirectory()` is nullable. The model's persistent
`noBackupFilesDir` location is not a cache directory, so Android's documented low-storage
cache eviction is not evidence for routine removal of these files. The model is excluded
from Auto Backup, making restoration without weights a relevant case if other app data
is restored. This does not decide which OpenFlow data is eligible for backup.

Sources: [Android app-specific storage](https://developer.android.com/training/data-storage/app-specific)
and [Android Auto Backup](https://developer.android.com/identity/data/autobackup).

### Handoff

ADR-0013 now records the post-setup entry point. This is a planning resolution; production
UI and download-lifecycle implementation remain to be built. No acceptance-gate scenario
or model-size headline decision is settled here. No further fog graduated.
