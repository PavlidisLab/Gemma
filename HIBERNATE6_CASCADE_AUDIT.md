# Hibernate 6 cascade-strictness audit

A focused scan of `*DaoImpl.java` / `*ServiceImpl.java` in `gemma-core/src/main/java`
for HB-5-era cascade patterns that Hibernate 6's stricter `ACTION_CHECK_ON_FLUSH`
cascade engine may trip over. Companion to the two `notable_cases.md` entries
(AuditTrail/AuditEvent cache bug — fixed at `ab8b4c443c`; EE-DAO 18-failure
TransientObjectException family — deferred).

Excludes `gemma.gsec.*` per scope.

## Patterns audited

- **A** — HQL bulk-delete `delete from X where ...` followed by `merge(parent)` /
  `update(parent)` (≡ `session.merge`) in the same session: bulk-delete bypasses
  the session, so the in-memory parent collection is stale; the subsequent merge
  re-cascades through stale collections and HB6 flags the resulting transient
  / DELETING refs.
- **B** — `session.delete(child)` followed by `session.update(parent)` or
  `session.merge(parent)` with the child still reachable from the parent's
  collection.
- **C** — `mutable="false"` HBM class + child collection + writes that cross
  `@Transactional` boundaries (AuditTrail/AuditEvent shape).
- **D** — `cascade="all"` or `cascade="all,delete-orphan"` on parent collection
  while child rows are removed via HQL/SQL bulk delete, bypassing cascade.
- **E** — `session.flush()` (explicit or auto-) between collection mutations
  without resynchronising the `PersistentSet` snapshot (the
  `replaceProcessedDataVectors` pre-`e304d1c2b3` shape).

## Methodology

Scope: 52 `*DaoImpl.java` + 116 `*ServiceImpl.java` in `gemma-core/src/main/java`.
Filtered to files calling `executeUpdate`, `session.delete`, `session.update`,
or `session.merge`. Cross-referenced against the 27 `mutable="false"` HBMs.
`update(entity)` in any `AbstractDao` subclass = `session.merge(entity)` (see
`AbstractDao.java:417`). Test coverage cross-referenced against test file
existence — no surefire reports were available in this worktree, so "tests
cover" means "a `*DaoTest` / `*ServiceTest` exercises the named method", not
"the test currently passes".

## Findings (10 candidate sites)

### HIGH risk (production data path, HB6-strict-flush candidate)

#### 1. `ExpressionExperimentDaoImpl.removeAllRawDataVectors` — Pattern A + D
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentDaoImpl.java`
**Lines**: 4279-4330 (helper); 2404 (caller in `remove(ee)`).
**Smell**: HQL `delete from RawExpressionDataVectorNumberOfCells ...` (4312-4315) +
HQL `delete from RawExpressionDataVector v where v.expressionExperiment = :ee`
(4316-4319) followed by `removeQts(ee, qtsToRemove)` which does
`session.delete(qt)` per QT (4647), then `update(ee)` ≡ `session.merge(ee)`
(4322). `Investigation.hbm.xml:95` maps `quantitationTypes` with `cascade="all"`,
so the merge re-cascades through QTs that are mid-`DELETING`. Same shape next
in `removeProcessedDataVectors` (4513-4573) called immediately after at
2405 — adding a second autoflush opportunity over an already-corrupted
session.
**Tests**: `ExpressionExperimentDaoTest` exercises this — see notable_cases
entry "ExpressionExperimentDaoTest 18 TransientObjectException failures"; 17/18
of those cite QT, 1 cites BioAssayDimension, all in `@After removeFixtures`
teardown autoflush. Currently failing on H2.
**Risk**: HIGH. Already known bug; the deferred-fix decision in notable_cases
applies here. Production deletion path runs once per session so the cascade
walk does not have a second chance to explode, but the latent inconsistency is
exactly what HB6 was hardened against.

#### 2. `ExpressionExperimentDaoImpl.removeProcessedDataVectors` — Pattern A + D
**File**: same as #1
**Lines**: 4513-4573.
**Smell**: same as #1 — HQL `delete from ProcessedExpressionDataVectorNumberOfCells`
(4550-4553) + HQL `delete from ProcessedExpressionDataVector` (4554-4557) +
`removeQts(ee, qtsToRemove)` + `update( ee )` at 4562. Pattern A + D in series
inside the same `remove(ee)` call (line 2405) as #1, making the autoflush
window even wider.
**Tests**: `ExpressionExperimentDaoTest.testCreateProcessedDataVectors*`,
`testReplaceProcessedDataVectors*`, `testRemoveProcessedDataVectors` — see
notable_cases.
**Risk**: HIGH. Same family as #1.

#### 3. `ExpressionExperimentDaoImpl.removeRawDataVectors(ee, qt, ...)` — Pattern A + D
**File**: same as #1
**Lines**: 4332-4381.
**Smell**: HQL `delete from RawExpressionDataVectorNumberOfCells` (4362-4366) +
HQL `delete from RawExpressionDataVector` (4367-4371) + `removeQts(ee, {qt})` +
`update(ee)` at 4373. Single-QT variant of #1; same risk shape.
**Tests**: covered indirectly by `testRemoveRawDataVectors*` in
`ExpressionExperimentDaoTest`.
**Risk**: HIGH.

#### 4. `ExpressionExperimentDaoImpl.replaceRawDataVectors` — Pattern A + E
**File**: same as #1
**Lines**: 4383-4417.
**Smell**: collection-clear (`ee.getRawExpressionDataVectors().removeIf(...)`)
at 4395 + HQL `delete from RawExpressionDataVectorNumberOfCells` (4396-4400) +
HQL `delete from RawExpressionDataVector` (4401-4405) + `ee.getRawExpressionDataVectors().addAll(vectors)`
+ `updateWithNewVectors( ee, vectors )` at 4411. The collection mutation
between bulk-deletes and the trailing update creates the exact "stale
`PersistentSet` snapshot" shape that was fixed in `replaceProcessedDataVectors`
at `e304d1c2b3` (see the comment block at 4617-4627). The raw-vector twin of
that fix is NOT applied here — only the `ensureEeInSession(ee)` call (4390)
which is a partial workaround, not the snapshot-resync the fix added on the
processed side.
**Tests**: `testReplaceRawDataVectors*` family in `ExpressionExperimentDaoTest`.
**Risk**: HIGH — same pattern that already required a targeted fix on the
sibling method; likely the same explosion in the raw path under the right
session state.

#### 5. `DifferentialExpressionAnalysisDaoImpl.remove(analysis)` — Pattern A + B + D
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/analysis/expression/diff/DifferentialExpressionAnalysisDaoImpl.java`
**Lines**: 414-429.
**Smell**: native SQL `delete cr from CONTRAST_RESULT ...` (418-421) + native
SQL `delete dear from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT ...` (422-425)
followed by `super.remove(analysis)` (428) ≡ `session.remove(analysis)`.
`Analysis.hbm.xml:DifferentialExpressionAnalysis` maps `resultSets` with
`cascade="all" mutable="false" <cache usage="read-only"/>`; bulk-deleting DEAR
rows out from under the result-set's `results` collection (also `mutable="false"`,
see `AnalysisResultSet.hbm.xml:40`) leaves the in-session `ExpressionAnalysisResultSet`
instances holding stale collection snapshots that the `session.remove(analysis)`
cascade then walks. The native bulk delete bypasses every level of HB's cascade
machinery; on HB6 the subsequent cascade re-visits the same rows via the
session-cached collection snapshot and trips `ACTION_CHECK_ON_FLUSH`.
**Tests**: `DifferentialExpressionAnalysisDaoTest.testCreateAnalysisWithResultSetAndPvalueDistribution`
exercises `remove(analysis)` at line 76 (after the create assertion).
**Risk**: HIGH. Production DEA-deletion path. Same family as the EE-DAO bug
but with TWO levels of `mutable="false"` cache (DEA → resultSets → results).

#### 6. AnalysisResultSet + DifferentialExpressionAnalysisResult — Pattern C
**HBMs**: `gemma-core/src/main/resources/ubic/gemma/model/analysis/AnalysisResultSet.hbm.xml`
(`mutable="false"` + `<cache usage="read-only"/>` + child set `results`
(`mutable="false"`) + child set `hitListSizes` (`mutable="false"` +
`<cache usage="read-only"/>`)) and `DifferentialExpressionAnalysisResult.hbm.xml`
(`mutable="false"` + child set `contrasts`).
**Smell**: identical to the AuditTrail/AuditEvent shape that broke at
`ab8b4c443c` — `mutable="false"` parent + child collection + child cache
across `@Transactional` boundaries. Any code that writes a DEAR or
HitListSize from a service method on a result-set loaded in an earlier
request will see the same stale-empty-bag-in-L2 symptom.
**Tests**: `ExpressionAnalysisResultSetDaoTest`,
`DifferentialExpressionResultDaoTest`, `DifferentialExpressionResultServiceTest`
— full HB6 audit of these has not been done; existing tests may not exercise
the cross-tx-write path that triggers the staleness.
**Risk**: HIGH. Same shape, two levels deep, hottest read path in
`gemma-core` (every DE result page hits this). If it's broken the symptom will
look like "[None]" / empty result table for a DEA the user just wrote to.

### MEDIUM risk (production code, narrower preconditions)

#### 7. `ArrayDesignDaoImpl.remove(arrayDesign)` — Pattern B
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/arrayDesign/ArrayDesignDaoImpl.java`
**Lines**: 829-848.
**Smell**: `iterator.remove()` on `arrayDesign.getCompositeSequences()`
(840-845) interleaved with `session.delete(cs)` per element, then
`super.remove(arrayDesign)` ≡ `session.remove(arrayDesign)`. CompositeSequence
is removed both via collection iterator (which marks orphan-removal if the
collection has `delete-orphan`) AND explicit `session.delete`. On HB6 the
subsequent `session.remove(arrayDesign)` cascades through the parent — by then
the collection has been mutated mid-iteration and the children are in
`DELETING` state, which is the same family as the EE-DAO bug. The fact that
the loop does `iterator.remove()` instead of `clear()` is suspicious — that
pattern was a HB-5-era workaround for `ConcurrentModificationException` that
HB6 may flag differently.
**Tests**: `ArrayDesignDaoTest`, `ArrayDesignServiceTest` — the `remove(ad)`
path is exercised but the audit of HB6 strictness here is not documented.
**Risk**: MEDIUM. AD-deletion is rare in production (curator-only) so symptoms
would be sparse; but it touches a hot subsystem.

#### 8. `GeneDaoImpl.remove(gene)` and `GeneDaoImpl.removeAll()` — Pattern A
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/genome/GeneDaoImpl.java`
**Lines**: 448-510 (`removeAll`), 512-533 (`remove(gene)`).
**Smell**: cascade of HQL `delete from BioSequence2GeneProduct ...` /
`delete from GeneSetMember ...` / `delete from Gene2GOAssociation ...` /
native `delete from DATABASE_ENTRY ...` (multiple) / `delete from GeneProduct ...`
/ `delete from GeneAlias ...` followed by `super.remove(gene)` (single) or
`delete from Gene` (bulk) + `delete from PhysicalLocation` after Gene is gone.
`super.remove(gene)` ≡ `session.remove(gene)`; the loaded `gene` still has
references to `products`, `aliases`, `accessions` collections that were just
bulk-deleted at the DB level but remain populated in the session — HB6's
cascade walk on `session.remove(gene)` will iterate those stale collections.
The bulk variant (`removeAll`) is even riskier — it bulk-deletes Gene THEN
PhysicalLocation, so any in-session Gene with a PhysicalLocation ref points
at a row about to be deleted.
**Tests**: `GeneDaoTest`, `GeneServiceTest` — tests exist but coverage of the
post-merge cascade walk is not documented.
**Risk**: MEDIUM. Gene reload from NCBI is a production-rare cron path; not
hot. Symptoms would appear during gene-reload runs only.

#### 9. `ExpressionExperimentDaoImpl.updateSingleCellDimension` + `deleteSingleCellDimension` — Pattern C / B
**File**: same as #1
**Lines**: 2886-2890 (update), 2983-2987 (delete).
**Smell**: `SingleCellDimension.hbm.xml` is `mutable="false"` + `<cache usage="read-only"/>`
with `cellTypeAssignments` (cascade=all-delete-orphan), `cellLevelCharacteristics`
(cascade=all-delete-orphan), `bioAssays` list (cache, `mutable="false"`).
`updateSingleCellDimension` calls `session.update(scd)` ≡ merge on a
`mutable="false"` parent; `deleteSingleCellDimension` calls `session.delete(scd)`
on the same. Both methods are wrapped by `validateSingleCellDimension` which
walks the collections — exactly the cross-tx-write path the AuditTrail bug
was. Note also `CellLevelCharacteristics.hbm.xml` has the same
`mutable="false"` + cache + child list shape (line 29: `characteristics` is
also `mutable="false"`).
**Tests**: single-cell paths are exercised by `ExpressionExperimentDaoTest`
and pipeline-level tests; `testReplaceRawDataVectorsWithNewDimension` in the
failing 18-set hits BioAssayDimension transient — adjacent failure.
**Risk**: MEDIUM. SC-experiment writes are increasing in production usage.

#### 10. `BioAssayDimension` HBM — Pattern C
**HBM**: `gemma-core/src/main/resources/ubic/gemma/model/expression/bioAssayData/BioAssayDimension.hbm.xml`
**Smell**: `mutable="false"` + `<cache usage="read-only"/>` + child list
`bioAssays` (with own cache). Exact AuditTrail/AuditEvent shape. The 18-failure
EE-DAO bug already lists 1 BioAssayDimension transient case — confirms this
HBM is a live tripwire under HB6.
**Tests**: implicit in EE-DAO test (one failure currently observed).
**Risk**: MEDIUM (one symptom observed, but tangled with #1-#4).

### LOW risk (defensive code already in place, or rare path)

#### 11. `PrincipalComponentAnalysisDaoImpl.remove(pca)` — Pattern A mitigated
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/analysis/expression/pca/PrincipalComponentAnalysisDaoImpl.java`
**Lines**: 74-98.
**Smell**: native SQL bulk-delete of EIGENVALUE / EIGENVECTOR / PROBE_LOADING
followed by `super.remove(entity)`. **Mitigation already present**: line 77
calls `session.evict(entity)` BEFORE the bulk deletes, then explicitly clears
the three collections with `entity.setEigenValues(new HashSet<>())` etc. The
final `super.remove(entity)` is on a detached entity (will be re-attached by
`session.remove`). This is the canonical "safe pattern" for HQL-bulk-delete
followed by parent-remove: evict, mutate, then remove a fresh instance. **Worth
keeping as a reference**.
**Tests**: PCA paths covered by `PrincipalComponentAnalysisServiceTest`-family.
**Risk**: LOW.

#### 12. `AuditTrailDaoImpl.removeByIds` — Pattern A
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/common/auditAndSecurity/AuditTrailDaoImpl.java`
**Lines**: 44-72.
**Smell**: HQL bulk-delete of AuditEvent, AuditEventType, then AuditTrail.
No subsequent `merge(parent)` — the parent is the AuditTrail itself which is
being bulk-deleted at the same level. Clean shape: bulk-deletes only, no merge.
**Tests**: `AuditTrailDaoTest`.
**Risk**: LOW — bulk-only deletion, no in-session merge follows.

#### 13. `BlacklistedEntityDaoImpl.removeAll` — Pattern A (clean)
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/blacklist/BlacklistedEntityDaoImpl.java`
**Lines**: 144-163.
**Smell**: HQL `delete from BlacklistedEntity` + HQL `delete from DatabaseEntry`.
No subsequent merge/update on any in-session parent.
**Tests**: `BlacklistedEntityDaoTest`.
**Risk**: LOW.

#### 14. `Gene2GOAssociationDaoImpl.removeAll` — Pattern A (clean)
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/association/Gene2GOAssociationDaoImpl.java`
**Lines**: 152-172.
**Smell**: HQL bulk-deletes of associations + characteristics. No subsequent
parent merge. Mapping class `Gene2GOAssociation` IS `mutable="false"` but has
no child collections.
**Tests**: `Gene2GOAssociationService`-family.
**Risk**: LOW.

#### 15. `GeneSetDaoImpl.removeAll` — Pattern A (clean) + delegation
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/genome/gene/GeneSetDaoImpl.java`
**Lines**: 229-252.
**Smell**: HQL bulk-deletes of GeneSetMember + native bulk-delete of
CHARACTERISTIC + native bulk-delete of GENE_SETS2LITERATURE_SOURCES + HQL
`delete from GeneSet` + delegation to `auditTrailDao.removeByIds(atIds)`.
No subsequent merge of any in-session parent. The audit-trail cleanup is
delegated to the bulk-only path in #12. Clean.
**Tests**: `GeneSetDaoTest` exists.
**Risk**: LOW.

#### 16. `RawAndProcessedExpressionDataVectorDaoImpl.removeByCompositeSequence` — Pattern A (clean)
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/bioAssayData/RawAndProcessedExpressionDataVectorDaoImpl.java`
**Lines**: 68-79.
**Smell**: HQL `delete RawExpressionDataVector` + HQL `delete ProcessedExpressionDataVector`
by composite sequence. No parent merge. Clean.
**Risk**: LOW.

#### 17. `ExpressionExperimentSubSetDaoImpl.remove` — Pattern B
**File**: `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentSubSetDaoImpl.java`
**Lines**: ~130-164.
**Smell**: `super.remove(entity)` (151) followed by `session.delete(ba)` and
`session.delete(bm)` (155, 161). Reversed order vs the bug shape — the parent
is removed FIRST, then orphan children. This is the safe order (no cascade
re-walks a parent that's already removed). `bioAssaysToRemove` and
`samplesToRemove` are computed before `super.remove`, so the collections
shouldn't be stale when the explicit deletes run.
**Risk**: LOW.

## Pattern counts

| pattern | count | files |
|---------|-------|-------|
| A (HQL bulk-delete + merge/update) | 6 | EE-DAO ×4, DEA-DAO ×1, GeneDAO ×1 |
| B (session.delete child + session.update/merge parent) | 3 | DEA-DAO ×1, ArrayDesign ×1, EE-DAO ×1 (overlaps A) |
| C (mutable=false + child collection + cross-tx writes) | 4 | AnalysisResultSet, BioAssayDimension, SingleCellDimension, CellLevelCharacteristics |
| D (cascade=all on parent + bulk-delete on children) | 3 | EE-DAO ×2 (via Investigation.quantitationTypes), DEA-DAO ×1 (DEA.resultSets) |
| E (stale PersistentSet snapshot) | 1 | EE-DAO `replaceRawDataVectors` |

(Sites overlap categories — total unique candidate sites = 10 with risk score,
plus 7 cleanly-coded sites included as defensive references.)

## Canonical safe pattern

For HQL bulk-delete of children whose parent is in-session:

```java
// 1) evict the parent BEFORE the bulk delete, OR clear the in-memory collection
session.evict( parent );
// or:
parent.getChildren().clear();
session.flush();   // synchronise the empty-collection snapshot

// 2) bulk-delete via HQL
session.createQuery( "delete from Child c where c.parent = :p" )
       .setParameter( "p", parent )
       .executeUpdate();

// 3) DO NOT merge/update the parent in the same session
// The bulk-delete is its own DB write; the parent state is consistent on
// next session load. If a parent-update IS needed, do it BEFORE the bulk
// delete (so the merge cascade walks the still-present children), or in a
// separate session/transaction.
```

For collection-clear + cascade-driven delete (safer alternative):

```java
// Load children, mutate, let cascade do the work
Parent p = session.load(Parent.class, id);
p.getChildren().clear();              // session-aware mutation;
// children flush via cascade="all,delete-orphan"
session.flush();                       // synchronise before further ops
```

For `mutable="false"` HBM + child collection (Pattern C): the only correct fix
is the HBM-level change documented in the AuditTrail notable-case — convert
the bag to a true bidirectional `inverse="true"` mapping, OR remove the
`mutable="false"` directive at the parent class level, OR remove the
`<cache>` directive on the child class. None of these is a Phase-3 fixture
fix; flag for a focused HB6-cache-staleness session.

## Recommended follow-up

1. **Don't touch #1-#4 / #5** until the EE-DAO 18-failure notable_case fix
   decision is made — they're the same family, want a coherent fix.
2. **Audit #6 (AnalysisResultSet)** before reading-DEA-results in HB6 prod —
   this is the highest-priority production read path that has NOT been tested
   for the AuditTrail-style cache-staleness symptom.
3. **#7 (ArrayDesign.remove)** — schedule a targeted test of HB6 array-design
   deletion under realistic ee-attachment fan-out before the next AD-removal
   from prod.
4. **#9 / #10 (SingleCellDimension family)** — add cross-tx-write tests
   modelled on the AuditTrail reproduction before single-cell-write traffic
   grows further.
5. **#11 (PCA)** — keep as the canonical reference pattern; reference from
   any future fix for #1-#5.

## Residual reassessment 2026-05-20 (session)

Re-checked items #8 + #11-#17 against current `HEAD`
(`74e4dd1f00`, branch `phase2-acl-migrate`). Audit doc landed at
`6a4d4c8dcc` on 2026-05-19. Checked `git log` on each cited file since
the audit landing — none of the residual files have been touched. The
intervening week's churn (audit migration Phase C, service decomposition,
JUnit 5 cleanup, lucene placeholder fix, `02c87a91ed` AnalysisResultSet
L2-cache drop for #6) did not move the residual-finding code.

| # | risk | file | new verdict |
|---|------|------|---|
| 8 | MEDIUM | `GeneDaoImpl.remove`/`removeAll` | still-deferred |
| 11 | LOW | `PrincipalComponentAnalysisDaoImpl.remove` | still-deferred (mitigation intact) |
| 12 | LOW | `AuditTrailDaoImpl.removeByIds` | still-deferred (still clean) |
| 13 | LOW | `BlacklistedEntityDaoImpl.removeAll` | still-deferred (still clean) |
| 14 | LOW | `Gene2GOAssociationDaoImpl.removeAll` | still-deferred (still clean) |
| 15 | LOW | `GeneSetDaoImpl.removeAll` | still-deferred (still clean) |
| 16 | LOW | `RawAndProcessedExpressionDataVectorDaoImpl.removeByCompositeSequence` | still-deferred (still clean) |
| 17 | LOW | `ExpressionExperimentSubSetDaoImpl.remove` | still-deferred (still safe-order) |

### Per-item notes

- **#8 `GeneDaoImpl`** — re-read: `remove(gene)` does HQL bulk-deletes
  only on tables NOT in Gene's cascading collections (`BioSequence2GeneProduct`,
  `GeneSetMember`, `Gene2GOAssociation`, dummy `GeneProduct`). The actual
  cascading collections (`products`, `aliases`, `accessions` per
  `ChromosomeFeature.hbm.xml:71/78/88`) are handled by the `super.remove(gene)`
  cascade walk, which IS the normal HB-safe path. The original audit's
  "stale collections" concern overstates the risk — those collections are
  NOT bulk-deleted underneath. `removeAll()` does `delete from Gene`
  followed by `delete from PhysicalLocation`; risk only materialises if a
  caller holds session-attached Gene refs concurrently, and there are NO
  in-tree callers of `geneService.removeAll`/`remove` beyond test code
  (`grep -rn` confirmed only `GeneServiceImpl.removeAll` delegates).
  Verdict: keep deferred — the PCA-style fix is small (5-10 lines) but
  the production trigger surface is empty; revisit only if NCBI-gene-reload
  is reactivated.
- **#11 PCA** — `session.evict(entity)` at line 77 + `setEigenValues/Vectors/ProbeLoadings(new HashSet<>())`
  at 83/89/95 + `super.remove(entity)` at 97 still intact. Canonical
  reference; no changes needed.
- **#12 AuditTrailDao.removeByIds** — bulk-only deletes (AuditEvent →
  AuditEventType → AuditTrail), no in-session parent merge follows. Still
  clean.
- **#13 BlacklistedEntityDao.removeAll** — bulk delete BE then DE by id
  list collected pre-delete. Still clean.
- **#14 Gene2GOAssociationDao.removeAll** — bulk delete G2G then
  Characteristic by id list. Still clean. (G2G is `mutable="false"` but
  has no child collections, so Pattern C does not apply.)
- **#15 GeneSetDao.removeAll** — bulk deletes + delegates audit cleanup
  to `auditTrailDao.removeByIds` (#12). No in-session parent merge. Still
  clean.
- **#16 RawAndProcessedDataVectorDao.removeByCompositeSequence** — two
  HQL bulk-deletes, no parent merge. Still clean.
- **#17 EE-SubSet.remove** — `super.remove(entity)` at 151 BEFORE the
  explicit `session.delete(ba)`/`session.delete(bm)` at 155/161; child
  sets `bioAssaysToRemove`/`samplesToRemove` are computed BEFORE
  `super.remove`. Safe order preserved.

### Net

No fixes applied. All 8 residual items remain in their previously
assessed state — the mitigations cited in the original audit are still
in place, no surrounding code has shifted, and #8 (`GeneDaoImpl`) on
closer inspection has a narrower risk surface than the original audit
text suggested. The deferral decision for Gemma 2.0 still stands.

### To discuss next session

1. **#8 GeneDaoImpl** — the original audit's "stale collections walk on
   `super.remove(gene)`" concern can be tightened or downgraded. The
   genuine risk is restricted to `removeAll()`'s bulk-Gene-then-bulk-PhysicalLocation
   ordering when other session callers hold Gene refs; in tree there are
   none. Worth either downgrading to LOW or attaching a HB6 cross-session
   regression guard like the #6 AnalysisResultSet fix.
2. **#6 AnalysisResultSet** — the L2 cache drop landed at `02c87a91ed`
   but no regression test pins the cross-tx-write invariant yet; the
   audit's "highest-priority production read path" status is still open.
   Worth pairing with the EE-DAO 18-failure family fix.

## HitListSize entity cache — decision

The `02c87a91ed` fix dropped the L2 entity cache on
`AnalysisResultSet` and the L2 collection cache on its `hitListSizes`
bag, but the entity-level `<cache usage="read-only"/>` on `HitListSize`
itself (`HitListSize.hbm.xml:8`, plus the matching `L2_CACHES.put` in
`EhcacheConfig.java:153`) was not touched at the time. Revisit the
completeness of the fix.

**Verdict: KEEP the entity cache on `HitListSize`.** Reasoning against
the four-criteria check for the AuditTrail-style stale-empty-bag bug:

1. **Value-like row?** Yes — four primitive columns (`Double thresholdQvalue`,
   `Integer numberOfProbes`, `Direction direction`, `Integer numberOfGenes`).
2. **Unidirectional?** Yes — no back-reference from `HitListSize` to its
   parent `ExpressionAnalysisResultSet`.
3. **Child collections?** No — zero `<set>`/`<list>`/`<bag>` declarations in
   `HitListSize.hbm.xml`. The bag amplifier requires a cached collection
   inside a cached parent; this entity has no collections to begin with.
4. **In-place mutation?** No — `LinearModelAnalyzer.computeHitListSizes`
   (line 240-242) constructs `HitListSize` instances once via the static
   factory; no in-tree caller invokes a setter on an already-persisted
   row or `session.evict(hitListSize)` on it. Removal happens only via
   `cascade="all"` from the parent result-set, which routes through
   `session.delete` and DOES invalidate the L2 entity cache for the
   removed id.

The bug the parent + collection cache caused was "empty bag served to
fresh session for a result-set that has new HitListSize rows in DB".
The entity cache cannot reproduce that symptom: it stores individual
rows by primary key, and primary-key reads of an immutable, never-mutated
entity are the textbook safe case for `<cache usage="read-only"/>`.

HBM-level rationale comment added at `HitListSize.hbm.xml` so the next
auditor doesn't re-litigate this.
