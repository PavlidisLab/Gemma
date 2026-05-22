# HANDOFF: Workflow-state storage (the 8-state lifecycle)

**Filed-by:** Paul Pavlidis (via curation-agents) — 2026-05-21
**Status:** request for Gemma-side persistence and endpoints;
not implemented.

---

## Motivation

The curation UI (`~/Dev/gemma-curation-ui/apps/curation/WORKFLOW_MANAGEMENT.md`)
defines an explicit eight-state experiment lifecycle:

> **Discovery → Candidate → Preboarding → Loaded → Curate → Process →
> Audit → Public**

with a **recuration loop** Audit → Curate (per
`AUDIT_AS_WORKFLOW_RECCE.md` §1.1).

Today this state lives entirely in the **agents-side SQLite mock**
(`~/Dev/gemma-curation-agents/.../curation.sqlite`). Production
needs a Gemma-side store so that:

- The curator's view of "what's where" is the same view the
  agents and the loader pipeline see.
- The lifecycle survives an agent-server restart.
- Curator triage queues ("everything in `Audit` state, sorted by
  age") are first-class queries against the database of record.
- The state-machine transitions emit audit events that join the
  rest of the EE's audit trail.

`AUDIT_AS_WORKFLOW_RECCE.md` already worked the long-form design
question of how this storage relates to the existing
`AUDIT_EVENT` table — its recommended answer is **Option B
(dedicated `Ticket` + `TicketEvent` parallel to `Auditable` +
`AuditEvent`)** and Paul's user decisions in §"User decisions
(2026-05-19)" formalize that direction (including replacing
`CurationDetailsService` outright). The workflow-state question
in this handoff is narrower: where does the eight-state value
itself live, and how do consumers query / mutate it?

This handoff *does not* attempt to re-litigate the
`AUDIT_AS_WORKFLOW_RECCE.md` decision. It asks specifically for
the REST surface that exposes the workflow state to the agent and
the UI.

---

## Required endpoints

### `GET /datasets/{id}/workflow`

Return the current workflow state for a dataset (an EE OR a
`PreboardingExperiment` per
`HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md`), plus the full
transition history.

**Response:**

```json
{
  "dataset_id": 12345,
  "dataset_type": "expression_experiment",
  "current_state": "Curate",
  "entered_current_state_at": "2026-05-19T14:33:01Z",
  "history": [
    { "state": "Discovery", "entered_at": "...", "actor": "agent:gemini-loader", "reason": null },
    { "state": "Candidate",  "entered_at": "...", "actor": "curator:alice",       "reason": "Triage approved" },
    { "state": "Preboarding",   "entered_at": "...", "actor": "agent:proposer-v0.8", "reason": null },
    { "state": "Loaded",     "entered_at": "...", "actor": "system:geo-loader",   "reason": null },
    { "state": "Curate",     "entered_at": "...", "actor": "curator:alice",       "reason": "Beginning curation" }
  ]
}
```

For preboarding, `dataset_id` is the preboarding id and `dataset_type`
is `"preboarding_experiment"`.

### `PUT /datasets/{id}/workflow`

Advance the dataset to a new state (or re-set to a previous state
in the recuration-loop case).

**Request:**

```json
{
  "target_state": "Audit",
  "reason": "Auditor flagged organism part discrepancy",
  "ticket_id": 9001  // optional; FK into the ticket store from AUDIT_AS_WORKFLOW_RECCE.md
}
```

**Response:** `200 OK`

```json
{
  "dataset_id": 12345,
  "previous_state": "Curate",
  "current_state": "Audit",
  "entered_current_state_at": "...",
  "audit_event_id": 7777
}
```

`409 Conflict` if the transition is not allowed by the state
machine (e.g. `Discovery → Public` skipping states). The body
includes the allowed next-states list.

`404 Not Found` if the dataset id is unknown.

`403 Forbidden` per the auth model below.

### `GET /workflow/queue?state=Audit&assignee=...&limit=50`

List datasets currently in a given state. The curator worklist
view — "show me everything that's waiting in Audit."

**Query params:**

- `state` (required): one of `Discovery`, `Candidate`,
  `Preboarding`, `Loaded`, `Curate`, `Process`, `Audit`, `Public`.
- `dataset_type` (optional): filter to `expression_experiment` or
  `preboarding_experiment`. Default: both.
- `assignee` (optional): user login; filter to datasets where the
  current open ticket (per
  `AUDIT_AS_WORKFLOW_RECCE.md`) is assigned to this user.
- `since` (optional): ISO-8601 timestamp; filter to datasets that
  entered the state on or after this time.
- `limit` / `offset` for pagination.

**Response:**

```json
{
  "state": "Audit",
  "count": 17,
  "datasets": [
    {
      "dataset_id": 12345,
      "dataset_type": "expression_experiment",
      "accession": "GSE12345",
      "entered_current_state_at": "...",
      "current_assignee": "alice",
      "ticket_count_open": 2
    },
    ...
  ]
}
```

### State-machine reference

Allowed transitions (lifted from
`gemma-curation-ui/apps/curation/WORKFLOW_MANAGEMENT.md` lines
21–40):

```
Discovery   → Candidate, Preboarding
Candidate   → Preboarding, Discovery (rejection back-flow)
Preboarding    → Loaded, Candidate (re-triage)
Loaded      → Curate
Curate      → Process, Audit
Process     → Audit, Curate
Audit       → Curate (recuration loop), Public
Public      → Curate (post-publication issue; rare)
```

The server enforces these transitions. The endpoint may also
need a "force" admin override for state corrections; defer until
asked for (out of scope for first cut).

---

## Authorization

- **`workflow:read`** — GET endpoints. Any authenticated user
  with read access to the parent dataset.
- **`workflow:advance`** — `PUT /datasets/{id}/workflow`. Curator
  role required for most transitions. Specific automated
  transitions (`Discovery → Candidate` for example) can be
  driven by agent service accounts; those need a more granular
  per-transition policy.

Recommendation: implement the basic `workflow:advance` authority
gated on curator role for first cut. The per-transition
granularity can be a follow-on PR once we see which transitions
the agents actually need to drive.

Specific automated transitions the agent project will need
agent-role authority for:

- `Discovery → Candidate` (agent triage decides the GEO record
  is worth pursuing).
- `Preboarding → Loaded` (the GEO loader pipeline; already a system
  account, not strictly the "agent" identity).
- `Loaded → Curate` (agent's proposer auto-advances when it
  attaches its first proposal — debatable; could also be a
  curator-only transition).

Open question; see "Open questions" item 1.

---

## Audit-event hooks

Each `PUT /datasets/{id}/workflow` emits exactly one typed
`AuditEvent` on the dataset's audit trail:

- **`WorkflowStateChangedEvent`** (new `AuditEventType` subclass).
  Carries `previous_state`, `target_state`, `actor`, optional
  `reason`, optional `ticket_id` in note/detail (or `payload` once
  `AuditEventPayload` lands per `AUDIT_PHASE_C_RECCE.md` §4d).

Per `AUDIT_PHASE_C_RECCE.md` patterns, the service method
performing the transition should be annotated declaratively:

```java
@Audited(WorkflowStateChangedEvent.class,
         messageSpel = "'Workflow ' + #previousState + ' -> ' + #targetState + (#reason != null ? ': ' + #reason : '')")
public WorkflowTransition advance(Auditable dataset, WorkflowState targetState, String reason, Long ticketId) { ... }
```

If `AUDIT_AS_WORKFLOW_RECCE.md`'s `Ticket` + `TicketEvent` layer
lands first (per its §"Decision 1: CurationDetailsService becomes
Tickets"), the workflow-state advance also emits a `TicketEvent`
of type `STATE_CHANGED` on the relevant ticket if `ticket_id`
was provided. The two events on the two streams are paired —
workflow advance is an entity-level audit event; the ticket
state change is a workflow-domain event. See
`AUDIT_AS_WORKFLOW_RECCE.md` §"Decision 6: Tickets are Auditable"
for the two-streams rationale.

---

## Failure modes + idempotency

**Idempotent advance.** PUTting `target_state == current_state`
is a no-op:
- Server returns `200 OK` with `previous_state == current_state`
  and no new audit event.
- Safe to retry on network errors.

**Disallowed transition.** `409 Conflict` with the body listing
the allowed next-states. No state change, no audit event.

**Concurrent advance attempts.** Last-writer-wins. The second
caller sees the dataset already at the target state (if their
target matches) or hits the `409` disallowed-transition path
(if the previous-state assumption broke). Optimistic-lock via
`If-Match` is optional; can defer.

**Bulk advance.** Curators sometimes want "advance these 20 EEs
from `Audit` to `Public` together". The first-cut API is
per-dataset; a `PUT /workflow/bulk` taking a list of `{dataset_id,
target_state}` can be a follow-on. Out of scope for first cut.

---

## Storage shape — recommendation

Per `AUDIT_AS_WORKFLOW_RECCE.md` §"User decisions (2026-05-19)"
the dataset's workflow state should be a **first-class column**,
not derived. The recce already chose explicit over derived
(`AUDIT_AS_WORKFLOW_RECCE.md` §"Top design constraints" item 2:
"The state machine must be explicit. The old Gemma dataset
manager inferred state from timestamp existence — universally
regarded as bad").

Concretely, recommend:

```sql
-- On the Investigation table (or a sibling table FK'd to it):
ALTER TABLE INVESTIGATION
  ADD COLUMN WORKFLOW_STATE VARCHAR(32) NOT NULL DEFAULT 'Loaded',
  ADD COLUMN WORKFLOW_STATE_ENTERED_AT DATETIME NULL;

-- Plus the existing AuditEvent stream gives full history via
-- the WorkflowStateChangedEvent type — no separate history table
-- needed. (The "history" array in GET /workflow is derived from
-- filtering AUDIT_EVENT WHERE event_type='WorkflowStateChangedEvent'.)
```

Open question, see "Open questions" item 3: separate workflow
table vs. column on `INVESTIGATION`. The recce already starts
answering this in favour of a column.

This recommendation is *not* coupled to the `Ticket` /
`TicketEvent` storage from `AUDIT_AS_WORKFLOW_RECCE.md`; the
workflow column can land independently and tickets can layer in
later. But the two are designed to coexist: a workflow-state
advance commonly references a ticket id, and ticket state
changes commonly correspond to workflow-state advances.

---

## Cross-references

- `AUDIT_AS_WORKFLOW_RECCE.md` (this repo) — the long-form
  workflow design recce. §1 covers the UI's eight-state
  lifecycle; §"User decisions (2026-05-19)" is the decided
  schema for tickets that this handoff is consistent with.
- `AGENT_WRITEBACK_RECCE.md` (this repo) — §"Workflow states
  worth naming" covers the 1↔2↔3↔4 state collapse that maps
  onto the UI's eight-state lifecycle.
- `AUDIT_PHASE_C_RECCE.md` (this repo) — declarative-audit
  patterns for the `WorkflowStateChangedEvent` emission.
- `HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md` (this dir) —
  `PreboardingExperiment` lives in `Discovery` / `Candidate` /
  `Preboarding` states; promotion to EE advances to `Loaded`.
- `HANDOFF_PUT_DATASETS_DESIGN.md` (this dir) — applying a
  design change typically corresponds to time spent in `Curate`
  state.
- `HANDOFF_DATASETS_ANNOTATIONS_WRITE.md` (this dir) — likewise.
- `~/Dev/gemma-curation-ui/apps/curation/WORKFLOW_MANAGEMENT.md`
  — UI-side canonical state machine spec.
- `~/Dev/gemma-curation-agents/gemma_curation_agents/shared/gemma.py`
  — Python client. Will gain `get_workflow_state`,
  `advance_workflow_state`, `list_workflow_queue` wrappers.

---

## Open questions for maintainers

1. **Per-transition role granularity.** First cut: any curator
   can advance to any allowed next-state. Stricter: agents drive
   `Discovery → Candidate`, system drives `Preboarding → Loaded`,
   curators drive everything else. Recommendation: ship the
   permissive first-cut; tighten when curator + agent
   responsibilities are more settled.
2. **`WORKFLOW_STATE` default for existing EEs.** Production has
   tens of thousands of EEs in various manual-curation states.
   The migration needs to map current `curationDetails` flags
   (which `AUDIT_AS_WORKFLOW_RECCE.md` §"Decision 1" is retiring
   anyway) onto the eight-state enum. Suggested mapping:
   - `troubled=true` → `Audit`
   - `needsAttention=true` → `Curate`
   - public EE → `Public`
   - unpublic EE with completed processing → `Process` or
     `Audit` depending on whether any open ticket exists
   - unprocessed EE → `Loaded`
   Refine with a curator before the migration runs. Backfill
   does NOT emit audit events (one-time migration; not a
   workflow advance).
3. **Separate `WORKFLOW` table vs. column on `INVESTIGATION`.**
   Recommendation: column. `AUDIT_AS_WORKFLOW_RECCE.md` §3 Option
   B already split tickets into their own tables; the
   workflow-state value is small enough to ride on the
   `INVESTIGATION` row, and queries like "all datasets in state
   X" benefit from a single-table index.
4. **Recuration loop bookkeeping.** When a dataset goes `Audit →
   Curate` (recuration), should the original `Curate` history
   entry be preserved? Yes — the audit-event stream gives full
   history naturally; no special bookkeeping needed.
5. **Public state immutability.** A dataset in `Public` state
   *can* be moved back to `Curate` per the state machine, but the
   transition is rare and should probably require admin role +
   an explicit reason. Recommendation: enforce
   `Public → Curate` requires admin role + non-empty `reason`.

---

## Acceptance criteria

This endpoint set is "done" when:

- [ ] `WORKFLOW_STATE` column exists on `INVESTIGATION` (or
      sibling table) with the eight-state enum.
- [ ] Existing production EEs are backfilled per the
      open-question §2 mapping.
- [ ] `GET /datasets/{id}/workflow` returns current state +
      history (history derived from `AUDIT_EVENT` filtered by
      `WorkflowStateChangedEvent`).
- [ ] `PUT /datasets/{id}/workflow` advances state; enforces the
      state-machine; emits `WorkflowStateChangedEvent`
      declaratively via `@Audited(...)`.
- [ ] `GET /workflow/queue?state=...` returns datasets in a
      state with optional `assignee`, `since`, pagination.
- [ ] Idempotent: PUTting current state is a no-op; no event.
- [ ] `409` on disallowed transitions; body lists allowed
      next-states.
- [ ] Auth: `workflow:read` for GETs; `workflow:advance` (curator
      role) for PUT.
- [ ] Integration test exercises full lifecycle on a fixture
      dataset: `Discovery → Candidate → Preboarding → Loaded →
      Curate → Audit → Curate → Process → Audit → Public`.
- [ ] `gemma-rest` OpenAPI spec updated.
- [ ] Python client wrappers in `shared/gemma.py` (sibling
      repo).
- [ ] No regression in any existing audit-event consumer (the
      new `WorkflowStateChangedEvent` rows just appear in the
      stream alongside everything else).
