# Slow-survivor perf probe — 2026-05-23

Per `SLOW_SWEEP_FINDINGS_2026_05_23.md` § "What's NOT actionable": the 71 slow-tagged classes that ran green are evidence of nothing on their own. This doc sorts them by wall-clock and classifies the top tier.

## Inputs

- Surefire reports: `.../agent-slow-sweep/gemma-core/target/surefire-reports/TEST-*.xml`
- Failsafe reports: `.../agent-slow-sweep/gemma-core/target/failsafe-reports/TEST-*.xml`
- Run config: `mvn verify -DexcludedGroups=network` (network excluded, `slow` pulled in), tip `304a2ff854`.

Wall-clock is `<testsuite time="…">`. All entries below have `errors=0, failures=0, skipped=0` unless noted.

## Top-20 slowest-surviving classes

| # | time (s) | phase | n | class | one-line |
|--:|---------:|------:|--:|------|----------|
| 1  | 213.61 | IT | 2  | `core.loader.expression.singleCell.MexSingleCellDataLoaderTest` | `testGSE141552` alone burns 187 s — MEX read of an unsliced classpath fixture. |
| 2  | 159.54 | SF | 15 | `core.loader.expression.geo.service.GeoBrowserTest` | Two methods (`testGSE97948` 75 s, `testGSE8579` 52 s) — GEO MINiML fetch + parse via cached classpath SOFT. |
| 3  | 150.82 | SF |  1 | `core.loader.expression.geo.GeoTermReplacementTest` | Single test method — replays a large GEO characteristic-term rewrite over fixture. |
| 4  | 148.89 | SF | 31 | `core.loader.expression.geo.GeoSingleCellDetectorTest` | `testGSE201032` 47 s, `testGSE196516` 33 s, `testGSE109774` 30 s — single-cell-fingerprint sniff on cached SOFT. |
| 5  | 144.06 | IT | 19 | `core.loader.expression.geo.GeoSingleCellDetectorTest` | IT variant — `testGSE125708` 35 s + CXG/SRA probes 26-35 s each. |
| 6  | 110.97 | IT | 13 | `core.loader.expression.geo.singleCell.GeoMexSingleCellDataLoaderConfigurerTest` | 6 methods 12-24 s each — MEX configurer enumerates per-sample MEX dirs in fixture archives. |
| 7  |  69.16 | IT |  2 | `core.analysis.service.CompositeSequenceGeneMapperServiceTest` | Both tests errored (pool exhaustion) — superseded by `RECCE_CSGENEMAPPER_MOCK_GOLDENPATH.md`. |
| 8  |  38.87 | IT |  1 | `core.analysis.singleCell.aggregate.SingleCellExpressionExperimentCreateSubSetsAndAggregateServiceTest` | `testRedoAggregate` 7 s but BeforeEach/context ~32 s; single-cell aggregation IT. |
| 9  |  38.30 | IT | 11 | `core.loader.expression.geo.service.GeoDatasetServiceTest` | `testFetchAndLoadGSE1133` 19 s — full GEO-to-EE pipeline over cached fixture. |
| 10 |  29.16 | IT | 33 | `persistence.service.expression.experiment.ExpressionExperimentServiceIntegrationTest` | 33 tests, none >2.3 s — broad EE service surface; cost is per-test context setup, not any one hotspot. |
| 11 |  28.57 | IT |  2 | `core.analysis.preprocess.SplitExperimentTest` | `testSplitGSE17183ByOrganismPart` 19 s — splits a real (cached) GSE by factor. |
| 12 |  22.25 | IT |  1 | `core.ontology.providers.UberonOntologyTest` | One method — full Uberon OWL load. |
| 13 |  20.71 | IT |  4 | `core.analysis.preprocess.MeanVarianceServiceTest` | `testServiceCreateCountData` 13 s — real mean-variance calc on a count matrix. |
| 14 |  20.50 | IT |  2 | `persistence.service.FilteringVoEnabledServiceIntegrationTest` | Two tests 9-11 s; exercises every `Filter` predicate across every VO-enabled service. |
| 15 |  19.97 | IT |  3 | `core.loader.expression.DataUpdaterTest` | Errored (QT collision) — superseded by `RECCE_DATAUPDATER_FAST_UNIT_TEST.md`. |
| 16 |  19.51 | IT |  7 | `core.loader.expression.singleCell.SingleCellDataTransformationsTest` | Seven methods 1.6-5.7 s — single-cell pack/sort/transpose over h5ad/MEX fixtures. |
| 17 |  13.83 | SF |  8 | `core.loader.expression.singleCell.AnnDataSingleCellDataLoaderTest` | Loads `GSE216457.h5ad` + `GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad` from classpath. |
| 18 |  13.82 | IT |  4 | `core.loader.expression.ucsc.cellbrowser.UcscCellBrowserUtilsTest` | UCSC cellbrowser metadata parse; classpath JSON fixtures. |
| 19 |  11.39 | SF |  7 | `core.security.authorization.acl.AclLinterServiceTest` | `BaseDatabaseTest5` + ACL fixture seed; cost is in context/seed, not the seven asserts. |
| 20 |  10.58 | SF |  6 | `core.loader.expression.sra.SraFetcherTest` | SRA XML parse of cached fixtures. |

Honorable mention: `AclSemanticsContractTest` (9.47 s SF for **106** tests = 89 ms/test) — already optimal density, no probe needed.

## Per-class classification (top-10)

| # | class | classification | suspected hotspot / fixture |
|---|-------|----------------|-----------------------------|
| 1 | `MexSingleCellDataLoaderTest` | **FIXTURE BLOAT** | `testGSE141552` 187 s — the MEX fixture for GSE141552 is full-size. Covered by `RECCE_MEX_LOADER_CHOP.md`. Apply the chop pattern: trim `matrix.mtx` rows + first-N barcodes/features. |
| 2 | `GeoBrowserTest` | **FIXTURE BLOAT** (cached MINiML XML) | `testGSE97948` 75 s + `testGSE8579` 52 s — MINiML is the *full* GEO record (1000s of characteristics). Cheap chop: trim platform-sample tables to first-N. File:line `gemma-core/src/test/resources/data/loader/expression/geo/GSE97948_*.miniml.xml` (verify). |
| 3 | `GeoTermReplacementTest` | **HEAVY COMPUTE** (term-replacement over real GEO fixture) | Single-method 150 s; classpath fixture replays a large characteristic-term rewrite — likely O(terms × replacements). Profile with `-XX:+FlightRecorder` for one run before chopping. |
| 4 | `GeoSingleCellDetectorTest` (SF) | **FIXTURE BLOAT** (cached SOFT) | Three methods own 110/149 s. Detector parses full SOFT to fingerprint SC; first-N-sample SOFT is sufficient. |
| 5 | `GeoSingleCellDetectorTest` (IT) | **CODE / FIXTURE** (CXG + SRA probes are full archives) | `testHasSingleCellDataInCellXGene` 35 s, `testDownloadSingleCellDataInCellXGeneWithoutACollectionId` 26 s — CXG H5AD + SRA XML. Classpath h5ad slice via `anndata`. |
| 6 | `GeoMexSingleCellDataLoaderConfigurerTest` | **FIXTURE BLOAT** | 6 methods 12-24 s each enumerating per-sample MEX dirs in tar fixtures. Trim per-sample dirs to 1-2 cells; configurer cares about *shape*, not cell count. |
| 7 | `CompositeSequenceGeneMapperServiceTest` | **CODE** (errored — superseded) | See `RECCE_CSGENEMAPPER_MOCK_GOLDENPATH.md`. |
| 8 | `SingleCellExpressionExperimentCreateSubSetsAndAggregateServiceTest` | **CODE** (context init dominates) | Test body 7 s, total 38 s — `BaseIntegrationTest5` context init + single-cell aggregation. Probe: is the SC fixture re-loaded per-method? `@DirtiesContext` audit. |
| 9 | `GeoDatasetServiceTest` | **HEAVY COMPUTE** (real GEO-to-EE) | `testFetchAndLoadGSE1133` 19 s drives the full ingestion pipeline on a cached fixture. Pin a baseline; don't chop — this is end-to-end coverage. |
| 10| `ExpressionExperimentServiceIntegrationTest` | **CODE** (context-per-method, no single hotspot) | 33 tests, all <2.3 s. Cost is `BaseIntegrationTest5` rebuild per `@DirtiesContext`. Probe: drop `@DirtiesContext` where ACL/security state isn't mutated. |

## Action queue (next perf-probe agent)

| class | classification | proposed fix |
|-------|----------------|--------------|
| `MexSingleCellDataLoaderTest` | FIXTURE BLOAT | Execute `RECCE_MEX_LOADER_CHOP.md`. Target: 213 s → <30 s. |
| `GeoBrowserTest` | FIXTURE BLOAT (MINiML) | Trim cached `GSE97948` + `GSE8579` MINiML to first-N samples + first-N characteristics; preserve XML validity (`xmllint` round-trip). |
| `GeoSingleCellDetectorTest` (both phases) | FIXTURE BLOAT (SOFT) | Trim SOFT fixtures for the 4 top-slow GSEs to first-3 samples; detector reads sample headers, not row counts. |
| `GeoMexSingleCellDataLoaderConfigurerTest` | FIXTURE BLOAT (MEX tarballs) | Replace fixture tarballs with 1-cell MEX dirs (`features.tsv` first-100 lines, `matrix.mtx` first-100 entries). |
| `GeoTermReplacementTest` | HEAVY COMPUTE | Profile single 150 s run; if O(n²) over term list, fix the loop. Else split the fixture and pin a baseline. |
| `UberonOntologyTest` | FIXTURE BLOAT (Uberon OWL) | Apply ROBOT module extraction (see `feedback_trim_ontology_with_protege.md`) — extract just the substantia-nigra subhierarchy. Target: 22 s → <3 s. |
| `ExpressionExperimentServiceIntegrationTest` | CODE (per-test context rebuild) | Audit `@DirtiesContext`; drop where ACL state isn't mutated. Cross-ref `BaseIntegrationTest5`. |
| `MeanVarianceServiceTest.testServiceCreateCountData` | HEAVY COMPUTE | 13 s real mean-variance over count matrix — pin baseline, don't chop (this IS the regression guard). |
| `SplitExperimentTest.testSplitGSE17183ByOrganismPart` | HEAVY COMPUTE | 19 s — pin baseline; covered by perf-priority hotspot list. |
| `AclLinterServiceTest` | CODE (DB seed dominates) | Profile `@BeforeEach` — likely ACL seed pattern from `BaseDatabaseTest5`. Cross-ref `AclSemanticsContractTest` (106 tests in 9 s) for a leaner seed shape. |

## Cross-references

- `RECCE_MEX_LOADER_CHOP.md` — covers item #1 (top).
- `RECCE_CSGENEMAPPER_MOCK_GOLDENPATH.md` — covers item #7 (skip here; agent in flight).
- `RECCE_DATAUPDATER_FAST_UNIT_TEST.md` — covers item #15 (skip here; agent in flight).
- `RECCE_VECTOR_RETRIEVAL_NPLUS1.md`, `RECCE_VISUALIZATION_PERF.md`, `RECCE_DEA_RETRIEVAL_NPLUS1.md` — Paul's perf hotspots; none of the top-20 slow survivors hit these surfaces directly (the SC + GEO loaders dominate slow-tag time, not the retrieval/viz/DEA paths). The hotspot probes need their own runs against `gemma-rest`, not the slow-sweep dataset.
- `feedback_fast_tests_playbook.md` — diagnostic ladder for items 2/4/5/6 (cache + trim, preserve `@Tag("integration")` variant).
- `feedback_trim_ontology_with_protege.md` — item #12 (Uberon).

## What's NOT in this list

`testGSE201032` (47 s inside item #4) and similar single-method outliers — the class-level wall-clock already surfaces them. The next agent should pull per-`<testcase>` times when scoping a specific chop; the slow-sweep XMLs have them.

Three other slow-tagged classes that ran without error and clock 10-20 s (`DataUpdaterTest`, `SingleCellDataTransformationsTest`, `AnnDataSingleCellDataLoaderTest`) are FIXTURE-shaped — defer to MEX chop pattern rather than recceing each.
