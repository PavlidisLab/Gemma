# STATUS: Dataset annotation write endpoints

**Date:** 2026-05-21
**Branch:** `feat-datasets-annotations-write`
**Spec:** `handoffs/HANDOFF_DATASETS_ANNOTATIONS_WRITE.md`
**Sibling status doc:** `handoffs/STATUS_PUT_DATASETS_DESIGN.md` (auth + agent-proposal decisions reused here)

## What landed

Three new endpoints on `AnnotationsWebService` for curator/agent annotation writeback:

| Method | Path | Audit event | Notes |
|---|---|---|---|
| POST | `/annotations/datasets/{dataset}/annotations` | `TagAddedEvent` (one per call) | 409 on duplicate `(categoryUri, valueUri)`; returns 201 + the persisted `AnnotationValueObject` |
| DELETE | `/annotations/datasets/{dataset}/annotations/{annotationId}` | `TagRemovedEvent` (one per call) | 404 if the id isn't on the dataset's characteristic set; returns 204 on success |
| PUT | `/annotations/datasets/{dataset}/annotations` | One `TagAddedEvent` per add + one `TagRemovedEvent` per remove | Idempotent (empty diff = 200 OK + empty added/removed/audit_event_ids); returns `AnnotationReplaceReport` (eeId, before, after, added[], removed[], unchanged, audit_event_ids[], unresolved_uris[]) |

Service layer (`gemma-core`):

- `ExpressionExperimentService.addAnnotation(ee, vc)` — new method, `@Audited(TagAddedEvent.class)` on the facade impl; delegates to `writeService.addCharacteristic` after a duplicate-by-(category, value) check.
- `ExpressionExperimentService.removeAnnotation(ee, annotationId)` — new method, `@AuditedConditional(TagRemovedEvent.class, when="#result != null")`; delegates to `writeService.removeCharacteristics(singleton)`; returns the removed `Characteristic` or `null`.
- The existing `addCharacteristic` / `removeCharacteristics` / `updateAnnotations` methods are unchanged — gemma-web callers and the existing DatasetsWebService PUT keep their existing audit semantics.

Audit event types (`gemma-core`):

- `ubic.gemma.model.common.auditAndSecurity.eventType.TagAddedEvent` (extends `AnnotationEvent`).
- `ubic.gemma.model.common.auditAndSecurity.eventType.TagRemovedEvent` (extends `AnnotationEvent`).

Tests (`gemma-rest`):

- `AnnotationsWebServiceTest` — 8 new test methods (add happy path, 409 duplicate, 400 blank category, 400 bad evidence code, delete happy path, 404 not found, PUT idempotent no-op, PUT applies diff). `expressionExperimentService` mock added to the per-test `reset()` so verify counts don't bleed across tests. Full test class passes (13/13).

## Validation

```
mvn -pl gemma-core,gemma-rest compile test-compile -q     # clean
mvn -pl gemma-rest test -Dtest=AnnotationsWebServiceTest  # 13/13 passing
```

`mvn verify` was NOT run from this branch (gemdtest is single-tenant; sister agents are working in parallel).

## Auth model

`@PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")` on all three handlers, matching the design-write decision in `STATUS_PUT_DATASETS_DESIGN.md`. Fine-grained `curation:annotation:write` is deferred until the curator-role gating story lands across the curation-UI surface.

## Coexistence with `DatasetsWebService.updateDatasetAnnotations`

Both endpoints target the same EE characteristic set. They differ in audit granularity:

- `PUT /datasets/{id}/annotations` (DatasetsWebService) → calls `expressionExperimentService.updateAnnotations`, which emits ONE `ManualAnnotationEvent` per call (or zero if no change). Existing endpoint, unchanged.
- `PUT /annotations/datasets/{id}/annotations` (AnnotationsWebService — new) → diffs in the handler, loops per row through `addAnnotation`/`removeAnnotation`, emits per-row `TagAddedEvent`/`TagRemovedEvent`.

Per the spec ("This gives 'what was the state of this EE's tags at time T?' a correct event-log answer"), the new endpoint is the one the curation-agents client should call. The old endpoint stays as the cheap aggregate path for callers that don't need per-row granularity.

This is the choice that minimised scope per the multi-agent guardrails ("Touch ONLY AnnotationsWebService"); folding the two PUTs into a single endpoint with a query-param toggle is a follow-up worth considering once the agents pipeline is wired end-to-end and the lab decides whether per-row events are the universal preference.

## Decisions made (not in HANDOFF, parked from STATUS_PUT_DATASETS_DESIGN.md)

1. **`AgentProposal` linkage parked.** POST/PUT accept `agentProposalId` (query param on POST, body field on PUT) and log it at DEBUG. Linkage to emitted audit events is wired through `@Audited` SpEL; the `AgentProposal` entity isn't built yet so the FK target doesn't exist. Re-introduce once the entity lands.
2. **`AuditEventPayload` structured-diff column parked.** The `@Audited(messageSpel=...)` puts a human-readable summary into the existing `AUDIT_EVENT.NOTE` text column. Once the structured payload column lands, swap the SpEL for a structured payload builder.
3. **URI resolution parked.** `unresolved_uris` is always empty in the report; the server trusts client-supplied URIs per the spec's "Failure modes — Unknown URIs" recommendation (accept + flag). Wired into the response shape now so the contract is stable when boundary resolution lands.
4. **`audit_event_ids` empty.** The `@Audited` AOP aspect emits events but doesn't surface their ids back to the caller. Returning empty for now; populating these requires capturing in an `AfterReturning` advice that writes the id back through a `ThreadLocal` (or refactoring the aspect to return the event id). Follow-up.
5. **Evidence code validation.** Strict `GOEvidenceCode.valueOf(code.toUpperCase())`; unknown codes return 400. Matches the spec.
6. **Duplicate detection at POST.** Service-layer check in `addAnnotation` (loop over `ee.getCharacteristics()`); throws `IllegalArgumentException` mapped to 409 in the handler. Same `(category, categoryUri, value, valueUri)` comparator used by `updateAnnotations`.

## Out of scope

- OpenAPI spec is auto-generated from the annotations; the existing `openapi.json` regeneration is independent of this work.
- Python client wrappers in `~/Dev/gemma-curation-agents/gemma_curation_agents/shared/gemma.py` are a sibling-repo task (per spec §"Cross-references").
- No integration test (`@Tag("integration")`) added — the unit-level coverage in `AnnotationsWebServiceTest` exercises all three endpoints + the failure modes against the mocked service; a real-database round-trip belongs in a follow-up `BaseDatabaseTest5` test once gemdtest stops being single-tenant for parallel agents.

## Files touched

```
gemma-core/src/main/java/ubic/gemma/model/common/auditAndSecurity/eventType/TagAddedEvent.java                                                     (new)
gemma-core/src/main/java/ubic/gemma/model/common/auditAndSecurity/eventType/TagRemovedEvent.java                                                   (new)
gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentService.java                                     (interface: + addAnnotation, removeAnnotation)
gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentServiceImpl.java                                 (impl: + addAnnotation, removeAnnotation, +Audited import)
gemma-rest/src/main/java/ubic/gemma/rest/AnnotationsWebService.java                                                                                (+ POST/DELETE/PUT, AnnotationDto, AnnotationsReplaceRequest, AnnotationReplaceReport)
gemma-rest/src/test/java/ubic/gemma/rest/AnnotationsWebServiceTest.java                                                                            (+ 8 tests, expressionExperimentService in resetMocks)
handoffs/STATUS_DATASETS_ANNOTATIONS_WRITE.md                                                                                                       (this file)
```
