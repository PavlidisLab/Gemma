# Structured logging + OpenTelemetry — Phase 3 recce

**Status:** recce only. No code written. No `mvn` invoked.
**Baseline:** branch `worktree-agent-a546af84c0482e499` from `phase2-acl-migrate@1cc7560f07`.
**Author intent:** define the cheapest path from today's plain-text log4j2 to
JSON-line structured logs plus OpenTelemetry trace correlation, ready for the
container/cloud target described in `PHASE_3_VISION.md` §"Cloud-ready".

Per the vision doc:

> **Structured logging + OpenTelemetry.** Replace Commons Logging.
> Correlation IDs across services, trace-driven debugging.

JCL→SLF4J was already done (Phase 1 of that bullet: `log4j-slf4j2-impl` is the
binding, all four modules log via SLF4J). What remains is the *format* and the
*correlation* — that is the scope of this recce.

---

## 1. Current state

### 1.1 log4j2 config files

| Path | Lines | Role |
|---|---:|---|
| `gemma-web/src/main/config/log4j2.xml` | 140 | Production webapp config |
| `gemma-web/src/main/config/log4j2-dev.xml` | 72 | Dev/embedded-Tomcat config |
| `gemma-cli/src/main/config/log4j2.xml` | 142 | Production CLI config |
| `gemma-cli/src/main/config/log4j2-dev.xml` | 44 | Dev CLI config |
| `gemma-core/src/test/resources/log4j2-test.xml` | 33 | Core integration tests |
| `gemma-rest/src/test/resources/log4j2-test.xml` | 30 | REST integration tests |
| `gemma-web/src/test/resources/log4j2-test.xml` | 34 | Web integration tests |
| **Total** | **495** | 7 files across 4 modules |

No standalone config in `gemma-rest/src/main/config`; gemma-rest runs inside
the gemma-web WAR and picks up `gemma-web/src/main/config/log4j2.xml`.

### 1.2 Appender topology (production webapp — the cloud target)

`gemma-web/src/main/config/log4j2.xml` ships 7 appenders, all PatternLayout
except the two custom ones:

| Appender | Type | Layout | Destination |
|---|---|---|---|
| `auditFile` | RollingFile (time, daily) | `PatternLayout %m%n` | `${gemma.log.dir}/gemma-audit.log` |
| `jsFile` | RollingFile (size, 10MB) | `PatternLayout %d %5p [Gemma - %t] %m%n` | `${gemma.log.dir}/gemma-javascript.log` |
| `file` | RollingFile (size, 10MB) | `PatternLayout ${gemma.log.pattern}` | `${gemma.log.dir}/gemma.log` |
| `annotationsFile` | RollingFile (size, 10MB) | `PatternLayout ${gemma.log.pattern}` | `${gemma.log.dir}/gemma-annotations.log` |
| `warningFile` | RollingFile (time, daily) | `PatternLayout ${gemma.log.pattern}` | `${gemma.log.dir}/gemma-warnings.log` |
| `errorFile` | RollingFile (time, daily) | `PatternLayout ${gemma.log.pattern}` | `${gemma.log.dir}/gemma-errors.log` |
| `slack` | Custom `Slack` plugin (`ubic.gemma.core.logging.log4j.SlackAppender`) | n/a | Slack webhook for ERROR+ |
| `progressUpdate` | Custom `ProgressUpdate` plugin (`ubic.gemma.core.logging.log4j.ProgressUpdateAppender`) | `PatternLayout %m` | In-memory task progress channel |

Shared pattern:

```
%d %p %pid [%t] %C.%M(%L) | %m%n
```

→ timestamp · level · pid · thread · class.method(line) · pipe · message.

### 1.3 Per-package log levels (production webapp)

Root is `INFO`. Quieted packages (excerpted):

- `com`, `net`, `org`, `org.springframework`, `org.hibernate`,
  `org.apache.commons`, `org.directwebremoting`, `nl.basjes.parse.useragent`,
  `com.opensymphony.oscache` → `WARN`
- `org.apache.jena.riot.RDFLanguages`,
  `org.hibernate.cache.ReadWriteCache`,
  `org.hibernate.engine.loading.LoadContexts`,
  `org.springframework.security.authentication.event.LoggerListener`,
  `org.springframework.web.servlet.PageNotFound`,
  `org.glassfish.jersey.internal.Errors`,
  `ubic.basecode.ontology.model.PropertyFactory`,
  `com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler` → `ERROR`
- `org.springframework.beans.GenericTypeAwarePropertyDescriptor` → `FATAL`
  (suppressed)
- `ubic.gemma`, `ubic.basecode`,
  `org.springframework.scheduling.quartz`,
  `org.springframework.security.access.event.LoggerListener`,
  `org.glassfish.jersey.server.ServerRuntime$Responder` → `INFO`
- Special-routing loggers (annotations + audit) override appender refs.

The CLI config (`gemma-cli/src/main/config/log4j2.xml`) layers a colourised
console appender on top of the same file set, with a stack-trace filter
property `gemma.log.filter` to hide proxy/reflection/AOP/transaction frames
from terminal output. JS-log routing is absent in CLI (no browser).

### 1.4 Existing log-correlation infrastructure (important — already there)

`gemma-core/src/main/java/ubic/gemma/core/logging/log4j/` already contains a
`ThreadContext` propagation layer:

- `BuildInfoThreadContextPopulator` — pushes `buildInfo` into `ThreadContext`.
- `DelegatingThreadContext{Runnable,Callable,Executor,ExecutorService,ScheduledExecutorService,AsyncTaskExecutor,SchedulingTaskExecutor,TaskExecutor}` —
  copy log4j `ThreadContext` (== MDC) across thread handoffs.
- `TaskExecutorThreadContextInheritPostProcessor` — wraps every `TaskExecutor`
  bean so `ThreadContext` survives `taskExecutor.submit(...)`.
- `Log4jConfig` — registers the above.
- `ThreadContextPopulator` (interface).
- `ubic.gemma.core.util.concurrent.Executors.newDelegatingExecutorService`
  chains `DelegatingSecurityContext*` + `DelegatingThreadContext*`.

**Consequence:** MDC values *already propagate* across executors today. Phase
2 below just needs to *put values in*; the rest is plumbing that exists.

### 1.5 Dependencies in play (`pom.xml`)

- `log4j.version` tracks `pavlab-starter-parent` → **2.25.3**.
- BOM-managed: `log4j-api`, `log4j-core`, `log4j-slf4j2-impl`,
  `log4j-jcl` (Commons-Logging→log4j2 bridge),
  `log4j-jul` (java.util.logging→log4j2).
- `slf4j.version` = 2.0.16.
- gemma-web pulls `log4j-web` for ServletContext init.
- Tomcat is started with `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`.

---

## 2. Goal

One log line = one JSON object. Per the task brief:

```json
{
  "timestamp": "2026-05-19T14:32:11.412Z",
  "level": "INFO",
  "logger": "ubic.gemma.core.expression.experiment.ExperimentServiceImpl",
  "thread": "http-nio-8080-exec-3",
  "message": "Loaded experiment GSE12345 (id=7723)",
  "mdc": {
    "requestId": "f0c1b8de-7c2a-4d1d-9b88-1d6c8e0f31a2",
    "userId": "alice",
    "experimentId": "7723",
    "buildInfo": "gemma 1.32.0-SNAPSHOT@abcdef0"
  },
  "traceId": "0af7651916cd43dd8448eb211c80319c",
  "spanId": "b7ad6b7169203331"
}
```

Required MDC keys:

| Key | Source | Phase |
|---|---|---|
| `buildInfo` | `BuildInfoThreadContextPopulator` | already present |
| `requestId` | UUID generated per HTTP request, or echoed from `X-Request-Id` if upstream supplied it | Phase 2 |
| `userId` | `SecurityContextHolder.getContext().getAuthentication().getName()` | Phase 2 |
| `experimentId` | Set inside controllers/services when an EE is in scope (AOP or explicit `try-with-CloseableThreadContext`) | Phase 2 (opt-in, low priority) |
| `traceId`, `spanId` | OTel `MDCContextInstrumenter` (auto) | Phase 3 |

Slack + audit + progress-update appenders are not in scope — they have their
own format constraints and shipping JSON would be a regression.

---

## 3. Migration path

Four phases, each independently shippable. Risk and validation called out
per phase.

### Phase 1 — JsonTemplateLayout on the production rolling-file appender

**Scope:**

| File | Δ LoC (est.) | Change |
|---|---:|---|
| `gemma-web/src/main/config/log4j2.xml` | +6 / -1 | Replace `<PatternLayout>` inside `<RollingFile name="file">` with `<JsonTemplateLayout eventTemplateUri="classpath:EcsLayout.json"/>` (or a custom template — see §4). Keep `warningFile`/`errorFile`/`annotationsFile`/`auditFile` on PatternLayout for now to avoid breaking grep-based downstream tools. |
| `gemma-cli/src/main/config/log4j2.xml` | +6 / -1 | Same change for the `file` appender. Console stays PatternLayout — humans read it. |
| `pom.xml` | +5 | Add `log4j-layout-template-json` to `<dependencyManagement>` pinned to `${log4j.version}` (it ships from the same release train as log4j-core, so the version is already known good). |
| `gemma-core/pom.xml` (or wherever the runtime classpath is assembled) | +4 | Add `<dependency>org.apache.logging.log4j:log4j-layout-template-json</dependency>` so the artifact is on the WAR / shaded-CLI classpath. |
| `gemma-web/src/main/config/log4j2-dev.xml`, `gemma-cli/src/main/config/log4j2-dev.xml` | 0 | Dev stays on coloured PatternLayout. |
| Test configs (`log4j2-test.xml` × 3) | 0 | Tests stay on PatternLayout — human readable, CI scrape-friendly. |

**Total Phase 1: ~22 LoC across 4 files.**

**Risk:** low.

- JsonTemplateLayout is part of log4j-core's release train, not a third-party
  layout. No risk of version drift.
- Slack appender, audit-file format, JS-log format, progress-update appender
  are untouched — the loggers writing into them are unchanged.
- One real risk: anything currently grepping `gemma.log` (e.g., admin
  shell scripts, log-rotate hooks, support runbooks) breaks. Mitigation:
  ship Phase 1 *behind a system property*: `${sys:gemma.log.format:-pattern}`
  selects layout via a `<Select>` block, defaulting to PatternLayout in
  production until ops is briefed.

**Validation:**

1. `mvn verify` — must pass (no test config changed; tests still PatternLayout).
2. Start gemma-web locally with `-Dgemma.log.format=json`, hit
   `GET /rest/v2/`, `tail -1 gemma.log | jq .` — assert valid JSON, presence
   of `timestamp`/`level`/`logger`/`message`.
3. Confirm a multi-line stack trace renders as a single JSON object with
   `"thrown"` populated (default ECS template handles this; custom template
   must opt in via `"stackTrace": {"$resolver": "exception"}`).
4. Bench: log 100k INFO lines under JsonTemplateLayout vs PatternLayout in a
   one-off `@JmhBenchmark`-style harness; expect ≤10% throughput delta.

### Phase 2 — Spring filter populates `requestId` + `userId` into MDC

**Scope:**

| File | Δ LoC (est.) | Change |
|---|---:|---|
| `gemma-core/src/main/java/ubic/gemma/core/logging/log4j/MdcRequestContextFilter.java` | +60 (new) | `OncePerRequestFilter` that, in `doFilterInternal`: reads `X-Request-Id` header or generates `UUID.randomUUID()`; pushes onto `ThreadContext.put("requestId", ...)`; reads `SecurityContextHolder.getContext().getAuthentication().getName()` (when non-anonymous) into `userId`; in a `finally` block calls `ThreadContext.remove(...)` for both keys; also writes the resolved `X-Request-Id` back onto the response header so callers/log shippers can correlate. Use `CloseableThreadContext.put(...).put(...)` for try-with-resources cleanup. |
| `gemma-web/src/main/webapp/WEB-INF/web.xml` (or `gemma-web/src/main/config/applicationContext-*.xml`) | +10 | Register the filter via `DelegatingFilterProxy` so the Spring bean wires in. Map to `/*`. |
| `gemma-rest/src/main/java/ubic/gemma/rest/servlet/...` | 0 | gemma-rest runs inside the same WAR; the filter applies. |
| `gemma-core/src/main/resources/ubic/gemma/applicationContext-component-scan.xml` (or `Log4jConfig`) | +3 | Declare the bean. |
| Unit test (`gemma-web/src/test/java/.../MdcRequestContextFilterTest.java`) | +80 | Verify (a) UUID is generated when no header present, (b) inbound `X-Request-Id` is honoured, (c) `userId` is `anonymousUser` when SecurityContext is unauthenticated, (d) `ThreadContext` is empty after the filter chain returns. |

**Total Phase 2: ~155 LoC across 5 files (~75 production, ~80 test).**

**Risk:** low–medium.

- Filter ordering matters: must run *after* Spring Security's filter chain
  populates `SecurityContextHolder` for `userId` to resolve to the real user.
  In Spring's `FilterChainProxy` ordering, that means registering the new
  filter at order `LOWEST_PRECEDENCE - 10` or via `<sec:custom-filter
  after="LAST"/>`.
- CLI: filter does not apply (no servlet). CLI gets `requestId` set once at
  CLI entry inside `GenericCLI.main` (≈3 LoC, `ThreadContext.put` before
  the command's `doWork()`). `userId` in CLI is the bootstrap admin user
  and can be populated the same way.

**Validation:**

1. Unit test above.
2. Manual: start gemma-web, send
   `curl -H 'X-Request-Id: abc-123' http://localhost:8080/rest/v2/datasets/1` ,
   tail `gemma.log` (with Phase 1 JSON layout enabled), assert
   `mdc.requestId == "abc-123"` and `mdc.userId == "anonymousUser"`.
3. Authenticated equivalent: assert `userId` is the logged-in username.
4. Concurrency: hit the same endpoint 100× in parallel; assert no MDC bleed
   (each line's `requestId` matches a single request — easy to spot-check by
   grouping by `requestId` and checking entry/exit `logger` names align).

### Phase 3 — OpenTelemetry SDK + log4j MDC adapter

**Scope:**

| File | Δ LoC (est.) | Change |
|---|---:|---|
| `pom.xml` | +20 | Add `io.opentelemetry:opentelemetry-bom` to `<dependencyManagement>`. Add `io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha` for the log4j ContextData provider. Pin to a single version property `${otel.version}` (e.g. `1.43.0` stable). |
| `gemma-core/pom.xml` | +12 | Add runtime deps: `opentelemetry-api`, `opentelemetry-sdk`, `opentelemetry-sdk-extension-autoconfigure`, `opentelemetry-log4j-context-data-2.17-autoconfigure` (the artifact that auto-injects `trace_id` + `span_id` into log4j `ThreadContext`). |
| `gemma-core/src/main/java/ubic/gemma/core/logging/otel/OpenTelemetryConfig.java` | +40 (new) | Spring `@Configuration` that instantiates `OpenTelemetrySdk` via `AutoConfiguredOpenTelemetrySdkBuilder` (env-var driven). Exposes `OpenTelemetry` bean. Behind profile `otel` so it stays off until explicitly enabled. |
| JSON template (Phase 1) | +4 | Add `traceId` / `spanId` entries pulling from `mdc:trace_id` / `mdc:span_id`. |

**Total Phase 3: ~76 LoC across 4 files.**

**Risk:** medium.

- OTel auto-configure has historically had startup-cost / classpath-shadowing
  surprises. Profile-gating mitigates: ops can disable on a flaky deploy.
- The log4j context-data-provider artifact must match the log4j-api major
  version (2.17+) — current Gemma is on 2.25.3, compatible.
- Alternative path (lower risk, higher ops cost): use the **OpenTelemetry
  Java agent** (`-javaagent:opentelemetry-javaagent.jar`) instead of the SDK
  dep. Agent does instrumentation + MDC injection without any source change
  — Phase 3 source diff drops to ~10 LoC (just the template tweak). Agent
  size is ~25 MB and adds ~3–5s to JVM startup. Recommend SDK route for the
  webapp (long-lived JVM, source-side control of what gets instrumented) and
  agent route for the CLI commands that need traces (rare).

**Validation:**

1. `mvn verify` with `-Potel` — assert OTel beans wire and no classpath
   conflicts.
2. Start gemma-web with `-Dspring.profiles.active=otel`,
   `-Dotel.traces.exporter=logging` (built-in stdout exporter — no collector
   yet). Hit an endpoint; assert `traceId` + `spanId` appear in the JSON log
   line *and* in stdout from the logging exporter, and that they match.
3. Inside a Jersey-Spring transactional path, assert nested spans inherit
   the parent's `traceId` but get distinct `spanId`s.

### Phase 4 — OTLP/gRPC exporter to a collector

**Scope:**

| File | Δ LoC (est.) | Change |
|---|---:|---|
| `gemma-core/pom.xml` | +5 | Add `opentelemetry-exporter-otlp` (gRPC exporter; HTTP variant if firewall rules forbid gRPC). |
| `gemma-core/src/main/java/.../OpenTelemetryConfig.java` | +15 | When `OTEL_EXPORTER_OTLP_ENDPOINT` is set, register `OtlpGrpcSpanExporter` (and `OtlpGrpcLogRecordExporter` if log forwarding is also wanted). |
| `docs/PHASE_3_OTEL.md` or `README.md` | +30 | Doc: env vars `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS`, `OTEL_SERVICE_NAME=gemma-web`, `OTEL_RESOURCE_ATTRIBUTES=deployment.environment=prod`, `OTEL_TRACES_SAMPLER=parentbased_traceidratio`, `OTEL_TRACES_SAMPLER_ARG=0.05`. |

**Total Phase 4: ~50 LoC across 3 files.**

**Risk:** medium (network-dependent).

- Exporter blocks on first send if the collector is down. Mitigate with
  `otel.bsp.export.timeout=2s` so failures degrade gracefully.
- Sampling matters: at 100% sampling on a busy webapp the exporter can
  dominate CPU. Default to 5% (`parentbased_traceidratio:0.05`); ops can
  bump if a specific incident needs full traces.
- No collector deployed yet — Phase 4 is *optional until* a collector
  endpoint exists (Tempo, Honeycomb, Grafana Cloud, Jaeger, whatever).

**Validation:**

1. Stand up an `otel-collector` container locally with a `logging` exporter
   downstream. Set `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317`,
   start gemma-web, send 10 requests, assert collector stdout shows spans
   with matching `traceId`s and the resource attribute
   `service.name=gemma-web`.
2. Kill the collector, assert gemma-web keeps serving (no thread starvation,
   no error storm).

---

## 4. Concrete diff — Phase 1 sample patch (not for commit)

`gemma-web/src/main/config/log4j2.xml`, lines 30–36 (the `file`
RollingFile appender):

```diff
-        <RollingFile name="file" fileName="${gemma.log.dir}/gemma.log" filePattern="${gemma.log.dir}/gemma.log.%i">
-            <PatternLayout pattern="${gemma.log.pattern}"/>
+        <RollingFile name="file" fileName="${gemma.log.dir}/gemma.log" filePattern="${gemma.log.dir}/gemma.log.%i">
+            <JsonTemplateLayout eventTemplateUri="classpath:gemma-log-template.json"/>
             <Policies>
                 <SizeBasedTriggeringPolicy size="10000KB"/>
             </Policies>
             <DefaultRolloverStrategy max="10" fileIndex="min"/>
         </RollingFile>
```

A minimal custom template `gemma-core/src/main/resources/gemma-log-template.json`:

```json
{
  "timestamp": {
    "$resolver": "timestamp",
    "pattern": {"format": "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "timeZone": "UTC"}
  },
  "level": {"$resolver": "level", "field": "name"},
  "logger": {"$resolver": "logger", "field": "name"},
  "thread": {"$resolver": "thread", "field": "name"},
  "message": {"$resolver": "message", "stringified": true},
  "thrown": {
    "exceptionClass": {"$resolver": "exception", "field": "className"},
    "exceptionMessage": {"$resolver": "exception", "field": "message"},
    "stackTrace": {"$resolver": "exception", "field": "stackTrace", "stackTrace": {"stringified": true}}
  },
  "mdc": {"$resolver": "mdc", "flatten": false},
  "traceId": {"$resolver": "mdc", "key": "trace_id"},
  "spanId":  {"$resolver": "mdc", "key": "span_id"},
  "host":    "${env:HOSTNAME:-unknown}",
  "service": "gemma-web"
}
```

Expected output (one line):

```json
{"timestamp":"2026-05-19T14:32:11.412Z","level":"INFO","logger":"ubic.gemma.core.expression.experiment.ExperimentServiceImpl","thread":"http-nio-8080-exec-3","message":"Loaded experiment GSE12345 (id=7723)","mdc":{"requestId":"f0c1b8de-7c2a-4d1d-9b88-1d6c8e0f31a2","userId":"alice","buildInfo":"gemma 1.32.0-SNAPSHOT@abcdef0"},"traceId":null,"spanId":null,"host":"gemma-prod-1","service":"gemma-web"}
```

(`traceId`/`spanId` are `null` until Phase 3 lands.)

---

## 5. Cost / dependencies

### New runtime dependencies

| Phase | Artifact | Size (approx) | Version source |
|---|---|---:|---|
| 1 | `org.apache.logging.log4j:log4j-layout-template-json` | ~85 KB | `${log4j.version}` (2.25.3 — same release train as log4j-core, already in dep mgmt) |
| 1 | (transitive) `com.fasterxml.jackson.core:jackson-core` | ~450 KB | Already on classpath via Jersey + Spring; no new pull |
| 3 | `io.opentelemetry:opentelemetry-api` | ~210 KB | `io.opentelemetry:opentelemetry-bom` (e.g. 1.43.0) |
| 3 | `io.opentelemetry:opentelemetry-sdk` | ~340 KB | bom |
| 3 | `io.opentelemetry:opentelemetry-sdk-extension-autoconfigure` | ~110 KB | bom |
| 3 | `io.opentelemetry.instrumentation:opentelemetry-log4j-context-data-2.17-autoconfigure` | ~25 KB | `opentelemetry-instrumentation-bom-alpha` |
| 4 | `io.opentelemetry:opentelemetry-exporter-otlp` | ~600 KB | bom |
| 4 | (transitive) `io.grpc:grpc-netty-shaded` | ~10 MB | bom |

**Total new direct deps: 6 across all 4 phases.** Adding the gRPC exporter
brings the WAR weight up by ~10 MB — the heaviest single line item. If the
ops collector supports OTLP/HTTP (most do), prefer `opentelemetry-exporter-otlp`
non-gRPC variant and drop the grpc-netty dep (saves ~10 MB).

### Runtime cost

- **JsonTemplateLayout vs PatternLayout:** per the log4j docs and matching
  community benchmarks, JsonTemplateLayout is ~5% slower than PatternLayout
  on dense INFO-volume workloads, and roughly on par with logback's
  LogstashEncoder. At Gemma's log volume (steady state: dozens of lines/s)
  this is invisible.
- **OTel SDK:** ~5 s JVM startup, ~30 MB heap baseline, ~0.3 µs per `span.end()`
  at the default exporter. Concerning only if Gemma is launching CLI commands
  per request — which it is not.
- **OTLP exporter:** batches with a 5s linger; CPU dominated by gRPC framing
  unless sampling is aggressive (recommended: 5%).

### What is *not* a new dependency

- `log4j-layout-template-json` is shipped from the same Apache release as
  `log4j-core`. There is no "third-party log4j layout" risk.
- `jackson-core` is already on the classpath via Jackson-databind (Jersey +
  Spring), so JsonTemplateLayout adds zero transitive pull on Jackson.
- SLF4J: nothing to change. `log4j-slf4j2-impl` is the binding today and
  stays the binding.

---

## 6. Out of scope (for now)

- **Log aggregation backends.** Loki, Elasticsearch, OpenSearch, Splunk —
  those are ops infrastructure decisions, independent of how Gemma emits its
  logs. JSON-line emission makes all four trivially shippable later
  (Promtail, Filebeat, FluentBit, Vector all parse JSON natively).
- **Metrics export.** Already covered: Micrometer + (planned) Prometheus
  endpoint per `ACTUATOR_RECCE.md`. OpenTelemetry's `MeterProvider` could
  *eventually* replace Micrometer, but Phase 3 stays on the Micrometer pipe
  to avoid a metrics-system swap on top of a logging swap.
- **Logback migration.** log4j2 is mature, fast, and already wired. No
  benefit to switching binders. SLF4J at the API level means downstream
  code never sees the change either way.
- **Replacing the Slack and audit appenders.** Both have format contracts
  (Slack: webhook payload; audit: ops greps it). They stay PatternLayout/custom.
- **JS log endpoint format change.** `gemma-javascript.log` is consumed by a
  browser→server endpoint with its own line format; out of scope.
- **CLI console output.** Humans read it; stays coloured PatternLayout.
- **Test log format.** CI scrapes greppable plain text; tests stay
  PatternLayout. JSON in test logs would make `mvn verify` output unreadable
  in `tail -f` and break a handful of existing log-pattern-scraping CI
  helpers.

---

## 7. Suggested commit sequencing

1. `Phase 3 logging: JsonTemplateLayout on production file appender (opt-in)`
   — Phase 1 only, defaulting to PatternLayout under a system property.
   No behaviour change for ops until they flip the flag.
2. `Phase 3 logging: requestId + userId MDC filter`
   — Phase 2. Behind no flag; MDC keys appear in both PatternLayout (`%X{requestId}`)
   and JSON output. Required: update the PatternLayout `gemma.log.pattern` to
   include `%X{requestId} %X{userId}` so legacy log readers see them too.
3. `Phase 3 logging: OpenTelemetry SDK behind 'otel' profile`
   — Phase 3. Inactive by default.
4. `Phase 3 logging: OTLP exporter env-driven`
   — Phase 4. Inactive until `OTEL_EXPORTER_OTLP_ENDPOINT` is set.

Each step independently revertable.

---

## 8. Open questions for the maintainer

1. **Do we want `requestId` *also* echoed onto every HTTP response** as
   `X-Request-Id`? Recommended yes — lets external callers correlate a
   support ticket to a specific log line without ops involvement. ~3 extra
   LoC in the filter.
2. **CLI `requestId` semantics.** Per CLI invocation, or per "logical
   operation" inside a CLI (e.g., per-experiment in a batch loader)? Probably
   per invocation; per-experiment is overkill until we have an aggregator.
3. **`experimentId` MDC key — opt-in via AOP or explicit `CloseableThreadContext`
   in hot paths?** AOP at service-layer entry is most consistent but
   touches a lot of files. Recommend: explicit `try-with-CloseableThreadContext`
   only in the half-dozen long-running paths (preprocessing, DEA,
   coexpression analysis) where correlation actually pays off.
4. **OTel collector** — does the lab already have a Honeycomb/Tempo/Grafana
   Cloud account, or do we need to provision a self-hosted `otel-collector`
   container? Determines Phase 4 ETA, not technical approach.

---

*End of recce. Doc length budget: 500 lines.*
