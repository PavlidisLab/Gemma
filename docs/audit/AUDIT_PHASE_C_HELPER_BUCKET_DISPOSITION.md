# Audit migration Phase C — helper-bucket disposition

**Filed:** 2026-05-19. Branch `audit-phaseB-helpers`, baseline
`c30041179b522c6d90ac8c8e02e626791e90eb0f` (phase2-acl-migrate HEAD).

**Companion docs:**
- `AUDIT_MIGRATION_PHASE_C_RECCE.md` — parent recce that named the
  buckets and tallied the 23 helper-bucket sites.
- `AUDIT_SYSTEM_AUDIT.md` — full current-state audit.
- `AUDIT_AS_WORKFLOW_RECCE.md` — Spring Modulith / Ticket-layer
  follow-on that wants the same publish-event hook.

**Status:** disposition only. No Java changes in this commit.

---

## TL;DR

The 23 helper-bucket sites identified in
`AUDIT_MIGRATION_PHASE_C_RECCE.md §1` **stay imperative** for the
Phase C landing. They keep calling `auditTrailService.addUpdateEvent(...)`
directly. The `AuditEmitter` helper sketched in the recce §2.2 is
**deferred to Phase D** ("programmatic audit emission helper +
audit-event publishing for the Spring Modulith readiness story"), per
the recce's own §5 recommendation.

This closes the "helper bucket" as a no-op for Phase C scope.

---

## 1. Why leave them imperative

The parent recce §5 already concluded this:

> The `AuditEmitter` helper from §2.2 is **out-of-scope for the Phase
> C landing**. It's the natural launching point for the Ticket-layer
> work in `AUDIT_AS_WORKFLOW_RECCE` but should be filed as its own
> follow-on PR ("Phase D: programmatic audit emission helper +
> audit-event publishing for the Spring Modulith readiness story").
> That keeps Phase C's PR diff focused: the AuditAdvice deletion + the
> listener addition + the cache HBM fix + the mechanical sweep, nothing
> else.

Three concrete reasons to defer:

1. **Phase C's load-bearing step is the Hibernate listener** (C-1),
   not the helper. C-1 + C-2 retire `AuditAdvice` and replace
   auto-CREATE / auto-DELETE emission with `PostInsertEventListener` /
   `PreDeleteEventListener`. The 23 helper-bucket sites do not block
   that work — they continue calling `addUpdateEvent` on the
   imperative API, which survives Phase C unchanged.

2. **`AuditTrailService.addUpdateEvent` is already the right helper
   for these shapes.** Reviewing the 23 sites (table below) shows
   each one is doing exactly what the imperative API exists for:
   conditional emission inside a public method, mid-method exit on a
   guard arm, multi-event-per-method REST endpoints, branched-event
   private helpers. None of these shapes simplify under `@Audited` —
   `@Audited` is one-annotation-one-event from `@AfterReturning` of
   the outer method, and these sites need either branching or
   mid-method emission.

3. **Adding `AuditEmitter` now would land the publish-event chain
   without the Spring Modulith / Ticket-layer consumer.** The whole
   value of the helper (per recce §2.2) is publishing `AuditedEvent`
   so downstream listeners — the Ticket write-back from
   `AUDIT_AS_WORKFLOW_RECCE` — see imperative writes too. Without
   that consumer landed, the helper is a no-op wrapper that adds 23
   call-site touches for zero observable behaviour change. Land them
   together in Phase D.

---

## 2. The 23 sites — confirmed inventory

Re-grepped against the worktree baseline; the 23 sites match the
recce's classification. They split into the same six shapes as
`AUDIT_MIGRATION_PHASE_C_RECCE.md §1` buckets 3 + 4 + 6 (9 + 8 + 6 =
23):

### 2.1 Conditional emission (shape 3, 9 sites)

These emit only when a predicate evaluates true inside the public
method's body. `@Audited` cannot express "emit IF this side-effect
fired" without leaking the predicate into a SpEL string. Stays
imperative.

| File | Line | Shape |
|---|---:|---|
| `ExpressionExperimentReportServiceImpl.java` | 392 | `if (batchProblems)` → BatchProblemsUpdateEvent |
| `ExpressionExperimentReportServiceImpl.java` | 407 | `if (batchEffect changed)` → BatchProblemsUpdateEvent |
| `BatchInfoPopulationServiceImpl.java` | 176 | `if (batchFactor != null)` → SingleBatchDeterminationEvent |
| `BatchInfoPopulationServiceImpl.java` | 179 | else-branch → BatchInformationFetchingEvent |
| `BatchInfoPopulationServiceImpl.java` | 219 | second predicate arm → SingleBatchDeterminationEvent |
| `BatchInfoPopulationServiceImpl.java` | 222 | else-branch → BatchInformationFetchingEvent |
| `GenericGenelistDesignGenerator.java` | 325 | `if (!noDB)` → AnnotationBasedGeneMappingEvent |
| `SingleCellExpressionExperimentServiceImpl.java` | 1452 | conditional → ExperimentalDesignUpdatedEvent |
| `SingleCellExpressionExperimentServiceImpl.java` | 1479 | conditional → ExperimentalDesignUpdatedEvent |

### 2.2 Early-return / mid-method exit (shape 4, 8 sites)

Emit a typed event before `return null` from a `catch` block or guard
arm. `@AfterReturning` would fire on the *outer* method's return —
it cannot distinguish "I exited via the FASTQHeaders exception arm"
from a normal return. Stays imperative.

| File | Line | Shape |
|---|---:|---|
| `BatchInfoPopulationHelperServiceImpl.java` | 92 | catch FASTQHeadersPresentButNotUsable → UninformativeFASTQHeadersForBatchingEvent → return null |
| `BatchInfoPopulationHelperServiceImpl.java` | 96 | catch SingletonBatchesException → SingletonBatchInvalidEvent → return null |
| `DataUpdaterImpl.java` | 405 | mid-method guard → FailedDataReplacedEvent |
| `DataUpdaterImpl.java` | 420 | mid-method guard → FailedDataReplacedEvent |
| `DataUpdaterImpl.java` | 492 | conditional emit → ExpressionExperimentPlatformSwitchEvent |
| `DataUpdaterImpl.java` | 666 | conditional emit → ExpressionExperimentPlatformSwitchEvent |
| `ExpressionExperimentWriteServiceImpl.java` | 245 | branch in for-loop → branched eventType |
| `ExpressionExperimentWriteServiceImpl.java` | 255 | branch in for-loop → branched eventType |

### 2.3 Multi-event-per-method (shape 6, 6 sites)

Same public method writes >1 event of potentially different types
depending on which fields the request body sets. Splitting into N
controller endpoints would break the REST API; splitting into N
private services is the kind of cargo-cult refactor that makes the
code worse. Stays imperative.

| File | Line | Shape |
|---|---:|---|
| `OutlierFlaggingServiceImpl.java` | 96 | SampleRemovalEvent OR SampleRemovalReversionEvent in same flow |
| `OutlierFlaggingServiceImpl.java` | 135 | (paired with above) |
| `SingleCellExpressionExperimentServiceImpl.java` | 936 | CellTypeAssignmentAddedEvent path |
| `SingleCellExpressionExperimentServiceImpl.java` | 994 | PreferredCellTypeAssignmentChangedEvent (set) path |
| `SingleCellExpressionExperimentServiceImpl.java` | 1044 | PreferredCellTypeAssignmentChangedEvent (clear) path |
| `DatasetsWebService.java` | 1154 | CurationNoteUpdateEvent (one of 5 typed events the same endpoint can emit) |

---

## 3. The Phase D contract (forward link)

When `AuditEmitter` lands in Phase D, the migration of these 23 sites
becomes a near-mechanical sweep:

```java
// before (current, stays this way through Phase C):
auditTrailService.addUpdateEvent( ee, FooEvent.class, note );

// after (Phase D):
auditEmitter.emit( ee, FooEvent.class, note );
```

with the bonus that `auditEmitter.emit` publishes `AuditedEvent` for
the Spring Modulith / Ticket-layer write-back consumer. The 23 sites
are pre-tagged in §2 of this doc so the Phase D agent can find them
in one grep.

The recce §2.2 sketch is the API contract:
- `emit(target, type, note)` — typed UPDATE + publish.
- `emitIf(cond, target, type, note)` — conditional variant for shape
  3 (9 sites).
- `emitFailure(target, type, note, throwable)` — REQUIRES_NEW
  variant for the 14 catch-block sites (separate bucket, not in
  this disposition's 23).

---

## 4. What this disposition does NOT decide

- The 28 mechanical-bucket sites (recce §1 shapes 1 + 12). Owned by
  the audit-c2 agent during C-3 sweep.
- The 17 stays-imperative sites (recce §1 shapes 2 + 7 + 11). Already
  documented as out-of-scope; no further action.
- The 7 judgment-call sites (recce §1 shapes 5 + 8 + 9). Need
  per-site review during Phase C-3 cleanup.
- The Hibernate listener (C-1) and `AuditAdvice` deletion (C-2).
  Owned by the audit-c2 agent / the parent recce.

---

## 5. Outcome

- **23 helper-bucket sites:** confirmed staying imperative through
  Phase C. No code changes in this branch.
- **`AuditEmitter` helper:** deferred to Phase D, per recce §5.
- **Helper bucket as a Phase C obstacle:** closed as no-op.
