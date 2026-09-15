# Single-cell filtering inventory

Snapshot: `singlecell-filtering-recce` @ `74e4dd1f0015c375a46eef9b7dca07d884adad8e`

Scope: enumerate every place in Gemma's single-cell pipeline where cells, genes, or assays can be excluded — at import time, at pseudobulk aggregation, and at DEA. Paths are repo-relative.

---

## 1. On import

### 1.1 Common config — `SingleCellDataLoaderConfig`

Path: `gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/SingleCellDataLoaderConfig.java`

This is the superclass shared by all single-cell loader configs. Most fields are about *metadata import* (cell-type assignments, CLCs), not raw filtering — the only field that actually drops cells is `discardEmptyCells`. The rest govern matching/inference behaviour (e.g. drop unmatched cell IDs).

| Parameter | Type | Default | Path / source | Effect |
|---|---|---|---|---|
| `ignoreSamplesLackingData` | bool | `false` | line 34 | MEX-only. Skip per-sample files that are missing/incomplete. |
| `cellTypeAssignmentFile` | `Path?` | `null` | line 43 | If null, no CTA imported from sidecar (loader may still pull one from in-file metadata, e.g. AnnData). |
| `cellTypeAssignmentName/Description/Protocol` | meta | `null` | 51, 57, 65 | Pure naming/provenance; not filtering. |
| `replaceExistingCellTypeAssignment` | bool | `false` | 72 | Overwrite existing CTA with same name. |
| `ignoreExistingCellTypeAssignment` | bool | `false` | 77 | Skip rather than fail if a CTA with same name exists. |
| `otherCellLevelCharacteristicsFile` | `Path?` | `null` | 83 | Optional CLC sidecar; structure same as CTA file. |
| `otherCellLevelCharacteristicsNames/DefaultValues/DefaultValueUris` | `List<String>?` | `null` | 91, 99, 107 | Per-column names/defaults for the CLC sidecar. |
| `replaceExistingOtherCellLevelCharacteristics` | bool | `false` | 114 | |
| `ignoreExistingOtherCellLevelCharacteristics` | bool | `false` | 119 | |
| `inferSamplesFromCellIdsOverlap` | bool | `false` | 129 | Match cells to samples by ID overlap when sample column missing. |
| `useCellIdsIfSampleNameIsMissing` | bool | `false` | 136 | Fall back to barcode-derived sample assignment. Breaks on barcode collision. |
| `ignoreUnmatchedCellIds` | bool | `false` | 142 | If true, cells in the sidecar that don't match any cell in the SingleCellDimension are silently dropped from the CTA/CLC import (NOT from the data). |
| `markSingleCellTypeAssignmentAsPreferred` | bool | `false` | 147 | |
| `preferredCellTypeAssignmentName` | `String?` | `null` | 155 | Overrides the above. |
| `recreateCellTypeFactorIfNecessary` | bool | `true` | 161 | |
| `ignoreCompatibleCellTypeFactor` | bool | `false` | 168 | |
| `preferSinglePrecision` | bool | `false` | 175 | Storage knob — not filtering. |
| `ignoreDataVectors` | bool | `false` | 184 | Skip vector load entirely (curator wants only the CTA/CLC). |
| **`discardEmptyCells`** | `Boolean?` | `null` (loader default: true, MEX) | 194 | **The one universal cell-dropping knob.** If true, cells with zero gene-associated counts are dropped. Currently only honoured by `MexSingleCellDataLoader`. |
| `transformExecutor`, `console` | infra | — | 200, 206 | |

There is **no** UMI/count threshold, no mitochondrial-fraction filter, no doublet-removal flag, no barcode whitelist/blacklist exposed on this config or any of its subclasses. The only "drop low-quality cells" hook is the 10x MEX filter (sec. 1.2).

### 1.2 Per-loader behavior

#### `AnnDataSingleCellDataLoader`
Path: `gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/AnnDataSingleCellDataLoader.java`
Config: `AnnDataSingleCellDataLoaderConfig.java`

Loader-specific knobs:

| Parameter | Default | Notes |
|---|---|---|
| `sampleFactorName` | `null` | Column in `obs` that identifies the sample (auto-detect if null). |
| `cellTypeFactorName` | `null` | Column in `obs` for cell-type labels (auto-detect). |
| `cellTypeUriFactorName` | `null` | Companion URI column. |
| `ignoreCellTypeFactor` | `false` | Skip CTA loading from in-file factor. |
| `unknownCellTypeIndicator` | `null` | String value to treat as "unknown" (auto-detected if not given — see configurer line 173). Cells with this label are tagged `UNKNOWN_CELL_TYPE` in the CTA; **they are kept**, not dropped. |
| `transpose` | `null` (auto) | Whether the matrix orientation needs flipping. |
| **`useRawX`** | `null` (must be set explicitly if `raw.X` exists) | Choose `raw.X` vs `X`. Loader asserts (line 1161) — explicit, no silent default. This is the closest thing to a "useRawX / useFiltered" toggle. |

Wiring: `AbstractAnnDataSingleCellDataLoaderConfigurer.java:201` → `loader.setUseRawX(config.getUseRawX())`. No cell-drop logic in this loader beyond the unknown-CTA tagging.

#### `MexSingleCellDataLoader`
Path: `gemma-core/src/main/java/ubic/gemma/core/loader/expression/singleCell/MexSingleCellDataLoader.java`
Config: `MexSingleCellDataLoaderConfig.java`

| Parameter | Default | Notes |
|---|---|---|
| `allowMappingDesignElementsToGeneSymbols` | `false` | Fallback gene-symbol lookup when ID match fails. |
| **`apply10xFilter`** | `null` (auto-detect) | If true (or auto-detect says "unfiltered 10x"), runs Cell Ranger's `filter-10x-mex.py` Python script (sec. 1.3). If false, no per-cell quality filter. |
| `use10xChemistry` | `null` (auto-detect) | Chemistry id passed to the 10x filter. |
| `useDoublePrecision` | `false` | Storage knob. |
| `discardEmptyCells` (inherited) | `true` (loader-side, see line 92) | Drops barcode rows whose matrix column has zero counts. Applied in `getSingleCellDimension` (line 126). |

Auto-detect for `apply10xFilter`: `AbstractMexSingleCellDataLoaderConfigurer.detectUnfiltered10xData` (lines 124-173) — fires the filter iff (a) data looks like 10x Chromium AND (b) the matrix has empty columns (heuristic: `nonEmptyCellIndices.length < numColumns`).

#### `SeuratDiskSingleCellDataLoader` and `LoomSingleCellDataLoader`
**Not present as Java loaders on this branch.** Only detectors exist:
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/singleCell/SeuratDiskDetector.java`
- `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/singleCell/LoomSingleCellDetector.java`

Implication: Seurat / Loom files are converted to AnnData (presumably via Python transforms in the `transform/` subpackage) and then ingested by `AnnDataSingleCellDataLoader`. Filtering surface = AnnData's surface.

### 1.3 Post-load filters

Between "loader yields vectors" and "vectors persisted to DB" there is **no Java-side filter**. `SingleCellDataLoaderServiceImpl.loadVectors` (line 471) just streams vectors → `addSingleCellDataVectors`. The only quality-filtering knob is upstream, in the loader chain:

- **`SingleCell10xMexFilter`** (`gemma-core/.../singleCell/transform/SingleCell10xMexFilter.java`) — wraps Cell Ranger's `filter-10x-mex.py`. Inputs: `inputFile`, `outputFile`, `genome`, optional `chemistry`. Thresholds (UMI counts, ambient-RNA, etc.) live in the Python/Cell Ranger code, **not in Java**, and are not surfaced through `SingleCellDataLoaderConfig`. Curators cannot tune them from Gemma.
- **`discardEmptyCells`** — see above, MEX only.

There is no gene-level filter at import (no min-cells-per-gene at this stage; that lives in DEA).

---

## 2. Aggregates / pseudobulk

Package: `gemma-core/src/main/java/ubic/gemma/core/analysis/singleCell/aggregate/`

### 2.1 Config — `SingleCellAggregationConfig`
Path: `.../aggregate/SingleCellAggregationConfig.java`

| Parameter | Type | Default | Effect |
|---|---|---|---|
| `mask` | `CellLevelCharacteristics?` | `null` | Categorical mask (category must be `Categories.MASK`). Masked cells are **excluded** from the aggregate; mask is parsed by `SingleCellMaskUtils.parseMask`. |
| `makePreferred` | bool | `false` | Marks the resulting QT as preferred. |
| `adjustLibrarySizes` | bool | `false` | Scales library sizes to reflect source-sample read depth. |
| `includeMaskedCellsInLibrarySize` | bool | `false` | If false (default), masked cells are excluded from the library-size denominator (treated as filtered out). If true, they count toward the denominator. See impl line 191. |
| `fetchSize` | int | `-1` | Streaming/perf knob. |
| `useCursorFetchIfSupported` | bool | `false` | Streaming/perf knob. |

There is **no min-cells-per-group / min-cells-per-sample threshold at the aggregation stage.** Empty/tiny cell-type groups are NOT dropped here — they get an aggregate vector filled with the descriptive's natural output (e.g. `NaN` from log2cpm if library size = 0, see impl line 196 warning).

### 2.2 Subsetting — `SingleCellExperimentSubSetsCreationConfig`
Path: `.../aggregate/SingleCellExperimentSubSetsCreationConfig.java`

| Parameter | Default | Effect |
|---|---|---|
| `ignoreUnmatchedCharacteristics` | `false` | Allow CLC values that don't map to any cell. |
| `ignoreUnmatchedFactorValues` | `false` | Allow factor values that don't appear in the cell-type mapping. |

Both are tolerance flags; they don't drop cells, they only relax error checks during subset assembly.

### 2.3 Aggregation behavior

Implementation: `SingleCellExpressionExperimentAggregateServiceImpl.aggregateVectors` (line 76).

- **Cell-type assignment source.** `aggregateVectorsByCellType` (line 63) requires both the **preferred** single-cell QT AND the **preferred** cell-type assignment to exist; throws `IllegalStateException` otherwise. Curator-set preferredness is the implicit selection knob — there is no parameter to pick a non-preferred CTA from the config.
- **Aggregation function — automatically chosen from QT scale**, not configurable (impl lines 107-123):
  - `LINEAR` / `COUNT` → `SUM`
  - `LOG1P` → `LOG1P_SUM`
  - `LN` / `LOG2` / `LOG10` / `LOGBASEUNKNOWN` → `LOG_SUM`
  - other → `UnsupportedScaleTypeForSingleCellAggregationException`
- Available methods in `SingleCellDataVectorAggregatorUtils.SingleCellAggregationMethod` (line 268): SUM, MEAN, MEDIAN, VARIANCE, STANDARD_DEVIATION, MAX, MIN, COUNT, COUNT_FAST — but `aggregateVectors` only chooses one of three sum-like variants. The other methods are utility-only.
- **log2cpm.** If QT is COUNT and scale is LOG2/LN/LOG10/LOG1P/LINEAR/COUNT, the aggregated output is converted to log2cpm (line 147, 183). This conversion needs a library size; library size is zero → `NaN` filled (line 196, 299).
- Mask applied at impl line 166-171 (parse), 418 (per-cell skip in aggregation), 541 (per-cell skip in normalization), 577-583 (excluded from sparsity metrics).

### 2.4 Cells/genes dropped at aggregate stage

- **Cells:** only those masked by `config.mask` (none if no mask). No min-cell-per-group threshold.
- **Genes:** none. Gene-level filtering doesn't happen here.

---

## 3. DEA

### 3.1 Config — `DifferentialExpressionAnalysisConfig`
Path: `gemma-core/src/main/java/ubic/gemma/core/analysis/expression/diff/DifferentialExpressionAnalysisConfig.java`

| Parameter | Type | Default | Effect |
|---|---|---|---|
| `analysisType` | enum | — | T/F-test family selection. |
| `moderateStatistics` | bool | `true` | empirical-Bayes moderation. |
| `factorsToInclude` | `Set<EF>` | — | |
| `interactionsToInclude` | `Set<Set<EF>>` | — | |
| `persist` | bool | `true` | |
| `subsetFactor` | `EF?` | `null` | Per-level subset analysis. |
| `subsetFactorValue` | `FactorValue?` | `null` | When analyzing an `ExpressionExperimentSubSet`. |
| `ignoreFailingSubsets` | bool | `false` | |
| `useWeights` | bool | `false` | RNA-seq flag. |
| **`minimumNumberOfCellsPerSample`** | `Integer?` | `null` → **100** (`MinimumCellsFilter.DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_SAMPLE`) | Drops a sample (mask its values) if its total cell count < threshold. Only fires when matrix or BioAssay reports a cell count (i.e. aggregated SC data). |
| **`minimumNumberOfCellsPerGene`** | `Integer?` | `null` → **3** (`MinimumCellsFilter.DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_GENE`) | Drops genes whose total cell count across the matrix < threshold. |
| `repetitiveValuesFilterMode` | enum | `AUTODETECT` | Mode of `RepetitiveValuesFilter`. |
| `minimumNumberOfSamplesToApplyRepetitiveValuesFilter` | `Integer?` | `null` → **4** | Don't apply the repetitive-values filter on tiny experiments. |
| `minimumFractionOfUniqueValues` | `Double?` | `null` → **0.3** | Drop a probe if <30% of its values are unique. |
| `minimumVariance` | `Double?` | `null` → **0.01** (`DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_VARIANCE`) | Drop low-variance probes (only when data is log2cpm). |
| `makeArchiveFile` | bool | `true` | |
| `maxAnalysisTimeMillis` | long | `0` | |

### 3.2 DEA filter chain — `DifferentialExpressionAnalysisFilter`
Path: `gemma-core/src/main/java/ubic/gemma/core/analysis/expression/diff/DifferentialExpressionAnalysisFilter.java`

Order is fixed and not configurable:
1. **OutliersFilter** (`gemma-core/.../preprocess/filter/OutliersFilter.java`) — masks (NaN) data for any BioAssay with `getIsOutlier() == true`. No threshold; pure curator-flag-driven. Throws `NoSamplesException` if every sample ends up masked.
2. **MinimumCellsFilter** (`gemma-core/.../preprocess/filter/MinimumCellsFilter.java`) — per-sample and per-gene cell-count thresholds. Only triggered if matrix `getNumberOfCells()` or a BioAssay's `getNumberOfCells()` is populated (i.e. this is an aggregated SC dataset).
3. **RepetitiveValuesFilter** (`gemma-core/.../preprocess/filter/RepetitiveValuesFilter.java`) — drops probes with too few unique values (RANK mode) or below the unique-value-fraction floor (NOMINAL mode). Skipped if sample count < `minimumNumberOfSamplesToApplyFilter`.
4. **LowVarianceFilter** (`gemma-core/.../preprocess/filter/LowVarianceFilter.java`) — **only runs if `QuantitationTypeUtils.isLog2cpm(qt)` is true** (line 130). Otherwise skipped.

Filter is invoked in `LinearModelAnalyzer.java:691`. Each stage that empties the matrix throws — DEA does not silently produce an empty result.

### 3.3 Subset selection

- DEA on a `ExpressionExperimentSubSet` (line 503) — only the assays in the subset go into the model. Subsets are created at aggregation stage (sec. 2) or via curator action.
- `subsetFactor` (config field) splits the experiment into per-level analyses; `subsetFactorValue` pins one level.

### 3.4 Library-size / count-threshold knobs at DEA stage

No explicit min-library-size at the DEA stage. Library size is computed in `LinearModelAnalyzer.getLibrarySizes` (line 705) and used in voom-style weighting but not as an exclusion threshold. A zero library size will surface as `NaN` aggregated vectors (sec. 2) and those will then be caught by MinimumCellsFilter / LowVarianceFilter downstream.

---

## 4. Cross-cutting observations

1. **Same-named parameter, three different defaults.** `minimumNumberOfCellsPerSample` is a constant in `MinimumCellsFilter` (100), exposed as nullable on the DEA config (null → 100 via filter ctor), and effectively zero everywhere else (no min-cells gate at import or aggregation). A curator who assumes "Gemma drops cells with <100 anywhere" will be wrong — that threshold ONLY applies inside the DEA filter, and only when cell counts are present on the matrix/BioAssay.

2. **Quality-filtering surface is asymmetric between formats.** MEX has an auto-detected Cell Ranger quality filter (`apply10xFilter` + `SingleCell10xMexFilter`). AnnData has *nothing* of the sort — if the user uploads unfiltered AnnData, all barcodes flow through. The `discardEmptyCells` knob is documented as common but is only wired on the MEX loader (see `MexSingleCellDataLoader.java:92` vs the absence of any such field on `AnnDataSingleCellDataLoader.java`).

3. **The 10x filter parameters are opaque.** `apply10xFilter` is a yes/no boolean, but the actual thresholds live in `filter-10x-mex.py` (Cell Ranger). There is no Gemma-side surface to tune them, and the audit trail won't record what version of Cell Ranger / what parameters were applied unless someone manually captures that.

4. **No mitochondrial / doublet / per-cell-UMI filtering anywhere in the Java code.** These standard scRNA-seq QC dimensions are entirely absent from Gemma's configuration surface; the assumption is that the upstream submitter / Cell Ranger has already handled them.

5. **Aggregation gives no min-cells-per-group safety net.** A cell type with 1-2 cells in a sample becomes a pseudobulk vector with potentially garbage statistics. DEA's `minimumNumberOfCellsPerSample` (default 100) is the only guard, applied much later.

6. **`useRawX` (AnnData) requires an explicit decision from the curator** (line 1161 asserts) — good — but the analogous decision for MEX (`apply10xFilter`) is auto-detected silently. Inconsistent UX for the same conceptual question ("use raw vs filtered counts").

---

## 5. Open questions

- **Where is the Python-side ingestion (Seurat → AnnData / Loom → AnnData) configured, and does it apply any cell-level filtering before handoff to Gemma?** The Java side only has detectors; the transformations probably live in `gemma-core/.../singleCell/transform/` or in companion Python scripts. If those scripts drop cells (e.g. via Scanpy filter_cells), curators have no Gemma-side knob to opt out.
- **Is there a curator UI surface for any of these knobs, or are they all CLI-only?** The configurer/config classes look CLI/programmatic. A curator-facing pseudobulk UI that doesn't expose `mask` or `adjustLibrarySizes` would be a doc gap.
- **What is the contract for `getNumberOfCells()` on aggregated BioAssays?** `MinimumCellsFilter` (and therefore the DEA cell-count gate) only fires when this is populated. If aggregation doesn't always set it, the cell-count filter silently turns off — risk of inconsistent DEA stringency across datasets.
- **Are there minimum-cells-per-cell-type safety nets at the curator level (e.g. don't pseudobulk if any cell-type-sample combination has <N cells)?** Code says no. Would benefit from a 30-second confirmation that this is intentional.
- **What does `SingleCell10xMexFilter` actually drop?** The Python script (`filter-10x-mex.py`) is not Java-visible from this recce. Need to read that script to fully document the import-time MEX filtering surface.
