# Pull request

<!--
Thanks. Please read CONTRIBUTING.md first — the dependency rule and the
attribution rules below are enforced by CI, not by review comments.
-->

## What this changes

<!-- One or two sentences. If it fixes an issue, `Fixes #123`. -->

## Related issue

<!--
An ADR or a map ticket, if there is one. OpenFlow decides in
`.scratch/openflow-v1/` before it builds; a PR that changes behaviour usually
has a decision behind it.
-->

## Checklist

- [ ] `./gradlew lint test assembleDebug` passes locally
- [ ] New or changed providers pass the shared Contract Test suite
- [ ] `docs/` updated if behaviour or an interface changed
- [ ] `GLOSSARY.md` updated if a domain term changed meaning
- [ ] An ADR added if the change is hard to reverse, surprising without
      context, and the result of a real trade-off — all three, not one

## Attribution

- [ ] The commit is authored by the repository owner
- [ ] No `Co-authored-by` or `Signed-off-by` trailer names a bot, an automation
      account, or an AI assistant
- [ ] If this re-lands a bot's diff, it went through the procedure in
      [CONTRIBUTING.md](../CONTRIBUTING.md#landing-a-dependabot-bump)

<!--
`.github/workflows/attribution.yml` fails the pull request if either of the first
two boxes above is untrue. This is not a style preference: GitHub counts trailer
names as contributors, and this repository's contributor list is people only.
Agents are tools, not contributors, whether they wrote the code or only reviewed
it.

The third box has no workflow behind it, which is why it is a link rather than a
restatement: a Dependabot pull request cannot be merged with GitHub's buttons at
all, and the reason is not something this template can usefully repeat.
-->

## Testing

<!--
What you ran, on what device, and what you did not test. Instrumentation on a
single emulator is not a substitute for the three-device acceptance gate; say so
plainly rather than letting a green tick imply it.
-->