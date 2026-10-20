# Audit migration Phase C — scoping recce

**Filed:** 2026-05-19. Branch `audit-phaseC-recce`, baseline
`aa18f8a323a36ca13f07a370c08b43a403bdda02` (phase2-acl-migrate HEAD at
session close 2026-05-19).

**Companion docs:**
- `AUDIT_SYSTEM_AUDIT.md` — full current-state audit (parent recce).
- `AUDIT_AS_WORKFLOW_RECCE.md` — Ticket-layer follow-on that piggybacks
  on the `@Audited` migration.
- `SESSION_CLOSE_NOTE_2026-05-19.md` — flags this as blocker; "needs
  PostInsert/PreDelete listeners + IT validation".

**Doc-only.** No Java changes in this commit.

---

## 0. Where things stand today

Phases A + B + B-2 + B-3 have shipped. The repo HEAD has:

- `gemma-core/src/main/java/ubic/gemma/core/security/audit/Audited.java`
  — annotation (`value` + `message` + `messageSpel`).
- `gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditedAspect.java`
  — `@AfterReturning` aspect, 229 LoC, `@Order(5)`, payload + SpEL +
  `ApplicationEventPublisher` chain.
- `gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditEventPayload.java`
  — typed payload contract (`@JsonTypeInfo`).
- `gemma-core/src/test/java/ubic/gemma/core/security/audit/AuditedAspectTest.java`
  — unit coverage of the aspect (arg resolution, SpEL, payload).

The legacy aspect `AuditAdvice.java` (373 LoC, `@Order(4)` on
`@Before` advices for `creator/updater/saver/deleter`) is **still
live** and still owns:

- Generic auto-UPDATE/SAVE emission on every DAO `update*`/`save*` call.
- Generic auto-CREATE on every DAO `create*`/`add*`/`findOrCreate*`/`persist*` call.
- Generic auto-DELETE on every DAO `remove*`/`delete*` call.
- Cascade walking via Hibernate `CascadeStyle` (BFS through the first
  argument's collection / map / iterable shapes).
- `Curatable + UPDATE` side-effect:
  `curatableDao.updateCurationDetailsFromAuditEvent(...)` at
  `AuditAdvice.java:334`.

A complementary Hibernate listener `AuditTrailEventListener` (77 LoC,
`PersistEventListener`) already exists but **only guarantees the
AuditTrail field is non-null at persist time** — it does NOT write
audit-event rows.

The remaining hardcoded imperative callers across `gemma-{core,cli,rest,web}/src/main`:

```
$ grep -rn 'auditTrailService\.add' --include='*.java' gemma-*/src/main | wc -l
76
```

Minus the one self-reference inside `AuditedAspect.java:118` (the
aspect calling `addUpdateEventWithPayload` is the canonical writer
under the new world, not a Phase B candidate). Real caller surface:
**75 sites across 27 files.**

(The session-close note's "~59" was a stale estimate; the post-Phase-B
true tail is 75 site-lines. The previous AUDIT_SYSTEM_AUDIT had ~77
total; Phase B-3 absorbed only a handful — most of those 77 still need
either a Phase C strategy or a deliberate "stays imperative" decision.)

---

## 1. Shape inventory — what each of the 75 callers looks like

Caller line `grep -rn 'auditTrailService\.add' --include='*.java'
gemma-*/src/main` was sliced and read in surrounding-method context.
Each call site falls into one of these shapes:

| # | Shape | Count | Phase-C strategy bucket |
|---|---|---:|---|
| 1 | **Single-statement happy-path UPDATE** at the end of a service method (clean `@Audited` candidate) | 22 | Phase C-mech (mechanical annotation sweep, follow-on Phase B continuation) |
| 2 | **Catch-block failure record** — `try { … } catch(Exception e) { auditTrailService.addUpdateEvent(ee, FailedFooEvent.class, msg, e); throw e; }` — `Throwable` overload, REQUIRES_NEW semantics | 14 | **Stays imperative** (Phase B doc already lists this as out-of-scope) |
| 3 | **Conditional emission inside a public method** — `if (someThing) addUpdateEvent(...)` (e.g. `recalculateExperimentBatchEffect` only emits when the effect actually changed) | 9 | Helper / programmatic emission (publish-event pattern) — keep imperative or refactor to a small `AuditEmitter.maybeAudit` helper |
| 4 | **Early-return / mid-method exit shape** — emit a typed event before `return null` from a `catch` or guard arm (e.g. `BatchInfoPopulationHelperServiceImpl` lines 92 + 96 emit on catching `FASTQHeadersPresentButNotUsableException` then `return null`) | 8 | Helper / programmatic emission. `@Audited` is `@AfterReturning` from the *outer* method only — it can't see "I exited via the second arm" |
| 5 | **Private-method audit helper** — single `private void audit(...)` collects the call, called from N public sites with branched `eventType` (e.g. `DataUpdaterImpl.audit(ee, note, replace)` line 736; `ArrayDesignProbeMapperCli.audit(arrayDesign, note, eventType)` line 484) | 4 (helpers, ≈12 call sites upstream) | Split into multiple annotated public methods (one per branch) OR keep helper + add `@Audited` on each public caller |
| 6 | **Multi-event-per-method** — same public method writes >1 event, possibly of different types (e.g. `DatasetsWebService.updateDatasetCurationDetails` line 986/991/996 emits TroubledStatusFlagEvent OR NotTroubledStatusFlagEvent OR NeedsAttentionEvent OR DoesNotNeedAttentionEvent OR CurationNoteUpdateEvent depending on which fields the request body sets) | 6 (≈3 methods × 2-3 events each) | Helper / programmatic emission — `@Audited` is one-annotation-one-event; splitting the controller method into per-field private services is over-engineering |
| 7 | **Truly dynamic event type from user input** — `Class.forName("...eventType." + req.param)` shape (`AuditController.java:79`) | 1 | **Stays imperative.** Documented as such in AUDIT_SYSTEM_AUDIT §4 |
| 8 | **Generic note-only auto-UPDATE** — `addUpdateEvent(ee, "note string")` with no event-type literal (`ProcessedExpressionDataVectorServiceImpl.java:165` reorderByDesign) | 1 | Either declare a `VectorsReorderedEvent` subclass and annotate, OR keep imperative |
| 9 | **Locally-typed branched eventType** — `Class<? extends PreferredDataChangedEvent> eventType = getPreferredDataChangedEventForVectorType(vectorType)` then `addUpdateEvent(ee, eventType, msg)` (`ExpressionExperimentWriteServiceImpl.java:245+255`) | 2 | Split into per-vector-type annotated methods (lose runtime dispatch) OR keep imperative |
| 10 | **Aspect self-reference** — `AuditedAspect.java:118` calling `addUpdateEventWithPayload` (canonical writer) | 1 | N/A — this IS the new path |
| 11 | **Catch-then-catch** — `try { … } catch(Exception e) { try { auditTrailService.addUpdateEvent(...); } catch(Exception e2) {…} throw e; }` (`DifferentialExpressionAnalyzerServiceImpl.java:225`) | 2 | Stays imperative — needs the inner try/catch for resilience |
| 12 | **CLI one-shot at end of `doWork`** — typical CLI shape where the audit emit is the last thing before `processItem` returns (e.g. `ArrayDesignBlatCli`, `ArrayDesignSequenceAssociationCli`, `MakeExperimentPrivateCli`, …) | 6 | Phase C-mech — clean `@Audited(X.class)` on the CLI's per-item method |
| **TOTAL** | | **75** | |

Bucket roll-up:

| Bucket | Sites | % |
|---|---:|---:|
| Phase C-mech (mechanical `@Audited` sweep) | 22 + 6 = **28** | 37% |
| Helper / programmatic emission | 9 + 8 + 6 = **23** | 31% |
| Stays imperative | 14 + 1 + 2 = **17** | 23% |
| Refactor-or-keep judgment call | 4 + 1 + 2 = **7** | 9% |

The 28 in the mechanical bucket are continuation of Phase B work — no
new aspect/listener machinery needed, just more sweeps. The
23 in the helper bucket are the real **Phase C** problem: they need
either the PostInsert/PreDelete listeners (to absorb auto-CREATE /
auto-DELETE so the catch-block + early-return + conditional shapes stop
needing the generic AuditAdvice underneath them) or a new programmatic
emission helper.

---

## 2. Phase C strategies

Three orthogonal lines of attack. They compose — landing 2.1 unlocks
landing 2.2; 2.3 stays out of the way.

### 2.1 PostInsert / PreDelete Hibernate listeners (the AUDIT_SYSTEM_AUDIT plan)

**Goal:** retire the `creator()` and `deleter()` `@Before` advices in
`AuditAdvice.java` (lines 95 + 141) and replace them with Hibernate
lifecycle event listeners. This is the load-bearing step — without
this, `AuditAdvice` cannot be deleted, and the imperative
`addCreateEvent` / `addDeleteEvent` API on `AuditTrailService` cannot
be retired either.

**Why PostInsert (not PersistEventListener):** at PostInsert, the
`AuditTrail` row already has its DB-assigned `id`, so adding an
`AuditEvent` to the trail's `<bag>` doesn't trigger
transient-entity-on-flush errors. PersistEventListener fires BEFORE
the insert SQL — the parent still has a null id and the cascaded
AuditTrail child also still has a null id; trying to enqueue an event
at that point requires deferred flushing that we'd rather not own.

**Why PreDelete (not PostDelete):** at PostDelete, the AuditTrail row
itself has already been removed from the session — we cannot write a
DELETE row into a trail that no longer exists. PreDelete fires before
the SQL DELETE; the trail is still attached, the AuditEvent insert
fits into the same flush as the parent's removal.

**Design sketch:**

```java
// gemma-core/src/main/java/ubic/gemma/persistence/audit/AuditTrailEventListener.java
public class AuditTrailEventListener
        implements PersistEventListener, PostInsertEventListener, PreDeleteEventListener {

    private UserManager userManager;  // injected via AuditTrailEventListenerConfig

    @Override
    public void onPersist(PersistEvent event) {
        ensureAuditTrail(event.getObject());  // existing behaviour
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Object entity = event.getEntity();
        if (entity instanceof Auditable && !(entity instanceof AuditTrail) && !(entity instanceof AuditEvent)) {
            emitLifecycleEvent((Auditable) entity, AuditAction.CREATE);
        }
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        Object entity = event.getEntity();
        if (entity instanceof Auditable && !(entity instanceof AuditTrail) && !(entity instanceof AuditEvent)) {
            emitLifecycleEvent((Auditable) entity, AuditAction.DELETE);
        }
        return false;  // do not veto the delete
    }

    private void emitLifecycleEvent(Auditable auditable, AuditAction action) {
        // FlushMode dance copied from AuditAdvice.doAuditAdvice lines 168-174 —
        // userManager.getCurrentUser() can cause Hibernate to flush, and we're
        // mid-flush already.
        Session session = sessionFactory.getCurrentSession();
        FlushMode previous = session.getHibernateFlushMode();
        session.setHibernateFlushMode(FlushMode.MANUAL);
        try {
            User user = userManager.getCurrentUser();
            if (user == null) return;  // anonymous — skip, same as AuditAdvice line 175-178
            AuditEvent ev = AuditEvent.Factory.newInstance(new Date(), action, null, null, user, null);
            auditable.getAuditTrail().getEvents().add(ev);
        } finally {
            session.setHibernateFlushMode(previous);
        }
    }

    @Override public boolean requiresPostCommitHandling(EntityPersister p) { return false; }
}
```

**Cascade interaction:** Hibernate fires PostInsert per entity that
gets inserted, including those that arrived via `cascade="all"`. The
EE → BioAssay → ArrayDesign chain that today is handled by
`AuditAdvice.cascadeAuditEvent` (line 266) is handled by Hibernate for
free under the listener path. *Verified by inspection: every
Auditable's HBM declares `cascade="all"` on the auditTrail many-to-one
(AUDIT_SYSTEM_AUDIT §2).*

**Risk: rolled-back transactions.** PostInsert fires inside the
transaction; if the transaction rolls back, the insert SQL gets
reverted but the event listener has already executed. The
`AuditEvent` row we appended to the trail's bag is part of the SAME
transaction — Hibernate's session-level dirty tracking will roll back
the audit row with the parent. **This is safer than the current
AuditAdvice path**, which uses an `@Before` advice — there, if the
DAO `update*` method throws after the audit row was enqueued, the
audit row also rolls back (same transaction), but if it's the
`Throwable` overload (REQUIRES_NEW), the audit row is committed
separately. PostInsert sits inside the main transaction, so it inherits
the right semantics by construction.

**Risk: cascade deletes.** Hibernate fires PreDelete per entity reached
by cascade. If an EE is deleted with `cascade="delete-orphan"` on a
collection, every cascaded child Auditable gets a PreDelete event —
this matches today's `AuditAdvice.cascadeAuditEvent` behaviour on the
deleter path. **Caveat:** if the AuditTrail itself is removed by
cascade BEFORE the parent's PreDelete fires (listener ordering matters
here), the DELETE row cannot be written. Hibernate's default ordering
fires entity PreDelete events in declaration order; the AuditTrail's
own deletion is cascaded from the parent's removal, so the parent's
PreDelete should fire first. **This needs IT verification** — see §3.

**Risk: interaction with `AuditedAspect`.** The new `@Audited`
annotation fires `@AfterReturning` from method calls (Spring AOP join
points). It writes UPDATE rows only — never CREATE / DELETE — so it
does not conflict with the Hibernate listener on those actions. The
two systems are orthogonal:
- UPDATE rows: `@Audited` (declarative, AOP) + a few `Throwable`-form
  imperatives in catch blocks (REQUIRES_NEW).
- CREATE rows: new `PostInsertEventListener` on
  `AuditTrailEventListener`.
- DELETE rows: new `PreDeleteEventListener` on the same class.

**Risk: the `Curatable + UPDATE` side-effect** at
`AuditAdvice.java:334` (calls
`curatableDao.updateCurationDetailsFromAuditEvent`). This already runs
inside `AuditTrailServiceImpl.doAddUpdateEvent` at line 131-133
(verified by grep on `updateCurationDetailsFromAuditEvent`) — the
listener path does not need to replicate it because CREATE/DELETE are
not Curatable side-effect triggers. *No-op for Phase C; documented for
posterity.*

### 2.2 Helper / programmatic emission for non-mechanical shapes

For the 23 "helper bucket" sites — conditional, multi-event,
early-return, catch-then-emit — we don't want either a giant aspect
matrix nor 23 carefully-split private methods.

Cleanest path: keep the imperative `AuditTrailService` API for these
sites but **migrate them to a thin emit helper** that also publishes
the `AuditedEvent` to the Spring `ApplicationEventPublisher` so
listeners (e.g., the Ticket-layer write-back from
`AUDIT_AS_WORKFLOW_RECCE`) see them too:

```java
// gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditEmitter.java  (~60 LoC)
@Component
public class AuditEmitter {
    private final AuditTrailService auditTrailService;
    private final ApplicationEventPublisher publisher;

    /** Emit a typed UPDATE event imperatively and publish the AuditedEvent.  */
    public AuditEvent emit(Auditable target, Class<? extends AuditEventType> type, String note) {
        AuditEvent ev = auditTrailService.addUpdateEvent(target, type, note);
        publisher.publishEvent(new AuditedEvent(this, target, instantiate(type), null, ev));
        return ev;
    }

    /** Conditional variant — emits only if the predicate is true.  */
    public Optional<AuditEvent> emitIf(boolean cond, Auditable t, Class<? extends AuditEventType> ty, String n) {
        return cond ? Optional.of(emit(t, ty, n)) : Optional.empty();
    }

    /** Catch-block failure variant — preserves REQUIRES_NEW semantics by delegating  */
    public void emitFailure(Auditable target, Class<? extends AuditEventType> type, String note, Throwable t) {
        auditTrailService.addUpdateEvent(target, type, note, t);  // REQUIRES_NEW
        publisher.publishEvent(new AuditedEvent(this, target, instantiate(type), null, null));
    }
}
```

Net delta: ~+60 LoC for the helper, ~−23 lines at call sites that
trade `auditTrailService.addUpdateEvent(...)` for
`auditEmitter.emit(...)` and pick up the event-publisher chain for
free. Not strictly necessary for Phase C to land — the imperative API
already works — but it's the right grouping for the
audit-as-workflow / Spring Modulith story (AUDIT_AS_WORKFLOW_RECCE
§4 recommends Option B "dedicated Ticket + TicketEvent parallel to
Auditable + AuditEvent" which wants the event-publish hook).

### 2.3 What MUST stay imperative

These patterns are out-of-scope for `@Audited` (and for the helper)
and should be left as direct `auditTrailService.addUpdateEvent(...)`
calls — the documentation in this section is the load-bearing
contract for "you tried to migrate this and we said no":

1. **`Throwable`-form catch blocks** (14 sites). The `Throwable`
   overload uses `Propagation.REQUIRES_NEW` (verified at
   `AuditTrailServiceImpl.java:90-94`) — the failure audit row commits
   even when the parent transaction rolls back. `@AfterReturning`
   cannot record failures (the method threw). `@AfterThrowing` could
   in principle, but the REQUIRES_NEW + stack-trace + detail field
   shape is too specific to lift cleanly.

2. **Catch-then-catch failure-of-failure** (2 sites,
   `DifferentialExpressionAnalyzerServiceImpl.java:225` etc.). Nested
   try/catch where the outer catch writes the failure audit and the
   inner catch absorbs failures-in-the-audit-write. AOP advice can't
   wrap nested catches without surrendering control flow.

3. **Truly dynamic event-type from user input** (1 site,
   `AuditController.java:79`). `Class.forName(req.param)` resolves to
   one of 94 `AuditEventType` subclasses at runtime. Cannot be
   compiled into an annotation literal.

4. **REST endpoints with multi-field-update bodies** (6 sites). The
   `DatasetsWebService.updateDatasetCurationDetails` shape — one
   request mutates 0..N fields, each potentially emitting a typed
   event of a different type. Splitting into N controller endpoints
   would break the REST API; splitting the controller's internal call
   into N private services is the kind of cargo-cult refactor that
   makes the code worse. Keep imperative; possibly route through
   `AuditEmitter.emitIf` for the publish-event chain.

5. **The note-only generic UPDATE** (1 site,
   `reorderByDesign`). Only one in the whole codebase. Either declare a
   `VectorsReorderedEvent` subclass (clean) or accept that this one
   imperative call survives. Recommend declaring the type — cost is 1
   new file ~15 LoC, gain is uniform event-typed history.

---

## 3. IT validation gate

These tests MUST pass against `gemdtest` before Phase C lands. The IT
gate is the long pole because `gemdtest` is single-tenant (memory
`feedback_parallel_gemma_agents.md`).

### 3.1 Unit-slice (must already be green pre-Phase-C; will be green post-Phase-C)

- `AuditedAspectTest` — already on disk, covers arg resolution + SpEL
  + payload + exception swallowing. No expected change.
- `AuditTrailServiceImplTest` — exercises the service API directly.
  No expected change (the imperative API survives).
- `AuditTrailDaoTest` — exercises the DAO. No change.
- `AuditEventServiceTest` — read-side. No change.

### 3.2 Integration-slice (the gate that's been blocking)

| Test | Path | Today's expectation | Post-Phase-C expectation |
|---|---|---|---|
| `AuditAdviceTest` | `gemma-core/src/test/java/ubic/gemma/core/security/audit/AuditAdviceTest.java` | Verifies generic auto-UPDATE rows fire on DAO `update*` calls AND auto-CREATE rows on DAO `create*` calls. | Either rename to `AuditListenerTest` and rewrite to check Hibernate-listener-driven CREATE / DELETE + typed-`@Audited` UPDATE; OR drop the assertions about generic UPDATE rows entirely. |
| `ExpressionExperimentServiceIntegrationTest` | line 499 | `containsExactly(AuditAction.CREATE, AuditAction.UPDATE)` | After Phase C, this CREATE is emitted by `PostInsertEventListener` (still `AuditAction.CREATE`); the generic UPDATE is emitted by Phase-B-migrated typed `@Audited` events on whatever update method the test calls. Assertion shape may need to switch to `getEventsWithType` or accept the new emitter. |
| `GeoDatasetServiceTest` | line 319 | Same CREATE+UPDATE pair. | Same fix. |
| `ArrayDesignReportServiceTest` | the cache-bug victim from `notable_cases.md` | Currently has a workaround for the L2-stale-bag bug. | Drop the workaround — Phase C step 2 of AUDIT_SYSTEM_AUDIT removes `mutable="false"` from `AuditTrail.hbm.xml`/`AuditEvent.hbm.xml` and the cache bug retires. |
| New: `AuditTrailListenerIntegrationTest` | NEW | Doesn't exist | Must be authored. Asserts: (a) creating any Auditable persists exactly one CREATE row; (b) deleting any Auditable persists exactly one DELETE row; (c) cascaded creates (EE → BioAssay) persist one CREATE per cascaded entity; (d) rolled-back transaction leaves zero CREATE rows; (e) anonymous user does not emit. |
| New: `AuditEventCacheBugRegressionTest` | NEW | Doesn't exist | Asserts the `notable_cases.md` line 1-19 fingerprint stays gone after the `mutable="false"` removal. Same shape as the workaround in `ArrayDesignReportServiceTest` but inverted (positive assertion). |
| `FactorValueNeedsAttentionServiceTest` | exists | Mocks AuditTrailService | After Phase C, the `markAsNeedsAttention` / `clearNeedsAttentionFlag` callers move to `@Audited` (Phase C-mech bucket); the mock-AuditTrailService scaffolding needs to switch to verifying the `AuditedEvent` was published instead. |
| `SingleCellExpressionExperimentServiceTest` | exists | Same pattern (mocked AuditTrailService). | Same fix for the 6 callers in that service (Phase C-mech bucket). |

### 3.3 Manual validation against `gemdtest`

1. Run `mvn verify -pl gemma-core` against `gemdtest` (per
   memory `reference_local_test_database.md` — JDK17, password via
   keychain `mysql-root`, single-tenant).
2. Spot-check `AUDIT_EVENT` row counts before and after a full audit
   trail-touching workflow (EE upload + EE update + EE delete) —
   should be exactly:
   - 1 CREATE row per Auditable persisted (was: 1 — same).
   - 0 generic UPDATE rows (was: many — the change).
   - N typed UPDATE rows where N = number of `@Audited` methods invoked.
   - 1 DELETE row per Auditable removed (was: 1 — same).
3. Cross-check the WhatsNew front-page widget still finds the test EE
   under its new `getUpdatedSinceDateForType` query (per
   AUDIT_SYSTEM_AUDIT step 4 / risk 1).
4. Cross-check `DatasetsWebService.getDatasetAuditEvents` payload —
   should now exclude generic UPDATE rows (per step 4 / risk 2).

### 3.4 Production database read (sanity check, not gating)

Per memory `reference_production_database.md`, port-forward 8000 to
prod MySQL READ-ONLY. Size the `AUDIT_EVENT` table:

```sql
SELECT ACTION, COUNT(*), COUNT(EVENT_TYPE_FK) AS typed
  FROM AUDIT_EVENT
 GROUP BY ACTION;
```

Expect: many millions of `U` rows with `EVENT_TYPE_FK IS NULL`
(generic auto-UPDATE — the prod tail Phase C retires), some thousands
with typed `EVENT_TYPE_FK`, a few hundred thousand `C` rows, a few
hundred `D` rows. Confirms the prod-side shape matches the model and
the cleanup CLI (`ArrayDesignAuditTrailCleanupCli`) can be retired
once Phase C lands.

---

## 4. Effort estimate

Three sessions of dedicated work, assuming the IT gate runs serially
against `gemdtest`:

| Session | Work | LoC delta |
|---|---|---:|
| **C-1** | Author + register `PostInsertEventListener` and `PreDeleteEventListener` on `AuditTrailEventListener`. Wire UserManager via `AuditTrailEventListenerConfig`. Author `AuditTrailListenerIntegrationTest` (~120 LoC test). Run `mvn verify -pl gemma-core` against gemdtest — green or diagnose. | +110 (listener) +120 (test) = **+230** |
| **C-2** | Delete the `creator()` + `deleter()` `@Before` advices from `AuditAdvice` (lines 95-107 + 141-153 + `addCreateAuditEvent` 212-249 + `addDeleteAuditEvent` 251-265 + the cascade walker support). Delete the `updater()` + `saver()` `@Before` advices (lines 110-130). Net: `AuditAdvice.java` collapses from 373 → ~80 LoC OR is deleted outright if nothing else depends on it. Drop `mutable="false"` from `AuditTrail.hbm.xml:7`, `AuditEvent.hbm.xml:6`, and `<cache usage="read-only"/>` from `AuditEvent.hbm.xml:8`. Migrate the 2 hard consumers (`WhatsNewServiceImpl`, `DatasetsWebService.getDatasetAuditEvents`). Retire `@IgnoreAudit` (no consumers after AuditAdvice is gone). Run full IT slice. | **−500** (AuditAdvice + cache HBM workaround + IgnoreAudit + hard-consumer cleanup) |
| **C-3** | Sweep the 28 Phase C-mech bucket call sites to `@Audited`. Update the 6 mocking tests in §3.2 to verify `AuditedEvent` publish rather than `AuditTrailService.addUpdateEvent` invocation. Optionally land `AuditEmitter` helper (§2.2) for the 23 helper-bucket sites (or defer to a follow-on). Author `AuditEventCacheBugRegressionTest`. | **−40** (sweep) +60 (AuditEmitter, optional) +40 (regression test) = **+60 / −40** |
| **TOTAL** | | **≈ −250 LoC**, possibly more if AuditAdvice is deleted entirely (then add another −80) |

The total is in the same ballpark as the AUDIT_SYSTEM_AUDIT
"Cumulative shape ≈ −455 LoC" estimate, but more conservative because
(a) the 23 helper-bucket sites stay imperative — they don't shrink —
and (b) we factor in the ~+230 LoC for the new listener + its IT.

**Parallelism caveat:** C-1 and C-2 must be sequential — C-2 can't
land until C-1 is green on `gemdtest`, otherwise trunk goes through a
window where neither AuditAdvice nor the listener owns CREATE / DELETE
and AuditEvent rows are silently dropped. C-3 can run in parallel with
C-2 (mechanical sweep is non-conflicting) but with the usual
gemdtest-single-tenant caveat (memory
`feedback_parallel_gemma_agents.md`).

**Calendar estimate:** 1 working week if the IT gate cooperates; up to
2 weeks if `AuditTrailListenerIntegrationTest` surfaces a
listener-ordering edge case that needs reworking (the cascade-delete
ordering question in §2.1 is the most likely culprit).

---

## 5. Recommendation

Land Phase C in the order **C-1 → C-2 → C-3** as a single coordinated
PR (per AUDIT_SYSTEM_AUDIT "Phase B-3 + Phase C should ship together
to avoid two trunk-disturbing audit churns"). Do NOT half-land C-1
without C-2 — trunk would have two CREATE-row writers (AuditAdvice +
listener) producing duplicate rows.

The `AuditEmitter` helper from §2.2 is **out-of-scope for the
Phase C landing**. It's the natural launching point for the
Ticket-layer work in `AUDIT_AS_WORKFLOW_RECCE` but should be filed as
its own follow-on PR ("Phase D: programmatic audit emission helper +
audit-event publishing for the Spring Modulith readiness story").
That keeps Phase C's PR diff focused: the AuditAdvice deletion + the
listener addition + the cache HBM fix + the mechanical sweep, nothing
else.

---

## 6. C-2 landing note (2026-05-19)

C-2 landed on branch `audit-phasec-c2` (off
`phase2-acl-migrate@893ee3545b`). Slices that shipped:

1. `AuditTrailEventListenerConfig`: switched to two-arg constructor
   (`@Autowired UserManager` + `@Autowired SessionFactory`),
   registered `POST_INSERT` + `PRE_DELETE` event types on
   `AuditTrailEventListener` in addition to `PERSIST` /
   `PERSIST_ONFLUSH`.
2. `AuditAdvice.doCreateAdvice` + `doDeleteAdvice` deleted, along
   with their dead-code helpers (`addCreateAuditEvent`,
   `addDeleteAuditEvent`) and the `OperationType.CREATE` /
   `OperationType.DELETE` switch arms. The class collapses from
   373 LoC → ~280 LoC. `doSaveAdvice` + `doUpdateAdvice` retained
   per the conservative C-2 brief; the transient-save CREATE
   cascade is absorbed by the listener's duplicate-CREATE guard.
3. `mutable="false"` dropped from both `AuditTrail.hbm.xml` and
   `AuditEvent.hbm.xml`. The L2 `<cache usage="read-only"/>` on
   AuditEvent was already gone from C-1.
4. `WhatsNewServiceImpl` + `DatasetsWebService.getDatasetAuditEvents`:
   inspected, no hardcoded references to the removed
   `AuditAdvice.doCreateAdvice` / `doDeleteAdvice` methods (both
   use the read-side `AuditEventService` API, which is unchanged).
   No migration needed.

Deferred to a follow-on (per conservative brief):

- **The 28-site `@Audited` mechanical sweep** (recce §1 bucket 1 + 12).
  Each site needs case-by-case analysis to confirm:
  (a) the Auditable is the first parameter of the enclosing method;
  (b) the method exits through a single normal return (no
  catch-block emit);
  (c) the event type is a literal class (no
  parameterized/dynamic dispatch). Risky to do in bulk without IT
  validation against gemdtest. The 28 sites are:

  CLI bucket (6 sites — bucket 12):
  - `ArrayDesignBlatCli:187`, `ArrayDesignSequenceAssociationCli:243`,
    `MakeExperimentPrivateCli:26`, `ArrayDesignBioSequenceDetachCli:114`,
    `ArrayDesignSubsumptionTesterCli:152`, `ArrayDesignRepeatScanCli:159`.

  Service bucket (representative 22 sites — bucket 1):
  - `MeanVarianceServiceImpl:93`, `VectorMergingServiceImpl:253`,
    `SVDServiceImpl:589`, `ArrayDesignMergeHelperServiceImpl:87`,
    `GeoServiceImpl:462`, `ExternalDatabaseServiceImpl:107/116`,
    `SingleCellExpressionExperimentServiceImpl:388/680/936/994/1044/1452/1479`,
    `ExpressionExperimentServiceImpl:1056`,
    `OutlierFlaggingServiceImpl:96/135`,
    `SingleCellExpressionExperimentSubSetServiceImpl:118`,
    `SingleCellExpressionExperimentAggregateServiceImpl:313`,
    `GeeqServiceImpl:566`, `ExperimentalDesignController:765`,
    `ExpressionExperimentController:1273`,
    `ExpressionExperimentEditController:854`.

- **`AuditAdviceTest` update** (recce §3.2): expects rewriting to
  assert the listener-driven CREATE/DELETE path rather than the
  deleted advice. Belongs to the same PR as the IT validation gate.

**IT validation gate** (per recce §3.3, memory
`feedback_parallel_gemma_agents.md`): single-tenant gemdtest, so
`mvn verify -pl gemma-core` against gemdtest MUST run green before
the C-2 commit merges. The dual-emission behavioural claim (listener
duplicate-CREATE guard absorbs the AuditAdvice save-cascade CREATE
in the same flush) is verified by code inspection only; gemdtest is
the verification of record.
