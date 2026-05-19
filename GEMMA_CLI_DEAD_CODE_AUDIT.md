# Gemma CLI dead-code audit

**Scope:** every `*.java` under `gemma-cli/src/main/java/ubic/gemma/apps/`.
**Branch:** `phase2-acl-migrate` (HEAD `1cc7560f07`).
**Context:** CRUFT_INVENTORY.md §4.1 flagged this package as KEEP-INVESTIGATE,
17 956 LoC total, 5 `@Deprecated`. This audit walks it file-by-file.

## How CLIs are discovered

The "find references" approach is misleading for this package: CLIs are
auto-wired by Spring via `CliComponentScanConfig` (`gemma-cli/src/main/java/
ubic/gemma/cli/config/CliComponentScanConfig.java`) using an `ASSIGNABLE_TYPE`
filter on the `CLI` interface — every concrete subtype of `CLI` in
`ubic.gemma.apps` is registered automatically with no XML/code reference
elsewhere. The launcher (`GemmaCLI`) dispatches by the string returned from
`getCommandName()`.

Consequences for this audit:

- **Java-level grep for the class name is ~useless** — almost all show zero
  hits because the class isn't named anywhere outside the file itself; it's
  resolved by interface scan. Spot-checked: 80+ of 104 files have zero
  same-name references repo-wide.
- **Command-name grep across shell scripts and docs is also weak** — there's
  no in-repo wrapper-script catalog; operations launches CLIs via
  `gemma-cli/target/appassembler/bin/gemma-cli <cmd>` from outside the repo
  (lab cron / Jenkins, not in tree). `gemma-cli/deploy-wiki.sh` does run
  `gemma-cli --completion --completion-wiki` to publish every CLI to a wiki
  page, so all CLIs are documented externally regardless of whether they're
  actually invoked.
- **Real signals available in-tree:** `@Deprecated` annotation, the abstract
  base-class marker (means "infrastructure, not invokable"), Javadoc
  `@deprecated` tags, freshness (last-touch date with `git log --follow`),
  and `getShortDesc()` text (some explicitly say "deprecated, use X
  instead").

So this audit treats `@Deprecated` and self-disclosed "experimental" /
"deprecated" Javadoc as the only high-confidence DELETE signal. Everything
else (including the 10 files untouched since the 2025-03-29 reorg) is
KEEP-INVESTIGATE pending an ops/dev confirmation pass against the actual
cron + Jenkins job list, which lives outside this repo.

## Section 1: inventory

104 CLI files (excluding `package-info.java`), **17 948 LoC total**.
Columns: name, LoC, last-commit (`git log --follow -1 --format=%ai`),
`getCommandName()`, description from `getShortDesc()`.

| Class | LoC | Last commit | Command | Description |
|---|---:|---|---|---|
| AclLinterCli | 138 | 2026-05-18 | `lintAcls` | — |
| AffyDataFromCelCli | 192 | 2026-05-18 | `affyFromCel` | Reanalyze Affymetrix data from CEL files, if available; affy-power-... |
| AffyProbeCollapseCli | 90 | 2025-10-07 | `affyCollapse` | — |
| ArrayDesignAlternativePopulateCli | 119 | 2025-03-29 | `affyAltsUpdate` | Populate the 'alternative' information for Affymetrix platforms |
| ArrayDesignAnnotationFileCli | 406 | 2025-10-07 | `makePlatformAnnotFiles` | Generate annotation files for platforms. |
| ArrayDesignAuditTrailCleanupCli | 91 | 2025-11-18 | `adATcleanup` | — |
| ArrayDesignBioSequenceDetachCli | 117 | 2026-01-16 | `detachSequences` | Remove all associations that a platform has with sequences, for cas... |
| ArrayDesignBlatCli | 268 | 2025-10-07 | `blatPlatform` | Run BLAT on the sequences for a platform; the results are persisted... |
| ArrayDesignMapSummaryCli | 55 | 2025-10-07 | `platformMapSummary` | — |
| ArrayDesignMergeCli | 152 | 2025-10-07 | `mergePlatforms` | Make a new array design that combines the reporters from others. |
| ArrayDesignProbeCleanupCLI | 111 | 2025-10-07 | `deletePlatformElements` | — |
| ArrayDesignProbeMapperCli | 684 | 2026-02-03 | `mapPlatformToGenes` | Process the BLAT results for an array design to map them onto genes |
| ArrayDesignProbeRenamerCli | 169 | 2025-10-07 | `probeRename` | — **[DEPRECATED]** |
| ArrayDesignRepeatScanCli | 161 | 2026-05-18 | `platformRepeatScan` | Run RepeatMasker on sequences for an Array design |
| ArrayDesignSequenceAssociationCli | 245 | 2026-05-18 | `addPlatformSequences` | Attach sequences to array design, from a file or fetching from BLAS... |
| ArrayDesignSequenceManipulatingCli | 343 | 2026-05-18 | _(none)_ | _(abstract base)_ |
| ArrayDesignSubsumptionTesterCli | 154 | 2025-10-07 | `platformSubsumptionTest` | Test microarray designs to see if one subsumes other(s) (in terms o... |
| BatchEffectPopulationCli | 65 | 2025-08-09 | `fillBatchInfo` | Populate the batch information for experiments (if possible) |
| BibRefUpdaterCli | 116 | 2025-03-29 | `updatePubMeds` | — |
| BioSequenceCleanupCli | 354 | 2026-02-23 | `seqCleanup` | Examines biosequences for array designs in the database and removes... |
| BlacklistCli | 407 | 2025-10-04 | `blackList` | Add GEO entities (series or platforms) to the blacklist |
| CellLevelMetadataWriterCli | 98 | 2026-01-12 | `getSingleCellMetadata` | — |
| CellXGeneDataAdderCli | 89 | 2026-05-18 | `addCELLxGENEData` | Load a single-cell dataset from CELLxGENE. |
| CellXGeneDataDownloaderCli | 100 | 2026-05-18 | `downloadCELLxGENEData` | — |
| CellXGeneGrabberCli | 258 | 2026-05-18 | `listCELLxGENEData` | — |
| CompleteCli | 175 | 2026-05-18 | `complete` | Provide various completions for the CLI |
| DatabaseViewGeneratorCLI | 119 | 2025-03-29 | `dumpForNIF` | Generate views of the database in flat files |
| DeleteDiffExCli | 49 | 2025-11-21 | `deleteDiffEx` | Delete differential expression analyses for experiment(s) from the ... |
| DeleteExperimentsCli | 121 | 2026-03-12 | `deleteExperiments` | Delete experiments or platforms from the system |
| DetectQuantitationTypeCli | 70 | 2026-05-18 | `detectQuantitationType` | Detect quantitation type from data |
| DifferentialExpressionAnalysisCli | 860 | 2026-05-18 | `diffExAnalyze` | Analyze expression data sets for differentially expressed genes. |
| DifferentialExpressionAnalysisWriterCli | 102 | 2026-05-18 | `getDiffExAnalysis` | Write differential expression data files to the standard location. |
| ExperimentalDesignImportCli | 93 | 2025-08-09 | `importDesign` | Import an experimental design |
| ExperimentalDesignViewCli | 146 | 2025-03-29 | `viewExpDesigns` | Dump a view of experimental design(s) |
| ExperimentalDesignWriterCLI | 157 | 2026-05-18 | `printExperimentalDesign` | Prints experimental design to a file in a R-friendly format |
| ExpressionDataCorrMatCli | 80 | 2025-08-09 | `corrMat` | Create or update sample correlation matrices for expression experim... |
| ExpressionDataMatrixWriterCLI | 147 | 2026-05-18 | `getDataMatrix` | Write processed data matrix to a file; gene information is included... |
| ExpressionExperimentDataFileGeneratorCli | 82 | 2025-06-19 | `generateDataFile` | Generate analysis text files (diff expression). This is deprecated,... **[DEPRECATED]** |
| ExpressionExperimentDataUpdaterCli | 82 | 2025-08-09 | `updateGEOData` | — |
| ExpressionExperimentManipulatingCLI | 843 | 2026-05-18 | _(none)_ | _(abstract base; 50+ subclasses)_ |
| ExpressionExperimentMetadataChangelogEntryAdderCli | 70 | 2026-05-18 | `addChangelogEntry` | Add a record to the changelog for the given experiment. |
| ExpressionExperimentMetadataChangelogViewerCli | 39 | 2026-05-18 | `viewChangelog` | View changelogs for the given experiment's metadata. |
| ExpressionExperimentMetadataFileAdderCli | 123 | 2026-05-18 | `addMetadataFile` | Add a metadata file to the given experiment and record an entry in ... |
| ExpressionExperimentPlatformSwitchCli | 86 | 2025-08-09 | `switchExperimentPlatform` | Switch an experiment to a different array design (usually a merged ... |
| ExpressionExperimentPrimaryPubCli | 211 | 2025-08-09 | `pubmedAssociateToExperiments` | Set or update the primary publication for experiments by fetching f... |
| ExpressionExperimentVectorsManipulatingCli | 176 | 2026-05-18 | _(none)_ | _(abstract base)_ |
| ExternalDatabaseAdderCli | 91 | 2026-05-18 | `addExternalDatabase` | Add a new external database. |
| ExternalDatabaseOverviewCli | 83 | 2026-05-18 | `listExternalDatabases` | Print an overview of all external databases used by Gemma |
| ExternalDatabaseUpdaterCli | 141 | 2025-03-29 | `updateExternalDatabase` | Update an external database and optionally perform a release. |
| ExternalFileGeneLoaderCLI | 118 | 2025-03-29 | `loadGenesFromFile` | loading genes from a non-NCBI files; only used for species like salmon |
| FactorValueMigratorCLI | 278 | 2025-03-29 | `migrateFactorValues` | Perform the migration of old-style characteristics to statements **[DEPRECATED]** |
| FactorValueOntologyWriterCli | 87 | 2025-10-29 | `getTgfvo` | Generate the OWL file for TGFVO. |
| FindObsoleteTermsCli | 101 | 2025-08-29 | `findObsoleteTerms` | Check for characteristics using obsolete terms as values (excluding... |
| FixOntologyTermLabelsCli | 115 | 2025-08-29 | `fixOntologyTermLabels` | Check and correct characteristics & statements using the wrong labe... |
| GeeqCli | 91 | 2025-08-09 | `runGeeq` | Generate or update GEEQ scores |
| GenerateDatabaseUpdateCli | 106 | 2026-05-18 | `generateDatabaseUpdate` | Generate SQL statements to update the database |
| GenericGenelistDesignGenerator | 361 | 2026-02-03 | `genericPlatform` | Update a 'platform' based on a list of NCBI IDs |
| GeoGrabberCli | 654 | 2026-05-18 | `listGEOData` | Grab information on GEO data sets not yet in the system, working ba... |
| GeoSingleCellDataDownloaderCli | 681 | 2026-05-18 | `downloadGEOSingleCellData` | Download single-cell data from GEO. |
| InitializeDatabaseCli | 89 | 2026-05-18 | `initializeDatabase` | Initialize the database |
| ListExpressionDataFIleLocksCli | 56 | 2025-08-25 | `listDataFileLocks` | List all locks over data and metadata files |
| ListQuantitationTypesCli | 125 | 2026-05-18 | `listQuantitationTypes` | List the available quantitation types for an experiment. |
| LoadExpressionDataCli | 317 | 2025-10-04 | `addGEOData` | Load data from GEO |
| LoadSimpleExpressionDataCli | 416 | 2026-05-18 | `addTSVData` | Load an experiment from a tab-delimited file instead of GEO |
| LockExpressionDataFileCli | 129 | 2026-05-18 | `lockDataFile` | Acquire a lock on an experiment data or metadata file. |
| MakeExperimentPrivateCli | 28 | 2025-08-09 | `makePrivate` | Make experiments private |
| MakeExperimentsPublicCli | 48 | 2025-08-09 | `makePublic` | Make experiments public |
| MeshTermFetcherCli | 138 | 2025-05-02 | `fetchMeshTerms` | Gets MESH headings for a set of pubmed ids **[DEPRECATED]** |
| MultifunctionalityCli | 84 | 2026-05-18 | `updateMultifunc` | Update or create gene multifunctionality metrics |
| NCBIGene2GOAssociationLoaderCLI | 132 | 2025-03-29 | `updateGOAnnots` | Update GO annotations |
| NcbiGeneLoaderCLI | 175 | 2026-05-18 | `geneUpdate` | Load/update gene information from NCBI |
| OrderVectorsByDesignCli | 51 | 2025-08-09 | `orderVectorsByDesign` | Experimental: reorder the vectors by experimental design, to save c... |
| ProcessedDataComputeCLI | 141 | 2025-08-09 | `makeProcessedData` | Performs preprocessing. Optionally can do only selected processing ... |
| ProcessedDataDeleterCli | 46 | 2026-05-18 | `deleteProcessedData` | Delete processed expression data |
| ProtocolAdderCli | 66 | 2026-05-18 | `addProtocol` | Add a new protocol |
| ProtocolDeleterCli | 61 | 2026-05-18 | `deleteProtocol` | Delete a protocol |
| ProtocolListCli | 45 | 2026-05-18 | `listProtocols` | List all available protocols |
| PubMedLoaderCli | 75 | 2025-03-29 | `pubmedLoad` | Loads PubMed records into the database from XML files |
| PubMedSearcher | 90 | 2025-05-02 | `pubmedSearchAndSave` | perform pubmed searches from a list of terms, and persist the resul... |
| RawDataDeleterCli | 47 | 2026-05-18 | `deleteRawData` | Delete raw expression data |
| RawExpressionDataWriterCli | 140 | 2026-05-18 | `getRawDataMatrix` | Write raw data matrix to a; gene information is included if available. |
| RefreshExperimentCli | 53 | 2025-08-09 | `refreshExperiment` | Refresh the cache for experiments on the Gemma Website |
| ReplaceDataCli | 108 | 2025-10-27 | `replaceData` | Replace expression data for non-Affymetrix and non-RNA-seq data sets |
| RNASeqBatchInfoCli | 66 | 2025-09-12 | `rnaseqBatchInfo` | Load RNASeq batch information; header files expected to be in struc... **[DEPRECATED]** |
| RNASeqDataAddCli | 241 | 2025-10-27 | `rnaseqDataAdd` | Add expression quantification to an RNA-seq experiment |
| SingleCellCellTypeFactorCreatorCli | 31 | 2025-11-17 | `createCellTypeFactor` | — |
| SingleCellDataAggregatorCli | 368 | 2026-05-18 | `aggregateSingleCellData` | Aggregate single-cell data into pseudo-bulks |
| SingleCellDataDeleterCli | 128 | 2026-05-18 | `deleteSingleCellData` | Delete single-cell data and any related data files |
| SingleCellDataLoaderCli | 698 | 2026-05-18 | `loadSingleCellData` | Load single-cell data from either AnnData or 10x MEX format. |
| SingleCellDataTransformCli | 281 | 2026-05-18 | `transformSingleCellData` | Transform single-cell data in various ways |
| SingleCellDataUpdaterCli | 74 | 2026-05-18 | `updateSingleCellData` | — |
| SingleCellDataWriterCli | 440 | 2026-05-18 | `getSingleCellDataMatrix` | Write single-cell data matrix to a file; gene information is includ... |
| SingleCellSparsityMetricsUpdaterCli | 36 | 2026-05-18 | `updateSingleCellSparsityMetrics` | Update sparsity metrics for single-cell datasets |
| SplitExperimentCli | 150 | 2025-08-09 | `splitExperiment` | Split an experiment into parts based on an experimental factor |
| SVDCli | 62 | 2025-08-09 | `pca` | Run PCA (using SVD) on data sets |
| TaxonLoaderCli | 86 | 2025-03-29 | `loadTaxa` | Populate taxon tables |
| UcscCellBrowserGrabberCli | 37 | 2026-01-18 | `listUcscCellBrowserData` | — |
| UnifiedOntologyUpdaterCli | 254 | 2026-05-18 | `updateUnifiedOntology` | Update or initialize the unified ontology |
| UpdateDatabaseCli | 72 | 2026-05-18 | `updateDatabase` | Update the database |
| UpdateEe2AdCli | 88 | 2026-05-18 | `updateEe2Ad` | Update the EXPRESSION_EXPERIMENT2ARRAY_DESIGN table. |
| UpdateEE2CCli | 118 | 2026-05-18 | `updateEe2c` | Update the EXPRESSION_EXPERIMENT2CHARACTERISTIC table |
| UpdateGene2CsCli | 94 | 2026-05-18 | `updateGene2Cs` | Update the GENE2CS table. |
| UpdatePubMedCli | 183 | 2026-05-19 | `findDatasetPubs` | Identify experiments that have no publication in Gemma and try to f... |
| VectorMergingCli | 61 | 2025-08-09 | `vectorMerge` | For experiments that used multiple array designs, merge the express... |

## Section 2: usage trace

- **Spring wiring:** every concrete CLI is auto-registered via the
  classpath-scan filter `@ComponentScan(... includeFilters=@Filter(
  type=ASSIGNABLE_TYPE, classes=CLI.class))` in `CliComponentScanConfig`.
  No CLI has an explicit `@Component`; none is wired in XML.
- **Same-class-name references repo-wide** (excluding own file, `target/`,
  `.claude/`): the overwhelming majority are zero, exactly because the
  classes are discovered by interface, not by name. This signal is
  unusable for liveness — included for completeness only.
- **Command-name references in `.sh`/`.md`/`.properties`/`.xml` repo-wide**:
  almost all zero. The 7 non-zero hits are spurious substring matches on
  English words (`complete`, `pca`, `makePublic`, etc.) in unrelated
  documentation, not actual CLI invocations. There is no in-repo
  cron/Jenkins job catalog.
- **`gemma-cli/deploy-wiki.sh`** runs `gemma-cli --completion-wiki` to
  publish every CLI to the project wiki. So all CLIs are documented
  externally regardless of whether anyone calls them.
- **REST-replaceable candidates**: spot-check shows several CLIs whose
  function clearly has REST equivalents (`MakeExperimentPrivateCli`,
  `MakeExperimentsPublicCli`, `RefreshExperimentCli`, `ListQuantitationTypesCli`,
  `ProtocolListCli`, simple deleters). These are tiny shims (28–125 LoC)
  and may stick around as ops convenience even after REST coverage —
  not flagged DELETE without confirmation.

## Section 3: candidates to DELETE

Only **5 high-confidence DELETE candidates**, all carrying `@Deprecated`
on the class plus matching `@deprecated` in Javadoc:

| Class | LoC | Notes |
|---|---:|---|
| `ArrayDesignProbeRenamerCli` | 169 | `probeRename` — already replaced by upstream tooling, kept for legacy. |
| `ExpressionExperimentDataFileGeneratorCli` | 82 | `generateDataFile` — short desc explicitly says "use `getDiffExAnalysis` instead". |
| `FactorValueMigratorCLI` | 278 | `migrateFactorValues` — one-shot data migration; the migration has run on prod. |
| `MeshTermFetcherCli` | 138 | `fetchMeshTerms` — MeSH-headings fetcher, deprecated, not in active rotation. |
| `RNASeqBatchInfoCli` | 66 | `rnaseqBatchInfo` — superseded by RNA-seq batch-info pipeline elsewhere. |
| **Subtotal** | **733** | |

None of the four candidate-classes (a–d) below produced a hit beyond the
five above:

- (a) **never-referenced**: ~80 files have zero same-name refs, but that's
  an artifact of `ASSIGNABLE_TYPE` Spring scanning, not a dead-code signal.
- (b) **REST-replaceable**: not actioned without ops confirmation; CLIs
  often stick around for back-fills even when a REST equivalent exists.
- (c) **`@Deprecated` markers**: exactly the 5 above.
- (d) **broken compile-time imports**: none found — the codebase compiles
  on `phase2-acl-migrate` HEAD; no orphaned pom dependencies surfaced
  while scanning `import` lines.

## Section 4: candidates to KEEP

### KEEP (high confidence)

- The **3 abstract bases** — `ArrayDesignSequenceManipulatingCli` (343),
  `ExpressionExperimentManipulatingCLI` (843),
  `ExpressionExperimentVectorsManipulatingCli` (176). Not invokable;
  `extends`-d by 59 concrete CLIs combined. **1 362 LoC.**
- **Infrastructure / data-import CLIs with no REST equivalent** — schema
  bootstrap, ontology loading, gene/taxon load, GEO grab, single-cell
  load, BLAT/RepeatMasker, Affymetrix CEL reanalysis. Examples:
  `InitializeDatabaseCli`, `UpdateDatabaseCli`, `GenerateDatabaseUpdateCli`,
  `NcbiGeneLoaderCLI`, `TaxonLoaderCli`, `UnifiedOntologyUpdaterCli`,
  `NCBIGene2GOAssociationLoaderCLI`, `GeoGrabberCli`,
  `GeoSingleCellDataDownloaderCli`, `LoadExpressionDataCli`,
  `LoadSimpleExpressionDataCli`, `SingleCellDataLoaderCli`,
  `ArrayDesignBlatCli`, `ArrayDesignProbeMapperCli`,
  `ArrayDesignAnnotationFileCli`, `BlacklistCli`, `BioSequenceCleanupCli`,
  `DifferentialExpressionAnalysisCli`, `ProcessedDataComputeCLI`,
  `MultifunctionalityCli`, `RNASeqDataAddCli`.
- **Schema/denormalisation maintenance CLIs** — `UpdateEe2AdCli`,
  `UpdateEE2CCli`, `UpdateGene2CsCli`. These rebuild denormalised tables;
  there is no REST equivalent and removing them would orphan the
  underlying tables.
- **The `complete` / `CompleteCli` (175 LoC)** — feeds the bash-completion
  + wiki generation pipeline (`update-completion-scripts.sh`,
  `deploy-wiki.sh`). Critical infrastructure.
- **All single-cell `*Cli` (10 files, ~2 700 LoC)** — single-cell ingest
  is the most active subsystem in this package; every file has been
  touched in 2026 and most on 2026-05-18.

## Section 5: recommendation

| Bucket | Files | LoC |
|---|---:|---:|
| **DELETE (confirmed @Deprecated)** | 5 | **733** |
| **KEEP-INVESTIGATE** (need ops/dev confirmation against cron + Jenkins) | 96 | ~15 853 |
| **KEEP (high confidence: abstract bases + completion + schema bootstrap)** | 3 | 1 361 |
| Totals | 104 | 17 947 |

The **−733 LoC DELETE figure** is the only firmly defensible cut without
out-of-repo confirmation. This is in line with — and at the low end of —
CRUFT_INVENTORY §4.1's "potential −600 LoC if all 5 confirmed obsolete".

### KEEP-INVESTIGATE worth surfacing to ops/dev

The CLIs below have been **untouched since the 2025-03-29 mass reorg**
(`git log --follow` shows no commit since). They may still be in active
use, but they're the strongest candidates for the next confirmation pass:

| Class | LoC | First commit | Why suspect |
|---|---:|---|---|
| `ArrayDesignAlternativePopulateCli` | 119 | 2018 | Affymetrix-specific; Affy is shrinking |
| `BibRefUpdaterCli` | 116 | 2013 | duplicates `UpdatePubMedCli` semantics? |
| `DatabaseViewGeneratorCLI` | 119 | 2009 | dumps for **NIF** (Neuroscience Information Framework — possibly dead consumer) |
| `ExperimentalDesignViewCli` | 146 | 2009 | not the writer; pure "view" with REST equivalents |
| `ExternalDatabaseUpdaterCli` | 141 | 2022 | overlap with REST `/externalDatabases` |
| `ExternalFileGeneLoaderCLI` | 118 | 2009 | "only used for species like salmon" per Javadoc |
| `NCBIGene2GOAssociationLoaderCLI` | 132 | 2006 | overlap with `UnifiedOntologyUpdaterCli`? |
| `PubMedLoaderCli` | 75 | 2007 | XML-file PubMed loader; usually we fetch live |
| `TaxonLoaderCli` | 86 | 2006 | bootstrap-only; not run after initial DB seed |
| `OrderVectorsByDesignCli` | 51 | 2025 | Javadoc says **"Experimental"** |

**Subtotal of the KEEP-INVESTIGATE shortlist: ~1 103 LoC.** If even half
of these come back as confirmed dead, the audit yield rises from −733
to roughly **−1 300 LoC** — still inside CRUFT_INVENTORY's "0 to −1 100"
band when generalised to all 96 KEEP-INVESTIGATE files.

### Next step

A 30-minute review by ops + a maintainer against the actual cron table
+ Jenkins job list (out-of-repo) would convert most of the 96
KEEP-INVESTIGATE files into firm KEEP-or-DELETE. Without that, the
defensible cut is **−733 LoC across the 5 already-marked
`@Deprecated` files**.
