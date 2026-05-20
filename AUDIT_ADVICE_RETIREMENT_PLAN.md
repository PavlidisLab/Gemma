# AuditAdvice retirement plan

Branch baseline: `audit-advice-retirement-recce` at `119e5dc898` (tip of `phase2-acl-migrate`).
Scope: terminal step of Phase C — fully retire `AuditAdvice` after Phase C-2 has already
migrated CREATE/DELETE emission to the Hibernate listener.

## 1. Scope summary

**Today.** `AuditAdvice` (`gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditAdvice.java`,
**338 LoC**) is a Spring AOP `@Aspect` that owns auto-`UPDATE` and auto-`SAVE` rows. It has
TWO remaining `@Before` advices keyed on `Pointcuts.updater()` (line 101) and
`Pointcuts.saver()` (line 126) — CREATE + DELETE advices were already deleted in Phase C-2.
Every DAO method matching `execution(* update*(*, ..))` or `execution(* save*(*, ..))`
emits an `AuditEvent { action=UPDATE, eventType=null }` (or `CREATE` on transient save) by
walking `extractAuditables(args[0])` and appending to the trail's bag. The save advice's
transient-CREATE cascade currently co-exists with `AuditTrailEventListener.onPostInsert`
without double-emission thanks to the listener's "trail already non-empty" guard
(AuditTrailEventListener.java:229).

**After retirement.** Auto-CREATE / auto-DELETE rows come from
`AuditTrailEventListener` (`gemma-core/src/main/java/ubic/gemma/persistence/audit/AuditTrailEventListener.java`,
**252 LoC**, already registered on `POST_INSERT` + `PRE_DELETE` in
`AuditTrailEventListenerConfig.java:83-84`). Imperative typed UPDATE rows continue
through `AuditTrailServiceImpl.addUpdateEvent(...)` — driven either by the
`@Audited` / `@AuditedConditional` / `@AuditedOnError` aspect (`AuditedAspect.java`)
on **31 already-migrated services** OR by the remaining imperative call sites.
Generic eventType=null `UPDATE` rows go away.

**LoC delta (measured).**
- `AuditAdvice.java`: 338 → 0 = **−338 LoC**.
- `Pointcuts.java`: `creator()`/`updater()`/`saver()`/`deleter()` defs and `daoMethod()`
  composition (`Pointcuts.java:51`,57,72,79,86,93) can be removed if no other consumer remains.
  Grep confirms only `AuditAdvice` references them (Pointcuts retained for
  `retryableOrTransactionalServiceMethod` consumers in `HibernateConfig.java`). **~−25 LoC**.
- `IgnoreAudit.java` (13 LoC) — sole consumer is `AuditAdvice.java:133` and
  `CuratableDao.java:38` (which uses it to suppress auto-update on a DAO method).
  CuratableDao usage becomes dead once AuditAdvice is gone. **−13 LoC**, plus the import
  on CuratableDao.
- Total: **~−376 LoC** of source removed, no net additions (listener already exists).

## 2. Pre-retirement inventory

### 2.1 Remaining imperative addUpdateEvent callsites

Grep: `grep -rn 'auditTrailService\.addUpdateEvent\|getAuditTrailService()\.addUpdateEvent'
gemma-{core,rest,web,cli}/src/main/java --include='*.java'` → **53 call sites** (34 in
gemma-core, 12 in gemma-cli, 2 in gemma-rest, 5 in gemma-web).

| File:line | Bucket | Note |
|---|---|---|
| gemma-core/.../AuditedAspect.java:242 | Aspect plumbing (the aspect itself dispatches imperatively) | Keep — internal to `@Audited` machinery. |
| gemma-core/.../AuditedAspect.java:281 | Aspect plumbing (`addUpdateEventWithPayload`) | Keep — internal. |
| gemma-core/.../singleCell/aggregate/SingleCellExpressionExperimentAggregateServiceImpl.java:313 | Imperative TYPED 4-arg (DataAddedEvent + detail) | Bucket 2f blocked (4-arg + detail). |
| gemma-core/.../singleCell/aggregate/SingleCellExpressionExperimentSubSetServiceImpl.java:118 | Imperative TYPED 4-arg | Bucket 2f blocked. |
| gemma-core/.../preprocess/PreprocessorServiceImpl.java:135 | Imperative TYPED 3-arg | Migratable to `@Audited(BatchCorrectionEvent.class, messageSpel=...)`. |
| gemma-core/.../preprocess/batcheffects/BatchInfoPopulationHelperServiceImpl.java:95,99 | Imperative TYPED 4-arg | Bucket 2f. |
| gemma-core/.../analysis/expression/diff/DifferentialExpressionAnalyzerServiceImpl.java:275 | Imperative TYPED | Migratable. |
| gemma-core/.../analysis/service/OutlierFlaggingServiceImpl.java:96,135 | Imperative TYPED | Migratable. |
| gemma-core/.../loader/expression/DataUpdaterImpl.java:175,505,681,751 | Imperative TYPED, some inside branches | Mixed: some already covered by `@Audited`/`@AuditedOnError` on the enclosing methods; line 751 is the dynamic `audit(ee, eventType, note)` helper. |
| gemma-core/.../persistence/service/common/description/ExternalDatabaseServiceImpl.java:107,116 | Imperative TYPED 5-arg (note + detail + lastUpdated) | Bucket 2f blocked (5-arg overload). |
| gemma-core/.../persistence/service/expression/experiment/SingleCellExpressionExperimentServiceImpl.java:1012,1485,1512 | Imperative TYPED | Migratable. |
| gemma-core/.../ExpressionExperimentWriteServiceImpl.java:245,255 | Dynamic-typed (`PreferredDataChangedEvent` subtype variable) | Keep imperative (Section 4 dynamic-typed bucket). |
| gemma-core/.../ExpressionExperimentServiceImpl.java:1008 | Imperative TYPED | Migratable. |
| gemma-core/.../GeeqServiceImpl.java:566 | Imperative TYPED 4-arg | Bucket 2f. |
| gemma-core/.../ProcessedExpressionDataVectorServiceImpl.java:112 | Imperative TYPED 4-arg | Bucket 2f. |
| gemma-core/.../ProcessedExpressionDataVectorServiceImpl.java:152 | Note-only (no event type — GENERIC 2-arg) | Need new `VectorsReorderedEvent` OR keep imperative. |
| gemma-cli/.../ArrayDesignBlatCli.java:187 | Imperative TYPED, CLI | Keep imperative (CLI not in scope for `@Audited`). |
| gemma-cli/.../MakeExperimentPrivateCli.java:26, MakeExperimentsPublicCli.java:45 | Imperative TYPED, CLI | Keep imperative. |
| gemma-cli/.../ArrayDesignSequenceAssociationCli.java:243 | CLI | Keep. |
| gemma-cli/.../ArrayDesignBioSequenceDetachCli.java:114 | CLI | Keep. |
| gemma-cli/.../ArrayDesignProbeMapperCli.java:484 | Dynamic-typed (`ArrayDesignGeneMappingEvent` subtype variable) | Keep imperative. |
| gemma-cli/.../ArrayDesignRepeatScanCli.java:159 | CLI | Keep. |
| gemma-cli/.../GenericGenelistDesignGenerator.java:325 | CLI, conditional (`if (!noDB)`) | Keep. |
| gemma-cli/.../ArrayDesignSubsumptionTesterCli.java:152 | CLI | Keep. |
| gemma-cli/.../ExpressionDataCorrMatCli.java:73,76 | CLI catch-block, 4-arg (throws) | Keep imperative — catch-block with `Throwable`. |
| gemma-cli/.../ArrayDesignProbeRenamerCli.java:132 | CLI | Keep. |
| gemma-rest/.../DatasetsWebService.java:1238 | REST controller | Keep imperative. |
| gemma-web/.../AuditController.java:79 | TRULY dynamic (`Class.forName` from HTTP param) | Must stay imperative. |
| gemma-web/.../ExpressionExperimentController.java:1273 | Web controller | Keep. |
| gemma-web/.../ExperimentalDesignController.java:765 | Web controller | Keep. |
| gemma-web/.../ExpressionExperimentEditController.java:854 | Web controller | Keep. |

**Bottom line:** retiring `AuditAdvice` does NOT require migrating the remaining 53
imperative callsites. They go through `AuditTrailServiceImpl.addUpdateEvent` directly,
which is independent of the AOP advices being deleted. Migration of the imperative
callsites to `@Audited` is a separate, optional sweep (Phase B continuation).

### 2.2 DAO mutation paths relying on AuditAdvice's blanket pointcut

**Pointcut definitions** (`gemma-core/src/main/java/ubic/gemma/persistence/util/Pointcuts.java`):

| Pointcut | Line | Expression |
|---|---|---|
| `daoMethod()` | :51 | bean is a `@Repository` / DAO |
| `updater()` | :78–79 | `daoMethod() && execution(* update*(*, ..))` |
| `saver()` | :85–86 | `daoMethod() && execution(* save*(*, ..))` |
| `creator()` | :71–72 | `daoMethod() && (execution(* create*(*, ..)) \|\| execution(* findOrCreate*(*, ..)) \|\| execution(* persist*(*, ..)) \|\| execution(* add*(*, ..)))` |
| `deleter()` | :92–93 | `daoMethod() && (execution(* remove*(*, ..)) \|\| execution(* delete*(*, ..)))` |

`creator()` / `deleter()` are already orphaned (Phase C-2 deleted their advices).
`updater()` / `saver()` become orphaned with this retirement.

**What `AuditTrailEventListener` currently covers** (per
`AuditTrailEventListener.java:84` and `AuditTrailEventListenerConfig.java:78–84`):
- `EventType.PERSIST` + `PERSIST_ONFLUSH` — AuditTrail-existence invariant guard.
- `EventType.POST_INSERT` — emits `AuditAction.CREATE`, eventType=null, on every
  Auditable that gets a row INSERT'd (cascades into BioAssays, etc.).
- `EventType.PRE_DELETE` — emits `AuditAction.DELETE`, eventType=null, on every
  Auditable about to be deleted.

**What auto-UPDATE/auto-SAVE currently emit that the listener does NOT replace:**

| Operation | What AuditAdvice emits today | After retirement |
|---|---|---|
| DAO `update*(Auditable)` | `AuditEvent { action=UPDATE, eventType=null, note="UPDATE event on entity …" }` | Nothing. UPDATE rows come only from imperative or `@Audited` paths. |
| DAO `save*(Auditable)` on already-persistent entity (`id != null`) | `AuditEvent { action=UPDATE, eventType=null }` | Nothing. |
| DAO `save*(Auditable)` on transient entity (`id == null`) | `AuditEvent { action=CREATE, eventType=null }` + cascade walker walks Auditables in the object graph and emits CREATEs on each | `POST_INSERT` listener fires per real INSERT, including cascaded inserts — equivalent coverage from Hibernate's own cascade machinery rather than AuditAdvice's hand-rolled walker. |

The transient-save → CREATE path is the **only place** AuditAdvice's manual cascade walker
(`AuditAdvice.cascadeAuditEvent`, line 231) does work the listener doesn't already
duplicate. Whether the listener catches the same set depends on whether `session.save(...)`
takes the transient-entity branch to `session.persist(...)` cascade (it does for an
Auditable arriving with `id==null` because the HBM mapping uses `cascade="all"`). PostInsert
fires once per row INSERT'd — covers all cascaded Auditables. **Net coverage: equivalent.**

### 2.3 Tests that depend on AuditAdvice firing

Search: `grep -rn 'verify( auditTrailService ).addUpdateEvent\|containsExactly.*AuditAction'
gemma-core/src/test` → ~10 sites, plus the two flagship integration tests.

| Test | What it verifies | Action under retirement |
|---|---|---|
| `gemma-core/src/test/java/ubic/gemma/core/security/audit/AuditAdviceTest.java` (281 LoC) | AuditAdvice's CREATE/UPDATE/SAVE behaviour end-to-end; the class **is** the AuditAdvice unit test | Rename → `AuditLifecycleListenerTest`; rewrite assertions to count listener-emitted CREATE/DELETE rows only. UPDATE assertions delete or move to `AuditedAspectTest`. |
| `gemma-core/src/test/java/ubic/gemma/persistence/util/PointcutsTest.java` (`AuditAdviceTestContextConfiguration`, line 30) | Exercises the AOP pointcut matching | Trim — remove updater/saver/creator/deleter pointcut tests (keep transactional/retryable). |
| `gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentServiceIntegrationTest.java:494–501` | `createdEE.getAuditTrail().getEvents() ... containsExactly(AuditAction.CREATE, AuditAction.UPDATE)` after a service-level update | Change to `containsExactly(AuditAction.CREATE)` — the `UPDATE` row went away. OR introduce a typed UPDATE via `@Audited` on the service method being exercised. |
| `gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/service/GeoDatasetServiceTest.java:323–328` | Asserts `events.size()==2`, `events[0].action==CREATE`, `events[1].action==UPDATE` | Same shape — delete the UPDATE assertion or back it with `@Audited`. |
| `gemma-core/src/test/java/ubic/gemma/persistence/service/common/auditAndSecurity/AuditTrailServiceImplTest.java:260` | `assertEquals(AuditAction.UPDATE, e.getAction())` on a service-API call | Safe — directly exercises `AuditTrailService.addUpdateEvent`, not the aspect. |
| `gemma-core/src/test/java/ubic/gemma/persistence/service/common/auditAndSecurity/AuditEventDaoTest.java` (multiple) | Inserts AuditEvents directly and queries; not aspect-driven | Safe — uses `AuditEvent.Factory.newInstance` directly. |
| `gemma-core/src/test/java/ubic/gemma/persistence/service/common/auditAndSecurity/CuratableDaoTest.java` | Curation-details side-effect via AuditAdvice's `Curatable + UPDATE` branch (AuditAdvice.java:299–301) | **CRITICAL**: see step 3, "curation side-effect"; this is the only behavioural surprise. |
| `gemma-core/src/test/java/ubic/gemma/core/analysis/singleCell/aggregate/SingleCellExpressionExperimentAggregateServiceTest.java` (7 sites, lines 303–744) | `verify(auditTrailService).addUpdateEvent(eq(ee), eq(DataAddedEvent.class), ...)` — service-level Mockito | Safe — verifies the imperative API call, not the aspect. |
| `gemma-core/src/test/java/ubic/gemma/core/security/audit/AuditedAspectTest.java` (3 sites, lines 394–467) | Exercises the new `@Audited` aspect with payload | Safe — orthogonal to AuditAdvice. |

The Curatable side-effect (`AuditAdvice.java:299`:
`if ( auditable instanceof Curatable && auditAction == AuditAction.UPDATE )
curatableDao.updateCurationDetailsFromAuditEvent(...)`) is the **one piece of behavioural
code in AuditAdvice that has no replacement**. Since auto-UPDATE rows go away entirely,
`updateCurationDetailsFromAuditEvent` no longer fires from blanket DAO updates — only from
typed UPDATE rows emitted via `AuditTrailServiceImpl.doAddUpdateEvent` (which already calls
the same side-effect on its own path, see `AuditTrailServiceImpl.java:126` per the
`AUDIT_SYSTEM_AUDIT.md:566` appendix). **Verify in step 3 that `AuditTrailServiceImpl`'s
imperative path already calls `updateCurationDetailsFromAuditEvent`** — if so, the
side-effect coverage is preserved for every typed UPDATE.

## 3. Implementation steps

### Step 1 — Verify Curatable side-effect coverage on imperative path

- **Files inspected (no changes):** `gemma-core/.../AuditTrailServiceImpl.java`,
  `gemma-core/.../GenericCuratableDao.java`, `AbstractCuratableDao.java`.
- **Acceptance:** `AuditTrailServiceImpl.addUpdateEvent` (every overload) calls
  `curatableDao.updateCurationDetailsFromAuditEvent` when the auditable is `Curatable` AND
  the action is `UPDATE` AND the event has a non-null `eventType`. If the imperative API
  doesn't already do this, **add it** before any other retirement step (small +5 LoC patch).
- **Risk:** LOW — read-only verification.
- **Validation:** `CuratableDaoTest`, `AuditTrailServiceImplTest`.

### Step 2 — Delete AuditAdvice.java + Pointcuts updater/saver/creator/deleter

Single coordinated commit:

- Delete `gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditAdvice.java` entirely
  (−338 LoC).
- In `gemma-core/src/main/java/ubic/gemma/persistence/util/Pointcuts.java`, remove
  `daoMethod()` (:50–53), `loader()` (:64), `creator()` (:71–73), `updater()` (:78–80),
  `saver()` (:85–87), `deleter()` (:92–94). Verify no remaining consumers (search confirmed
  `HibernateConfig` only references `retryableOrTransactionalServiceMethod`). −25 LoC.
- Delete `gemma-core/src/main/java/ubic/gemma/core/security/audit/IgnoreAudit.java` (−13
  LoC) **only if** the `CuratableDao.java:38` annotation use is unblocked. Otherwise leave
  IgnoreAudit alone — its only consumer becomes dead code but still compiles.
- **Files touched:** AuditAdvice.java (delete), Pointcuts.java (edit), optionally
  IgnoreAudit.java (delete), CuratableDao.java (drop import if previous deleted).
- **Risk:** MED — deletes the catch-all generic UPDATE emitter. Anything that was
  silently relying on a generic UPDATE row appearing in an audit trail after a `dao.update`
  call will lose that row.
- **Validation:** `mvn -pl gemma-core test -Dtest='Audit*,Pointcuts*'` unit slice.
  Then full `mvn verify` against gemdtest.

### Step 3 — Fix the integration tests that asserted generic UPDATE rows

Same commit as step 2 OR follow-up commit (decision: split for reviewability — the test
edits are mechanical and unrelated to listener correctness):

- `ExpressionExperimentServiceIntegrationTest.java:501` — replace
  `containsExactly(AuditAction.CREATE, AuditAction.UPDATE)` with
  `containsExactly(AuditAction.CREATE)` (the test scenario performs a DAO-level update
  whose generic UPDATE row no longer appears).
- `GeoDatasetServiceTest.java:323–328` — same. The `assertEquals(2, events.size())` becomes
  `assertEquals(1, ...)`. If the test scenario could route through a typed event, prefer
  introducing `@Audited(SomeUpdateEvent.class)` on the relevant service method instead.
- `AuditAdviceTest.java` (281 LoC) — rename + rewrite (or delete if the listener test
  already covers CREATE/DELETE). Recommended: rename to `AuditLifecycleListenerTest`,
  keep CREATE/DELETE assertions (now sourced from the listener), drop UPDATE/SAVE.
- `PointcutsTest.java` — drop the pointcut-matching tests for creator/updater/saver/deleter.
- **Files touched:** 4 test classes.
- **Risk:** LOW — pure test fixture updates.
- **Validation:** affected test classes pass green.

### Step 4 — Migrate hard consumers off generic-UPDATE assumptions

Per `AUDIT_SYSTEM_AUDIT.md` section 5, two HARD consumers:

- `gemma-core/src/main/java/ubic/gemma/core/analysis/report/WhatsNewServiceImpl.java:218–219`
  — `auditEventService.getUpdatedSinceDate(ArrayDesign.class, date)` and
  `getUpdatedSinceDate(ExpressionExperiment.class, date)` currently query
  `WHERE ae.action='U'`. After retirement, near-zero rows would match. Switch to a new
  `getUpdatedSinceDateForType(class, date)` method (or filter `ae.eventType is not null`).
  ~10 LoC.
- `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java:1096`
  (`getDatasetAuditEvents`) — REST `GET /datasets/{id}/auditEvents` returns ALL events.
  Switch to `auditEventService.getEventsWithType(ee)` (already exists); document the
  shape change in REST release notes. ~3 LoC + doc.
- **Files touched:** 2 main, 1 new DAO method (`AuditEventDaoImpl.java`).
- **Risk:** MED — visible behaviour change for the WhatsNew dashboard widget. Spot-check
  with prod-like data via the read-only port-forward (per
  `reference_production_database.md`).
- **Validation:** existing `WhatsNewServiceTest` (if any) + manual smoke against gemdtest.

### Step 5 (optional, separate session) — Drop `mutable="false"` HBM workaround

Phase C step 2 in the main audit doc: remove `mutable="false"` from
`AuditTrail.hbm.xml:7` + `AuditEvent.hbm.xml:6`, remove `<cache usage="read-only"/>` from
`AuditEvent.hbm.xml:8`. Fixes the `notable_cases.md` cache-staleness bug as a side effect.
This is independent of AuditAdvice retirement but a natural follow-on.

- **Risk:** MED — cache directives changing in HBM has historically been a source of
  Hibernate 6 surprises. Not coupled to retirement; defer if step 2-4 succeed.

## 4. IT validation surface

Run against gemdtest (per `feedback_parallel_gemma_agents.md`, single-tenant; serialize).

| Test path | Verifies | Approx runtime |
|---|---|---|
| `gemma-core/src/test/java/ubic/gemma/core/security/audit/AuditAdviceTest.java` (renamed) | CREATE/DELETE via listener; UPDATE no longer auto-emitted | ~30s |
| `gemma-core/src/test/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentServiceIntegrationTest.java` | EE create + service update path emits exactly the rows we expect | ~60s |
| `gemma-core/src/test/java/ubic/gemma/core/loader/expression/geo/service/GeoDatasetServiceTest.java` | GEO load → 1 CREATE event, no auto-UPDATE | ~5min (loader-bound) |
| `gemma-core/src/test/java/ubic/gemma/persistence/service/common/auditAndSecurity/AuditTrailServiceImplTest.java` | Imperative `addUpdateEvent` + Curatable side-effect | ~10s |
| `gemma-core/src/test/java/ubic/gemma/persistence/service/common/auditAndSecurity/CuratableDaoTest.java` | `updateCurationDetailsFromAuditEvent` still fires on typed UPDATE rows | ~15s |
| `gemma-core/src/test/java/ubic/gemma/core/analysis/singleCell/aggregate/SingleCellExpressionExperimentAggregateServiceTest.java` | TYPED `DataAddedEvent` rows still emit | ~45s |
| `gemma-core/src/test/java/ubic/gemma/core/security/audit/AuditedAspectTest.java` | `@Audited` aspect untouched | ~15s |
| `gemma-core/src/test/java/ubic/gemma/persistence/util/PointcutsTest.java` (trimmed) | Transactional / retryable pointcuts still match | ~5s |

Full `mvn verify` baseline (per memory `project_gemdtest_schema_phase2.md`: drop+recreate
gemdtest before each verify). Expected delta from current baseline (1369 tests / 4F / 10E
/ 37S): test count drops by ~2–5 (removed pointcut tests), failures+errors should stay
flat.

## 5. Rollback strategy

**Fastest revert:** single-commit revert of step 2 restores `AuditAdvice.java` and
`Pointcuts.java`. The Hibernate listener registration in
`AuditTrailEventListenerConfig.java` does NOT need reverting — it has already been live
in `phase2-acl-migrate` since Phase C-2; the duplicate-CREATE guard at
`AuditTrailEventListener.java:229` means re-introducing AuditAdvice's save advice does NOT
double-emit (it would, however, mean two source-of-truth paths for CREATE again — fine
for a rollback hotfix).

If the test edits in step 3 land as a separate commit, revert that one too. If step 4
lands separately, revert independently.

**Hibernate listener registration:** no irreversible change. The listener was registered
in Phase C-2 and is unaffected by this retirement; rollback does NOT need to touch
`AuditTrailEventListenerConfig`.

**Risk of incomplete revert:** the dynamic-typed callsites
(`ExpressionExperimentWriteServiceImpl.java:245,255`,
`ArrayDesignProbeMapperCli.java:484`) and the truly-dynamic `AuditController.java:79`
remain on the imperative path regardless — no rollback needed for them.

## 6. Recommended next concrete step

**Single first commit:** Step 1 verification + Step 2 deletion combined IF step 1 confirms
the Curatable side-effect is already on the imperative path. If it isn't, that's a tiny
preparatory commit on its own.

- Title: `refactor(audit): retire AuditAdvice + dead Pointcuts.{creator,updater,saver,deleter} (Phase C terminal)`
- Scope: delete `AuditAdvice.java`, edit `Pointcuts.java` to drop the four orphaned
  pointcuts + `daoMethod()` + `loader()`, drop `IgnoreAudit` annotation usage on
  `CuratableDao.java:38` if applicable.
- Estimated diff: ~−376 LoC (1 deletion, 1 edit, optional 2 small deletions).
- Validation gate: `mvn verify` against gemdtest, expecting test fixture updates of step 3
  to land in the **same** commit (the integration tests will go red otherwise).

Realistically step 2 + step 3 should be **one commit** to keep `mvn verify` green; they
are coupled by the test fixture's behavioural expectation.

## 7. Open questions / blockers

1. **Curatable side-effect duplication.** Does
   `AuditTrailServiceImpl.doAddUpdateEvent` already call
   `curatableDao.updateCurationDetailsFromAuditEvent`? `AUDIT_SYSTEM_AUDIT.md:566` says yes
   (it cites `AuditTrailServiceImpl:126`), but a read-and-confirm before deletion is
   mandatory. **Decision needed before step 2.**

2. **WhatsNew dashboard scope.** Step 4 changes the front-page "What's New" widget's
   definition of "updated" from "any DAO update" to "any typed UPDATE event". This is a
   visible product behaviour change. Confirm with the curator team that this is desirable
   (Phase C's whole point IS that auto-UPDATE rows are noise — but the dashboard's
   prior intent was to surface DAO churn, not curator actions). **Product decision.**

3. **`AuditController.java:79` truly-dynamic path.** Confirm the controller still works
   with no AuditAdvice — it doesn't depend on the aspect, but it does call
   `auditTrailService.addUpdateEvent(entity, clazz, comment, detail)` which lives in
   `AuditTrailServiceImpl` and is unaffected. Spot-check via the existing
   `AuditController` integration test (if any).

4. **Generic-UPDATE row historical readability.** Existing prod rows with
   `action=U, eventType=null` remain in the `AUDIT_EVENT` table. No migration needed —
   they continue to be readable; no new rows of that shape will appear. Document this in
   the next REST API release notes.

5. **`ProcessedExpressionDataVectorServiceImpl.java:152`** is the lone GENERIC
   (no-event-type) imperative callsite: `auditTrailService.addUpdateEvent(ee, "Reordered
   the data vectors by experimental design")`. After retirement, this is one of the few
   places where an `eventType=null` UPDATE row will still appear. Either declare a
   `VectorsReorderedEvent` (1 file, ~15 LoC) and migrate, or document that this one
   specific operation produces a generic row. **Low priority cleanup.**

6. **CuratableDao `@IgnoreAudit` on the curation-detail update method
   (`CuratableDao.java:38`).** This annotation is only ever consulted by
   `AuditAdvice.java:133`. Once AuditAdvice is gone, the annotation is silently dead —
   strip the import + annotation to keep the codebase honest. Trivial follow-up.
