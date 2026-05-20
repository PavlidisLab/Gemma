# baseCode `math` + `linearmodels` (+ neighbours) — pull-in recce

> **STATUS: DONE (2026-05-19).** The pull-in described below was
> executed; the math / linearmodels / linalg / distribution /
> metaanalysis classes live in-tree under `ubic.gemma.core.util.math.*`
> (with `linearmodels`, `linalg`, `distribution`, `metaanalysis`
> sub-packages), the graphics subset under
> `ubic.gemma.core.util.graphics.*`, and `MatrixWriter` under
> `ubic.gemma.core.util.matrix.*`. The baseCode Maven dep was retired
> at `9f216558d5`. Kept for historical reference.

**Date:** 2026-05-19
**Branch:** `basecode-math-linearmodels-recce` off `phase2-acl-migrate` @ `c4b88913f01ee74b85e8fd066f7353c3102487fd`
**Question:** The matrix port (`4be748f512`) is dormant in-tree under `ubic.gemma.core.util.matrix`. Consumer rewiring is blocked by 11 of 60 consumers that pass `DoubleMatrix` into baseCode helpers (`MatrixStats`, `MatrixRowStats`, `LeastSquaresFit`, `DesignMatrix`, `MeanVarianceEstimator`, `SingularValueDecomposition`, `MatrixWriter`, `ColorMatrix`). Until those helpers also live in-tree, the rewrite is type-mismatched. What's the actual shape of the remaining math + linearmodels subsystem, and what's the smallest viable port that unblocks the consumer rewire?

**TL;DR:** Reachable subset is **30 files, ~10,400 LoC** across six sub-packages. The matrix-consumer rewire is unblocked by **22 classes / ~7,800 LoC** (the math statistical helpers + the full linearmodels cluster + linalg + Histogram + MatrixWriter + ColorMatrix/MatrixDisplay/ColorMap). No "blocked-by" external deps — everything reachable is either pure-Java + colt + already-in-tree matrix, or a self-contained cluster (linearmodels, linalg). `Constants` / `MathUtil` / `StringDistance` are already in-tree from the math pilot. **Recommendation: pull the entire reachable 22-class subset in one focused session (~5–7h).** Doing it incrementally (e.g. just the 8 named helpers) leaks back into baseCode via transitive deps and doesn't actually let the matrix consumer rewire go green.

---

## 1. Inventory — remaining baseCode imports that touch `DoubleMatrix`

Search:

```
grep -rh '^import ubic\.basecode\.\(math\|datastructure\.matrix\|graphics\|io\.writer\)' \
  gemma-core gemma-cli gemma-rest gemma-web 2>/dev/null \
  | sed 's/import //; s/;//' | sort -u
```

Non-`dataStructure.matrix` results (matrix package was done by the matrix-recce):

| Import | Consumer files | Already in-tree? |
|---|---:|---|
| `ubic.basecode.math.CorrelationStats` | 1 | no |
| `ubic.basecode.math.DescriptiveWithMissing` | 15 | no |
| `ubic.basecode.math.Distance` | 1 | no |
| `ubic.basecode.math.KruskalWallis` | 2 | no |
| `ubic.basecode.math.MatrixNormalizer` | 1 | no |
| `ubic.basecode.math.MatrixRowStats` | 1 | no |
| `ubic.basecode.math.MatrixStats` | 9 | no |
| `ubic.basecode.math.MultipleTestCorrection` | 2 | no |
| `ubic.basecode.math.Rank` | 4 | no |
| `ubic.basecode.math.Stats` | 1 | no |
| `ubic.basecode.math.Constants` | 1 | **yes** (`ubic.gemma.core.util.math.Constants`) |
| `ubic.basecode.math.MathUtil` | 1 | **yes** (`ubic.gemma.core.util.math.MathUtil`) |
| `ubic.basecode.math.StringDistance` (legacy ref) | 1 | **yes** (`ubic.gemma.core.util.math.StringDistance`) |
| `ubic.basecode.math.distribution.Histogram` | 9 | no |
| `ubic.basecode.math.linearmodels.*` (wildcard) | 1 (`LinearModelAnalyzer`) | no |
| `ubic.basecode.math.linearmodels.DesignMatrix` | 2 | no |
| `ubic.basecode.math.linearmodels.LeastSquaresFit` | 2 | no |
| `ubic.basecode.math.linearmodels.MeanVarianceEstimator` | 2 | no |
| `ubic.basecode.math.metaanalysis.MetaAnalysis` | 2 | no |
| `ubic.basecode.math.linalg.SingularValueDecomposition` | 1 | no |
| `ubic.basecode.io.writer.MatrixWriter` | 3 | no |
| `ubic.basecode.graphics.ColorMatrix` | 2 | no |
| `ubic.basecode.graphics.ColorMap` | 1 | no |
| `ubic.basecode.graphics.MatrixDisplay` | 2 | no |

The matrix-recce agent flagged 8 named helper classes; the actual reachable set is larger because `LeastSquaresFit` drags the whole linearmodels cluster (Anova*, LinearModelSummary*) and the linalg cluster (`QRDecomposition` → `Dqrsl` → `Blas`).

---

## 2. Per-class details

Source root: `/Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/`.

### 2a. `ubic.basecode.math.*` (statistical helpers)

| Class | LoC | Gemma usage | Transitive baseCode deps |
|---|---:|---|---|
| `CorrelationStats` | 499 | Pearson/Spearman + Fisher's-z helpers; one consumer | none |
| `DescriptiveWithMissing` | 961 | NaN-aware mean/var/median/correlation; **15 consumers** — the central stats utility | none (only colt) |
| `Distance` | 198 | Euclidean / Manhattan / correlation distance; one consumer | none |
| `KruskalWallis` | 139 | Kruskal–Wallis; two consumers | none |
| `MatrixNormalizer` | 150 | Row-quantile / log normalization; one consumer | `DenseDoubleMatrix`, `DoubleMatrix` (in-tree), **`datafilter.RowMissingFilter`** |
| `MatrixRowStats` | 123 | Per-row mean/sum/var on a `DoubleMatrix`; one consumer | `DoubleMatrix` (in-tree) |
| `MatrixStats` | 440 | Matrix-level mean/cov/var; **9 consumers** | `DenseDoubleMatrix`, `DenseDoubleMatrix1D`, `DoubleMatrix`, `MatrixUtil`, `SparseDoubleMatrix` (all in-tree) |
| `MultipleTestCorrection` | 199 | BH/Bonferroni; two consumers | none |
| `Rank` | 432 | Tie-aware ranking; four consumers | none |
| `Stats` | 334 | Skew/kurtosis/quartiles; one consumer | none |
| `SpecFunc` | 843 | (transitive — pulled by `ModeratedTstat` only) | `MatrixUtil` (in-tree) |
| `Wilcoxon` | 320 | (not directly imported but pulled by `DescriptiveWithMissing` transitively? no — independent) | none |

### 2b. `ubic.basecode.math.distribution.*`

| Class | LoC | Gemma usage | Deps |
|---|---:|---|---|
| `Histogram` | 410 | 9 consumers; bins + percentile lookups | none (colt + jfreechart only) |

The other 8 files in `math/distribution/` (`Dirichlet`, `HistogramSampler`, density/probability computers, `Wishart`) are **not reachable** from Gemma — skip.

### 2c. `ubic.basecode.math.linearmodels.*` (the whole cluster moves as one)

`LeastSquaresFit` references `LinearModelSummary`, `LinearModelSummaryImpl`, `GenericAnovaResult`, `AnovaEffect` directly. Pulling `LeastSquaresFit` requires the rest of the package.

| Class | LoC | Direct use? | Deps |
|---|---:|---|---|
| `DesignMatrix` | 727 | yes (2 consumers) | `DenseDoubleMatrix`, `DoubleMatrix`, `ObjectMatrix`, `StringMatrix` (in-tree) |
| `LeastSquaresFit` | 1571 | yes (2 consumers) | `DoubleMatrix`, `DoubleMatrixFactory`, `MatrixUtil`, `ObjectMatrix` (in-tree), **`Constants`** (in-tree), **`QRDecomposition`** |
| `MeanVarianceEstimator` | 337 | yes (2 consumers) | `DoubleMatrix` (in-tree), **`DescriptiveWithMissing`**, **`MatrixRowStats`**, **`Smooth`**, **`QRDecomposition`** |
| `LinearModelSummary` | 122 | transitive | `DoubleMatrix` (in-tree) |
| `LinearModelSummaryImpl` | 339 | transitive | `DoubleMatrix` (in-tree) |
| `LinearModelSummaryUtils` | 51 | transitive | `DoubleMatrix` (in-tree) |
| `GenericAnovaResult` | 63 | transitive | none |
| `GenericAnovaResultImpl` | 244 | transitive | uses `AnovaEffect` |
| `AnovaEffect` | 124 | transitive | none |
| `AnovaResult` | 37 | transitive | none |
| `OneWayAnovaResult` | 30 | transitive | none |
| `TwoWayAnovaResult` | 49 | transitive | none |
| `ModeratedTstat` | 259 | yes (called from `LinearModelAnalyzer.ebayes`) | `MatrixUtil` (in-tree), **`DescriptiveWithMissing`**, **`SpecFunc`** |
| `Smooth` (`ubic.basecode.math.Smooth`) | 197 | transitive (pulled by `MeanVarianceEstimator`) | none |
| `SpecFunc` (`ubic.basecode.math.SpecFunc`) | 843 | transitive (pulled by `ModeratedTstat`) | `MatrixUtil` (in-tree) |

### 2d. `ubic.basecode.math.linalg.*`

| Class | LoC | Direct use? | Deps |
|---|---:|---|---|
| `SingularValueDecomposition` | 196 | yes (1 consumer) | `DenseDoubleMatrix`, `DoubleMatrix` (in-tree) — independent, pulls only colt |
| `QRDecomposition` | 553 | transitive (pulled by `LeastSquaresFit`, `MeanVarianceEstimator`) | `DenseDoubleMatrix1D`, `MatrixUtil` (in-tree), **`Dqrsl`** |
| `Dqrsl` | 760 | transitive | **`Blas`** |
| `Blas` | 1583 | transitive | none (pure numerics) |

### 2e. `ubic.basecode.math.metaanalysis.*`

| Class | LoC | Direct use? | Deps |
|---|---:|---|---|
| `MetaAnalysis` | 302 | yes (2 consumers) | `Constants` (in-tree) |
| `CorrelationEffectMetaAnalysis` | 256 | transitive (subclass) | `CorrelationStats` |
| `MeanDifferenceMetaAnalysis` | 176 | transitive (subclass) | none |

### 2f. `ubic.basecode.io.writer.MatrixWriter`

| Class | LoC | Notes |
|---|---:|---|
| `MatrixWriter` | 272 | 3 consumers. Uses `Matrix2D` (in-tree), `MatrixUtil` (in-tree), **`Matrix3D`** (NOT in-tree — drop the `writeMatrix(Matrix3D)` overload during port; no Gemma consumer uses 3D). |

### 2g. `ubic.basecode.graphics.*` (QC plots / experimental-design heatmaps)

| Class | LoC | Direct use? | Deps |
|---|---:|---|---|
| `ColorMatrix` | 411 | yes (2 consumers: `ExperimentalDesignVisualizationServiceImpl`, `ExpressionExperimentQCController`) | `DenseDoubleMatrix`, `DoubleMatrix` (in-tree), **`DoubleMatrixReader`** (in `io.reader`, NOT yet ported), `Constants` (in-tree), **`DescriptiveWithMissing`**, **`MatrixStats`** |
| `ColorMap` | 173 | yes (1 consumer) | none |
| `MatrixDisplay` | 713 | yes (2 consumers) | `DoubleMatrix` (in-tree), **`graphics.text.Util`** (88 LoC, no baseCode deps) |
| `graphics.text.Util` | 88 | transitive | none |

`ColorMatrix` is the only graphics class that drags in non-trivially-portable code (it touches `DoubleMatrixReader`, which lives in `io.reader` and was punted in the matrix recce). Two options: (a) port the trimmed-down `ColorMatrix` without the `DoubleMatrixReader`-based constructor (Gemma's two consumers construct it from an existing `DoubleMatrix` instance, **not** by reading from disk — verified); (b) also port `DoubleMatrixReader` / `StringMatrixReader` (would tack on ~600 LoC across two more files plus `ReaderHelpers`).

### 2h. Datafilter (transitive — only via `MatrixNormalizer`)

`MatrixNormalizer` (1 consumer) imports `datafilter.RowMissingFilter`. That class depends on `AbstractFilter` + `Filter` (two small files, no further baseCode deps; both already constrained over `Matrix2D` which is in-tree). Net: porting `MatrixNormalizer` adds 3 datafilter files (~250 LoC).

---

## 3. Grouped by sub-package + categorization

| Sub-package | Reachable classes (incl. transitive) | LoC | Category |
|---|---|---:|---|
| `math` (statistical helpers) | `CorrelationStats`, `DescriptiveWithMissing`, `Distance`, `KruskalWallis`, `MatrixRowStats`, `MatrixStats`, `MultipleTestCorrection`, `Rank`, `Stats`, `SpecFunc` (+ `Smooth`, `MatrixNormalizer` if datafilter follows) | ~4,800 | **Trivial** — pure helpers, deps only on already-in-tree matrix + colt |
| `math.distribution` | `Histogram` only | 410 | **Trivial** |
| `math.linearmodels` | All 13 files (`DesignMatrix`, `LeastSquaresFit`, `MeanVarianceEstimator`, `ModeratedTstat`, `LinearModelSummary`*, `GenericAnovaResult`*, `AnovaEffect`, `AnovaResult`, `OneWayAnovaResult`, `TwoWayAnovaResult`) | ~3,950 | **Requires-cluster** — `LeastSquaresFit` API surface references all of them; ports as a single unit. Also requires `linalg` (QRDecomp), `Smooth`, `DescriptiveWithMissing`, `SpecFunc` from `math` |
| `math.linalg` | `SingularValueDecomposition` (independent), `QRDecomposition`, `Dqrsl`, `Blas` | ~3,090 | **Requires-cluster** for QR (QR → Dqrsl → Blas); SVD is independent and trivial |
| `math.metaanalysis` | `MetaAnalysis`, `CorrelationEffectMetaAnalysis`, `MeanDifferenceMetaAnalysis` | 734 | **Requires-cluster** — Correlation variant needs `CorrelationStats` from `math` |
| `io.writer` | `MatrixWriter` (drop Matrix3D overload) | 272 | **Trivial** (after dropping 3D method) |
| `graphics` | `ColorMatrix`, `ColorMap`, `MatrixDisplay`, `graphics.text.Util` | ~1,385 | **Trivial** if we accept dropping `ColorMatrix`'s `DoubleMatrixReader`-based ctor; otherwise **Blocked-by-`io.reader`** |
| `datafilter` (transitive via `MatrixNormalizer`) | `Filter`, `AbstractFilter`, `RowMissingFilter` | ~280 | **Trivial** |

There is no class in this whole reachable set that is genuinely "blocked-by-X-not-yet-recced". The only soft block is `ColorMatrix`'s `DoubleMatrixReader` ctor, which is unused by Gemma and can be omitted during the port.

---

## 4. Smallest viable port that unblocks the matrix-consumer rewire

The matrix-recce agent flagged 11 consumers blocked by 8 helper classes. To make those consumers compile against the in-tree matrix types, the helpers' API must accept the in-tree matrix types — which means the helpers themselves must be ported, plus their transitive baseCode deps.

**Smallest unblocking subset = 22 files / ≈7,800 LoC**, in three commits:

**Commit A — `math` helpers (pure)** *(~10 files, ~3,950 LoC)*

- `DescriptiveWithMissing`, `MatrixStats`, `MatrixRowStats`, `Rank`, `Stats`, `CorrelationStats`, `KruskalWallis`, `Distance`, `MultipleTestCorrection`, `Smooth`, `SpecFunc`

Optional: `MatrixNormalizer` + the 3 datafilter files (+~430 LoC) — only if you want to unblock the lone `MatrixNormalizer` consumer at the same time. Cheap.

**Commit B — `linalg` + `linearmodels` cluster** *(~17 files, ~7,040 LoC)*

- `linalg`: `Blas`, `Dqrsl`, `QRDecomposition`, `SingularValueDecomposition`
- `linearmodels`: all 13 files

This is the big one — `LeastSquaresFit` is 1571 LoC, `Blas` is 1583 LoC. Mostly mechanical: package + import rewrites only. No structural changes.

**Commit C — `Histogram`, `MatrixWriter`, `graphics`, `metaanalysis`** *(~7 files, ~3,100 LoC)*

- `math.distribution.Histogram`
- `io.writer.MatrixWriter` (drop 3D overload, drop `cern.colt.matrix.DoubleMatrix2D` overload if also unused — check; otherwise keep)
- `graphics.ColorMatrix` (drop the `DoubleMatrixReader` ctor), `ColorMap`, `MatrixDisplay`, `graphics.text.Util`
- `metaanalysis.MetaAnalysis`, `CorrelationEffectMetaAnalysis`, `MeanDifferenceMetaAnalysis`

Then: bulk-rewrite the ~94 remaining consumer-side imports (`ubic.basecode.math.X` → `ubic.gemma.core.util.math.X`, etc.) and the 60 matrix consumer-side imports left over from the matrix recce. The matrix port becomes live, baseCode-as-an-external-dep narrows to `util.FileTools` / `io.reader` / `dataStructure.CountingMap`.

---

## 5. Effort estimates

**Smallest viable port (22 files, all three commits, single session):** **5–7 hours, one agent.** Almost entirely mechanical (package rename + import rewrite + license-header swap). Risk areas:

- `LeastSquaresFit` is large (1571 LoC) and its `summarize()` / `ebayes()` paths are exercised by `LinearModelAnalyzer` — Gemma's diff-ex integration tests are the regression net.
- `ColorMatrix` constructor pruning needs verification (confirm no Gemma caller passes a `DoubleMatrixReader`; spot-check above says no).
- `MatrixWriter`'s 3D overload removal needs the same confirmation (verified above — no `Matrix3D` references in Gemma).
- `DescriptiveWithMissing` is the heaviest consumer-side dep (15 files); the import rewrite must hit them all in one commit to keep `mvn compile` green.

**Full math + linearmodels + adjacents migration (including external-dep retirement):** **8–12 hours / 2 sessions.** Adds: deleting the baseCode dep from `pom.xml`, doing the last `FileTools` + `DoubleMatrixReader` + `CountingMap` ports (each is small but lives in different sub-packages), and re-running the full `mvn verify` matrix.

Comparison points for sanity:

- Matrix recce projected one session (~3–4h) for 14 files / 5,665 LoC — that was done as `4be748f512`.
- Ontology pull-in (step 3) took multiple sessions but had real structural work (Lucene 9 index, Jena adaptations). Math has none of that — it's all delegation over colt + already-in-tree matrix.

---

## 6. Recommended next step

**Pull the smallest unblocking subset (22 files) in one session, three commits as outlined in §4.** Concrete plan:

1. Target packages:
   - `ubic.gemma.core.util.math.*` — already exists with `Constants`, `MathUtil`, `StringDistance`. Add the 10 statistical helpers + `Smooth` + `SpecFunc` here.
   - `ubic.gemma.core.util.math.distribution.Histogram`
   - `ubic.gemma.core.util.math.linalg.*` (4 files)
   - `ubic.gemma.core.util.math.linearmodels.*` (13 files)
   - `ubic.gemma.core.util.math.metaanalysis.*` (3 files)
   - `ubic.gemma.core.util.io.MatrixWriter` (or just `ubic.gemma.core.util.matrix.MatrixWriter` since it's matrix-coupled)
   - `ubic.gemma.core.util.graphics.*` (4 files)
2. Validation bar: `mvn -pl gemma-core,gemma-cli,gemma-web -am compile` clean, then `mvn -pl gemma-core verify` against `gemdtest` (serialize per the agent-gotchas list — single-tenant DB).
3. After the port lands, the dormant matrix port becomes live: do the matrix-consumer rewire that was blocked, in the same session if time permits, otherwise as a follow-up commit.
4. **Do not** try to also pull `FileTools` / `DoubleMatrixReader` / `CountingMap` in this session — they live in different sub-packages and aren't on the critical path for unblocking matrix.

**Defer:** the unused-by-Gemma `math.distribution` files (Dirichlet, density/probability computers, Wishart, HistogramSampler), `math.KSTest`, `math.PrecisionRecall`, `math.ROC`, `math.RandomChooser`, `math.Wilcoxon`. If a future caller needs them, port singly — none has external deps that would surprise us.

---

## Appendix — verification commands

```bash
# Remaining baseCode imports (non-matrix, non-ontology)
grep -rh '^import ubic\.basecode\.\(math\|graphics\|io\.writer\)' \
     gemma-core gemma-cli gemma-rest gemma-web 2>/dev/null \
  | sort | uniq -c | sort -rn

# Per-class consumer count
for c in DescriptiveWithMissing MatrixStats LeastSquaresFit DesignMatrix \
         MeanVarianceEstimator SingularValueDecomposition Histogram \
         MatrixWriter ColorMatrix MatrixDisplay; do
  n=$(grep -rln "basecode.*\.$c\b" gemma-core gemma-cli gemma-rest gemma-web \
        2>/dev/null | wc -l | tr -d ' ')
  echo "$c $n"
done

# baseCode sources & LoC for each candidate
wc -l /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/math/*.java \
      /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/math/linearmodels/*.java \
      /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/math/linalg/*.java \
      /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/math/distribution/Histogram.java \
      /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/math/metaanalysis/*.java \
      /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/io/writer/MatrixWriter.java \
      /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/graphics/*.java
```
