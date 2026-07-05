# baseCode dep audit — post-ontology pull-in

> **STATUS: COMPLETE / superseded (2026-05-19).** The recommendation in this
> doc ("Keep the dep") was overtaken by events. Between this audit and
> `b6acd05c80`, the matrix, math, linearmodels, graphics, util, and
> Configuration subsystems were all ported in-tree (see commit log:
> `basecode-matrix-port`, `basecode-math-linearmodels-port`,
> `basecode-readers-port`, `basecode-util-port` series, and
> `basecode-configuration-port`). At `9f216558d5` (2026-05-19) the
> `baseCode` Maven dep was removed; zero `import ubic.basecode.*` lines
> remain in the source tree. Kept for historical context: the inventory
> + per-package summary below documents what we ported and roughly where
> each piece landed.

**Date:** 2026-05-19
**Branch:** `basecode-dep-audit` off `phase2-acl-migrate` @ `a37a7e73a9`
**Question:** Now that the ontology classes are pulled in-tree, what `ubic.basecode.*` references remain in Gemma, and can we drop `baseCode-1.1.34-RENOVATIONS-SNAPSHOT` from the Maven graph?

**Original TL;DR (now obsolete — actual outcome: dep dropped):** No. **Keep the dep.** baseCode is still the home of three substantial subsystems Gemma relies on (matrix + linear-algebra core, statistics + linear-models, FileTools), with **248 import sites across 142 files**. Pulling these in-tree is a multi-month project, not a session.

---

## 1. Inventory

Search used:

```
grep -rn '^import ubic\.basecode' gemma-core/src gemma-cli/src gemma-rest/src gemma-web/src \
  | grep -v 'ubic.basecode.ontology\.' | sort -u
```

- **248 import lines** remaining (excluding `ubic.basecode.ontology.*`, now in-tree under `ubic.gemma.core.ontology.basecode.*`).
- **142 unique consumer files**.
- Distribution by module: gemma-core 225, gemma-web 16, gemma-cli 7, gemma-rest 0.

### Packages still imported

| Package | Imports | Notes |
|---|---:|---|
| `ubic.basecode.dataStructure.matrix` | 93 | Core `DoubleMatrix` API — the lingua franca of Gemma's analytics. |
| `ubic.basecode.util` | 66 | Mostly `FileTools` (42); also `StringUtil`, `Configuration`, `NetUtils`, `SQLUtils`, `DateUtil`, `RegressionTesting`, `ConfigUtils`. |
| `ubic.basecode.math` | 43 | `DescriptiveWithMissing`, `MatrixStats`, `Rank`, `Constants`, `MathUtil`, `MultipleTestCorrection`, `KruskalWallis`, `CorrelationStats`, `Distance`, `MatrixRowStats`, `MatrixNormalizer`, `StringDistance`, `Stats`. |
| `ubic.basecode.io.reader` | 19 | `DoubleMatrixReader` (18), `StringMatrixReader`. |
| `ubic.basecode.math.distribution` | 9 | `Histogram`. |
| `ubic.basecode.math.linearmodels` | 6 | `DesignMatrix`, `LeastSquaresFit`, `MeanVarianceEstimator`. |
| `ubic.basecode.graphics` | 5 | `MatrixDisplay`, `ColorMatrix`, `ColorMap`. |
| `ubic.basecode.math.metaanalysis` | 2 | `MetaAnalysis`. |
| `ubic.basecode.dataStructure` | 2 | `CountingMap`, `DoublePoint`. |
| `ubic.basecode.math.linalg` | 1 | `SingularValueDecomposition`. |
| `ubic.basecode.io.writer` | 1 | `MatrixWriter`. |
| `ubic.basecode.io` | 1 | `ByteArrayConverter`. |

### Top consumer files (imports per file)

| Imports | File |
|---:|---|
| 8 | `gemma-core/.../analysis/preprocess/batcheffects/ComBat.java` |
| 7 | `gemma-web/.../controller/expression/experiment/ExpressionExperimentQCController.java` |
| 7 | `gemma-core/.../analysis/expression/sampleCoexpression/SampleCoexpressionAnalysisServiceImpl.java` |
| 7 | `gemma-core/.../analysis/expression/diff/LinearModelAnalyzer.java` |
| 6 | `gemma-core/.../analysis/preprocess/batcheffects/ExpressionExperimentBatchCorrectionServiceImpl.java` |
| 5 | `gemma-core/.../visualization/ExperimentalDesignVisualizationServiceImpl.java` |
| 5 | `gemma-core/.../datastructure/matrix/ExpressionDataDoubleMatrix.java` |
| 5 | `gemma-core/.../analysis/preprocess/svd/ExpressionDataSVD.java` |

The single largest consumer is **`ComBat.java`** (8 baseCode imports across matrix, math, and linearmodels) — the in-Gemma port of the ComBat batch-correction algorithm. It's also the densest because the algorithm is matrix-heavy and uses `DesignMatrix` + `LeastSquaresFit` directly.

---

## 2. Per-package summary — what each does and who calls it

### `dataStructure.matrix` (93 imports — Substantial)

baseCode's core matrix API. `DoubleMatrix` is essentially the type system for everything Gemma does to expression data: subset, transpose, row/column views, ragged matrices, integration with Colt under the hood. Used by:

- All preprocessing: `ComBat`, `ExpressionDataSVD`, `MeanVarianceServiceImpl`, `LinearModelAnalyzer`, `SampleCoexpressionAnalysisServiceImpl`, `RepetitiveValuesFilter`.
- All matrix I/O: `ExpressionDataDoubleMatrix`, `ExpressionDataMatrixServiceImpl`, `DiffExAnalyzerUtils`, `DataUpdaterImpl`, `AffyPowerToolsProbesetSummarize`.
- Visualization: `ExperimentalDesignVisualizationServiceImpl`, `ExpressionExperimentQCController`.

Classes used (count): `DoubleMatrix` 56, `DenseDoubleMatrix` 16, `ObjectMatrix` 6, `ObjectMatrixImpl` 4, `AbstractMatrix` 3, `StringMatrix` 2, `DoubleMatrixFactory` 2, `SparseDoubleMatrix` 1, `MatrixUtil` 1, `IntegerMatrix` 1, `DenseDoubleMatrix1D` 1.

**Retirement difficulty: Substantial.** This *is* the API. Replacing it means rewriting Gemma's analytics on top of Colt/EJML/Apache Commons Math directly, plus a long tail of `MatrixUtil` helpers. Multi-month effort, with high regression risk in numerics.

### `math` (43 imports — Substantial)

Statistical primitives:

- `DescriptiveWithMissing` (15) — Colt `DescriptiveStatistics` extended to ignore NaN. Used wherever expression data has missing values, i.e. nearly everywhere.
- `MatrixStats` (8) — row/column means, variances, rank-transform of matrices.
- `Constants` (5) — `Constants.SMALLISH`, `Constants.SMALL`, `Constants.TINY` numeric thresholds.
- `Rank` (4) — fast rank transform with tie correction.
- `MultipleTestCorrection` (2) — Benjamini-Hochberg FDR.
- `KruskalWallis` (2), `MathUtil` (1), `MatrixRowStats` (1), `MatrixNormalizer` (1), `StringDistance` (1), `Stats` (1), `CorrelationStats` (1), `Distance` (1).

**Retirement difficulty: Substantial.** Apache Commons Math has analogues for some, but `DescriptiveWithMissing` and the `Matrix*` helpers are bespoke to the baseCode `DoubleMatrix` type system; they can't be peeled away from the matrix package separately.

### `math.linearmodels` + `math.distribution` + `math.linalg` + `math.metaanalysis` (18 imports — Substantial)

`DesignMatrix`, `LeastSquaresFit`, `MeanVarianceEstimator`, `Histogram`, `SingularValueDecomposition`, `MetaAnalysis`. These are the *interesting* statistical content of baseCode — limma-style linear-model fits, variance modeling, meta-analysis. Used by `LinearModelAnalyzer`, `ComBat`, `SampleCoexpressionAnalysisServiceImpl`, `MeanVarianceServiceImpl`, `ExpressionDataSVD`. **Not feasible** to port in-tree without dragging in `DoubleMatrix` and the math package — they're all one organism.

### `util.FileTools` (42 imports — Medium)

A grab-bag of file utilities. Methods called (count):

- `resourceToPath` 26, `getInputStreamFromPlainOrCompressedFile` 12, `cleanForFileName` 8, `createDir` 7, `unGzipFile` 2, `unZipFiles` 1, `listSubDirectories` 1, `listDirectoryFiles` 1, `isGZipped` 1, `deleteFiles` 1, `chompExtension` 1.

**Retirement difficulty: Medium.** Each method is a thin wrapper over `java.io` / `java.nio.file` / Apache Commons IO / Commons Compress. Could be reimplemented as `ubic.gemma.core.util.FileTools` in 1–2 sessions, then migrate ~60 call sites mechanically. No baseCode transitive surface beyond `java.io`.

### `util.Configuration` + `util.ConfigUtils` (8 imports — Medium, load-bearing)

baseCode's properties-based configuration store. Critical because:

- `BaseCodeConfigurer` (Spring `BeanFactoryPostProcessor`) is what makes baseCode read Gemma's `Gemma.properties` via `Configuration.setString()`. Without baseCode-the-library this is moot, but **the in-tree ontology code still reads `Configuration` directly** (e.g. `OntologyLoader`, `RestrictionFactory`, `OntologyTermImpl`, several others — comments in those files explicitly say "continue to use baseCode's `ubic.basecode.util.Configuration` via the…").

So `Configuration` has *internal* coupling: the freshly-ported ontology code in `gemma-core/src/main/java/ubic/gemma/core/ontology/basecode/` still depends on it. Until those references are switched to a Gemma-native config bean, `baseCode-util` cannot be removed.

**Retirement difficulty: Medium.** 1 session to introduce a Spring-backed replacement and rewire the 8 call sites — but coordinate with anyone still touching the ported ontology code.

### `util.StringUtil`, `NetUtils`, `SQLUtils`, `DateUtil`, `RegressionTesting` (16 imports — Trivial-to-Medium)

- `StringUtil` (7) — `makeNames`, `makeUnique`, `commonPrefix` — used in CellBrowser writers and `DatasetCombiner`. Pure string helpers; trivial.
- `NetUtils` (3) — `connect`, `ftpDownloadFile`, `ftpFileSize`, `bytePerSecondToDisplaySize`. FTP wrappers in `FtpFetcher` / `NetDatasourceUtil` / `LoggingProgressReporter`. Trivial.
- `SQLUtils` (2) — `blobToString` in `GoldenPathQuery` / `GoldenPathSequenceAnalysis`. Trivial.
- `DateUtil` (2), `RegressionTesting` (2 — tests only, `closeEnough`), `ConfigUtils` (1). Trivial.

Each is a one-class extraction.

### `io.reader.DoubleMatrixReader` (18 imports — Medium)

Reads tab-delimited matrix files into baseCode `DoubleMatrix`. Used in tests (random/test matrix loading) and in `ExpressionDataMatrixServiceImpl`. Coupled to `dataStructure.matrix` so can't be retired separately.

### `graphics.MatrixDisplay`, `ColorMatrix`, `ColorMap` (5 imports — Medium)

Renders matrix heatmaps to PNG for the QC controller and visualization service. Depends on `dataStructure.matrix`. Custom Gemma renderer would be a small project (1–2 sessions) but only after matrix retires.

### `io.ByteArrayConverter`, `io.writer.MatrixWriter`, `dataStructure.{CountingMap,DoublePoint}` (4 imports — Trivial)

Tiny one-off uses. Could absorb in-tree in an afternoon.

---

## 3. Retirement-difficulty summary

| Bucket | Packages | Imports | Files |
|---|---|---:|---:|
| **Trivial** (peel off in 1 session) | `util.StringUtil`, `util.NetUtils`, `util.SQLUtils`, `util.DateUtil`, `util.RegressionTesting`, `util.ConfigUtils`, `dataStructure.{CountingMap,DoublePoint}`, `io.ByteArrayConverter` | ~20 | ~15 |
| **Medium** (1–2 sessions, contained) | `util.FileTools`, `util.Configuration` + `BaseCodeConfigurer` (coordinated with in-tree ontology) | ~50 | ~50 |
| **Substantial** (months, deep numerics) | `dataStructure.matrix` + `math` + `math.linearmodels` + `math.distribution` + `math.linalg` + `math.metaanalysis` + `io.reader` (DoubleMatrixReader) + `graphics` + `io.writer.MatrixWriter` | ~180 | ~110 |

The Substantial bucket is one big organism. `DoubleMatrix` is the type that `MatrixStats`, `DescriptiveWithMissing`, `DesignMatrix`, `LeastSquaresFit`, `DoubleMatrixReader`, `ColorMatrix`, `MatrixDisplay`, and most of the math helpers operate on. You can't extract any one of them without extracting the whole numeric core.

---

## 4. Recommendation

**Keep the `baseCode` dependency.** Specifically, keep it for:

- `ubic.basecode.dataStructure.matrix.*` and everything that consumes it.
- `ubic.basecode.math.*` (including `distribution`, `linalg`, `linearmodels`, `metaanalysis`).
- `ubic.basecode.io.reader.DoubleMatrixReader`, `io.writer.MatrixWriter`.
- `ubic.basecode.graphics.*`.

These are Gemma's analytic core, share a tight type system, and re-implementing them is a multi-month numerics project with substantial regression risk. The baseCode JAR is small; the transitives that *did* worry us (old Jena, old Lucene) have already been wrestled into submission via the pom's `<exclusions>` block as part of the ontology pull-in.

**Optional cleanup we can do without dropping the dep:**

- **Retire `ubic.basecode.util.*` over one or two sessions.** Move `FileTools`, `StringUtil`, `NetUtils`, `SQLUtils`, `DateUtil`, `RegressionTesting`, `ConfigUtils` in-tree as `ubic.gemma.core.util.*`. ~75 import sites, mechanical rewrite. This shrinks the baseCode surface to just the numeric core, which makes future audits easier and decouples the in-tree ontology code from baseCode's static `Configuration` store. **Estimated effort: 2 sessions.**
- **Retire `ubic.basecode.util.Configuration`** by introducing a Spring `@ConfigurationProperties` bean for the baseCode-prefixed keys, rewiring the in-tree ontology call sites, and deleting `BaseCodeConfigurer`. **Estimated effort: 1 session** (do this with or right after the FileTools retirement; the two are entangled in `Gemma.properties` plumbing).

If both optional cleanups are done, the remaining baseCode surface is *only* the numeric/matrix/graphics core (~180 imports across ~110 files), which is exactly the use case the dep is best suited for.

---

## 5. Effort estimate

**To drop the dep entirely:** not recommended. Order-of-magnitude **3–6 months** of focused engineering plus a serious numerics-regression test plan. The risk/reward is poor — baseCode is stable, small, and Gemma's lab maintains it.

**To shrink the surface to the numeric core only** (the recommended path):

- Util-package retirement: **2 sessions** (FileTools + smaller utils).
- Configuration retirement: **1 session**.
- **Total: ~3 sessions**, doc-only-or-mechanical, low risk.

After that, baseCode stays as a Maven dep purely for `dataStructure.matrix`, `math.*`, `io.reader.DoubleMatrixReader`, `io.writer.MatrixWriter`, and `graphics.*`.

---

## 6. Packages that should remain consumed via the JAR

If we do the recommended scoping pass, keep these baseCode packages on the classpath:

- `ubic.basecode.dataStructure.matrix`
- `ubic.basecode.math`
- `ubic.basecode.math.distribution`
- `ubic.basecode.math.linalg`
- `ubic.basecode.math.linearmodels`
- `ubic.basecode.math.metaanalysis`
- `ubic.basecode.io.reader`
- `ubic.basecode.io.writer`
- `ubic.basecode.graphics`

Everything else (`util.*`, the tiny `dataStructure.{CountingMap,DoublePoint}`, `io.ByteArrayConverter`) can be retired in-tree over a small handful of sessions whenever the team wants to take a swing at it.
