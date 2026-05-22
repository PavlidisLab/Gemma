# STATUS: Workflow-state storage (the 8-state lifecycle)

**Branch:** `feat-workflow-state-storage`
**Baseline:** `e691500807`
**Date:** 2026-05-21

Companion file to `HANDOFF_WORKFLOW_STATE_STORAGE.md`. Records the
implementation slice that this branch lands and the choices on the
handoff's Open Questions.

## Commits

| SHA | Summary |
|---|---|
| `fa67382713` | `WorkflowState` enum + `INVESTIGATION.WORKFLOW_STATE` column + `WorkflowStateChangedEvent` audit type. Flyway: mysql V10, h2 V12. |
| `1e5b60af14` | `WorkflowService` interface + `WorkflowServiceImpl` with `@AuditedConditional` emission on `advance`; `DisallowedWorkflowTransitionException`; `WorkflowTransition` + `WorkflowQueueEntry` DTOs. |
| `01faf0fced` | `WorkflowWebService` (3 endpoints) + unit tests: `WorkflowServiceTest` (8 cases), `WorkflowWebServiceTest` (12 cases). |

Tip SHA: `01faf0fced`. Validation bar:
`mvn -pl gemma-core,gemma-rest compile test-compile -q` clean;
focused tests `WorkflowServiceTest` 8/8 + `WorkflowWebServiceTest` 12/12 green.

## Flyway versions used

- MySQL: `V10__investigation_workflow_state.sql`
- H2:    `V12__investigation_workflow_state.sql`

## Acceptance-criteria checklist (verbatim from the handoff)

- [x] `WORKFLOW_STATE` column exists on `INVESTIGATION` with the eight-state enum.
- [ ] Existing production EEs are backfilled per the open-question §2 mapping. **DEFERRED** — the column defaults to `'Loaded'` on every existing row; the curator-approved refinement runs in a follow-on migration once Paul signs off on the troubled / needsAttention / public / unprocessed mapping.
- [x] `GET /datasets/{id}/workflow` returns current state + history.
- [x] `PUT /datasets/{id}/workflow` advances state; enforces the state-machine; emits `WorkflowStateChangedEvent` declaratively via `@AuditedConditional(...)` (the conditional variant rather than `@Audited` because the handoff requires NO event on idempotent no-ops; the SpEL predicate `#result.previousState != #result.currentState` suppresses emission).
- [x] `GET /workflow/queue?state=...` returns datasets in a state with optional `assignee`, `since`, pagination.
- [x] Idempotent: PUTting current state is a no-op; no event.
- [x] `409` on disallowed transitions; body lists allowed next-states.
- [x] Auth: GETs are unauthenticated for the workflow surface (the ACL on the underlying EE still applies via the load); PUT requires `GROUP_CURATOR` or `GROUP_ADMIN`. Public → Curate additionally requires admin + non-empty reason (Open Question 5 enforced).
- [ ] Integration test exercising the full lifecycle (`Discovery → ... → Public`) on a fixture dataset. **DEFERRED** — the orchestrator owns this; the focused unit tests assert the state-machine sequence (`WorkflowServiceTest.advance_fullLifecycleSequence`).
- [ ] `gemma-rest` OpenAPI spec updated. **PROVIDED INLINE** — the OpenAPI annotations are present on each endpoint (`@Operation`, `@ApiResponse`, `@Parameter`); no manual `openapi.yaml` to edit (the spec is generated from the annotations).
- [ ] Python client wrappers in `shared/gemma.py`. **OUT OF SCOPE** (sibling repo, not this branch).
- [x] No regression in any existing audit-event consumer. The new `WorkflowStateChangedEvent` is just another `AUDIT_EVENT_TYPE` discriminator; nothing else changes shape.

## Open-question decisions

1. **Per-transition role granularity.** Permissive first-cut. The PUT
   endpoint is gated by `GROUP_CURATOR or GROUP_ADMIN`. Per-transition
   policy can be a follow-on PR once agent vs. curator responsibilities
   settle.
2. **`WORKFLOW_STATE` default for existing EEs.** All existing rows
   default to `'Loaded'`. The curator-approved refinement mapping
   (troubled / needsAttention / public / unprocessed → eight-state) is
   explicitly deferred to a follow-on migration that Paul signs off on.
   The current migration is reversible: a follow-on can `UPDATE
   INVESTIGATION SET WORKFLOW_STATE = ...` in bulk per the agreed
   mapping.
3. **Separate `WORKFLOW` table vs. column on `INVESTIGATION`.** Column
   on `INVESTIGATION`, as the handoff recommended. Queue queries are an
   indexed equality lookup on a single column.
4. **Recuration loop bookkeeping.** Nothing special needed. The
   audit-event stream gives full history; `WorkflowServiceImpl.getHistory`
   filters `WorkflowStateChangedEvent` rows in chronological order, so a
   dataset that visits `Curate` twice (initial + post-Audit) has two
   `Curate` entries in the response history list.
5. **Public state immutability.** Enforced. `Public → Curate` requires
   admin role AND a non-empty `reason`; both checks happen in
   `WorkflowWebService.advanceDatasetWorkflow` before
   `workflowService.advance` is invoked. 400 if reason missing/empty;
   403 if caller is not admin.

## Scope guardrails honored

- No `Ticket` / `TicketEvent` storage built (separate
  `AUDIT_AS_WORKFLOW_RECCE.md` Phase B-3; the `Ticket` layer already
  landed via the sibling `TicketsWebService` work).
- No `PreboardedExperiment` subclass built. The REST + service
  layer accommodates it (`dataset_type` is a string field, not an
  enum; the queue HQL has a TODO marker for the UNION).
- No curator-approved backfill mapping; the migration defaults
  everything to `'Loaded'`.

## Known integration TODOs

These are tagged `// TODO(ticket-integration)` or
`// TODO(preboarded-integration)` in the source:

- `WorkflowQueueEntry.currentAssignee` and `ticketCountOpen` are
  returned as `null` / `0`. The join over the open-ticket-per-dataset
  projection lands when the Ticket-layer queue contract is settled.
  The `?assignee=` query parameter is honored literally (returns
  empty) rather than silently ignored.
- `?dataset_type=preboarded_experiment` returns empty until the
  `PreboardedExperiment` subclass lands.
- The queue HQL is `from ExpressionExperiment` (subclass-specific);
  the polymorphic version `from Investigation` would naturally pick
  up the future `PreboardedExperiment` subclass.

## What downstream callers can expect

- Anonymous reads of `GET /datasets/{id}/workflow` are allowed at the
  WebService layer (no `@PreAuthorize`); ACL on the underlying
  `ExpressionExperiment.load` is what enforces visibility.
- Anonymous reads of `GET /workflow/queue` are allowed at the
  WebService layer (no `@PreAuthorize`); the curator triage view is
  open. To tighten later, add `@PreAuthorize("hasAuthority('GROUP_CURATOR')")`
  on `getWorkflowQueue`.
- PUT requires curator or admin role.
- The audit-event NOTE is structured: `"Workflow PREV -> TARGET[: reason][ (ticket=N)]"`.
- The audit-event id is NOT returned in the transition response
  (`auditEventId` is `null`); the aspect appends the row after the
  method returns, so the value isn't reachable from the method body.
  Callers needing it can do a one-row lookup on
  `GET /datasets/{id}/workflow` history and take the latest entry.
