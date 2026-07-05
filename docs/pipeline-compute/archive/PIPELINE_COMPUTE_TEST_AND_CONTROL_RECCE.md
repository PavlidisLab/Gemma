# Pipeline compute: test harness + monitoring/control surface — recce

**Filed:** 2026-06-23. Builds on the shipped PipelineJob skeleton
(`V18__pipeline_jobs.sql`, `core/pipeline/*`, `AdminPipelineWebService`,
`InternalPipelineWebService`) catalogued in
`WORKFLOW_AND_COMPUTE_ARCHITECTURE.md` §3. Recce only — no production
code changed.

**Goal:** make the compute side (a) developable and testable *without a
real Slurm/Nextflow*, against a controllable mock that is the shared
contract fixture between Gemma and gemma-curation-ui (UIB); and (b)
complete the monitoring + control endpoints — logs, suspend/resume,
cancel, and the **retry / mop-up** flow that the run-and-done skeleton
doesn't model yet.

The driving use case, in the user's words: *a batch runs, some
experiments fail, so they have to be reattempted when appropriate — a
mop-up operation.* Run-and-done is wrong; partial failure + selective
reattempt is the normal path.

---

## 1. Current surface (shipped)

| Layer | What exists | Gap for this recce |
|---|---|---|
| Model | `PipelineJobBatch` (OPEN/CLOSED/CANCELLED), `PipelineJob` (PENDING→QUEUED→RUNNING→DONE/FAILED/CANCELLING→CANCELLED), `PipelineJobEvent` (kind: progress\|stage\|stderr\|killed\|error\|completed\|heartbeat) | no attempt chain, no failure classification, no batch disposition rollup |
| SPI | `PipelineScheduler {kind, submit, poll, cancel}`; `SchedulerHandle`, `SubmitRequest(gemmaJobId,…)`, `JobSnapshot` | no suspend/resume; no log fetch |
| Schedulers | `MockPipelineScheduler` (15s synthetic, poll-only, always succeeds); Nextflow/Luigi **stubs that throw** | mock can't fail, can't be scripted, can't push, no deterministic clock, no log lines |
| Reconciler | `JobReconciler` @Scheduled poll of stale non-terminal jobs | fine as-is; gains retry-aware skips (§3.4) |
| Service | `PipelineJobBatchService {submit, get, findByOwner, cancelBatch, cancelJob, recordEvent, findEvents, findStaleJobs}` | no `retryFailed`, no `suspend/resume`, no log accessor, no rollup |
| REST | `/admin/pipeline` (admin) submit/list/get/cancel + job events; `/internal/pipeline` push callback (bearer) | no retry, suspend, log, rollup, or live-stream endpoints |

The push callback is keyed by **Gemma's** `jobId`
(`POST /internal/pipeline/jobs/{jobId}/events`), round-tripped to the
scheduler via `SubmitRequest.gemmaJobId`. Keep that — it means the
scheduler side never needs to know Gemma's batch model.

---

## 1b. Where job state should live — envelope in MySQL vs delegated runtime (gating decision)

**The concern (2026-06-23):** persisting live job state in the main
Gemma MySQL (gemd) is the wrong default. This section gates everything
below it — the §2 retry tables, §4 log storage, and the whole monitoring
shape change depending on its answer.

The V18 tables conflate two different kinds of state:

| Kind | Example | Write rate | Right home |
|---|---|---|---|
| **Durable domain fact / provenance** | "curator X ran pipeline Y over these 12 EEs on date Z; final: 10 DONE / 2 FAILED-permanent; we retried EE123 twice" | tiny | **Gemma MySQL** — tied to the EE, to audit, to who-did-what. Real domain data. |
| **Live operational state** | queue position, progress %, current stage, stdout, heartbeats, reconcile churn | high, ephemeral | **NOT MySQL** — the orchestrator already owns it; mirroring it row-by-row into gemd is the smell. |

`PIPELINE_JOB_EVENT` (a row per progress tick) and the `JobReconciler`
poll loop are the offenders — they exist only to mirror operational state
the runtime already tracks.

**Recommended architecture: thin envelope + delegated control plane.**
Gemma persists only the durable envelope (batch + per-EE submission +
terminal result + retry-provenance). Live state, progress, logs, and the
retry/suspend/resume **control plane delegate to the orchestrator**.
Gemma's monitoring endpoints become a **proxy/projection** over the
runtime, not a poll-fed mirror. The mop-up itself is largely *not
Gemma's to build* — Nextflow's `-resume` (skip completed, re-run failed)
**is** the mop-up; Tower/Slurm already do logs, cancel, retry-failed.
Gemma records *that* EE123 was retried twice (provenance), not the
machinery of doing it.

**Counter-argument (kept honest):** a second mechanism is operational
weight on a monolith whose track is "modernise without splitting." The
original Pipelines recce chose MySQL+reconciler *specifically* to avoid a
broker. So the offload's cost is real, and the right mechanism depends on
the runtime committed to:

| Mechanism | Gives us | Cost |
|---|---|---|
| **Nextflow Tower / Seqera Platform** | live logs, per-task status, cancel, **resume = mop-up**, retry — natively. Gemma goes thin over its API. | a product (community ed. self-hostable; Pro licensed); API auth/network |
| **Slurm direct + small Redis cache** | ephemeral progress + log-tail in Redis (TTL/streams), no MySQL churn; resubmit via Slurm | one light new service; still hand-roll the envelope + resubmit logic |
| **Durable workflow engine (Temporal)** | retry policy, suspend/cancel signals, timers, durable state — first-class for partial-failure-and-reattempt | biggest new-infra bet; likely over-engineered here |
| **RabbitMQ/Modulith broker** (parked recce) | decouples the churn off MySQL | still build the state machine somewhere; broker to run |
| **Minimal: keep MySQL for envelope only** | drop `PIPELINE_JOB_EVENT` + reconciler; keep batch+submission+terminal | no new infra; doesn't add the live control-plane niceties — relies on the orchestrator's own UI for live ops |

### 1b.1 Two facts that narrow the choice

Before weighing options, two things already true in this codebase change
the calculus:

1. **Nextflow is the converging executor.** sc-annotation already runs
   Nextflow-on-Slurm; rnaseq is slated to port off Luigi to Nextflow
   (architecture doc §3.4). So the runtime isn't really open — it's
   "Nextflow + what monitors it." And Nextflow ships its own event
   stream: **`nextflow run -with-weblog <url>`** POSTs workflow/process
   events (submitted, started, completed, failed, with trace records) to
   an HTTP endpoint as JSON, live. That endpoint shape is *already what
   Gemma built*: `POST /internal/pipeline/jobs/{id}/events`. Nextflow can
   push straight into Gemma's existing callback — no broker, no Tower, no
   adapter to write.
2. **V18 already has the low-churn columns.** `PIPELINE_JOB` carries
   `LAST_PROGRESS_JSON` + `LAST_EVENT_AT` + `LAST_EVENT_KIND` — an
   overwrite-in-place snapshot of current state. The *only* churn source
   is the append-only `PIPELINE_JOB_EVENT` table (a row per tick). So the
   MySQL concern is fixed by **not persisting every progress tick as a
   row** — keep the snapshot (one overwrite per job) + persist only
   milestone/terminal events as durable facts. That's a write-policy
   change, not a re-platforming.

### 1b.2 Options in detail

**A. Nextflow-native, minimal-infra (recommended).** Nextflow *is* the
runtime; `-with-weblog` pushes events into Gemma's existing internal
callback; `-resume` (skip completed, re-run failed) *is* the mop-up;
logs live in the workdir / `.nextflow.log` and Gemma proxies a byte
range. Gemma persists only the thin envelope + the overwrite snapshot +
milestone/terminal facts.
- *Pros:* zero new infra; reuses the callback already built; delegates to
  the runtime we're already adopting; kills the `PIPELINE_JOB_EVENT`
  churn with a write-policy change; provenance stays in MySQL where it
  belongs; `-resume` gives correct, battle-tested mop-up for free.
- *Cons:* couples us to Nextflow's weblog/trace shape (stable, but
  versioned — pin it); no fancy dashboard out of the box (we're building
  the UI in gemma-curation-ui anyway, so this is a non-cost); live "tail"
  granularity is whatever Nextflow emits (process-level, not sub-second).
- *Pick if:* you want the smallest correct move. **This is the
  recommendation.**

**B. + Redis live-tail cache (optimization on A).** Same as A, but a
Redis stream with TTL absorbs the weblog firehose for fast,
fan-out-friendly UI reads instead of leaning on the MySQL snapshot.
- *Pros:* sub-second live updates across many concurrent jobs without
  touching MySQL; natural TTL for ephemeral state.
- *Cons:* one more service to run + monitor; unnecessary until the UI
  demonstrably needs faster-than-snapshot updates.
- *Pick if:* live monitoring of large concurrent batches becomes a
  measured pain. Add later, not now.

**C. Nextflow Tower / Seqera Platform.** A full monitoring/control product
over Nextflow: dashboard, logs, cancel, resume, retry, API.
- *Pros:* mature control plane; robust API; nothing to build for live ops.
- *Cons:* **redundant with the gemma-curation-ui goal** — the whole point
  is curators stay in the Gemma UI, not a second dashboard; self-hosting
  Seqera Platform Enterprise is a real deploy (own DB, licensing for the
  enterprise tier); another auth/network boundary.
- *Pick if:* we decide *not* to build pipeline monitoring in the curation
  UI and would rather adopt Tower's dashboard wholesale. Given the UI
  work is the stated goal, this mostly argues against Tower.

**D. Durable workflow engine (Temporal/Cadence).** Model the batch as a
workflow: activity retry policies, suspend/cancel signals, timers, durable
state.
- *Pros:* textbook fit for partial-failure-and-reattempt; first-class
  retry/suspend/saga; would also serve future cross-pipeline DAGs
  (handoff recce §9).
- *Cons:* heaviest new-infra bet (Temporal server + its datastore +
  workers); a lot of machine for an academic curation queue; duplicates
  what Nextflow `-resume` already does for the mop-up case.
- *Pick if:* orchestration grows well beyond one-pipeline-per-batch into
  complex multi-stage sagas. Premature now.

**E. RabbitMQ/Modulith broker** (the parked recce). Events through a
broker; durable state off MySQL.
- *Pros:* decouples churn; fan-out to multiple independent consumers;
  the event-schema discipline is already designed.
- *Cons:* a broker to run; you still build the state machine somewhere;
  `-with-weblog` → HTTP already gives the decoupling without the broker.
- *Pick if:* multiple heterogeneous external producers/consumers appear
  (the §4 revival trigger in the architecture doc). Not the case today.

**F. MySQL-envelope-only, no weblog.** The do-nothing-new baseline: drop
the churn table + reconciler, keep batch+submission+terminal, and rely on
Nextflow's own CLI/`.nextflow.log` for live ops (no Gemma live view).
- *Pros:* truly zero new moving parts.
- *Cons:* no live monitoring in Gemma at all — curators back to reading
  cluster logs; doesn't deliver the UI goal.
- *Pick if:* live monitoring turns out not to be wanted. Unlikely given
  the brief.

### 1b.3 Recommendation

**Option A — Nextflow-native, minimal-infra.** It's the smallest move
that fixes the actual concern (drop `PIPELINE_JOB_EVENT` churn via a
write-policy change; keep the snapshot + milestones), adds zero infra,
reuses the callback already shipped, and delegates the hard parts
(mop-up via `-resume`, logs via workdir) to the runtime we're already
adopting. Redis (B) is a clean *later* optimization gated on a measured
need; Tower (C) is redundant with the curation-UI goal; Temporal (D) and
the broker (E) are real options only if orchestration scope grows past
one-job-per-EE. Provenance — *that* EE was retried twice, by whom, when,
with what outcome — stays in MySQL under every option, because it's a
domain fact, not operational churn.

**What survives every option:** the durable envelope in MySQL (provenance
is a domain fact regardless of runtime), and the **scripted mock** (§5) —
which simply moves down a layer to mock the *orchestrator's control
plane* behind Gemma's proxy interface, so endpoints + UIB integration
proceed regardless of which runtime wins.

**Implication for the sections below:** §2 (retry) and §4 (logs) are
written MySQL-centric and are **conditional on this decision**. Under the
delegated model: §2's attempt columns shrink to a per-EE retry-count +
terminal-outcome on the envelope (the full attempt *machinery* lives in
the runtime); §4's log storage becomes pure proxy (no tail-in-MySQL
needed if the runtime serves logs). Read §2/§4 as "if we keep this in
MySQL"; the delegated model is the recommendation. The gating question is
**which runtime** (Q1 in §10) — answer that first.

---

## 2. The mop-up / retry model (the centrepiece)

> **Conditional on §1b.** The model below assumes Gemma owns the attempt
> machinery in MySQL. Under the recommended delegated model, the runtime
> owns attempts/retries and Gemma keeps only a per-EE retry-count +
> terminal outcome on the envelope. The *concepts* (attempt-not-mutate,
> failure classification, batch disposition) carry over either way — only
> the storage location changes.

### 2.1 Retry mints a new attempt; it does not mutate the failed job

A failed `PipelineJob` is **immutable history** — its events, error
message, scheduler handle, and timings are the debugging record. Retry
creates a **new** `PipelineJob` for the same `(batch, experiment)`,
linked to its predecessor. Never flip a `FAILED` job back to `PENDING`.

Why: curators (and agents) will retry repeatedly with tweaked params;
collapsing attempts loses "it failed on OOM, we bumped mem, it failed on
a bad SRA file, we swapped accession, third try worked." That chain is
the audit trail.

**Model delta on `PIPELINE_JOB`:**

```sql
ALTER TABLE PIPELINE_JOB
  ADD COLUMN ATTEMPT         INT          NOT NULL DEFAULT 1,   -- 1-based
  ADD COLUMN RETRY_OF_FK     BIGINT       NULL,                 -- previous attempt
  ADD COLUMN SUPERSEDED_BY_FK BIGINT      NULL,                 -- the retry that replaced this one
  ADD COLUMN FAILURE_CLASS   VARCHAR(16)  NULL,                 -- TRANSIENT | PERMANENT | UNKNOWN
  ADD CONSTRAINT FK_PIPELINE_JOB_RETRY_OF
      FOREIGN KEY (RETRY_OF_FK) REFERENCES PIPELINE_JOB (ID),
  ADD KEY IDX_PIPELINE_JOB_BATCH_EE_ATTEMPT (BATCH_FK, EXPERIMENT_FK, ATTEMPT);
```

- **Current attempt** for an `(batch, ee)` = the row with no
  `SUPERSEDED_BY_FK` (equivalently, max `ATTEMPT`). Rollups and the UI
  show current attempts; the chain is drill-down.
- Setting `SUPERSEDED_BY_FK` on the old row when a retry is minted makes
  "is this the live attempt?" a single-column check — no max() subquery
  in the hot list path. `SUPERSEDED_BY_FK` is the **one** column ever
  written on a terminal job, and it is **monotonic** (null → set once,
  never cleared) — so "immutable history" holds for everything that
  matters (state, events, error, timings) while still giving the O(1)
  is-current check.
- The three columns are **deliberately redundant**: `SUPERSEDED_BY_FK`
  (forward, for the is-current hot path), `RETRY_OF_FK` (back, to walk
  the chain on drill-down), `ATTEMPT` (denormalized 1-based counter for
  display + sort). Each serves a different read; none is reconstructed
  from the others on a hot path.

### 2.2 "When appropriate" = failure classification

The pipeline (or scheduler poll) reports *why* a job failed in the
`error` event payload; Gemma persists it to `FAILURE_CLASS`:

| Class | Meaning | Default mop-up |
|---|---|---|
| `TRANSIENT` | SRA throttle, OOM, node died, scheduler lost handle, network | **auto-eligible** for retry |
| `PERMANENT` | malformed input, no raw data, validation reject, unsupported chemistry | **not** retried without curator override |
| `UNKNOWN` | unclassified / no signal | surfaced; curator decides |

Classification source, in priority order: (1) explicit `failureClass` in
the pipeline's `error` event payload (the pipeline knows best); (2) a
Gemma-side heuristic mapping of scheduler exit codes / messages when the
pipeline doesn't say; (3) `UNKNOWN`. The heuristic is a small, overridable
table — don't over-engineer it; the pipeline reporting its own class is
the real answer.

This is where the parked broker recce's **event-schema discipline** pays
off (`WORKFLOW_AND_COMPUTE_ARCHITECTURE.md` §4): the `error` event
payload is a versioned record — `{failureClass, message, exitCode,
retryHint, stderrTailUri}` — shared as a JSON Schema both sides pin to.

### 2.3 Batch disposition (derived rollup)

`PIPELINE_JOB_BATCH.state` stays the curator's explicit lifecycle flag
(`OPEN` = I'm still working this, `CLOSED` = done with it, `CANCELLED`).
**Disposition is derived, not stored** — a rollup VO over current
attempts:

```
BatchRollup {
  total, pending, queued, running, done, failed, cancelled,   // counts
  failedRetryable,            // FAILED current attempts, FAILURE_CLASS=TRANSIENT
  failedPermanent,
  needsAttention: boolean     // state=OPEN && failed>0 && no live retry in flight
  terminal: boolean           // every current attempt is terminal
}
```

`needsAttention` is the mop-up signal that drives the UI's "this batch
isn't finished" badge. A batch is **not** "done" just because every job
reached a terminal state — it's done when the curator accepts the
outcome (closes it) or all current attempts are `DONE`.

### 2.4 Mop-up operation

Service: `retryFailed(batchId, RetrySpec)` and `retryJob(jobId, RetrySpec)`.

```
RetrySpec {
  onlyRetryable: boolean = true,    // default: skip PERMANENT failures
  jobIds: List<Long>? = null,       // null = all eligible failed current attempts
  paramsOverrideJson: String? = null // e.g. bump --mem, swap accession
}
```

Semantics:
- For each eligible job (FAILED, current attempt, not already superseded,
  matches filter): mint attempt N+1, copy params (apply override),
  `SUPERSEDED_BY_FK` the old row, dispatch through the scheduler.
- **Idempotency**: refuse to retry a job that already has a
  non-terminal successor (guards double-clicks and concurrent curators).
- Re-opens the batch to `OPEN` if it was `CLOSED`.
- Returns the new rollup so the UI repaints in one round-trip.

"Mop-up the whole batch" is then one call: `retryFailed(batchId, {})` —
retries every transient failure, leaves permanents for human eyes.

---

## 3. Control surface: cancel / suspend / resume

### 3.1 Cancel — exists, keep

`cancelBatch` / `cancelJob` already drive `CANCELLING → CANCELLED`. No
change beyond making them retry-aware (cancelling a job clears any
pending auto-retry).

### 3.2 Suspend / resume — new, scheduler-honest

Two genuinely different things hide under "suspend"; model both:

1. **Throttle / hold the batch dispatcher** (scheduler-agnostic, always
   available). A batch of 500 EEs shouldn't fire 500 `sbatch`es at once.
   Add a batch-level `maxConcurrent` + a `HELD` flag; a dispatcher pass
   only submits `PENDING` jobs up to the concurrency budget, and skips
   held batches. "Pause this batch" = set `HELD`; "resume" = clear it.
   This needs no scheduler support and covers the common operational
   want.
2. **Suspend a running job** (scheduler-dependent). Slurm can
   `scontrol suspend`; Nextflow **cannot** suspend a running process
   mid-flight. So this is an *optional* SPI capability:

```java
// default methods on PipelineScheduler — unsupported unless overridden
default void suspend(SchedulerHandle h) { throw new UnsupportedOperationException(); }
default void resume(SchedulerHandle h)  { throw new UnsupportedOperationException(); }
default boolean supportsSuspend() { return false; }
```

REST surfaces `supportsSuspend()` in the registry/capabilities response
so the UI hides the button when the active scheduler can't do it, and
returns **409 Conflict** if called anyway. Add a `SUSPENDED` job state
only if/when a scheduler that supports it lands (Slurm) — until then,
ship #1 (batch hold) and leave #2 as the capability-gated stub.

**Recommendation:** ship batch hold + `maxConcurrent` now (high value,
zero scheduler dependency); defer per-job suspend behind the capability
flag until the Slurm adapter exists.

---

## 4. Monitoring surface: events, logs, artifacts, live updates

### 4.1 Event timeline — exists

`findEvents(jobId, since, limit)` + `GET …/jobs/{jobId}/events?sinceMillis=`
is the structured timeline (progress/stage/error/completed). Keep; it's
the backbone of the per-job drawer.

### 4.2 Logs — new, do NOT store in MySQL

Real logs (`slurm-%j.out`, `.nextflow.log`, Cell Ranger output) are big
and live on the cluster filesystem. Two-tier:

- **Tail on terminal** *(fallback-only — see §1b)* — the pipeline
  includes the last ~4–8 KB of stderr in its terminal event payload
  (`stderrTail`). Useful when there's no runtime to proxy to; under the
  delegated model the runtime serves the log and Gemma stores no tail.
- **Full log on demand** — `GET …/jobs/{jobId}/log?offset=&limit=`
  proxies through the active `PipelineScheduler` to the underlying log
  (byte-range read of the file, or Tower/Slurm REST). New optional SPI
  method `readLog(handle, offset, limit) → LogChunk{bytes, nextOffset,
  eof}`. Mock returns scripted lines; Nextflow/Slurm read the file. The
  `offset` cursor gives incremental "tail -f" in the UI without
  re-fetching.

Artifacts (the `scp web_summary.html` killer): `GET
…/jobs/{jobId}/artifacts/{name}` streams a known output file from the
job workdir. Same SPI proxy pattern. Whitelist names per pipeline.

### 4.3 Live updates — poll first, SSE if needed

The UI wants a moving picture. Start with **incremental polling** (the
`sinceMillis` / `offset` cursors already make this cheap) at 2–5s while
a batch is non-terminal. Add SSE (`GET …/batches/{id}/stream`) only if
poll proves too laggy — it's a backstop, not a day-one requirement
(matches the architecture doc's §8 guidance). SSE through nginx is fine;
WebSocket is not worth it here.

---

## 5. The scripted mock — the shared contract fixture

The current mock is a smoke toy. To let UIB build the retry/log/suspend
UI and to test Gemma's endpoints, the mock must be **programmable,
deterministic, and able to fail**. This is the highest-leverage piece —
both repos develop against it.

### 5.1 ScriptedMockScheduler (evolve `MockPipelineScheduler`)

A scenario registry keyed by experiment id (or round-robin when
unspecified). Each scenario scripts an outcome:

```
Scenario {
  outcome: SUCCEED | FAIL | STALL,
  failureClass: TRANSIENT | PERMANENT | UNKNOWN,   // when FAIL
  stages: [ {afterMs, kind, payload} ],            // synthetic event script
  logLines: [String],                              // served by readLog
  transport: POLL | PUSH                            // which path to exercise
}
```

Three capabilities the smoke mock lacks:

1. **Can fail, with a class.** A canned `PARTIAL_BATCH` scenario fails
   ⌊N/3⌋ of N jobs as `TRANSIENT` — directly exercises mop-up. On retry,
   the *second* attempt succeeds (scenario is attempt-aware), so the UI
   dev sees fail → retry → green end to end.
2. **Deterministic clock.** Don't rely on wall-clock 5/10/15s. A
   `POST …/_mock/advance?ms=` (dev profile only) steps every scripted
   timer, so a test or a UI dev can drive a job through its whole
   lifecycle on demand. Tests assert without `Thread.sleep`.
3. **Both transports.** A scenario can `PUSH` (mock calls
   `recordEvent` itself, exercising the internal callback path) or
   `POLL` (mock only answers `poll`, exercising the reconciler). The
   smoke mock is poll-only; scripted covers both.

### 5.2 Dev-only mock control REST (UIB's live fixture)

Behind `@Profile("scheduler-mock")` so it can never exist in prod:

```
POST /admin/pipeline/_mock/scenario     {experimentId?, scenario}   set canned outcome
POST /admin/pipeline/_mock/advance       {ms}                        step the clock
POST /admin/pipeline/_mock/emit          {jobId, kind, payloadJson}  fire an arbitrary event
GET  /admin/pipeline/_mock/scenarios                                 list active scenarios
```

This makes the mock the contract UIB demos against: they POST a scenario,
submit a batch through the *real* `/admin/pipeline/batches`, advance the
clock, and watch the real endpoints emit the real shapes. No FastAPI
mock drift — the fixture *is* Gemma.

### 5.3 Scenario file as cross-repo contract

Canonical scenarios (`SUCCEED_FAST`, `PARTIAL_BATCH`, `ALL_TRANSIENT`,
`PERMANENT_REJECT`, `STALL_THEN_RECONCILE`) live as a JSON fixture under
`gemma-rest/src/test/resources/pipeline-scenarios/` AND ship to UIB. Both
sides pin to it; a CI diff catches drift, same discipline the broker
recce prescribed for event schemas.

---

## 6. Testing layers

| Layer | What | Where |
|---|---|---|
| Unit | state machine (`recordEvent` kind→state), retry minting + idempotency, failure classification, rollup math — no Spring | `gemma-core` `*Test` |
| Service IT | submit → scripted mock drives PARTIAL_BATCH → assert rollup.needsAttention → `retryFailed` → advance → assert all DONE; against gemdtest | `gemma-core` `@Tag("integration")` |
| Reconciler IT | STALL_THEN_RECONCILE: poll-only scenario, tick the reconciler, assert synthetic events land | `gemma-core` |
| REST IT | full admin surface incl. retry/suspend/log/rollup via JerseyTest5 + scripted mock | `gemma-rest` |
| Contract | scenario JSON fixture pinned in both repos; CI diff | shared |

The scripted mock + deterministic clock is what makes all of these fast
and non-flaky — no real network, no `Thread.sleep`, runs in `mvn verify`.

---

## 7. Proposed REST surface for UIB (consolidated)

Existing (keep): `GET /admin/pipeline/registry`, `POST …/batches`,
`GET …/batches[?state=&limit=]`, `GET …/batches/{id}`,
`POST …/batches/{id}/cancel`, `GET …/batches/{id}/jobs/{jobId}/events`,
`POST …/batches/{id}/jobs/{jobId}/cancel`,
`POST /internal/pipeline/jobs/{jobId}/events`.

New:

| Verb | Path | Purpose |
|---|---|---|
| GET | `/admin/pipeline/capabilities` | active scheduler kind + `supportsSuspend`, `supportsLog`, `supportsArtifacts` — UI feature-gates off this |
| GET | `/admin/pipeline/batches/{id}/rollup` | the `BatchRollup` (§2.3); cheap repaint source |
| POST | `/admin/pipeline/batches/{id}/retry-failed` | mop-up; body = `RetrySpec` (§2.4) |
| POST | `/admin/pipeline/batches/{id}/jobs/{jobId}/retry` | single-job retry |
| POST | `/admin/pipeline/batches/{id}/hold` · `/resume` | batch dispatcher hold (§3.2 #1) |
| PATCH | `/admin/pipeline/batches/{id}` | set `maxConcurrent`, note |
| POST | `/admin/pipeline/batches/{id}/jobs/{jobId}/suspend` · `/resume` | 409 if `!supportsSuspend` (§3.2 #2) |
| GET | `/admin/pipeline/batches/{id}/jobs/{jobId}/log?offset=&limit=` | incremental log tail (§4.2) |
| GET | `/admin/pipeline/batches/{id}/jobs/{jobId}/artifacts/{name}` | stream output file |
| GET | `/admin/pipeline/batches/{id}/jobs/{jobId}` | single current-attempt detail + attempt chain |
| GET | `/admin/pipeline/batches/{id}/stream` *(optional)* | SSE; add only if poll lags |

Wire-shape: snake_case via `@JsonProperty` per the curation-UI
convention (`PIPELINESTATUS_WIRE_AUDIT.md`), with a unit test pinning the
keys. Auth stays `GROUP_ADMIN` for now; the curator-vs-admin split is the
separate Tickets Decision 5.

---

## 8. Model + SPI deltas (summary)

- `PIPELINE_JOB`: `+ATTEMPT, +RETRY_OF_FK, +SUPERSEDED_BY_FK, +FAILURE_CLASS` (§2.1) → new Flyway `V23` (mysql) + h2 sister.
- `PIPELINE_JOB_BATCH`: `+MAX_CONCURRENT INT NULL, +HELD BOOLEAN NOT NULL DEFAULT FALSE` (§3.2).
- `PipelineScheduler`: default `suspend/resume/supportsSuspend` + optional `readLog(handle, offset, limit)` + `readArtifact(handle, name)`.
- `PipelineJobBatchService`: `+retryFailed, +retryJob, +holdBatch, +resumeBatch, +suspendJob, +resumeJob, +rollup, +readLog`.
- `JobReconciler`: skip jobs that have a live successor; honour batch `HELD` (don't reconcile held jobs into spurious states).
- New `ScriptedMockScheduler` (replaces/extends `MockPipelineScheduler`) + dev `_mock` REST + scenario fixtures.

No change to the push-callback keying or the `Ticket` linkage
(`WORKFLOW_AND_COMPUTE_ARCHITECTURE.md` §5.1: a `FAILED` job's terminal
failure can still auto-open a Ticket — orthogonal to retry, and complementary:
auto-ticket the PERMANENT failures, auto-retry the TRANSIENT ones).

---

## 9. Phased plan

**Slice 0 (gating, no code): pick the runtime** (§1b / §10 Q1). The
envelope-vs-delegated split and which mechanism back the control plane.
Everything below assumes the delegated model; the MySQL-only path is the
fallback if no runtime is committed.

1. **Scripted mock of the control plane + deterministic clock + `_mock`
   REST + scenario fixtures** (§5, reframed §1b). Mocks the *orchestrator*
   behind Gemma's proxy interface — so it's runtime-independent and
   unblocks UIB on day one. No schema change. **Buildable before slice 0
   resolves**, because it defines the proxy interface both real runtimes
   will implement.
2. **Thin envelope + proxy monitoring** (§1b): shrink/retire
   `PIPELINE_JOB_EVENT` churn; Gemma keeps batch + per-EE submission +
   terminal outcome + retry-count; live state/events/logs proxy from the
   runtime (or the scripted mock). The architectural correction.
3. **Mop-up surface** (§2 concepts, storage per slice 0): `retryFailed`/
   `retryJob` + rollup + failure classification — as a *delegation* to
   the runtime's resume/retry under the delegated model, or as MySQL
   attempt rows under the fallback.
4. **Batch hold + `maxConcurrent` + dispatcher throttle** (§3.2 #1).
5. **Log + artifact proxy endpoints** (§4.2).
6. **Capabilities endpoint + per-job suspend stub** (§3.2 #2, gated).
7. **(Deferred)** SSE stream; real `NextflowSlurmScheduler` /
   Tower-or-Slurm adapter (architecture doc §8 item 1).

Slice 1 is the heart of unblocking UIB and is buildable immediately; slices
2–3 wait on slice 0.

---

## 10. Open questions

### Gating (answer before slices 2–3)

1. **Which runtime / mechanism backs the control plane?** (§1b — full
   options analysis + pros/cons in §1b.2.) Recommendation: **Option A,
   Nextflow-native minimal-infra** — `-with-weblog` → Gemma's existing
   callback, `-resume` for mop-up, workdir proxy for logs, thin envelope
   in MySQL with the churn table dropped. Redis (B) is a later
   optimization; Tower (C)/Temporal (D)/broker (E) only if scope grows.
   Confirmation needs Paul + whoever owns the cluster — but A adds no
   infra, so it's low-commitment to start.
2. **maxConcurrent / aggregate cap.** Per-pipeline (rnaseq wants more than
   sc-annotation, architecture doc §3.4) or one global cap? Ties to
   Pipelines-recce Q7 (cluster admin's aggregate submission cap). Needs
   the cluster admin.

### Resolved in this refinement (recorded; reversible)

3. **Retry params override** → **batch-level** default (one override for
   all retried jobs); per-job override deferred until asked for.
4. **Auto-retry vs manual** → **manual-first**. Curator clicks "retry
   transient"; add a per-pipeline `autoRetryMax` only once the classifier
   is trusted — avoids retry storms. (Under the delegated model this is a
   parameter on the runtime's resume call, not a Gemma loop.)
5. **Failure-class source of truth** → **pipeline reports** it in the
   terminal event payload; Gemma's exit-code heuristic only fills
   `UNKNOWN`. Never let Gemma's guess override the pipeline's own class.
6. **Retry scope** → `retryJob` works on any **terminal** current attempt
   (FAILED, CANCELLED, and DONE-for-reprocess); `retryFailed(batch)` is
   the convenience that targets FAILED + retryable current attempts only.
7. **`needsAttention`** → simplified to `state==OPEN && failedCurrent>0`.
   A retry in flight supersedes the failure, so the count drops to 0
   automatically while the retry runs — no separate "in flight" clause.
8. **Mock leak to prod** → profile-gate on `scheduler-mock` **plus** a
   startup assertion that fails fast if `production` and `scheduler-mock`
   are both active. Belt and suspenders.

### Still open (lower stakes)

9. **Log tail size** on terminal events (4 KB vs 8 KB) and whether
   full-log proxying re-checks auth per fetch. Only matters under the
   MySQL-fallback; moot under pure proxy.
10. **Per-job "accept failure" without closing the batch** — a curator
    may want to acknowledge an unfixable PERMANENT failure so it stops
    flagging `needsAttention` while other jobs still run. Possible
    refinement (a per-submission `acknowledged` flag); not baked in —
    closing the batch is the accept action for now.

---

*Recce 2026-06-23. No production code modified.*
