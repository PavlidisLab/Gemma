# HikariCP audit + modernization

Phase 3 build, branch `worktree-hikari-modernize`, baseline `08e760bdaf`.

## 1. Version + bump decision

- **Pinned:** `pom.xml` line 376 — `com.zaxxer:HikariCP:5.1.0`
- **Latest stable upstream:** 5.1.0 (released 2023-10, still current as of audit).
- **Decision:** No bump. We are on the current 5.x line; HikariCP releases are infrequent and stable. 4.x was the last EOL line; we are clear of it.

## 2. Pool configuration inventory

Three call sites construct or configure a `HikariDataSource`:

| Site | File | Pool name | Pool-size knobs | Other knobs | Notes |
|---|---|---|---|---|---|
| Prod / dev `dataSource` | `gemma-core/.../applicationContext-dataSource.xml` (lines 22-31) | `gemma` | `maximumPoolSize=${gemma.db.maximumPoolSize}` (default 10), `minimumIdle=${gemma.db.minimumIdle}` (default = maximumPoolSize) | `driverClassName`, `username`, `password`, `jdbcUrl`, `dataSourceProperties=dataSourceProps` | All other knobs at Hikari defaults |
| Test / testdb `dataSource` | same file (lines 50-58, post-edit 50-62) | (no poolName) | `maximumPoolSize=${gemma.testdb.maximumPoolSize}` (default 10), `minimumIdle=${gemma.testdb.minimumIdle}` (default = maximumPoolSize) | + `leakDetectionThreshold=60000` (added in this branch) | Test-only |
| Agent bootstrap (Flyway/schema bring-up) | `gemma-core/.../persistence/initialization/BootstrappedDataSourceFactory.java` | inherits from source | inherits via `copyStateTo` from the canonical `dataSource` for HikariDataSource inputs; otherwise just `jdbcUrl/username/password/dataSourceProperties` from driver-based input | — | Strips the path component from the JDBC URL (catalog removed) |
| GoldenPath (UCSC mirror DB) | `gemma-core/.../core/goldenpath/GoldenPath.java` (line 78-95) | `goldenpath` | `maximumPoolSize=${gemma.goldenpath.db.maximumPoolSize}` (default 10) | `relaxAutoCommit=true` (MySQL connector property) | Separate UCSC golden-path mirror DB, not the gemd DB |

`dataSourceProperties` (the MySQL connector knobs applied to every pooled connection on both gemd dataSources) is `dataSourceProps` in the XML and resolves to:

| Property | Value | Rationale |
|---|---|---|
| `useCursorFetch` | `true` | Server-side cursor fetching for large result-set streaming |
| `rewriteBatchedStatements` | `true` | Merge multiple insert/updates into a single round-trip |
| `connectionTimeZone` | `America/Vancouver` | Lab-local DATETIME interpretation for `java.util.Date` columns |
| `sessionVariables` | `sql_mode='STRICT_TRANS_TABLES,...,NO_ENGINE_SUBSTITUTION'` | Drops `ONLY_FULL_GROUP_BY` (Gemma HQL produces aggregates without GROUP-BY-listing every non-aggregate select) |

Metrics binding (`HikariCPMetrics`) wires the pool into Micrometer's `MeterRegistry` via `dataSource.setMetricRegistry(registry)` — modern Micrometer-based metrics; no JMX needed.

## 3. Knob analysis vs HikariCP best-practices

| Knob | Current | Best-practice | Verdict |
|---|---|---|---|
| `maximumPoolSize` | 10 (default; configurable per env) | Workload-dependent; ops-tuned | Leave alone — ops territory |
| `minimumIdle` | = `maximumPoolSize` (via `${gemma.db.minimumIdle}` default) | Equal to `maximumPoolSize` for steady-state workloads (HikariCP author's recommendation) | Already correct |
| `connectionTimeout` | default 30000ms | 30s default is fine | OK |
| `idleTimeout` | default 600000ms (10 min) | < `maxLifetime` | OK (default satisfies the constraint) |
| `maxLifetime` | default 1800000ms (30 min) | At least 30s less than DB `wait_timeout` (MySQL default `wait_timeout=28800`s = 8h) | OK — 30 min << 8 h, huge safety margin |
| `keepaliveTime` | default 0 (off) | Optional; only useful if pool sees long idle stretches under a network with aggressive intermediary timeouts | Leave off |
| `leakDetectionThreshold` | default 0 (off) on prod; **now 60000ms (60s) on test/testdb** | Recommended on dev/test (catch un-closed connections early), off or high (e.g. 5 min) in prod | **Applied to test only — see Section 4** |
| `validationTimeout` | default 5000ms | 5s default sane for a MySQL pool | OK |
| `registerMbeans` | default false | Not set → JMX not registered. Already using Micrometer (preferred) via `HikariCPMetrics`. | Leave off (Micrometer wins) |

## 4. Applied changes

**Single XML edit** to `gemma-core/src/main/resources/ubic/gemma/applicationContext-dataSource.xml`:

- Added `<property name="leakDetectionThreshold" value="60000"/>` to the `test,testdb` profile `dataSource` bean only.
  - Rationale: 60 s un-closed-connection warnings surface real test leaks early (and we have had them historically — slow tests pinning a connection past a try-with-resources boundary). On test pools sized to 10, a leaked connection rapidly starves the pool.
  - Production pool deliberately *not* changed — leak detection is non-trivial overhead and prod doesn't need it.
  - Trivially revertable: delete the one line.

**No version bump** — already at 5.1.0.

**No other knob changes** — defaults are sensible given the configured `wait_timeout`; nothing stale.

## 5. Merge implication with sibling `agent-xml-datasource`

The sibling agent migrates `applicationContext-dataSource.xml` -> `ubic.gemma.core.config.DataSourceConfig` (Java `@Configuration`). After that merge lands, the XML file will be removed and the equivalent `leakDetectionThreshold` setting must be ported into `DataSourceConfig.testDataSource(...)` as:

```java
ds.setLeakDetectionThreshold( 60000L );
```

This is a one-line follow-up. Flagged here so the merger doesn't drop the audit's only behavioural change.

## 6. Deferred recommendations

- **MySQL `wait_timeout` confirmation against prod**: Hikari's default `maxLifetime=30min` is far below MySQL's default 8h, so we're safe — but if ops has tightened `wait_timeout` on the prod server below 1830s the assumption breaks. Worth a one-shot `SHOW VARIABLES LIKE 'wait_timeout'` against the prod port-forward (read-only).
- **Per-env property override surface**: `leakDetectionThreshold` is hard-coded as 60000 in the XML rather than externalized as `${gemma.testdb.leakDetectionThreshold}`. If ops ever wants to disable it without a code change (e.g. a flaky CI box with slow tests), promote to a property. Not worth doing pre-emptively.
- **GoldenPath pool**: `GoldenPath.createDataSource(...)` only sets `maximumPoolSize` and the `relaxAutoCommit` driver property. No `minimumIdle` configured, so Hikari defaults `minimumIdle = maximumPoolSize` — fine. Could mirror the `dataSourceProperties` used on the gemd pool (timezone, sql_mode) if GoldenPath ever surfaces timezone-sensitive DATETIME columns — currently it does not.
- **Micrometer-already-in-use**: `HikariCPMetrics` exposes pool stats to Micrometer. If ops wants to standardize Grafana dashboards for the pool (active / idle / pending / wait-time), the metric stream is already there — no JMX exporter needed.

## 7. Open questions for ops

1. Is prod `wait_timeout` on the gemd MySQL still at the default 28800s? (Confirm vs Hikari's 1800s `maxLifetime` default.)
2. Should prod also opt in to `leakDetectionThreshold` (e.g. 300000 ms = 5 min) once we've shaken out the test-side warnings, or do you prefer to keep prod overhead minimal?
3. Are the current `gemma.db.maximumPoolSize` / `gemma.db.minimumIdle` overrides in your prod config sized appropriately for the current request rate? (We are not changing them — just asking for visibility.)
