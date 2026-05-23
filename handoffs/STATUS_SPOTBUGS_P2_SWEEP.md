# Spotbugs P2 sweep — status

Branch `fix-spotbugs-p2-v2` off `phase2-acl-migrate@488204bbc7` (which
already carried the STCAL DateFormat synchronization fix from a prior
attempt).

## Inventory (post-baseline, pre-sweep)

Spotbugs run via `mvn -pl gemma-core,gemma-rest,gemma-cli compile spotbugs:spotbugs -q`.

| module      | P1 | P2   |
|-------------|----|------|
| gemma-core  | 3  | 2578 |
| gemma-rest  | 2  | 393  |
| gemma-cli   | 30 | 187  |

Top P2 patterns in gemma-core:

| count | pattern                                       |
|-------|-----------------------------------------------|
| 766   | NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE        |
| 571   | RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE      |
| 420   | EI_EXPOSE_REP2                                |
| 356   | EI_EXPOSE_REP                                 |
| 105   | CT_CONSTRUCTOR_THROW                          |
| 76    | PATH_TRAVERSAL_IN                             |
| 35    | VA_FORMAT_STRING_USES_NEWLINE                 |
| 32    | SE_BAD_FIELD                                  |
| 27    | URLCONNECTION_SSRF_FD                         |
| 18    | DLS_DEAD_LOCAL_STORE                          |
|  9    | UC_USELESS_OBJECT                             |
|  5    | OBL_UNSATISFIED_OBLIGATION                    |

The high-count buckets (NP_NULL_ON_SOME_PATH, RCN_REDUNDANT_NULLCHECK)
are bulk noise from `@Nullable`-vs-`Objects.requireNonNull` shape
mismatches and are not surgical-fix territory. EI_EXPOSE_REP (~776
across both forms) is mostly mutable-collection / Lombok-generated
getters where defensive copy would either break callers that mutate
in place or fight the lombok contract — needs a design pass, not
surgical fixes.

## Fixes landed (3 commits)

### 1. OS_OPEN_STREAM resource leaks (commit `489a1f3e65`)

Wrap stream acquisitions in try-with-resources:

| file                                                            | change                                                                  |
|-----------------------------------------------------------------|-------------------------------------------------------------------------|
| `FileTools.unZipFiles`                                          | ZipFile + per-entry InputStream/OutputStream now closed                  |
| `TextResourceToSetOfLinesFactoryBean.createInstance`            | BufferedReader on classpath resource closed                              |
| `UnifiedOntologyUpdaterCli.doWork`                              | BufferedReader on classpath resource closed                              |
| `DifferentialExpressionAnalysisDaoImpl.insertRowsAndAssignGeneratedKeys` | ResultSet from `getGeneratedKeys()` closed (also drops unused `idType` / `dialect` locals + `org.hibernate.type.Type` import) |

### 2. DLS_DEAD_LOCAL_STORE cleanup (commit `e597b75572`)

| file                                                            | dead local removed                                                                       |
|-----------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `Stats.meanAboveQuantile`                                       | `temp = new double[effectiveSize]` overwritten by alias on next line                     |
| `MetaAnalysis.weightedMean` (both overloads)                    | `return wm /= s` → plain division (local assignment was dead)                            |
| `GeneDaoImpl.remove`                                            | `int removedDummyProducts` never read                                                    |
| `ExpressionExperimentSearchServiceImpl.searchExpressionExperiments` | `List<Long> eeIds` never read (caller recomputes inline)                              |
| `AnnotationsWebService.replaceDatasetAnnotations`               | `Set<AnnotationValueObject> currentVOs` never read after a previous refactor             |

### 3. EI_EXPOSE_REP defensive copy (commit `b4da9e3efe`)

| file                                | getter                | array         |
|-------------------------------------|-----------------------|---------------|
| `anndata/DenseMatrix.getShape`      | clone returned        | `int[2]`      |
| `anndata/SparseMatrix.getShape`     | clone returned        | `int[2]`      |
| `anndata/SparseMatrix.getIndptr`    | clone returned        | `long[shape+1]` |

Shape arrays are 2 ints; indptr length is small relative to the H5-backed
data so the copy is negligible against load cost. Other primitive-array
exposures were deferred:

- `SVDResult.variances` — lombok `@Value`-generated getter; would require
  switching off lombok on this class.
- `DenseDoubleMatrix1D.elements` / `ColorMatrix.m_rowKeys` — used in
  hot-path linear-algebra loops; defensive copy would defeat purpose.
- `Histogram.getArray` / `CategoricalArray.getCodes` — called in tight
  plotting / per-cell loops by `AnnDataSingleCellDataLoader`.

## Deferred (deliberately)

- **OBL_UNSATISFIED_OBLIGATION** on `ExpressionExperimentDaoImpl.streamCellIds`
  / `streamCellLevelCharacteristics` — the `PreparedStatement` and
  `ResultSet` are owned by the session lifecycle managed by
  `QueryUtils.createStream`. Closing them in the lambda would close
  before the returned stream is consumed.
- **SE_BAD_FIELD** on `Geo*` and `Task*Command` and various heatmap VOs
  — these hold mutable entities (`BioAssaySet`, `ExpressionExperiment`,
  `Factor*`). Marking `transient` is plausible but each one has
  different serialization-actually-used semantics; needs a design pass.
- **FileTools.getInputStreamFromPlainOrCompressedFile** — intentionally
  returns an InputStream to the caller; `@SuppressWarnings("resource")`
  reflects this; caller-owns-close contract.
- **AffyScanDateExtractor / AffyChipTypeExtractor** DLS — the dead-local
  reads have side effects (advancing `DataInputStream`) and already carry
  `@SuppressWarnings("unused")`.
- **ArrayDesignAnnotationServiceImpl.create** L220/223/226 — three Path
  locals get re-declared and used later in the method; harmless dead
  store but `getFileName` may have side effects worth keeping.

## Validation

`mvn -pl gemma-core,gemma-rest,gemma-cli compile test-compile -q` clean
after each commit (output empty).

## Branch tip

`b4da9e3efe` — ready for `--no-ff` merge into `phase2-acl-migrate`.
