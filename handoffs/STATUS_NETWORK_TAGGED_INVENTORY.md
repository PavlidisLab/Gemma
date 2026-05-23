# STATUS — `@Tag("network")` test inventory + upstream reachability

**Branch**: `fix-failsafe-network-tagged-sweep` off `phase2-acl-migrate` @ `c5695fcbd0`
**Probed**: 2026-05-22
**Phase**: Compile-only inventory (failsafe run pending orchestrator slot).

## Summary

- **50 test classes** carry `@Tag("network")` and/or `@NetworkAvailable(url=…)` and are excluded from the default `mvn verify` (parent pom `excludedGroups` injects `network`).
- **14 unique upstream URLs** referenced across all probes.
- **14/14 reachable** today (all HTTP responses in [200, 226, 302] — `curl --max-time 8`).
- No URL drift detected at this point. When the gemdtest slot is free, the failsafe pass (`mvn verify -DexcludedGroups=slow`) can be run with reasonable confidence the failures (if any) will be Gemma-side, not endpoint drift.

## Reachability probe (2026-05-22)

`curl -sS -o /dev/null -w "%{http_code}" --max-time 8 <url>`. `226` is the FTP-over-curl success code for a directory listing. Anything not in `[200, 226, 301, 302, 401, 403]` would be flagged.

| URL | Code | Status |
|---|---|---|
| `https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi` (`EntrezUtils.ESEARCH`) | 200 | OK |
| `https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi` (`EntrezUtils.ESUMMARY`) | 200 | OK |
| `https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=sra&id=SRX12015965` (`EntrezUtils.EFETCH`) | 200 | OK |
| `https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi` | 200 | OK |
| `https://www.ncbi.nlm.nih.gov/geo/browse/` | 200 | OK |
| `https://dtd.nlm.nih.gov/ncbi/pubmed/out/pubmed_190101.dtd` | 200 | OK |
| `https://dtd.nlm.nih.gov/ncbi/pubmed/out/pubmed_180101.dtd` | 200 | OK |
| `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` | 226 | OK |
| `ftp://ftp.ncbi.nlm.nih.gov/pub/HomoloGene/last-archive/homologene.data` | 226 | OK |
| `ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/SMDB/E-SMDB-1853/E-SMDB-1853.sdrf.txt` | 226 | OK |
| `https://api.cellxgene.cziscience.com` | 302 | OK (redirect) |
| `http://purl.obolibrary.org/` | 302 | OK (redirect) |
| `https://raw.githubusercontent.com/PavlidisLab/TGEMO/master/TGEMO.OWL` | 200 | OK |
| `https://gemma.msl.ubc.ca/rest/v2` | 200 | OK |

## Test-class inventory

50 classes total. Grouped by primary upstream dependency. `EntrezUtils.ESEARCH/ESUMMARY/EFETCH` resolve to the three `https://eutils.ncbi.nlm.nih.gov/entrez/eutils/*.fcgi` endpoints.

### NCBI Entrez E-utilities (ESEARCH/ESUMMARY/EFETCH) — 31 classes

These touch eutils mainly because their Spring contexts auto-wire taxon / gene loaders that probe NCBI at init, or they call PubMed/SRA fetch helpers directly.

| Test class | URL | Probe purpose |
|---|---|---|
| `analysis/expression/diff/BaselineDetectionTest` | `ESEARCH` | diffex baseline w/ live taxon load |
| `analysis/expression/diff/ContinuousVariableDiffExTest` | `ESEARCH` | diffex continuous factor |
| `analysis/expression/diff/DiffExTest` | `ESEARCH` (×2) | diffex end-to-end |
| `analysis/expression/diff/DiffExWithInvalidInteractionTest` | `ESEARCH` | diffex interaction validation |
| `analysis/expression/diff/LowVarianceDataTest` | `ESEARCH` | diffex low-variance guard |
| `analysis/preprocess/batcheffects/BatchInfoPopulationServiceIntegrationTest` | `ESEARCH` (×2) | batch-info populate |
| `analysis/preprocess/batcheffects/ExpressionExperimentBatchCorrectionServiceTest` | `ESEARCH` | batch correction |
| `analysis/preprocess/batcheffects/RNASeqBatchInfoPopulationTest` | `ESEARCH` (×3) | RNA-seq batch info |
| `analysis/preprocess/MeanVarianceServiceTest` | `ESEARCH` (×3) + `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` | mean-variance compute |
| `analysis/preprocess/ProcessedExpressionDataCreateServiceTest` | `ESEARCH` | processed-vector build |
| `analysis/preprocess/SplitExperimentTest` | `ESEARCH` (×2) | split EE by factor |
| `analysis/preprocess/svd/SVDServiceImplTest` | `ESEARCH` (×2) | SVD on processed vectors |
| `analysis/preprocess/TwoChannelExpressionDataDoubleMatrixTest` | `ESEARCH` | two-channel matrix build |
| `datastructure/matrix/io/ExperimentalDesignWriterTest` | `ESEARCH` | design writer round-trip |
| `loader/entrez/EutilFetchTest` | `ESEARCH` | raw eutils fetch |
| `loader/entrez/pubmed/PubMedSearchTest` (class-level) | `ESEARCH` | PubMed search |
| `loader/expression/DataUpdaterTest` | `ESEARCH`, `ESUMMARY`, `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` | data-updater multi-source |
| `loader/expression/ExpressionExperimentPlatformSwitchTest` | `ESEARCH` | platform switch |
| `loader/expression/geo/DatasetCombinerTest` | `ESEARCH` (×3) | GEO dataset combiner |
| `loader/expression/geo/fetcher2/GeoFetcherTest` (class-level + method) | `ESEARCH` | GEO new fetcher |
| `loader/expression/geo/service/GeoBrowserTest` | `ESUMMARY` + `https://www.ncbi.nlm.nih.gov/geo/browse/` | GEO browser scrape |
| `loader/expression/geo/service/GeoDatasetServiceTest` (class-level) | `ESEARCH` | GEO dataset service |
| `loader/expression/sra/SraFetcherTest` (class-level + 2 methods) | `ESEARCH`, `EFETCH?db=sra&id=SRX12015965` | SRA fetcher |
| `persistence/.../ProcessedExpressionDataVectorServiceTest` | `ESEARCH` | vector service (live taxon) |

### NCBI GEO FTP / acc.cgi — 9 classes

| Test class | URL | Probe purpose |
|---|---|---|
| `loader/expression/arrayDesign/ArrayDesignSequenceAlignmentandMappingTest` | `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` | array-design seq mapping |
| `loader/expression/arrayDesign/ArrayDesignSequenceProcessorFastacmdTest` | `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` | fastacmd processor |
| `loader/expression/geo/GeoConverterTest` | `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` | GEO SOFT → Gemma convert |
| `loader/expression/geo/GeoSingleCellDetectorTest` | `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` (class-level + 20 methods) | GEO single-cell detect |
| `loader/expression/geo/singleCell/GeoMexSingleCellDataLoaderConfigurerTest` | `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` | MEX loader configurer |
| `loader/expression/singleCell/MexSingleCellDataLoaderTest` | `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` (×2) | MEX loader |
| `loader/expression/singleCell/transform/SingleCell10xMexFilterTest` | `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` (×3) | 10x MEX filter |
| `persistence/.../RawAndProcessedExpressionDataVectorServiceGeoTest` | `ftp://ftp.ncbi.nlm.nih.gov/geo/series/` | raw+processed vec via GEO |
| `loader/entrez/pubmed/ExpressionExperimentBibRefFinderTest` | `https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi` (×2) | EE → PubMed via acc.cgi |
| `loader/expression/geo/service/GeoBrowserServiceTest` | `https://www.ncbi.nlm.nih.gov/geo/browse/` (×2) | GEO browser scrape |

### NCBI HomoloGene FTP — 1 class

| Test class | URL | Probe purpose |
|---|---|---|
| `loader/genome/gene/ncbi/homology/HomologeneServiceTest` | `ftp://ftp.ncbi.nlm.nih.gov/pub/HomoloGene/last-archive/homologene.data` | HomoloGene archive fetch |

### NLM PubMed DTD — 1 class

| Test class | URL | Probe purpose |
|---|---|---|
| `loader/entrez/pubmed/PubMedXMLParserTest` | `https://dtd.nlm.nih.gov/ncbi/pubmed/out/pubmed_{190101,180101}.dtd` | DTD-validated XML parse |

### EBI ArrayExpress — 1 class

| Test class | URL | Probe purpose |
|---|---|---|
| `loader/expression/arrayExpress/SDRFFetcherTest` | `ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/SMDB/E-SMDB-1853/E-SMDB-1853.sdrf.txt` | SDRF fetch |

### CELLxGENE — 4 classes

| Test class | URL | Probe purpose |
|---|---|---|
| `loader/expression/cellxgene/CellXGeneAnnDataSingleCellDataLoaderTest` | (bare `@NetworkAvailable` — CZI API by usage) | CxG h5ad loader |
| `loader/expression/cellxgene/CellXGeneConverterTest` | `https://api.cellxgene.cziscience.com` | CxG → Gemma convert |
| `loader/expression/cellxgene/CellXGeneDataLoaderServiceTest` | `https://api.cellxgene.cziscience.com` | CxG loader service |
| `loader/expression/cellxgene/CellXGeneFetcherTest` | `https://api.cellxgene.cziscience.com` | CxG fetcher |

### OBO Foundry / ontology — 2 classes

| Test class | URL | Probe purpose |
|---|---|---|
| `loader/expression/geo/GeoTermReplacementTest` | `http://purl.obolibrary.org/` | OBO purl resolver |
| `ontology/providers/GemmaOntologyServiceTest` | `https://raw.githubusercontent.com/PavlidisLab/TGEMO/master/TGEMO.OWL` | TGEMO OWL fetch |

### Misc — 4 classes

| Test class | URL | Probe purpose |
|---|---|---|
| `loader/expression/geo/GeoConverterTest2` | bare `@NetworkAvailable` (GEO via FTPClientFactory in `setUp`) | parameterized GEO convert |
| `loader/expression/geo/service/GeoBrowserServiceParseTest` | bare `@NetworkAvailable` (3 methods) | GEO browser parse |
| `loader/expression/ucsc/cellbrowser/UcscCellBrowserUtilsTest` | bare `@NetworkAvailable` (4 methods) | UCSC cell browser scrape |
| `loader/entrez/pubmed/PubMedXMLFetcherTest` | bare `@NetworkAvailable` (4 methods, class also `@Tag("pubmed")` `@Tag("slow")`) | PubMed XML fetch |
| `util/GemmaRestApiClientTest` | `https://gemma.msl.ubc.ca/rest/v2` | live Gemma REST API |
| `util/test/ExternalUrlReachabilityTest` | n/a — meta-probe of own curated `ENDPOINTS` list | dashboard producer |

Bare `@NetworkAvailable` (no `url=`) tests don't actually probe via the extension's URL check; they rely on the actual upstream call to fail naturally if the host is unreachable. They're still excluded from default `mvn verify` via `@Tag("network")` (or class-level annotation) or by class-level `@Tag("slow")`.

## Followups

1. **Failsafe pass pending.** When the gemdtest slot is free, run:
   ```bash
   mvn -pl gemma-core verify -DexcludedGroups=slow \
       -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) \
       -Dgemma.hibernate.hbm2ddl.auto=create
   ```
   then update this doc with per-test pass/fail disposition.
2. **Dashboard idea (deferred).** `ExternalUrlReachabilityTest` already emits `target/external-url-reachability.json`. The memory item `project_external_endpoint_reachability_test.md` flags a nightly job + dashboard as the durable solution; not in scope here.
3. **`CellXGeneAnnDataSingleCellDataLoaderTest` is bare-annotated.** If the failsafe pass surfaces flake, consider adding `@NetworkAvailable(url = "https://api.cellxgene.cziscience.com")` at class level to short-circuit when CZI is down.
