# On-disk cascade recce — the diff-ex artifacts a curation commit leaves behind

A structural curation commit (and therefore a sign, and a snapshot restore) drops the dataset's
differential expression analyses through `differentialExpressionAnalysisService.remove(...)`, which
deletes database rows only. `DifferentialExpressionAnalyzerServiceImpl.deleteAnalysis(...)` is the
method that also removes the on-disk artifacts. This recce establishes which call sites are involved,
which artifacts actually exist, and whether the two methods can be joined.

**Verdict: PART 1 ONLY — the seam is not clean, and the blocker is not the one that was expected.**
The circular injection is real but has three in-repo precedents for breaking it. The blocker that
stops the change is `@Transactional(propagation = Propagation.NEVER)`, which sits on **both**
`DifferentialExpressionAnalyzerServiceImpl` and `ExpressionDataFileServiceImpl`. The commit path is
`@Transactional`, so it cannot call either one — in any direction, at any point in the transaction,
including from an after-commit callback. Options and a recommendation are in §7.

Measured on the worktree at `8e6be807b0` (branch `agent-ondisk-cascade`, off `phase2-acl-migrate`).

---

## 1. The call sites that remove a diff-ex analysis on the curation commit / sign path

All four write routes funnel into one private method and from there into one service call.

| Step | File:line | What it does |
|---|---|---|
| `PUT /datasets/{id}/curation` | `gemma-rest/.../DatasetsWebService.java:3143` | → `doCommitCuration(datasetArg, body, false, force, false, null)` |
| `POST /datasets/{id}/curation/sign` | `DatasetsWebService.java:2598` | → `doCommitCuration(datasetArg, doc, dryRun, false, true, signer)` |
| `POST .../annotation-sets/{id}/restore` | `DatasetsWebService.java:2153` | → `doCommitCuration(datasetArg, snapshot, dryRun, force, false, null)` |
| `POST /datasets/{id}/curation/preview` | `DatasetsWebService.java:3176` | dry run; writes nothing |
| — | `DatasetsWebService.java:3392` | `doCommitCuration` calls `expressionExperimentService.commitCuration( ee, request, dryRun )` |
| — | `ExpressionExperimentServiceImpl.java:2341` | `commitCuration`, `@Transactional` |
| — | `ExpressionExperimentServiceImpl.java:2472`, `:2488` | design section: `self.applyDesignChange( ee, edvo1 )`, and a second pass for deferred new-FV assignments |
| — | `ExpressionExperimentServiceImpl.java:790` | `applyDesignChange`, `@Transactional` |
| **the cascade** | **`ExpressionExperimentServiceImpl.java:889-905`** | step 2 — for each analysis id the preflight enumerated: `differentialExpressionAnalysisService.remove( a )` |

The set being removed is decided in one place, `previewDesignChange`
(`ExpressionExperimentServiceImpl.java:722-752`): any structural change or math-changing edit to a kept
factor value enumerates **every** analysis `findByExperiment( ee, true )` returns, subsets included.
Step 2 executes that list rather than re-deriving it.

There is a second, subordinate removal path on the same route:

- `ExpressionExperimentServiceImpl.java:925-934` — step 4 removes dropped factors through
  `experimentalFactorService.remove( ef )`, which calls
  `differentialExpressionAnalysisService.removeForExperimentalFactor(...)`
  (`ExperimentalFactorServiceImpl.java:60`, and `:85` for the batch form). Step 2 has already removed
  everything this could reach on a structural commit, so it is a no-op there — but it is a second
  database-only removal on the path and would orphan files if it ever fired first.

The standalone `PUT /datasets/{id}/design` route (`DatasetsWebService.java:8243` →
`DatasetArgService.java:381-393` → the same `applyDesignChange`) reaches step 2 as well.

`ExpressionExperimentServiceImpl` and `DatasetArgService` are the only callers of `applyDesignChange`;
`doCommitCuration` is the only caller of `commitCuration`. `DatasetsWebService` declares no
`@Transactional` methods.

## 2. What `deleteAnalysis` does that `remove` does not

`DifferentialExpressionAnalysisServiceImpl.remove(...)` (`:109-126`) removes the meta-analyses that use
the analyzed experiment, then `super.remove(toDelete)`. Nothing else — no filesystem, no cache.

`DifferentialExpressionAnalyzerServiceImpl.deleteAnalysis(...)` (`:124-137`) calls the same `remove`,
then three more things:

| # | Called from | Artifact | Resolves to |
|---|---|---|---|
| 1 | `:134` → `deleteStatistics` (`:330-343`) | `<shortName>.an<analysisId>.pvalues.dist.txt` | `${gemma.analysis.dir}/diff/diffExStatDistributions/<shortName>/` — built by `DifferentialExpressionFileUtils.getBaseDifferentialDirectory` (`:32-47`); `gemma.analysis.dir=${gemma.appdata.home}/analysis` (`gemma-core/src/main/resources/default.properties:16`) |
| 2 | `:135` → `expressionDataFileService.deleteDiffExArchiveFile` (`ExpressionDataFileServiceImpl.java:256-258`) | `<eeShortName>_diffExpAnalysis_<analysisId>.zip` — name from `ExpressionDataFileUtils.getDiffExArchiveFileName` (`:91-98`) | `${gemma.appdata.home}/dataFiles/` (`ExpressionDataFileServiceImpl.java:132`) |
| 3 | `:136` → `deleteResultSetTsvCaches` (`:432-437`) → `expressionDataFileService.deleteDifferentialExpressionResultSetTsvFile` (`ExpressionDataFileServiceImpl.java:260-264`) | `resultSets/resultSet_<resultSetId>.tsv.gz`, one per result set | `${gemma.appdata.home}/dataFiles/resultSets/` |
| 3b | `:435` | `differentialExpressionResultCache.clearResultSetCountsCache( rsId )` (`DifferentialExpressionResultCacheImpl.java:162`) | in-memory, not a file |

The result-set ids come from `collectResultSetIds` (`:415-427`), which thaws the analysis **before** the
database delete because the entity is gone afterwards.

### Artifact 1 is vestigial — nothing writes it any more

`grep` over `gemma-core`, `gemma-rest` and `gemma-cli` finds exactly one producer of a
`PVALUE_DIST_SUFFIX` path, and it is `deleteStatistics` itself; `prepareDirectoryForDistributions`
(`:561-578`) has no other caller. The p-value distribution now lives in the database as
`ExpressionAnalysisResultSet.pvalueDistribution`, written by `addPvalueDistribution` (`:345-357`).
`git log -S writeDistributions` last touches the file writer in `c50121b0a1` /`cc37e75c37`.

Consequence: **`deleteStatistics` deletes a file that no current run produces, and
`prepareDirectoryForDistributions` calls `FileTools.createDir` on the way in — so invoking it creates
an empty directory rather than removing anything.** Only artifacts 2 and 3 accumulate today.

### What actually orphans, per cascaded analysis

- one `${gemma.appdata.home}/dataFiles/<eeShortName>_diffExpAnalysis_<id>.zip`
- N `${gemma.appdata.home}/dataFiles/resultSets/resultSet_<rsId>.tsv.gz`, N = result sets in the analysis
- N stale entries in `DiffExResultSetCountsCache` (`EhcacheConfig.java:127` — 5000 entries, 30-minute
  TTL), read by `DifferentialExpressionAnalysisReadServiceImpl.java:208`. Result-set ids are not reused,
  so these are unreachable entries under a bounded, TTL'd cache — occupancy, not a wrong answer.

Both files are regenerable caches: `writeOrLocateDiffExAnalysisArchiveFile`
(`ExpressionDataFileServiceImpl.java:1294`) and `writeOrLocateDifferentialExpressionResultSetTsvFile`
(`:1241`) rebuild them from the database on first request. The orphans are dead weight on the volume,
not lost data.

## 3. The circular dependency

```
DifferentialExpressionAnalyzerServiceImpl.java:76   @Autowired ExpressionExperimentService expressionExperimentService
                                                    └── used once, at :228, for expressionExperimentService.isRNASeq( ee )
ExpressionExperimentServiceImpl.java                would need @Autowired DifferentialExpressionAnalyzerService
```

That is the whole cycle: one field, one use, in `runDifferentialExpressionAnalyses`. It is not the
reason the change cannot be made.

## 4. The blocker that does stop it — `Propagation.NEVER`

```java
// DifferentialExpressionAnalyzerServiceImpl.java:61-62
@Component
@Transactional(propagation = Propagation.NEVER)

// ExpressionDataFileServiceImpl.java:89-91
@Service
@Transactional(propagation = Propagation.NEVER)
```

`@EnableTransactionManagement(order = 3)` is active (`HibernateConfig.java:99`), so these are enforced
at the proxy. `commitCuration` and `applyDesignChange` are both `@Transactional` (REQUIRED). A call
from either into `deleteAnalysis` throws `IllegalTransactionStateException: Existing transaction found
for transaction marked with propagation 'never'`.

The analyzer's own code already reasons about this — `collectResultSetIds` (`:415-420`) thaws the
analysis explicitly "because … this service runs at `Propagation.NEVER`, so the lazy `resultSets`
collection cannot be initialized on demand from here."

**An after-commit `TransactionSynchronization` does not escape it.** Spring invokes `afterCommit` and
`afterCompletion` from inside `AbstractPlatformTransactionManager.processCommit`, before
`cleanupAfterCompletion` unbinds the Hibernate `SessionHolder` and clears
`TransactionSynchronizationManager`. `isExistingTransaction(...)` is therefore still true when the
callback runs, and `NEVER` still throws.

### This is a layer, not an accident

Eleven beans carry `propagation = Propagation.NEVER`: `PreprocessorServiceImpl`,
`PreprocessorHelperServiceImpl`, `SplitExperimentServiceImpl`, `ExpressionExperimentReportServiceImpl`,
`DifferentialExpressionAnalyzerServiceImpl`, `OutlierFlaggingServiceImpl`,
`ExpressionDataDeleterServiceImpl`, `ExpressionDataFileServiceImpl`, `DataUpdaterImpl`,
`ArrayDesignProbeMapperServiceImpl`, `CellXGeneDataLoaderServiceImpl`.

Every consumer of those eleven is a task impl, a CLI, a Quartz job, the REST layer, or another
NEVER bean. **No `@Transactional` service under `persistence/service/` autowires any of them.**
Checked: `ArrayDesignAnnotationServiceImpl`, `SimpleExpressionDataLoaderServiceImpl` and
`GeoServiceImpl` are bare `@Component`s with no class-level `@Transactional`;
`BatchInfoRepopulationJob` is a Quartz bean.

`PreprocessorServiceImpl` is the shape of a bean that legitimately holds both sides: it is NEVER
itself (`:41`) and autowires `DifferentialExpressionAnalyzerService` (`:47`) and
`ExpressionExperimentService` (`:53`).

So the request — reach `deleteAnalysis` from inside `commitCuration` — is a call from the
transactional tier up into the orchestration tier, which nothing in the codebase currently does.

## 5. Prior art for breaking a Spring cycle (adopt / adapt survey)

| Mechanism | Present? | Where |
|---|---|---|
| `@Lazy @Autowired` | **yes, 3 precedents** | `ExpressionExperimentServiceImpl.java:157-165` (`self`, the proxy self-reference `commitCuration` uses for `applyDesignChange`, annotated `@Lazy` "avoids a circular-init failure"); `ExpressionExperimentReadServiceImpl.java:117-130` (three fields); `RelationshipPersister.java:87-100` (`@Lazy PersisterHelperImpl dispatcher`). `SuppressArchUnit.java:42-44` documents the pattern as a sanctioned category. |
| `ObjectProvider` | only in Spring Security config | `MethodSecurityConfig.java:160-186`. Never used to break a service cycle. |
| `ApplicationContext` lookup | not used for this | — |
| Events / listener seam | **declared, unused** | `AuditedEvent` (`gemma-core/.../security/audit/AuditedEvent.java`) is an `ApplicationEvent` published by `AuditedAspect` on every successful `@Audited` method, and its javadoc names "cache eviction" and "analysis re-run triggers" as intended subscribers, recommending `@TransactionalEventListener`. **There is not one `@TransactionalEventListener`, `@EventListener` on it, `TransactionSynchronizationManager` or `registerSynchronization` call anywhere in `gemma-core`, `gemma-rest` or `gemma-cli` `src/main`.** The seam exists as a publisher with no subscribers. |

`@Lazy` would break the cycle in §3 in one line. It does nothing about §4.

## 6. Does anything already clean these files up?

No.

- **No scheduled sweep.** The five `@Scheduled` methods in the codebase are `JobReconciler` (pipeline
  jobs), `HomeStatsRefresher`, `DiffExGeneWarmupService`, `ScheduledSearchReindexer` and
  `SubmittedTasksMaintenance`. None touches `dataFiles/`.
- **No maintenance CLI for orphaned analysis files.** `DeleteDiffExCli` deletes analyses through
  `deas.removeForExperiment( ee, true )` (`:44`) — database only, so it *creates* orphans rather than
  clearing them.
- **`ExpressionDataFileService.deleteAllAnalysisFiles( ee )`** (`ExpressionDataFileServiceImpl.java:236-253`)
  is the closest existing thing: it iterates the experiment's analyses and deletes each archive file,
  plus the coexpression file. It is called from `PreprocessorServiceImpl.java:176`. It enumerates from
  the database, so it can only reach files whose analysis row still exists — useless after the fact —
  and it does not cover the `resultSets/*.tsv.gz` caches.
- **`deleteAllFiles( ee )`** (`:135-153`) is used by `SplitExperimentServiceImpl.java:336`; same
  enumerate-from-database limitation, and it is whole-dataset, far wider than this cascade.

Nothing existing is merely disabled or narrow here. There is no cleanup to re-enable.

## 7. Options, and the recommendation

### Option A — `@Lazy` inject the analyzer into `ExpressionExperimentServiceImpl` and call `deleteAnalysis` from step 2
**Does not work.** Fixes §3, runs straight into §4. Would throw `IllegalTransactionStateException` on
every structural commit. A mocked unit test (`ExpressionExperimentServiceImplTest` wires the impl
directly with no proxy and no transaction manager) would pass green while production 500s — the exact
failure mode the "test the transaction boundary, not around it" rule names.

### Option B — after-commit `TransactionSynchronization` inside `applyDesignChange`
**Does not work**, for the reason in §4: the Hibernate resources are still bound when `afterCommit`
and `afterCompletion` run.

### Option C — delete the analyses through the analyzer *before* the commit, from `doCommitCuration`
The REST layer already runs the preflight before committing (`DatasetsWebService.java:3269`), so it
knows the analysis ids, and it is outside any transaction, so `NEVER` is satisfied. But it deletes the
analyses for a commit that has not happened yet: any later section of `commitCuration` — tags, sample
characteristics, a shortName collision, an optimistic-lock failure — rolls the design change back and
the analyses are already gone. **This widens what gets deleted and should not be done.**

### Option D — carry the artifact identity out of the transaction and clean up at the REST boundary
Capture, inside step 2 while the entities are attached, the archive filename and the result-set ids;
surface them on `DesignApplyOutcome` and `CurationCommitResult`; delete after `commitCuration` /
`applyDesignChange` returns. Correct ordering, and complete — REST is the only entry point to both
methods. Costs: a new carrier type, a new field on two value objects, plumbing through the second
design pass in `commitCuration`, and a new file-cleanup entry point taking ids rather than entities.
Note that the filename cannot be computed after the fact: `formatExperimentAnalyzedFilename`
(`ExpressionDataFileUtils.java:116-131`) calls `Hibernate.unproxy` on `experimentAnalyzed` and needs an
open session.

### Option E (recommended) — narrow the propagation on the pure-filesystem deletes, then route through the existing method
Three edits:

1. `ExpressionDataFileServiceImpl.deleteDiffExArchiveFile` and
   `deleteDifferentialExpressionResultSetTsvFile` — add method-level
   `@Transactional(propagation = Propagation.SUPPORTS)`, overriding the class-level `NEVER`. Both are
   pure filesystem (`deleteAndLog(dataDir.resolve(...))`); neither touches the database.
2. `DifferentialExpressionAnalyzerServiceImpl` — hoist the artifact trio out of `deleteAnalysis` into a
   public `deleteAnalysisArtifacts(ee, analysis, resultSetIds)` at `Propagation.SUPPORTS`;
   `deleteAnalysis` and `deleteAnalyses` keep calling it, so the artifact set stays defined in one
   place.
3. `ExpressionExperimentServiceImpl` — `@Lazy @Autowired DifferentialExpressionAnalyzerService`
   (precedent: the `self` field at `:157-165` in the same class); in step 2, collect the result-set ids
   before `remove(a)` and call `deleteAnalysisArtifacts` after it, mirroring the order `deleteAnalysis`
   already uses.

This is roughly three files and forty lines, keeps one chokepoint for "which files belong to an
analysis", and fixes every caller of `applyDesignChange` rather than only the REST ones.

**The trade-off Paul has to rule on:** the files go before the transaction commits, so a rollback
leaves surviving analyses with their `.zip` and `.tsv.gz` deleted. Both are regenerated on next request
(§2), so the cost of that case is a rebuild, not lost data — but it is still a file deleted for an
analysis that was not removed, and the brief for this work says not to widen what gets deleted. Option
D has no such case and costs the plumbing instead.

**Recommendation: Option E if a regenerable cache file may be dropped on a rolled-back commit; Option D
if it may not.** Not implemented pending that call.

## 8. Other database-only removals found while tracing (same defect, different route)

These orphan the same artifacts. None is on the curation commit path; listed so the fix, whichever
option lands, can be pointed at them deliberately rather than found again later.

| File:line | Route | Note |
|---|---|---|
| `DifferentialExpressionAnalysisTaskImpl.java:70` | `DELETE /datasets/{id}/tasks/differential/{analysisId}` (`DatasetsWebService.java:6545-6555`) | Calls `differentialExpressionAnalysisService.remove( toRemove )`. **This class already autowires `DifferentialExpressionAnalyzerService` at `:52` and runs outside any transaction**, so switching this line to `deleteAnalysis( ee, toRemove )` is a one-line change with no propagation and no injection problem. The dedicated "remove one analysis" endpoint is currently the cheapest way to orphan a file. |
| `DeleteDiffExCli.java:44` | `deleteDiffEx` CLI | `removeForExperiment( ee, true )`. `DifferentialExpressionAnalysisCli.java:376,384` does the same job through `differentialExpressionAnalyzerService.deleteAnalyses(...)` and does remove the files. |
| `AnalysisUtilServiceImpl.java:72` | `deleteOldAnalyses` | `@Component`, not transactional — could call the analyzer directly. |
| `ExperimentalFactorServiceImpl.java:60,85` | factor removal | `removeForExperimentalFactor(s)`; reached from `applyDesignChange` step 4. |
| `FactorValueDeletionImpl.java:96` | factor-value deletion | |
| `ExpressionExperimentSubSetServiceImpl.java:92` | subset removal | `removeForExperimentAnalyzed( subset )` |
| `ExpressionExperimentWriteServiceImpl.java:340` | whole-dataset delete | `removeForExperiment( ee, true )`. `DeleteExperimentsCli.java:117` calls `eeService.remove( ee )` with no `deleteAllFiles` call, so a deleted dataset leaves its `dataFiles/` entries too. |

## 9. Where a routing test would go

`ExpressionExperimentServiceImplTest` (`gemma-core/src/test/.../ExpressionExperimentServiceImplTest.java`)
already pins the current contract at `:1404` (`testApplyRemovesTheAnalysesThePreflightEnumerated`,
`verify( deaService ).remove( dea )`) and `:1423` (`testApplyRemovesNoAnalysisForALabelOnlyEdit`,
`verify( deaService, never() ).remove( ... )`), with the stub at `:1332`. A `verify(analyzerService)`
assertion drops in beside them.

That test is a `BaseTest5` mocked context with no proxying and no transaction manager, so it cannot
observe the `Propagation.NEVER` violation in Option A. Whichever option lands needs a check that runs
against a real proxied context, or the routing assertion will be green on a path that throws in
production.
