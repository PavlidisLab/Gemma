# Pipeline Status wire-shape audit — UI vs gemma-rest

Follow-up to batch 86: the bulk `POST /datasets/pipeline-status` endpoint
flagged that the curation-UI's `PipelinePanel.tsx` (and friends) read fields
that don't match the actual `PipelineStatusValueObject` shape. The UI was
built against a FastAPI mock (`workflow_schemas.py`) and its TS types are
`snake_case` mirrors of that mock — Gemma is `camelCase` and structurally
different.

This audit catalogues both shapes, scores match/mismatch per field, and
recommends per-field alignment.

## 1. UI shape catalogue

Source: `apps/curation/src/api/workflowTypes.ts::ExperimentPipelineStatus`
plus every component that reads off the returned object.

Top-level fields the UI accesses on `pipelineStatus`:

| Field | Type | Read by |
|---|---|---|
| `dataset_id` | `number` | (declared, never read in the panel) |
| `analysis` | `AnalysisTrack` | `PipelinePanel.tsx`, `PipelineTrackStrip.tsx` |
| `curation` | `CurationTrack` | `PipelinePanel.tsx`, `PipelineTrackStrip.tsx` |
| `is_public` | `boolean` | `PipelinePanel.tsx` (`VisibilitySection`) |
| `is_troubled` | `boolean` | (declared, not read in panel; `PipelineStatusRow` reads `dataset.troubled`) |
| `needs_attention` | `boolean` | (declared, not read in panel) |
| `curation_note` | `string \| null` | `PipelineStatusRow.tsx` |
| `geeq_quality` | `number \| null` | (declared, not read in panel — the panel uses a separate `useGeeq` hook) |
| `geeq_suitability` | `number \| null` | (declared, not read in panel) |
| `candidate_provenance` | `CandidateProvenance \| null` | (declared, not read in panel) |

`AnalysisTrack` (an object keyed by step):
`missing_value_analysis`, `batch_info`, `preprocessing`, `dea`, `diagnostics`
— each a `PipelineStep` = `{ status, last_run, details }`. `status` is one
of `not_run`/`ok`/`failed`/`in_progress`/`needs_attention`/`na`.

`CurationTrack`:
`design`, `tags`, `outlier_review`, `batch_decision`, `audit` — same
`PipelineStep` shape.

Other `pipelineStatus.*` reads in the codebase: `grep -rn 'pipelineStatus\.'`
returns zero matches — every consumer destructures (`status.analysis`,
`status.curation`, `status.curation_note`) rather than chaining off the
literal name `pipelineStatus`. The three consumer files are
`PipelinePanel.tsx`, `PipelineStatusRow.tsx`, and `PipelineTrackStrip.tsx`.

## 2. Java shape catalogue

Source: `gemma-rest/src/main/java/ubic/gemma/rest/PipelineStatusValueObject.java`
plus the populating handler `DatasetsWebService.getDatasetPipelineStatus`
(and bulk twin `getDatasetPipelineStatusBulk`).

Top-level fields on `PipelineStatusValueObject`:

| Field | Type | Notes |
|---|---|---|
| `experimentId` | `Long` | populated by both handlers |
| `steps` | `List<PipelineStepValueObject>` | **flat list, not split into analysis/curation tracks** |
| `hasBatchInformation` | `boolean` | populated |
| `hasDifferentialExpressionAnalysis` | `boolean` | populated |
| `hasCoexpressionAnalysis` | `boolean` | always `false` (coex removed Phase 1c) |
| `troubled` | `boolean` | populated |
| `troubleDetails` | `String` | populated (empty when not troubled) |
| `needsAttention` | `boolean` | populated |
| `curationNote` | `String?` | admin-only; `null` for non-admins |
| `isPublic` | `boolean` | serialised as `isPublic` (Lombok + explicit `getIsPublic`) |
| `geeq` | `GeeqValueObject?` | full GEEQ VO inline |

`PipelineStepValueObject`:
- `step` — one of `batchInfo`, `preprocess`, `batchCorrection`, `pca`,
  `sampleCorrelation`, `meanVariance`, `dea`, `coexpression`, `missingValue`.
- `state` — one of `ok`, `failed`, `notRun`, `notApplicable`.
- `lastRun` — `Date?`.
- `eventType` — audit-event class simple name, e.g. `BatchInformationFetchingEvent`.
- `message` — note attached to the latest audit event.

There is **no curation track at all** in the Java VO. Curation status
(design / tags / outlier review / batch decision / audit) lives in
separate services on the Gemma side; the pipeline-status endpoint is
preprocess-and-analysis-only.

## 3. Side-by-side table

| UI access | UI semantic | Java field | Match? | Gap |
|---|---|---|---|---|
| `pipelineStatus.is_public` | bool, dataset visibility | `isPublic` | ~ | snake vs camel JSON key |
| `pipelineStatus.is_troubled` | bool, curator-troubled flag | `troubled` | ~ | snake + name (`is_troubled` vs `troubled`) |
| `pipelineStatus.needs_attention` | bool | `needsAttention` | ~ | snake vs camel |
| `pipelineStatus.curation_note` | string\|null | `curationNote` | ~ | snake vs camel |
| `pipelineStatus.geeq_quality` | number\|null | `geeq.publicQualityScore` (nested) | ✗ | flat-vs-nested + name; UI expects flattened |
| `pipelineStatus.geeq_suitability` | number\|null | `geeq.publicSuitabilityScore` (nested) | ✗ | flat-vs-nested + name |
| `pipelineStatus.dataset_id` | number | `experimentId` | ~ | snake + `dataset` vs `experiment` |
| `pipelineStatus.candidate_provenance` | obj\|null | — | ✗ | UI-only concept (pre-Gemma screening); no Gemma equivalent |
| `pipelineStatus.analysis.missing_value_analysis` | `PipelineStep` | `steps[step="missingValue"]` | ✗ | shape: object-of-steps vs list-of-steps |
| `pipelineStatus.analysis.batch_info` | `PipelineStep` | `steps[step="batchInfo"]` | ✗ | same |
| `pipelineStatus.analysis.preprocessing` | `PipelineStep` | `steps[step="preprocess"]` | ✗ | same + name (`preprocessing` vs `preprocess`) |
| `pipelineStatus.analysis.dea` | `PipelineStep` | `steps[step="dea"]` | ✗ | same |
| `pipelineStatus.analysis.diagnostics` | `PipelineStep` | composite of `pca` + GEEQ events | ✗ | UI's "diagnostics" doesn't map 1:1; Java has `pca`, `sampleCorrelation`, `meanVariance` as separate steps |
| `pipelineStatus.curation.design` | `PipelineStep` | — | ✗ | no curation track on Java side |
| `pipelineStatus.curation.tags` | `PipelineStep` | — | ✗ | ditto |
| `pipelineStatus.curation.outlier_review` | `PipelineStep` | — | ✗ | ditto |
| `pipelineStatus.curation.batch_decision` | `PipelineStep` | — | ✗ | ditto |
| `pipelineStatus.curation.audit` | `PipelineStep` | — | ✗ | ditto |
| `PipelineStep.status` ∈ {not_run, ok, failed, in_progress, needs_attention, na} | enum | `state` ∈ {ok, failed, notRun, notApplicable} | ✗ | name (`status` vs `state`) + value vocabulary (no `in_progress`, no `needs_attention`, value casing `not_run` vs `notRun`) |
| `PipelineStep.last_run` | iso string\|null | `lastRun` (Date) | ~ | snake + Date-vs-iso (Jackson default fine) |
| `PipelineStep.details` | string\|null | `message` | ~ | name |
| — | — | `hasBatchInformation` | — | UI doesn't currently read; could replace `pipelineStatus.analysis.batch_info.status === "ok"` |
| — | — | `hasDifferentialExpressionAnalysis` | — | UI doesn't currently read |
| — | — | `troubleDetails` | — | UI doesn't currently read (uses `curation_note` instead) |
| — | — | `eventType` | — | UI doesn't currently read; useful for failure-reason display |

Tally: **2 matching exactly · 7 snake/camel-only mismatches · 14 structural mismatches**.

## 4. Recommended alignment per field

### 4a. Snake/camel naming — Java wire-rename via `@JsonProperty`

The Python mock has trained the UI to read snake_case. The Gemma Java VO
is camelCase. For *cosmetic* mismatches (same semantics, different key),
the cheapest fix is to add `@JsonProperty("...")` annotations to the
Java VO so the wire shape matches the UI's expectations without touching
the UI or the Java field names:

- `experimentId` → `@JsonProperty("dataset_id")` (also aligns with the
  bulk endpoint's `dataset_ids` request convention already established
  in `PipelineStatusBulkRequest`).
- `troubled` → `@JsonProperty("is_troubled")` (matches UI semantic).
- `isPublic` → already serialised as `isPublic` via `getIsPublic` — add
  `@JsonProperty("is_public")` to align.
- `needsAttention` → `@JsonProperty("needs_attention")`.
- `curationNote` → `@JsonProperty("curation_note")`.
- `hasBatchInformation` → `@JsonProperty("has_batch_information")` (if
  the UI starts reading it).
- `hasDifferentialExpressionAnalysis` → `@JsonProperty("has_dea")`.
- `hasCoexpressionAnalysis` — slated for removal (always false); strip
  rather than rename.
- `troubleDetails` → `@JsonProperty("trouble_details")`.
- On `PipelineStepValueObject`: `step` (keep), `state` → `@JsonProperty("status")`,
  `lastRun` → `@JsonProperty("last_run")`, `eventType` → `@JsonProperty("event_type")`,
  `message` → `@JsonProperty("details")`.

Effort: ~30 minutes. One file. No behavioural change. Add a unit test
that asserts the wire JSON contains the snake_case keys.

### 4b. Structural: object-of-steps vs list-of-steps — UI changes

The UI reads `pipelineStatus.analysis.missing_value_analysis` (object keyed
by step name). Java emits `steps: [{step: "missingValue", state: "ok", ...}, ...]`.

Two ways forward:

- **UI changes (recommended)**: introduce a thin client-side adapter that
  collapses the flat `steps[]` into an `analysis` map after fetch. Single
  function (~15 lines) in `api/workflow.ts`'s `usePipelineStatus` query
  function. Keeps the wire shape close to what Gemma already produces;
  doesn't require a server-side schema rev; matches the bulk endpoint's
  existing shape without ceremony. Vocabulary normalization (`notRun` →
  `not_run`, `notApplicable` → `na`) goes in the same adapter.
- Java changes: add `analysis` / `analysisByKey` derived getter on the
  VO that returns a `Map<String, PipelineStepValueObject>`. Adds duplicate
  data on the wire. Reject — bloats payloads, two sources of truth.

Effort (UI route): ~1 hour. Adapter + types + small unit test.

### 4c. Status vocabulary — UI changes

UI: `not_run | ok | failed | in_progress | needs_attention | na`.
Java: `notRun | ok | failed | notApplicable`.

Gemma has no concept of `in_progress` (the pipeline runs synchronously per
audit-event taxonomy — by the time you read the status, the step is done
or failed) and no `needs_attention` *per step* (the EE-level
`needsAttention` flag is dataset-wide, not step-scoped).

Recommendation: UI normalises the four Java states to its six-state enum in
the adapter from 4b. Treat `notRun`→`not_run`, `notApplicable`→`na`,
`ok`/`failed` pass through. `in_progress` and `needs_attention` simply
don't appear from Gemma; UI keeps them in the type for use by the async-task
banner overlay (which is task-scoped, not VO-scoped — already wired through
`useTask`).

Effort: bundled into 4b.

### 4d. Diagnostics step — Java changes + UI adapter

UI shows one `diagnostics` step (PCA / GEEQ). Java emits `pca`,
`sampleCorrelation`, `meanVariance` as three separate steps plus a `geeq`
sidecar.

Recommendation: collapse in the UI adapter. `diagnostics.status` = worst
of `pca` / `sampleCorrelation` / `meanVariance` (`failed` > `notRun` >
`notApplicable` > `ok`). `diagnostics.last_run` = latest of the three.
`diagnostics.details` = first non-empty `message` from a failed step,
else null. This is presentation logic and belongs UI-side. No Java change.

Effort: included in 4b (~20 lines).

### 4e. Curation track — out of scope for this endpoint

UI's `pipelineStatus.curation` (design/tags/outlier_review/batch_decision/audit)
has zero overlap with `PipelineStatusValueObject`. These are separate
curation concerns surfaced by other Gemma endpoints (experimental design,
characteristics/tags, outlier patches, audit reports).

Recommendation: **drop `curation` from the `PipelineStatus`-derived
component path entirely.** The UI adapter sets `curation = undefined` when
sourcing from Gemma; `PipelinePanel.tsx` already handles `curation ===
undefined` (renders "No curation status available."). A follow-up task
should wire each curation row to its real source endpoint individually
(separate audit recommended; not pipeline-status's job).

Effort: trivial in the adapter (`curation: undefined`). The follow-up
curation-track wiring is a larger piece of work — out of this audit's scope.

### 4f. GEEQ flattening — UI changes

UI declares top-level `geeq_quality` / `geeq_suitability`. Java nests the
full `GeeqValueObject` under `geeq`.

The panel doesn't actually read these top-level fields — it uses a
separate `useGeeq(experimentId)` query. The fields exist only because the
mock typed them. Recommendation: drop both from `ExperimentPipelineStatus`
in the UI types; let `useGeeq` keep doing its thing.

Effort: 2 lines of TS.

### 4g. `candidate_provenance` — UI-only, drop or keep optional

This is pre-Gemma screening metadata that lives in the curation-UI's mock
candidate registry. Gemma has no equivalent.

Recommendation: keep `candidate_provenance?: ... | null` typed as `null`
when sourcing from Gemma. The screening workflow doesn't go through
`/pipeline-status`. The UI adapter sets it to `null`.

Effort: 1 line.

## 5. Estimated total effort

| Bucket | Owner | Effort |
|---|---|---|
| 4a — `@JsonProperty` wire-renames + unit test | Java | ~30 min |
| 4b — UI adapter (steps → analysis map + vocab normalize) | UI | ~1 h |
| 4c — status vocabulary normalization | UI | (bundled in 4b) |
| 4d — diagnostics collapse | UI | (bundled in 4b) |
| 4e — drop `curation` from this codepath | UI | trivial |
| 4f — drop `geeq_quality`/`geeq_suitability` from `ExperimentPipelineStatus` | UI | 2 min |
| 4g — `candidate_provenance: null` | UI | 1 min |

**Total: ~2 hours**, split ~30 min Java + ~90 min UI. No DB work, no Spring
config, no behavioural change to the handler — just a wire-key rename pass
on the Java VO and a shape-adapter on the UI fetch path.

Recommended order:

1. Java wire-rename PR (small, isolated, easy to review).
2. UI adapter PR consuming the renamed shape (replaces the FastAPI mock as
   the data source for the pipeline panel).
3. Separate follow-up: wire the UI's curation track to real Gemma endpoints
   (out of scope here).
