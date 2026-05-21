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

## Bucket B — `Gene altered from null to 4309` (11 errors)

- `GeneServiceTest` (8 errors, all methods)
- `GeneSetServiceTest.setUp` (3 errors, all setUps for tests that need
  `getTestPersistentGene`)

Stack: HibernateException at the FIRST query inside each test, even
queries that don't touch Gene (e.g. `taxonService.findByCommonName`).
The exception fires during session flush. Smoking gun: the same gene id
(4309) shows up in every test of the run.

**Needed:** investigate whether `PersistentDummyObjectHelper.getTestPersistentGene`
+ `GenomePersister.persistGene` leaves the input Gene with a null id but
attached to the session after persist (so a downstream re-set of id from
the inflated row triggers the "altered from null" check).

## Bucket C — Audit transactional rollback (3 remaining)

After fixing the date-equality assertions in `AuditTrailServiceImplTest`,
3 still fail:

- `testAddEventWhenTransactionIsRolledBack:248` — expected 1 event after
  rollback, got 2. The audit event is persisted via a path that survives
  the outer rollback.
- `testAddEventWhenTransactionIsRolledBack2:265` — expected last event
  to be UPDATE, got CREATE. Event ordering (date desc, id desc) probably
  changed under HB6 / MySQL `datetime(3)` precision.
- `testAddTroubleEventWhenCurationDetailsAreModified:285` —
  `session.flush()` on an `sessionFactory.openSession()` instance not
  bound to the test's `pta.getTransaction(...)` transaction throws
  `TransactionRequired: no transaction is in progress`. HB6 tightened
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
