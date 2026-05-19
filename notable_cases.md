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
