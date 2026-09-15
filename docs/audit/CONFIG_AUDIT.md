# Config Audit — Spring Profiles, `@Value`, Property Files

**Branch**: `phase2-acl-migrate` (HEAD `29b21206c2`)
**Date**: 2026-05-19
**Scope**: `gemma-*/src/main/{java,resources}` (production code only — tests, target/, and `.claude/worktrees/` ignored throughout).
**Purpose**: Phase 3 recce — inventory current config surface before deciding what 12-factor / containerization fixes to make. **No code changed.**

---

## 1. `@Value` injection sites

**Total**: 162 `@Value(...)` occurrences in `gemma-*/src/main/java`.

| Module       | Count |
|--------------|------:|
| `gemma-core` |    93 |
| `gemma-cli`  |    30 |
| `gemma-web`  |    23 |
| `gemma-rest` |    16 |

### 1.1 Grouped by property prefix (top groups)

Prefix uses the first two dotted segments of the placeholder key. The total below counts placeholder occurrences (a single `@Value` may use multiple placeholders in a SpEL expression; one site is counted per placeholder substring).

| Prefix                       | Sites | Read from                              |
|------------------------------|------:|----------------------------------------|
| `entrez.efetch.*`            |    14 | `project.properties`                   |
| `gemma.hosturl`              |    12 | `default.properties`                   |
| `gemma.appdata.*`            |    12 | `default.properties`                   |
| `gemma.testdb.*`             |     8 | `Gemma.properties` (no default — set per env) |
| `load.ontologies` / `load.*` |    9  | `default.properties`                   |
| `gemma.staticAssetServer.*`  |     6 | `default.properties`                   |
| `gemma.ontology.*`           |     6 | `default.properties`                   |
| `gemma.hibernate.*`          |     6 | `default.properties` + `hibernate.properties` |
| `gemma.download.*`           |     5 | `default.properties`                   |
| `gemma.db.*`                 |     5 | `default.properties` (user override mandatory) |
| `cellxgene.local.*`          |     4 | `project.properties`                   |
| `geo.local.*` / `geo.*`      |     7 | `project.properties` (+ a few in `default.properties`) |
| `gemma.support.*`            |     3 | `default.properties`                   |
| `gemma.runas.*` / `gemma.agent.*` | 4 | `default.properties` (creds → require override) |
| `gemma.cellBrowser.*`        |     2 | `default.properties`                   |
| `gemma.cache.*`              |     2 | `default.properties`                   |
| `gemma.build.*`              |     2 | manifest (`ManifestUtils`)             |
| `gemma.expressionDataFileTasks.*` | 2 | `default.properties`                  |
| `gemma.fastq.*`              |     2 | `default.properties`                   |
| `gemma.cache.*`              |     2 | `default.properties`                   |
| `gemma.admin.*`              |     2 | `default.properties`                   |
| `mail.*`                     |     4 | `default.properties` (placeholders only — real creds in user override) |
| `ncbi.*`                     |     4 | `project.properties`                   |
| `arrayExpress.*`             |     3 | `project.properties`                   |
| `ga.*`                       |     3 | `default.properties`                   |
| `tomcat.sendfile.*`          |     2 | `default.properties`                   |
| `repeatMasker.exe`, `python.exe`, `npm.exe`, `fastaCmd.exe`, `cellranger.dir`, `tgfvo.path` | 7 | `default.properties` (tool paths) |
| `gemma.version`              |     1 | manifest (fallback `null`)             |
| singleton `gemma.*` keys     |    ~25 | `default.properties` (one site each)  |

Singleton `gemma.*` keys (one occurrence each) — `gemma.transaction.maxretries`, `gemma.scratch.dir`, `gemma.recaptcha.privateKey`, `gemma.noreply.email`, `gemma.metrics.scrapeToken`, `gemma.localTasks.corePoolSize`, `gemma.javascript.log`, `gemma.health.diskSpace.thresholdBytes`, `gemma.goldenpath.db.rat`, `gemma.gene2cs.path`, `gemma.gemBrow.url`, `gemma.externalDatabases.featured`, `gemma.backgroundTasks.numberOfThreads`, `gemma.analysis.dir`, `gemma.allow.new.probes.onexisting.platforms`.

### 1.2 Defect found

`gemma-cli/src/main/java/ubic/gemma/apps/UpdatePubMedCli.java:57` — malformed placeholder, missing closing brace:

```java
@Value("${entrez.efetch.apikey")   // <-- never substitutes; injects literal string
private String ncbiApiKey;
```

Probably silent in production (CLI not run regularly without a key) but should be a LOW-priority follow-up fix.

---

## 2. Spring profiles in use

**Total unique profile names**: 8 — `production`, `dev`, `test`, `testdb`, `cli`, `web`, `scheduler`, `metrics`, `profiling`. (Constants in `gemma-core/src/main/java/ubic/gemma/core/context/EnvironmentProfiles.java`.)

Profiles split into two semantic groups per `EnvironmentProfiles` javadoc:
- **Mutually-exclusive environment**: `production`, `dev`, `test`. Exactly one must be active.
- **Independent feature toggles**: `web`, `cli`, `scheduler`, `metrics`, `profiling`. Any combination.
- `testdb` is an additional non-mutually-exclusive variant of `test` used for Flyway baseline / migration verification (`InitializeDatabaseCli`, `UpdateDatabaseCli`).

### 2.1 Bean / config gating by profile

| Profile        | Where used                                                                                                                                                                                                                                                                                                                                                                              |
|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `production`   | `DataSourceConfig.dataSource` (Hikari → MySQL via `gemma.db.*`), `groupAgentSecurityContext` (real creds), `mailSender` (real `JavaMailSenderImpl`), `createDatabaseInitializerStub` / `dataSourceInitializerStub` (stubs for hibernate `depends-on`).                                                                                                                                  |
| `dev`          | Same as `production` for `dataSource`, `groupAgentSecurityContext`, stubs (points at prod schema for local-against-prod debugging; only pool size differs via `gemma.db.*` overrides).                                                                                                                                                                                                  |
| `test`         | `DataSourceConfig.testDataSource` (Hikari + `gemma.testdb.*` + leak detection 60s), `testGroupAgentSecurityContext` (`gemma.testdb.agent.*`).                                                                                                                                                                                                                                            |
| `testdb`       | Same bindings as `test`; additionally activates `InitializeDatabaseCli`, `UpdateDatabaseCli`.                                                                                                                                                                                                                                                                                            |
| `!production`  | `DataSourceConfig.dummyMailSender` (`DummyMailSender`; warns via log).                                                                                                                                                                                                                                                                                                                  |
| `!test`        | `OntologyConfig` (ontology stack disabled under `test` because tests use a different set).                                                                                                                                                                                                                                                                                              |
| `!test & !testdb` | Documented but not on a live annotation — referenced in `DataSourceConfig` javadoc only.                                                                                                                                                                                                                                                                                              |
| `cli`          | `CliComponentScanConfig` (CLI bean scan root) — also belt-and-suspenders on `<beans profile="cli">` in `gemma-cli/.../applicationContext-component-scan.xml`. `ConfigurationLinter` (CLI startup warnings).                                                                                                                                                                              |
| `web`          | `AnalyticsConfig` (`gemma-rest/.../analytics/ga4/AnalyticsConfig.java`) — and `<beans profile="web">` on `gemma-rest/.../applicationContext-analytics.xml`. Activated programmatically in `gemma-web/.../InitializeContext.java:45` (`cac.getEnvironment().addActiveProfile("web")`).                                                                                                    |
| `scheduler`    | `SchedulerConfig` (Quartz). Auto-activated when `quartzOn=true` in `SpringContextUtils` (see §3).                                                                                                                                                                                                                                                                                        |
| `metrics`      | `MetricsConfig` (Micrometer registry). Also `<beans profile="metrics">` on `gemma-web/.../applicationContext-metrics.xml`. CLI variant: `MeterRegistryCliConfigurer` (`@Profile(EnvironmentProfiles.METRICS)`).                                                                                                                                                                          |
| `profiling`    | `ProfilingConfig` (`@Profile(EnvironmentProfiles.PROFILING)`). Activated via CLI flag (see §3).                                                                                                                                                                                                                                                                                          |

### 2.2 XML `profile=` attribute (legacy form)

Three XML files still carry `profile="..."` on `<beans>` — all kept as belt-and-suspenders alongside `@Profile` on the `@Configuration` class per the in-file comments:

- `gemma-cli/src/main/resources/ubic/gemma/applicationContext-component-scan.xml` → `profile="cli"`
- `gemma-rest/src/main/resources/ubic/gemma/applicationContext-analytics.xml` → `profile="web"`
- `gemma-web/src/main/resources/ubic/gemma/applicationContext-metrics.xml` → `profile="metrics"`

---

## 3. Profile auto-activation logic

### 3.1 `SpringContextUtils.prepareContext` (`gemma-core/.../context/SpringContextUtils.java`)

Canonical post-load hook called by all bootstrappers. Logic (lines 100–143):

1. If **no** environment profile (`production` / `dev` / `test`) is active → log a warning and **default to `dev`** ("Use `-Dspring.profiles.active=dev` explicitly to remove this warning").
2. If `Settings.getBoolean("quartzOn")` is true and `scheduler` is **not** active → auto-add `scheduler` (log: "you should add `scheduler` to the active profiles instead").
3. Invariant check: exactly one of `production` / `dev` / `test` must be active — `IllegalStateException` otherwise.
4. Logs the final active-profile list at startup.

### 3.2 `InitializeContext` (`gemma-web/.../web/context/InitializeContext.java`)

`ApplicationContextInitializer` for the web app. Hardcodes `cac.getEnvironment().addActiveProfile("web")` in `activateWebProfile()`, then delegates to `SpringContextUtils.prepareContext`. The remainder loads servlet-context attributes from `Settings`.

### 3.3 `GemmaCLI.main` (`gemma-cli/.../main/GemmaCLI.java:209–221`)

```java
List<String> profiles = new ArrayList<>();
profiles.add( "cli" );
if ( commandLine.hasOption( TESTDB_OPTION ) )    profiles.add( "testdb" );
if ( commandLine.hasOption( PROFILING_OPTION ) ) profiles.add( "profiling" );
ctx = SpringContextUtils.getApplicationContext( profiles.toArray( new String[0] ) );
```

Activates `cli` unconditionally; `testdb` / `profiling` via CLI flags. The environment profile (`production` / `dev`) must come in via `-Dspring.profiles.active=...` or it falls back to `dev` per §3.1.

### 3.4 Other env consumption (System properties / `System.getenv`)

| Site                                              | Variable                       | Used for                                            |
|---------------------------------------------------|--------------------------------|-----------------------------------------------------|
| `SettingsConfig:125`                              | `gemma.config` (system prop)   | Optional path to override `Gemma.properties`        |
| `SettingsConfig:141`                              | `CATALINA_BASE` (env)          | Tomcat-managed `Gemma.properties` lookup            |
| `SettingsConfig:154`                              | `user.home` (system prop)      | `$HOME/Gemma.properties` fallback                   |
| `GemmaCLI:59`                                     | `gemma.log.dir` (system prop)  | Log4j config                                        |
| `GemmaCLI:324,326`                                | `SHELL` (env)                  | Shell-completion script generation                  |
| `SimpleFastaCmd:181`                              | `BLASTDB` (env)                | BLAST data dir                                      |
| `SystemCLIContext:47`                             | full `System.getenv()`         | Passed to subprocesses                              |

---

## 4. Property file locations

### 4.1 Files shipping in `src/main/resources`

| Path                                                                        | Lines | Role                                                                                  |
|-----------------------------------------------------------------------------|------:|---------------------------------------------------------------------------------------|
| `gemma-core/src/main/resources/default.properties`                          |   281 | **User-overrideable defaults** (paths, URLs, DB, security stubs, mail, GA, ontology). |
| `gemma-core/src/main/resources/project.properties`                          |    91 | **Internal defaults** — NCBI/GEO/ArrayExpress/SMD FTP coords, CSV column indices, biomart URLs. |
| `gemma-core/src/main/resources/hibernate.properties`                        |    ~8 | Hibernate dialect (only used by schema-export tools that load it directly).           |
| `gemma-core/src/main/resources/fetcher.properties`                          |     0 | Empty stub (legacy).                                                                  |
| `gemma-core/src/main/resources/ubic/gemma/core/loader/affy.{mps,celmappings,cdfs}.properties` | various | Affymetrix mapping tables (data, not config).                              |
| `gemma-core/src/main/resources/ubic/gemma/core/logging/log4j/messages.properties` | — | i18n strings for logging.                                                          |
| `gemma-core/src/main/resources/ubic/gemma/core/messages.properties`         |     — | i18n strings for core.                                                                |
| `gemma-web/src/main/resources/velocity.properties`                          |     — | Velocity engine config.                                                               |
| `gemma-web/src/main/resources/logging.properties`                           |     — | JUL → log4j bridge.                                                                   |
| `gemma-web/src/main/resources/messages*.properties` (en / fr / pt_BR / es / nl) | — | i18n bundles.                                                                      |

### 4.2 Canonical loader: `SettingsConfig` (`gemma-core/.../config/SettingsConfig.java`)

Exposes a single `PropertySources` bean (`settingsPropertySources`) consumed by `PropertySourcesPlaceholderConfigurer` (for `@Value` placeholder resolution) and by the legacy `BaseCodeConfigurer`. Load order (first wins) — see lines 113–179:

1. **System properties** filtered to known Gemma keys (only properties already declared in `default.properties` or `project.properties` are passed through; system properties without the `gemma.` prefix that happen to match a Gemma key trigger a warning and are skipped).
2. **`-Dgemma.config=...`** user file (if set), else
3. **`$CATALINA_BASE/Gemma.properties`** (if `CATALINA_BASE` env var set), else
4. **`$HOME/Gemma.properties`** (always tried as fallback).
   *At least one user-configuration source must resolve; otherwise startup throws.*
5. Classpath `default.properties`.
6. Classpath `project.properties`.
7. Manifest properties (build hash + timestamp via `ManifestUtils`).

`Gemma.properties` is **read at permissions level** — if the file is `o+r`, `SettingsConfig.warnIfReadableByOthers()` warns at startup (credentials are expected to live there).

### 4.3 Legacy access path: `Settings` (`gemma-core/.../config/Settings.java`)

Static-style accessor still used by `InitializeContext.lintConfiguration` (`Settings.getBoolean("load.ontologies")`), by `SpringContextUtils` (`Settings.getBoolean("quartzOn")` to auto-enable scheduler), and by various legacy callers. `PropertySourcesConfiguration` (Apache Commons-Configuration `AbstractConfiguration` adapter, deprecated in source javadoc) is the bridge that backs `Settings`.

---

## 5. Hardcoded paths / credentials

### 5.1 In `src/main/java`

`grep -rn '/tmp/\|/var/\|jdbc:mysql\|gemmatest\|root@localhost' gemma-*/src/main/java`:

→ **zero hits.** All paths and DB URLs are externalised via `@Value` or the `Settings` accessor.

### 5.2 In `src/main/resources`

| File                                                       | Concern                                                                                  |
|------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `gemma-core/src/main/resources/default.properties:9`       | `gemma.appdata.home=/var/tmp/gemmaData` — **default-only**, user is expected to override. Comment line 23 notes "/var/tmp is usually inadequate, so it is recommended to override this". |
| `gemma-core/src/main/resources/default.properties:53`      | `gemma.db.url=jdbc:mysql://${gemma.db.host}:${gemma.db.port}/${gemma.db.name}?useSSL=false` — **template URL**, host/port/name all overridable via the same properties file. `useSSL=false` is hardcoded into the template though. |
| `gemma-core/src/main/resources/default.properties:119`     | `gemma.goldenpath.db.url=jdbc:mysql://${gemma.goldenpath.db.host}:...` — same shape.   |
| `gemma-core/src/main/resources/default.properties:235`     | `gemma.testdb.url=jdbc:mysql://${gemma.testdb.host}:${gemma.testdb.port}/${gemma.testdb.name}?useSSL=false` — same shape. |
| `gemma-core/src/main/resources/default.properties:passim`  | `gemma.db.password=XXXXXX`, `gemma.runas.password=XXXXXXX`, `gemma.agent.password=XXXXXXX`, `mail.password=` — all **placeholders**, no real creds. |

→ **No real credentials are committed**, but `default.properties` does ship a `/var/tmp/gemmaData` path and `useSSL=false` JDBC fragments that are baked into the default URL template.

### 5.3 In Java source — Hikari datasource defaults

`gemma-core/src/main/java/ubic/gemma/core/config/DataSourceConfig.java` hardcodes (not externalised):

- `com.mysql.cj.jdbc.Driver` class name (lines ~165, 195).
- `useCursorFetch=true`, `rewriteBatchedStatements=true`, `connectionTimeZone=America/Vancouver`, and a long `sessionVariables` override of MySQL `sql_mode` (drops `ONLY_FULL_GROUP_BY`). Documented in javadoc as deliberate.
- `setLeakDetectionThreshold(60_000L)` on the test datasource.

These are MySQL/MySQL-Connector-specific. Acceptable for a MySQL-only product, but worth flagging if multi-DB ever becomes a goal.

---

## 6. Recommendations (HIGH / MEDIUM / LOW)

### HIGH — blocks containerization

1. **`Gemma.properties` discovery is filesystem-only**. ~~`SettingsConfig` requires a `Gemma.properties` file from one of three filesystem locations (`-Dgemma.config`, `$CATALINA_BASE`, `$HOME`) and throws at startup if none resolves.~~ **FIXED** (Phase 3, branch `phase2-acl-migrate`): `SettingsConfig` now emits a `WARN` and continues when no on-disk file resolves. Env vars (`GEMMA_FOO_BAR` → `gemma.foo.bar`) are now also consulted as a first-class property source (highest precedence, filtered against declared keys). See [CONTAINER_CONFIG.md](CONTAINER_CONFIG.md) for the env-var-only deploy pattern.
2. **`gemma.appdata.home=/var/tmp/gemmaData` default**. ~~Containerized deploys typically need a writable mount at a configurable path; the `/var/tmp` default works on a host VM but bakes a Unix-only assumption into the shipped artifact.~~ **FIXED** (Phase 3): default now `${java.io.tmpdir}/gemmaData` — portable across Linux, macOS, Windows, and containers. Production installs still expected to override with a persistent path.
3. **Profile fallback to `dev`** (`SpringContextUtils:115`). In a container we'd want startup to **fail-fast** if no environment profile is set, not silently activate `dev` (which can point at real prod DB credentials via `gemma.db.*`). Today `dev` and `production` share the same `dataSource` bean definition — accidental activation of `dev` against a prod-pointing `Gemma.properties` is a foot-gun. (Not addressed in the Phase 3 env-var pass; tracked as a separate task.)

### MEDIUM — works but smells

4. **MySQL-specific Hikari tuning hardcoded in Java** (`DataSourceConfig.hikariDataSourceProperties`). Connection-pool tuning should ideally come from properties so it can be swapped per env (e.g. test runs may want different `sql_mode`).
5. **`useSSL=false` baked into the default URL templates** in `default.properties`. Containerized prod usually wants `useSSL=true` + `requireSSL=true`. The template currently forces users overriding only the host/port/name to inherit `useSSL=false`.
6. **`PropertySourcesConfiguration` is `@Deprecated`** in source but still backs `Settings.getBoolean(...)` calls that participate in profile activation (`quartzOn`, `load.ontologies`, `load.homologene`). Cleaning this up is necessary before the Apache Commons-Configuration dep can be dropped.
7. **Stub `dataSourceInitializer` / `createDatabaseInitializer` beans** under `@Profile({"production", "dev"})`. These exist purely to satisfy `<bean depends-on=...>` in legacy XML; future cleanup is to remove the XML `depends-on` and delete the stubs.
8. **`gemma.config` system property + `CATALINA_BASE` env + `$HOME` discovery** is three orthogonal mechanisms competing for the same role. A 12-factor refactor should collapse these to one path: env var → standard `application.properties` Spring conventions.

### LOW — cosmetic

9. **Malformed placeholder** at `gemma-cli/src/main/java/ubic/gemma/apps/UpdatePubMedCli.java:57`: `@Value("${entrez.efetch.apikey")` (missing `}`). One-character fix.
10. **`fetcher.properties` is empty** but shipping in the jar. Delete.
11. **Inconsistent profile annotation style** — `@Profile("cli")` vs `@Profile(EnvironmentProfiles.CLI)` vs `@Profile({ "production", "dev" })`. Mechanical normalization to the constants form would make the active-profile invariants more refactor-safe.
12. **XML `profile="..."` redundancy with `@Profile`** on three `applicationContext-*.xml` files (CLI scan, REST analytics, web metrics) — kept as belt-and-suspenders per in-file comments. Could be deleted after a cycle of green-build verification.
13. **`hibernate.properties` is a 1-line dialect file** that only takes effect for tools that load it outside the Spring context. Could be inlined as a constant in the dialect's `HibernateConfig` and the file deleted.

---

## 7. Out-of-scope (noted but not audited deeply)

- `gemma-rest`'s OpenAPI configuration / Jersey wiring.
- Log4j2 configuration files (`log4j2.xml`) — these consume some properties but were not exhaustively cross-referenced.
- The Velocity / Tiles / JSP layer in `gemma-web` (slated for retirement per Phase 3 vision).
- Profile usage inside `src/test/` — only `main/` was audited.
