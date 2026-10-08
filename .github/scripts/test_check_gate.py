#!/usr/bin/env python3
"""Verification for check_gate.py, run on every pull request.

    python3 -m unittest discover -s .github/scripts -p 'test_*.py'

Each test names the failure it defends against rather than the code path it walks,
because the point of this checker is what it *refuses*. Two of them assert the
refusals that matter most:

  * an empty `classes` **blocks**, which is this repository's real state — a
    checker that passed it would be a checker that had never been run against
    the only record that exists;
  * the populated example fence and dated prose never reach the parser, so a
    reworded sentence cannot move a gate.

The last test reads the real protocol out of the working tree, so a schema change
that this file has not been taught about fails here rather than in a release.
"""

import copy
import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / ".github" / "scripts"))

import check_gate  # noqa: E402

GATE_DOC = REPO / "docs" / "quality" / "acceptance-gate.md"
BUILD_FILE = REPO / "app" / "build.gradle.kts"

COMMIT_A = "a" * 40
COMMIT_B = "b" * 40
SHA_REAL = "c" * 64
SHA_OTHER = "d" * 64

REQUIRED = [f"G{i}" for i in range(1, 16)]

PROTOCOL_PREAMBLE = """# The acceptance gate

Prose that must not be machine input. Dated historical prose says fourteen
scenarios, which is a fact about the past and not a requirement.

## The scenarios

| Id | Scenario |
| --- | --- |
| G1 | Short chat |
| G9 | Model download with a bad or absent network |

## Gate status
"""


def live_block(record: dict) -> str:
    return "```json\n" + json.dumps(record, indent=2) + "\n```\n"


def example_fence() -> str:
    """The shape section, with placeholder values a parser must never read."""
    return (
        "\n### The shape of a populated block\n\n"
        'Prose count: 14 scenarios, thirty runs.\n\n'
        "```json\n"
        + json.dumps(
            {
                "gate_version": 5,
                "required_scenarios": ["G1"],
                "as_of": "<YYYY-MM-DD>",
                "tag": "<vMAJOR.MINOR.PATCH>",
                "commit": "<full APK source commit SHA>",
                "version_code": 0,
                "apk_sha256": "<64 lowercase hex>",
                "protocol_commit": "<full protocol SHA>",
                "protocol_reviewed": True,
                "classes": {"<device class>": {}},
            },
            indent=2,
        )
        + "\n```\n"
    )


def full_coverage(classes=("pixel-like",), abi="arm64-v8a", runs=None):
    entry = {
        "oem_skin": "stock",
        "android_major": 15,
        "device_model": "reference",
        "hostility": {
            "background_survival": "no",
            "overlay_persistence": "no",
            "background_microphone": "no",
        },
        "config": {
            "battery_optimisation_exempt": "yes",
            "autostart_allowed": "yes",
            "per_app_mic_allowed": "yes",
            "unrestricted_battery_mode": "no",
        },
        "scenarios": {
            gid: {"abi": abi, "runs": list(runs or ["pass", "pass", "fail"])}
            for gid in REQUIRED
        },
    }
    return {key: copy.deepcopy(entry) for key in classes}


def record(**overrides) -> dict:
    base = {
        "gate_version": 6,
        "required_scenarios": list(REQUIRED),
        "as_of": "2026-10-05",
        "tag": "v0.2.0",
        "commit": COMMIT_A,
        "version_code": 2000,
        "apk_sha256": SHA_REAL,
        "protocol_commit": COMMIT_B,
        "protocol_reviewed": True,
        "classes": full_coverage(),
    }
    base.update(overrides)
    return base


def make_record(checksum: str, **overrides) -> dict:
    """The record for an artifact whose bytes hash to `checksum`."""
    base = record(**overrides)
    base["apk_sha256"] = checksum
    return base


class GateTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp)
        self.abis = check_gate.read_shipped_abis(BUILD_FILE)
        self.apk = self.tmp / "app-release.apk"
        self.apk.write_bytes(b"pretend this is a signed apk")
        self.checksum = check_gate.file_sha256(self.apk)

    def protocol(self, body: str, name: str = "acceptance-gate.md") -> Path:
        path = self.tmp / name
        path.write_text(body, encoding="utf-8")
        return path

    def run_gate(self, rec, tag="v0.2.0", version_code=2000, version_name="0.2.0",
                 commit=COMMIT_A, checksum=None, waiver=None, abis=None, pin=True):
        # The record's identity is made to agree with the artifact by default, so each
        # test's failure comes from the field it is actually about. `pin=False` leaves
        # the record's own identity alone, which is what the mismatch tests need.
        actual = self.checksum if checksum is None else checksum
        rec = copy.deepcopy(rec)
        if pin:
            rec["apk_sha256"] = actual
            rec["tag"] = tag
            rec["version_code"] = version_code
        return check_gate.evaluate(
            rec,
            tag=tag,
            source_commit=commit,
            version_code=version_code,
            version_name=version_name,
            shipped_abis=list(abis if abis is not None else self.abis),
            apk_sha256=actual,
            waiver_reason=waiver,
        )

    def assertPublishable(self, report, why=""):
        self.assertEqual(report.hard_failures, [], f"expected no hard failures {why}")
        self.assertTrue(report.may_publish(), f"expected publishable {why}: {report.hard_failures}")

    def assertBlocked(self, report, needle=None):
        self.assertFalse(report.may_publish(), "expected blocked")
        if needle:
            # The reason a cell is excluded lives in the coverage rows, which is where a
            # reader sees it, so the rendered report is the right place to look.
            joined = " ".join(
                report.hard_failures + report.notes + [check_gate.render_report(report)]
            )
            self.assertIn(needle, joined)

    # --- the live block is the only input ---------------------------------

    def test_reads_the_first_json_fence_not_the_example(self):
        path = self.protocol(PROTOCOL_PREAMBLE + live_block(record()) + example_fence())
        rec = check_gate.read_gate_record(path)
        self.assertEqual(rec["gate_version"], 6)
        self.assertEqual(rec["as_of"], "2026-10-05")

    def test_example_fence_values_never_reach_the_checker(self):
        # An example fence carrying green-looking placeholder classes must not be
        # readable as a record.
        example = (
            "\n### The shape of a populated block\n\n```json\n"
            + json.dumps(
                {
                    "gate_version": 6,
                    "required_scenarios": ["G1"],
                    "as_of": "2020-01-01",
                    "tag": "v9.9.9",
                    "commit": COMMIT_A,
                    "version_code": 9999999,
                    "apk_sha256": SHA_REAL,
                    "protocol_commit": COMMIT_B,
                    "protocol_reviewed": True,
                    "classes": full_coverage(classes=("pixel-like", "samsung-class", "xiaomi-class")),
                },
                indent=2,
            )
            + "\n```\n"
        )
        path = self.protocol(PROTOCOL_PREAMBLE + live_block(record(classes={})) + example)
        rec = check_gate.read_gate_record(path)
        self.assertEqual(rec["classes"], {})

    def test_dated_historical_json_is_excluded(self):
        history = (
            "\n## History\n\n### v0.1.0\n\n```json\n"
            + json.dumps(record(classes=full_coverage(classes=("xiaomi-class",))), indent=2)
            + "\n```\n"
        )
        path = self.protocol(PROTOCOL_PREAMBLE + live_block(record(classes={})) + history)
        self.assertEqual(check_gate.read_gate_record(path)["classes"], {})

    def test_prose_count_and_subsection_precedence(self):
        # The record must precede any subsection; a protocol where it does not is a
        # protocol whose record cannot be found, and guessing would be worse.
        path = self.protocol(PROTOCOL_PREAMBLE + example_fence() + live_block(record()))
        with self.assertRaises(check_gate.GateError):
            check_gate.read_gate_record(path)

    def test_missing_record_is_an_input_error_not_a_pass(self):
        path = self.protocol(PROTOCOL_PREAMBLE + example_fence())
        with self.assertRaises(check_gate.GateError):
            check_gate.read_gate_record(path)

    # --- schema refusals ---------------------------------------------------

    def test_unknown_gate_version_is_refused(self):
        for version in (1, 5, 7, "6", None):
            with self.subTest(version=version):
                self.assertBlocked(self.run_gate(record(gate_version=version)), "gate_version")

    def test_registry_must_be_present_non_empty_and_unique(self):
        for bad in (None, [], [""], [1], REQUIRED + ["G1"], ["G1", "G1"]):
            with self.subTest(registry=bad):
                rec = record()
                if bad is None:
                    del rec["required_scenarios"]
                else:
                    rec["required_scenarios"] = bad
                self.assertBlocked(self.run_gate(rec), "required_scenarios")

    def test_unexpected_scenario_id_invalidates_the_record(self):
        classes = full_coverage()
        classes["pixel-like"]["scenarios"]["G99"] = {
            "abi": "arm64-v8a", "runs": ["pass", "pass", "pass"]
        }
        self.assertBlocked(self.run_gate(record(classes=classes)), "G99")

    def test_a_class_that_never_reached_the_scenarios_qualified_nothing(self):
        # A hard-failed class and an unknown-answer class both return before the
        # per-scenario stage, so their per-scenario lists are empty. The row must
        # read 0/15 — a row that printed 15/15 beside "invalid" or "unresolved" is
        # the green-looking number this gate exists to refuse.
        def row(classes):
            return next(
                r
                for r in check_gate._coverage_rows(self.run_gate(record(classes=classes)))
                if "pixel-like" in r
            )

        invalid = full_coverage()
        invalid["pixel-like"]["scenarios"]["G99"] = {
            "abi": "arm64-v8a",
            "runs": ["pass", "pass", "pass"],
        }
        self.assertIn("| invalid | 0/15 |", row(invalid))

        unresolved = full_coverage()
        unresolved["pixel-like"]["hostility"]["background_microphone"] = "unknown"
        self.assertIn("| unresolved | 0/15 |", row(unresolved))

    # --- identity ----------------------------------------------------------

    def test_null_identity_cannot_qualify(self):
        for key in ("tag", "commit", "version_code", "apk_sha256"):
            with self.subTest(field=key):
                # pin=False so the record's own null survives the harness's alignment.
                self.assertBlocked(self.run_gate(record(**{key: None}), pin=False), "null")

    def test_different_apk_checksum_with_same_source_and_version_is_rejected(self):
        # Same tag, same source commit, same version code: only the bytes differ. This
        # is the case a source-and-version check would wave through.
        rec = record(commit=COMMIT_A, version_code=2000, tag="v0.2.0")
        rec["apk_sha256"] = SHA_OTHER
        self.assertBlocked(self.run_gate(rec, checksum=SHA_REAL, pin=False), "apk_sha256")
        # and a waiver does not cover it
        self.assertFalse(
            self.run_gate(rec, checksum=SHA_REAL, waiver="no hardware", pin=False).may_publish()
        )

    def test_evidence_for_another_tag_source_or_version_is_rejected(self):
        # The record describes a different artifact than the one being published.
        self.assertBlocked(self.run_gate(record(tag="v0.3.0"), pin=False), "tag")
        self.assertBlocked(self.run_gate(record(commit=COMMIT_B), pin=False), "commit")
        self.assertBlocked(self.run_gate(record(version_code=3000), pin=False), "version_code")
        # The artifact's own metadata contradicts the tag.
        self.assertBlocked(
            self.run_gate(record(), tag="v0.2.0", version_name="0.2.1"), "versionName"
        )
        self.assertBlocked(
            self.run_gate(record(), tag="v0.2.0", version_code=2001), "implies"
        )

    def test_unreviewed_protocol_cannot_be_waived(self):
        for bad in (False, None, "true"):
            with self.subTest(reviewed=bad):
                rec = record(protocol_reviewed=bad)
                self.assertBlocked(self.run_gate(rec), "protocol_reviewed")
                self.assertBlocked(
                    self.run_gate(rec, waiver="ship it anyway"), "protocol_reviewed"
                )
        self.assertBlocked(self.run_gate(record(protocol_commit=None)), "protocol_commit")

    def test_version_code_must_follow_the_tag(self):
        for tag, code, name in (("v0.0.1", 1, "0.0.1"), ("v0.1.0", 1000, "0.1.0"),
                                ("v1.0.0", 1000000, "1.0.0"), ("v0.2.1", 2001, "0.2.1"),
                                ("v1.2.3", 1002003, "1.2.3")):
            with self.subTest(tag=tag):
                ident = check_gate.parse_release_tag(tag)
                self.assertEqual(ident["version_code"], code)
                self.assertEqual(ident["version_name"], name)
        # A record consistent with the tag, but an artifact whose own metadata is not:
        # the tag decides, so this is refused rather than taken from the APK.
        self.assertBlocked(
            self.run_gate(record(), tag="v0.2.1", version_code=2000, version_name="0.2.0"),
            "implies",
        )

    def test_malformed_tag_is_refused(self):
        for tag in ("v0.2", "v0.2.0-rc1", "v0.02.0", "v0.0.0", "v1000.0.0", "0.2.0", "vx.y.z"):
            with self.subTest(tag=tag):
                with self.assertRaises(check_gate.GateError):
                    check_gate.parse_release_tag(tag)

    # --- completeness is derived, not counted ------------------------------

    def test_missing_required_scenario_does_not_qualify_at_equal_count(self):
        classes = full_coverage()
        cell = classes["pixel-like"]["scenarios"].pop("G15")
        # Replaced with an extra run of an existing one: same total runs, wrong content.
        classes["pixel-like"]["scenarios"]["G1"]["runs"] = ["pass"] * 3
        classes["pixel-like"]["scenarios"]["G1"] = {
            "abi": "arm64-v8a", "runs": ["pass", "pass", "pass"]
        }
        del cell
        report = self.run_gate(record(classes=classes))
        self.assertBlocked(report, "G15")
        self.assertEqual(report.complete_classes, [])

    def test_same_sized_set_with_a_replaced_id_is_not_complete(self):
        classes = full_coverage()
        scenarios = classes["pixel-like"]["scenarios"]
        scenarios["G99"] = scenarios.pop("G15")
        self.assertBlocked(self.run_gate(record(classes=classes)), "G99")

    def test_two_of_three_passes_qualifies_one_of_three_does_not(self):
        classes = full_coverage(runs=["pass", "pass", "fail"])
        self.assertPublishable(self.run_gate(record(classes=classes)), "2/3 is a pass")
        classes = full_coverage(runs=["pass", "fail", "fail"])
        self.assertBlocked(self.run_gate(record(classes=classes)), "1/3")

    def test_fewer_than_three_runs_is_incomplete_not_a_pass(self):
        classes = full_coverage(runs=["pass", "pass"])
        self.assertBlocked(self.run_gate(record(classes=classes)), "G1")

    def test_more_than_three_runs_is_an_invalid_record(self):
        classes = full_coverage(runs=["pass", "pass", "pass", "pass"])
        self.assertBlocked(self.run_gate(record(classes=classes)), "fourth run")

    def test_malformed_run_result_is_refused(self):
        classes = full_coverage(runs=["pass", "maybe", "pass"])
        self.assertBlocked(self.run_gate(record(classes=classes)), "'maybe'")

    # --- ABI ---------------------------------------------------------------

    def test_abi_list_is_read_from_the_build(self):
        self.assertEqual(self.abis, ["arm64-v8a", "armeabi-v7a", "x86_64"])
        drifted = self.tmp / "build.gradle.kts"
        drifted.write_text(
            'android { defaultConfig { ndk { abiFilters += listOf("arm64-v8a") } } }',
            encoding="utf-8",
        )
        self.assertEqual(check_gate.read_shipped_abis(drifted), ["arm64-v8a"])

    def test_missing_or_empty_abi_list_is_an_input_error(self):
        empty = self.tmp / "b1.kts"
        empty.write_text('abiFilters += listOf()', encoding="utf-8")
        with self.assertRaises(check_gate.GateError):
            check_gate.read_shipped_abis(empty)
        absent = self.tmp / "b2.kts"
        absent.write_text("android { }", encoding="utf-8")
        with self.assertRaises(check_gate.GateError):
            check_gate.read_shipped_abis(absent)

    def test_unshipped_missing_and_unrecognised_abi_exclude_the_cell(self):
        for abi in ("x86", "arm64", "aarch64", "64-bit", None, ""):
            with self.subTest(abi=abi):
                classes = full_coverage(abi="arm64-v8a")
                if abi is None:
                    del classes["pixel-like"]["scenarios"]["G1"]["abi"]
                else:
                    classes["pixel-like"]["scenarios"]["G1"]["abi"] = abi
                report = self.run_gate(record(classes=classes))
                self.assertBlocked(report, "G1")
                # excluded, not failed: the scenario itself is not judged
                self.assertEqual(report.hard_failures, [])

    def test_x86_64_counts_because_the_build_ships_it(self):
        self.assertPublishable(
            self.run_gate(record(classes=full_coverage(abi="x86_64"))), "x86_64 ships"
        )

    def test_abi_parser_tracks_the_build_not_a_copy(self):
        # If the build drops an ABI, a cell on it stops qualifying with no code change.
        classes = full_coverage(abi="x86_64")
        self.assertBlocked(
            self.run_gate(record(classes=classes), abis=["arm64-v8a"]), "not shipped"
        )

    # --- class derivation --------------------------------------------------

    def test_class_key_must_agree_with_its_own_answers(self):
        # xiaomi-class with zero hostile answers: the key the protocol calls invalid.
        self.assertBlocked(
            self.run_gate(record(classes=full_coverage(classes=("xiaomi-class",)))),
            "derive",
        )

    def test_class_is_recomputed_from_the_hostility_count(self):
        for key, answers in (
            ("pixel-like", ("no", "no", "no")),
            ("samsung-class", ("yes", "no", "no")),
            ("xiaomi-class", ("yes", "yes", "no")),
            ("xiaomi-class", ("yes", "yes", "yes")),
        ):
            with self.subTest(key=key, answers=answers):
                classes = full_coverage(classes=(key,))
                hostility = classes[key]["hostility"]
                for probe, answer in zip(check_gate.HOSTILITY_PROBES, answers):
                    hostility[probe] = answer
                self.assertPublishable(
                    self.run_gate(record(classes=classes)), f"{key} from {answers}"
                )

    def test_entry_without_hostility_is_refused(self):
        classes = full_coverage()
        del classes["pixel-like"]["hostility"]
        self.assertBlocked(self.run_gate(record(classes=classes)), "falsified")

    def test_answer_outside_the_three_values_is_refused(self):
        for bad in ("Yes", "true", 1, None, ""):
            with self.subTest(answer=bad):
                classes = full_coverage()
                classes["pixel-like"]["hostility"]["background_survival"] = bad
                self.assertBlocked(self.run_gate(record(classes=classes)), "not one of")

    def test_any_unknown_carries_no_class_and_is_not_defaulted(self):
        for probe in check_gate.HOSTILITY_PROBES:
            with self.subTest(probe=probe):
                classes = full_coverage()
                classes["pixel-like"]["hostility"][probe] = "unknown"
                report = self.run_gate(record(classes=classes))
                self.assertBlocked(report, "no class")
                self.assertEqual(report.hard_failures, [])
                self.assertTrue(report.coverage[0].unresolved)
                # Not credited as pixel-like either.
                self.assertEqual(report.complete_classes, [])

    def test_unknown_class_key_is_refused(self):
        classes = full_coverage()
        classes["pixel_6_emulator"] = classes.pop("pixel-like")
        self.assertBlocked(self.run_gate(record(classes=classes)), "not one of")

    # --- the bars ----------------------------------------------------------

    def test_prerelease_needs_one_class_and_shipped_needs_three(self):
        self.assertEqual(check_gate.required_class_bar("v0.2.0"), 1)
        self.assertEqual(check_gate.required_class_bar("v1.0.0"), 3)
        self.assertEqual(check_gate.required_class_bar("v2.0.0"), 3)

        one = record(classes=full_coverage(classes=("pixel-like",)), version_code=1000000)
        self.assertBlocked(self.run_gate(one, tag="v1.0.0", version_code=1000000,
                                         version_name="1.0.0"), "Shipped")

        def derived(*classes):
            built = full_coverage(classes=classes)
            if "samsung-class" in built:
                built["samsung-class"]["hostility"]["background_survival"] = "yes"
            if "xiaomi-class" in built:
                built["xiaomi-class"]["hostility"]["background_survival"] = "yes"
                built["xiaomi-class"]["hostility"]["overlay_persistence"] = "yes"
            return built

        two = record(classes=derived("pixel-like", "samsung-class"))
        self.assertBlocked(self.run_gate(two, tag="v1.0.0", version_code=1000000,
                                         version_name="1.0.0"), "Shipped")

        three = record(classes=derived("pixel-like", "samsung-class", "xiaomi-class"))
        self.assertPublishable(
            self.run_gate(three, tag="v1.0.0", version_code=1000000, version_name="1.0.0"),
            "three classes on a v1 tag",
        )

    def test_empty_classes_blocks_both_bars_and_is_absence_not_zero(self):
        rec = record(classes={})
        for tag, code, name in (("v0.2.0", 2000, "0.2.0"), ("v1.0.0", 1000000, "1.0.0")):
            with self.subTest(tag=tag):
                report = self.run_gate(rec, tag=tag, version_code=code, version_name=name)
                self.assertEqual(report.hard_failures, [])
                self.assertFalse(report.may_publish())
                self.assertIn("no run has been performed", " ".join(report.notes).lower())

    def test_waiver_covers_insufficient_coverage_only(self):
        rec = record(classes={})
        self.assertFalse(self.run_gate(rec).may_publish())
        self.assertIn("Not met", check_gate.render_report(self.run_gate(rec)))
        waived = self.run_gate(rec, waiver="no third device class available yet")
        self.assertEqual(waived.hard_failures, [])
        self.assertTrue(waived.may_publish())
        self.assertEqual(waived.verdict(), "waived")
        # whitespace is not a reason
        self.assertFalse(self.run_gate(rec, waiver="   \n ").may_publish())

    def test_the_waiver_path_is_unreachable_until_the_record_has_an_identity(self):
        # The state this repository is actually in. Null identity is not a coverage gap,
        # so a reason cannot buy a release here — and a publisher hitting that needs it
        # said plainly rather than discovering it as a mysterious refusal.
        rec = record(classes={})
        rec["tag"] = None
        rec["commit"] = None
        rec["version_code"] = None
        rec["apk_sha256"] = None
        rec["protocol_commit"] = None
        rec["protocol_reviewed"] = False
        report = self.run_gate(rec, waiver="no hardware yet", pin=False)
        self.assertFalse(report.may_publish())
        self.assertEqual(report.verdict(), "blocked")
        joined = " ".join(report.hard_failures)
        self.assertIn("null", joined)
        self.assertIn("unreviewed", joined)

        # Record the identity and the protocol, and the same waiver now works, still
        # reporting the coverage gap truthfully.
        identified = record(classes={})
        waived = self.run_gate(identified, waiver="no hardware yet")
        self.assertTrue(waived.may_publish())
        self.assertEqual(waived.verdict(), "waived")
        self.assertIn("0/1 required", check_gate.render_report(waived))

    def test_a_waiver_does_not_invent_green_coverage(self):
        rec = record(classes=full_coverage(classes=("pixel-like",)),
                     version_code=1000000)
        report = self.run_gate(rec, tag="v1.0.0", version_code=1000000,
                               version_name="1.0.0", waiver="no Samsung or Xiaomi handset available")
        self.assertTrue(report.may_publish())
        self.assertEqual(report.verdict(), "waived")
        # truth is preserved: coverage still reports one class, and the reason is quoted
        self.assertEqual(len(report.complete_classes), 1)
        text = check_gate.render_report(report)
        self.assertIn("1/3 required", text)
        self.assertIn("no Samsung or Xiaomi handset available", text)
        self.assertIn("published with insufficient device coverage", text)

    # --- the real document -------------------------------------------------

    def test_the_repositorys_own_record_blocks_and_says_why(self):
        rec = check_gate.read_gate_record(GATE_DOC)
        report = check_gate.evaluate(
            rec, tag="v0.2.0", source_commit=COMMIT_A, version_code=2000,
            version_name="0.2.0", shipped_abis=self.abis, apk_sha256=SHA_REAL,
        )
        self.assertEqual(rec["gate_version"], 6)
        self.assertEqual(rec["required_scenarios"], REQUIRED)
        self.assertEqual(rec["classes"], {})
        self.assertFalse(report.may_publish())
        self.assertEqual(report.verdict(), "blocked")

    def test_report_names_every_missing_required_scenario(self):
        classes = full_coverage()
        for gid in ("G1", "G9", "G15"):
            del classes["pixel-like"]["scenarios"][gid]
        report = self.run_gate(record(classes=classes))
        text = check_gate.render_report(report)
        for gid in ("G1", "G9", "G15"):
            self.assertIn(gid, text)


if __name__ == "__main__":
    unittest.main(verbosity=2)
