# HANDOFF: `PUT /datasets/{id}/design`

**Filed-by:** Paul Pavlidis (via curation-agents) — 2026-05-21
**Status:** request for Gemma-side endpoint; not implemented.
**Related PR / branch:** follow-on to PR #1656 (`api_fixes`, deployed
on staging-gemma `1364ff9286`, build 1.32.7-SNAPSHOT).

---

## Motivation

`POST /datasets/{id}/designPreflight` already exists on `api_fixes`
and returns a `DesignPreflightReport` — blockers, factors/FVs to
delete, DEA cascade preview, stale-anchor subsets. The mutating
verb that actually *applies* the validated design has never been
written. The `DesignPreflightReport` javadoc names this endpoint
as its destination.

Curator-side use case: an agent or curator proposes a revised
experimental design for an EE that's already loaded. Today they
can validate the proposal (preflight), but the only path to
landing it is the existing manual Curation Tools UI — which
defeats the point of the agent producing a structured proposal.
Without this endpoint, the curation-agents project cannot apply
any design write to real Gemma; the loop is broken at the last
step.

This is the #1 blocker called out in
`AGENT_WRITEBACK_RECCE.md` lines 39–40 ("Conspicuously absent")
and §"Open scoping questions" item 5 ("Surface to ship first").

---

## Required endpoint

### `PUT /datasets/{id}/design`

**Request body:** the same payload shape that
`POST /datasets/{id}/designPreflight` already accepts (full
proposed design — factors, factor values, sample-to-FV
assignments). Same schema both endpoints, so the client can
preflight a payload, get a clean report, and POST the *same*
payload to apply it.

**Response body** (sketch — finalize with maintainers):

```json
{
  "applied": true,
  "ee_id": 12345,
  "design": { /* full updated design, same shape as GET */ },
  "mutations": {
    "factors_added": 2,
    "factors_removed": 0,
    "fvs_added": 6,
    "fvs_removed": 1,
    "sample_assignments_updated": 48,
    "dea_results_invalidated": 1
  },
  "preflight_at_apply": { /* DesignPreflightReport actually evaluated at apply-time */ }
}
```

The `preflight_at_apply` block is the re-validation done as a
safety net (see "Failure modes" below) and is returned regardless
of success so the caller has a record of what was checked.

**HTTP semantics:**

- `200 OK` on successful apply.
- `409 Conflict` on preflight failure — body contains the
  `DesignPreflightReport` with blockers listed. Caller should
  surface to the curator.
- `404 Not Found` on unknown dataset id.
- `403 Forbidden` on auth-role mismatch (see below).
- `400 Bad Request` on malformed payload.

---

## Authorization

Curator-write role required. Specifically:

- A human curator (`ROLE_GROUP_USER` with the curation-curator
  granted authority — per `AUTH_FOR_SPA_RECCE.md`) MAY call.
- An agent identity calling this endpoint MUST be 403 unless the
  agent has been explicitly granted curator-write. Most agent
  runs are in "audit" or "proposal" mode (see
  `gemma-curation-agents/docs/THREE_MODES.md`) and SHOULD NOT
  bypass curator review by directly applying designs.
- In "audit" mode the agent's recommendations land as an
  `AgentProposal` attached to the EE; a curator then chooses to
  apply.
- Audit-mode agent calls to this PUT endpoint should 403 by
  default.

Recommendation: introduce a `curation:design:write` granted
authority and require it on the controller method. Initially
only the curator role gets it. A future "auto-apply low-risk
proposals" feature can grant it to a specific agent service
account explicitly.

---

## Audit-event hooks

Each successful apply emits exactly one new typed `AuditEvent`:

- **`DesignChangeEvent`** (new `AuditEventType` subclass under
  `gemma-core/.../auditAndSecurity/eventType/`) — fires on every
  applied design mutation.
- The event's `note` summarises the mutation counts (factors
  added/removed, FVs added/removed, assignments updated, DEA
  invalidations).
- The event's `detail` (or — once `AuditEventPayload` lands per
  `AUDIT_PHASE_C_RECCE.md` §4d — its `payload` column) carries
  the structured diff: which factor IDs were touched, which FV
  IDs were added/removed, the pre-apply `DesignPreflightReport`.
- If `AgentProposal` (per `AGENT_WRITEBACK_RECCE.md` §"The model")
  drove the apply, the event carries an FK to the
  `AgentProposal` row so the audit trail links proposal →
  decision → effect.

Per `AUDIT_PHASE_C_RECCE.md` patterns, the call site should be
declarative via `@Audited(DesignChangeEvent.class)` on the
service method that performs the apply — not an imperative
`auditTrailService.addUpdateEvent(...)`. If
`@Audited(EventType.class)` isn't yet wired for design changes,
flag this in the PR and migrate the call site to the declarative
form in the same change.

DEA invalidations triggered as a side effect of the design
change MUST also emit their existing `FailedDifferentialExpressionAnalysisEvent`
/ removal events on the same audit trail — preserve the current
DEA-cascade audit semantics; don't collapse them into the
`DesignChangeEvent`.

---

## Failure modes + idempotency

**Preflight at apply-time.** The endpoint MUST re-run the
preflight server-side at apply-time, even if the caller already
preflighted. If the apply-time preflight finds new blockers
(e.g. the EE was modified between preflight and apply), return
`409 Conflict` with the new `DesignPreflightReport` in the body
and emit NO audit event. The caller is expected to preflight
first as a UX optimization, but the apply endpoint is the
authoritative gate.

**Idempotency.** Recommend YES — re-submitting the same design
to an EE that already has that exact design is a no-op:

- Server computes the diff between current design and submitted
  design.
- If diff is empty, return `200 OK` with `applied: false` and
  `mutations` all zero.
- No `DesignChangeEvent` is emitted in the no-op case (audit-
  event de-dup).
- This makes the endpoint safe to retry on network errors and
  safe to call from "apply if not already applied" agent logic.

**Transactional semantics.** The full apply (factor delta + FV
delta + assignment update + DEA cascade) MUST be one DB
transaction. Partial application on error is not acceptable.

**Concurrent edits.** If two callers PUT designs concurrently,
last-writer-wins with the apply-time preflight catching most
real conflicts (the second writer's preflight will report
"factor X you're trying to delete doesn't exist"). Consider an
optimistic-lock ETag header on `GET /datasets/{id}/design` ↔ `If-Match`
on PUT for stricter semantics — open question, can ship without.

---

## Cross-references

- `AGENT_WRITEBACK_RECCE.md` (this repo) — origin recce; the
  "Conspicuously absent" table and §"Open scoping questions"
  item 5.
- `AUDIT_PHASE_C_RECCE.md` (this repo) — declarative-audit
  patterns the new write method should match.
- `AUDIT_AS_WORKFLOW_RECCE.md` (this repo) — ticket / workflow
  context for how design changes flow into the audit and ticket
  surfaces.
- `AUTH_FOR_SPA_RECCE.md` (this repo) — auth model the curator-
  write role plugs into.
- `~/Dev/gemma-curation-agents/docs/THREE_MODES.md` — agent
  mode A/B/C semantics; clarifies why audit-mode agent calls
  should 403.
- `~/Dev/gemma-curation-agents/gemma_curation_agents/shared/gemma.py`
  — Python client that will wrap this endpoint (sibling to the
  existing `update_curation_details` wrapper).
- `gemma-core/.../DesignPreflightReport.java` (on `api_fixes`)
  — the javadoc that names this endpoint as its destination.
- `gemma-rest/.../DatasetsWebService.java` (on `api_fixes`) —
  where the existing `designPreflight` handler lives; the
  apply handler belongs in the same controller.

---

## Open questions for maintainers

1. **Endpoint verb.** `PUT` per REST conventions for a full-
   replace of the design resource. Alternative: `POST /datasets/{id}/design/apply`
   if the team prefers explicit-verb endpoints. Curation-agents
   side has no preference; whichever lands first.
2. **Response payload size.** Full updated design + mutations +
   preflight could be sizeable on a 200-sample multi-factor EE.
   Acceptable, or should the response be just `{applied, ee_id,
   mutations}` and the caller GETs the design afresh?
3. **Optimistic-lock ETag.** Add `If-Match` support now or defer?
   Recommendation: defer; not blocking; add when concurrent-
   edit complaints surface.
4. **DEA cascade scope.** Preflight already returns a DEA
   cascade preview. Apply behaviour: invalidate the DEAs
   eagerly (current preflight assumption), or background-queue
   the invalidation via the existing task system? Recommend
   eager invalidation inside the same transaction; deferred
   re-run via the existing `POST /datasets/{id}/tasks/differential`.
5. **`AgentProposal` linkage.** Does the PUT body accept an
   optional `agent_proposal_id` field that the server links into
   the emitted `DesignChangeEvent`? Useful for the audit trail
   ("this design change was driven by agent proposal #4321")
   without requiring the agent to round-trip through a separate
   `attach proposal` call.

---

## Acceptance criteria

This endpoint is "done" when:

- [ ] `PUT /datasets/{id}/design` accepts the preflight payload
      schema and returns the response shape above.
- [ ] A `DesignChangeEvent` `AuditEventType` subclass exists and
      is emitted on every successful apply.
- [ ] The service method is annotated `@Audited(DesignChangeEvent.class)`
      (per Phase-C patterns) rather than calling
      `auditTrailService.addUpdateEvent` imperatively.
- [ ] `403` on agent-role callers without explicit
      `curation:design:write` authority; `200` for curator role.
- [ ] `409` on apply-time preflight failure with the
      `DesignPreflightReport` in the body.
- [ ] Idempotent: PUTting the same design twice produces one
      `DesignChangeEvent`, not two; second call returns
      `applied: false`.
- [ ] Transactional: any error in factor/FV/assignment/DEA
      handling rolls back the whole apply.
- [ ] Integration test exercises preflight → apply → re-apply
      (no-op) → conflicting concurrent PUT on the same EE.
- [ ] `gemma-rest` OpenAPI spec updated; sibling `shared/gemma.py`
      Python client wraps the endpoint as `update_dataset_design(ee_id, design_payload)`.
