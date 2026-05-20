# Curation-UI HANDOFF asks vs gemma-rest implementation

Scope: 19 `*_HANDOFF.md` docs under `/Users/pzoot/Dev/gemma-curation-ui/apps/curation/`,
cross-checked against `@Path`/`@GET`/`@POST`/`@PUT`/`@PATCH`/`@DELETE` annotations
in `gemma-rest/src/main/java/ubic/gemma/rest/` at commit
`3c232b2777e9e14dcf864cd2fffb9126e260f1d8` of branch `phase2-acl-migrate`.

Crucial context: the React app's `src/api/client.ts` talks to a **FastAPI mock**
in the `gemma-curation-agents` repo on `:8080`, NOT to gemma-rest. Most "handoff"
docs are asks against the mock. The relevance of this inventory is what happens
when the UI is pointed at the **real** Gemma (the "remote mode" referenced in
`CLAUDE.md`). I evaluate each ask against gemma-rest's surface today.

Legend: implemented = endpoint exists on real Gemma with the right shape;
partial = endpoint exists but shape differs from what UI/mock uses today;
missing = no matching `@Path`; stub = endpoint exists but doesn't yet do the
work (no stubs of relevance in this repo — gemma-rest endpoints are all real).

## 1. Per-handoff status table

| # | HANDOFF | Endpoint asks (UI side) | Status | Gap |
|---|---|---|---|---|
| 1 | `GEMMA_WIRE_ALIGNMENT_HANDOFF` | `GET /datasets/{id}/auditEvents`, `GET/PUT /datasets/{id}/curationDetails`, `PUT /datasets/{id}/permissions`, `GET /datasets/{id}/pipelineStatus`, `isPublic` inline on EE VO, SSE envelope camelCase | implemented | All five endpoints exist (`DatasetsWebService.java` lines 992, 1033, 1112, 1240, 1304). SSE doesn't exist on gemma-rest at all — only on the mock. |
| 2 | `AUDIT_DEFENDER_VERDICT_HANDOFF` | extra `defender_verdict` field on `AuditFinding` (read-only) | missing | No `AuditFinding` / `/audits` surface on gemma-rest. Audit features live entirely in the mock. |
| 3 | `AUDIT_DISPOSITION_EDIT_HANDOFF` | echo `dismiss_reason`/`accept_reason`/`not_sure_reason` on disposition read; `finalized_notes` on `AuditReport` | missing | No `AuditReport` surface on gemma-rest. |
| 4 | `AUDIT_DISPOSITION_REASONS_HANDOFF` | trim+extend `DismissReason`/`AcceptReason`/`NotSureReason` enums; `PATCH /audits/{id}` | missing | No audit endpoints. Pure mock-side ask. |
| 5 | `AUDIT_STATUS_CLOSED_RULE_HANDOFF` | `audit_status` derivation rule on storage | missing | No audit storage on gemma-rest. |
| 6 | `CALIBRATION_DISPOSITION_REASONS_HANDOFF` | extend `DismissReason`/`AcceptReason` for calibration chips | missing | Audit-adjacent, mock only. |
| 7 | `DEBATE_BADGE_HANDOFF` | `badge` field on `proposals.jsonl` rows; `stuck_items.jsonl`; `n_stuck` on convergence report | missing | No proposals endpoint or debate concept on gemma-rest. Mock-only product domain. |
| 8 | `DEBATE_TRANSCRIPT_HANDOFF` | `debate_transcripts.jsonl` per-GSE; `comparison_proposal.factors` on `AuditReport` | missing | Same — no audit/proposer endpoints in gemma-rest. |
| 9 | `DESIGN_COMPARISON_ALIGNMENT_HANDOFF` | `match_type` + `gemma_ref` on `FactorProposal`/`FactorValueProposal`/`TagProposal` | missing | These types exist only in mock Pydantic schemas. |
| 10 | `DESIGN_COMPARISON_HANDOFF` | render `comparison_proposal.factors` from `audit.json` | missing | Audit JSON is a mock-only calibration-package artifact. |
| 11 | `DESIGN_DEBATE_HANDOFF` | `statements` on `FactorValueProposal`; `design_debate_transcripts.json` sidecar | missing | Mock-only. |
| 12 | `EE_TAG_EVIDENCE_QUALITY_HANDOFF` | prompt-tuning + section labels on `paper_excerpt` in the proposer | missing | No proposer LLM at all on gemma-rest; this is an LLM-pipeline ask, not a REST ask. |
| 13 | `FACTOR_CALIBRATION_FINDINGS_HANDOFF` | factor-level `AuditFinding`s with new `issue_code`s + `ApplyAction` kinds | missing | Mock-only audit feature. |
| 14 | `FACTOR_DEFENDER_VERDICT_HANDOFF` | extend `AttachedDefenderVerdict` enum for factor verdicts | missing | Audit/judge layer is mock-only. |
| 15 | `PREDICATE_URI_HANDOFF` | populate `predicate.uri` on `StatementProposal` from canonical label→URI map | missing | `StatementProposal` is a proposer-pipeline type, mock-only. |
| 16 | `STRIP_CURATION_OVERREACH_HANDOFF` | `strip_curation` import path should preserve `publications` + `external_source` | missing | The `strip_curation` import is on the mock's `POST /datasets/import`; gemma-rest has no such endpoint. |
| 17 | `SUBJECT_URI_HANDOFF` | put URI on `subject.uri` (not object) for canonical FV shapes | missing | Same — mock-side proposer-pipeline ask. |
| 18 | `WORKFLOW_MANAGEMENT_HANDOFF` | Candidate / Group / pipeline-status / bulk-pipeline-status endpoints; bulk-list shape + pagination | partial | `pipelineStatus` (single) exists on gemma-rest (line 1304); `Group`/`Candidate`/bulk-pipeline endpoints all missing. The dataset list endpoint exists with pagination, but the `WorkflowDatasetRow` shape (notably `n_pending_proposals`, `n_unactioned_blocker`, `latest_audit_verdict`) is mock-only. |
| 19 | (also via UI source) annotation typeahead `GET /annotations/search` | partial | `AnnotationsWebService` line 256 has `/annotations/search` and `/annotations/search/{query}`, but returns `SearchWebService.SearchResult<CharacteristicValueObject>`-style hits — not the UI's flat `AnnotationCandidate { label, uri, category_label, category_uri, usage_count }` array. UI's TS comment already calls out `usage_count` isn't currently exposed. |
| extra | UI source: `POST /find-publication`, `POST /find-term` | missing | Both proxied via Vite to `:8080` mock. No corresponding endpoints in gemma-rest (would need new ones to run "remote mode"). |
| extra | UI source: `GET /rest/v2/me`, `POST /rest/v2/login`, `POST /rest/v2/logout` | partial | `RootWebService.java:105` has `/users/me`; no `/me` alias, no `/login`/`/logout` JSON endpoints (gemma-rest uses HTTP Basic / cookie auth via the host webapp). |

## 2. Missing-endpoint shortlist (the ❌ rows)

These are endpoints the UI calls today (against the mock) that have no
gemma-rest analogue. If "remote mode" is to be wired up, each one needs a
new REST surface on the Gemma side, OR the relevant feature stays exclusive
to local/standalone curation mode.

| Surface area | Proposed REST signature | Notes |
|---|---|---|
| **Audits** | `GET  /rest/v2/audits` → `AuditListResponse`. `GET  /rest/v2/audits/{auditId}` → `AuditReport`. `POST /rest/v2/audits/{accession}` body `{ scope_include[], model_tier }` → `AuditReport`. `PATCH /rest/v2/audits/{auditId}` body `{ findings: [{ target_id, status, dismiss_reason?, accept_reason?, not_sure_reason?, notes }] }` → `AuditReport`. `POST /rest/v2/audits/{auditId}/finalize` body `{ notes }`. `POST /rest/v2/audits/{auditId}/reopen` body `{ notes }`. `GET  /rest/v2/datasets/{id}/auditEvents` already exists — distinct from agent audits. | Whole audit/disposition product (HANDOFFs 2–6, 13, 14) is built around endpoints that don't exist on real Gemma. The closest existing concept is `/datasets/{id}/auditEvents` (curation events), which is a different surface. |
| **Curation proposals** | `GET  /rest/v2/curation-proposals?dataset_id=…&status=…` → `ProposalListResponse`. `GET  /rest/v2/curation-proposals/{id}` → `Proposal`. `POST /rest/v2/curation-proposals/{id}/{action}` (action ∈ accept, reject) → `Proposal`. `PATCH /rest/v2/curation-proposals/{id}` body `{ status, notes }` → `Proposal`. `POST /propose` (SSE stream) body `{ dataset_id, mode }`. | Entire proposer surface (HANDOFFs 7–11, 12, 15, 17) is mock-only. |
| **Find publication** | `POST /rest/v2/datasets/{id}/find-publication` body `{ overrides? }` → `FindPublicationsResult`. | Today UI hits `POST /find-publication` against the mock. |
| **Find term** | `POST /rest/v2/annotations/find-term` body `{ query, category? }` → `FindTermResult`. | Today UI hits `POST /find-term` against the mock. |
| **Groups** | `GET    /rest/v2/groups[?type=&created_by=]` → `Group[]`. `POST   /rest/v2/groups` body `{ name, type, description? }` → `Group`. `GET    /rest/v2/groups/{id}[?include_summaries=true]` → `Group` (with optional `member_summaries`). `PATCH  /rest/v2/groups/{id}` body `{ name?, description? }`. `DELETE /rest/v2/groups/{id}`. `GET    /rest/v2/groups/{id}/members` → `Candidate[]` or `ExperimentStub[]`. `POST   /rest/v2/groups/{id}/members` body `{ member_ids: string[] }`. `DELETE /rest/v2/groups/{id}/members/{memberId}`. `GET    /rest/v2/datasets/{id}/groups` → `Group[]` (reverse-lookup). | Whole workflow-management group concept absent. |
| **Candidates** | `GET    /rest/v2/candidates[?status=&source=&source_batch=&reviewer=]` → `Candidate[]`. `POST   /rest/v2/candidates` body `Candidate`. `POST   /rest/v2/candidates/bulk` body `{ source_batch, source, items }` → `Candidate[]`. `GET    /rest/v2/candidates/{id}`. `PATCH  /rest/v2/candidates/{id}` body `{ status?, decision_reason?, notes?, gemma_id?, … }`. `DELETE /rest/v2/candidates/{id}`. | Pre-Gemma "screening world" entity has no analogue on gemma-rest. |
| **Bulk pipeline status** | `POST /rest/v2/datasets/pipelineStatus` body `{ dataset_ids: int[] }` → `Record<datasetId, PipelineStatusValueObject>`. | Single-row `GET /rest/v2/datasets/{id}/pipelineStatus` exists; the bulk variant required for the workflow list view does not. |
| **Login / session JSON** | `GET  /rest/v2/me` → `User \| null`. `POST /rest/v2/login` body `{ username, password }` → `LoginResponse`. `POST /rest/v2/logout`. | gemma-rest's `/users/me` (line 105) covers the read; no JSON login/logout — the existing webapp expects session cookies set by the Spring login form. |
| **Outlier toggle** | `PUT /rest/v2/datasets/{id}/samples/{sampleId}/outlier` body `{ outlier: boolean }`. | No `/samples/{sid}/outlier` route exists; UI calls one against the mock. |
| **Quantitation type preferred** | `PATCH /rest/v2/datasets/{id}/quantitationTypes/{qtId}` body `{ isPreferred: boolean }`. | `GET /datasets/{id}/quantitationTypes` exists (line 2140); no `PATCH` to flip the preferred flag. |
| **GEEQ recompute task** | `POST /rest/v2/datasets/{id}/geeq/recalculate` → `AsyncTask`. | `GET`/`PUT /datasets/{id}/geeq` exist (lines 1394, 1426); the dispatch endpoint that returns a task handle does not. Closest existing dispatch surface is `/datasets/{id}/tasks/{preprocess|diagnostics|batchInfo|differential}`. |
| **DEA actions** | `POST /rest/v2/datasets/{id}/analyses/differential` (UI calls this for DEA dispatch). `DELETE /rest/v2/datasets/{id}/analyses/differential/{analysisId}` (UI deletes a DEA). | The dispatch path on gemma-rest is `POST /datasets/{id}/tasks/differential` (line 1565) — different path. Delete exists at `DELETE /datasets/{id}/tasks/differential/{analysisId}` (line 1682) — also a `tasks/` prefix. Path mismatch in the UI client. |

## 3. Shape-mismatch shortlist (the partial rows)

| Endpoint | UI/mock wants | gemma-rest returns | Action |
|---|---|---|---|
| `GET /rest/v2/datasets` (list) | `{ data: WorkflowDatasetRow[], totalElements, offset, limit }` where each row carries `n_pending_proposals`, `n_unactioned_blocker`, `n_unactioned_major`, `latest_audit_verdict`. | `QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentWithSearchResultValueObject>` — no curation-layer extras. | Either gemma-rest grows opt-in `?include=curation` fields, or the workflow list does a two-step fetch (Gemma list + mock curation overlay). |
| `GET /rest/v2/datasets/{id}/curationDetails` (and `PUT`) | Flat `last_*_at` / `last_*_by` fields (mock); UI mid-migration to nested `lastTroubledEvent`/`lastNoteUpdateEvent`/`lastNeedsAttentionEvent` (real Gemma shape per phase-2 of `GEMMA_WIRE_ALIGNMENT`). | Real Gemma already returns the nested `AuditEventValueObject` form (this is the migration target). | UI cuts over; mock follows. No gemma-rest gap. |
| `GET /rest/v2/datasets/{id}/auditEvents` | `AuditEvent[]` with `event_type` (snake) + optional `shape` summary. | Real Gemma returns `AuditEventValueObject[]` with `eventType` (camel), no `shape`. | UI's `src/api/history.ts` rename `event_type` → `eventType` and drop `shape`. Tracked in `GEMMA_WIRE_ALIGNMENT_HANDOFF` §1. |
| `PUT /rest/v2/datasets/{id}/permissions` | `{ isPublic: boolean? }` → `{ isPublic, isShared }`. | Matches real Gemma exactly (mock landed this 2026-05-13). | UI swap `usePublishExperiment` to this endpoint; drop legacy `POST /publish` + `GET /visibility` from mock. |
| `isPublic` on EE VO | Today via separate `useDatasetVisibility` (mock `/visibility` endpoint). | Real Gemma returns `isPublic` inline on `ExpressionExperimentValueObject`. | UI drops the separate hook and reads it from `GET /datasets/{id}`. |
| `GET /rest/v2/annotations/search` | flat `AnnotationCandidate { label, uri, category_label, category_uri, usage_count }[]`. | `AnnotationsWebService` `/annotations/search` returns a search-result envelope; no `usage_count` field; categories nested differently. | gemma-rest grows a `usage_count` field on `OntologyTermValueObject` (already noted in `GEMMA_WIRE_ALIGNMENT_HANDOFF` §"Adopt-able new endpoints"), and UI's TS mirror moves to the canonical Gemma shape. |
| `GET /rest/v2/me` | `User \| null` keyed under `/rest/v2/me`. | `GET /users/me` returns the same value at a different path. | Either gemma-rest adds a `/me` alias, or UI renames its client to `/users/me`. Trivial. |

## 4. Sizing per gap

For an engineer adding the missing endpoint to gemma-rest (Spring/Hibernate stack):

| Gap | Size | Why |
|---|---|---|
| Audit surface (HANDOFFs 2–6, 13, 14) | **L** | Whole new entity: AuditReport, AuditFinding, AuditFindingDisposition with append-only storage, finalize/reopen state machine, calibration chip enums, defender-verdict ride-along, factor-side findings. Currently lives entirely in the FastAPI mock with SQLite storage. Porting to Hibernate is a multi-week effort. |
| Curation proposals (HANDOFFs 7–11, 12, 15, 17) | **L** | Same as above for the proposer side: Proposal entity, accept/reject lifecycle, SSE streaming via Jersey SSE, plus the proposer LLM pipeline itself (which isn't a REST concern but does need to be reachable from gemma-rest if the React app is to drive it). |
| Find-publication / Find-term | **M** each | New endpoints with biolit + ontology lookups behind them. The lookup logic exists in the mock as `find_publication.py` / `find_term.py`; would need to be a Spring service or a sidecar process. |
| Workflow Groups | **M** | New entity (Group, GroupMember) with simple CRUD + reverse-lookup (`/datasets/{id}/groups`) + member-summary expansion. No agents-side LLM dependency. Straightforward to implement. |
| Candidates | **M** | New entity for pre-Gemma screening with bulk-create, state machine, decision-reason validator. No agents-side LLM dependency. Independent of audits/proposals — could ship standalone. |
| Bulk pipeline-status | **S** | Single-row exists; bulk takes a list of IDs and loops/fans-out. Likely a 30-line addition. |
| Login / session JSON | **S** | `/me` alias on `RootWebService`; JSON login/logout endpoints sit on top of the existing Spring Security filters. |
| Outlier PUT | **S** | One new route on `DatasetsWebService` calling existing `BioAssayService.markAsOutlier`. |
| QT preferred PATCH | **S** | Similar — flips a flag on `QuantitationType`. |
| GEEQ recompute task | **S** | New `POST /datasets/{id}/geeq/recalculate` mirroring the existing `/tasks/preprocess` shape. |
| DEA path mismatch | **S** | Either gemma-rest adds `/analyses/differential` POST/DELETE aliases, or UI client renames to `/tasks/differential`. |
| Annotations search shape | **S** | Add `usageCount` to `OntologyTermValueObject` (and similar) — already in the wire-alignment doc's "adopt-able new endpoints". |
| Workflow `WorkflowDatasetRow` overlay | **M** | Either bake curation-counter fields into `ExpressionExperimentValueObject` (cross-cutting) or expose a separate `/datasets/curation-overlay?ids=…` endpoint. Real Gemma doesn't track agent proposals, so the data has to come from elsewhere regardless. |

## 5. Recommended order — most blocking gaps to ship a minimal curation flow

The shortest path to "a curator opens the React app pointed at real Gemma and
gets a working basic loop":

1. **Bulk pipeline-status (`POST /rest/v2/datasets/pipelineStatus`)** — **S**.
   Single-row endpoint exists; bulk is required for the workflow list view to
   not be N+1. Without it the list view is unusable for any group with more
   than ~5 experiments.

2. **Workflow Groups (full CRUD + members + `/datasets/{id}/groups`)** — **M**.
   The set-navigator UI (chip popover, prev/next arrows, `[`/`]` shortcuts) is
   already wired and is the primary navigation surface inside a review batch.
   Without groups on real Gemma the React app falls back to "single experiment
   at a time", which is the gemma-web behaviour we're trying to escape.

3. **Annotations search shape + `usage_count`** — **S**. The typeahead is on
   every chip add in the design editor and EE-tag panel. Today it works via
   the mock; against real Gemma it returns the wrong shape and the picker
   stops sorting by usage (which is what makes it usable for curators).

4. **Outlier PUT + QT preferred PATCH + GEEQ recompute task + DEA path alias**
   — **S** apiece. These are the workflow-step actions the curator clicks
   from the experiment page. Without them the "Run preprocessing", "Mark
   outlier", "Recompute GEEQ" buttons all dead-end. Cluster-ship them in one
   PR — they're each tiny additions to `DatasetsWebService`.

5. **`/me` alias + Candidates CRUD** — **S** + **M**. The session-auth alias
   is a 5-line change; Candidates is a self-contained new entity that
   unblocks the screening-world half of the workflow product. Audits and
   proposals can stay mock-only longer because they're the LLM-pipeline-
   driven surfaces and require co-deploying a curation-agents sidecar
   regardless — but Candidates is pure CRUD with no LLM dependency, so it
   ships cleanly on the Gemma side alone.

Audits + curation-proposals + the LLM-pipeline surfaces (find-publication,
find-term, propose stream, judge / defender) are explicitly *out* of this
shortlist. They're cohesively a separate product (the "curation agent"
sidecar) that currently lives in the mock and would either need a wholesale
port to gemma-rest OR continue as a sidecar service the React app talks to
in addition to gemma-rest. The latter is the model the `CLAUDE.md` mentions
("future remote mode points at the real Gemma REST API" — implying the
agents service is separately deployed).

## Surprises

- **The wire-alignment doc is mostly already aligned.** `GET /datasets/{id}/auditEvents`,
  `GET/PUT /curationDetails`, `PUT /permissions`, `GET /pipelineStatus`, and inline
  `isPublic` all exist on gemma-rest today. The remaining lockstep is a UI-side
  TS rename (snake → camel, drop `shape`) + dropping the visibility helper hook.
  This handoff doc reads "still open" in lots of places but the gemma-rest side
  is done; what's "open" is the **mock + UI** catching up.

- **`POST /rest/v2/datasets/import`** is referenced by the UI's
  `useImportDataset` hook (`datasets.ts:144`) but no equivalent exists in
  gemma-rest. Real Gemma imports happen via the GeoBrowser admin path on the
  Spring webapp, not the REST API. This is a meaningful gap for the
  candidate→skeleton "load" step in the workflow-management product.

- **`GET /rest/v2/datasets/{id}/groups`** (reverse-lookup: which groups
  contain this dataset) appears in the UI source but **isn't in
  `WORKFLOW_MANAGEMENT_HANDOFF`'s table** — the experiment-banner
  prev/next navigator relies on it. Worth lifting into the workflow handoff
  explicitly.

- **`/users/me` exists but `/me` doesn't.** Trivially fixable but the React
  app's `session.ts` will fail authentication probing without either an alias
  or a UI-side rename.

- **The DEA dispatch path is `/datasets/{id}/tasks/differential` on
  gemma-rest** (with a `tasks/` prefix) **but the UI calls
  `/datasets/{id}/analyses/differential`** without it (`workflow.ts:165`).
  Will silently 404 in remote mode. The `useTriggerDifferential` and
  `useDeleteDifferential` hooks both have this bug.
