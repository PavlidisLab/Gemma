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

Anything slow / network-bound / env-dependent stays in the codebase but is *tagged* so it doesn't fire by default. Surefire's `excludedGroups` (parent `pom.xml` line ~1121) already excludes `@Tag("integration")`.

**Tag taxonomy** (the `excludedGroups` default at the bottom of parent `pom.xml` and the failsafe `<excludedGroups>` both consume `${excludedGroups}` = `network,slow,ubic.gemma.core.util.test.category.SlowTest`):

- `@Tag("integration")` / `@Category(IntegrationTest.class)` — runs in failsafe only, skipped from surefire. Day-to-day `mvn verify` runs these.
- `@Tag("network")` — cheap external-URL reachability probe. Excluded by default; opt-in with `-DexcludedGroups=` (run everything) or a different list.
- `@Tag("slow")` / `@Category(SlowTest.class)` — heavy in-JVM work or large external download (GEO archives, Uberon OWL, UCSC matrices, BLAT alignments, Python subprocesses, etc.). Excluded from BOTH surefire and failsafe by default. Run explicitly with `mvn verify -DexcludedGroups=network` (keep the network exclusion, drop slow) or `-DexcludedGroups=` (clear everything).

When you add a tag, prefer to pair Jupiter `@Tag("slow")` with the JUnit 4 `@Category(SlowTest.class)` (the vintage exposure path) so both engines see it consistently.

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

Working design docs / recces / audits live under `docs/` (see `docs/INDEX.md`); only `README.md`
and this file stay at the repo root.

- `docs/audit/AUDIT_SYSTEM_AUDIT.md` — full audit-system architecture + migration phases.
- `docs/recce/AUDIT_PHASE_C_RECCE.md` — bucket-by-bucket migration inventory.
- `docs/audit/AUDIT_ADVICE_RETIREMENT_PLAN.md` — terminal step for Phase C.
- `docs/audit/HIBERNATE6_CASCADE_AUDIT.md` — HB6 upgrade cascade audit.

## Branch context

Working branch is typically `phase2-acl-migrate` (Gemma 2.0 release target). Three gates in flight: `hotfix-1.32.7` minor release, dev → phase2-acl-migrate catch-up merge, then ship phase2-acl-migrate as Gemma 2.0. See user memory `project_release_plan.md`.
