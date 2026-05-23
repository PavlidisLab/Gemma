# Recce — fast-mock unit test for DataUpdater

**Filed:** 2026-05-23, mid-Agent-A-flight.
**Status:** Queued. Fires the moment `agent-fix-dataupdater-qt-collision` reports back with the production fix.
**Owner:** Single focused agent; gemma-core surefire only (NOT gemdtest).

## Why

Paul, 2026-05-23: "do we have a fast-mock version of [DataUpdaterTest]?" — no.

The four tests that exercise `DataUpdater` are ALL `@Tag("slow")` integration tests extending `AbstractGeoServiceTest5`:

| Test | Tag | What |
|---|---|---|
| `DataUpdaterTest` (3 methods) | per-method `@Tag("slow")` | full GEO load + persist |
| `ExonArrayDataAddIntegrationTest` | class `@Tag("slow")` | exon array variant |
| `MeanVarianceServiceTest` | `@Tag("geo")` + `@Tag("slow")` | MV downstream of `addCountData` |
| `DiffExTest` | (slow) | DEA downstream |

Default `mvn verify` exercises **zero** of `DataUpdater.addCountData`'s persistence chain. The QT-collision bug the parallel agent is currently fixing has been silently hiding for who knows how long because slow-tagged tests don't fire in CI.

## What to add

A fast unit / `BaseDatabaseTest5`-level test that exercises `DataUpdater.addCountData` end-to-end through the persistence layer, with all GEO-load and BLAT plumbing mocked. Same pattern as the GoldenPath / BLAT plays (mock the external, keep the integration as truth source).

Suggested file: `gemma-core/src/test/java/ubic/gemma/core/loader/expression/DataUpdaterUnitTest.java` (or `DataUpdaterFastTest.java` — pick a name that doesn't collide).

### Surface

- Wire the real `DataUpdaterImpl` against an H2 `BaseDatabaseTest5` context.
- Mock everything `DataUpdaterImpl` autowires that touches BLAT / GEO / NCBI: `arrayDesignSequenceProcessingService`, `arrayDesignSequenceAlignmentService`, `arrayDesignProbeMapperService`, etc.
- Construct a small in-memory `ExpressionExperiment` + `ArrayDesign` + `BioMaterial`/`BioAssay` fixture in `@BeforeEach`; persist them directly (no GEO fetch).
- Build a tiny `DoubleMatrix<CompositeSequence, BioMaterial>` (e.g. 10 probes × 5 samples) as the count matrix.
- Call `dataUpdater.addCountData(ee, ad, countMatrix, rpkmMatrix, seqMetadata, false)`.
- Assertions:
  - Persisted `RawExpressionDataVector`s have the expected count, the expected `QuantitationType`, the expected `BioAssayDimension`.
  - `EntityExistsException` does NOT fire (the regression-guard assertion for the bug Agent A just fixed).
  - Audit event fired (verify via mock `auditTrailService`).
  - Re-invoking `addCountData` with `replaceExisting=true` correctly replaces vectors (no QT-identity collision on the second call — this is the canonical repro).

### Tagging

- NO `@Tag("slow")`. NO `@Tag("integration")` (it's fast surefire). NO `@Tag("network")`.
- Runs in default `mvn verify`.

### Expected wall-clock

A `BaseDatabaseTest5`-style test with H2 + a tiny fixture: ~1-2 seconds per test method. Whole class < 10s.

## Order of operations for the agent

1. Read Agent A's just-merged production-code fix to understand the exact code path that was wrong. The regression-guard assertion needs to specifically exercise that path.
2. Read `DataUpdaterTest.testLoadRNASeqData` to understand the assertion shape (what counts as "loaded correctly").
3. Pick the cheapest H2 base class that gives a real Hibernate `Session` (`BaseDatabaseTest5` is canonical; check whether `DataUpdaterImpl` has `@Secured` constraints that require Spring's security context too — if so, use `BaseSpringContextTest5` instead).
4. Write the test. Validate:
   ```bash
   export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
   export PATH="$JAVA_HOME/bin:$PATH"
   mvn -pl gemma-core test -Dtest='DataUpdaterUnitTest' \
       -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w)
   ```
5. Re-run the slow `DataUpdaterTest` to confirm Agent A's prod fix didn't get reverted by the new test setup:
   ```bash
   mvn -pl gemma-core verify -DexcludedGroups=network -Dit.test='DataUpdaterTest' ...
   ```
6. Full default-run `mvn -pl gemma-core verify` must remain `surefire 1526+N/0F+0E+18S, failsafe 378/0F+0E+9S` (+N = new test methods landing in surefire).

## Constraints

- **JDK 25 (temurin-25).**
- **No `Co-Authored-By: Claude`.** No "load-bearing".
- **Don't modify the existing `DataUpdaterTest`** — it stays as the slow truth-source.
- **Don't modify `DataUpdaterImpl`** — Agent A's just-landed production fix is the source of truth; the new test verifies it stays fixed.
- One commit.

## Cross-references

- Whatever Agent A merges (production fix). The next-session brief should include that SHA explicitly.
- `handoffs/SLOW_SWEEP_FINDINGS_2026_05_23.md` — original slow-sweep classification (item #2).
- `handoffs/RECCE_SLOW_SWEEP_AS_PERF_PROBE.md` — Paul's framing.
- `handoffs/RECCE_MEX_LOADER_CHOP.md`, `handoffs/RECCE_CSGENEMAPPER_MOCK_GOLDENPATH.md` — same pattern for other slow tests (mock the external, keep the integration as truth source).
