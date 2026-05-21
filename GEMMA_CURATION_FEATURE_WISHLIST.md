# Gemma curation-agents — feature wishlist

Recce of `~/Dev/gemma-curation-agents/` against the Java Gemma backend
(`phase2-acl-migrate` @ `f7b764de45`). Forward-looking companion to the
two TODO docs already maintained in the agents repo
(`TODO-gemma-api.md` — curation read/write, and `TODO-gemma-api-2.md` —
workflow management). Those docs are the spec; this doc is the strategic
map, plus the gaps that don't live in either spec yet.

Cross-reference of memory consulted:

* `~/.claude/projects/-Users-pzoot-Dev-gemma-curation-agents/memory/workflow_management_vision.md` —
  the 14-step pipeline + what the old ExtJS dataset manager covers.
* `~/.claude/projects/-Users-pzoot-Dev-gemma-curation-agents/memory/gemma_ecosystem.md` —
  the seven-repo orbit and where the old UI lives.
* `~/.claude/projects/-Users-pzoot-Dev-eclipseworkspace-Gemma/memory/project_heatmap_rewrite_to_client.md` —
  the "server ships raw, client orchestrates" preference; same pattern
  applies to design + workflow surfaces.
* This repo's `CLAUDE.md` — Phase 2/3 ACL-migrate gates, audit-migration
  Phase C annotations (`@Audited`, `@AuditedConditional`,
  `@AuditedOnError`).

---

## 1. Executive summary

`gemma-curation-agents` is the Python pipeline that proposes (factors,
factor-values, statements, sample assignments, experiment tags) and
audits (post-hoc judges on already-curated experiments) Gemma metadata.
A separate React UI (`gemma-curation-ui`) renders the same shapes for
curator review. Today the agents and the UI both run against a local
FastAPI mock (`gemma_curation_agents/local_api/`) that fakes every
write surface real Gemma is missing — proposals, audit reports,
audit-dispositions, calibration batches, curator notes, workflow
groups, pipeline-task dispatch. The mock is the wire spec the real
Gemma needs to grow into. Until it does, **the new UI is a read-only
inspector against production Gemma and only round-trips against the
local mock.**

The blocking gaps fall into three tiers. (1) The single biggest is
write surfaces: there's no REST path to push a curator-edited Design,
no place to post a curation proposal, no place to record an
audit-finding disposition. (2) The next tier is workflow management:
no REST surface for the per-experiment pipeline-step status that the
legacy ExtJS dataset manager surfaced via DWR, no dispatch endpoints
to trigger preprocess / batch-info / DEA / PCA from a non-DWR client,
no async-task polling. (3) The third tier is read-side data shape
improvements that exist but cost extra roundtrips today: full Design
GET (today reconstructed from `/samples`), audit-trail GET (recently
landed in `hotfix-1.32.7`), publications GET, public/private state
on `EE`, cell-type subset structure for single-cell experiments, and
the cluster of bugs in the existing `/annotations/search` gene-search
path. Most of these have existing Java service implementations — the
gap is HTTP/REST surfacing, not new persistence logic.

## 2. Current state of the integration

### What `gemma-curation-agents` does today

End-to-end pipeline (`gemma_curation_agents/agents/`):

* **scrape_screen** — filters GEO TSV with deterministic rules + a
  YAML project filter + an Anthropic Haiku LLM screen, then compares
  results against existing Gemma curation. Read-only; uses gemmapy
  + direct REST for membership lookups.
* **curation_proposer** — for an experiment already loaded into Gemma
  as a "skeleton" (no design yet), proposes factors, FVs, statements,
  tags, sample assignments. Submits to the mock's
  `POST /rest/v2/datasets/{id}/curation-proposals`.
* **audit** — for an already-curated experiment, runs six judges
  (three deterministic, three LLM) and emits structured
  `AuditFinding` records. Submits to the mock's
  `POST /rest/v2/datasets/{id}/audits`. Curators triage dispositions
  via `PATCH /audits/{id}` and close via `POST /audits/{id}/finalize`.
* **pub_finder** — given a GEO accession with no linked publication,
  searches PubMed + PMC + Europe PMC preprints via six strategies in
  decreasing-confidence order. Returns candidate PMIDs / DOIs.
* **find_term** — free-text → ontology URI candidates via Gemma's
  `/annotations/search` (Phase 1); Phase 2 (direct OBO / OLS) and
  Phase 3 (LLM rerank) flagged as TODO in the source.

### What Gemma already supports cleanly (used today against production)

* `GET /datasets`, `GET /datasets/{id}` — single + listing with
  structured `filter=` expressions. `in (...)` operator for batched
  accession lookups.
* `GET /datasets/{id}/samples` — biomaterials + factor values + per-
  sample characteristics. Tolerable shape; some fields drop on the
  gemmapy DataFrame path (see §A1).
* `GET /datasets/{id}/annotations` — experiment-level tags + evidence
  codes.
* `GET /annotations/search`, `GET /annotations/term` — typeahead +
  URI → term lookup. The endpoints exist and largely work; the gene-
  search path within `annotations/search` is the source of seven
  open improvements (see §D).
* `GET /datasets/categories` — canonical EFC list (landed 2026-05-06,
  commit `e8cfb24976`).
* `GET /datasets/{id}/auditEvents` — landed in staging
  (`hotfix-1.32.7`, commit `dbbb36f9f4`). UI History tab can now
  read this from real Gemma.
* `GET/PUT /datasets/{id}/curationDetails` — landed in
  `hotfix-1.32.7` (commit `996d1ce27c`). Troubled / needs-attention
  / curation-note round-trip works.
* `GET /search?resultTypes=…&taxon=…` — gene search with taxon hint;
  partially working (see §D for the seven open improvements).

### What gemma-curation-agents currently fakes via the local mock

The mock owns these wire shapes today (FastAPI under
`gemma_curation_agents/local_api/`):

| Surface | Mock owns | Real Gemma has |
|---|---|---|
| Curation proposals (POST/GET/PATCH) | yes | no |
| Audit reports (POST/GET) | yes | no |
| Audit dispositions (PATCH per-finding, finalize, reopen) | yes | no |
| Curation groups (curator-managed lists) | yes | no |
| Calibration batches (eval imports) | yes | no |
| Whole-Design PUT | yes | partial (`curationDetails` only) |
| Curator notes scratchpad | yes | uses `curationDetails.curationNote` |
| Per-experiment pipeline-status snapshot | yes | data exists; no REST |
| Async task polling | yes (synthetic) | DWR only |
| Pre-publication checklist computed server-side | yes | no |
| Categories enum (`/categories`) | yes | yes (landed) |
| Annotation usage counts (`usageCount`) | yes | yes (landed) |
| Predicate vocabulary | yes (hand-curated from Confluence) | no |
| Gene-symbol → NCBI_GENE URI | yes (via NcbiGeneResolver) | partial (G1-G7 needed) |

The full mock surface is the design contract real Gemma needs to grow.

---

## 3. Wishlist by category

### A. Read access — fat VOs and batch endpoints

Each item shows: Python caller, current workaround, proposed Gemma
addition, effort estimate (S/M/L), and blocking impact.

#### A1. gemmapy DataFrame strips fields the REST endpoint carries

* **Callers**: `shared/gemma.py:get_dataset_annotations` (drops
  `evidenceCode`), `:get_dataset_factor_facts` (drops
  `experimentalFactorType`, `measurement.value`),
  `:get_dataset_samples_raw` (drops statement `objectUri`).
* **Workaround**: `shared/gemma.py` reimplements three calls against
  raw REST + `requests`; ~600 LOC of projection code that mirrors
  gemmapy's column shape.
* **Proposed**: fix gemmapy upstream (`TODO-gemma-api.md` §20–§21);
  not a Gemma REST gap. Track as gemmapy issue. Sidecar: when these
  ship, the agents repo can drop ~600 LOC of REST projection code.
* **Effort**: S (gemmapy-side).
* **Blocks**: nothing in production today — the workaround works —
  but every new field Gemma adds to the REST surface risks being
  silently dropped on the gemmapy frame. A continuous-integration
  check on gemmapy that asserts wire-field → DataFrame-column parity
  would be the durable fix.

#### A2. Dataset Design GET endpoint

* **Callers**: `agents/curation_proposer/skeleton.py:fetch_skeleton`
  reconstructs factors / FVs / statements from
  `get_dataset_samples`'s nested `sample_factor_values` frame.
  Statement IDs are not stable across reads.
* **Workaround**: post-process the per-sample FV rows into a Design
  graph; every PUT becomes whole-design replacement instead of patch.
* **Proposed**: `GET /rest/v2/datasets/{id}/design` returning the full
  `Design` (factors + FVs + statements with stable IDs +
  biomaterial→FV assignments + tags + external source + publications).
  Mirror of the PUT in `TODO-gemma-api.md` §4.
* **Effort**: M. Java services already serve this internally; gap is
  HTTP serialization.
* **Blocks**: clean patch-style updates; whole-design replacement is
  the only contract available today, which forces optimistic
  concurrency to do whole-body diffing.

#### A3. Full Design + Cell-type subsets for single-cell experiments

* **Caller**: `skeleton.py` cannot reconstruct the
  bioassay → cell-type-subset mapping from `/samples` alone.
* **Workaround**: none usable; curators inspect single-cell datasets
  in the legacy Gemma UI.
* **Proposed**: cell-type assignment as a structured field on
  per-bioassay rows, or a sibling `GET /datasets/{id}/cellTypeSubsets`
  endpoint. See `TODO-gemma-api.md` §16.
* **Effort**: M.
* **Blocks**: the new UI's sc-table view; was filed by Paul as a
  blocker for curator review of single-cell datasets.

#### A4. Pipeline-step status snapshot

* **Caller**: workflow-management UI (`gemma-curation-ui`) wants a
  per-experiment "where in the 14-step pipeline is this" snapshot.
* **Workaround**: cobble together from the mock's
  `/datasets/{id}/pipeline-status` (today synthetic, not backed by
  real audit events).
* **Proposed**: `GET /rest/v2/datasets/{id}/pipeline-status` returning
  per-step `{state, last_run, event_type, message}` for the six steps
  (batch_info, preprocess, pca, dea, coexpression, missing_value).
  Most fields already exist on `ExpressionExperimentDetailsValueObject`
  as `dateBatchFetch` / `batchFetchEventType` / etc.; this is
  serialization. See `TODO-gemma-api-2.md` §1.
* **Effort**: S (serialization only).
* **Blocks**: workflow dashboard UI; pre-publish checklist
  (§E1) depends on this.

#### A5. Publications / BibliographicReference

* **Caller**: `local_api/import_from_gemma._extract_publications_from_description`
  scrapes PMIDs / DOIs from EE description text.
* **Workaround**: regex scrape; misses author/title/year; misses
  non-GEO experiments entirely.
* **Proposed**: `GET /datasets/{id}/publications` returning a
  `BibliographicReferenceValueObject` list. Java SDK already has the
  DTO class — `gemmapy` has no method. See `TODO-gemma-api.md` §13.
* **Effort**: S–M.
* **Blocks**: Overview-tab "Publications" card in the new UI;
  pub_finder can't reconcile its proposed PMID against the existing
  primary-publication link today (so re-adding the same PMID is a
  no-op vs duplicate, and the UI can't tell).

#### A6. Public/private state on `ExpressionExperimentValueObject`

* **Caller**: `gemma-curation-ui` Banner wants to warn "this
  experiment is currently public — consider making it private before
  substantial edits" per the Confluence workflow.
* **Workaround**: TopBar shows hardcoded `status="private"` — no
  signal driver.
* **Proposed**: `isPublic` (or `securityLevel`) on EE VO. ACL data
  exists; gap is exposure. See `TODO-gemma-api.md` §14.
* **Effort**: S.
* **Blocks**: edit-safety banner; pairs with the §B6 toggle.

#### A7. Diagnostic plots + outlier-prediction list

* **Caller**: Diagnostics tab in `gemma-curation-ui`.
* **Workaround**: Diagnostics tab is mostly inert; curator hops to
  legacy Gemma. Predicted-outliers list is unobtainable from REST.
* **Proposed**:
  - `GET /datasets/{id}/diagnostics/{plot_kind}` → image bytes for
    `sampleCorrelation` / `pcaScree` / `pcaFactor` / `meanVariance` /
    `pvalDistribution`.
  - `GET /datasets/{id}/diagnostics/outliers` → JSON list of
    `{sampleId, score, predicted}`.
* **Effort**: M.
* **Blocks**: in-UI diagnostics review; lower priority (curators do
  this in legacy Gemma today). See `TODO-gemma-api.md` §11.

#### A8. Curator user roles + permissions

* **Caller**: `gemma-curation-ui` shows every action regardless of
  role.
* **Workaround**: none; everyone sees "publish" / "mark unusable" /
  "edit anything".
* **Proposed**: extend `/users/me` with `roles[]` and `permissions{}`.
  The Spring Security session already carries this. See
  `TODO-gemma-api.md` §12.
* **Effort**: S.
* **Blocks**: multi-curator deployments; not blocking solo work.

#### A9. GEEQ subscores + manual-override read

* **Caller**: GEEQ subscore panel; `audit/pipeline.py` GEEQ-aware
  checks.
* **Workaround**: scoop subscores from
  `ExpressionExperimentValueObject.geeq` blob; field naming churn-y.
* **Proposed**: dedicated `GET /datasets/{id}/geeq` returning all
  subscores + manual-override flags. See `TODO-gemma-api-2.md` §3.
* **Effort**: S.
* **Blocks**: GEEQ subscore display in the new UI; data exists, gap
  is shape.

#### A10. Skeleton-fetching as a single fat call

* **Caller**: `agents/curation_proposer/skeleton.py:fetch_skeleton`
  currently issues 5+ REST calls per experiment: `/datasets/{id}` +
  `/samples` + `/annotations` + `/quantitationTypes` + a GEO fetch.
* **Workaround**: parallel `concurrent.futures` calls when caller
  asks for many skeletons; per-call overhead is real (each call ~200
  ms when warm).
* **Proposed**: `GET /datasets/{id}/skeleton` returning the union in
  one body — biomaterials, characteristics, factor values, tags,
  quantitation types, publications, pipeline status. Server-side
  fan-out, single response. Equivalent to the existing per-
  bioassay loadAsMap batching pattern (see perf-probe-round3 commits
  `352118e781` / `3c1a41fd21` in this repo's recent history).
* **Effort**: M.
* **Blocks**: not strictly blocking — the parallel-call workaround is
  fine — but a high-impact perf win for batched proposer runs
  (proposer-eval runs 50 experiments / minute today; a fat endpoint
  would 3-5x that). Maps cleanly onto the existing batch-loadAsMap
  Hibernate work.

### B. Write access — idempotent ops, batch-writes, OCC

#### B1. Whole-design PUT

* **Caller**: every curator-commit in `gemma-curation-ui`; agent
  submitter (post-approval).
* **Workaround**: mock owns `PUT /datasets/{id}/design`.
* **Proposed**: `PUT /rest/v2/datasets/{id}/design ← Design`.
  Whole-body replacement is the simplest contract; matches the UI's
  single Commit button. See `TODO-gemma-api.md` §4. The endpoint
  should also accept biomaterial-name edits, biomaterial
  characteristics, EE-level metadata (`title`, `description`,
  `shortName`), publication links, external-source pointer — or
  this endpoint is the union and fans out server-side to the
  appropriate `*Service` calls.
* **Effort**: L. This is the keystone.
* **Blocks**: production rollout of the new UI. Without this the UI
  is read-only.

#### B2. ETag / If-Match concurrency on writes

* **Caller**: every Design + CurationDetails PUT.
* **Workaround**: none; mock single-threads, last write wins
  silently.
* **Proposed**: `If-Match: <etag>` header on writes, `ETag:` on
  reads, 412 on mismatch (or 409 with the latest body so the UI
  can three-way merge). Etag = id of the latest
  `ExperimentalDesignUpdatedEvent` (cheapest) or a hash of the
  design body. See `TODO-gemma-api.md` §10.
* **Effort**: S (standard HTTP, no exotic logic).
* **Blocks**: multi-curator edits + agent ↔ curator races; invisible
  in solo work.

#### B3. Curation proposals queue

* **Caller**: `agents/curation_proposer/submitter.py`,
  `agents/audit/submitter.py`.
* **Workaround**: mock owns `POST /datasets/{id}/curation-proposals`
  + `GET /curation-proposals` + `PATCH /curation-proposals/{pid}`.
  All proposals live in `local_curation.sqlite`; when the agent
  points at real Gemma, the queue is lost.
* **Proposed**: first-class proposal resource on Gemma — full schema
  in `TODO-gemma-api.md` §5. The Pydantic shapes in
  `agents/curation_proposer/schemas.py` are the wire spec (with
  `ConfigDict(extra="ignore")`, camelCase aliases). State machine:
  `pending` → `{accepted, rejected, needs_changes}` with iterative
  re-submission re-opening.
* **Effort**: L. Net-new persistence + audit-event integration. The
  new `@Audited` / `@AuditedConditional` / `@AuditedOnError`
  annotations on `phase2-acl-migrate` are the right tool for
  recording `ProposalAcceptedEvent` / `ProposalRejectedEvent` /
  `ProposalNeedsChangesEvent` (audit events the agents repo already
  emits on its mock-side as of `0.4.0`).
* **Blocks**: the entire agent ↔ curator workflow loop. Without this,
  proposers run against production reads but their output never
  reaches the curator.

#### B4. Audit reports + dispositions

* **Caller**: `agents/audit/submitter.py` and
  `agents/audit/disposition_sink.py`. Curator UI's audit tab.
* **Workaround**: mock owns the entire surface
  (`POST /datasets/{id}/audits`, `GET /audits/{audit_id}`,
  `PATCH /audits/{audit_id}` per-finding,
  `POST /audits/{audit_id}/finalize`,
  `POST /audits/{audit_id}/reopen`).
* **Proposed**: first-class audit resource — same shape as the mock.
  Audit reports are append-only; dispositions are per-finding state
  carrying disposition (`accepted`, `dismissed`, `needs_more_info`,
  `accepted+resolved`), reviewer, reviewed_at, optional fix notes.
  Finalize gates the report into the
  `eval_analysis.audit_dispositions` aggregation.
* **Effort**: L. Net-new. Could land as a separate sub-resource
  alongside `/curation-proposals`.
* **Blocks**: closing the curator-feedback loop that retrains the
  agent's prompts. Solo eval against production reads works today
  via the mock; production deployment requires this.

#### B5. Quantitation Type editing

* **Caller**: pre-publish checklist QT items in `gemma-curation-ui`.
* **Workaround**: read-only QT view from gemmapy's raw call.
* **Proposed**: `PATCH /rest/v2/datasets/{id}/quantitationTypes/{qtId}`
  accepting a partial `QuantitationTypeValueObject`. Atomic preferred-
  flag swap (setting Pref on one clears it on others within the same
  vector type). See `TODO-gemma-api.md` §6 + `TODO-gemma-api-2.md`
  §4b.
* **Effort**: S–M.
* **Blocks**: pre-publish checklist; lower priority than B1.

#### B6. Public/private toggle

* **Caller**: publish button in `gemma-curation-ui`'s banner.
* **Workaround**: disabled with "real publish endpoint not yet
  wired".
* **Proposed**: `PUT /datasets/{id}/visibility { "public": true }` or
  dedicated `POST /datasets/{id}/makePublic` /`/makePrivate`. ACL
  layer already gates. See `TODO-gemma-api.md` §14b +
  `TODO-gemma-api-2.md` §4c.
* **Effort**: S.
* **Blocks**: workflow step 14 (the final publish action).

#### B7. Outlier management

* **Caller**: diagnostics tab in `gemma-curation-ui`.
* **Workaround**: none.
* **Proposed**: `PUT /datasets/{id}/samples/{sampleId}/outlier
  { "outlier": true }`. Replaces DWR
  `BioAssayController.markOutlier`. See `TODO-gemma-api-2.md` §4a.
* **Effort**: S.
* **Blocks**: in-UI outlier review; workflow step 7.

#### B8. Pipeline-step dispatch + async task polling

* **Caller**: workflow-management UI; the agents repo's audit
  pipeline currently can't trigger a fresh DEA on a flagged
  experiment.
* **Workaround**: DWR-only today. Mock returns synthetic task IDs
  that never complete.
* **Proposed**: five `POST` endpoints (preprocess / preprocess
  diagnostics / batch-info / DEA / DEA-redo) + `DELETE` for DEA
  removal, all returning a `task_id`. `GET /tasks/{taskId}` for
  polling. Wraps existing `TaskRunningService.submit*` calls. See
  `TODO-gemma-api-2.md` §2.
* **Effort**: M. Most of the work is wrapping the existing
  `ProgressStatusController.getProgressStatus` in a REST shape.
* **Blocks**: workflow-management UI can't drive pipeline steps
  from the new UI without it.

#### B9. Structured detail JSON on commit audit events

* **Caller**: History-tab badge rendering in `gemma-curation-ui`.
* **Workaround**: mock stamps JSON into `audit_event.detail` at PUT
  /design time.
* **Proposed**: spec in `TODO-gemma-api.md` §18 — v1 JSON envelope
  with `shape` + `delta` + `proposal_id` + `validator_ok` +
  `checklist` snapshot. Lets the History tab render rich badges
  without scraping free-text `note`.
* **Effort**: S–M. The new `@Audited` family on `phase2-acl-migrate`
  is the right hook — `messageSpel` can emit the JSON.
* **Blocks**: rich History tab; downstream "how many commits used
  the proposer" analytics. Lower priority than B1 itself.

### C. Workflow / state — proposal lifecycle, curator-attention surface

These overlap with section B but are scoped to the workflow-level
abstractions on top of individual write endpoints.

#### C1. Curation groups (curator-managed experiment lists)

* **Caller**: `gemma-curation-ui` workflow dashboard.
* **Workaround**: mock owns full CRUD at `/rest/v2/curation-groups`
  + `/rest/v2/groups/*` per `test_groups_for_experiment.py`. Storage
  in `local_curation.sqlite`.
* **Proposed**: first-class CurationGroup entity (option 1 in
  `TODO-gemma-api-2.md` §5) OR live only in the curation-agents layer
  (option 3, current). The agents-side mock includes typed groups —
  `screening / pipeline / review` — that the curator uses for
  triage queues. Real Gemma probably doesn't need to own this; a
  hosted curation-service (the agents repo's local server upgraded
  to a real deployment) is the cleaner home.
* **Effort**: L if Gemma-side; M if dedicated curation service.
* **Blocks**: high UX value — curators can't track "the 12
  experiments I'm working on this week" without it. Not blocking
  solo work via the mock.

#### C2. Calibration batches (eval imports)

* **Caller**: `agents/curation_proposer/eval/` + the eval repo
  (`gemma-curation-agents-eval`).
* **Workaround**: mock owns `POST /calibration-batches/import`.
* **Proposed**: not necessarily Gemma's responsibility. Like C1, this
  belongs in a hosted curation-service.
* **Effort**: -- (out of scope for Gemma core).

#### C3. "Needs curator attention" surface

* **Caller**: workflow dashboard wants a roll-up view of "which
  experiments have open audit findings", "which have proposed-but-
  unreviewed proposals", "which have failed DEA", "which have
  troubled status".
* **Workaround**: aggregate client-side from multiple `GET
  /datasets?filter=...` calls.
* **Proposed**: `GET /datasets?attentionState=open` or a sibling
  `GET /datasets/attention` returning a slim
  `{id, accession, attention_reasons[]}` list. Server-side
  aggregation avoids N+1 round-trips when the dashboard renders.
* **Effort**: M.
* **Blocks**: workflow dashboard perf at scale; not blocking
  functional work.

### D. Search + discovery — ontology, similarity, query-by-example

#### D1. Gene-symbol search hardening (G1-G7)

* **Caller**: `ontology/gemma_gene_resolver.py` (the gene-route
  resolver feeding the proposer's genotype-FV subject URIs).
* **Workaround**: client-side preprocessing of mutant suffixes,
  parens taxon hints, slash-aliases, bracket-form, plus source-name
  filtering (only commit on the top-four `source` values). Documented
  in detail at `docs/GEMMA_GENE_SEARCH_TODO.md` in the agents repo.
* **Proposed** (seven items, in priority order):
  1. **G1**: `?taxon=` as a HARD filter, not a soft hint.
  2. **G2**: structured `matchTier` field in search responses so
     clients don't pattern-match on Java service names.
  3. **G3**: suppress `getGOGroupGenes` for short / non-gene queries.
  4. **G4**: word-stem / prefix tolerance on `findByOfficialName`.
  5. **G5**: camelCase / case-boundary tokenizer split (so `Zfp281KO`
     finds Zfp281).
  6. **G6**: minimum-relevance threshold on full-text matches
     (kills the `COVID-19` → SNORD-19 false-positive class).
  7. **G7**: bracket-form `Symbol [taxon] description` parse.
* **Effort**: S each (the full G1-G7 set is M–L total).
* **Blocks**: ~1,000 of the 4,877-case gene-route eval slice fall
  into the `short_descriptive` bucket that today abstains unrecoverably.
  Sequential server fixes obsolete most of the client preprocessing.

#### D2. Direct URI → ontology-term lookup — landed (`58b5f9e032`)

* Listed for completeness. `TODO-gemma-api.md` §17 — `getAnnotationTerm`
  endpoint shipped. `GemmaResolver.lookup_by_uri` uses it. No
  remaining work on Gemma's side.

#### D3. Ontology hierarchy traversal beyond term lookup

* **Caller**: `ontology/` for parent/child chasing during URI
  validation; `find_term/finder.py` Phase 2 plan (TODO in source).
* **Workaround**: `get_annotation_parents` /
  `get_annotation_children` work; out-of-Gemma OBO downloads for
  things Gemma doesn't have loaded.
* **Proposed**: probably nothing — Gemma's `OntologyService` already
  exposes this. The agent's local FAISS / BM25 indexes
  (`~/Data/ontology_index/`) are a different question scoped to the
  agent's resolver, not Gemma.
* **Effort**: -- (out of scope).

#### D4. Predicate vocabulary endpoint

* **Caller**: agent prompt (`design_constants.PREDICATES`), UI
  StatementEditor, validator (`KNOWN_PREDICATE_URIS`) — three
  hand-curated copies of the same ~25 entries from Confluence.
* **Workaround**: synchronized copies, no source of truth in Gemma.
* **Proposed**: `GET /predicates` returning the allow-list. See
  `TODO-gemma-api.md` §7. Trivial if served from a checked-in TGEMO
  resource file.
* **Effort**: S.
* **Blocks**: occasionally bites (six wrong URIs found in the
  Apr 2026 audit). Low impact but cheap.

#### D5. Free-text dataset typeahead

* **Caller**: "Import from Gemma" form in `gemma-curation-ui`.
* **Workaround**: mock proxies through `gemmapy.get_datasets(query=…)`.
  Works.
* **Proposed**: `GET /datasets/search` (typeahead) returning a slim
  `{id, accession, short_name, title, taxon, n_samples}` shape, OR a
  `q=` parameter on `GET /datasets`. See `TODO-gemma-api.md` §9.
* **Effort**: S.
* **Blocks**: nothing.

#### D6. Similar-prior-curations endpoint

* **Caller**: curator-side "show me what other experiments have used
  this FV / tag" lookup; the proposer's calibration-pool sampler.
* **Workaround**: filter on `/datasets` by characteristic URI;
  multiple calls, fragile.
* **Proposed**: `GET /annotations/{uri}/datasets` returning the EE
  ids that carry the given (URI, category). Or `usageCount`'s siblings
  — `usageExamples[<dataset_ids>]`.
* **Effort**: M.
* **Blocks**: not blocking — but unlocks the "previously-used FV
  catalogue" feature in the curator UI.

### E. Cross-cutting — dry-run, change-streams, atomic batches

#### E1. Pre-publication checklist endpoint

* **Caller**: pre-publish checklist in `gemma-curation-ui`.
* **Workaround**: assembled client-side from 6+ endpoint calls.
* **Proposed**: `GET /datasets/{id}/checklist` returning per-item
  pass/warn/fail computed server-side. See `TODO-gemma-api-2.md` §6.
* **Effort**: M. Each item maps to existing computable predicates.
* **Blocks**: nothing — but consolidates the UI's 6 calls into 1.

#### E2. Dry-run mode on writes

* **Caller**: agents pre-commit validation; UI "preview commit"
  affordance.
* **Workaround**: none.
* **Proposed**: every Design / curationDetails PUT accepts a
  `?dryRun=true` query param. Returns what would have changed
  (diff body + audit-event preview) without persisting. Mirrors
  the dry-run mode the agent's pipeline already runs internally.
* **Effort**: S.
* **Blocks**: nothing. Quality-of-life for testing curator workflows
  against production reads.

#### E3. Atomic batch writes

* **Caller**: `agents/audit/disposition_sink.py` writes one PATCH per
  finding when finalizing an audit. Calibration-batch imports write
  N proposals + N audits in sequence.
* **Workaround**: sequential PATCH per finding; partial-failure
  recovery in the agent.
* **Proposed**: `PATCH /audits/{id}` accepts an array of disposition
  updates atomically (already supported by the mock). Generalized
  shape: `POST /batch` accepting an array of operations, all-or-nothing.
* **Effort**: M.
* **Blocks**: not blocking; reduces N+1 traffic.

#### E4. Change streams / SSE for collaborative curation

* **Caller**: `gemma-curation-ui` proposal-stream view; multi-curator
  awareness ("Paul is currently editing the design of EE 89342").
* **Workaround**: mock has `proposeStream` SSE channel for proposer
  progress events; no equivalent on real Gemma.
* **Proposed**: SSE channel for per-experiment commit events
  (`ExperimentalDesignUpdatedEvent`, `CurationNoteUpdateEvent`,
  proposal-status transitions). Subscription form
  `GET /datasets/{id}/events?sinceEventId=...`. Server-Sent Events
  fits naturally with the existing audit-event model.
* **Effort**: M–L.
* **Blocks**: not blocking; unlocks real-time collaboration. Defer
  until multi-curator workload is real.

#### E5. Provenance metadata on every write

* **Caller**: agents repo's `agent_version` + paper-provenance
  stamping (every proposal carries
  `evidence.extra.paper_provenance`).
* **Workaround**: mock stores the provenance blob as part of the
  proposal body.
* **Proposed**: Gemma's write endpoints (B1, B3) accept an
  `X-Provenance: <blob>` header or a `provenance` field that lands
  in the audit-event detail. Lets "what did the agent see" be a
  one-query answer against real Gemma. Pairs with B9.
* **Effort**: S.
* **Blocks**: nothing. Forensic / reproducibility value.

---

## 4. Top 10 prioritized

1. **B1 — Whole-design PUT** (`/datasets/{id}/design`). Without this
   the new UI is read-only against production. Unblocks the entire
   write side. Effort L.
2. **B3 — Curation proposals queue**. The agent-curator loop's
   on-ramp; the mock's full shape is the spec. Effort L.
3. **B4 — Audit reports + dispositions**. The agent-curator loop's
   off-ramp (feedback-back-to-prompt-tuning). Effort L.
4. **A4 + B8 — Pipeline status + dispatch + task polling**. Replaces
   the legacy DWR dataset-manager surface; foundation of workflow
   management. Effort S+M.
5. **D1 — Gene-search hardening (G1–G7)**. Closes ~1,000 abstain
   cases in the proposer's gene-route eval; sequential S each.
6. **A2 — Design GET endpoint**. Mirrors B1; lets clients move from
   whole-design replacement to patch-style. Effort M.
7. **A3 — Cell-type subset structure for sc**. Filed by Paul as a
   blocker for the new sc-table UI view. Effort M.
8. **A5 — Publications endpoint**. Overview-tab Publications card +
   pub_finder reconciliation. Effort S–M.
9. **A6 + B6 — Public/private state read + toggle**. Workflow step
   14; safety banner. Effort S.
10. **B2 — ETag / If-Match on writes**. Invisible until two curators
    collide; cheap, standard HTTP. Effort S.

The three L-effort items (B1, B3, B4) are the workhorses — landing
all three unblocks the new UI for production curators and closes
the agent-curator feedback loop. The rest is incremental.

---

## 5. Out of scope

* **Embedding indexes + dense ontology retrieval** (FAISS HNSW in
  `~/Data/ontology_index/`). Lives in the agents repo; no Gemma-side
  involvement needed. The agents' resolver chain can use Gemma's
  read endpoints as one tier.
* **OBO / OLS / NCBI mirror caches**. The agents repo owns
  `~/Data/ontology_owl/`, `~/.cache/huggingface/`, and the local
  `gene_info.gz` ingest. Gemma doesn't need to mirror these.
* **Vision-LLM supplementary-PDF table extraction** (idea #3 in
  `docs/IDEAS_FROM_CONCURRENT_WORK.md`). Lives in the proposer
  pipeline as a future Stage 1 augmentation; Gemma doesn't need to
  serve supp PDFs.
* **Cross-field arbitration debate loop** (idea #2 in the same doc).
  Lives in the agent's `review_loop/`. Gemma's job is to accept the
  resulting proposal, not to orchestrate the debate.
* **Per-field LoRA adapters** (idea #7 in the same doc). Currently
  deferred entirely. Gemma plays no role.
* **Hosted curation-service infrastructure** (curator notes, audit
  dispositions, calibration batches, curation groups). Plausibly
  belongs in a hosted upgrade of the agents-repo local server — NOT
  in Gemma core. Decoupling lets the curation service iterate fast
  without coupling to Gemma's release cadence.
* **gemmapy DataFrame field-drop fixes (A1)**. Listed because it's
  a real workaround in the agents repo, but the fix is in `gemmapy`,
  not in Gemma's REST.

---

## 6. Open questions for Paul

1. **Does Gemma own the proposals / audits / curation-groups
   persistence, or does that live in a hosted curation-service** (a
   production deploy of the current agents-repo FastAPI mock)? If
   curation-service, Gemma only needs B1 (whole-design PUT) +
   provenance hooks (E5) — the L-effort B3/B4 fall out of scope for
   Gemma core. If Gemma-side, B3+B4 are net-new persistence + audit
   wiring, but the workflow stays in one place.

2. **Whole-design PUT vs. fan-out PUT.** The mock's PUT body covers
   factors / FVs / statements / tags AND biomaterial-name edits AND
   EE-level metadata AND publication links. Real Gemma's write
   surface today is split across `ExpressionExperimentService` /
   `BioMaterialService` / `BibliographicReferenceService`. Should
   the REST endpoint accept the union and fan out server-side
   (atomically), or should the UI fan out client-side across
   multiple endpoints (and lose atomicity)? The mock chooses union;
   matching that on real Gemma is a Larger Decision.

3. **Pipeline-step dispatch granularity.** `TODO-gemma-api-2.md`
   §2 enumerates 5 dispatch endpoints (preprocess / diagnostics /
   batch-info / DEA / DEA-redo). Is there appetite for a
   generalized `POST /datasets/{id}/run/{stepName}` instead, or
   does per-step shape matter (DEA's optional `{factor_ids,
   include_interactions, subset_factor_id}` body argues yes)?

---

## 7. References

* `gemma-curation-agents/TODO-gemma-api.md` — the canonical
  curation-data spec (22 sections). This wishlist references it
  throughout.
* `gemma-curation-agents/TODO-gemma-api-2.md` — the workflow-
  management spec (6 sections).
* `gemma-curation-agents/docs/GEMMA_GENE_SEARCH_TODO.md` — G1–G7
  gene-search improvements (§D1).
* `gemma-curation-agents/docs/IDEAS_FROM_CONCURRENT_WORK.md` — what
  to borrow from Mondal / Mittal / Hak; informs §5 out-of-scope.
* `gemma-curation-agents/README.md` — pipeline architecture.
* `gemma-curation-ui/CROSS_REPO_COMPAT.md` (sibling repo) — the
  agent ↔ UI wire-shape source of truth.
* `~/.claude/projects/-Users-pzoot-Dev-gemma-curation-agents/memory/workflow_management_vision.md` —
  the 14-step pipeline + what the old ExtJS dataset manager covers.
