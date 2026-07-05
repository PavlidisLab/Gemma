# Java 21 Readiness Recce

Phase 3 infrastructure modernization. Status: **recce-only**. Doc-only output;
no source / build changes. The codebase currently targets and runs on JDK 17
(amazon-corretto). JDK 21 is the next LTS (Sep 2023) and brings virtual threads
(JEP 444), pattern matching for switch (JEP 441), sequenced collections (JEP
431), and generational ZGC (JEP 439) — features that meaningfully apply to
Gemma's IO-bound batch / REST workloads.

Baseline: `08e760bdaf` (`phase2-acl-migrate` HEAD on 2026-05-18).

---

## 1. Current JDK pinning

Single source of truth lives in the root `pom.xml`. Submodule poms
(`gemma-core`, `gemma-cli`, `gemma-rest`, `gemma-web`) inherit and do **not**
override JDK level.

| File:line | Setting |
|---|---|
| `pom.xml:1006` | `<maven.compiler.release>17</maven.compiler.release>` |
| `pom.xml:1013` | `<maven.compiler.parameters>true</maven.compiler.parameters>` (preserved across bumps) |
| `pom.xml:632-634` | `maven-enforcer-plugin` → `<requireJavaVersion>[17,)</requireJavaVersion>` (range, so JDK 21 already *passes* enforcement) |
| `pom.xml:1066` | `--add-opens=java.base/java.lang=ALL-UNNAMED` in `jvmOptions` (test + run; marked FIXME) |
| `pom.xml:1064` | `-Dspring.security.strategy=MODE_INHERITABLETHREADLOCAL` (virtual-thread-relevant; see §4) |

Notes:

- Build/runtime JDK is amazon-corretto-17 per `RENOVATIONS.md`. Bytecode
  target was 8 → 11 → 17 across Phases 0/1/2.
- Lombok / AspectJ / JaCoCo plugin versions are **not** declared in
  `pom.xml`; they're inherited from `pavlab-starter-parent:1.2.29`. Parent
  pom is not present in the local m2 cache for this worktree, so the exact
  inherited versions can't be read from this recce — see §7 open question.

---

## 2. Third-party compatibility matrix

| Dependency | Version pinned (this repo) | JDK 21 status | Action |
|---|---|---|---|
| Spring Framework | 6.1.20 (`pom.xml:1022`) | OK — JDK 21 supported since 6.1 | none |
| Spring Security | 6.3.10 (`pom.xml:1023`) | OK | none |
| Hibernate ORM | 6.4.10.Final (`pom.xml:1024`) | OK — Hibernate 6.4 supports JDK 21 | none |
| Jersey | 3.1.10 (`pom.xml:1028`) | OK | none |
| Tomcat (embedded/runtime) | 10.1.34 (`pom.xml:1044`) | OK — 10.1.x runs on JDK 21 | confirm production Tomcat JVM (see §7) |
| Jackson | 2.21.0 (`pom.xml:1029`) | OK | none |
| Swagger | 2.2.42 (`pom.xml:1031`) | OK | none |
| Flyway core + flyway-database-h2 + flyway-mysql | 10.22.0 (`pom.xml:1027`) | OK — Flyway 10 requires JDK 17+ and supports 21 | none |
| MySQL connector/j | 8.4.0 (`pom.xml:1043`) | OK | none |
| Mockito | 5.21.0 (`pom.xml:1048`) | OK — 5.x supports JDK 21 | none |
| AssertJ | 3.27.7 (`pom.xml:1049`) | OK | none |
| AntLR runtime | 4.13.2 (`pom.xml:1045`) | OK | none |
| Slack SDK | 1.47.0 (`pom.xml:1033`) | OK | none |
| Micrometer | 1.13.11 (`pom.xml:1035`) | OK | none |
| PrettyTime | 5.0.6.Final (`pom.xml:1047`) | OK | none |
| **Lombok** | inherited from `pavlab-starter-parent:1.2.29` (unknown) | **JDK 21 requires Lombok ≥ 1.18.30**; 1.18.32 recommended | **VERIFY** — see §7 |
| **AspectJ** | inherited (unknown) | JDK 21 requires aspectj-tools / aspectjweaver ≥ 1.9.21 | VERIFY |
| **JaCoCo** | inherited (unknown) | JDK 21 requires jacoco-maven-plugin ≥ 0.8.11 | VERIFY |
| HDF5 JNI (`hdf5.version` 1.12.3) | `pom.xml:1040` | Native lib — class-file bytecode is independent of JDK; risk is the dylib's interaction with the JDK's `java.library.path` resolver. JDK 21 made no breaking change in JNI / `System.loadLibrary` semantics. | Smoke-test only |

**Biggest third-party blocker risk: Lombok**, because:
1. Its version is hidden in the parent pom and we couldn't confirm from this
   recce that it's ≥ 1.18.30.
2. Even when nominally JDK-21-compatible, Lombok historically lags JDK
   releases by 1–2 months for new javac internals and ships hotfix releases
   after the GA. Anyone bumping JDK on day 1 of a new release should expect
   a Lombok bump in the same commit.
3. The repo uses **delombok** in the javadoc path
   (`pom.xml:729-750, 971-974`) — broken Lombok breaks `mvn verify` even
   when `-DskipTests`.

AspectJ and JaCoCo are lower-risk: their JDK-21-compatible releases (1.9.21
and 0.8.11) shipped in late 2023 and are now mature.

---

## 3. Codebase-level risks

Searches performed in this recce:

```
grep -rEn "import sun\.|import com\.sun\." --include='*.java' .
grep -rEn "add-opens|add-exports" --include='pom.xml' .
grep -rEn "setAccessible|getDeclaredField\(|getDeclaredMethod\(" --include='*.java' .
```

Findings:

- **No `sun.*` / `com.sun.*` imports** anywhere in production or test code.
  This is the single most encouraging signal — Gemma has stayed inside
  the supported API surface, so JDK 21's continued strong-encapsulation
  tightening is not expected to break compilation.
- **Reflection: only 2 hits** for `setAccessible` / `getDeclaredField` /
  `getDeclaredMethod` across the whole repo. Very low surface area for
  JDK-internals reflection breakage.
- **`--add-opens=java.base/java.lang=ALL-UNNAMED`** is present in
  `pom.xml:1066` and marked with a FIXME comment ("remove this once we've
  migrated to Spring 5"). The FIXME predates the Spring 6 migration; the
  opens directive is *probably* no longer needed for Spring itself, but
  Mockito 5's inline mocker and the surefire `-javaagent` line
  (`pom.xml:1073`) may still want it. Recommended: **leave it in** during
  the JDK 21 bump; revisit as a separate cleanup. JDK 21 still honours
  `--add-opens`; nothing breaks.

**Top 2 codebase risks** (both low):

1. **Lombok annotation processor lag** (see §2). The bigger of the two —
   delombok is on the javadoc critical path.
2. **`MODE_INHERITABLETHREADLOCAL` + virtual threads.** Spring Security's
   inheritable-threadlocal strategy is set in `pom.xml:1064` so the
   security context propagates to spawned worker threads. With virtual
   threads (§4), each thread is essentially short-lived; inheritable
   threadlocals work but at high virtual-thread counts the inheritance
   cost is non-trivial. **Not a blocker** for the JDK 21 bump (the strategy
   keeps working), but worth revisiting when actually adopting virtual
   threads. Spring Security 6.3 ships `DelegatingSecurityContextExecutor`
   which is the modern, virtual-thread-friendly substitute.

---

## 4. Features worth leveraging under JDK 21

### Virtual threads (JEP 444) — significant opportunity

Inventory of thread-pool / async sites in this codebase
(`Executors.newFixedThreadPool` + `newSingleThreadExecutor` +
`newCachedThreadPool` + `ThreadPoolTaskExecutor` + `@Async`):

- **41 Java files** with `ExecutorService` / `ThreadPoolTaskExecutor` /
  `@Async` / `ForkJoinPool` references.
- ~30 distinct call sites for `Executors.newXxx(...)`:
  - **IO-bound**: `FtpFetcher`, `FtpArchiveFetcher`, `HttpFetcher`,
    `SimpleDownloader`, `UnifiedOntologyUpdaterCli`,
    `GoogleAnalytics4Provider`, `ExpressionDataFileServiceImpl`,
    `SingleCellDataLoaderCli` (network + disk pulls). **Prime virtual
    threads candidates**.
  - **CPU-bound**: `ComBat` (batch effect correction — math), and
    transform stages of single-cell loaders. These should stay on
    platform-thread pools.
- A repo-local wrapper `ubic.gemma.core.util.concurrent.Executors` already
  centralizes executor creation — **virtual-thread adoption can land
  through that single file** rather than 30 callsites. This is a
  meaningfully clean migration story.
- Spring `ThreadPoolTaskExecutor` is bound to two beans in
  `applicationContext-serviceBeans.xml` (`taskExecutor`,
  `expressionDataFileTaskExecutor`). Spring 6.1 supports
  `Executors.newVirtualThreadPerTaskExecutor()` as a `TaskExecutor`
  adapter — direct drop-in.

**Sizing**: IO-bound CLIs (FTP / HTTP / ontology fetchers) and the REST
layer's downstream-DB calls are the realistic wins. Virtual threads turn
"thread pool sized for max-concurrent-IO" into "one virtual thread per
in-flight request"; for batch loaders that fetch hundreds of files this
removes a tuning parameter and improves utilization. CPU-bound paths
(ComBat, statistical analyses) get **no benefit** and should be left
alone.

### Other JDK 21 features (lower priority)

- **Pattern matching for switch (JEP 441)**: stylistic; would clean up
  some `instanceof` chains in the REST argument resolvers and the data
  loader factories. No urgency.
- **Sequenced collections (JEP 431)**: minor convenience. Some
  `LinkedHashMap` / `LinkedHashSet` callsites currently do
  `iterator().next()` to get first / last — would become `getFirst()` /
  `getLast()`. Pure readability.
- **Records improvements**: opportunistic. Couples with the
  Lombok→records migration thread already discussed in Phase 3 vision
  (and the delombok footprint in the pom makes "replace Lombok value
  classes with records" a coherent long-term play).
- **Generational ZGC (JEP 439)**: production GC choice. Gemma's heap
  workloads (ontology in-memory caches, single-cell matrix slabs) could
  benefit; needs prod ops sign-off.

### Things JDK 21 does **not** give us that we might want

- **Structured concurrency** is still preview in JDK 21 (`--enable-preview`
  required). Don't depend on it for production.
- **Scoped values** are also preview in 21. Defer.

---

## 5. Recommended phased approach

### Phase 1 — version-floor bumps WHILE STAYING ON JDK 17

Goal: every plugin / annotation processor on a JDK-21-compatible release,
proven green on JDK 17 first. No JDK change in this phase.

Actions:

1. Resolve the actual Lombok / AspectJ / JaCoCo versions inherited from
   `pavlab-starter-parent:1.2.29` (read the parent pom; either fetch it
   into m2 cache or read it from the pavlab repo). Document them in
   `RENOVATIONS.md`.
2. If any of those is below the JDK-21 floor (Lombok 1.18.30, AspectJ
   1.9.21, JaCoCo 0.8.11), override the version in Gemma's root `pom.xml`
   `<properties>` block — same pattern as the 8 dependency overrides
   already there (`pom.xml:149`+). Don't wait for a `pavlab-starter-parent
   1.3` bump.
3. Run full `mvn verify` on JDK 17. Confirm no regression.

**Effort: 0.5 day.** Mostly research + a one-line property bump.

### Phase 2 — dual-compile CI matrix

Goal: prove JDK 21 works without committing to it.

Actions:

1. Add a Jenkins job matrix variant: `mvn verify` on JDK 21 alongside
   the existing JDK 17 build. Both must pass.
2. Triage any new compiler warnings on JDK 21 (likely a handful of
   "this-escape" warnings in constructor chains; benign).
3. Smoke-test HDF5 JNI on JDK 21 — load `libhdf5_java.dylib` and round-trip
   a small matrix. (No code change expected; just verify.)

**Effort: 1 day** (Jenkins config + investigation of any new warnings).
**Gate**: at least one full week of green dual-build CI before Phase 3.

### Phase 3 — flip the runtime + bytecode

Actions:

1. `<maven.compiler.release>21</maven.compiler.release>` in `pom.xml:1006`.
2. `<requireJavaVersion>[21,)</requireJavaVersion>` in the enforcer rule
   (`pom.xml:633`).
3. Update `RENOVATIONS.md` and CI/Jenkins JDK config.
4. Coordinate with deployment: production Tomcat JVM must move to JDK 21
   *first* (or in lockstep). See §7.
5. Remove the JDK 17 leg from the CI matrix.

**Effort: 0.5 day code-side + production rollout coordination.**

### Phase 4 — opportunistically adopt virtual threads

Actions:

1. Switch the two Spring `ThreadPoolTaskExecutor` beans in
   `applicationContext-serviceBeans.xml` to
   `Executors.newVirtualThreadPerTaskExecutor()` (wrapped in
   `TaskExecutorAdapter`) **for the IO-bound bean**
   (`expressionDataFileTaskExecutor`). Leave the general `taskExecutor`
   as platform threads until profiled.
2. In `ubic.gemma.core.util.concurrent.Executors`, add a parallel
   `newVirtualThreadPerTaskExecutor()` factory and migrate the
   IO-bound callsites identified in §4 one CLI at a time.
3. Profile each migration; revert any that show no improvement or worse
   tail latency.
4. Defer pattern-matching-switch / sequenced-collections refactors —
   these are stylistic and can ride along with feature work, not a
   dedicated phase.

**Effort: 2–4 days, iterative, low risk per increment.**

---

## 6. Effort summary

| Phase | Effort | Risk | Blocking? |
|---|---|---|---|
| 1. Version-floor bumps on JDK 17 | 0.5 day | very low | no |
| 2. Dual-compile CI | 1 day + 1 week soak | low | gates phase 3 |
| 3. Flip to JDK 21 | 0.5 day code + ops coord | low | requires prod JVM bump |
| 4. Virtual threads adoption | 2–4 days, iterative | low (per-bean) | post-cutover |

**Total to "on JDK 21 in production": ~2 days of engineering + 1 week of CI soak + the production Tomcat JVM upgrade window.**

---

## 7. Open questions for Paul

1. **Production Tomcat JVM**: what JDK is the Jenkins-deployed Gemma WAR
   running on in production today? `pom.xml` ships
   `tomcat.version=10.1.34` (embedded for tests; supports 21), but the
   prod Tomcat instance has its own JDK. Confirming this is the blocker
   for Phase 3.
2. **`pavlab-starter-parent:1.2.29` — exact Lombok / AspectJ / JaCoCo
   versions**? The parent pom isn't in the local m2 cache for this
   recce. If Paul has it cached or can point at the pavlab-starter-parent
   git tag, that resolves the §2 unknowns in 5 minutes.
3. **Prod-ops appetite for a JDK bump**: is there a window opening (e.g.
   bundled with the Phase 2 Flyway / Spring 6 prod rollout), or should
   the JDK 21 move be deferred until the current Spring 6 stack has been
   in production for N weeks?
4. **Hibernate Search re-instatement**: Phase 3 vision mentions
   reinstating HS 7 (or OpenSearch). If that lands first, the search
   stack will get its own JDK 21 validation. Order matters: HS 7 first
   or JDK 21 first?
5. **`--add-opens=java.base/java.lang=ALL-UNNAMED`** in `pom.xml:1066` is
   marked with a stale "FIXME: remove this once we've migrated to Spring
   5" comment. Spring is now 6. Can the directive be removed, or is
   something else (Mockito agent? aspectjweaver?) depending on it?
   Separate cleanup, not part of the JDK 21 bump.

---

## 8. Bottom line

Gemma is **structurally ready** for JDK 21. The Spring 6 / Hibernate 6 /
Jersey 3 / jakarta climb done in Phase 2 already cleared the hard
compatibility work — nothing in the major dependency set blocks the bump.
The remaining work is mostly mechanical (Lombok / AspectJ / JaCoCo
floor-version bumps), a CI matrix run, and a production JVM coordination.
The single most valuable JDK 21 feature for this codebase is virtual
threads, applied selectively to IO-bound CLI / REST hot paths via the
existing `ubic.gemma.core.util.concurrent.Executors` wrapper.

Recommended path: **execute Phases 1 + 2 immediately** (low risk,
high information value), gate Phase 3 on Paul's answer to the prod-JVM
question, treat Phase 4 as opportunistic.
