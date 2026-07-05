# Mock fixture conversion recce — top-N tests to convert

Goal: enumerate Gemma test classes that currently make live network calls AND have a tractable path to "capture once, replay forever" via classpath fixtures, so the integration-test phase also gets fast and resilient.

Baseline SHA: `f3190e696c`. Branch: `recce-mock-fixturization`. Worktree: `.claude/worktrees/agent-mock-recce`.

Reference pattern: commit `c2b2429757` cached 38 SOFT family files under `src/test/resources/data/loader/expression/geo/series/` and rewrote `readSeriesFromGeo()` to prefer classpath. That is the template for every Bucket-A candidate below.

---

## Top 5 picks, ranked

| Rank | Class | Bucket | Est. wall-clock saved | Why this one |
|---|---|---|---|---|
| 1 | `GeoBrowserTest` (per-accession methods only) | B | ~120s of the 314s class total | 8 of its 15 methods are stable per-accession lookups (`testGSE93825`, `testGSE171541`, `testGSE2569`, `testGSE8579`, `testFetchDetailedGeoSeries`, `testFetchDetailedGeoSeriesWithInvalidUtf8Characters`, `testGSE97948`, `testGSE125708`-equivalents) — they fetch one eSummary/MINiML XML per accession. The other 7 (search/recent/all-records) are inherently live and stay `@Tag("integration")`. |
| 2 | `UcscCellBrowserUtilsTest` | A | ~9s | `getDatasets()`, `getDataset("aging-brain")`, `getDatasetDescription("aging-brain")` each fetch a single small JSON. `parseDatasets(URL)` is already split out and the test already has classpath-backed companion methods. Add `parseDatasets(InputStream)` overload + cache the three JSONs. |
| 3 | `GeoConverterTest2` | A | (skipped in current run; ~10-30s when enabled) | 28-accession `@ParameterizedTest` that mirrors the exact `readSeriesFromGeo()` pattern from `c2b2429757`. Pure SOFT fetch → parse → convert. Drop in 28 cached `_family.soft.gz` files in `series/`, swap the helper to prefer classpath. Same recipe as the reference commit. |
| 4 | `DatasetCombinerTest` (eUtils methods only) | B | ~13s of 17s | `testFindGDSGrouping` / `testFindGSEForGDS` / `testFindGSE267` each hit eSummary with one accession-filter query. Each XML response is small + stable. Requires injecting an `EutilFetch`-equivalent into the static `DatasetCombiner.findGDSforGSE` / `findGSEforGDS` (small refactor — pass `Function<String, Document>` or move to instance method). The remaining `testFindGSE13`/etc. already use classpath SOFT. |
| 5 | `PubMedXMLFetcherTest` | A | ~1.3s but ELIMINATES NLM DTD fetch (which intermittently 503s) | 3 stable PMID retrievals (`15173114`, `24850731`, `23865096`). `PubMedSearch` already calls `PubMedXMLParser` on a stream. Cache 3 efetch XMLs + cache the NLM DTDs they reference (already partly cached by `NcbiEntityResolver`). Test wires a stub URL-fetch or constructs `PubMedXMLParser` directly with classpath stream. |

**Total estimated savings if top-5 land**: ~150-170 seconds off the integration-test phase. Bigger win: the failsafe phase becomes resilient to NCBI/UCSC outages — same as how surefire became resilient after `c2b2429757`.

---

## Candidate inventory

Sources scanned:
- `@NetworkAvailable` grep — 47 hits across `gemma-core/src/test/java`.
- Verify-log timings (`bkd8o27ld.output`) — only tests >1s are shown below; faster ones are noise relative to the 41-minute total.

Categories: A = single small file, drop-in classpath replacement; B = needs a small refactor or modest fixture trim; C = inherently live (changing upstream state, full dataset download).

### Top-impact tier (>5s class time)

| Class | Category | External endpoint | Bucket | Loader-side feasibility | Est. saved |
|---|---|---|---|---|---|
| `GeoBrowserTest` | GEO eSummary + MINiML + FTP | NCBI eUtils + ftp.ncbi.nlm.nih.gov | **B (per-accession 8/15) / C (live-search 7/15)** | `GeoBrowserImpl` constructor takes only an API key; XML-fetch happens in a private path. Needs a refactor to inject a `UrlOpener` OR redirect the eUtils URL via system property. Per-accession XML payloads are tiny (~5-50KB). | ~120s of 314s |
| `MexSingleCellDataLoaderTest` | GEO FTP MEX downloads | ftp.ncbi.nlm.nih.gov + GEO query | **C** | The non-integration methods are already classpath-backed (`testGSE224438` uses `data/loader/expression/singleCell/GSE224438/`). Integration methods (`testGSE141552`, `testGSE125708`) need full-MEX downloads — heavy to trim per-test. | 0 (already tagged @integration) |
| `GeoMexSingleCellDataLoaderConfigurerTest` | GEO FTP — full configurer tests | ftp.ncbi.nlm.nih.gov | **C** | Class-tagged `@Tag("integration")`. Each test downloads a MEX dataset to detect filter config. Trimming MEX per-test is non-trivial. | 0 |
| `MexSingleCellDataLoaderPersistenceTest` | DB + MEX classpath | (mostly local) | **A (likely no network)** | Need to verify it's not in the network bucket. Listed at 29s. | unclear |
| `SingleCellDataTransformationsTest` | Python/h5ad transformations | local Python | **(env-bound, not network)** | Listed at 20.6s. Bash-out to Python — not in scope of this recce. | 0 |
| `DatasetCombinerTest` | eUtils + SOFT classpath | NCBI eSummary | **B (3 methods) / Done (rest)** | `findGDSforGSE` / `findGSEforGDS` are `public static` and call `EutilFetch.summary(...)` which itself does HTTP. Needs a small refactor to make `EutilFetch` injectable, OR mock the URL via a JVM-level URLStreamHandler. | ~13s of 17.5s |
| `UberonOntologyTest` | OBO via Uberon download | purl.obolibrary.org | **B** (trim with ROBOT) | Single test; the production code calls `UberonOntologyService.initialize(true, false)` which streams the OWL. Use ROBOT to extract a tiny module containing `UBERON_0000955` + `UBERON_0002038` + `UBERON_0001965` + ancestors, ship as classpath fixture, point `initialize` at a `file://...` URL via the existing override. Follows the `feedback_trim_ontology_with_protege.md` playbook (also referenced in CLAUDE.md). | ~16s |
| `SraFetcherTest` | NCBI SRA eUtils | EFETCH+ESEARCH for SRX/SRP | **B** | `SraFetcher` has `URL.openStream()` inline. To cache: refactor `SraFetcher` to take a `Function<URL, InputStream>` (or use the existing `EutilFetch` pattern), then cache 1 SRX + 1 SRP XML response (each ~50KB-1MB). 6 tests. | ~12s of 13.5s |
| `AnnDataSingleCellDataLoaderTest` | (local h5ad fixtures) | none in non-@NetworkAvailable methods | **N/A** | Verify all methods; if all class-path-backed, this is just slow due to anndata reads — not a candidate. | 0 |
| `GeoFetcherTest` | GEO FTP / GEO query | ftp.ncbi.nlm.nih.gov + acc.cgi | **C** | Class-tagged `@Tag("integration")`. Tests the fetcher itself (verifies the .soft.gz arrives) — caching it defeats the test's purpose. Keep tagged. | 0 |
| `UcscCellBrowserUtilsTest` | UCSC Cell Browser JSON | cells.ucsc.edu | **A** | `parseDatasets(URL)` and `parseDataset(URL)` already split out. Add `parseDatasets(InputStream)` / `parseDataset(InputStream)` overloads (or build a `file://` URL pointing into the classpath). Three small JSONs. | ~9s of 10.2s |
| `AclLinterServiceTest` / `AclSemanticsContractTest` | (DB, not network) | none | N/A | Listed at 9s each but not network-bound. Out of scope. | 0 |
| `PubMedSearchTest` | NCBI eUtils eSearch | EFETCH+ESEARCH | **B (testSearchAndRetrieveByDoi) / C (rest)** | `searchAndRetrieveByDoi("10.1038/s41588-025-02083-8")` resolves to stable PMID `39962241` — cacheable. The other tests assert size of search results — inherently live. | ~2s |
| `GeoBrowserServiceParseTest` | (classpath XML + NLM DTD) | dtd.nlm.nih.gov on first parse | **B (DTD cache)** | Class-level `@NetworkAvailable`; the test classpath-loads the XML but the DOM parser dereferences the DTD. Fix: pre-stage 2-3 DTDs in `NcbiEntityResolver`'s classpath cache (`/data/loader/entrez/dtd/eSummary_*.dtd`). Then drop the @NetworkAvailable. | ~7s of 8s |
| `BioMartEnsemblNcbiFetcherTest` | BioMart REST | www.ensembl.org/biomart/martservice | **C / B for one** | `testGetEnsemblNcibidata` hits BioMart with a query that streams a CSV. The other 2 methods are pure logic. Caching the CSV is feasible (single static response per known taxon) — Bucket B. | ~7s |
| `CellXGeneAnnDataSingleCellDataLoaderTest` | CELLxGENE API + S3 H5AD | api.cellxgene.cziscience.com + S3 | **C** | Full h5ad download. Trim with `anndata` Python — heavy. | 0 |
| `OntologyLoadingTest` | Multiple OBO downloads | OBO foundry | **B (heavy, per ontology)** | Production-config smoke test: initialize MONDO/OBO suite from prod URLs. Trim each via ROBOT — but this defeats the test's purpose (verifying that production URLs return loadable OWL). Keep tagged. | 0 |

### Mid-impact tier (1-5s class time)

| Class | Category | External endpoint | Bucket | Loader-side feasibility | Est. saved |
|---|---|---|---|---|---|
| `GeneOntologyService2Test` | OBO GO load | purl.obolibrary.org/obo/go.owl | B (ROBOT trim) | Similar to Uberon | ~3s |
| `PatoOntologyServiceTest` | OBO PATO load | purl.obolibrary.org/obo/pato.owl | B (ROBOT trim) | Same playbook | ~3s |
| `EutilFetchTest` | NCBI eSummary | EFETCH+ESEARCH | C (smoke) | Single-test "does eUtils work" — keep live. | 0 |
| `PubMedXMLFetcherTest` | EFETCH | EFETCH | **A** | 3 stable PMID retrievals. Most direct mock fixturization candidate — see implementation outline below. | ~1.3s + DTD-fetch elimination |
| `ExpressionExperimentBibRefFinderTest` | GEO query → PubMed | acc.cgi + eUtils | **B** | Stable accession `GSE3023` resolves to a stable PubMed citation. Two-hop fetch; refactor to inject HTTP client or cache both hops. | ~1.5s |
| `HomologeneServiceTest` | NCBI FTP homologene.data | ftp.ncbi.nlm.nih.gov/pub/HomoloGene | B (already trimmed; check) | Already has classpath fixture (`homologene.data` is small). May only be `@NetworkAvailable` due to a reachability probe in setup. | ~2s |
| `CellXGeneConverterTest` | local h5ad + API | api.cellxgene | B (small JSON) | Mix of API calls and parsing. | ~2-3s |

### Already-converted / out of scope

- `GeoSingleCellDetectorTest` — Phase 1 done in `c2b2429757`; remaining methods are inherently end-to-end (Bucket C). NOTE: the 1451s `testGSE109774` is an integration test that should NOT be running in the default verify; double-check its tags.
- `BatchInfoPopulationServiceIntegrationTest` / `RNASeqBatchInfoPopulationTest` — listed @NetworkAvailable but in the verify log they don't appear in the slow tier; probably FTP-reachability gated and ran fast or were skipped.
- `GemmaRestApiClientTest` — Gemma's own production endpoint; testing against prod is the point. Bucket C.
- `GeoTermReplacementTest`, `GemmaOntologyServiceTest` — currently skipped (per verify log).
- `SDRFFetcherTest` — `@Disabled` (broken upstream URL).
- `ArrayDesignSequenceAlignmentandMappingTest`, `ArrayDesignSequenceProcessorFastacmdTest`, `SimpleFastaCmdTest` — these are env-bound (BLAST binaries), not network-bound. Different ladder rung.

---

## Vestigial-annotation cleanup (free wins, zero refactor)

These tests have `@NetworkAvailable` at the class level but the methods that need it have method-level annotations. The class-level annotation should be DROPPED so the offline methods run by default:

- `PubMedXMLParserTest` — class-level `@NetworkAvailable`. Only 2 of 7 methods need DTD fetch (`testParseMulti`, `testParseRetracted`). The other 5 already work fully offline. Patch: drop class-level annotation; keep the two method-level ones. Better: cache the 2 DTDs and drop those too. Bucket A-trivial.
- `GeoBrowserServiceParseTest` — same shape. The XML payloads are already classpath; only the DTD dereference is live. Cache 2-3 NLM DTDs in the `NcbiEntityResolver` classpath bucket → drop class-level `@NetworkAvailable`.

---

## Implementation outlines

### #2 — `UcscCellBrowserUtilsTest`

1. Curl the three small JSONs once and gzip them:
   - `https://cells.ucsc.edu/dataset.json` → `src/test/resources/data/loader/expression/ucsc/cellbrowser/dataset.json` (already exists for the `parseDatasets` test — verify; just reuse).
   - `https://cells.ucsc.edu/aging-brain/dataset.json` → `aging-brain.json` (already exists).
   - `https://cells.ucsc.edu/aging-brain/desc.json` → new fixture `aging-brain-desc.json`.
2. In `UcscCellBrowserUtils.java`, leave `getDatasets()` / `getDataset()` / `getDatasetDescription()` calling the live URL. Add `getDatasets(URL)` / `getDataset(URL, String)` / `getDatasetDescription(URL, String)` overloads that take a base URL — the existing methods become `getDatasets() { return getDatasets(new URL(UCSC_CELL_BROWSER_URL)); }`.
3. In `UcscCellBrowserUtilsTest`:
   - `testGetDatasets` / `testGetDataset` / `testGetDatasetDescription` become two methods each: an offline one calling the new overloads with `getClass().getResource("...")` → no `@NetworkAvailable`, no `@Tag("integration")`; and the existing live-URL one re-tagged `@Tag("integration")`.
4. The `testGetDatasetDataMatrix` method stays `@Tag("integration")` (it hits a 100MB+ matrix endpoint — Bucket C).
5. Verify `mvn -pl gemma-core test -Dtest=UcscCellBrowserUtilsTest` passes and the offline methods run with `@NetworkAvailable` blocked.

### #5 — `PubMedXMLFetcherTest`

1. Curl the three eFetch XMLs once and stash them:
   - `https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=15173114&retmode=xml&rettype=full` → `src/test/resources/data/loader/entrez/pubmed/pubmed_15173114.xml`.
   - Same for `24850731` and `23865096`.
2. Also stash the 2 NLM DTDs referenced in those XMLs (`pubmed_190101.dtd`, `pubmed_180101.dtd`) at `src/test/resources/data/loader/entrez/dtd/...` — `NcbiEntityResolver` already resolves these via classpath lookup; just check it picks them up.
3. In `PubMedSearch.java`, expose a package-private `BibliographicReference parse(InputStream)` (it already exists effectively — just delegate to `PubMedXMLParser.parse`).
4. In `PubMedXMLFetcherTest`, change each test to:
   - Open the classpath XML as an `InputStream`.
   - Call `PubMedXMLParser.parse(is)` directly.
   - Assert on the resulting `BibliographicReference` (the assertions don't change).
5. Drop method-level `@NetworkAvailable`; keep `@Tag("integration")` on a single new method (`testRetrieveLiveSmoke`) that does the live call to make sure the over-the-wire path still works.
6. Verify `mvn -pl gemma-core test -Dtest=PubMedXMLFetcherTest` — offline methods green, live method only runs under integration tag.

---

## Distribution summary

- Total `@NetworkAvailable` candidate classes: **47** (with `NetworkAvailableExtension.java` excluded).
- Of those that actually contribute to verify-phase wall-clock: **~16 classes**.
- Bucket distribution (among the 16):
  - **A — trivial**: 4 classes (`UcscCellBrowserUtilsTest`, `GeoConverterTest2`, `PubMedXMLFetcherTest`, vestigial-annotation cleanup of `PubMedXMLParserTest` and `GeoBrowserServiceParseTest`).
  - **B — medium**: 6 classes (`GeoBrowserTest` per-accession subset, `DatasetCombinerTest` eUtils subset, `UberonOntologyTest`, `SraFetcherTest`, `BioMartEnsemblNcbiFetcherTest`, `ExpressionExperimentBibRefFinderTest`; also `OntologyLoadingTest`/`GeneOntologyService2Test`/`PatoOntologyServiceTest` if ROBOT trims are wanted).
  - **C — keep live**: 6 classes (`MexSingleCellDataLoaderTest`, `GeoMexSingleCellDataLoaderConfigurerTest`, `GeoFetcherTest`, `CellXGeneAnnDataSingleCellDataLoaderTest`, `GeoSingleCellDetectorTest` remaining methods, `GemmaRestApiClientTest`, plus the live-search subsets of `GeoBrowserTest` and `PubMedSearchTest`).

---

## Notes / open questions

- `GeoSingleCellDetectorTest.testGSE109774` ate 1444s of the verify run. That test is `@Tag("integration")` per the Phase 1 commit, but the verify run was failsafe-phase (`mvn verify`), which DOES run `@Tag("integration")`. The 24-minute hang is the dominant cost. Worth a sidebar to either further-trim that fixture or hard-cap the test timeout.
- The reference SOFT-cache pattern in `GeoSingleCellDetectorTest.readSeriesFromGeo()` is exactly what Bucket-A candidates need to mirror — the worktree should reuse that helper shape rather than reinventing the prefer-classpath-with-fallback logic per test.
- `EutilFetch` and the eUtils HTTP plumbing are the chokepoint for several B-bucket conversions. A small upstream refactor — exposing `EutilFetch` as an instance/bean with a swappable `URLStreamFactory` — would unblock `DatasetCombinerTest`, `SraFetcherTest`, and parts of `GeoBrowserTest` in one shot. That refactor is its own follow-up and not part of this recce.

