# History & recovery storage

Type: grilling
Status: resolved
Blocked by: none

## Question

Confirm history storage: Room vs DataStore vs files, schema (transcript, provider, timings, status, language, feedback), retention/delete controls, and retry state recovery after process death.

## Answer

History storage settled:

- **Room**, one `transcript_entry` table: id, created_at, raw_text?, final_text, provider_id, language, duration_ms, outcome, insertion_target_package?, feedback_score?, audio_path?, retry_count, last_attempt_at?.
- Retry after process death: entries with outcome=error_recoverable and not dismissed surface on History with a retry affordance that re-inserts stored text (no re-transcription).
- Retention: keep indefinitely default; optional N-day purge via periodic Room sweep.
- Pre-1.0: `fallbackToDestructiveMigration` allowed with a settings warning.
- JSON export streams the table via Room DAO query.
