# Static Analysis Audit — Phase 3 Spring 6+ Infrastructure

**Date:** 2026-05-18
**Baseline:** `08e760bdaf` (branch `worktree-static-analysis-audit` off `phase2-acl-migrate`)
**JDK floor:** 17 (Amazon Corretto). Build verifies with JDK 17; JDK 21 is the next-phase target.

## 1. Tools currently in use

The Gemma build (root `pom.xml` + parent `ubc.pavlab:pavlab-starter-parent:1.2.29`) ships **one** static-analysis-adjacent plugin and **one** annotation-only dependency. Every other widely-used Java static analyzer is absent.

| Tool | Kind | Version | Phase binding | Fails build? | Notes |
| --- | --- | --- | --- | --- | --- |
| OWASP `dependency-check-maven` | Plugin | **12.2.0** | None (no `<executions>`) — runs only on explicit `mvn dependency-check:check` | No — opt-in CLI invocation | Configured to disable `assemblyAnalyzer` + `yarnAuditAnalyzer`. JDK 17/21 compatible. Plugin lives in root POM `<build><plugins>`. |
| `com.google.code.findbugs:jsr305` | Annotation jar | **3.0.2** | n/a (compile-scope dep) | n/a | Provides `@Nullable`/`@CheckForNull`/etc. NOT an analyzer; the FindBugs *project* is dead (last release 2015). Modern equivalent is `com.github.spotbugs:spotbugs-annotations`. |
| `maven-enforcer-plugin` | Plugin | **3.6.2** | `validate` (default) | Yes (`enforce-maven` execution) | Requires Maven ≥3.6.3, Java ≥17, plus `dependencyConvergence`. Counts as static analysis at the dependency-graph layer. |
| `versions-maven-plugin` | Plugin | **2.21.0** | None — manual invocation | No | Used for `versions:display-dependency-updates`, version drift inspection. |

The parent POM (`pavlab-starter-parent:1.2.29`) was inspected and contains **zero** references to `jacoco`, `spotbugs`, `findbugs`, `errorprone`, `checkstyle`, `pmd`, `dependency-check`, or `sonar` — it pins Maven core plugin versions only.

Submodule POMs (`gemma-core`, `gemma-cli`, `gemma-rest`, `gemma-web`) also contain no static-analysis tooling.

No `.github/workflows/`, no `.gitlab-ci.yml`, no Jenkinsfile in-tree. CI is external (`https://jenkins.pavlab.msl.ubc.ca/job/Gemma/`); whatever it adds is not visible from the repo. **Claim that the Maven-modernization agent implicitly bumped JaCoCo via the parent is false** — JaCoCo is not configured anywhere in the build graph.

## 2. Tools absent

These are the canonical static-analyzer suite for a Spring 6+ / JDK 17–21 Java codebase. None is currently wired:

- **SpotBugs** (`com.github.spotbugs:spotbugs-maven-plugin`) — bytecode bug pattern detector. Successor to FindBugs.
- **SpotBugs annotations** (`com.github.spotbugs:spotbugs-annotations`) — `@Nullable` / `@NonNull` replacement for jsr305. jsr305 is unmaintained.
- **`find-sec-bugs`** SpotBugs plugin — security-focused rules (SQLi, XSS, weak-crypto).
- **Error Prone** (`com.google.errorprone:error_prone_core` via `-Xplugin:ErrorProne` on `maven-compiler-plugin`) — Google's compile-integrated checker. Catches null-deref, generic mismatches, time-handling foot-guns.
- **Checkstyle** (`maven-checkstyle-plugin`) — style + simple structural rules.
- **PMD** (`maven-pmd-plugin`) — additional code-smell rules + copy/paste detector (CPD).
- **JaCoCo** (`org.jacoco:jacoco-maven-plugin`) — line/branch test coverage. The CI may run coverage externally; the POM does not generate `jacoco.exec`.
- **Sonar / SonarQube scanner** (`sonar-maven-plugin`) — aggregate static-analysis dashboard. Optional; usually CI-only.
- **CodeQL** — GitHub-side; would need `.github/workflows/codeql.yml`. Absent.

## 3. Version drift

Latest stable as-of-Jan-2026 (per task brief plus checks):

| Tool | Project version | Current stable (Jan 2026) | Drift |
| --- | --- | --- | --- |
| OWASP `dependency-check-maven` | **12.2.0** | 12.2.x (12.x line current) | **None — current** |
| `findbugs:jsr305` | 3.0.2 | 3.0.2 (terminal; project dead since 2015) | None — but the project itself is end-of-life |
| `maven-enforcer-plugin` | 3.6.2 | 3.6.2 | None — current |
| `versions-maven-plugin` | 2.21.0 | 2.21.x | None — current |
| **SpotBugs** plugin | absent | 4.9.x | n/a — would need adding |
| **JaCoCo** plugin | absent | 0.8.12+ | n/a — would need adding |
| **Error Prone** | absent | 2.32+ | n/a — would need adding |
| **Checkstyle** | absent | 10.20+ | n/a — would need adding |
| **PMD** | absent | 7.x | n/a — would need adding |

## 4. Trivial bumps applied

**None.** Every plugin currently pinned is already at its current stable version. No micro-bumps available. The audit is closed without POM changes.

## 5. Recommendations

### Quick wins (low risk, defer to a follow-up commit)

1. **Replace `findbugs:jsr305` with `spotbugs-annotations`.** The annotation surface is API-compatible enough that a global import rewrite is tractable; the jsr305 jar has been unmaintained for ~9 years and is increasingly flagged by supply-chain auditors. Add as a `<dependencyManagement>` entry first to avoid version churn across modules.
2. **Bind `dependency-check:aggregate` to the `verify` phase behind a `-Psecurity` profile.** Today the security scan only runs when a developer remembers to invoke it. A profile keeps it out of the hot `mvn install` path but documents the canonical invocation.

### Worth adding (bigger conversation, do not add unilaterally)

1. **JaCoCo (`jacoco-maven-plugin 0.8.12+`)** — `prepare-agent` in `initialize`, `report` in `verify`, no fail-on-coverage threshold to start. Coverage data alone (without a gate) is low-friction and high-value. Spring 6 + Jersey 3 are well-supported.
2. **SpotBugs (`spotbugs-maven-plugin 4.9.x`)** with an empty `spotbugs-exclude.xml` baseline. Start at `effort=Default`, `threshold=High`; tighten over time. Optional: add the `findsecbugs-plugin` for security rules.
3. **Error Prone via `maven-compiler-plugin`** — compile-time checks; opt-in via a `-Perror-prone` profile during stabilisation. Catches a class of bugs SpotBugs misses (e.g. immutable-collection misuse, generic-type inference traps).
4. **Checkstyle + PMD** — defer until the codebase has a documented style. Adopting them retroactively against a 20-year-old codebase produces noise rather than signal.

### Deliberately not recommended right now

- **Sonar/SonarQube** — adds operational overhead without an existing server. The Jenkins instance may already run one; check there before duplicating.
- **CodeQL** — requires GitHub Actions, which the project does not currently use. The Jenkins pipeline is the canonical CI.

## 6. Open questions

1. Does Jenkins (`jenkins.pavlab.msl.ubc.ca/job/Gemma/`) already run JaCoCo, SpotBugs, or SonarQube externally? If so the in-POM picture is misleading and we should mirror those into the POM so local builds match CI.
2. Is the jsr305 → spotbugs-annotations swap blocked by any third-party API that exposes jsr305 types in its signatures? (Hibernate Validator, Lombok, baseCode — worth a quick `grep -r javax.annotation.Nullable` before committing.)
3. The OWASP `dependency-check` plugin has no `<executions>` binding — is this intentional (run only on demand) or a leftover from when an earlier dev wired it up for `verify` and someone backed it out? The Phase 2 enforcement re-enable commit (Phase 2 Step 9) didn't touch this.

## 7. Build verification

`JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn compile test-compile -DskipTests -q` passes cleanly on the baseline. No POM changes were applied, so the verification is a no-op sanity check rather than a regression test.
