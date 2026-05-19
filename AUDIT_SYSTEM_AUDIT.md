# AUDIT_SYSTEM_AUDIT.md

Recce for retiring the generic auto-update audit mechanism in favour of
method-level `@Audited(EventType.class)` annotations. Also resolves the
`AuditTrail` Hibernate-6 cache-staleness bug documented in
`notable_cases.md` (lines 1–19) as a side effect.

Branch: `phase2-acl-migrate` (HEAD `41e612f547`).

---

## 1. How auto-audit is wired today

The mechanism is a Spring AOP aspect, not a Hibernate Interceptor. The aspect
class is:

- `gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditAdvice.java` (373 LoC)

It declares four `@Before` advices keyed on `Pointcuts` defined at
`gemma-core/src/main/java/ubic/gemma/persistence/util/Pointcuts.java`
(lines 71–94):

| Operation | Pointcut | Pointcut definition (Pointcuts.java) |
|---|---|---|
| CREATE  | `Pointcuts.creator()` (AuditAdvice.java:95)  | `daoMethod() && (execution(* create*(*, ..)) || execution(* findOrCreate*(*, ..)) || execution(* persist*(*, ..)) || execution(* add*(*, ..)))` (line 71) |
| UPDATE  | `Pointcuts.updater()` (AuditAdvice.java:110) | `daoMethod() && execution(* update*(*, ..))` (line 78) |
| SAVE    | `Pointcuts.saver()`   (AuditAdvice.java:126) | `daoMethod() && execution(* save*(*, ..))` (line 85) |
| DELETE  | `Pointcuts.deleter()` (AuditAdvice.java:141) | `daoMethod() && (execution(* remove*(*, ..)) || execution(* delete*(*, ..)))` (line 92) |

All four converge in `doAuditAdvice(JoinPoint, OperationType)`
(AuditAdvice.java:146) which:

1. Honours `@IgnoreAudit` opt-out (AuditAdvice.java:148; annotation defined at
   `gemma-core/src/main/java/ubic/gemma/core/security/audit/IgnoreAudit.java`, 13 LoC).
2. Fetches `User user = userManager.getCurrentUser()` under
   `FlushMode.MANUAL` (line 168–174) to dodge issue #1093.
3. Calls `extractAuditables(arg)` (line 346) which BFS-walks the first
   argument (`Map`/`Collection`/`Iterable`/array) and harvests every
   `Auditable`.
4. Per Auditable, dispatches to `addCreateAuditEvent` / `addUpdateAuditEvent` /
   `addSaveAuditEvent` / `addDeleteAuditEvent` (lines 212–257).
5. Each path calls `addAuditEvent(...)` (line 320) which mutates
   `auditable.getAuditTrail().getEvents()` in place; persistence relies on
   `cascade="all"` on the Auditable's `auditTrail` many-to-one + the
   `<bag>` on `AuditTrail.events` (also `cascade="all"`).

Event-type is **always null** for the auto-update path:
`AuditEvent.Factory.newInstance(date, auditAction, note, null, user, null)`
(AuditAdvice.java:332, last arg `null`). The action is recorded as
`AuditAction.UPDATE` (or `CREATE`/`DELETE`) without an `AuditEventType`
subclass — i.e. **generic** in the sense of section 4 below.

Cascade walker `cascadeAuditEvent(...)` (line 266) follows Hibernate
`CascadeStyle` to produce additional events on dependent Auditables (e.g.
EE → BioAssay → ArrayDesign chain on persist).

Curation side-effect: when the audited entity is `Curatable` and action is
`UPDATE`, `curatableDao.updateCurationDetailsFromAuditEvent(...)` runs
(AuditAdvice.java:334–336). This is the hook that keeps
`CurationDetails.lastUpdated` aligned with audit-trail writes.

Wiring: `AuditAdvice` is `@Aspect @Component`; Spring AOP picks it up via
component-scan. No XML registration. `@Order(4)` per advice (lines 94, 109,
125, 140).

A complementary Hibernate `PersistEventListener` was added on top of the
aspect:
- `gemma-core/src/main/java/ubic/gemma/persistence/audit/AuditTrailEventListener.java` (77 LoC, commit `9d0357c66d`).
- `gemma-core/src/main/java/ubic/gemma/persistence/audit/AuditTrailEventListenerConfig.java` (64 LoC).

This listener does NOT write `AuditEvent` rows — it only guarantees every
`Auditable` arrives at `session.persist` with a non-null `AuditTrail`
(AuditTrailEventListener.java:60–76). It is the infrastructure we extend
under "Phase C: keep auto-CREATE via Hibernate listener" below.

---

## 2. Auditable entities + cache settings

### Auditable hierarchy

- Root marker: `gemma-core/src/main/java/ubic/gemma/model/common/auditAndSecurity/Auditable.java` (15 LoC) extends `Securable`.
- Abstract base with field init: `gemma-core/src/main/java/ubic/gemma/model/common/auditAndSecurity/AbstractAuditable.java`:30 — `auditTrail = new AuditTrail()` on construction (this is why the persist-listener defensive branch rarely fires).
- One direct `implements Auditable` outside the abstract: `ExternalDatabase.java`.

Concrete Auditable entities (those with an `auditTrail` mapping in HBM):

| Class | HBM file | `class mutable=` | top-level `<cache>` |
|---|---|---|---|
| `Investigation` (abstract; `BioAssaySet` → `ExpressionExperiment`, `ExpressionExperimentSubSet` subclass) | `gemma-core/src/main/resources/ubic/gemma/model/analysis/Investigation.hbm.xml` line 5 | — (default mutable) | `read-write` line 6 |
| `ExpressionExperimentSet` | `gemma-core/src/main/resources/ubic/gemma/model/analysis/expression/ExpressionExperimentSet.hbm.xml` line 5 | — | `read-write` line 7 |
| `UserGroup` | `gemma-core/src/main/resources/ubic/gemma/model/common/auditAndSecurity/UserGroup.hbm.xml` line 7 | — | `read-write` line 8 |
| `ExternalDatabase` | `gemma-core/src/main/resources/ubic/gemma/model/common/description/ExternalDatabase.hbm.xml` line 5 | — | `nonstrict-read-write` line 6 |
| `ArrayDesign` | `gemma-core/src/main/resources/ubic/gemma/model/expression/arrayDesign/ArrayDesign.hbm.xml` line 7 | — | `read-write` line 8 |
| `GeneSet` | `gemma-core/src/main/resources/ubic/gemma/model/genome/gene/GeneSet.hbm.xml` line 7 | — | `read-write` line 8 |

All six declare `<many-to-one name="auditTrail" ... cascade="all" lazy="proxy"
fetch="select">` (e.g. ArrayDesign.hbm.xml:14, GeneSet.hbm.xml:14).

**No top-level Auditable entity has `mutable="false"`.** The cache-bug
victims are exactly two:

| Entity | HBM file:line | `mutable` | `<cache>` |
|---|---|---|---|
| `AuditTrail` | `gemma-core/src/main/resources/ubic/gemma/model/common/auditAndSecurity/AuditTrail.hbm.xml` line 7 | **`mutable="false"`** | (none on class) |
| `AuditEvent` | `gemma-core/src/main/resources/ubic/gemma/model/common/auditAndSecurity/AuditEvent.hbm.xml` line 6 | **`mutable="false"`** | **`<cache usage="read-only"/>`** line 8 |

`AuditEventType.hbm.xml:5` also `mutable="false"` (concrete types are
immutable singletons — fine to keep; they're never the parent of a stale
bag).

The `notable_cases.md` cache-bug victim is therefore the `AuditTrail.events`
bag specifically: `AuditTrail` is `mutable="false"` and its `<bag>` (line
14, `lazy="false" fetch="select" cascade="all"`) carries `AuditEvent` rows
which are also `mutable="false"` + `read-only` cached. Inserts to that bag
across separate `@Transactional` boundaries (e.g. `addUpdateEvent` from a
service method on an EE that was loaded earlier in the same request) leave
stale empty bags in the L1/query/L2 cache.

---

## 3. Event types

Total concrete `AuditEventType` subclasses under
`gemma-core/src/main/java/ubic/gemma/model/common/auditAndSecurity/eventType/`:
**94 `.java` files** (this includes the abstract `AuditEventType.java` plus
abstract intermediaries like `AnnotationEvent`, `CellTypeAssignmentEvent`,
`CellLevelCharacteristicsEvent`, `CurationDetailsEvent`,
`NeedsAttentionAlteringEvent`, `TroubledStatusFlagAlteringEvent`,
`PreferredDataChangedEvent`, `VersionedEvent`,
`ExpressionExperimentAnalysisEvent`, `ArrayDesignAnalysisEvent`,
`AnalysisSuitabilityEvent`, `ExpressionExperimentUpdateFromGEOEvent`).

Top concrete typed events by `addUpdateEvent(...)` call count (from
section 4 grep, ranked by reference count, gemma-{core,rest,web,cli}/src/main):

| Count | Event type (TYPED) |
|---|---|
| 4 | FailedSampleCorrelationAnalysisEvent |
| 4 | FailedProcessedVectorComputationEvent |
| 4 | ExpressionExperimentPlatformSwitchEvent |
| 3 | ProcessedVectorComputationEvent |
| 3 | ExperimentalDesignUpdatedEvent |
| 2 | SingleBatchDeterminationEvent |
| 2 | ReleaseDetailsUpdateEvent |
| 2 | PreferredCellTypeAssignmentChangedEvent |
| 2 | FailedDataReplacedEvent |
| 2 | BatchProblemsUpdateEvent |
| 2 | BatchInformationFetchingEvent |
| 2 | DataAddedEvent |
| 1 | each of the remaining ~30 |

GENERIC (auto-UPDATE, eventType = null): emitted by `AuditAdvice` on every
DAO `update*` call. Not represented as a Java class — recorded as
`AuditEvent.action = AuditAction.UPDATE` with `eventType = null`. From the
`notable_cases.md` evidence: prod row counts are dominated by these auto-UPDATE
rows (CREATE/DELETE auto + a few hundred typed UPDATEs vs many thousands of
generic UPDATEs).

---

## 4. Caller inventory — who creates audit events today (imperative)

Total call sites of `auditTrailService.add{Update,Create,Delete,Event}*`
across `gemma-{core,rest,web,cli}/src/main/java`: **83**.

Classification:

| Class | Count | Definition |
|---|---|---|
| TYPED hardcoded | **77** | `auditTrailService.addUpdateEvent(entity, ConcreteEvent.class, ...)` — event-type literal at the call site. Migration candidate for `@Audited(ConcreteEvent.class)`. |
| Dynamic but locally-typed | **4** | Local helper branches over a small known set of `*.class` literals; helper signature accepts `Class<? extends AuditEventType>`. Migration candidate (hoist branch into multiple annotated methods, or one method per branch). |
| GENERIC (no event-type) | **1** | `ProcessedExpressionDataVectorServiceImpl.java:160` — `auditTrailService.addUpdateEvent(ee, "Reordered the data vectors by experimental design")` — note-only. Migration option: declare a `VectorsReorderedEvent` and switch to `@Audited`. |
| Truly dynamic (user-supplied type) | **1** | `gemma-web/.../AuditController.java:79` — `Class.forName("ubic.gemma.model.common.auditAndSecurity.eventType." + auditEventType)`. Comes from a controller request param. **Keep imperative.** |

### Top 10 typed-hardcoded call sites (chosen for representative migration)

| File | Line | Event type |
|---|---|---|
| `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/MeanVarianceServiceImpl.java` | 93 | `MeanVarianceUpdateEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/svd/SVDServiceImpl.java` | 589 | `PCAAnalysisEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/PreprocessorServiceImpl.java` | 139 | `BatchCorrectionEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/VectorMergingServiceImpl.java` | 253 | `ExpressionExperimentVectorMergeEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/service/OutlierFlaggingServiceImpl.java` | 96 | `SampleRemovalEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/core/analysis/preprocess/batcheffects/BatchInfoPopulationHelperServiceImpl.java` | 92 | `UninformativeFASTQHeadersForBatchingEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/core/loader/expression/arrayDesign/ArrayDesignMergeHelperServiceImpl.java` | 87 | `ArrayDesignMergeEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/persistence/service/common/description/ExternalDatabaseServiceImpl.java` | 108 | `ReleaseDetailsUpdateEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/GeeqServiceImpl.java` | 566 | `GeeqEvent.class` |
| `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/FactorValueNeedsAttentionServiceImpl.java` | 41 | `FactorValueNeedsAttentionEvent.class` |

Pattern in every TYPED case: the call is the last line(s) of a service
method whose name describes the operation (`addSampleRemoval`,
`updatePreprocessed`, `setBatchInformation`, `updateRelease`, …). The
event-type is determined statically by which method ran — exactly the
`@Audited(X.class)` shape.

### Dynamic-typed call sites (4)

1. `gemma-core/src/main/java/ubic/gemma/core/loader/expression/DataUpdaterImpl.java:736` — `private void audit(ExpressionExperiment ee, String note, boolean replace)` branches `DataReplacedEvent.class` vs `DataAddedEvent.class`. Trivial to split.
2. `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentWriteServiceImpl.java:245` and `:255` — `Class<? extends PreferredDataChangedEvent> eventType = getPreferredDataChangedEventForVectorType(vectorType)`. Returns one of three subtype classes. Annotation migration would need either (a) a `@Audited(PreferredDataChangedEvent.class)` covering all three (loses specificity), (b) one annotated method per vector type, or (c) keep imperative.
3. `gemma-cli/src/main/java/ubic/gemma/apps/ArrayDesignProbeMapperCli.java:484` — same `eventType` parameter pattern; helper called from two sites with different `ArrayDesignGeneMappingEvent` subclasses. Can split.

### Truly dynamic (1)

- `gemma-web/src/main/java/ubic/gemma/web/controller/common/auditAndSecurity/AuditController.java:79` — event-type from `Class.forName(...)` against user input. Must stay imperative. Spec: `@Audited` aspect should coexist with `AuditTrailService.addUpdateEvent(...)` for this one path.

---

## 5. Auto-update CONSUMERS — critical risk inventory

Question: which code reads back generic auto-UPDATE rows (eventType = null,
action = U)? If a consumer depends on them, dropping auto-UPDATE changes
its output. Surveyed all callers of `auditEventService.*` (20 files) and
direct `auditTrail.getEvents()` access (2).

### HARD dependency on generic auto-UPDATE rows

| Consumer | File:line | What it does | Behavioural impact if generic UPDATE removed |
|---|---|---|---|
| `WhatsNewServiceImpl.getReport` | `gemma-core/src/main/java/ubic/gemma/core/analysis/report/WhatsNewServiceImpl.java` lines 218–219 | Calls `auditEventService.getUpdatedSinceDate(ArrayDesign.class, date)` and same for `ExpressionExperiment` — DAO query `where ae.action='U'` (AuditEventDaoImpl.java:117–128). Drives the front-page "What's New" widget showing updated datasets/platforms. | **HIGH risk**: drops to near-zero results. Today every DAO update on an EE bumps it onto the dashboard; with auto-UPDATE retired, only EEs receiving a typed UPDATE event in the window would appear. Mitigation: switch to "updated since" = "received any typed event since". |
| `DatasetsWebService.getDatasetAuditEvents` | `gemma-rest/src/main/java/ubic/gemma/rest/DatasetsWebService.java:871` | REST `GET /datasets/{id}/auditEvents` returns ALL events via `auditEventService.getEvents(ee)`. | **MEDIUM risk**: REST payload shrinks (no more 1-line "U" rows). External API change — must be documented. Mitigation: switch endpoint to `getEventsWithType` to preserve only typed history. |
| `ArrayDesignReportServiceImpl.getCreateDate` | `gemma-core/src/main/java/ubic/gemma/core/analysis/report/ArrayDesignReportServiceImpl.java:422–425` | Reads `getEvents(ad).get(0)` — assumes first event is CREATE. | **LOW risk**: CREATE is preserved under Phase C (Hibernate `PersistEventListener` will own it). Still works, but assertion of "events[0] == CREATE" becomes stronger guarantee. |
| `ArrayDesignAuditTrailCleanupCli` | `gemma-cli/src/main/java/ubic/gemma/apps/ArrayDesignAuditTrailCleanupCli.java:46–60` | Cleanup CLI that buckets events by type and deletes all but the most recent per type. Has a special bucket for `eventType==null && action==UPDATE` (line 55–59). | **LOW risk**: the bucket simply becomes empty under Phase C. The cleanup script's whole reason for existing is to prune the auto-UPDATE clutter — retiring auto-UPDATE makes this CLI partially obsolete. |
| `ExternalDatabaseOverviewCli.summarize` | `gemma-cli/src/main/java/ubic/gemma/apps/ExternalDatabaseOverviewCli.java:60` | Diagnostic CLI: prints all events. | **LOW risk**: fewer lines printed; operator-facing only. |
| `AuditAdvice.addAuditEvent` itself | `gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditAdvice.java:327` | Reads `auditable.getAuditTrail().getEvents().isEmpty()` to decide whether to skip a cascaded CREATE. | **NONE**: this is internal to AuditAdvice; replaced wholesale under Phase C. |

### SAFE consumers (filter for typed events; auto-UPDATE removal is a no-op)

| Consumer | Why safe |
|---|---|
| `AuditController.getEvents` (`gemma-web/.../AuditController.java:97`) | Already uses `getEventsWithType()` — filters `eventType != null`. |
| `AbstractAutoSeekingCLI.noNeedToRun` (`gemma-cli/.../AbstractAutoSeekingCLI.java:210–239`) | Iterates `getEvents(...)` but only acts on `eventClass.isInstance(eventType)`. Generic rows are skipped. |
| `ArrayDesignSequenceManipulatingCli.getEvents/needToAutoRun` (`:252`, `:269`, `:299–300`) | Filters on `eventClass.isAssignableFrom(...)`. Generic rows skipped. |
| `ArrayDesignProbeMapperCli` (`:286`) | Same `eventType != null && ...isAssignableFrom(...)` filter. |
| `DataUpdaterImpl.hasVectorMergeEvent` (`:898`) | Filters `event.getEventType() instanceof ExpressionExperimentVectorMergeEvent`. |
| `ArrayDesignReportServiceImpl.getLastEvent` (`:427–456`) | Filters typed. |
| `TableMaintenanceUtilImpl.updateGene2CsEntries` (`:477`, `:485`) | Uses `getNewSinceDate` (CREATE) + `getLastEvents(..., ArrayDesignGeneMappingEvent.class)` (typed). |
| `AuditEventService.getLastEvent(auditable, type)` & `getLastEvents(auditableClass, type)` family | Server-side filters via HQL `type(et) in :classes` (AuditEventDaoImpl.java:175–235). |
| `AuditEventService.getNewSinceDate` | Filters `ae.action='C'` — CREATE-only. |
| `AuditEventService.getCreateEvents` | Same — CREATE-only. |

### Critical-risk summary

Two HARD dependencies: `WhatsNewServiceImpl` and `DatasetsWebService`.
Both have straightforward fixes:

- `WhatsNew`: re-define "updated" as "has a non-create typed event in window".
  One DAO change (add `getUpdatedSinceDateWithType`), one service edit.
- `DatasetsWebService`: switch the public endpoint to `getEventsWithType`, document
  the response-shape change in the next REST API release notes.

Everything else either filters server-side (safe) or only consumes typed
events client-side (safe).

---

## 6. Proposed migration plan

### Phase A — Introduce `@Audited` infrastructure (~120 LoC added, 0 LoC removed)

Goal: aspect proven on 2–3 high-traffic call sites; behaviour identical to
imperative path.

1. **New annotation** `gemma-core/src/main/java/ubic/gemma/core/security/audit/Audited.java` (~15 LoC):
   ```java
   @Target(METHOD) @Retention(RUNTIME) @Documented
   public @interface Audited {
       Class<? extends AuditEventType> value();
       String noteTemplate() default ""; // optional SpEL or printf hint
   }
   ```
2. **New aspect** `gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditedAdvice.java` (~80 LoC):
   - `@AfterReturning` (not `@Before`) on `@annotation(Audited)` — so failure
     paths don't record success events.
   - Resolve the first `Auditable` argument (mirror
     `AuditAdvice.extractAuditables`).
   - Delegate to `auditTrailService.addUpdateEvent(auditable, ann.value(), note)`.
   - Honour `@IgnoreAudit`.
   - `@Order(5)` so it fires after Spring transactional advice but before
     `AuditAdvice` (which is `@Order(4)` on DAOs — different join-point
     anyway).
3. **Migrate 3 pilot sites** (annotation-only diffs, ~20 LoC):
   - `MeanVarianceServiceImpl.calculateMeanVariance(ee)` → annotate with `@Audited(MeanVarianceUpdateEvent.class)`; remove line 93.
   - `OutlierFlaggingServiceImpl.markAsMissing(...)` → `@Audited(SampleRemovalEvent.class)`; remove line 96.
   - `GeeqServiceImpl.calculateScore(ee)` → `@Audited(GeeqEvent.class)`; remove line 566.
4. **Validation**:
   - `mvn -pl gemma-core test -Dtest='*Audit*'` (existing AuditAdvice tests
     still pass — `AuditAdvice` is untouched).
   - Integration test: `MeanVarianceServiceTest` (or whichever covers SVD/Geeq
     pipelines) on `gemdtest` confirms an `AuditEvent` of the expected type
     appears after the annotated method runs. New tests for the aspect
     itself: aspect + `@IgnoreAudit` + transient-entity guard.

Risk: low. New code path, no callers removed. Two paths coexist briefly.

### Phase B — Sweep TYPED hardcoded callers to `@Audited` (~−250 LoC net)

Per the inventory: 77 TYPED hardcoded callers + 3 dynamic-but-locally-typed
(split into ~7 annotated methods) = ~84 sites converted, ~84 imperative
lines removed.

Each migration is mechanical:
1. Locate the call site's enclosing service method.
2. Add `@Audited(X.class)` on the method signature.
3. Remove the `auditTrailService.addUpdateEvent(entity, X.class, note)` line.
4. If the call site referenced `auditTrailService` only for this, drop the
   field + autowire.

Hardest cases (do these last, or keep imperative):
- `DataUpdaterImpl.audit(ee, note, replace)` (line 736) — split into two
  annotated methods (`addRawData` / `replaceRawData`) or keep imperative.
- `ExpressionExperimentWriteServiceImpl.setPreferred(...)` (lines 245, 255)
  — three subtypes of `PreferredDataChangedEvent`. Cleanest fix: annotate
  with the supertype and accept the loss of subtype precision, OR retain
  imperative for this one method.
- `ArrayDesignProbeMapperCli.audit(...)` (line 484) — CLI, two annotated
  private methods per branch are fine.

Out-of-scope for `@Audited` (keep imperative):
- `AuditController.addAuditEvent(...)` (truly dynamic from HTTP).
- `auditTrailService.addUpdateEvent(Auditable, Class, String, Throwable)`
  callers (12 sites grepped — `addUpdateEvent` with `Throwable` arg) — these
  are catch-block failure recordings that must record stack-trace detail
  beyond what `@Audited` can sense. The `REQUIRES_NEW` semantics are also
  load-bearing.

Validation: full `mvn verify` against `gemdtest` per the parallel-agent
single-tenancy rule (memory `feedback_parallel_gemma_agents.md`); spot-check
a curation workflow E2E (a `FactorValueNeedsAttentionServiceImpl.markAsNeedsAttention(ee, fv)`
call should fire `FactorValueNeedsAttentionEvent` automatically AND update
`CurationDetails.needsAttention`).

Risk: medium. The `Curatable + UPDATE` curation-details side-effect
(AuditAdvice.java:334) lives in the OLD aspect. The new aspect must
replicate it OR we leave the old AuditAdvice running for `Curatable`
updates only.

### Phase C — Drop generic auto-UPDATE + fix cache bug (~−300 LoC net)

1. **Delete generic UPDATE / SAVE from `AuditAdvice`**:
   - Remove `@Before("...updater()")` advice (AuditAdvice.java:108–113).
   - Remove `@Before("...saver()")` advice (AuditAdvice.java:120–129).
   - Keep `creator()` (will be migrated to Hibernate listener — see step 3).
   - Keep `deleter()` (low-frequency, useful provenance — also migratable to
     listener but lower priority).
   - Remove `OperationType.UPDATE` and `OperationType.SAVE` branches.
   - Net: ~−130 LoC in `AuditAdvice.java` (373 → ~240).

2. **Drop `mutable="false"` from `AuditTrail.hbm.xml:7` and `AuditEvent.hbm.xml:6`**;
   drop `<cache usage="read-only"/>` from `AuditEvent.hbm.xml:8`.
   Trail/event become normal mutable entities with no L2 cache. Removes the
   Hibernate-6 stale-bag bug (`notable_cases.md`).

3. **Move auto-CREATE to the existing Hibernate listener**: extend
   `AuditTrailEventListener` (currently 77 LoC) to enqueue a CREATE
   `AuditEvent` on first persist of each Auditable (~+30 LoC). It already
   has the chokepoint; `cascade="all"` already carries new entities into the
   persist phase. Use `PostInsertEventListener` instead of `PersistEventListener`
   to ensure the AuditTrail row has its ID before adding events (avoids
   transient-entity errors). This is the same shape as `AuditAdvice.addCreateAuditEvent`
   (lines 212–215) but driven by Hibernate's lifecycle events rather than
   AOP join-points on DAO methods.

4. **Migrate the 2 hard consumers**:
   - `WhatsNewServiceImpl`: replace `getUpdatedSinceDate(class, date)` calls
     with a new `getUpdatedSinceDateForType(class, date)` that joins on
     `ae.eventType is not null` instead of `ae.action='U'`. ~10 LoC.
   - `DatasetsWebService.getDatasetAuditEvents`: switch to
     `getEventsWithType`. Update OpenAPI/Swagger doc to note generic UPDATE
     events are no longer returned. ~3 LoC + doc.

5. **Drop the now-unused `AuditAction` enum value `UPDATE`** from
   `getUpdatedSinceDate` — keep the enum but the action column will only
   ever hold `C` / `D` rows in new writes. Historical `U` rows in production
   remain readable.

Risk: medium. Once cache bug is fixed, the test from `notable_cases.md`
case 1 (`ArrayDesignReportServiceTest`) should go green without any
workaround.

### Cumulative shape

| Phase | Action | LoC delta |
|---|---|---|
| A | Add `@Audited` annotation + aspect; migrate 3 pilots | +95 |
| B | Sweep ~84 typed callers; remove `auditTrailService.addUpdateEvent(typed,...)` calls | −250 |
| C | Drop auto-UPDATE/SAVE from `AuditAdvice`; drop cache mutability; move auto-CREATE to listener; migrate 2 hard consumers | −300 |
| **Total** | | **≈ −455 LoC** |

---

## 7. LoC accounting

### Audit subsystem TODAY (HEAD `41e612f547`)

Direct subsystem files (gemma-core):

| File | LoC |
|---|---|
| `core/security/audit/AuditAdvice.java` | 373 |
| `core/security/audit/AuditLogger.java` | 39 |
| `core/security/audit/IgnoreAudit.java` | 13 |
| `persistence/audit/AuditTrailEventListener.java` | 77 |
| `persistence/audit/AuditTrailEventListenerConfig.java` | 64 |
| `persistence/service/common/auditAndSecurity/AuditTrailService.java` | 95 |
| `persistence/service/common/auditAndSecurity/AuditTrailServiceImpl.java` | 153 |
| `persistence/service/common/auditAndSecurity/AuditEventService.java` | 102 |
| `persistence/service/common/auditAndSecurity/AuditEventServiceImpl.java` | 133 |
| `persistence/service/common/auditAndSecurity/AuditEventDao.java` | 103 |
| `persistence/service/common/auditAndSecurity/AuditEventDaoImpl.java` | 301 |
| `persistence/service/common/auditAndSecurity/AuditTrailDao.java` | 39 |
| `persistence/service/common/auditAndSecurity/AuditTrailDaoImpl.java` | 72 |
| **Subsystem total** | **1564** |

Plus 83 imperative call sites in `gemma-{core,rest,web,cli}` ≈ 1–2 LoC each
≈ ~100 LoC of caller boilerplate.

Plus HBM workaround: 3 lines (`mutable="false"` on AuditTrail.hbm.xml:7,
`mutable="false"` + `<cache usage="read-only"/>` on AuditEvent.hbm.xml:6 +
8) that are responsible for the cache bug.

**Grand total in scope today: ~1664 LoC + 3 HBM workaround lines.**

### Audit subsystem AFTER Phase C

| Change | LoC |
|---|---|
| `AuditAdvice.java` (UPDATE+SAVE removed, CREATE/DELETE migrated to listener so the file shrinks further) | 373 → ~120 (−253) |
| New `AuditedAdvice.java` aspect | +80 |
| New `Audited.java` annotation | +15 |
| `AuditTrailEventListener.java` extended to own CREATE | 77 → ~110 (+33) |
| `AuditEvent.hbm.xml` cache directive removed | −1 line |
| `AuditTrail.hbm.xml` mutable=false removed | −1 line |
| `AuditEvent.hbm.xml` mutable=false removed | −1 line |
| Caller boilerplate (~84 sites, ~1.5 lines each, net −1 line per site after annotation added) | −84 |
| `WhatsNewServiceImpl` + new DAO method | +10 |
| `DatasetsWebService` endpoint switch | +1 |
| `AuditTrailService` `addUpdateEvent` overloads remain (used by truly-dynamic + Throwable paths) | unchanged |
| `AuditEventService` / `AuditEventDao` unchanged | unchanged |

**Net delta: ≈ −455 LoC across `gemma-{core,rest,web,cli}/src/main/java`
plus the cache bug retired.**

(Optimistic: Phase B replaces ~84 add-event lines + their accumulated
imports & autowires; conservative: −300 LoC if some imperative callers stay
for SpEL-resistant note formatting.)

---

## Appendix A — HBM cache-bug fingerprint (verbatim from notable_cases.md)

> `<cache usage="read-only"/>` on AuditEvent (`AuditEvent.hbm.xml:8`),
> combined with `mutable="false"` on both `AuditTrail.hbm.xml:7` and
> `AuditEvent.hbm.xml:6` — INSERT-then-set-FK across separate
> `@Transactional` boundaries leaves a stale empty-bag in the L2 / query
> cache that subsequent reads honour over the DB truth.

Phase C step 2 is the surgical fix for exactly this fingerprint.

## Appendix B — Files surveyed but not directly touched by the migration

- `gemma-core/.../persistence/service/common/auditAndSecurity/curation/{AbstractCuratableDao,GenericCuratableDaoImpl}.java` — `updateCurationDetailsFromAuditEvent` invoked from `AuditAdvice:335` and `AuditTrailServiceImpl:126`. The new aspect must invoke this for `Curatable + UPDATE`. ~5 LoC delta.
- `gemma-core/.../model/common/auditAndSecurity/eventType/AuditEventType.hbm.xml:5` — `mutable="false"` is fine to keep (event types are conceptually singletons).
- `applicationContext-component-scan.xml` / equivalent — picks up the new `@Component @Aspect` automatically. No XML edit.
