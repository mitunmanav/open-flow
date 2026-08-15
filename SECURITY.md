# Security policy — open-flow

## Defaults (non-negotiable)

- No account
- No analytics / ads / trackers
- **`INTERNET` declared.** Unused until user picks a net ear/brain or downloads a model. Do not strip.
- Public hosts: HTTPS only. Cleartext HTTP only for NSC listed LAN literals (no CIDR) + localhost via `network_security_config`. `HostUrl` matches that list.
- API keys: device Keystore + separate prefs file. Never FlowPrefs. Never logs. Never backup.
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
| Voice leaves device | Default system STT + rules. Cloud ear/brain only after user pick. Honesty line per vendor. |
| Backup leak | allowBackup false; fullBackupContent + data extraction exclude all |
| Cleartext MITM | NSC cleartext false except listed LAN literals; HostUrl matches NSC |
| Over-broad perms | Request only when used |
| Accessibility abuse | Minimal events; insert-only; password skip; open source audit |
| Supply chain | FOSS deps; pin versions in Gradle |

## Reporting

Use **GitHub Security Advisories** on this repo (Security tab → Report a vulnerability).

Do not put personal data, phone numbers, or device IDs in public issues.

No bounty program yet.
