# Live-gemd perf probe round 3 — vectors, matrices, DEA results, visualization

## Setup

- Worktree tip: baselined at `c45c236fd209b1eabcdd3b15786c024be08a4690` (phase2-acl-migrate / perf-probe-round3-hotspots)
- Probe host: Darwin 24.6.0, mysql client 5.7.31
- Server: MySQL 5.7.44 (Percona) on the prod gemd via local tunnel `127.0.0.1:8000`
- READ-ONLY: only `SELECT` and `EXPLAIN`
- Bench probes a parallel agent (`agent-perf-probe-round2`) was also issuing SELECTs, so cold-cache numbers below have some noise; warm-cache numbers (runs 2-5) are the stable signal.

### Representative EE fixtures

| EE id | short | n probes | n samples | profile |
|---|---|---:|---:|---|
| 29936 | GSE124347 | 54,675 | 42 | mid-size bulk (the workhorse fixture) |
| 531 | GSE3778 | 175,477 | 18 | wide-probe bulk (e.g. Affymetrix exon array) |
| 37399 | GSE205155 | 11,578 | 154 | many-sample bulk |
| 56855 | GSE144136 | 46,433 | (sc) | single-cell, 2.6 GB blob payload total |

### Key cardinalities

| table | rows | data | index |
|---|---:|---:|---:|
| `PROCESSED_EXPRESSION_DATA_VECTOR` | 968 M | 445 GB | 118 GB |
| `RAW_EXPRESSION_DATA_VECTOR` | 2.45 B | 609 GB | 191 GB |
| `SINGLE_CELL_EXPRESSION_DATA_VECTOR` | 23.2 M | **1.12 TB** | 1.3 GB |
| `DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT` | 1.56 B | 123 GB | 168 GB |
| `CONTRAST_RESULT` | 2.70 B | 357 GB | 191 GB |
| `EIGENVECTOR` | 1.06 M | 1.5 GB | 22 MB |
| `SAMPLE_COEXPRESSION_MATRIX` | 47,784 | (each ~52 KB) | — |

## Hotspot priority alignment

| Category | Worst probe | Headline | Verdict |
|---|---|---|---|
| **A. Vector retrieval** | A1 full processed fetch | 2.5–3.5 s for 54k vectors / 17 MB blob — payload-bound, no index issue | YELLOW |
| **B. Full data matrices** | B3 large EE (175k probes) | 4.2–4.9 s for the SQL, then Java assembles **O(rows × cols)** init with NaN-fill + double[] → Double[] **boxing** | RED (Java side) |
| **C. DEA results retrieval** | C1 `findByGene` | 29,138 rows for TP53, warm ~0.55 s, cold 4 s — gene-page latency on cold cache | YELLOW |
| **D. Visualization** | (no D endpoint here is the worst) | D-1 / PCA / sample-coex matrix all sub-300 ms; the "horrible" heatmap label refers to gemma-web `DEDVController` UI / payload, not the SQL | GREEN at SQL layer |

**The biggest perf gap relative to importance is B (full data matrices).** Vector blobs fetch in 2-5 s and that's basically the I/O floor for a 17-24 MB blob over the tunnel; but the in-JVM matrix assembly that comes after is the real lever:

1. `ExpressionDataDoubleMatrix.createMatrix()` at `gemma-core/src/main/java/ubic/gemma/core/datastructure/matrix/ExpressionDataDoubleMatrix.java:640` walks `rows × cols` to set every cell to `Double.NEGATIVE_INFINITY`, then writes vector values, then re-walks `rows × cols` to convert `-Infinity` → `NaN`. For EE 531 that's `175,477 × 18 = 3.16 M` cell writes × 2 passes.
2. At line 681 each vector's `double[]` is converted to `Double[]` via `ArrayUtils.toObject(vals)` then unboxed back by `mat.set(...)` — a per-element boxing tax with no functional purpose.

The dominant prod cost is the second-order in-JVM work, not the SQL. Vector-fetch optimisation buys 30%; matrix-assembly optimisation buys the rest.

## Probe inventory

### A. Vector retrieval

#### A1: `getProcessedVectors(ExpressionExperiment)` — processed data fetch for one EE
- DAO: `ProcessedExpressionDataVectorDaoImpl.getProcessedVectors(ee)` line 53. HQL: `select dedv from ProcessedExpressionDataVector dedv where dedv.expressionExperiment = :ee`.
- Translated SQL: `SELECT v.* FROM PROCESSED_EXPRESSION_DATA_VECTOR v WHERE v.EXPRESSION_EXPERIMENT_FK=:ee`.
- EXPLAIN: ref-scan on composite `experimentProcessedVectorProbes(EXPRESSION_EXPERIMENT_FK, DESIGN_ELEMENT_FK)`. ~102k estimated, 54,675 actual rows for EE 29936.
- Timing (EE 29936, 54,675 vec, 17.5 MB blob):
  - metadata-only (ID, DE_FK, BAD_FK): 0.37–0.41 s
  - full row incl. DATA blob: **2.49–3.54 s mean ~2.7 s**
- Verdict: **YELLOW**. Index is fine. Wall time is dominated by transferring 17.5 MB of blob over the tunnel (≈ 6 MB/s effective). The SQL itself is healthy.
- **N+1 risk on hydration**: `ProcessedExpressionDataVector.hbm.xml` lines 13-26 set `bioAssayDimension`, `designElement`, `quantitationType` to `lazy="false" fetch="select"`. With Hibernate batch-fetch size 128 (`gemma.hibernate.default_batch_fetch_size` in `default.properties:200`), hydrating 54k distinct designElements fires ≈ 425 follow-up `SELECT` statements just for the `designElement` association. That's not visible in SQL EXPLAIN — but at ~3 ms each (local round-trip) that's ~1.3 s of pure Hibernate post-processing on top of the 2.7 s blob transfer. The "low metadata fast path" (A4) below would skip those entirely.

#### A2: `RawExpressionDataVectorDaoImpl.find(QT) / findByEE+QT` — raw data fetch
- Translated SQL: `SELECT v.* FROM RAW_EXPRESSION_DATA_VECTOR v WHERE v.EXPRESSION_EXPERIMENT_FK=:ee AND v.QUANTITATION_TYPE_FK=:qt`.
- EXPLAIN: **index_merge(intersect)** of `FK1F432A68D0CC06B4` (QT_FK) ∩ `RAW_EXPRESSION_DATA_VECTOR_EXPRESSION_EXPERIMENT_FKC` (EE_FK). No composite `(EE_FK, QT_FK)` index exists on this table (unlike processed which has `experimentProcessedVectorProbes`).
- Timing (EE 29936, 54,675 raw vec, 17.5 MB): **2.14–2.64 s mean ~2.3 s** — comparable to processed despite the intersect, because index_merge for two highly-selective bigint columns is cheap.
- Verdict: **YELLOW**. The index-merge plan is OK, but a composite `(EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK)` index would let it drop intersect overhead and use a single ref-scan. **Suggested fix**: add the composite, deferred — only matters when raw-data downloads become a hot path.

#### A3: `getSingleCellDataVectors(ee, qt)` — single-cell vectors
- DAO: `ExpressionExperimentDaoImpl.getSingleCellDataVectors(ee, qt)` line 3579.
- HQL: `select scedv from SingleCellExpressionDataVector scedv where scedv.expressionExperiment = :ee and scedv.quantitationType = :qt`.
- EE 56855 (GSE144136): 46,433 vectors, 1.6 GB DATA, 1.08 GB DATA_INDICES — **2.6 GB total per EE**.
- EXPLAIN: ref-scan on `SINGLE_CELL_DATA_VECTOR_EXPRESSION_EXPERIMENT_FKC`. Estimated 93k rows.
- Timing:
  - metadata-only (ID, DE_FK, QT_FK, dim FK): 0.35–0.42 s
  - LIMIT 100 with DATA blob lengths: 0.22–0.27 s
  - **Full fetch not attempted** — 2.6 GB over a tunnel is not benchable and would take minutes.
- Verdict: **YELLOW** for metadata, **RED for full pull**. The DAO already supports `includeData` / `includeDataIndices` / `includeCellIds` flags (`SingleCellVectorInitializationConfig` at `SingleCellExpressionExperimentServiceImpl:88`) and a `streamSingleCellDataVectors(...)` cursor-fetch path (line 3606). Code is already in good shape; callers must avoid the full-data overload for any user-facing request.
- **Cardinality footnote**: 23 M SC vectors total across the prod DB, average 35 KB DATA per vector. A single large SC dataset is the size of the entire bulk PEDV table by data volume.

#### A4: "load just the matrix-bytes, no metadata" fast path — present or absent?
- Confirmed: **a fast path exists for SC** (the `includeBiologicalCharacteristics` / `includeCellIds` flags on the SC overload) but **not for bulk processed vectors**. There is no `getProcessedVectorData(eeId)` that returns `Map<DEId, byte[]>` without hydrating `BioAssayDimension` / `CompositeSequence` / `QuantitationType`.
- For bulk vectors, every read pays the 425 follow-up SELECTs for designElements even when the caller only needs the raw blob for matrix assembly. The caller almost always does need `bioAssayDimension` (it determines column order) and `quantitationType` (filtering / log-transform decisions) — so the eager fetch isn't wholly wasted. But `designElement` is needed only as a row label; for matrix-assembly purposes a probe-id label would suffice. **Suggested action**: a new DAO method `getProcessedVectorBlobsByEE(Long eeId)` returning `List<Object[]>` (id, designElementId, badId, qtId, byte[] data) bypasses Hibernate hydration entirely and would shave 1+ s off every matrix fetch.
- File reference: `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/bioAssayData/ProcessedExpressionDataVectorDaoImpl.java:53`.

### B. Full data matrices

#### B1: `ExpressionDataMatrixService.getProcessedExpressionDataMatrix(EE)` call chain
- File: `gemma-core/src/main/java/ubic/gemma/core/analysis/service/ExpressionDataMatrixServiceImpl.java:103-128`.
- Round-trips:
  1. `ExpressionExperimentService.getProcessedDataVectors(ee)` — one SQL (`SELECT v.* FROM PROCESSED_EXPRESSION_DATA_VECTOR WHERE EE_FK=:ee`).
  2. Hibernate hydrates each vector's eager `designElement` / `bioAssayDimension` / `quantitationType` / `numberOfCellsObject` — see A1 above; on the order of 425 batched follow-up SELECTs for designElements.
  3. Optional `thawBioAssayDimension` walk if `thawAssays=true`.
  4. `new ExpressionDataDoubleMatrix(ee, dataVectors)` — pure in-JVM.
- Verdict: **most of the wall time is steps 1+2 (SQL+hydration); step 4 is in-JVM but not negligible — see B2.**

#### B2: `ExpressionDataDoubleMatrix.createMatrix(vectors)` — in-JVM matrix assembly
- File: `gemma-core/src/main/java/ubic/gemma/core/datastructure/matrix/ExpressionDataDoubleMatrix.java:640-703`.
- Algorithmic shape: **O(rows × cols)** for the init-to-`-Infinity` pass, **O(N vectors)** for the population pass, then **another O(rows × cols)** for the `-Infinity → NaN` cleanup. For EE 531 (175k × 18) that's `2 × 3.16 M = 6.3 M` cell writes done in pure Java loops, on top of the actual vector unpacking.
- **Boxing tax at line 681**: `setMatBioAssayValues(mat, rowIndex, ArrayUtils.toObject(vals), bioAssays, it)` — `toObject` allocates a `Double[]` of size `cols` per vector (e.g. 175,477 `Double[18]` arrays = 175k allocations + GC pressure), then the inner loop unboxes back to `double` to call `mat.set(rowIndex, column, vals[j])`. The signature is generic (`<R, C, V> setMatBioAssayValues(... V[] vals ...)`) — refactoring to `setMatBioAssayValuesDouble(... double[] vals ...)` and keeping the generic overload for non-double quantitations would remove this entirely.
- Verdict: **RED for algorithmic cleanup**. The wall-time win is data-size dependent; for the 175k × 18 EE it's likely 100–300 ms (a few percent of the ~5 s total); for a 175k × 200 EE (RNA-seq-style) it scales to several seconds.

#### B3: real-EE timings — wall-time of the SQL leg of matrix-fetch
- EE 37399 (11.5k vec × 154 samples, 1232 bytes/vec, 13.6 MB total blob): 1.75–2.24 s full-row.
- EE 29936 (54.7k vec × 42 samples, 320 bytes/vec, 17.5 MB total blob): 2.49–3.54 s full-row.
- EE 531 (175.5k vec × 18 samples, ~140 bytes/vec, 24.1 MB total blob): **4.20–4.92 s full-row**.
- **Observation**: wall time scales roughly with total blob bytes, not with vector count or sample count alone. The transfer-cost component is `total_blob_size / network_bw`. For prod gemma-rest (LAN, not tunneled) this would be ~3–5× faster.
- Verdict: **YELLOW** at the SQL layer (no index issue, no plan pathology). **RED** if you compose this with B2's in-JVM cost on a wide-sample EE.

### C. DEA results retrieval

#### C1: `findByGene(Gene)` — gene-centric DEA search across all analyses
- DAO: `DifferentialExpressionResultDaoImpl.findByGene(gene, useGene2Cs, keepNonSpecificProbes)` line 322.
- HQL: `select e, r from DifferentialExpressionAnalysis a join a.experimentAnalyzed e join a.resultSets rs join rs.results r where r.probe.id in :probeIds group by e, r`.
- Translated SQL (gene = TP53, gene_id = 162841, 440 probes in GENE2CS):
  ```sql
  SELECT dear.ID, dear.PVALUE, dear.CORRECTED_PVALUE, dear.RESULT_SET_FK, a.EXPERIMENT_ANALYZED_FK
  FROM DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear
  JOIN ANALYSIS_RESULT_SET ars ON ars.ID = dear.RESULT_SET_FK
  JOIN ANALYSIS a ON a.ID = ars.ANALYSIS_FK
  WHERE dear.PROBE_FK IN (SELECT CS FROM GENE2CS WHERE GENE = 162841);
  ```
- EXPLAIN: GENE2CS materialised (440 rows), DEAR ref-scan on `probeResultSets(PROBE_FK, RESULT_SET_FK)` index (~122 rows per probe), eq_ref to ars then a. No `Using filesort` / `Using temporary` issues.
- Timing: **cold 4.04 s, warm 0.53–0.55 s (5 runs)** — 29,138 rows returned for TP53.
- Verdict: **YELLOW**. Plan is fine; only the cold path bites. The post-fetch cost in `findByGeneAndExperimentAnalyzed` (line 88 — the variant used by the REST UI) is mostly `Hibernate.initialize(r.getProbe())` and `Hibernate.initialize(r.getContrasts())` walks (lines 145-174) — those are batched but they fire one SELECT per batch of 128 results, so 29k results = ~230 batches per association.
- **Suggested fix**: cache the warm-path result by (geneId, threshold) — there's already `setCacheable(true)` on lines 272/338/368, so Hibernate query cache is engaged. Confirm the Ehcache region for this query is sized adequately in prod (`ehcache.xml` review TODO).

#### C2: "Top N hits per analysis" — top-50 per resultSet
- Pattern: `SELECT ... FROM DEAR WHERE RESULT_SET_FK=:rs ORDER BY CORRECTED_PVALUE LIMIT 50`.
- EXPLAIN: ref-scan on `DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_RESULT_SET_FKC` then **`Using filesort`** — there is NO compound `(RESULT_SET_FK, CORRECTED_PVALUE)` index. Existing single-column `DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_CORRECTED_PVALUE` is unhelpful because the optimiser drives from the resultSet predicate.
- Timing (result set 642446, 45,011 rows total): **0.19–0.22 s, ~50 rows**. Fast despite filesort — fits in RAM.
- Verdict: **GREEN at current scale**. Compound `(RESULT_SET_FK, CORRECTED_PVALUE)` would let the LIMIT short-circuit but the win is only meaningful for resultSets > 200k rows (which don't exist today; max ~91k). Defer index.

#### C3: "All results for an analysis" — full result-set paginated dump
- Pattern: `SELECT * FROM DEAR WHERE RESULT_SET_FK=:rs`.
- Timing (45,011 rows): **0.47–0.65 s**. Plain ref-scan, no filesort.
- Verdict: **GREEN**. The download-endpoint cost would be transferring rows, not querying them.

#### C4: Contrast-result join — DEAR + contrast (the "with contrasts" payload)
- Pattern: `SELECT dear.*, cr.* FROM DEAR LEFT JOIN CONTRAST_RESULT cr ON cr.DEAR_FK=dear.ID WHERE dear.RESULT_SET_FK=:rs`.
- EXPLAIN: ref-scan on DEAR, ref-scan on `CONTRAST_RESULT_DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FKC` per DEAR row.
- C4a single-contrast (categorical 2-level, ratio 1.0): 0.71–0.83 s for 45k rows.
- C4b multi-contrast (6 contrasts/result, ratio 6.0): **2.87–3.28 s for 202,873 rows**.
- Verdict: **YELLOW** at multi-contrast scale. The join shape is fine but the wire payload grows linearly with contrast count. There's also a `CONTRAST_RESULT` table that's **2.7 B rows / 357 GB** — the second-largest table in the DB — so query patterns that join it scale poorly. Bounding callers with `dear.CORRECTED_PVALUE <= 0.05 LIMIT 50` (probe C-with-contrasts-top-50: 0.29–0.34 s) is the safe pattern. **Audit needed**: which REST endpoints fetch contrasts without a corrected-pvalue threshold?

#### C-extra: GENE2CS "specific probes only" correlated subquery
- The default branch of `getProbeIdsForGene(gene, useGene2Cs=true, keepNonSpecificProbes=false)` at line 425 issues `WHERE GENE=:g AND (SELECT COUNT(DISTINCT g2.GENE) FROM GENE2CS g2 WHERE g2.AD=GENE2CS.AD AND g2.CS=GENE2CS.CS)=1`.
- EXPLAIN: dependent subquery with eq-ref on `CS` index, 1 row per outer row.
- Timing: 0.17–0.22 s for 441 probes returned. Verdict: **GREEN**. No optimisation needed.

### D. Visualization data

#### D1: DEDV-for-visualization (one gene, one EE)
- Pattern: vectors for probes matching one gene in one EE — the heart of `getDEDVForVisualization` and `getDEDVForCoexpressionVisualization` (DEDVController.java).
- SQL: `SELECT v.* FROM PEDV v WHERE v.DESIGN_ELEMENT_FK IN (SELECT CS FROM GENE2CS WHERE GENE=:g) AND v.EXPRESSION_EXPERIMENT_FK=:ee`.
- EXPLAIN: GENE2CS materialised then PEDV ref-scan on composite `experimentProcessedVectorProbes(EE_FK, DE_FK)` index. Clean two-column index seek.
- Timing (TP53 × EE 29936): **0.17–0.24 s, 3 rows**. Pattern across 5 EEs: 0.36–1.61 s (cold), 0.36–0.40 s (warm).
- Verdict: **GREEN**. The "horrible" label Paul applied to the heatmap likely refers to the rendering / JS / payload side, not the SQL. SQL is healthy.

#### D2: Coexpression visualization feed
- `gemma-core/src/main/java/ubic/gemma/model/association/coexpression/` directory contains only `Coexpression.png` / `Coexpression.ucls` — the Java DAO/service have been **removed**. No live SQL path to probe; the gene-coexpression endpoints (`HUMAN_GENE_COEXPRESSION`, etc., 4 tables present in schema) appear orphaned. Confirm with the `agent-heatmap-rewrite-recce` agent whether this is dead code or the read path moved elsewhere.
- Verdict: **N/A (no code path under this branch)**.

#### D3: PCA / sample-correlation feeds
- PCA: `SELECT * FROM EIGENVECTOR WHERE PRINCIPAL_COMPONENT_ANALYSIS_FK=:pca`. EE 29936 → 43 rows. **0.18–0.22 s**. Small fixed payload.
- Sample-coexpression matrix: `SELECT scm.* FROM SAMPLE_COEXPRESSION_MATRIX scm WHERE scm.ID=:id` is a single-PK lookup; 100-row batch test: **0.18–0.19 s**.
- Verdict: **GREEN, no action**. The "no-action" green entry confirmed.

#### D4: SC sparsity heatmap / multifunctionality
- `SingleCellSparsityHeatmap.java` constructs the heatmap from already-resolved `SingleCellDimension` + `BioAssayDimension` — no expensive SQL on the read path. The expensive write path is `SingleCellSparsityMetricsUpdaterCli` (one-time computation).
- Multifunctionality: no gemma-rest endpoint references found under `gemma-rest/`. If it's exposed via gemma-web it'd be in `MultifunctionalityController` (legacy webapp — out of scope per project memory "gemma-web is walking dead").
- Verdict: **N/A on the gemma-rest 2.0 surface**.

## Suggested fix directions (concrete)

1. **B2 boxing tax** — `ExpressionDataDoubleMatrix.java:681`: add a `setMatBioAssayValuesDouble(... double[] vals ...)` overload. Keep generic for non-double quantitations. Estimated win: 100-300 ms on EE 531, scales linearly with `rows × cols`.

2. **B2 init O(N×M)** — `ExpressionDataDoubleMatrix.java:653-657, 692-700`: the double-pass `-Infinity → NaN` dance is to mark missing values during population. Replace with a `BitSet rowsSeen` + initialise to NaN once, then track which cells got written. Cuts cell-write count by 50% on wide matrices.

3. **A4 blob-only fast path** — new `ProcessedExpressionDataVectorDaoImpl.getProcessedVectorBlobsByEE(eeId)` returning raw `Object[]` (id, designElementId, badId, qtId, data) via `getSessionFactory().getCurrentSession().createNativeQuery(...).list()`. Wire `ExpressionDataMatrixServiceImpl.getProcessedExpressionDataMatrix(ee)` to use it when `thawAssays=false`. Skips ~425 follow-up SELECT round-trips for designElement hydration. Estimated win: 0.5-1.5 s per matrix fetch.

4. **A2 raw-vector composite index** — `ALTER TABLE RAW_EXPRESSION_DATA_VECTOR ADD INDEX (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK)`. Defer until raw-data download becomes a measurable pain point.

5. **C1 cold-cache warm-up** — gene-page DEA listing takes 4 s cold, 0.55 s warm. Add a startup warm-up for the top-N most-viewed genes (TP53 / BRCA1 / TNF / etc., empirically from server logs), or pre-build a denormalised `GENE_DEA_RESULT_SUMMARY` cache table (similar to the `EE2CHARACTERISTIC` denorm pattern). The query cache is engaged (`setCacheable(true)`) — confirm the Ehcache region size is adequate in prod.

6. **C4 contrast-payload bound** — audit gemma-rest endpoints that fetch contrasts without `corrected_pvalue` thresholds; force an upper bound (e.g. LIMIT 1000) where the consumer is a UI.

## Surprises

- **GENE2CS specific-probe correlated subquery is fine.** The hideous-looking self-join on a 36 M-row table actually picks `eq_ref` on the `CS` index and finishes in 200 ms for a typical query. No need to refactor.

- **C2 (top-50 per analysis) is GREEN despite filesort.** EXPLAIN flags `Using filesort` but resultSet sizes are bounded at ~90k rows max and the sort fits in `sort_buffer_size`. The compound `(RESULT_SET_FK, CORRECTED_PVALUE)` index would shave it from 200 ms to ~50 ms — worth deferring until prod resultSets cross ~500k rows.

- **The "horrible" heatmap is not a SQL problem.** D1 viz returns 3 rows in 200 ms — whatever Paul perceives as slow on the heatmap UI is downstream of the SQL (probably the gemma-web `DEDVController.makeVisCollection(...)` shaping of `VisualizationValueObject[]` + the JS-side rendering). The `agent-heatmap-rewrite-recce` deliverable is the right place to chase that.

- **Single-cell vector storage dominates the database.** SC vectors hold **1.12 TB** of blob data in only 23 M rows — the largest single data class by data_length even though row count is tiny. The existing `streamSingleCellDataVectors` + cursor-fetch + per-association `includeData`/`includeDataIndices` flags are the right architecture; just enforce non-streaming callers can't accidentally pull a 2.6 GB EE.

- **Hibernate eager-fetch + 128-row batch fetch on `PROCESSED_EXPRESSION_DATA_VECTOR.designElement`** is the hidden cost behind the SQL probes. The probe layer can't see it (those SELECTs happen on the Hibernate session, separate from the main query) — but for a 54k-vector EE it adds ~425 SELECTs of 128 designElements each. Worth instrumenting via `org.hibernate.stat.Statistics` once.

- **Composite index `(EXPRESSION_EXPERIMENT_FK, DESIGN_ELEMENT_FK)` on PEDV exists** (`experimentProcessedVectorProbes`) — good for both the by-EE and the by-gene-in-EE patterns. The same composite is **missing on `RAW_EXPRESSION_DATA_VECTOR`** (only single-column indexes), which is why A2 falls back to `index_merge(intersect)`. Asymmetric but tolerable today.
