# Gemma UI feature catalog — what's needed to fully replace gemma-web

Date: 2026-05-21
Branch: `gemma-ui-feature-catalog`
Baseline: `59d9ffbf34`

This is a feature-level inventory (not URL-level — sister agent
`gemma-ui-endpoint-gap` does that). Each section names a feature area,
describes what gemma-web does today, lists the REST surface available
or missing, and notes Paul-flagged priority + dubious bits.

Sources surveyed:
- `gemma-web/src/main/java/.../web/controller/*` — 47 Spring MVC + DWR
  controllers
- `gemma-web/src/main/webapp/pages/*.jsp` — 47 user-facing pages
- `gemma-rest/src/main/java/ubic/gemma/rest/*` — 15 JAX-RS resources
- `gemma-curation-agents/gemma_curation_agents/local_api/*` — FastAPI
  mock (server.py = 31 routes, workflow_routes.py = 32 routes)
- `gemma-ui/apps/{browser,curation}/src/features/*` — 23 React feature
  folders
- Confluence curation guide `How-to-Curate-an-Experiment` — partial
  (only top-level headings render; subheadings recovered via raw
  text scrape)

---

## Section A — Currently in gemma-ui (don't re-implement)

### browser app (public side)
- `about` — static about page
- `browser` — DatasetManager (Paul: already handled)
- `dataset` — dataset detail view (incomplete per Paul)
- `gene` — gene detail view (incomplete per Paul)
- `heatmap-demo` — visualization prototype
- `home` — landing page
- `mcp` — MCP integration
- `platforms` — array design browse
- `shared` — common UI primitives

### curation app
- `audit` — audit report viewer (FastAPI-backed, AI workflow)
- `auth` — login/logout/me
- `design` — experimental design editor
- `diagnostics` — pipeline diagnostics surface
- `experiment` — experiment shell
- `history` — audit history viewer
- `inbox` — curator queue (FastAPI proposal-backed)
- `landing` — curation landing
- `notes` — curator notes
- `overview` — experiment overview
- `proposal` — AI curation proposal review
- `quantitation` — QT selection / preferred-QT
- `samples` — bioassay / biomaterial display
- `settings` — user prefs
- `workflow` — pipeline action launcher (preprocess, GEEQ, DE)

**Gap that's obvious from the folder list**: no `search`, no
`publications`, no `gene-set` / `experiment-set` management, no `admin`,
no `user-management`, no `system-monitoring`, no `arrayDesign` editor.

---

## Section B — Feature catalog

### B1. Dataset page (Paul: **incomplete, called out**)

**What gemma-web shows** (`expressionExperiment.detail.jsp` +
`ExpressionExperimentController.loadExpressionExperimentDetails`):
- header: shortName, name, description, accession + GEO link,
  primary publication
- counts: samples, factors, platforms, vectors, QT
- annotations panel (`AnnotationController.getAnnotation`)
- experimental design summary (factors + factor values), drilldown to
  `experimentalDesign.detail.jsp`
- bioassay/biomaterial tables (`bioAssays`, `bioMaterials` subviews)
- subsets (`expressionExperiment.subSets.jsp`)
- QC tab: PCA, correlation matrix, mean-variance, p-value dist,
  outliers, eigen-genes, single-cell sparsity heatmap, factor analysis
  (all served by `ExpressionExperimentQCController` as PNG endpoints)
- DEDV (differential expression detail vectors) plots
- audit-trail panel (`AuditController.getEvents`)
- curation details (needs-attention flag, troubled flag, last-updated)
- ownership/permissions panel (`SecurityController.getSecurityInfo`)
- "edit" affordances on factors, factor values, primary publication,
  basics, description
- per-platform "show experiments using this platform"
- single-cell expression data view (separate JSP)

**REST surface needed (vs available)**:
- GET `/datasets/{id}` — exists
- GET `/datasets/{id}/samples`, `/platforms`, `/publications`,
  `/auditEvents`, `/curationDetails`, `/permissions`,
  `/pipelineStatus`, `/geeq`, `/quantitationTypes` — all exist
- GET `/datasets/{id}/heatmap-data` — exists (DatasetVisualizationWS)
- GET QC images (PCA scree, corrmat, meanvar, pvalue dist, outliers,
  eigengenes, SC sparsity, factor analysis) — **missing in REST**;
  gemma-web returns PNG via `/expressionExperiment/visualize*.html`.
  Paul's "client-side heatmap rewrite" plan implies these become
  raw-data endpoints not pre-rendered images.
- GET DEDV (expression vectors with factor overlay) — partially exists
  (`/datasets/{ds}/expressions/genes/{g}`); the visualization-tuned
  shape gemma-web's DEDVController exposes is **not** in REST
- GET `/datasets/{id}/subSets`, `/subSetGroups` — exists
- GET sample correlation matrix raw — **missing**; only the PNG path
- POST update primary publication (`ExpressionExperimentController.
  updatePubMed`) — **missing in REST** (PATCH curationDetails covers
  some but not pubmed swap)
- POST `updateBasics` (short-name, name, description) — **missing**
- POST `removePrimaryPublication` — **missing**
- POST `recreateCellTypeFactor`, `deleteCellTypeAssignment`,
  `deleteCellLevelCharacteristics` (single-cell) — **missing**

**Priority**: High (Paul-flagged)
**Effort**: L — large surface; many existing endpoints, but
visualization data shapes need rework and several edit operations
have no REST equivalent.
**Dubious bits**: the per-image PNG endpoints; if heatmap-rewrite-to-
client lands, this whole batch becomes raw-data endpoints and the
server-side PNG rendering retires.

---

### B2. Gene page (Paul: **incomplete, called out**)

**What gemma-web shows** (`gene.detail.jsp` + `GeneController`):
- header: official symbol, name, NCBI ID, taxon, aliases, ensembl ID
- GO terms (`GeneController.findGOTerms`)
- gene products (`GeneController.getProducts`)
- chromosome location (gemma-rest TaxaWS exists)
- expression overview across datasets (DEDV per-gene)
- coexpression links (DWR `CoexpressionSearchController`-equivalent —
  **the legacy coexpression controllers were retired**; the data is
  still there via DEDV/expressions endpoints)
- differential expression results across all experiments
  (DEDV + `DifferentialExpressionSearchController`)
- multifunctionality score
- phenotype associations (Phenocarta — see B11)
- bibref papers citing this gene

**REST surface needed (vs available)**:
- GET `/genes/{gene}` — exists
- GET `/genes/{gene}/goTerms` — exists
- GET `/genes/{gene}/probes` — exists
- GET `/genes/{gene}/locations` — exists (also taxon-scoped)
- GET differential-expression-across-experiments for a gene —
  **partially exists** via `/datasets/{datasets}/expressions/
  differential`; the gene-centric framing (find datasets where gene
  shows DE) requires a different endpoint shape
- GET gene products — **missing in REST**
- GET coexpression for gene — **missing** (gemma-web has it via DWR;
  status unclear — Paul-input needed: is coexpression retired?)
- GET multifunctionality score — **missing**
- GET phenotype associations — **missing** (Phenocarta status)

**Priority**: High (Paul-flagged)
**Effort**: M — most simple endpoints exist; the cross-dataset DE
view needs a new gene-centric REST shape.
**Dubious bits**: coexpression — the controller was retired in
gemma-web but the page section probably still references it. Confirm
with Paul whether gene coexpression is staying.

---

### B3. Gene set / Experiment set management

**What gemma-web shows** (`geneGroupManager.jsp`,
`expressionExperimentSetManager.jsp` + the two `*SetController`s):
- list user's saved gene sets / experiment sets
- list session-scoped (anonymous) sets
- create/rename/describe/delete gene set or EE set
- edit set membership (add/remove genes or EEs by ID)
- view by id (`geneSet.detail.jsp`, `expressionExperimentSet.detail.jsp`)
- find gene sets containing a gene
- find gene sets by name
- per-set permissions (read/write/group) via SecurityController

**REST surface needed**:
- ALL of it is **missing in gemma-rest**. There is no `/geneSets`,
  no `/expressionExperimentSets` resource. The 21 DWR methods on
  `GeneSetController` and 23 on `ExpressionExperimentSetController`
  have no REST equivalent.

**Priority**: High (Paul-flagged)
**Effort**: L — fresh REST resource design + impl + UI from zero;
session-scoped (anonymous) sets add complexity (need session
attribute store).
**Dubious bits**: session-bound sets are a usability feature that
predates having logged-in users. Possibly retire and require login.
Confirm with Paul.

---

### B4. User management (Paul: called out)

**What gemma-web shows**:
- public: signup (`SignupController` + `register.jsp`), email-confirm
  registration (`confirmRegistration.html`), password-hint reset
  (`UserFormMultiActionController.resetPassword`), edit own profile
  (`personForm.jsp` + `editUser.html`), ajax-login-check
- admin: `userManager.jsp` (list all users, promote/disable),
  `activeUsers.jsp` (currently authenticated users count + names),
  group management (`manageGroups.jsp` — create groups, add/remove
  members) — wired via `SecurityController` DWR

**REST surface needed (vs available)**:
- POST `/login`, POST `/logout`, GET `/me` — exist (AuthWebService)
- GET `/users/me`, GET `/users/{username}` — exist (RootWebService)
- POST signup — **missing**
- POST confirmRegistration — **missing**
- POST resetPassword — **missing**
- PATCH `/users/{username}` (edit profile) — **missing**
- GET `/users` admin list — **missing**
- POST/PUT user status (enable/disable, role grant) — **missing**
- GET `/groups`, POST `/groups`, etc — **exist in curator-agents
  FastAPI mock** as `/rest/v2/groups...` but NOT in gemma-rest. The
  legacy DWR SecurityController has them. **Paul-input needed**:
  do the FastAPI groups routes migrate into gemma-rest, or stay
  separate?
- GET active-users count (`SecurityController.getAuthenticatedUserCount`)
  — **missing**, useful for admin dashboard

**Priority**: High (foundation for any logged-in UI)
**Effort**: M — most operations are CRUD on User/UserGroup
**Dubious bits**: the FastAPI groups vs Spring Security groups
overlap. Pick one.

---

### B5. System monitoring (Paul: called out)

**What gemma-web shows** (`systemStats.jsp` +
`SystemMonitorController` + `SystemStatsController`):
- cache status (ehcache stats per region)
- hibernate statistics (entity load/insert/update/delete counts,
  query cache hit ratio)
- clearAllCaches / clearCache(name)
- enable/disable hibernate statistics gathering
- reset hibernate stats

**REST surface (vs available)**:
- GET `/health` — exists (HealthWebService, simple liveness)
- GET `/info` — exists (InfoWebService, build info)
- GET `/metrics` — exists (MetricsWebService, Micrometer/Prometheus)
- GET cache stats — **missing**
- GET hibernate stats — **missing**
- POST clear-cache — **missing**
- POST enable/disable stats — **missing**

**Priority**: Medium (admin-only, but Paul-flagged)
**Effort**: S — wrap existing CacheManager + SessionFactory.
getStatistics() in a /admin/caches and /admin/hibernate resource.
**Dubious bits**: Prometheus `/metrics` already covers a lot. Maybe
the legacy cache/hibernate panel is redundant for an SRE-y monitoring
stack; offer it only as a "diagnostics" page, not first-class.

---

### B6. Search index refresh / Hibernate Search admin (Paul: called out)

**What gemma-web shows** (`indexer.jsp` + `IndexerTask` via DWR
`IndexService`):
- pick which entity types to reindex (EE, ArrayDesign, BioSequence,
  BibRef, ProbeSequence, Gene)
- submit async indexing job
- progress polling via `ProgressStatusController`
- a separate `reIndexOntologies.jsp` for ontology reindex

**REST surface (vs available)**:
- POST `/genes/probes/refresh` — exists (refresh gene-probe map)
- POST `/datasets/platforms/refresh` — exists (refresh dataset-
  platform mapping)
- POST `/datasets/annotations/refresh` — exists (refresh dataset
  annotations)
- POST trigger Hibernate Search reindex of EE/AD/Gene/etc —
  **missing**
- POST reindex ontologies — **missing**
- GET search index status — **missing**

**Priority**: Medium (admin-only, but Paul-flagged — search quality
depends on this firing after corpus changes)
**Effort**: S-M — `IndexerTask` already exists; wrap as
`/admin/search-index/rebuild` async task + GET status. Wire into
existing TasksWebService poll mechanism.

---

### B7. Publication search / Bibliographic browser (Paul: called out)

**What gemma-web shows** (`BibliographicReferenceController` +
`PubMedQueryController`):
- search PubMed by query string, by accession (PMID)
  (`bibRefSearch.html`)
- view all EE-linked bibrefs (`showAllEeBibRefs.html`)
- add bibref from PubMed result (`bibRefAdd.html`)
- delete bibref (`deleteBibRef.html`)
- view single bibref (`bibRefView.html`)
- list all bibrefs paginated (`bibRefList.jsp`)

**REST surface (vs available)**:
- GET `/datasets/{id}/publications` — exists
- POST/DELETE bibref — **missing**
- search bibrefs (via SearchWS resultTypes) — **possible** but not
  publication-specific
- direct PubMed proxy — **missing** (gemma-web hits PubMed E-utilities
  server-side)

**Priority**: Medium (curators need this when linking pubs to EEs;
public users want bibliographic browse less)
**Effort**: M — REST resource for `/publications` + a small PubMed
search proxy endpoint.

---

### B8. General full-text search

**What gemma-web shows** (`GeneralSearchController` +
`generalSearch.jsp`):
- single search box that hits all result types: EE, gene, ArrayDesign,
  BibRef, Characteristic, GeneSet, ExperimentSet
- termUri-based ontology search ("everything tagged with GO:0006915")
- per-type result filtering and pagination

**REST surface (vs available)**:
- GET `/search?query=...` — exists (SearchWebService); returns
  unified result envelope across types
- GET `/annotations/search/{query}/datasets` — exists (ontology-
  scoped EE search)

**Priority**: High (no global-search box in gemma-ui browser app yet)
**Effort**: S — REST endpoint is solid; build a search-results page
+ omnibox component.

---

### B9. Differential expression viewer / search

**What gemma-web shows**:
- `DifferentialExpressionAnalysisController` — admin tools: redo
  analysis, refresh stats, remove, run, runCustom (pick factors,
  include interactions, subset factor)
- `DifferentialExpressionSearchController` — search across many
  experiments for genes meeting threshold
- `DiffExMetaAnalyzerController` — meta-analysis manager
  (`metaAnalysisManager.jsp`): create meta-analysis from selected
  result sets, save, browse, delete
- `analysesResultsSearch.jsp` — heatmap-style cross-experiment
  visualization (the meta-heatmap, `metaheatmap.jsp`)

**REST surface (vs available)**:
- GET `/datasets/{ds}/analyses/differential` — exists
- POST `/datasets/{ds}/tasks/differential` — exists (run DE)
- POST `/datasets/{ds}/tasks/redo/{aid}` — exists (redo)
- GET `/datasets/{ds}/expressions/differential` — exists (cross-
  dataset DE results for given genes)
- GET `/resultSets/{rs}` — exists (AnalysisResultSetsWS)
- POST custom DE config (which factors, interactions, subset) —
  **missing in REST**; the curator-agents FastAPI has a stub
  (`POST /rest/v2/datasets/{id}/analyses/differential`) but the
  param surface is leaner than gemma-web's `runCustom`
- meta-analysis CRUD — **missing entirely in REST**

**Priority**: Medium-High (DE viewing is core; meta-analyzer is
power-user / paper-fodder)
**Effort**: M for the viewer (most REST exists); L for the meta-
analyzer (totally absent in REST)
**Dubious bits**: the meta-analyzer is rarely used. Confirm with
Paul whether it ships in 2.0 or gets retired.

---

### B10. Coexpression / sample correlation

**What gemma-web shows**:
- coexpression search by gene (the legacy CoexpressionSearchController
  was retired but the page still references it via JSP)
- per-EE sample correlation matrix (PNG via QCController)
- per-EE probe correlation distribution

**REST surface (vs available)**:
- coexpression search — **missing**
- sample correlation raw data — **missing** (only PNG)

**Priority**: Low to Medium (Paul-input needed: is coexpression
sunsetted? gemma-web JSPs still reference it but no controller)
**Effort**: M
**Dubious bits**: Probably retire. Paul confirms.

---

### B11. Phenocarta / Characteristic browser

**What gemma-web shows**:
- `CharacteristicBrowserController` (`characteristicBrowser.html`):
  browse all characteristics in DB, search by value prefix or full
  query, bulk delete/update characteristics (admin-only mass-edit
  tool)
- `OntologyController` — internal-ontology endpoints for TGEMO and
  TGFVO (Gemma's locally-published ontologies; serve RDF/OWL and
  per-term HTML pages)

**REST surface (vs available)**:
- GET `/annotations/search` — exists (find AnnotationValueObjects)
- GET `/annotations/categories` — exists
- GET `/annotations/parents`, `/children`, `/term` — exists
  (ontology nav)
- bulk mass-edit / remove characteristics — **missing**
- TGEMO + TGFVO publish endpoints — **exist in gemma-web only**
  (RDF/OWL on `/ont/TGEMO`, `/ont/TGFVO`); not in gemma-rest, but
  they don't really belong in REST either (they're served as
  content-typed RDF for external ontology consumers)

**Priority**: Medium (curator-only tool; mass-edit is dangerous)
**Effort**: M
**Dubious bits**: the public RDF/OWL ontology publish endpoints
(`/ont/*`) — should those move to a separate ontology-publish
servlet, stay in gemma-web reskinned, or get a dedicated
ontology-server submodule? Paul-input.

---

### B12. Curation queue + curator dashboard (FastAPI mock-only)

**What the FastAPI mock exposes** (`gemma-curation-agents` server.py
+ workflow_routes.py — these are NOT in gemma-rest yet, the mock IS
the gap):
- curation proposals: POST/GET/PATCH/DELETE `/curation-proposals` —
  AI-generated proposals on an experiment
- audit reports: POST/GET/PATCH `/audits`, finalize, reopen — AI-
  generated audit reports
- dataset search (`/rest/v2/datasets/search`) — gemma-rest covers
  this but with different shape
- dataset import (`/rest/v2/datasets/import`) — **missing in REST**
- screening candidates: full CRUD on `/candidates` — **missing in
  REST**
- calibration batches (import + per-experiment progress) —
  **missing in REST**
- find-term, find-publication (proposer endpoints in
  `proposer_service.py`) — AI tools, FastAPI-only by design
- streaming variants of propose/audit — AI tools

**Priority**: High for the inbox/proposal/audit features (Paul's
gemma-curation-ui app is built around them); Medium for screening
and calibration batches.
**Effort**: depends on whether these stay FastAPI or migrate.
**Dubious bits**: **architectural decision needed** — does the
curator-agents FastAPI stay as a separate microservice (recommended
since it talks to LLMs), or do the CRUD parts (proposals, audits,
candidates, calibration batches) move into gemma-rest? Paul-input.

---

### B13. Audit-event history viewer

**What gemma-web shows** (`AuditController` DWR + audit panels in
EE detail / ArrayDesign detail):
- list audit events for an entity (curator note, troubled flag,
  needs-attention, etc)
- add audit event with type + comment + detail
- annotation curators rely on this for "what was done to this EE
  and when"

**REST surface (vs available)**:
- GET `/datasets/{id}/auditEvents` — exists
- POST audit event — **missing in REST**; the AuditedAspect now
  auto-emits these for state changes, but a manual curator-note
  endpoint isn't exposed
- audit events for non-EE entities (ArrayDesign, BioMaterial,
  FactorValue) — **missing in REST**

**Priority**: Medium (curator-only)
**Effort**: S — wrap existing AuditTrailService in a generic
`/auditEvents` POST resource.

---

### B14. Annotation editing (the curator's daily driver)

**What gemma-web shows** (`AnnotationController` +
`ExperimentalDesignController`):
- on EE: add/remove characteristic (tag), update characteristic
- on FactorValue: create/update/delete characteristics; mark
  needs-attention; clear needs-attention; duplicate; delete
- on BioMaterial: update characteristics, factor value assignments
- on Factor: create/delete factor; update factor metadata;
  recreate cell-type factor; delete cell-type assignment
- on whole design: create design from file (TSV upload)
- multi-platform support: factor values across platform redesigns

**REST surface (vs available)**:
- PATCH `/datasets/{id}/curationDetails` — exists (troubled/
  needsAttention flags only)
- PUT `/rest/v2/datasets/{id}/design` — **FastAPI-only**; the design
  edit ops are the bulk of curator work, and they live in DWR
  `ExperimentalDesignController` (24 methods) with no REST mirror
- annotation add/remove on EE — **missing in REST**
- annotation candidates (`/rest/v2/annotations/search`) —
  **FastAPI mock only**

**Priority**: Very High (this is THE curator workflow)
**Effort**: XL — ~24 distinct operations, each non-trivial because
they mutate experimental design semantics. The FastAPI mock has a
single PUT for the whole design; the legacy granular API is finer
but harder to port faithfully.
**Dubious bits**: which shape wins — granular per-factor-value PATCH
(legacy) or whole-design PUT (FastAPI)? The whole-design PUT loses
optimistic-locking and audit-trail granularity. Paul-input needed.

---

### B15. Array design (Platform) management

**What gemma-web shows** (`ArrayDesignController` +
`ArrayDesignFormController` + `ArrayDesignProbeMapperController` +
`arrayDesigns.jsp`, `arrayDesign.detail.jsp`, `arrayDesign.edit.jsp`,
`arrayDesignAdd.jsp`):
- browse all array designs
- view detail: name, accession, taxon, vendor, technology type,
  probe count, gene count, sequence count, status (troubled?
  blacklisted?)
- edit: name, description, taxon, vendor, etc
- add new array design (mostly admin-only — most ADs come from GEO
  loaders)
- list EEs using this array design
- download annotation file (TSV/CSV of probe → gene mappings)
- generate annotation file (async task)
- delete (admin)
- ArrayDesign probe mapper: re-map sequences to genes (BLAT-based);
  admin-only
- ArrayDesign repeat scan: identify repeat-masked probes (admin)
- composite-sequence (probe) detail + filter

**REST surface (vs available)**:
- GET `/platforms`, `/platforms/{id}` — exists
- GET `/platforms/{id}/datasets` — exists
- GET `/platforms/{id}/elements` — exists (paginated probes)
- GET `/platforms/{id}/elements/{probe}/genes` — exists
- GET `/platforms/{id}/annotations` — exists (annotation file)
- GET `/platforms/count`, `/blacklisted` — exists
- POST regenerate annotation file — **missing**
- POST probe mapper / repeat scan / merge / delete — **missing**
- PUT update AD metadata — **missing**

**Priority**: Medium (mostly admin; browse is public)
**Effort**: M — most read paths exist; write paths absent.
**Dubious bits**: probe mapper and repeat scan are batch jobs that
should be CLI-only in 2.0 (they're already CLI commands too).
Confirm with Paul whether the UI for them retires.

---

### B16. File upload (data + design)

**What gemma-web shows** (`FileUploadController` +
`ExpressionDataFileUploadController` + `dataUpload.jsp`):
- upload custom expression matrix file (data + design)
- multi-step wizard: pick taxon, upload file, validate, choose
  array design (or auto-create one), submit as async load job
- design-from-file upload (`ExperimentalDesignController.
  createDesignFromFile`)

**REST surface**:
- POST `/rest/v2/datasets/import` — **FastAPI mock only**
- POST upload custom EE — **missing in REST entirely**
- the gemma-web flow is multipart and stateful (chunked upload +
  validation step); REST has nothing equivalent

**Priority**: Medium (rare action; most EEs come from GEO loader)
**Effort**: L — multipart upload + async validation + commit step.
**Dubious bits**: maybe defer — the curator-agents `/datasets/import`
covers GEO-by-accession import which is the common case. Custom
matrix upload could be CLI-only.

---

### B17. GEO record browser (admin: find new EEs to load)

**What gemma-web shows** (`GeoRecordBrowserController` +
`geoRecordBrowser.jsp`):
- search GEO for series matching a query
- preview details (sample count, platforms, taxon)
- click "load" → triggers `ExpressionExperimentLoadController` (GEO
  loader job)

**REST surface**:
- GET `/rest/v2/datasets/search` — **FastAPI mock only** (gemma-
  curation-agents proxies this)
- POST `/rest/v2/datasets/import` — **FastAPI mock only**
- gemma-rest has nothing GEO-side

**Priority**: Medium (admin/curator only)
**Effort**: M — wrap the existing GeoService in REST endpoints.

---

### B18. Async task progress / job dashboard

**What gemma-web shows** (`ProgressStatusController` +
`TaskCompletionController` + `processProgress.jsp`):
- poll task status by taskId
- list all submitted tasks (admin can see everyone's)
- cancel a job
- add email alert for completion
- task progress stream (ProgressData log lines)

**REST surface**:
- GET `/tasks/{taskId}` — exists (TasksWebService)
- GET `/rest/v2/tasks/{task_id}` — FastAPI mirror, exists
- POST cancel — **missing in REST**
- GET list all submitted tasks (admin) — **missing**
- POST email alert — **missing**
- streaming progress lines — **missing** (REST polls only)

**Priority**: High (every action that fires a job needs the UI to
poll status)
**Effort**: S — add cancel + admin-list + (optional) SSE stream to
TasksWebService.

---

### B19. Tickets (curation comments / discussion threads)

**What gemma-web shows**: nothing — this is new in gemma-rest.

**REST surface**:
- GET/POST `/tickets`, `/{id}`, `/{id}/events` — exists
- linked from `/datasets/{id}/tickets`, `/platforms/{id}/tickets`

**Priority**: Medium-Low (depends on whether curators have adopted it)
**Effort**: S for the UI (REST is complete)
**Dubious bits**: there's overlap with FastAPI "curation-proposals"
and "audits" — does Tickets get used in 2.0 or sidelined for the
AI-curation flow? Paul-input.

---

### B20. WhatsNew / RSS feeds / Home page recent activity

**What gemma-web shows** (`HomePageController` + `WhatsNewController`
+ `RssFeedController` + `home.jsp` + `systemNotices.jsp`):
- home page: count summary (total EE, AD, gene; recent additions),
  feature blurbs, contact info
- "what's new" widget: experiments added/updated this week
- RSS feed: machine-readable feed of recent changes
- system notices: announcements

**REST surface**:
- recent activity counts — **missing**
- RSS feed — gemma-web only
- system notices — gemma-web only (probably a config file)

**Priority**: Low (cosmetic on the public side; "what's new" is nice
for return visitors)
**Effort**: S
**Dubious bits**: RSS feed almost certainly retires. WhatsNew might
be folded into the home-page hero. Confirm with Paul.

---

### B21. TGEMO / TGFVO ontology publishing

**What gemma-web shows** (`OntologyController`):
- `/ont/TGEMO` and `/ont/TGEMO.OWL` — Gemma-published ontology
  endpoints used by external consumers
- `/ont/TGFVO`, `/ont/TGFVO.OWL`, per-factor-value RDF pages

**REST surface**: not applicable — these are content-negotiated
RDF/OWL responses, not really REST.

**Priority**: Low (external consumers care, internal UI does not)
**Effort**: S to keep working as-is in a small static-content servlet
**Dubious bits**: this stays on the server, not the UI. The UI can
display the ontology browser (it's covered by `/annotations/parents`
etc) without re-implementing the publish layer.

---

### B22. Login, signup, password reset (subset of B4 but
user-facing)

Covered under B4 but called out separately because gemma-ui
**curation/auth** folder only does login/logout/me — signup, password
reset, email confirmation are still gemma-web JSPs.

**Priority**: Medium (only matters if non-admins can self-signup)

---

### B23. SVD / Preprocess admin (analysis chain)

**What gemma-web shows** (`PreprocessController` + `SvdController` +
`BatchInfoFetchController` + `TwoChannelMissingValueController`):
- trigger SVD on EE
- trigger preprocess pipeline
- trigger batch-info fetch
- two-channel missing-value computation (legacy, rare)
- ExpressionExperimentReportGenerationController — regenerate cached
  per-EE reports

**REST surface (vs available)**:
- POST `/datasets/{id}/tasks/preprocess` — exists
- POST `/datasets/{id}/tasks/diagnostics` — exists
- POST `/datasets/{id}/tasks/batchInfo` — exists
- POST `/datasets/{id}/geeq/recompute` — exists
- POST SVD-only — **missing** (folded into diagnostics)
- POST report regenerate — **missing**
- POST two-channel missing-value — **missing**

**Priority**: Low (admin tools; most are now CLI commands)
**Effort**: S
**Dubious bits**: two-channel missing-value is dead-code for modern
single-channel data. Retire.

---

## Section C — Cross-reference: what's "dubious" (Paul to decide)

A condensed list of decisions the catalog surfaces, each requiring
Paul's call before scoping work:

1. **Per-image PNG endpoints (B1)**: heatmap-rewrite-to-client means
   QC images become raw-data endpoints. Decision: kill the PNG paths
   in 2.0, or keep them as a fallback?
2. **Coexpression on Gene page (B2, B10)**: was the legacy
   coexpression search retired, or just unwired? Confirm whether the
   gene-page section ships or disappears.
3. **Session-bound gene/EE sets (B3)**: anonymous user storage —
   retire and require login, or keep?
4. **FastAPI groups vs Spring Security groups (B4, B12)**: two
   parallel group APIs. Pick one.
5. **`SystemMonitor` cache/hibernate panel (B5)**: `/metrics`
   already covers it for SRE. Keep as "diagnostics" page, or retire?
6. **Search index admin UI (B6)**: keep the form-based admin page or
   trigger from CLI only? Most admins use the CLI commands.
7. **Bibliographic browser (B7)**: full standalone publication
   browser or just per-EE pub list? The standalone browse is rarely
   used.
8. **Meta-analyzer (B9)**: ship the manager UI in 2.0 or retire?
9. **Custom-DE-config UI vs FastAPI simple POST (B9, B14)**: granular
   per-factor-value PATCH (rich) vs whole-design PUT (lossy). Pick
   one shape.
10. **Mass-edit characteristics (B11)**: keep the dangerous bulk
    edit tool or restrict to CLI?
11. **TGEMO/TGFVO publish endpoints (B11, B21)**: separate ontology-
    server module, stay in gemma-web reskinned, or retire?
12. **AI curation FastAPI vs gemma-rest split (B12)**: do proposals/
    audits/candidates/calibration migrate into gemma-rest or stay
    in the FastAPI microservice forever?
13. **Tickets vs Proposals/Audits (B19)**: do they coexist or does
    Tickets get retired in favor of the AI-curation flow?
14. **WhatsNew + RSS feed (B20)**: retire.
15. **Custom data file upload (B16)**: keep in UI or CLI-only?
16. **Two-channel missing-value (B23)**: retire.
17. **Array design probe-mapper + repeat-scan UI (B15)**: retire
    (CLI only)?

---

## Section D — Prioritized roadmap (top 20)

Ordering: impact × dependency-readiness ÷ effort. Quick wins (REST
already exists, only UI missing) lead.

| # | Item | REST endpoint(s) | Effort | Depends on |
|---|---|---|---|---|
| 1 | Global search box / results page | GET `/search`, `/annotations/search/{q}/datasets` — exist | S | — |
| 2 | Async task polling UI (toast/banner + cancel) | GET `/tasks/{id}` exists; cancel missing | S | (4) for cancel |
| 3 | Dataset page audit-event panel | GET `/datasets/{id}/auditEvents` — exists | S | — |
| 4 | Cancel async task endpoint (server-side) | new: DELETE `/tasks/{id}` | S | — |
| 5 | Dataset page curation-details + permissions panels | GET/PATCH `/datasets/{id}/curationDetails`, GET `/permissions` — exist | S | (8) for write |
| 6 | Login + me + logout completion | exist (AuthWebService) | S | — |
| 7 | Gene page (read-only): symbol, GO, products, probes, locations | most exist except gene-products + multifunc | M | new: products + MF endpoints |
| 8 | Permissions write (make public/private, group read/write) | exists (PUT `/datasets/{id}/permissions`); cross-entity missing | M | — |
| 9 | System monitoring page (Prometheus already there) | use `/metrics` | S | — |
| 10 | Dataset page QC visualizations (client-side rewrite) | new raw-data shape endpoints | L | heatmap-rewrite plan |
| 11 | Search-index rebuild admin | new: POST `/admin/search-index/rebuild` | S-M | (2,4) |
| 12 | User signup + password reset | new: POST `/signup`, `/resetPassword` | M | — |
| 13 | Admin user-list + role grant | new: GET `/admin/users`, PUT `/users/{u}/roles` | M | (6) |
| 14 | Differential expression result viewer (per-EE) | most exists | M | — |
| 15 | Gene set CRUD (user-owned, no session-bound) | new: full `/geneSets` resource | L | (6) |
| 16 | Experiment set CRUD | new: full `/expressionExperimentSets` | L | (6) |
| 17 | Publication search + per-EE pub edit | new: PubMed proxy + bibref CRUD | M | — |
| 18 | Curator inbox (proposals/audits) — stays FastAPI | existing FastAPI | M (UI only) | architecture decision |
| 19 | Experimental design editor (full granular edit) | mostly new in REST | XL | (8), Paul-decision on shape |
| 20 | GEO record browser + import trigger | new: REST mirror of FastAPI shape | M | (4) |

**Quick wins (items 1-9)**: REST endpoints exist; only UI work
needed. Land these first; they collectively give a public-facing UI
parity around 60% of gemma-web's read surface.

**Heavy lift (items 14-20)**: each is a new REST resource and a UI
surface from zero. These are the work that turns gemma-ui from a
viewer into a curator-grade workbench.

**Strategically deferred / decision-blocked**:
- Custom data upload (B16) — defer / CLI-only?
- Coexpression (B10) — confirm retirement
- Meta-analyzer (B9) — confirm retirement
- WhatsNew + RSS (B20) — confirm retirement
- Mass-edit characteristics (B11) — confirm retirement

---

## Curation guide coverage estimate

The Confluence export `How-to-Curate-an-Experiment_41681510.html` is
mostly an image-embed (the `GemmaCurationWorkFlow.png` flowchart).
Recoverable text-headings are:

- Basic Curation
- Curate the Experimental Tags (= annotation tags on EE)
- Curate the experimental design (= factor + factor-value editing)
- Curating Multiplatform Datasets
- Link the experiment to its publication
- Check if appropriate taxon has been annotated
- Filling Batch Information
- Reprocess Vectors and Perform Batch Correction
- Re-process data vectors
- Remove Outliers
- Compute differential expression
- Perform coexpression analysis
- Check the Diagnostics Tab
- Changing Experiment Taxon
- Generating Flat Files for Downloading
- Making the experiment public or Marking as Unusable
- AffyFromCel (loading Affy CEL files)
- Experiment Checklist

**Mapping to catalog**:
- Curate Experimental Tags → B14 (annotation editing)
- Curate experimental design → B14 (design editor)
- Multiplatform → B15 partly + B14
- Link publication → B7 + B1 edit ops
- Filling Batch Information → B23 (batch-info task)
- Reprocess + batch correct → B23 + B1
- Remove Outliers → B1 (mark outlier — endpoint exists at PUT
  `/datasets/{id}/samples/{baid}/outlier`)
- Compute DE → B9 (already covered)
- Coexpression analysis → B10 (confirm retirement)
- Diagnostics tab → B1 + B23 + GEEQ
- Changing taxon → B1 edit (no REST endpoint)
- Generating flat files → exists (multiple download endpoints)
- Make public / unusable → exists (PUT permissions, PATCH
  curationDetails)
- AffyFromCel → admin CLI; not UI-bound

**Gaps the curation guide flags that this catalog doesn't have a
specific item for**:
- "Changing Experiment Taxon" — a rare repair operation, no REST
  endpoint, no UI affordance. Flagged.
- "AffyFromCel" — CLI; correctly not in catalog.

Otherwise the curation guide is well-covered by B1, B9, B14, B15,
and B23.

---

## Notes on methodology

- Controllers were buckled into 23 feature areas; not all 47
  individual controllers got their own section because many are
  per-page-shell utilities (`TaxonController`, `HomePageController`,
  `personForm` form-controller) that the SPA architecture replaces
  with client-side routes.
- DWR endpoints are surveyed via the controllers' `public` methods
  with VO return types (these are typically DWR-exposed). The
  `dwr.xml` config wasn't found in this branch; either it's been
  removed or all DWR is now annotation-driven.
- gemma-rest surveys are accurate as of `59d9ffbf34`.
- FastAPI mock surveys are accurate as of `gemma-curation-agents`
  HEAD on 2026-05-21.
