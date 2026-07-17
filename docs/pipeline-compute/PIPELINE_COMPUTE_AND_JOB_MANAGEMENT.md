# Pipeline compute & job management — developer handoff

## Start here

This is the working handoff for finishing Gemma's pipeline/job-management
and distributed-compute (Nextflow) work. A `PipelineJob` skeleton and the
`Ticket` layer have **shipped**; the mop-up/retry model, real cluster
schedulers, the monitoring/control surface, and the curator UI are
**not**. Read §Implementation status for what exists and its real entry
points, then §Remaining work for the ordered task list.

**RESOLVED 2026-07-13 (Ogan): delegated / Nextflow-native (Option A).** The
gating question below is settled — live state delegates to Nextflow, Gemma keeps
the thin durable envelope. Gemma-internal tasks (2, 3) proceed now; the
infra-facing commitments still need the cluster owner at task 7 (see §7 task 0).

**The (now-resolved) question that gated slices 2+:** *where does live job state
live — mirrored row-by-row in Gemma's MySQL, or delegated to the
orchestrator (Nextflow) with Gemma keeping only a thin durable envelope?*
The chosen answer is the **delegated / Nextflow-native** model
(§3.1, §4.2 Option A): `nextflow run -with-weblog` pushes events into the
callback Gemma *already built*, `-resume` **is** the mop-up, logs proxy
from the workdir, and Gemma persists only batch + per-EE submission +
terminal outcome + retry-provenance. It adds no infrastructure, so it is
low-commitment to start. Confirm with Paul + whoever owns the cluster
before building slices 2–3. Everything downstream reads as "delegated
model is the default; MySQL-only is the documented fallback."

The driving concern, in the user's words: *a batch runs, some experiments
fail, so they have to be reattempted when appropriate — a mop-up
operation.* Run-and-done is the wrong mental model; partial failure plus
selective reattempt is the normal path. Build around that.

**Scope note.** This document consolidates four overlapping recces filed
across separate sessions (2026-05-19 → 2026-06-23) into one dev-executable
handoff and supersedes them for day-to-day reference; the sources are
archived (see [Provenance](#provenance)). No production code is modified by
this document itself.

---

## Implementation status

Legend: ✅ shipped · 🟡 partial (skeleton exists, gaps noted) · ⬜ not started.

| Component | State | Concrete entry points (verbatim anchors) |
|---|---|---|
| **Ticket layer** (Spine 1) | ✅ | `TICKET` / `TICKET_TARGET` / `TICKET_EVENT` tables (`V3__ticket_layer.sql`, `V19__ticket_mode_and_target_status.sql`); `TicketService`, `TicketDao*`, `TicketsWebService`; events `TicketOpenedEvent`, `TicketStateChangedEvent`, `TicketAssignedEvent`, `TicketMetadataChangedEvent`, `TicketTargetStatusChangedEvent` |
| **PipelineJob model** (Spine 2) | ✅ | `PIPELINE_JOB_BATCH` / `PIPELINE_JOB` / `PIPELINE_JOB_EVENT` (mysql `V18__pipeline_jobs.sql` + h2 sister `V23_1__pipeline_jobs.sql`); `model/pipeline/*`, `persistence/service/pipeline/*` |
| **Scheduler SPI** | ✅ | `PipelineScheduler {kind, submit, poll, cancel}` + `SubmitRequest(gemmaJobId,…)`, `SchedulerHandle`, `JobSnapshot`, and the additive optional caps `supportsLog`/`readLog`, `supportsArtifacts`/`readArtifact`, `supportsSuspend`/`suspend`/`resume` (tasks 5, 6). |
| **Schedulers (impls)** | 🟡 | `ScriptedMockScheduler` (deterministic virtual clock, PUSH/POLL, failure-capable, profile `scheduler-mock`; task 1). `NextflowSlurmScheduler` **built** — `submit`/`poll`/`cancel` (sbatch head job + squeue/sacct/scancel + weblog ingest), unit-tested; remaining = O8 default wiring + end-to-end run (task 7). `LuigiScheduler` = stub that throws. |
| **Reconciler** | ✅ | `JobReconciler` `@Scheduled` poll of stale non-terminal jobs via the `(state, last_event_at)` index. |
| **Batch service** | ✅ | `PipelineJobBatchService` — base ops + `retryFailed`/`retryJob`/`computeRollup` (task 3), `holdBatch`/`resumeBatch`/`updateBatch`/`dispatchPending` (task 4), `readJobLog`/`readJobArtifact` (task 5), `capabilities`/`suspendJob`/`resumeJob` (task 6). |
| **REST — admin** | ✅ | `AdminPipelineWebService` at `/admin/pipeline` — submit/list/get/cancel + events, retry-failed/retry, hold/resume, `PATCH` maxConcurrent, log, artifacts, rollup, capabilities, suspend/resume (409 stub). SSE stream deferred (task 12). |
| **REST — push callback** | ✅ | `InternalPipelineWebService`: `POST /internal/pipeline/jobs/{jobId}/events` (Gemma-native `{kind,payloadJson}`) **and** `POST …/jobs/{jobId}/weblog` (raw Nextflow `-with-weblog` → `NextflowWeblogTranslator` → `recordEvent`; task 7/O3). Bearer-token auth; keyed by Gemma's `jobId`. |
| **CLI reporter** | ✅ | `PipelineJobReporter`. |
| **Attempt/retry chain** | ✅ | `ATTEMPT`/`RETRY_OF_FK`/`SUPERSEDED_BY_FK`/`FAILURE_CLASS`/`PARAMS_JSON` on `PIPELINE_JOB` (mysql V23 + h2 V24); `FailureClass`, `BatchRollup`, `RetrySpec`; attempt-chain (not counter) via `retryFailed`/`retryJob` (task 3). |
| **Mop-up / control surface** | ✅ | retry-failed/retry, batch hold/`maxConcurrent`/dispatcher throttle, log/artifact proxy, capabilities, suspend stub (tasks 3–6). SSE deferred (task 12). |
| **Scripted mock + `_mock` REST** | ✅ | `ScriptedMockScheduler` — deterministic, virtual clock, PUSH/POLL, `succeedOnAttempt`; `MockSchedulerControl` + `/admin/pipeline/_mock` (advance clock, set scenario, emit) drive it over HTTP (task 1). |
| **WorkflowGroup** (Spine 3) | ⬜ | Not built. Design in `WORKFLOW_GROUPS_RECCE.md`; lands as Flyway V23+. |
| **Real Nextflow dispatch** | 🟡 | Design **resolved** (see `NEXTFLOW_DISPATCH_RESOLUTIONS.md`, R1–R13): SSH-to-submit-node, one run per EE, `sbatch` the head process, `/space/gemmaData` mount, per-pipeline `maxConcurrent`. **O3 built**: `-with-weblog` → translator → `recordEvent`. Remaining: `NextflowSlurmScheduler.submit/poll/cancel` + end-to-end run (retires the Jenkins button). rnaseq still **Luigi** (task 8). |
| **Curator UI (Pipelines tab / bulk view)** | ⬜ | In gemma-curation-ui ("UIB"); replaces the RNA-seq + Single-Cell Tracker Google Sheets. |
| **Prior task-dispatch foundation** | ✅ (reuse) | `TaskRunningService` + `TasksWebService` + `@POST /datasets/{ee}/tasks/{preprocess,diagnostics,batchInfo,differential}` (202 + `Location:`). The pattern PipelineJob extends. |

---

## Table of contents

1. [The unified model — three spines](#1-the-unified-model--three-spines)
2. [Current shipped surface](#2-current-shipped-surface)
3. [Job state & control plane](#3-job-state--control-plane)
4. [Compute backend — Nextflow, Slurm, and the runtime decision](#4-compute-backend--nextflow-slurm-and-the-runtime-decision)
5. [External pipeline handoff — Tickets + events (parked)](#5-external-pipeline-handoff--tickets--events-parked)
6. [Scheduler, curator CLI surface & the pain it replaces](#6-scheduler-curator-cli-surface--the-pain-it-replaces)
7. [Remaining work (ordered)](#7-remaining-work-ordered)
8. [Open decisions (with recommended defaults)](#8-open-decisions-with-recommended-defaults)
9. [How to run / test what exists](#9-how-to-run--test-what-exists)
10. [Provenance](#provenance)

---

## 1. The unified model — three spines

The recces circled the same territory from different angles and, read
together, blur into "build a workflow/job system." Implementation pulled
them apart into **three distinct spines** that must not collapse into each
other. Naming them is the whole reconciliation:

| Spine | Entity (shipped?) | Question it answers | Lifespan |
|---|---|---|---|
| **Work item** | `Ticket` ✓ | *What should happen to this entity/set, who owns it, what state is it in?* | Opened → worked → closed; days–weeks. |
| **Compute run** | `PipelineJob` ✓ | *What specific external computation did we dispatch, where is it, did it land?* | Submit → run → terminal; minutes–hours. |
| **Working set** | `WorkflowGroup` ✗ | *Which EEs is a curator navigating/batching together right now?* | Durable saved queue; weeks–months. |

They were conflated because all three "track work over time and have a
state machine." But they have different owners (curator intent vs. cluster
vs. UI navigation), different lifespans, and different write rates. **Each
gets its own table and its own state machine.** The value is in the *edges
between them* (§1.2).

One-liner per spine:

- A **Ticket** is "do this thing" — a durable, auditable, assignable unit
  of curator (or agent) intent that targets 1..N entities.
- A **PipelineJob** is "this `nextflow run` against the cluster" — one
  external computation, correlated by an opaque scheduler handle.
- A **WorkflowGroup** is "my screening / pipeline / review queue" — an
  ordered, navigable set of EEs the UI pages through.

### 1.1 Grouping: WorkflowGroup vs PipelineJobBatch

Both group experiments; they are **not** the same and **should not** share
a table:

| | `WORKFLOW_GROUP` (proposed) | `PIPELINE_JOB_BATCH` (shipped) |
|---|---|---|
| Purpose | Curator's saved navigable queue | One compute submission over N EEs |
| Lifespan | Durable; reused for months | Per-run; closes when jobs finish |
| Membership | Ordered (`position`), incl. non-EE UUID candidates | EE-only, set of jobs |
| Types | `screening / pipeline / review` | n/a (one `pipeline` string) |
| Dispatched | Many times | Once |

Keep both. `WorkflowGroup` is the front-of-house working set the curator
UI pages through (set-navigator prev/next); `PipelineJobBatch` is
back-of-house compute bookkeeping. "Dispatch this group" mints a batch
from the group's current EE members (§1.2 edge 2). Do **not** try to host
`WorkflowGroup` on `PipelineJobBatch` (no ordering, no screening UUIDs,
wrong lifespan) nor on `ExpressionExperimentSet`. `WorkflowGroup` lands as
a future Flyway migration (V23+); the live schema tip is V22.

### 1.2 The edges between the spines (where the value is)

1. **PipelineJob → Ticket (failure/attention).** When a `PipelineJob`
   goes `FAILED` — or finishes `DONE` but needs curator judgement — open
   (or relate) a `Ticket` targeting that EE, with the failure detail in
   the first `TICKET_EVENT`. The *compute* handle is `scheduler_handle`;
   the *human follow-up* handle is the Ticket. Auto-open is policy-gated
   per pipeline (transient errors → retry in place; hard errors →
   ticket).
2. **WorkflowGroup → PipelineJobBatch (dispatch a set).** A curator
   batches a saved `WorkflowGroup` of EEs into one `PIPELINE_JOB_BATCH`
   submission. The group is the durable working set; the batch is one
   compute act over it — which is why they stay separate tables.
3. **Ticket ↔ WorkflowGroup (worklist).** An audit/recuration loop can
   materialize its worklist as a `review`-type WorkflowGroup; tickets
   target the same EEs. Membership is by EE id — no FK tangle.
4. **PipelineJob → per-target Ticket status.** A batch driven from a
   ticket's targets writes back `TICKET_TARGET.status`
   (`NOT_DONE → UNDERWAY → DONE`) as each job lands — the `MODE=AUTO`
   advance path (V19) was built for exactly this agent-driven stepping.

### 1.3 Wire-shape convention (cross-cutting)

The curation UI (gemma-curation-ui, "UIB") was built against a FastAPI
mock and expects **snake_case**; Gemma VOs are camelCase. Settled
convention (`PIPELINESTATUS_WIRE_AUDIT.md` precedent):

- **Cosmetic snake/camel mismatch** → fix Java-side with
  `@JsonProperty("snake_case")` on the UI-facing VO
  (`PipelineStatusValueObject`, `TicketValueObject`, future
  `WorkflowGroupValueObject`), plus a unit test asserting the wire keys.
- **Structural mismatch** (object-of-steps vs list-of-steps, vocabulary,
  collapsed diagnostics) → fix UI-side with a thin fetch adapter; do not
  bloat the wire with duplicated/derived shapes.
- **UI-only concepts** with no Gemma equivalent (`candidate_provenance`,
  per-step `in_progress`/`needs_attention`) → adapter supplies
  null/default; don't model server-side until there's a real source.

---

## 2. Current shipped surface

### 2.1 Spine 1 — Tickets (shipped)

Decision record: `AUDIT_AS_WORKFLOW_RECCE.md` (Option B; user decisions
1–7 at its tail are authoritative). Schema in `V3__ticket_layer.sql` +
`V19__ticket_mode_and_target_status.sql`.

- `TICKET` — `type`, `state` (explicit, **not** derived), `priority`,
  `due_date`, `reporter_fk`, `assignee_fk`, `mode` (`MANUAL | AUTO`),
  `external_issue_url` + `external_issue_sync_state`. `Ticket` is itself
  `Auditable` (owns an `AUDIT_TRAIL_FK`) — Decision 6.
- `TICKET_TARGET` — 1..N targets of mixed `target_type`
  (`EXPRESSION_EXPERIMENT`, `ARRAY_DESIGN`, … extensible), each with a
  per-target `status` (`NOT_DONE | UNDERWAY | DONE`) for agent step
  tracking. `(target_type, target_id)` composite index serves the "open
  tickets targeting this EE" reverse lookup without joins.
- `TICKET_EVENT` — append-only workflow log (`COMMENTED`,
  `STATE_CHANGED`, `ASSIGNED`, target-status changes) with JSON payload.
- **Two parallel streams** (Decision 6): `TICKET_EVENT` carries domain
  workflow facts; the inherited `AuditEvent` trail carries governance
  ("who touched this row"). Both populated on writes.
- `TicketService` / `TicketDao*` / `TicketsWebService` (full read+write
  REST). Event types `TicketOpenedEvent`, `TicketStateChangedEvent`,
  `TicketAssignedEvent`, `TicketMetadataChangedEvent`,
  `TicketTargetStatusChangedEvent`.

**Settled decisions (now law):** tickets replace `CurationDetails` /
`needsAttention` / `troubled` (D1); 1..N mixed-type targets (D2); comment
auth = any authenticated curator/admin (D3); append-only for now (D4);
GitHub mirror columns provisioned but sync deferred (D7).

**Still open:** full retirement of `CurationDetails` read-paths in favour
of "is there an OPEN ticket targeting this EE?"; the curator↔admin role
split (D5); comment edit-in-place (D4); GitHub issue sync activation (D7).

### 2.2 Spine 2 — PipelineJobs + scheduler (shipped skeleton)

The `PIPELINES_AND_SCHEDULER_RECCE.md` sketched a `PIPELINE_RUN` table +
`PipelineExecutor` SPI; implementation **evolved the shape** into a
batch→job→event hierarchy with a reconciler and renamed the SPI. Schema in
`V18__pipeline_jobs.sql` + `model/pipeline/*` + `core/pipeline/*` +
`persistence/service/pipeline/*`.

- `PIPELINE_JOB_BATCH` — one curator submission of a `pipeline` (e.g.
  `rnaseq-quant`) over N experiments. `Auditable`; owner-scoped via
  `submitted_by_fk`; `state` `OPEN | CLOSED | CANCELLED`;
  `kill_requested_at`; versioned `params_json`.
- `PIPELINE_JOB` — one EE's run within a batch. `state` `PENDING → QUEUED
  → RUNNING → DONE | FAILED | CANCELLING | CANCELLED`; scheduler-agnostic
  via `scheduler_kind` (`luigi | nextflow | mock`) + opaque
  `scheduler_handle`; **overwrite-in-place snapshot** columns
  (`LAST_PROGRESS_JSON`, `LAST_EVENT_AT`, `LAST_EVENT_KIND`); a dedicated
  `(state, last_event_at)` **reconciler index** for the "non-terminal
  jobs whose last event is stale" poll seek.
- `PIPELINE_JOB_EVENT` — append-only per-job event stream (`kind`:
  `progress | stage | stderr | killed | error | completed | heartbeat`);
  `kind` is free VARCHAR so the scheduler can add kinds without a
  migration.
- `PipelineScheduler` SPI: `{kind, submit → SchedulerHandle, poll,
  cancel}` with value types `SubmitRequest(gemmaJobId, …)`,
  `SchedulerHandle`, `JobSnapshot`. `MockPipelineScheduler` in place (15 s
  synthetic, poll-only, always succeeds); Nextflow/Luigi adapters are
  **stubs that throw**.
- `JobReconciler` — `@Scheduled` poll of stale non-terminal jobs via the
  reconciler index.
- `PipelineJobBatchService` — `{submit, get, findByOwner, cancelBatch,
  cancelJob, recordEvent, findEvents, findStaleJobs}`.
- REST: `/admin/pipeline` (admin) submit/list/get/cancel + job events;
  `/internal/pipeline` push callback (bearer). The push callback is keyed
  by **Gemma's** `jobId` (`POST /internal/pipeline/jobs/{jobId}/events`),
  round-tripped via `SubmitRequest.gemmaJobId` — so the scheduler side
  never needs to know Gemma's batch model. Keep that keying.

**Two integration modes, both schema-supported:**

1. **Poll** — `JobReconciler` seeks stale non-terminal jobs and asks the
   scheduler for status. Default; works for any scheduler with a status
   query (`squeue`, Nextflow `trace.txt`, Luigi scheduler API).
2. **Push** — the running job (or its epilogue script) calls the Gemma
   callback. Better for long jobs that outlive a worker.

**Gaps against the target (drive §3–§4):** no attempt chain, no failure
classification, no batch-disposition rollup; no suspend/resume; no log
fetch; the mock can't fail, can't be scripted, can't push, has no
deterministic clock. Real `NextflowSlurmScheduler` / `LuigiScheduler`
adapters, the curator-UI Pipelines surface, and the rnaseq Luigi→Nextflow
port all remain open.

### 2.3 Spine 3 — WorkflowGroup (not built)

Build per `WORKFLOW_GROUPS_RECCE.md` §3–§4 when the curation-UI
set-navigator is greenlit: entity + `/groups` CRUD +
`/datasets/{id}/groups`, VO snake-case via `@JsonProperty`, ~1,700 LOC,
landing as Flyway V23+ with an h2 sister migration. Do not host it on an
existing table (§1.1).

---

## 3. Job state & control plane

### 3.1 Gating decision — durable envelope in MySQL vs delegated runtime

**The concern (2026-06-23):** persisting live job state in the main Gemma
MySQL (gemd) is the wrong default. The V18 tables conflate two different
kinds of state:

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
retry/suspend/resume control plane **delegate to the orchestrator**.
Gemma's monitoring endpoints become a **proxy/projection** over the
runtime, not a poll-fed mirror. The mop-up itself is largely *not Gemma's
to build* — Nextflow's `-resume` (skip completed, re-run failed) **is** the
mop-up; Tower/Slurm already do logs, cancel, retry-failed. Gemma records
*that* EE123 was retried twice (provenance), not the machinery of doing
it.

**Counter-argument, kept honest:** a second mechanism is operational
weight on a monolith whose track is "modernise without splitting." The
original scheduler recce chose MySQL+reconciler *specifically* to avoid a
broker. So the offload's cost is real; the right mechanism depends on the
runtime committed to (full options in §4.2).

**Two facts already true in this codebase narrow the choice:**

1. **Nextflow is the converging executor.** sc-annotation already runs
   Nextflow-on-Slurm; rnaseq is slated to port off Luigi to Nextflow
   (§4.3). So the runtime isn't really open — it's "Nextflow + what
   monitors it." Nextflow ships its own event stream:
   `nextflow run -with-weblog <url>` POSTs workflow/process events
   (submitted, started, completed, failed, with trace records) to an HTTP
   endpoint as JSON, live. That endpoint shape is *already what Gemma
   built*: `POST /internal/pipeline/jobs/{id}/events`. Nextflow can push
   straight into the existing callback — no broker, no Tower, no adapter.
2. **V18 already has the low-churn columns.** `PIPELINE_JOB` carries the
   overwrite-in-place snapshot (`LAST_PROGRESS_JSON` + `LAST_EVENT_AT` +
   `LAST_EVENT_KIND`). The *only* churn source is the append-only
   `PIPELINE_JOB_EVENT` table. So the MySQL concern is fixed by **not
   persisting every progress tick as a row** — keep the snapshot (one
   overwrite per job) + persist only milestone/terminal events as durable
   facts. That's a write-policy change, not a re-platforming.

**Recommendation — Option A, Nextflow-native minimal-infra:** Nextflow
*is* the runtime; `-with-weblog` pushes events into the existing internal
callback; `-resume` *is* the mop-up; logs live in the workdir /
`.nextflow.log` and Gemma proxies a byte range. Gemma persists only the
thin envelope + snapshot + milestone/terminal facts. Zero new infra,
reuses the callback already shipped, delegates the hard parts to the
runtime already being adopted. (Redis live-tail is a *later* optimization
gated on measured need; Tower/Temporal/broker only if orchestration scope
grows — see §4.2.)

**What survives every option:** the durable envelope in MySQL (provenance
is a domain fact regardless of runtime), and the **scripted mock** (§3.6),
which simply moves down a layer to mock the *orchestrator's control plane*
behind Gemma's proxy interface — so endpoints + UIB integration proceed
regardless of which runtime wins.

**Implication for §3.2 & §3.5:** they are written MySQL-centric and are
**conditional on this decision**. Under the delegated model, §3.2's
attempt columns shrink to a per-EE retry-count + terminal-outcome on the
envelope (the attempt *machinery* lives in the runtime); §3.5's log storage
becomes pure proxy. Read them as "if we keep this in MySQL"; the delegated
model is the recommendation. The gating question is **which runtime**
(§7 Q1) — answer that first.

### 3.2 The mop-up / retry model

> **Conditional on §3.1.** Under the recommended delegated model the
> runtime owns attempts/retries and Gemma keeps only a per-EE retry-count
> + terminal outcome on the envelope. The *concepts* below (attempt-not-
> mutate, failure classification, batch disposition) carry over either
> way — only the storage location changes.

**Retry mints a new attempt; it does not mutate the failed job.** A failed
`PipelineJob` is **immutable history** — its events, error message,
scheduler handle, and timings are the debugging record. Retry creates a
**new** `PipelineJob` for the same `(batch, experiment)`, linked to its
predecessor. Never flip a `FAILED` job back to `PENDING`. Curators (and
agents) will retry repeatedly with tweaked params; collapsing attempts
loses "it failed on OOM, we bumped mem, it failed on a bad SRA file, we
swapped accession, third try worked" — that chain is the audit trail.

Model delta on `PIPELINE_JOB` (MySQL-fallback shape; → Flyway `V23` +
h2 sister):

```sql
ALTER TABLE PIPELINE_JOB
  ADD COLUMN ATTEMPT          INT          NOT NULL DEFAULT 1,   -- 1-based
  ADD COLUMN RETRY_OF_FK      BIGINT       NULL,                 -- previous attempt
  ADD COLUMN SUPERSEDED_BY_FK BIGINT       NULL,                 -- the retry that replaced this one
  ADD COLUMN FAILURE_CLASS    VARCHAR(16)  NULL,                 -- TRANSIENT | PERMANENT | UNKNOWN
  ADD CONSTRAINT FK_PIPELINE_JOB_RETRY_OF
      FOREIGN KEY (RETRY_OF_FK) REFERENCES PIPELINE_JOB (ID),
  ADD KEY IDX_PIPELINE_JOB_BATCH_EE_ATTEMPT (BATCH_FK, EXPERIMENT_FK, ATTEMPT);
```

- **Current attempt** for a `(batch, ee)` = the row with no
  `SUPERSEDED_BY_FK` (equivalently, max `ATTEMPT`). Rollups and the UI
  show current attempts; the chain is drill-down.
- `SUPERSEDED_BY_FK` is the **one** column ever written on a terminal job,
  and it is **monotonic** (null → set once, never cleared) — so "immutable
  history" holds for everything that matters while giving an O(1)
  is-current check with no `max()` subquery on the hot list path.
- The three columns are **deliberately redundant**, each serving a
  different read: `SUPERSEDED_BY_FK` (forward, is-current hot path),
  `RETRY_OF_FK` (back, walk the chain on drill-down), `ATTEMPT`
  (denormalized counter for display + sort).

**"When appropriate" = failure classification.** The pipeline (or
scheduler poll) reports *why* a job failed in the `error` event payload;
Gemma persists it to `FAILURE_CLASS`:

| Class | Meaning | Default mop-up |
|---|---|---|
| `TRANSIENT` | SRA throttle, OOM, node died, scheduler lost handle, network | **auto-eligible** for retry |
| `PERMANENT` | malformed input, no raw data, validation reject, unsupported chemistry | **not** retried without curator override |
| `UNKNOWN` | unclassified / no signal | surfaced; curator decides |

Classification source, in priority order: (1) explicit `failureClass` in
the pipeline's `error` event payload — the pipeline knows best; (2) a
Gemma-side heuristic mapping of scheduler exit codes / messages when the
pipeline doesn't say; (3) `UNKNOWN`. The heuristic is a small, overridable
table — the pipeline reporting its own class is the real answer. The
`error` event payload is a versioned record —
`{failureClass, message, exitCode, retryHint, stderrTailUri}` — shared as
a JSON Schema both sides pin to (§5.3 discipline).

**Batch disposition (derived, not stored).** `PIPELINE_JOB_BATCH.state`
stays the curator's explicit lifecycle flag (`OPEN` = still working,
`CLOSED` = done, `CANCELLED`). Disposition is a rollup VO over current
attempts:

```
BatchRollup {
  total, pending, queued, running, done, failed, cancelled,   // counts
  failedRetryable,            // FAILED current attempts, FAILURE_CLASS=TRANSIENT
  failedPermanent,
  needsAttention: boolean     // state==OPEN && failedCurrent>0
  terminal: boolean           // every current attempt is terminal
}
```

`needsAttention` drives the UI's "this batch isn't finished" badge. A
batch is **not** "done" just because every job reached a terminal state —
it's done when the curator accepts the outcome (closes it) or all current
attempts are `DONE`. A retry in flight supersedes the failure, so
`failedCurrent` drops to 0 automatically while the retry runs — no
separate "in flight" clause needed.

**Mop-up operation.** Service: `retryFailed(batchId, RetrySpec)` and
`retryJob(jobId, RetrySpec)`.

```
RetrySpec {
  onlyRetryable: boolean = true,     // default: skip PERMANENT failures
  jobIds: List<Long>? = null,        // null = all eligible failed current attempts
  paramsOverrideJson: String? = null // e.g. bump --mem, swap accession
}
```

Semantics: for each eligible job, mint attempt N+1, copy params (apply
override), set `SUPERSEDED_BY_FK` on the old row, dispatch. **Idempotency:**
refuse to retry a job that already has a non-terminal successor (guards
double-clicks and concurrent curators). Re-opens the batch to `OPEN` if it
was `CLOSED`. Returns the new rollup so the UI repaints in one round-trip.
"Mop-up the whole batch" is one call: `retryFailed(batchId, {})` — retries
every transient failure, leaves permanents for human eyes.

Resolved parameters (recorded, reversible): retry-param override is
**batch-level** default (per-job deferred); **manual-first** (curator
clicks "retry transient"; add per-pipeline `autoRetryMax` only once the
classifier is trusted — avoids retry storms); `retryJob` works on any
**terminal** current attempt (FAILED, CANCELLED, and DONE-for-reprocess),
`retryFailed(batch)` targets FAILED + retryable only.

### 3.3 Cancel — exists, keep

`cancelBatch` / `cancelJob` already drive `CANCELLING → CANCELLED`. Only
change: make them retry-aware (cancelling a job clears any pending
auto-retry).

### 3.4 Suspend / resume — two different things

1. **Throttle / hold the batch dispatcher** (scheduler-agnostic, always
   available). A batch of 500 EEs shouldn't fire 500 `sbatch`es at once.
   Add batch-level `MAX_CONCURRENT INT NULL` + `HELD BOOLEAN NOT NULL
   DEFAULT FALSE`; a dispatcher pass submits `PENDING` jobs up to the
   concurrency budget and skips held batches. "Pause this batch" = set
   `HELD`; "resume" = clear it. No scheduler support needed; covers the
   common operational want. **Ship this now — high value, zero scheduler
   dependency.**
2. **Suspend a running job** (scheduler-dependent). Slurm can `scontrol
   suspend`; Nextflow **cannot** suspend a running process mid-flight. So
   this is an *optional* SPI capability:

```java
// default methods on PipelineScheduler — unsupported unless overridden
default void suspend(SchedulerHandle h) { throw new UnsupportedOperationException(); }
default void resume(SchedulerHandle h)  { throw new UnsupportedOperationException(); }
default boolean supportsSuspend() { return false; }
```

REST surfaces `supportsSuspend()` in the capabilities response so the UI
hides the button when the active scheduler can't do it, and returns **409
Conflict** if called anyway. Add a `SUSPENDED` job state only if/when a
scheduler that supports it lands (Slurm). Until then, ship batch hold and
leave per-job suspend as the capability-gated stub.

### 3.5 Monitoring surface — events, logs, artifacts, live updates

- **Event timeline (exists).** `findEvents(jobId, since, limit)` +
  `GET …/jobs/{jobId}/events?sinceMillis=` is the structured timeline;
  backbone of the per-job drawer. Keep.
- **Logs — do NOT store in MySQL.** Real logs (`slurm-%j.out`,
  `.nextflow.log`, Cell Ranger output) are big and live on the cluster
  filesystem. Two-tier: (a) **tail on terminal** *(fallback only)* — the
  pipeline includes the last ~4–8 KB of stderr in its terminal event
  payload; under the delegated model the runtime serves the log and Gemma
  stores no tail. (b) **Full log on demand** —
  `GET …/jobs/{jobId}/log?offset=&limit=` proxies through the active
  `PipelineScheduler` (byte-range read, or Tower/Slurm REST) via a new
  optional SPI method `readLog(handle, offset, limit) → LogChunk{bytes,
  nextOffset, eof}`. The `offset` cursor gives incremental "tail -f"
  without re-fetching.
- **Artifacts** (the `scp web_summary.html` killer):
  `GET …/jobs/{jobId}/artifacts/{name}` streams a whitelisted output file
  from the job workdir. Same SPI proxy pattern.
- **Live updates — poll first, SSE if needed.** Start with incremental
  polling (the `sinceMillis` / `offset` cursors make it cheap) at 2–5 s
  while a batch is non-terminal. Add SSE
  (`GET …/batches/{id}/stream`) only if poll proves laggy — a backstop,
  not a day-one requirement. SSE through nginx is fine; WebSocket is not
  worth it here.

### 3.6 The scripted mock — the shared contract fixture

The current `MockPipelineScheduler` is a smoke toy. Evolve it into a
`ScriptedMockScheduler` that is **programmable, deterministic, and able to
fail** — this is the highest-leverage piece, since both Gemma and UIB
develop against it. A scenario registry keyed by experiment id:

```
Scenario {
  outcome: SUCCEED | FAIL | STALL,
  failureClass: TRANSIENT | PERMANENT | UNKNOWN,   // when FAIL
  stages: [ {afterMs, kind, payload} ],            // synthetic event script
  logLines: [String],                              // served by readLog
  transport: POLL | PUSH                            // which path to exercise
}
```

Three capabilities the smoke mock lacks: (1) **can fail, with a class** —
a `PARTIAL_BATCH` scenario fails ⌊N/3⌋ of N jobs as `TRANSIENT`, and the
*second* attempt succeeds (scenario is attempt-aware), so the UI dev sees
fail → retry → green end to end; (2) **deterministic clock** —
`POST …/_mock/advance?ms=` (dev profile only) steps every scripted timer,
so tests assert without `Thread.sleep`; (3) **both transports** — a
scenario can `PUSH` (mock calls the internal callback) or `POLL` (mock
answers `poll`, exercising the reconciler).

Dev-only mock control REST behind `@Profile("scheduler-mock")` (plus a
startup assertion that fails fast if `production` and `scheduler-mock` are
both active — belt and suspenders):

```
POST /admin/pipeline/_mock/scenario   {experimentId?, scenario}   set canned outcome
POST /admin/pipeline/_mock/advance    {ms}                        step the clock
POST /admin/pipeline/_mock/emit       {jobId, kind, payloadJson}  fire an arbitrary event
GET  /admin/pipeline/_mock/scenarios                              list active scenarios
```

Canonical scenarios (`SUCCEED_FAST`, `PARTIAL_BATCH`, `ALL_TRANSIENT`,
`PERMANENT_REJECT`, `STALL_THEN_RECONCILE`) live as a JSON fixture under
`gemma-rest/src/test/resources/pipeline-scenarios/` **and** ship to UIB;
a CI diff catches drift. The fixture *is* Gemma — no FastAPI mock drift.

### 3.7 REST surface for UIB (consolidated)

Existing (keep): `GET /admin/pipeline/registry`, `POST …/batches`,
`GET …/batches[?state=&limit=]`, `GET …/batches/{id}`,
`POST …/batches/{id}/cancel`, `GET …/batches/{id}/jobs/{jobId}/events`,
`POST …/batches/{id}/jobs/{jobId}/cancel`,
`POST /internal/pipeline/jobs/{jobId}/events`.

New:

| Verb | Path | Purpose |
|---|---|---|
| GET | `/admin/pipeline/capabilities` | active scheduler kind + `supportsSuspend`/`supportsLog`/`supportsArtifacts` — UI feature-gates off this |
| GET | `/admin/pipeline/batches/{id}/rollup` | the `BatchRollup` (§3.2); cheap repaint source |
| POST | `/admin/pipeline/batches/{id}/retry-failed` | mop-up; body = `RetrySpec` |
| POST | `/admin/pipeline/batches/{id}/jobs/{jobId}/retry` | single-job retry |
| POST | `/admin/pipeline/batches/{id}/hold` · `/resume` | batch dispatcher hold (§3.4 #1) |
| PATCH | `/admin/pipeline/batches/{id}` | set `maxConcurrent`, note |
| POST | `/admin/pipeline/batches/{id}/jobs/{jobId}/suspend` · `/resume` | 409 if `!supportsSuspend` (§3.4 #2) |
| GET | `/admin/pipeline/batches/{id}/jobs/{jobId}/log?offset=&limit=` | incremental log tail |
| GET | `/admin/pipeline/batches/{id}/jobs/{jobId}/artifacts/{name}` | stream output file |
| GET | `/admin/pipeline/batches/{id}/jobs/{jobId}` | single current-attempt detail + attempt chain |
| GET | `/admin/pipeline/batches/{id}/stream` *(optional)* | SSE; add only if poll lags |

Wire-shape: snake_case via `@JsonProperty` (§1.3), with a key-pinning unit
test. Auth stays `GROUP_ADMIN` for now; the curator-vs-admin split is
Tickets Decision 5.

**Testing layers** (all fast + non-flaky thanks to the scripted mock +
deterministic clock, running in `mvn verify`): unit (state machine, retry
minting + idempotency, classification, rollup math — no Spring); service
IT (submit → `PARTIAL_BATCH` → assert `needsAttention` → `retryFailed` →
advance → assert all DONE, against gemdtest); reconciler IT
(`STALL_THEN_RECONCILE`); REST IT (full admin surface via JerseyTest5 +
scripted mock); contract (scenario JSON fixture pinned in both repos, CI
diff).

---

## 4. Compute backend — Nextflow, Slurm, and the runtime decision

### 4.1 Transport reconciliation — reconciler won, broker parked

This is the one place the recces **actively disagreed**, now resolved by
what shipped. `EXTERNAL_PIPELINE_HANDOFF_RECCE.md` proposed a
Spring-Modulith `event_publication` table + `@Externalized` → **RabbitMQ**
broker as the two-way handoff substrate, with the `Ticket` as the
correlation handle. `PIPELINES_AND_SCHEDULER_RECCE.md` proposed a
Gemma-side job table polled against the scheduler. **V18 +
`JobReconciler` chose the latter.** The correlation handle for a compute
run is the `PIPELINE_JOB.scheduler_handle`, not a Ticket; durability is the
`PIPELINE_JOB` row + `PIPELINE_JOB_EVENT` log, not an `event_publication`
table.

**Why the reconciler model is the design of record:** no new broker to
run, secure, and keep alive (RabbitMQ container, users, TLS, retention —
all avoided); Gemma already owns a MySQL row per job, and that row *is* the
durable source of truth; the poll/push pair already covers the two real
cases (short jobs: poll; long jobs: epilogue push); one operational story,
fewer moving parts on a monolith the parent track is explicitly
"modernise without splitting."

**What the broker recce keeps for the record (parked, not discarded):**
its **event-schema discipline** — immutable record events, `payloadVersion`
discriminator, shared JSON Schema with a CI diff against the Python shadow
classes — is the right contract for the push-callback payloads too. Adopt
it for `PIPELINE_JOB_EVENT` `payload_json` shapes even though the transport
is HTTP, not AMQP. **Revive the broker only if** a future need appears the
reconciler can't serve cleanly: many heterogeneous external producers,
fan-out to multiple independent consumers, or back-pressure/replay
semantics across process restarts at a volume where row-polling is a
measured bottleneck. Until then, `event_publication` + `@Externalized`
stays out of the classpath.

### 4.2 Runtime options analysis

Answering §3.1's gating question. Options weighed, with the recommendation
first:

| Option | Gives us | Cost / verdict |
|---|---|---|
| **A. Nextflow-native, minimal-infra** *(recommended)* | `-with-weblog` → existing callback; `-resume` = mop-up; workdir log proxy; thin MySQL envelope, churn table dropped | zero new infra; couples to Nextflow's weblog/trace shape (stable — pin it); process-level tail granularity. **The smallest correct move.** |
| **B. + Redis live-tail cache** | sub-second live updates + fan-out without touching MySQL; natural TTL | one light new service; **add later**, gated on measured need. |
| **C. Nextflow Tower / Seqera Platform** | mature control plane, dashboard, logs, cancel, resume, retry, API | **redundant with the curation-UI goal** (curators stay in Gemma, not a second dashboard); self-host is a real deploy + licensing; another auth boundary. |
| **D. Durable workflow engine (Temporal/Cadence)** | first-class retry/suspend/saga; would serve future cross-pipeline DAGs | heaviest new-infra bet; duplicates what `-resume` already does; **premature**. |
| **E. RabbitMQ/Modulith broker** (parked) | decouples churn; fan-out to many consumers; schema discipline designed | a broker to run; `-with-weblog` → HTTP already gives the decoupling. Only if many producers/consumers appear (§4.1 trigger). |
| **F. MySQL-envelope-only, no weblog** | truly zero new moving parts | **no live monitoring in Gemma** — curators back to reading cluster logs; fails the UI goal. |

Provenance — *that* EE was retried twice, by whom, when, with what outcome
— stays in MySQL under every option, because it's a domain fact, not
operational churn.

### 4.3 rnaseq-pipeline (Luigi today) → Nextflow

**Bulk RNA-seq** currently runs a **Luigi** pipeline whose state lives in a
Google Sheet (`experiment_id`, `priority`, `data`, `batch_info`) and a
`luigid` daemon at `localhost:8082`. DAG closure (from `tasks.py`), top
entry `SubmitExperimentToGemma` (a `WrapperTask`):

```
SubmitExperimentToGemma
├── SubmitExperimentDataToGemma  (bulk OR single-cell branch)
│   ├── SubmitBulkExperimentDataToGemma -> gemma-cli rnaseqDataAdd
│   │   └── CountExperiment → AlignExperiment → AlignSample (Slurm submit)
│   │       └── TrimSample/QualityControlSample → DownloadSample → PrepareReference
│   └── SubmitSingleCellExperimentDataToGemma -> gemma-cli loadSingleCellData
│       └── AlignSingleCellExperiment → AlignSingleCellSample (Cell Ranger)
├── SubmitExperimentBatchInfoToGemma  -> gemma-cli (batch info)
└── SubmitExperimentReportToGemma     -> gemma-cli (MultiQC report attach)
```

Slurm integration via Bioluigi (`scheduler=slurm`); caps in `[resources]`
(`slurm_jobs=384`, `prefetch_jobs=2`, `fastq_dump_jobs=40`, …). Outputs
round-trip to Gemma via three `gemma-cli` calls (`rnaseqDataAdd` /
`loadSingleCellData`, batch info, MultiQC via `addMetadataFile`) plus a
Slack webhook.

**Migration assessment: effort medium, verdict GO — but only after the
Gemma scheduler skeleton exists.** ~25 task classes, ~5 source plugins
(geo/sra/arrayexpress/gemma). The hardest parts are not DAG translation
but: (a) Bioluigi's resource accounting (`slurm_jobs=384`,
`geo_http_connections=4`) doesn't map 1:1 to Nextflow's
`executor.queueSize` — needs per-process limits or a global `maxForks`;
(b) the conditional bulk-vs-SC branch needs a `branch` operator or two
entry workflows; (c) shared-memory STAR preload has no exact Nextflow
equivalent — preload once + pin, or accept cold-start cost. **Risks:**
re-validate quantification reproducibility (plan: replay 10 known-good
GSEs side-by-side, diff count matrices — STAR/RSEM are deterministic;
differences would come from SRA prefetch or scratch-staging). **Benefits:**
one executor across both pipelines; nf-test snapshots + nf-schema
samplesheet validation; first-class Apptainer/Singularity; nf-core
conventions. **Google-Sheet polling moves into Gemma** (a scheduled job
populates the queue from the sheet) rather than into Nextflow. Doing the
port *before* the scheduler means writing the sheet-polling adapter twice.

### 4.4 sc-annotation-pipeline (Nextflow already)

nf-core-shaped DSL2, six subworkflows (`INPUT_CHECK`, `PREPARE_REFERENCE`,
`PROCESS_QUERIES`, `CLASSIFY_CELLTYPES`, `QC_REPORTING`, `GEMMA_UPLOAD`).
Slurm executor: `process.executor='slurm'`,
`clusterOptions='-C thrd64 --cpus-per-task=8 --mem=32G'`,
`executor.queueSize=25`. Apptainer preferred. Input modes: samplesheet CSV
(preferred), `--study_names` / `--study_paths` (legacy). Per-species params
(`params.hs.json` / `params.mm.json`). The `GEMMA_UPLOAD` subworkflow
already exists (`upload_cta`/`upload_clc`/`upload_mask`/`upload_multiqc`
toggles; creds via `GEMMA_USERNAME`/`GEMMA_PASSWORD`).

**Today it's driven by a Jenkins button** — the entirety of the
orchestration is `try { sh 'nextflow run …' } catch { error(...) }`.
Replacing it with a Gemma-side dispatch is the smallest change to make the
SC pipeline curator-friendly, and the same plumbing serves the rnaseq
port. Lessons: samplesheet-CSV-as-input is the right shape (Gemma writes
one at submit time → nf-schema validation for free); preserve per-species
params files; `queueSize=25` is far below Bioluigi's `slurm_jobs=384`, so
per-pipeline queue caps will differ. **This is the first target for the
real `NextflowSlurmScheduler`** — it already runs end-to-end, so the work
is purely "Gemma can dispatch it."

---

## 5. External pipeline handoff — Tickets + events (parked)

This section documents the parked broker design (§4.1). It stays on record
because its **event-schema discipline** is adopted for the HTTP push
callback, and because it is the revival blueprint if the reconciler ever
can't serve a future need.

### 5.1 The pattern

Use the Modulith event-publication substrate (persistent
`event_publication` table + `@Externalized`) with the `Ticket` as the
durable correlation handle:

1. **Gemma emits a process-request event.** A curator (or auto-trigger)
   opens a `Ticket`; the ticket service publishes a request event;
   Modulith persists the publication and `@Externalized` routes it to
   RabbitMQ.
2. **External pipeline consumes** (Nextflow run, Python worker, or LLM
   agent), pulls raw data, does the work, writes results to a known
   location (S3/NFS/scratch).
3. **Pipeline emits a process-completed (or -failed) event** back through
   the broker, optionally with intermediate progress events.
4. **Gemma ingests results.** An `@ApplicationModuleListener` attaches
   results to the target entity and transitions the `Ticket` to
   `RESOLVED` (or reopens on failure).

The `Ticket.id` rides on every event in both directions; ingestion is
idempotent on `(ticketId, terminal state)`. This is **explicitly not a
workflow engine** — no DAG across tickets, no compensating transactions,
no retry beyond Modulith's "replay incomplete publications on startup".
One ticket = one external job.

### 5.2 The RNA-seq worked example

Open a ticket with a typed payload:

```java
Ticket t = ticketService.openTicket(
    reporter,
    TicketType.RNASEQ_PROCESSING_REQUESTED,
    "Process GSE12345",
    List.of(new TicketTarget(EE_ID_123, TicketTargetType.EXPRESSION_EXPERIMENT)),
    new RnaSeqRequestPayload("SRP000123", List.of("SRR1","SRR2","SRR3"),
                             "GRCh38.p14", Quantifier.SALMON));
```

`openTicket` is `@Audited`; the audit aspect writes the audit row, then
the service publishes a Spring `ApplicationEvent`. Modulith writes the
`event_publication` row **in the same tx as the ticket INSERT** (both
commit or neither); after commit, in-process listeners fire and
`@Externalized("rnaseq-process-requested::rnaseq")` routes JSON to the
RabbitMQ `rnaseq` exchange. If the JVM crashes after the ticket commit but
before RabbitMQ acks, the publication row is incomplete on restart and
Modulith replays it — no lost requests.

The worker subscribes, parses JSON into a Pydantic shadow class, pulls
from SRA, runs `salmon quant`, writes to
`s3://gemma-rnaseq-results/<ticketId>/`, and publishes a terminal event.
The inbound `RnaSeqResultIngester` (two `@ApplicationModuleListener`
methods, `onComplete`/`onFail`) attaches vectors and resolves or reopens
the ticket. The listener runs after the producer tx commits, on a separate
thread, in its own `REQUIRES_NEW` tx, with its own persistent publication
row (replays on restart).

### 5.3 Event-schema discipline (the part that's adopted)

Events are immutable Jackson records with a `payloadVersion` JSON-Schema
discriminator, bumped only on a backward-incompatible wire change;
Pydantic shadow classes branch on it. Result side effects (attaching
vectors, updating `CurationDetails`, refreshing search index) are **not**
in the event — they happen in the listener; events stay minimal facts.
The **biggest risk is schema drift** between the Java record and the
Pydantic shadow; the mitigation — generate both sides from a shared JSON
Schema under `gemma-rest/src/main/resources/event-schemas/`, CI-diffed on
every PR — is exactly what §3.2's `error`-payload contract and §3.6's
scenario fixture reuse.

### 5.4 Generalization & pipeline-side requirements

Adding a pipeline is mechanical: one `TicketType` value, three record
events (Requested/Completed/Failed, request + terminal `@Externalized`),
one `@ApplicationModuleListener` class with `onComplete`/`onFail`
(~120 LoC, mostly the pipeline-specific result loader). No schema
migration — events ride payload-JSON in `event_publication`; tickets are
already polymorphic on `TicketType`. Documented targets: single-cell QC,
variant calling, methylation, and **LLM-based curation** (the "worker" is
the `gemma-curation-agents` process; the completed event carries a
proposed curation diff — the cleanest replacement for the existing
`PUT /datasets/{id}/curationDetails` polling).

The external worker, in any language/orchestrator, must: subscribe to the
request topic; parse the JSON (match the record shape); do the work (free
choice of nf-core / Snakemake / ad-hoc Python / LLM agent); **always
publish exactly one terminal event per ticket** (wrap the loop in a
try/except that publishes `…ProcessFailedEvent` on any uncaught error — no
silent abandons); include the `ticketId` for idempotency (workers need no
dedup store — the ingester no-ops on an already-`RESOLVED` ticket). The
system is **at-least-once, not exactly-once**.

**Slurm/Luigi idempotency note:** if a worker crashes after `sbatch`
returned a job id but before publishing, the job runs unattended.
Mitigation — the Slurm job's **epilogue script always publishes the
terminal event** using `GEMMA_TICKET_ID` as the correlation key; worker
crashes become harmless. (Same via Luigi's task `event_handler` hooks.)
This "hook publishes the terminal event" pattern is precisely the **push**
integration mode that shipped in §2.2 — the durable part of the parked
design that survived into the reconciler world.

**Operational notes (for a future revival):** single-node RabbitMQ in
docker-compose; durable exchanges/queues/persistent messages; wire
`CompletedEventPublications.deletePublicationsOlderThan` to the nightly
maintenance job (90 days successful, 365 days failed); two failure
policies per pipeline (*reopen* → curator retries, default for transient;
*hard fail* → new `TicketState`, reserve for unrecoverable); broker users
`gemma-publisher` / `gemma-worker` restricted by exchange + routing key,
TLS from Phase 3.

---

## 6. Scheduler, curator CLI surface & the pain it replaces

### 6.1 What a curator's day looks like today

A curator's day splits between a browser tab (Gemma UI for ED / tags /
outliers) and an SSH terminal on `lisa` running `gemma-cli` across ~20
subcommands. Three sequencing modes:

- **Microarray:** `addGEOData` → `affyFromCel` (Affy only) → browser
  curate → `processedDataCompute` (or REST `/tasks/preprocess`) →
  `diffExAnalyze` → make public.
- **Bulk RNA-seq:** same browser steps, but the terminal work is subsumed
  by the Luigi pipeline (§4.3) driven from the Google Sheet; the curator
  monitors `localhost:8082`, reads Slack, and `scp`s a `web_summary.html`
  to their laptop to diagnose Cell Ranger.
- **scRNA-seq (heaviest):** `addGEOData` → `downloadSingleCellData` →
  manual split-by-`organism_part` decision → `loadSingleCellData`
  (with `--renaming-file`, `--infer-samples-from-cell-ids-overlap`, …) →
  curate ED → cell-type branch (author-submitted: manual R wrangling +
  `addMetadataFile` + `loadSingleCellData --load-cell-type-assignment`;
  or no-annotations brain-only: Jenkins-trigger sc-annotation) →
  `aggregateSingleCellData --make-preferred` → `diffExAnalyze -subset
  cell_type` → hand-track ~17 status columns in the Single-Cell
  Experiment Tracker Google Sheet.

**Pain points the curator-UI scope absorbs:** manual R barcode wrangling
(paste a function into the R console); `--redo` archaeology (the QT name
is only discoverable by running with a deliberately wrong `-qt` and
reading the error); the `scp web_summary.html` loop; the three-week
author-email loop (verbatim template + three reminders); the Google Sheet
column-order coupling (parsed by index, not header); ~17 hand-filled
status columns (most derivable from Gemma DB state). **Google Sheets is
the de-facto job queue today** — a spreadsheet Sanja and Carlton edit, not
a DB table; replacing it is mostly a curator-onboarding exercise once the
UI replacement exists.

### 6.2 The foundation already half-built

Gemma already has a usable task-dispatch foundation:
`TaskRunningService` + `TasksWebService` + the four `@POST
/datasets/{ee}/tasks/*` endpoints (`preprocess`/`diagnostics`/`batchInfo`/
`differential`) implement exactly the 202-and-poll pattern (HTTP 202 +
`Location: /tasks/{id}`, in-memory store, 10-minute eviction). The gaps
this architecture fills are (a) persistence (the store was in-memory), (b)
external executors (no Slurm/Nextflow wiring), (c) the curator-UI surface
— all now addressed by the shipped PipelineJob skeleton (§2.2). The work
was "extend," not "build from scratch."

`SchedulerConfig.java` is **Quartz** — for in-Gemma scheduled jobs
(`BatchInfoRepopulationJob`, `Ee2AdUpdateJob`, table maintenance),
profile-gated on `scheduler` so only one production node runs it. Distinct
from curator-facing pipeline scheduling, but the precedent — "scheduled
background work, profile-gated to one node" — is the right model for the
poll loop that talks to Slurm and for the Google-Sheet-drain job.

### 6.3 Curator UI surface (target)

- **Per-EE "Pipelines" tab** alongside ED / Diagnostics / DEA: a "Run
  pipeline" dropdown (preprocess, DEA, rnaseq, sc-annotation, custom), a
  run-history table, an expandable per-run drawer with live stdout tail +
  a link to the upstream report (Nextflow `report.html` / future).
- **Bulk view** at `/curation/pipelines`: faceted by status (queued /
  running / failed / succeeded / cancelled), pipeline kind, curator.
  "Run all in queue" / "Retry failed". Replaces **both** the RNA-seq
  Google Sheet and the Single-Cell Experiment Tracker.
- **Cell Ranger web-summary fetcher**: `…/artifacts/web_summary.html`
  streams the file from the workdir (§3.5), killing the `scp lisa:…` loop.

---

## 7. Remaining work (ordered)

Ordered by dependency. **Slice 0** is a decision, not code, and gates
tasks 2+; task 1 is buildable immediately in parallel because it defines
the proxy interface both real runtimes implement. Each task names what to
build, which existing code to extend, and its acceptance signal.

- [x] **0. GATING DECISION — pick the runtime / state-location model**
  (§3.1, §4.2, §8 D1). **DECIDED 2026-07-13 (Ogan): delegated / Nextflow-native
  (Option A).** Live state delegates to Nextflow; Gemma keeps the thin durable
  envelope. Tasks 2–3 are Gemma-internal and proceed now with no external
  sign-off; the infra-facing commitments (Nextflow `-with-weblog` reaching Gemma
  from compute nodes, a shared resumable workdir, per-pipeline concurrency caps)
  still need the cluster owner's confirmation when the real `NextflowSlurmScheduler`
  lands (task 7). Correction: §3.2 does NOT shrink to a bare counter — the
  attempt-chain (immutable per-attempt rows) is retained (task 3 decision below);
  the delegated model offloads the *compute-level rerun* to `-resume`, not the
  attempt record. §3.5 logs become pure proxy.

- [x] **1. Scripted mock + deterministic clock + `_mock` REST + scenario
  fixtures** (§3.6). **LANDED 2026-07-13.** `ScriptedMockScheduler` +
  `MockSchedulerControl` + `Scenario`/`BatchScenario` (gemma-core),
  `AdminPipelineMockWebService` + 5 fixtures (gemma-rest); `MockPipelineScheduler`
  deleted. Verified: 8 unit + 13 gemma-rest fast tests + 2 gemdtest ITs
  (`PipelineJobBatchServiceMockIT` PARTIAL_BATCH, `JobReconcilerMockIT` STALL) all
  green. Original task spec below. Extend/replace `MockPipelineScheduler` into
  `ScriptedMockScheduler` (scenario registry keyed by experiment id;
  `SUCCEED | FAIL | STALL`, `failureClass`, scripted `stages`, `logLines`,
  `POLL | PUSH` transport, attempt-aware). Add dev-only REST behind
  `@Profile("scheduler-mock")`: `POST /admin/pipeline/_mock/scenario`,
  `…/advance`, `…/emit`, `GET …/_mock/scenarios`. Ship canonical scenario
  JSON under `gemma-rest/src/test/resources/pipeline-scenarios/`
  (`SUCCEED_FAST`, `PARTIAL_BATCH`, `ALL_TRANSIENT`, `PERMANENT_REJECT`,
  `STALL_THEN_RECONCILE`) and share to UIB. Add a startup assertion that
  fails fast if `production` + `scheduler-mock` are both active.
  *No schema change. Acceptance: a service IT drives `PARTIAL_BATCH` →
  advance clock → assert ⌊N/3⌋ jobs FAILED, no `Thread.sleep`; UIB can set
  a scenario, submit a real batch, and watch real endpoints emit real
  shapes.*

- [x] **2. Thin envelope + proxy monitoring** (§3.1). **LANDED 2026-07-13.**
  `recordEvent` now persists only milestone/terminal rows; `progress`/`heartbeat`
  are snapshot-only (`SNAPSHOT_ONLY_KINDS`). Verified by a write-policy IT
  (`stage → progress → progress → completed` yields 2 rows, no progress). Original
  spec below. Stop persisting
  every progress tick: keep the `PIPELINE_JOB` overwrite snapshot
  (`LAST_PROGRESS_JSON`/`LAST_EVENT_AT`/`LAST_EVENT_KIND`) + only
  milestone/terminal rows in `PIPELINE_JOB_EVENT`. Make Gemma's monitoring
  a proxy over the runtime (or the scripted mock), not a poll-fed mirror.
  Under the delegated model this is a write-policy change, not a
  re-platforming. *Acceptance: a running batch produces O(1) event rows
  per job (milestones only), snapshot still current; `JobReconciler`
  churn drops.*

- [x] **3. Attempt/retry model + mop-up surface** (§3.2). **LANDED 2026-07-13
  (attempt-chain, per the task-3 decision — not the counter).** Added
  `ATTEMPT`/`RETRY_OF_FK`/`SUPERSEDED_BY_FK`/`FAILURE_CLASS`/`PARAMS_JSON` to
  `PIPELINE_JOB` (mysql **V23** + h2 **V24**) + `FailureClass` enum + `BatchRollup`
  VO (snake_case) + `RetrySpec`; `retryFailed`/`retryJob` mint attempt N+1 via
  `jobDao.create` (never through the `batch.jobs` Set — hashCode pitfall) and set
  `supersededBy`; `computeRollup` over current attempts; `recordEvent` parses
  `failureClass` from the `error` payload; `maybeCloseBatch` now keeps a batch OPEN
  while a current attempt is FAILED. REST: `GET …/rollup`, `POST …/retry-failed`,
  `POST …/jobs/{jobId}/retry`. Verified: `PipelineJobRetryMockIT` (fail→retry→green,
  chain preserved, idempotent) + `BatchRollupTest` + REST IT. Original spec below.
  Add `ATTEMPT`/`RETRY_OF_FK`/`SUPERSEDED_BY_FK`/`FAILURE_CLASS` to
  `PIPELINE_JOB` (Flyway **V23** + h2 sister). Add `PipelineJobBatchService.retryFailed(batchId,
  RetrySpec)` + `retryJob(jobId, RetrySpec)` (mint attempt N+1, copy
  params + override, set `SUPERSEDED_BY_FK`, dispatch; **refuse if a
  non-terminal successor exists**). Add `BatchRollup` VO +
  `GET /admin/pipeline/batches/{id}/rollup`. Persist `FAILURE_CLASS` from
  the `error` event payload (pipeline-reported first, exit-code heuristic
  fills `UNKNOWN`). *Acceptance: IT — submit → `PARTIAL_BATCH` → assert
  `rollup.needsAttention` → `retryFailed` → advance → assert all DONE;
  double-retry is rejected (idempotent).*

- [x] **4. Batch hold + `maxConcurrent` + dispatcher throttle** (§3.4 #1).
  **LANDED 2026-07-13.** `MAX_CONCURRENT`/`HELD` on `PIPELINE_JOB_BATCH` (mysql
  **V24** + h2 **V25**); `submit` overload throttles at dispatch (budget =
  `maxConcurrent − in-flight`, where in-flight = QUEUED/RUNNING/CANCELLING, NOT
  PENDING); `dispatchPending(batchId)`/`()` + `@Scheduled PipelineJobDispatcher`
  (profile `scheduler`) top up; `holdBatch`/`resumeBatch`/`updateBatch`; REST
  `POST …/hold`·`/resume`, `PATCH …/batches/{id}`, `SubmitBatchRequest.maxConcurrent`.
  **Deviation from the spec below:** hold gates *dispatch* only — the reconciler still
  reconciles in-flight jobs of a held batch (hold ≠ ignore running work); the reconciler
  got a `supersededBy`-skip guard instead. Verified by `PipelineJobThrottleMockIT`
  (never >cap in flight; hold blocks, resume restarts) + REST routes. Original spec below.
  Add `MAX_CONCURRENT INT NULL` + `HELD BOOLEAN NOT NULL DEFAULT FALSE` to
  `PIPELINE_JOB_BATCH`; dispatcher submits `PENDING` up to the budget,
  skips held batches. `PipelineJobBatchService.holdBatch/resumeBatch`;
  `POST …/batches/{id}/hold`·`/resume`, `PATCH …/batches/{id}`. Make
  `JobReconciler` skip jobs with a live successor and skip held batches.
  *Acceptance: a 500-EE batch with `maxConcurrent=10` never has >10
  RUNNING; `hold` stops new submissions, `resume` restarts.*

- [x] **5. Log + artifact proxy endpoints** (§3.5). **LANDED 2026-07-14.** First
  (additive-default) `PipelineScheduler` SPI change: `supportsLog`/`readLog →
  LogChunk{text,next_offset,eof}` + `supportsArtifacts`/`readArtifact → Artifact`.
  Service `readJobLog`/`readJobArtifact` proxy through the scheduler (null → 404,
  never persisted); REST `GET …/jobs/{jobId}/log?offset=&limit=` (JSON) +
  `…/artifacts/{name}` (raw stream). Mock serves `Scenario.logLines` (finally
  consumed) + a canned artifact. **Deferred:** per-pipeline filename whitelist +
  large-file streaming → task 7 (real workdir); here the REST layer does a
  path-traversal guard (400 on `/`,`\`,`..`) and artifacts are `byte[]`. Verified by
  unit + `PipelineJobLogMockIT` (incremental tail + artifact) + REST routes.
  Original spec below. New optional SPI
  `readLog(handle, offset, limit) → LogChunk{bytes,nextOffset,eof}` +
  `readArtifact(handle, name)`. `GET …/jobs/{jobId}/log?offset=&limit=`
  and `…/artifacts/{name}` (whitelist per pipeline). Mock serves scripted
  lines; Nextflow/Slurm read the workdir file.

- [x] **6. Capabilities endpoint + per-job suspend stub** (§3.4 #2).
  **LANDED 2026-07-14.** SPI `supportsSuspend`/`suspend`/`resume` (additive
  defaults: false/throw); `PipelineCapabilities` VO (snake_case);
  `GET /admin/pipeline/capabilities` (kind + `supports_suspend`/`_log`/`_artifacts`);
  `POST …/jobs/{jobId}/suspend`·`/resume` → **409** via a `capabilities().supportsSuspend`
  guard (all schedulers report false today, so it always 409s — the intended stub). No
  `SUSPENDED` job state added (deferred until a Slurm scheduler that supports it lands).
  Verified: unit (mock `supportsSuspend`=false, `suspend` throws), IT (`capabilities()`
  reflects the mock: log/artifacts true, suspend false), REST (capabilities shape + 409).
  Original spec below.
  `GET /admin/pipeline/capabilities` (active kind + `supportsSuspend`/
  `supportsLog`/`supportsArtifacts`). Default `suspend/resume/
  supportsSuspend` on `PipelineScheduler` throwing/false; REST returns
  **409** when `!supportsSuspend`.

- [ ] **7. Real `NextflowSlurmScheduler` — sc-annotation first** (§4.4).
  **IN PROGRESS.** Design fully resolved in
  [`NEXTFLOW_DISPATCH_RESOLUTIONS.md`](./NEXTFLOW_DISPATCH_RESOLUTIONS.md)
  (R1–R13, O1–O9): SSH-to-submit-node dispatch (container has no Slurm/munge);
  **one `nextflow run` per EE** (fits the per-job SPI, trivial correlation);
  `sbatch --parsable` the nextflow head process **as a Slurm job**, handle =
  head-job id (`scancel`/`squeue`/`sacct`); `/space/gemmaData` bind-mounted;
  per-pipeline default `maxConcurrent` (sc-annotation = 25); pipeline checkout
  path a config property (local copy for dev).
  **O3 BUILT (2026-07-16):** `NextflowWeblogTranslator` (gemma-core) +
  `POST /internal/pipeline/jobs/{id}/weblog` ingest translate `-with-weblog`
  messages → `recordEvent` (payload shape pinned vs. real Nextflow 24.10.3;
  terminal from `completed.metadata.workflow.success`; per-task FAILED is
  `progress`, not job failure). Tested against captured fixtures (10 unit +
  5 Jersey cases).
  **`NextflowSlurmScheduler` BUILT (2026-07-16):** `submit`/`poll`/`cancel` —
  `sbatch --parsable` a wrapper (head-job id = handle), `squeue`+`sacct` poll,
  `scancel`; one run per EE; samplesheet+wrapper written to the `/space` mount;
  `-with-weblog … /weblog`. Pure `NextflowSlurmCommandBuilder` + `SshCommandRunner`
  seam (only impure edge); 18 unit tests, existing mock ITs unaffected.
  **Remaining:** per-pipeline `maxConcurrent` default wiring (O8), `readLog`/
  `readArtifact` off the mount (later) + end-to-end run. Cluster/Rachel
  items: canonical checkout path (O5, point at the cached `PREPARE_CACHE`
  version), a partition/QOS for long-lived head jobs (O4). *(O9 reference-sharing
  resolved 2026-07-16 with Rachel — a cached `PREPARE_CACHE` means per-EE runs
  don't redo census+scVI.)* Retires the Jenkins button. *Acceptance: a
  curator-dispatched sc-annotation run reports live events into Gemma and lands
  terminal.*

- [ ] **8. rnaseq-pipeline Luigi → Nextflow port** (§4.3) — the long pole,
  after the SPI is proved on sc-annotation. Port `PrepareReference` +
  `AlignSample` + `CountExperiment` (same patched STAR/RSEM binaries),
  then the SC/Cell Ranger branch, then drop the `gemma-cli` round-trip
  tail in favour of Gemma-side ingest. Move Google-Sheet polling into a
  Gemma scheduled job that drains the sheet into the queue. *Acceptance:
  10-GSE replay panel — count matrices diff-clean vs Luigi.*

- [x] **9. PipelineJob → Ticket edge** (§1.2 #1). **LANDED 2026-07-14.** New
  `TicketType.PIPELINE_FAILED` (enum-only, no migration); `recordEvent`'s error
  branch calls `maybeOpenTicketOnFailure` — for PERMANENT/UNKNOWN (skip TRANSIENT,
  gated on `gemma.pipeline.autoTicket.enabled` default true) it opens (or, via
  `findOpenForTarget` dedup, appends to) a `PIPELINE_FAILED` ticket targeting the EE
  with the failure detail in a COMMENTED event; reporter = `batch.submittedBy`;
  best-effort (try/catch so a ticket glitch never fails the terminal event). Mirrors
  `GeoScrapeServiceImpl`. Verified by `PipelineJobTicketMockIT` (permanent→one ticket
  +detail, deduped across batches; transient→none). **Deferred:** per-pipeline policy
  granularity + auto-retry on TRANSIENT (D8 manual-first) + ticket→retry / close-on-
  success round-trip (§1.2 #4). Original spec below. Policy-gated auto-open
  of a `Ticket` targeting the EE on `FAILED`/attention-needed
  (auto-ticket PERMANENT, auto-retry TRANSIENT).

- [ ] **10. WorkflowGroup entity + `/groups` CRUD** (§2.3), Flyway V23+.
  Per `WORKFLOW_GROUPS_RECCE.md` §3–§4; VO snake-case via `@JsonProperty`.
  Wire the "dispatch group → batch" edge (§1.2 #2). *Acceptance:
  `POST /groups`, `GET /datasets/{id}/groups`, and "dispatch" mints a
  `PIPELINE_JOB_BATCH` from the group's current EE members.*

- [x] **11. CurationDetails retirement** (Tickets D1). **LANDED 2026-07-14 as the
  ticket-derived-CACHE variant** (Ogan's call — the "flip every read-path to a ticket
  lookup" reading was a lossy multi-week migration: the `TROUBLED`/`NEEDS_ATTENTION`
  columns back the core `/datasets` filter+sort surface, and a "has open ticket" *sort*
  isn't cheaply expressible). Instead: tickets stay source of truth (writes already went
  there → columns were frozen/stale), and `CurationFlagCache` recomputes the target
  Curatable's `troubled`/`needsAttention` from its open tickets on every
  `TicketServiceImpl.openTicket`/`transition` (cycle-free: the caller passes the queried
  open tickets in; write via the EE/AD DAO for L2 coherence). All existing reads/filters/
  sorts keep working unchanged; the columns are now truthful. Shared type-mapping with the
  `CurationDetailsService` shim (+ `PIPELINE_FAILED` → needs-attention, composing task 9).
  Verified: `CurationFlagCacheMockIT` (open QUALITY_REVIEW → troubled+needsAttention +
  `loadTroubledIds` sees it; resolve → clears; BATCH_INFO → needsAttention-only; pipeline
  PERMANENT failure → needs-attention via the auto-ticket). **Prod backfill BUILT** (2026-07-15):
  `LegacyCurationFlagMigrator` + `migrateCurationFlagsToTickets` CLI **forward-migrate** the frozen
  legacy flags into tickets (troubled → QUALITY_REVIEW, needsAttention-only → GENERIC; date from the
  `lastTroubled/NeedsAttentionEvent` pointer with a null-safe fallback; provenance in the title +
  a structured comment; idempotent) — it **never clears** a legacy flag (they're real signal, not
  stale), preserving the corpus. Run once at deploy. Verified by `LegacyCurationFlagMigratorIT`.
  **Deferred:** ArrayDesign flag migration (AD columns keep working untouched); the "pure" column
  drop / repointing the ~18 point-readers (unnecessary now the cache is truthful).
  Original spec below. Flip
  `needsAttention`/`troubled` read-paths to "is there an OPEN ticket
  targeting this EE?".

- [ ] **12. (Deferred) live-updates + long tail.** SSE
  `GET …/batches/{id}/stream` (only if poll lags); Redis live-tail (§4.2
  B, gated on measured need); expose `addGEOData`/`downloadSingleCellData`/
  `loadSingleCellData`/`aggregateSingleCellData` as REST endpoints through
  the scheduler so curators stop SSHing entirely.

---

## 8. Open decisions (with recommended defaults)

Each carries a recommended default so a dev is never blocked; the default
is reversible and marked where it needs a human before committing infra.

| # | Decision | Recommended default | Needs sign-off? |
|---|---|---|---|
| **D1** | Runtime / job-state location (§3.1, §4.2) | **DECIDED 2026-07-13 (Ogan): Delegated / Nextflow-native (Option A)**: `-with-weblog` → existing callback, `-resume` = mop-up, workdir log proxy, thin MySQL envelope with the churn table trimmed. Zero new infra. | ✅ decided; infra pieces (weblog path, workdir, caps) still need the cluster owner at task 7. |
| **D2** | `maxConcurrent` / aggregate submission cap (§3.4) | **Per-pipeline-kind cap** (rnaseq needs more than sc-annotation's `queueSize=25`), plus a global ceiling. | Yes — cluster admin sets the numbers. |
| **D3** | Pipeline output handoff: push vs pull | **Pull** where practical (Gemma fetches completed outputs, ingests via its own loaders — cleaner for credentials); keep sc-annotation's `GEMMA_UPLOAD` push path working as the fallback. | Soft — revisit per pipeline. |
| **D4** | Provenance stamp on `PIPELINE_JOB` | **Yes** — record pipeline git SHA + container digest in `PARAMS_JSON` (global repro convention). | No — just do it. |
| **D5** | Workdir location / `-resume` semantics | **Shared resumable** (`/cosmos/data/…`) for `-resume` to work across attempts, with a disk-pressure cleanup job; per-run scratch only for throwaway steps. | Yes — cluster owner (disk policy). |
| **D6** | Google Sheet sunset | **One-way export Gemma→Sheet during transition**, then cutover once curators (Sanja, Salva) are on the UI. Don't hard-cut. | Soft — curator onboarding. |
| **D7** | Retry-param override granularity | **Batch-level** default (one override for all retried jobs); per-job override only when asked. | No. |
| **D8** | Auto-retry vs manual | **Manual-first** — curator clicks "retry transient"; add per-pipeline `autoRetryMax` only once the classifier is trusted (avoids retry storms). | No. |
| **D9** | `FAILURE_CLASS` source of truth | **Pipeline reports it** in the terminal event payload; Gemma's exit-code heuristic only fills `UNKNOWN`, never overrides. | No. |
| **D10** | Per-job "accept failure" without closing batch | **Not now** — closing the batch is the accept action; add a per-submission `acknowledged` flag only if curators ask. | No. |
| **D11** | WorkflowGroup ACL / sharing | **Creator-write + group-read** default; per-group sharing toggle later. | Soft. |
| **D12** | Ticket comment edit-in-place (D4 in Tickets) | **Append-only** for now; add `edited_at`/`edited_by` only if edit-latest-row is wanted. | Soft. |
| **D13** | Curator↔admin role split (Tickets D5) | **Keep `GROUP_ADMIN`** on the pipeline surface for now; split when the curator role is defined. | Soft. |

**Parked (no action; revival triggers noted):** RabbitMQ/Modulith broker
(§4.1 — revive only for many heterogeneous producers / multi-consumer
fan-out / replay at a volume where row-polling measurably bottlenecks);
GitHub issue sync (Tickets D7); **cross-spine DAG orchestration** ("after
RNA-seq DONE auto-open a DEA ticket" — a `parent_ticket_id` column + a
listener; revisit once ≥2 pipelines are live); cancellation-mid-run event
round-trip; Nextflow Tower and Temporal (only if orchestration grows past
one-job-per-EE into multi-stage sagas).

---

## 9. How to run / test what exists

**Exercise the shipped surface (mock scheduler):**

- Submit a batch: `POST /admin/pipeline/batches` (auth `GROUP_ADMIN`),
  list `GET /admin/pipeline/batches[?state=&limit=]`, detail
  `GET …/batches/{id}`, cancel `POST …/batches/{id}/cancel`.
- Watch job events: `GET …/batches/{id}/jobs/{jobId}/events?sinceMillis=`.
  The `MockPipelineScheduler` drives a ~15 s synthetic run that always
  succeeds (poll-only today — it cannot fail or push yet; task 1 fixes
  that).
- Push callback (what a real runtime hits):
  `POST /internal/pipeline/jobs/{jobId}/events` (bearer), keyed by Gemma's
  `jobId`.
- CLI: `PipelineJobReporter` reports batch/job status.
- The `JobReconciler` `@Scheduled` loop polls stale non-terminal jobs
  automatically (profile-gated, like `SchedulerConfig` Quartz jobs — only
  one node runs it in prod).

**Test layers (all run in `mvn verify`, fast + non-flaky once the scripted
mock + deterministic clock land — no real network, no `Thread.sleep`):**

| Layer | What | Where |
|---|---|---|
| Unit | state machine (`recordEvent` kind→state), retry minting + idempotency, failure classification, rollup math | `gemma-core` `*Test` |
| Service IT | submit → scripted mock `PARTIAL_BATCH` → assert `rollup.needsAttention` → `retryFailed` → advance clock → assert all DONE (against gemdtest) | `gemma-core` `@Tag("integration")` |
| Reconciler IT | `STALL_THEN_RECONCILE`: poll-only scenario, tick the reconciler, assert synthetic events land | `gemma-core` |
| REST IT | full admin surface incl. retry/suspend/log/rollup via JerseyTest5 + scripted mock | `gemma-rest` |
| Contract | scenario JSON fixture pinned in both repos; CI diff catches drift | shared |

Canonical `mvn verify` invocation and gemdtest setup are in the project
`CLAUDE.md` (JDK 25 / temurin-25; keychain `mysql-root` password).

**Real-cluster pipelines today (pre-Gemma-dispatch):** sc-annotation runs
via `nextflow run sc-annotate.nf -profile conda -params-file params.mm.json
… -process.executor slurm -resume` behind a **Jenkins button**; rnaseq
runs via `rnaseq-pipeline-cli run rnaseq_pipeline.tasks.
SubmitExperimentToGemma --experiment-id GSEXXX --workers 30` (Luigi;
`luigid` UI at `localhost:8082`).

---

## Provenance

Consolidated 2026-07-05 from four recces filed 2026-05-19 → 2026-06-23.
The four source recces are retained as the detailed reasoning behind each
piece; their archive location is `docs/pipeline-compute/archive/` (move
the originals there once this handoff is adopted — they currently live at
`docs/*.md`). Source files:
`WORKFLOW_AND_COMPUTE_ARCHITECTURE.md`,
`PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md`,
`PIPELINES_AND_SCHEDULER_RECCE.md`,
`EXTERNAL_PIPELINE_HANDOFF_RECCE.md`.

| Consolidated section | Primary source(s) |
|---|---|
| §1 The unified model — three spines | `WORKFLOW_AND_COMPUTE_ARCHITECTURE.md` (§1, §5, §6, §7) |
| §2 Current shipped surface | `WORKFLOW_AND_COMPUTE_ARCHITECTURE.md` (§2, §3); `PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md` (§1) |
| §3 Job state & control plane | `PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md` (§1b–§8) |
| §4 Compute backend — Nextflow, Slurm, runtime | `WORKFLOW_AND_COMPUTE_ARCHITECTURE.md` (§4); `PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md` (§1b.2); `PIPELINES_AND_SCHEDULER_RECCE.md` (§3, §4) |
| §5 External pipeline handoff (parked) | `EXTERNAL_PIPELINE_HANDOFF_RECCE.md` (all) |
| §6 Scheduler, curator CLI surface | `PIPELINES_AND_SCHEDULER_RECCE.md` (§1, §2, §5, Surprises) |
| §7 Remaining work + §8 Open decisions | `WORKFLOW_AND_COMPUTE_ARCHITECTURE.md` (§8, §9); `PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md` (§9, §10); `PIPELINES_AND_SCHEDULER_RECCE.md` (§6, §7) |
| §9 How to run / test | `PIPELINE_COMPUTE_TEST_AND_CONTROL_RECCE.md` (§6); `PIPELINES_AND_SCHEDULER_RECCE.md` (§2, §4.2) |
| Implementation status table | all four (verbatim class/endpoint anchors) |

*Update this document when a spine's "still open" list changes; retire a
source recce's standalone status here when its remaining work lands.*
