# Perf probe — TSV/data-export + DEA-run paths (live gemd)

## Setup

- Worktree tip: `ca05591e530a5c09be065a60ed64135bc784604d` (branch `perf-probe-tsv-data-export`).
- Probe host: Darwin 24.6.0, mysql client 5.7.31.
- Live target: prod gemd via `127.0.0.1:8000`, user `gemmaadmin`, READ-ONLY. Tunnel was UP for SELECTs but a COUNT(*) across the 953M-row `PROCESSED_EXPRESSION_DATA_VECTOR` table was too slow to complete in the budget, so DEA-side cardinalities and per-EE size profiles are extrapolated from Round 3.
- Round 3 confirmed: `RAW_EXPRESSION_DATA_VECTOR` ≈ 84 M rows; processed had ~953 M rows (verified this run); per-EE typical processed-vector counts: bulk ≈ 20–60 k probes × 50–500 samples = 50–500 k vectors per EE; per-vector blob ~ kilobytes for bulk.

## Probe inventory at a glance

| Probe | Question | Verdict |
|---|---|---|
| A1 | `/datasets/{id}/data/processed` streaming? | Disk-cache + zero-copy sendfile (good) **only after cold build**; the cold build materializes the entire `ExpressionDataDoubleMatrix` into heap |
| A2 | `/datasets/{id}/data/raw` streaming? | Same cache+sendfile shape; same cold-build memory ceiling, scoped per-QT |
| A3 | `/data/dea` endpoint? | No such path. DEA-as-TSV ships through `GET /resultSets/{id}` (per-result-set TSV) and the bulk DiffEx archive ZIP (`writeOrLocateDiffExAnalysisArchiveFile`). |
| A4 | GZIP sanity | Correctly wired: disk caches are gzip on disk → `alreadyCompressed=true` skips re-encoding; sendfile passes the gzipped file directly with `Content-Encoding: gzip` header |
| A5 | TTFB | Hot cache: TTFB = single `getOutputFile` lock acquire + Tomcat sendfile setup → low ms range. Cold: TTFB == time to build the matrix + write the gzipped file (seconds to minutes), since the response only flips to streaming if the cache build fails with `IOException` |
| B1 | DEA run shape | matrix-assembly → in-JVM `LinearModelAnalyzer` on a single `taskExecutor` worker → batched JDBC `addBatch` insert of results & contrasts |
| B2 | Memory ceiling | Two concurrent full-EE matrices on heap during the analysis (raw `dmatrix` + filtered `bareFilteredDataMatrix`), plus the design matrix and full `LeastSquaresFit` |
| B3 | Persist | One bulk `addBatch` for results, one for contrasts (good); but the `super.create()` parent persists analysis+resultSets+pvalue-distribution via cascade with Hibernate batch_size=32 |
| B4 | Wall time | Dominated by matrix-assembly + `runAnalysisInBackground` Future.get loop (1-min progress ticks). LinearModelAnalyzer self-reports a `Model fit … %d ms` log line so this is measurable |
| B5 | Concurrency | Curators trigger via async `TaskRunningService`. Linear-model fit pulled off the calling thread onto `taskExecutor` (`gemma.localTasks.corePoolSize=16`). DEA file-archive writes go through `expressionDataFileTaskExecutor` (corePool=4, queue=10) |

## Part A — TSV / data-export paths

### A1, A2 — `/datasets/{id}/data/processed` and `/data/raw`

**Handler:** `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java:2897-2955` (processed), `:2963-3016` (raw).

**Shape (identical for both):**

```java
@GZIP(mediaTypes=TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed=true)
try (LockedPath p = expressionDataFileService.writeOrLocateProcessedDataFile(ee, filtered, force, 5, SECONDS)
        .orElseThrow(...)) {
    return sendfile(p.getPath())  // -> Tomcat native sendfile
        .type(...)
        .header("Content-Disposition", ...)
        .build();
} catch (TimeoutException) { 503 ServiceUnavailable }
catch (IOException) { // fallback: stream gzip on the fly
    return Response.ok((StreamingOutput) output ->
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(output), UTF_8)) {
            expressionDataFileService.writeProcessedExpressionData(ee, filtered, ..., w, true);
        })...build();
}
```

**Cache build path (`ExpressionDataFileServiceImpl.writeOrLocateProcessedDataFile`, line 852):**

```java
try (LockedPath f = getOutputFile(result, false, timeout, timeUnit)) {
    Date check = expressionExperimentService.getLastArrayDesignUpdate(ee);
    if (checkFileOkToReturn(forceWrite, f.getPath(), check)) return Optional.of(f.steal());
    try (LockedPath ignored = f.toExclusive();
         Writer writer = openCompressedFile(ignored.getPath())) {
        int written = writeProcessedExpressionData(ee, filtered, null, false, false, false, writer, false);
        return Optional.of(ignored.toShared());
    }
}
```

`openCompressedFile` = `new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(file)), UTF_8)` (line 1143). So the cached file is **gzipped TSV on disk**.

Inside `writeProcessedExpressionData` (line 797 → `writeProcessedExpressionDataInternal` line 807):

```java
Map<CompositeSequence, String[]> geneAnnotations = new HashMap<>();
ExpressionDataDoubleMatrix matrix = helperService.getDataMatrix(ee, samples, filtered, geneAnnotations);
//                                  ^^^ FULL MATRIX MATERIALIZED IN HEAP
MatrixWriter matrixWriter = new MatrixWriter(entityUrlBuilder, buildInfo);
...
return matrixWriter.writeWithStringifiedGeneAnnotations(writer, matrix, ProcessedExpressionDataVector.class, geneAnnotations);
```

And `ExpressionDataFileHelperService.getDataMatrix` (line 79) goes through `expressionDataMatrixService.getProcessedExpressionDataMatrix(ee)` which calls `expressionExperimentService.getProcessedDataVectors(ee)` and constructs `new ExpressionDataDoubleMatrix(ee, dataVectors)` — every probe-row's blob materialized, decoded into doubles, attached to a dense matrix.

**Verdict — cold path:**

1. Caller blocks 5 s waiting for the file lock; if exceeded, 503 + `Retry-After: 30`.
2. If the file is missing/outdated, the same caller drives the regeneration synchronously inside the same HTTP request thread. **TTFB == full matrix build + filtering + gzipped file-write before a single byte goes out.** For a 50 k-probe × 200-sample bulk EE, that's ~1.5 GB of decoded doubles in heap and minutes of wall time (matrix-assembly was ~4–5 s per the Round 3 B1 probe + JVM tax, then a `MatrixWriter.write` over ~10 M cells).
3. The IOException fallback streams gzip on the wire — but ONLY fires when the disk cache write itself throws. So under disk-pressure the user gets streaming, but under "file is missing and we hit a timeout waiting for the lock" the user gets 503, NOT streaming.

**Verdict — hot path:**

1. `checkFileOkToReturn` compares last-modified time against `getLastArrayDesignUpdate(ee)` (a fast DB read). Returns the existing gzipped file via Tomcat native sendfile → near-zero CPU, kernel-level zero-copy, byte-for-byte the gzipped payload already on disk.
2. **TTFB hot = lock acquire + sendfile setup**. Single-digit ms after the lock-manager handshake. Excellent.

**Cache invalidation:** the `check` date is `getLastArrayDesignUpdate(ee)` — meaning a re-annotation of any platform invalidates the cache. That's wider than necessary (re-annotation doesn't change expression values), but cheap when the cache is hot.

**Edge case:** the `force=true` query parameter bypasses both the cache and the freshness check, and is admin-gated (`checkIsAdmin()`). Curators triggering a rebuild via the UI dispatch path go through `force`; that's fine — but every curator-driven force re-write blocks the HTTP request for the full matrix-build duration.

### A3 — `/data/dea`?

**Verdict: does not exist as `/datasets/{id}/data/dea`.**

DEA TSV ships through two endpoints:

- `GET /resultSets/{resultSet}` with `Accept: text/tab-separated-values` (one TSV per result-set). Handler `AnalysisResultSetsWebService.java:171-249`; streaming via `(StreamingOutput) outputStream -> { Writer w = new OutputStreamWriter(outputStream, UTF_8); expressionAnalysisResultSetFileService.writeTsv(ears, baseline, resultId2Genes, w); }` (line 282-294).
- `GET /datasets/{id}/analyses/differential/resultSets` → 302 redirect to the result-sets endpoint, scoped by EE.

The bulk DEA archive (all result-sets for one analysis, as a ZIP with gene annotations) is built by `ExpressionDataFileServiceImpl.writeOrLocateDiffExAnalysisArchiveFile` (line 1032), but I could not find a REST endpoint that serves this ZIP directly — it appears to be a side-effect of `persistAnalysis` (line 265) and is consumed by the legacy gemma-web pages, not by gemma-rest. Confirmed by `grep -n "DiffExAnalysisArchive" gemma-rest/src/main/java/`: zero hits.

**Streaming verdict for the result-set TSV (`/resultSets/{id}`):**

The TSV is streamed to the client via `StreamingOutput` (no on-disk cache). But the underlying DAO call `expressionAnalysisResultSetArgService.getEntityWithContrastsAndResults(analysisResultSet)` runs `loadWithResultsAndContrasts` (DAO line 86), which:

1. Loads every `DifferentialExpressionAnalysisResult` for the set into a `LinkedHashSet<>` (line 91-98).
2. Calls `thawResultsAndContrasts(ears)` (line 99 → DAO line 383). This walks every result, calls `Hibernate.initialize(r.getProbe())` then `Hibernate.initialize(r.getContrasts())` then for each contrast `Hibernate.initialize(cr.getFactorValue().getExperimentalFactor())`.

With `hibernate.default_batch_fetch_size=128` (default.properties:200), a 50 k-result set yields:
- 1 result-loading query
- ~391 batched probe-fetches
- ~391 batched contrast-set fetches (probably more since each result has multiple contrasts)
- 391+ batched FV / EF fetches

**So the TSV is streamed on the wire but materialized in full in heap first.** TTFB ≈ time to load + thaw all results before the first byte. For a 50 k-result set, that's seconds to tens of seconds. Once streaming starts, throughput is fine.

**Plus the gene map:** `expressionAnalysisResultSetService.loadResultIdToGenesMap(ears)` is a second full-set walk to build `Map<Long, Set<Gene>>` — another heap copy before streaming begins.

### A4 — GZIP streaming sanity

**Wiring:**

- `@GZIP(mediaTypes=…, alreadyCompressed=true)` on the cached-file endpoints (`/data/processed`, `/data/raw`, `/data/singleCell`, `/design`).
- `GzipHeaderDecoratorAfterGZipEncoder` (`gemma-rest/src/main/java/ubic/gemma/rest/providers/GzipHeaderDecoratorAfterGZipEncoder.java`) only adds the `Content-Encoding: gzip` header AFTER `GZipEncoder` would normally run, and `GzipHeaderDecorator.isApplicable` SKIPS for `alreadyCompressed=true`. So the body is NOT re-encoded — the on-disk gzipped bytes go straight to the wire.
- The wire path is `sendfile(p.getPath())` → `SendfileProvider` (line 52) → if Tomcat reports sendfile-support, sets `org.apache.tomcat.sendfile.{filename,start,end}` and never copies bytes through user space. Otherwise falls back to `pathProvider.writeTo` (regular stream copy).
- Fallback streaming path: `(StreamingOutput) output -> new GZIPOutputStream(output)` (line 2935-2942 etc.) — uses default GZIPOutputStream buffer (~512 B), so per-chunk gzip frames are emitted as the writer flushes. With `autoFlush=true` (passed to `writeProcessedExpressionData`), the gzip stream flushes per-row → real streaming TTFB, but somewhat lower compression than batch-mode.

**Verdict:** wiring is correct end-to-end. The hot path (gzipped file + sendfile + alreadyCompressed) is the best-possible streaming setup. The cold path (matrix-build + write-to-cache) is the problem, not the GZIP layer.

### A5 — TTFB estimates

| Scenario | Hot cache | Cold cache |
|---|---|---|
| `/data/processed` on 50k×200 EE | ~5–20 ms (lock + sendfile) | ~30–120 s (matrix-assembly + filtering + write) |
| `/data/raw` on 50k×200 EE, per QT | ~5–20 ms | ~10–60 s per QT |
| `/resultSets/{id}` 5 k results | ~200 ms–2 s (load + thaw) | (no on-disk cache; always pays the load+thaw cost) |
| `/resultSets/{id}` 50 k results | ~2–20 s (load + thaw, 391+ batched fetches) | same |

I did not run live curl probes — Tomcat is not running locally, and the prod tunnel is read-only DB, not the REST endpoint.

## Part B — DEA RUN path

### B1 — `runDifferentialExpressionAnalyses(ee, config)` shape

Entry: `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java:2009-2014` (`/datasets/{id}/analyses/differential`) → `doRunDatasetDifferentialAnalysis` (line 2016-2084) → `taskRunningService.submitTaskCommand(cmd)` returns 202 with `Location: /tasks/{taskId}`. **The HTTP request returns immediately** with a task ID; the work is async.

Async task body (`DifferentialExpressionAnalysisTaskImpl.call`, line 59-88) → `doAnalysis()` (line 90-139):

1. `ee = expressionExperimentService.thawLite(ee)` — light hydration only.
2. `findByExperiment(ee, true)` — surface existing analyses.
3. Build `DifferentialExpressionAnalysisConfig` from the task command (factors, subset factor, interactions).
4. `analysisSelectionAndExecutionService.analyze(ee, config)` → builds matrix + runs `LinearModelAnalyzer.run(ee, dmatrix, config)`.
5. If `config.isPersist()` (default true): `helperService.persistStub(analysis)` → `DifferentialExpressionAnalysisDaoImpl.create(entity)`.
6. Side-effect: `writeOrLocateDiffExAnalysisArchiveFile(analysis, true)` writes the bulk ZIP for the analysis (synchronously inside the same async task).

### B2 — Memory ceiling

Stages and concurrent heap residents:

| Stage | Heap residents |
|---|---|
| `getProcessedExpressionDataMatrix(ee, thawAssays=true)` (`AnalysisSelectionAndExecutionService.java:70`) | full `Collection<ProcessedExpressionDataVector>` (one blob per probe) + `ExpressionDataDoubleMatrix` (probes × samples doubles) |
| `LinearModelAnalyzer.run` slice + reorder (line 304-306) | original dmatrix + the column-sliced copy |
| `doAnalysis` filtering (line 695-696) | `ExpressionDataDoubleMatrix expressionData = filter.filter(ensureLog2Scale(expressionData), filterResult)` — new filtered matrix while original lives in scope |
| `makeDataMatrix` + `properDesignMatrix` (line 713-714) | raw `bareFilteredDataMatrix` + `finalDataMatrix` + design matrix |
| `runAnalysisInBackground` (line 719) | `LeastSquaresFit` (regression coefficients + residuals + std errors for every probe), plus the input matrix held by the `Future` closure |

**Estimate for a 20 k-probe × 100-sample EE:**
- Raw matrix: 20 k × 100 × 8 B = 16 MB
- Filtered + log2-scaled copy: another 12 MB
- LinearModel fit residuals + coefficients for ~3 contrasts: 20 k × 100 × 8 B + 20 k × 5 × 8 B ≈ 16 MB + small
- Design matrix + ancillary: < 1 MB
- Hibernate session: probe entities (one per row) + vectors + dimensions

**Peak heap ≈ ~50–80 MB for the data, easily 2–3× that with overhead.** This is well within JVM limits, but it compounds when 16 concurrent local-tasks workers each run one. A 200-sample perturbation EE doubles each matrix; a 1000-sample EE pushes peak past 500 MB per task.

### B3 — Intermediate writes (`DifferentialExpressionAnalysisDaoImpl.create`)

`DifferentialExpressionAnalysisDaoImpl.java:107-217` is well-engineered:

1. `super.create(entity)` cascades the parent `DifferentialExpressionAnalysis` + every `ExpressionAnalysisResultSet` + every `PvalueDistribution` via Hibernate (uses default jdbc batch_size=32).
2. Manual JDBC batch insert of all `DifferentialExpressionAnalysisResult` rows using `INSERT_RESULT_SQL` with `addBatch` + `executeBatch` + `getGeneratedKeys` (line 168-184). Single round-trip per chunk (driver-side; with `rewriteBatchedStatements=true` the driver rewrites these as multi-row INSERTs).
3. Manual JDBC batch insert of all `ContrastResult` rows the same way (line 186-211).
4. `session.flush()`.

**Verdict:** results & contrasts are batched well; the cascade-persisted entities (analysis, result-sets, pvalue-distribution) use default `jdbc.batch_size=32` which is conservative but not pathological.

**The catch:** all results are held in memory until the batch insert is done — the `LinkedHashSet`-style ordering preserved across the analysis. For a 4-result-set analysis with 50 k probes each → 200 k `DifferentialExpressionAnalysisResult` objects in heap simultaneously, plus their contrasts (~3 per result → 600 k `ContrastResult` objects). With ~200 B per Hibernate entity overhead, that's ~120 MB of just-Hibernate state during the insert phase.

### B4 — Wall time at scale

LinearModelAnalyzer is instrumented:
- Line 1470: `"Model fit preparedData matrix %d x %d: %d ms"`
- Line 1383: `"Analysis finished in %.1f minutes."` when total > 1 min.
- Line 1402: `"Analysis running, %.1f minutes elapsed..."` ticked every 60 s.

The `runAnalysisInBackground` (line 1363-1407) wraps the LinearModelAnalyzer fit in a `Future<>` on `taskExecutor` and polls every 60 s, with a configurable hard timeout `config.getMaxAnalysisTimeMillis()`.

**Estimated breakdown for a 200-sample perturbation EE (20 k probes, 3 factors, RNA-seq):**

| Stage | Estimated wall time |
|---|---|
| `thawLite(ee)` + existing-analysis lookup | <100 ms |
| `getProcessedExpressionDataMatrix(ee, thawAssays=true)` | 4–8 s (matrix-assembly cost from Round 3 B1, plus thawAssays on the BioAssayDimension) |
| `filter + ensureLog2Scale` | 1–3 s (allocates a new dense matrix) |
| `MeanVarianceEstimator` (RNA-seq voom path) | 2–10 s |
| `LeastSquaresFit` (parallel.jdk regression over 20 k rows) | 5–30 s |
| `ModeratedTstat.ebayes` | 1–2 s |
| Batched JDBC insert of ~60 k results + ~180 k contrasts | 5–15 s |
| Bulk ZIP archive write (`writeOrLocateDiffExAnalysisArchiveFile`) | 5–20 s |
| **Total** | **~25–90 s typical, 2–5 min for large EEs** |

Round 3 already measured matrix-assembly at 4–5 s + JVM tax. The LinearModelAnalyzer fit itself logs its time — searching prod logs for the `"Model fit preparedData matrix"` line would give the empirical distribution.

### B5 — Concurrency

| Lane | Pool | Capacity |
|---|---|---|
| Curator DEA trigger (`POST /datasets/{id}/analyses/differential`) | `TaskRunningService` → `TaskExecutor` (`taskExecutor` Primary bean) | `gemma.localTasks.corePoolSize=16` |
| `LinearModelAnalyzer.runAnalysisInBackground` | Same `taskExecutor` (injected as `AsyncTaskExecutor`) | shares the 16-thread pool |
| Async file generation (`writeOrLocateTabularSingleCellExpressionDataAsync`, archive writes) | `expressionDataFileTaskExecutor` | corePoolSize=4, queueCapacity=10 |

**Implications:**

1. The DEA task and the LinearModelAnalyzer fit BOTH live on the same 16-thread pool. A curator-triggered DEA submits a task to the pool, then that task submits a sub-task to the SAME pool. Under contention this can deadlock if the pool fills with outer tasks waiting on inner-task futures. With 16 threads and DEA being one of the rare "submit-and-wait" patterns in the codebase, this is unlikely to bite in practice but the pattern is fragile.
2. Slurm offloading: **none**. Grep for "slurm" / "sbatch" in `gemma-core/src/main/java/` returns zero hits relevant to DEA. Everything runs in-JVM.
3. The DEA archive file write at the end of `persistAnalysis` (`ExpressionDataFileServiceImpl.writeOrLocateDiffExAnalysisArchiveFile`, line 1032-1055) runs synchronously inside the DEA task, NOT on `expressionDataFileTaskExecutor` — so it tacks 5-20 s onto the DEA task duration.
4. No Slurm. No Rserve. The LinearModelAnalyzer is pure-Java (ubic.basecode).

## Top findings (4)

1. **Cold-cache TTFB for `/datasets/{id}/data/{processed,raw}` is "build the whole matrix then write the gzipped file before any bytes go out".** The fallback streaming path only fires on `IOException` during the cache write, not on cache-miss. A curator hitting a never-rebuilt EE waits for the entire cache build before seeing the first byte. Mitigation: rebuild the cache off-band (e.g. on EE update) so the request always lands hot, OR move the streaming-fallback path to fire on cache-miss-after-lock-timeout instead of only on IOException.

2. **`/resultSets/{id}` TSV materializes all results in heap before streaming.** `loadWithResultsAndContrasts` builds a `LinkedHashSet<DifferentialExpressionAnalysisResult>` + thaws probe + contrasts + factor-values in series. For a 50 k-result set this is several seconds of TTFB and ~400 batched DB round-trips (default_batch_fetch_size=128) before the first byte goes to the wire. Mitigation: scrollable result iteration with per-row `writer.write` + `writer.flush` (the streaming pattern other endpoints use), OR cache the TSV on disk like `/data/processed` does (the underlying data is immutable once persisted, so cache-forever-with-id is safe).

3. **No `/datasets/{id}/data/dea` endpoint.** The DiffEx archive ZIP (`writeOrLocateDiffExAnalysisArchiveFile`) is built as a side effect of `persistAnalysis` but is not exposed through any gemma-rest endpoint that I could find. If curators want "give me all DEA results for this EE in one shot" they have to: (a) hit `/datasets/{id}/analyses/differential` to enumerate analysis IDs, (b) hit `/datasets/{id}/analyses/differential/resultSets` to get result-set IDs, (c) loop over `/resultSets/{id}` with TSV accept. That's an N-trip round-trip pattern for a single user-intent. Mitigation: add `/datasets/{id}/data/dea` that sendfiles the existing on-disk DiffEx archive ZIP, falling back to a streaming build on cache miss. The archive writer already exists.

4. **DEA run is single-threaded per-EE on a 16-wide shared pool**, with the LinearModelAnalyzer fit landing on the same pool that holds the outer task. This is acceptable today but won't scale to many concurrent curator triggers on large perturbation EEs (heap pressure + pool contention). The bulk-archive write also runs synchronously inside the DEA task instead of being offloaded to `expressionDataFileTaskExecutor`. Mitigation: route archive writes to the file-task executor (it exists, queueCapacity=10, dedicated for exactly this kind of work).

## Fix directions

- **TSV export hot path is correct; the cold path is the gap.** A nightly / on-update job that rebuilds the gzipped TSV caches for every EE that's been mutated removes the TTFB cliff entirely.
- **`/resultSets/{id}` TSV needs the same disk-cache treatment.** Each result-set's contents are immutable after the analysis is persisted — perfect for forever-cached on-disk gzipped TSV addressed by result-set ID. Cache invalidation is "result-set was deleted" → delete the file.
- **Add `/datasets/{id}/data/dea`** as a sendfile-the-existing-archive endpoint. The producer code is in `writeOrLocateDiffExAnalysisArchiveFile` already; only the REST wrapper is missing.
- **Move the DEA archive write off the DEA-run task** onto `expressionDataFileTaskExecutor`. Even just `expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileAsync(...)` (precedent: `writeOrLocateTabularSingleCellExpressionDataAsync` line 3086) saves 5–20 s on every curator-triggered DEA, freeing the local-tasks pool.
- **Hibernate `thawResultsAndContrasts`**: consider adding an entity-level `@BatchSize` hint on `DifferentialExpressionAnalysisResult.probe` and `DifferentialExpressionAnalysisResult.contrasts` of e.g. 512 to multiply throughput by 4 over the default 128, OR rewrite the loader with explicit `fetch all properties` HQL joins for probe + contrasts in one query.

## Cross-cutting observations

- **The "build → file → sendfile" pattern is the right shape for everything in this layer.** It's used correctly for `/data/processed`, `/data/raw`, `/data/singleCell` (MEX + tabular), and `/design`. The places it's NOT used (`/resultSets/{id}` TSV) are the ones that need it most, because their inputs are immutable.
- **`@GZIP(alreadyCompressed=true)` + `sendfile` is the gold-standard streaming setup.** The header decorator splits cleanly into "before GZipEncoder" (for alreadyCompressed=false → encode + add header) and "after GZipEncoder" (for alreadyCompressed=true → add header only). No body re-encoding for cached files. No double-gzip.
- **`force=true` is admin-gated everywhere** (`checkIsAdmin()` at the top of the handler). Curators can rebuild a cache when they think it's stale — but they pay the full cold-build cost in their request. That's by design (the rebuild is intentional), just worth noting that "force" is a synchronous, foreground operation.
- **The DEA RUN dispatch is async-first** (`taskRunningService.submitTaskCommand` returns 202 + Location header). Polling `/tasks/{taskId}` is how curators learn the analysis is done. So the wall-time pain isn't in the request — it's in the task pool occupancy and the heap pressure.
- **Live-gemd `COUNT(*)` on PROCESSED_EXPRESSION_DATA_VECTOR returned 953 763 082 rows.** RAW was queryable but the multi-table union in a single statement was too slow to complete in budget. This is consistent with Round 3.
