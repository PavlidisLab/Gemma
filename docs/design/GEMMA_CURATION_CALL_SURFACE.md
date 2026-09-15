# Gemma curation-flow call-surface map

Read-only recce mapping `gemma-curation-agents` (Python) → Gemma REST → Java DAO,
to identify the per-EE hot loop and missing batch endpoints. Source-trace only;
no Python pipeline was actually run.

## Setup

- **Python repo probed:** `~/Dev/gemma-curation-agents/gemma_curation_agents`
  (read-only; do-not-modify).
- **Canonical HTTP client modules:**
  - `shared/gemma.py` — read+write REST helpers (`requests`).
  - `ontology/gemma_resolver.py` — annotation-search / term lookup (`requests.Session`).
  - `ontology/gemma_gene_resolver.py` — `/rest/v2/search` for genes (`requests.Session`).
  - `ontology/curation_resolver.py` — wraps GemmaResolver + the local "curation API"
    (the FastAPI mock under `gemma_curation_agents/local_api`, mounted at `/rest/v2`).
  - `agents/curation_proposer/submitter.py`, `agents/audit/submitter.py` — write-side
    POSTs (httpx), currently routed at the **mock** curation API only.
  - `agents/curation_proposer/skeleton.py` — composes per-EE fetch
    (`get_dataset` + `get_dataset_samples_raw` + `get_dataset_factor_facts` +
    `get_dataset_annotations` + `/publications` + GEO biolit).
- **Gemma branch under map:** `perf-probe-curation-flow` baselined at
  `ae28039634` (sibling of `phase2-acl-migrate`).
- **Public base default:** `https://gemma.msl.ubc.ca` (env override:
  `GEMMA_BASE_URL`). Live prod for read-only sanity is `127.0.0.1:8000` → `gemd`.

## Python call inventory

| Python caller | URL pattern | Java handler | Cost class | Hits per EE (typical) |
|---|---|---|---|---|
| `shared.gemma.get_dataset(reference)` (skeleton.py:659) | `GET /rest/v2/datasets/{id}` OR `GET /rest/v2/datasets?filter=accession.accession=…&limit=1` (fallback `?filter=shortName=…`) | `DatasetsWebService.getDatasets` / `getDataset` (DatasetsWebService.java:260, 778) → `DatasetArgService.getEntity` | small lookup + `thawLite` | 1 (sometimes 2 with shortName fallback) |
| `shared.gemma.get_dataset_samples_raw(eid)` (skeleton.py:683) | `GET /rest/v2/datasets/{id}/samples` | `DatasetsWebService.getDatasetSamples` (line 893) → `DatasetArgService.getSamples` → `thawBioAssays` + `baService.loadValueObjects` | **heavy** (per-BA VO + characteristics + fvbasicVOs) | 1 |
| `shared.gemma.get_dataset_factor_facts(accession)` (skeleton.py:973) | `GET /rest/v2/datasets/{id}/samples` (**same endpoint, re-fetched**) | same as above | **heavy duplicate** | 1 (redundant — see foot-gun #1) |
| `shared.gemma.get_dataset_annotations(eid)` (skeleton.py:1015) | `GET /rest/v2/datasets/{id}/annotations` | `DatasetsWebService.getDatasetAnnotations` (~ line 524) → `DatasetArgService.getAnnotations` → `EEService.getAnnotations` | medium | 1 |
| `shared.gemma.get_dataset_platforms_raw(eid)` (optional, currently NOT in skeleton path) | `GET /rest/v2/datasets/{id}/platforms` | `DatasetsWebService.getDatasetPlatforms` (line 873) | small | 0–1 |
| `shared.gemma._gemma_base()/.../publications` (skeleton.py:1116) | `GET /rest/v2/datasets/{id}/publications` | `DatasetsWebService.getDatasetPublications` (line 1022) | small | 1 |
| `shared.gemma.get_dataset_curation_events(eid)` (audit apply path) | `GET /rest/v2/datasets/{id}` (re-fetch for `lastTroubledEvent` etc.) | same dataset handler | small | 1 (only on audit) |
| `shared.gemma.get_curation_details / update_curation_details` (disposition_sink.py) | `GET / PUT /rest/v2/datasets/{id}/curationDetails` | `DatasetsWebService.getCurationDetails` / `updateCurationDetails` (1116, 1195) | small | 0–1 (read), 0–1 (write — audit only) |
| `shared.gemma.is_in_gemma / in_gemma_bulk(...)` | `GET /rest/v2/datasets?filter=accession.accession in (…)` | `DatasetsWebService.getDatasets` listing | small (1/chunk of 50) | 0 in proposer hot loop; used by bulk scrape only |
| `shared.gemma.find_datasets / count_datasets / get_dataset` (sampler / cli) | `GET /rest/v2/datasets?filter=…&limit=…&offset=…` | `DatasetsWebService.getDatasets` | small | 0 per EE in the proposer (corpus-level only) |
| `GemmaResolver.search(label, …)` (curation_resolver.py:252 + gemma_resolver.py:66) | `GET /rest/v2/annotations/search?query=…&limit=…` (`?uri=…` for direct) | `AnnotationsWebService.searchAnnotations` (`AnnotationsWebService.java`) | medium (full-text + DB join) | **N per EE** — see foot-gun #2 |
| `GemmaResolver.lookup_by_uri(uri)` (gemma_resolver.py:194) | `GET /rest/v2/annotations/term?uri=…` | `AnnotationsWebService.getAnnotationTerm` | small | up to N tag URIs per EE during term validation |
| `GemmaResolver.get_categories()` (gemma_resolver.py:257) | `GET /rest/v2/datasets/categories?limit=200` | `DatasetsWebService.getDatasetCategories` (line 460) | medium (aggregate count) | **1 per fresh resolver instance** (lazy + cached) |
| `GemmaGeneResolver._fetch(query, …)` (gemma_gene_resolver.py:279) | `GET /rest/v2/search?query=…&resultTypes=ubic.gemma.model.genome.Gene&taxon=…&limit=…` | `SearchWebService.search` | medium (hibernate-search + DB) | **per gene-bearing tag**, often N per EE |
| `agents/curation_proposer/submitter.submit(proposal, …)` | `POST {GEMMA_CURATION_URL}/rest/v2/datasets/{id}/curation-proposals` | **NOT IMPLEMENTED in `gemma-rest`** (only in `local_api/server.py` mock) | n/a | 1 per EE (writes to mock today) |
| `agents/audit/submitter.submit(report, …)` | `POST {GEMMA_CURATION_URL}/rest/v2/datasets/{id}/audits` | **NOT IMPLEMENTED in `gemma-rest`** (mock-only) | n/a | 1 per audited EE |
| `agents/audit/disposition_sink.GemmaRestDispositionSink` | `PUT /rest/v2/datasets/{id}/curationDetails` | `DatasetsWebService.updateCurationDetails` (1195) | small | 1 per applied audit outcome |

### Distinct URL patterns inventoried: **15** (excluding the mock-only curation-proposals/audits family). With the mock endpoints counted: **17**.

## Hot loops

### Proposer pass on one EE (`fetch_skeleton` + `propose_tags` + grounding + submit)

Sequential, per-EE — request count breakdown:

1. `GET /datasets/{id}` — 1 (resolve reference).
2. `GET /datasets/{id}/samples` — **2** (once for the BM/factor frame in
   `get_dataset_samples_raw`, then immediately again in
   `get_dataset_factor_facts` for `experimentalFactorType` + `measurement.value`).
3. `GET /datasets/{id}/annotations` — 1.
4. `GET /datasets/{id}/publications` — 1.
5. **(non-Gemma)** GEO per-sample fetch via `biolit.fetch_geo_samples(accession)` — 1
   external NCBI call (cached on disk).
6. `GET /rest/v2/annotations/search` — **N**, once per (label, category) the
   proposer/grounder/term-validator wants to resolve. In-process cache keyed on
   `(label, category_hint)`. For a typical EE with 8 proposed tags + factor
   categories + statement objects + statement predicates, N is in the
   **20–60 range**. CurationApiResolver wraps GemmaResolver, so each miss
   doubles up (mock first, real Gemma fallback).
7. `GET /rest/v2/annotations/term?uri=…` — up to N, in `term_validator` URI
   round-trip checks (cached in `uri_cache`).
8. `GET /rest/v2/datasets/categories?limit=200` — 1 per resolver instance
   (lazy-loaded; cached for the rest of the run).
9. `GET /rest/v2/search?resultTypes=Gene&…` — **G**, once per gene-bearing tag
   (1–5 typical, up to 20 on heavy perturbation EEs).
10. `POST /rest/v2/datasets/{id}/curation-proposals` — 1 (to mock today).

**Floor per EE (small experiment, 5 tags, 1 gene):** ~10 Gemma requests.
**Ceiling per EE (perturbation experiment, 15 tags, 8 genes, statement-heavy):**
**60–100 Gemma requests**, dominated by `/annotations/search`.

### Audit pass on one EE

Same skeleton fetch (5 requests including the duplicate `/samples`), then:

- Repeat resolver searches over the proposer's tags **plus** the existing tags
  (the audit re-grounds the curator's URIs to detect drift) — another 20–60
  `/annotations/search` calls.
- `POST .../audits` (1, mock).
- Optional `PUT .../curationDetails` apply (1, real Gemma).

Audit ≈ proposer in request count; they often run **back-to-back on the same
EE** (silent-proposer-inside-audit pattern in `agents/audit/pipeline.py`),
double-counting the skeleton fetch.

### Parallelism status

Per-EE calls inside one proposer pass are **sequential by code shape**:
`requests` is sync; `httpx.Client` is opened sync; the resolver cache means
later searches can't start until earlier ones return. **EEs across a corpus**
are processed by `proposer_service.py` with `PROPOSER_MAX_CONCURRENCY`
(default 4 IIRC); within one EE there is no fan-out.

### Wall-time arithmetic on a representative EE

Assuming the standing prod RTT plus the Gemma handler measurements implied by
`SAMPLES_DESIGN_PERF_RECCE.md` (referenced by `DatasetArgService.getSamples`
javadoc) and the audit's recently-landed batch fixes:

- `/datasets/{id}` — 30–60 ms.
- `/datasets/{id}/samples` — **300–800 ms** depending on EE size (this is the
  endpoint Paul most recently tuned — `loadAsMap` migrations, sourceBM batched
  thaw etc.). **Two calls.**
- `/datasets/{id}/annotations` — 50–150 ms.
- `/datasets/{id}/publications` — 30–60 ms.
- `/annotations/search` — 30–80 ms each, ×30 = ~1.5 s.
- `/rest/v2/search` Gene — 80–150 ms each, ×5 = ~0.6 s.
- `POST /curation-proposals` — mock, 10–20 ms.

Floor ≈ **2.5 s** per EE on a warm cache; ceiling ≈ **6–8 s** with a heavy
search workload. The two duplicated `/samples` calls account for ~10–25% of
the per-EE wall time on their own.

## Foot-guns + fix directions

### #1: `/datasets/{id}/samples` is fetched TWICE per skeleton build

**Evidence.** `skeleton.py:683` calls `get_dataset_samples_raw(eid)` to populate
the BioMaterial frame. `skeleton.py:973` then calls `get_dataset_factor_facts(eid)`
(`shared/gemma.py:147`) **which hits the same `/datasets/{id}/samples` endpoint
again**, this time to read `experimentalFactorType` and `measurement.value`
fields the Python wrapper stripped on the first parse. The two calls are
sequential, ~80 lines apart in one function, on the same EE.

**Why it exists.** `get_dataset_samples_raw` and `get_dataset_factor_facts` were
written as standalone helpers and the skeleton composer never noticed they
share an endpoint. The first call already gets back `fvbasicVOs[].experimentalFactorType`
and `fvbasicVOs[].measurement.value` — they're just being thrown away during
DataFrame construction.

**Fix direction.** **Client-side fix is trivial:** parse the factor-type + measurement
fields out of the `_samples_raw.items` JSON the first call already returns. Server
needs no change. Saves one `/samples` call per EE — at 300–800 ms each, this is
the single biggest per-EE win available. Estimated 10–25% wall-time reduction
on `fetch_skeleton`.

### #2: `/annotations/search` re-fired for every (label, category) seen across an EE corpus

**Evidence.** `CurationApiResolver._session` and `GemmaResolver._session` use
`requests.Session` for connection pooling, and each instance keeps a
**per-instance** `_cache: dict[(label, cat), [Candidate]]`. But the proposer
service constructs a fresh resolver per-EE (resolver instance leaks into the
agent-config closure; see `_build_proposer_helper_tools` + the cli factory
path). Common labels — `tissue`, `cell type`, `treatment`, `organism part`,
`Homo sapiens`, `Mus musculus`, plus the top ~50 ontology terms applied across
hundreds of EEs — get refetched once per EE.

**Why it exists.** Cache lifetime is bound to the resolver instance, not the
process. The `proposer_service.py` HTTP service was retro-fitted in but doesn't
share a resolver across `/propose/{accession}` calls.

**Fix direction.** **Client-side process-wide cache.** Hoist the resolver
construction to module scope in `proposer_service.py` (or pass a single
resolver into the per-EE worker pool). This is a 5-line change and would drop
20–40% of `/annotations/search` calls on a corpus run. Server-side, an LRU on
`AnnotationsWebService.searchAnnotations` keyed on `(query, limit, category)`
with a short TTL (60s) would also help and benefits other clients
(curation-ui typeahead).

### #3: `/rest/v2/search?resultTypes=Gene` issued per gene symbol — no batch endpoint

**Evidence.** `GemmaGeneResolver._fetch(query, …)` (`gemma_gene_resolver.py:279`)
takes a single `query=` string. A perturbation EE that proposes tags for genes
`TP53, MYC, KRAS, EGFR, BRAF, …` issues one `/search` request per symbol. There
is no `?query=in(TP53,MYC,…)` form on `SearchWebService` — `query` is a single
free-text string and `SearchService` runs hibernate-search per call.

**Fix direction.** **Server-side: add a bulk symbol resolver endpoint** —
e.g. `GET /rest/v2/genes?officialSymbol in (TP53, MYC, KRAS) [&taxon=human]`
that bypasses hibernate-search entirely and does a straight DAO `findByOfficialSymbolIn`
keyed on (symbol, taxon). Per gene, the trusted-source paths in
`gemma_gene_resolver._TRUSTED_SOURCES` all reduce to indexed lookups —
`findByOfficialSymbol`, `findByOfficialSymbolInexact`, `findByOfficialName`,
`findByAlias`. A batch DAO call is O(1) round-trips for the gene list; today
it's O(G). For a 10-gene EE this saves ~9 requests and ~1 s wall time.

### #4: `lookup_by_uri` round-trip per existing tag URI in audit + term_validator

**Evidence.** `term_validator.py:150` calls `_uri_lookup_fn(canonical)` once per
distinct URI seen in tags + factor categories + statement subjects/objects.
`GemmaResolver.lookup_by_uri` (`gemma_resolver.py:194`) issues
`GET /annotations/term?uri=…` — one request per URI. The audit path also
re-validates all existing curator URIs the same way. There is no
`?uri in (…)` form.

**Fix direction.** **Server-side: accept a URI list** —
`GET /rest/v2/annotations/term?uri=A&uri=B&uri=C` (or `POST` with a body for
large sets). Maps to a single `AnnotationDao.loadValueObjectsByValueUriIn`
call. Saves up to N round-trips per audit. The Java endpoint already wraps
the single-URI path in `AnnotationsWebService`; the multi-URI form is a small
addition.

## REST endpoints that should exist but don't (the "missing batch API" list)

Ranked by per-EE wall-time impact:

1. **`POST /rest/v2/datasets/{id}/curation-proposals`** — the write surface
   the Python proposer is targeting today. Submitter sends here
   (`submitter.py:67`); only `gemma_curation_agents/local_api/server.py:436`
   serves it. Until this is real, the proposer pipeline cannot land on prod
   Gemma. **This is the headline gap** — without it, the entire curation-side
   loop is mock-bound.

2. **`POST /rest/v2/datasets/{id}/audits`** — twin of #1 for the audit pipeline.
   Mock-only today (`local_api/server.py:523`). Needed to ship audit outputs
   to prod Gemma.

3. **`GET /rest/v2/genes?officialSymbol=… in (…) [&taxon=…]`** (bulk gene
   resolver) — fix direction for foot-gun #3. Replaces N calls to
   `/rest/v2/search?resultTypes=Gene` with 1. The handler lives one DAO
   method away (`GeneDao.findByOfficialSymbolIn`).

4. **`GET /rest/v2/annotations/term?uri in (…)`** OR `POST` body variant —
   fix direction for foot-gun #4. Replaces N `lookup_by_uri` calls with 1.

5. **`GET /rest/v2/datasets/{id}/skeleton` (or `/curation-bundle`)** — a single
   fat-VO endpoint that returns *everything* `fetch_skeleton` needs in one
   request: dataset header + samples + factor type / measurement + annotations
   + publications + curationDetails. This is the **architectural win**:
   collapses 4–5 Gemma round-trips per EE into 1, eliminates the duplicate
   `/samples` parse (foot-gun #1), and makes the per-EE floor ~1.0 s instead
   of 2.5 s. The shape would mirror what `skeleton.py` ultimately constructs
   — let the server assemble it once. Aligns with the same theme as Paul's
   "ship raw data + meta to gemma-curation-ui" directive
   (`project_heatmap_rewrite_to_client.md`).

6. **`GET /rest/v2/annotations/search` server-side `(query, category, limit)`
   LRU cache** — not really a new endpoint, but a server-side cache (60 s TTL)
   would deflate foot-gun #2 even without the client-side fix.

## Cross-cutting observations

- **The curation API "exists" only as a Python mock** (`gemma_curation_agents/local_api/`).
  The proposer + audit submitter both write to `GEMMA_CURATION_URL`, not to
  real Gemma. Until `/datasets/{id}/curation-proposals` and `/datasets/{id}/audits`
  ship in `gemma-rest`, the pipeline cannot graduate from staging to prod.
  Cross-reference `TODO-gemma-api.md` and `TODO-gemma-api-2.md` for the
  current upstream-API gaps the Python side has been catalog-ing.

- **The dominant per-EE call is `/annotations/search`**, not `/samples`. With
  a small EE the search count (~30) dwarfs everything else. The samples
  endpoint is heavier per call but issued only twice — search wins on count.
  Both fix-direction #1 (sample dedup) and fix-direction #2 (process-wide
  resolver cache) compound: dedup saves 1× heavy call; cache saves ~20× medium
  calls.

- **`/datasets/{id}/samples` is doing dual duty**: the BioAssay-VO shape AND
  the experimental-design factor-type/measurement data. That's why it's
  expensive (the `BioAssayValueObject` ctor implicitly pulls the BM +
  factor-value graph). A dedicated `GET /datasets/{id}/factorFacts` that
  returns just the (factorId → factorType) + ((factorId, fvId) →
  measurementValue) maps would be a fraction of the cost and let foot-gun
  #1's "trivial client fix" become unnecessary.

- **Sessions/connection reuse is healthy on the read side** — both
  `requests.Session` and `httpx.Client` (per-call) are used correctly.
  Connection-pool turnover is not the bottleneck; request count is.

- **The Python side already documents the upstream gaps** — see
  `~/Dev/gemma-curation-agents/TODO-gemma-api.md` (§1, §2, §11, §17, §20, §21).
  The recently-landed Gemma 1.32.7 commits (`e8cfb24976`, `129d56e5e6`,
  `58b5f9e032`, `0222012098`) closed §1/§2/§17. §11 (usage counts on
  `/annotations/search`) and §20/§21 (`evidenceCode` / `experimentalFactorType` /
  `measurement.value` on the proper endpoints, not as a side-channel reparse
  of `/samples`) are still open.

- **No retry/back-off layer** between the Python client and Gemma. A 503 or
  network hiccup mid-skeleton aborts the EE, and the proposer-service then
  retries the *entire* EE pipeline — multiplying the request count. A
  per-call retry with capped backoff would smooth transient failures
  without inflating the cost on the happy path.

- **The audit pipeline runs a silent proposer inside itself** — that's
  another full proposer call-set per audited EE. If `/skeleton` (missing
  endpoint #5) lands, audit + proposer can share the same skeleton fetch in
  one process and halve the per-audit cost.
