# OpenTelemetry tracing — Phase 3 detailed recce

**Status:** recce only. No code written. No `mvn` invoked. No `pom.xml` edits
made in this commit.
**Baseline:** branch `worktree-agent-a00c1c183b55fda9b` from
`phase2-acl-migrate@976d659d47`.
**Scope:** flesh out Phase 3 of `LOGGING_MODERNIZATION_RECCE.md` — the
OpenTelemetry slice. Phases 1 + 2 of that doc (`JsonTemplateLayout` plus
`RequestIdMdcFilter`) are already merged into `phase2-acl-migrate`; this
recce picks up where they stop and proposes a Java-agent-first path to
distributed tracing.
**Companion doc:** `LOGGING_MODERNIZATION_RECCE.md` §2.3 Phase 3 + §4–§7
remain the canonical reference for sequencing and cost. This file deepens
those sections only.

---

## 1. What does Phase 3 buy us?

- **`traceId` per request → cross-service log join.** Today a failing
  `/rest/v2/datasets/123/expression` request lands in `gemma.log` with
  the Phase 2 requestId — but a downstream gemma-cli job kicked off by
  that request lands in a different log file with no link. With a W3C
  `traceparent` header propagated by the agent, both records share a
  16-byte `traceId`; `jq 'select(.traceId=="abc…")' gemma.log gemma-cli.log`
  becomes a single pipeline.
- **`spanId` per operation → "which DB call was slow."** Auto-instrumentation
  emits a span around every JDBC statement, every Hibernate session, every
  outbound `HttpClient` call, every Jersey resource method, every Spring
  MVC handler. Any OTel-compatible backend (Tempo, Honeycomb, Jaeger,
  Grafana Cloud) shows a Gantt of where the 4-second request went —
  usually one Hibernate N+1 hot spot the current logs can't point at.
- **Free instrumentation of stuff we don't own.** The agent ships
  built-in instrumentations for Spring MVC, Jersey 2.x, Hibernate 5/6,
  JDBC, Apache HttpClient 4/5, JMS, gRPC, Quartz, the Servlet API.
  Gemma exercises all of those; no source changes needed.
- **Future-proofing for microservice extraction.** If gemma-rest splits
  off from gemma-web, or gemma-curation-ui makes server-to-server calls,
  the propagated `traceparent` glues their logs together for free.

Phase 3 is "turn the agent on; logs start carrying trace IDs; spans
show up in the backend." Not "scatter manual `Tracer` calls everywhere."

---

## 2. Java agent vs SDK programmatic — what to pick

### 2.1 The two options

| Aspect | OTel Java agent | OTel SDK programmatic |
|---|---|---|
| Deploy artifact | One JAR (`opentelemetry-javaagent.jar`, ~25 MB) attached via `-javaagent:` | 4–6 Maven deps (~700 KB direct, ~12 MB with gRPC exporter) |
| Source change | **Zero.** No `pom.xml`, no Java config bean. | `pom.xml` deps + `@Configuration` class wiring `OpenTelemetrySdk` |
| Auto-instrumentation | Spring MVC, Jersey, Hibernate, JDBC, HttpClient, JMS, Quartz, Servlet — all built in | None. Only what you wrap manually in `tracer.spanBuilder(...)` |
| MDC injection | Built in (agent ships its own `log4j-context-data` provider on a child classloader) | Requires the `opentelemetry-log4j-context-data-2.17-autoconfigure` artifact on the app classpath |
| Manual spans | `@WithSpan` annotation works once `opentelemetry-instrumentation-annotations` is on the app classpath (compile-only dep) | `tracer.spanBuilder("foo").startSpan()` everywhere |
| Cost | ~25 MB image bump, ~3–5 s JVM startup, ~5–10 MB resident, ~2–5% CPU at default sampling | ~12 MB classpath bump, ~1–2 s startup, ~30 MB resident, ~0.3 µs/`span.end()` |
| Update cadence | Bump one JAR version; no Maven coordination needed | Bump `${otel.version}`, re-test BOM compatibility |
| Turn off | Remove `-javaagent:` flag, restart | Drop the `otel` Spring profile, restart |

### 2.2 Recommendation

**Agent-first.** Specifically:

- **Phase 3a** (the cheap PoC, ~10 LoC of Dockerfile + env vars):
  drop the agent JAR into the container image, add `-javaagent:` to
  `CATALINA_OPTS`, set `OTEL_EXPORTER_OTLP_ENDPOINT` to the collector,
  done. Logs gain `traceId`/`spanId` MDC keys automatically; trace
  Gantt charts show up in the backend without touching a single `.java`
  file.
- **Phase 3b** (only if a specific hot path needs manual spans): add
  `opentelemetry-instrumentation-annotations` as a `compile`-scope dep
  in `gemma-core/pom.xml` (~5 KB), annotate the handful of long-running
  service methods (preprocessing, DEA, coexpression) with `@WithSpan`.
  This is `~15 LoC` of pom + N annotations, fully reversible.
- **Phase 3c (deferred indefinitely)**: programmatic SDK. Only worth it
  if we hit a concrete limitation of the agent (e.g., need a custom
  exporter the agent doesn't ship, or need to instrument an in-house
  protocol the agent doesn't know about). Today there is no such limit.

The existing Phase 3 stub in `LOGGING_MODERNIZATION_RECCE.md` §2.3
proposes the SDK route as the primary path. This recce supersedes it:
**agent-first**, SDK as deferred fallback.

---

## 3. MDC integration — how `traceId`/`spanId` reach the JSON log line

The agent ships `io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-log4j-context-data-2.17`
as one of its built-in instrumentations. On every log event it auto-writes
two keys into log4j `ThreadContext` (MDC):

- `trace_id` — 32-char lowercase hex of the W3C TraceId
- `span_id` — 16-char lowercase hex of the active SpanId

If no span is active (e.g., a background thread outside any traced
operation), both keys are absent — they do not show up as empty strings
or nulls.

The Phase 1 JSON template (`gemma-core/src/main/resources/gemma-log-template.json`,
already merged) emits:

```json
"mdc": {"$resolver": "mdc", "flatten": false},
"traceId": {"$resolver": "mdc", "key": "trace_id"},
"spanId":  {"$resolver": "mdc", "key": "span_id"}
```

The `traceId`/`spanId` top-level fields already exist in the template
and currently emit `null`. **No template edit is needed for Phase 3a.**
Once the agent is attached, they start carrying real values; without the
agent, they remain `null`. The structured-log consumer downstream (Loki,
Elasticsearch, whatever) can treat them as optional.

Confirmed against `LOGGING_MODERNIZATION_RECCE.md` §4: the template
already pulls from `mdc:trace_id` / `mdc:span_id`. Good — no churn.

---

## 4. Exporter — OTLP/HTTP over OTLP/gRPC

| Aspect | OTLP/gRPC | OTLP/HTTP |
|---|---|---|
| Wire format | Protobuf over HTTP/2 (gRPC) | Protobuf over HTTP/1.1 |
| Transport dep | `io.grpc:grpc-netty-shaded` (~10 MB fat JAR, native epoll bits) | Java's stock `java.net.http.HttpClient` (JDK 11+, zero new deps) |
| Image bump | ~10 MB | 0 |
| Throughput | Higher (HTTP/2 multiplexing) | Lower but still way above Gemma's needs |
| Firewall friendliness | Some corp networks block HTTP/2 or unknown ports | Plain HTTPS on 4318 — universally allowed |
| Default endpoint | `:4317` | `:4318/v1/traces` |

**Recommend OTLP/HTTP** for Gemma. The agent picks the protocol from
`OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf` (default in recent agents is
gRPC, so set this explicitly). 10 MB image savings and one less dep
class of failure to debug; throughput is irrelevant at our log/trace
volume.

If a future deployment needs gRPC (typically only when bandwidth-bound
on huge trace volumes), it is a one-env-var flip, no rebuild.

---

## 5. Minimum-viable PoC — concrete diff (not for commit)

Three changes, all in the container layer. Zero `pom.xml` edits, zero
Java source changes.

### 5.1 Download the agent JAR into the image

Add to `Dockerfile`, after the runtime stage's `RUN groupadd` block:

```dockerfile
# OpenTelemetry Java agent. Version pinned for reproducibility.
# Source: https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases
ARG OTEL_AGENT_VERSION=2.10.0
ADD --chmod=0644 \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar \
    /opt/opentelemetry-javaagent.jar
```

(Or, if the build is air-gapped, vendor the JAR into `docker/` and `COPY`
it instead. SHA-256 should be pinned either way — `ADD --checksum=sha256:…`
is supported by buildkit 1.6+.)

### 5.2 Wire `-javaagent:` into `CATALINA_OPTS`

Replace the existing `ENV CATALINA_OPTS=...` block in `Dockerfile`:

```diff
 ENV GEMMA_APPDATA_HOME=/data/gemma \
     CATALINA_OPTS="-Dgemma.appdata.home=/data/gemma \
                    -Dspring.profiles.active=production \
                    -Djava.security.egd=file:/dev/./urandom \
                    -XX:MaxRAMPercentage=75.0 \
-                   -XX:+ExitOnOutOfMemoryError"
+                   -XX:+ExitOnOutOfMemoryError \
+                   -javaagent:/opt/opentelemetry-javaagent.jar"
```

The agent is a no-op when no OTLP endpoint is configured (it falls back
to the `none` exporter for traces and emits nothing). Safe to bake into
every image; activation is purely env-driven below.

### 5.3 Env-var activation

Add to `docker/env.smoke.example`:

```bash
# --- OpenTelemetry tracing (optional) -----------------------------------
# Leave OTEL_TRACES_EXPORTER=none to disable tracing entirely. Set to
# otlp + point OTEL_EXPORTER_OTLP_ENDPOINT at a collector to start
# emitting spans. See OPENTELEMETRY_RECCE.md for the full env list.
OTEL_SERVICE_NAME=gemma-rest
OTEL_TRACES_EXPORTER=none
# OTEL_TRACES_EXPORTER=otlp
# OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
# OTEL_EXPORTER_OTLP_ENDPOINT=https://collector.example.com:4318
# OTEL_EXPORTER_OTLP_HEADERS=authorization=Bearer ${COLLECTOR_TOKEN}
# OTEL_RESOURCE_ATTRIBUTES=deployment.environment=smoke,service.version=1.32.0-SNAPSHOT
# OTEL_TRACES_SAMPLER=parentbased_traceidratio
# OTEL_TRACES_SAMPLER_ARG=1.0   # dev/smoke: 100%; prod: 0.05
# OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED=true
# OTEL_METRICS_EXPORTER=none    # leave metrics off; Micrometer still owns those
# OTEL_LOGS_EXPORTER=none       # log forwarding is Phase 4, not 3
```

**Total diff: ~5 lines of Dockerfile + ~15 lines of env example.**

### 5.4 What to verify after a smoke run

With a local `otel-collector` container (config: `receivers.otlp.protocols.http`,
`exporters.logging.verbosity: detailed`) on `:4318`:

1. `docker compose up otel-collector gemma-rest` with
   `OTEL_TRACES_EXPORTER=otlp` and a sampler arg of `1.0`.
2. `curl -i http://localhost:8080/rest/v2/datasets?limit=1` — assert the
   response carries `traceparent: 00-<32hex>-<16hex>-01`.
3. `docker logs gemma-rest | jq 'select(.traceId)' | head` — assert
   `traceId` matches the value from step 2 and `spanId` is non-null.
4. `docker logs otel-collector` — assert at least these span names:
   `GET /rest/v2/datasets` (Servlet), `DatasetsWebService.getDatasets`
   (Jersey resource), `SessionImpl.list` (Hibernate), `SELECT …`
   (JDBC). Their `traceId`s all match step 2.
5. Kill the collector mid-test (`docker stop otel-collector`); send 50
   more requests; assert gemma-rest keeps responding 200 (the agent's
   default `BatchSpanProcessor` drops spans on backpressure rather than
   blocking the request thread).
6. Heap snapshot before/after a 5-minute soak at 5 req/s — assert
   resident growth < 30 MB.

---

## 6. Operational concerns

### 6.1 Sampling

The agent samples per-trace at the entry point. Configuration:

- **Dev / smoke:** `OTEL_TRACES_SAMPLER=parentbased_traceidratio`,
  `OTEL_TRACES_SAMPLER_ARG=1.0` — 100% of traces. Volumes are tiny;
  debuggability wins.
- **Prod:** `OTEL_TRACES_SAMPLER_ARG=0.05` — 5% sampled. Gemma serves on
  the order of 10–100 req/s peak; 5% keeps the collector load reasonable
  while still catching enough traces to diagnose tail-latency issues.
  Bump to 1.0 transiently when investigating a specific incident.
- **`parentbased_…` matters**: when a request comes in with an inbound
  `traceparent` header, honour the upstream sampling decision; only sample
  fresh-root requests at the configured ratio. Without `parentbased_`,
  inter-service traces fragment.

### 6.2 PII in spans

Default agent config records: HTTP method, URL **path** (not query
string), status code; servlet route template (`/datasets/{id}`, not
`/…/123`); DB statement text with parameters redacted to `?`; JDBC URL
without credentials.

Gemma-specific risks:

- **`Authorization` headers**: not captured by default. Keep
  `otel.instrumentation.http.{client,server}.capture-request-headers`
  empty; override only for explicitly safe headers like `X-Request-Id`.
- **Query strings**: some endpoints take `?query=…` free text — verify
  stripped from `http.target` in smoke step 4.
- **User emails in URL paths**: a few legacy endpoints take an
  email-shaped username as a path param. Span name templates it
  (`{username}`) but `http.target` records the resolved path.
  Either set `otel.instrumentation.http.server.emit-resolved-url=false`
  or hash in a custom span processor — defer to 3b unless it shows up.
- **Hibernate SQL parameters**: redacted to `?` by default. Do **not**
  flip `otel.instrumentation.jdbc.statement-sanitizer.enabled=false`;
  Gemma queries touch experiment data that aggregates to user-identifying.

### 6.3 Hibernate SQL spans

The prime motivator for tracing. Caveat: each Hibernate session can
issue dozens of SELECTs per HTTP request (lazy-load chains, L2 cache
misses); at 100% sampling and 100 req/s that's order-10⁴ spans/sec.
Within budget but noisy in the backend UI. Mitigations: lower the
sampler in prod (5% recommended); or disable JDBC spans while keeping
Hibernate spans (`OTEL_INSTRUMENTATION_JDBC_ENABLED=false` — loses
per-statement timing, keeps per-session). Start with both on; dial back
if the backend complains.

### 6.4 Other gotchas

- **Startup.** Agent class-scan adds ~3–5 s; Tomcat's HEALTHCHECK
  `--start-period=180s` already absorbs it.
- **WAR classloader.** Agent attaches to the system classloader ahead
  of Tomcat's per-webapp loader; no conflict with the WAR's
  log4j2/jersey/hibernate jars per the agent's compat matrix.
- **Boot noise.** First boot logs a few INFO lines from
  `io.opentelemetry.javaagent.bootstrap`; quiet to `WARN` in
  `log4j2.xml` post-merge.
- **CLI.** Same agent works for gemma-cli; set
  `OTEL_SERVICE_NAME=gemma-cli`, `otel.bsp.export.timeout=10s` for
  flush on JVM exit. Not on the 3a critical path.

---

## 7. Phase 3 effort estimate

| Sub-phase | Files touched | LoC | Risk |
|---|---|---:|---|
| 3a — agent in image, env activation | `Dockerfile`, `docker/env.smoke.example`, 1 paragraph in `CONTAINER_CONFIG.md` | ~25 | Low — fully reverts by removing `-javaagent:` |
| 3b — `@WithSpan` on 3–5 hot paths (optional) | `gemma-core/pom.xml` (+1 dep), 3–5 service classes | ~30 | Low — annotations are no-ops without the agent |
| 3c — programmatic SDK (deferred) | `pom.xml`, `gemma-core/pom.xml`, new `OpenTelemetryConfig.java` | ~80 | Medium — only if 3a is insufficient |

**Total Phase 3a (the recommended slice): ~25 LoC across 3 files, ~1–2
hours of work plus a smoke-test cycle.** Phase 3b is opportunistic and
should land only after one real "we wish we had a span here" incident.
Phase 3c stays parked.

Phase 4 (OTLP exporter wiring + sampling tuning in prod) collapses into
Phase 3a under this plan — there is no separate exporter step, because
the agent ships its own OTLP exporter. The only Phase 4 work left is
**deploying a collector**, which is ops infrastructure, not code.

---

## 8. Out of scope

- Log forwarding via OTLP (`OTEL_LOGS_EXPORTER`). The JSON log file is
  the contract; ops can ship it with Promtail/Filebeat/Vector. Trace-log
  correlation lives in the `traceId` field on each line, not in OTLP
  log records.
- Metric export via OTel. Micrometer already covers metrics per
  `ACTUATOR_RECCE.md`; do not double-bill.
- Replacing Phase 2's `RequestIdMdcFilter`. The Phase 2 filter generates
  a `requestId` MDC key for in-process correlation regardless of whether
  tracing is on. Keep it. `traceId` and `requestId` answer different
  questions ("which trace?" vs "which inbound HTTP call?") and a future
  microservice may carry one but not the other.
- Manual `tracer.spanBuilder(...)` calls. If we ever need them, that's
  Phase 3c, not 3a/3b.

---

## 9. Open questions

1. **Collector endpoint.** Does ops have a Tempo/Honeycomb/Grafana Cloud
   target ready, or do we deploy `otel-collector` ourselves? Same
   question as `LOGGING_MODERNIZATION_RECCE.md` §8.4; Phase 3a stays
   parked behind `OTEL_TRACES_EXPORTER=none` until answered.
2. **Service naming.** `gemma-rest` for the REST WAR, `gemma-cli` for
   batch jobs, `gemma-web` if/when the legacy webapp gets the agent.
   Confirm before any production rollout — `service.name` is the join
   key in every backend UI.
3. **Agent version cadence.** OTel Java agent ships monthly. Pin to a
   minor (`2.10.x`) in `Dockerfile`, refresh quarterly unless a CVE
   forces it. Document in `CONTAINER_IMAGE_RECCE.md`.
