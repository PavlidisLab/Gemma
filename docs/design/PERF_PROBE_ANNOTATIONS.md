# Perf probe — Annotations + Characteristic (live gemd, 2026-05-20)

Baseline SHA: `ae28039634` on `perf-probe-annotations-characteristic` (off `phase2-acl-migrate`).
Target: live `gemd` via 127.0.0.1:8000 port-forward (READ-ONLY).
MySQL 5.7.44, InnoDB. All EXPLAIN-level + timing only; no schema changes.

## Setup

| Table | Rows |
| --- | --- |
| `CHARACTERISTIC` | 12,042,228 (11.86M discriminator NULL Characteristic + 177k Statement) |
| `EXPRESSION_EXPERIMENT2CHARACTERISTIC` (EE2C denormalized) | 2,531,907 |
| `INVESTIGATION` (parent of EE characteristics via INVESTIGATION_FK) | ~28k (proxy: cardinality of FKC index = 55,827; ~19,576 distinct EEs in EE2C) |

> Note: there is no separate `STATEMENT` table. Statements live inside `CHARACTERISTIC` with `class = 'Statement'` discriminator and the second-predicate / second-object columns. EE2C is the access-optimized denormalized projection (one row per characteristic occurrence per EE-rooted entity).

EE2C carries `ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK` — a per-row denormalized ACL bitmask that lets anonymous reads filter with `(mask & 1) <> 0` and skip the join to `ACL_OBJECT_IDENTITY`. The base CHARACTERISTIC table has NO ACL column; ACL must be applied via the parent (INVESTIGATION etc.).

`CHARACTERISTIC` covering indexes:
- `(CATEGORY_URI(100), CATEGORY, VALUE_URI(100), VALUE)` — wide 4-col composite
- `(VALUE_URI(100), VALUE)`, `(OBJECT_URI(100), OBJECT)`, `(PREDICATE_URI(100), PREDICATE)`, `(SECOND_OBJECT_URI(100), SECOND_OBJECT)`, `(SECOND_PREDICATE_URI(100), SECOND_PREDICATE)`
- single-col: `(VALUE)`, `(CATEGORY)`, plus an FKC for every parent-entity FK.
- prefix length on `_URI` columns is 100; long ontology URIs (e.g. CXG-style with parameter strings) lose selectivity past 100 chars.

`EXPRESSION_EXPERIMENT2CHARACTERISTIC` mirrors the same shape — `(VALUE_URI(100), VALUE)`, the `OBJECT_*` / `SECOND_*` pairs, plus `EE2C_LEVEL` and the wide `(CATEGORY_URI, CATEGORY, VALUE_URI, VALUE)` composite. **No covering index on `(VALUE_URI, EXPRESSION_EXPERIMENT_FK)`** — i.e. the count-by-URI projection still has to load the row to read the EE FK.

## Probe inventory

### Probe A1 — `findByValue` LIKE (autocomplete backbone, CharacteristicDaoImpl:498)

DAO emits `select {C.*} from CHARACTERISTIC where C.VALUE like :search [+ category? + parent-class constraint?]`. The realistic shape called by `OntologyServiceImpl.findExperimentsCharacteristicTags` (gemma-rest /annotations/search) is `prepareDatabaseLikeQuery(searchQuery) + parentClasses = {ExpressionExperiment.class}, includeNoParents = false, maxResults = 1000`. `prepareDatabaseLikeQuery` produces `search%` (prefix only).

| Query | Hits | Timing | Verdict |
| --- | --- | --- | --- |
| `VALUE LIKE 'liver%'` (count, no LIMIT, no parent) | 45,378 | 15 ms | GREEN |
| `VALUE LIKE 'neur%'` (count) | 66,949 | 19 ms | GREEN |
| `VALUE LIKE 'homo sap%'` | 721 | 0.4 ms | GREEN |
| `VALUE LIKE '%liver%'` (count) | 58,461 | **5,423 ms** | RED — full scan |
| `VALUE LIKE '%cell%'` (count) | 684,541 | **2,944 ms** | RED — full scan |
| **REAL shape**: `LIKE 'liver%' AND INVESTIGATION_FK IS NOT NULL AND INVESTIGATION_FK IN (...EE)` LIMIT 1000 | 163 | **110 ms** | YELLOW |
| **REAL shape**: `LIKE 'cell%' AND ...` LIMIT 1000 | 163 | **143 ms** | YELLOW |

EXPLAIN: `range CHARACTERISTIC_VALUE` with 125,610 estimated rows; filtered down to 2.23% by the IN-subselect on `INVESTIGATION` (eq_ref join). The 100–140ms is the cost of scanning 125k VALUE-index entries on `cell%`/`liver%` and doing an eq_ref lookup to `INVESTIGATION` for each survivor. Only ~163 rows survive (those rooted on a real EE), but the scan still has to touch every prefix match.

> Fix direction: This is the autocomplete query — invoked on every keystroke once length>=3. 110ms/keystroke is right at the edge of "feels laggy". Two options: (1) push the parent-class constraint into a covering composite index, e.g. `(VALUE, INVESTIGATION_FK)` so the index range scan can drop rows that don't have an EE root. (2) Build the autocomplete out of EE2C (which only contains EE-rooted entries) — substring-prefix LIKE on EE2C.VALUE hits a 2.5M-row table instead of 12M and is automatically scoped to EE.

### Probe A2 — `findByCategory(String)` (CharacteristicDaoImpl:144)

| Query | Hits | Timing | Verdict |
| --- | --- | --- | --- |
| `CATEGORY = 'organism part'` (count) | 467,387 | 198 ms | YELLOW |
| `CATEGORY = 'disease'` (count) | 70,463 | 25 ms | GREEN |
| `CATEGORY = 'cell type'` (count) | 937,036 | **343 ms** | YELLOW |

EXPLAIN: `ref CHARACTERISTIC_CATEGORY`, ~792k rows for the worst case. Selectivity is reasonable but the result set is huge because the call is unbounded — the API method has no limit parameter and returns ALL characteristics with that category. Mostly used in bulk/dev paths (it returns full entities, so `findByCategoryUri` + parent-class limits are the user-facing form, see A4); flag for review.

> Fix direction: this method has no maxResults — fix that. Or deprecate in favour of `findByCategoryUri(uri, parentClasses, includeNoParents, maxResults)`.

### Probe A3 — `findByUri(String)` exact (CharacteristicDaoImpl:349)

| Query | Hits | Timing | Verdict |
| --- | --- | --- | --- |
| `VALUE_URI = 'UBERON_0002107'` (liver, common) | 29,557 | 20 ms | GREEN |
| `VALUE_URI = 'CL_0000540'` (neuron) | 6,195 | 4.6 ms | GREEN |
| `VALUE_URI = '<nonexistent>'` | 0 | 0.15 ms | GREEN |

EXPLAIN: `ref CHARACTERISTIC_VALUE_URI_VALUE` const lookup. No drama.

### Probe A4 — `findByUri` + parent-class constraint (the real REST URI-search path)

Called by AnnotationsWebService.getTerms() lines 517-518 when the search query itself looks like a URI.

| Query | Hits | Timing | Verdict |
| --- | --- | --- | --- |
| `VALUE_URI = 'UBERON_0002107' AND (full 9-FK OR clause)` (count, no LIMIT) | 27,510 | **74 ms** | GREEN |
| Same, LIMIT 100 | 100 | 1.7 ms | GREEN |

EXPLAIN: `ref CHARACTERISTIC_VALUE_URI_VALUE`, 52,450 estimated rows. The 9-FK OR clause produces a wide `possible_keys` list but the optimizer picks the VALUE_URI index correctly. ~74ms unbounded is acceptable.

### Probe A5 — Per-EE characteristic listing (curation page)

| Query | Hits | Timing | Verdict |
| --- | --- | --- | --- |
| `CHARACTERISTIC WHERE INVESTIGATION_FK = 7252` | small | 0.5 ms | GREEN |
| `EE2C WHERE EXPRESSION_EXPERIMENT_FK = 7252` (all-EE-rooted: EE itself + BM + ED) | small | 18 ms | GREEN |
| BioMaterials per-EE via BIO_ASSAY join | bounded | 3 ms | GREEN |

> EE2C is the right table for the curation surface — it includes EE+BM+ED rows in one scan.

### Probe A6 — `findBestByUri` (group-by + count desc + LIMIT 1)

| Query | Timing | Verdict |
| --- | --- | --- |
| `GROUP BY VALUE … WHERE VALUE_URI = UBERON_0002107` (29,557 rows) | 30 ms | GREEN |
| Same for CL_0000540 (6,195 rows) | 6 ms | GREEN |

Indexed range scan on `VALUE_URI_VALUE` covers both columns — fast.

### Probe A7 — `findValueGroupedByValueUri` (full-table scan, CharacteristicDaoImpl:436)

| Query | Timing | Verdict |
| --- | --- | --- |
| `SELECT VALUE_URI, VALUE FROM CHARACTERISTIC GROUP BY VALUE_URI` (no limit, no parent constraint) | **10,730 ms** | RED |

EXPLAIN: `type=ALL` (10M+ rows), `Using temporary; Using filesort`. Only called by the gemma-cli CompleteCli (not user-facing), but worth noting — anything that triggers this will lock a connection for ~11s. Should be gated to `maxResults` (it already accepts one but the CLI doesn't set it).

### Probe B1 — `findExperimentsByUris` (CharacteristicDaoImpl:177) — the engine of `/annotations/search`'s usage-count enrichment and the search-by-URI path in OntologySearchSource

The most consequential native query in the annotations stack. After ontology expansion it can receive many URIs and is invoked with `includeSubjects = includePredicates = includeObjects = true`, exercising a 5-column OR on EE2C.

| Query | Hits | Timing | Verdict |
| --- | --- | --- | --- |
| 1 URI × 5 columns (UBERON_0002107) | 5,930 | 9 ms | GREEN |
| 10 URIs × 5 columns | 5,930 | 27 ms | GREEN |
| 10 URIs × 1 column (VALUE_URI only) | 5,822 | 9 ms | GREEN |
| **50 URIs × 3 columns** (VALUE_URI + OBJECT_URI + SECOND_OBJECT_URI), warm | 151,034 | **1,092 ms** | RED |
| Same, re-run hot | 151,034 | **1,098 ms** | RED |
| 50 URIs × 5 columns + anonymous ACL mask | — | (not measured separately — ACL adds ~0 ms, see Probe D) | — |

EXPLAIN on the 10-URI single-column variant: `range EE2C_VALUE_URI_VALUE`, 44,935 rows. EXPLAIN on the multi-column OR variant: `index_merge sort_union` across 5 indexes. Index merge sort_union is expensive because (a) it scans each of the 5 indexes for matching URIs, (b) sorts and unions row-id lists in memory, (c) then fetches each row.

> Fix direction: at 50 URIs the planner picks index_merge sort_union and the work is dominated by the 5-way merge. A composite/covering index doesn't help with 5-OR.  Options:
> 1. Split into 5 separate queries (one per column) and UNION ALL in the application — each becomes a simple ref lookup. Sum of 5 × 9ms ≈ 45 ms — a 24× speedup over 1.09 s.
> 2. Rewrite as UNION ALL in SQL — same effect.
> 3. Normalize: most callers only need VALUE_URI hits (subject). The default `includeSubjects/Predicates/Objects = (true,true,true)` is rarely the *intent* — `/annotations/search` reports counts under a VALUE_URI key only; predicate/object hits inflate the count. Worth checking whether the existing caller actually needs to count predicate/object matches.

### Probe B2 — `/annotations/categories` and `/annotations/predicates`

In-memory (`OntologyServiceImpl.getCategoryTerms` iterates over a `Set<OntologyTerm>` loaded from `EFO.factor.categories.txt`). No DB hit. GREEN.

### Probe B3 — `/annotations/parents` and `/annotations/children`

Three-step flow:

1. `ontologyService.getTerm(uri, …)` — in-memory ontology cache. GREEN.
2. `ontologyService.getParents/getChildren(…, direct, includeAdditionalProperties, timeout)` — in-memory ontology recursion via `OntologyCache`. The `direct=false` recursive path can be slow for deeply-rooted terms (e.g. UBERON anatomic_entity) but lives outside the DB.
3. `getDistinctEeCountsByUri(uris)` — DB-bound, calls `findExperimentsByUris(uris, true, true, true, null, -1, false, false)`. This is Probe B1 with `uris.size()` = number of returned parent/child terms (can be hundreds for non-`direct` calls).

> When `direct=false` on a high-up term (e.g. `/annotations/children?uri=http://purl.obolibrary.org/obo/UBERON_0000061` anatomical structure), step 3 hits the 50-URI × 5-column shape Probe B1 measured at >1 s.

### Probe D — Cold vs warm split

Re-ran the 50-URI × 3-column query back-to-back: **1.092 s** then **1.098 s**. No buffer-pool effect — the cost is in index_merge sort_union work, not I/O. Adding `AND (ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK & 1) <> 0` on the 10-URI × 5-column variant kept timing flat at ~21–28 ms; the ACL fast path is essentially free for anonymous reads.

We didn't see the DEA-style 3.5 s cold tax on these queries. The annotations hot paths are CPU-bound on B-tree work, not I/O-bound.

## Top findings (ranked by impact × frequency)

### 1. `findExperimentsByUris` with many URIs × multi-column OR — RED, 1+ s warm

`/annotations/search` and `/annotations/{parents,children}` end up calling `findExperimentsByUris(uris, true, true, true, …)`. With 50 expanded URIs across 3 of 5 columns: **1.09 s consistently** — the index_merge sort_union plan loads ~150k EE2C rows. Every annotation search the user types ultimately pays this cost when computing usage counts.

Fix direction: split the 5-OR query into 5 single-column `ref` queries and union in the app (or as SQL UNION ALL). Expected drop from 1.09 s → ~50 ms.

### 2. `findByValueLike` autocomplete real shape — YELLOW, 110-140ms per keystroke

`LIKE 'liver%' AND INVESTIGATION_FK in (EE)` LIMIT 1000 takes 110-140 ms. This is the autocomplete prefix on `/annotations/search`. The optimizer picks `CHARACTERISTIC_VALUE` range and does 125k eq_ref joins to `INVESTIGATION`. A covering composite `(VALUE, INVESTIGATION_FK)` (or, better, route the autocomplete through EE2C.VALUE which is EE-scoped by construction) would reduce work substantially. Currently borderline acceptable for one-off autocomplete; janky if the user types fast.

### 3. `findValueGroupedByValueUri` full-table scan — RED in CLI, not currently in user path

10.7 s, type=ALL, temp + filesort. Only `gemma-cli CompleteCli` calls it. Document a maxResults default in the CLI path; not urgent for end-users.

## Index recommendations

| Table | Column(s) | Reason | Migration sketch |
| --- | --- | --- | --- |
| `CHARACTERISTIC` | `(VALUE, INVESTIGATION_FK)` composite | Autocomplete path scans 125k VALUE-prefix matches then eq_ref joins to INVESTIGATION; covering composite lets MySQL drop non-EE rows during the range scan. | `ALTER TABLE CHARACTERISTIC ADD INDEX IDX_CHARACTERISTIC_VALUE_INVESTIGATION_FK (VALUE(64), INVESTIGATION_FK);` — Flyway migration on phase2-acl-migrate. Size estimate: ~12M rows × (64 prefix + 8 FK + 7 overhead) ≈ 950 MB index. Defer until benchmarked against the EE2C-redirect alternative below. |
| `EXPRESSION_EXPERIMENT2CHARACTERISTIC` | `(VALUE_URI(100), EXPRESSION_EXPERIMENT_FK)` covering | The B1 50-URI × 5-OR shape returns 151k rows; if a covering index existed for the subject case, the planner could avoid loading the row for the COUNT-by-EE-FK projection. | `ALTER TABLE EXPRESSION_EXPERIMENT2CHARACTERISTIC ADD INDEX IDX_EE2C_VALUE_URI_EE_FK (VALUE_URI(100), EXPRESSION_EXPERIMENT_FK);` — but inspect whether the planner picks it over the existing `EE2C_VALUE_URI_VALUE`; may need explicit hint. **Lower priority than the query rewrite in Top Finding #1.** |
| (no new index needed for B1 multi-column OR) | — | Sort_union is intrinsic to the 5-OR shape; new indexes won't help. Fix is to split the query. | — |

No new index is the highest-leverage fix. The largest gain comes from the query rewrite in Top Finding #1.

## REST-vs-DAO cost split

For the `/annotations/search?query=liver` endpoint (representative shape, anonymous request):

| Stage | Where | Estimated cost |
| --- | --- | --- |
| Jersey routing + param parse | gemma-rest | ~ms (Jersey) |
| `findExperimentsCharacteristicTags` — DB free-text | CharacteristicDao.findByValueLike (real shape, Probe A1) | **110–140 ms** |
| `findCharacteristicsFromOntology` — in-memory ontology threads | OntologyService | ms-to-seconds depending on which ontologies are loaded (out of scope for this probe) |
| `getDistinctEeCountsByUri` — DB usage-count enrichment | CharacteristicDao.findExperimentsByUris (Probe B1) | **30 ms to >1 s** depending on \|uris\| |
| ACL filter | `(mask & 1) <> 0` for anonymous; admin = skip; authenticated = real join | anonymous: free; authenticated user: + ACL_OBJECT_IDENTITY join overhead (not measured live) |
| JSON serialization (Jackson) | gemma-rest | ms for ≤1000 hits |

So end-to-end wall time for an anonymous `/annotations/search` is dominated by:
- the ontology in-memory search (untouched here)
- the `findByValueLike` autocomplete query (~120 ms)
- the `findExperimentsByUris` usage-count query (~30 ms with few URIs, up to 1+ s with 50)

ACL is essentially free for anonymous (EE2C carries the denormalized mask). For authenticated users, ACL adds an `ACL_OBJECT_IDENTITY` JOIN, which we didn't probe — round-3 DEA already showed this can be expensive on hot paths.

## Cross-cutting observations

1. **`includeSubjects = includePredicates = includeObjects = true` is the universal default at the callsites of `findExperimentsByUris`** (AnnotationsWebService:545 and OntologySearchSource:292). Neither code path actually scores predicate / second-object hits differently from subject hits — both just count distinct EEs per URI. The whole 5-column OR exists "to be safe", and it's the worst-case for the optimizer. Worth a code-level audit: do we actually need to count when a URI shows up in OBJECT_URI / SECOND_OBJECT_URI separately from VALUE_URI?

2. **EE2C is the right table to drive almost everything user-facing.** It's 5× smaller (2.5M vs 12M rows), already filtered to EE-rooted entries, and carries the anonymous-ACL fast path. Where the current code goes to `CHARACTERISTIC` directly and then enforces the parent-class constraint, redirecting to EE2C usually wins. The autocomplete (A1) is the cleanest example.

3. **No `class` index on `CHARACTERISTIC.class`** with practical selectivity — it has cardinality 1 in `SHOW INDEXES` because 99% of rows are NULL (vanilla Characteristic). If someone tries to query "all Statements" they will full-scan.

4. **EE2C maintenance is opaque from this probe** — the table is rebuilt by `TableMaintenanceUtil`. If updates lag, search counts will be stale but won't slow down. Worth a separate probe on the maintenance job.

5. **The `_URI(100)` prefix indexes are tight**. UBERON / EFO / CHEBI URIs fit comfortably under 100 chars; CXG-style URIs with embedded parameters can exceed 100 and lose selectivity. Not an issue today but worth knowing if we ingest such terms in future.

6. **`findExperimentsCharacteristicTags` has a 200 ms internal warn threshold** (OntologyServiceImpl:203) — meaning the team already knows this can be slow. The native B1 shape almost certainly crosses that threshold whenever ontology expansion is broad.

## Probes executed: 11

A1 (× 4 sub-variants), A2 (× 3), A3 (× 3), A4 (× 2), A5 (× 3 sub-shapes), A6 (× 2), A7, B1 (× 6 sub-shapes), B2 (source-read only), B3 (source-read + dependency on B1), D (cold-vs-warm baked into B1 reruns).
