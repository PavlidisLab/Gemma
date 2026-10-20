# JDK 21 features — migration recce

Date: 2026-05-21. Baseline `06c5ff5de1`. Branch `recce-jdk21-features`. Read-only.

This is a sites-and-citations enumeration for the next round of JDK-21-feature uptake. The repo already runs on amazon-corretto-21 and already ships a forward-prep factory (`ubic.gemma.core.util.concurrent.Executors.newVirtualThreadPerTaskExecutorIfAvailable()`); ~8 callsites use it. The doc-comment on that factory says "no callsite has been migrated yet" — that comment is stale (gemma-core/src/main/java/ubic/gemma/core/util/concurrent/Executors.java:68).

## ROI ranking — read this first

| Rank | Change | Effort | Win | Risk |
|---|---|---|---|---|
| 1 | `TaskRunningServiceImpl` executor → VT-per-task | low (1 line + bind-to metric rewrite) | medium-large (background-tasks are heterogenous, mostly IO+DB blocking) | low — no `synchronized` in the task path |
| 2 | `expressionDataFileTaskExecutor` → VT-per-task | low (Spring `@Bean` rewrite to wrap `Executors.newVirtualThreadPerTaskExecutorIfAvailable()` as a `ConcurrentTaskExecutor` for the same bean name) | large (every TSV/CEL archive build blocks on disk + DB streaming) | low — work is pure IO/streaming |
| 3 | Generational ZGC flag in `Dockerfile` `CATALINA_OPTS` | trivial (2 flags) | medium for heap pressure / tail latency on big DEA/matrix endpoints | low — fall back to G1 by removing flag |
| 4 | Pattern-matching switch on the `AbstractMatrix.asDoubles` numeric-instanceof chain (8 branches, line 121-139) | low-medium (mechanical) | small (readability + a small dispatch micro-opt) | none |
| 5 | `Math.clamp` — exactly one mechanical site (`GeoBrowserImpl.java:303`) | trivial | nil (cosmetic) | needs NaN check (input is `int`, safe) |
| 6 | `HashMap.newHashMap(int)` sweep — ~25 capacity-hint sites | trivial (mechanical) | nil (cosmetic + correct capacity math) | none |

Top 3 wins: TaskRunningServiceImpl, expressionDataFileTaskExecutor, generational ZGC.

Blocking pinning-risk site that must be refactored before broader VT adoption in the Entrez path: `EntrezUtils.doNicely` (gemma-core/src/main/java/ubic/gemma/core/loader/entrez/EntrezUtils.java:55). See Section 1.4.

---

## 1. Virtual threads — ExecutorService migration candidates

The repo's two forms of executor:
1. `ubic.gemma.core.util.concurrent.Executors.*` — Gemma's wrapping factory, which delegates through `DelegatingSecurityContextExecutorService` + `DelegatingThreadContextExecutorService` so MDC + SecurityContext propagate. Already has a VT helper.
2. Raw `java.util.concurrent.Executors.*` or `org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor` — bypass the wrapping. Some of these should be migrated to the Gemma factory regardless of VTs.

### 1.1 Already migrated to VTs (no action — verification only)

| Site | Work | Notes |
|---|---|---|
| `gemma-core/src/main/java/ubic/gemma/core/loader/util/fetcher/FtpArchiveFetcher.java:75` | FTP archive fetch (download single file) | IO. VT good. |
| `gemma-core/src/main/java/ubic/gemma/core/loader/util/fetcher/FtpArchiveFetcher.java:168` | Archive unpack (`expander.perform()` — Ant Untar) | mixed (disk IO + libz CPU); fine as VT, libz releases the carrier on syscall |
| `gemma-core/src/main/java/ubic/gemma/core/loader/util/fetcher/FtpFetcher.java:98` | FTP download | IO. VT good. |
| `gemma-core/src/main/java/ubic/gemma/core/loader/util/fetcher/HttpFetcher.java:76` | HTTP download via `URL.openStream` | IO. VT good. |
| `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/GeoFamilyParser.java:119` | Stream-read + parse one GSE family file | IO-dominated. VT good. |
| `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/singleCell/GeoSingleCellDetector.java:604` | Concurrent supplementary-file downloads | IO. VT good. **Caveat**: `getExecutor()` is `synchronized` (line 601), but only as a lazy-init guard — doesn't block IO under the lock. Safe. |
| `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/service/GeoBrowserServiceImpl.java:104` | One-off background `initializeLocalInfo` | IO. VT good. |
| `gemma-cli/src/main/java/ubic/gemma/apps/UnifiedOntologyUpdaterCli.java:135` | Parallel OBO/OWL downloads via `SimpleDownloader.downloadInParallel` | IO. VT good. |

The doc-comment on `Executors.newVirtualThreadPerTaskExecutorIfAvailable()` should be updated to reflect that ~8 callsites are migrated (file: `gemma-core/src/main/java/ubic/gemma/core/util/concurrent/Executors.java:68` — "No callsite has been migrated to this factory yet" is now wrong).

### 1.2 Candidates — STRONG (IO-bound, fan-out, currently platform threads)

#### `TaskRunningServiceImpl` — fixed pool of background tasks
- File:line: `gemma-core/src/main/java/ubic/gemma/core/job/TaskRunningServiceImpl.java:66`
- Construction: `Executors.newFixedThreadPool( numberOfThreads, new SimpleThreadFactory(...) )`
- Work: submits arbitrary `Task` impls (Spring-MVC-style background jobs — long preprocessing/DEA/import jobs). The work is heterogeneous — predominantly DB + IO, with some CPU-heavy DEA jobs interleaved.
- Recommendation: switch to `Executors.newVirtualThreadPerTaskExecutorIfAvailable()`. Tasks are typically minutes-long and dominated by DB and disk. `numberOfThreads` is a soft cap today; a VT executor would remove the queue-head-of-line blocking on heterogeneous tasks. Need to drop the metrics wiring (`GenericExecutorMetrics` at line 159 — won't bind cleanly to a per-task VT executor; the Micrometer binders only handle `ThreadPoolExecutor` + Spring `ThreadPoolTaskExecutor` per `GenericExecutorMetrics.java:34` / `GenericTaskExecutorMetrics.java:39`).
- Win: medium-large.
- Pinning audit: `submitTask` body uses `DelegatingSecurityContextCallable` — no `synchronized`. Clean.

#### `expressionDataFileTaskExecutor` — bounded data-file build pool
- File:line: `gemma-core/src/main/java/ubic/gemma/core/config/ServiceBeansConfig.java:68`
- Construction: Spring `ThreadPoolTaskExecutor` with `corePoolSize` + `queueCapacity`.
- Work: archive builds (TSV / CEL / count matrix) — pure disk-write + DB-stream. The default core pool is small (~4) and the bounded queue is the bottleneck under load.
- Recommendation: migrate to `Executors.newVirtualThreadPerTaskExecutorIfAvailable()`. Backpressure changes meaningfully (no queue cap; submission never blocks) — note this is a deliberate behavioural shift, ops needs to know. If preserving backpressure matters, wrap with a `Semaphore` permit-based gate.
- Win: large. The two recent commits (`4b454b450a` DEA archive async, `1aedf6bd46` data-export async-build) feed this executor.
- Pinning audit: `ExpressionDataFileHelperService` and `ExpressionDataFileUtils` — no `synchronized` in the file-build path (sampled both; only `instanceof` chains).

#### `ontologyTaskExecutor` — ontology loader pool
- File:line: `gemma-core/src/main/java/ubic/gemma/core/ontology/OntologyConfig.java:49`
- Construction: `ThreadPoolTaskExecutor` with `corePoolSize` (`gemma.ontology.loader.corePoolSize`).
- Work: parallel ontology loads (TDB + Jena `read`); each load is a long HTTP+disk-IO operation.
- Recommendation: VT-per-task. The number of ontologies is fixed (~12 declared in `OntologyConfig`); each load blocks on download. Removing the pool-size ceiling lets all loads start in parallel; first-boot ontology-warmup wall time drops.
- Win: medium (boot-time only; not steady-state).
- Pinning audit: `TdbOntologyService` / `AbstractOntologyService` — DELEGATES to baseCode `OntologyService`, which is external. Out-of-tree; we don't control the lock posture. Recommend verifying with a one-off `-Djdk.tracePinnedThreads=full` boot test before flipping. Tracking risk.

#### `taskExecutor` (primary) — local short-lived tasks
- File:line: `gemma-core/src/main/java/ubic/gemma/core/config/ServiceBeansConfig.java:54`
- Construction: Spring `ThreadPoolTaskExecutor`, `corePoolSize=${gemma.localTasks.corePoolSize}`.
- Work: anything injected as `TaskExecutor` without qualifier. Wide net. Sampled grep finds it consumed by `OntologyServiceImpl:125` (where it's *overridden* to `SimpleAsyncTaskExecutor`), but other consumers exist.
- Recommendation: SECOND PASS — audit consumers (`grep -rn '@Qualifier("taskExecutor")\\|TaskExecutor.*taskExecutor' gemma-core/src/main`) before flipping. The fact that `OntologyServiceImpl` shadowed it with a plain `SimpleAsyncTaskExecutor` is suspicious — there's a historical reason. Save for after the obvious wins land.
- Win: unknown until consumer audit completes.

### 1.3 Candidates — MEDIUM

#### `SingularValueDecomposition.computeSVD` — timeout-guarded single SVD
- File:line: `gemma-core/src/main/java/ubic/gemma/core/util/math/linalg/SingularValueDecomposition.java:163`
- Construction: `Executors.newSingleThreadExecutor()` (raw java, no wrapping — should at least be moved to `ubic.gemma.core.util.concurrent.Executors`).
- Work: CPU-bound SVD wrapped in a future for wall-clock timeout fallback.
- Recommendation: **NOT a VT candidate** (CPU-bound). But should be migrated to the Gemma wrapping factory for MDC + SecurityContext propagation parity. Mechanical.
- Win: cosmetic — pure correctness + observability win.

#### `SingleCellDataLoaderCli` — single-cell transform pool
- File:line: `gemma-cli/src/main/java/ubic/gemma/apps/SingleCellDataLoaderCli.java:359`
- Construction: `Executors.newFixedThreadPool( transformThreads, ... )`
- Work: H5AD / MEX / AnnData transforms. CPU + disk-IO mix; the transforms run external Python scripts (`AbstractPythonScriptBasedTransformation`) so are wall-clock-bound on the script.
- Recommendation: VT-per-task — the transform is dominated by Python subprocess wall-clock. The fixed-pool cap (`transformThreads`) is a manual concurrency knob the user passes via `--transform-threads`. Worth keeping a parameterized cap (semaphore) for memory pressure on the host.
- Win: medium for users who set `--transform-threads` high.

#### `ComBat.runNonParametric` — batch-effect nonparametric fit
- File:line: `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/batcheffects/ComBat.java:410`
- Construction: `java.util.concurrent.Executors.newCachedThreadPool()` (raw java).
- Work: **CPU-bound** matrix math (`nonParametricFit`).
- Recommendation: **DO NOT migrate to VT** (CPU-bound; VT would hurt). But should switch from `newCachedThreadPool()` to `newFixedThreadPool(numThreads)` — the loop on line 405 already computes `numThreads = min(batches.size(), Runtime.getRuntime().availableProcessors())`. Today's `newCachedThreadPool` only allocates `numThreads` threads anyway because that's how many submissions happen, but a fixed pool is more correct. Also move to the Gemma wrapping factory.
- Win: cosmetic correctness.

#### `AbstractCLI.createExecutor` — CLI batch-task pool
- File:line: `gemma-cli/src/main/java/ubic/gemma/cli/util/AbstractCLI.java:549`
- Construction: `Executors.newFixedThreadPool( this.numThreads, threadFactory )` (Gemma factory).
- Work: per-CLI batch task — varies widely (DEA, GEO loads, ontology updates, etc).
- Recommendation: SECOND PASS — too heterogenous to flip blindly. Per-CLI VT adoption is fine; the orchestrator should decide on each.
- Win: depends on CLI.

#### `GoogleAnalytics4Provider` — single-thread scheduled flush
- File:line: `gemma-rest/src/main/java/ubic/gemma/rest/analytics/ga4/GoogleAnalytics4Provider.java:66`
- Construction: `Executors.newSingleThreadScheduledExecutor()` (raw java).
- Work: periodic HTTP POST to GA4 collect endpoint.
- Recommendation: stays platform-thread (single-thread scheduled). But should migrate to Gemma wrapping factory.
- Win: cosmetic.

### 1.4 PINNING-RISK sites (audit before broader VT push)

#### `EntrezUtils.doNicely` — rate-limiter that holds the monitor across the HTTP call
- File:line: `gemma-core/src/main/java/ubic/gemma/core/loader/entrez/EntrezUtils.java:55`
- Code shape:
  ```java
  synchronized ( monitor ) {
      // ... wait / sleep for rate-limit window
      try { return task.call(); }   // <-- HTTP call HERE, under the monitor
      finally { lastCall = System.currentTimeMillis(); }
  }
  ```
- JDK 21 behaviour: `Object.wait()` on `monitor` doesn't pin (special-cased in JDK 21), but the `task.call()` invocation INSIDE the `synchronized` block DOES pin the carrier if `task.call()` blocks on a socket read. Every Entrez consumer (`PubMedSearch`, `NCBIGeneInfo`, GEO metadata) goes through this.
- However: today the entire `doNicely` is a serialization gate by design — only one Entrez call at a time. So VT pinning is moot — there's never more than one VT inside this monitor at a time. **The risk is for downstream code: if any consumer wraps `doNicely` inside a VT-per-task fan-out, ALL those VTs serialize on the carrier-pinning monitor, and the carriers go down.**
- Recommendation: keep `synchronized` for now (the intent — rate-limit — is correct). When/if a VT fan-out hits this path and tracePinnedThreads flags it, rewrite to `ReentrantLock` + explicit sleep/await. NOT a blocker for the wins in 1.2.

#### `EntrezXmlUtils.parse` — DocumentBuilder synchronization
- File:line: `gemma-core/src/main/java/ubic/gemma/core/loader/entrez/EntrezXmlUtils.java:45,63`
- `synchronized ( documentBuilder )` around `documentBuilder.parse( is )`. DocumentBuilder is not thread-safe — the comment at line 34 documents this.
- VT impact: `parse()` reads from an InputStream — if the stream is socket-backed and a chunk is unavailable, the carrier pins. With VT fan-out into Entrez parsing, this serializes on the single static `documentBuilder`.
- Recommendation: switch to `ThreadLocal<DocumentBuilder>` or to a pool keyed by carrier (or just construct one per call — `XMLUtils.createDocumentBuilder` cost is sub-millisecond). Then drop the `synchronized`. Out of scope for the JDK-21 mechanical sweep but worth a tracking note.

#### `SraDateParser` / `SraRuninfoParser` — `SimpleDateFormat` mutex
- Files: `gemma-core/src/main/java/ubic/gemma/core/loader/expression/sra/model/SraDateParser.java:14,21`, `SraRuninfoParser.java:132`
- `synchronized ( format )` around `format.parse(...)`. No IO inside the lock — pure CPU parse. Pinning is safe (no carrier syscall in the locked region). Move to `DateTimeFormatter` (immutable, thread-safe, no lock needed) — cheap independent cleanup.

### 1.5 NON-CANDIDATES (CPU-bound, keep platform threads)

- `ComBat` parallel batch fit — Section 1.3 above. CPU.
- DEA `LinearModelAnalyzer` parallel-stream usage — pure math.
- Any `Stream.parallel()` consumer — uses ForkJoinPool common; that's correct for CPU work.

### 1.6 `CompletableFuture.supplyAsync` callsites

Only one `supplyAsync` call in production:
- `gemma-core/src/main/java/ubic/gemma/core/job/TaskRunningServiceImpl.java:132` — passes an explicit `executor` argument (the same `executorService` from 1.2.1). NOT defaulting to ForkJoinPool. Correct.

No `runAsync` callsites in production.

### 1.7 ThreadLocal audit

VTs allocate one thread per task → high TL allocation can sting.

| Site | Pattern | VT concern |
|---|---|---|
| `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/convert/ScaleTypeConversionUtils.java:21-24` | Four primitive-array TLs (1-element bufs) for scalar conversion | **Yes** — each VT allocates four small arrays. Cost: ~64 bytes / VT. Not catastrophic, but: `clearScalarConversionThreadLocalStorage()` only fires from `MatrixWriter` (line 123) — VT-per-row workloads bypass the cleanup. Recommend dropping the TLs entirely (use stack-local arrays — JIT will scalarize). |
| `gemma-core/src/main/java/ubic/gemma/persistence/util/AclQueryUtils.java:264` | `NATIVE_AOI_ID_COLUMN` (request-scoped column-name stash) | Used inside ACL-filtered queries; one TL string per VT. Negligible. |
| `gemma-core/src/main/java/ubic/gemma/core/security/acl/AclEventListener.java:113` | `STASH` (event-listener Map per thread) | Per VT one HashMap — could be 10s of KB if events stash a lot. Audit if VTs ever cross this path; today event listeners run inside Hibernate flush, not in user code. |
| `gemma-core/src/main/java/ubic/gemma/model/analysis/expression/diff/RandomDifferentialExpressionAnalysisUtils.java:19` | `ThreadLocal<NormalDistribution>` — one Apache Commons NormalDistribution per thread (random sampler) | One per VT × number-of-VTs in flight. NormalDistribution is small but contains a Well19937c RNG state (~2.5KB). If thousands of VTs sample concurrently, ~10MB. Borderline; acceptable. |
| `gemma-core/src/main/java/ubic/gemma/core/security/authorization/acl/AclEntryAfterInvocationCollectionFilteringProvider.java:62` (and the parallel class under `core/security/acl/afterinvocation/`) | `ThreadLocal<Iterator<DomainObjectWithPermission>>` | Request-scoped iterator. Per VT one ref. Fine. |

Overall: **no TL site is a blocker**, but `ScaleTypeConversionUtils` is a cleanup candidate worth raising independent of JDK 21.

---

## 2. Generational ZGC — JVM flag changes

### Dockerfile
- File:line: `Dockerfile:93-97`
- Current `CATALINA_OPTS`:
  ```
  -Dgemma.appdata.home=/data/gemma
  -Dspring.profiles.active=production
  -Djava.security.egd=file:/dev/./urandom
  -XX:MaxRAMPercentage=75.0
  -XX:+ExitOnOutOfMemoryError
  ```
- Recommendation: add `-XX:+UseZGC -XX:+ZGenerational`. The `ZGenerational` flag enables JDK 21's new generation-aware Z mode (the default G1 collector is fine for short heaps; for production Tomcat with multi-GB heaps under the kinds of allocations Gemma does — large matrix loads, transient ResultSet rows — generational Z noticeably cuts pause-time outliers).
- Order of operations: keep `MaxRAMPercentage`, drop nothing. Add the two new flags after `ExitOnOutOfMemoryError`.
- Sanity check: production Tomcat heap is multi-GB (per ops; not in repo). ZGen needs >2GB heap to benefit. Tiny dev heaps (1GB) won't gain.

### Maven / local dev
- No `MAVEN_OPTS` block in repo (checked `pom.xml`, `.mvn/`, `gemma-core/pom.xml`). Local `mvn verify` runs short-lived JVMs; G1 default is fine. No change.

### Surefire / Failsafe forks
- `gemma-core/pom.xml` defines argLines for the test JVMs. Not worth ZGen there — test JVMs are short-lived. No change.

### Production server JVM (outside Docker)
- Not in repo — ops controls their own `setenv.sh` on the bare Tomcat install. Flag the recommendation in the rollout note so they sync the Docker flags.

JDK 21 release-notes caveats: with `+ZGenerational`, the JVM uses a new mark-evacuate pattern; small heaps (<2GB) see no benefit, and the page-coalescing behaviour changes — memory observability dashboards that key off generation names (`G1 Old Gen` etc) need to be updated to `ZGC Old Generation` / `ZGC Young Generation`. Tell the ops team.

---

## 3. `Math.clamp` sweep — mechanical candidates

Exactly one site matched the `Math.max(.. Math.min ..)` / inverse pattern in production:

| Site | Current | Suggested | NaN concern |
|---|---|---|---|
| `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/service/GeoBrowserImpl.java:303` | `Math.min( pageSize, Math.max( count - start, 0 ) )` | `Math.clamp( count - start, 0, pageSize )` | none — all `int`s |

This is a cap-into-range clamp on a pagination expression. Trivial. No NaN concern because the values are integer counters (`pageSize`, `count`, `start`). `Math.clamp(long, int, int)` overload covers this exactly.

No probability-clamp sites (`Math.max(0, Math.min(1, x))`) were found in production — those are the dangerous-NaN-semantics ones, so this sweep is free of footguns.

---

## 4. Pattern matching for `switch` — dispatch site candidates

The fertile sites are numeric-instanceof chains where every branch downcasts to a `Number`. Three strong candidates and one weak one:

### 4.1 `AbstractMatrix.asDoubles` — 8-way numeric type dispatch
- File:line: `gemma-core/src/main/java/ubic/gemma/core/util/matrix/AbstractMatrix.java:121-143`
- Today: 8 `else if (value instanceof X) result = ((X) value).doubleValue()` branches.
- Rewrite:
  ```java
  result[i][j] = switch (value) {
      case null -> Double.NaN; // never reached — isMissing check before
      case Integer n -> n.doubleValue();
      case Long n -> n.doubleValue();
      case Double n -> n;
      case Boolean b -> b ? 1.0 : 0.0;
      case String s -> tryParseDouble(s);
      case BigDecimal b -> b.doubleValue();
      case BigInteger b -> b.doubleValue();
      case Date d -> (double) d.getTime();
      default -> (double) value.hashCode();
  };
  ```
- Win: small. Readability. Also exhaustiveness checking by compiler.

### 4.2 `ScaleTypeConversionUtils.convertScalar` — 4-way Number dispatch
- File:line: `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/convert/ScaleTypeConversionUtils.java:48-66`
- Four branches: Float / Double / Integer / Long. Mechanical. Each branch uses a ThreadLocal scratch array — see Section 1.7 for the orthogonal recommendation to drop the TLs.

### 4.3 `TsvUtils.format(Object)` — 4-way numeric+Date dispatch
- File:line: `gemma-core/src/main/java/ubic/gemma/core/util/TsvUtils.java:235-249`
- Five branches: Double / Integer / Long / Date / other-non-null. Trivial rewrite.

### 4.4 `QuantitationTypeDetectionUtils` — 7 instanceof-chain sites against `Matrix` types
- File: `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/detect/QuantitationTypeDetectionUtils.java`
- Lines 111, 223, 248, 275, 295, 319, 360, 461 — all branch on `matrix instanceof DenseDoubleMatrix2D` / `CompRowMatrix`. Each chain is short (2-3 branches) so the win is small but consistent.

### 4.5 `HibernateSearchSource.extractId` — NOT a strong candidate
- File:line: `gemma-core/src/main/java/ubic/gemma/core/search/source/HibernateSearchSource.java:334`
- Single-branch `instanceof` check; the project brief specifically mentioned this site, but it's a single `if (... instanceof EntityReference)` (line 334) followed by a nested ternary on `Long` (line 336). The classic `searchFor` dispatch chain doesn't exist here — search dispatch is by `Class<T> clazz` argument, not instanceof. Skip.

### 4.6 Skipped — single-branch `instanceof` sites
About 20 single-branch `else if instanceof X` sites across the codebase (mostly `expressionExperiment instanceof ExpressionExperimentSubSet`). Pattern matching for switch is overkill for single-branch dispatch; `if (x instanceof X y)` binding is sufficient there and is already JDK-16 syntax. Out of scope.

---

## 5. Quick wins — `HashMap.newHashMap(int)`, `SequencedCollection`

### `HashMap.newHashMap(int)` sweep
JDK 19+ adds `HashMap.newHashMap(int)` / `LinkedHashMap.newLinkedHashMap(int)` which take the expected entry count instead of the legacy capacity-hint (which had to be `expected / 0.75 + 1` to actually avoid resize). About 25 sites in production code pass a sized hint:

Top candidates (representative — full list via grep):
- `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/batcheffects/ExpressionExperimentBatchInformationServiceImpl.java:90`
- `gemma-core/src/main/java/ubic/gemma/core/analysis/expression/diff/LinearModelAnalyzer.java:735,736,1330`
- `gemma-core/src/main/java/ubic/gemma/core/util/ListUtils.java:24,31`
- `gemma-core/src/main/java/ubic/gemma/core/security/SecurityServiceImpl.java:94,120,266`
- `gemma-core/src/main/java/ubic/gemma/core/datastructure/matrix/AbstractMultiAssayExpressionDataMatrix.java:103,105,452,454`
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/AnnDataSingleCellDataLoader.java:796,982`
- `gemma-core/src/main/java/ubic/gemma/persistence/service/common/auditAndSecurity/AuditEventDaoImpl.java:227,254`
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentReadServiceImpl.java:1042`

Mechanical IntelliJ sweep. One commit. Cosmetic + the actual capacity math is now correct (the old idiom `new HashMap<>(N)` reserves slots for `N * 0.75` entries before resize, so most callers were slightly under-provisioning).

### `SequencedCollection` — minor
Not surveyed exhaustively. One example: code like `list.get(0)` to peek the head, or `list.get(list.size() - 1)` to peek the tail, is mechanically replaceable with `.getFirst()` / `.getLast()`. Out of scope for this recce — pickup opportunistically in code touched for the other items.

---

## Commit sequencing recommendation

The orchestrator can land these as discrete commits, in this order:

1. `chore(jdk21): generational ZGC flags in Dockerfile CATALINA_OPTS` — 2-line change.
2. `chore(jdk21): Math.clamp at GeoBrowserImpl pagination` — 1-line change. Test bait — confirms the whole sweep style works.
3. `chore(jdk21): HashMap.newHashMap(int) sweep (~25 sites)` — mechanical.
4. `feat(jdk21): TaskRunningService background-tasks executor → VT-per-task` — drop Micrometer binder; otherwise drop-in.
5. `feat(jdk21): expressionDataFileTaskExecutor → VT-per-task` — flag behavioural-shift (no queue cap) in commit message.
6. `feat(jdk21): ontologyTaskExecutor → VT-per-task` — verify with `-Djdk.tracePinnedThreads=full` smoke during boot.
7. `chore(jdk21): pattern switch for AbstractMatrix.asDoubles, ScaleTypeConversionUtils, TsvUtils` — readability + exhaustiveness.
8. (Optional, follow-up sweep) `refactor(jdk21): drop SimpleDateFormat ThreadLocal in SRA parsers, switch to DateTimeFormatter` — independent cleanup surfaced by the pinning audit.
9. (Optional, follow-up) `refactor: drop scalar-conversion ThreadLocals in ScaleTypeConversionUtils` — independent cleanup surfaced by the VT TL audit.

Items 4-6 each warrant a brief perf probe before/after (cold-cache TaskRunningService wall time; expressionDataFile end-to-end TSV-build wall time; boot-time ontology-warmup wall time). All three are claimed wins that should show on a probe.
