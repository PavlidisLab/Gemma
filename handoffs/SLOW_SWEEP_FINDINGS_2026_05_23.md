# Slow-sweep findings — 2026-05-23

Inputs:
- `handoffs/SLOW_SWEEP_INVENTORY_2026_05_23.md` — the 77-class @Tag("slow") inventory (merged from agent-slow-sweep).
- `handoffs/RECCE_SLOW_SWEEP_AS_PERF_PROBE.md` — Paul's perf-probe framing.

Run: `mvn -pl gemma-core verify -DexcludedGroups=network -Dit.test='!DatasetCombinerTest' -Dfailsafe.timeout=900` on tip `304a2ff854` (post task #63).

## Result shape

Final tallies from Agent F's full run (28:41 wall clock):

- **Surefire**: 1625 / 0F+0E+34S — default baseline (1526) + 99 slow-tagged classes pulled in. Zero failures in this phase.
- **Failsafe**: 506 / 1F+5E+29S — default baseline (376) + 130 slow integration tests pulled in. All six failures listed below.
- **Longest individual class**: 213 s (`MexSingleCellDataLoaderTest`).
- **`BUILD FAILURE` at the end** is a **JVM-shutdown forker artifact** (`maven-failsafe-plugin:verify` reported "timeout in the fork" at process tear-down), NOT a test hang. The failsafe test phase completed, all 506 tests ran. Probable cause: HDF5 / Lucene / Jena native resource not closing on JVM exit; resource-close audit is its own task.

## Surefire failures, classified per Paul's perf-probe lens

| Test | Error | Classification | Probe / fix |
|---|---|---|---|
| `CompositeSequenceGeneMapperServiceTest.setUp` × 2 | `CannotGetJdbcConnection: Failed to obtain JDBC Connection` after `blatCollapsedSequences:194` | **CODE — pool exhaustion / connection leak** | Trace the connection-acquisition path in `loadData:212 → blatCollapsedSequences:194`. Suspect: a holder never returns a connection in an upstream thread; Hikari `maxPoolSize` saturates. Test failing TWICE in a row (both `testGetCompositeSequences` style methods) implies leak, not just slowness. |
| `DataUpdaterTest.testLoadRNASeqData:302` | `EntityExists: A different object with the same identifier value was already associated with the session: QuantitationType#170` | **CODE — Hibernate session merge bug or test-isolation hole** | Either the production loader caches a QT instance across sessions, or the test reuses Spring context state. The two-different-objects-same-id pattern is the canonical "you saved with `session.persist` after a `session.merge` happened upstream" symptom. |
| `TwoWayAnovaWithInteractionTest2.test:161` | `NoDesignElementsException: No rows left after filtering for repetitive values with TooFewDistinctValuesFilter Threshold=30.00%` | **FIXTURE — too-narrow stub data** | The 30% distinct-values filter ate the entire matrix. Either the fixture matrix's variance was always too tight for this filter, OR the filter threshold changed and the fixture wasn't refreshed. Cheap check: print row count + min variance for the input matrix. Then either regen with more variance or pin a smaller threshold for this test. |
| `RNASeqBatchInfoPopulationTest.testGSE156689NoBatchinfo:198` | `UnexpectedRollback: Transaction rolled back because it has been marked as rollback-only` | **NEEDS RECCE — underlying exception is upstream of the rollback** | A swallowed exception inside the tx scope marked it rollback-only; Spring throws `UnexpectedRollback` on commit. Read the surefire `*-output.txt` for the test in the slow-sweep worktree's `target/surefire-reports/` to find the underlying cause. May be code, may be fixture. |
| `HibernateSqmFragileShapesIT.probe_implicitPolymorphismOnUnmappedBase:89` | `UnknownEntityException: Could not resolve root entity 'BulkExpressionDataVector'` | **ORDERING FLAKE — not slow-tagged itself, failed because slow tests ran first** | Agent F flagged this as a cross-class Hibernate-metamodel state coupling: something the slow tests run earlier mutates the metamodel state the SQM probe inspects, surfacing the exception in a shape the probe's `assertThrows`-style guard doesn't unwrap. Recce both sides: which slow test mutates metamodel, and how the probe should be hardened against ordering. |

## Failsafe shutdown forker (separate concern)

**Correction from an earlier draft of this doc:** the failsafe phase did NOT hang at startup — it ran all 506 tests. The `BUILD FAILURE` came from the `maven-failsafe-plugin:verify` goal reporting "There was a timeout in the fork" at JVM shutdown, AFTER the test phase completed. Common cause: a native resource (HDF5 / Lucene / Jena TDB / ehcache disk store) doesn't release on shutdown and the forker waits past its timeout for the JVM to exit cleanly.

This is its own task (audit native-resource closers in the test contexts), not a slow-tagged-test finding.

### `HibernateSqmFragileShapesIT.probe_implicitPolymorphismOnUnmappedBase` — ordering coupling

Agent F observed: this test is NOT slow-tagged, but it **failed specifically in this run** because the slow tests ran first. Suspected mechanism: Hibernate metamodel state-coupling — earlier slow tests register or mutate entity-metadata state (e.g. via a `MassIndexer`, a `BootstrapServiceRegistry`, or a Hibernate Search lifecycle hook) that the SQM probe's expected-exception path now reads differently. This is genuine ordering flake territory and deserves its own recce.

## Action queue for the next perf-probe agent

1. **CompositeSequenceGeneMapperServiceTest pool leak** — first because it's the clearest CODE bottleneck signal.
2. **DataUpdaterTest QT identity collision** — second, similarly CODE-shaped.
3. **TwoWayAnovaWithInteractionTest2 fixture** — third, FIXTURE chop is the playbook.
4. **HibernateSqmFragileShapesIT expected-exception unwrap** — quick test fix; ship alongside as cleanup.
5. **RNASeqBatchInfoPopulationTest rollback-only** — recce only until the underlying cause is named.

Don't fold these into one commit. Per Paul's framing: each probe + fix is a discrete commit with before/after timing.

## What's NOT actionable from this run

- The 71 other slow classes that DIDN'T fail are evidence of nothing on their own — they ran to completion in the surefire phase. The next agent should pull elapsed-time data from `gemma-core/target/surefire-reports/*.xml` (the `time=` attribute on each `<testcase>`) and sort to find the slowest survivors. Those are the next-tier probe candidates even if green.
