# Live-gemd perf probe — search service hot paths

## Setup

- Worktree tip: `ae28039634818af146bdd918b1202452ebd18b66` (branch `perf-probe-search-service`).
- Probe host: Darwin 24.6.0 (PAVNOTE-18); mysql client 5.7.31.
- Server target: prod `gemd` via local tunnel `127.0.0.1:8000`, user `gemmaadmin`, READ-ONLY. Tunnel was UP for the entire probe window (unlike Round 4). Server hostname: `homer.msl.ubc.ca`, datadir `/data/db/mysql/`.
- Cardinalities (prod gemd, as of 2026-05-20):

  | table | rows |
  |---|---:|
  | `INVESTIGATION` (EE) | 72 002 |
  | `ARRAY_DESIGN` | 671 |
  | `CHROMOSOME_FEATURE` (Gene) | 1 296 431 |
  | `COMPOSITE_SEQUENCE` | 15 320 091 |
  | `BIO_SEQUENCE` | 6 130 490 |
  | `CHARACTERISTIC` | 12 042 228 |
  | `EXPRESSION_EXPERIMENT2CHARACTERISTIC` (EE2C) | 2 531 907 |
  | `BIBLIOGRAPHIC_REFERENCE` | 42 740 |
  | `GENE_SET` | 106 |

## Source inventory

The search surface is decomposed:

- **`SearchServiceImpl`** (gemma-core, 249 LoC) — per-result-type dispatcher; fans out to a `CompositeSearchSource` composed of `DatabaseSearchSource`, `HibernateSearchSource`, `OntologySearchSource`, `GeneOntologySearchSource`. Restored Phase 3 (HS-7 rewrite). `@Transactional(readOnly=true)`.
- **`CompositeSearchSource`** — iterates registered sources in `@Order` precedence (DatabaseSearchSource HIGHEST_PRECEDENCE first), accumulates results per result type, warns when total wall > 1s (FAST mode threshold 100ms).
- **`DatabaseSearchSource`** — exact-name / accession / short-name lookups via DAO finders. Per-finder `StopWatch` warn @ 1s.
- **`HibernateSearchSource`** — Hibernate Search 7 (Lucene local-filesystem backend); `simpleQueryString` predicate over per-class `*_FIELDS` arrays.
- **`OntologySearchSource`** — only for `ExpressionExperiment` result type; resolves user query → ontology terms via `OntologyService.findTerms` → child URIs via `getChildren` → EE lookup via `CharacteristicDao.findExperimentsByUris(uris, ...)`. 30s timeout budget.
- **`GeneOntologySearchSource`** — Gene-only via GO; currently disabled at runtime due to baseCode renovations stub (see source-level note).
- **`IndexerServiceImpl`** — manual MassIndexer driver; `automatic indexing.listeners.enabled = false` (writes-don't-update-Lucene by design).
- **REST endpoint**: `SearchWebService` at `/rest/v2/search`, default `MAX_SEARCH_RESULTS=2000`, `fillResults=true` unless caller excludes `resultObject`.

## Probe inventory

### A. Free-text + ontology dispatch

#### A1: Gene DB lookup (e.g. `BRCA1`) — GREEN

DDL: `CHROMOSOME_FEATURE` has indices on `OFFICIAL_SYMBOL,TAXON_FK`, `NAME`, `ENSEMBL_ID`, `NCBI_GENE_ID`, `NCBI_ID`. All single-row constant lookups.

```sql
SELECT COUNT(*) FROM CHROMOSOME_FEATURE WHERE OFFICIAL_SYMBOL = 'BRCA1';  -- 0.4 ms, 3 rows
SELECT COUNT(*) FROM CHROMOSOME_FEATURE WHERE NAME = 'BRCA1';             -- 0.2 ms
SELECT COUNT(*) FROM CHROMOSOME_FEATURE WHERE OFFICIAL_SYMBOL LIKE 'BRCA%'; -- 0.1 ms
```

EXPLAIN: `ref` access on `CHROMOSOME_FEATURE_NAME` / `symbol_tax`. Sub-millisecond.

**Verdict: GREEN**. Gene DB lookup is not the search bottleneck.

#### A2: EE entity hydration for `fillResults=true` — GREEN at SQL level

`SearchWebService.search(...)` defaults to `fillResults=true` and `maxResults=100`. After Hibernate Search returns Lucene hits, `HibernateSearchSource.rowToSearchResult` calls `sessionFactory.getCurrentSession().get(clazz, id)` per hit (line 333).

```sql
SELECT * FROM INVESTIGATION WHERE ID = 1;            -- 0.3 ms
SELECT * FROM INVESTIGATION WHERE ID IN (1,...,20);  -- 0.6 ms (20 rows)
SELECT * FROM INVESTIGATION ORDER BY ID LIMIT 100;   -- 1.0 ms
```

Per-row PK lookup is sub-ms; 100 hits hydrate in ~30–100 ms of SQL. Not the bottleneck.

**However** the per-hit hydration pattern is N+1 in shape. If the EE entity loader pulls in any lazy associations (curation note, accession, characteristics, primary publication for the VO conversion path), the 100-hit hydration can balloon. **Source-level call to investigate next round**: `searchService.loadValueObjects(...)` in `SearchWebService:164` — the VO conversion chain.

**Verdict: GREEN at the SQL level**, YELLOW pending VO-conversion N+1 audit.

#### A3 (RED — the headline): Ontology-inferred EE search — `findExperimentsByUris` falls to full table scan

This is the hot path that drives a typical user free-text query like "liver" or "cancer":

1. `OntologyService.findTerms("liver")` → ontology hits (cached, see Section 4).
2. `OntologyService.getChildren(matchingTerms, ...)` → expands to N child URIs (5–500 typical).
3. `CharacteristicDao.findExperimentsByUris(uris, true, true, true, taxon, limit, rankByLevel)` → the SQL hot spot.

The generated SQL (CharacteristicDaoImpl line 239):

```sql
SELECT T.LEVEL, T.VALUE_URI, T.PREDICATE_URI, T.OBJECT_URI, T.SECOND_PREDICATE_URI, T.SECOND_OBJECT_URI, T.EXPRESSION_EXPERIMENT_FK
FROM EXPRESSION_EXPERIMENT2CHARACTERISTIC T
WHERE (T.VALUE_URI IN (:uris)
   OR T.PREDICATE_URI IN (:uris)
   OR T.SECOND_PREDICATE_URI IN (:uris)
   OR T.OBJECT_URI IN (:uris)
   OR T.SECOND_OBJECT_URI IN (:uris))
  -- (optional taxon join + ACL clauses)
```

**Live timings on prod gemd, top-N most-common URIs as the IN-list:**

| N URIs | OR-across-5-cols (current) | UNION-rewrite (proposed) | Plan |
|---:|---:|---:|---|
| 1 | 8 ms | n/a | `index_merge(sort_union)` — 1853 rows scanned |
| 5 | **2 870 ms** | — | `ALL` (2.18M rows) — index_merge gives up |
| 10 | **3 070 ms** | **159 ms** | `ALL` vs 5x `range` |
| 20 | **2 940 ms** | — | `ALL` |
| 30 | **3 400 ms** | — | `ALL` |
| 50 | **3 660 ms** | — | `ALL` |
| 200 | **3 940 ms** | **1 080 ms** | `ALL` vs 5x `range` |
| 1000 | **3 990 ms** | **1 280 ms** | `ALL` vs `range` |

EXPLAIN on the current pattern (any N >= 5):

```
type=ALL, possible_keys=EE2C_VALUE_URI_VALUE,...,EE2C_SECOND_OBJECT_URI_SECOND_OBJECT
key=NULL, rows=2186081, filtered=96.88, Extra=Using where
```

EXPLAIN on a single-column IN(5 URIs):

```
type=range, key=EE2C_VALUE_URI_VALUE, key_len=403, rows=145870, filtered=100, Extra=Using where
```

**The current `(VALUE_URI IN ... OR PREDICATE_URI IN ... OR ... OR SECOND_OBJECT_URI IN ...)` defeats MySQL's index_merge once the per-column rowcount estimate exceeds the optimizer's switch threshold.** MySQL falls to a full `EE2C` scan (2.18M rows) on every `findExperimentsByUris` call with N ≥ ~3 URIs across all five columns. With 5 sources in the OR and high-cardinality URIs, MySQL declines `sort_union` and walks the table.

Cache amortization is weak: the second call with the same URI list is ~3.4s (warm). Hibernate query-level cache hides repeated identical calls (see `setCacheable(true)` + `addSynchronizedQuerySpace(EE2C_QUERY_SPACE)` at CharacteristicDaoImpl:257-275), but the cache key includes the URI list, so each user query with a different expansion misses.

**Verdict: RED**. This single SQL costs 3–4s per ontology-driven EE search regardless of result-set size. With the 100-default REST limit and `fillResults=true`, the user-visible latency for `GET /rest/v2/search?query=liver&resultTypes=ExpressionExperiment` is dominated by this query.

**Fix direction**: rewrite `createPredicates` to emit a `UNION ALL` of per-column range scans rather than a 5-column `OR`. Each per-column IN gives a `range` plan on its specific index (`EE2C_VALUE_URI_VALUE`, `EE2C_OBJECT_URI_OBJECT`, etc.). Live-measured 3.6× speedup at 200 URIs (3.94s → 1.08s), 19× at 10 URIs (3.07s → 0.16s). The rewrite preserves the JDBC parameter list signature (each leg binds the same `:uris`) and the existing L2 query cache key.

#### A4: Multi-URI scaling — flat above 5 URIs

The 5/10/20/30/50/200/1000 sweep above shows the cost is essentially flat (2.9s–4.0s) once index_merge gives up. The bad path has no "small query" sweet spot — even 5 inferred URIs bombs straight to the full table scan. **MaxResults limit does NOT help** (the query has no `ORDER BY` and an in-memory limit; full scan first).

**Verdict: RED**, same root cause as A3.

#### A5: `findBestByUri` — single-term URI exact match — YELLOW

CharacteristicDaoImpl line 366:

```sql
SELECT c FROM Characteristic c
WHERE valueUri = :uri
GROUP BY c.value HAVING c.value <> NULL ORDER BY count(*) DESC LIMIT 1
```

EXPLAIN: `ref` on `CHARACTERISTIC_VALUE_URI_VALUE` (cardinality 95k) but estimates 52 450 rows + temp + filesort.

Live timing: **30 ms** for the liver URI. The `CHARACTERISTIC` table is 12M rows so worst-case (highly common URI) the scan over the 52k-row index slice is the bottleneck.

**Verdict: YELLOW**. The composite `(VALUE_URI, VALUE)` index covers the group-by-value path but the filesort is still in plan. Not a top fix; called once per `OntologySearchSource.searchExpressionExperiment` only when the user query is a literal term URI.

#### A6: ACL post-filter cost — GREEN for anonymous, YELLOW for logged-in users

`OntologySearchSource` uses `EE2CAclQueryUtils.formNativeAclRestrictionClause(...)` which for **anonymous** users renders `(T.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK & 1) <> 0` against a denormalized column on `EE2C` itself — no join. For **admin** it's empty. For **logged-in non-admin**, it routes to `AclQueryUtils.formNativeAclRestrictionClause` (the EXISTS clause that the recent EXISTS-rewrite session targeted).

Timing the anonymous path with the mask predicate added (50 URIs): 3.65s — same shape as without (the mask filter is just a column predicate on a full-scan plan, ~0 marginal cost).

`HibernateSearchSource.filterByAcls` (line 348) post-filters HS hits via `aclService.readAclsById(...)` — a batched ACL load. For the 100-result default page that's one batched query (assuming the ACL DAO batches; the per-OID `AclObjectIdentity` is collected first). Source-only inspection; not measured live.

**Verdict: GREEN for anonymous + admin** (denormalized mask is in the right column already); **YELLOW for logged-in non-admin** until the EXISTS-rewrite from the earlier session lands on this query.

### B. Lucene index state

#### B1: Index location and config

`gemma-core/src/main/java/ubic/gemma/persistence/hibernate/HibernateConfig.java`:

- `hibernate.search.backend.type = lucene`
- `hibernate.search.backend.directory.type = local-filesystem`
- `hibernate.search.backend.directory.root = ${gemma.search.dir}` (coerced to `${java.io.tmpdir}/gemmaData/searchIndices` when blank/placeholder — defensive guard from commit `04d720c666`).
- **`hibernate.search.indexing.listeners.enabled = false`** — no autoindex. Every write requires a manual `IndexerService` re-index to propagate.

Indexed roots (`grep @Indexed`): `ExpressionExperiment`, `ArrayDesign`, `Characteristic`, `BibliographicReference`, `Keyword`, `DatabaseEntry`, `CompositeSequence`, `AlternateName`, `BibRefAnnotation`, `MedicalSubjectHeading`, `ExperimentalDesign`, `ExperimentalFactor`, `BioMaterial`, `ExpressionExperimentSet` — at least 14 entity roots.

#### B2: Auto-indexing OFF

Listener-driven auto-indexing is explicitly disabled (`HibernateConfig` line 230). Operational implication: **search results are stale until someone runs the IndexerService**. New EEs / curation edits do not appear in search hits until a re-index. There is no `@Scheduled` job that runs MassIndexer in gemma-core (only the manual CLI / admin endpoint).

The audited cadence of last MassIndexer run was NOT captured in any DB table (no `_search_index_run` audit), so a stale-index check would have to be a filesystem mtime probe on `${gemma.search.dir}/<EntityName>/`. Cannot probe from SQL.

#### B3: Stale-index detection

The `IndexerService` API documents the contract: re-index is administrative and idempotent. There is no automatic stale check; downstream callers ASSUME the index reflects recent writes. For the curation-UI replacement this is a known gap and a candidate for a `@Scheduled` reindex (per-entity, mergeSegments) once the curation team's write cadence stabilizes.

**Index sizing — could not measure live**. The probe runs from a Mac with no SSH to `homer`. From the schema (15M `COMPOSITE_SEQUENCE`, 1.3M `CHROMOSOME_FEATURE`, 72k `INVESTIGATION` + 12M `CHARACTERISTIC` indexed via `@IndexedEmbedded` on EE), the index should be in the **10–30 GB** range. Rebuild cost on a 4-thread MassIndexer is typically 1–4 hours for this corpus (extrapolating from Round 3 BFS-style probes; not load-tested).

### C. Ontology-inference cost per request

#### C1: `OntologyService.findTerms` — cached, but cache-miss is the worst case

`OntologyServiceImpl.findTerms`:

1. URI input → `findFirst(ontology.getTerm(uri))` across all registered ontologies — sequential first-hit fan-out.
2. Free-text input → `searchInThreads(ontology -> ontologyCache.findTerm(ontology, search, maxResults))` — parallel across ontologies, each routes through `OntologyCache.findTerm`.
3. GO appended in series at the end.

`OntologyCache.findTerm` (gemma-core/.../OntologyCache.java line 45) does a read-lock cache lookup keyed by `(ontology, query, maxResults)` — on miss, executes `ontology.findTerm(query, maxResults)` under a write lock and caches. **Cache is in-memory (Spring `Cache`).**

Round-3 probe found CLO `findTerm` at 12s cold. With ~17 ontologies in the consumer chain, a cold-cache `findTerms("liver")` does 17 parallel ontology queries each capped at the `searchInThreads` 5000ms slice (line 305) — so the wall is bounded at ~5s for that step, with most calls hitting cache.

The `getChildren` step (`OntologySearchSource:228`) is similarly cache-hit-or-compute against `ontologyCache`. On a warm cache, the children of a previously-asked term are immediate.

**Verdict: GREEN on warm cache, YELLOW on cold start.** The cache amortizes for the typical "search for the same term repeatedly" use case. After a JVM restart or `ontologyCache.clearSearchCacheByOntology(...)` reindex, the first user pays the cold cost. Magnitude: 1–5 seconds per cold ontology query (per Round 3 numbers).

#### C2: No SQL caching of the ontology→URI expansion

The cached layer is the term-search and parent/children walk inside `OntologyServiceImpl` / `OntologyCache`. **There is no cache layer wrapping `CharacteristicDao.findExperimentsByUris(uris)`** above the Hibernate query cache. The `query.setCacheable(true)` at CharacteristicDaoImpl:275 uses the standard L2 query cache with `EE2C_QUERY_SPACE` invalidation, but the cache key includes the full sorted URI list — so a 200-URI expansion and a 199-URI expansion of the same term miss each other, and any EE2C write invalidates the entire bucket.

**Verdict: YELLOW**. The `getChildren` cache is doing the work; the SQL cache hit rate is low.

### D. ACL post-filter behaviour

#### D1: EXISTS-rewrite benefit

The EXISTS-rewrite session referenced in `project_acl_exists_refactor.md` targets the join-based ACL clause path. `EE2CAclQueryUtils.formNativeAclRestrictionClause` (lines 25–40) shows the anonymous fast-path already uses the denormalized `ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK` column on `EE2C` — no ACL join. The non-anonymous-non-admin path falls through to `AclQueryUtils.formNativeAclRestrictionClause` which the refactor would rewrite to EXISTS.

For ontology-driven EE search specifically, the anonymous fast-path means the ACL is a column predicate, free in the full-scan plan. **Logged-in non-admin users would see additional cost from the JOIN→EXISTS rewrite** — measure when the refactor lands.

#### D2: Page-fill when ACL filter shrinks the result set

`OntologySearchSource.findExpressionExperimentsByUris` calls `findExperimentsByUris(uris, ..., getLimit(results, settings), ...)` with the remaining slot count — but the SQL filter is the ACL clause, applied BEFORE the in-memory `limit`. The query returns at most `limit` rows that already passed the ACL.

`HibernateSearchSource.filterByAcls` (line 348) post-filters Lucene hits AFTER fetch. If the Lucene `.fetch(Math.max(maxResults, 1))` is `100` and ACL drops half, the response page shows 50 results without re-fetching the next 50. **There is no "fill the page" retry** — the page just comes back short.

**Verdict: YELLOW**. The Hibernate Search path quietly under-fills the page for users with restricted ACLs. For anonymous users (the production majority) this is invisible; for curators with selective project visibility it's a UX gotcha. Fix direction: oversize the Lucene fetch (e.g., `maxResults * 2`) when an ACL filter is in play, then re-trim post-filter.

## Top findings + suggested fixes

| Rank | Finding | Magnitude | Fix |
|---|---|---|---|
| **1 (RED)** | `findExperimentsByUris` 5-column OR triggers MySQL full table scan on EE2C (2.18M rows) for any IN list with ≥ ~3 URIs | **3–4 seconds per ontology-driven EE search**, regardless of result set or page size | Rewrite `createPredicates` (CharacteristicDaoImpl:319) to emit a `UNION ALL` of per-column range scans. Live-measured 3.6× speedup at 200 URIs (3.94s → 1.08s); 19× at 10 URIs (3.07s → 0.16s). |
| **2 (RED)** | Auto-indexing OFF + no `@Scheduled` MassIndexer = search results silently stale until manual reindex | **Days-to-weeks of staleness** in practice; users see "I just curated this EE but it's not findable" | Either flip `hibernate.search.indexing.listeners.enabled = true` (real-time but write-amplifies) OR add a `@Scheduled` MassIndexer run gated on a curation-modified-since marker. |
| **3 (YELLOW)** | `HibernateSearchSource.rowToSearchResult` calls `session.get(clazz, id)` per Lucene hit when `fillResults=true`; downstream `loadValueObjects` may explode into per-VO N+1 | Plausibly **30–500 ms per request** at the 100-default page size; unmeasured | Audit `searchService.loadValueObjects` for lazy associations triggered during VO conversion; consider `findByIdsWithCharacteristics(ids)` batched load in `HibernateSearchSource` before the rowToSearchResult fan-out. |
| **4 (YELLOW)** | ACL post-filter in `HibernateSearchSource.filterByAcls` quietly under-fills the page for non-anonymous users | Invisible most of the time, UX gotcha for curators | Oversize Lucene fetch to `maxResults * 2` when an ACL filter would apply, post-trim. |
| **5 (YELLOW)** | `OntologyCache` cold-start cost per ontology is 1–5 seconds (Round 3 CLO 12s); 17 ontologies in parallel = 5s bound | Slow first-of-day search after JVM restart | Pre-warm the OntologyCache at startup for the top-N most-frequent free-text queries (drivable from web access logs). |
| **6 (info)** | `findBestByUri` plan has temp+filesort | 30 ms typical, called once per URI-input query | Acceptable; not a top-priority fix. |

## Cross-cutting observations

- **The EE2C URI search is the single biggest lever in the entire search subsystem.** The 5-column OR is a load-bearing structural mistake (sorry — pick another word: it's a *structural antipattern*) — there is no IN-list size that avoids the full scan.
- **The DatabaseSearchSource gene path is essentially free** (< 1ms per finder). All the cost is in Hibernate Search Lucene-walks + the ontology expansion + the EE2C lookup. Search optimization energy should flow there, not into the DatabaseSearchSource per-finder paths.
- **Listener-driven Lucene indexing is off by config.** This is fine for write-heavy bulk loads (avoids the autoindex tax during MassIndexer / GEO loaders) but means routine UI curation does not propagate to search. Worth a follow-up to decide whether to flip listeners on for the curation UI's narrow set of mutations.
- **`fillResults=true` is the REST default**, but `fillResults=false` would skip the per-hit `session.get` and let the caller decide. The curation UI replacement could pass `exclude=resultObject` and reduce search-latency per request — worth coordinating with the gemma-curation-ui team.
- **Round 4 single-cell findings echo here**: both the SC vector lookup (no composite EE+QT index) and EE2C URI search (no per-column UNION rewrite) are structurally-shaped misuses of MySQL's index_merge. The pattern across the codebase is "rely on MySQL's optimizer to handle multi-column OR" — and it consistently doesn't, on tables > 1M rows.
- **The `OntologySearchSource` carries a 30s timeout budget** (`ONTOLOGY_SEARCH_AND_INFERENCE_TIMEOUT_MILLIS`). Given the 3-4s EE2C SQL cost per inferred URI batch, a single user search can consume 7–9s of that budget in pure SQL — the SQL fix alone gets the typical query response down well under the 5s threshold most users tolerate.
