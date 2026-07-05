# Releasing Gemma

Gemma uses a manual **git-flow + hand-edited version bumps** model.
No `maven-release-plugin`, no `jgitver`, no scripted helper. The
Jenkinsfile validates branch/version-suffix consistency but does
**not** itself perform any bump or tag.

Background and rationale: `MAVEN_RELEASE_AUDIT.md`. Cadence is
7-10 releases/year (~1 minor and 6-13 patches between minors).

## The 5 POMs

Every version bump touches the same five files. They must stay in
lockstep:

```
pom.xml            (reactor root)
gemma-core/pom.xml
gemma-cli/pom.xml
gemma-rest/pom.xml
gemma-web/pom.xml
```

The `maven-enforcer-plugin` rule `reactorModuleConvergence` (in the
root `pom.xml`) will fail `mvn validate` if you miss one. Trust the
rule.

## Release ritual (minor or patch)

1. From `development` (for minor `x.y.0` or patch off the active
   line) or `master` (for hotfix off a shipped release), cut a
   branch:
   - `release-1.32.7` for a planned release
   - `hotfix-1.32.7` for an urgent fix off `master`

2. Bump versions in all 5 POMs: strip `-SNAPSHOT`. Either by hand
   (find/replace on `<version>X.Y.Z-SNAPSHOT</version>`) or via:
   ```
   mvn versions:set -DnewVersion=1.32.7 -DgenerateBackupPoms=false
   ```

3. Commit with message `Update versions for release` (or
   `Update versions for hotfix`).

4. Pre-release verification (JDK 21 corretto):
   ```
   JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn verify
   ```
   This runs the full unit + integration test suite. Do not skip.

5. Merge the release/hotfix branch to `master`. The merge commit
   is what gets tagged: `git tag 1.32.7 <merge-sha>` then
   `git push origin 1.32.7`.

6. Back on `development` (or the active support line for hotfixes),
   merge the release branch in, then bump all 5 POMs to the next
   `-SNAPSHOT`:
   ```
   mvn versions:set -DnewVersion=1.32.8-SNAPSHOT -DgenerateBackupPoms=false
   ```
   Commit as `Update for next development version`.

## Jenkinsfile branch-suffix gating

`.jenkins/Jenkinsfile` enforces (lines 53-80):

| Branch                | Version suffix       | If violated      |
|-----------------------|----------------------|------------------|
| `master`              | must NOT be SNAPSHOT | build errors out |
| `support-*`           | must NOT be SNAPSHOT | build errors out |
| `release-*`, `hotfix-*` | must be SNAPSHOT (this is the staging suffix; the suffix is stripped right before the merge to master) | build errors out |
| `development`         | must be SNAPSHOT     | build errors out |

The actual logic: the staging branches (`release-*` / `hotfix-*`)
build with `-SNAPSHOT` until the very last commit on the branch
strips the suffix, then merge to `master` (which has no `-SNAPSHOT`).
Mirror this in your bump commit order.

## Common gotchas

- **Forgetting a module POM.** Pre-Phase-3 this was the #1 risk.
  `reactorModuleConvergence` catches it now; if your build fails
  with "The reactor contains different versions", grep the 5 POMs
  for `<version>` and find the drift.
- **Tag on the wrong commit.** The tag goes on the merge commit
  into `master`, not on the last commit of the release branch.
- **Forgetting the next-dev-version bump.** Easy to merge release
  back to development and forget to flip back to `-SNAPSHOT`.
  Jenkins will error out on the next `development` build.
- **REST API changelog drift.** The only per-release human-readable
  notes live in `gemma-rest/src/main/resources/restapidocs/CHANGELOG.md`.
  Update before the release branch is merged.

## What we explicitly do NOT use

- `maven-release-plugin` (two-phase prepare/perform) -- conflicts
  with the existing git-flow conventions.
- `jgitver` / `axion-release` (git-derived versioning) -- not worth
  the IDE/extension setup cost at our cadence.
- `nexus-staging-maven-plugin` -- no Maven Central deploy; the lab
  Maven repo gets `mvn deploy` straight from Jenkins.
