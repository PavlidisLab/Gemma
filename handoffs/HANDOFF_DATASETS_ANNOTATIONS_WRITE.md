# HANDOFF: Dataset annotation write endpoints

**Filed-by:** Paul Pavlidis (via curation-agents) — 2026-05-21
**Status:** request for Gemma-side endpoints; not implemented.
**Related PR / branch:** follow-on to PR #1656 (`api_fixes`).

---

## Motivation

`AnnotationsWebService` is **read-only**. EE-level annotations
(the "tags" curators attach to an experiment — organism part,
disease, treatment, cell type, sex, developmental stage, …) have
no writeback path through the REST API today. They can only be
mutated through the legacy Curation Tools UI by a human curator
clicking through the form.

This is the bulk of what the curation-agents project produces:
the agent's per-EE output is largely a set of tags (more than
factors-and-FVs, by row count). With no annotation write API, the
agent → Gemma loop for tags is **completely blocked** — every tag
the agent proposes today is stranded in `AgentProposal` JSON with
no path to materialise on the EE.

This is the #2 blocker called out in
`AGENT_WRITEBACK_RECCE.md` lines 41 ("Conspicuously absent"
table — `Annotation writes`).

Curator-side use case: a curator reviews the agent's tag
proposals for an EE (in the curation-agents UI / Gemma Curation
calibration tool), approves some, rejects others, edits a few.
The approved set needs to land on the EE. Today the curator
manually re-enters them in the Curation Tools UI. The agent + UI
already produce the structured tag list; the only missing piece
is the Gemma-side write endpoint.

---

## Required endpoints (sketch — finalize with maintainers)

The shape below is one viable decomposition. Maintainers should
push back on any of it.

### `POST /datasets/{id}/annotations`

Add a single tag to a dataset.

**Request:**

```json
{
  "category_uri": "http://purl.obolibrary.org/obo/UBERON_0000178",
  "category_label": "blood",
  "value_uri": "http://purl.obolibrary.org/obo/UBERON_0001088",
  "value_label": "urine",
  "evidence_code": "IEA",
  "predicate_uri": "http://purl.obolibrary.org/obo/BFO_0000050"  // optional
}
```

**Response:** `201 Created` with the new annotation row including
its server-assigned `annotation_id` and the audit-event id of the
emitted `TagAddedEvent`.

`409` if an annotation with the same `(category_uri, value_uri)`
pair already exists on the dataset.

### `DELETE /datasets/{id}/annotations/{annotation_id}`

Remove a single tag.

**Response:** `204 No Content` on success; `404` if the annotation
doesn't exist on this dataset.

### `PUT /datasets/{id}/annotations`

Bulk-replace the dataset's full tag set.

**Request:**

```json
{
  "annotations": [
    {
      "category_uri": "...",
      "category_label": "...",
      "value_uri": "...",
      "value_label": "...",
      "evidence_code": "IEA",
      "predicate_uri": "..."  // optional
    },
    ...
  ]
}
```

**Response:** `200 OK`

```json
{
  "ee_id": 12345,
  "before": 12,
  "after": 14,
  "added": [ { "annotation_id": 789, "value_uri": "..." }, ... ],
  "removed": [ { "annotation_id": 456, "value_uri": "..." }, ... ],
  "unchanged": 11,
  "audit_event_ids": [ /* one per add + one per remove */ ]
}
```

Server computes the diff against the current tag set and applies
adds + removes atomically. One `TagAddedEvent` per add, one
`TagRemovedEvent` per remove. This is the endpoint the agent will
use most: "here's the full agreed-upon tag set, server figures
out what to do."

### `GET /datasets/{id}/annotations` (existing, no change)

Already exists on `AnnotationsWebService`. Just calling out that
the diff-from-current logic in the PUT handler reads through this
same view.

---

## Authorization

Same curator-write role as `PUT /datasets/{id}/design`. Specifically:

- `curation:annotation:write` granted authority required.
- Curator role gets it by default.
- Agent identity gets `403` unless explicitly granted.
- In "audit" mode (per
  `~/Dev/gemma-curation-agents/docs/THREE_MODES.md`), the agent
  produces tag *proposals* that land on the EE's `AgentProposal`
  row, not by calling these endpoints directly. A curator reviews
  and then applies via the UI, which calls these endpoints under
  the curator's credentials.

Recommendation: combine with the
`HANDOFF_PUT_DATASETS_DESIGN.md` authority into a single
`curation:write` role that gates both design and annotation
mutations, OR keep them split (`curation:design:write` +
`curation:annotation:write`) so individual agent service accounts
can be granted one without the other. Slight preference for
split, since the threat model is different (a bad design write
invalidates DEAs; a bad annotation write is recoverable by reverting).

---

## Audit-event hooks

Each annotation mutation emits exactly one typed `AuditEvent` on
the dataset's audit trail:

- **`TagAddedEvent`** — new `AuditEventType` subclass. Carries
  the annotation's category/value URIs and labels in its
  note/detail (or `payload` once `AuditEventPayload` lands per
  `AUDIT_PHASE_C_RECCE.md` §4d).
- **`TagRemovedEvent`** — new `AuditEventType` subclass.

The bulk `PUT` emits N + M events, not one summary event. This
gives "what was the state of this EE's tags at time T?" a
correct event-log answer and matches existing per-mutation
granularity for design / curationDetails changes.

Per `AUDIT_PHASE_C_RECCE.md` patterns, the service methods should
be annotated declaratively:

```java
@Audited(TagAddedEvent.class)
public Annotation addAnnotation(ExpressionExperiment ee, AnnotationDto dto) { ... }

@Audited(TagRemovedEvent.class)
public void removeAnnotation(ExpressionExperiment ee, Long annotationId) { ... }
```

The bulk PUT calls the per-tag methods in a loop inside a single
transaction; each emits its own event. (Don't try to batch the
event emission — the per-row events are the point.)

`AgentProposal` linkage: if a tag was applied as a result of
accepting an agent-proposed tag, the `TagAddedEvent` should
carry an FK to the `AgentProposal` row (same pattern as
`HANDOFF_PUT_DATASETS_DESIGN.md`). The PUT body should accept an
optional `agent_proposal_id` field that the server attaches to
each emitted event for tags whose `(category_uri, value_uri)`
matches a proposal entry.

---

## Failure modes + idempotency

**`POST` idempotency.** Submitting the same annotation twice
returns `409` on the second call. The first call's response
contains the `annotation_id` the caller should use for future
DELETEs. No quiet de-dup.

**`PUT` idempotency.** Submitting the same full tag set twice is
a no-op:
- Server diffs against current; finds zero adds, zero removes.
- Returns `200 OK` with `added=[], removed=[], audit_event_ids=[]`.
- No events emitted.
- Safe to retry on network errors.

**Partial failure inside bulk PUT.** Whole bulk PUT runs in one
DB transaction; any failure on any per-row mutation rolls back
all of them. Caller sees `500 Internal Server Error` (or `409` if
the failure was a constraint violation) with the failing row
identified in the body. Audit events are also rolled back (none
visible).

**Unknown URIs.** If a `category_uri` or `value_uri` doesn't
resolve to a known ontology term, the server has two choices:
1. **Reject** with `400 Bad Request` listing the bad URIs.
2. **Accept** and store the URI as a free-text annotation
   (matches how the existing manual UI handles "novel" terms).

Recommendation: **accept** but flag in the response (`unresolved_uris: [...]`)
so the curator can decide whether to grow the ontology or pick a
different term. This matches existing Gemma behaviour for
manually-entered novel URIs.

**Validation of `evidence_code`.** Must be one of the existing
ECO codes Gemma already recognises (`IEA`, `IDA`, `IC`, …).
Reject with `400` if unknown.

**Idempotency for the agent-proposal applier.** Critical because
the agents-side runner retries naturally on transient
infrastructure errors. The `PUT` shape was chosen specifically to
make retries safe.

---

## Cross-references

- `AGENT_WRITEBACK_RECCE.md` (this repo) — origin recce; this is
  the second of the three "conspicuously absent" gaps.
- `AUDIT_PHASE_C_RECCE.md` (this repo) — declarative-audit
  patterns.
- `AUDIT_AS_WORKFLOW_RECCE.md` (this repo) — context on how
  these tag events surface in the curator ticket workflow.
- `HANDOFF_PUT_DATASETS_DESIGN.md` (this dir) — sibling write
  endpoint; same auth model + same `AgentProposal` linkage
  pattern.
- `~/Dev/gemma-curation-agents/docs/THREE_MODES.md` — agent mode
  semantics; clarifies the proposal vs. direct-write split.
- `~/Dev/gemma-curation-agents/gemma_curation_agents/shared/gemma.py`
  — Python client. Will gain `add_annotation`, `remove_annotation`,
  `replace_annotations` wrappers paralleling
  `update_curation_details`.
- `gemma-rest/.../AnnotationsWebService.java` — current read-only
  controller; write methods belong here or in a sibling
  `AnnotationsWebService` extension.

---

## Open questions for maintainers

1. **Endpoint location.** Add the write methods to the existing
   `AnnotationsWebService`, or split into a new
   `DatasetAnnotationsWriteWebService`? Curation-agents side has
   no preference.
2. **Predicate URIs.** Gemma's annotation model supports
   subject-predicate-object statements. The agent today emits
   `(category_uri, value_uri)` pairs (the dominant case) but the
   API should accept an optional `predicate_uri` for the statement
   shape. Confirm the field name and what value to default to when
   the agent provides a flat tag.
3. **Bulk-replace scope.** Does `PUT /datasets/{id}/annotations`
   replace ALL annotations on the EE, or only annotations of a
   specific category (one PUT per category)? Recommendation: full
   replace; the diff logic handles the per-category case correctly
   anyway, and "full replace" is what the agent's output shape
   matches.
4. **Statement vs. tag annotations.** A subset of Gemma annotations
   are *factor-value statements* (sample-level, not EE-level).
   Those are out of scope for this handoff — they're written via
   the design endpoint (see `HANDOFF_PUT_DATASETS_DESIGN.md`).
   Confirm the EE-level annotation table is in fact the right
   storage for the agent's "tag" output, not the FV-statement table.
5. **Ontology resolution at the server boundary.** Agent already
   resolves free-text → URI via Gemma's `/rest/v2/search` endpoint
   client-side. Does the server want to re-resolve at the write
   boundary as a safety net, or trust the client's URIs? Recommend
   trust + flag-unresolved (per "Failure modes" above).

---

## Acceptance criteria

This endpoint set is "done" when:

- [ ] `POST /datasets/{id}/annotations` adds a single annotation;
      `409` on duplicate; emits `TagAddedEvent`.
- [ ] `DELETE /datasets/{id}/annotations/{annotation_id}` removes
      a single annotation; emits `TagRemovedEvent`.
- [ ] `PUT /datasets/{id}/annotations` bulk-replaces; emits one
      event per per-row mutation; idempotent (no-op on
      already-applied set).
- [ ] All three methods are annotated `@Audited(...)` declaratively
      per Phase-C patterns.
- [ ] Auth: `403` on agent-role callers without
      `curation:annotation:write` authority.
- [ ] Optional `agent_proposal_id` field on POST and PUT bodies;
      server attaches FK on emitted audit events.
- [ ] Transactional: bulk PUT is all-or-nothing.
- [ ] Integration test exercises POST → DELETE → bulk PUT with
      mixed adds/removes/unchanged → re-PUT (no-op).
- [ ] `gemma-rest` OpenAPI spec updated.
- [ ] Python client wrappers in `shared/gemma.py` (sibling repo).
- [ ] Existing read endpoint on `AnnotationsWebService` continues
      to work unchanged.
