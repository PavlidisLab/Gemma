# Live-gemd perf probe round 4 — single-cell hot paths

## Setup

- Worktree tip: `c2706ef71b1c536259ec0067074c52ec204f903a` (branch `perf-probe-round4-singlecell`).
- Probe host: Darwin 24.6.0, mysql client 5.7.31.
- Server target: prod gemd via local tunnel `127.0.0.1:8000`, user `gemmaadmin`, READ-ONLY.
- **Tunnel was DOWN for the entire window of this probe (connection refused at 8000).** Live `EXPLAIN` / timing data could NOT be collected. The probe therefore relies on:
  1. DDL inspection of `gemma-core/src/main/resources/db/migration/mysql/V1__prod_baseline.sql`.
  2. Source-only analysis of every DAO method that touches `SINGLE_CELL_EXPRESSION_DATA_VECTOR`, `SINGLE_CELL_DIMENSION`, and `CELL_LEVEL_CHARACTERISTICS`.
  3. Caller-graph sweep for non-streaming materialization foot-guns.
- Cardinality reminder (from Round 3, unchanged): `SINGLE_CELL_EXPRESSION_DATA_VECTOR` is **23.2 M rows / 1.12 TB data / 1.3 GB index**, with the biggest single EE (`56855 / GSE144136`) at 46,433 vectors / 2.6 GB blob payload.

## Schema reference (the part that matters)

```sql
CREATE TABLE `SINGLE_CELL_EXPRESSION_DATA_VECTOR` (
  `ID` bigint NOT NULL AUTO_INCREMENT,
  `SINGLE_CELL_DIMENSION_FK` bigint NOT NULL,
  `DESIGN_ELEMENT_FK` bigint NOT NULL,
  `DATA` longblob NOT NULL,
  `DATA_INDICES` longblob NOT NULL,
  `QUANTITATION_TYPE_FK` bigint NOT NULL,
  `EXPRESSION_EXPERIMENT_FK` bigint NOT NULL,
  `ORIGINAL_DESIGN_ELEMENT` varchar(255),
  PRIMARY KEY (`ID`),
  KEY (SINGLE_CELL_DIMENSION_FK),
  KEY (DESIGN_ELEMENT_FK),
  KEY (QUANTITATION_TYPE_FK),
  KEY (EXPRESSION_EXPERIMENT_FK)
);
```

Every key is a single-column FK index. **No composite index** on `(EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK)`, `(EXPRESSION_EXPERIMENT_FK, SINGLE_CELL_DIMENSION_FK)`, or `(EXPRESSION_EXPERIMENT_FK, DESIGN_ELEMENT_FK)`. Every DAO method below filters on `expressionExperiment = :ee AND quantitationType = :qt`. Compare to `RAW_EXPRESSION_DATA_VECTOR` (Round 3 A2) which had the same gap, and to `PROCESSED_EXPRESSION_DATA_VECTOR` which DOES have the composite `experimentProcessedVectorProbes (EE_FK, DESIGN_ELEMENT_FK)` — i.e. the prod schema fixed it for processed but not for raw or single-cell.

## Hotspot priority alignment

| Category | Worst observation | Verdict |
|---|---|---|
| **A. SC vector load — index** | No composite `(EE_FK, QT_FK)`; MySQL must `index_merge(intersect)` or fall back to one FK scan + post-filter | YELLOW |
| **A. SC vector load — Hibernate metadata** | SCEDV maps `singleCellDimension`, `designElement`, `quantitationType` as `lazy=false fetch=select` (no batch hint at field level) — large per-row hydration cost amplified by 23 M rows | RED |
| **B. SC dimension lookup** | Every `getSingleCellDimension*` / `getCellTypeAssignment*` / `getCellLevelCharacteristics*` method routes through `SingleCellExpressionDataVector scedv join scedv.singleCellDimension dim where scedv.ee = :ee … group by dim`. Scanning a 23 M-row table to find ONE dimension row. | RED |
| **C. Non-streaming composite reads** | `getSingleCellExpressionDataMatrix(ee, qt)` materializes the entire 2.6 GB blob payload into `Collection<SingleCellExpressionDataVector>` then into `SingleCellExpressionDataDoubleMatrix`. No streaming variant exists. | RED |
| **C. MEX writer** | `writeMexSingleCellExpressionData(...)` always non-streaming. The tabular writer streams when `fetchSize > 0`; MEX has no such branch. | RED |

The headline gap is **B — dimension/CTA lookups that join through the 23 M-row SCEDV table to retrieve a single dimension row**. This is the architectural mistake the rest of the SC layer is built on.

## Probe inventory

### A. SC vector load

#### A1: `ExpressionExperimentDaoImpl.getSingleCellDataVectors(ee, qt)`
- HQL (line 3614-3622): `select scedv from SingleCellExpressionDataVector scedv where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt`.
- Translated SQL: `SELECT v.* FROM SINGLE_CELL_EXPRESSION_DATA_VECTOR v WHERE v.EXPRESSION_EXPERIMENT_FK=:ee AND v.QUANTITATION_TYPE_FK=:qt`.
- EXPLAIN (predicted, given DDL):
  - Best case: MySQL chooses `index_merge(intersect)` of `EXPRESSION_EXPERIMENT_FK` + `QUANTITATION_TYPE_FK` indexes. Two ranged FK scans + intersection.
  - Worse case (if optimizer mis-estimates): single ref-scan on whichever FK has lower estimated cardinality, then row-level filter for the other. On big EEs with many QTs (preferred + raw counts + log-norm) the `EXPRESSION_EXPERIMENT_FK` ref scan walks ALL vectors for the EE (46k+ rows).
- Round-3 already measured the timing here (A3, "RED for full pull"). Round 4 calls out the missing composite index as the structural fix.
- **Verdict: YELLOW** at SQL plan level. **Suggested fix**: add composite `(EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK)` index. Cheap (small relative to the 1.3 GB existing index footprint) and avoids the intersect.

#### A2: `getSingleCellDataVectorWithoutCellIds(ee, qt, designElement)` — single-vector probe-page path
- HQL (line 3703-3717): adds `and scedv.designElement = :de`.
- EXPLAIN (predicted): no composite `(EE_FK, QT_FK, DE_FK)` and no `(DE_FK, EE_FK)`. The optimizer picks the most selective single-column FK (probably `DESIGN_ELEMENT_FK`), which for a popular probe across many EEs returns 100s of candidate rows that then filter on EE+QT in the WHERE.
- For an EE/probe combo this should still be fast (< 50 ms warm) — a probe is in at most one vector per (EE, QT) — but cold-cache may take 200–500 ms.
- **Verdict: YELLOW**. Composite `(DESIGN_ELEMENT_FK, EXPRESSION_EXPERIMENT_FK)` would help, but only if the single-probe SC fetch is a real user-facing path (it powers the per-gene SC view in gemma-curation-ui — confirm before paying the write cost on a 23 M-row table).

#### A3: `streamSingleCellDataVectors(ee, qt, fetchSize, ..., createNewSession)` — the cursor path
- DAO line 3641-3660. Same WHERE clause as A1; uses ScrollableResults + cursor fetch.
- Same composite-index gap as A1.
- **Prefetch step (line 3645-3651)** runs before the stream opens:
  1. `session.get(EE, id)` — one row.
  2. `session.get(QT, id)` — one row.
  3. `getSingleCellDimension(ee, qt, session)` — see B1 below; that's the 23 M-row group-by scan that Round 4 calls out as the worst.
  4. `prefetchDesignElements(ee, qt, session)` — `select vector.designElement from SCEDV where ee=:ee and qt=:qt` → loads ALL 46k CompositeSequence rows into the session cache.
- The prefetched-DE step is necessary because the SCEDV mapping has `designElement` as `lazy=false fetch=select` — without prefetching, the stream's row-by-row hydration fires 46k single-row SELECTs. With the prefetch, it's one batched query + first-level-cache hits. Solid.
- **Verdict: GREEN for the stream cursor itself, YELLOW for the prefetch chain** — three of those prefetches (B, design elements, dimension cellIds blob) read more than the stream consumer asked for.

#### A4: Non-streaming surface — every caller that loads ALL vectors at once

Source-grep (excluding test code, DAO impl, service impl forwarder):

| File:line | Call | Notes |
|---|---|---|
| `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/SingleCellExpressionExperimentServiceImpl.java:304` | `getSingleCellDataVectors(ee, samples, qt)` inside `getSingleCellExpressionDataMatrix(...)` | Full 2.6 GB pull, materialized into matrix |
| `…/SingleCellExpressionExperimentServiceImpl.java:325` | `getSingleCellDataVectors(ee, qt)` inside `getSingleCellExpressionDataMatrix(ee, qt)` (no-samples overload) | Same; called by `ExpressionDataFileHelperService:164-165` and `DetectQuantitationTypeCli:65` |
| `…/SingleCellExpressionExperimentServiceImpl.java:278` | `getSingleCellDataVectors(ee, qt)` inside `getSingleCellExpressionDataVectorMatrixAsOptional` | Wraps the full pull in `Optional` |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/service/ExpressionDataFileHelperService.java:136-137` | `getSingleCellVectors(ee, samples, qt, cs2gene)` for the tabular writer **non-streaming branch** | Tabular writer has both branches; chooses non-stream when `fetchSize == 0` |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/service/ExpressionDataFileServiceImpl.java:596` | `writeMexSingleCellExpressionDataInternal(...)` | **No streaming branch** — always non-streaming. Documented separately in C2. |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/singleCell/aggregate/SingleCellExpressionExperimentAggregateServiceImpl.java:94` | `getSingleCellDataVectors(ee, qt, vectorInitConfig)` when `fetchSize == 0` | Streaming branch at line 90 then `.collect(toList())` at line 92 — defeats the cursor. |
| `gemma-cli/src/main/java/ubic/gemma/apps/SingleCellDataWriterCli.java:296-298` | `getSingleCellDataVectors(...)` | CLI caller |

The matrix overload is the biggest production foot-gun — any REST or curation-UI path that calls `getSingleCellExpressionDataMatrix(ee, qt)` will OOM the JVM on a large SC EE. There is no streaming alternative for matrix construction; the type itself (`SingleCellExpressionDataDoubleMatrix`) requires all vectors up front.

### B. SC dimension load — the architectural gap

#### B1: `getSingleCellDimension(ee, qt)` — fetch the one dimension for one (EE, QT)
- HQL (line 2685-2693):
  ```sql
  select scedv.singleCellDimension
  from   SingleCellExpressionDataVector scedv
  where  scedv.expressionExperiment = :ee and scedv.quantitationType = :qt
  group by scedv.singleCellDimension
  ```
- Translated SQL must scan all rows for `(ee, qt)` in SCEDV, then group on `SINGLE_CELL_DIMENSION_FK`. For EE 56855 / preferred QT that's 46k rows minimum.
- The result is ONE row (one dimension per QT). The 46k-row scan is structural waste.
- **There is no FK from `SINGLE_CELL_DIMENSION` back to `EXPRESSION_EXPERIMENT` or to `QUANTITATION_TYPE`.** SCD currently only has `ID`, `NAME`, `DESCRIPTION`, `CELL_IDS` (LONGBLOB!), `NUMBER_OF_CELLS`, `BIO_ASSAYS_OFFSET` (LONGBLOB). The (EE, QT) → dimension mapping is encoded ONLY in the SCEDV table.
- **Verdict: RED**. The 30+ HQLs that do `… from SingleCellExpressionDataVector scedv … group by dim/cta/clc …` are all paying this tax to navigate a relationship that should be on a small side table.
- **Suggested fix**: introduce a `SINGLE_CELL_DIMENSION_TO_EXPERIMENT` (or `_TO_QUANTITATION_TYPE`) link table — `(EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK, SINGLE_CELL_DIMENSION_FK)` with composite index — populated at vector-write time. Replaces 46k-row scan with 1-row index lookup. Migration: derive from existing data via `SELECT DISTINCT EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK, SINGLE_CELL_DIMENSION_FK FROM SINGLE_CELL_EXPRESSION_DATA_VECTOR`.

#### B2: `getCellTypeAssignments(ee)` / `getCellTypeAssignments(ee, qt)` / variants
- HQL (lines 3287-3431, ~25 query bodies):
  ```sql
  select cta
  from SingleCellExpressionDataVector scedv
  join scedv.singleCellDimension scd
  join scd.cellTypeAssignments cta
  where scedv.expressionExperiment = :ee [ and scedv.quantitationType = :qt ] [ and cta.id = :ctaId | name = :ctaName | preferred = true ]
  group by cta
  ```
- Same problem as B1, compounded — same 46k-row SCEDV scan PLUS join through dim → CTA. The dim → CTA join itself is cheap (CTAs are a small set per dim) but the scan is wasted.
- **Verdict: RED**, same fix as B1 — once we have the dim ↔ EE link table, CTA lookups become `… from CellTypeAssignment cta join cta.singleCellDimension dim join LINK l on l.dim = dim where l.ee = :ee …`.

#### B3: `SingleCellDimension.cellIds` is an eager LONGBLOB
- Mapping (`SingleCellDimension.hbm.xml:13-21`): `cellIds` is a `CompressedStringListType` over a `LONGBLOB` column. The mapping does NOT mark it as a lazy property (Hibernate lazy props require `lazy="true"` and a build-time bytecode enhancer).
- Effect: ANY `getSingleCellDimension*` call that returns a hydrated `SingleCellDimension` entity (not the `*WithoutCellIds` variant) pulls the full `CELL_IDS` blob. For a 100k-cell dataset compressed at ~50% that's still tens of MB per call.
- The DAO mitigation is the `*WithoutCellIds` family (line 2634-2762), which projects only the small properties via the `SingleCellDimensionWithoutCellIdsInitializer` (selecting `id, numberOfCellIds, bioAssaysOffset` explicitly).
- **The streaming-vector prefetch path (line 3647) calls `getSingleCellDimension(prefetchedEe, prefetchedQt, session)` — the EAGER variant — for every stream open.** Anyone who streams SC vectors pays the CELL_IDS-blob tax on top, even if they never read `dimension.getCellIds()`.
- **Verdict: YELLOW**. Audit: does the stream consumer ever read `dimension.getCellIds()`? If not, switch the prefetch to `getSingleCellDimensionWithoutCellIds(ee, qt, session)`. Saves tens of MB per stream open.

### C. SC composite reads

#### C1: `getSingleCellExpressionDataMatrix(ee, qt)` — full-matrix assembly
- `SingleCellExpressionExperimentServiceImpl:324-341`. Calls `getSingleCellDataVectors(ee, qt)` (full pull), then `new SingleCellExpressionDataDoubleMatrix( vectors )`.
- For EE 56855 that's 2.6 GB of vector blobs in memory just to build the matrix wrapper.
- Mirrors the bulk-vector matrix anti-pattern Round 3 called out (B-series). The SC variant is worse because the per-EE blob payload is 1-2 orders of magnitude bigger.
- **Verdict: RED**. **Suggested fix**: matrix wrapper should be backed by a lazy / chunk-streamed view. Short-term: gate the method behind an `Assert.isTrue(ee.getSize() < threshold, "use streaming")` until a streaming `SingleCellMatrixView` lands.

#### C2: MEX writer — no streaming path
- `ExpressionDataFileServiceImpl.writeMexSingleCellExpressionDataInternal:594-607`: always calls `helperService.getSingleCellVectors(ee, samples, qt, cs2gene)` (non-streaming), then constructs a `SingleCellExpressionDataDoubleMatrix` from the full collection.
- Other writers in the same file (`writeTabularSingleCellExpressionDataInternal:541`, `writeCellBrowserSingleCellExpressionData:563`) branch on `fetchSize > 0` and use the streaming overload.
- The MEX writer caller-level `writeMexSingleCellExpressionData(ee, qt, scaleType, useEnsemblIds, OutputStream)` (line 585) and its sister with `fetchSize` (line 610) BOTH end up at the non-streaming internal — the `fetchSize` overload at line 610 routes to a different `writeMexSingleCellExpressionDataInternal` (line 619-, not shown) that does have streaming. Caller hygiene matters.
- **Verdict: RED for the no-fetchSize MEX path**. **Suggested fix**: delete the non-streaming overload, or have it delegate to the streaming version with a sensible default `fetchSize` (30, matching the aggregate service).

#### C3: `loadWithSingleCellVectors(id)` — the eager-collection hot wire
- `SingleCellExpressionExperimentServiceImpl:69-78`:
  ```java
  ExpressionExperiment ee = expressionExperimentDao.load( id );
  expressionExperimentDao.thawLite( ee );
  Hibernate.initialize( ee.getSingleCellExpressionDataVectors() );
  ```
- Loads ALL SC vectors for ALL QTs of the EE — possibly several preferred + raw count + log-norm sets — into a single in-memory Set. For EE 56855 that's 2.6 GB minimum, more if multiple QTs share the EE.
- Sole caller (`SingleCellDataLoaderServiceImpl:237`) is the data-loader, used after a freshly-loaded SC dataset is persisted. The loader has the EE in memory anyway from the upstream pipeline — it doesn't need to round-trip through the DB. The `Hibernate.initialize` here is gratuitous for the loader-flush case.
- **Verdict: YELLOW** (only one caller and it's offline data-loading, not a user-facing path). **Suggested fix**: replace with a more targeted method (`loadAndInitializeSingleCellDimensions(id)`) that only initializes dimensions / CTAs, not the full vector collection.

### D. Sample-level slice

#### D1: `getSingleCellDataVectors(ee, samples, qt)` — load all SC vectors for a list of samples
- Service impl `SingleCellExpressionExperimentServiceImpl:97-125`: pulls ALL SCEDV rows for `(ee, qt)` (the FULL DAO method), then computes `sampleStarts/sampleEnds` from the dimension's `bioAssaysOffset` and slices each vector's `data` / `dataIndices` byte arrays in JVM via `createSlicer(...)`.
- This is the right algorithm because SC vectors are gene-keyed not sample-keyed — there's no way to filter at the SQL layer. BUT it still pulls 2.6 GB across the wire for a one-sample slice.
- **Verdict: YELLOW**. The "load it all, slice in JVM" is structurally unavoidable given the schema. **Suggested architectural fix** (long-term): per-sample vector chunking — if blobs were stored sliced per-sample, this would become trivially streamable. Not a probe fix.

## Headline findings

1. **Dimension/CTA lookups join through a 23 M-row table to retrieve a single row.** B1 + B2. The fix is a small EE↔SCD link table. Single highest-impact change in this report.
2. **No composite `(EE_FK, QT_FK)` index on SCEDV.** A1. Same gap as on the raw-vector table (Round 3 A2). Cheap fix, large win for the majority of SC reads.
3. **`getSingleCellExpressionDataMatrix(ee, qt)` is a 2.6 GB OOM waiting to happen.** C1. No streaming alternative. Should be gated until a streaming wrapper lands.
4. **MEX writer has no streaming branch on the no-`fetchSize` overload.** C2. Loads full matrix into JVM unconditionally.
5. **Aggregate service `collect(toList())`s after streaming.** AggregateServiceImpl:90-92. The `Stream<>` advantage is wasted — the materialization is purely lexical, but the actual heap pressure is identical to the non-streaming branch.
6. **Stream-path prefetch pulls the full CELL_IDS blob.** B3. Easy fix — switch the in-stream prefetch to the `*WithoutCellIds` variant.

## Index gap recommendations

| Table | Column(s) | Reason | Cost estimate |
|---|---|---|---|
| `SINGLE_CELL_EXPRESSION_DATA_VECTOR` | `(EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK)` | Replaces index_merge intersect / FK-only ref-scan for the most common SC filter pair. Used by every method in B1/B2 (the group-by scans) and A1. | Small (~150 MB added to existing 1.3 GB index footprint) |
| `SINGLE_CELL_EXPRESSION_DATA_VECTOR` | `(EXPRESSION_EXPERIMENT_FK, DESIGN_ELEMENT_FK)` | Per-gene SC view in curation-UI. Only worth adding if confirmed user-facing path. | Small |
| **New link table** `SINGLE_CELL_DIMENSION_EXPERIMENT` | `(EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK, SINGLE_CELL_DIMENSION_FK)` primary key + reverse index | Eliminate the 30+ "scan SCEDV, group by dim" queries. Single highest-impact schema change. Populate from `SELECT DISTINCT …` on existing data; maintain via the SC write path. | Tiny — at most ~few thousand rows |

## Non-streaming-caller foot-guns

| File:line | Method | Severity |
|---|---|---|
| `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/SingleCellExpressionExperimentServiceImpl.java:303` | `getSingleCellExpressionDataMatrix(ee, samples, qt)` | RED — 2.6 GB matrix construction, no streaming path. |
| `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/SingleCellExpressionExperimentServiceImpl.java:324` | `getSingleCellExpressionDataMatrix(ee, qt)` | RED — same. |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/service/ExpressionDataFileServiceImpl.java:596` | `writeMexSingleCellExpressionDataInternal(ee, samples, qt, …)` (no-`fetchSize` overload) | RED — MEX always non-streaming. |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/service/ExpressionDataFileHelperService.java:136-137` | `getSingleCellVectors(ee, samples, qt, cs2gene)` | YELLOW — only called from the non-streaming branch of the tabular writer; tabular has a streaming alternative. |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/singleCell/aggregate/SingleCellExpressionExperimentAggregateServiceImpl.java:90-94` | `aggregateVectors(...)` — streams then collects | YELLOW — pseudo-streaming; same heap as non-streaming. |
| `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/SingleCellExpressionExperimentServiceImpl.java:71` | `loadWithSingleCellVectors(id)` — `Hibernate.initialize(ee.getSingleCellExpressionDataVectors())` | YELLOW — only one caller, offline loader. |
| `gemma-cli/src/main/java/ubic/gemma/apps/SingleCellDataWriterCli.java:296,298` | CLI calls `getSingleCellDataVectors(...)` | YELLOW — CLI not user-facing, but the CLI has a streaming-capable companion writer it could switch to. |

## Verdict summary

| Probe | Verdict | Fix priority |
|---|---|---|
| A1 vector load composite index | YELLOW | P2 — add the composite index |
| A2 single-vector probe path index | YELLOW | P3 — only if user-facing |
| A3 stream prefetch chain | GREEN with caveat | P2 — switch stream prefetch to `*WithoutCellIds` |
| B1 dimension lookup via SCEDV scan | **RED** | **P1 — introduce EE↔SCD link table** |
| B2 CTA lookup via SCEDV scan | **RED** | **P1 — falls out of the link-table fix** |
| B3 eager CELL_IDS blob | YELLOW | P2 — switch callers to `*WithoutCellIds` where the blob isn't read |
| C1 `getSingleCellExpressionDataMatrix` | **RED** | **P1 — gate behind size assertion until streaming wrapper lands** |
| C2 MEX writer no-streaming overload | **RED** | **P1 — delete it or delegate to streaming** |
| C3 `loadWithSingleCellVectors` | YELLOW | P3 — narrow to dim-only init |
| D1 sample-slice loads full EE | YELLOW (structural) | P3 — long-term per-sample chunking |

## Surprises

- The DAO has a comprehensive `*WithoutCellIds` family that does the right thing (small-property projection only) — but **the streaming hot path doesn't use it**. The most performance-sensitive call (every SC stream open in the aggregate service / file writers) pays the CELL_IDS-blob tax.
- The `group by` HQL pattern shows up **30+ times** across the DAO — clearly an established convention. The fix is structural (link table) not local, but the breadth of impact means the link table buys a lot.
- The aggregate service uses `streamSingleCellDataVectors(...).collect(toList())`. The cursor / fetch size never gets a chance to constrain heap — the materialization happens immediately. Either the streaming branch is dead code, or it should expose the stream further down the aggregation pipeline.
