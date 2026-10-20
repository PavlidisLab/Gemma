# baseCode `dataStructure.matrix` subsystem — pull-in recce

> **STATUS: DONE (2026-05-19).** The pull-in described below was
> executed; the matrix classes live in-tree under
> `ubic.gemma.core.util.matrix.*` and the baseCode Maven dep was
> retired at `9f216558d5`. Kept for historical reference.

**Date:** 2026-05-19
**Branch:** `basecode-matrix-recce` off `phase2-acl-migrate` @ `c187831d2baa2dbf7f585a251354149cef554e45`
**Question:** Per `BASECODE_DEP_AUDIT.md`, the matrix subsystem is the next big-ticket pull-in target (93 imports). The math pilot pulled `Constants` / `MathUtil` / `StringDistance` in-tree under `ubic.gemma.core.util.math` and found the remaining 41 math imports are all `DoubleMatrix`-bound — so matrix is now the gate. What's the actual shape of the subsystem, how does it interlock with `cern.colt`, and what would a clean pull-in look like?

**TL;DR:** Whole matrix package is **23 files, ~5,665 LoC**. Of those, **only 14 classes (≈4,400 LoC)** are reachable from Gemma. The whole subsystem is built on `cern.colt` as a thin generic+labelled wrapper — and **Gemma already uses `cern.colt` directly** (59 import sites), so colt is not removable and not an issue. **Recommendation: pull the reachable 14-class subset in one session.** No `mtj` is touched on the reachable side; `Constants` already lives in-tree; one-time mechanical rename of 93 import sites.

---

## 1. Class inventory — `ubic.basecode.dataStructure.matrix.*`

Source: `/Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/dataStructure/matrix/` (working checkout matching `1.1.34-RENOVATIONS-SNAPSHOT`).

### 1a. Reachable from Gemma (used; pull these in)

| Class | LoC | Gemma imports | Role | Transitive baseCode deps |
|---|---:|---:|---|---|
| `DoubleMatrix` (abstract) | 210 | 57 | The central API. Abstract base for all double matrices. | `AbstractMatrix`, `PrimitiveMatrix` |
| `DenseDoubleMatrix` | 377 | 16 | Concrete dense matrix backed by `cern.colt.matrix.impl.DenseDoubleMatrix2D`. | `DoubleMatrix`, `cern.colt.matrix.*` |
| `ObjectMatrix` (interface) | 49 | 6 | Generic-value matrix contract. | `Matrix2D` |
| `ObjectMatrixImpl` | 232 | 4 | Concrete `ObjectMatrix` backed by `cern.colt.matrix.impl.DenseObjectMatrix2D`. | `AbstractMatrix`, `ObjectMatrix`, `cern.colt.matrix.*` |
| `AbstractMatrix` (abstract) | 373 | 3 | Row/column-name machinery + `equals`/`toString`. Pure Java; no colt. | `Matrix2D` |
| `StringMatrix` | 244 | 2 | `ObjectMatrixImpl<R,C,String>` w/ pretty-print. | `AbstractMatrix`, `cern.colt.matrix.*` |
| `DoubleMatrixFactory` | 74 | 2 | Static factory methods (`dense`, `sparse`, `fastrow`, `compressedsparse`). | `DenseDoubleMatrix`, `SparseDoubleMatrix`, `FastRowAccessDoubleMatrix`, `CompressedSparseDoubleMatrix` |
| `SparseDoubleMatrix` | 354 | 1 | Sparse colt-backed double matrix. | `DoubleMatrix`, `cern.colt.matrix.impl.SparseDoubleMatrix2D` |
| `MatrixUtil` | 569 | 1 | Static helpers: NaN-aware row-mean, removeMissing, view-as-1D, threshold-mask. | `DoubleMatrix`, `ubic.basecode.math.Constants` (already in-tree) |
| `IntegerMatrix` | 168 | 1 | `IntegerMatrix` backed by `ObjectMatrixImpl<R,C,Integer>`. | `AbstractMatrix`, `ObjectMatrixImpl`, `PrimitiveMatrix` |
| `DenseDoubleMatrix1D` | 81 | 1 | Thin subclass of `cern.colt.matrix.impl.DenseDoubleMatrix1D` that exposes `elements[]` directly (faster `getQuick`, no-copy `toArray`). | `cern.colt.matrix.impl.DenseDoubleMatrix1D` only |
| `PrimitiveMatrix` (interface) | 48 | 0* | Tagging interface for primitive-valued matrices. *Pulled in transitively via `DoubleMatrix`/`IntegerMatrix`.* | `Matrix2D` |
| `Matrix2D` (interface) | 217 | 0* | The root 2D matrix contract (`rows()`, `columns()`, `getEntry`, names). *Pulled in transitively from `AbstractMatrix`.* | none (java.util only) |
| `FastRowAccessDoubleMatrix` | 347 | 0* | Reachable transitively via `DoubleMatrixFactory.fastrow()`. Row-major 2D `double[][]` backing. | `DoubleMatrix`, `cern.colt.matrix.DoubleMatrix1D` |
| `CompressedSparseDoubleMatrix` | 376 | 0* | Reachable transitively via `DoubleMatrixFactory.compressedsparse()`. Backed by **`no.uib.cipr.matrix.sparse.FlexCompRowMatrix`** (MTJ). | `DoubleMatrix`, `no.uib.cipr.matrix.*` |

`*` indirectly reachable — must be pulled in to satisfy the public-API surface.

**Total reachable: 14 classes, ≈3,719 LoC** (excluding the always-included Constants).

### 1b. Unreachable from Gemma (skip)

| Class | LoC | Why skip |
|---|---:|---|
| `AbstractMatrix3D` | 208 | No 3D matrix consumers in Gemma. |
| `Matrix3D` (interface) | 213 | Same. |
| `DoubleMatrix3D` | 133 | Same. |
| `DenseDouble3dMatrix` | 184 | Same. |
| `DenseObject3DMatrix` | 124 | Same. |
| `CompressedBitMatrix` | 464 | No consumers; MTJ-backed; bit-storage. |
| `RCDoubleMatrix1D` | 228 | Row-compressed 1D — no consumers. |
| `SparseRaggedDoubleMatrix` | 392 | No consumers. |

**Skip total: 8 classes, ~1,946 LoC.**

---

## 2. Core dependency spine

The "everything-else-leans-on-these" backbone is small and clean:

```
Matrix2D (interface, no deps)
  └── AbstractMatrix (row/col name plumbing, pure Java)
        └── DoubleMatrix (abstract, +PrimitiveMatrix)
              └── DenseDoubleMatrix  ─┐
              └── SparseDoubleMatrix ─┼── delegate to cern.colt 2D matrices
              └── FastRowAccessDoubleMatrix
              └── CompressedSparseDoubleMatrix ── delegates to MTJ
        └── ObjectMatrix (interface)
              └── ObjectMatrixImpl  ── delegates to colt DenseObjectMatrix2D
                    └── StringMatrix
                    └── IntegerMatrix
```

Standalone helpers: `DoubleMatrixFactory`, `MatrixUtil`, `DenseDoubleMatrix1D`.

The **core five** that nothing else can be pulled without: `Matrix2D`, `AbstractMatrix`, `PrimitiveMatrix`, `ObjectMatrix`, `DoubleMatrix`. About **897 LoC** of pure-Java + one colt-list import. These are also the most stable and least colt-coupled — a clean pull.

---

## 3. The `cern.colt` connection

`cern.colt` is **the storage layer** for every concrete matrix in the subsystem:

- `DenseDoubleMatrix` wraps `cern.colt.matrix.impl.DenseDoubleMatrix2D`.
- `SparseDoubleMatrix` wraps `cern.colt.matrix.impl.SparseDoubleMatrix2D`.
- `ObjectMatrixImpl` wraps `cern.colt.matrix.impl.DenseObjectMatrix2D`.
- `DoubleMatrix.viewRow()` returns `cern.colt.matrix.DoubleMatrix1D`.
- `DenseDoubleMatrix1D` directly **extends** `cern.colt.matrix.impl.DenseDoubleMatrix1D`.

The `baseCode` matrix types are a generic-typed, named-axis veneer over Colt; they do not abstract Colt away — their public APIs leak `DoubleMatrix1D` / `DoubleArrayList` types.

**Is `cern.colt` OK to keep as a direct dep?** Yes — and we have no choice. Gemma already has `colt:colt:1.2.0` declared in `pom.xml` and uses it directly (59 import sites across `gemma-core/cli/web`: `cern.colt.matrix.linalg.Algebra`, `cern.colt.list.DoubleArrayList`, `cern.colt.matrix.impl.DenseDoubleMatrix2D`, etc.). Removing colt would require replacing **both** the baseCode matrix subsystem and Gemma's direct colt usage — a much bigger project than this recce contemplates. **Keep colt.**

**MTJ** (`no.uib.cipr.matrix:mtj:1.0.4`) is similarly already a Gemma direct dep (declared in `gemma-core/pom.xml` for MatrixMarket reading). Pulling in `CompressedSparseDoubleMatrix` does not introduce a new dep.

---

## 4. Pull-in feasibility per class

| Class | Difficulty | Notes |
|---|---|---|
| `Matrix2D` | **Trivial** | Pure interface, no baseCode deps. |
| `PrimitiveMatrix` | **Trivial** | Pure interface extending `Matrix2D`. |
| `ObjectMatrix` | **Trivial** | Pure interface extending `Matrix2D`. |
| `AbstractMatrix` | **Trivial** | Pure-Java name/index plumbing, only depends on `Matrix2D`. |
| `DoubleMatrix` | **Trivial** | Abstract class; touches `cern.colt.list.DoubleArrayList` + `cern.colt.matrix.DoubleMatrix1D`, both already on the classpath. |
| `DenseDoubleMatrix1D` | **Trivial** | 81 LoC, single-file subclass of colt class — copy verbatim. |
| `ObjectMatrixImpl` | **Trivial** | Wraps colt `DenseObjectMatrix2D`. |
| `StringMatrix` | **Trivial** | Subclass of `ObjectMatrixImpl`. |
| `IntegerMatrix` | **Trivial** | Subclass of `ObjectMatrixImpl`. |
| `DenseDoubleMatrix` | **Trivial** | The concrete workhorse, ~377 LoC, only colt deps. |
| `SparseDoubleMatrix` | **Trivial** | Same pattern as DenseDoubleMatrix. |
| `FastRowAccessDoubleMatrix` | **Trivial** | Same pattern. |
| `CompressedSparseDoubleMatrix` | **Trivial** | Uses MTJ (already a direct dep), no surprises. |
| `DoubleMatrixFactory` | **Trivial** | Five-line static factories over the four concrete types. |
| `MatrixUtil` | **Trivial** | 569 LoC of static helpers; one ref to `ubic.basecode.math.Constants`, which is already in-tree at `ubic.gemma.core.util.math.Constants` — single import-line edit. |

**No class is blocked. There is no "requires-cluster" or "blocked-by-X" tier here** — the matrix subsystem is its own self-contained DAG over colt + MTJ + the already-pulled `Constants`. The transitive deps for the unused half of the math package (`Rank`, `DescriptiveWithMissing`, `LeastSquaresFit`, etc.) flow the *other* direction (math → matrix), not matrix → math. Confirmed by `grep '^import ubic\.basecode' src/.../matrix/*.java` showing only one such import in the entire reachable subset (`MatrixUtil` → `Constants`).

---

## 5. Effort estimate — pull the whole reachable subset in-tree

**One session (≈3–4 hours), single agent, doc-only validation bar = compile-clean + tests-green.**

Steps:

1. Create `gemma-core/src/main/java/ubic/gemma/core/util/matrix/` with the 14 reachable files (+ optional package-info).
2. Single-pass `sed` per file: rewrite `package ubic.basecode.dataStructure.matrix;` → `package ubic.gemma.core.util.matrix;`, drop the Apache 2.0 license header preamble or convert to Gemma's standard header.
3. In `MatrixUtil`, change `import ubic.basecode.math.Constants;` → `import ubic.gemma.core.util.math.Constants;`.
4. Mechanical bulk-rewrite of 93 Gemma import sites: `import ubic.basecode.dataStructure.matrix.X;` → `import ubic.gemma.core.util.matrix.X;`. The line counts above (57+16+6+4+3+2+2+1+1+1+1 = 94 — confirmed). One-line per file in 60-ish consumer files.
5. `mvn -pl gemma-core,gemma-cli,gemma-web -am compile` then `mvn verify`.

Risk areas, in decreasing order:

- **Test parity.** The baseCode tests for `DenseDoubleMatrix` / `MatrixUtil` are *not* coming with us. Gemma's own preprocessing/SVD/ComBat/sample-coexpression tests already exercise the matrix API end-to-end and are the de-facto regression net.
- **3D + bit-matrix classes.** Skip them. If a future caller appears, pull as a single follow-up.
- **`MatrixUtil` API surface.** 569 LoC is the largest file; sanity-check that every `MatrixUtil.*` callsite in Gemma still compiles after the rename (only one Gemma file imports it, so the blast radius is tiny).

Confidence: **high.** The subsystem is mature, settled, and has no surprises in its dependency graph. No cross-package circular refs.

---

## 6. Recommended next step

**Pull the reachable 14-class subset in-tree in one session.** Concrete plan:

- Target: `ubic.gemma.core.util.matrix.*` (mirrors the math pilot's `ubic.gemma.core.util.math.*`).
- Scope: the 14 classes in §1a. Skip 3D + `CompressedBitMatrix` + `SparseRaggedDoubleMatrix` + `RCDoubleMatrix1D`.
- Validation bar: `mvn -pl gemma-core,gemma-cli,gemma-web -am compile && mvn -pl gemma-core verify` clean. (Integration tests will hit `gemdtest`; serialize with the agent gotchas list.)
- Once green, the 41 remaining `ubic.basecode.math.*` imports unblock — `Rank`, `MatrixStats`, `DescriptiveWithMissing`, `Stats`, `MultipleTestCorrection`, etc. all depend on these matrix types being on a stable in-tree package.

**Do not** pull math + linearmodels in the same session — they are 60+ files combined and need their own recce. Matrix is the gate; math comes after.

**Do not** attempt to remove `cern.colt`. It is a permanent direct dep regardless of whether baseCode lives in-tree or external.

---

## Appendix — verification commands

```bash
# Imports per matrix class (from Gemma sources)
grep -rh 'import ubic\.basecode\.dataStructure\.matrix\.' gemma-core gemma-cli gemma-rest gemma-web 2>/dev/null \
  | sort | uniq -c | sort -rn

# baseCode matrix sources & LoC
wc -l /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/dataStructure/matrix/*.java

# Imports used by the matrix package itself
grep -h '^import ' /Users/pzoot/Dev/eclipseworkspace/baseCode/src/ubic/basecode/dataStructure/matrix/*.java \
  | sort -u

# Direct cern.colt usage in Gemma (proof colt stays regardless)
grep -rh '^import cern\.colt' gemma-core/src gemma-cli/src gemma-web/src 2>/dev/null | sort -u
```
