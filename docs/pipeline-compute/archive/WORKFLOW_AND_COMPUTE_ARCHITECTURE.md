# Workflow management + distributed compute — unified architecture

**Status:** consolidated design of record. Supersedes and reconciles four
prior recces. Reflects what has **shipped** as of 2026-06-23, what is
**parked**, and what remains **open**.

**Reconciles:**

| Source recce | Filed | Disposition |
|---|---|---|
| `AUDIT_AS_WORKFLOW_RECCE.md` | 2026-05-19 | **Shipped** — Ticket layer (Option B). |
| `PIPELINES_AND_SCHEDULER_RECCE.md` | 2026-05-20 | **Shipped (skeleton)** — PipelineJob + scheduler SPI + reconciler. |
| `EXTERNAL_PIPELINE_HANDOFF_RECCE.md` | 2026-05-19 | **Parked** — RabbitMQ/Modulith event bus; superseded by the poll/push reconciler. |
| `WORKFLOW_GROUPS_RECCE.md` | (recce) | **Open** — not built; reconciled here against PipelineJobBatch. |

The four recces stay on disk as the detailed reasoning behind each piece.
This document is the single map that sits over them: it states the model
that actually exists, resolves where the recces overlapped or disagreed,
and lists the remaining work as one plan instead of four.

---

## 1. The core reconciliation: three spines, not one

The recces circled the same territory from different angles and, read
together, blur into "build a workflow/job system." Implementation pulled
them apart into **three distinct spines** that must not be collapsed into
each other. Naming them is the whole reconciliation:

| Spine | Entity (shipped?) | Question it answers | Lifespan |
|---|---|---|---|
| **Work item** | `Ticket` ✓ | *What should happen to this entity / set, who owns it, what state is it in?* | Opened → worked → closed; days–weeks. |
| **Compute run** | `PipelineJob` ✓ | *What specific external computation did we dispatch, where is it, did it land?* | Submit → run → terminal; minutes–hours. |
| **Working set** | `WorkflowGroup` ✗ | *Which EEs is a curator navigating / batching together right now?* | Durable saved queue; weeks–months. |

These were conflated across the recces because all three "track work over
time and have a state machine." But they have different owners (curator
intent vs. cluster vs. UI navigation), different lifespans, and different
write rates. **Each gets its own table and its own state machine.** The
value is in the *edges between them*, defined in §5.

A useful one-liner per spine:

- A **Ticket** is "do this thing" — a durable, auditable, assignable unit
  of curator (or agent) intent that targets 1..N entities.
- A **PipelineJob** is "this `nextflow run` against the cluster" — one
  external computation, correlated by an opaque scheduler handle.
- A **WorkflowGroup** is "my screening / pipeline / review queue" — an
  ordered, navigable set of EEs the UI pages through.

---

## 2. Spine 1 — Tickets (shipped)

Decision record: `AUDIT_AS_WORKFLOW_RECCE.md` (Option B chosen; user
decisions 1–7 at its tail are authoritative). Implemented across the
`feat(tickets)` commit series; schema in `V3__ticket_layer.sql` +
`V19__ticket_mode_and_target_status.sql`.

**What exists:**

- `TICKET` — `type`, `state` (explicit, **not** derived), `priority`,
  `due_date`, `reporter_fk`, `assignee_fk`, `mode` (`MANUAL | AUTO`),
  `external_issue_url` + `external_issue_sync_state`. `Ticket` is itself
  `Auditable` (owns an `AUDIT_TRAIL_FK`) — Decision 6.
- `TICKET_TARGET` — 1..N targets of mixed `target_type`
  (`EXPRESSION_EXPERIMENT`, `ARRAY_DESIGN`, … extensible), each with a
  per-target `status` (`NOT_DONE | UNDERWAY | DONE`) for agent step
  tracking. `(target_type, target_id)` composite index serves the
  "open tickets targeting this EE" reverse lookup without joins.
- `TICKET_EVENT` — append-only workflow log (`COMMENTED`,
  `STATE_CHANGED`, `ASSIGNED`, target-status changes) with JSON payload.
- Two parallel streams (Decision 6): `TICKET_EVENT` carries domain
  workflow facts; the inherited `AuditEvent` trail carries governance
  ("who touched this row"). Both populated on writes.
- `TicketService` / `TicketDao*` / `TicketsWebService` (full read+write
  REST). Event types `TicketOpenedEvent`, `TicketStateChangedEvent`,
  `TicketAssignedEvent`, `TicketMetadataChangedEvent`,
  `TicketTargetStatusChangedEvent`.

**Settled decisions** (from the recce tail, now law): tickets replace
`CurationDetails` / `needsAttention` / `troubled` (Decision 1); 1..N
mixed-type targets (Decision 2); comment auth = any authenticated
curator/admin (Decision 3); append-only for now (Decision 4); GitHub
mirror columns provisioned but sync deferred (Decision 7).

**Still open on this spine:** full retirement of `CurationDetails`
read-paths in favour of "is there an OPEN ticket targeting this EE?";
the curator↔admin role split (Decision 5); comment edit-in-place
(Decision 4); GitHub issue sync activation (Decision 7).

---

## 3. Spine 2 — PipelineJobs + scheduler (shipped skeleton)

Design: `PIPELINES_AND_SCHEDULER_RECCE.md`. The recce sketched a
`PIPELINE_RUN` table + `PipelineExecutor` SPI; implementation **evolved
the shape** into a batch→job→event hierarchy with a reconciler, and
renamed the SPI. The recce's §5–§7 (curator pain inventory, Luigi→Nextflow
assessment, open questions) remain the reference; its §5.4 schema is
**superseded** by what landed.

**What exists** (`V18__pipeline_jobs.sql` + `model/pipeline/*` +
`core/pipeline/*` + `persistence/service/pipeline/*`):

- `PIPELINE_JOB_BATCH` — one curator submission of a `pipeline` (e.g.
  `rnaseq-quant`) over N experiments. `Auditable`; owner-scoped via
  `submitted_by_fk`; `state` `OPEN | CLOSED | CANCELLED`;
  `kill_requested_at`; versioned `params_json`.
- `PIPELINE_JOB` — one EE's run within a batch. `state` `PENDING →
  QUEUED → RUNNING → DONE | FAILED | CANCELLING | CANCELLED`;
  scheduler-agnostic via `scheduler_kind` (`luigi | nextflow | mock`) +
  opaque `scheduler_handle`; `last_event_*` snapshot columns; a
  dedicated `(state, last_event_at)` **reconciler index** for the
  "non-terminal jobs whose last event is stale" poll seek.
- `PIPELINE_JOB_EVENT` — append-only per-job event stream
  (`progress | stage | stderr | killed | error | completed`); `kind`
  is free VARCHAR so the scheduler can add kinds without a migration.
- `PipelineScheduler` SPI (`submit` → `SchedulerHandle`, poll, cancel)
  with `MockPipelineScheduler` in place; `SubmitRequest` /
  `SchedulerHandle` value types; `JobReconciler` poll loop; CLI
  `PipelineJobReporter`.

**The two integration modes (both schema-supported):**

1. **Poll** — `JobReconciler` seeks stale non-terminal jobs via the
   reconciler index and asks the scheduler for status. Default; works
   for any scheduler that exposes a status query (`squeue`, Nextflow
   `trace.txt`, Luigi scheduler API).
2. **Push** — the running job (or its epilogue script) calls a Gemma
   callback keyed by `(scheduler_kind, scheduler_handle)` — unique
   key supports lookup by scheduler id alone. Better for long jobs
   that outlive a worker.

**Still open on this spine:** real `NextflowSlurmScheduler` +
`LuigiScheduler` adapters (only `mock` today); the push-callback REST
endpoint; the curator-UI "Pipelines" surface; the Luigi→Nextflow port of
`rnaseq-pipeline` (recce §3.4); retiring the RNA-seq Google Sheet and the
single-cell tracker sheet in favour of DB-backed queue views.

---

## 4. Transport reconciliation: reconciler won, broker parked

This is the one place the recces **actively disagreed**, and the
disagreement is now resolved by what shipped.

`EXTERNAL_PIPELINE_HANDOFF_RECCE.md` proposed a Spring-Modulith
`event_publication` table + `@Externalized` → **RabbitMQ** broker as the
two-way handoff substrate, with the `Ticket` as the correlation handle.
`PIPELINES_AND_SCHEDULER_RECCE.md` proposed a Gemma-side job table polled
against the scheduler. **V18 + `JobReconciler` chose the latter.** The
correlation handle for a compute run is the
`PIPELINE_JOB.scheduler_handle`, not a Ticket; durability is the
`PIPELINE_JOB` row + `PIPELINE_JOB_EVENT` log, not an `event_publication`
table.

**Why the reconciler model is the design of record:**

- No new broker to run, secure, and keep alive (RabbitMQ container,
  users, TLS, retention — all avoided). Gemma already owns a MySQL row
  per job; that row *is* the durable source of truth.
- The poll/push pair already covers the two real cases (short jobs:
  poll; long jobs: epilogue push) the handoff recce itself enumerated
  in its §5.1.
- One operational story, fewer moving parts on a monolith that the
  parent track is explicitly "modernise without splitting."

**What the broker recce keeps for the record** (not discarded — parked
with a revival trigger): the event-schema discipline (immutable record
events, `payloadVersion` discriminator, shared JSON Schema with a CI
diff against the Python shadow classes) is the **right contract for the
push callback payloads too** — adopt it for `PIPELINE_JOB_EVENT`
`payload_json` shapes even though the transport is HTTP, not AMQP.
**Revive the broker only if** a future need appears that the reconciler
can't serve cleanly: many heterogeneous external producers, fan-out to
multiple independent consumers, or back-pressure/replay semantics across
process restarts at a volume where row-polling is a measured bottleneck.
Until then, `event_publication` + `@Externalized` stays out of the
classpath.

---

## 5. The edges between the spines (where the value is)

The spines are separate tables; the system is the relationships:

1. **PipelineJob → Ticket (failure / attention).** When a
   `PIPELINE_JOB` goes `FAILED` — or finishes `DONE` but needs curator
   judgement — open (or relate) a `Ticket` targeting that EE, with the
   failure detail in the first `TICKET_EVENT`. This is the handoff
   recce's "ticket as correlation handle" idea, correctly placed: the
   *compute* handle is `scheduler_handle`; the *human follow-up* handle
   is the Ticket. Auto-open is policy-gated per pipeline (transient
   errors → retry in place; hard errors → ticket).
2. **WorkflowGroup → PipelineJobBatch (dispatch a set).** A curator
   batches a saved `WorkflowGroup` of EEs into one `PIPELINE_JOB_BATCH`
   submission. The group is the durable working set; the batch is one
   compute act over it. This is why they stay separate tables (§6) — a
   group is dispatched many times over its life.
3. **Ticket ↔ WorkflowGroup (worklist).** An audit/recuration loop can
   materialize its worklist as a WorkflowGroup of `review` type; tickets
   target the same EEs. Membership is by EE id — no FK tangle between
   the two.
4. **PipelineJob → per-target Ticket status.** A batch driven from a
   ticket's targets can write back `TICKET_TARGET.status`
   (`NOT_DONE → UNDERWAY → DONE`) as each job lands — the `MODE=AUTO`
   advance path (V19) was built for exactly this agent-driven stepping.

---

## 6. Grouping reconciliation: WorkflowGroup vs PipelineJobBatch

Both group experiments; the recces never reconciled them because
`WORKFLOW_GROUPS_RECCE` predates `PIPELINE_JOB_BATCH` landing. They are
**not** the same and **should not** share a table:

| | `WORKFLOW_GROUP` (proposed) | `PIPELINE_JOB_BATCH` (shipped) |
|---|---|---|
| Purpose | Curator's saved navigable queue | One compute submission over N EEs |
| Lifespan | Durable; reused for months | Per-run; closes when jobs finish |
| Membership | Ordered (`position`), incl. non-EE UUID candidates | EE-only, set of jobs |
| Types | `screening / pipeline / review` | n/a (one `pipeline` string) |
| Dispatched | Many times | Once |

**Reconciliation:** keep both. `WorkflowGroup` is the front-of-house
working set the curator UI pages through (set-navigator prev/next);
`PipelineJobBatch` is back-of-house compute bookkeeping. The edge is §5.2
— "dispatch this group" mints a batch from the group's current EE
members. Build `WorkflowGroup` per `WORKFLOW_GROUPS_RECCE.md` §3–§4 when
the curation-UI set-navigator is greenlit; do **not** try to host it on
`PIPELINE_JOB_BATCH` (no ordering, no screening UUIDs, wrong lifespan)
nor on `ExpressionExperimentSet` (the recce's §2 already rejected that).

A note the WorkflowGroups recce got slightly stale on: its §3 assumed the
next free MySQL migration was `V4` and referenced a `V2 audit_event` /
`V3 ticket` sequence. The live tip is **V22**; a WorkflowGroup migration
lands as **V23+** (mysql) with its h2 sister. The rest of that recce
(entity layout, REST surface, VO snake-case via `@JsonProperty`, sizing
~1,700 LOC) still stands.

---

## 7. Wire-shape convention (cross-cutting)

`PIPELINESTATUS_WIRE_AUDIT.md` is the worked precedent for the whole
curation-UI surface and generalizes to all three spines: the UI was
built against a FastAPI mock and expects **snake_case**; Gemma VOs are
camelCase. The settled convention:

- **Cosmetic snake/camel mismatch** → fix Java-side with
  `@JsonProperty("snake_case")` on the curation-UI-facing VO
  (`PipelineStatusValueObject`, `TicketValueObject`, future
  `WorkflowGroupValueObject`). One file, no behavioural change, plus a
  unit test asserting the wire keys.
- **Structural mismatch** (object-of-steps vs list-of-steps, vocabulary,
  collapsed "diagnostics") → fix **UI-side** with a thin fetch adapter;
  do not bloat the wire with duplicated/derived shapes.
- **UI-only concepts** with no Gemma equivalent (`candidate_provenance`,
  per-step `in_progress`/`needs_attention`) → adapter supplies
  null/default; don't model server-side until there's a real source.

---

## 8. Unified forward plan

Ordered by dependency, merging the four recces' phase lists and dropping
everything already shipped:

1. **Real schedulers.** `NextflowSlurmScheduler` (+ `LuigiScheduler` if
   the RNA-seq port slips) implementing `PipelineScheduler` against the
   `sc-annotation-pipeline` first (already Nextflow-on-Slurm). Retires
   the Jenkins button. *(Pipelines recce session 2.)*
2. **Push-callback REST endpoint** keyed by `(scheduler_kind,
   scheduler_handle)`, with payloads following the parked broker recce's
   event-record discipline (§4). Epilogue script publishes terminal
   events for long jobs.
3. **PipelineJob → Ticket edge** (§5.1): policy-gated auto-open on
   `FAILED` / attention-needed.
4. **Curator-UI Pipelines surface** — per-EE tab + bulk
   `queued|running|failed|succeeded` view, SSE/poll status, Cell Ranger
   `web_summary.html` fetcher. Retires the RNA-seq Google Sheet + SC
   tracker sheet. *(Pipelines recce session 4.)*
5. **WorkflowGroup entity + `/groups` CRUD + `/datasets/{id}/groups`**
   per `WORKFLOW_GROUPS_RECCE.md`, as V23+. Wire the §5.2 "dispatch
   group → batch" edge. *(WorkflowGroups recce, ~1 session.)*
6. **rnaseq-pipeline Luigi→Nextflow port**, validated on a 10-GSE replay
   panel. *(Pipelines recce session 3 — the long pole; do after the
   scheduler SPI proved out on sc-annotation.)*
7. **CurationDetails retirement** — flip `needsAttention`/`troubled`
   read-paths to "OPEN ticket targeting this EE?" *(Tickets Decision 1
   tail.)*

Deferred / parked: RabbitMQ broker (§4 revival trigger); GitHub issue
sync (Tickets Decision 7); curator↔admin role split (Tickets Decision 5);
cross-spine DAG orchestration ("after RNA-seq DONE, auto-open DEA
ticket" — the handoff recce §9 idea; revisit once ≥2 pipelines are live).

---

## 9. Consolidated open questions

Deduplicated across the four recces; the ones still genuinely unsettled:

1. **Pipeline output handoff: push vs pull.** Does the scheduler push
   results to Gemma (sc-annotation's `GEMMA_UPLOAD` subworkflow), or
   does Gemma pull completed outputs and ingest via its own loaders?
   Pull is cleaner for credentials; push reuses existing wiring.
   *(Pipelines Q5.)*
2. **Pipeline provenance stamp.** Record pipeline git SHA + container
   digest on `PIPELINE_JOB` / `PARAMS_JSON`? The global repro
   convention says yes. *(Pipelines Q8.)*
3. **Scheduler queue caps.** Aggregate Slurm submission cap the cluster
   admin wants Gemma to respect — global or per-pipeline-kind?
   *(Pipelines Q7.)*
4. **Workdir location / `-resume` semantics.** Per-run scratch vs shared
   resumable; disk-pressure policy. *(Pipelines Q4.)*
5. **WorkflowGroup ACL / sharing.** Creator-write + group-read default,
   or per-group sharing toggle? *(WorkflowGroups Q2.)*
6. **Google Sheet sunset.** Some curators work from the sheet, not the
   UI — one-way export Gemma→Sheet during transition, or hard cutover?
   *(Pipelines Q6.)*
7. **Comment edit-in-place** for tickets — append-only vs the sibling
   repo's edit-latest-row semantics; needs `edited_at`/`edited_by` now
   if ever. *(Tickets Decision 4.)*

---

*Consolidated 2026-06-23. No production code modified by this document —
it is the map over the four recces, not a new proposal. Update it when a
spine's "still open" list changes, and retire a recce's standalone status
line here when its remaining work lands.*
