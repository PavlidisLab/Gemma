
# Gemma — repo conventions for Claude Code

Ground rules for adding features and tests in this repo. Project-specific; supplements (does not replace) the user-global `~/.claude/CLAUDE.md`.

## Build

- **JDK 25** (temurin-25, matches production Tomcat). Builds fail with cryptic enforcer errors on JDKs older than the source level. Set `JAVA_HOME` before invoking `mvn` from a fresh shell:
  ```bash
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
  (Older corretto-21 paths in earlier commits / sub-agent briefs are stale — use temurin-25 going forward.)
- **`mvn verify` canonical invocation** (full IT pass against MySQL gemdtest):
  ```bash
  mvn -pl gemma-core verify -Dgemma.hibernate.hbm2ddl.auto=create
  ```
  The gemdtest password defaults to `1234` (`default.properties`), which matches the `testdb` service in `docker-compose.yml` — no override needed. Pass `-Dgemma.testdb.password=…` only if your local MySQL's `gemmatest` account uses a different password (it's a throwaway local-dev credential, not a secret).

  **`-Dgemma.hibernate.hbm2ddl.auto=create` is required, not a convenience.** `default.properties:292` leaves the property EMPTY, so without it Hibernate materializes no schema and `applicationContext-dataSourceInitializer` runs `sql/init-acls.sql` against an empty database:
  ```
  Failed to execute SQL script statement #2 of [sql/init-acls.sql]:
      ALTER TABLE acl_entry ADD COLUMN audit_success BIT NOT NULL DEFAULT 0
  Caused by: Table 'gemdtest.acl_entry' doesn't exist
  ```
  `init-acls.sql` says so in its own header — Hibernate creates `acl_sid` / `acl_object_identity` / `acl_entry` from gsec's HBM mappings *on hbm2ddl=create*; only `acl_class` is created by the script. The dependency arrived with the schema-native ACL work (`74982e6f16`, `6e5a3c90ae`).

  **The failure mode is the trap, not the fix.** One failed bean fails the whole `applicationContext-*.xml` context, and every test sharing it is then reported as `IllegalState ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context`. That produces hundreds of identical errors (476 in one CI run) that are all symptoms — the real cause appears exactly once, far above them. **Always search for the first `Failed to load ApplicationContext` / `Caused by:` before reading anything else**; the threshold lines carry no diagnostic content.

  This bit CI rather than developers: the Jenkins integration stage did not pass the flag, so it could never have passed, while everyone locally followed the invocation above. Fixed in `.jenkins/Jenkinsfile`; keep the flag there.
- **gemdtest auto-reset (default path)** — `CreateDatabasePopulator` runs at test context startup (default `gemma.testdb.initialize=true`) and drops+recreates `gemdtest` before Flyway + Hibernate rebuild it. This requires the test user (`gemmatest`) to hold the server-level CREATE privilege on top of the database-scoped grant; one-time fix:
  ```sql
  -- Run once as root; lets `gemmatest` recreate gemdtest after the populator drops it
  GRANT CREATE ON *.* TO 'gemmatest'@'localhost';
  FLUSH PRIVILEGES;
  ```
  Without this grant, `mvn verify` fails on the CREATE DATABASE step inside the populator and you have to drop/recreate manually (the procedure below).
- **Pre-Spring schema reset via `-Dtestdb.reset` (Flyway plugin)** — opt-in profile `testdb-reset` (parent `pom.xml`) binds `flyway-maven-plugin:clean` to `pre-integration-test`, so the schema is dropped BEFORE Spring boots. Use this when you want to clear the slate before the in-JVM populator runs (e.g. investigating wedged Flyway history rows or a broken Hibernate snapshot the populator can't get past). Requires `gemmatest` to hold `DROP ON gemdtest.*` (already covered by the standard `GRANT ALL PRIVILEGES ON gemdtest.*` grant). Activation is gated on the `testdb.reset` system property so default `mvn verify` is unaffected:
  ```bash
  mvn -pl gemma-core verify -Dtestdb.reset \
      -Dgemma.testdb.password=1234 \
      -Dgemma.hibernate.hbm2ddl.auto=create
  ```
  (`1234` is the `docker-compose.yml` testdb password. Unlike the canonical run above, the Flyway plugin has no password default in the pom, so this leg must pass it explicitly.)
- **Manual schema reset (last-ditch fallback)** — if neither the populator nor the Flyway plugin can run (e.g. the DB is wedged in a state Flyway can't reconcile), drop+recreate `gemdtest` as `gemmatest`, which can DROP/CREATE the database given the `CREATE ON *.*` + `GRANT ALL ON gemdtest.*` grants above. The sandbox blocks credentialed destructive DB ops, so the user runs it via `!` in the prompt:
  ```bash
  mysql -h 127.0.0.1 -P 3307 -ugemmatest -p1234 \
      -e 'DROP DATABASE IF EXISTS gemdtest; CREATE DATABASE gemdtest;'
  ```
  (`-P 3307` / `-p1234` are the `docker-compose.yml` testdb coordinates. The container's root password is random — `MYSQL_RANDOM_ROOT_PASSWORD=yes` — so if the `gemmatest` grants themselves are missing, fix them through the container instead: `docker compose exec testdb mysql -uroot -p"$(docker compose logs testdb | grep 'GENERATED ROOT PASSWORD')"`, or just recreate the container.)
- **Compile-clean is the bar for sub-agents.** `mvn -pl gemma-core compile test-compile -q` must pass after any sub-agent edit. Full `mvn verify` is reserved for orchestrator-led runs; it needs gemdtest creds and serializes against other parallel runs.
- **Javadoc is a build gate, and `mvn verify` does not run it.** The `attach-javadocs` execution lives in the `release` profile of `pavlab-starter-parent`, which has no `<activation>` — Jenkins turns it on through the node's `settings.xml`, so every local `mvn verify` / `mvn package` / compile-clean check passes while javadoc is broken. It fails in the **Package** stage, after the whole suite has already gone green, and it fails per-module, so an error in `gemma-core` hides every error in `gemma-cli` and `gemma-rest`. Reproduce the CI step with:
  ```bash
  mvn -B -P release package -DskipTests
  ```
  `doclint` is `all,-missing`, so these two are hard errors (empty `<p>`, duplicate `@return`, and `@author` on a method are warnings only — 13 exist today and don't fail the build):
  - **`reference not found`** — `{@link Foo}` where `Foo` isn't imported (a signature spelling the type out fully-qualified is the usual cause), or `{@link #method(A)}` naming an overload that doesn't exist. Copy the parameter list from the real signature.
  - **`heading used out of sequence`** — the first heading in a **class** doc comment must be `<h2>`. Javadoc has rendered the class title as `<h1>` since JDK 13; `<h3>` was right under JDK 8/11, which is why the habit persists. Deeper nesting is fine (`<h3>` under an `<h2>`). **In a *method* doc comment the first heading must be `<h4>`** — the implicit preceding heading there is `<h3>` (class `<h1>` → "Method Detail" `<h2>` → the method `<h3>`), so an `<h2>` inside a method comment is an error, which reads as a contradiction of the rule above until you notice the two are different contexts.
- **`mvn -P release package` does NOT cover the Maven site stage.** It exercises `attach-javadocs` from `<build>`; the **Deploy Maven website** stage runs `mvn site-deploy`, whose `aggregate` / `test-aggregate` reports read the SEPARATE `maven-javadoc-plugin` configuration under **`<reporting>`** (`pom.xml` ~1580). The two blocks are near-copies that drift, and Maven shares nothing between them:
  - Config added to one and not the other passes locally and fails in CI. That is how the `@todo` `<tags>` registration went missing from `<reporting>` and produced five `unknown tag. Unregistered custom tag?` errors that only the site stage could see. There is a KEEP IN SYNC comment on the reporting block; honour it.
  - `<pluginManagement>` does **not** apply to `<reporting>` plugins. The reporting javadoc version is pinned to `3.3.2` for that reason — unpinned, it resolved 3.12.0 locally and 3.3.2 on the agent, so the two machines ran different doclint versions over the same sources. Do not remove that `<version>`.
  - Reproduce the site stage with `mvn -B site` (full reactor). Do **not** use `-N`: a non-recursive run has no module classpaths and buries the real errors under thousands of bogus `cannot find symbol` / `package does not exist` lines.
  - A javadoc **resolution** error (e.g. `wrong number of type arguments`) aborts before doclint runs, so it masks every tag/heading error in the same module. Expect a second wave after fixing one.
  - `test-aggregate` javadocs `src/test/java`. It reports 34 errors under 3.12.0 and is unexercised until `aggregate` passes; test sources are held to the same doclint bar as main.
- **A dependency version can differ per module and only `javadoc:aggregate` will notice.** `gemma-core` pinned `spring-retry` to `1.0.3.RELEASE` while `gemma-cli` / `gemma-rest` resolved `2.0.12` through the parent BOM, which remanages transitives. Per-module javadoc compiles each module against its own classpath and passes; the aggregate report merges all three, the newer jar wins, and gemma-core's sources fail against an API that changed shape. Check with `mvn dependency:tree -Dincludes=<group>:<artifact>` across the reactor before assuming a version is uniform.

## Parallel work (multi-agent renovations)

The repo supports parallel sub-agents through git worktrees. Pattern that has been validated end-to-end:

1. Orchestrator creates a pre-baselined worktree from the main checkout:
   ```bash
   git worktree add -B <branch> .claude/worktrees/agent-<short-id> <expected-sha>
   ```
2. Sub-agent is briefed with the worktree's absolute path and an explicit "stay in this path" rule. **Agents must not `cd` into the main repo** — doing so wipes `target/` for any other parallel agent.
3. Sub-agent reports compile-clean (and optionally a focused `mvn -Dtest=...`), commits with a concise message (NO `Co-Authored-By: Claude` trailer — overridden from the global default for this repo), reports back the SHA.
4. Orchestrator merges with `--no-ff` so the integration commit is visible in `git log`.

`isolation:"worktree"` on the Agent tool is broken on this branch (branches off `development`, not current branch — and the sandbox blocks `git reset --hard` from inside the agent so an agent on the wrong baseline can't fix itself). Use the manual worktree pattern instead.

`gemdtest` is single-tenant — parallel agents that run integration tests will corrupt each other's schema. Either gate parallel agents to compile-only validation, or serialize them.

The reusable sub-agent brief skeleton lives in user memory (`feedback_agent_brief_template.md`). Use it.

## Tests

### Fast by default, slow on demand

`mvn verify` is the day-to-day signal. It MUST be fast and deterministic on Mac and Linux dev boxes — no real network, no env binaries beyond `mysql`, no platform-specific filesystem assumptions.

Anything slow / network-bound / env-dependent stays in the codebase but is *tagged* so it doesn't fire by default. The single source of truth is `${excludedGroups}` = **`network,slow`** (parent `pom.xml` line ~1737, with the taxonomy documented in the comment above it). Surefire consumes it as `integration,${excludedGroups}` (~1113), so `integration` is excluded there too; failsafe consumes it bare (~1170), which is what makes integration tests run in failsafe only.

**Tag taxonomy:**

- `@Tag("integration")` — runs in failsafe only, skipped from surefire. Day-to-day `mvn verify` runs these.
- `@Tag("network")` — cheap external-URL reachability probe. Excluded by default; opt-in with `-DexcludedGroups=` (run everything) or a different list.
- `@Tag("slow")` — heavy in-JVM work or large external download (GEO archives, Uberon OWL, UCSC matrices, BLAT alignments, Python subprocesses, etc.). Excluded from BOTH surefire and failsafe by default. Run explicitly with `mvn verify -DexcludedGroups=network` (keep the network exclusion, drop slow) or `-DexcludedGroups=` (clear everything).
- `@Tag("geo")` / `@Tag("pubmed")` / `@Tag("goldenPath")` — **descriptive markers, not filters.** They are NOT excluded by default. A class carrying only one of these runs in the fast suite. Pair every one of them with `@Tag("slow")` or `@Tag("integration")` at CLASS level so it is filtered transitively.

Two traps that have each cost a red build:

- **A method-level `@Tag("slow")` does not filter the class.** The untagged methods still run. Tag the class.
- **`@NetworkAvailable` is not an exclusion.** It skips only when the host is *unreachable*. A reachable host that rejects the request — an expired API key, a 400, a 403 — runs the test and fails it. Jenkins build #4 reported 23 such errors that read as code failures; the cause was a revoked NCBI key.

The JUnit 4 vintage path is gone: `@Category` appears in **zero** test files. `ubic.gemma.core.util.test.category.SlowTest` / `IntegrationTest` still exist but are unreferenced and can be deleted. Do not add `-Dgroups=SlowTest` / `-DexcludedGroups=SlowTest` to any invocation — those name a category nothing carries, so the first silently selects no tests and the second silently *replaces* the real `network,slow` default. Both were live in `.jenkins/Jenkinsfile` until 3cdd5a1975.

Selecting slow tests needs both halves: `-Dgroups=slow -DexcludedGroups=network`. On the JUnit Platform an exclude filter beats an include filter, and `slow` is in the default exclusion list, so `-Dgroups=slow` alone matches nothing.

The diagnostic ladder for moving a test off the default-run network/env path:

1. **OS/binary-bound** → `@EnabledOnOs(OS.LINUX)` or `assumeThat(binary).exists()` skip-guard.
2. **External network** → cache the fetched payload as a classpath fixture under `src/test/resources/data/.../<accession>_<artifact>.<ext>`, rewrite the loader to PREFER classpath with NETWORK FALLBACK, and `@Tag("integration")` any method that ALSO downloads non-cacheable data.
3. **Large payload (ontology OWL, h5ad, MEX)** → use a format-aware tool that preserves file validity:
   - OWL → **ROBOT** (`robot extract --method STAR --term-file ... --output ...`) or Protégé module extraction. Never hand-edit OWL with `sed`.
   - h5ad → Python `anndata` slicing.
   - MEX → trim `matrix.mtx` rows; keep `features.tsv` / `barcodes.tsv` first-N.
   - SOFT / GEO metadata → already small; cache verbatim.
4. **Heavy in-JVM setup** → ask whether `@ContextConfiguration` is really needed. Direct construction in `@BeforeEach` dodges the spring-test 6.2 `MockitoResetTestExecutionListener` pre-init trap on `AbstractAsyncFactoryBean` beans (see `HomologeneServiceTest`).
5. **Real concurrency race** → don't `Thread.sleep` your way out. Reproduce + fix in production code; add a regression guard that doesn't depend on timing.

Always preserve the over-the-wire variant as `@Tag("integration")` (or `@Tag("network")` for cheap reachability probes) — never silently lose coverage.

### JUnit framework

- **JUnit 5 (Jupiter)** is the target. The legacy JUnit 4 base-test chain (`BaseIntegrationTest` / `BaseSpringContextTest` / `AbstractGeoServiceTest`) has been retired (commit `bcabc50567`). Use the `*5` forks: `BaseTest5`, `BaseIntegrationTest5`, `BaseSpringContextTest5`, `BaseDatabaseTest5`, `BaseCliTest5`, `AbstractGeoServiceTest5`.
- `BaseDatabaseTest5` disables L2 cache — assertions about L2-amplified cache-staleness CANNOT be reproduced through this path. Write a regression guard that pins the cross-session-reload invariants instead; the L2 amplifier is downstream.
- Network gating via `@ExtendWith(NetworkAvailableExtension.class)` + `@NetworkAvailable(url = "...")` at the class or method level.

## Performance testing

Perf is treated as a first-class regression target — the `mvn verify` suite catches behavioural breaks, but slow responses against the real prod-shape database have to be measured against a running instance.

### Reusable perf probe — `scripts/perf_search.py`

Single Python file, no extra deps. Hits a configurable base URL (default `frink:8080`), authenticates via macOS Keychain (`GEMMA_USERNAME` / `GEMMA_PASSWORD` → POST `/rest/v2/login` → Bearer token), runs a fixed matrix of queries against every search-adjacent endpoint we've tuned, reports min/p50/p95/max per case grouped by endpoint.

```bash
scripts/perf_search.py                            # default frink, 3 runs, stdout
scripts/perf_search.py --runs 5 --out perf-$(date +%Y%m%d).md
scripts/perf_search.py --evict --only annotations # response-cache busted per probe
scripts/perf_search.py --base http://localhost:8080 --anonymous
```

Re-run after every perf-touching commit lands on frink. Targets covered: `/genes/search`, `/annotations/search`, `/goTerms/{id}/genes` + `/genes/count`, `/datasets`, `/datasets/{id}/expressions/differential`. Adding a new endpoint to the matrix is one entry in the `*_cases()` builder.

### Hotspot identification — pattern

When a perf probe lands a slow case, surface where time goes before guessing at fixes. Three layered tools:

1. **CompositeSearchSource per-source log** (already emitted at WARN). For Gene / annotation searches it breaks down wall time across `DatabaseSearchSource`, `HibernateSearchSource`, `GeneOntologySearchSource`, `OntologySearchSource` — you immediately see whether the DB leg, Lucene, or a GO subtree walk owns the latency.
2. **Per-phase StopWatch + threshold log** on a hot endpoint. `AnnotationsWebService.getTerms` emits one INFO line when `total > 1000ms` with `find/filter/counts/rank/topCounts/enrich` ms breakdown; ditto `ProcessedExpressionDataVectorServiceImpl.getExpressionLevelsDiffEx`. Same pattern: cheap to leave on, surfaces regressions automatically, no temp-commit logging needed.
3. **DAO-side `Diff ex results: Nms` / `Fetched N vectors in Nms` warnings**. Already in place for the diffex / vector-cache hot paths; mirror that style when adding a new DAO method that could become a hotspot.

### What to fix vs. what to cache

- **Sub-100 ms responses** — don't bother caching; the response cache is a poison surface (transient empties pin the UI). See `AnnotationsWebService.SEARCH_CACHE` — only non-empty results are cached, and an admin POST endpoint exists to flush it.
- **100-500 ms** — fix the hotspot if it's a clean win (parallel fan-out, batched IN-clause, skip-when-unneeded). Cache as a backstop if the fix is structural.
- **>1s** — must have a structural fix. Caching alone is not acceptable because the first-hit user pays.

### Caches go through the unified admin endpoint

There's exactly one cache-eviction endpoint surface: `AdminWebService` at `GET /admin/caches` (list with hit/miss stats), `DELETE /admin/caches` (flush all), `DELETE /admin/caches/{cacheName}` (flush one). Browser admin views drive that.

A new in-process cache becomes admin-evictable by:
1. Registering it in `EhcacheConfig#APP_CACHES` with a `CacheSpec(maxEntries, ttl)`.
2. Resolving via `cacheManager.getCache(NAME)` at the use site (lazy field, since the CacheManager isn't available at constructor time on some Spring boot orderings).

Don't add per-endpoint bespoke `POST /foo/cache/evict` handlers — they duplicate `/admin/caches/{name}` and bypass the stats / unified eviction view. We retired `POST /annotations/search/cache/evict` and `POST /goTerms/cache/evict` for exactly this reason.

### Performance is critical

If something is slow, fix it. Re-engineer if necessary — caching to hide bad code is a temporary solution, not a permanent one.

## Feature workflow

- **Scope minimal.** Don't add features, refactor, or introduce abstractions beyond what the task requires. A bug fix doesn't need surrounding cleanup. Don't design for hypothetical future requirements.
- **No half-finished implementations.** If a change requires a multi-commit landing (refactor + migration + cleanup), each commit must compile and pass focused tests. The merge ordering of feature commits matters for the `git log` story.
- **Service decomposition.** Several services have been split into `*ReadService` + `*WriteService` (see `feature.gsec.absorption` chain). Tests that depend on the split need their `@TestComponent` `@Configuration` updated with the new `*ReadService` mock beans — context init fails otherwise. The template is commit `17dfbd208c`.
- **Audit migration (Phase C).** Three new annotations replace imperative `auditTrailService.addUpdateEvent(...)` calls:
  - `@Audited(value, message?, messageSpel?)` — `@AfterReturning`; fires on every successful return.
  - `@AuditedConditional(value, when=spel, ...)` — `@AfterReturning`; fires only when the SpEL predicate is true.
  - `@AuditedOnError(value, exception, messageSpel?)` — `@AfterThrowing`; fires on catch-block emission, routes through `REQUIRES_NEW` so the row survives wrapping rollback. Repeatable (`@AuditedOnErrors` container) with most-specific-instanceof dispatch.
  - The first `Auditable` argument is the target. SpEL has access to parameters by name (the `-parameters` compile flag is on project-wide), `#result`, `#exception`.
  - Aspects do NOT fire on private or self-invoked methods. To migrate such a callsite, hoist into a co-bean (`*HelperServiceImpl` precedent) and call through the bean.
  - In AOP-less test contexts (`*ServiceTest` wiring the impl directly with no proxy), `verify(auditTrailService).addUpdateEvent(...)` lines become stale after migration — drop them; aspect coverage lives in `AuditedAspectTest`.
  - Plan in `docs/audit/AUDIT_ADVICE_RETIREMENT_PLAN.md`.
- **No `Co-Authored-By: Claude`** in commit messages on this repo. Overrides global default.
- **Don't use the phrase "load-bearing"** in commit messages, comments, or memory notes — it has become a crutch. Pick a specific word.

## Pitfalls (don't re-learn these)

- **`AbstractAsyncFactoryBean` + spring-test 6.2 init trap** — registering one as a `@Bean` in a JUnit 5 Spring test context forces async init BEFORE `@BeforeEach`, breaking any setter that requires `!isInitialized()`. Workaround: drop `@ContextConfiguration`, construct directly in `@BeforeEach`, call `factory.destroy()` in `@AfterEach`. See `HomologeneServiceTest` 2026-05-20 (commit `139ea7a388`).
- **Hibernate Search 7 `directory.root` placeholder coercion** — if `gemma.search.dir` resolves to blank / `${...}`-leftover / a relative path, HS 7 writes per-entity Lucene index directories at the CWD (gemma-core's own dir). `HibernateConfig.resolveSearchIndexBase` coerces to `${java.io.tmpdir}/gemmaData/searchIndices`. See commit `04d720c666`.
- **Sandbox blocks credentialed destructive DB ops** even with explicit user chat authorization. The user must run `DROP DATABASE gemdtest; CREATE DATABASE gemdtest;` via `! <command>` in the prompt.
- **Never run a single test via `mvn surefire:test`** (or `failsafe:integration-test`, or any other direct goal invocation) — use `mvn test -Dtest=…` / `mvn verify -Dtest=…`. `testJvmOptions` (`pom.xml` ~1744) contains `-javaagent:${org.mockito:mockito-core:jar}`, and that placeholder is resolved by `maven-dependency-plugin:properties` bound to **`process-test-classes`** (`pom.xml` ~1075-1077); surefire and failsafe both consume it through `<argLine>` (`pom.xml` ~1095 / ~1123). A direct goal invocation runs no lifecycle phase, so the placeholder stays literal, the forked JVM is handed a bogus agent path, and it dies during VM init. The reported error hides the cause — you get `Tests run: 0` plus `The forked VM terminated without properly saying goodbye. VM crash or System.exit called?`, which reads like a JVM or test bug; the actual line is `Error opening zip file or JAR manifest missing : ${org.mockito:mockito-core:jar}` further up. Going through the lifecycle costs a recompile pass, which is near-free when nothing changed.
- **Entity `hashCode()` + mixed persisted/transient collections.** Gemma codebases often hold both saved (`id != null`) and transient (`id == null`) instances of the same entity in a single `Set<...>` — e.g. building up FactorValues for an EE, Characteristics for a sample, ticket targets, etc. The naive `hashCode()` patterns all have failure modes here:
  - **`getId()`-based** — id flips from null → value on persist. An entity added to a HashSet while transient ends up under the wrong hash bucket post-save; `set.contains(entity)` returns `false` even though the entity is in the set. This is the canonical "Hibernate hashCode footgun" Guillaume flagged on PR #1659.
  - **Business-key-based** (`hash(accession)`, `hash(ncbiId)` etc.) — same bug class if the key can be null/mutated after the object is added to a Set. Safer than id-based ONLY when the key is set at construction and never changes.
  - **Mixing equals strategies** (equals by id when both have ids, else by business key) with a hashCode that picks ONE strategy — silently violates the equals/hashCode contract whenever a transient and a persisted instance with the same business key collide.
  When in doubt, the bulletproof pattern is a constant hashCode (`return getClass().hashCode();`) plus the existing id-or-business-key equals — never wrong, just degrades a single bucket's lookup to O(n). For < ~200-entity collections that's invisible. If you need real hash distribution, the business key must be set at construction-time and immutable thereafter (enforce via factory + no setter). PR #1659's `PreboardedExperiment.Factory.newInstance(source, accession)` is the pattern to copy. See `PreboardedExperiment` (compat-preboarded-readside branch) vs phase2's `PreboardedExperiment` for the contrast.

## Reference docs

🛑 **New recces, figures and working design docs go in the EVAL repo**
(`~/Dev/gemma-curation-agents-eval`), not here (Paul, 2026-08-28). This repo tracks code.

The existing `docs/` tree (152 docs, see `docs/INDEX.md`) stays where it is and remains the
reference for anything already written; only `README.md` and this file sit at the repo root.
Correct a doc in place if it is already here — the rule is about what gets ADDED.

- `docs/audit/AUDIT_SYSTEM_AUDIT.md` — full audit-system architecture + migration phases.
- `docs/recce/AUDIT_PHASE_C_RECCE.md` — bucket-by-bucket migration inventory.
- `docs/audit/AUDIT_ADVICE_RETIREMENT_PLAN.md` — terminal step for Phase C.
- `docs/audit/HIBERNATE6_CASCADE_AUDIT.md` — HB6 upgrade cascade audit.

## Branch context

Working branch is typically `phase2-acl-migrate` (Gemma 2.0 release target). Three gates in flight: `hotfix-1.32.7` minor release, dev → phase2-acl-migrate catch-up merge, then ship phase2-acl-migrate as Gemma 2.0. See user memory `project_release_plan.md`.
