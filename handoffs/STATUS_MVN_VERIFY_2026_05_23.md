# mvn verify status — 2026-05-23

## Headline

**Surefire BUILD FAILURE.** Failsafe never ran. Single-source regression
introduced by commit `739b5fc86c` (fix(acl): cache SessionFactory at
init) blew up 9 unit-test classes; `AclSemanticsContractTest` and 5
DAO tests that exercise ACL-filtered HQL all blow up with the same
`IllegalStateException: AclQueryUtils.sessionFactory not set;
AclClassIdInitializer must run before any ACL-filtered query.`

## Run metadata

- **Branch**: `agent-mvn-verify-smoke`
- **Worktree**: `/Users/pzoot/Dev/eclipseworkspace/Gemma/.claude/worktrees/agent-mvn-verify-1779531249`
- **Baseline SHA**: `d46e689ce1` (Merge agent-fix-rest-status-doc-refresh)
- **Started**: 2026-05-23 ~03:15 PDT
- **Finished**: 2026-05-23 03:20 PDT
- **Runtime**: 5:25 (surefire only — failsafe did not run because surefire failed)
- **EXIT**: `1`

## Surefire summary

```
[ERROR] Tests run: 1526, Failures: 53, Errors: 15, Skipped: 18
```

| Category | This run | Prior snapshot (05-22, failsafe) | Note |
|---|---|---|---|
| Tests run | 1526 (surefire) | 376 (failsafe) | Different phases — not directly comparable |
| Failures | 53 | 1 | All 53 caused by AclQueryUtils.sessionFactory null |
| Errors | 15 | 1 | Same root cause as failures |
| Skipped | 18 | 9 | Tagged-out + ArchUnit allowEmptyShould skips |

The 05-22 baseline (376/1F+1E+9S) was a **failsafe** (integration)
count; surefire passed cleanly then. Today surefire breaks, so
failsafe was never reached. Apples-to-apples re-baseline of failsafe
is blocked behind fixing surefire.

## Per-failure triage

### Bucket A: AclQueryUtils.sessionFactory null (single root cause)

**Root cause.** `AclClassIdInitializer` (`gemma-core/src/main/java/ubic/gemma/persistence/util/AclClassIdInitializer.java`,
introduced in `739b5fc86c`) is the only writer of the static
`AclQueryUtils.sessionFactory`. It's a `@Component` so production
component-scan picks it up, but **no test
context-configuration registers it as a bean** — `BaseDatabaseTest5`
and `BaseDatabaseTestContextConfiguration` don't include it, and the
per-test `@TestComponent` configs in the failing test files don't
declare it either. With the initializer absent the static field stays
null, and `AclQueryUtils.resolveAclClassId` throws on the first ACL
predicate-rendering call.

**Affected unit-test classes** (all blowing up with the same
exception):

| Class | F | E | First cause |
|---|---|---|---|
| `ubic.gemma.persistence.util.AclSemanticsContractTest` | 50 | 0 | All parameterized rows hit the ACL path |
| `…service.expression.experiment.ExpressionExperimentDaoTest` | 0 | 3 | testFilterAndCountByArrayDesign, testFilterWithStatementSecondObject, testGetPerTaxonCount |
| `…service.expression.experiment.ExpressionExperimentDaoCursorTest` | 0 | 5 | testBackwardCursorReturnsPreviousPage + 4 others |
| `…service.expression.arrayDesign.ArrayDesignDaoTest` | 0 | 1 | testCountExpressionExperiments |
| `…service.expression.designElement.CompositeSequenceDaoTest` | 0 | 2 | testLoad, testLoadIds |
| `…service.expression.bioAssayData.CachedProcessedExpressionDataVectorServiceTest` | 0 | 3 | testGetVectors{,ForSubset,ForSubBioAssays} |
| `…service.genome.GeneDaoTest` | 0 | 1 | testGetCompositeSequences |

**Fix scope.** One edit. Register `AclClassIdInitializer` in
`BaseDatabaseTestContextConfiguration` (or wherever the shared test
DAO wiring lives) so every test context gets it. Alternative: drop
the static-singleton pattern and inject the SessionFactory directly
into `AclQueryUtils.resolveAclClassId` callsites. The first is the
minimum-disturbance fix.

### Bucket B: ProcessedExpressionDataVectorDaoTest.testGetProcessedVectors

```
expected: <100> but was: <0>
```

at `ProcessedExpressionDataVectorDaoTest.java:116`. Likely
downstream of the same ACL break — the query renders to a clause that
short-circuits to zero rows when ACL injection fails silently rather
than throwing. Worth confirming after the initializer wiring is fixed
that this case also clears; if not, treat as a separate regression.

### Bucket C: AutowireImplRuleTest (2 failures)

```
Rule '... should have raw type have a simple name ending with 'Impl'…'
failed to check any classes.
```

ArchUnit `failOnEmptyShould` complaining — the `that()` clause has
nothing to match. Either the rule is genuinely vacuous now (we
finished the Impl→interface migration so there's nothing left to
guard against), or the clause's package/class filter is too narrow.
Lowest-disturbance fix: set `.allowEmptyShould(true)` on both
guards, with a Javadoc noting "intentionally vacuous when the
codebase is clean." Not blocking the release.

## Recommendations

1. **Immediate**: register `AclClassIdInitializer` in
   `BaseDatabaseTestContextConfiguration` (or equivalent shared test
   wiring). Without this, every ACL-filtered HQL DAO test stays red
   — failsafe re-baseline can't proceed.
2. **After surefire green**: re-run `mvn verify` to re-baseline
   failsafe against the 05-22 snapshot (376/1F+1E+9S).
3. **Cleanup**: flip `AutowireImplRuleTest` rules to
   `allowEmptyShould(true)` — vacuous-but-intentional guards.
4. **Confirm Bucket B**: re-check `testGetProcessedVectors` once the
   initializer wiring is fixed; if still red, file as a separate
   regression with its own RCA.

## File pointers

- `gemma-core/src/main/java/ubic/gemma/persistence/util/AclClassIdInitializer.java` (the @Component that doesn't get picked up in tests)
- `gemma-core/src/main/java/ubic/gemma/persistence/util/AclQueryUtils.java` (the static-singleton consumer)
- `gemma-core/src/test/java/ubic/gemma/core/util/test/BaseDatabaseTest5.java` (test base — does NOT register the initializer)
- `gemma-core/src/test/java/ubic/gemma/persistence/util/AclSemanticsContractTest.java` (the canary; 50/106 parameterized rows red)
- `/tmp/mvn-verify.txt` (full surefire transcript, retained)
- Commit `739b5fc86c` (the change that introduced the static-singleton init pattern)
