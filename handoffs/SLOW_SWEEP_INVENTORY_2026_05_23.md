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

(To be filled in once `mvn -pl gemma-core verify -DexcludedGroups=network -Dit.test='!DatasetCombinerTest' -Dsurefire.timeout=900 -Dfailsafe.timeout=900` completes.)

Run started: 2026-05-23 10:55 local.

## 3. Triage decisions

(To be filled in.)

## 4. Open items

- `DatasetCombinerTest` (class-level `@Tag("slow") @Tag("network")`) — historically hangs the run. Excluded here. Needs its own dedicated triage commit: either (a) fixture-cache the eSearch responses + drop network OR (b) keep network-only + tighten per-test `@Timeout` so it surfaces as a fail rather than hanging the whole run.
