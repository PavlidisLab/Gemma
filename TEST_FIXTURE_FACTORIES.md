# Test fixture factories

Phase 3 (PHASE_3_VISION.md §3) is migrating the test-fixture surface off
`PersistentDummyObjectHelper` (1083 lines, HB5-shaped) onto small typed
factories that are HB6-safe by construction. This document is the
template + naming convention + migration playbook for that work.

## Why we are moving

`PersistentDummyObjectHelper` was the main pain point of the Hibernate 5
→ 6 migration. Roughly 30% of Phase 2 session debug time went to test
fixtures, not production code. Patterns that HB5's persister was
permissive about — replacing collections on managed entities with
freshly-instantiated `HashSet`s, leaning on `persisterHelper`'s
walk-and-findOrCreate semantics, fabricating transient `Taxon`s on the
fly, mixing transient and managed associations across the EE graph —
HB6 refuses outright. The helper still works for the cases it was
shaped for, but extending it for single-cell / spatial / multi-modality
fixtures will hit the same wall.

The factories are also nicer to read:

```java
// before
ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment(arrayDesign);

// after
ExpressionExperiment ee = experimentFactory.bulkRna().withArrayDesign(ad).build();
```

## Template

Every factory follows the same shape:

1. `@Component`, autowired into integration tests directly (no inheritance
   required, no `BaseSpringContextTest` coupling).
2. Top-level methods that start a fluent `Builder`, named after a
   *meaningful default* — `bulkRna()`, `singleCell()`, `cdnaProbe()`,
   `affymetrixGeneChip()`, etc. Each starter pre-loads its builder with
   defaults appropriate to that flavour.
3. `Builder.withXxx(...)` methods that return `this`. Each `withXxx` is a
   pure mutation of the builder; no side effects until `build()`.
4. `Builder.build()` that constructs the entity graph, persists it through
   the **production service layer** (so ACL / audit listeners fire on the
   same code path as real ingestion), and returns the persisted root.

HB6-respecting patterns the factories enforce:

* Mutate existing collections via `getXxx().add(...)`, never replace
  them via `setXxx(new HashSet<>(...))`. (Replacement is the classic
  PersistentSet pitfall on managed entities and is a habit we want to
  kill on the test side.)
* Pre-persist anything that needs an ID before the root is saved (e.g.
  `BioMaterial` before `ExpressionExperiment`, because EE→BA cascades
  but BA→sampleUsed doesn't). Use the entity's own service `create(...)`
  rather than `persisterHelper.persist(...)`.
* Resolve seeded data (taxa, external databases, user groups) from
  services — never `Factory.newInstance()` + set fields and hope the
  persister deduplicates. If the seed isn't there, throw a clear error
  pointing at the missing fixture SQL.

## Naming convention

* Factory file: `<RootEntity>Factory.java` under
  `gemma-core/src/test/java/ubic/gemma/core/util/test/fixture/`.
* Public starter methods named after the meaningful default
  (`bulkRna()`, `singleCell()`, not `create()`, not `default()`).
* Modality / flavour enums declared **on the factory** itself (no
  separate model-level enum); they're test-only.
* `withN()` for counts (`withSamples(50)`), `withXxx(value)` for
  attached entities.

## Migration playbook

1. Find tests calling `PersistentDummyObjectHelper.getTestPersistentXxx`
   (transitively through `BaseSpringContextTest`). Start with the
   simplest call sites — those that take 0–1 args and don't reach back
   into the helper for follow-on entities (`getExperimentalFactors(...)`
   etc.).
2. Inject the factory: `@Autowired ExperimentFactory experimentFactory;`.
3. Replace the call. If the test handed in an AD, use
   `.withArrayDesign(ad)`. Don't try to migrate the helper's downstream
   `addFactor` / `addFactorValue` calls in the same commit — leave
   those on the helper for now and circle back when the matching
   `ExperimentalFactorFactory` lands.
4. Run that single test under `mvn ... -Dit.test=<TestName>`. If it
   passes, commit. If it fails, the diagnostic is usually "the helper
   handed back an EE with X attached that the test relied on";
   document the gap and either add the missing default to the factory
   or add an explicit `.withXxx(...)` in the test.

## What's done

| File | Status |
|---|---|
| `gemma-core/.../fixture/ExperimentFactory.java` | Done — `bulkRna()`, `singleCell()`, `.withSamples(N)`, `.withArrayDesign(ad)`, `.withTaxon(t)`, `.withShortName(s)`, `.includeRawDataQt(boolean)` |
| `gemma-core/.../fixture/ExperimentFactoryTest.java` | Done — 4 tests, defaults + overrides |
| `CuratableValueObjectTest` | Migrated (first migration off the helper) |

## What's planned

* `BioMaterialFactory` — `.withTaxon(t).withCharacteristic(c).build()`.
* `ArrayDesignFactory` — `.withProbes(N).withSequence(true).withTaxon(t).build()`.
* `ExperimentalFactorFactory` — `.categorical().withLevels(2).attachTo(ee)`.
* `FactorValueFactory` — paired with the above.
* `DifferentialExpressionAnalysisFactory` — `.attachTo(ee).withProbes(N)`.
* Single-cell vector population helpers split out of
  `RandomSingleCellDataUtils` into a `SingleCellVectorFactory` callable
  on the output of `experimentFactory.singleCell().build()`.

`PersistentDummyObjectHelper` stays in place throughout this work. It
becomes the fallback for legacy tests; once a critical mass of fixtures
are on the factory pattern we can revisit its retirement.

## Open design question

The current `ExperimentFactory.Builder.build()` still pre-persists
`BioMaterial` explicitly because EE→BA→sampleUsed isn't cascaded. The
better long-term answer is probably to make cascade=ALL or `MERGE`
attach all the way through (lifecycle-owned BMs), or to lean on a
single `EntityManager.persist(rootGraph)` call with HB6's deeper
TransientPropertyResolution support. The factory hides this from
callers either way — but the day we change the persistence shape,
this is the seam to update.
