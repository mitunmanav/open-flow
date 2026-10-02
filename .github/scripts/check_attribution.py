#!/usr/bin/env python3
"""Enforce the repository's attribution rules on every commit.

The rules this gate defends, in one place so CI and a local hook agree:

  R1  No commit may carry a `Co-authored-by:` (or `Signed-off-by:`) trailer
      naming a bot, an automation account, or an AI assistant. GitHub counts
      trailer names as contributors; an agent is not a contributor and must not
      appear in this repository's contributor graph.
  R2  No commit that lands on `main` may be authored by anyone but the repo
      owner. `main` is the owner's line; a collaborator's work arrives as a PR.
  R3  No commit at all may be authored or committed by a bot account. Bots may
      open pull requests (Dependabot is expected to), but automation must never
      write to a branch in this repository.

Human co-authors are not the target. GitHub appends `Co-authored-by` trailers
on its own when it squashes a multi-author pull request, and squashing that away
is not this repository's business.

Usage:

    python3 .github/scripts/check_attribution.py                 # HEAD~1..HEAD
    python3 .github/scripts/check_attribution.py <rev-range>     # explicit range

Exit codes: 0 clean, 1 violations found, 2 the range could not be read.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys

# The repo owner. Commits on `main` must be this author; see AGENTS.md.
OWNER_NAME = "Mitun Manav G Y"
OWNER_EMAIL = "238927830+mitunmanav@users.noreply.github.com"

TRAILER_RE = re.compile(r"^(Co-authored-by|Co-authored|Signed-off-by|Reviewed-by|Tested-by|Author|Committer)\s*:\s*(.+)$", re.IGNORECASE)

# Account names GitHub treats as automation. The `[bot]` suffix is GitHub's own
# convention and covers new ones; the rest are named because they are common
# enough in Gradle/Node/Android repos that they show up here too.
BOT_ACCOUNT_RE = re.compile(r"(\[bot\]$|(^|[-_@.])bot($|[-_@.])|dependabot|github-actions|renovate|release-please|imgbot|greenkeeper|stale$)", re.IGNORECASE)

# Assistant identities. Kept as a list of name and address fragments because
# both spellings appear in the wild ("Co-authored-by: Claude <noreply@...>",
# "Generated with Claude Code").
AI_IDENTITY_RE = re.compile(
    r"\b("
    r"claude|anthropic|chatgpt|openai|gpt-?\d|copilot|cursor|codeium|devin|"
    r"gemini|google ai|replit agent|cody|opencode|aider|devin|agent|assistant|llm"
    r")\b",
    re.IGNORECASE,
)

# Bot *emails* that carry no `[bot]` suffix.
BOT_EMAIL_RE = re.compile(r"(noreply@github\.com$|^users\.noreply\.github\.com$|@bots?\.|$@github\.com$)", re.IGNORECASE)


class Commit:
    def __init__(self, sha: str, subject: str, author: str, committer: str, body: str) -> None:
        self.sha = sha
        self.subject = subject
        self.author = author
        self.committer = committer
        self.body = body


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True
    ).stdout


def parse_identity(raw: str) -> tuple[str, str]:
    """Split a `Name <email>` identity."""
    match = re.match(r"^(?P<name>.*?)\s*<(?P<email>[^>]*)>\s*$", raw.strip())
    if not match:
        return raw.strip(), ""
    return match.group("name").strip(), match.group("email").strip()


def load_commits(rev_range: str, limit: int) -> list[Commit]:
    fmt = "%H%x1f%s%x1f%an <%ae>%x1f%cn <%ce>%x1f%B%x1e"
    out = git("log", f"--max-count={limit}", f"--pretty=format:{fmt}", rev_range)
    commits: list[Commit] = []
    for chunk in out.split("\x1e"):
        chunk = chunk.strip("\n")
        if not chunk.strip():
            continue
        parts = chunk.split("\x1f")
        if len(parts) < 5:
            continue
        commits.append(Commit(parts[0], parts[1], parts[2], parts[3], parts[4]))
    return commits


def is_bot(name: str, email: str) -> bool:
    return bool(BOT_ACCOUNT_RE.search(name) or BOT_ACCOUNT_RE.search(email.split("@")[0]) or BOT_EMAIL_RE.search(email))


# GitHub's own merge machinery, not an automation account. When a PR is merged or
# squashed through the web UI, GitHub commits on the owner's behalf and records
# itself as committer; and every `pull_request` event carries a synthetic
# test-merge commit whose committer is this same identity. Flagging it as a bot
# made the check unsatisfiable -- it failed every PR, and would then fail on
# `main` the moment an owner merged through the UI. AGENTS.md tolerates exactly
# this: "the trailers GitHub appends by itself when squashing".
#
# Exempt as COMMITTER only. Nothing may author a commit as GitHub, and the real
# automation accounts ([bot] accounts, dependabot, github-actions) stay flagged.
GITHUB_MERGE_IDENTITY = ("github", "noreply@github.com")


def is_github_merge_committer(name: str, email: str) -> bool:
    return name.strip().casefold() == GITHUB_MERGE_IDENTITY[0] and email.strip().casefold() == GITHUB_MERGE_IDENTITY[1]


def is_ai(name: str, email: str) -> bool:
    return bool(AI_IDENTITY_RE.search(name) or AI_IDENTITY_RE.search(email))


def is_owner(name: str, email: str) -> bool:
    return name.strip().casefold() == OWNER_NAME.casefold() or email.strip().casefold() == OWNER_EMAIL.casefold()


def check_commit(commit: Commit, on_main: bool) -> list[str]:
    problems: list[str] = []
    short = commit.sha[:9]

    a_name, a_email = parse_identity(commit.author)
    c_name, c_email = parse_identity(commit.committer)

    # R1 — no AI or bot in a trailer.
    for line in commit.body.splitlines():
        trailer = TRAILER_RE.match(line.strip())
        if not trailer:
            continue
        value = trailer.group(2).strip()
        t_name, t_email = parse_identity(value)
        if is_bot(t_name, t_email) or is_ai(t_name, t_email):
            problems.append(
                f"{short} R1: trailer '{trailer.group(1)}: {value}' names a bot or an assistant. "
                "Only humans are co-authors in this repository."
            )

    # R2 — main is owner-authored.
    if on_main and not is_owner(a_name, a_email):
        problems.append(
            f"{short} R2: author '{commit.author}' is not the repo owner "
            f"({OWNER_NAME} <{OWNER_EMAIL}>). Owner-authored on main only."
        )

    # R3 — no bot-authored or bot-committed commits.
    if is_bot(a_name, a_email):
        problems.append(f"{short} R3: author '{commit.author}' is a bot account. Automation must not commit here.")
    if is_bot(c_name, c_email) and not is_github_merge_committer(c_name, c_email):
        problems.append(f"{short} R3: committer '{commit.committer}' is a bot account.")

    # Attribution integrity — an assistant must never appear as the author.
    if is_ai(a_name, a_email):
        problems.append(
            f"{short} R3: author '{commit.author}' is an assistant. "
            "The human owner is the author; the assistant is a tool, not a contributor."
        )

    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description="Enforce the repo's commit-attribution rules.")
    parser.add_argument("rev_range", nargs="?", default=None, help="rev range to check (default: last commit on HEAD)")
    parser.add_argument("--main", action="store_true", help="treat the range as landing on main (enforces R2)")
    parser.add_argument("--limit", type=int, default=200, help="max commits to inspect")
    args = parser.parse_args()

    rev_range = args.rev_range
    if rev_range is None:
        rev_range = "HEAD" if args.main else "HEAD~1..HEAD"

    try:
        commits = load_commits(rev_range, args.limit)
    except subprocess.CalledProcessError as exc:
        print(f"check-attribution: cannot read rev range '{rev_range}': {exc.stderr.strip()}", file=sys.stderr)
        return 2

    if not commits:
        print(f"check-attribution: no commits in '{rev_range}'.")
        return 0

    problems: list[str] = []
    for commit in commits:
        problems.extend(check_commit(commit, on_main=args.main))

    for problem in problems:
        print(f"::error::{problem}", file=sys.stderr)

    if problems:
        print(
            f"\ncheck-attribution: {len(problems)} attribution violation(s) in "
            f"{len(commits)} commit(s) of '{rev_range}'.\n"
            "  - Remove any Co-authored-by/Signed-off-by trailer that names a bot or an assistant.\n"
            "  - Only the repo owner authors on main.\n"
            "  - Dependabot PRs are welcome; the bot's commits must not be merged into main as-is.",
            file=sys.stderr,
        )
        return 1

    print(f"check-attribution: OK — {len(commits)} commit(s) in '{rev_range}' clean.")
    return 0


if __name__ == "__main__":
    sys.exit(main())