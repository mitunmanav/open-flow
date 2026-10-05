#!/usr/bin/env python3
"""Verification for land_dependency_pr.py, run on every pull request.

    python3 -m unittest discover -s .github/scripts -p 'test_*.py'

These tests exist because ticket 39's reproduction was not faithful, and the way
it was unfaithful is the reason they are shaped this way.

Ticket 39 ran 46 assertions against "a real bare `origin`" and reported them all
passing. On the first live outing against GitHub the script could not land
anything, in either invocation style: `preflight` reads the proposal's tip with
`git log -1 --pretty='%an <%ae>' <source.ref>`, and after
`git fetch origin <source.ref>` **nothing named `source.ref` resolves**. The fetch
writes `FETCH_HEAD` and no local ref.

A bare-origin fixture cannot catch that, because a hand-built bare repo can be
given whatever refspec the author likes — most naturally one that *does* publish
`refs/pull/*` into the client, which is precisely the world GitHub does not
provide. The reproduction's fixture was not merely a simplification; it asserted
a capability the real remote withholds. The one live PR-number run it did make
was against a **human** pull request, which `resolve_pr` refuses on its very
first check, so it never reached `preflight` at all.

So the fixture below is built to match GitHub rather than to be convenient:

  * the bare origin carries the bot's proposal under `refs/pull/<n>/head`, which
    is where GitHub puts it;
  * the client's refspec is `+refs/heads/*:refs/remotes/origin/*` — the default
    every clone has, and the one under which `refs/pull/*` is unreachable;
  * **no** `refs/pull/*` refspec is added, because adding one is the bug being
    papered over.

Two assertions then hold this in place. One checks that the *name* genuinely
does not resolve in that client — so the fixture cannot quietly become
GitHub-compatible again and stop testing anything. The other drives the whole
landing path and asks the gate its question.
"""

from __future__ import annotations

import contextlib
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
sys.dont_write_bytecode = True
sys.path.insert(0, str(REPO / ".github" / "scripts"))

import check_attribution  # noqa: E402
import land_dependency_pr as L  # noqa: E402

# The identity AGENTS.md pins, and the one the script insists on.
OWNER = ("Mitun Manav G Y", "238927830+mitunmanav@users.noreply.github.com")

# Dependabot's real authorship, read off a live pull request on 2026-10-05.
# Both halves matter and they differ: the *author* is
# <49699333+dependabot[bot]@users.noreply.github.com>, while the sign-off in the
# body is <support@github.com>. A gate that only recognised the first would pass
# the real commit.
BOT = ("dependabot[bot]", "49699333+dependabot[bot]@users.noreply.github.com")
BOT_SIGNOFF = "Signed-off-by: dependabot[bot] <support@github.com>"

# The real body shape, including the `updated-dependencies` block and the `...`
# separator, and with no trailing newline after the sign-off -- which is how
# GitHub wrote all five live pull requests.
BOT_BODY = (
    "Bumps [example/action](https://github.com/example/action) from 4.2.1 to 4.3.0.\n"
    "- [Release notes](https://github.com/example/action/releases)\n"
    "\n"
    "---\n"
    "updated-dependencies:\n"
    "- dependency-name: example/action\n"
    "  dependency-version: '4.3.0'\n"
    "  dependency-type: direct:production\n"
    "  update-type: version-update:semver-minor\n"
    "...\n"
    "\n"
    + BOT_SIGNOFF
)


def run_git(cwd: Path, *args: str, env: dict | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["git", *args], cwd=cwd, capture_output=True, text=True, env=env, check=True
    )


@contextlib.contextmanager
def in_dir(path: Path):
    """Run the script's own `git()` helpers inside a particular clone.

    `land_dependency_pr.git` shells out in the process working directory, which
    is how it behaves for the owner on the command line. The tests drive the real
    functions rather than reimplementing them, so they have to move the process
    too — otherwise they would quietly measure this repository instead of the
    fixture.
    """
    previous = Path.cwd()
    os.chdir(path)
    try:
        yield path
    finally:
        os.chdir(previous)


class GithubShapedFixture:
    """A bare origin and a clone configured the way GitHub configures one."""

    def __init__(self, root: Path, branch: str = "dependabot/example/action-4.3.0") -> None:
        self.origin = root / "origin.git"
        self.work = root / "work"
        self.branch = branch
        self.pr = 7
        self.github_url = "https://github.com/example/example"
        self._build()

    def _build(self) -> None:
        run_git(Path.cwd(), "init", "--quiet", "--bare", str(self.origin))

        # A staging repo holds the bot's commit, then pushes it to the ref
        # GitHub actually serves a pull request from.
        stage = self.origin.parent / "stage"
        stage.mkdir()
        run_git(stage, "init", "--quiet", "-b", "main")
        run_git(stage, "config", "user.name", OWNER[0])
        run_git(stage, "config", "user.email", OWNER[1])
        (stage / "README.md").write_text("base\n")
        run_git(stage, "add", ".")
        run_git(stage, "commit", "--quiet", "-m", "base")
        self.base_sha = run_git(stage, "rev-parse", "HEAD").stdout.strip()

        run_git(stage, "checkout", "--quiet", "-b", self.branch)
        (stage / "README.md").write_text("bumped\n")
        run_git(stage, "add", ".")
        # Author and committer set separately: Dependabot authors as the bot and
        # is *committed* by GitHub, and the gate treats that committer as exempt
        # and the author as a violation.
        run_git(stage, "commit", "--quiet", "-m", BOT_BODY)
        run_git(
            stage,
            "-c", "user.name=GitHub", "-c", "user.email=noreply@github.com",
            "commit", "--quiet", "--amend", "--no-edit", "--author", f"{BOT[0]} <{BOT[1]}>",
        )
        self.bot_sha = run_git(stage, "rev-parse", "HEAD").stdout.strip()

        run_git(stage, "remote", "add", "origin", str(self.origin))
        # Dependabot's branch is a real branch *and* the pull request serves
        # refs/pull/<n>/head. Both exist on GitHub, and the script's two
        # invocation styles reach the same commit by different routes.
        run_git(stage, "push", "--quiet", "origin", f"refs/heads/{self.branch}:refs/heads/{self.branch}")
        run_git(stage, "push", "--quiet", "origin", f"{self.branch}:refs/pull/{self.pr}/head")
        run_git(stage, "push", "--quiet", "origin", "main")

        # The client: a plain clone, which gets GitHub's default refspec and
        # nothing else. This is the part ticket 39's fixture got wrong.
        #
        # `insteadOf` rewrites the github-shaped URL to the local bare repo, so
        # `remote.origin.url` reads as a GitHub URL while fetches still land
        # locally. That matters beyond convenience: the landing procedure builds
        # its `Refs:` pointer by matching `remote.origin.url` against github.com,
        # so a fixture whose origin is a filesystem path would silently produce
        # a commit with no audit trail and the test would never notice.
        run_git(
            Path.cwd(),
            "-c", f"url.{self.origin}.insteadOf={self.github_url}",
            "clone", "--quiet", self.github_url, str(self.work),
        )
        run_git(self.work, "config", f"url.{self.origin}.insteadOf", self.github_url)
        run_git(self.work, "config", "user.name", OWNER[0])
        run_git(self.work, "config", "user.email", OWNER[1])

    def fresh_clone(self, path: Path) -> Path:
        """Another client, GitHub-shaped identically, for tests that need one."""
        run_git(
            Path.cwd(),
            "-c", f"url.{self.origin}.insteadOf={self.github_url}",
            "clone", "--quiet", self.github_url, str(path),
        )
        run_git(path, "config", f"url.{self.origin}.insteadOf", self.github_url)
        run_git(path, "config", "user.name", OWNER[0])
        run_git(path, "config", "user.email", OWNER[1])
        return path


class LandingProcedureTest(unittest.TestCase):
    """One fixture per test; these assertions depend on a pristine ref state."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.tmp = Path(tempfile.mkdtemp(prefix="land-dep-test-"))
        cls.fx = GithubShapedFixture(cls.tmp)

    @classmethod
    def tearDownClass(cls) -> None:
        shutil.rmtree(cls.tmp, ignore_errors=True)

    # -- the fixture itself, asserted so it cannot rot into a happy accident --

    def test_fixture_does_not_publish_pull_refs(self) -> None:
        """The client's refspec must not make `pull/<n>/head` resolvable.

        If someone "fixes" a failing test by adding a `refs/pull/*` refspec, the
        fixture stops resembling GitHub and every other assertion in this file
        becomes vacuous. So the un-resolvability is itself the assertion.
        """
        refspecs = run_git(self.fx.work, "config", "--get-all", "remote.origin.fetch").stdout
        self.assertIn("+refs/heads/*:refs/remotes/origin/*", refspecs)
        self.assertNotIn("refs/pull", refspecs)

        run_git(self.fx.work, "fetch", "--quiet", "origin", f"pull/{self.fx.pr}/head")
        for name in (f"pull/{self.fx.pr}/head", f"refs/pull/{self.fx.pr}/head"):
            probe = subprocess.run(
                ["git", "rev-parse", "--verify", "--quiet", name],
                cwd=self.fx.work, capture_output=True, text=True,
            )
            self.assertNotEqual(
                probe.returncode, 0,
                f"{name} resolved, so this fixture is no longer GitHub-shaped",
            )
        self.assertTrue(
            subprocess.run(
                ["git", "rev-parse", "--verify", "--quiet", "FETCH_HEAD"],
                cwd=self.fx.work, capture_output=True,
            ).returncode == 0,
            "FETCH_HEAD must resolve; it is the only thing the fetch leaves behind",
        )

    def test_resolving_the_source_by_name_is_the_bug(self) -> None:
        """The original defect, named executably rather than by its symptom.

        `preflight` read the proposal's tip with
        `tip_identity(source.ref)` — `git log -1 --pretty='%an <%ae>' pull/<n>/head`
        — and on GitHub that is an ambiguous argument, so the script could not
        land a pull request at all. Ticket 39's 46 assertions never reached this
        line on a real pull request, because the only live PR-number run it made
        was against a human PR, which `resolve_pr` refuses before `preflight`.

        Asserted directly so that the test fails on the original code for the
        original reason, rather than merely because a new helper went missing.
        """
        run_git(self.fx.work, "fetch", "--quiet", "origin", f"pull/{self.fx.pr}/head")
        with in_dir(self.fx.work):
            source = L.Source(
                ref=f"pull/{self.fx.pr}/head", number=self.fx.pr,
                url="", title="", body="", base="main",
            )
            with self.assertRaises(L.Failure):
                L.tip_identity(source.ref)
            # The same read succeeds through the pinned id, which is the fix.
            L.pin_fetched(source)
            self.assertEqual(L.tip_identity(L.tip_of(source)), BOT)

    def test_branch_shorthand_does_not_resolve_either(self) -> None:
        """The branch-named invocation fails for a second, different reason.

        Not the fetch: git resolves the shorthand `a/b` as remote `a`, branch
        `b`, so it misses the `origin/<branch>` ref the fetch *did* write. Only
        `origin/<branch>` resolves. Worth its own assertion because the fix for
        it is not the same as the fix for `pull/<n>/head`.
        """
        run_git(self.fx.work, "fetch", "--quiet", "origin", self.fx.branch)
        probe = subprocess.run(
            ["git", "rev-parse", "--verify", "--quiet", self.fx.branch],
            cwd=self.fx.work, capture_output=True, text=True,
        )
        self.assertNotEqual(probe.returncode, 0, f"{self.fx.branch} resolved as a shorthand")
        self.assertTrue(
            subprocess.run(
                ["git", "rev-parse", "--verify", "--quiet", f"origin/{self.fx.branch}"],
                cwd=self.fx.work, capture_output=True,
            ).returncode == 0,
            "the remote-tracking ref should exist; if not, the fixture is wrong",
        )

    # -- the fix --

    def test_pin_fetched_resolves_a_name_that_does_not_exist_locally(self) -> None:
        """`pin_fetched` is the whole fix: the id resolves where the name cannot."""
        run_git(self.fx.work, "fetch", "--quiet", "origin", f"pull/{self.fx.pr}/head")
        with in_dir(self.fx.work):
            source = L.Source(
                ref=f"pull/{self.fx.pr}/head", number=self.fx.pr,
                url="", title="", body="", base="main",
            )
            self.assertIsNone(source.fetched)
            sha = L.pin_fetched(source)
            self.assertEqual(sha, self.fx.bot_sha)
            self.assertEqual(L.tip_of(source), sha)
            # And it is the bot's commit, read through the id.
            name, email = L.tip_identity(L.tip_of(source))
            self.assertEqual((name, email), BOT)

    def test_landing_carries_the_diff_and_the_gate_passes(self) -> None:
        """The whole path, against GitHub's refspec, with the gate asked.

        Runs `main()` in-process on a fresh clone of the fixture so the script's
        real code runs — not a transcription of it — then asks
        `check_attribution.py --main` the question CI asks.
        """
        work = Path(tempfile.mkdtemp(prefix="land-dep-run-", dir=self.tmp))
        self.fx.fresh_clone(work)

        # A branch-named source, so `gh` is never needed: the PR-number path
        # needs a live pull request and the branch path exercises the identical
        # code from `resolve_ref` onward.
        with in_dir(work):
            source = L.resolve_ref(self.fx.branch)
            self.assertEqual(source.base, "main")
            L.git("fetch", "origin", source.ref)
            L.pin_fetched(source)
            L.adopt_commit_message(source)

            # Not a major bump, so `--read-changelog` is not owed. Asserted
            # rather than assumed: the accountability gate is the one thing here
            # that a test must not quietly stand in for.
            self.assertFalse(L.is_major_bump(source), "4.2.1 -> 4.3.0 is a minor bump")

            base = f"origin/{source.base}"
            L.preflight(source, base)

            branch = "deps/example"
            L.git("checkout", "-B", branch, base)
            L.git("merge", "--squash", L.tip_of(source))
            self.assertIn("README.md", L.git_out("diff", "--cached", "--name-only"))

            L.git("commit", "-q", "-m", L.build_message(source, ""))

            name, email = L.tip_identity("HEAD")
            self.assertEqual((name, email), OWNER, "the landed commit must be owner-authored")

            body = L.git_out("log", "-1", "--pretty=%B", "HEAD")
            self.assertNotIn(BOT_SIGNOFF, body, "the bot's sign-off must not be carried over")
            self.assertIn("Refs: ", body, "the audit-trail pointer ADR-0009 requires")
            for line in body.splitlines():
                trailer = check_attribution.TRAILER_RE.match(line.strip())
                if trailer:
                    t_name, t_email = check_attribution.parse_identity(trailer.group(2))
                    self.assertFalse(
                        check_attribution.is_bot(t_name, t_email) or check_attribution.is_ai(t_name, t_email),
                        f"R1: trailer {line.strip()!r} names a bot or an assistant",
                    )

            # The diff really is the bot's.
            landed = run_git(work, "show", "HEAD:README.md").stdout
            self.assertEqual(landed, "bumped\n")

            # The gate CI runs, asked about the range that would land on main.
            gate = subprocess.run(
                [sys.executable, str(REPO / ".github" / "scripts" / "check_attribution.py"),
                 "--main", f"{base}..HEAD"],
                cwd=work, capture_output=True, text=True,
            )
            self.assertEqual(gate.returncode, 0, f"gate rejected the landed commit:\n{gate.stderr}")
            self.assertIn("clean", gate.stdout)

    def test_branch_sourced_refs_points_at_the_commit_not_the_branch(self) -> None:
        """A branch-sourced landing must not get a moving pointer.

        `resolve_ref` guesses a `tree/<branch>` URL. Dependabot rewrites its
        branch on every run, so that pointer can change or 404 after the landing,
        which makes it a weaker audit trail than the commit that actually landed
        — and ADR-0009's whole argument for keeping a `Refs:` pointer is that it
        is the only surviving link once the free `(#n)` is gone. Found on the
        live run: the branch-sourced measurement printed
        `Refs: .../tree/dependabot/github_actions/actions/checkout-7`.
        """
        work = self.fx.fresh_clone(Path(tempfile.mkdtemp(prefix="land-dep-refs-", dir=self.tmp)))
        with in_dir(work):
            source = L.resolve_ref(self.fx.branch)
            self.assertTrue(
                source.url.endswith(f"/tree/{self.fx.branch}"),
                "precondition: resolve_ref guesses a branch URL for this to improve on",
            )
            L.git("fetch", "origin", source.ref)
            L.pin_fetched(source)
            L.adopt_commit_message(source)
            self.assertEqual(source.number, None, "branch-sourced, not pull-request-sourced")
            self.assertEqual(
                source.url, f"{self.fx.github_url}/commit/{self.fx.bot_sha}",
                "Refs must name the immutable commit, not the rewritten branch",
            )

    def test_pull_request_sourced_refs_keeps_the_pull_request_url(self) -> None:
        """The opposite direction, so the fix above cannot over-reach.

        A pull-request source has a better link than its commit: the pull
        request is where the discussion, the changelog and the close all live.
        Rewriting it to a commit URL would trade a durable audit trail for a
        durable *snapshot*, so the PR URL is the one that must survive.
        """
        with in_dir(self.fx.work):
            run_git(self.fx.work, "fetch", "--quiet", "origin", f"pull/{self.fx.pr}/head")
            source = L.Source(
                ref=f"pull/{self.fx.pr}/head", number=self.fx.pr,
                url=f"{self.fx.github_url}/pull/{self.fx.pr}", title="", body="", base="main",
            )
            L.pin_fetched(source)
            L.adopt_commit_message(source)
            self.assertEqual(source.url, f"{self.fx.github_url}/pull/{self.fx.pr}")

    # -- and that the same gate can still fail --

    def test_gate_rejects_the_bot_commit_this_fixture_would_have_merged(self) -> None:
        """A gate shown only passing has measured nothing.

        The very commit the landing path is built to rescue must fail the gate
        on its own. This asserts all three rules fire on Dependabot's real
        authorship and real sign-off — R1 on the `<support@github.com>` sign-off,
        R2 and R3 on the `<49699333+dependabot[bot]@...>` author.
        """
        work = Path(tempfile.mkdtemp(prefix="land-dep-bot-", dir=self.tmp))
        run_git(Path.cwd(), "clone", "--quiet", str(self.fx.origin), str(work))
        run_git(work, "fetch", "--quiet", "origin", f"pull/{self.fx.pr}/head")

        gate = subprocess.run(
            [sys.executable, str(REPO / ".github" / "scripts" / "check_attribution.py"),
             "--main", f"{self.fx.base_sha}..{self.fx.bot_sha}"],
            cwd=work, capture_output=True, text=True,
        )
        self.assertEqual(gate.returncode, 1, "the bot's commit must fail the gate")
        for rule in ("R1", "R2", "R3"):
            self.assertIn(rule, gate.stderr, f"{rule} did not fire on the real bot commit")
        self.assertIn(BOT_SIGNOFF, gate.stderr, "R1 must fire on the real sign-off")

    # -- the refusals, which are the design --

    def test_refuses_a_source_that_is_not_automation_authored(self) -> None:
        """A collaborator's commit must never be laundered into the owner's name."""
        work = Path(tempfile.mkdtemp(prefix="land-dep-human-", dir=self.tmp))
        self.fx.fresh_clone(work)

        human = work / "README.md"
        human.write_text("a collaborator's work\n")
        run_git(work, "checkout", "--quiet", "-B", "collaborator")
        run_git(work, "add", ".")
        run_git(work, "commit", "--quiet", "-m", "collaborator commit")
        run_git(work, "push", "--quiet", "origin", "collaborator")

        source = L.resolve_ref("collaborator")
        with in_dir(work):
            L.git("fetch", "origin", source.ref)
            L.pin_fetched(source)
            with self.assertRaises(L.Failure) as caught:
                L.preflight(source, "origin/main")
        self.assertIn("not by automation", str(caught.exception))

    def test_refuses_a_non_owner_git_identity(self) -> None:
        """The commit would be rejected by R2, so the script refuses to make it."""
        work = Path(tempfile.mkdtemp(prefix="land-dep-ident-", dir=self.tmp))
        run_git(Path.cwd(), "clone", "--quiet", str(self.fx.origin), str(work))
        run_git(work, "config", "user.name", "Someone Else")
        run_git(work, "config", "user.email", "someone@example.invalid")

        source = L.resolve_ref(self.fx.branch)
        with in_dir(work):
            L.git("fetch", "origin", source.ref)
            L.pin_fetched(source)
            with self.assertRaises(L.Failure) as caught:
                L.preflight(source, "origin/main")
        self.assertIn("not the repository owner", str(caught.exception))


if __name__ == "__main__":
    unittest.main()