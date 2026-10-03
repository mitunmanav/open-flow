# Third-Party Notices

OpenFlow is licensed under Apache-2.0. Runtime dependencies and their licences will be listed here as they are added.

## Vendored assets

Not dependencies, and not loaded from a CDN — both files are committed to
`website/fonts/` so that no visitor's IP address reaches a third party when the
project site loads.

| Asset | Version | Licence | Copyright |
| --- | --- | --- | --- |
| Archivo (variable, latin subset, woff2) | 25 | SIL Open Font License 1.1 | Omnibus-Type |
| JetBrains Mono (variable, latin subset, woff2) | 25 | SIL Open Font License 1.1 | JetBrains |

The OFL requires the licence to accompany the font files, so the licence text is
committed alongside them, verbatim as published, as `website/fonts/OFL.txt`. It
is the unmodified licence rather than a filled-in copy: the copyright lines in
the OFL template are placeholders, and each project's own copyright notice is in
its font metadata. Attribution is the table above; the canonical licence is at
<https://openfontlicense.org>.

Referenced (not vendored) projects studied during design are catalogued in `docs/architecture/reference-apps.md`. GPL-licensed or source-first projects (e.g. Sayboard, HeliBoard, FUTO Voice Input) are reference-only; no code from them is reused.
