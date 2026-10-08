#!/usr/bin/env python3
"""Verification for check_docs.py's gate-scenario rule, run on every pull request.

    python3 -m unittest discover -s .github/scripts -p 'test_*.py'

Each test mutates a copy of the real protocol in exactly one way and asserts the rule
fires. Two of them are the cases a count check waves through: a scenario added to the
table without its registry entry, and a same-sized set with one ID replaced. A third
asserts the rule's *restraint* — a dated historical count in prose is a true statement
about the past, and a check that flagged it would train its readers to delete history.
"""

import shutil
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / ".github" / "scripts"))

import check_docs  # noqa: E402

PROTOCOL = REPO / "docs" / "quality" / "acceptance-gate.md"
REL = "docs/quality/acceptance-gate.md"


class GateScenarioRuleTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp)
        self.text = PROTOCOL.read_text(encoding="utf-8")

    def findings(self, text: str | None = None):
        root = self.tmp
        protocol = root / "docs" / "quality" / "acceptance-gate.md"
        protocol.parent.mkdir(parents=True, exist_ok=True)
        protocol.write_text(self.text if text is None else text, encoding="utf-8")
        return check_docs.check_gate_scenarios(root)

    def rules(self, text: str | None = None):
        return {f.rule for f in self.findings(text)}

    def test_the_repositorys_own_protocol_passes(self):
        self.assertEqual(self.findings(), [])

    def test_a_table_row_without_a_registry_entry_fails(self):
        drifted = self.text.replace(
            "| G15 | Router degradation and latency guard |",
            "| G15 | Router degradation and latency guard |\n"
            "| G16 | A scenario somebody added to the table only | Passes when it passes. |",
            1,
        )
        self.assertNotEqual(drifted, self.text, "the fixture edit did not apply")
        self.assertIn("gate-scenario-mismatch", self.rules(drifted))
        detail = " ".join(f.detail for f in self.findings(drifted))
        self.assertIn("G16", detail)

    def test_a_registry_entry_without_a_table_row_fails(self):
        drifted = self.text.replace(
            '    "G9", "G10", "G11", "G12", "G13", "G14", "G15"',
            '    "G9", "G10", "G11", "G12", "G13", "G14", "G15", "G99"',
            1,
        )
        self.assertNotEqual(drifted, self.text, "the fixture edit did not apply")
        self.assertIn("gate-scenario-mismatch", self.rules(drifted))

    def test_a_same_sized_set_with_a_replaced_id_fails(self):
        # G15 out of the registry, G99 in: the count is identical, so a count check
        # passes this and an identity check does not.
        drifted = self.text.replace('"G14", "G15"', '"G14", "G99"', 1)
        self.assertNotEqual(drifted, self.text, "the fixture edit did not apply")
        self.assertIn("gate-scenario-mismatch", self.rules(drifted))

    def test_a_duplicate_registry_entry_fails(self):
        drifted = self.text.replace('"G14", "G15"', '"G14", "G15", "G15"', 1)
        self.assertIn("gate-registry-duplicate", self.rules(drifted))

    def test_an_empty_or_missing_registry_fails(self):
        empty = self.text.replace(
            '  "required_scenarios": [\n'
            '    "G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8",\n'
            '    "G9", "G10", "G11", "G12", "G13", "G14", "G15"\n'
            "  ],",
            '  "required_scenarios": [],',
            1,
        )
        self.assertNotEqual(empty, self.text, "the fixture edit did not apply")
        self.assertIn("gate-registry-missing", self.rules(empty))

    def test_a_duplicate_table_id_fails(self):
        drifted = self.text.replace(
            "| G14 | Recovery re-insert |",
            "| G14 | Recovery re-insert |\n| G14 | Recovery re-insert, again | Passes when. |",
            1,
        )
        self.assertIn("gate-scenario-duplicate", self.rules(drifted))

    def test_a_dated_prose_count_is_not_a_violation(self):
        # The protocol already says "fourteen" and "forty-two" about the past. Those are
        # true statements about history, and the rule must leave them alone.
        drifted = self.text.replace(
            "## Two bars",
            "## Historical note\n\nThe gate grew to fourteen scenarios, and thirty runs\n"
            "became forty-two.\n\n## Two bars",
            1,
        )
        self.assertNotEqual(drifted, self.text, "the fixture edit did not apply")
        self.assertEqual(self.findings(drifted), [])

    def test_the_example_fence_is_not_read_as_the_record(self):
        # The populated example lists placeholder values — "<YYYY-MM-DD>",
        # "<vMAJOR.MINOR.PATCH>", a class key of "<device class>". If the rule read it
        # instead of the live block, the real registry of fifteen IDs would look like a
        # mismatch against the one scenario the example names.
        self.assertIn('"as_of": "<YYYY-MM-DD>"', self.text)
        self.assertIn('"<device class>"', self.text)
        self.assertEqual(self.findings(), [])

    def test_the_example_fence_is_not_read_when_the_live_block_is_removed(self):
        # Delete the live block and the rule must report the section as recordless,
        # not validate the populated example under its `###` subsection as if it were
        # the record. A section whose record was removed must not pass because a
        # worked example is still sitting below it.
        head, _, rest = self.text.partition("```json")
        _, _, tail = rest.partition("```")
        without_live = head + tail
        self.assertIn("### The shape", without_live)
        self.assertNotIn('"as_of": null', without_live)
        self.assertIn("gate-status-missing", self.rules(without_live))

    def test_a_missing_gate_status_section_fails_loudly(self):
        drifted = self.text.split("## Gate status")[0]
        self.assertIn("gate-status-missing", self.rules(drifted))

    def test_the_whole_docs_check_still_passes(self):
        # The new rule runs inside the existing gate, so a regression there is a
        # regression here.
        self.assertEqual(self.rules(), set())


if __name__ == "__main__":
    unittest.main(verbosity=2)
