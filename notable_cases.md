## Spring-MVC legacy cruft — SimpleFormController shim (2026-05-19)

**Case**: PR [#508](https://github.com/PavlidisLab/Gemma/pull/508) ("Move to Spring 4", still OPEN as of 2026-05-19) flagged Spring's removal of `SimpleFormController` as a migration blocker years ago. The shim `gemma-web/src/main/java/ubic/gemma/web/compat/SimpleFormController.java` (96 LoC, the sole file in the `compat/` package) was created as a compile-time stand-in. Today, exactly one caller remains: `ArrayDesignFormController` (167 LoC, carrying a FIXME marker pointing back at the same problem).

**Why it matters**: 263 LoC of legacy Spring-MVC cruft that has no real users — the controller is part of `gemma-web` which is on the chopping block (`project_gemma_web_replacement.md`). PR #508 and Issue #116 ("Migrate to Spring 5") were both filed to plan this work; we're now four major Spring versions past their target (current: Spring 6.2 / Hibernate 6.6 / Spring Security 6.5).

**Lesson**: When `gemma-web` is retired, the entire `web/compat/` package and `ArrayDesignFormController` go with it. No separate cleanup needed.

**Fix scope**: 0 LoC needed now; deletion happens automatically with gemma-web retirement. **TODO**: close GitHub PR #508 + Issue #116 with a "superseded by Phase 3" comment.

---

## ArrayDesignReportServiceTest baseline failures (2026-05-19)

**Case**: 3/3 tests in `ArrayDesignReportServiceTest` fail with `assertTrue(!report.equals("[None]"))` at lines 78/88/98 — the SUT's `getLast*Event(id)` returns the no-event sentinel even though `setUp()` has explicitly inserted 5 typed `AuditEvent`s via `AuditTrailService.addUpdateEvent(...)`.

Reproduction needs the canonical mvn invocation (failsafe lifecycle, port 3306 / user `root`, password from keychain `mysql-root`, AND `-Dgemma.hibernate.hbm2ddl.auto=create`); the bare `mvn test ...` invocation in the brief picks zero tests because the surefire `excludedGroups` filter drops every `BaseSpringContextTest` subclass (they're `@Tag("integration")`).

**Root cause**: Hibernate 6 / unidirectional `one-to-many` regression around the `AuditTrail → AuditEvent` collection, NOT broken test-setup wiring. Direct DB inspection after a failed run shows trail id=41 with 6 correctly-linked AUDIT_EVENT rows (1 CREATE from `AuditAdvice` + 5 typed UPDATE events from the test's `addUpdateEvent` calls), all carrying `AUDIT_TRAIL_FK=41` and the right `EVENT_TYPE_FK`. The SUT's `AuditEventDaoImpl.getEvents` HQL (`select e from AuditTrail t join t.events e where t = :at order by e.date, e.id`) executes with the right parameter but returns 1 event for test 1 (the CREATE, `event_type=NULL`, which the SUT then filters out) and 0 events for tests 2 and 3 (no eventType lookup follows the events query). Smell: collection-cache / query-cache + `mutable="false"` on both `AuditTrail.hbm.xml` line 7 and `AuditEvent.hbm.xml` line 6, combined with `<cache usage="read-only"/>` on AuditEvent — INSERT-then-set-FK across separate `@Transactional` boundaries is leaving a stale empty-bag in the L2 / query cache that subsequent reads honour over the DB truth.

**Why it matters**: any code path that writes audit events outside the same Hibernate session that originally loaded the parent's audit trail will hit this — covers every `addUpdateEvent(...)` invoked from a service method on an entity that was loaded earlier in the request. Production curators will see "[None]" displays in array-design reports even when audit events have been recorded. Whatever Phase 2/3 commit broke this almost certainly broke a wider class of audit-event reads than just this one test class.

**Lesson**: Hibernate 6's stricter handling of `mutable="false"` parent + cascading `cache usage="read-only"` children turns previously-tolerated cross-session writes into silent cache-staleness bugs; any HBM with `mutable="false"` deserves an audit during the Phase 2/3 Hibernate-6 migration, not just the obvious column-or-association regressions.

**Fix scope**: NOT a clean small fix. Options ranked by expected effort:
1. Remove `mutable="false"` from `AuditTrail.hbm.xml` (one-line change) — low risk per se but needs follow-up review for every code path that previously relied on Hibernate skipping update-flush for trails, plus regression-testing the audit-event read path across the dozens of integration tests that load trails.
2. Remove `<cache usage="read-only"/>` from `AuditEvent.hbm.xml` (one-line change) — would relax the entity-cache constraint; cheaper but may have perf implications across hot audit-event reads.
3. Convert the `AuditTrail.events` bag to a bidirectional mapping with `inverse="true"` and an explicit `auditTrail` back-reference on `AuditEvent` — the architecturally correct fix but multi-file, multi-table-spanning, and touches the entire audit subsystem.
4. Add an explicit `sessionFactory.getCurrentSession().refresh(trail)` in `AuditEventDaoImpl.getEvents` before the HQL query — surgical workaround for the read path; doesn't fix the underlying cache-staleness but unblocks the test.

Recommend: **defer** — this is a Hibernate-6-migration issue that wants a deliberate fix decision (with audit-subsystem owner sign-off on which option), not a quick patch slipped in under a Phase 3 fixture cleanup. None of the options is <50 LoC + <3 files + clearly-correct under the brief's gate.

**Branch**: `phase2-acl-migrate` (worktree `agent-a00d3a5957c3cdc95`, baseline commit `29b21206c2`).

---

## ExpressionExperimentDaoTest 18 TransientObjectException failures (2026-05-19)

**Case**: 18/55 tests in `gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentDaoTest.java` fail in the `@After removeFixtures` + `flushAndClearSession` teardown chain with two stacked `org.hibernate.TransientObjectException` errors per test. 17 of the 18 cite `QuantitationType`; one (`testReplaceRawDataVectorsWithNewDimension`) cites `BioAssayDimension`. The test bodies themselves pass; every assertion succeeds. Failures land entirely in cleanup. Failing tests share one pattern: they exercise vector / QT lifecycle (`testAddRawDataVectors`, `testCreateProcessedDataVectors*`, `testReplaceProcessedDataVectors*`, `testReplaceRawDataVectors*`, `testRemoveRawDataVectors*`, `testRemoveProcessedDataVectors`, `testRemoveAllRawDataVectors`, `testGetRawDataVectors`, `testGetGenesUsedByProcessedVectors`, `testGetArrayDesignUsed`, `testRemoveRawDataVectorsWhenQtIsUnknown`). Tests that don't touch the vector graph are green.

Reproduction (single test, surefire, H2 in-memory): `JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test -pl gemma-core -Dtest='ExpressionExperimentDaoTest' -Dgemma.testdb.password=$(security find-generic-password -s mysql-root -w) -DskipUnitTests=false -DskipIntegrationTests=true -Dgemma.hibernate.hbm2ddl.auto=create`.

**Root cause (working hypothesis after Hibernate-6 cascade tracing)**: `ExpressionExperimentDaoImpl.remove(ee)` (line 2334) sequentially calls `removeAllRawDataVectors(ee, true)` (line 2404) then `removeProcessedDataVectors(ee, true)` (line 2405). `removeAllRawDataVectors` issues an HQL `delete from RawExpressionDataVector` (executeUpdate, line 4316), then `session.delete(qt)` per QT via `removeQts` (line 4321), then `update(ee)` ≡ `session.merge(ee)` (line 4322). The HQL bulk delete clears the DB rows but leaves the in-session `RawExpressionDataVector` instances managed and dangling. The QT `session.delete()` calls put the QTs in `DELETING` state. `merge(ee)` re-cascades the whole graph including `ee.getQuantitationTypes()` (cascade="all"), which re-imports those half-dead QTs. When `removeProcessedDataVectors`'s first executeUpdate triggers an autoflush, Hibernate 6's `ACTION_CHECK_ON_FLUSH` cascade walks the stale RawExpressionDataVector → quantitationType many-to-one and flags the DELETING/half-merged QT as transient, raising `TransientObjectException`. The stack confirms `CascadingActions$9.cascade → cascadeToOne → cascadeAssociation → cascadeProperty → cascade ← AbstractFlushingEventListener.prepareEntityFlushes`.

Concrete trace from `org.hibernate.engine.internal.Cascade` at TRACE level on `testAddRawDataVectors` shows the autoflush walking `RawExpressionDataVector` with `ACTION_CHECK_ON_FLUSH` directly before the exception fires — confirming the stale-vector / dead-QT pairing.

**Attempted minimal fixes (none worked)**:
1. `sessionFactory.getCurrentSession().flush()` at the top of `@After removeFixtures` (before `dao.load() + dao.remove()`) — flush itself failed with the same TransientObjectException. Means the stale state is present BEFORE remove() is even called: the test body's calls to `addRawDataVectors` / `replaceRawDataVectors` / `removeAllRawDataVectors` already leave the session dirty.
2. `sessionFactory.getCurrentSession().flush()` between `removeAllRawDataVectors` and `removeProcessedDataVectors` inside `ExpressionExperimentDaoImpl.remove` — flush at that point fails with the same TransientObjectException. Confirms `removeAllRawDataVectors`'s internal autoflush + subsequent `merge(ee)` is what corrupts the session, not the boundary between the two remove calls.

**Why it matters**: in production, `ExpressionExperimentDaoImpl.remove(ee)` is invoked from `ExpressionExperimentService.remove` and the curator UI's delete flow. With H2 in-memory tests, the cascade walk explodes on the first autoflush; with MySQL prod, the same path runs but may be tolerated by Hibernate 6 because production sessions don't typically have the same dangling-merge state (entities are read fresh per request, deletions run once, the session ends). Still, the underlying inconsistency — vectors deleted via bulk HQL while their QT references are session-resident — is a latent bug that would surface as soon as a service method tries to do anything else with the same session after a vector cleanup.

**Fix scope**: this is NOT a small / surgical fix. The candidates:
1. **Refactor `ExpressionExperimentDaoImpl.remove` to manage session state explicitly**: between `removeAllRawDataVectors` and `removeProcessedDataVectors`, evict the stale raw-vector entities (`session.clear()` is too aggressive; need a targeted eviction), or flush + clear-then-reload-ee. Touches the SUT, requires careful re-testing of every code path that calls `EE remove` (deletion of full experiments, of subsets, of vectors). ~100-200 LoC across `ExpressionExperimentDaoImpl.java` + likely the service layer; high integration-test risk.
2. **Change `removeAllRawDataVectors` to NOT call `update(ee)` (the trailing merge)**: the merge re-cascades QTs that have just been deleted, which is the proximate trigger. But `update(ee)` exists for a reason (cache eviction? denormalised counts?); removing it needs a careful read of every caller (single-cell pipeline, replace-data flow, etc.).
3. **Change cascade configuration on `Investigation.quantitationTypes` from `cascade="all"` to `cascade="save-update"`** in `Investigation.hbm.xml` — would stop merge from re-cascading QTs. But changes production cascade semantics broadly; needs a sweep of every site that relies on QT being deleted when EE is deleted.
4. **Test-only workaround: in each failing test, explicitly `session.flush(); session.clear();` after each `addRawDataVectors` / `replaceRawDataVectors` / `removeAllRawDataVectors` call and reload `ee` before continuing**. Surgical, isolated to the test file, but adds noise to ~18 tests and doesn't fix the latent production bug.

Recommend: **defer to a focused Hibernate-6-vector-lifecycle session**. None of options 1-3 is <100 LoC + <3 files + obviously safe. Option 4 is the only fast unblock and is purely cosmetic. The bug pattern is consistent with the broader Phase-2/3 Hibernate-6 migration issues already documented above (ArrayDesignReport `mutable="false"` cache staleness, the `replaceProcessedDataVectors` "stale PersistentSet snapshot" fix at commit `e304d1c2b3`) — the cascade engine in Hibernate 6 is intolerant of patterns Hibernate 5 silently accepted, and the EE-vector cleanup path predates that strictness.

**Failure count**: 18 before, 18 after both attempted fixes (reverted).

**Branch**: `phase2-acl-migrate` (worktree `agent-a9ba7a302536b7f89`, baseline commit `82ed58d8cf`).
