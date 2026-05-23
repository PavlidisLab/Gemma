# Failsafe Residual Triage

After the test-side drift fix pass (commits `e1cd7cef1d`..`5b38aa131d`),
this branch addressed ~25-30 of the original 87 failsafe failures. The
list below is what remains, bucketed by likely root cause and with a
one-line "what's needed" recommendation per class.

Run baseline: 87 issues (24 F + 63 E) across 42 test classes.

## Bucket A — Deep Hibernate-6 merge / matrix-assembly cascade (~17 errors)

These share two stack signatures that originate in production code, not
tests, and are interlinked. Do NOT chase test-side; the production
behavior needs investigation.

- `BaselineDetectionTest.setUp` — NPE `Map.size() because "sn" is null`
  inside `PersistentSet.equalsSnapshot` during DefaultMergeEventListener
  → cascade-from-EE-persist. HB6 merge no longer tolerates an unsnapshotted
  PersistentSet.
- `DiffExTest.testCountData` — same `sn` NPE.
- `SplitExperimentTest.testSplitGSE17183ByOrganismPart`,
  `SplitExperimentTest.testSplitGSE123753ByCollectionOfMaterial` — same.
- `ExperimentalDesignImporterTest.testParse`, `testParseDryRun`,
  `testParseWhereExtraValue` — same.
- `ExperimentalDesignImportDuplicateValueTest.testParse` — same.

- `ContinuousVariableDiffExTest.setUp`,
  `DiffExTest.testGSE35930`, `DiffExWithInvalidInteractionTest.setUp`,
  `ProcessedExpressionDataCreateServiceTest.testComputeDevRankForExpressionExperimentMultiArrayWithGaps`,
  `SampleCoexpressionAnalysisServiceTest.test`,
  `TwoChannelExpressionDataDoubleMatrixTest.testMatrixConversion`,
  `ExpressionExperimentBatchCorrectionServiceTest.testComBatOnEE`,
  `SVDServiceImplTest.testSvd`, `testSvdGapped` — all
  `IllegalState: No dimensions to setup columns from` from
  `AbstractMultiAssayExpressionDataMatrix.java:429`. The matrix builder
  is fed an empty BioAssayDimensions set; upstream is likely a silent
  no-op from the GEO/persist path that the `sn` NPE also affects.

**Needed:** one-person-half-day to investigate why
`PersistentSet.equalsSnapshot` gets a null snapshot through the EE merge
path — fix is upstream of the bag-initialization contract.

## Bucket B — `Gene altered from null to 4309` (11 errors) — RESOLVED 2026-05-22

> **Status**: fixed upstream by commit `c4f883546c`
> (`fix(gene): remove side-effect delete from GeneDao.find(Gene)`,
> HQL_SQL_AUDIT C4). The "find" method previously called
> `geneDao.remove(...)` on duplicate-NCBI-ID candidates, which under a
> read-only transaction was staged for flush-time replay. Flush then
> tripped on the deprecated row whose id had not been reset, surfacing as
> `Gene altered from null to <id>` on the FIRST query of any test that
> went through `getTestPersistentGene` → `GenomePersister.persistGene` →
> `geneDao.find(gene)`.
>
> Validation: `GeneServiceTest` 8/8 + `GeneSetServiceTest` 7/7 pass in
> isolation against `phase2-acl-migrate` HEAD post-C4.

- `GeneServiceTest` (8 errors, all methods) — now pass.
- `GeneSetServiceTest.setUp` (3 errors, all setUps for tests that need
  `getTestPersistentGene`) — now pass.

## Bucket C — Audit transactional rollback (3 remaining) — RESOLVED 2026-05-22

> **Status**: all three failures already fixed in a prior session pass.
> Re-verified 2026-05-22: `mvn verify -Dit.test='AuditTrailServiceImplTest'`
> BUILD SUCCESS, 14/14 pass. Fixes are documented in-file (comments at
> lines 172-176, 263-266, 290-304, 307-308 of `AuditTrailServiceImplTest`).

- `testAddEventWhenTransactionIsRolledBack:248` — plain `addUpdateEvent`
  does NOT route through @AuditedOnError REQUIRES_NEW (only the Throwable
  overload does); audit row IS rolled back. Assertion correct as-is.
- `testAddEventWhenTransactionIsRolledBack2:265` — fixed by replacing
  positional `events.get(0)` with `stream.filter(UPDATE)`. The audit bag
  is `order-by="date"` only at `datetime(3)` precision, so CREATE +
  REQUIRES_NEW UPDATE can land in the same millisecond.
- `testAddTroubleEventWhenCurationDetailsAreModified:285` — fixed (a)
  by switching from `sessionFactory.openSession()` to
  `sessionFactory.getCurrentSession()` (HB6 requires flush on a bound
  session), and (b) by dropping a now-redundant outer-tx `addUpdateEvent`
  that deadlocked with the REQUIRES_NEW Throwable overload on the
  AuditTrail.lastEvent FK row.
  this.

**Needed:** the audit-lastEvent-denorm work + REQUIRES_NEW propagation
needs to be exercised against these rollback contracts; one engineer-day.

## Bucket D — Quartz 1.x → 2.x mismatch (6 errors)

- `BatchInfoRepopulationJobTest.test`, `testWithPreviousFireTime` —
  `IncompatibleClassChange: Found class org.quartz.JobExecutionContext,
  but interface was expected`.
- `SchedulerSecurityTest.runSecuredMethodOnSchedule`,
  `runSecuredMethodOnScheduleMultiGroup`, `runUnauthorizedMethodOnSchedule`
  — `NoClassDefFound: org/quartz/impl/JobDetailImpl` (the @Lazy fix in
  this batch unblocked context init but now exposes this Quartz
  mismatch).
- `SchedulerSecurityTest.testSecureJob` — same `IncompatibleClassChange`.

The repo pins quartz 1.8.6 (gemma-core/pom.xml ~line 320, comment says
"latest compatible with Spring 3"). Spring 6 requires Quartz 2.x.

**Needed:** bump Quartz to 2.5.x. This is a focused dep upgrade with
fanout into `SecureMethodInvokingJobDetailFactoryBean`,
`SecureQuartzJobBean`, scheduler config; one engineer-day.

## Bucket E — AUDIT_TRAIL.LAST_EVENT_FK cascade (3 errors)

- `GeneSetServiceTest.tearDown` (3 instances) — `ConstraintViolation: a
  foreign key constraint fails ... AUDIT_TRAIL.FK_AUDIT_TRAIL_LAST_EVENT
  REFERENCES AUDIT_EVENT(ID)`.

The Flyway migration V8 declares `ON DELETE SET NULL`, but with
`hbm2ddl.auto=create` Hibernate creates its own FK (no cascade) BEFORE
Flyway runs, so the SET NULL never lands. The cascade-delete path through
`GeneSet.remove` hits the FK before AUDIT_TRAIL is deleted.

**Needed:** either tell Hibernate not to emit the FK
(`@org.hibernate.annotations.OnDeleteAction.SET_NULL` doesn't exist for
the HBM XML many-to-one; use `<key on-delete="cascade">` on the inverse
or `<sql-update>` callback), OR add an explicit pre-remove that nulls
`lastEvent` before cascade.

## Bucket F — ACL after-invocation throws instead of filters (1 error)

- `DifferentialExpressionResultServiceTest.testFindByGeneAndExperimentAnalyzed:114`
  — under anonymous user,
  `AclEntryAfterInvocationCollectionFilteringProvider.decide` throws
  `NotFoundException: Unable to locate a matching ACE for passed
  permissions and SIDs` instead of filtering the result to empty.

(The original `max-results cannot be negative` issue is fixed by this
batch; the test now reaches a new failure mode.)

**Needed:** the ACL provider should swallow `NotFoundException` and emit
an empty result; one-line provider fix.

## Bucket G — Lock-mode rejected by HB6 (2 errors)

- `CompositeSequenceGeneMapperServiceTest.testGetGenesForCompositeSequence`
  (×2) — `UnsupportedLockAttemptException: Lock mode not supported` in
  `blatCollapsedSequences:197`.

**Needed:** locate the `setLockMode(...)` callsite at
`blatCollapsedSequences:197`-ish and replace the unsupported mode with
the HB6 equivalent.

## Bucket H — Single-line drift / data-flow regressions (~12 errors)

These are independent one-offs that need bespoke investigation. Listing
with the visible symptom:

- `LowVarianceDataTest.setUp:141` — `EntityNotFoundException:
  ExperimentalFactor#28` after persist; fixture state leak.
- `MeanVarianceServiceTest.testServiceCreateCountData:252` —
  `EntityNotFoundException: BioAssay#76` on merge; fixture state.
- `DataUpdaterTest.testLoadRNASeqDataWithMissingSamples:362` —
  `EntityNotFoundException: BioAssay#1607` (don't touch per brief).
- `ExternalFileGeneLoaderServiceTest.testLoad:123` —
  `EntityNotFoundException: GeneProduct#2745`.
- `GeoDatasetServiceTest.testFetchAndLoadGSE9048:261` — expected
  not-null, got null (don't touch per brief).
- `NCBIGeneLoadingTest.testGeneLoader:98` — `expected: <4> but was: <0>`.
- `GeneMultifunctionalityPopulationServiceTest.test:155` — `expected: >=
  2 but was 1`; only one audit event written where two expected.
- `DifferentialExpressionAnalysisServiceTest.testCreate:147` —
  ObjectIdentity Type drift (`DifferentialExpressionAnalysis` expected,
  got `ExpressionExperiment`).
- `GeneSearchTest.testSearchGenes:80` — HibernateSearch error (network
  / classpath / startup ordering).
- `GeneWriteServiceTest.testGiRotationInPlace:281` — `expected: <1> but
  was: <0>`.
- `RawAndProcessedExpressionDataVectorServiceGeoTest.testFindByQt:109` —
  `IllegalArgumentException: Not an entity:
  ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector`.
- `SecureValueObjectAuthorizationTest.testSecuredExpressionExperimentValueObject:94`
  — `NotFoundException: Unable to locate a matching ACE` (same family as
  bucket F).
- `AclAdviceTest.testExpressionExperimentAcls:277` — `Could not obtain
  transaction-synchronized Session for current thread`.
- `CharacteristicServiceTest.testGetParents`,
  `testFindExperimentsByUris`, `testFindExperimentsByUrisAsProxies` —
  data not returned (fixture or join-graph drift). The
  `testBrowseWithInvalidField` is fixed by this batch.
- `GeneSetServiceTest.testLoadValueObject:279` — expected 1 set, got 2
  (data leak across tests).
- `ExpressionExperimentServiceIntegrationTest` (don't touch — 3 errors
  belong to speedup agent surface).
- `ProcessedExpressionDataVectorCreationHelperServiceTest.testThaw` —
  "before thaw" assertion fails (the L2-cache-disabled session keeps the
  managed instance with bag hydrated). Naive session-clear breaks the
  "after thaw" half. Needs a fresh-session wrapper helper.
- `ArrayDesignServiceTest.testThaw` — same shape.

## Bucket I — Bigger refactors needed (1 error)

- `FilteringVoEnabledServiceIntegrationTest.testFilteringByAllFilterableProperties:54`
  and `testSortingByAllFilterableProperties:92` — `PathElementException:
  Could not resolve attribute 'value' of 'ExpressionAnalysisResultSet'`.
  Iterates over all filterable properties of all FilteringVoEnabledServices;
  one of them has a path that doesn't resolve. Bisect by finding which
  property fails first.

## What this batch fixed

Commits on this branch:

- `e1cd7cef1d` — 4 buckets:
  - `BaseSpringContextTest5.countRowsInTable/deleteFromTables` lazy-init
    via `getJdbcTemplate()` → 2 errors in `EeWriteServiceImplQtDedupTest`.
  - `AclQueryUtilsTest` — rewrite 6 assertions to the post-EXISTS
    refactor contract (5 F + 1 E).
  - `AuditTrailServiceImplTest` — fix 2 date-equality assertions
    (`Timestamp.equals(Date)` is asymmetric — compare via `getTime()`).
  - `SchedulerSecurityTest` — `@Lazy` on the `groupAgentSecurityContext`
    field so the FactoryBean is not eagerly resolved during
    test-instance autowiring. (Unblocks context init but exposes Bucket
    D Quartz issue.)
- `8d5fb016bc` — 4 more buckets:
  - `BioAssayDaoImpl.findBioAssayDimensions` — HQL rewrite for HB6
    (`:bioAssay in ba` → `ba = :bioAssay`). Fixes
    `BioAssayServiceTest.testFindBioAssayDimensionsLong` and
    `ExpressionExperimentPlatformSwitchTest.testPlatformSwitchingWithExpressionData`.
  - `DifferentialExpressionResultDaoImpl.findByGeneAndExperimentAnalyzed`
    — guard `setMaxResults(limit)` when `limit <= 0` (HB6 rejects
    negative). Unblocks
    `DifferentialExpressionResultServiceTest.testFindByGeneAndExperimentAnalyzed`
    (now hits a downstream ACL issue — bucket F).
  - `ExpressionAnalysisResultSetServiceTest` — fix 3 assertions to
    match current `configureFilterableProperties` (aliases `sfvc`/`bc`,
    only `analysis.name` unregistered, size-paths keep full dotpath
    propertyName).
- `da80c6737b` — revert harmful session-clear thaw test changes.
- `d125520d0f` — size-filter propertyName stays full dotpath.
- `5b38aa131d` — `CharacteristicServiceTest.testBrowseWithInvalidField`
  expects `IllegalArgumentException` not `QueryException` (HB6 wraps).

### Confirmed-passing test classes after this batch

- `AclQueryUtilsTest` — 14/14.
- `BioAssayServiceTest` — 3/3.
- `ExpressionExperimentPlatformSwitchTest` — 2/2.
- `ExpressionAnalysisResultSetServiceTest` — 6/6 (1 skipped).
- `EeWriteServiceImplQtDedupTest` — 2/2 (was 2 errors).

Tests that improved but still have residual failures:
- `AuditTrailServiceImplTest` — 5 → 3 (2 date-equality fixed; 3 txn
  rollback / flush remain).
- `SchedulerSecurityTest` — context init fixed; Quartz mismatch now
  surfaced.
- `DifferentialExpressionResultServiceTest` — maxResults fixed; ACL
  issue now surfaced.
- `CharacteristicServiceTest` — 4 → 3 (browse-invalid-field fixed).

Rough net fixed: ~22 of original 87.

## Batch 2 — failsafe-residuals-batch2 (2026-05-21)

Starting state: 21 issues (9F + 12E) across ~10 test classes. Commits:

- `ad9644a009` `fix(audit): null lastEvent before deleting events in
  AuditTrailDao.removeByIds` — Bucket E. Pre-deletes the denormalised
  LAST_EVENT_FK pointer before deleting the events so the
  hbm2ddl-generated FK (which has no cascade rule, unlike the V8
  migration's ON DELETE SET NULL) doesn't reject the delete. Also
  scopes the aeIds / aetIds queries to the trail ids being deleted —
  previously they slurped every audit-event id in the DB. Confirmed:
  GeneSetServiceTest 7/7 (was 6E + 1F).
- `919892e7be` `fix(acl): guard onClose against null session in stream
  filtering providers` — fixes
  `AclEntryAfterInvocationStreamFilteringProvider.decide` NPE when
  `AclService.openSession()` returns null (Gemma's intentional contract).
  Partially unblocks
  `ExpressionExperimentServiceIntegrationTest#testStreamExperiments`
  (NPE gone, downstream AccessDenied remains — see residuals below).
- `f848800ff4` `fix(acl): swallow NotFoundException in VO
  populateValueObject WRITE check` — Bucket F. The WRITE-check
  `acl.isGranted` inside populateValueObject was the last unguarded
  NotFoundException throw on the collection-VO path. Confirmed:
  SecureValueObjectAuthorizationTest 1/1 (was 1E).
- `27f293e76f` `test(vectors): reload EE after addRaw/createProcessed
  mutations` — Bucket H. The vector-add/create methods mutate the
  re-fetched managed instance (via `ensureEeInSession`), not the
  test's local `ee`. With L2 cache disabled in BaseDatabaseTest5 the
  local reference doesn't see the writes. Reload through the service
  before asserting. Confirmed:
  ProcessedExpressionDataVectorCreationHelperServiceTest 4/4 (was 4F);
  ProcessedExpressionDataVectorServiceTest 2/2 (was 1F).
- `f508146ee8` `test(externalGeneLoader): reload gene after GP removal
  to avoid stale merge` — Bucket H. Merging the local gene after the
  GP has already been removed via the service trips HB6 merge into
  fetching the deleted row; reload first. Confirmed:
  ExternalFileGeneLoaderServiceTest 4/4 (was 1E).

Batch 2 net: 14 of 21 residuals fixed across 5 commits (7 GeneSet + 4
processed-vector creation + 1 processed-vector service + 1
SecureValueObject + 1 ExternalFileGeneLoader + 1 EE stream NPE
component).

### Batch 2 residuals (7 remaining)

- `ExpressionExperimentServiceIntegrationTest.testLoadValueObjectsByFactorValueCharacteristic`
  (1E) — `Filter.parseItem` rejects "null" because the transient
  `Statement` added to a FactorValue isn't given an id by the
  enclosing `expressionExperimentService.update(ee)` call (same
  managed-instance gap as the vector tests, but the value is fed to
  `String.valueOf(s.getId())` before the reload could land — test
  needs a per-statement create / reload-fv-and-pick-up-id step before
  asking for `s.getId()`).
- `ExpressionExperimentServiceIntegrationTest.testLoadValueObjectsBySampleUsedCharacteristic`
  (1E) — same managed-instance shape on Characteristic id; same fix
  pattern.
- `ExpressionExperimentServiceIntegrationTest.testCacheInvalidationWhenACharacteristicIsDeleted`
  (1F) — assertion-not-null after a cache invalidation flow; likely
  the same managed-instance gap.
- `ExpressionExperimentServiceIntegrationTest.testStreamExperiments`
  (1E remaining) — after the NPE fix, hits
  `AccessDeniedException: Access is denied` from `UnanimousBased.decide`
  on the BEFORE invocation. `streamAll()` is annotated
  `@Secured("IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_STREAM_READ")`
  so any logged-in user should pass the before-invocation vote.
  Smells like a `RoleVoter` / `AuthenticatedVoter` mismatch or a
  configuration drift in the stream-secured method security wiring;
  needs investigation.
- `DifferentialExpressionAnalysisServiceTest.testCreate` (1F) — ACL
  ObjectIdentity drift: `resultSet1`'s parent ACL identity comes back
  as `ExpressionExperiment#44` instead of the expected
  `DifferentialExpressionAnalysis#13`. Either the ACL parent
  assignment on ExpressionAnalysisResultSet now points one hop too
  high, or `SecuredChild#getSecurityOwner` returns the EE for the
  result set instead of the analysis. The revert in
  `d0f141efd3` ("Revert: fix(acl): point SecuredChild parent ACL at
  the immediate security owner") is the smoking gun — that revert
  re-introduced this drift. Needs a different fix that doesn't break
  the OTHER thing the revert was un-fixing.
- `GeneSearchTest.testSearchGenes` (1E) — HibernateSearch error at
  query time. Likely Lucene index directory / startup-ordering /
  Hibernate Search 7 config; per `HibernateConfig.resolveSearchIndexBase`
  comment that path was already adjusted for HS7. Needs in-JVM
  Hibernate Search reindex or a startup-condition guard.
- `GeneWriteServiceTest.testGiRotationInPlace` (1F) — `updateGene`
  returns a Gene whose products bag is empty (expected 1). Reloading
  the gene after update still shows 0 products. Suggests the GI
  rotation path inside `handleGeneProductChangedGIs` /
  `removeGeneProducts` actually deletes the GP rather than rotating
  its NcbiGi in place — a real production bug, not a test issue.
  Needs to read the rotation algorithm under HB6 and check whether
  the "switching gene product from one gene to another" branch fires
  spuriously.

## Batch 3 — failsafe-residuals-batch3 (2026-05-21)

Starting state: 7 issues carried over from batch 2 plus the new
residuals introduced by the recent merges (skeleton workflow,
annotations write, PUT-design, workflow-state, hbm-default-quoting fix
at `d19dcf45d8`). gemdtest is single-tenant and locked by other parallel
agents, so this batch is compile-validation only — no focused failsafe
run was possible inside the worktree.

Commits:

- `ca9f98517d` `test(EESIT): reload after update to capture
  merge-generated ids` — Bucket H managed-instance gap, three EESIT
  tests in one shot:
  - `testLoadValueObjectsByFactorValueCharacteristic` — capture
    pre-update Statement id set on the FactorValue, reload EE post
    `expressionExperimentService.update(ee)`, identify the new Statement
    by id-diff, then build the Filter against the resolved id.
  - `testLoadValueObjectsBySampleUsedCharacteristic` — same pattern on
    `BioMaterial.characteristics` via `bioMaterialService.load + thaw`.
  - `testCacheInvalidationWhenACharacteristicIsDeleted` — capture
    baseline ids, resolve the persisted Characteristic via the reloaded
    EE before the not-null assertion. The downstream
    `characteristicService.remove(c)` / `load(c.getId())` /
    `doesNotContain(c)` paths were retargeted at the reloaded managed
    `persistedC` so the remove receives a valid id.
- `6d19b85442` `test(geneSet): make testLoadValueObject robust to
  cross-test data residue` — Bucket H one-off. Hard-coded
  `loadAllValueObjects().size() == 1` assumed a freshly empty schema;
  failsafe ITs share gemdtest, so any earlier-class leak inflated the
  count. Capture baseline pre-create, assert baseline+1.

Batch 3 attempted but not landed (reasoning):

- `ExpressionExperimentServiceIntegrationTest.testStreamExperiments`
  (AccessDenied) — needs a runtime trace of which voter is denying on
  `IS_AUTHENTICATED_ANONYMOUSLY` + `AFTER_ACL_STREAM_READ`. The other
  patterns (`@Secured("IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_*")`)
  work elsewhere, so the divergence is specifically around stream
  filtering. Without a focused failsafe run, blind voter / provider
  shuffling risks regression. Defer to a session that can run the
  test class against gemdtest.
- `DifferentialExpressionAnalysisServiceTest.testCreate` — confirmed
  the structural cause: `ParentIdentityRetrievalStrategyImpl` returns
  the IMMEDIATE getSecurityOwner (which for ResultSet is DEA), but
  `BaseAclAdvice.locateSecuredParent` (post-revert `d0f141efd3`)
  recurses to the TOP-LEVEL Securable (EE for ResultSet). Test
  expects 3-level chain `EE -> DEA -> ResultSet`; revert collapses
  it to `EE -> ResultSet`. Re-applying the immediate-owner fix
  re-breaks the 5 setUp tests the revert was protecting. A proper
  fix probably has to teach `chooseParentForAssociations` (or the
  `AclEventListener` stash) to distinguish "ED/EF/FV inherit from
  EE" from "ResultSet inherits from DEA" — needs the OTHER 5
  failing tests in front of you to design against. Deferred.
- `GeneSearchTest.testSearchGenes` — Hibernate Search 7 query-time
  error. Without the actual stack from a run, no minimal fix is
  visible from a code read; `HibernateConfig.resolveSearchIndexBase`
  already coerces the directory.root to `${java.io.tmpdir}` so
  startup ordering may not be the failure mode any more. Defer.
- `GeneWriteServiceTest.testGiRotationInPlace` — traced the algorithm
  end-to-end. The "same name, different GI" path SHOULD recognize the
  existing GP by name (in `updatedGpMap`), update its NcbiGi in place
  via `updateGeneProduct`, then `handleGeneProductChangedGIs` sees the
  existing GP already carrying the new GI (which is in `usedGIs`) and
  skips it — `toRemove` ends up empty. On paper, no GP removal should
  happen. So the "products bag is empty" observation must originate
  somewhere I can't pin from a static read alone: either a session-
  level managed-instance refresh dropping the GP after the merge, an
  L2 cache eviction, or an unintended cascade. Needs a focused run
  with logging on `handleGeneProductChangedGIs` and the post-update
  reload. Deferred.
- Bucket A (`sn` NPE through `PersistentSet.equalsSnapshot`) and
  Bucket B (`Gene altered from null to 4309`) — out of scope for a
  surgical batch; both need upstream HB6 investigation. Skipped per
  the brief's "chip away" framing.
- Sister-owned files (DAOs under `persistence/service`,
  `gemma-rest/src/test`, unified-curation-draft files) — left alone
  per scope guardrails.

No new regressions emerged from the recent merges in the files this
batch touched (compile-clean against current main).

## Batch 4 — failsafe-residuals-batch4 (2026-05-21)

Starting state: ~10 residuals carried over from batch 3 plus any new
divergence introduced by HQL high-severity, SpotBugs P1 batch 2, and
gene-page endpoint merges. gemdtest is single-tenant so this batch is
compile-validation only.

Recent-merge regression check (read-only):
- HQL fixes (ArrayDesignDaoImpl.getGenesByCompositeSequence collection
  overload, BibRefDaoImpl.browse ORDER BY whitelist,
  ExpressionAnalysisResultSetDaoImpl.getBaselinesForInteractionsByIds
  MAX wrapper, CharacteristicDaoImpl '<> null' → 'is not null',
  TableMaintenanceUtilImpl EE2AD truncate addSynchronizedQuerySpace) —
  searched gemma-core/src/test for callsites with shape-dependent
  assertions on these methods. ArrayDesignDaoTest exercises only smoke
  calls on getGenesByCompositeSequence with no result-shape assertions
  (empty AD, expecting empty maps). No matching tests for
  browse(orderField), getBaselinesForInteractionsByIds, or the EE2C
  truncate path. No test regressions visible from a static read.
- SpotBugs P1 batch 2 UTF-8 pinning — touched LinearModelAnalyzer
  debug-output, DatabaseViewGenerator views, ArrayDesignAnnotationService
  read/write, GeoBrowser HTTP I/O, AbstractScriptBasedTransformation
  stdout, GemmaRestApiClient basic-auth getBytes. None of these have
  byte-comparison assertions in their test partners
  (ExpressionDataFileServiceTest mocks ArrayDesignAnnotationService;
  GemmaRestApiClientTest doesn't assert auth-header bytes). No
  regressions.
- Renamed types SkeletonInvestigation → PreboardingExperiment →
  PreboardedExperiment — no stale references in gemma-core/src/test or
  gemma-rest/src/test.

Commits:

- `ef2f537327` `test(diffex): reload EE after factor removal in
  LowVarianceDataTest setUp` — Bucket H one-off. Replaces the
  in-place `.getExperimentalFactors().clear()` after
  `experimentalFactorService.remove(toremove)` with a fresh
  `loadAndThaw`. The stale in-memory ee still carried references to
  the deleted EFs, so the next `expressionExperimentService.update(ee)`
  triggered HB6 merge to refetch ExperimentalFactor#28 and threw
  EntityNotFoundException. Same managed-instance gap shape fixed by
  batch 2/3 patches elsewhere.

Batch 4 inspected and deferred:

- `MeanVarianceServiceTest.testServiceCreateCountData` — same
  EntityNotFoundException shape but on BioAssay#76 after a deliberate
  failed `addCountData(..., false)` (expected IAE on line 247) followed
  by a retry with `allowMissingSamples=true` on line 252. The retry path
  is the one throwing; needs runtime trace of which BioAssay
  `addCountData` is hitting during the second pass to know whether the
  failed first pass left orphan state. Deferred.
- `CompositeSequenceGeneMapperServiceTest.testGetGenesForCompositeSequence`
  (Bucket G lock-mode) — repo-wide `setLockMode`/`LockOptions.UPGRADE`
  call-sites are already migrated to `Session.lock(entity, LockMode)`
  in ArrayDesignDaoImpl (3 spots). The remaining lock-mode error
  inside `aligner.processArrayDesign` (via ArrayDesignProbeMapperService)
  isn't visible from a static read of those services — they hold no
  explicit lock calls. The error must originate from a transitive
  helper or framework adapter; needs a stack trace from a focused run.
  Deferred.
- `RawAndProcessedExpressionDataVectorServiceGeoTest.testFindByQt`
  (`Not an entity: BulkExpressionDataVector`) — the three known
  hot-spot DAO methods (`find(QuantitationType)`,
  `find(Collection<QuantitationType>)`, `findByExpressionExperiment`)
  are already manually rewritten in
  RawAndProcessedExpressionDataVectorDaoImpl to dispatch to the two
  concrete entity types. The test on line 109 calls
  `rawAndProcessedService.find(qt)` which routes through the
  AbstractBulkExpressionDataVectorService.find(qt) wrapper into the
  overridden DAO method — should not trip the metamodel. The actual
  throw site needs a runtime stack; possibly a thaw/ACL post-filter
  call going through an un-overridden `findByProperty` indirection.
  Deferred.
- `GeneMultifunctionalityPopulationServiceTest.test:155` (expected
  ≥2 audit events, got 1) — multifunctionality update path; if the
  audit-Phase-C migration replaced one of the imperative addUpdateEvent
  calls with `@Audited`/`@AuditedConditional` and the path is
  self-invoked (private method or `this.x()`), the aspect does not
  fire. Needs to read the migration commit for this service and see
  which call became aspect-based plus whether the call is reached via
  a proxy boundary. Deferred to a session that can run the test.
- `CharacteristicServiceTest.testGetParents` /
  `testFindExperimentsByUris` / `testFindExperimentsByUrisAsProxies`
  — setUp calls
  `tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries(null, false)`
  to refresh the EE2C lookup table. The EE2C path in
  TableMaintenanceUtilImpl already uses `addSynchronizedQuerySpace`
  correctly (not affected by the EE2AD scalar-vs-querySpace fix in
  c9-batch). The likely cause is upstream of the lookup-table refresh
  — characteristics aren't reaching EE2C, or the join graph in
  `findExperimentsByUris` walks a path that now returns empty under
  HB6 join semantics. Needs runtime trace. Deferred.
