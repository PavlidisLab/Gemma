# Maven Release / Version-Management Audit

Phase 3 build recce. Reviewed on top of `phase2-acl-migrate` HEAD
`08e760bdaf`. Doc-only — no version edits, no `mvn` invocations.

## 1. Current release workflow (inferred)

Gemma uses a **manual git-flow + hand-edited version bumps** model.
No release plugin, no derived versioning, no scripted bump helper.

What the git history shows for every released version (e.g. 1.32.0
through 1.32.6, 24+ hotfixes on the 1.31.x line):

1. Work proceeds on `development` with a `-SNAPSHOT` version.
2. A `release-<x.y.z>` branch is cut. The release manager hand-edits
   the `<version>` in the **root `pom.xml` and each of the four
   module poms** (`gemma-core`, `gemma-cli`, `gemma-rest`,
   `gemma-web`) to strip the `-SNAPSHOT` suffix. The commit message
   is invariably `Update versions for hotfix` (or `Update for next
   development version` going the other way).
3. The release branch merges to `master` (the production branch).
   The merge commit is what gets tagged (e.g. tag `1.32.6` on
   `c954aef545` "Merge branch 'hotfix-1.32.6'").
4. A subsequent commit on `development` bumps versions to the
   next `-SNAPSHOT` (e.g. `b15f4019d4` "Update for next development
   version" → `1.32.7-SNAPSHOT`).
5. Hotfixes use the same pattern but branch from `master` as
   `hotfix-<x.y.z>` and merge back to both `master` and the active
   development line.

Each version-bump commit is the canonical 5-file change pattern:

```
gemma-cli/pom.xml  | 2 +-
gemma-core/pom.xml | 2 +-
gemma-rest/pom.xml | 2 +-
gemma-web/pom.xml  | 2 +-
pom.xml            | 2 +-
```

The Jenkinsfile (`.jenkins/Jenkinsfile`) reads the version via
`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
and *validates* branch ↔ version-suffix consistency (e.g. a
`master` build must NOT end in `-SNAPSHOT`; a `release-*` /
`hotfix-*` build MUST NOT end in `-SNAPSHOT`; everything else MUST
end in `-SNAPSHOT`). It does NOT itself perform any version bumps;
Jenkins only consumes the version chosen by hand.

There is no `RELEASING.md`, no `release.sh`, no `Makefile` /
`justfile` target for releases. (`justfile` exists but is for
test orchestration.)

## 2. Plugins / tooling in use

| Plugin | Where | Version | Purpose |
|---|---|---|---|
| `versions-maven-plugin` | root `pom.xml:782` | 2.21.0 | Dependency-update **reporting only** (`mvn versions:display-dependency-updates`). Config sets `processDependencyManagement=false` and a long `dependencyExcludes` list, which is consistent with "I run this to see what's outdated, I don't run it to write changes back." Never invoked from `Jenkinsfile`. |
| `git-commit-id-maven-plugin` | `gemma-core/pom.xml:15` | 9.0.2 | Stamps `${git.commit.id}` into the Gemma-Core jar manifest as `Gemma-Build-GitHash` alongside `Gemma-Version=${project.version}` and `Gemma-Build-Timestamp`. Runs in `initialize` phase, `<includeOnlyProperties>git.commit.id</includeOnlyProperties>`, `useNativeGit=true`, `offline=true`. |
| `maven-release-plugin` | **NOT PRESENT** anywhere in any pom | — | The "two-phase release:prepare + release:perform" Maven flow is intentionally *not* used. |
| `nexus-staging-maven-plugin` | **NOT PRESENT** | — | No Central / staging deploy. Deploys are to a UBC-hosted Maven repo via `mvn deploy` and to staging Tomcats via shell scripts (`gemma-web/deploy.sh`, `gemma-cli/deploy.sh`). |
| `jgitver` / `axion-release` | **NOT PRESENT** | — | No git-derived version strategy. |

The parent POM is `ubc.pavlab:pavlab-starter-parent:1.2.29`, a
lab-managed parent that contributes opinions for `maven-jar-plugin`
/ `maven-site-plugin` / etc. It does *not* contribute a release
plugin either (per the absence in the reactor).

## 3. Release cadence (inferred from git tags)

Most recent 15 tags + dates:

```
1.32.6  2026-02-19
1.32.5  2026-01-07
1.32.4  2025-10-28
1.32.3  2025-08-26
1.32.2  2025-08-05
1.32.1  2025-06-20
1.32.0  2025-05-16   <- minor bump (release-1.32.0)
1.31.13 2025-03-27
1.31.12 2024-10-18
1.31.11 2024-09-20
1.31.10 2024-08-06
1.31.9  2024-07-08
1.31.8  2024-06-25
1.31.7  2024-06-05
1.31.6  2024-05-16
```

Pattern:
- ~1 minor (`x.y.0`) per ~12 months (`1.31.0` → `1.32.0` ≈ 1 yr).
- 6–13 patch/hotfix releases between minors.
- Hotfix cadence: rough average **every 4–6 weeks**.
- Versioning is straight semver-ish `MAJOR.MINOR.PATCH` with no
  pre-release / RC suffixes outside of `-SNAPSHOT`.

This makes the per-release ceremony (5 file edits + 1 commit) cheap
in absolute terms but **repeated 7-10 times a year**, every time by
hand, with two opportunities to forget a module (the `release` bump
*and* the `next development version` bump).

## 4. Pain points

Observable pain points from the setup as it stands today:

1. **Bump-by-hand across 5 files.** A miss leaves one module on a
   stale parent-pointer; the build still works (`mvn` resolves
   transitively from root), but the reactor is inconsistent. No
   guard rail prevents this; the Jenkinsfile only checks the root
   project's `project.version` matches the branch.
2. **Two commits per release, both with generic messages.** Every
   hotfix produces `Update versions for hotfix` followed (post-merge
   on development) by `Update for next development version`. There
   is no per-release changelog; the *only* per-release human-readable
   notes for the REST module live in
   `gemma-rest/src/main/resources/restapidocs/CHANGELOG.md`. Whole
   project release notes are absent.
3. **No tag automation.** Tags are created by hand on the merge
   commit. There is no guard that `git tag 1.32.6` matches the
   `<version>` in `pom.xml` at that commit. (In practice they have
   matched, but enforcement is social.)
4. **No `-SNAPSHOT` self-deploy snapshots.** Jenkins `mvn deploy`
   pushes both release and snapshot artifacts on every successful
   build of a tracked branch; this works for the lab-internal Maven
   repo but means snapshot deploys are not versioned by build
   number (e.g. no `1.32.7-SNAPSHOT-20260518.143012-3` discipline
   beyond what Maven does by default).
5. **`git-commit-id-maven-plugin` is one major behind.** Stuck on
   `9.0.2`; `10.0.0` available. Confirmed deferred in the May 2026
   Maven modernization audit (commit `bbf5210bcf`, summarised in
   the original branch `worktree-maven-modernize`) because 10.x
   changed `<includeOnlyProperties>` to a permissive matcher and
   moved configuration keys.

None of these are *crises*. They are the cumulative friction of a
process that has worked fine for a small team for years.

## 5. Modernization recommendations

**Headline: no change recommended for Phase 3.** The current
workflow is boring, transparent, and reliable. Replacing it has a
real cost (parent POM coordination, Jenkinsfile rework, retraining
both human committers) and the benefits are modest at this cadence
(7-10 releases/year). Phase 3 has bigger fish (Spring 6.2, Hibernate
6.6, Flyway prod baseline). Don't burn cycles on release tooling.

That said, the options worth knowing about, ranked by fit:

### A. Status quo + one defensive add-on (RECOMMENDED if anything)

Add a single `maven-enforcer-plugin` rule (the plugin is already in
the parent POM's `dependencyConvergence` config) to require that
every child module's `<parent><version>` matches the reactor root's
`<version>`. The rule already exists in upstream enforcer
(`requireSameVersions` / a custom rule). This eliminates pain
point #1 (the "forgot one module" case) for zero ceremony cost: any
botched bump fails at `mvn validate`.

Add a short `RELEASING.md` documenting the actual ritual (which is
currently tribal knowledge). Half a page. Mention the two commit
messages, which branches to merge where, and the tag-on-merge step.

That's it. No plugin churn.

### B. `maven-release-plugin` (NOT RECOMMENDED)

The traditional answer. Cons that matter here:
- Two-phase `release:prepare` + `release:perform` does its own
  commit-and-tag dance; it will not coexist cleanly with the
  current git-flow `release-*` / `hotfix-*` branch convention that
  the Jenkinsfile already keys off of.
- Brittle in reactors with `<scm>` declarations that need to
  resolve at release time. (Gemma has one in the root pom.)
- Long history of friction reports; mostly considered legacy now.
- Replaces 2 obvious commits with 4 less-obvious commits.

If the team were starting fresh on a single-module project, fine.
For Gemma in 2026, no.

### C. `jgitver` / `axion-release` (NOT RECOMMENDED — yet)

Derive the version from git tags. Zero pom edits ever. Pros:
- The `pom.xml` `<version>` becomes a placeholder (e.g. `0`); the
  reactor version comes from `git describe`.
- Tags become the source of truth.

Cons that matter here:
- Requires every IDE-loaded build, every developer, and the
  Jenkinsfile to have the plugin's `core extension` registered (an
  `.mvn/extensions.xml` file). Subtle; a developer cloning without
  that file gets a broken version string.
- Conflicts with the existing branch-suffix-aware Jenkinsfile
  validation (which expects `-SNAPSHOT` literals).
- The `<scm>` and `<distributionManagement>` URLs the lab Maven repo
  serves are version-named; jgitver-style versions need a strategy
  rule to keep those stable.
- Real benefit only for projects releasing dozens of times a month.
  Gemma releases 7-10 times a year.

Revisit only if release cadence increases 5×.

### D. `versions-maven-plugin` invoked for bumps (NEUTRAL)

`mvn versions:set -DnewVersion=1.32.7` (already configured in
the root pom for dependency-update reporting; same plugin can
write versions). One command replaces the 5 manual edits. A
two-line shell wrapper (`scripts/bump-version.sh`) would make it
ergonomic.

Risk: low. Cost: low. Benefit: small. Fold this into option (A)
if motivated — `RELEASING.md` says "run `mvn versions:set -DnewVersion=X.Y.Z`
followed by `mvn versions:commit`" and you're done.

This is the only changeover I'd *consider* doing inside Phase 3,
and only as a 30-minute tightening alongside the enforcer rule.

## 6. `git-commit-id-maven-plugin` 9 → 10

Status: deferred in the May 2026 Maven modernization audit (commit
`bbf5210bcf`, body: *"10.x changed `<includeOnlyProperties>` to a
more permissive matcher and moved several configuration keys"*).

Current config (`gemma-core/pom.xml:15–32`):

```xml
<configuration>
    <offline>true</offline>
    <useNativeGit>true</useNativeGit>
    <includeOnlyProperties>git.commit.id</includeOnlyProperties>
</configuration>
```

The 9 → 10 changes that touch this config:
- `<includeOnlyProperties>` switched from exact-match to regex. The
  value `git.commit.id` happens to still work as a regex literal,
  but the safer form is the regex `^git\.commit\.id$`.
- 10.x removed the deprecated `runOnlyOnce` shortcut and
  re-organised a handful of less-used config keys (none of which
  Gemma uses).
- Java baseline lifted to 11 (Gemma is on 17, no issue).

**Recommendation: bump in Phase 3.** It is a 1-line config change
(`<includeOnlyProperties>^git\.commit\.id$</includeOnlyProperties>`)
plus the version bump. Plenty cheap, and consistent with the rest
of the Phase 3 plugin-modernization sweep. Worth pursuing now while
the build is being touched anyway.

Not worth doing standalone after Phase 3 ships; would be busywork
then.

## 7. Open questions for Paul

1. **Pavlab-starter-parent 1.3?** RENOVATIONS.md and the root POM
   comment hint at an in-flight 1.3 of the parent. If 1.3 changes
   the release story (e.g. inherits a release plugin), this audit
   needs revisiting. If 1.3 is just version bumps, no change.
2. **Lab-internal Maven repo retention?** The Jenkinsfile's
   `mvn deploy` step pushes every successful tracked-branch build,
   yielding snapshot artifact accumulation. If the repo is filling
   up, snapshot pruning is a separate concern from versioning but
   worth flagging.
3. **Release notes appetite?** Project-wide changelog (vs the
   REST-only `gemma-rest/src/main/resources/restapidocs/CHANGELOG.md`)
   is a process change, not a tooling change, but it would compose
   well with option (A). Adopting Keep-a-Changelog discipline
   wouldn't require any plugin.
4. **Hotfix vs minor decision criteria?** Implicit in the
   `release-1.32.0` vs `hotfix-1.32.x` branch convention but not
   documented; would land naturally in the proposed `RELEASING.md`.

## Summary

| Question | Answer |
|---|---|
| What manages versions today? | Hand edits across 5 pom files, two-commits-per-release, on git-flow `release-*` / `hotfix-*` branches. |
| Is a release plugin in use? | No. |
| Is a derived-version tool in use? | No. |
| Phase 3 recommendation | Status quo + one `requireSameVersions` enforcer rule + a half-page `RELEASING.md`. |
| `git-commit-id` 9 → 10 | Worth doing now as part of Phase 3 plugin sweep (1-line config tweak). |
| Replacing the whole flow | Not worth it at current release cadence. |
