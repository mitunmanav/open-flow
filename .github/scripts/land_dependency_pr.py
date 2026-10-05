#!/usr/bin/env python3
"""Land a Dependabot bump as an owner-authored commit, verified locally.

`AGENTS.md` and ADR-0009 say the same thing from two directions: automation may
propose, but a person authors and a person merges. Nothing implements that, and
the obvious implementation of "re-author by hand" is a trap -- `git cherry-pick`
preserves the *original* author, so it produces exactly the commit
`check_attribution.py` exists to reject. This script is the other route.

How it works, and why each step is what it is:

  fetch    The source branch or `pull/<n>/head` is fetched, not merged.
  merge    `git merge --squash` applies the bot's diff and stages it **without
           creating a commit**, so there is no authorship to preserve. The
           commit that follows is authored by whoever runs this script.
  message  The message is built from scratch. `merge --squash` leaves a
           SQUASH_MSG containing Dependabot's own body, including
           `Signed-off-by: dependabot[bot] <support@github.com>` -- a claim
           that did not happen, on a commit this owner performed. Reading
           SQUASH_MSG is how that line gets in, so this script never does.
  verify   `check_attribution.py --main <base>..HEAD` runs before anything is
           pushed. `enforce_admins` is on, this is a solo-maintained
           repository, and the check has already produced one false positive
           that made the repository unmergeable -- a remote red run has no
           cheap escape hatch, a local one costs a minute.
  stop     Nothing is pushed and no pull request is opened. Both are the human's,
           and pushing is the irreversible half.

It refuses to re-author anything that is not automation-authored. That is not
tidiness: a script whose purpose is "make this commit pass attribution" would,
applied to a collaborator's branch, launder a human's commit into the owner's
name -- R2's actual purpose inverted. Narrowing what the script will do is the
only reason it is safe to hand someone a script that runs the gate.

This script is not the enforcement point. CI is. It exists so the slow path is
reliable rather than skipped, and it has no flag that makes the check skippable.

Usage:

    python3 .github/scripts/land_dependency_pr.py 42
    python3 .github/scripts/land_dependency_pr.py dependabot/gradle/wrapper-9.x
    python3 .github/scripts/land_dependency_pr.py 42 --dry-run

A major bump additionally requires `--read-changelog`. That flag is a claim the
owner makes, not a thing this script can check, and the reason it exists is that
ADR-0009's argument for keeping automation out of authorship is accountability:
somebody is answerable for the bump. A procedure that did not say so would
quietly remove the person it was written to keep.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

# Importing the sibling module would otherwise write `__pycache__/` into
# `.github/scripts/` on every run. That directory is gitignored, so it would not
# dirty the tree -- but the owner should not have to remember that, and a script
# that litters the directory it lives in is asking to be trusted less.
sys.dont_write_bytecode = True

# One definition of "the owner" and of "a bot", so this script cannot drift from
# the gate it runs. Both are re-exported by check_attribution.py's own rules.
import check_attribution  # noqa: E402

SCRIPTS_DIR = Path(__file__).resolve().parent
ATTRIBUTION_CHECK = SCRIPTS_DIR / "check_attribution.py"

# Dependabot titles its pull requests "Bump <dep> from <a> to <b>", and a
# grouped bump carries one such pair per dependency in the body. Grouped titles
# ("... with 3 updates") are why the body is searched too: the interesting pair
# is not always in the title.
BUMP_RE = re.compile(r"\bfrom\s+v?(\d+)(?:\.\d+)*\s+to\s+v?(\d+)(?:\.\d+)*", re.IGNORECASE)

# Only as many of the body as a changelog pointer needs; the rest is prose.
BODY_SCAN_LIMIT = 2000


class Failure(Exception):
    """Something the owner has to fix. Printed without a traceback."""


def git(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    proc = subprocess.run(["git", *args], capture_output=True, text=True)
    if check and proc.returncode != 0:
        detail = (proc.stderr or proc.stdout).strip()
        raise Failure(f"git {' '.join(args)} failed: {detail}")
    return proc


def git_out(*args: str, check: bool = True) -> str:
    return git(*args, check=check).stdout.strip()


def pin_fetched(source: Source) -> str:
    """Resolve the fetch to a commit id, because `source.ref` does not resolve.

    `git fetch origin <ref>` writes `FETCH_HEAD` and nothing else, so after the
    fetch the only thing that reliably names the proposal is a commit id. Two
    separate traps sit behind that, and both were found on the first live run
    rather than in a reproduction:

      * `pull/<n>/head` gets no local ref at all. GitHub keeps pull-request refs
        under `refs/pull/*`, which falls outside the
        `+refs/heads/*:refs/remotes/origin/*` refspec every clone has, so
        `git log pull/6/head` fails as an ambiguous argument.
      * A *branch* name does not resolve either, and not because of the fetch.
        Git reads `dependabot/github_actions/x` as remote `dependabot`, branch
        `github_actions/x`, so the shorthand misses the remote-tracking ref that
        the fetch did write. `origin/<branch>` would resolve; the bare name
        never does.

    Pinning the id once also closes a real race. Dependabot rewrites its branch
    on every run, so a name re-resolved later could have one commit inspected
    and a different one merged. With the id pinned, what gets verified is
    exactly what gets merged.
    """
    sha = git_out("rev-parse", "FETCH_HEAD^{commit}")
    source.fetched = sha
    return sha


def tip_of(source: Source) -> str:
    """What to read the proposal's tip from: the pinned id, else the name."""
    return source.fetched or source.ref


def gh(*args: str) -> str:
    if shutil.which("gh") is None:
        raise Failure("the GitHub CLI (`gh`) is required to resolve a pull request number")
    proc = subprocess.run(["gh", *args], capture_output=True, text=True)
    if proc.returncode != 0:
        raise Failure(f"gh {' '.join(args)} failed: {(proc.stderr or proc.stdout).strip()}")
    return proc.stdout


class Source:
    """The bot's proposal, however the owner named it."""

    def __init__(self, ref: str, number: int | None, url: str, title: str, body: str, base: str) -> None:
        self.ref = ref
        self.number = number
        self.url = url
        self.title = title
        self.body = body
        self.base = base
        # The commit id the fetch actually produced. `self.ref` is what the
        # owner typed or what the pull request is called, and neither resolves
        # after a fetch — see `pin_fetched`.
        self.fetched: str | None = None


def is_pr_number(value: str) -> bool:
    return value.isdigit()


def resolve_pr(number: int) -> Source:
    """Resolve a pull request number to the ref its head branch lives at."""
    fields = "number,title,url,headRefName,baseRefName,body,author,isCrossRepository"
    data = json.loads(gh("pr", "view", str(number), "--json", fields))

    if data.get("isCrossRepository"):
        raise Failure(
            f"PR #{number} comes from a fork ({data['headRefName']}); "
            "this script only lands Dependabot's in-repository branches"
        )
    if not data.get("author", {}).get("is_bot"):
        raise Failure(
            f"PR #{number} was opened by a human ({data['author']['login']}), not by automation. "
            "Re-authoring someone's pull request into the owner's name is not what this is for."
        )

    # `pull/<n>/head` rather than `headRefName`: Dependabot rewrites its branch
    # on every run, so the branch name is a moving target while the pull-request
    # ref always names what is actually proposed right now.
    return Source(
        ref=f"pull/{number}/head",
        number=number,
        url=data["url"],
        title=data.get("title", ""),
        body=data.get("body") or "",
        base=data.get("baseRefName", "main"),
    )


def resolve_ref(name: str) -> Source:
    """Resolve a branch name to a Source with no pull request attached."""
    remote = git_out("config", "--get", "remote.origin.url")
    slug = ""
    m = re.search(r"github\.com[:/]+([^/]+/[^/.]+)", remote)
    if m:
        slug = m.group(1)
    return Source(
        ref=name,
        number=None,
        url=f"https://github.com/{slug}/tree/{name}" if slug else "",
        title="",
        body="",
        base=default_branch(),
    )


def default_branch() -> str:
    """The branch bumps land on, as `origin` knows it rather than as we assume."""
    proc = git("symbolic-ref", "--quiet", "--short", "refs/remotes/origin/HEAD", check=False)
    if proc.returncode == 0 and proc.stdout.strip():
        return proc.stdout.strip().split("/", 1)[1]
    return "main"


def is_major_bump(source: Source) -> bool:
    """Does this proposal cross a major version boundary?

    A heuristic over Dependabot's own wording, used only to decide whether to
    *ask* for `--read-changelog`. A false negative here costs a reminder; it
    cannot make an unreviewed major bump pass any gate, because no gate reads
    this flag's absence. It is deliberately not load-bearing.
    """
    haystack = f"{source.title}\n{source.body[:BODY_SCAN_LIMIT]}"
    for match in BUMP_RE.finditer(haystack):
        if int(match.group(2)) > int(match.group(1)):
            return True
    return False


def human_identity() -> tuple[str, str]:
    name = git_out("config", "user.name")
    email = git_out("config", "user.email")
    return name, email


def tip_identity(ref: str) -> tuple[str, str]:
    raw = git_out("log", "-1", "--pretty=%an <%ae>", ref)
    return check_attribution.parse_identity(raw)


def preflight(source: Source, base: str) -> None:
    """Refuse everything that would make the result wrong or unreviewable."""
    if git_out("rev-parse", "--git-dir") == "":
        raise Failure("not inside a git repository")

    # Untracked files are not staged and cannot be committed by `git commit`
    # without `-a`, so they cannot contaminate the bump commit -- which is the
    # only reason this guard exists. Blocking on them would be a false positive
    # on the owner's own scratch files. Where an untracked file *does* matter --
    # because the bot's diff creates the same path -- `merge --squash` refuses
    # on its own, loudly.
    if git_out("status", "--porcelain", "--untracked-files=no"):
        raise Failure(
            "the working tree has uncommitted changes to tracked files.\n"
            "  This script stages the bot's diff with `git merge --squash`, so anything\n"
            "  already staged or modified would land inside the bump commit.\n"
            "  Commit, stash, or clean first."
        )

    head, head_email = human_identity()
    if not check_attribution.is_owner(head, head_email):
        raise Failure(
            f"git is configured as '{head} <{head_email}>', which is not the repository owner "
            f"({check_attribution.OWNER_NAME} <{check_attribution.OWNER_EMAIL}>).\n"
            "  The commit this script makes would be rejected by R2. Fix `git config user.name`\n"
            "  and `git config user.email` first -- AGENTS.md pins both."
        )

    bot_name, bot_email = tip_identity(tip_of(source))
    if not check_attribution.is_bot(bot_name, bot_email):
        raise Failure(
            f"the tip of {source.ref} ({tip_of(source)[:9]}) is authored by '{bot_name} <{bot_email}>', "
            "not by automation.\n"
            "  This script re-authors *bot* proposals. Point it at a Dependabot branch or a\n"
            "  dependabot pull request number."
        )

    ahead = git("rev-list", "--count", f"{base}..{tip_of(source)}", check=False)
    if ahead.returncode != 0 or not ahead.stdout.strip():
        raise Failure(
            f"cannot compare {tip_of(source)[:9]} against {base}.\n"
            f"  Is the base branch fetched?\n"
            f"  Try: git fetch origin {source.base}"
        )


def build_message(source: Source, bump_line: str) -> str:
    """A message written here, not one lifted from the bot.

    Two things are deliberately absent: Dependabot's body, which carries its own
    sign-off, and any trailer naming it as an author. What is added is a
    `Refs:` pointer, because the manual route loses the `(#n)` link a
    squash-merged pull request gets for free -- without it the audit trail for
    every dependency bump disappears at the moment the landing path became
    manual. See ADR-0009.
    """
    parts = [
        bump_line or (source.title or f"Apply {source.ref}").strip(),
        "",
        "The diff is Dependabot's. The authorship is not: automation proposes, a",
        "person authors, a person merges. See",
        "docs/adr/0009-automated-dependency-landing-path.md.",
        "",
        "Dependabot's own `Signed-off-by` is not carried over -- it signed work this",
        "commit did not perform.",
    ]
    if source.url:
        parts += ["", f"Refs: {source.url}"]
    return "\n".join(parts)


def sanitize(name: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._/-]+", "-", name).strip("-/")
    # Dependabot's own prefix would otherwise double up in `deps/dependabot/...`.
    if cleaned.startswith("dependabot/"):
        cleaned = cleaned[len("dependabot/") :]
    return cleaned or "dependency-bump"


def adopt_commit_message(source: Source) -> Source:
    """Fill in what only the fetched commit knows.

    A branch named on the command line carries no pull request, so there is no
    title, no body and no PR URL to work from. Both matter: without the title the
    major-bump check below is silently skipped for every invocation that names a
    branch rather than a pull-request number -- which is the more careful
    invocation, so it would be the one losing the guard. Without a URL the
    landed commit gets no `Refs:` pointer, which is the audit trail ADR-0009
    exists to preserve, so a branch-sourced landing would quietly be the
    unauditable one. Dependabot's subject carries the same "from a to b" wording
    its titles do, and its commit is the same object either way.
    """
    if not source.title:
        source.title = git_out("log", "-1", "--pretty=%s", tip_of(source))
        source.body = git_out("log", "-1", "--pretty=%b", tip_of(source))
    remote = git_out("config", "--get", "remote.origin.url", check=False)
    m = re.search(r"github\.com[:/]+([^/]+/[^/.]+?)(?:\.git)?$", remote or "")
    # For a branch-sourced landing, replace the branch URL `resolve_ref` guessed
    # with the immutable commit URL. Both name the proposal, but Dependabot
    # rewrites its branch on every run, so a `tree/<branch>` pointer can change
    # or 404 later -- which makes it a weaker audit trail than the commit it
    # actually landed. A pull-request source keeps its PR URL, which is the
    # link worth keeping there.
    if m and source.number is None:
        source.url = f"https://github.com/{m.group(1)}/commit/{tip_of(source)}"
    elif not source.url and m:
        source.url = f"https://github.com/{m.group(1)}/commit/{tip_of(source)}"
    return source


def run_verification(base: str) -> None:
    """The gate this script exists to run before a push."""
    rev_range = f"{base}..HEAD"
    print(f"\nVerifying {rev_range} with check_attribution.py (as it runs on main)...")
    proc = subprocess.run(
        [sys.executable, str(ATTRIBUTION_CHECK), "--main", rev_range],
        capture_output=True,
        text=True,
    )
    sys.stdout.write(proc.stdout)
    sys.stderr.write(proc.stderr)
    if proc.returncode != 0:
        raise Failure(
            f"check_attribution.py rejected the new commit ({rev_range}).\n"
            "  Nothing has been pushed. Fix the commit -- do not merge around this; the\n"
            "  rule is the point. If the check itself is wrong, that is an ADR, not an\n"
            "  exception."
        )


def main() -> int:
    # Progress goes to stdout, refusals to stderr. Unbuffered stderr against
    # block-buffered stdout means that whenever stdout is a pipe the refusal
    # prints *above* the "Fetching ..." line it belongs under, which reads as
    # though the run failed before it fetched anything. Not load-bearing, but on
    # the first live outing it made a real failure actively misleading.
    sys.stdout.reconfigure(line_buffering=True)

    parser = argparse.ArgumentParser(
        description="Re-author a Dependabot bump as an owner-authored commit, verified locally.",
        epilog="Nothing is pushed. Opening and merging the pull request is yours.",
    )
    parser.add_argument("source", help="a dependabot pull request number, or a branch name")
    parser.add_argument(
        "--branch-name",
        default=None,
        help="local branch to create (default: deps/<pr> or deps/<sanitised ref>)",
    )
    parser.add_argument(
        "--subject",
        default=None,
        help="commit subject; defaults to Dependabot's own pull request title",
    )
    parser.add_argument(
        "--read-changelog",
        action="store_true",
        help="confirm you read the upstream changelog; required when a major bump is detected",
    )
    parser.add_argument("--dry-run", action="store_true", help="report what would happen; create nothing")
    args = parser.parse_args()

    try:
        source = resolve_pr(int(args.source)) if is_pr_number(args.source) else resolve_ref(args.source)
        base = f"origin/{source.base}"
        default_branch_name = args.branch_name or (
            f"deps/{source.number}" if source.number else f"deps/{sanitize(source.ref)}"
        )

        print(f"Fetching {source.ref}...")
        git("fetch", "origin", source.ref)
        if not git("rev-parse", "--verify", "--quiet", "FETCH_HEAD", check=False).returncode == 0:
            raise Failure(f"{source.ref} did not resolve to a commit")
        pin_fetched(source)
        adopt_commit_message(source)

        # After the fetch, not before: for a branch named on the command line the
        # title that reveals a major bump only exists once the commit is here.
        if is_major_bump(source) and not args.read_changelog:
            raise Failure(
                f"'{source.title}' looks like a major version bump.\n"
                "  Read the upstream changelog or release notes first, then re-run with\n"
                "  --read-changelog. The flag is a claim this script cannot check, and it is\n"
                "  here because ADR-0009 keeps a person accountable for the bump -- which is\n"
                "  the whole reason automation is not the author."
            )

        print("Checking preconditions...")
        preflight(source, base)

        if args.dry_run:
            print("\n--dry-run: nothing created, nothing pushed.")
            print(f"  source:  {source.ref} ({'major bump' if is_major_bump(source) else 'no major bump detected'})")
            print(f"  base:    {base}")
            print(f"  branch:  {default_branch_name}")
            print(f"  subject: {args.subject or source.title or '(none)'}")
            print(f"  refs:    {source.url or '(none)'}")
            return 0

        original_branch = git_out("rev-parse", "--abbrev-ref", "HEAD")

        # `git checkout -B` resets an existing branch to the base, which would
        # silently discard whatever was on it. There is no `--force` here on
        # purpose: this script must never be the thing that loses work.
        if git("show-ref", "--verify", "--quiet", f"refs/heads/{default_branch_name}", check=False).returncode == 0:
            raise Failure(
                f"a local branch named '{default_branch_name}' already exists.\n"
                "  Continuing would reset it to the base branch and lose whatever is on it.\n"
                "  Delete or rename it, or pass --branch-name."
            )

        ahead = git("rev-list", "--count", f"{base}..{source.base}", check=False)
        if ahead.returncode == 0 and ahead.stdout.strip() not in ("", "0"):
            raise Failure(
                f"local {source.base} has {ahead.stdout.strip()} commit(s) not on {base}.\n"
                "  The bump is branched from the *fetched* base, so those commits would be\n"
                "  left behind rather than included. Push or rebase them first."
            )

        created = False
        succeeded = False
        try:
            git("checkout", "-B", default_branch_name, base)
            created = True

            # The one step that carries the diff. `--squash` stages the merge
            # result and creates no commit, so authorship is whatever the
            # following `git commit` decides -- and no authorship is inherited.
            # The pinned id, not FETCH_HEAD: the same object preflight checked
            # is the one that gets merged.
            git("merge", "--squash", tip_of(source))

            if not git_out("diff", "--cached", "--name-only"):
                print(
                    f"\nNothing to land: {source.ref} introduces no change against {base}.\n"
                    "  The bump is already in the base branch, or the bot's diff is empty."
                )
                return 0

            print(f"\nStaged by the bot's diff:\n{git_out('diff', '--cached', '--stat')}")

            message = build_message(source, (args.subject or "").strip())
            git("commit", "-q", "-m", message)

            # Re-read what git actually wrote rather than what we asked for.
            written_name, written_email = tip_identity("HEAD")
            if not check_attribution.is_owner(written_name, written_email):
                raise Failure(
                    f"the new commit is authored by '{written_name} <{written_email}>', which is not\n"
                    f"  the owner. Undo with `git reset --hard {base}` and check your git identity."
                )

            # Ask the gate's own question rather than grepping for one bot's name:
            # a substring check for "dependabot" would pass on a security-fix
            # branch signed by github-actions[bot], which is the same claim and
            # the same rule. R1 is exactly this test.
            body = git_out("log", "-1", "--pretty=%B", "HEAD")
            for line in body.splitlines():
                trailer = check_attribution.TRAILER_RE.match(line.strip())
                if not trailer:
                    continue
                name, email = check_attribution.parse_identity(trailer.group(2))
                if check_attribution.is_bot(name, email) or check_attribution.is_ai(name, email):
                    raise Failure(
                        f"the commit body carries '{line.strip()}', naming a bot or an assistant.\n"
                        f"  Undo with `git reset --hard {base}`. The message is built in this\n"
                        "  script, so a leak means that guarantee has broken — please report it."
                    )

            print(f"Committed as {written_name} <{written_email}> on {default_branch_name}")
            run_verification(base)

            print(
                f"""
Next steps -- all of them yours, and none of them done for you:

  1. Read the diff once with your own eyes:
       git show --stat HEAD
  2. Push the branch and open a pull request *from your branch*:
       git push -u origin {default_branch_name}
     Then close Dependabot's pull request. Do not merge the bot's: a squash
     attributes to the pull request's author, so merging {source.ref} would land
     dependabot[bot] on main and fail R2 -- even though the commit is yours now.
     A squash of *your* pull request attributes to you, which is the point.
  3. Merge your pull request through the web UI. The `Refs:` trailer keeps the
     link to the bot's pull request that the squash would have given for free.
"""
            )
            # Left set on purpose: the branch holding the bump is the deliverable,
            # and the instruction above is to push it. Cleaning it up here would
            # delete the work the script was asked to do and then print a `git
            # push` line for a branch that no longer exists.
            succeeded = True
            return 0
        finally:
            # Hand the owner back the branch they were on -- but only on the
            # failure paths. `merge --squash` writes no MERGE_HEAD, so there is
            # nothing for `merge --abort` to unwind; the reset is the cleanup.
            if created and not succeeded:
                git("merge", "--abort", check=False)
                if git_out("status", "--porcelain", "--untracked-files=no"):
                    print(
                        f"\nThe branch is in the state the script left it ({default_branch_name}); "
                        "nothing was pushed.",
                        file=sys.stderr,
                    )
                else:
                    git("checkout", "--quiet", original_branch)
                    git("branch", "-D", default_branch_name)
                    print(f"\nCleaned up the temporary branch; back on {original_branch}.")
    except Failure as exc:
        print(f"\nland-dependency-pr: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())