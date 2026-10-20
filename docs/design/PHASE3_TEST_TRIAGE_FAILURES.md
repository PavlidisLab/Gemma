# Phase 3 Test Triage — Pre-existing Failure Batch (2026-05-18)

Sibling to `PHASE3_TEST_TRIAGE.md` (the @Ignore audit, on unmerged branch
`worktree-ignore-audit-v2` @ `40a3526017`). This file covers the cluster
of *active* (non-@Ignored) test failures that multiple Phase 3 agents
surfaced and flagged as PRE-EXISTING relative to their own work.

Baseline: branch `phase2-acl-migrate` @ `08e760bdaf` (verified before
triage started).

---

## 1. Failure inventory

| # | Test | Symptom | Class |
|---|---|---|---|
| 1 | `ArrayDesignReportServiceTest` (all 3) | `assertTrue(!report.equals("[None]"))` — audit-event lookup returns `[None]` | fixture/audit |
| 2 | `ExpressionExperimentServiceIntegrationTest.testStreamExperiments` | NPE in `gsec.AclEntryAfterInvocationStreamFilteringProvider:42` | gsec / ACL |
| 3 | `ExpressionExperimentServiceIntegrationTest.testCacheInvalidationWhenACharacteristicIsDeleted` | `c.getId()` is null after `update(ee)` | cascade |
| 4 | `ExpressionExperimentServiceIntegrationTest.testLoadValueObjectsByFactorValueCharacteristic` | `Filter.parseItem` fails on null Long conversion | cascade -> filter |
| 5 | `ExpressionExperimentServiceIntegrationTest.testLoadValueObjectsBySampleUsedCharacteristic` | same as #4 | cascade -> filter |
| 6 | `ArrayDesignServiceTest.testLoadCompositeSequences` | `IllegalArgumentException: max-results cannot be negative` at `ArrayDesignDaoImpl:641` | HB6 |
| 7 | CoexpressionAnalysis tests (3) | HQL errors | HB6 / HQL |
| 8 | `SecureValueObjectAuthorizationTest.testSecuredExpressionExperimentValueObject:94` | `NotFound: Unable to locate a matching ACE for passed permissions and SIDs` | ACL fixture |

---

## 2. Hypothesized root-cause grouping

### Group A — Hibernate 6 strict pagination (HB6 side-effect)

Failure: #6 (`testLoadCompositeSequences`).

Root cause: `ArrayDesignServiceImpl.getCompositeSequences(ArrayDesign)`
calls `arrayDesignDao.loadCompositeSequences(arrayDesign, -1, 0)`. The DAO
unconditionally called `query.setMaxResults(-1)`. Under Hibernate 5 this
was a no-op meaning "no limit"; under Hibernate 6 it throws
`IllegalArgumentException: max-results cannot be negative`.

The codebase already has the canonical "guard" pattern in five other
DAOs (`ExpressionAnalysisResultSetDaoImpl:195`,
`AbstractCriteriaFilteringVoEnabledDao:131,152`,
`AbstractQueryFilteringVoEnabledDao:185,222`):

```java
if ( limit > 0 ) q.setMaxResults( limit );
```

**Disposition: FIXED in this commit** — `ArrayDesignDaoImpl:635-643` now
guards both `setFirstResult` and `setMaxResults`.

### Group B — Cascade-flush-timing on transient sub-entities (test fixture)

Failures: #3, #4, #5.

Root cause: each test does

```java
Statement s = new Statement();          // or Characteristic
fv.getCharacteristics().add( s );        // attach to managed collection
expressionExperimentService.update( ee );// expecting cascade to insert s
assertThat( c.getId() ).isNotNull();     // <- NPE / fails
```

The local Java reference `s` (or `c`) is the transient instance added to
a child collection three levels deep. After `update(ee)` the cascade is
expected to assign an ID, but on Hibernate 6 + the Spring 6 transaction
manager the flush may not be propagating the generated ID back onto the
*originally-added* Java object in all three-level cascade paths
(`ExpressionExperiment -> ExperimentalDesign -> ExperimentalFactor ->
FactorValue -> Statement`; `ExpressionExperiment -> BioAssay ->
BioMaterial -> Characteristic`).

In #4/#5 the symptom then becomes `String.valueOf(null)` -> `"null"` ->
`ConversionService.convert("null", Long.class)` failure inside
`Filter.parseItem`. So #4/#5 are downstream of the same root cause as #3
— not three independent bugs.

**Disposition: DEFERRED.** Not 1-5 line obvious. Need to investigate
whether HB6 changed cascade ID-propagation semantics on collection-of-
collection paths, or whether the test was always racy and only failed
now because Spring 6 changed flush ordering. Probably needs explicit
`flush()` calls in the test, OR a `sessionFactory.getCurrentSession()
.refresh(s)` after `update(ee)`. Recommend assigning to whoever owns
the EE persistence Phase 3 work.

### Group C — gsec ACL machinery (external / Phase B in flight)

Failures: #2 (`testStreamExperiments`), #8 (`SecureValueObjectAuth`).

Both surface inside `gemma.gsec.*` classes. Per task brief, #2 "likely
fixed by the Phase B CS+DV agent's Gemma-owned port"; #8 was
"confirmed pre-existing by Phase B VO agent". The first is a stream-
filtering NPE; the second is an ACE-lookup miss after `makeOwnedByUser`
(probably the post-`makeOwnedByUser` ACL row was not picked up by the
read-side ACL cache flush — same family as the bugs the ACL phase is
already addressing).

**Disposition: DEFERRED — owned by Phase B ACL/VO branches.** Re-test
once those branches merge.

### Group D — Audit-event lookup (fixture / audit-trail)

Failure: #1 (3 sub-tests).

Test creates an ArrayDesign + adds three `addUpdateEvent` calls
explicitly via `AuditTrailService`, then asks `arrayDesignReportService
.getLastSequenceUpdateEvent(ad.getId())` to find them.
`getLastEvent(...)` returns `[None]` when no `AuditEvent` whose
`eventType` is assignable from `ArrayDesignSequenceUpdateEvent.class` is
found in `auditEventService.getEvents(ad)`. Possibilities:

1. The `addUpdateEvent` flushes are happening in a separate
   transaction that the lookup transaction can't see (most likely).
2. `getEvents(ad)` is now filtering out events for which the polymorphic
   `eventType` association doesn't load eagerly (HB6 lazy-loading
   tightened semantics).
3. The static `persisted` flag means data carries across test method
   instances — works on a clean DB run but a stale `persisted=true` from
   a prior session would make subsequent runs use a stale `ad.getId()`.

**Disposition: DEFERRED.** Not 1-5 line obvious. Need to step through
with a debugger to determine which of (1)/(2)/(3) is firing. Static
state at L40-L41 is questionable but is *pre-existing* and not part of
this triage's mandate. Recommend a small follow-up that drops the static
caching and re-checks: if that fixes it -> (3); if not, dig into HB6
audit-event lazy-loading.

### Group E — CoexpressionAnalysis HQL (HB6 / HQL syntax)

Failures: #7 (3 tests).

**Disposition: DEFERRED — already triaged by Phase B VO agent as
pre-existing.** Owned by HQL migration phase.

---

## 3. Applied fixes

| File | Lines | Reason |
|---|---|---|
| `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/arrayDesign/ArrayDesignDaoImpl.java` | 635-643 | Guard `setFirstResult` / `setMaxResults` with `> 0` to match the codebase's HB6 pattern; fixes failure #6. |

Compile verified clean (`mvn compile test-compile -DskipTests -q`).

---

## 4. Deferred items

| Group | Failures | Owner / disposition |
|---|---|---|
| B (cascade flush) | #3, #4, #5 | Investigate HB6 collection-of-collection cascade ID propagation; may need explicit `flush()` in test. Recommend filing as `phase3/ee-cascade-id-flush`. |
| C (gsec ACL) | #2, #8 | Owned by Phase B ACL / VO branches in flight. Re-test post-merge. |
| D (audit lookup) | #1 (x3) | Likely transaction-isolation between `addUpdateEvent` and `getEvents`, or HB6 lazy-loading on `AuditEvent.eventType`. Needs a debugger session. |
| E (Coex HQL) | #7 (x3) | Owned by HQL migration phase per Phase B VO agent's earlier triage. |

---

## 5. gemdtest schema verdict

Reported as "missing core tables" by the E5 agent. **Verified false at
2026-05-18:** `gemdtest` has 84 tables including `INVESTIGATION`,
`CONTACT`, and `TAXON` (all confirmed via `SHOW TABLES LIKE …`). The
schema is intact on this developer machine.

If a future agent does hit a half-bootstrapped state, the recommended
reset procedure (DO NOT auto-run — destructive, drops all test data) is:

```
mvn verify -pl gemma-core \
    -Dgemma.hibernate.hbm2ddl.auto=create \
    -Dit.test='BusinessKeyTest' \
    -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w)
```

`hbm2ddl.auto=create` forces Hibernate to drop + recreate the full
schema during context startup; `BusinessKeyTest` is a cheap IT that
exercises the full mapping. After it passes the schema is rebuilt and
subsequent ITs can run normally.

Better long-term: have a `make reset-gemdtest` target that does the
DROP DATABASE + CREATE DATABASE + Flyway-migrate dance with a single
confirmation prompt. Out of scope for this triage.
