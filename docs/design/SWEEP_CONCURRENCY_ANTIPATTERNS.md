# Concurrency anti-pattern sweep (post-15006ca9c0)

Branch baseline: `ca05591e53`. Worktree: `.claude/worktrees/agent-perf-concurrency-broader-sweep`.
Companion to `SWEEP_SYNCHRONIZED_COLLECTION_ITERATION.md` (different anti-patterns; no overlap).

## Coverage

Swept (10 categories):

1. Missing `volatile` on shared-mutable fields
2. `synchronized` held across slow I/O (DB / file / network / subprocess)
3. Double-checked locking without `volatile`
4. `ConcurrentHashMap.compute` / `merge` semantics
5. `AtomicReference.compareAndSet` misuse
6. Race-prone class initializer / `static {}` blocks
7. `Thread.sleep` as a retry / backoff
8. `wait` / `notify` without explicit lock object
9. Spring `@Async` methods that share `@Transactional` boundary
10. `ExecutorService` not shut down on bean destroy

**Excluded** (covered by prior sweep): `Collections.synchronized*` iteration —
already addressed in `SWEEP_SYNCHRONIZED_COLLECTION_ITERATION.md`.

---

## Findings per category

### 1. Missing volatile

| file:line | code shape | risk | classification | fix? |
|---|---|---|---|---|
| `SlackAppender.java:75` | `private Slack slackInstance;` — written in `synchronized getSlackInstance()`, read unsync'd in `stop()` | stale-null in `stop()`, leaks Slack instance | NEEDS-FIX-LOW-RISK | **FIXED** |
| `ExtendedRuntime.java:14` | `private static ExtendedRuntime currentRuntime;` — DCL with NO sync and NO volatile, two-thread construction race + half-constructed publish risk | concurrent `getRuntime()` from FileLockManagerImpl readers | NEEDS-FIX-LOW-RISK | **FIXED** |
| `CorrelationStats.java:44,54` | static `DoubleMatrix2D` lookup caches written via `setQuick()` from concurrent callers (assigned in `static {}`; matrix mutated lazily, no sync) | torn cache writes; `SparseDoubleMatrix2D.setQuick` is not thread-safe | NEEDS-FIX-HIGH-RISK | surfaced (no fix; risk of breaking math hotpath) |
| `ManifestUtils.java:23` | `cachedProps` — accessed only in `synchronized` method | none | SAFE | — |
| `SettingsConfig.java:105` | `cachedSettingsPropertySources` — accessed only in `synchronized` method | none | SAFE | — |

### 2. Synchronized across slow I/O

| file:line | code shape | risk | classification | fix? |
|---|---|---|---|---|
| `ShellDelegatingBlat.java:266 startServer()` | `public synchronized` — holds monitor across `ProcessBuilder.start()`, socket-connect, AND a `while(true)` busy-spin `isServerReachable()` loop (line 303-308, **no sleep, no timeout**) | one BLAT server start blocks the class monitor; busy-spin pegs a CPU core | NEEDS-FIX-HIGH-RISK | surfaced (BLAT path is rarely used; risk of breaking dev tooling) |
| `NcbiEntityResolver.java:30 resolveEntity()` | synchronized across `getResourceAsStream()` + `IOUtils.toByteArray()` | classpath-only; non-thread-safe `WeakHashMap` mandates sync | SAFE | — |
| `EntrezUtils.java:54 doNicely()` | synchronized across NCBI HTTP call (`task.call()`) | INTENTIONAL — rate limiter; lone slow call blocks all Entrez | SAFE-by-design | — |
| `OntologyExternalLinks.java:51,82` | sync on `externalLinks` across `reload()` (classpath) and lookup loop | classpath-only, fast | SAFE | — |
| `AbstractExpressionDataMatrix.java:92` | `synchronized( nf )` across `NumberFormat.format()` | required (NF not thread-safe); body is microseconds | SAFE | — |
| `StaticCacheKeyLock.java:35` | `synchronized( lockByKey )` across two-level `computeIfAbsent` on `WeakHashMap` | required; brief | SAFE | — |
| `FileLockManagerImpl.java:40` | `synchronized( fileLocks )` snapshot before iteration | brief snapshot; correct | SAFE | — |

### 3. Double-checked locking without volatile

| file:line | code shape | risk | classification |
|---|---|---|---|
| `ExtendedRuntime.getRuntime()` (see §1) | DCL without volatile AND without sync | as above, both legs broken | **FIXED** |
| `SlackAppender.getSlackInstance()` | NOT classic DCL — method is `synchronized` (no outer null check). With volatile field, all readers see correct state. | none after fix | **FIXED** |
| `AbstractAsyncFactoryBean.java:65-81` | classic DCL on `singletonBean`; field IS `volatile` (line 47) | correctly implemented | SAFE |

### 4. ConcurrentHashMap compute / merge

| file:line | shape | risk |
|---|---|---|
| `SingleCellExpressionExperimentAggregateServiceImpl.java:592,595` | `.compute(sample, (k,v) -> ...)` whose lambda calls `SingleCellSparsityMetrics.getNumberOfDesignElements(...)` — pure compute, no map re-entry, no I/O | SAFE |
| `BulkDataSlicerUtils.java`, `SingleCellSlicerUtils.java`, `SingleCellDataVectorAggregatorUtils.java` | `computeIfAbsent` lambdas build slice metadata — no map re-entry | SAFE |
| `FileLockManagerImpl.java:99,114 acquirePathLock` | `computeIfAbsent( path, this::createReadWriteLock )` — lambda is pure (creates ReentrantReadWriteLock) | SAFE |
| `SecurityServiceImpl.java:867` | `result.computeIfAbsent( s, k -> new HashSet<>() ).add( groupName )` — note `result` is local, not concurrent; non-issue | SAFE |

No re-entry / no I/O found in any compute/merge callback.

### 5. AtomicReference / Atomic* misuse

No bare `AtomicReference<Map<…>>` swap-mutate pattern found. All `AtomicBoolean` / `AtomicLong` uses are producer-done flags or simple counters — straightforward.

| file:line | usage | classification |
|---|---|---|
| `NCBIGene2GOAssociationLoader.java:46-47` | producer/consumer done flags, set+get | SAFE (race exists but is the intended polling loop — see §7) |
| `ArrayDesignProbeMapperServiceImpl.java:152-153` | same shape | SAFE-but-fragile (see §7) |
| `ReadWriteFileLock.java:50,120` | hold counters | SAFE |

### 6. Race-prone class initializer / `static {}`

22 `static {}` blocks audited. None perform network I/O. A few do classpath / filesystem reads:

| file | static {} content | risk |
|---|---|---|
| `BuildInfo.java`, `ManifestUtils.java` | read JAR manifest — first-accessor thread blocks until done, then publish is safe | SAFE |
| `AbstractExpressionDataMatrix.java:12` | configure NumberFormat | SAFE |
| `CorrelationStats.java:56` | allocate sparse matrices | SAFE for init; mutation later is the bug (see §1) |
| `HibernateSearchSource.java:140`, `OntologyIndexer.java:86` | configure analyzers | SAFE |
| `AclClassMetadata.java:62` | reflection cache | SAFE |

### 7. Thread.sleep as retry / backoff

| file:line | shape | risk | classification |
|---|---|---|---|
| `NCBIGene2GOAssociationLoader.java:104` | `while (!producerDone \|\| !consumerDone) Thread.sleep(1000)` — producer/consumer poll, no deadline | up to +1s latency on completion, CPU OK | NEEDS-FIX-HIGH-RISK (refactor to BlockingQueue / Future.get) — surfaced |
| `ArrayDesignProbeMapperServiceImpl.java:201` | same shape | same | surfaced |
| `NcbiGeneLoader.java:226` | same shape | same | surfaced |
| `SingularValueDecomposition.java:175` | poll `svdFuture.isDone()` with `Thread.sleep(100)` + deadline check; **but `Executors.newSingleThreadExecutor()` leaked** (never shut down) | thread-pool leak per SVD call | NEEDS-FIX-HIGH-RISK — surfaced |
| `SimpleRetry.java:54` | retry loop with `backoffDelay` (explicit retry helper) | INTENTIONAL | SAFE |

### 8. wait / notify

| file:line | shape | classification |
|---|---|---|
| `EntrezUtils.java:61` | `monitor.wait(timeout)` inside `synchronized(monitor)` — used to back off until rate-limit window passes | SAFE (textbook) |

No misuse found.

### 9. @Async methods sharing @Transactional

`@Async` is NOT used in production code (only referenced in `SchedulerConfig` doc comment and a test). No findings.

### 10. ExecutorService not shut down on bean destroy

| file:line | shape | risk | classification |
|---|---|---|---|
| `ComBat.java:410` | `Executors.newCachedThreadPool()` is shut down at line 435 — correct, scoped local | SAFE |
| `SingularValueDecomposition.java:171` | `Executors.newSingleThreadExecutor().execute(svdFuture)` — **the executor is NOT shut down anywhere**; leaks one thread per SVD | NEEDS-FIX-MEDIUM-RISK — surfaced |
| `TaskRunningServiceImpl.java:66` | shutdown in `destroy()` — correct | SAFE |
| `GeoSingleCellDetector.java:601 getExecutor()` | lazy-init via synchronized; class is `Closeable`, presumably shuts down on close. **Confirm shutdown in close().** | surfaced for orchestrator check |
| `AbstractAsyncFactoryBean.java` | `shutdownExecutorOnDispose` flag + `destroy()` — correct | SAFE |

---

## Fixes applied in this branch

| # | commit | shape |
|---|---|---|
| 1 | (this branch) | `SlackAppender.slackInstance` made `volatile` — eliminates stale-null read in `stop()` |
| 2 | (this branch) | `ExtendedRuntime.currentRuntime` made `volatile` + proper DCL — eliminates two-thread construction race and unsafe publication |

Both `mvn -pl gemma-core compile test-compile -q` compile-clean against baseline.

---

## Surfaced for orchestrator review (HIGH-RISK, not auto-fixed)

1. **`ShellDelegatingBlat.startServer()` (gemma-core/src/main/java/ubic/gemma/core/analysis/sequence/ShellDelegatingBlat.java:266)** — `public synchronized` held across (a) `ProcessBuilder.start()` fork, (b) `serverProcess.waitFor(100ms)`, (c) `while(true) { if (isServerReachable(...)) break; }` BUSY-SPIN with **no sleep, no timeout**. While the method holds the monitor, every other Blat caller blocks. Worse, on slow gfServer startup the busy-spin pegs a CPU core indefinitely. Recommended fix: add `Thread.sleep(100)` in the wait loop, add a deadline, and release the monitor (just use the running-server check pattern outside the sync block). Risk shape: BLAT is dev-tooling-only on most deployments, but a hung BLAT start would block all other concurrent BLAT users until JVM exit.

2. **`CorrelationStats.correlationPvalLookup` / `spearmanPvalLookup` (gemma-core/src/main/java/ubic/gemma/core/util/math/CorrelationStats.java:44,54)** — static `DoubleMatrix2D` caches mutated via `setQuick()` from concurrent callers with no synchronization. `SparseDoubleMatrix2D` is not thread-safe — under contention this can corrupt the sparse matrix internal structure (lost writes are tolerable; corruption isn't). Risk shape: invoked from coexpression / svd / batch-confound hotpaths. Fix is non-trivial (either `ConcurrentHashMap<Long,Double>` keyed on `(bin,dof)`, or switch to per-call computation without memo).

3. **`SingularValueDecomposition.computeSVD` (gemma-core/src/main/java/ubic/gemma/core/util/math/linalg/SingularValueDecomposition.java:171)** — `Executors.newSingleThreadExecutor().execute(svdFuture)` then `Thread.sleep(100)` poll loop, BUT the executor is never `shutdown()`. Every SVD computation leaks one ExecutorService (one thread + its TLS). Risk shape: SVD runs once per experiment during preprocess; on a fresh JVM with N preprocesses, N threads accumulate.

---

## Cross-cutting observations

- **The "polling loop with Thread.sleep" anti-pattern is endemic in loader code** (3 callsites across NCBI/Gene2GO/ArrayDesign/NcbiGene). Each is a producer/consumer where the consumer's `Thread.sleep(1000)` adds up to 1s of latency at the end of every load. A `CountDownLatch`-based rendezvous (or `BlockingQueue.take()` with a poison pill) is straightforward but spans 3 files with shared shape; worth a dedicated refactor commit.

- **Static caches lazily mutated from multiple threads** is the most common subtle bug (CorrelationStats, the synchronized-collection cases from the prior sweep). The repo already uses `ConcurrentHashMap` correctly in many places (e.g., `AuditedAspect.eventTypeCache`, `FilterablePropertyMeta` cache, `PropertySourcesConfiguration`); the outliers are legacy hand-rolled caches.

- **DCL bug count is small** (one — ExtendedRuntime). The codebase mostly avoids singleton patterns and uses Spring DI / class-init for initialization, which is the right call.

- **ExecutorService discipline is generally good** — most are tied to bean lifecycle with proper `destroy()`. The two leaks (SingularValueDecomposition + possibly GeoSingleCellDetector) are both buried in low-frequency analysis paths.

- **Compile-clean confirmed**: `mvn -pl gemma-core compile test-compile -q` passes after both fixes.

---

## Path

`/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-perf-concurrency-broader-sweep/SWEEP_CONCURRENCY_ANTIPATTERNS.md`
