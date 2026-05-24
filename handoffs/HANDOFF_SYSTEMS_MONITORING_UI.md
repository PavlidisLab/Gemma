# Handoff to GB — Systems Monitoring panel for gemma-curation-ui

Date: 2026-05-23
Backend branch: `phase2-acl-migrate` (Gemma 2.0)
Server-side scope: closed for this round; UI is yours to build.

Goal: rebuild the legacy `pages/admin/systemStats.jsp` + `pages/admin/activeUsers.jsp` as a
first-class Systems Monitoring panel in `gemma-curation-ui`. Several values should be
plotted over time (heap, query/cache counters, active sessions, queued jobs, …); see the
plotting section at the bottom for the recommended buffering pattern.

All `/admin/*` endpoints require `GROUP_ADMIN` (basic-auth or cookie). `/info` and
`/health` are anonymous-safe.

---

## Endpoint inventory

### Build / process identity (anonymous)

- `GET /rest/v2/info` — build version, gitHash, build timestamp, JVM identity, OS identity, uptime. Use this for the page header "Gemma 2.0 built on … from `<gitHash>`".
- `GET /rest/v2/health` — UP/DOWN component roll-up (db, cache, disk space). Returns 503 when any component is DOWN so the UI can show a top-bar alert without parsing JSON.

### JVM / OS resources (admin)

- `GET /rest/v2/admin/system`

  ```json
  {
    "data": {
      "heap":    { "usedBytes": 1234567890, "committedBytes": ..., "maxBytes": ... },
      "nonHeap": { "usedBytes": ..., "committedBytes": ..., "maxBytes": -1 },
      "threads": { "liveCount": 87, "daemonCount": 65, "peakCount": 142 },
      "startTimeMillis": 1716489600000,
      "uptimeMillis": 432000000,
      "osName": "Linux", "osVersion": "5.15.0", "osArch": "amd64",
      "availableProcessors": 16,
      "systemLoadAverage": 1.42
    }
  }
  ```

  `maxBytes` may be `-1` when the JVM does not advertise a maximum (common for non-heap).
  `systemLoadAverage` may be `-1.0` on platforms where the JVM can't read it (some Windows JVMs).

### Hibernate stats (admin)

- `GET /rest/v2/admin/hibernate/stats` — flat snapshot of `SessionFactory.getStatistics()`: session opens/closes, transactions, flushes, prepared statements, queries executed, max-time query + its text, query-cache hit/miss/put, L2 hit/miss/put. `statisticsEnabled=false` means all counters will be zero — flag this in the UI.
- `POST /rest/v2/admin/hibernate/reset` — `204`. Zeroes the counters. Button on the panel.

### Cache stats (admin)

- `GET /rest/v2/admin/caches` — `{ count, names: [alphabetical] }`. Per-cache hit/miss numbers are NOT available on the current build (post-EhCache-2 era — the legacy `enableStatistics` / `disableStatistics` toggles are stubs). Show the list with "clear" buttons.
- `DELETE /rest/v2/admin/caches` — `204`. Clears every cache.
- `DELETE /rest/v2/admin/caches/{cacheName}` — `204` on success, `404` if no such cache. Confirm before firing — these are not free.

### Background jobs (admin)

- `GET /rest/v2/admin/jobs` — list of currently-tracked `SubmittedTask`s + counts by status (`queued`, `running`, `completed`, `failed`, `cancelling`, `unknown`). Tasks evict from the in-memory store ~10 minutes after completion, so this is a near-real-time view, not a historical job log.

  Sorted newest-first by `submittedAt`. Each row is a `TaskStatusValueObject` (see `gemma-rest/src/main/java/ubic/gemma/rest/TaskStatusValueObject.java` for the full shape — id, status, submittedAt, startedAt, finishedAt, owner, taskName, taskClass, optional message/error).

### Hibernate Search 7 indices (admin)

- `GET /rest/v2/admin/search/indices` — per-`@Indexed` entity:
  - `entityName` (JPA), `className`, `indexName` (Lucene sub-directory name)
  - `indexPath` (absolute) + `exists` + `lastModified` (mtime on disk)
  - `documentCount` (matchAll `fetchTotalHitCount`; `-1` on query failure, with `error`)
  - Top-level: `indexBase` (= `gemma.search.dir`), `totalDocumentCount`, `totalDocumentCountExact` (false if any per-index query failed)

  Rebuild actions are intentionally CLI-only (`IndexGemmaCLI`); this endpoint is read-only.

### Loaded ontologies (admin)

- `GET /rest/v2/admin/ontologies` (optional `?includeTermCount=true`)
  - One row per `OntologyService` bean (Mondo, PATO, CHEBI, Uberon, CellType, ExperimentalFactor, CellLine, HumanPhenotype, MammalianPhenotype, MouseDevelopment, Sequence, Gemma, OBI, GO, the unified TDB, …).
  - Per row: `className`, `name`, `description`, `enabled`, `loaded`, `initializing` (background thread alive), `initializationCancelled`, `inferenceMode` (NONE/TRANSITIVE/MICRO/MINI/FULL), `languageLevel` (FULL/DL/LITE), `searchEnabled`, `processImports`.
  - `termCount` is omitted by default — `getAllURIs()` walks the in-memory model on each request. Pass `includeTermCount=true` only on the detail view or on user demand.
  - If a bean throws while being inspected, the row carries `error: "ClassName: message"` and the rest of the ontologies still come back.
  - Top-level counters: `count`, `enabledCount`, `loadedCount`, `initializingCount`.

### Database connection pool (admin)

- `GET /rest/v2/admin/db/pool` — HikariCP snapshot:
  - `poolName`, `maximumPoolSize`, `minimumIdle`, `connectionTimeoutMillis`, `idleTimeoutMillis`, `maxLifetimeMillis` (configured limits)
  - `activeConnections`, `idleConnections`, `totalConnections`, `threadsAwaitingConnection` (live MX-bean numbers)
  - Returns 503 if the wired DataSource isn't HikariCP (test profile cases).
  - Worth plotting `activeConnections` and `threadsAwaitingConnection` over time — sustained `threadsAwaitingConnection > 0` is the pool-saturation early-warning.

### Curation-agents liveness (admin)

- `GET /rest/v2/admin/curation-agent/health` — out-of-process probe:
  - Configured via `gemma.curationAgent.healthUrl` (unset = `status: NOT_CONFIGURED`, no probe fired).
  - Timeout via `gemma.curationAgent.healthTimeoutMs` (default 3000).
  - `status` = `UP` (2xx/3xx), `DOWN` (4xx/5xx OR exception), or `NOT_CONFIGURED`.
  - Also returns `httpStatus`, `latencyMillis`, `error` (when DOWN).
  - **Always returns HTTP 200** even on DOWN — the UI can poll without triggering error handlers.

### Users (admin, read-only for now)

- `GET /rest/v2/admin/users` — every User row:
  - `username`, `email`, `enabled`, `isAdmin`, `groups[]` (alphabetical), `signupTokenPending` (true when a signup token exists and the user isn't enabled — i.e. never-completed invite), `signupTokenDate`.
  - Top-level: `total`, `enabledCount`, `pendingSignupCount`.
  - Mutations (`POST` create+invite, `PATCH` toggle enabled / role, `DELETE` soft-delete) deferred to a follow-up commit that introduces the tombstone columns. The legacy gemma-web `UserListController` still serves the equivalent write paths in the meantime.

### Authenticated sessions (admin)

- `GET /rest/v2/admin/sessions` — finally delivers the "FIXME table of authenticated users" the legacy `activeUsers.jsp` punted on for a decade.
  - `authenticatedUserCount` (distinct principals), `activeSessionCount` (sum of non-expired sessions across principals).
  - `principals[]`: `username`, `sessionCount`, `lastRequest` (most-recent activity across the user's sessions), `authorities` (alphabetical list of `GROUP_*` etc., null when the principal is a basic-auth string rather than a `UserDetails`).
  - Sorted by `lastRequest` desc (nulls last).
  - Anonymous sessions are NOT tracked by `SessionRegistry` — this is only authenticated activity.

### Prometheus scrape (machine, not admin panel)

- `GET /rest/v2/metrics` — token-gated (`X-Scrape-Token` header, value from `gemma.metrics.scrapeToken`). Disabled (returns 404) when the property is unset. This is for Prometheus/Grafana, NOT for the admin panel to poll directly. See the plotting section.

---

## Suggested UI layout

```
┌─ Systems Monitoring ──────────────────────────────────────────────────┐
│ Gemma 2.0 — built 2026-05-22 from 6bed581ed4  ·  UP  ·  uptime 5d 3h  │
├─ JVM / OS ───────────────────┬─ Hibernate ──────────────────────────────┤
│ Heap        7.2 / 12.0 GB    │ Queries executed   42,318  [reset]      │
│   ▁▂▃▄▆▅▆▇▆▅ (last 5 min)    │   max time         3,421 ms             │
│ Threads     87 live / 65 d   │ Sessions open      1,204                │
│ Load avg    1.42  (16 cpus)  │ Q-cache hit/miss   88% (1142/151)       │
├─ Background jobs ────────────┼─ Caches ──────────────────────────────────┤
│ running 3  queued 1  done 12 │ 47 caches  [clear all]                  │
│   ▆▆▆▇▆▅▄▃▂▁ queued history  │   ExperimentLoadCache    [clear]        │
│   table of submitted tasks   │   GeneSetCache           [clear]        │
│                              │   …                                     │
├─ Sessions ───────────────────┼─ Search indices ──────────────────────────┤
│ 6 users  ·  8 active sessions│ 12 indices · 4,213,008 docs total       │
│   alice (admin) 2 sess · 5s  │   ExpressionExperiment  18,402  ok      │
│   bob          1 sess · 14m  │   Gene                3,847,201 ok      │
│   …                          │   …                                     │
├─ Ontologies ─────────────────┴───────────────────────────────────────────┤
│ 14 loaded · 2 initializing · 1 disabled                                 │
│   Mondo            loaded   transitive   search                         │
│   PATO             loaded   transitive   search                         │
│   CHEBI            loading                                              │
│   …                                                                     │
└──────────────────────────────────────────────────────────────────────────┘
```

Use the existing curation-UI design tokens; don't introduce new ones for this panel.

---

## Plotting "data over time"

The backend exposes single-read snapshots, not historical series, on every endpoint except
`/metrics` (which exposes Prometheus exposition for the metrics-profile Micrometer registry).
Two paths, depending on how serious the time-series need is:

### Option A — Client-side polling buffer (recommended for the in-app panel)

For each metric we want to plot, poll the snapshot endpoint on a fixed interval (suggested
defaults below) and keep a ring buffer of `{ timestamp, value }` samples in browser memory
(or `IndexedDB` if you want it to survive a page refresh). Discard samples older than the
window you display (e.g. last 15 min, last 1 h). When the user closes the panel, stop polling.

Suggested poll intervals (cheap on the server, cheap on the wire):

| Metric                             | Source                          | Interval |
|------------------------------------|---------------------------------|----------|
| Heap used / committed              | `GET /admin/system`             | 5 s      |
| Thread count                       | `GET /admin/system`             | 5 s      |
| System load average                | `GET /admin/system`             | 10 s     |
| Hibernate query count + max time   | `GET /admin/hibernate/stats`    | 10 s     |
| Hibernate L2 / Q-cache hit ratio   | `GET /admin/hibernate/stats`    | 10 s     |
| Active session count               | `GET /admin/sessions`           | 30 s     |
| Job status counts                  | `GET /admin/jobs`               | 10 s     |
| Search index doc counts            | `GET /admin/search/indices`     | 60 s     |
| Ontology load / initializing count | `GET /admin/ontologies`         | 30 s     |

Use a single polling scheduler in the panel so the requests are batched in time —
don't spin up a timer per chart. Pause polling when the tab is hidden (`document.hidden`).

Plot styling: follow the user-global figure conventions in `~/.claude/CLAUDE.md` (flat,
gray-200 gridlines, ACCENT `#2563eb` for primary series; Tailwind tokens for everything
else). For these dashboard widgets, sparkline-style line charts work well — no axis
labels needed on small panels, but always show the current value as a big number next to
the sparkline.

### Option B — Prometheus + Grafana (for long-horizon, multi-host, cross-restart)

`/metrics` already exposes Micrometer Prometheus exposition when the `metrics` Spring
profile is active and `gemma.metrics.scrapeToken` is set. If we want graphs that survive
JVM restarts, span multiple hosts (when we deploy a second Tomcat), or retain weeks of
history, the right answer is to scrape into Prometheus and chart in Grafana — NOT to build
a server-side time-series store inside Gemma. Decision deferred until we deploy a second
host or care about cross-restart history; for now the in-app polling buffer is enough.

---

## Files

Backend (already landed on this branch):

- `gemma-rest/src/main/java/ubic/gemma/rest/AdminWebService.java`
- `gemma-rest/src/main/java/ubic/gemma/rest/monitoring/InfoWebService.java` (`/info`)
- `gemma-rest/src/main/java/ubic/gemma/rest/monitoring/HealthWebService.java` (`/health`)
- `gemma-rest/src/main/java/ubic/gemma/rest/monitoring/MetricsWebService.java` (`/metrics`)
- `gemma-rest/src/test/java/ubic/gemma/rest/AdminWebServiceTest.java`

OpenAPI spec is auto-generated from the annotations on these classes — the curation-UI
SDK should regenerate cleanly from a fresh build of `phase2-acl-migrate`.

## Open questions for you

1. Should the panel route be `/admin/system` in the curation-UI, or nested under an
   existing admin area? (Server route is `/rest/v2/admin/*`; UI route is your call.)
2. Do we want per-cache stats restored at some point? It would mean a backend revisit
   (post-EhCache-2 Spring `Cache` doesn't expose stats by default; we'd need a Caffeine
   `recordStats()` adapter). Park this until the panel exists and we feel the gap.
3. Force-logout of a session? `DELETE /admin/sessions/{sessionId}` would be straightforward
   (call `SessionInformation.expireNow()`) — but a destructive action that needs a
   confirmation modal. Add later if the panel feels incomplete without it.
