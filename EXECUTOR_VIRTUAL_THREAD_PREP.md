# Executor centralization audit + virtual-thread prep

**Date:** 2026-05-18
**Branch:** `worktree-executor-virtual-prep` (off `phase2-acl-migrate` @ `08e760bdaf`)
**Scope:** Verify the central-`Executors`-helper claim from the Java 21 readiness recce
(`JAVA21_READINESS.md`, commit `86858b756`), enumerate rogue callsites, propose a path
to virtual-thread adoption, and add a forward-prep factory so future migration can
proceed callsite-by-callsite.

This is **recce + minor prep only**. No existing callsite has been changed.

---

## 1. Existing central helper

`gemma-core/src/main/java/ubic/gemma/core/util/concurrent/Executors.java`

Drop-in shadow of `java.util.concurrent.Executors`. Every factory returns an
`ExecutorService` / `ScheduledExecutorService` wrapped with:

- `DelegatingSecurityContextExecutorService` — propagates the Spring Security
  `SecurityContext` across task boundaries (so the task sees the same authenticated
  principal as the submitter).
- `DelegatingThreadContextExecutorService` — propagates the Log4j2 `ThreadContext`
  / MDC across task boundaries (so task logs carry the same correlation IDs).

Factory inventory **before this commit**:

| method | purpose |
|---|---|
| `newSingleThreadExecutor()` | one platform thread, default `ThreadFactory` |
| `newSingleThreadExecutor(ThreadFactory)` | one platform thread, named via factory |
| `newFixedThreadPool(int)` | bounded platform pool |
| `newFixedThreadPool(int, ThreadFactory)` | bounded platform pool, named via factory |
| `newCachedThreadPool()` | unbounded elastic platform pool |
| `newSingleThreadScheduledExecutor()` | scheduled single thread |

No virtual-thread-aware factory existed. No JDK-version detection. No built-in
`ThreadFactory` defaults — callers that want named threads pass in a
`SimpleThreadFactory` from `ubic.gemma.core.util.concurrent` (used by
`TaskRunningServiceImpl`, `GeoSingleCellDetector`, `SingleCellDataLoaderCli`,
`UnifiedOntologyUpdaterCli`).

**Added in this commit:** `newVirtualThreadPerTaskExecutorIfAvailable()` — see
section 5.

---

## 2. Rogue callsites

**Result: zero rogue `java.util.concurrent.Executors` factory calls.** Every
callsite under `gemma-*/src/{main,test}/java/**` resolves the `Executors` symbol
to `ubic.gemma.core.util.concurrent.Executors`, not the JDK class.

Method used to verify:

```
grep -rn "java\.util\.concurrent\.Executors\b" --include='*.java' .
   → 0 hits outside the central helper itself

grep -rn "Executors\.newCachedThreadPool|newFixedThreadPool|newScheduledThreadPool|
          newSingleThreadExecutor|newSingleThreadScheduledExecutor|newWorkStealingPool|
          newThreadPerTaskExecutor|newVirtualThreadPerTaskExecutor"
   → 28 callsites across 23 files; every file imports
     `ubic.gemma.core.util.concurrent.Executors`
```

The 23 files (14 main + 9 test):

**Main (14):**

| file | factory | use case |
|---|---|---|
| `gemma-cli/.../AbstractCLI.java` (2 calls) | `newFixedThreadPool` / `newSingleThreadExecutor` | mixed — CLI dispatch |
| `gemma-cli/.../UnifiedOntologyUpdaterCli.java` | `newFixedThreadPool` | **I/O** — ontology downloader |
| `gemma-cli/.../SingleCellDataLoaderCli.java` | `newFixedThreadPool` | mixed — transform pipeline |
| `gemma-core/.../analysis/preprocess/batcheffects/ComBat.java` | `newCachedThreadPool` | **CPU** — batch-effect math |
| `gemma-core/.../context/AbstractAsyncFactoryBean.java` | `newSingleThreadExecutor` | mixed — Spring async factory |
| `gemma-core/.../job/TaskRunningServiceImpl.java` | `newFixedThreadPool` | mixed — background tasks |
| `gemma-core/.../loader/expression/geo/GeoFamilyParser.java` | `newSingleThreadExecutor` | **I/O** — GEO parser |
| `gemma-core/.../loader/expression/geo/service/GeoBrowserServiceImpl.java` | `newSingleThreadExecutor` | **I/O** — NCBI eutils |
| `gemma-core/.../loader/expression/geo/singleCell/GeoSingleCellDetector.java` | `newFixedThreadPool` | **I/O** — GEO fetcher |
| `gemma-core/.../loader/util/fetcher/HttpFetcher.java` | `newSingleThreadExecutor` | **I/O** — HTTP download |
| `gemma-core/.../loader/util/fetcher/FtpFetcher.java` | `newSingleThreadExecutor` | **I/O** — FTP download |
| `gemma-core/.../loader/util/fetcher/FtpArchiveFetcher.java` (2 calls) | `newSingleThreadExecutor` | **I/O** — FTP + tar archive |
| `gemma-core/.../metrics/binder/jpa/Hibernate4QueryMetrics.java` | `newSingleThreadScheduledExecutor` | I/O-ish — metrics scrape |
| `gemma-rest/.../analytics/ga4/GoogleAnalytics4Provider.java` | `newSingleThreadScheduledExecutor` | **I/O** — GA4 REST flush |

**Test (9):** `AsyncFactoryTest`, `AsyncSingletonFactoryTest`, `AuditAdviceTest`,
`MexMatrixWriterTest`, `GeoTermReplacementTest`,
`GeoMexSingleCellDataLoaderConfigurerTest`, `ExecutingTaskTest`,
`StaticCacheKeyLockTest`, `GoogleAnalytics4ProviderTest`. All use the central helper.

### Adjacent territory (not rogue, but worth noting)

- **Raw `new ThreadPoolExecutor` / `new ScheduledThreadPoolExecutor` / `new ForkJoinPool`:**
  zero in production code. The only matches (in
  `gemma-core/.../metrics/binder/{GenericExecutorMetrics,ThreadPoolTaskExecutorMetrics}.java`)
  are consumers casting an injected `Executor` to `ThreadPoolExecutor` for
  micrometer instrumentation — they do not create pools.
- **Spring `ThreadPoolTaskExecutor`** (separate ecosystem — not affected by the
  central helper, runs through Spring's `TaskExecutor` abstraction):
  - XML beans: `taskExecutor` and `expressionDataFileTaskExecutor` in
    `gemma-core/src/main/resources/ubic/gemma/applicationContext-serviceBeans.xml`.
  - Java config: `OntologyConfig#ontologyTaskExecutor()` in
    `gemma-core/.../core/ontology/OntologyConfig.java`.
  - `OntologyServiceImpl` holds an `AsyncTaskExecutor` (defaulted to
    `SimpleAsyncTaskExecutor`).
  - Tests (`SimpleAsyncTaskExecutor` instances) in 7 places.

These Spring task-executors are a separate migration track — see section 4.

---

## 3. Use-case categorization

### I/O-bound (prime virtual-thread candidates)

These callsites spend their wall-clock time in network/disk syscalls. Each is a
small fixed pool today; under virtual threads they could submit one task per
remote resource without burning carrier threads.

1. `HttpFetcher` — single-threaded HTTP download wrapper around `URL.openStream()`.
2. `FtpFetcher` — single-threaded FTP fetch.
3. `FtpArchiveFetcher` (2 callsites) — FTP + tar extraction.
4. `GeoSingleCellDetector` — fixed pool fanning out fetches per GEO accession;
   `numberOfFetchThreads` is a user-tunable knob today.
5. `UnifiedOntologyUpdaterCli` — fixed pool fanning out ontology downloads.
6. `GeoFamilyParser` — single-thread reader.
7. `GeoBrowserServiceImpl` — single-thread NCBI eutils call.
8. `GoogleAnalytics4Provider` — single-thread scheduled flush of GA4 events
   over HTTPS.
9. `Hibernate4QueryMetrics` — scheduled scrape of metrics (mostly local but
   touches a metrics registry; low-priority migration).

### CPU-bound (keep platform threads)

- `ComBat#runParallel` — matrix algebra. Bounded `newCachedThreadPool` here is
  fine; should arguably be a fixed pool sized by `Runtime.getRuntime().availableProcessors()`,
  but **virtual threads would hurt** (no syscall parking, just compute hogging carriers).

### Mixed / context-dependent

- `AbstractCLI#getBatchTaskExecutor` — depends entirely on what each subcommand
  submits. Migration decision deferred per-subcommand.
- `TaskRunningServiceImpl` — Gemma's general-purpose background task pool;
  workloads include both expression-analysis math and GEO fetches. Don't migrate
  wholesale.
- `AbstractAsyncFactoryBean` — single-thread bean-construction helper. Trivial;
  no migration benefit either way.
- `SingleCellDataLoaderCli` `transformExecutor` — transform may be CPU-heavy;
  keep platform.

---

## 4. Spring 6 virtual-thread support pattern

Spring 6.1+ ships `org.springframework.core.task.VirtualThreadTaskExecutor` and
`SimpleAsyncTaskExecutor#setVirtualThreads(boolean)`. Both fail at runtime on
JDK 17 (the Spring class itself compiles on JDK 17, but invoking it tries to
spawn a virtual thread).

When Gemma moves to JDK 21, the recommended pattern for Spring beans is:

```java
@Bean
public TaskExecutor ontologyTaskExecutor() {
    if ( Runtime.version().feature() >= 21 ) {
        // I/O-bound: ontology downloads are network-heavy.
        return new VirtualThreadTaskExecutor( "gemma-ontology-loader-vthread-" );
    }
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize( corePoolSize );
    executor.setThreadNamePrefix( "gemma-ontology-loader-thread-" );
    return executor;
}
```

For the XML-defined `taskExecutor` and `expressionDataFileTaskExecutor` beans
in `applicationContext-serviceBeans.xml`, convert to Java config or use a
profile-gated bean override — XML can't do `Runtime.version()` checks.

---

## 5. Forward-prep factory added

Added one method to `ubic.gemma.core.util.concurrent.Executors`:

```java
public static ExecutorService newVirtualThreadPerTaskExecutorIfAvailable()
```

Behavior:

- **JDK 21+:** invokes `java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()`
  via reflection (so the source compiles cleanly on JDK 17), then wraps in the
  same security-context + log4j-thread-context delegating executors used by every
  other factory in this class.
- **JDK 17 (current):** falls back to `newCachedThreadPool()`, wrapped identically.

The reflective indirection means **no JDK toolchain change** is required to land
this factory. The class still compiles on JDK 17 (verified:
`mvn -pl gemma-core -am compile test-compile` clean; `javap` shows the new
public method on the compiled class).

**No callsite has been migrated to use this factory yet** — that's deliberate.
Migration is a separate decision per callsite and should wait until the platform
is on JDK 21 so the production behavior matches what tests + CI exercise.

---

## 6. Recommended migration order (when JDK 21 lands)

Priority by I/O-boundedness, blast radius, and revertability:

1. **`HttpFetcher`, `FtpFetcher`, `FtpArchiveFetcher`** — narrow surface,
   one-task-per-fetch pattern, obvious wins. Each is < 50 LOC; revert is one
   import swap.
2. **`UnifiedOntologyUpdaterCli`, `GeoSingleCellDetector`** — fanned-out fetch
   pools. Replacing the bounded `newFixedThreadPool(n, factory)` with
   `newVirtualThreadPerTaskExecutorIfAvailable()` lets concurrency match the
   number of remote endpoints rather than an artificial `numThreads` knob.
   **Watch for connection-pool back-pressure** — virtual threads remove the
   thread-count throttle, so the underlying HTTP client must enforce its own
   limit (Apache HttpClient `MaxConnPerRoute`, OkHttp `Dispatcher.maxRequests`).
3. **`GoogleAnalytics4Provider`** — scheduled GA4 flush. Low-risk if the GA4
   client doesn't pin on a `synchronized` block; verify before migrating.
4. **`GeoFamilyParser`, `GeoBrowserServiceImpl`** — single-threaded readers.
   Less compelling — single-thread doesn't benefit from virtual threads — but
   harmless to migrate for consistency.
5. **`Hibernate4QueryMetrics`** — last; scheduled metrics scrape, gain is
   marginal.

**Do NOT migrate:** `ComBat` (CPU-bound), `AsyncFactoryBean` family (no benefit),
the generic `taskExecutor` / `TaskRunningServiceImpl` (mixed workloads — would
need workload-aware routing, not a blanket switch).

---

## 7. Open questions

1. **Pinning audit.** Before migrating any callsite, run a JDK 21 build with
   `-Djdk.tracePinnedThreads=full` against the integration-test suite. Any
   `synchronized` block that owns the carrier thread during a syscall (FTP
   library internals are a classic offender) needs to move to
   `ReentrantLock`. Apache Commons Net's `FTPClient` is `synchronized`-heavy
   and may need replacement before `FtpFetcher` migrates.
2. **`ThreadFactory` semantics under virtual threads.** Callsites that pass a
   `SimpleThreadFactory` for naming
   (`gemma-unified-ontology-downloader-thread-`, etc.) won't see those names
   under virtual threads — `newVirtualThreadPerTaskExecutor` doesn't accept a
   `ThreadFactory`. Logging that relies on the thread-name prefix for
   correlation needs to switch to MDC values (Gemma already propagates MDC via
   `DelegatingThreadContextExecutorService`, so this is mostly a matter of
   updating log appender patterns).
3. **Spring XML task-executors.** `applicationContext-serviceBeans.xml`
   defines `taskExecutor` and `expressionDataFileTaskExecutor` as
   `ThreadPoolTaskExecutor` beans. To make these virtual-thread-capable we'd
   convert to Java `@Bean` config with a `Runtime.version()` switch, or use
   a Spring profile (`@Profile("jdk21")`).
4. **Pool sizing constants.** Several callsites consume tunable thread counts
   (`gemma.localTasks.corePoolSize`, `gemma.ontology.loader.corePoolSize`,
   `numberOfFetchThreads`). Under virtual threads these become moot — but the
   property surface is user-facing config. Decide whether to deprecate, keep
   as semaphore caps, or honor only when the platform-thread fallback is in use.
5. **Test fixtures.** All 9 test callsites use platform threads via
   `newFixedThreadPool` / `newSingleThreadExecutor`. These don't need to
   migrate — tests should keep deterministic platform-thread pools for
   reproducibility — but the test base should still run cleanly under JDK 21
   without raising deprecation warnings about thread-name conventions.
