#!/usr/bin/env python3
"""Resolve the acceptance gate's coverage for one release artifact, and refuse to publish when it is not met.

ADR-0008 puts the enforcement here rather than in branch protection, because a tag
is not a branch: required status contexts never see it. That makes this file the
*only* thing standing between a red gate and a published Release, which is why it is
written to refuse rather than to succeed.

What it reads, and nothing else
-------------------------------

The fenced ``json`` block under ``## Gate status`` in ``docs/quality/acceptance-gate.md``,
at the evidence revision the publisher selected. The prose around that block is not
machine input. A reworded sentence, a dated scenario count in a historical section, or an
edit to the example fence must not be able to move a gate -- and the only way to be sure
of that is to parse one block and refuse everything else.

The block is the **first** ``json`` fence after the ``## Gate status`` heading, and the
protocol requires it to appear before any ``###`` subsection. The section under
``### The shape of a populated block`` is a shape, not a record: its values are
placeholders, and reading it would report the example. A parser that takes the last fence
in the document is a parser that reports on placeholder values and passes.

What it refuses, and why each refusal is separate
------------------------------------------------

Hard failures, never waivable by ``gate_waiver_reason`` -- a waiver covers insufficient
*coverage*, and inventing a bypass for these would make the waiver a master key:

  * an unknown ``gate_version``, so a future schema is not parsed with today's guesses
  * a missing, empty or duplicated ``required_scenarios`` registry
  * a scenario ID that is not in the registry, which invalidates the record rather than
    filling a missing requirement
  * a class entry with no ``hostility``, whose key would be unfalsifiable; or an answer
    outside ``yes``/``no``/``unknown``, the same rule as an unrecognised ``abi``
  * a class key that disagrees with the one recomputed from its own answers, which is how
    the label version of an unfalsifiable claim gets caught
  * a malformed ``runs`` list, or a run result that is neither ``pass`` nor ``fail``
  * a null or mismatched artifact identity, or a null/unreviewed protocol revision
  * a different APK SHA-256, version name or version code than the artifact selected for
    publication

Insufficient coverage, waivable with a non-whitespace reason: a required scenario with no
qualifying cell, a cell on an ABI the build does not ship, and an unresolved device class.
Absence of evidence is not a pass, so an empty ``classes`` blocks both bars.

Two things are excluded from coverage rather than judged, because failing them would be
failing a scenario for something that is not OpenFlow's behaviour:

  * a cell whose ``abi`` is not in the build's own ``abiFilters`` list, or has no ``abi``
  * a class whose Hostility Profile contains any ``unknown`` -- crediting an unmeasured
    device would invent coverage, and calling it hostile would narrow the gate silently

Usage:

    python3 .github/scripts/check_gate.py \\
        --apk app/build/outputs/apk/release/app-release.apk \\
        --tag v0.2.0 \\
        --source-commit "$(git rev-parse HEAD)" \\
        --version-code 2000 --version-name 0.2.0 \\
        [--protocol docs/quality/acceptance-gate.md] \\
        [--build-file app/build.gradle.kts] \\
        [--waiver-reason "..."] [--report gate-report.md]

Exit status is 0 when the artifact may be published, 1 when it may not, and 2 for a
usage or input error. ``--report`` writes the same Markdown the release notes and the
workflow summary carry, so a blocked attempt says what was missing and where.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

GATE_DOCUMENT = Path("docs/quality/acceptance-gate.md")
BUILD_FILE = Path("app/build.gradle.kts")

# The schema this parser knows. A record at any other version is refused rather than
# guessed at: versions 1-5 each added a shape, and reading a newer one with this
# parser's assumptions is how a checker reports on a block it does not understand.
SUPPORTED_GATE_VERSION = 6

# The class is a published function of the Hostility Profile, so a checker recomputes it
# instead of trusting the key a tester typed. Counting rather than worst-axis is
# deliberate: one flaky probe moves a device one tier instead of reassigning it whole.
CLASS_BY_HOSTILE_COUNT = {0: "pixel-like", 1: "samsung-class", 2: "xiaomi-class", 3: "xiaomi-class"}
# De-duplicated, because the count table maps two and three hostile answers onto the same
# class and a "three classes" bar that reads four is a bar nobody can satisfy.
CLASS_KEYS = tuple(dict.fromkeys(CLASS_BY_HOSTILE_COUNT.values()))

HOSTILITY_PROBES = ("background_survival", "overlay_persistence", "background_microphone")
HOSTILE_ANSWERS = ("yes",)
PROFILE_ANSWERS = ("yes", "no", "unknown")
# A class's `config` block is recorded and shown in the report, but nothing here reads it
# for a verdict: the class is derived from `hostility` alone, so a typo in a setting
# describes a device wrongly without changing which tier it earns. Failing a release over
# it would be refusing on something no coverage claim depends on.

RUN_RESULTS = ("pass", "fail")
RUNS_PER_CELL = 3
RUNS_TO_PASS = 2

# The two bars. The tag's own major version selects the threshold, so a version number
# cannot disagree with the claim the release makes. "Prerelease" here means the
# major-zero maturity bar, not a SemVer suffix.
SHIPPED_FROM_MAJOR = 1

REQUIRED_TOP_LEVEL = (
    "gate_version",
    "required_scenarios",
    "as_of",
    "tag",
    "commit",
    "version_code",
    "apk_sha256",
    "protocol_commit",
    "protocol_reviewed",
    "classes",
)

GATE_STATUS_HEADING_RE = re.compile(r"^##\s+Gate status\s*$", re.MULTILINE)
SECTION_HEADING_RE = re.compile(r"^(#{1,2})\s+\S")
SUBSECTION_HEADING_RE = re.compile(r"^#{3,6}\s+\S")
FENCE_OPEN_RE = re.compile(r"^\s*```+\s*([A-Za-z0-9_+-]*)\s*$")
FENCE_CLOSE_RE = re.compile(r"^\s*```+\s*$")
ABI_FILTERS_RE = re.compile(r"abiFilters\s*\+?=\s*listOf\(([^)]*)\)", re.DOTALL)
QUOTED_RE = re.compile(r"\"([^\"]*)\"|'([^']*)'")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
FULL_SHA_RE = re.compile(r"^[0-9a-f]{40}$")
VERSION_CODE_RE = re.compile(r"^(0|[1-9][0-9]*)$")
ISO_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
RELEASE_TAG_RE = re.compile(r"^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$")
MAX_VERSION_COMPONENT = 999


class GateError(Exception):
    """An input could not be read at all, as distinct from a gate that is not met."""


# ---------------------------------------------------------------------------
# The gate document
# ---------------------------------------------------------------------------


def read_gate_record(path: Path) -> dict:
    """Return the live Gate Status record, and nothing else, from the protocol.

    The first ``json`` fence after ``## Gate status``, and only if it precedes any
    subsection. The protocol states this rule because adding a worked example next to a
    live machine-read format is a change to that format's contract: a checker that reads
    the wrong fence reports on placeholder values and passes.
    """
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        raise GateError(f"cannot read the gate document at {path}: {exc}") from exc

    heading = GATE_STATUS_HEADING_RE.search(text)
    if not heading:
        raise GateError(f"{path} has no '## Gate status' section")

    lines = text[heading.end() :].splitlines()
    index = 0
    while index < len(lines):
        line = lines[index]
        if SECTION_HEADING_RE.match(line) or SUBSECTION_HEADING_RE.match(line):
            break  # left the section without finding the record
        opener = FENCE_OPEN_RE.match(line)
        if opener:
            info = (opener.group(1) or "").lower()
            body: list[str] = []
            index += 1
            while index < len(lines) and not FENCE_CLOSE_RE.match(lines[index]):
                body.append(lines[index])
                index += 1
            if info == "json":
                return _parse_json_record("\n".join(body), path)
        index += 1

    raise GateError(
        f"{path} has no 'json' block under '## Gate status' before any subsection. "
        "The live record is the first json fence after the heading; the populated "
        "example below it is a shape, not a record."
    )


def _parse_json_record(body: str, path: Path) -> dict:
    try:
        record = json.loads(body)
    except json.JSONDecodeError as exc:
        raise GateError(f"the live Gate Status block in {path} is not valid JSON: {exc}") from exc
    if not isinstance(record, dict):
        raise GateError(f"the live Gate Status block in {path} is not a JSON object")
    return record


# ---------------------------------------------------------------------------
# The build, read for the ABI list it actually ships
# ---------------------------------------------------------------------------


def read_shipped_abis(path: Path) -> list[str]:
    """Read the shipped ABI list from the build rather than hardcoding a copy.

    The gate disqualifies a cell on an ABI OpenFlow does not ship, so the authority for
    that list is ``abiFilters`` in the build. A copy here would be a second statement of
    the same fact, and this repository's experience is that the second one goes stale
    silently. A missing or empty list is a hard failure: there is no fallback list to
    guess with.
    """
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        raise GateError(f"cannot read the build file at {path}: {exc}") from exc

    match = ABI_FILTERS_RE.search(text)
    if not match:
        raise GateError(
            f"{path} has no abiFilters list to read. The gate qualifies coverage "
            "against the ABIs the artifact actually contains, so it reads the build "
            "rather than a copied list."
        )
    abis = [a or b for a, b in QUOTED_RE.findall(match.group(1))]
    if not abis:
        raise GateError(f"the abiFilters list in {path} is empty")
    return abis


# ---------------------------------------------------------------------------
# Release identity
# ---------------------------------------------------------------------------


def parse_release_tag(tag: str) -> dict:
    """Derive the artifact identity a release tag implies.

    One deterministic calculation, used by the build and re-derived here: the checker
    verifies the APK against the tag rather than against a version string the workflow
    chose. Components are bounded so digit ranges cannot overlap, and a malformed or
    out-of-range tag fails rather than being normalised -- a version that is silently
    repaired is a version nobody agreed to.
    """
    match = RELEASE_TAG_RE.match(tag)
    if not match:
        raise GateError(
            f"'{tag}' is not a plain release tag. Expected vMAJOR.MINOR.PATCH with "
            "decimal components, no leading zeroes, no prerelease suffix and no build "
            "metadata."
        )
    major, minor, patch = (int(part) for part in match.groups())
    for name, value in (("major", major), ("minor", minor), ("patch", patch)):
        if value > MAX_VERSION_COMPONENT:
            raise GateError(f"'{tag}' has a {name} version of {value}, above the {MAX_VERSION_COMPONENT} bound")
    if (major, minor, patch) == (0, 0, 0):
        raise GateError("'v0.0.0' is not a release; there is no artifact to identify")
    return {
        "tag": tag,
        "major": major,
        "version_name": f"{major}.{minor}.{patch}",
        "version_code": major * 1_000_000 + minor * 1_000 + patch,
    }


def required_class_bar(tag: str) -> int:
    """How many device classes the tag's own major version demands.

    Releasable (``v0.y.z``) is one complete class; Shipped (``v1.0.0`` and above) is
    all three. The tag states which claim is being made, so the threshold cannot drift
    from the version a user reads.
    """
    return len(CLASS_KEYS) if parse_release_tag(tag)["major"] >= SHIPPED_FROM_MAJOR else 1


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise GateError(f"cannot read the artifact at {path}: {exc}") from exc
    return digest.hexdigest()


# ---------------------------------------------------------------------------
# Coverage
# ---------------------------------------------------------------------------


class ClassCoverage:
    """One device class's standing, and the reason for it when it is not complete."""

    def __init__(self, key: str) -> None:
        self.key = key
        self.complete = False
        self.missing: list[str] = []
        self.not_passing: list[str] = []
        self.excluded: list[str] = []
        self.unresolved = False
        self.notes: list[str] = []

    def status(self) -> str:
        if self.complete:
            return "complete"
        if self.unresolved:
            return "unresolved"
        return "incomplete"

    def reason(self) -> str:
        parts = []
        if self.missing:
            parts.append("no qualifying result for " + ", ".join(self.missing))
        if self.not_passing:
            parts.append("not green for " + ", ".join(self.not_passing))
        if self.excluded:
            parts.append("excluded from coverage: " + ", ".join(self.excluded))
        if self.unresolved:
            parts.append("Hostility Profile is incomplete, so the device carries no class")
        return "; ".join(parts) or "incomplete"


class Report:
    """The gate's verdict for one artifact, and everything that led to it."""

    def __init__(self, tag: str) -> None:
        self.tag = tag
        self.hard_failures: list[str] = []
        self.notes: list[str] = []
        self.coverage: list[ClassCoverage] = []
        self.required_classes = 0
        self.complete_classes: list[str] = []
        self.waiver_reason: str | None = None
        self.bar = ""
        self.identity: dict[str, object] = {}
        self.protocol_commit: str | None = None
        self.protocol_reviewed = False
        self.record = {}

    def fail(self, message: str) -> None:
        self.hard_failures.append(message)

    def note(self, message: str) -> None:
        if message not in self.notes:
            self.notes.append(message)

    def evaluate_bar(self) -> bool:
        return len(self.complete_classes) >= self.required_classes

    @property
    def coverage_met(self) -> bool:
        return self.evaluate_bar()

    def may_publish(self) -> bool:
        if self.hard_failures:
            return False
        if self.coverage_met:
            return True
        return bool(self.waiver_reason and self.waiver_reason.strip())

    def verdict(self) -> str:
        if self.hard_failures:
            return "blocked"
        if self.coverage_met:
            return "met"
        if self.waiver_reason and self.waiver_reason.strip():
            return "waived"
        return "blocked"


def _hostile_count(hostility: dict) -> int:
    return sum(1 for probe in HOSTILITY_PROBES if hostility.get(probe) in HOSTILE_ANSWERS)


def _derive_class(hostility: dict) -> str | None:
    """The class the profile earns, or None when the profile does not earn one."""
    if any(hostility.get(probe) not in PROFILE_ANSWERS for probe in HOSTILITY_PROBES):
        return None
    if any(hostility.get(probe) == "unknown" for probe in HOSTILITY_PROBES):
        return None
    return CLASS_BY_HOSTILE_COUNT[_hostile_count(hostility)]


def _check_runs(value: object, scenario_id: str, report: Report) -> int | None:
    """The passes in a cell, or None when the cell is not interpretable.

    Fewer than three recorded runs has not demonstrated two of three, so the cell does
    not qualify -- that is a coverage gap, and a waivable one. A run result that is
    neither ``pass`` nor ``fail`` is not a gap but a malformed record: defaulting it
    would be exactly the tolerant check that passes where nobody is looking.
    """
    if not isinstance(value, list):
        report.fail(f"{scenario_id}: 'runs' is not a list")
        return None
    if len(value) > RUNS_PER_CELL:
        report.fail(
            f"{scenario_id}: {len(value)} runs recorded. The protocol keeps three per "
            "cell, so a fourth run is a second cell rather than a fourth run."
        )
        return None
    for run in value:
        if run not in RUN_RESULTS:
            report.fail(f"{scenario_id}: run result {run!r} is neither 'pass' nor 'fail'")
            return None
    if len(value) < RUNS_PER_CELL:
        # Two passes out of two is not two of three. The cell has not demonstrated the
        # rule, so it does not qualify — which is a gap rather than a failure.
        return None
    return sum(1 for run in value if run == "pass")


def _resolve_class(
    key: str,
    entry: object,
    required_ids: list[str],
    shipped_abis: set[str],
    report: Report,
) -> ClassCoverage:
    coverage = ClassCoverage(key)
    if not isinstance(entry, dict):
        report.fail(f"class '{key}' is not a JSON object")
        return coverage

    # The class is recomputed, never trusted. A key that disagrees with its own answers
    # is a record whose central claim nothing else can check.
    hostility = entry.get("hostility")
    if not isinstance(hostility, dict):
        report.fail(
            f"class '{key}' has no 'hostility' object, so its key cannot be "
            "falsified against anything"
        )
        return coverage
    for probe in HOSTILITY_PROBES:
        if probe not in hostility:
            report.fail(f"class '{key}': hostility is missing '{probe}'")
            return coverage
        if hostility[probe] not in PROFILE_ANSWERS:
            report.fail(
                f"class '{key}': hostility '{probe}' is {hostility[probe]!r}, which is "
                f"not one of {'/'.join(PROFILE_ANSWERS)}"
            )
            return coverage

    derived = _derive_class(hostility)
    if derived is None:
        # A real, honestly recorded value, not a malformed one. The device is excluded
        # from coverage and reported unresolved; it is never defaulted either way.
        coverage.unresolved = True
        unknown = [p for p in HOSTILITY_PROBES if hostility[p] == "unknown"]
        report.note(
            f"class '{key}' carries no class: unknown {', '.join(unknown)}. Excluded "
            "from coverage without being judged."
        )
        return coverage
    if derived != key:
        report.fail(
            f"class '{key}': {derived} answers derive to '{derived}'-key "
            f"'{derived}', not '{key}'. The class is derived from the Hostility "
            "Profile by counting hostile answers, and the key has to agree with it."
        )
        return coverage

    scenarios = entry.get("scenarios")
    if not isinstance(scenarios, dict):
        report.fail(f"class '{key}': 'scenarios' is not a JSON object")
        return coverage

    unexpected = sorted(set(scenarios) - set(required_ids))
    if unexpected:
        # An ID nobody required invalidates the record rather than substituting for the
        # requirement that is actually missing.
        report.fail(
            f"class '{key}': scenario id(s) {', '.join(unexpected)} are not in "
            "required_scenarios. An unexpected result invalidates the record rather "
            "than filling a missing requirement."
        )
        return coverage

    for scenario_id in required_ids:
        cell = scenarios.get(scenario_id)
        if not isinstance(cell, dict):
            coverage.missing.append(scenario_id)
            continue
        abi = cell.get("abi")
        if not isinstance(abi, str) or not abi:
            # Absence of evidence is not a pass, and an absent ABI is a cell that cannot
            # be checked at all.
            coverage.excluded.append(f"{scenario_id} (no abi recorded)")
            continue
        if abi not in shipped_abis:
            coverage.excluded.append(f"{scenario_id} (abi {abi} not shipped)")
            continue
        passes = _check_runs(cell.get("runs"), f"{key}/{scenario_id}", report)
        if passes is None:
            coverage.excluded.append(f"{scenario_id} (unreadable runs)")
            continue
        if passes >= RUNS_TO_PASS:
            continue
        if passes == 0 and len(cell.get("runs") or []) < RUNS_PER_CELL:
            coverage.missing.append(scenario_id)
        else:
            coverage.not_passing.append(f"{scenario_id} ({passes}/3)")

    coverage.complete = not coverage.missing and not coverage.not_passing and not coverage.excluded
    return coverage


def _validate_registry(record: dict, report: Report) -> list[str]:
    registry = record.get("required_scenarios")
    if registry is None:
        report.fail("the record has no 'required_scenarios' registry")
        return []
    if not isinstance(registry, list):
        report.fail("'required_scenarios' is not a list")
        return []
    if not registry:
        report.fail("'required_scenarios' is empty, so the gate requires nothing")
        return []
    if not all(isinstance(item, str) and item.strip() for item in registry):
        report.fail("'required_scenarios' holds a value that is not a scenario id")
        return []
    duplicates = sorted({item for item in registry if registry.count(item) > 1})
    if duplicates:
        report.fail("'required_scenarios' repeats " + ", ".join(duplicates))
        return []
    # The total is derived from this list, never from a numeral in prose and never from
    # the number of results that happen to be present.
    return list(registry)


def _validate_identity(record: dict, expected: dict, report: Report) -> None:
    """Match the record's active artifact against the artifact being published.

    A result from another APK cannot fill a gap, even with the same version or source
    commit, so this compares all four identity fields rather than any one of them.
    """
    for key in ("tag", "commit", "version_code", "apk_sha256"):
        value = record.get(key)
        report.identity[key] = value
        if value is None:
            report.fail(f"the record's '{key}' is null: no active APK has been selected")
            continue
        if value != expected[key]:
            report.fail(
                f"the record's {key} is {value!r}, and the artifact selected for "
                f"publication is {expected[key]!r}"
            )

    commit = record.get("commit")
    if commit is not None and (not isinstance(commit, str) or not FULL_SHA_RE.match(commit)):
        report.fail(f"the record's commit {commit!r} is not a full 40-character SHA")
    checksum = record.get("apk_sha256")
    if checksum is not None and (not isinstance(checksum, str) or not SHA256_RE.match(checksum)):
        report.fail(f"the record's apk_sha256 {checksum!r} is not a lowercase SHA-256")

    protocol_commit = record.get("protocol_commit")
    report.protocol_commit = protocol_commit if isinstance(protocol_commit, str) else None
    if not report.protocol_commit:
        report.fail(
            "the record names no reviewed testing protocol (protocol_commit is null). "
            "Every release states the protocol its runs were performed under."
        )
    elif not FULL_SHA_RE.match(report.protocol_commit):
        report.fail(f"protocol_commit {report.protocol_commit!r} is not a full 40-character SHA")

    reviewed = record.get("protocol_reviewed")
    report.protocol_reviewed = reviewed is True
    if reviewed is not True:
        # Compatibility is the owner's attestation, not something a parser can judge, so
        # the only check available is that it was made. Unreviewed is not waivable.
        report.fail(
            "protocol_reviewed is not true. The owner has not attested that these runs "
            "apply to the protocol at the selected evidence revision, and unreviewed "
            "compatibility cannot be waived."
        )


def evaluate(
    record: dict,
    tag: str,
    source_commit: str,
    version_code: int,
    version_name: str,
    shipped_abis: list[str],
    apk_sha256: str,
    waiver_reason: str | None = None,
) -> Report:
    """Resolve coverage for one artifact, and record every reason it is not met."""
    identity = parse_release_tag(tag)
    report = Report(tag)
    report.record = record
    report.waiver_reason = waiver_reason
    report.required_classes = len(CLASS_KEYS) if identity["major"] >= SHIPPED_FROM_MAJOR else 1
    report.bar = (
        f"Shipped: every required scenario on all {report.required_classes} device classes"
        if report.required_classes == len(CLASS_KEYS)
        else "Releasable: every required scenario on at least one device class"
    )

    version = record.get("gate_version")
    if version != SUPPORTED_GATE_VERSION:
        report.fail(
            f"the record is gate_version {version!r}; this checker knows version "
            f"{SUPPORTED_GATE_VERSION} and refuses to guess at a shape it does not know"
        )
        return report

    missing_keys = [key for key in REQUIRED_TOP_LEVEL if key not in record]
    if missing_keys:
        report.fail("the record is missing " + ", ".join(missing_keys))
        return report

    as_of = record.get("as_of")
    if as_of is None or not (isinstance(as_of, str) and ISO_DATE_RE.match(as_of)):
        report.fail(f"the record's as_of is {as_of!r}, which is not an ISO date")
    elif not _is_real_date(as_of):
        report.fail(f"the record's as_of {as_of!r} is not a date that exists")

    # The version the build reported and the version the tag implies are both inputs, so
    # a workflow that passes the wrong number is caught here rather than trusted.
    if version_name != identity["version_name"]:
        report.fail(
            f"the artifact reports versionName {version_name!r}, and tag {tag} implies "
            f"{identity['version_name']!r}"
        )
    if version_code != identity["version_code"]:
        report.fail(
            f"the artifact reports versionCode {version_code!r}, and tag {tag} implies "
            f"{identity['version_code']}"
        )

    # The artifact's checksum is read from the bytes on disk by the caller and compared
    # here, rather than being passed in as a number this script also computed. A check
    # that asserts a value is satisfied by asserting the right value.
    _validate_identity(
        record,
        {
            "tag": tag,
            "commit": source_commit,
            "version_code": version_code,
            "apk_sha256": apk_sha256,
        },
        report,
    )

    required_ids = _validate_registry(record, report)
    if not required_ids:
        return report

    classes = record.get("classes")
    if not isinstance(classes, dict):
        report.fail("'classes' is not a JSON object")
        return report

    abi_set = set(shipped_abis)
    for key in sorted(classes):
        if key not in CLASS_KEYS:
            report.fail(
                f"class key {key!r} is not one of {', '.join(CLASS_KEYS)}. Never an "
                "emulator, and never a fourth tier."
            )
            continue
        coverage = _resolve_class(key, classes[key], required_ids, abi_set, report)
        report.coverage.append(coverage)
        if coverage.complete:
            report.complete_classes.append(key)

    if not report.coverage:
        # An empty `classes` is absence of evidence, and it blocks both bars. The right
        # state for this repository today, which is not the same as a failing state.
        report.note(
            "no device class is recorded, so coverage is unknown rather than zero. "
            "No run has been performed."
        )
    return report


def _is_real_date(value: str) -> bool:
    import datetime as dt

    try:
        dt.date.fromisoformat(value)
    except ValueError:
        return False
    return True


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------


def _coverage_rows(report: Report) -> list[str]:
    rows = [
        "| Device Class | Status | Required scenarios | Note |",
        "| --- | --- | --- | --- |",
    ]
    if not report.coverage:
        total = len(report.record.get("required_scenarios") or [])
        rows.append(
            f"| _(none recorded)_ | — | 0/{total} | No run has been performed. |"
        )
        return rows
    total = len(report.record.get("required_scenarios") or [])
    for coverage in report.coverage:
        if coverage.complete:
            note = "every required scenario green on a shipped ABI"
        else:
            note = coverage.reason()
        # An excluded cell is not counted as covered, so the numerator only ever names
        # required scenarios that actually qualified.
        green = total - len(coverage.missing) - len(coverage.not_passing) - len(coverage.excluded)
        rows.append(f"| `{coverage.key}` | {coverage.status()} | {green}/{total} | {note} |")
    return rows


def render_report(report: Report, *, evidence_sha: str = "", publisher_revision: str = "") -> str:
    """The Markdown that lands in the workflow summary and the release notes."""
    lines = [
        f"## Acceptance gate — `{report.tag}`",
        "",
        f"**Verdict: {report.verdict()}**",
        "",
        f"Bar required by this tag: {report.bar}.",
        "",
    ]
    lines += _coverage_rows(report)
    lines += [
        "",
        f"Complete classes: {len(report.complete_classes)}/{report.required_classes} required.",
        "",
    ]
    if report.identity:
        lines += [
            "### Pinned identities",
            "",
            "| Field | Value |",
            "| --- | --- |",
        ]
        for key in ("tag", "commit", "version_code", "apk_sha256"):
            lines.append(f"| `{key}` | `{report.identity.get(key)}` |")
        lines += [
            f"| `protocol_commit` | `{report.protocol_commit}` |",
            f"| `protocol_reviewed` | `{str(report.protocol_reviewed).lower()}` |",
            "",
        ]
    if evidence_sha:
        lines += [f"Reviewed evidence revision: `{evidence_sha}`.", ""]
    if publisher_revision:
        lines += [f"Publisher revision: `{publisher_revision}`.", ""]

    if report.hard_failures:
        lines += ["### Refused", ""]
        lines += [f"- {item}" for item in report.hard_failures]
        lines += [
            "",
            "A `gate_waiver_reason` does not cover these. A waiver covers insufficient "
            "coverage, not an invalid record, a mismatched artifact or an unreviewed "
            "protocol.",
            "",
        ]
    if report.notes:
        lines += ["### Recorded", ""]
        lines += [f"- {item}" for item in report.notes]
        lines += [""]

    if report.verdict() == "waived":
        lines += [
            "### Waived",
            "",
            f"This release was published with insufficient device coverage. Reason given by "
            f"the publisher: **{report.waiver_reason}**",
            "",
        ]
    elif not report.coverage_met:
        lines += [
            "### Not met",
            "",
            f"This artifact does not yet meet the bar its tag implies: "
            f"{len(report.complete_classes)} complete class(es) of {report.required_classes} "
            "required. Publish it with a non-empty `gate_waiver_reason` if it must ship "
            "without the hardware, and the reason will be recorded here and in the release "
            "notes.",
            "",
        ]
    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Resolve acceptance-gate coverage for one release artifact."
    )
    parser.add_argument("--apk", required=True, help="the exact signed APK selected for publication")
    parser.add_argument("--tag", required=True, help="the release tag being published")
    parser.add_argument("--source-commit", required=True, help="the full source SHA the APK was built from")
    parser.add_argument("--version-code", required=True, help="the artifact's versionCode")
    parser.add_argument("--version-name", required=True, help="the artifact's versionName")
    parser.add_argument("--protocol", default=str(GATE_DOCUMENT), help="the gate document at the evidence revision")
    parser.add_argument("--build-file", default=str(BUILD_FILE), help="the build file to read abiFilters from")
    parser.add_argument("--waiver-reason", default=None, help="non-empty to publish with insufficient coverage")
    parser.add_argument("--report", default=None, help="write the Markdown report here")
    parser.add_argument("--evidence-sha", default="", help="the reviewed evidence revision, for the report")
    parser.add_argument("--publisher-revision", default="", help="the workflow revision that published, for the report")
    args = parser.parse_args(argv)

    try:
        version_code = int(args.version_code)
    except ValueError:
        print(f"::error::--version-code {args.version_code!r} is not an integer.", file=sys.stderr)
        return 2
    if not VERSION_CODE_RE.match(str(version_code)):
        print(f"::error::--version-code {version_code!r} is not a valid Android version code.", file=sys.stderr)
        return 2

    try:
        record = read_gate_record(Path(args.protocol))
        shipped_abis = read_shipped_abis(Path(args.build_file))
        checksum = file_sha256(Path(args.apk))
    except GateError as exc:
        print(f"::error::{exc}", file=sys.stderr)
        return 2

    expected = parse_release_tag(args.tag)
    report = evaluate(
        record,
        tag=args.tag,
        source_commit=args.source_commit,
        version_code=version_code,
        version_name=args.version_name,
        shipped_abis=shipped_abis,
        apk_sha256=checksum,
        waiver_reason=args.waiver_reason,
    )
    # The checksum is read here, from the bytes on disk, rather than passed in. A check
    # that compares a value with something the same process just computed is the only
    # kind here that cannot be satisfied by asserting the right number.
    markdown = render_report(
        report, evidence_sha=args.evidence_sha, publisher_revision=args.publisher_revision
    )
    if args.report:
        Path(args.report).write_text(markdown + "\n", encoding="utf-8")
    print(markdown)

    if report.may_publish():
        print(f"\nGate {report.verdict()}: {args.tag} may be published.", file=sys.stderr)
        return 0
    for failure in report.hard_failures:
        print(f"::error::{failure}", file=sys.stderr)
    print(f"::error::Gate blocked: {args.tag}.", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
