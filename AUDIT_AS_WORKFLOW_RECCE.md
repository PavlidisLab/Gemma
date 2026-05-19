# Audit-system as workflow / ticket tracker — Phase 3 recce

**Filed:** 2026-05-19. Companion to the in-flight `AUDIT_SYSTEM_AUDIT.md`
(parallel recce on current-state + `@Audited(EventType.class)` migration
scope) and to `AGENT_WRITEBACK_RECCE.md` (the writeback side of the same
loop). This doc asks the next question: once Gemma's audit system has
been refactored to method-level annotations, can it carry **tickets /
workflow state** in addition to its current "what happened to this
entity" log?

Scope: how the sibling repos already model curation tickets + workflow,
how that maps onto Gemma's `AuditEvent` primitives, three storage
shapes, a recommendation, and a phased rollout that piggybacks on the
`@Audited` migration.

---

## 1. Sibling-repo synthesis

Both sibling repos were located and read end-to-end (the relevant
subsets):

| Repo | Path | Disk | Notable docs |
|---|---|---|---|
| Agent backend | `~/Dev/gemma-curation-agents` | 1.7 GB (includes `.venv`, sqlite mock, eval data) | `docs/THREE_MODES.md`, `docs/SHADOW_PIPELINE_SPEC.md`, `docs/IDEAS_FROM_CONCURRENT_WORK.md`, `docs/RESOLVER_VISION.md`, mock SQLite tables |
| UI frontend | `~/Dev/gemma-curation-ui` | 257 MB (mostly node_modules) | `apps/curation/WORKFLOW_MANAGEMENT.md`, `WORKFLOW_MANAGEMENT_HANDOFF.md`, `AUDIT_FEATURE.md`, `AUDIT_STATUS_CLOSED_RULE_HANDOFF.md`, `AUDIT_DISPOSITION_*`, `CROSS_REPO_COMPAT.md`, `GEMMA_WIRE_ALIGNMENT_HANDOFF.md` |

The sibling `AUDIT_SYSTEM_AUDIT.md` (parallel agent
`a2ff520b2e4f5ba7c`) is **not yet on disk** at the time of this recce —
this doc proceeds on the published audit primitives directly and will
need a cross-check pass once the sibling lands.

### 1.1 The lifecycle the UI wants Gemma to track

`WORKFLOW_MANAGEMENT.md` (gemma-curation-ui, lines 21–33) defines an
**eight-state experiment lifecycle**:

> Discovery → Candidate → Skeleton → Loaded → Curate → Process →
> Audit → Public

with a **recuration loop** (Audit → Curate, lines 35–40):

> "When the auditor flags a problem and the curator accepts the
> disposition, the experiment returns to the Curate state with the
> audit's findings as the worklist."

Plus two cross-cutting layers (lines 42–72):

- **Task tickets** — "specific work items targeted at one or more
  experiments… needs alignment to genome / outlier review / batch
  confound revisit / publication relink / tag drift sweep… Tickets
  can fire against any lifecycle state. They are a separate object
  type from audits."
- **Evaluations** — metrics-producing runs (holdout sets,
  calibration packages, ablations) that share provenance + audit
  trail with production curation.

### 1.2 What already exists in the mock (curation.sqlite)

The Python agent backend already runs a SQLite mock with this shape:

```
audits                 -- one row per AuditReport (audit_id, experiment_id,
                          audited_at, model, body_json, finalized_at,
                          finalized_by, finalized_notes)
audit_dispositions     -- append-only log: (audit_id, target_id, status,
                          reviewer, reviewed_at, notes, dismiss_reason,
                          accept_reason, not_sure_reason, applied_fix,
                          issue_code, severity, target_kind, judge,
                          inherited_from, …)
audit_events           -- per-experiment timeline mirror of Gemma's
                          AuditEvent (experiment_id, date, performer,
                          action, event_type, note, detail, body_json)
candidates             -- pre-Gemma triage (state machine: pending →
                          in_review → approved/excluded/deferred → loaded)
curation_groups        -- typed queues: screening / pipeline / review
curation_group_members
proposals              -- agent-proposed curation, append-only
tasks                  -- async pipeline-step dispatch handles
pipeline_status        -- per-experiment per-step (status, last_run, details)
designs / design_history  -- design drafts + revision log
```

`AUDIT_FEATURE.md` (lines 60–122) is the authoritative wire schema for
`AuditFinding` / `AuditReport` / `AuditFindingDisposition`. The mock
ships an end-to-end ticket-style lifecycle today:

- A `severity` per finding (`ok / minor / major / blocker`)
- A `status` per disposition (`pending / accepted / dismissed /
  needs_more_info`)
- A `finalized_at + finalized_by + finalized_notes` close trio per
  audit (the `AUDIT_STATUS_CLOSED_RULE_HANDOFF.md` fix shipped
  2026-05-13)
- An `inherited_from` chain for cascaded dispositions
- Counts surfaced on `DatasetSummary`: `n_unactioned_blocker /
  _major / _minor` and `latest_audit_verdict`

That is already a ticket-tracker in everything but name. **It's just
not in Gemma.** Today it lives in the agent-side SQLite, and the only
write-back path to real Gemma is `PUT /datasets/{id}/curationDetails`
(per `AGENT_WRITEBACK_RECCE.md`).

### 1.3 The "candidate" pre-Gemma queue

`WORKFLOW_MANAGEMENT_HANDOFF.md` lines 30–88 defines a separate
`Candidate` entity for pre-Gemma triage with its own state machine
(`pending → in_review → approved → loaded`, with `excluded` /
`deferred` branches). The interesting bit: a `Candidate` carries
`source_batch`, `reviewer`, `decision_reason`, and on promotion to
Gemma binds a `gemma_id`. The screening-world / Gemma-world boundary
(handoff lines 13–28) is **a one-way promotion**, never a soft
reference — once a candidate becomes a `SkeletonInvestigation` (per
`AGENT_WRITEBACK_RECCE.md`), it leaves the candidate table.

### 1.4 Top design constraints surfaced

Distilling the three sibling docs and the inline AGENT_WRITEBACK_RECCE:

| # | Constraint | Source |
|---|---|---|
| 1 | **Tickets must coexist with the existing `AuditEvent` log, not replace it.** Audit findings (auditor-produced) and tickets (curator/pipeline-produced) are different kinds of work item but share assignee / disposition / close semantics. | `WORKFLOW_MANAGEMENT.md` lines 42–57: "Tickets… are a separate object type from audits… a ticket is 'do this specific thing' rather than 'react to this finding.'" |
| 2 | **The state machine must be explicit.** The old Gemma dataset manager (UI doc lines 144–211) inferred state from timestamp existence — universally regarded as bad. `pending → in_review → … → closed` is what the curators actually need. | `WORKFLOW_MANAGEMENT.md` lines 199–211: "No explicit state machine — workflow state inferred from which timestamps exist. Nothing says 'this experiment is at step 8.'" |
| 3 | **Append-only / event-sourced is the desired shape.** The mock disposition log is already append-only with "latest wins per (audit_id, target_id)"; the `AuditEvent` table on Gemma is also append-only. The new schema must not regress to mutable status fields. | `AUDIT_FEATURE.md` lines 250–258 + `AGENT_WRITEBACK_RECCE.md` lines 92–98 ("attaching JSON to an audit event overloads event payload semantics — events are receipts, not state"). |

---

## 2. Gemma's existing audit primitives — quick recap

(Full audit lives in the in-flight `AUDIT_SYSTEM_AUDIT.md`; here just
what's load-bearing for this doc.)

```
Auditable (interface)        — anything that has an AuditTrail
  └─ AuditTrail              — owns List<AuditEvent>
        └─ AuditEvent
              ├─ id, date, action (C/U/D), detail, note, performer
              └─ eventType : AuditEventType  ← 94 subclasses
```

- **94 `AuditEventType` subclasses** under
  `gemma-core/src/main/java/.../auditAndSecurity/eventType/`
  covering everything from `DataAddedEvent` to
  `ExperimentalDesignUpdatedEvent` to `CurationNoteUpdateEvent`.
- **`AuditAdvice`** (the AspectJ aspect at
  `gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditAdvice.java`)
  fires on four DAO pointcuts (`creator() / updater() / saver() /
  deleter()` from `persistence/util/Pointcuts.java`) and emits a
  CREATE/UPDATE/DELETE event with a generic event type.
- **Specific event types are emitted by services** via
  `AuditTrailService.addUpdateEvent(entity, EventType.class, note,
  detail)` — currently called from ~50 service classes
  (`SingleCellExpressionExperimentAggregateServiceImpl`,
  `PreprocessorServiceImpl`, batch-info classes, …).

The Option 3 migration the user has decided on swaps those imperative
`addUpdateEvent(...)` calls for declarative
`@Audited(SomeEventType.class)` method annotations + an AOP aspect that
reads the annotation and synthesises the event. That migration is the
**enabling refactor** for everything in this doc.

### What a workflow ticket needs that `AuditEvent` doesn't have

| Need | AuditEvent today | Gap |
|---|---|---|
| Stable identity of the **work item** (so comments / state transitions reference it) | event.id | no — event IDs are per-event, not per-thread; "a ticket" today = an audit_id + target_id pair |
| **Parent / target entity** (EE, AD) | implicit via owning `Auditable.auditTrail` | ok |
| **State machine** (open/in_progress/resolved/cancelled) | not modelled; `eventType` proliferation is the workaround | needs first-class status |
| **Assignee** | none | needs `assignee : User` |
| **Comments** as their own thread | only `note + detail` strings on the event | needs ordered child rows |
| **Due date / SLA** | none | optional but desired |
| **Cross-entity relationships** ("blocks", "duplicate-of") | none | desired |
| **Subscope target** (a factor, a tag, an FV, not just "the EE") | none — audit always rolls up to the parent | `target_kind + target_id` (already on `AuditFinding`) |

---

## 3. Three storage shapes

### Option A — Ride on `AuditEvent`, derive ticket state from events

A `Ticket` is a thin pointer entity; everything substantive lives as
typed `AuditEvent`s on the parent `Auditable`'s existing audit trail.

**Schema delta**

```sql
CREATE TABLE TICKET (
  ID                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  PARENT_AUDITABLE_FK BIGINT NOT NULL,  -- FK into the Auditable's table
  PARENT_TYPE         VARCHAR(255) NOT NULL,  -- discriminator: 'ExpressionExperiment', 'ArrayDesign'
  KIND                VARCHAR(64) NOT NULL,   -- 'audit_finding', 'curation_task', 'pipeline_review', …
  TARGET_KIND         VARCHAR(32) NOT NULL,   -- 'experiment'|'factor'|'fv'|'tag'|'assignment'
  TARGET_ID           VARCHAR(255) NOT NULL DEFAULT '',
  CREATED_AT          DATETIME NOT NULL,
  CREATED_BY_FK       BIGINT NULL
);
-- New AuditEventType subclasses for the state machine:
--   TicketOpenedEvent, TicketAssignedEvent, TicketStatusChangedEvent,
--   TicketCommentedEvent, TicketClosedEvent
-- Each event carries the ticket FK in its detail/note column (or as
-- a new TICKET_FK column on AUDIT_EVENT).
ALTER TABLE AUDIT_EVENT ADD COLUMN TICKET_FK BIGINT NULL;
```

**`@Audited` annotation shape**

```java
@Audited(TicketOpenedEvent.class)
public Ticket openTicket(Auditable parent, TicketKind kind, String summary) { … }

@Audited(TicketStatusChangedEvent.class)
public Ticket transitionTicket(Long ticketId, TicketStatus newStatus) { … }
```

The aspect synthesises an `AuditEvent` with `ticketFk` set on each
transition; **current `status` is computed by reading the latest
status event** for that ticket.

**Pros**
- Minimal new schema (one stable-identity table + one nullable FK).
- The aspect is identical to the entity-level @Audited aspect — same
  annotation, same pointcut machinery.
- "What happened to this entity?" stays a single query against
  `AUDIT_EVENT`; tickets and direct-entity audits interleave
  naturally.
- Genuine event-sourcing — projection-friendly, audit trail is
  literally the source of truth.

**Cons**
- "All open tickets" requires aggregating events: `SELECT
  ticket_id, MAX(date) FROM audit_event WHERE event_type IN
  ('TicketStatusChangedEvent', 'TicketClosedEvent', …) GROUP BY
  ticket_id` and then filtering. Doable but every query needs a
  view or materialised projection to be fast.
- Comments are first-class only as a specific `TicketCommentedEvent`
  type — no separate ordered relation.
- Hard to add lightweight per-ticket fields later (priority, due
  date) without yet more event types.

**Migration risk:** Low. Pure additive — one new table, one nullable
column on `AUDIT_EVENT`, ~5 new `AuditEventType` subclasses. Flyway
migration is straightforward.

---

### Option B — Dedicated `Ticket` + `TicketEvent` parallel to `Auditable` + `AuditEvent`

Tickets get their own audit-trail-shaped table; entity audits stay
where they are.

**Schema delta**

```sql
CREATE TABLE TICKET (
  ID                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  PARENT_AUDITABLE_FK BIGINT NOT NULL,
  PARENT_TYPE         VARCHAR(255) NOT NULL,
  KIND                VARCHAR(64) NOT NULL,
  TARGET_KIND         VARCHAR(32) NOT NULL,
  TARGET_ID           VARCHAR(255) NOT NULL DEFAULT '',
  STATUS              VARCHAR(32) NOT NULL DEFAULT 'open',
  ASSIGNEE_FK         BIGINT NULL,
  SUMMARY             VARCHAR(512) NOT NULL DEFAULT '',
  CREATED_AT          DATETIME NOT NULL,
  CREATED_BY_FK       BIGINT NULL,
  DUE_AT              DATETIME NULL,
  CLOSED_AT           DATETIME NULL,
  CLOSED_BY_FK        BIGINT NULL,
  CLOSE_REASON        VARCHAR(64) NOT NULL DEFAULT ''
);
CREATE TABLE TICKET_EVENT (
  ID            BIGINT PRIMARY KEY AUTO_INCREMENT,
  TICKET_FK     BIGINT NOT NULL,
  EVENT_TYPE    VARCHAR(255) NOT NULL,  -- 'opened'|'assigned'|'status_changed'|'commented'|'closed'
  DATE          DATETIME NOT NULL,
  PERFORMER_FK  BIGINT NULL,
  NOTE          MEDIUMTEXT NULL,
  DETAIL        MEDIUMTEXT NULL,
  PAYLOAD_JSON  JSON NULL
);
CREATE INDEX IDX_TICKET_EVENT_TICKET ON TICKET_EVENT (TICKET_FK, DATE);
```

(Note the shape mirror of `AuditTrail` ⇄ `Ticket`, `AuditEvent` ⇄
`TicketEvent` — deliberate, so reasoning about one transfers to the
other.)

**`@Audited` annotation shape**

```java
@Audited(value = TicketOpenedEvent.class, target = AuditTarget.TICKET)
public Ticket openTicket(Auditable parent, TicketKind kind, …) { … }
```

The aspect routes to either `AuditTrailService.addEvent` or
`TicketEventService.addEvent` based on `target`.

**Pros**
- "All open tickets" / "all tickets assigned to X" are direct
  indexed queries. No materialised view needed.
- Ticket is first-class — schema can grow (`priority`, `labels`,
  `due_at` already wired in) without proliferating event types.
- Comments as `event_type='commented'` rows are naturally ordered
  and easily editable.
- Cleanly mirrors the agent-side mock SQLite (`audits` +
  `audit_dispositions`) — easy port of the existing Python schema.

**Cons**
- Parallel audit subsystem: two near-identical tables, two services,
  two query layers.
- The `@Audited` annotation has to disambiguate (`target=`
  parameter) — small loss of "one annotation, one meaning."
- "What happened to this experiment?" now needs a UNION over
  `AUDIT_EVENT` ∪ `TICKET_EVENT` to get the full picture.

**Migration risk:** Low–medium. Two new tables, one new service, no
schema disruption to the existing audit chain. The dual-write story
(close a ticket → also emit an entity-level `AuditEvent` saying "ticket
X closed") doubles the row count for any state transition that should
be visible on the EE's history page. That's the conscious cost.

---

### Option C — Unified audit + workflow events via discriminator

One `AUDIT_EVENT` table; new `TARGET_TYPE` / `TICKET_FK` columns
discriminate between "entity audit" and "ticket audit". A `Ticket` row
exists for stable identity but ticket lifecycle lives entirely on the
existing `AUDIT_EVENT` log with discriminator filtering.

**Schema delta**

```sql
ALTER TABLE AUDIT_EVENT
  ADD COLUMN TARGET_TYPE  VARCHAR(32) NOT NULL DEFAULT 'entity',  -- 'entity'|'ticket'
  ADD COLUMN TICKET_FK    BIGINT NULL,
  ADD COLUMN PAYLOAD_JSON JSON NULL;  -- structured payloads for new events

CREATE TABLE TICKET (
  ID                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  PARENT_AUDITABLE_FK BIGINT NOT NULL,
  PARENT_TYPE         VARCHAR(255) NOT NULL,
  KIND                VARCHAR(64) NOT NULL,
  TARGET_KIND         VARCHAR(32) NOT NULL,
  TARGET_ID           VARCHAR(255) NOT NULL DEFAULT '',
  CREATED_AT          DATETIME NOT NULL,
  CREATED_BY_FK       BIGINT NULL
);
```

Status is **always derived from latest event** (no mutable state
columns on `TICKET`). The `TARGET_TYPE` column lets every existing
"all events for this entity" query stay correct (filter
`TARGET_TYPE='entity'`); a "ticket history" query filters
`TARGET_TYPE='ticket' AND TICKET_FK=?`.

**`@Audited` annotation shape**

Same as Option A — single annotation; the aspect inspects whether
the method is in a `Ticket*Service` and sets `TARGET_TYPE='ticket'`
automatically.

**Pros**
- One annotation, one aspect, one query layer.
- Existing audit infrastructure (DAO, queries, history page) is
  reused — the audit history page just gets a "ticket events"
  filter.
- Genuine event-source unification — projection / Spring Modulith
  story is cleanest here.

**Cons**
- Schema is wider — every audit-event row now carries
  `TARGET_TYPE` + nullable `TICKET_FK` + nullable `PAYLOAD_JSON`,
  even for the 99% of rows that are entity audits.
- Conceptual conflation: "an event about the experiment" and "an
  event about a ticket about the experiment" share a row shape, so
  queries / UIs that don't expect tickets need to be discriminator-
  aware everywhere.
- The 94 existing `AuditEventType` subclasses can't sensibly become
  ticket events without a flag — the type hierarchy now has two
  axes (kind × scope) instead of one.
- High-volume audit events (`ProcessedExpressionDataVectorComputationEvent`,
  `BatchInformationEvent` from automated pipelines) coexist with
  low-volume curator ticket events in the same table — performance
  characteristics diverge, but they share the same indexes.

**Migration risk:** Medium. The new columns ALTER on `AUDIT_EVENT` is
the awkward step — production `AUDIT_EVENT` has historically been one
of the largest tables (millions of rows; check the prod count via the
port-forwarded read-only access per
`memory/reference_production_database.md`). An online ADD COLUMN
without DEFAULT is fast on MySQL 8+, but a default-backfill across
historical rows would lock for a while. Mitigation: nullable columns,
no backfill.

---

## 4. Recommendation: **Option B (dedicated `Ticket` + `TicketEvent`)**

Pick **Option B**. Justification:

1. **It mirrors the agent-side mock 1:1.** The Python repo already has
   `audits` + `audit_dispositions` as two tables, with the disposition
   log append-only and "latest per (audit_id, target_id) wins" as the
   read rule. Lifting that into Gemma as `Ticket` + `TicketEvent` is a
   straight port; the wire shapes in `AUDIT_FEATURE.md` lines 51–122
   already match. Option A would force the UI to denormalise from a
   wider event-log query; Option C would force the Python side to
   adopt Gemma's discriminator instead.
2. **Tickets are the kind of thing that grow fields.** Priority,
   labels, due date, parent-ticket-for-blocked-by — all natural
   adds once tickets are first-class. Options A and C make every
   such field either a new `AuditEventType` subclass (A) or a new
   nullable column on every audit row (C). Option B puts them on
   `TICKET` where they belong.
3. **The dual-table cost is bounded.** The "what happened to this
   experiment?" UNION-query objection is real but contained: it's
   one query path, in one service, with both tables pre-joined by
   the same `parent_auditable_fk`. Spring Data / Hibernate can
   express the union cleanly.

The cleanest pathway to event-sourcing / projection / Spring Modulith
is **B**, despite the surface-level appeal of Option C: B gives you
**two clean modules** (`audit` for entity events, `tickets` for work-
item events), each with its own bounded context and event stream,
which is exactly what Spring Modulith wants. C smushes them into one
module with a discriminator; A blurs the boundary entirely.

---

## 5. Phased plan

| Phase | Scope | Estimated sessions |
|---|---|---|
| **A — Complete `@Audited` migration** | Per the in-flight `AUDIT_SYSTEM_AUDIT.md`. Replace ~50 imperative `AuditTrailService.addUpdateEvent(...)` call sites with method-level `@Audited(EventType.class)`; introduce the `AuditedAspect` reading the annotation; keep current event-emission behaviour byte-identical. Tests: existing audit-event tests stay green. | 4–6 |
| **B — Introduce `Ticket` + `TicketEvent`** | New entities, Flyway migration, `TicketService` + `TicketEventService`, REST endpoints for read (GET `/datasets/{id}/tickets`, GET `/tickets/{id}`). Backfill one workflow end-to-end: **`ee-needs-review`** — auditor agent file findings → curator opens ticket → curator transitions through statuses → close ticket. No UI yet; verify via REST. | 5–7 |
| **C — Extend `@Audited` to workflow events** | Add `@Audited(value=…, target=AuditTarget.TICKET)` form; the same aspect routes to `TicketEventService` when `target=TICKET`. Migrate the new `TicketService` write methods to declarative annotations. Add new `AuditEventType` subclasses (`TicketOpenedEvent`, `TicketStatusChangedEvent`, etc.) — these live in the ticket subsystem but reuse the type hierarchy. | 3–4 |
| **D — REST endpoints for ticket queries + writes** | Full CRUD: `POST /tickets` (open), `PATCH /tickets/{id}` (assign / transition / comment), `POST /tickets/{id}/comments`, `POST /tickets/{id}/close`. Filter API: `GET /tickets?status=open&assignee=me&kind=audit_finding`. Backfill the candidate-promotion lifecycle (`AGENT_WRITEBACK_RECCE.md` state 1 ↔ 2 ↔ 3) as ticket events. | 4–6 |
| **E — Wire to `gemma-curation-ui`** | UI consumes the new REST endpoints. The existing `AuditSidebarPanel` and audit inbox shapes are already ticket-shaped — the migration is mostly point the fetcher at the real Gemma endpoint instead of the mock. Mock-side parity tests catch wire drift. | 3–4 |

**Total: ~19–27 sessions across A–E.** Phase A is the longest pole
(touching ~50 service classes); phase B is the load-bearing schema
work; phase E is mostly wiring.

---

## 6. Open questions

These came up while reading the sibling docs but are **not settled**;
the user should weigh in before any of these phases land commits.

### 6.1 Comment authorisation model

Who can comment on a ticket?

- **All logged-in users** — matches the "team-visible by default"
  decision for curation groups (`WORKFLOW_MANAGEMENT_HANDOFF.md`
  line 253: "Team-visible by default… No per-user ACL in the mock —
  curators are a small team and sharing queues is the primary use
  case").
- **Only assigned curators + admins** — tighter; matches the ACL
  pattern in the `gsec` module that the rest of Gemma uses for
  edits.
- **Only the auditor and the assignee** — narrowest; least
  conflict-prone but locks out review-by-peers.

The default for the first cut is probably "any user who can read the
parent EE" (so ACL inherits) — but the user should confirm.

### 6.2 Notifications / @mentions / email on state change

Out of scope for Phase B, but the data model needs to not preclude it.
Concrete questions:

- Do we want an `@curator-name` syntax in comment bodies that
  fans out to email / Slack?
- Should ticket assignment trigger an email by default? With opt-out
  per user, or per ticket?
- Is the existing Gemma mailer (which sends "your experiment was
  published" notifications) reusable, or does this want a new
  delivery layer?

Filed under "Phase D or later." The schema can defer these without
locking us in; the `TICKET` table just needs an `assignee_fk` column
which it already has.

### 6.3 Soft delete vs hard delete for ticket events

The existing `AuditEvent` table is **append-only** in practice — no
DELETE handler, no soft-delete column. Tickets ride that convention by
default. But:

- A curator who hits "close" then immediately realises they meant
  "reopen": do we want a UI affordance that **removes** the
  spurious close event, or does it stack a new "reopened" event on
  top? The sibling mock currently stacks (`AUDIT_DISPOSITION_EDIT_HANDOFF.md`
  ships disposition editing as "edit-in-place updates the latest
  row, not a new row" — but that's a comment edit, not a status
  edit).
- For comment editing specifically (which the sibling already
  ships): does the new ticket comment endpoint mirror the
  edit-in-place semantics (`AUDIT_DISPOSITION_EDIT_HANDOFF.md`), or
  go full append-only with no edits?

User should confirm. Recommendation: **append-only for status
transitions; in-place edit allowed only for free-text fields
(comment body, notes)** — but the wire schema needs the
`edited_at` / `edited_by` columns now if we're going to do that.

### 6.4 Relationship to existing `Curation*` services

A quick grep shows multiple existing services in the curation space:

- `CurationDetailsService` — the `curationDetails` write endpoint
  per `AGENT_WRITEBACK_RECCE.md`.
- `CuratableValueObject` / `Curatable` interface — what every
  ticket-able entity already implements.
- `CurationNoteUpdateEvent` (in the eventType hierarchy) — already
  fires when `curationNote` changes.

Are tickets:

- **A higher-level wrapper** around `curationDetails` writes (so a
  `needs_attention=true` flip auto-opens a ticket)?
- **A peer** — `curationDetails` stays as the "experiment state"
  surface, tickets are the "work item" surface, and they
  cross-reference?
- **A replacement** for `needsAttention` + `curationNote` in the
  long run, with `curationDetails` reduced to `troubled` + read-
  through ticket count?

Decision needed before Phase D so the REST surfaces don't bake in
the wrong relationship. Default recommendation: **peer**, with one
implicit ticket auto-opened when `needsAttention=true` is set
through the curation-details writeback.

---

## 7. Schema hand-back to the agent side

The agent-side mock SQLite already has the shape we want — modulo
naming. Recommended Gemma-side ↔ agent-side mapping under Option B:

| Gemma (Java/JPA) | Agent (Python/SQLite) | Notes |
|---|---|---|
| `Ticket` | `audits` row + (new) `tickets` row | `audit_id` becomes the FK; an audit is one kind of ticket (`kind='audit_finding'`) |
| `TicketEvent` (status_changed) | `audit_dispositions` row | append-only on both sides |
| `TicketEvent` (commented) | `audit_dispositions` row with comment payload | already exists in the agent mock |
| `Ticket.status` (computed) | `audit_status` ("closed" / "in_progress") | same derivation rule (latest disposition wins; closed iff `finalized_at` set) |
| `Ticket.kind` | new column on `audits` table or a `tickets` parent table | the agent side will need to add `kind` to distinguish `audit_finding` from `curation_task` etc. |

The wire contract in `AUDIT_FEATURE.md` (UI doc) already documents
`AuditReport` / `AuditFinding` / `AuditFindingDisposition`. Adding the
ticket layer means **renaming** at the wire level eventually — but
that's a `CROSS_REPO_COMPAT.md` matrix entry, not a breaking change for
the first roll-out (Phase E only).

---

## 8. Status & next steps

- **Cross-check with `AUDIT_SYSTEM_AUDIT.md`** when the sibling
  recce lands — particularly the `@Audited` aspect shape and the
  list of 94 event types (does any of them already overlap with
  workflow concepts? e.g. `CommentedEvent` exists today — does it
  become `TicketCommentedEvent`?).
- **Confirm Option B** with Paul before any Phase-B code lands.
- **Confirm the four open questions** in §6 before Phase D.
- **Size the prod `AUDIT_EVENT` table** via the read-only port-
  forward (`memory/reference_production_database.md`) so the
  migration plan is calibrated for actual row counts.

---

*Recce written 2026-05-19 by Claude (Opus 4.7 1M). No production code
modified.*

---

## User decisions (2026-05-19)

These supersede earlier speculation in this doc.

### Decision 1: CurationDetailsService becomes Tickets

Tickets REPLACE `CurationDetailsService` and its `needsAttention` / `troubled` / `curationNote` fields on `ExpressionExperiment`. The CurationDetails model is wrong because it presumes 1:1 with the experiment; tickets are top-level objects that may target a SET of entities.

**Concretely retired**:
- `CurationDetails` entity + its fields embedded in `ExpressionExperiment`.
- `CurationDetailsService.update*` callers — migrate to `TicketService.open / addComment / resolve`.
- The `needsAttention` / `troubled` boolean flags — replaced by "is there an OPEN ticket targeting this EE?".

### Decision 2: Ticket has 1..N targets, of mixed types

Two `TargetType` values initially: `EXPRESSION_EXPERIMENT`, `ARRAY_DESIGN`. Extensible without schema migration.

Schema:
```sql
CREATE TABLE ticket (
  id BIGINT PK AUTO_INCREMENT,
  type VARCHAR(64) NOT NULL,           -- TicketType enum (BATCH_INFO_NEEDED, REALIGNMENT_NEEDED, ...)
  state VARCHAR(32) NOT NULL,          -- TicketState enum, explicit, NOT derived
  priority VARCHAR(16) NOT NULL,
  due_date DATETIME NULL,
  title VARCHAR(255) NOT NULL,
  reporter_id BIGINT FK -> contact,
  assignee_id BIGINT FK -> contact NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE ticket_target (
  id BIGINT PK AUTO_INCREMENT,
  ticket_id BIGINT FK -> ticket NOT NULL,
  target_type VARCHAR(32) NOT NULL,    -- TargetType enum
  target_id BIGINT NOT NULL,           -- bare FK, NOT declared in JPA
  INDEX idx_ticket_target_lookup (target_type, target_id),
  UNIQUE (ticket_id, target_type, target_id)
);

CREATE TABLE ticket_event (
  id BIGINT PK AUTO_INCREMENT,
  ticket_id BIGINT FK -> ticket NOT NULL,
  actor_id BIGINT FK -> contact NOT NULL,
  occurred_at DATETIME NOT NULL,
  type VARCHAR(64) NOT NULL,           -- TicketEventType enum
  payload JSON NULL                    -- same shape as audit_event.payload
);
```

The `(target_type, target_id)` composite index supports the "open tickets targeting this EE" query without joins through `ticket_id`.

### Decision 3: Comment auth = any authenticated curator/admin role

Anonymous users cannot comment or change state. Authenticated users with role `curator` OR `admin` can:
- Add comments (`TicketEventType.COMMENTED`).
- Change state (`TicketEventType.STATE_CHANGED`).
- Assign (`TicketEventType.ASSIGNED`).

(Future refinement: per-event-type role restrictions if needed; e.g., only admin can DELETE a comment, only assignee can RESOLVE. Out of scope for first cut.)

### Decision 4: Comment edits — TBD

User flagged this is "another story". For now: append-only (mirrors the audit-event model and the prior `AGENT_WRITEBACK_RECCE.md` "events are receipts, not state" constraint). The sibling repo's in-place edit (`AUDIT_DISPOSITION_EDIT_HANDOFF.md`) is acknowledged but not adopted in Phase 1.

### Decision 5: Curator vs admin role split — out of scope

User flagged separate curator and admin roles are needed but "another story". This recce assumes a single "authenticated authorized user" check for all ticket operations in Phase 1. The role split can be a follow-on PR that adds per-event-type role restrictions.

