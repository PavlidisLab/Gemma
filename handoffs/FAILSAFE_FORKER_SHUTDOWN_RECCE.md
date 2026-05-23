# Failsafe forker shutdown — recce

**Filed:** 2026-05-23. Branch: `agent-shutdown-forker-recce` off tip `b6296c613f`.
**Scope:** Investigation only. No code changes.
**Inputs:** `handoffs/SLOW_SWEEP_FINDINGS_2026_05_23.md`, `handoffs/RECCE_SLOW_SWEEP_AS_PERF_PROBE.md`, `handoffs/SLOW_SWEEP_INVENTORY_2026_05_23.md`.

## 1. Symptom

From `handoffs/SLOW_SWEEP_FINDINGS_2026_05_23.md`:

> "**`BUILD FAILURE` at the end** is a **JVM-shutdown forker artifact** (`maven-failsafe-plugin:verify` reported "timeout in the fork" at process tear-down), NOT a test hang. The failsafe test phase completed, all 506 tests ran. Probable cause: HDF5 / Lucene / Jena native resource not closing on JVM exit; resource-close audit is its own task."

Restated: Failsafe ran 506 IT methods to completion (1F+5E+29S, 28:41 wall clock). After the last test finished, the failsafe forker process did not exit within `forkedProcessExitTimeoutInSeconds` (default **30 s**, not overridden in `pom.xml` lines 1119-1154). Failsafe then reported `There was a timeout in the fork`. Something in the JVM kept the process alive past the close grace period.

`pom.xml`:1121-1154 — failsafe config has only `argLine`, `groups`, `excludedGroups`, `redirectTestOutputToFile`, `skip`. No `forkedProcessExitTimeoutInSeconds`, no `forkedProcessTimeoutInSeconds`, no `shutdown` policy. Defaults apply.

## 2. Inventory of native / non-daemon-thread holders

### `System.loadLibrary` / `System.load`
Grep result: **zero hits** in `gemma-core/src/main/java` and `gemma-rest/src/main/java`. Native libraries (HDF5) are loaded by the jhdf5 jar's own static initialiser, not directly by Gemma code.

### `Runtime.getRuntime().addShutdownHook`
Grep result: **zero hits** in main sources. The only shutdown hooks come from third-party libs (jhdf5, JCache provider, possibly Lucene FSLockFactory).

### `@Bean(destroyMethod=…)`
- `gemma-core/src/main/java/ubic/gemma/core/config/DataSourceConfig.java:147,181` — `dataSource` (HikariDataSource) → `close`. Hikari closes its housekeeping thread + connection pool cleanly.
- `gemma-core/src/main/java/ubic/gemma/core/ontology/OntologyConfig.java:184` — `ontologySearchService` (`JenaTextOntologySearchService`) → `close`. **Profile-gated to `!TEST`** (`OntologyConfig` line 31), so does not load in failsafe.
- `gemma-core/src/main/java/ubic/gemma/persistence/cache/EhcacheConfig.java:261` — `jCacheCacheManager` → `close`. JCache provider close releases pooled disk-store writers.

### `DisposableBean` / explicit shutdown
- `gemma-core/src/main/java/ubic/gemma/core/job/TaskRunningServiceImpl.java:48-79` — `executorService.shutdown()` + `awaitTermination(5, TimeUnit.MINUTES)` then `shutdownNow()`. **5-minute polite-wait per Spring context**. See section 3 — the headline suspect.
- `gemma-core/src/main/java/ubic/gemma/core/context/AbstractAsyncFactoryBean.java:115-136` — `executor.shutdown()` (no `awaitTermination`) + cancel pending futures. Spring then blocks on the executor thread implicitly because the executor was created with `Executors.newSingleThreadExecutor()` (non-daemon).
- `gemma-core/src/main/java/ubic/gemma/persistence/hibernate/HibernateSessionFactoryBean.java:131-133` — `sessionFactory.close()`. Releases HS6 / Lucene index writers.
- `gemma-core/src/main/java/ubic/gemma/persistence/initialization/BootstrappedDataSourceFactory.java` — `DisposableBean`, closes the boot-time DS.
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/service/GeoBrowserServiceImpl.java:65` — `DisposableBean`, presumably closes an HTTP client.

### Executors / Threads
- `gemma-core/src/main/java/ubic/gemma/core/util/concurrent/SimpleThreadFactory.java:22-24` — `new Thread( runnable, threadNamePrefix + threadId )`. **Does NOT call `setDaemon(true)`**. Every executor built on top of this factory creates non-daemon threads (whose direct lineage is `java.util.concurrent.Executors`-default factory, also non-daemon — Gemma's wrappers do not override). A leaked executor → JVM stays alive past `main` exit.
- `gemma-core/src/main/java/ubic/gemma/core/util/concurrent/Executors.java:26-87` — wrappers that delegate to `java.util.concurrent.Executors` defaults; no custom thread factory; no daemon flag override.
- `gemma-core/src/main/java/ubic/gemma/core/metrics/binder/jpa/Hibernate4QueryMetrics.java:28` — `Executors.newSingleThreadScheduledExecutor()`. Profile-gated to `metrics`, not in test path.
- `gemma-core/src/main/java/ubic/gemma/core/context/AbstractAsyncFactoryBean.java:57` — `Executors.newSingleThreadExecutor()`. Shut down on Spring context dispose.
- `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/batcheffects/ComBat.java:410` — `Executors.newCachedThreadPool()`. **Local to a method; not explicitly shutdown** — relies on caller calling `service.shutdown()`. ComBat IT (`ExpressionExperimentBatchCorrectionServiceTest`, network-bound) is in the failsafe slow set.
- `gemma-core/src/main/java/ubic/gemma/core/util/math/linalg/SingularValueDecomposition.java:163` — `Executors.newSingleThreadExecutor()`. **Local to a method; not explicitly shutdown**. Hit by `ExpressionDataSVDTest`, `SVDServiceImplTest`, `SampleCoexpressionAnalysisServiceTest` (all in failsafe slow set).
- `gemma-core/src/main/java/ubic/gemma/core/config/ServiceBeansConfig.java:57-63` — `taskExecutor` (`ThreadPoolTaskExecutor`). No `destroyMethod`, no `setDaemon(true)`. Spring's own `DisposableBean` semantics close it on context teardown, but if a submitted task is still running, Spring waits.
- `gemma-core/src/main/java/ubic/gemma/core/config/ServiceBeansConfig.java:82-86` — `expressionDataFileTaskExecutor`. VT-per-task on JDK 21+; VTs are daemon-by-default, so this one is safe.

### JMX / Micrometer registries
- `gemma-core/src/main/java/ubic/gemma/core/config/MetricsConfig.java:57-63` — `JmxMeterRegistry`. **Profile-gated to `metrics`**, not in failsafe test path.

### External processes (`ProcessBuilder` / `Runtime.exec`)
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/AffyPowerToolsProbesetSummarize.java:624`
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/transform/AbstractScriptBasedTransformation.java:34`
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/transform/AbstractCellRangerBasedTransformation.java:96,100`
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/transform/AbstractPythonScriptBasedTransformation.java:46` — `Runtime.exec`. If the child process is alive at JVM exit, the JVM normally closes its pipes and exits; this is rarely a fork-exit blocker, but is worth noting.

### HDF5
- `gemma-core/src/main/java/ubic/gemma/core/loader/util/hdf5/H5File.java`, `H5Group.java`, `H5Dataset.java`, `H5Attribute.java`, `H5Type.java` — all are per-handle `AutoCloseable`s; no global library init/shutdown call (no `H5.H5open()` / `H5.H5close()` in main code). The jhdf5 jar installs its own `Runtime.getRuntime().addShutdownHook` internally to call `H5close()` and delete the unpacked temp library file. That hook is single-threaded and acquires the native library mutex; if any test left a JNI call mid-flight (unlikely in our well-formed try-with-resources patterns) or has spawned worker threads that hold H5 handles, the hook blocks.
- `pom.xml`:1098-1105 sets `DYLD_LIBRARY_PATH` / `LD_LIBRARY_PATH` so the JNI shim resolves its `libhdf5.200.dylib` dependency. macOS-specific behaviour: if `libhdf5_java.dylib` was unpacked to `/var/folders/.../jhdf5*` the shutdown hook attempts a `delete()` which can race with the OS holding the file open.

### Jena TDB
- `gemma-core/src/main/java/ubic/gemma/core/ontology/basecode/jena/TdbOntologyService.java:88-91, 114-126` — `TDBFactory.createDataset(...)` + `TDBFactory.release(dataset)` in `close()`. The unified TDB bean (`OntologyConfig.unifiedOntologyService`) is profile-gated to `!TEST`. **TDB is not in the test path** unless an individual test bypasses `TestOntologyConfig` — none in the inventory does.

### Lucene (Hibernate Search 6/7)
- `gemma-core/src/main/java/ubic/gemma/persistence/hibernate/HibernateConfig.java:227-245` — HS7 backend is `lucene`, `local-filesystem`, directory root coerced to `${java.io.tmpdir}/gemmaData/searchIndices`. HS owns the `IndexWriter` lifecycle and closes it through `SessionFactory.close()`. The `HibernateSessionFactoryBean.destroy()` path (`HibernateSessionFactoryBean.java:131-133`) calls `sessionFactory.close()` — that should drain HS7's Lucene writers cleanly.

## 3. Ranked suspects

### #1 — `TaskRunningServiceImpl.destroy()` 5-minute polite wait  (HIGH confidence)

`gemma-core/src/main/java/ubic/gemma/core/job/TaskRunningServiceImpl.java:69-79`:

```java
@Override
public void destroy() throws Exception {
    executorService.shutdown();
    if ( !executorService.isTerminated() ) {
        log.warn( "There are still running tasks, will wait at most 5 minutes before shutting them down." );
    }
    if ( !executorService.awaitTermination( 5, TimeUnit.MINUTES ) ) {
        log.info( "TaskRunningService executor was still running after 5 minutes, interrupting pending tasks..." );
        executorService.shutdownNow();
    }
}
```

The `awaitTermination(5, TimeUnit.MINUTES)` blocks for **up to 5 minutes per Spring context** if any submitted future is still alive (and uninterruptible by `shutdown()`'s "no new tasks" — only `shutdownNow()` interrupts). With `forkedProcessExitTimeoutInSeconds=30`, the forker kills the fork at 30 s and reports the timeout — which matches the symptom exactly.

Failsafe contexts that wire `TaskRunningServiceImpl` (`@Component("taskRunningService")`) and exercise it: any test that submits a `Task` via the running service. Production callers include the GEO loader and the data-file generator; an IT that triggers either through the service and then doesn't await its completion would leave a pending future.

Why this is the strongest candidate:
1. Numerical match — 5 min >> 30 s default, so the forker definitely kills the fork.
2. Affects only failsafe (failsafe contexts wire the full bean graph; surefire unit contexts usually don't).
3. Doesn't depend on native libs / file locks — pure JVM threadpool.
4. The 5-minute window is precisely the kind of "polite-wait" that bites Maven forkers.

### #2 — Non-daemon executor threads from `Executors.newSingleThreadExecutor()` in IT-only utility code  (MEDIUM confidence)

`SingularValueDecomposition.java:163` and `ComBat.java:410` create local `ExecutorService` instances and rely on a `shutdown()` happening before method exit. If any code path (especially failure / early-return) misses the `shutdown()`, the executor's worker thread is a non-daemon JVM thread that survives the test → blocks JVM exit. Hit by `ExpressionDataSVDTest`, `SVDServiceImplTest`, `SampleCoexpressionAnalysisServiceTest`, `ExpressionExperimentBatchCorrectionServiceTest`.

`SimpleThreadFactory.java:22-24` also fails to set the daemon flag, but is not used by these callsites (they go through stock `java.util.concurrent.Executors` defaults, which are also non-daemon).

### #3 — HDF5 shutdown hook race  (MEDIUM-LOW confidence)

The slowest individual test is `MexSingleCellDataLoaderTest` (213 s, heavy MEX/HDF5 work). jhdf5's internal shutdown hook serialises on the H5 library mutex. If any H5 handle (file / group / dataset) is still open at JVM exit (e.g., a leaked `H5File` from a test that threw mid-iteration without `close()`), `H5close()` will block waiting for the handle to be released — there's nothing to release it because the holding thread has gone away. Probability: lower than #1 because our `H5*` types are all `AutoCloseable` and most callsites use try-with-resources. Worth ruling in/out via thread dump.

### #4 — `ScheduledExecutorService` from `AbstractAsyncFactoryBean` with a stuck `createObject()`  (LOW confidence)

If a singleton factory bean is mid-`createObject()` (e.g., an ontology loader stuck in network retry), `destroy()` calls `executor.shutdown()` (NOT `shutdownNow()`) and then `f.cancel(true)` on pending futures. The cancel sends an interrupt; if the in-flight `createObject()` is doing uninterruptible blocking I/O (synchronous HTTP read, native ontology parse), the worker thread doesn't unwind. Since the executor's worker is non-daemon, JVM exit blocks. Failsafe context teardown would surface this as a long shutdown wait. This pattern is the same family as the `AbstractAsyncFactoryBean` + spring-test 6.2 init trap noted in `CLAUDE.md`, but on the destruction side.

### #5 — `ThreadPoolTaskExecutor` (`taskExecutor`) with un-awaited Spring `@Async` work  (LOW confidence)

Spring's `ThreadPoolTaskExecutor.destroy()` calls `shutdown()` and waits for the configured `awaitTerminationSeconds` (default 0 → immediate shutdown). Not configured in `ServiceBeansConfig`, so this is harmless if Spring's defaults stand. Listed for completeness.

## 4. Repro recipe (DO NOT RUN; document only)

The DataUpdater agent holds gemdtest and another agent holds gemma-rest surefire. Do not run any `mvn` command from this worktree. When this work picks back up:

```bash
# 1. Slow-sweep run with relaxed forker timeout + a second-shell jstack trigger.
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# In shell A: run slow sweep with a long forker timeout so the fork survives long enough to dump.
mvn -pl gemma-core verify \
    -DexcludedGroups=network \
    -Dit.test='!DatasetCombinerTest' \
    -Dfailsafe.timeout=900 \
    -Dforked.process.exit.timeout.seconds=300 \
    -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) \
    -Dgemma.hibernate.hbm2ddl.auto=create 2>&1 | tee /tmp/forker-recce.log

# In shell B: when shell A reports "Tests run: ... 506 / Failures: 1 / Errors: 5 / Skipped: 29",
# the test phase is done. Locate the failsafe fork PID and dump its threads:
JPID=$(jcmd | awk '/SurefireBooter/ {print $1}')
jcmd "$JPID" Thread.print > /tmp/forker-thread-dump.txt
# Optionally also: jcmd "$JPID" GC.heap_info  /  VM.native_memory summary
```

Look for non-daemon threads still alive: anything named `gemma-local-tasks-thread-*`, `pool-N-thread-M`, or jhdf5 native-resource holders. The thread state + stack pinpoints which suspect from §3 is actually responsible.

To rule TRS in/out cheaply without running the whole sweep, an even more targeted probe is to add `-Dlogger.ubic.gemma.core.job.TaskRunningServiceImpl=DEBUG` to the slow-sweep argline and grep the resulting log for `There are still running tasks, will wait at most 5 minutes`. If that line appears, #1 is confirmed.

## 5. Fix shapes (for the implementing agent — sketch only)

### If #1 (TaskRunningServiceImpl polite wait)

Shorten the wait. Production cares about graceful task shutdown; tests don't. Two shapes:

a. **Make the polite-wait test-aware.** Inject a `@Value("${gemma.tasks.shutdownTimeoutSeconds:300}")` field; in `BaseDatabaseTest5` / test properties, set it to 5 s. Production keeps 5 min.

b. **Use `shutdownNow()` first on a configurable flag.** Same shape but a boolean `shutdownNowInsteadOfWait` gated by a test property. Less polite but the test path doesn't care.

Either way, also lower the default forker exit timeout for safety: add `<forkedProcessExitTimeoutInSeconds>120</forkedProcessExitTimeoutInSeconds>` to the failsafe plugin config in `pom.xml` so even if some other shutdown laggard creeps in, the forker waits longer than 30 s.

### If #2 (leaked SVD / ComBat executor)

Wrap the local `ExecutorService` in try/finally so `shutdown()` (or `shutdownNow()`) is always called on the exit path, OR pass an injected executor from the Spring graph (the `taskExecutor` bean) so its lifecycle is owned by the container. The latter is the cleaner shape — the SVD code shouldn't be spawning ad-hoc threadpools.

Independently, fix `SimpleThreadFactory.newThread` to call `t.setDaemon( true )` on the returned `Thread`. Every executor in Gemma is bean-managed and shutdown by the container; daemon threads are the right default for that posture. Any callsite that genuinely needs a non-daemon thread (which is none in the current main code) should construct its own `ThreadFactory`.

### If #3 (HDF5 handle leak)

Audit the `MexSingleCellDataLoader` / `AnnDataSingleCellDataLoader` for any `H5File`-derived handle (`H5Group`, `H5Dataset`, `H5Type`, `H5Attribute`) opened outside try-with-resources. Specifically `gemma-core/src/main/java/ubic/gemma/core/loader/util/anndata/*.java` callers — those wrap the H5 handles in higher-level types and forward `close()` ownership. A missed `close()` on the wrapper leaks the H5 handle and stalls jhdf5's shutdown hook.

## 6. Cross-references

- `handoffs/SLOW_SWEEP_FINDINGS_2026_05_23.md` — origin of this recce (the forker-timeout observation).
- `handoffs/RECCE_SLOW_SWEEP_AS_PERF_PROBE.md` — the perf-probe lens for slow-tagged tests.
- `handoffs/SLOW_SWEEP_INVENTORY_2026_05_23.md` — 77 slow-tagged classes that ran in the affected sweep.
- `CLAUDE.md` "Pitfalls" — `AbstractAsyncFactoryBean` + spring-test 6.2 init trap (related shape on the *init* side; this recce is the symmetric *destroy* side).
- `pom.xml`:1119-1154 — failsafe plugin config (no forker exit timeout override; defaults apply).

## 7. Sub-findings worth elevating

- **`SimpleThreadFactory` doesn't set daemon flag.** Two-line fix; defensive; reduces blast radius for every future executor-leak pitfall. Worth its own ticket regardless of which §3 suspect is the actual forker-timeout culprit.
- **`SingularValueDecomposition` and `ComBat` spawn ad-hoc executor pools** instead of using the container-managed `taskExecutor`. Architectural smell; the perf-probe wave already touched both. Worth folding into a future pass.
- **No `forkedProcessExitTimeoutInSeconds` override** in the failsafe plugin config. 30 s default is too tight for a forked JVM that's holding HS7 + HDF5 + JCache. Setting it to 120 s would be a cheap belt-and-braces independent of fixing the actual culprit.
