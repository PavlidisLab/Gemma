# `/datasets/{id}/samples` and `/datasets/{id}/design` perf recce

Baseline: `3de192d8f858ce3e2d64632866d8200238636981` (matched).

Warm-Tomcat / prod-tunnel timings to beat:

| Endpoint | Time |
|---|---|
| `/samples` | 339ms |
| `/design`  | 229ms |

## 1. `/datasets/{id}/samples` (no QT params)

### Call path

`DatasetsWebService#getDatasetSamples` (line 905) → no cursor, no QT →
`DatasetArgService#getSamples(DatasetArg)` (line 218):

```java
ExpressionExperiment ee = service.thawLite( this.getEntity( arg ) );
List<BioAssayValueObject> vos = baService.loadValueObjects( ee.getBioAssays(), null, true, true );
populateOutliers( ee, vos );
return vos;
```

### Where the time goes

1. **`service.thawLite(ee)`** —
   `ExpressionExperimentDaoImpl#thawLite` (line 2499) does `thawLiter(ee)` +
   per-assay `Thaws.thawBioAssay`. `thawLiter` warms quantitation types,
   characteristics, accession+externalDatabase, mean-variance, geeq,
   curation details, otherParts, all experimental factors + factor
   values + types, primary publication + other publications. That's
   ~10+ collection-init queries, **none of which the VO ctor reads**
   (see `BioAssayValueObject` ctor at line 119 — only touches
   `arrayDesignUsed`, `originalPlatform`, `processingDate`, sequence*,
   metadata, numberOfCells*, `accession`, `sampleUsed` → BM graph).
2. **Per-assay `thawBioAssay`** (called inside thawLite for every
   assay) — needed: warms `arrayDesignUsed.designProvider`,
   `originalPlatform.designProvider`, `sampleUsed` →
   `thawBioMaterial(bm, initializeBioAssaysUsedIn=true)` which iterates
   the `sourceBioMaterial` chain initializing `sourceTaxon`,
   `treatments`, `factorValues.experimentalFactor`, and
   `bioAssaysUsedIn`. This is THE bulk per-assay cost and the VO ctor
   really does need it (via `BioMaterialValueObject` walking
   `getAllFactorValues()` / `getAllCharacteristics()` through the
   sourceBioMaterial chain).
3. **`loadValueObjects`** —
   `BioAssayReadServiceImpl#loadValueObjects` (line 129) builds an
   `ad2vo` map via `arrayDesignDao.loadValueObjects(...)` then
   constructs one `BioAssayValueObject` per assay. Each BMV ctor with
   `basic=true, allFactorValuesAndCharacteristics=true` does an
   `LRU` walk of `sourceBioMaterial`, building
   `FactorValueBasicValueObject`s and `CharacteristicValueObject`s
   (line 152 onward).
4. **`populateOutliers`** —
   `OutlierDetectionServiceImpl#getOutlierDetails` (line 52) is cached
   in the `OutlierDetailsCache`, so on warm Tomcat this is a hashmap
   hit + a set-membership loop.

### Bottleneck identification

The "easy" win would be replacing the heavyweight `service.thawLite(ee)`
with the much narrower per-assay `Thaws.thawBioAssay` loop — the
unused-by-VO work in `thawLiter` accounts for ~10 collection-init
queries (publications, otherParts, factors, factor values, types,
QT, characteristics, accession, MV, geeq, curationDetails). That's
the obvious savings on paper.

### Why deferred

- **No proof point.** I don't have a local end-to-end perf rig to
  measure before/after on this worktree, and the cursor-mode sibling
  (which already skips `thawLite`) is a different code path (loads
  assays via fresh HQL, doesn't navigate `ee.getBioAssays()` as a
  lazy collection). A direct replacement here could trigger
  `LazyInitializationException` if any downstream caller of
  `DatasetArgService.getSamples(DatasetArg)` (not just the REST
  handler) was relying on the EE being thaw-lited as a side effect.
- **Two known callers in DatasetsWebService alone** call this code
  path (the `/samples` JSON and the no-QT branch in the larger
  handler). At least one path used to feed downstream code that
  reached into `ee.getExperimentalDesign()` and related.
- The 339ms is on a prod tunnel; the network RTT and the JSON
  serialization of `allFactorValues=true + characteristics` for a
  large assay set is also non-trivial. Without local measurement we
  can't attribute a known fraction to thawLite specifically.

**Recommendation:** convert in a follow-up that lands with a JMH
microbench *or* a before/after timed `curl` against the same dataset
on a warm tunnel.

### Other observations

- `populateOutliers` is cached but the cache is keyed only on EE id —
  no eviction on dataset edit recorded here. Not the perf bottleneck
  (cache hit is microseconds) but a freshness footgun worth a
  separate ticket.

## 2. `/datasets/{id}/design` (JSON variant)

### Call path

`DatasetsWebService#getDatasetDesignJson` (line 2655) →
`DatasetArgService#getExperimentalDesign` (line 284) →
`ExpressionExperimentReadServiceImpl#getExperimentalDesignValueObject`
(line 666):

```java
ee = expressionExperimentDao.reload( ee );
ExperimentalDesign ed = ee.getExperimentalDesign();
if ( ed == null ) return null;
for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
    Hibernate.initialize( ef.getFactorValues() );
    for ( FactorValue fv : ef.getFactorValues() ) {
        Hibernate.initialize( fv.getCharacteristics() );
        if ( fv.getMeasurement() != null ) Hibernate.initialize( fv.getMeasurement() );
    }
}
Hibernate.initialize( ee.getBioAssays() );
for ( BioAssay ba : ee.getBioAssays() ) {
    BioMaterial bm = ba.getSampleUsed();
    if ( bm != null ) Thaws.thawBioMaterial( bm );
}
return new ExperimentalDesignValueObject( ed, ee.getBioAssays() );
```

### Where the time goes

1. `reload(ee)` — one PK lookup. Cheap.
2. **For each ExperimentalFactor, init factorValues; for each FV, init
   characteristics + measurement.** This is N (factors) × M (FVs per
   factor) lazy collection inits. Each FV's `characteristics` init
   is a separate query unless batched.
3. **Per-assay `thawBioMaterial(bm)`** — walks `sourceBioMaterial`
   chain initializing sourceTaxon, treatments,
   factorValues.experimentalFactor on each. **Same per-assay cost as
   the `/samples` path's `thawBioAssay`-driven BM thaw** (without
   `initializeBioAssaysUsedIn=true`).
4. **`new ExperimentalDesignValueObject( ed, ee.getBioAssays() )`** —
   builds the structured VO from the now-thawed graph.

### Bottleneck identification

The N+1 risk on FV `characteristics` is the most likely culprit:
HQL-batched, this is one query per `fv.getCharacteristics()` collection
init. For a dataset with 4 factors × 8 factor values × ~3 characteristics
each = 32 separate collection-init queries just for FV chars.

A genuine fix would be: replace the per-FV `Hibernate.initialize` loop
with a single HQL fetch query (`select fv from FactorValue fv left
join fetch fv.characteristics left join fetch fv.measurement where
fv.experimentalFactor.experimentalDesign = :ed`) that returns all
factor-value rows with their chars + measurement in one round trip,
then assemble the VO graph in memory.

### Why deferred

- This is a structural query rewrite, not a one-line cache-bypass fix.
- The existing test surface for `ExperimentalDesignValueObject` is
  thin — I'd want to add a stable assertion of the produced VO shape
  before touching the load path, otherwise a regression would only
  surface in GemBrow.
- The 229ms is borderline acceptable; the `/samples` (339ms) and
  `/pipelineStatus` (934ms) endpoints offer larger absolute wins.

**Recommendation:** ticket as a separate follow-up; the fix is real but
non-trivial and needs a perf measurement to validate.

## 3. Stop-condition summary

- Neither endpoint has a one-line, low-risk fix.
- The `/samples` thawLite-replacement is *one-line low-risk in shape*
  but *medium-risk in caller blast radius*; needs a measurement to
  justify and a careful audit of callers that might rely on the
  side-effect thaw.
- The `/design` N+1 is a real structural issue but the fix is a
  multi-query rewrite, not a low-risk patch.
- Neither fix is appropriate without a local before/after timing
  rig.

## Files referenced

- `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java`
  (lines 887, 2640, 2677) — handlers; **not modified per task hygiene**.
- `gemma-rest/src/main/java/ubic/gemma/rest/util/args/DatasetArgService.java`
  (lines 218, 284, 435) — service-layer wrappers.
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentDaoImpl.java`
  (lines 2499, 2505) — `thawLite` / `thawLiter`.
- `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentReadServiceImpl.java`
  (line 666) — `getExperimentalDesignValueObject`.
- `gemma-core/src/main/java/ubic/gemma/persistence/util/Thaws.java`
  — `thawBioAssay` / `thawBioMaterial`.
- `gemma-core/src/main/java/ubic/gemma/model/expression/bioAssay/BioAssayValueObject.java`
  (line 119) — VO ctor used in `/samples`.
- `gemma-core/src/main/java/ubic/gemma/model/expression/biomaterial/BioMaterialValueObject.java`
  (line 152) — BMV ctor used in `/samples`.
- `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/OutlierDetectionServiceImpl.java`
  (line 52) — outlier cache (already fast on warm Tomcat).
