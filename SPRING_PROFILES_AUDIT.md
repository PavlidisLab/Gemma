# Spring Profiles Audit (Phase 3 — Spring 6 modernization)

**Branch:** `worktree-spring-profiles-audit`
**Baseline:** `08e760bdaf` (off `phase2-acl-migrate`)
**Date:** 2026-05-18
**Mode:** Recce / documentation only — no substantive code changes.

## 0. TL;DR

- **9 live profile names** plus **1 dead profile** (`testing`) referenced only from a deprecated path.
- Profile model is well-defined: `EnvironmentProfiles.{PRODUCTION,DEV,TEST}` are mutually exclusive and enforced at runtime in `SpringContextUtils.prepareContext()`. All other profiles (`testdb`, `web`, `cli`, `scheduler`, `metrics`, `profiling`) are orthogonal "feature" profiles layered on top.
- No casing/spelling inconsistencies. No `prod` vs `production` drift. No typo'd `@Profile` strings.
- One real risk: **`testing` is a dead profile** still referenced from the deprecated `SpringContextUtils.getApplicationContext(boolean testing, ...)` overload (line 91). No `@Profile("testing")` or `profile="testing"` declarations exist anywhere — anything calling that path through with `testing=true` activates a profile that wires nothing.
- A handful of profile literals are still string-typed (`"cli"`, `"web"`, `"scheduler"`, `"metrics"`, `"testdb"`, `"profiling"`) — `EnvironmentProfiles` only enumerates the three environment profiles. A small follow-up: promote the orthogonal feature profiles into the same constants class to eliminate string-literal sprawl.

## 1. Profile matrix

| profile | activation source(s) | wires (configs / beans) | excludes |
|---|---|---|---|
| **`production`** | `-Dspring.profiles.active=production` (deploy time); production Tomcat | `applicationContext-dataSource.xml` (real MySQL `dataSource`, `groupAgentSecurityContext` via prod creds, `JavaMailSenderImpl`) | `mailSender=DummyMailSender` (excluded by `!production`); XML schema validation disabled (`SpringContextUtils:134`) |
| **`dev`** | IntelliJ run configs (`Gemma_Web.xml`, `Gemma_CLI.xml`); `SpringContextUtils.prepareContext` fallback when no env profile is active | Same `dataSource` block as `production` (shares `profile="production,dev"`); enables dev-only swagger UI (`OpenApiConfig`), dev exception traces (`ExceptionTag`), dev static asset resolution (`StaticAssetResolver`), dev ontology external links (`OntologyConfig.ontologyExternalLinks`); falls under `!production` so `DummyMailSender` is wired | – |
| **`test`** | `BaseTest`, `BaseDatabaseTest`, `BaseJerseyTest`, `SettingsConfigTest` (via `@ActiveProfiles(EnvironmentProfiles.TEST)`); `SpringContextUtilsTest` programmatic | `applicationContext-dataSource.xml` test branch (`profile="test,testdb"` — test creds, test `dataSource`); `applicationContext-dataSourceInitializer.xml` (`createDatabaseInitializer`, `dataSourceInitializer` for the legacy MySQL integration-test path); `TestOntologyConfig` (no-op test ontologies) | `OntologyConfig` (excluded by `@Profile("!test")`) — tests get `TestOntologyConfig` instead. Falls under `!production` so `DummyMailSender` is wired |
| **`testdb`** | CLI flag `--testdb` (→ `GemmaCLI.java:213`); IntelliJ `Generate_testdb.xml` / `Update_testdb.xml` run configs (`dev,testdb`) | `InitializeDatabaseCli` (`@Profile("testdb")`), `UpdateDatabaseCli` (`@Profile("testdb")`); shares the `test,testdb` block in `applicationContext-dataSource.xml` (test creds). `AbstractAuthenticatedCLI` switches to `GEMMA_TESTDB_*` env vars when `acceptsProfiles("test","testdb")`. **Note**: `testdb` is **not** an env profile (`PRODUCTION/DEV/TEST`); combined with `dev` per CLI run configs | – |
| **`web`** | `InitializeContext.activateWebProfile()` (programmatic, in Tomcat); `BaseJerseyTest`, `BaseWebTest`, `BaseWebIntegrationTest` test base classes | `applicationContext-analytics.xml` (GA4 provider + client/user id strategies, only for `web`) | – |
| **`cli`** | `GemmaCLI.java:210` programmatic (`profiles.add("cli")`); `BaseCliTest`, `BaseCliIntegrationTest` test bases | `applicationContext-component-scan.xml` (component-scans `ubic.gemma.cli`, `ubic.gemma.apps`, `ubic.gemma.contrib.apps` with `PrototypeScopeResolver`); `LazyInitByDefaultPostProcessor`; `ConfigurationLinter` (`@Profile("cli")`); CLI-level metric tag binder `AbstractCLI:538` | – |
| **`scheduler`** | `SpringContextUtils.prepareContext`: auto-activates if `Settings.getBoolean("quartzOn") == true` (`default.properties:133` ships `quartzOn=false`); also accepts explicit `-Dspring.profiles.active=...,scheduler` | `applicationContext-schedule.xml` — `SchedulerFactoryBean` + 10 Quartz cron triggers (batchInfo, EE-report, AD-report, whatsNew, gene2Cs, ee2c × 3, ee2ad, indexExperiments) | – |
| **`metrics`** | Explicit `-Dspring.profiles.active=...,metrics` only (no programmatic activation) | `applicationContext-metrics.xml` (web metrics configurer); `applicationContext-serviceBeans.xml` nested `<beans profile="metrics">` (JMX `meterRegistry`, `GenericMeterRegistryConfigurer` with JVM/log/Hibernate/Hikari/TaskExecutor binders, `MeterRegistryEhcacheConfigurer`, `TimedAspect` + AOP autoproxy); `MeterRegistryCliConfigurer` (CLI-side environment+user tags) | – |
| **`profiling`** | CLI flag `--profiling` (`GemmaCLI.java:217`); explicit `-Dspring.profiles.active=...,profiling` | `ProfilingConfig` (@Configuration): `BeanInitializationTimeMonitor` + a `ContextRefreshedEvent` listener that logs slow-bean breakdowns | – |
| **~~`testing`~~** | (dead) only referenced by deprecated `SpringContextUtils.getApplicationContext(boolean testing, …)` overload (line 91) | **nothing** — no `@Profile("testing")` or `profile="testing"` exists | – |

## 2. Test profile activation by base class

| Base class | `@ActiveProfiles` | Notes |
|---|---|---|
| `BaseTest` (gemma-core) | `EnvironmentProfiles.TEST` = `"test"` | Root of every test-context test |
| `BaseDatabaseTest` (gemma-core) | `EnvironmentProfiles.TEST` | Inherits AbstractTransactionalJUnit4… ; defines `BaseDatabaseTestContextConfiguration` with H2/Flyway |
| `BaseIntegrationTest` (gemma-core) | inherits `test` from `BaseTest` | adds `@ContextConfiguration(locations="classpath*:ubic/gemma/applicationContext-*.xml")` |
| `SettingsConfigTest` | `EnvironmentProfiles.TEST` | one-off |
| `BaseCliTest` (gemma-cli) | `"cli"` + inherited `"test"` | extends `BaseTest` |
| `BaseCliIntegrationTest` (gemma-cli) | `"cli"` + inherited `"test"` | extends `BaseIntegrationTest` |
| `BaseWebTest` (gemma-web) | `"web"` + inherited `"test"` | extends `BaseTest`, `@WebAppConfiguration` |
| `BaseWebIntegrationTest` (gemma-web) | `"web"` + inherited `"test"` | extends `BaseTest`, web app config + servlet ctx |
| `BaseJerseyTest` (gemma-rest) | `{ "web", EnvironmentProfiles.TEST }` | does **not** extend `BaseTest` (cannot — extends `JerseyTest`); profile list declared inline |

Coverage by feature profile:
- `web` → covered (3 base classes use it).
- `cli` → covered (2 base classes use it).
- `testdb` → **no test coverage** (it's an operational profile for `InitializeDatabaseCli` / `UpdateDatabaseCli`, used outside tests).
- `scheduler` → **no test coverage** of the profile-gated XML. The `applicationContext-schedule.xml` cron triggers are never instantiated in tests.
- `metrics` → **no test coverage** of the `metrics`-profile branches.
- `profiling` → **no test coverage** (and unlikely to need any — it's a debug aid).
- `production` → never exercised in tests (correct; the env profile is enforced mutually exclusive).
- `dev` → never set via `@ActiveProfiles`; `SpringContextUtils.prepareContext` would fall back to `dev` if a test context loaded through the CLI bootstrap path, but the test infra goes through `@ContextConfiguration`/`@ActiveProfiles` instead, so `dev` is not auto-applied in test contexts.

## 3. Inconsistencies / risks

### R1. Dead `"testing"` profile string (legacy)
`gemma-core/src/main/java/ubic/gemma/core/context/SpringContextUtils.java:91` passes the literal `"testing"` to `addActiveProfile()` when the deprecated `getApplicationContext(boolean testing, …)` overload is called with `testing=true`. **Nothing wires off `"testing"`** — no `@Profile("testing")`, no XML `profile="testing"`. The deprecated method appears to be a hard-deprecated compatibility shim ("only kept for backward-compatibility with external scripts"). Risk: any external caller still hitting this path silently gets a context with no test data wiring (no `dataSource` test creds, no test ontologies, …). 

**Recommendation:** When the deprecated overload is finally removed, the `"testing"` literal goes with it. Until then, consider renaming to `EnvironmentProfiles.TEST` (`"test"`) in that line, or at least emitting a warning. Out of scope for this recce.

### R2. `EnvironmentProfiles` constants class is incomplete
`EnvironmentProfiles` declares only `PRODUCTION`, `DEV`, `TEST`. The other six live profile names (`testdb`, `web`, `cli`, `scheduler`, `metrics`, `profiling`) are still string literals scattered across `@Profile(...)`, `acceptsProfiles(...)`, XML, IDE run configs, and `GemmaCLI`. A typo in any of those literals would not be caught at compile time.

**Recommendation:** Promote the six feature profiles into the same class (e.g. `FeatureProfiles.METRICS`, `.SCHEDULER`, ...) — or rename `EnvironmentProfiles` to a single `Profiles` class with both groups. Then audit `acceptsProfiles("test", "testdb")` and `addActiveProfile("scheduler")` sites to use the constants. Saves future grep cycles.

### R3. `dataSource.xml` couples `test` and `testdb` under one block
`applicationContext-dataSource.xml:49` is `<beans profile="test,testdb">`. The two are conceptually different:
- `test` = the gemma-core test profile, used by base test classes against the H2/in-memory DB or the legacy MySQL test fixture.
- `testdb` = a CLI flag that runs Gemma against the test MySQL **from production code paths** (so `InitializeDatabaseCli` can create/drop it).

Sharing the bean definitions works today because both want the same `gemma.testdb.*` properties — but the semantic overlap is implicit. The IDE run configs use `dev,testdb` (not `test,testdb`), and `SpringContextUtils.prepareContext` would reject `test+dev` as two env profiles. So there's already an asymmetry: the activation patterns are `test` (alone) or `dev+testdb`, never `test+testdb`. The XML's `profile="test,testdb"` is a logical-OR of two scenarios that never coexist.

**Recommendation:** Low priority. Document the intent in a comment in the XML, or split into two `<beans>` blocks if/when the prod-MySQL test-fixture migration lands.

### R4. `scheduler` profile has a quiet auto-activation path
`SpringContextUtils.prepareContext()` lines 119-122 auto-activate `scheduler` if `quartzOn=true` in `Gemma.properties`. The code also warns *"You should add 'scheduler' to the active profiles instead"* — so this is known and accepted, but it's a second activation channel that wouldn't be obvious from grep alone. `default.properties` ships `quartzOn=false`, so this fires only in deployed production envs that flip it.

**Recommendation:** None. Behaviour is intentional and warned-about. Worth knowing during the Spring 6 bump.

### R5. `metrics` profile has zero test coverage
The `metrics` profile wires a fairly large bean graph (JMX `meterRegistry` + 8 binders + AOP TimedAspect + ehcache configurer + web/CLI configurers). None of that is exercised by any base test class. A future bump that breaks (say) the `Hibernate4Metrics` constructor signature against Hibernate 6 / Micrometer N would not be caught.

**Recommendation:** Add a `MetricsContextSmokeTest` that loads `applicationContext-*.xml` under `test,metrics` profiles and asserts the `meterRegistry` bean exists. (Phase 3 follow-up; not part of this recce.)

### R6. `scheduler` profile has zero test coverage
Same shape as R5. `applicationContext-schedule.xml` wires 10 Quartz cron-trigger beans referencing real services. A change in any of those service interfaces would break the Quartz wiring only at production startup. **Smoke test recommended.**

### R7. No declared exclusion between feature profiles
There's no runtime check that `cli` and `web` aren't both active (they shouldn't be — one's a Tomcat web app, one's a CLI), but nothing in `SpringContextUtils.prepareContext` enforces it. Same for `scheduler` in CLI mode (probably harmless since CLI runs are short-lived, but Quartz threads would still spin up). The env profiles (`PRODUCTION/DEV/TEST`) are guarded; the feature profiles are not.

**Recommendation:** Document the intended feature-profile combinations in `EnvironmentProfiles` / `SpringContextUtils` Javadoc. No code change needed unless an accidental combination is observed.

## 4. Recommendations summary

| # | Action | Priority |
|---|---|---|
| 1 | Promote `testdb`/`web`/`cli`/`scheduler`/`metrics`/`profiling` into a `FeatureProfiles` (or unified `Profiles`) constants class | medium |
| 2 | Add a smoke test loading the full XML graph under `test,metrics` | medium |
| 3 | Add a smoke test loading the full XML graph under `test,scheduler` (or stub `quartzOn=true` in test props) | medium |
| 4 | When removing `SpringContextUtils.getApplicationContext(boolean, ...)` deprecated overload, drop the `"testing"` literal | low (tracked with deprecation removal) |
| 5 | Split or comment the `profile="test,testdb"` block in `applicationContext-dataSource.xml` | low |
| 6 | Document intended feature-profile combinations in `SpringContextUtils` Javadoc | low |
| 7 | Audit `acceptsProfiles("test", "testdb")` / `addActiveProfile("scheduler")` sites once constants exist | follow-up to #1 |

## 5. Open questions

1. **Is `production` ever combined with `dev`?** The `<beans profile="production,dev">` block in `applicationContext-dataSource.xml` reads as "one OR the other" — Spring's profile selector treats the comma as OR. The mutual-exclusion check in `SpringContextUtils.prepareContext()` (line 124-131) enforces *exactly one* of {production, dev, test}, so they cannot both be active. Confirmed not-an-issue, but the XML comment "we use the same database for production and development" deserves a Javadoc cross-reference.
2. **Where does `web` get activated in non-Tomcat embedded scenarios?** Only `InitializeContext.activateWebProfile()` (Tomcat path) and the test base classes activate it. If anyone ever spins up a standalone Jetty / standalone Jersey via `SpringContextUtils.getApplicationContext`, they'd need to pass `"web"` explicitly. Documented in `BaseJerseyTest` and `InitializeContext`; just worth flagging for the future REST-standalone work.
3. **Should `cli` and `metrics` be combinable?** They are today (`MeterRegistryCliConfigurer` is `@Profile("metrics")` and uses `@Component` so the `cli` component scan picks it up). Just confirming the design: yes — CLI runs *can* opt into metrics via `-Dspring.profiles.active=dev,cli,metrics` (or equivalent). The IDE run configs don't do this, but it's wired correctly.

## 6. Files surveyed

Source-of-truth files:
- `gemma-core/src/main/java/ubic/gemma/core/context/EnvironmentProfiles.java`
- `gemma-core/src/main/java/ubic/gemma/core/context/SpringContextUtils.java`
- `gemma-web/src/main/java/ubic/gemma/web/context/InitializeContext.java`
- `gemma-cli/src/main/java/ubic/gemma/cli/main/GemmaCLI.java`
- `gemma-core/src/main/resources/ubic/gemma/applicationContext-dataSource.xml`
- `gemma-core/src/main/resources/ubic/gemma/applicationContext-schedule.xml`
- `gemma-core/src/main/resources/ubic/gemma/applicationContext-serviceBeans.xml`
- `gemma-rest/src/main/resources/ubic/gemma/applicationContext-analytics.xml`
- `gemma-web/src/main/resources/ubic/gemma/applicationContext-metrics.xml`
- `gemma-cli/src/main/resources/ubic/gemma/applicationContext-component-scan.xml`
- `gemma-core/src/test/resources/ubic/gemma/applicationContext-dataSourceInitializer.xml`
- `gemma-core/src/main/java/ubic/gemma/core/ontology/OntologyConfig.java`, `TestOntologyConfig.java`
- `gemma-core/src/main/java/ubic/gemma/core/profiling/ProfilingConfig.java`
- `gemma-cli/src/main/java/ubic/gemma/cli/metrics/MeterRegistryCliConfigurer.java`
- `gemma-cli/src/main/java/ubic/gemma/cli/config/ConfigurationLinter.java`
- `gemma-cli/src/main/java/ubic/gemma/apps/InitializeDatabaseCli.java`, `UpdateDatabaseCli.java`
- `gemma-cli/src/main/java/ubic/gemma/cli/util/AbstractAuthenticatedCLI.java`, `AbstractCLI.java`
- Base test classes (see §2)

IDE run configs surveyed: `.idea/runConfigurations/{Gemma_Web,Gemma_CLI,Generate_testdb,Update_testdb}.xml`.
