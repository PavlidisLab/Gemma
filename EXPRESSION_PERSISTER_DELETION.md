# ExpressionPersister Final Deletion Tracker

Status as of chunk E5 (Phase 3, 2026-05-18).

## Where we are

- `ExpressionPersister.java` is a 100-LoC strangler-fig delegate (`@Deprecated`).
- All body methods relocated to `EeWriteServiceImpl` (chunk E3).
- 2 hot direct callers cut over to `EeWriteService.create(...)` (chunk E4):
  - `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/service/GeoServiceImpl.java`
  - `gemma-core/src/main/java/ubic/gemma/core/loader/expression/simple/SimpleExpressionDataLoaderServiceImpl.java`
- Spring proxy autowire path verified (commit `0dd5cf599b`).
- The class is annotated `@Deprecated` plus a javadoc deletion contract (chunk E5, this commit).

## Why we are not deleting yet

The persister chain (`ExpressionPersister extends ArrayDesignPersister extends
GenomePersister extends RelationshipPersister extends CommonPersister extends
AbstractPersister`) is still load-bearing for polymorphic
`persisterHelper.persist(Object)` callers. The `doPersist(T, Caches)` dispatch
arm on `ExpressionPersister` is the only place that routes
`ExpressionExperiment`, `BioAssayDimension`, `BioMaterial`, `BioAssay`,
`Compound`, and `ExpressionExperimentSubSet` into `EeWriteServiceImpl`.

Deleting `ExpressionPersister` collapses the chain so that
`PersisterHelperImpl` falls through to `ArrayDesignPersister.doPersist`, which
has no EE branch. Polymorphic callers that pass an EE through the dispatch
table would either silently fall through to a parent persister (wrong) or NPE.

## Remaining work to make the deletion safe

### 1. Migrate the EE-typed polymorphic callers (highest-risk first)

Files that pass `ExpressionExperiment` through `persisterHelper.persist(...)`:

| File | Lines | Notes |
|---|---|---|
| `gemma-core/src/test/java/ubic/gemma/core/util/test/PersistentDummyObjectHelper.java` | 307, 360, 614, 800, 848 | Test fixture factory; calls both `persist(ee)` and `persist(ee, cache)`. Cut over to `EeWriteService.create(...)`. |
| `gemma-core/src/test/java/ubic/gemma/core/analysis/preprocess/TwoChannelMissingValuesTest.java` | 79, 135, 167, 226 | `persist(ee, prepare(ee))` pattern. |

### 2. Migrate the EE-subgraph-typed polymorphic callers

These pass `BioMaterial`, `BioAssay`, `BioAssayDimension`, `Compound`, or
`ExpressionExperimentSubSet` (all owned by the EE dispatch arm):

| File | Lines | Type |
|---|---|---|
| `gemma-web/src/main/java/ubic/gemma/web/controller/expression/experiment/ExpressionExperimentController.java` | 1211 | BioMaterial |
| `gemma-web/src/main/java/ubic/gemma/web/controller/expression/experiment/ExpressionExperimentEditController.java` | 835 | BioMaterial |
| `gemma-core/src/test/java/ubic/gemma/persistence/service/common/auditAndSecurity/curation/CuratableValueObjectTest.java` | 86, 91 | BioMaterial, BioAssay |
| `gemma-core/src/test/java/ubic/gemma/core/util/test/PersistentDummyObjectHelper.java` | 645, 650, 655 | BioAssay, BioMaterial x2 |

### 3. Audit ambiguous-type polymorphic callers

These callers pass typed-but-not-statically-EE values (e.g.
`BioAssaySet`, `Identifiable`, or generic `Object` from a collection)
through `persisterHelper.persist(...)`. Many are NOT EE in practice but the
static signature does not prove it:

- `gemma-core/src/main/java/ubic/gemma/core/analysis/expression/diff/DifferentialExpressionAnalysisHelperServiceImpl.java` lines 51 (Protocol), 57 (BioAssaySet — could be EE at runtime; needs trace).
- `gemma-core/src/test/java/ubic/gemma/core/security/authorization/acl/AclAdviceTest.java:272` — `ExpressionExperimentSet` (not EE itself, but routed via dispatch chain).

### 4. Non-EE callers (safe to leave)

The remaining ~50 call sites pass non-EE types (`ArrayDesign`,
`BibliographicReference`, `Taxon`, `Gene`, `Chromosome`, `BioSequence`,
`GeneProduct`, `BlatResult`, `QuantitationType`) that route through
`ArrayDesignPersister` / `GenomePersister` / `RelationshipPersister` /
`CommonPersister`. These continue to work without `ExpressionPersister` —
their dispatch arms live on the *parent* classes that survive the deletion.

## How to delete `ExpressionPersister` (future session)

When the EE-typed callers above are all migrated to
`EeWriteService.create(...)` or one of its siblings:

1. **Move the EE dispatch arm.** Either:
   - **(A)** Pull the `doPersist(T, Caches)` EE/BioMaterial/BioAssay/etc.
     instanceof chain into `PersisterHelperImpl.persist(Object)` so it runs
     before delegation to `super.doPersist`. This keeps the persister chain
     intact for non-EE types while routing EE entities to
     `EeWriteServiceImpl` directly.
   - **(B)** Delete the dispatch arm entirely. Only viable if every polymorphic
     caller has been migrated (verified by `grep -rn 'persisterHelper\.persist'`
     showing zero EE-typed call sites).
2. **Delete `ExpressionPersister.java`** and update `ArrayDesignPersister` so
   it (or one of its descendants) is the new top of the abstract-persister
   chain that `PersisterHelperImpl` extends.
3. **Verify the dispatch table.** Run the EE-touching integration tests
   (`*ExpressionExperiment*Test`, `*GeoService*Test`,
   `TwoChannelMissingValuesTest`, `PersistentDummyObjectHelper`-driven
   fixtures) against `gemdtest`.
4. **Drop this tracker doc** as part of the deletion commit.

## Estimated effort

- Migrating the EE-typed callers: ~4 hours (mostly mechanical s/persisterHelper.persist(ee/eeWriteService.create(ee/, plus injecting `EeWriteService` where missing).
- Migrating the BioMaterial / BioAssay / BioAssayDimension callers: ~2 hours (need an `EeWriteService` method per type, or expose `EeWriteServiceImpl`'s package-private helpers via a thin public seam).
- Final deletion + dispatch-arm move + IT verification: ~2 hours.

Total: ~1 working day in a focused session, gated on Phase 3 fixture migration
finishing so the `PersistentDummyObjectHelper` migration does not collide with
test-fixture refactors in flight.
