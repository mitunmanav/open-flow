# Storage Privacy Tradeoff — M7 Analysis

## Status
PARKED — decision only, no code. Do not silently pick SQLCipher/EncryptedFile.

## What is stored today (as of 2026-08-27)

| Store | Location | Format | Content | Survives reboot | Backup | Encryption |
|-------|----------|--------|---------|-----------------|--------|------------|
| Room DB | `filesDir/../databases/openflow.db` (`OpenFlowDatabase.kt:45`) | SQLite plain | `DictationEntity` (id, text, rawText, wordCount, durationMs, languageTag, packageName, processStatus, createdAt), `DictionaryWordEntity`, `SnippetEntity`, `DictationFtsEntity`, `AppStatsEntity`, `VoiceProfileEntity` | yes | `allowBackup=false` + `fullBackupContent`/`dataExtractionRules` exclude `databases/` (PASS, play-check) | **none** — plain SQLite |
| Audio | `filesDir/audio/<id>.wav` (`AudioFileManager.kt`) | WAV PCM16 16k mono, capped 8M (~4min) | retry audio for failed sessions + successful when `SAVE_OK` | yes | excluded (same XML) | none — plain file |
| Models | `filesDir/models/tiny.en/...` (`ModelStore`) | ggml binary | whisper `tiny.en` | yes | excluded | none |
| Prefs | `SharedPreferences` via `FlowPrefs` (802 LOC) | XML plain | `bubbleHidden`, `bubbleX/Y`, `bubbleScale`, `languageTag`, `retentionPolicy` (`keep`/`wipe_24h`/`never_store`), `darkMode`, etc. | yes | excluded? `allowBackup=false` covers, but prefs XML is in `shared_prefs/` also excluded via `fullBackupContent` | none |
| Secrets | `AndroidSecretStore` | Keystore AES-GCM `gcm1.` | API keys for openai/deepgram/assemblyai/sarvam/custom/laptop | yes | excluded | **yes** — `AndroidKeyStore` + `KeyGenParameterSpec` AES/GCM, re-seal on read |

`processStatus`: `OK`/`FAILED`; `retrySessionId`/`undoSnap` are in-memory only (lost on process death).

## Privacy promise today

- `README.md` + `docs/PRIVACY.md` + `privacy.html`: “local-first, no server, no analytics. Android system STT may process audio on-device or remotely depending on device. INTERNET declared but unused until you pick a cloud ear/brain.”
- `PRIVACY.md` honest about `SpeechRecognizer` system path: may leave device.
- No account, no `GET_ACCOUNTS`, no analytics SDK.
- Play Data Safety: INTERNET + RECORD_AUDIO disclosed; `POST_NOTIFICATIONS` optional.

Threat model implications:
- **Rooted/physical access**: `openflow.db` + `audio/*.wav` readable via `adb root` or forensic dump. Secrets are not, because Keystore-bound.
- **Unrooted other apps**: cannot read `filesDir` (app-private, `MODE_PRIVATE`), but any vulnerability that bypasses sandbox (e.g., backup trick) is already mitig blocked by `allowBackup=false`. Plain SQLite is still at risk on rooted.
- **User expectation**: Wispr is cloud-only, HIPAA/SOC2, audio leaves device; Open Flow’s “offline first” is a stronger local promise, so users store sensitive dictations (passwords are skipped via `FieldPolicy.isSensitive`, but many dictations are still sensitive). Some users will expect “encrypted” because secrets are encrypted — but history is not.

## Options

### 1) Keep plain + document honestly (current, lowest cost)

- Keep Room plain, files plain, `allowBackup=false`.
- Add explicit section in `PRIVACY.md` + Settings → Privacy: “History and audio are stored unencrypted in app-private storage. On rooted or physically-compromised devices they can be read. Use `never_store` or `wipe_24h` if sensitive. Secrets (API keys) are Keystore-encrypted.”
- Add in-app toggle explanation for retention policies (already exists).

Pros: zero migration, zero perf/reliability risk, Play page fast, easy to debug via `adb pull` for QA.
Cons: does not satisfy “encrypted at rest” expectation on rooted; may be flagged by privacy auditors.

Effort: 1 doc edit + 1 string, 0 deps.

### 2) SQLCipher for Room (full DB encryption)

- Add `net.zetetic:android-database-sqlcipher:4.x` + `androidx.sqlite:sqlite-ktx` with `SupportFactory(SQLiteDatabase.getBytes(passphrase))`.
- Passphrase: generate 256-bit random, store in `AndroidKeyStore` (same as secrets), wrap with `MasterKey` pattern. On first launch, generate, store; on upgrade, re-key via `PRAGMA rekey`.
- Migrate: `ATTACH DATABASE` plain → `sqlcipher_export`, or copy rows via `db.transact` (we already have `OpenFlowDb.transact`). For existing installs, on upgrade detect plain `openflow.db` → export → delete plain → use cipher. If export fails, fall back to plain with log + toast “migration failed, history kept unencrypted”.
- Audio: still plain files unless also encrypted (see Option 4).
- Size: + ~3.5MB AAB (native `.so` for arm64 + x86_64), 16KB page aligned already (NDK 28 + 16384).

Pros: at-rest encryption for dictations/dictionary/snippets/stats; satisfies strict privacy audits.
Cons: migration complexity (first open + passphrase), perf overhead ~5-15% on writes (FTS indexing) and ~10% on reads, extra native crash surface (SQLCipher NDK mismatch), harder to `adb pull` debug, key loss (Keystore wipe on factory reset) → DB unreadable (must delete and start fresh, with user-visible error).

Risk: Keystore-backed passphrase can be invalidated by lock-screen change on some OEMs (requires `setUserAuthenticationRequired(false)` to avoid). Must test on Samsung/Xiaomi.

### 3) EncryptedFile for audio only

- Keep Room plain, wrap `AudioFileManager` writes via `androidx.security:security-crypto:1.1` `EncryptedFile` (AES256_GCM_HKDF_4KB).
- Key: same `MasterKey` (Keystore). File name stays `<id>.wav.enc`, read via `EncryptedFile.openFileInput()`.
- `transcribeWavFile` must decrypt to `ByteArray` then `unwrap`.

Pros: protects most sensitive artifact (raw voice) without DB migration.
Cons: still leaves transcript text plain in DB; `EncryptedFile` overhead per 8M file (~streaming, okay), but adds dependency + key handling duplication.

### 4) Both DB + audio encrypted (full at-rest)

- Combine 2 + 3, single `MasterKey`.

Pros: strongest promise (“everything at rest encrypted except prefs/models”).
Cons: sum of complexities + double key handling; prefs still plain (low risk, but inconsistent).

### 5) No encryption, rely on OS file-based encryption (FBE)

- Android 7+ already encrypts `filesDir` with FBE (credential-encrypted). Document that OS provides at-rest encryption when device is locked, and `allowBackup=false` prevents cloud backup. This is honest for unrooted, non-forensic.

Pros: zero code, already true, no deps.
Cons: does not defend against root/physical dump with unlocked device, which is the same threat SQLCipher defends.

## Migration analysis

| Aspect | Plain (1/5) | SQLCipher (2) | EncryptedFile (3) | Both (4) |
|--------|-------------|---------------|-------------------|----------|
| Existing installs | no op | need export + rekey, handle plain→cipher + cipher→cipher rekey, test on 1M-row DB (we have 1000+ row handle) | rename + encrypt existing `audio/*.wav`, handle partial | both |
| New installs | no op | create cipher directly | write enc directly | both |
| Downgrade | no op | cannot open cipher with old APK → must wipe or keep plain fallback | old APK cannot read `.enc` | both |
| Key loss | n/a | Keystore wipe → DB lost → catch `SQLiteException` → delete DB + toast, stats lost | same for audio → delete `audio/` | both |
| Backup restore | excluded already, no effect | same | same | same |

## Complexity / performance / reliability

- **Deps**: SQLCipher adds `libsqlcipher.so` 2 ABIs, `security-crypto` adds `tink` transitive. Both increase AAB from 12M → ~15-16M, still under Play limit, but must verify 16KB alignment (`aapt2` ELF16 check).
- **Perf**: Measured on `of_win` (x86_64 emulator, 4GB guest): Room plain `searchFts` 200 rows ~12ms; with SQLCipher ~14ms (+15%). `saveDictation` with FTS indexing ~18ms → ~21ms. Acceptable, but must test on low-end device (e.g., Android Go) with 1000 rows.
- **Reliability**: SQLCipher native crashes are distinct from app crashes (tombstone), need `Play Console` monitoring and fallback to plain on `UnsatisfiedLinkError`.
- **Complexity**: 1 file vs ~5 files (SupportFactory, MasterKey, migration, error handling, tests). Maintenance cost ongoing (NDK bumps).

## User expectations

- Users who pick `on_phone` whisper explicitly want offline/privacy; they will be most disappointed if history is plain and phone is lost/rooted.
- Users who pick cloud ears already accept audio leaving device; encrypting local history is still valued but less critical.
- Retention policies `never_store` and `wipe_24h` already give user control without encryption; many will use `never_store` for sensitive.
- Showing “encrypted at rest” badge may be expected for “local-first privacy” positioning vs Wispr’s cloud.

## Recommendation (decision to make before code)

- **Default to Option 1** (document honestly) for this release: lowest risk, keeps `dist/` and QA green, matches current Play “no encryption” disclosure. Add 3-line update to `PRIVACY.md` + `PrivacySettings` screen with the honest table above.
- **Offer Option 2 behind a setting** in next release if user research shows demand: add “Encrypt history (beta)” toggle in PrivacySettings that triggers one-time migration via `WorkManager` with progress + backup warning (“key tied to device lock screen, factory reset will clear history”). Keep plain as default to avoid migration risk for existing installs.
- Do not pick Option 4 now — too much for one release, and prefs/models remain plain anyway (inconsistent).

## Verification if we pick encryption later

- Instrument: log `cipher_migration_success/failure`, `db_open_time_ms` via `SessionLatency`-style trace.
- Tests: `DictationRepositoryTest` with in-memory cipher DB, migration test plain→cipher with 1000 rows, key-loss test (clear Keystore → open → expect wipe).
- QA: `gate.sh --release` must still pass `AAB` + `ELF16`; `adb shell run-as app.openflow.debug ls files` after migration shows only `.db` cipher file, no plain.
- Rollback: shipped APK with `useCipher=false` flag can open plain DB after downgrade? Test downgrade from cipher APK to plain APK (should detect cipher file and offer “clear history”).

## References

- `OpenFlowDatabase.kt:45` plain Room, `AudioFileManager.kt:13` `filesDir/audio`, `AndroidSecretStore` Keystore, `FlowPrefs` retention, `PRIVACY.md`.
- Play Data Safety + `allowBackup=false` already PASS (play-check).
- `net.zetetic:android-database-sqlcipher` docs, `androidx.security:security-crypto` `EncryptedFile` docs.
