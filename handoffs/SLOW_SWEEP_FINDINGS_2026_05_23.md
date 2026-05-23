# Slow-sweep findings — 2026-05-23

Inputs:
- `handoffs/SLOW_SWEEP_INVENTORY_2026_05_23.md` — the 77-class @Tag("slow") inventory (merged from agent-slow-sweep).
- `handoffs/RECCE_SLOW_SWEEP_AS_PERF_PROBE.md` — Paul's perf-probe framing.

Run: `mvn -pl gemma-core verify -DexcludedGroups=network -Dit.test='!DatasetCombinerTest' -Dfailsafe.timeout=900` on tip `304a2ff854` (post task #63).

## Result shape

- **Surefire**: 506 tests, 1F + 5E + 29S — ran the 77 slow-tagged classes filtered by the surefire-side tag expression. Took ~10 min.
- **Failsafe**: hung at fork startup; 15-min `failsafe.timeout` fired before the first test ran. Spring context boot or pre-test populator stalled — separate concern, not a per-test signal.

## Surefire failures, classified per Paul's perf-probe lens

| Test | Error | Classification | Probe / fix |
|---|---|---|---|
| `CompositeSequenceGeneMapperServiceTest.setUp` × 2 | `CannotGetJdbcConnection: Failed to obtain JDBC Connection` after `blatCollapsedSequences:194` | **CODE — pool exhaustion / connection leak** | Trace the connection-acquisition path in `loadData:212 → blatCollapsedSequences:194`. Suspect: a holder never returns a connection in an upstream thread; Hikari `maxPoolSize` saturates. Test failing TWICE in a row (both `testGetCompositeSequences` style methods) implies leak, not just slowness. |
| `DataUpdaterTest.testLoadRNASeqData:302` | `EntityExists: A different object with the same identifier value was already associated with the session: QuantitationType#170` | **CODE — Hibernate session merge bug or test-isolation hole** | Either the production loader caches a QT instance across sessions, or the test reuses Spring context state. The two-different-objects-same-id pattern is the canonical "you saved with `session.persist` after a `session.merge` happened upstream" symptom. |
| `TwoWayAnovaWithInteractionTest2.test:161` | `NoDesignElementsException: No rows left after filtering for repetitive values with TooFewDistinctValuesFilter Threshold=30.00%` | **FIXTURE — too-narrow stub data** | The 30% distinct-values filter ate the entire matrix. Either the fixture matrix's variance was always too tight for this filter, OR the filter threshold changed and the fixture wasn't refreshed. Cheap check: print row count + min variance for the input matrix. Then either regen with more variance or pin a smaller threshold for this test. |
| `RNASeqBatchInfoPopulationTest.testGSE156689NoBatchinfo:198` | `UnexpectedRollback: Transaction rolled back because it has been marked as rollback-only` | **NEEDS RECCE — underlying exception is upstream of the rollback** | A swallowed exception inside the tx scope marked it rollback-only; Spring throws `UnexpectedRollback` on commit. Read the surefire `*-output.txt` for the test in the slow-sweep worktree's `target/surefire-reports/` to find the underlying cause. May be code, may be fixture. |
| `HibernateSqmFragileShapesIT.probe_implicitPolymorphismOnUnmappedBase:89` | `UnknownEntityException: Could not resolve root entity 'BulkExpressionDataVector'` | **TEST ASSERTION SHAPE — the regression guard is firing on its expected case** | This is literally what the SQM probe checks ("`BulkExpressionDataVector` is an unmapped base; HQL `FROM BulkExpressionDataVector` must throw"). The test was expecting the exception via `assertThrows`-equivalent and the exception arrived wrapped in `IllegalArgumentException`. Tighten the probe's expected-exception unwrap (e.g. check `getCause() instanceof UnknownEntityException` rather than the wrapping class). Test fix, not a real regression. |

## Failsafe-fork hang (separate concern)

The failsafe phase started, allocated its fork, and never reported a `Running ubic…` line before the 15-min `failsafe.timeout` killed it. Probable causes (any):

1. Spring context init scanning a slow-sweep-bloated gemdtest schema.
2. `MassIndexer` triggered on the new (denser) corpus.
3. ehcache warm-up over the larger persisted-vector tables.

This is NOT a slow-tagged-test finding. It's a **first-failsafe-test cold-start cost** that the slow surefire phase amplified by writing more rows than usual. Independent recce when needed; not part of the perf-probe lens.

## Action queue for the next perf-probe agent

1. **CompositeSequenceGeneMapperServiceTest pool leak** — first because it's the clearest CODE bottleneck signal.
2. **DataUpdaterTest QT identity collision** — second, similarly CODE-shaped.
3. **TwoWayAnovaWithInteractionTest2 fixture** — third, FIXTURE chop is the playbook.
4. **HibernateSqmFragileShapesIT expected-exception unwrap** — quick test fix; ship alongside as cleanup.
5. **RNASeqBatchInfoPopulationTest rollback-only** — recce only until the underlying cause is named.

Don't fold these into one commit. Per Paul's framing: each probe + fix is a discrete commit with before/after timing.

## What's NOT actionable from this run

- The 71 other slow classes that DIDN'T fail are evidence of nothing on their own — they ran to completion in the surefire phase. The next agent should pull elapsed-time data from `gemma-core/target/surefire-reports/*.xml` (the `time=` attribute on each `<testcase>`) and sort to find the slowest survivors. Those are the next-tier probe candidates even if green.
