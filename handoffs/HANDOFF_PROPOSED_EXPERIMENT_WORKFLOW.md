# HANDOFF: Proposed-but-not-loaded experiment surface (`SkeletonInvestigation` + `AgentProposal`)

**Filed-by:** Paul Pavlidis (via curation-agents) — 2026-05-21
**Status:** request for Gemma-side endpoints and persistence;
not implemented. Schema shape decided 2026-05-18 (see
"Reference" below); endpoints not yet specced.

---

## Motivation

The curation-agents runner targets GEO accessions. Many of those
accessions are not yet loaded into Gemma when the agent runs. All
existing Gemma write surfaces (curationDetails, the design write
in `HANDOFF_PUT_DATASETS_DESIGN.md`, the annotation writes in
`HANDOFF_DATASETS_ANNOTATIONS_WRITE.md`) assume an
`ExpressionExperiment` row exists.

This blocks the agent's "Proposal" mode (per
`~/Dev/gemma-curation-agents/docs/THREE_MODES.md` Mode B: "fresh
skeleton — no EE in Gemma yet"). Today those proposals live in
the agents-side SQLite mock with no Gemma identity, no curator
triage surface, and no path to land on a loaded EE once the data
arrives. The lifecycle from "GEO accession spotted" → "loaded EE
with curation applied" has a missing middle.

This is the #3 blocker in `AGENT_WRITEBACK_RECCE.md` lines 42
("Anything proposed-but-not-loaded"), formally specced in
`AGENT_WRITEBACK_RECCE.md` §"The model" (Paul, 2026-05-18).

---

## Reference: the decided shape

Quoted from `AGENT_WRITEBACK_RECCE.md` §"The model — `Investigation`
subclass + JSON-blob proposal":

> **Decided (Paul, 2026-05-18):** subclass route. Gemma has to deal
> with all the options, so the discriminator churn is the right
> investment up front.
>
> - `SkeletonInvestigation extends Investigation` — represents
>   states 1+2 (proposed; not yet loaded). Sibling of
>   `ExpressionExperiment` under `Investigation`.
> - `AgentProposal` — a first-class entity holding the JSON-ified
>   skeleton payload the agent produces. Append-only; one row per
>   agent run. Columns roughly `(investigation_fk, run_id,
>   agent_version, model, ran_at, payload_json)`.
> - `AuditEvent` of a new type (`AgentProposalEvent`) references
>   the `AgentProposal` row by FK rather than carrying the JSON
>   inline.

Cross-reference: the agents-eval memory file
`skeleton_in_gemma_as_investigation.md` (2026-05-18) holds the
same decision in the agent project's notebook.

---

## Required endpoints

### `POST /skeletons`

Create a `SkeletonInvestigation` from a GEO accession.

**Request:**

```json
{
  "accession": "GSE12345",
  "source": "GEO",
  "identifying_metadata": {
    "title": "...",
    "summary": "...",
    "submitter": "...",
    "pubmed_id": "..."
  }
}
```

`source` defaults to `"GEO"`; future values may include
`"ArrayExpress"`, `"manual"`.

**Response:** `201 Created`

```json
{
  "skeleton_id": 9876,
  "accession": "GSE12345",
  "created_at": "...",
  "state": "proposed"
}
```

`409` if a `SkeletonInvestigation` OR an `ExpressionExperiment`
already exists with this accession. The 409 response should
include the existing entity's id and type so the caller can
switch tactics.

### `GET /skeletons/{id}`

Fetch a skeleton's current state, including its latest
`AgentProposal`.

**Response:**

```json
{
  "skeleton_id": 9876,
  "accession": "GSE12345",
  "state": "proposed",
  "created_at": "...",
  "identifying_metadata": { ... },
  "latest_proposal": {
    "proposal_id": 42,
    "run_id": "abc123",
    "agent_version": "0.8.0",
    "model": "claude-opus-4-7-1m",
    "ran_at": "...",
    "payload_json": { /* the agent's full proposal payload */ }
  },
  "proposal_count": 3,
  "audit_trail_url": "/skeletons/9876/auditEvents"
}
```

`GET /skeletons?accession=GSE12345` resolves accession → skeleton
id (the agent needs this when re-running against the same
accession to know whether to POST or attach a new proposal).

### `POST /skeletons/{id}/proposals`

Attach a new `AgentProposal` to a skeleton.

**Request:**

```json
{
  "run_id": "abc123",
  "agent_version": "0.8.0",
  "model": "claude-opus-4-7-1m",
  "payload_json": { /* full proposal — factors, FVs, tags, etc. */ }
}
```

**Response:** `201 Created`

```json
{
  "proposal_id": 42,
  "skeleton_id": 9876,
  "audit_event_id": 555
}
```

The endpoint emits an `AgentProposalEvent` audit event linked to
the new `AgentProposal` row.

`AgentProposal` is **append-only**. Re-running the agent against
the same accession creates a new proposal row; the previous one is
preserved as historical record.

### `POST /skeletons/{id}/promote`

Promote a skeleton to a loaded `ExpressionExperiment`. Called
when the data has been loaded and an `ExpressionExperiment` row
exists (typically as the final step of the GEO loader pipeline,
or by a curator who has manually loaded the data).

**Request:**

```json
{
  "ee_id": 12345,
  "apply_latest_proposal": true
}
```

`apply_latest_proposal` (default `false`) controls whether the
most recent `AgentProposal` payload immediately drives factor /
FV / sample-assignment / tag creation on the new EE, or whether
the EE starts clean and a curator applies the proposal
interactively.

**Response:** `200 OK`

```json
{
  "skeleton_id": 9876,
  "ee_id": 12345,
  "promoted_at": "...",
  "proposals_rebound": 3,
  "audit_events_rebound": 12,
  "applied_proposal_id": 42  // null if apply_latest_proposal=false
}
```

Promotion mechanics — open question, see "Open questions" item 1.
Concretely the call either:
- flips the `Investigation` discriminator on the skeleton row
  in-place (clean for downstream URIs, costly in Hibernate), or
- rebinds the `AgentProposal` rows and audit events to the
  separate `ExpressionExperiment` row (clean for Hibernate, more
  FK rebind work).

Either way, the post-promotion state is: one `ExpressionExperiment`
holding the curatable artifacts; the historical `AgentProposal`
rows accessible from it; the audit trail intact.

### `GET /skeletons?state=proposed&since=...`

List skeletons in a given state (curator triage view). Reuses
the `GET /workflow/queue` pattern from
`HANDOFF_WORKFLOW_STATE_STORAGE.md` if that storage lands first;
otherwise a per-resource filter on the skeleton collection.

---

## Authorization

Two distinct roles needed:

- **`skeleton:write`** — POST new skeletons, POST proposals.
  Granted to agent service accounts. Curator role also holds it
  (curators can create skeletons manually).
- **`skeleton:promote`** — POST `/skeletons/{id}/promote`. Curator
  role only. **Agents MUST NOT be able to promote** — promotion
  binds curatable artifacts to a real EE and is a curator
  decision, not an agent decision.

This is the explicit role-split called out in the original task
brief: "agent-role can POST skeletons + proposals; only
curator-role can promote."

Per `AUTH_FOR_SPA_RECCE.md` patterns: implement as two granted
authorities on the curator/agent role definitions. An agent
calling `/promote` with only `skeleton:write` gets `403`.

GET endpoints: read access for any authenticated user who can
read the parent corpus (matches existing EE read-access patterns).

---

## Audit-event hooks

New `AuditEventType` subclasses:

- **`SkeletonCreatedEvent`** — fires on `POST /skeletons`. Emitted
  against the new `SkeletonInvestigation`'s own audit trail
  (skeletons are `Auditable` — they inherit from `Investigation`).
- **`AgentProposalEvent`** — fires on `POST /skeletons/{id}/proposals`.
  Emitted against the skeleton's audit trail. Carries an FK to the
  new `AgentProposal` row (NOT the JSON payload inline — that's the
  decision in `AGENT_WRITEBACK_RECCE.md` §"Why JSON-blob over
  audit-event-attached").
- **`SkeletonPromotedEvent`** — fires on `POST /skeletons/{id}/promote`.
  Emitted against the (possibly rebound) audit trail. Carries the
  promoted EE id and the applied proposal id (if any).

All three should use the declarative `@Audited(EventType.class)`
form per `AUDIT_PHASE_C_RECCE.md` patterns, with the SpEL `message`
attribute carrying the accession / proposal id / EE id as
appropriate.

If `HANDOFF_WORKFLOW_STATE_STORAGE.md` lands together with this,
the promotion event also advances the workflow state (e.g.
`Skeleton → Loaded`).

---

## Failure modes + idempotency

**`POST /skeletons` with existing accession.** `409 Conflict` with
the existing entity's id and type (`skeleton` or
`expression_experiment`). Caller can decide whether to attach a
new proposal (if skeleton) or write against the EE directly (if
already loaded).

**`POST /skeletons/{id}/proposals` retry safety.** Idempotency
key: the `run_id` field. If a proposal with the same
`(skeleton_id, run_id)` already exists, return `200 OK` with the
existing proposal id rather than `201 Created` with a new one. No
duplicate `AgentProposalEvent`s. This matters because the
agents-side runner retries on transient infrastructure errors;
re-uploading the same `run_id`'s payload should be a no-op.

**`POST /skeletons/{id}/promote` when EE already loaded
elsewhere.** If `ee_id` in the body points to an EE that's
already promoted from a different skeleton, return `409 Conflict`.
A given EE should not be the promotion target of more than one
skeleton.

**`POST /skeletons/{id}/promote` with `apply_latest_proposal=true`
and the proposal payload is structurally invalid for the loaded
EE** (e.g. proposed factors reference sample IDs that don't exist
in the loaded data): roll back the apply, leave the skeleton
unpromoted, return `409` with a structured error report. Caller
should re-run the agent against the loaded EE in Mode A (audit)
and apply through the curator review path.

**Concurrent promote calls.** Last-writer-wins is acceptable; the
second call sees the skeleton already promoted and returns the
existing promotion record. Optimistic-lock via `If-Match` ETag is
optional.

---

## Cross-references

- `AGENT_WRITEBACK_RECCE.md` (this repo) — origin recce; §"The
  model" is the decided shape; §"Promotion semantics" describes
  the state-2 → state-3 transition.
- `AUDIT_PHASE_C_RECCE.md` (this repo) — declarative-audit
  patterns the new write endpoints should match.
- `AUDIT_AS_WORKFLOW_RECCE.md` (this repo) — ticket / workflow
  context. The "Candidate" → "Skeleton" → "Loaded" → "Curate"
  states from the UI's `WORKFLOW_MANAGEMENT.md` are tracked via
  the storage in `HANDOFF_WORKFLOW_STATE_STORAGE.md`; promotion
  advances the state.
- `HANDOFF_WORKFLOW_STATE_STORAGE.md` (this dir) — sibling
  handoff; the per-skeleton workflow state lives in that store.
- `HANDOFF_PUT_DATASETS_DESIGN.md` (this dir) — what the
  promoted EE's design endpoint accepts when
  `apply_latest_proposal=true` triggers it server-side.
- `HANDOFF_DATASETS_ANNOTATIONS_WRITE.md` (this dir) — what the
  promoted EE's annotation endpoint accepts likewise.
- `~/Dev/gemma-curation-agents/docs/THREE_MODES.md` — Mode A
  (audit), Mode B (proposal — this is the case the skeleton
  surface enables), Mode C (calibration).
- Agents-eval memory: `skeleton_in_gemma_as_investigation.md`
  (Paul 2026-05-18 decision).
- `~/Dev/gemma-curation-agents/gemma_curation_agents/shared/gemma.py`
  — Python client. Will gain `create_skeleton`,
  `get_skeleton`, `attach_proposal`, `promote_skeleton`
  wrappers.

---

## Open questions for maintainers

1. **Promotion mechanics: in-place class flip vs. new-row + FK rebind.**
   The `AGENT_WRITEBACK_RECCE.md` §"Promotion semantics" open
   question is the load-bearing one — same-row class flip (clean for
   downstream URIs, costly in Hibernate) vs. new EE row that links
   back to the skeleton (clean in Hibernate, breaks accession-stable
   URIs). The endpoint contract above is agnostic between the two,
   but the implementation has to pick.
2. **`AgentProposal` payload size limit.** Empirical: agent proposals
   for a 200-sample multi-factor EE are O(50–200 KB) of JSON. MySQL
   `JSON` column or `LONGTEXT`? Either is fine.
3. **Where does state 1 live?** Per
   `AGENT_WRITEBACK_RECCE.md` §"Open questions" item 4: does the
   GEO-only-not-yet-triaged state get a `SkeletonInvestigation`
   immediately on agent run, or only when a curator triages it (so
   state 1 stays in the agents-side mock until then)? Recommendation:
   create the skeleton on agent run (state 1 + 2 collapsed) — gives
   curators a single triage surface, doesn't blur the boundary
   between agent-only state and Gemma state.
4. **ACL on skeleton rows.** Skeletons inherit `Investigation`'s
   ACL machinery. Default ACL: same as the EE that the skeleton
   will become? Or a permissive "team-visible" default with the
   tighter ACL applied only at promotion? Curation-agents side
   has no preference; whichever is easier given the existing
   ACL aspect (see `ACL_ENTRY_VOTER_MIGRATION.md`).
5. **Auto-promote on data-load detection.** When the GEO loader
   pipeline imports data for an accession that already has a
   `SkeletonInvestigation`, should it auto-call `/promote`, or
   wait for an explicit curator action? Recommendation: auto-
   promote with `apply_latest_proposal=false` so the curator
   still reviews the proposal before it lands. This matches the
   conservative default of "agents propose, curators apply."

---

## Acceptance criteria

This endpoint set is "done" when:

- [ ] `SkeletonInvestigation` JPA entity extends `Investigation`;
      Flyway migration adds discriminator value + any
      skeleton-specific columns.
- [ ] `AgentProposal` JPA entity exists with the column shape from
      `AGENT_WRITEBACK_RECCE.md` §"The model".
- [ ] `POST /skeletons` creates a skeleton; idempotency on
      accession; emits `SkeletonCreatedEvent`.
- [ ] `GET /skeletons/{id}` returns skeleton + latest proposal.
- [ ] `GET /skeletons?accession=...` resolves accession → skeleton.
- [ ] `POST /skeletons/{id}/proposals` appends a proposal;
      idempotent on `run_id`; emits `AgentProposalEvent`.
- [ ] `POST /skeletons/{id}/promote` promotes to EE; emits
      `SkeletonPromotedEvent`; optional `apply_latest_proposal`
      drives initial factor/FV/tag creation.
- [ ] Auth: agent role can POST skeletons + proposals; only
      curator role can promote.
- [ ] All write methods use `@Audited(...)` declarative form.
- [ ] Audit-event payloads carry FKs to `AgentProposal` rows, not
      inlined JSON.
- [ ] Integration test exercises: create skeleton → attach
      proposal → re-attach same `run_id` (no-op) → attach new
      `run_id` → promote with `apply_latest_proposal=true` →
      audit trail intact across promotion.
- [ ] `gemma-rest` OpenAPI spec updated.
- [ ] Python client wrappers in `shared/gemma.py` (sibling repo).
