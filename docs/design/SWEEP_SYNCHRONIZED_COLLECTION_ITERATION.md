# Synchronized-collection iteration sweep (post 15006ca9c0)

## Summary

- Total callsites scanned: 4 (across gemma-core, gemma-cli, gemma-rest main java)
- Already safe: 2
- Already fixed pre-sweep: 1 (the bug in commit 15006ca9c0)
- Fixed in this branch: 1
- Surfaced (needs orchestrator review): 0

Sweep also covered the secondary patterns called out in the brief
(`parallelMapRange`, `parallelStream`, `CompletableFuture`,
`ExecutorService.submit`, `ForkJoinPool`) in the three main-Java trees.
`parallelMapRange` appears only at the already-fixed
`AbstractMexSingleCellDataLoaderConfigurer` site;
`parallelStream` is absent from production main-java in these modules;
the executor-driven accumulator paths that exist (e.g. fetcher
download orchestrations) either use `AtomicReference` / per-task
returns or are dispatched-and-joined before any iteration of the
accumulator.

## Findings (per callsite)

### gemma-core/src/main/java/ubic/gemma/core/util/locking/FileLockManagerImpl.java:30  --  FileLockManagerImpl.getAllLockInfos
- Code shape: `private static final Map<Path,ReadWriteFileLock> fileLocks = Collections.synchronizedMap(new WeakHashMap<>())` iterated via `fileLocks.entrySet().stream()` in `getAllLockInfos()`.
- Classification: NEEDS FIX (low risk)
- Reasoning: Concurrent writers definitely exist — `acquirePathLock` / `tryAcquirePathLock` call `fileLocks.computeIfAbsent(...)` from arbitrary request threads. The reader iterated the synchronized-map's `entrySet` view unguarded, which the `Collections.synchronizedMap` javadoc explicitly forbids. CME is possible whenever `getAllLockInfos()` (admin / JMX / test endpoint) runs concurrently with any lock acquisition. Call frequency for the reader is low (no production hot-path callers found), but the textbook snapshot-under-lock fix is cheap and matches the 15006ca9c0 precedent. The sibling `getAllLockInfosByWalking` does only single-point reads (`containsKey` / `get`) on the synchronized wrapper, which are individually atomic — no iteration of a view, so no fix needed there.
- Fix: commit `0e929d0749` (snapshot `entrySet` inside `synchronized(fileLocks){...}` before streaming).

### gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/AbstractMexSingleCellDataLoaderConfigurer.java:195  --  AbstractMexSingleCellDataLoaderConfigurer.cleanupFiltered10xMexData
- Code shape: `Collections.synchronizedList(new ArrayList<>())` accumulator mutated by `parallelMapRange` workers via `list.add(...)` in `filter10xSample`'s `finally`; iterated in `cleanupFiltered10xMexData`.
- Classification: ALREADY FIXED (pre-sweep, commit 15006ca9c0)
- Reasoning: This is the production CME bug the sweep was prompted by. The snapshot-under-lock fix lives at lines 288-308 today and the javadoc on `cleanupFiltered10xMexData` documents why.
- Fix: 15006ca9c0 (existing).

### gemma-cli/src/main/java/ubic/gemma/cli/batch/TextBatchTaskSummaryWriter.java:17  --  TextBatchTaskSummaryWriter.writeSummary
- Code shape: `Collections.synchronizedList(new ArrayList<>())` mutated by `write()` from many CLI worker threads; iterated by `writeSummary()` (three `.stream().filter(...).collect(...)` passes) called from `close()`.
- Classification: SAFE
- Reasoning: All writers go through `BatchTaskProgressReporter.addBatchProcessingResult`, which calls `synchronized(summaryWriter){ summaryWriter.write(result); }`. By the documented lifecycle, `close()` runs only after batch task workers have finished (CLI tear-down), so there is a happens-before edge between every `write()` and the iteration in `writeSummary()`. The `synchronizedList` is defensive rather than load-bearing, and no unguarded reader runs while writers are still active. A defensive snapshot would be cheap, but I'm avoiding gold-plating — no functional bug.
- Fix: none.

### gemma-rest/src/main/java/ubic/gemma/rest/providers/AnalyticsRequestEventListener.java:21  --  AnalyticsRequestEventListener.onEvent
- Code shape: `Collections.synchronizedMap(new IdentityHashMap<>())` mutated by `put` (on START) and `remove` (on FINISHED). Never iterated.
- Classification: SAFE
- Reasoning: Only single-point mutations (`put`, `remove`) on the synchronized wrapper — these are individually atomic. No view iteration, no compound check-then-act, no cleanup pass. There is no CME shape here.
- Fix: none.

## Notes / surprises

- No same-antipattern occurrences in test fixtures (the secondary
  worry from the brief). The `Collections.synchronized*` callsite
  count in `src/test` across the three modules is small and none of
  them mix executor writers with unguarded iteration.
- The two SAFE classifications above both rely on EXTERNAL
  synchronization (`BatchTaskProgressReporter`'s wrapper /
  request-lifecycle for the listener). If either of those external
  contracts changes — e.g. if `TextBatchTaskSummaryWriter.close()`
  starts being called from a thread that races with active workers —
  the CME risk reappears. Worth a comment if/when those classes
  evolve.
