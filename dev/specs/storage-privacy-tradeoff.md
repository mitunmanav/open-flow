# Storage Privacy Tradeoff — M7 Analysis (rev2)

## Decision

- **STATUS:** PARKED — revised. **DO NOT IMPLEMENT** until decisions below are approved.
- **DECISION REQUIRED:** Pick at-rest encryption mode before any code. See §5.
- **DECISION:** Recommended `M7-plain` (keep app-private plain + `allowBackup=false` + honest `PRIVACY.md`) for this release. `M7-cipher` (Room via current `net.zetetic:sqlcipher-android`) is opt-in only behind a setting with explicit migration, and only after review of §6–§9.
- **DO NOT IMPLEMENT UNTIL:** (1) encryption mode signed off, (2) Room 2.8.4 + `sqlcipher-android` + `androidx.sqlite` versioning verified on `of_win`, (3) `EncryptedFile` removal acknowledged, (4) FBE vs app-layer threat model accepted, (5) key lifecycle + 16 KB verification + downgrade test plan approved.
- **NEVER:** After a migration failure, silently keep or revert to plaintext without a user-visible signal and a non-blank passphrase requirement — §8 forbids encrypted→plaintext silent fallback.

## Material changes from rev1 (2026-08-27)

1. **Removed `EncryptedFile` as an implementation choice** — `androidx.security:security-crypto` `EncryptedFile` is `@Deprecated` since `1.1.0-alpha07` /**“Deprecated all APIs in favour of existing platform APIs”** and class javadoc **“This class is deprecated. Use `java.io.File` instead.”** ([releases](https://developer.android.com/jetpack/androidx/releases/security))([api ref](https://developer.android.com/reference/androidx/security/crypto/EncryptedFile)). Deleted Option 3 (“EncryptedFile for audio only”) and replaced any audio-file encryption with Keystore AES-GCM streaming (not JetSec) if ever pursued.
2. **Replaced legacy `net.zetetic:android-database-sqlcipher` with current `net.zetetic:sqlcipher-android`** — old artifact **“no longer being updated”** ([README](https://github.com/sqlcipher/android-database-sqlcipher)), new artifact introduced 2022 and requires migration ([migration guide](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-migration/)). All SQLCipher references now use `sqlcipher-android` + `SupportOpenHelperFactory`.
3. **Inspected actual `Room 2.8.4` in Open Flow** (`app/build.gradle.kts:161`) and specified correct integration path for Room 2.8.4/2.x via `androidx.sqlite:sqlite:2.7.0`, `System.loadLibrary("sqlcipher")`, `SupportOpenHelperFactory` (Room 2 and 3) ([sqlcipher-android README](https://github.com/sqlcipher/sqlcipher-android)).
4. **Distinguished app-layer encryption from platform FBE/app-private** — added §2.1 / §4 contrasting FBE (credential-encrypted, device-level) vs `MODE_PRIVATE` vs SQLCipher page-level encryption ([FBE docs](https://source.android.com/docs/security/features/encryption/file-based)).
5. **Added Android backup / device-transfer caveat** — `allowBackup=false` + `dataExtractionRules`/`fullBackupContent` already exclude all domains, but `cloud-backup` vs `device-transfer` distinction and AUTO backup end-to-end rules now documented (§3) ([backup guide](https://developer.android.com/identity/data/autobackup))([security backup recs](https://developer.android.com/privacy-and-security/risks/backup-best-practices)).
6. **Forbade silent encrypted→plaintext fallback** after migration failure — §8 defines fail-loud behavior with persisted `cipher_migration_failed` flag, user toast/dialog, and no automatic plaintext reopen.
7. **Defined exact attacker/threat model** application-layer encryption defends against — §2.
8. **Defined exactly which Open Flow data would be encrypted** in any future cipher mode — §2.2 table per store/file.
9. **Defined key lifecycle, Keystore config, migration, failure recovery, downgrade, and 16 KB/native verification** — §6–§9, §11.
10. **Removed truncated/legacy links, fixed Room/sqlite version mentions, added 16 KB alignment check for `libsqlcipher.so`.**

## 1. What is stored today (as of 2026-08-27, inspected)

| Store | Location | Format | Content | Survives reboot | Backup | Encryption |
|-------|----------|--------|---------|-----------------|--------|------------|
| Room DB | `getDatabasePath("openflow.db")` (`OpenFlowDatabase.kt:45`) | SQLite plain via `androidx.room:room-runtime:2.8.4` | `DictationEntity` (id, text, rawText, wordCount, durationMs, languageTag, packageName, processStatus, createdAt), `DictionaryWordEntity`, `SnippetEntity`, `DictationFtsEntity` (FTS5), `AppStatsEntity`, `VoiceProfileEntity` | yes (CE storage) | excluded (§3) | **none** |
| Audio | `filesDir/audio/<id>.wav` (`AudioFileManager.kt`) | WAV PCM16 16 kHz mono, `CaptureCap 8M` (~4 min) | retry audio for failed + successful when `SAVE_OK` | yes (CE) | excluded | none |
| Models | `filesDir/models/tiny.en/...` | ggml binary | whisper `tiny.en` | yes | excluded | none |
| Prefs | `SharedPreferences` via `FlowPrefs` (`shared_prefs/`) | XML plain | `bubbleHidden`, `bubbleX/Y`, `bubbleScale`, `languageTag`, `retentionPolicy` (`keep`/`wipe_24h`/`never_store`), `darkMode`, `sttTuning`, `EnginePrefs` | yes | excluded | none |
| Secrets | `shared_prefs/openflow_secrets.xml` via `AndroidSecretStore` | `gcm1.` blobs | API keys (openai/deepgram/assemblyai/sarvam/custom/laptop) | yes | excluded | **yes** — `AndroidKeyStore` AES-256 `KeyGenParameterSpec` `BLOCK_MODE_GCM` `ENCRYPTION_PADDING_NONE` `PURPOSE_ENCRYPT|DECRYPT` (see `SecretStore.kt:112`) |

`processStatus: OK/FAILED`; `retrySessionId`/`undoSnap` in-memory only.

## 2. Threat model (exact)

Application-layer SQLCipher protects against:

- **(T1) Offline forensic dump of app-private files after device loss/theft with unlocked CE storage readable** — attacker gets `filesDir` bytes (e.g., via `adb root`, chip-off, or privileged backup exploit). With FBE alone, CE files are decrypted once device is booted and user has unlocked once; physical dump after first unlock yields plaintext. SQLCipher page encryption defends here.
- **(T2) Rooted device or privilege escalation where sandbox (`MODE_PRIVATE`) is bypassed but Keystore remains hardware-backed** — other apps or `run-as` bypass could read plain SQLite but cannot extract Keystore AES key material without TEE/StrongBox compromise.
- **Not** defended: (N1) live memory compromise while DB is open (key in process RAM), (N2) Keystore wiped/factory-reset, (N3) compromised OS that exfiltrates during `open()`, (N4) cloud STT audio already sent off-device when cloud ear chosen (out of scope).

Platform FBE + `MODE_PRIVATE` already defends unrooted, non-forensic case (locked or not-rooted). Application-layer adds defense only for T1/T2. That is the intended payoff, and its cost is justified only if users store sensitive dictations on devices at risk of loss with `keep` retention.

## 2.1 FBE vs app-private vs app-layer

- **FBE (File-Based Encryption):** Since Android 7, `filesDir` is credential-encrypted (CE) and device-encrypted (DE). All devices launching with Android 10+ **must** use FBE ([FBE docs](https://source.android.com/docs/security/features/encryption/file-based)). CE unlock requires LSKF (PIN/pattern/password) after boot (`vold` + `Keymaster HAL`). While locked, CE files are not readable. After first unlock, CE files are decryptable by kernel, so a live-unlocked forensic dump sees plaintext unless app-layer encrypts.
- **App-private (`MODE_PRIVATE`):** Kernel DAC + SELinux prevents unrooted other apps from reading `filesDir`. No crypto.
- **App-layer (SQLCipher):** Page-level AES-256 with PBKDF2-HMAC-SHA512 per DB salt (SQLCipher docs). Transparent to Room queries; DB header is randomized, not SQLite magic. Protects T1/T2 even after CE unlock, at cost of key lifecycle.

## 2.2 Exactly what would be encrypted in M7-cipher

If `M7-cipher` is ever built, it encrypts:

- **Encrypted:** Room DB file + WAL (`openflow.db`, `openflow.db-wal`, `openflow.db-shm`) — all dictations, dictionary words, snippets, FTS index, app stats, voice profiles. Each is DB page data.
- **Not encrypted in M7-cipher (still plain):** `filesDir/audio/*.wav`, `filesDir/models/*`, `shared_prefs/*.xml` (`FlowPrefs`, `openflow_secrets.xml` ciphertext blobs are already Keystore-wrapped but prefs file structure is plain), `no-backup` caches, logs. Audio encryption would be a separate `M7-file` mode (not `EncryptedFile`, see §5) — not included in this spec’s `M7-cipher`.
- **Already encrypted:** `openflow_secrets` values (`gcm1.`) at rest via `AndroidKeyStore`.

Saying “everything at rest encrypted” would be false for `M7-cipher` — audio and prefs remain plain by design.

## 3. Backup / transfer caveat

Current posture (verified `AndroidManifest.xml:36-38`):

- `android:allowBackup="false"`, `android:fullBackupContent="@xml/backup_rules"`, `android:dataExtractionRules="@xml/data_extraction_rules"`
- `data_extraction_rules.xml` and `backup_rules.xml` exclude `<cloud-backup>` and `<device-transfer>` for domains `root`/`file`/`database`/`sharedpref`/`external`. This is the “exclude all” pattern for `allowBackup=false` equivalent on Android 12+ ([Auto Backup docs](https://developer.android.com/identity/data/autobackup))([SO allowBackup](https://stackoverflow.com/questions/70365809/how-to-specify-to-not-allow-any-data-backup-with-androiddataextractionrules)).
- Standard backup, when used, is encrypted in transit and at rest, and end-to-end with lock-screen secret on Android 9+ if set ([security recs](https://developer.android.com/privacy-and-security/risks/backup-best-practices)). Our exclusion makes those guarantees moot — no history leaves via backup.

**Caveat:** If a future `M7-cipher` is shipped, backup exclusion must remain. If backup is ever re-enabled, encrypted DB backup without the Keystore key (which is not backed up) would be unrecoverable — restore would see `SQLiteException: file is not a database`. Device-to-device (`device-transfer`) transfer also excluded today; enabling it would have same key-loss implication. Spec recommends keeping exclusion forever for encrypted builds.

## 4. Privacy promise today (unchanged)

- `README.md` + `docs/PRIVACY.md`: “local-first, no server, no analytics. Android system STT may process audio on-device or remotely. INTERNET declared but unused until you pick a cloud ear/brain.”
- `PRIVACY.md` honest about `SpeechRecognizer` may leave device.
- No account, no `GET_ACCOUNTS`, no SDK.
- Play Data Safety: `RECORD_AUDIO` + `INTERNET` disclosed.

With `M7-plain`, need explicit `PRIVACY.md` + Settings → Privacy row: “History, dictionary, snippets, and audio are stored **unencrypted** in app-private storage, protected by Android sandbox and FBE while locked. On rooted or physically-dumped unlocked devices they can be read. Use `never_store` / `wipe_24h` if sensitive. API keys are `AndroidKeyStore`-encrypted.”

## 5. Options (revised — EncryptedFile removed)

### M7-plain — Keep plain + document honestly (current, recommended this release)

- Keep Room 2.8.4 plain, files plain, `allowBackup=false` + exclusions.
- Docs change only (§4).

Pros: zero migration, zero native crash surface, `adb pull` debuggable, `gate.sh` green.
Cons: T1/T2 not defended; auditors expecting “encrypted at rest” will flag.

Effort: 1 doc + 1 string.

### M7-cipher — SQLCipher for Room via current `sqlcipher-android` (opt-in later)

- **Deps (verified for Room 2.8.4):**

  ```kotlin
  implementation("net.zetetic:sqlcipher-android:4.18.0@aar")
  implementation("androidx.sqlite:sqlite:2.7.0") // required by sqlcipher-android; works with Room 2.8.4 (Room 2 + 3 support)
  // Room 2.8.4 already: androidx.room:room-runtime:2.8.4 + room-ktx
  ```

  Legacy `net.zetetic:android-database-sqlcipher:4.x` is **not** used ([Zetetic migration](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-migration/) — “introduced… 2022 … replacement API”).

- **Load:** `System.loadLibrary("sqlcipher")` before any `Room.databaseBuilder` (must precede `SupportOpenHelperFactory` use) ([README](https://github.com/sqlcipher/sqlcipher-android)).

- **Passphrase:** 32-byte CSPRNG (`SecureRandom`) generated on first launch, wrapped via `AndroidKeyStore` AES-GCM (same pattern as `SecretStore.KeystoreAes`: `KeyGenParameterSpec` `BLOCK_MODE_GCM` `ENCRYPTION_PADDING_NONE` 256-bit, `setUserAuthenticationRequired(false)`, no `setUserAuthenticationValidityDurationSeconds`). Store wrapped passphrase in `SharedPreferences` as `gcm1.` blob (like secrets), **not** as char. Alternative: generate raw passphrase and derive via `PBKDF2-HMAC-SHA512` inside SQLCipher (SQLCipher does this per DB salt); spec stores the random passphrase bytes, not a user password.

- **Factory:**

  ```kotlin
  val passphrase = SQLiteDatabase.getBytes(storedPassphraseChars) // or ByteArray variant
  val factory = SupportOpenHelperFactory(passphrase, hook = null, clearPassphrase = true) // single-use if true
  // or val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphrase)
  Room.databaseBuilder(ctx, OpenFlowDatabase::class.java, "openflow.db")
      .openHelperFactory(factory)
      .build()
  ```

  Three-ctor form (`passphrase`, `hook`, `clearPassphrase`) mirrors legacy `SupportFactory` — set `clearPassphrase=true` means single-use; document single-use. New code uses `SupportOpenHelperFactory` ([integration guide](https://github.com/sqlcipher/android-database-sqlcipher#using-sqlcipher-for-android-with-room) legacy vs [new README](https://github.com/sqlcipher/sqlcipher-android)).

- **Scope:** DB only. Audio stays plain (no `EncryptedFile`). If audio encryption is needed later, use platform `Cipher` streaming (`AES/GCM/NoPadding` with `KeystoreAes` key + per-file IV) — not `EncryptedFile` ([deprecated](https://developer.android.com/reference/androidx/security/crypto/EncryptedFile)).

- **Size:** `libsqlcipher.so` for `arm64-v8a` + `x86_64` adds ~3–4 MB AAB; NDK `28.2.13676358` + `-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON` + `graphics-path:1.1.0` must still pass 16 KB alignment check (`scripts/qa/play-check.sh` `aapt2` `ELF16`).

Pros: defends T1/T2 for transcript history (most-sensitive text).
Cons: migration complexity, perf +5–15% writes (FTS), native crash surface, key-loss → DB lost, debug `adb pull` requires key, Play AAB larger, ongoing dep bumps.

### M7-file — (not proposed) App-layer file encryption for `audio/*.wav`

Would use `javax.crypto` streaming (`AES/GCM` per file, IV per file, Keystore key). Not in current triage because `EncryptedFile` is deprecated and duplication is high, and `M7-plain` retention policies already give users `never_store`. If pursued later, specify Tink or `Cipher` streaming with streaming-AAD and file-name `*.wav.gcm` — not part of this `M7-cipher` spec.

### M7-fbeDoc — Rely on FBE + app-private only (documented variant of M7-plain)

Zero code, but make FBE explicit in `PRIVACY.md` (§2.1). Already covered by `M7-plain`. Listed separately only to contrast with `M7-cipher`.

## 6. Key lifecycle (for any future M7-cipher)

- **Generation:** `SecureRandom().nextBytes(32)` once, on first `Room` open under `M7-cipher` preference enabled. No user password, no `PBKDF2` in app (SQLCipher does its own PBKDF2 per DB salt).
- **Storage:** Wrap with `KeystoreAes` (same `ALIAS="openflow_secrets_aes"` or dedicated `openflow_db_passphrase` alias; if dedicated, same `KeyGenParameterSpec` as `SecretStore.kt:112-119`: `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, `256`, `PURPOSE_ENCRYPT|DECRYPT`, `setUserAuthenticationRequired(false)` to avoid lock-screen-change invalidation on Samsung/Xiaomi). Persist wrapped blob in `SharedPreferences` (`M7_PREFS="m7_cipher"` key `passphrase_gcm`) as `gcm1.` base64 (IV+CT). Not in plaintext.
- **Use:** On each `Room` init, unwrap passphrase via `KeystoreAes`, `SQLiteDatabase.getBytes()` or direct `ByteArray`, pass to `SupportOpenHelperFactory`. If `clearPassphrase=true`, recreate factory per `Room` instance (do not reuse bytes after zeroed).
- **Rotation / rekey:** Not scheduled. `PRAGMA rekey` only if future passphrase rotation UX added. Not in v1.
- **Invalidation:** If `KeyStore` `getKey()` throws `KeyPermanentlyInvalidatedException` (rare with `setUserAuthenticationRequired(false)` but possible on hardware reset), treat as key loss (§8). Do not attempt `MasterKey` `setUserAuthenticationValidityDurationSeconds`.
- **Hardware:** Prefer `isStrongBoxBacked` if available, but do not require it; Keystore without StrongBox still defends T2 against `MODE_PRIVATE` bypass.

## 7. Migration (§8 failure handling forbids silent fallback)

- **Detection:** On open, try `SupportOpenHelperFactory(passphrase)` open. If `SQLiteException: file is not a database` / `not a database` or header is plain SQLite `SQLite format 3\x00`, run migration.
- **Migration path (existing plain → cipher):** Inside `SQLiteDatabaseHook` or helper: `ATTACH DATABASE plain AS plain KEY '' ; SELECT sqlcipher_export('main', 'plain'); DETACH plain;` then atomic rename: plain file → `openflow.db.plain.bak`, cipher file → `openflow.db`. Or row-copy via `OpenFlowDb.transact` for small DB. For new installs, create cipher directly.
- **Atomicity:** Write cipher to `openflow.db.cipher.tmp`, `fsync`, then `rename` over `openflow.db`. Delete tmp on failure. Never partially-overwrite original plain until cipher integrity verified (`PRAGMA cipher_integrity_check` or `SELECT count(*) FROM sqlite_master`).
- **No silent fallback:** If migration fails (e.g., `ATTACH` fails, disk full, `sqlcipher_export` error), **do not** reopen as plain and pretend success. Persist `m7_migration_failed=true` in prefs, keep plain DB intact, notify user: toast/dialog “History encryption failed — history kept unencrypted. Retry or keep plain?” Log `cipher_migration_failure`. Require explicit user action to retry or to keep plain (which sets `M7_PREFS cipher_enabled=false`). The DB remains plain and next launch will offer migration again; the app must never write new rows to an assumed-cipher DB while actually plain.

## 8. Failure recovery / downgrade

- **Key loss (Keystore wipe, factory reset, `secdiscardable` loss):** `KeystoreAes.getOrCreateOrNull()` returns null or unwrap fails → DB cannot be opened (`SQLiteException`). Catch, persist `m7_key_lost=true`, show blocking dialog: “Encryption key unavailable — history cannot be opened. Delete history to continue or uninstall.” Offer `Delete history (wipe openflow.db* and audio/ + prefs flag)` vs `Close app`. **Never** create a new DB silently over the old cipher file without user consent (would leak old cipher bytes undeleted).
- **Downgrade (cipher APK → plain APK):** Plain APK cannot open cipher file — will get `file is not a database`. Must detect header not SQLite and show “History is encrypted. Please update or clear history.” via `Play` update prompt. Not silent wipe.
- **Plain → cipher Downgrade after flag flipped false:** If user disables `M7-cipher` after migration, not in v1 — require “Disable encryption requires clearing history” confirmation; do not decrypt-to-plain silently.

## 9. 16 KB / native-library verification

- **Build:** Still `ndkVersion = "28.2.13676358"` with `externalNativeBuild.cmake.arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"` (`app/build.gradle.kts:28`). All native `.so` must be 16 KB ELF aligned (Android 15+ 16 KB page requirement). `sqlcipher-android` 4.18.0+ is expected 16 KB-compatible; verify per Zetetic post ([16KB page support](https://www.zetetic.net/blog/2025/06/26/sqlcipher-for-android-16kb-page-size-support/)). Verify alongside `graphics-path:1.1.0` (already 16 KB aligned).
- **Checks:** `scripts/qa/play-check.sh` must pass `AAB` + `ELF16` after adding `libsqlcipher.so` for `arm64-v8a` + `x86_64`. CI should run `aapt2` / `readelf --program-headers` or `play-check.sh`’s `ELF16` check. If `libsqlcipher.so` is not 16 KB aligned, **do not ship** — pin to fixed version with alignment.
- **Testing:** `gate.sh --release` still passes `bundleRelease` + `aapt2` target 36 + page size 16384.

## 10. User expectations / retention

- `on_phone` users (offline-first) most sensitive to T1; `cloud` users already accept audio leaving device but still value local text encryption.
- `retentionPolicy = never_store` / `wipe_24h` already gives local control without crypto; many will use `never_store` for sensitive utterances. `M7-plain` makes this explicit in UI.

## 11. Complexity / performance / reliability (revised)

- **Deps:** `sqlcipher-android` adds `libsqlcipher.so` 2 ABIs + `androidx.sqlite:sqlite:2.7.0`. `security-crypto` is **not** added ( `EncryptedFile` deprecated ) — avoids `tink` transitive. AAB ~ +3–4 MB.
- **Perf:** Same estimate as rev1 — Room plain `searchFts` 200 rows ~12 ms → ~14 ms cipher (+~15%), `saveDictation` with FTS indexing ~18 ms → ~21 ms. Acceptable, but must measure on low-end device (Android Go) with 1000 rows and on `of_win`. No claim of improvement.
- **Reliability:** Native crashes are tombstones distinct from Java crashes; monitor via Play Console. Do not add `UnsatisfiedLinkError` fallback to plain (that would be silent fallback). `System.loadLibrary("sqlcipher")` failure is fatal for `M7-cipher` mode → show dialog and fall back to `M7-plain` only with user-visible failure flag (§8).

## 12. Recommendation (decision to make before code) — unchanged

- **Default to `M7-plain`** for this release: lowest risk, QA green, honest `PRIVACY.md`. One doc edit + one string.
- **Offer `M7-cipher` behind a `Settings → Privacy → Encrypt history (beta)` toggle next release** if research shows demand. Behind toggle: one-time `WorkManager` migration with progress + warning “key tied to this device, factory reset will clear history.” Default off to avoid migration risk for existing installs. Do not ship auto-migrate on first launch.

## 13. Verification if M7-cipher is pursued later

- Instrument: `cipher_migration_success/failure`, `db_open_time_ms` via `SessionLatency`-style trace; no PII in logs.
- Tests: `DictationRepositoryTest` with in-memory cipher DB via `SupportOpenHelperFactory`; migration test plain→cipher with 1000 rows; key-loss test (clear Keystore → open → expect `m7_key_lost` dialog, not silent plain).
- QA: `gate.sh --release` must still pass `ELF16`; `adb shell run-as app.openflow.debug ls databases/` shows `openflow.db` + `openflow.db-wal` with header not `SQLite format 3`, no `openflow.db.plain` leak; downgrade test plain→cipher→plain shows correct dialog.
- Rollback: shipped APK with `M7-cipher` flag `false` does not open cipher DB silently; test downgrade from cipher APK to plain APK detects cipher and offers clear.

## 14. References (authoritative)

- `Room 2.8.4` in `app/build.gradle.kts:161-163` + `androidx.sqlite:sqlite:2.7.0` requirement for `sqlcipher-android` ([sqlcipher-android README](https://github.com/sqlcipher/sqlcipher-android)).
- Legacy vs current artifact: `android-database-sqlcipher` **no longer updated** ([README](https://github.com/sqlcipher/android-database-sqlcipher)) vs `sqlcipher-android` replacement ([migration guide](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-migration/)).
- `Room + SQLCipher` via `SupportOpenHelperFactory` / `SupportFactory` + `System.loadLibrary("sqlcipher")` ([legacy guide](https://github.com/sqlcipher/android-database-sqlcipher#using-sqlcipher-for-android-with-room)) and ([current README](https://github.com/sqlcipher/sqlcipher-android)).
- `EncryptedFile` **deprecated** since `security-crypto 1.1.0-alpha07` /**“Deprecated all APIs…”** ([releases](https://developer.android.com/jetpack/androidx/releases/security)) and class deprecation notice ([api ref](https://developer.android.com/reference/androidx/security/crypto/EncryptedFile)).
- FBE: All Android 10+ devices **required** to use FBE, CE/DE, `Keymaster HAL` ([FBE docs](https://source.android.com/docs/security/features/encryption/file-based)).
- Backup: `dataExtractionRules`/`fullBackupContent` cloud + device-transfer exclusion pattern ([Auto Backup](https://developer.android.com/identity/data/autobackup)) and security guidance ([security recs](https://developer.android.com/privacy-and-security/risks/backup-best-practices)).
- Open Flow impl: `OpenFlowDatabase.kt:45`, `AudioFileManager.kt:13`, `AndroidSecretStore` `KeystoreAes` `KeyGenParameterSpec` (`SecretStore.kt:112`), `FlowPrefs` retention.
- 16 KB: NDK `28.2.13676358` + `ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON` + `graphics-path:1.1.0` ([page sizes](https://developer.android.com/guide/practices/page-sizes)) and SQLCipher 16 KB page support ([Zetetic](https://www.zetetic.net/blog/2025/06/26/sqlcipher-for-android-16kb-page-size-support/)).
- Play checks: `allowBackup=false` PASS (`play-check.sh`).

