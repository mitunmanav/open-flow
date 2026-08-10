# Security policy — open-flow

## Defaults (non-negotiable)

- No account
- No analytics / ads / trackers
- **No `INTERNET` permission** in base app
- Cleartext HTTP **blocked** (`network_security_config`)
- `allowBackup="false"`
- Mic permission only at runtime, when user records / dictating
- Foreground service type `microphone` for background record
- Local data: app-private storage; encrypt at rest (Keystore / EncryptedFile) when storing audio
- Opt-in online features only; document any new permission in the feature plan
- **Accessibility** used only for: detect editable focus + insert dictated text (Wispr-style bubble). Disclose in UI.
- Skip password / phone / sensitive field types for insert
- Do **not** use accessibility to scrape screen content for analytics

## Threat model (simple)

| Risk | Mitigation |
|------|------------|
| Voice leaves device | Default offline STT; no network perm |
| Backup leak | allowBackup false; data extraction rules exclude |
| Cleartext MITM | NSC cleartext false |
| Over-broad perms | Request only when used |
| Accessibility abuse | Minimal events; insert-only; password skip; open source audit |
| Supply chain | FOSS deps; pin versions in Gradle |

## Reporting

Open an issue in the repo. No bounty program yet.
