# Phase 3 — `@Ignore` Test Audit

Branch: `worktree-ignore-audit-v2` (off `phase2-acl-migrate` @ `08e760bdaf`).

## Scope

Audit every test annotated with `@Ignore` (or `@org.junit.Ignore`) across the
monorepo, and decide for each: (a) re-enable + fix, (b) delete, or (c) leave
with a clearer marker + tracking note.

The task brief calls out `// PHASE_2_RESIDUAL` markers — the actual codebase
uses no such marker comment. The closest equivalent is `@Ignore` annotations
whose comment string explicitly invokes "Phase 2" or "Phase 3". A whole-tree
grep for `@Ignore` surfaced 44 entries; only **two** are Phase-2/3 residuals
in the strict sense. The rest are pre-existing flaky / CI-timeout / external-
service-broken / known-slow-to-run skips that the renovations stream did not
touch and that are out of scope for this audit (they remain as-is).

## Disposition table

| Status | File / line | Test method | Comment / rationale | Action taken |
|---|---|---|---|---|
| **RE-ENABLED** | `gemma-core/.../CharacteristicDaoTest.java:296` | `testGetParents` | "Hibernate 5 removed `SessionFactory.getAllClassMetadata()`; the entity-walk at line 344 needs rewriting against the JPA metamodel." | Body was already ported to `sessionFactory.getMetamodel().getEntities()` in an earlier Phase 2 round — the `@Ignore` is stale. Removed `@Ignore`; test passes against `gemdtest`. |
| **SPLIT** | `gemma-core/.../DiseaseOntologyTest.java:19` | `test` | "Blocked on Phase 3 search-subsystem rebuild: baseCode's renovations branch gutted the Lucene 3 ontology indexer […]. The `getTerm`-by-URI assertions further down do not need Lucene and would still pass — re-enable this test once `findTerm` works." | Split into two methods. `testGetTermByUri` (URI lookups; no Lucene dependency) is re-enabled and passes. `testFindTerm` (the `findTerm` call) stays `@Ignore`'d, but with a tighter marker pointing at this doc. |
| _no Phase-2/3 residuals deleted_ | — | — | — | None of the surveyed @Ignore'd tests were guarding deleted AOP/ACL/AfterReturning machinery — that machinery's tests either (a) were rewritten in place during the ACL cutover, not @Ignore'd, or (b) live under `gemma-core/.../security/authentication/` and `.../security/authorization/acl/` which are claimed by other agents. |

## Out-of-scope `@Ignore` entries (left untouched)

The remaining 42 `@Ignore` entries (full list below) carry rationales unrelated
to Phase 2/3 — CI flakes, missing remote resources, slow integration tests,
external tools like RepeatMasker, etc. They were not touched.

### Full inventory of `@Ignore` entries surveyed

```
gemma-core/src/test/java/ubic/gemma/core/metrics/GenericMeterRegistryConfigurerTest.java:110         @Timed on interface — issue #541
gemma-core/src/test/java/ubic/gemma/core/context/AsyncBeanAutowiringTest.java:102                    Spring 3 parametrized-bean injection — issue #612
gemma-core/src/test/java/ubic/gemma/core/context/AsyncBeanAutowiringTest.java:108                    Spring 3 parametrized-bean injection — issue #612
gemma-core/src/test/java/ubic/gemma/core/analysis/preprocess/VectorMergingServiceTest.java:130       (no reason)
gemma-core/src/test/java/ubic/gemma/core/analysis/preprocess/ProcessedExpressionDataCreateServiceTest.java:118  Randomly fails — issue #1158
gemma-core/src/test/java/ubic/gemma/core/analysis/preprocess/ProcessedExpressionDataCreateServiceTest.java:279  (no reason)
gemma-core/src/test/java/ubic/gemma/core/analysis/report/WhatsNewServiceTest.java:22                 Fails randomly on CI
gemma-core/src/test/java/ubic/gemma/core/analysis/expression/diff/SubsettedAnalysis2Test.java:110    (no reason)
gemma-core/src/test/java/ubic/gemma/core/analysis/expression/diff/DiffExWithInvalidInteraction2Test.java:112    UnknownLogScaleException — issue #582
gemma-core/src/test/java/ubic/gemma/core/analysis/expression/diff/SubsettedAnalysis3Test.java:114    (no reason)
gemma-core/src/test/java/ubic/gemma/core/analysis/expression/diff/DiffExMetaAnalyzerServiceTest.java:157         (no reason)
gemma-core/src/test/java/ubic/gemma/core/analysis/expression/diff/DifferentialExpressionAnalyzerServiceTest.java:69   class-level: randomly fails on CI
gemma-core/src/test/java/ubic/gemma/core/analysis/expression/diff/DifferentialExpressionAnalyzerServiceTest.java:167  (no reason)
gemma-core/src/test/java/ubic/gemma/core/analysis/sequence/RepeatScanTest.java:41                    RepeatMasker broken — issue #53
gemma-core/src/test/java/ubic/gemma/core/analysis/expression/diff/TwoWayAnovaWithInteractionsTest2.java:76        (no reason)
gemma-core/src/test/java/ubic/gemma/core/analysis/sequence/ShellDelegatingBlatTest.java:41           Slow
gemma-core/src/test/java/ubic/gemma/core/analysis/sequence/ProbeMapperTest.java:151                  (no reason)
gemma-core/src/test/java/ubic/gemma/core/util/BuildInfoTest.java:59                                  Manifest not available during test phase
gemma-core/src/test/java/ubic/gemma/core/ontology/GemmaAndExperimentalFactorOntologyTest.java:42     class-level: timing out on CI
gemma-core/src/test/java/ubic/gemma/core/ontology/providers/DiseaseOntologyTest.java:19              **PHASE 3 — handled (split)**
gemma-core/src/test/java/ubic/gemma/core/loader/expression/ExonArrayDataAddIntegrationTest.java:48   class-level: broken + slow
gemma-core/src/test/java/ubic/gemma/core/loader/expression/singleCell/transform/SingleCell10xMexFilterTest.java:185  Pre-GEM 10x unsupported
gemma-core/src/test/java/ubic/gemma/core/loader/expression/arrayDesign/ArrayDesignSequenceProcessorTest.java:129     Issue #1082
gemma-core/src/test/java/ubic/gemma/core/loader/expression/singleCell/SingleCellDataLoaderServiceIntegrationTest.java:49    (no reason)
gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/service/GeoBrowserTest.java:112       Broken since 2025-03-25
gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/GeoSingleCellDetectorTest.java:111, :155, :229, :256, :290, :386, :521, :629, :702, :744   Various: slow / external GEO state / zip-corruption
gemma-core/src/test/java/ubic/gemma/core/loader/expression/arrayExpress/SDRFFetcherTest.java:44      Missing remote file — issue #766
gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/service/GeoDatasetServiceTest.java:325                Randomly fails on CI
gemma-core/src/test/java/ubic/gemma/persistence/service/analysis/expression/diff/ExpressionAnalysisResultSetServiceTest.java:51    Issue #518
gemma-core/src/test/java/ubic/gemma/persistence/service/common/description/CharacteristicDaoTest.java:175                          H2 group-by aggregator — open FIXME
gemma-core/src/test/java/ubic/gemma/persistence/service/common/description/CharacteristicDaoTest.java:296            **PHASE 2 — handled (re-enabled)**
gemma-web/src/test/java/ubic/gemma/web/controller/common/description/bibref/BibRefControllerTest.java:49             class-level: CGLIB proxy issue
gemma-cli/src/test/java/ubic/gemma/cli/util/CompletionGeneratorTest.java:65                          Does not work on CI
gemma-cli/src/test/java/ubic/gemma/apps/NCBIGene2GOAssociationLoaderCLITest.java:90                  Too slow — issue #1056
```

## Counts

* Found: **44** `@Ignore` entries.
* Re-enabled (Phase 2/3 residual cleared): **2** (`CharacteristicDaoTest.testGetParents`; `DiseaseOntologyTest.testGetTermByUri`).
* Deleted: **0** — no @Ignore'd test was found to be guarding deleted AOP/ACL/AfterReturning machinery within this audit's territory.
* Kept with clearer marker: **1** (`DiseaseOntologyTest.testFindTerm`, narrowed scope, marker now points at this doc).
* Deferred (out of scope — not Phase 2/3 residuals): **41**.

## Verification

```
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test -pl gemma-core \
  -Dtest=CharacteristicDaoTest#testGetParents \
  -DfailIfNoTests=false \
  -Dgemma.hibernate.hbm2ddl.auto=update
# -> Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test -pl gemma-core \
  -Dtest=DiseaseOntologyTest \
  -DfailIfNoTests=false \
  -Dgemma.hibernate.hbm2ddl.auto=update
# -> Tests run: 2, Failures: 0, Errors: 0, Skipped: 1
```
