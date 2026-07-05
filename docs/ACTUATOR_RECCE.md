# Actuator-style observability endpoints — Phase 3 recce

**Status:** recce only. No code written. No `mvn` invoked.
**Baseline:** branch `worktree-actuator-recce` from `phase2-acl-migrate@08e760bdaf`.
**Author intent:** identify the cheapest path to production health/info/metrics
endpoints now that the Phase 2 ehcache deletion has been recovered and the
Micrometer pipeline produces useful numbers again.

---

## 1. Current observability state

### 1.1 What exists today

| Capability | Location | Status |
|---|---|---|
| Micrometer `MeterRegistry` bean | `gemma-core/src/main/resources/ubic/gemma/applicationContext-serviceBeans.xml` (profile `metrics`) | Wired. Backing impl is `io.micrometer.jmx.JmxMeterRegistry` — exports to JMX MBeans only. |
| JVM / classloader / processor / log4j2 binders | Same XML, lines 38-43 | Bound to registry on startup. |
| Hibernate stats binder | `ubic.gemma.core.metrics.binder.jpa.Hibernate4Metrics` + `Hibernate4QueryMetrics` | Bound. |
| HikariCP pool binder | `ubic.gemma.core.metrics.binder.database.HikariCPMetrics` | Bound. |
| TaskExecutor binders | `ubic.gemma.core.metrics.binder.GenericTaskExecutorMetrics` (+ thread-pool variants) | Bound for `taskExecutor`, `taskRunningService`. |
| **JCache (ehcache) binder** | `ubic.gemma.core.metrics.MeterRegistryEhcacheConfigurer` — restored in commit `c7eed8477c` on the unmerged `worktree-metrics-jcache-restore` branch | **Restored, NOT yet on `phase2-acl-migrate`.** This recce assumes it will be merged. |
| Jersey REST request metrics | `gemma-rest/src/main/java/ubic/gemma/rest/providers/MetricsApplicationEventListener.java` | Active when `MeterRegistry` is present; emits `gemmaRestServlet` timer per route. |
| Servlet (MVC) request metrics | `gemma-web/src/main/java/ubic/gemma/web/metrics/binder/servlet/ServletMetricsFilter.java`, wired via `web.xml` | Active for `gemma` DispatcherServlet. |
| `@Timed` AOP annotation | `io.micrometer.core.aop.TimedAspect` registered in the `metrics` profile | Available across `gemma-core`/`gemma-rest`/`gemma-web`. |

### 1.2 What is missing

- **No HTTP-exposed metrics endpoint.** Numbers live in JMX only — requires
  JConsole / Prometheus JMX exporter sidecar to be readable by Prometheus,
  Grafana Cloud Agent, etc.
- **No health endpoint.** `RootWebService` at `GET /rest/v2/` returns
  `ApiInfoValueObject` with build info + featured external databases, but
  nothing about live process health (DB up? cache up? disk space?).
- **No structured info endpoint.** Build info is folded into the root API
  response but is not a discrete path — anything that polls a single endpoint
  for "what version is running" has to parse `ApiInfoValueObject.buildInfo`.
- **No Spring Boot Actuator.** Gemma is plain Spring 6 + Jersey 3 + classic
  `web.xml`, not Spring Boot. The actuator artifact pulls Boot
  auto-configuration that does not apply here (confirmed: `pom.xml` has zero
  references to `spring-boot-*`).
- **Existing `/monitoring` admin URL** (`applicationContext-security.xml:56`)
  is a Spring MVC page restricted to `GROUP_ADMIN`. Not a programmatic
  endpoint.

### 1.3 Security posture for `/rest/v2/*`

`gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml:41`
applies a permissive http chain to `/rest/v2/**` with anonymous access
permitted by default (`IS_AUTHENTICATED_ANONYMOUSLY` catch-all). Anything new
under `/rest/v2/` will follow that — so `/rest/v2/health` and `/rest/v2/info`
would be anonymous out of the box, which matches conventional actuator
posture for health/info. `/rest/v2/metrics` is the one that needs explicit
thinking (see Open Questions).

---

## 2. Spring Boot Actuator vs alternatives for non-Boot

| Option | Setup cost | Fits Gemma? | Recommendation |
|---|---|---|---|
| **A. Spring Boot Actuator (full)** | Requires `@SpringBootApplication` or extensive manual `@Configuration` of `WebMvcEndpointHandlerMapping`, `EndpointAutoConfiguration`, `HealthEndpointAutoConfiguration`, etc. Conflicts with Jersey 3 (Boot's default endpoint dispatcher is MVC). | No. | Reject. |
| **B. `spring-boot-actuator` core only (no autoconfigure)** | Pull `org.springframework.boot:spring-boot-actuator` (not `-autoconfigure`). Wire each `Endpoint` bean manually plus a custom dispatcher. Still drags `spring-boot` core. | Possible, but the dispatcher integration with Jersey 3 is undocumented and brittle. | Reject. |
| **C. Micrometer + `micrometer-registry-prometheus`, expose via Jersey resource** | Add one dependency (`io.micrometer:micrometer-registry-prometheus`, same `1.13.11` line). Register an extra `PrometheusMeterRegistry` alongside `JmxMeterRegistry` (Micrometer's `CompositeMeterRegistry`). Add a Jersey resource at `/rest/v2/metrics` that calls `prometheusRegistry.scrape()`. | Yes. Minimal blast radius. | **Recommend.** |
| **D. Bare Jersey health/info resources** | One Jersey `@Path` class per concern. Pure DI, no new deps. | Yes. | **Recommend in combination with C.** |

The combined approach (C + D) gives a useful surface in <1 day of work, ships
no Boot transitive baggage, and keeps Jersey 3 + plain Spring 6 as the only
moving parts.

---

## 3. Proposed 3-endpoint design

All endpoints live under the existing `/rest/v2/` Jersey servlet mapping. All
three classes are `@Service` + `@Path` Jersey resources, scanned via the
existing `jersey.config.server.provider.packages=...,ubic.gemma.rest` init
param in `web.xml`.

### 3.1 `GET /rest/v2/health`

**Auth:** anonymous (matches `IS_AUTHENTICATED_ANONYMOUSLY` default for
`/rest/v2/**`).
**Response (200 + JSON):**

```json
{
  "status": "UP",
  "components": {
    "db":          { "status": "UP", "details": { "database": "MySQL", "validationQuery": "SELECT 1" } },
    "cache":       { "status": "UP", "details": { "cacheManager": "JCache", "caches": 41 } },
    "diskSpace":   { "status": "UP", "details": { "total": ..., "free": ..., "threshold": ... } }
  }
}
```

**Status codes:** 200 when `status == UP`, 503 when any component is `DOWN`.
**Checks:**
- `db`: borrow a connection from `dataSource` (HikariCP) and run
  `SELECT 1`. Timeout via `Connection.isValid(2)`. Aggregate per-shard if
  multiple datasources (Gemma currently has one).
- `cache`: confirm `cacheManager` bean (JCache) is non-null and enumerable;
  count caches. No probe per cache (would distort hit ratios).
- `diskSpace`: check `${gemma.appdata.home}` and `${gemma.analysis.dir}` —
  if either is below configurable threshold (default 100 MB) report `DOWN`.
**Shape rationale:** matches Spring Boot Actuator's `HealthEndpoint` JSON so
any existing dashboard or uptime tool (Grafana, Pingdom, Better Stack) that
already reads Boot health works without remapping.

### 3.2 `GET /rest/v2/info`

**Auth:** anonymous.
**Response (200 + JSON):**

```json
{
  "build": {
    "version":   "1.32.0-SNAPSHOT",
    "timestamp": "2026-05-18T09:14:11Z",
    "gitHash":   "08e760bdafb486a0b67705fb527ab8472d02d386"
  },
  "java": {
    "version":   "17.0.x",
    "vendor":    "Amazon.com Inc.",
    "vm":        "OpenJDK 64-Bit Server VM"
  },
  "os": {
    "name":    "Linux",
    "version": "...",
    "arch":    "amd64"
  }
}
```

**Source:** wraps existing `ubic.gemma.core.util.BuildInfo` (already injected
into `RootWebService`). Adds JVM/OS reads via standard `System.getProperty`.
**Rationale:** lifts the build block out of `ApiInfoValueObject` so version
polling can hit a stable URL without parsing the larger root payload. Existing
`gitHash` is populated by `git-commit-id-maven-plugin` (confirmed in
`gemma-core/pom.xml:15-16`).

### 3.3 `GET /rest/v2/metrics`

**Auth:** **TBD — see Open Questions §6.** Default proposal: token-gated.
**Content-Type:** `text/plain; version=0.0.4; charset=utf-8` (Prometheus
exposition format).
**Body:** result of `prometheusMeterRegistry.scrape()` — one HELP/TYPE/value
triple per meter currently registered. With the bound binders this is:
- JVM (memory, threads, classloaders, GC)
- Processor (system load, CPU)
- Log4j2 events per level
- Hibernate session statistics
- Hibernate per-query statistics
- HikariCP pool stats
- Local task executors
- Task running service (jobs queued / running)
- JCache hit/miss/eviction (post-JCacheMetrics restoration)
- Jersey REST request timers (`gemmaRestServlet` by route)
- Servlet MVC request timers (`gemmaWebServlet` by route)
- Anything `@Timed`-annotated

**Wiring:** add `PrometheusMeterRegistry` to the existing `metrics` profile in
`applicationContext-serviceBeans.xml`. Wrap both Jmx and Prometheus in a
`CompositeMeterRegistry` so all existing binders write to both — no change to
any binder. Inject the composite as the `MeterRegistry` bean; inject the
Prometheus one separately by qualifier into the metrics resource.

---

## 4. Implementation outline

### 4.1 New files

```
gemma-rest/src/main/java/ubic/gemma/rest/monitoring/
    HealthWebService.java          # @Service @Path("/health")
    InfoWebService.java            # @Service @Path("/info")
    MetricsWebService.java         # @Service @Path("/metrics")
    HealthValueObject.java         # response VO + nested ComponentVO
    InfoValueObject.java           # response VO (build/java/os)
    health/
        HealthIndicator.java       # SAM: HealthStatus check()
        DbHealthIndicator.java     # @Component, autowires DataSource
        CacheHealthIndicator.java  # @Component, autowires CacheManager
        DiskSpaceHealthIndicator.java
```

`HealthWebService` autowires `List<HealthIndicator>` (Spring collects all
beans of that type). Each indicator returns `HealthStatus` (UP/DOWN +
detail map); the resource aggregates. Pattern mirrors Boot's
`HealthContributor` but stays in our package.

### 4.2 Modified files

| File | Change |
|---|---|
| `pom.xml` (dependencyManagement) | Add `io.micrometer:micrometer-registry-prometheus` pinned to `${micrometer.version}` (`1.13.11`). |
| `gemma-rest/pom.xml` | Add scope-compile dep on `micrometer-registry-prometheus`. |
| `gemma-core/src/main/resources/ubic/gemma/applicationContext-serviceBeans.xml` | In `metrics` profile: replace single `meterRegistry` bean with `compositeMeterRegistry` that fans out to `jmxMeterRegistry` + `prometheusMeterRegistry`. Expose `prometheusMeterRegistry` as named bean so `MetricsWebService` can inject by qualifier. |
| `gemma-web/src/main/resources/ubic/gemma/applicationContext-security.xml` | Add `<s:intercept-url pattern="/rest/v2/metrics" access="hasAuthority('GROUP_ADMIN') or @scrapeTokenChecker.matches(request)"/>` above the `/rest/v2/users/**` line. Add a `scrapeTokenChecker` bean that compares `X-Scrape-Token` header against `${gemma.metrics.scrapeToken}`. Decision pending §6. |

### 4.3 Test coverage

- Unit: each `HealthIndicator` with a mocked dependency.
- Integration: a Jersey `JerseyTest` per resource hitting `/health`, `/info`,
  `/metrics`. Verify JSON shape for the first two, presence of `# HELP jvm_`
  prefix for `/metrics`.
- No new fixture: reuse `BaseTest` plumbing already used by the rest of
  `gemma-rest`.

---

## 5. Effort estimate

| Stage | Hours |
|---|---|
| `pom.xml` + `applicationContext-serviceBeans.xml` wiring (composite registry) | 1 |
| `HealthIndicator` SAM + 3 indicators (`DbHealthIndicator`, `CacheHealthIndicator`, `DiskSpaceHealthIndicator`) | 2 |
| `HealthWebService` + VOs + aggregation + 503-when-DOWN | 1.5 |
| `InfoWebService` + `InfoValueObject` (largely wraps `BuildInfo`) | 0.5 |
| `MetricsWebService` (one-liner `scrape()` call + content-type) | 0.5 |
| Security wiring for `/rest/v2/metrics` (depends on §6 decision) | 0.5-2 |
| Unit + integration tests | 2 |
| OpenAPI annotations (so `@Operation` shows up in restapidocs) | 1 |
| Manual smoke against local | 1 |
| Documentation (README section + CHANGELOG entry) | 0.5 |

**Total: ~10-12 hours**, plus whatever §6 implies if Paul wants OAuth2 or
mTLS instead of a static token.

---

## 6. Open questions for Paul

1. **`/rest/v2/metrics` auth model.** Production Prometheus scraping
   conventionally uses one of:
   - **Static bearer/shared token** in a header (`X-Scrape-Token`,
     `Authorization: Bearer ...`) — simple, well-supported by Prometheus
     and the Grafana Agent. Recommend this as the default.
   - **`GROUP_ADMIN` session/basic auth** — works with existing Gemma
     plumbing, but Prometheus does not easily carry HTTP basic to a session
     cookie; basic auth alone is fine if the scraper supports it.
   - **Anonymous + network ACL** (only loopback or the cluster scrape
     subnet can reach the URL) — operational, not application-layer.
     Cheapest if Apache/nginx in front of Tomcat already firewalls
     `/rest/v2/metrics`.
   **Default proposal:** static token via property
   `${gemma.metrics.scrapeToken}`, with empty-value meaning
   "metrics endpoint disabled". Confirm.

2. **`/rest/v2/health` — full vs liveness/readiness split.** Boot ships
   `/health/liveness` (process alive) and `/health/readiness` (deps healthy).
   Kubernetes-style probes prefer the split. Gemma deploys on bare Tomcat
   today, so the split has no consumer yet. **Default proposal:** single
   `/health` for now. Add the split when/if we containerize.

3. **Disk space threshold.** Default Boot is 10 MB; that is too low for a
   data-heavy app. **Default proposal:** 100 MB on `gemma.appdata.home` and
   `gemma.analysis.dir`, override via
   `${gemma.health.diskSpace.thresholdBytes}`. Confirm.

4. **Show secrets in `/info`?** Some teams expose `git.commit.message` and
   `git.commit.user.email` in `/info` via the `git-commit-id` plugin's
   `git.properties` file. **Default proposal:** expose hash + timestamp +
   short message only; never email. Confirm.

5. **Metrics endpoint and `metrics` Spring profile.** Today the entire
   metrics pipeline is conditional on the `metrics` profile being active.
   If that profile is not active, `/rest/v2/metrics` should 404 (or return
   `# no metrics enabled`) rather than 500. **Default proposal:**
   `MetricsWebService.scrape()` returns 503 with body
   `metrics profile not active` when the Prometheus bean is missing.
   Confirm.

6. **Path versioning.** Boot puts actuator under `/actuator/*` not
   `/rest/v2/actuator/*`. Gemma's existing servlet mapping is `/rest/v2/*`
   only — exposing under `/actuator/*` would require an extra Jersey
   servlet mapping in `web.xml`. **Default proposal:** ship under
   `/rest/v2/{health,info,metrics}` since the servlet is already mapped;
   document the difference from Boot in restapidocs. Confirm.

---

## 7. Risk register

| Risk | Likelihood | Mitigation |
|---|---|---|
| `PrometheusMeterRegistry` adds transitive `simpleclient` jars that collide with something on classpath | Low | `1.13.11` ships clean against current pom — check `mvn dependency:tree` post-add. |
| `/rest/v2/metrics` anonymous by accident leaks per-route timings (low-grade info disclosure) | Medium until §6 resolved | Default the implementation to "disabled when scrape token unset" — fail closed. |
| Health endpoint DB probe runs on every external scrape (Prometheus default: 15s) and warms a connection unnecessarily | Low | Use `Connection.isValid(2)` which is HikariCP-cached, not a full SELECT. Cache health result for 5s if needed. |
| `JCacheMetrics` restoration commit `c7eed8477c` not merged at the time this lands | High right now | Block this PR on the JCacheMetrics merge; the binders the metrics endpoint exposes assume it. |

---

## 8. Out of scope (deferred to a follow-up recce)

- `/rest/v2/loggers` (live log-level adjustment) — useful but invites abuse;
  skip until there is a real ops need.
- `/rest/v2/threaddump`, `/rest/v2/heapdump` — security-sensitive and easily
  replaced by `jstack`/`jmap` on the host.
- OpenTelemetry tracing export — Phase 3+ infrastructure work, separate from
  pull-based metrics. Micrometer 1.13 has `micrometer-tracing-bridge-otel`
  available when we get there.
- Migrating away from the `metrics` Spring profile (always-on metrics) —
  worth doing eventually so production cannot accidentally start without
  it, but a separate change.

---

## 9. Definition of done for the future implementation PR

- [ ] `/rest/v2/health` returns 200 UP for a healthy local instance, 503
      DOWN when MySQL is stopped.
- [ ] `/rest/v2/info` returns the same `gitHash` as the manifest
      `gemma.build.gitHash` property.
- [ ] `/rest/v2/metrics` returns Prometheus exposition with at least one
      `jvm_`, `hikaricp_`, `hibernate_`, and `cache_` family present.
- [ ] Security default: `/rest/v2/metrics` is **closed** unless a scrape
      token is configured.
- [ ] OpenAPI doc page lists all three endpoints with example responses.
- [ ] `mvn verify` green on JDK 17 amazon-corretto.
- [ ] Notable case appended documenting the JCacheMetrics dependency
      between this PR and `worktree-metrics-jcache-restore`.
