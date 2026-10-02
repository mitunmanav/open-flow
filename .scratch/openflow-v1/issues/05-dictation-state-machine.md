# Dictation state machine spec

Type: grilling
Status: resolved
Blocked by: 04

## Question

Specify the Dictation lifecycle (IDLE/PREPARING/RECORDING/TRANSCRIBING/REFINING/INSERTING/DONE + CANCELLED/ERROR_RECOVERABLE/ERROR_FATAL): states, transitions, which component owns each transition, and how cancellation propagates through audio/STT/insertion. Must rule out boolean-flag soup.

## Answer

State machine settled (full detail in `docs/adr/0002-dictation-state-machine.md`):

- States: `Idle`, `Preparing`, `Recording(startedAtMs)`, `Transcribing` (provider finalizing after stop; partials overlap Recording), `Refining(rawText)`, `Inserting(refinedText, targetDescription)`, `Done(transcriptId, outcome)`, `Cancelled`, `ErrorRecoverable(reason, transcriptId?)`, `ErrorFatal(reason)`.
- `Done`/`Cancelled`/`ErrorRecoverable` auto-return to `Idle`; `ErrorFatal` requires re-`Preparing` on next start.
- One `DictationController` owns the `StateFlow<DictationState>`; all other components send `DictationEvent`s through one channel. No booleans; transition table unit-tested.
- `Inserting` is non-cancellable. Cancel elsewhere discards audio, persists nothing.
- Every suspending step has a timeout → `ErrorRecoverable(Timeout)`.
- Refiner failure degrades to raw transcript marked unprocessed.
