# Agent writeback — Gemma-side reconnaissance

**Filed:** 2026-05-18. Scope: what the curation-agents project needs
from Gemma to close the loop between agent-proposed curation and
the database of record. This is a recce, not a design spec — it
maps what's already shippable, what's missing, and where the
schema-level forks lie.

Sibling context:

- The curation-agents Python repo just shipped
  `feature/audit-writeback` (commit `032fcab` on
  `~/Dev/gemma-curation-agents`) which adds a REST client + sink
  abstraction for `PUT /datasets/{id}/curationDetails`. Mock-only
  by design — no real writes against staging or prod until
  staging stops sharing the production DB.
- PR #1656 (`api_fixes`, last touched 2026-05-15) on this repo is
  deployed on staging-gemma (`1364ff9286`, build 1.32.7-SNAPSHOT)
  and is what the Python client targets.

## Where the integration sits today

### Already landed on `api_fixes` (PR #1656)

| Endpoint | Use |
| --- | --- |
| `PUT  /datasets/{id}/curationDetails` | troubled / needsAttention / curationNote, with audit-event emission |
| `PUT  /datasets/{id}/permissions` | sharing |
| `PUT  /datasets/{id}/geeq` | GEEQ scores |
| `POST /datasets/{id}/designPreflight` | dry-run validation of a proposed design — returns blockers, factors/FVs to delete, DEA cascade preview, stale-anchor subsets |
| `POST /datasets/{id}/tasks/{preprocess,diagnostics,batchInfo,differential,redo/{id}}` | async pipeline dispatch |
| `DELETE /datasets/{id}/tasks/differential/{id}` | DEA removal |
| `GET  /tasks/{taskId}` | poll status (in-memory store, ~10 min retention) |
| `GET  /datasets/{id}/pipelineStatus`, `auditEvents` | observability |

### Conspicuously absent

| Gap | What it blocks |
| --- | --- |
| **`PUT /datasets/{id}/design`** | The actual mutating verb for design changes. `DesignPreflightReport`'s javadoc already names it as the destination; the handler doesn't exist yet. Without it, the agent can validate a proposal but can't apply it. |
| **Annotation writes** (`PUT /datasets/{id}/annotations`, …) | `AnnotationsWebService` is read-only. EE tags (organism part, disease, treatment) — the bulk of the agent's tag output — have no writeback path. |
| **Anything proposed-but-not-loaded** | All current write surfaces assume the `ExpressionExperiment` exists. Today the agent runs against GEO accessions that may or may not be in Gemma; the proposal has no home until somebody manually loads the experiment. |

## Workflow states worth naming

The integration spans four states. Today only state 3→4 has any
Gemma-side machinery; states 1+2 live entirely in the agents
repo's mock SQLite.

| State | Today | What's missing |
| --- | --- | --- |
| (1) Proposed, no Gemma row | Agent has skeleton; lives in mock curation server | No Gemma identity; can't surface in curator triage |
| (2) Skeleton in Gemma, no data | doesn't exist | A lightweight `Investigation` row holding the JSON-ified skeleton |
| (3) Data loaded, awaiting curation | `ExpressionExperiment` exists; agent re-runs against the loaded EE | Per-instance design / tag writeback (the missing PUT endpoints above) |
| (4) Curated | `ExpressionExperiment` fully curated | curationDetails writeback (✅ shipped on `api_fixes`) |

Skeleton-in-Gemma collapses 1↔2 into one row that exists from
proposal time. State 2 → 3 becomes a *promotion*: the data load
binds the skeleton's curatable artifacts (factors, FVs,
assignments, tags, audit trail) to the new EE.

## The model — `Investigation` subclass + JSON-blob proposal

**Decided (Paul, 2026-05-18):** subclass route. Gemma has to deal
with all the options, so the discriminator churn is the right
investment up front. The lighter `dataState`-flag alternative is
filed under "rejected — would push implicit filters into every
EE query forever" rather than expanded here.

### Shape

- `SkeletonInvestigation extends Investigation` — represents
  states 1+2 (proposed; not yet loaded). Sibling of
  `ExpressionExperiment` under `Investigation`. Holds accession,
  identifying metadata, ACLs, audit trail — everything that
  doesn't require a `BioAssay` to exist.
- `AgentProposal` — a first-class entity holding the JSON-ified
  skeleton payload the agent produces. Append-only; one row per
  agent run. Columns roughly `(investigation_fk, run_id,
  agent_version, model, ran_at, payload_json)`. The JSON column
  is whatever MySQL gives us cheapest (native JSON or TEXT — not
  decomposed into relational tables; the schema lives in the
  JSON itself and matches the agents repo's `Proposal` Pydantic
  model).
- `AuditEvent` of a new type (e.g. `AgentProposalEvent`)
  references the `AgentProposal` row by FK rather than carrying
  the JSON inline. Keeps audit-event payloads small + gives the
  proposal its own lifecycle independent of the event.

### Why JSON-blob over audit-event-attached

The skeleton row exists to hold the proposal, so the JSON belongs
on a row in its own right. Attaching JSON to an audit event
overloads event payload semantics ("events are receipts, not
state") and makes querying "what does the agent think now" awkward
when there are five runs. A dedicated `AgentProposal` entity gives
clean "live vs historical" semantics and trivially answers "what
changed between v1 and v3 of the agent run."

### Promotion semantics (state 2 → 3)

When data lands and a real `ExpressionExperiment` comes into
existence:

1. The `SkeletonInvestigation` row gets re-classified to
   `ExpressionExperiment` (the hard option — Hibernate class
   flip), or the EE is a new row that links back to the skeleton
   (the easier-on-Hibernate option, but breaks accession-stable
   URIs). Decision deferred — depends on what `INSERT`/`DELETE`-
   vs-`UPDATE`-discriminator costs us in this schema.
2. The most recent `AgentProposal` payload drives initial
   factor / FV / sample-assignment creation on the EE.
3. `AgentProposal` rows rebind from the skeleton to the EE.
   Audit events that referenced the proposal trail along.
4. The JSON stays as a frozen historical artifact next to the
   relational design — "what the agent saw at time T" vs "what
   the curator landed on."

### Open scoping questions

1. **Promotion mechanics**: same-row class flip (clean for
   downstream, costly in Hibernate) vs new-row + link (clean in
   Hibernate, FK rebind effort everywhere else). Grep audit-event
   FK consumers + accession-URI builders before deciding.
2. **`AgentProposal` mutability**: append-only with timestamps
   (recommended — honest history, easy "what changed" query) vs
   single mutable row (smaller table, lose history). I'd default
   to append-only.
3. **Where does promotion fire?**: explicit curator action vs
   automatic on data-load detection. Affects whether the JSON's
   `accepted=true` fields auto-materialise or wait for a manual
   confirm.
4. **Discriminator footprint**: a one-afternoon grep would size
   the churn. HQL `from Investigation` + DAO base queries +
   admin views are the obvious targets.
5. **Surface to ship first**: regardless of how the subclass
   lands, **`PUT /datasets/{id}/design`** + annotation writes
   unlock state-3 writeback (the loaded-but-not-curated case),
   which is the most-painful manual gap today.

## Open questions for Java-side scoping

1. **Promotion mechanics** — same-row class flip vs new-row + FK
   rebind. See "Promotion semantics" above.
2. **Endpoint roadmap** — appetite for `PUT
   /datasets/{id}/design` + annotation writes on the `api_fixes`
   branch, or a follow-up? The Python client is structured to
   accept either timeline.
3. **Staging DB cut** — when does staging stop sharing the prod
   DB? Gates when the agents repo flips from mock-only writes to
   real-staging smoke testing.
4. **Where does state 1 live?** — does the GEO-only proposed
   state get a `SkeletonInvestigation` row immediately, or only
   when a curator decides to triage it (so state 1 stays in the
   agents repo until then)? Depends on whether curators triage
   *which to load* vs. *what's already loaded*.

## Pointers

- Python writeback client + sink abstraction:
  `gemma-curation-agents` branch `feature/audit-writeback`,
  commit `032fcab`. Files of interest:
  `gemma_curation_agents/agents/audit/disposition_sink.py`,
  `gemma_curation_agents/shared/gemma.py`
  (`update_curation_details` / `get_curation_details`),
  `scripts/writeback_curation_details.py` (one-shot CLI with the
  prod-URL guard).
- The endpoints this recce references:
  `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java`
  on `origin/api_fixes`.
- `DesignPreflightReport`:
  `gemma-core/src/main/java/ubic/gemma/model/expression/experiment/DesignPreflightReport.java`
  on `origin/api_fixes`. Its javadoc is the spec for the
  PUT /design handler that hasn't been written yet.
