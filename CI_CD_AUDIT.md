# CI/CD Pipeline Audit

**Branch:** `phase2-acl-migrate`
**HEAD:** `29b21206c28f28eec1a39a674ffa2b48de313b54`
**Date:** 2026-05-19
**Scope:** Verify the CI pipeline can validate the Phase 3 framework bumps
(Spring Boot 3.5.6, Spring 6.x, Hibernate 6, Jersey 3, JUnit 5.12.2, JDK 17).

---

## 1. CI Configuration Inventory

A single CI configuration lives in the repo:

| Path | Type | Purpose |
|---|---|---|
| `.jenkins/Jenkinsfile` | Declarative Jenkins pipeline | The canonical build/test/deploy pipeline. |
| `.jenkins/README.md` | Doc (9 lines) | One-liner: "run `validate-jenkinsfile` before pushing changes". |
| `.jenkins/validate-jenkinsfile` | bash (5 lines) | POSTs the Jenkinsfile to `https://jenkins.pavlab.msl.ubc.ca/pipeline-model-converter/validate` for syntax check. |

No other CI configs exist in this repo. There is:
- **NO** `.github/workflows/` directory.
- **NO** `.gitlab-ci.yml`.
- **NO** `azure-pipelines.yml`, `bitbucket-pipelines.yml`, `.travis.yml`, `.circleci/`.

The Jenkins server itself (`https://jenkins.pavlab.msl.ubc.ca/job/Gemma/`) is
external; this audit covers only what is checked into the repo. The pom's
`<ciManagement>` element confirms Jenkins is the canonical CI system.

### Jenkinsfile structure

Declarative pipeline, `agent any`. Stages (in order):

1. **Checkout scm** — `checkout scm`; resolves `gemmaVersion` + `baseCodeVersion`
   from `mvn help:evaluate`; classifies the build as production / support /
   staging / development by branch name; validates `-SNAPSHOT` suffix matches
   branch type; runs `mvn -B clean`.
2. **Build** — `mvn -B compile`. Posts GitHub commit status.
3. **Run quick unit tests** — `mvn -B test --fail-at-end -DexcludedGroups=SlowTest`.
   Publishes surefire JUnit XML.
4. **Package** — `mvn -B package -DskipTests`. Archives `**/target/*.jar` and
   `**/target/*.war` with fingerprinting.
5. **Parallel block** (gated to `master`, `support-*`, `hotfix-*`, `release-*`,
   `development`, or `FORCE_*` params):
   - **Slow unit tests** — `mvn -B test --fail-at-end -Dgroups=SlowTest -DskipWebpack`,
     guarded by `lock('gemma-slow-tests')`.
   - **Integration tests** — `mvn -B verify --fail-at-end -DskipUnitTests
     -DskipJavadoc -DskipWebpack`, guarded by `lock('database/gemdtest')`.
     Publishes failsafe JUnit XML.
   - **SonarQube Analysis** — `mvn package … dependency-check:check sonar:sonar`
     under `withSonarQubeEnv('UBC SonarQube')`, guarded by
     `lock('gemma-sonarqube-analysis')`.
   - **Deploy artifacts** — `mvn -B deploy …` to internal Maven repo.
   - **Deploy Maven website** — `mvn -B site-deploy …` then `chmod` + symlink.
   - **Deploy Gemma Web** — `./gemma-web/deploy.sh`.
   - **Deploy Gemma CLI** — `./gemma-cli/deploy.sh` and `./gemma-cli/deploy-wiki.sh`.

Triggers: branch-based (master / hotfix-* / release-* / development / support-*).
The pipeline does not declare any `triggers {}` block — runs are presumed to be
fired by the Jenkins multibranch / webhook configuration on the server (out of
scope for repo-level audit).

Post actions: Slack notification on `fixed` (recovery) and `unsuccessful`.

---

## 2. JDK / Maven Versions vs Repo Requirements

### Repo requirements (from root `pom.xml`)

| Requirement | Value | Source |
|---|---|---|
| Maven floor | `[3.6.3,)` | `maven-enforcer-plugin` `requireMavenVersion` |
| Java floor | `[17,)` | `maven-enforcer-plugin` `requireJavaVersion` |
| Compiler release | `17` | `<maven.compiler.release>17</maven.compiler.release>` |
| Spring Boot | `3.5.6` | `<spring-boot.version>3.5.6</spring-boot.version>` |

### Jenkinsfile-declared toolchain

```groovy
tools {
    maven 'Maven 3.6.3'
}
```

**At the pipeline level there is NO `jdk` entry.** The only stage that pins
a JDK explicitly is SonarQube:

```groovy
stage('SonarQube Analysis') {
    tools { jdk 'Java 17' }
    ...
}
```

This means Build, Quick Tests, Package, Slow Tests, Integration Tests,
Deploy Artifacts, Deploy Maven Site, Deploy Gemma Web, and Deploy Gemma CLI
**all rely on whatever JDK the Jenkins agent has as its environment default**.
That default is set on the Jenkins server itself (not visible from the repo).

#### Risk

If the Jenkins agent's default `JAVA_HOME` is JDK 11 (or even JDK 8 for a
legacy node), every stage except SonarQube will fail with the Boot 3.5.6 +
Hibernate 6 + Jersey 3 + Jakarta-namespace bump now in flight on this branch
— Spring Boot 3.x requires Java 17 minimum. Pre–Phase 3 the codebase may
have tolerated JDK 11; it no longer does.

#### Maven version: zero headroom

Jenkins pins `Maven 3.6.3`, which exactly matches the pom's enforcer floor.
No Maven-side risk for the Phase 3 bumps, but no upgrade buffer either. Maven
3.6.3 was released 2019-11; long out of support.

---

## 3. Test Execution + Credential Handling

### How CI runs tests

- **Quick unit tests:** `mvn -B test --fail-at-end -DexcludedGroups=SlowTest`
  on every build, all branches.
- **Slow unit tests:** `mvn -B test --fail-at-end -Dgroups=SlowTest -DskipWebpack`
  in parallel block, gated to long-lived branches. Single-tenant lock via
  `lock('gemma-slow-tests')`.
- **Integration tests:** `mvn -B verify --fail-at-end -DskipUnitTests
  -DskipJavadoc -DskipWebpack`, single-tenant lock via
  `lock('database/gemdtest')`.

Tests are categorised via JUnit `@Category` / Jupiter `@Tag` (`SlowTest`
group); pom-level surefire + failsafe plugins are configured in the root
pom. Both surefire (`**/target/surefire-reports/*.xml`) and failsafe
(`**/target/failsafe-reports/*.xml`) XML are collected by the `junit` step.

### Credential handling for `gemma.testdb.password`

The Jenkinsfile **never** passes `-Dgemma.testdb.password=...`. The default
property value lives in `gemma-core/src/main/resources/default.properties`:

```
gemma.testdb.password=1234
```

i.e. the CI's `gemdtest` MySQL is assumed to be reachable with the
default password `1234`. This matches the canonical local
`mvn verify -Dgemma.testdb.password="$(security find-generic-password -s mysql-root -w)"`
pattern only in shape — locally the user pulls from macOS Keychain;
on Jenkins the agent host evidently runs a `gemdtest` MySQL configured with
that hardcoded test password (or a server-level Jenkins credential injects
it via a global `MAVEN_OPTS` / properties file not visible here).

The only credential the Jenkinsfile pulls explicitly is
`GEMMA_CLI_WIKI_DEPLOY_TOKEN = credentials('confluence-wiki-token')` — a
Confluence wiki deploy token, unrelated to test DB.

#### Gap

There is no protection if the agent's `gemdtest` password drifts from
the default. A keychain-style indirection on Jenkins would be safer (Jenkins
`credentials('gemma-testdb-password')` injected as a Maven property), but
this is a MEDIUM-priority concern, not blocking.

---

## 4. Build Matrix — Reactor Modules

The root pom declares **four** modules (not five):

```xml
<modules>
  <module>gemma-core</module>
  <module>gemma-cli</module>
  <module>gemma-rest</module>
  <module>gemma-web</module>
</modules>
```

CI invokes Maven from the reactor root (`mvn -B compile`, `mvn -B test`,
`mvn -B package`, `mvn -B verify`) so all four modules are built and tested
on every run. Integration tests run via `mvn -B verify -DskipUnitTests` in
the parallel stage, exercising failsafe across all modules.

There is no profile slicing in the pipeline — full reactor every time.

---

## 5. Artifacts Published

| Stage | Artifact / Action |
|---|---|
| Package | `archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war', fingerprint: true` — captures **all** `.jar` and `.war` files including `gemma-cli`, `gemma-core`, `gemma-rest`, `gemma-web` outputs. |
| Quick Tests | Surefire XML reports collected via `junit` step. |
| Slow Tests | Surefire XML reports collected via `junit` step. |
| Integration Tests | Failsafe XML reports collected via `junit` step. |
| Deploy artifacts | `mvn -B deploy` — published to internal Maven repo (URL configured in pom `<distributionManagement>` / settings.xml on the agent). |
| Deploy Maven website | `mvn -B site-deploy` to `${MAVEN_SITES_DIR}/gemma/gemma-${gemmaVersion}` then chmod + symlink into `${dataDir}/gemma-devsite`. |
| Deploy Gemma Web | `./gemma-web/deploy.sh ${deployRef}` (target server resolved from branch). |
| Deploy Gemma CLI | `./gemma-cli/deploy.sh ${cliRef}` + `./gemma-cli/deploy-wiki.sh` for Confluence docs. |

No SBOM, no container image, no GitHub Releases attachment. Deployment
targets are internal Pavlab hosts (`moe`, `chalmers`).

---

## 6. Maven Local-Repo Caching

**No explicit Maven cache configuration in the Jenkinsfile.**

The pipeline does not:
- mount or restore `~/.m2/repository` via a `cache` step
- use the Jenkins `pipeline-maven` plugin's caching directives
- run with `-Dmaven.repo.local=...` pointing at a workspace-scoped cache

Because `agent any` re-uses the host agent's filesystem between builds, the
Jenkins agent's user-home `~/.m2/repository` is presumably persistent across
runs by virtue of the agent being a long-lived VM/host. This works in
practice but:

- has no published cache key / no eviction policy
- breaks if the build moves to ephemeral agents (Docker, K8s)
- offers no per-build "cold" cache validation

After the Boot 3.5.6 bump pulled in a substantial set of new transitive
deps (Jakarta-namespace replacements, Hibernate 6 ORM, Jersey 3 transitives),
the first post-merge build on the agent will incur a one-time download
penalty but subsequent builds are unaffected. This is MEDIUM concern.

---

## 7. Modernization Recommendations

### HIGH (blocks Phase 3 framework-bump validation)

1. **Pin JDK 17 at the pipeline level, not just SonarQube.** Move
   `jdk 'Java 17'` from the SonarQube stage's `tools` block to the top-level
   `tools` block alongside `maven 'Maven 3.6.3'`. Without this, the Build,
   Test, Package, Integration Test, and Deploy stages rely on the Jenkins
   agent's default `JAVA_HOME`. If that default is still JDK 11 (as it
   plausibly was pre-Phase 3), every post-bump build will fail with
   `UnsupportedClassVersionError` or compile errors against Boot 3.5.6's
   Java 17 baseline. This is a one-line change but it is currently the
   single largest unaudited assumption in the pipeline.

2. **Add a pre-flight `java -version` echo in the Checkout stage.** Even
   after pinning, surface the JDK version into the build log so framework-bump
   regressions are debuggable from console output alone. `sh 'mvn -version
   && java -version'` at the start of Build. Trivial cost; large diagnostic
   payoff for the bumps already in flight.

### MEDIUM (slow / fragile but works)

3. **Externalise `gemma.testdb.password` via Jenkins credentials.** Today
   the integration-test stage relies on the hardcoded `1234` default. Wire
   a Jenkins `credentials('gemma-testdb-password')` into a `withCredentials`
   block and pass `-Dgemma.testdb.password=$GEMMA_TESTDB_PASSWORD` to the
   `mvn verify` invocation. Matches the keychain pattern used locally and
   removes a hardcoded secret-shaped value from defaults.

4. **Document or pin the agent's Maven local-repo cache.** Either:
   (a) explicitly set `-Dmaven.repo.local=$WORKSPACE/.m2/repository` and
   cache that directory via `pipeline-maven` or the `cache` plugin, or
   (b) at minimum document in `.jenkins/README.md` that the agent's
   `~/.m2/repository` is the cache and must be preserved across runs.
   Phase 3's transitive-dep churn makes this more load-bearing than before.

5. **Bump Maven from 3.6.3 → 3.9.x.** The pipeline pins exactly the enforcer
   floor. Maven 3.6.3 reached end-of-life years ago; 3.9.x is the current LTS
   and improves dependency resolution + parallelism. Bump the Jenkins tool
   definition (server-side) and update `requireMavenVersion` floor to
   `[3.9.0,)`.

6. **Split the SonarQube `mvn package` from the main Package stage.** The
   SonarQube stage re-runs `mvn package -DskipTests -DskipJavadoc -DskipWebpack`
   from scratch in parallel with the deploy stages, duplicating ~5 min of
   work already done in the sequential `Package` stage. Use
   `dependsOn` / artifact pass-through, or run sonar against the already-
   packaged tree.

### LOW (cosmetic / nice-to-have)

7. **Consider adding a minimal `.github/workflows/ci.yml`** that runs
   `mvn -B test -DexcludedGroups=SlowTest` on PRs for OSS-visibility — the
   project is public on GitHub but PRs from non-Pavlab contributors get no
   feedback because the Jenkins instance is internal. A 30-line GH Actions
   workflow with `setup-java@v4` (`distribution: temurin`, `java-version: 17`)
   and m2 caching would close that gap. Not blocking; not urgent.

8. **`agent any` is broad.** Pinning a label (e.g. `agent { label 'gemma-build' }`)
   would make resource expectations explicit and let the Pavlab admin route
   builds to a known-good node.

9. **Migrate Sonar `mvn` invocation off the unversioned scanner plugin GAV**
   (`org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` resolves "latest"
   on each run — flaky). Pin a version.

10. **`fingerprint: true` on `archiveArtifacts`** captures *every* `.jar`
    in `**/target/`, including test-classifier artifacts. Narrow to the four
    distributable artifacts (`gemma-cli.jar`, `gemma-core.jar`,
    `gemma-rest.jar`, `gemma-web.war`) to keep the Jenkins artifact store
    lean.

---

## Summary

The repo's CI footprint is one Declarative Jenkinsfile (323 lines) plus a
9-line README and a 5-line validator script — concise, well-structured,
covers Build / Test / Package / Integration / Sonar / Deploy in one pass.

The single HIGH-priority gap is that the pipeline-level `tools` block pins
Maven 3.6.3 but does **not** pin a JDK; only the SonarQube stage requests
`jdk 'Java 17'`. With Spring Boot 3.5.6 / JDK 17 already merged on this
branch, the Build / Test / Integration / Deploy stages will all silently
inherit the Jenkins agent's default `JAVA_HOME` — a configuration value
not under repo control. This must be resolved before the framework-bump
work hits a long-lived branch.

Everything else is MEDIUM (testdb-credential indirection, Maven repo cache,
Maven 3.9 upgrade) or LOW (GitHub Actions mirror, agent labels, artifact
narrowing). None of those are blocking.
