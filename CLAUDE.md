# Gemma — repo conventions for Claude Code

Ground rules for adding features and tests in this repo. Project-specific; supplements (does not replace) the user-global `~/.claude/CLAUDE.md`.

## Build

- **JDK 21** (amazon-corretto-21). Builds fail with cryptic enforcer errors on JDKs older than the source level. Set `JAVA_HOME` before invoking `mvn` from a fresh shell:
  ```bash
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- **`mvn verify` canonical invocation** (full IT pass against MySQL gemdtest):
  ```bash
  mvn -pl gemma-core verify \
      -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) \
      -Dgemma.hibernate.hbm2ddl.auto=create
  ```
- **gemdtest auto-reset** — `CreateDatabasePopulator` runs at test context startup (default `gemma.testdb.initialize=true`) and drops+recreates `gemdtest` before Flyway + Hibernate rebuild it. This requires the test user (`gemmatest`) to hold the server-level CREATE privilege on top of the database-scoped grant; one-time fix:
  ```sql
  -- Run once as root; lets `gemmatest` recreate gemdtest after the populator drops it
  GRANT CREATE ON *.* TO 'gemmatest'@'localhost';
  FLUSH PRIVILEGES;
  ```
  Without this grant, `mvn verify` fails on the CREATE DATABASE step inside the populator and you have to drop/recreate manually via root (the legacy procedure below).
- **Manual schema reset (fallback only)** — if the populator can't run (e.g. test user grants missing, or the DB is wedged in a state Flyway can't reconcile), drop+recreate `gemdtest` from a root session. The sandbox blocks credentialed destructive DB ops, so the user runs it via `!` in the prompt:
  ```bash
  mysql -h 127.0.0.1 -uroot -p$(security find-generic-password -s mysql-root -w) \
      -e 'DROP DATABASE IF EXISTS gemdtest; CREATE DATABASE gemdtest;'
  ```
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
  - Plan in `AUDIT_ADVICE_RETIREMENT_PLAN.md` (repo root).
- **No `Co-Authored-By: Claude`** in commit messages on this repo. Overrides global default.
- **Don't use the phrase "load-bearing"** in commit messages, comments, or memory notes — it has become a crutch. Pick a specific word.

## Pitfalls (don't re-learn these)

- **`AbstractAsyncFactoryBean` + spring-test 6.2 init trap** — registering one as a `@Bean` in a JUnit 5 Spring test context forces async init BEFORE `@BeforeEach`, breaking any setter that requires `!isInitialized()`. Workaround: drop `@ContextConfiguration`, construct directly in `@BeforeEach`, call `factory.destroy()` in `@AfterEach`. See `HomologeneServiceTest` 2026-05-20 (commit `139ea7a388`).
- **Hibernate Search 7 `directory.root` placeholder coercion** — if `gemma.search.dir` resolves to blank / `${...}`-leftover / a relative path, HS 7 writes per-entity Lucene index directories at the CWD (gemma-core's own dir). `HibernateConfig.resolveSearchIndexBase` coerces to `${java.io.tmpdir}/gemmaData/searchIndices`. See commit `04d720c666`.
- **Sandbox blocks credentialed destructive DB ops** even with explicit user chat authorization. The user must run `DROP DATABASE gemdtest; CREATE DATABASE gemdtest;` via `! <command>` in the prompt.

## Reference docs

- `AUDIT_SYSTEM_AUDIT.md` — full audit-system architecture + migration phases.
- `AUDIT_PHASE_C_RECCE.md` — bucket-by-bucket migration inventory.
- `AUDIT_ADVICE_RETIREMENT_PLAN.md` — terminal step for Phase C.
- `HIBERNATE6_CASCADE_AUDIT.md` — HB6 upgrade cascade audit.

## Branch context

Working branch is typically `phase2-acl-migrate` (Gemma 2.0 release target). Three gates in flight: `hotfix-1.32.7` minor release, dev → phase2-acl-migrate catch-up merge, then ship phase2-acl-migrate as Gemma 2.0. See user memory `project_release_plan.md`.
