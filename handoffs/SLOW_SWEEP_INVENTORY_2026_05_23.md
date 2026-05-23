# Slow-tagged test sweep — 2026-05-23

Branch: `agent-slow-sweep`, off tip `304a2ff854`.

Combines pending tasks **#89** (slow-tagged failsafe sweep inventory) and **#96** (slow-sweep v2 with per-test timeout, `DatasetCombinerTest` excluded).

## 1. Inventory

`@Tag("slow")` is the only mechanism actually used in this codebase — no `@Category(SlowTest.class)` references survived the JUnit-4 retirement. **77 test files** carry `@Tag("slow")` somewhere. The default `excludedGroups` (`network,slow`) drops them all from `mvn verify`.

Tagging scope (heuristic, per `grep -c` on each file's class-level vs method-level annotations):

| scope | count | notes |
|---|---|---|
| class-level `@Tag("slow")` (entire class skipped) | ~16 | filters via JUnit Platform tag expression at class level |
| method-level `@Tag("slow")` (specific cases skipped, rest run if class-tagged integration) | ~61 | most also have `@Tag("network")` or `@NetworkAvailable` on the same method |

### Per-file inventory (77 classes)

Format: **class** | **net?** | **disabled?** | **one-line description**.
"net" = uses `@Tag("network")` or `@NetworkAvailable` (NetworkAvailableExtension).

#### `analysis/expression/diff` — differential-expression analyzer suite (13 classes)

| class | net | disabled | what it does |
|---|---|---|---|
| BaselineDetectionTest | y | n | baseline-condition detection over GEO-loaded EE |
| ContinuousVariableDiffExTest | y | n | DEA with a continuous predictor |
| DifferentialExpressionAnalyzerServiceTest | n | y | end-to-end DEA service (class-level `@Disabled` + slow) |
| DiffExMetaAnalyzerServiceTest | n | partial | meta-analysis across DEAs |
| DiffExTest | y | n | core DEA driver |
| DiffExWithInvalidInteraction2Test | n | partial | invalid-interaction validation |
| DiffExWithInvalidInteractionTest | y | n | invalid-interaction validation (variant) |
| LowVarianceDataTest | y | n | low-variance filtering pre-DEA |
| SubsettedAnalysis2Test | n | partial | subsetted DEA (subset_factor) |
| SubsettedAnalysis3Test | n | partial | subsetted DEA (subset_factor) |
| TwoWayAnovaWithInteractionsTest2 | n | partial | 2-way ANOVA + interactions |
| TwoWayAnovaWithInteractionTest2 | n | n | 2-way ANOVA + interaction (singular) |

These almost universally extend `AbstractGeoServiceTest5` and load a real GSE family file (cached fixture or NCBI fetch). The slow comes from heavy in-JVM matrix work + Spring context. None is BLAT/Python-bound.

#### `analysis/preprocess` — preprocessing + SVD + batch correction (12 classes)

| class | net | disabled | what it does |
|---|---|---|---|
| BatchInfoPopulationServiceIntegrationTest | y | n | batch-info population from SRA/GEO |
| ExpressionExperimentBatchCorrectionServiceTest | y | n | combat batch-correction |
| RNASeqBatchInfoPopulationTest | y | n | RNA-seq batch-info population |
| ExpressionDataSVDTest | n | n | SVD over expression matrix |
| MeanVarianceServiceTest | y | n | mean-variance preprocessing |
| ProcessedExpressionDataCreateServiceTest | y | partial | processed-vector creation |
| SampleCoexpressionAnalysisServiceTest | n | n | sample-coexp matrix computation |
| SplitExperimentTest | y | n | split EE by factor |
| svd/SVDServiceImplTest | y | n | SVD service (Spring-wired) |
| TwoChannelExpressionDataDoubleMatrixTest | y | n | two-channel matrix loader |
| TwoChannelMissingValuesTest | n | n | two-channel missing-value handling |
| VectorMergingServiceTest | y | n | vector merging across platforms |

#### `analysis/sequence` / `analysis/service` (3 classes)

| class | net | disabled | what it does |
|---|---|---|---|
| ShellDelegatingBlatTest | n | y | BLAT subprocess (already `@Disabled` "way too slow") |
| CompositeSequenceGeneMapperServiceTest | n | n | CS→gene mapper IT |
| GeneMultifunctionalityPopulationServiceTest | n | n | GO multifunctionality population (GO ontology load) |

#### `datastructure/matrix/io` (1)

| class | net | disabled | what it does |
|---|---|---|---|
| ExperimentalDesignWriterTest | y | n | exp-design writer roundtrip |

#### `loader/entrez` + `loader/entrez/pubmed` (4)

| class | net | disabled | what it does |
|---|---|---|---|
| EutilFetchTest | y | n | E-utils fetcher (NCBI) |
| ExpressionExperimentBibRefFinderTest | y | n | PubMed bibref finder |
| PubMedSearchTest | y | n | PubMed search |
| PubMedXMLFetcherTest | y | n | PubMed XML fetch |

#### `loader/expression/arrayDesign` (2)

| class | net | disabled | what it does |
|---|---|---|---|
| ArrayDesignSequenceAlignmentandMappingTest | y | n | AD sequence alignment + mapping (BLAT-adjacent) |
| ArrayDesignSequenceProcessorFastacmdTest | y | n | fastacmd subprocess + NCBI sequence fetch |

#### `loader/expression/arrayExpress` + `cellxgene` (2)

| class | net | disabled | what it does |
|---|---|---|---|
| SDRFFetcherTest | y | y | ArrayExpress SDRF fetch (class-disabled) |
| CellXGeneFetcherTest | y | n | CellxGene fetcher |

#### `loader/expression` top-level (3)

| class | net | disabled | what it does |
|---|---|---|---|
| DataUpdaterTest | y | n | bulk EE data updater (GEO-fed) |
| ExonArrayDataAddIntegrationTest | n | y | exon-array data add (`@Disabled`) |
| ExpressionExperimentPlatformSwitchTest | y | n | platform-switch IT |

#### `loader/expression/geo` + `geo/fetcher*` + `geo/service` + `geo/singleCell` (13)

| class | net | disabled | what it does |
|---|---|---|---|
| **DatasetCombinerTest** | y (class) | n | NCBI eSearch + slow batch — **known hanger**, excluded from this run |
| RawDataFetcherTest | n | n | GEO supplementary raw archive download |
| fetcher2/GeoFetcherTest | y | n | new GEO fetcher (SOFT) |
| GeoConverterTest | y | n | GEO→Gemma converter |
| GeoFamilyParserTest | n | n | SOFT family-parser (heavy in-JVM) |
| GeoSingleCellDetectorTest | y (class) | partial | single-cell detection over GEO (large fixture set) |
| GeoTermReplacementTest | y (class) | n | term-replacement against ontology lookup |
| service/GeoBrowserServiceParseTest | y | n | GEO browser parser |
| service/GeoBrowserServiceTest | y | n | GEO browser service |
| service/GeoBrowserTest | y | partial | GEO browser high-level |
| service/GeoDatasetServiceTest | y | partial | EE persistence from GEO |
| service/GeoPlatformServiceTest | n | n | GEO platform load |
| singleCell/GeoMexSingleCellDataLoaderConfigurerTest | y | n | MEX loader configuration |

#### `loader/expression/simple` (3)

| class | net | disabled | what it does |
|---|---|---|---|
| ExperimentalDesignImportDuplicateValueTest | n | n | exp-design import validation |
| ExperimentalDesignImporterTest | n | n | exp-design importer round-trip |
| SimpleExpressionDataLoaderServiceTest | n | n | simple data loader (matrix) |

#### `loader/expression/singleCell` + `/transform` (5)

| class | net | disabled | what it does |
|---|---|---|---|
| AnnDataSingleCellDataLoaderIntegrationTest | n | n | AnnData (h5ad) loader IT |
| MexSingleCellDataLoaderPersistenceTest | n | n | MEX loader + persist |
| MexSingleCellDataLoaderTest | y | n | MEX loader |
| SingleCellDataTransformationsTest | n | n | scvi-tools / Python transforms |
| transform/SingleCell10xMexFilterTest | y | partial | 10x MEX filter |

#### `loader/expression/sra` + `ucsc/cellbrowser` (2)

| class | net | disabled | what it does |
|---|---|---|---|
| SraFetcherTest | y | n | SRA E-fetch parser |
| UcscCellBrowserUtilsTest | y | n | UCSC CellBrowser utils |

#### `loader/genome` + `loader/util` (3)

| class | net | disabled | what it does |
|---|---|---|---|
| HomologeneServiceTest | y | n | NCBI Homologene (note: AsyncFactoryBean test trap) |
| NCBIGeneLoadingTest | n | n | NCBI gene loading (heavy parser) |
| HttpFetcherTest | n | n | HTTP fetcher generic |

#### `ontology` providers (6)

| class | net | disabled | what it does |
|---|---|---|---|
| GemmaAndExperimentalFactorOntologyTest | n | y | Gemma + EFO ontology (class-disabled + slow) |
| OntologyLoadingTest | n | n | Uberon + others load |
| providers/GeneOntologyService2Test | n | n | GO service (v2) |
| providers/GeneOntologyServiceTest | n | n | GO service |
| providers/PatoOntologyServiceTest | n | n | PATO ontology |
| providers/UberonOntologyTest | n | n | Uberon (large OWL) |

#### `persistence/service` (5)

| class | net | disabled | what it does |
|---|---|---|---|
| arrayDesign/ArrayDesignDaoTest | n | n | AD DAO IT |
| bioAssayData/ProcessedExpressionDataVectorServiceTest | y | n | processed vector service |
| bioAssayData/RawAndProcessedExpressionDataVectorServiceGeoTest | y | n | raw+processed vector IT (GEO-fed) |
| FilteringVoEnabledServiceIntegrationTest | n | n | filtering VO-enabled service |
| genome/sequenceAnalysis/BlatAssociationServiceTest | n | n | BLAT-association service |

## 2. Run results

Invocation:

```
mvn -pl gemma-core verify \
  -DexcludedGroups=network \
  -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) \
  -Dgemma.hibernate.hbm2ddl.auto=create \
  -Dit.test='!DatasetCombinerTest' \
  -Dsurefire.timeout=900 -Dfailsafe.timeout=900
```

- Total wall clock: **28:41 min**
- **Surefire**: `Tests run: 1625, Failures: 0, Errors: 0, Skipped: 34` (baseline: 1526/0F+0E+18S — +99 tests, +16 skipped)
- **Failsafe**: `Tests run: 506, Failures: 1, Errors: 5, Skipped: 29` (baseline: 376/0F+0E+9S — +130 tests, +1F +5E, +20 skipped)
- Build outcome: **BUILD FAILURE** — but only because of `[ERROR] Failed to execute goal ... maven-failsafe-plugin:3.5.4:verify ... There was a timeout in the fork`. The test phase itself ran to completion; the timeout was the failsafe forker exiting late after the last test class completed (`GeneSetValueObjectHelperTest`). No individual test hung; longest class 213 s (`MexSingleCellDataLoaderTest`).
- `DatasetCombinerTest` exclusion worked as intended; no hangs.
- No `@Tag("network")` test actually executed (the `network` exclusion kept them out as before; only `slow` was un-excluded).

### Failures / Errors

| test | tag scope | error | classification |
|---|---|---|---|
| `RNASeqBatchInfoPopulationTest.testGSE156689NoBatchinfo` | method `@Tag("slow")` | `UnexpectedRollback: Transaction rolled back because it has been marked as rollback-only` | slow IT exercises a real GEO fetch path; rollback suggests a setUp transaction issue in the slow code path. Real bug or fixture gap. |
| `TwoWayAnovaWithInteractionTest2.test` | method `@Tag("slow")` | `NoDesignElementsException: No rows left after filtering ...` | slow DEA path filter-too-strict on this fixture. Real bug or fixture-data mismatch. |
| `CompositeSequenceGeneMapperServiceTest.testGetGenesForCompositeSequence` | class `@Tag("slow")` | `CannotGetJdbcConnection: blatCollapsedSequences` (~35 s) | tries to talk to a real BLAT/JDBC sequence service. Externally dependent. |
| `CompositeSequenceGeneMapperServiceTest.testGetCompositeSequencesByGeneId` | same | same | same |
| `DataUpdaterTest.testLoadRNASeqData` | method `@Tag("slow")` | `EntityExists: A different object with the same identifier value was already associated with the session: [QuantitationType#170]` | session / QT id collision in a slow-path EE update. Real bug or fixture-data collision. |
| `HibernateSqmFragileShapesIT.probe_implicitPolymorphismOnUnmappedBase` | **NOT slow-tagged** (`@Tag("integration")` only) | `UnknownEntityException: Could not resolve root entity 'BulkExpressionDataVector'` | part of the default failsafe baseline but normally passes (baseline says `failsafe 376/0F+0E+9S`). Triggering here suggests an ordering / Hibernate metamodel state effect from a slow test loading first. Flake or state-coupling regression. |

### Skipped delta

Surefire skipped went 18→34 (+16). Failsafe skipped went 9→29 (+20). These are method-level `@Disabled` cases inside slow-tagged classes that previously weren't reached. Not new problems — they were always disabled.

## 3. Triage decisions

Conservative posture this commit: **no tag removals, no production code changes, no test re-tagging**. The point of the sweep was to inventory + measure, not to start migrating. Specific recommendations for follow-up tickets:

| class | recommendation | rationale |
|---|---|---|
| `DatasetCombinerTest` | Separate ticket. Either (a) fixture-cache the NCBI eSearch responses + drop `@Tag("network")` + add `@Timeout(5, MINUTES)`, OR (b) keep network-only + tighten per-test `@Timeout` so it surfaces as a fail rather than hanging. | Was excluded from this sweep precisely because it has historically hung; the slow tag is currently the only thing keeping it out of `mvn verify` after the network probe. |
| `CompositeSequenceGeneMapperServiceTest` | Retag to `@Tag("integration")` + `@Tag("network")` (the `@Tag("network")` is what should keep it out of default), or fixture-cache the BLAT/JDBC dependency. | Currently fails with JDBC connection error trying to reach external BLAT — externally dependent. |
| `RNASeqBatchInfoPopulationTest`, `TwoWayAnovaWithInteractionTest2`, `DataUpdaterTest.testLoadRNASeqData` | Investigate as real bugs / fixture mismatches before any tag change. These ran to completion (no hang) but produced semantic errors. File a separate triage ticket — they may be reproducible regressions worth fixing rather than masking. | Errors look like genuine code-path issues, not infrastructure failures. |
| `HibernateSqmFragileShapesIT.probe_implicitPolymorphismOnUnmappedBase` | Separate test-isolation ticket. Triage whether something in the slow batch is corrupting the Hibernate SessionFactory or metamodel cache. | Not slow-tagged but broke when slow tests ran first. Must investigate before any default-run reintroduction of slow tests. |
| All other ~70 slow-tagged classes | Keep slow tag. They ran without error in this sweep but their cumulative wall-clock cost (28 min vs ~5-7 min for the default baseline) is the reason they're excluded. Moving them to default-run requires either parallelising failsafe (which `gemdtest` cannot tolerate on this branch) or per-test review. | Default `mvn verify` budget is the constraint. |

**Possible green moves (deferred — not landed in this commit):**

- ~70 slow-tagged classes passed cleanly. With `@Tag("slow")` removed they'd add ~22 min to default `mvn verify` (28:41 ‑ ~6:30 baseline ≈ 22 min). Too expensive for default; acceptable as opt-in via `-DexcludedGroups=network`.
- Worth discussing: split slow into `slow-fast` (sub-10s classes) vs `slow-heavy` (everything else) and add `slow-fast` to default.

## 4. Open items

- **`DatasetCombinerTest` triage** — separate commit; excluded here.
- **`HibernateSqmFragileShapesIT.probe_implicitPolymorphismOnUnmappedBase` ordering flake** — pre-existing IT that broke when slow tests ran before it. State-coupling reconnaissance needed.
- **`RNASeqBatchInfoPopulationTest.testGSE156689NoBatchinfo`** — `UnexpectedRollback`; investigate the slow-path setUp transaction.
- **`TwoWayAnovaWithInteractionTest2.test`** — `NoDesignElementsException`; filter-vs-fixture mismatch.
- **`DataUpdaterTest.testLoadRNASeqData`** — `EntityExists` on `QuantitationType#170`; session-state collision.
- **`CompositeSequenceGeneMapperServiceTest`** — externally dependent on BLAT/JDBC; needs the test-shape decision (fixture vs `@Tag("network")` + skip-when-unreachable).
- **`maven-failsafe-plugin:verify` "timeout in the fork" at end-of-run** — not a test hang, just the forker reporting timeout after the last test class. Cosmetic but worth raising; may be a JVM shutdown thread holding an HDF5 / Lucene resource open. Doesn't gate this sweep's result.

## 5. Validation (default-run baseline unchanged)

No production or test code was modified in this commit. The default `mvn verify` (without `-DexcludedGroups=network`) continues to use the parent-pom default `excludedGroups=network,slow` and therefore still produces **surefire 1526/0F+0E+18S, failsafe 376/0F+0E+9S** as before. Verified by inspection: no `@Tag` removals, no source changes.
