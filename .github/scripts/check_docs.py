#!/usr/bin/env python3
"""Fail the build on stale documentation.

"Stale" here means a document that lies to its reader: it points at a file that
does not exist, promises a file that was never written, or is a record that no
longer matches the thing it records. Every rule below is a way a document in
this repo has actually been wrong already, not a hypothetical lint.

Called by `.github/workflows/docs-check.yml` and, if you want the same gate
before you push, by a local git hook:

    python3 .github/scripts/check_docs.py

Known violations may be listed in `.github/docs-baseline.txt` to let the gate
land without going red on day one. The baseline ratchets: a baseline entry that
stops matching a real violation is itself a failure, so the file cannot quietly
become a permanent exemption list. Removing the last entry deletes the baseline.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import re
import sys
from collections import Counter
from pathlib import Path

# Directories that never contain linkable documentation.
SKIP_DIRS = {
    ".git",
    ".gradle",
    "build",
    ".idea",
    ".kotlin",
    ".worktrees",
    "node_modules",
    "out",
    "__pycache__",
}

DOCS_ROOT = "docs"
ADR_DIR = Path(DOCS_ROOT) / "adr"
INDEX = Path(DOCS_ROOT) / "README.md"
BASELINE = Path(".github/docs-baseline.txt")

# Scope. This gate governs documentation this project publishes and maintains,
# so it deliberately does not read two other kinds of markdown:
#
#   .agents/  vendored agent skills pinned by skills-lock.json. Editing them
#             here would fight their upstream and they are not our documents.
#   .scratch/ the wayfinder working set — a live planning conversation, which
#             forward-references files that are being written right now.
#
# Link *targets* in those trees are still resolved; it is only their prose that
# goes unchecked. That distinction matters: ADR-0006 (in scope) already catches
# the missing provider guides, so excluding .scratch/ loses no real coverage.
SCOPE_ROOTS = ("docs", ".github")
SCOPE_ROOT_FILES = True

# Markdown link target: inline image, then link, then optional title.
LINK_RE = re.compile(r"!?\[[^\]]*\]\(\s*(<[^>]+>|[^\s)]+)(?:\s+[\"'][^\"']*[\"'])?\s*\)")
# Inline code span.
CODE_RE = re.compile(r"`([^`\n]+)`")
# Fenced code block, including an optional info string.
FENCE_RE = re.compile(r"^(?:```|~~~).*?^(?:```|~~~)\s*$", re.MULTILINE | re.DOTALL)
# A backticked token that names a file we could plausibly have written already.
FILEREF_RE = re.compile(
    r"(?<![\w/.-])"
    r"((?:[\w.-]+/)*[\w.-]+\.(?:md|kt|kts|java|xml|yml|yaml|json|toml|gradle|properties|html|css|js|ts))"
    r"(?![\w/.-])"
)
# Extensions worth resolving. Deliberately narrow: `LICENSE` and `SECURITY.md`
# are documents too, so both are checked.
KNOWN_SUFFIXES = {".md", ".kt", ".kts", ".java", ".xml", ".yml", ".yaml", ".json", ".html", ".css", ".js"}

# Markers that turn a reference into a declared forward reference rather than a
# broken promise. "X, if it exists" and "results, when run" are honest English
# for "this may not be here"; a bare mention is not. This is a phrase
# heuristic and it is deliberately biased toward silence: a rule that punishes
# a correct conditional sentence gets worked around by deleting the sentence,
# which is a worse outcome than missing one violation. The complement is the
# ratchet below — the baseline can shrink, never grow silently.
FORWARD_MARKERS = re.compile(
    r"\b("
    r"if (?:it|a|the)? ?\w* ?exists|when run|when a |when the |once |not yet|planned|"
    r"presence of|will be|to be (?:written|created|added)|future|"
    r"if (?:any|the) of these"
    r")\b",
    re.IGNORECASE,
)

# `Vad.kt`, `OnlineRecognizer.kt`: a CamelCase stem names a type in a third-party
# library, not a file this repo should contain. Our own files are kebab- or
# snake_case, which is a house rule worth not having to state in every doc.
CAMEL_TYPE_RE = re.compile(r"^[A-Z][A-Za-z0-9]*$")

SCHEME_RE = re.compile(r"^([a-zA-Z][a-zA-Z0-9+.-]*):")
EXTERNAL_SCHEMES = {"https", "mailto", "tel"}


class Finding:
    """One violation, identified by a key that is stable across runs."""

    def __init__(self, rule: str, path: str, detail: str = "") -> None:
        self.rule = rule
        self.path = path
        self.detail = detail
        self.key = f"{rule}:{path}" + (f":{detail}" if detail else "")

    def __str__(self) -> str:  # pragma: no cover - formatting only
        where = self.path or "<repo>"
        return f"{self.rule}: {where}" + (f" — {self.detail}" if self.detail else "")


def in_scope(rel: Path) -> bool:
    """Is this markdown a document this project publishes and maintains?"""
    if any(part in SKIP_DIRS for part in rel.parts):
        return False
    parts = rel.parts
    if len(parts) == 1:
        return SCOPE_ROOT_FILES
    return parts[0] in SCOPE_ROOTS


def markdown_files(root: Path) -> list[Path]:
    out: list[Path] = []
    for path in sorted(root.rglob("*.md")):
        rel = path.relative_to(root)
        if in_scope(rel):
            out.append(path)
    return out


def resolve_file_ref(root: Path, md: Path, ref: str) -> Path:
    """Resolve a backticked path the way a reader would try it.

    A bare filename is tried next to the document first, then at the repo root,
    so `triage-labels.md` in `docs/agents/` means its sibling rather than a
    mystery file at the top level.
    """
    if ref.startswith("."):
        return (md.parent / ref).resolve()
    sibling = (md.parent / ref).resolve()
    if sibling.exists():
        return sibling
    return (root / ref).resolve()


def is_external(target: str) -> bool:
    return bool(SCHEME_RE.match(target)) or target.startswith("//")


def strip_fragment(target: str) -> str:
    return target.split("#", 1)[0].split("?", 1)[0]


def prose_only(text: str) -> str:
    """Blank out fenced code blocks, keeping line numbers intact.

    Text inside a fence is literal: a `[link](path)` there is an illustration,
    and a path in an ASCII directory tree is a shape, not a claim. Checking
    them produces false positives, which is worse than not checking them.
    """
    return FENCE_RE.sub(lambda m: "\n" * m.group(0).count("\n"), text)


def check_links(root: Path, files: list[Path]) -> list[Finding]:
    """Every relative markdown link must resolve on disk."""
    findings: list[Finding] = []
    for md in files:
        rel = md.relative_to(root).as_posix()
        text = prose_only(md.read_text(encoding="utf-8"))
        for match in LINK_RE.finditer(text):
            raw = match.group(1).strip()
            target = raw[1:-1] if raw.startswith("<") and raw.endswith(">") else raw
            if is_external(target):
                scheme = SCHEME_RE.match(target)
                if scheme and scheme.group(1) == "http":
                    findings.append(Finding("insecure-link", rel, target))
                continue
            bare = strip_fragment(target)
            if not bare:
                continue  # pure `#anchor` link
            resolved = (md.parent / bare).resolve()
            if not resolved.exists():
                findings.append(Finding("broken-link", rel, target))
    return findings


def check_file_refs(root: Path, files: list[Path]) -> list[Finding]:
    """Backticked file paths must exist, unless the reference is declared
    forward-looking or names a type in someone else's library.

    The directory condition is what makes this rule useful instead of
    exhausting: `providers/whisper/Foo.kt` may be named before `providers/`
    exists, but `docs/providers/thing.md` may not be named when
    `docs/providers/` exists and `thing.md` does not. That is precisely the
    shape of ADR-0006 promising three provider guides that were never written.
    """
    findings: list[Finding] = []
    for md in files:
        rel = md.relative_to(root).as_posix()
        text = prose_only(md.read_text(encoding="utf-8"))

        # Forward-reference markers are scoped to the paragraph, not the line.
        # "Not yet written: `a.md`,\n`b.md`" is one sentence spread over two
        # lines, and a line-scoped rule would flag the wrapped half.
        for paragraph in re.split(r"\n\s*\n", text):
            if FORWARD_MARKERS.search(paragraph):
                continue
            for span in CODE_RE.finditer(paragraph):
                token = span.group(1).strip()
                if token.startswith("-") or " " in token:
                    continue
                # A path template (`.scratch/<effort>/map.md`) is a shape.
                if "<" in token or ">" in token:
                    continue
                for hit in FILEREF_RE.finditer(token):
                    ref = hit.group(1)
                    if Path(ref).suffix.lower() not in KNOWN_SUFFIXES:
                        continue
                    if ref.startswith("http"):
                        continue
                    if CAMEL_TYPE_RE.match(Path(ref).stem):
                        continue  # a type in a third-party library
                    resolved = resolve_file_ref(root, md, ref)
                    if resolved.exists():
                        continue
                    if resolved.parent.exists():
                        findings.append(Finding("missing-file-ref", rel, ref))
    return findings


def check_adr_sequence(root: Path) -> list[Finding]:
    """ADRs are numbered from 0001 with no gaps and no duplicates."""
    findings: list[Finding] = []
    adr_dir = root / ADR_DIR
    if not adr_dir.is_dir():
        return [Finding("adr-index-missing", "docs/adr", "directory absent")]

    seen: dict[int, str] = {}
    for path in sorted(adr_dir.glob("*.md")):
        if path.name.lower() == "readme.md":
            continue
        match = re.match(r"^(\d{4})-", path.name)
        if not match:
            findings.append(Finding("adr-numbering", f"{ADR_DIR.as_posix()}/{path.name}", "filename has no NNNN- prefix"))
            continue
        number = int(match.group(1))
        if number in seen:
            findings.append(Finding("adr-numbering", f"{ADR_DIR.as_posix()}/{path.name}", f"duplicates {seen[number]}"))
        seen[number] = path.name

    if seen:
        expected = list(range(1, max(seen) + 1))
        missing = [n for n in expected if n not in seen]
        if missing:
            names = ", ".join(f"{n:04d}" for n in missing)
            findings.append(Finding("adr-sequence", ADR_DIR.as_posix(), f"gap at {names}"))
    return findings


def check_adr_records(root: Path) -> list[Finding]:
    """Each ADR's title, date, and status must agree with its filename."""
    findings: list[Finding] = []
    adr_dir = root / ADR_DIR
    if not adr_dir.is_dir():
        return findings

    for path in sorted(adr_dir.glob("[0-9]*.md")):
        rel = path.relative_to(root).as_posix()
        text = path.read_text(encoding="utf-8")

        match = re.match(r"^(\d{4})-", path.name)
        want = match.group(1) if match else ""
        heading = re.search(r"^#\s+(\d{4})\s*:", text, re.MULTILINE)
        if not heading:
            findings.append(Finding("adr-heading", rel, f"no '# {want}: ...' heading"))
        elif heading.group(1) != want:
            findings.append(Finding("adr-heading", rel, f"title says {heading.group(1)}, filename says {want}"))

        date = re.search(r"^Date:\s*(\S+)", text, re.MULTILINE)
        if not date:
            findings.append(Finding("adr-date", rel, "no 'Date: YYYY-MM-DD' line"))
        else:
            try:
                dt.date.fromisoformat(date.group(1))
            except ValueError:
                findings.append(Finding("adr-date", rel, f"unparseable date {date.group(1)!r}"))

        if not re.search(r"^##\s+Status\s*$", text, re.MULTILINE):
            findings.append(Finding("adr-status", rel, "no '## Status' section"))

        for section in ("Context", "Decision"):
            if not re.search(rf"^##\s+{section}\s*$", text, re.MULTILINE):
                findings.append(Finding("adr-section", rel, f"no '## {section}' section"))
    return findings


def check_index(root: Path) -> list[Finding]:
    """Every document under docs/ must be reachable from docs/README.md."""
    findings: list[Finding] = []
    index_path = root / INDEX
    docs_dir = root / DOCS_ROOT
    if not docs_dir.is_dir():
        return [Finding("docs-index-missing", DOCS_ROOT, "docs/ does not exist")]
    if not index_path.exists():
        return [Finding("docs-index-missing", INDEX.as_posix(), "no docs index; every doc must be reachable from it")]

    index_text = index_path.read_text(encoding="utf-8")
    for path in sorted(docs_dir.rglob("*.md")):
        rel = path.relative_to(root)
        if any(part in SKIP_DIRS for part in rel.parts):
            continue
        if rel == INDEX:
            continue
        if Path(rel.name).name not in index_text:
            findings.append(Finding("orphan-doc", rel.as_posix(), "not listed in docs/README.md"))
    return findings


def check_changelog(root: Path) -> list[Finding]:
    """A CHANGELOG that exists but is empty is worse than none: it reads as
    'nothing has happened yet' on a repo with tagged history."""
    path = root / "CHANGELOG.md"
    if not path.exists():
        return [Finding("changelog-missing", "CHANGELOG.md", "absent")]

    text = path.read_text(encoding="utf-8")
    # Comments first, and with DOTALL on their own pass: `#.*` under DOTALL
    # would match from the first heading to end of file and empty the document.
    text = re.sub(r"<!--.*?-->", "", text, flags=re.DOTALL)
    text = re.sub(r"^\s*#.*$", "", text, flags=re.MULTILINE)
    if not text.strip():
        return [Finding("changelog-empty", "CHANGELOG.md", "no entries")]
    return []


SITE_DIR = Path("website")

# A hand-written site has no generator, so nothing stops its four pages drifting
# apart: add a page and update three navs, forget one, and a visitor lands on a
# page whose links are subtly wrong. That is the same failure as the unwritten
# provider guides, one layer up, so it gets a rule. See ticket 26.
NAV_RE = re.compile(r"<nav\b[^>]*>(.*?)</nav>", re.DOTALL | re.IGNORECASE)
FOOTER_NAV_RE = re.compile(r"<ul\b[^>]*\bdata-nav\b[^>]*>(.*?)</ul>", re.DOTALL | re.IGNORECASE)
HREF_RE = re.compile(r"""\bhref\s*=\s*["']([^"']+)["']""", re.IGNORECASE)
SRC_RE = re.compile(r"""\bsrc\s*=\s*["']([^"']+)["']""", re.IGNORECASE)
CSS_URL_RE = re.compile(r"""url\(\s*["']?([^"')]+)["']?\s*\)""")


def site_pages(root: Path) -> list[str]:
    site = root / SITE_DIR
    if not site.is_dir():
        return []
    return sorted(p.name for p in site.glob("*.html"))


def internal_html(hrefs: list[str]) -> list[str]:
    """Relative links that point at another page of this site."""
    return [h for h in hrefs if not is_external(h) and h.endswith(".html")]


def check_site_nav(root: Path) -> list[Finding]:
    """Every page must list every page, in the same order, in its nav.

    Three ways this drifts, all real: a page is added and only some navs learn
    about it; a page is renamed and a nav keeps pointing at the old name; the
    footer's list of pages and the header nav disagree. Each is silent, each is
    a broken link for a visitor, and none of them is visible in review of a
    single file.
    """
    findings: list[Finding] = []
    pages = site_pages(root)
    if not pages:
        return findings

    site = root / SITE_DIR
    navs: dict[str, list[str]] = {}

    for name in pages:
        rel = f"{SITE_DIR.as_posix()}/{name}"
        text = (site / name).read_text(encoding="utf-8")

        nav = NAV_RE.search(text)
        if not nav:
            findings.append(Finding("site-nav-missing", rel, "no <nav> element to check"))
            continue
        hrefs = internal_html(HREF_RE.findall(nav.group(1)))
        navs[name] = hrefs

        footer = FOOTER_NAV_RE.search(text)
        if not footer:
            findings.append(
                Finding("site-nav-missing", rel, "the footer's page list is not marked data-nav")
            )
        elif internal_html(HREF_RE.findall(footer.group(1))) != hrefs:
            findings.append(
                Finding(
                    "site-nav-footer-mismatch",
                    rel,
                    "footer lists "
                    f"{internal_html(HREF_RE.findall(footer.group(1)))} but the nav lists {hrefs}",
                )
            )

    # One report, not one per page: the interesting information is which pages
    # are the odd ones out, so name the majority and list only the deviants.
    tally = Counter(tuple(v) for v in navs.values())
    canonical = tally.most_common(1)[0][0]
    if len(tally) > 1:
        deviants = "; ".join(
            f"{name} -> {', '.join(hrefs) or '(none)'}"
            for name, hrefs in sorted(navs.items())
            if tuple(hrefs) != canonical
        )
        findings.append(
            Finding(
                "site-nav-divergent",
                SITE_DIR.as_posix(),
                f"every page should list {list(canonical)}; deviates: {deviants}",
            )
        )

    for name, hrefs in sorted(navs.items()):
        rel = f"{SITE_DIR.as_posix()}/{name}"
        for href in hrefs:
            if not (site / href).exists():
                findings.append(Finding("site-nav-dead", rel, f"nav points at {href}, which does not exist"))
        for page in pages:
            if page not in hrefs:
                findings.append(
                    Finding("site-nav-orphan", rel, f"{page} exists but is not listed in the nav")
                )

    return findings


def check_site_assets(root: Path) -> list[Finding]:
    """Relative hrefs, srcs and CSS url()s in the site must exist on disk.

    There is no build step to complain, so a renamed stylesheet or a moved
    font file ships as a silently unstyled page. External URLs are not fetched:
    a gate that depends on the network is a gate that fails for reasons that
    have nothing to do with the repository.
    """
    findings: list[Finding] = []
    site = root / SITE_DIR
    if not site.is_dir():
        return findings

    for page in site_pages(root):
        rel = f"{SITE_DIR.as_posix()}/{page}"
        text = (site / page).read_text(encoding="utf-8")
        targets = HREF_RE.findall(text) + SRC_RE.findall(text)
        for ref in targets:
            bare = strip_fragment(ref)
            if not bare or is_external(ref):
                continue
            if not (site / bare).exists():
                findings.append(Finding("site-missing-asset", rel, ref))

    for sheet in sorted(site.rglob("*.css")):
        rel = sheet.relative_to(root).as_posix()
        for ref in CSS_URL_RE.findall(sheet.read_text(encoding="utf-8")):
            bare = strip_fragment(ref)
            if not bare or is_external(ref) or bare.startswith("#"):
                continue
            if not (sheet.parent / bare).exists():
                findings.append(Finding("site-missing-asset", rel, ref))

    return findings


GATE_PROTOCOL = Path(DOCS_ROOT) / "quality" / "acceptance-gate.md"

# The gate's required-scenario registry lives in the live JSON block and the scenario
# definitions live in the tables under "The scenarios". Those are two statements of the
# same thing in one document, which is the shape that goes stale quietly: add a table row
# and forget the registry, and the release checker derives a total from a list that no
# longer describes what the protocol asks a tester to run. So the two are compared
# exactly, by identity rather than by count -- an equal-sized set with a replaced id is
# the case a count check waves through.
SCENARIOS_HEADING_RE = re.compile(r"^##\s+The scenarios\s*$", re.MULTILINE)
GATE_STATUS_HEADING_RE = re.compile(r"^##\s+Gate status\s*$", re.MULTILINE)
SECTION_BREAK_RE = re.compile(r"^##\s+\S")
FENCE_OPEN_RE = re.compile(r"^\s*```+\s*([A-Za-z0-9_+-]*)\s*$")
FENCE_CLOSE_RE = re.compile(r"^\s*```+\s*$")
TABLE_ROW_RE = re.compile(r"^\s*\|(.*)\|\s*$")
# A scenario id as the tables spell it. Anchored on the whole cell so a cell like
# "`G15` (see below)" is not silently accepted as G15.
SCENARIO_ID_RE = re.compile(r"^(?:G\d+)$")


def _section_lines(text: str, heading: re.Pattern[str]) -> list[str] | None:
    """The lines under a `##` heading, up to the next `##`."""
    match = heading.search(text)
    if not match:
        return None
    lines = text[match.end() :].splitlines()
    for index, line in enumerate(lines):
        if SECTION_BREAK_RE.match(line):
            return lines[:index]
    return lines


def _first_json_fence(lines: list[str]) -> str | None:
    """The first ```json block in a slice of the document."""
    index = 0
    while index < len(lines):
        opener = FENCE_OPEN_RE.match(lines[index])
        if opener:
            body: list[str] = []
            info = (opener.group(1) or "").lower()
            index += 1
            while index < len(lines) and not FENCE_CLOSE_RE.match(lines[index]):
                body.append(lines[index])
                index += 1
            if info == "json":
                return "\n".join(body)
        index += 1
    return None


def check_gate_scenarios(root: Path) -> list[Finding]:
    """The required-ID registry and the scenario tables must be the same set.

    Deliberately narrow: it reads the live JSON block and the first column of the tables
    under "The scenarios", and it does not scan prose for numerals. A dated historical
    count ("fourteen scenarios ... forty-two runs became") is a true statement about the
    past, and a rule that flagged it would train its readers to delete real history.
    """
    findings: list[Finding] = []
    path = root / GATE_PROTOCOL
    if not path.exists():
        return findings
    rel = GATE_PROTOCOL.as_posix()
    text = path.read_text(encoding="utf-8")

    status_lines = _section_lines(text, GATE_STATUS_HEADING_RE)
    if status_lines is None:
        return [Finding("gate-status-missing", rel, "no '## Gate status' section")]
    body = _first_json_fence(status_lines)
    if body is None:
        return [
            Finding("gate-status-missing", rel, "no 'json' block under '## Gate status'")
        ]
    try:
        record = json.loads(body)
    except json.JSONDecodeError as exc:
        return [Finding("gate-status-unparseable", rel, str(exc))]
    if not isinstance(record, dict) or "required_scenarios" not in record:
        return [
            Finding("gate-registry-missing", rel, "the live block has no required_scenarios")
        ]

    registry = record["required_scenarios"]
    if not isinstance(registry, list) or not registry:
        return [Finding("gate-registry-missing", rel, "required_scenarios is missing or empty")]
    if not all(isinstance(item, str) and item.strip() for item in registry):
        return [
            Finding("gate-registry-invalid", rel, "required_scenarios holds a non-id value")
        ]
    duplicates = sorted({item for item in registry if registry.count(item) > 1})
    if duplicates:
        findings.append(
            Finding("gate-registry-duplicate", rel, "repeats " + ", ".join(duplicates))
        )
    registry_set = set(registry)

    scenario_lines = _section_lines(text, SCENARIOS_HEADING_RE)
    if scenario_lines is None:
        return findings + [Finding("gate-scenarios-missing", rel, "no '## The scenarios' section")]

    # First column of every table row, skipping the header and its separator. The
    # separator is the one made only of dashes and colons.
    table_ids: list[str] = []
    for line in scenario_lines:
        row = TABLE_ROW_RE.match(line)
        if not row:
            continue
        cells = [cell.strip() for cell in row.group(1).split("|")]
        if not cells or not cells[0]:
            continue
        first = cells[0].strip("`* ")
        if not first or set(first) <= set("-: "):
            continue
        if SCENARIO_ID_RE.match(first):
            table_ids.append(first)

    if not table_ids:
        findings.append(
            Finding("gate-scenarios-missing", rel, "no scenario id found in the tables")
        )
        return findings

    repeated = sorted({item for item in table_ids if table_ids.count(item) > 1})
    if repeated:
        findings.append(
            Finding("gate-scenario-duplicate", rel, "table id(s) repeated: " + ", ".join(repeated))
        )
    table_set = set(table_ids)

    only_registry = sorted(registry_set - table_set)
    only_table = sorted(table_set - registry_set)
    for missing in only_registry:
        findings.append(
            Finding(
                "gate-scenario-mismatch",
                rel,
                f"required_scenarios lists {missing}, which no scenario table defines",
            )
        )
    for extra in only_table:
        findings.append(
            Finding(
                "gate-scenario-mismatch",
                rel,
                f"the scenario table defines {extra}, which required_scenarios omits",
            )
        )
    return findings


def read_baseline(root: Path, explicit: Path | None) -> set[str]:
    target = explicit if explicit is not None else root / BASELINE
    if not target.exists():
        return set()
    keys: set[str] = set()
    for line in target.read_text(encoding="utf-8").splitlines():
        line = line.split("#", 1)[0].strip()
        if line:
            keys.add(line)
    return keys


def main() -> int:
    parser = argparse.ArgumentParser(description="Fail on stale documentation.")
    parser.add_argument("--root", default=".", help="repository root")
    parser.add_argument("--baseline", default=None, help="path to the baseline file")
    parser.add_argument("--no-baseline", action="store_true", help="ignore the baseline, report every violation")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    files = markdown_files(root)

    findings: list[Finding] = []
    findings += check_links(root, files)
    findings += check_file_refs(root, files)
    findings += check_adr_sequence(root)
    findings += check_adr_records(root)
    findings += check_index(root)
    findings += check_changelog(root)
    findings += check_site_nav(root)
    findings += check_site_assets(root)
    findings += check_gate_scenarios(root)

    # Deduplicate: the same rule can fire twice on one target.
    unique: dict[str, Finding] = {}
    for finding in findings:
        unique.setdefault(finding.key, finding)
    findings = [unique[k] for k in sorted(unique)]

    if args.no_baseline:
        baseline: set[str] = set()
    else:
        baseline_arg = Path(args.baseline) if args.baseline else None
        baseline = read_baseline(root, baseline_arg)

    known = [f for f in findings if f.key in baseline]
    fresh = [f for f in findings if f.key not in baseline]
    # Ratchet: a baseline entry that no longer matches anything has been fixed.
    matched = {f.key for f in known}
    consumed = baseline - matched

    for finding in fresh:
        print(f"::error file={finding.path}::{finding}", file=sys.stderr)
    if fresh:
        print()
        print(f"docs-check: {len(fresh)} new documentation violation(s).", file=sys.stderr)
        print("Fix them, or add a line to .github/docs-baseline.txt if the reference", file=sys.stderr)
        print("is deliberately forward-looking. Baseline entries are one-shot.", file=sys.stderr)

    # Reported even when the run is already failing, so that closing a gap does
    # not take two rounds to notice that its exemption is now redundant.
    for key in sorted(consumed):
        print(f"::warning file=.github/docs-baseline.txt::baseline entry no longer matches a real violation, delete it: {key}", file=sys.stderr)
    if consumed:
        print(f"\ndocs-check: {len(consumed)} stale baseline entry(ies).", file=sys.stderr)
        print("That reference has been fixed or removed — the exemption must go too.", file=sys.stderr)

    if fresh or consumed:
        return 1

    checked = len(files)
    note = f" ({len(known)} baselined)" if known else ""
    print(f"docs-check: OK — {checked} markdown file(s) checked{note}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())