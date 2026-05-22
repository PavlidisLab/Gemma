# AUDIT_RESIDUAL_INVENTORY.md

Targeted inventory of the imperative `auditTrailService.addUpdateEvent(...)`
callsites still living in `gemma-core/src/main/java` as of
`64e468f72f3ef1a63c3c4cc2c884543eb93ff4ef`
(`phase2-acl-migrate`). Excludes the audit framework itself
(`AuditedAspect.java`, `AuditedConditional.java`, `AuditedOnError.java`)
and javadoc-only references in the five `*AuditServiceImpl` /
`*HelperServiceImpl` companion beans (which were created precisely so the
imperative call no longer happens in business code — see
`DataUpdaterAuditServiceImpl`, `ArrayDesignMergeAuditServiceImpl`,
`GeoUpdateAuditServiceImpl`, `GeeqAuditServiceImpl`,
`PreprocessorHelperServiceImpl`).

Buckets refer to `AUDIT_PHASE_C_RECCE.md` §2.

| # | Service | Callsite | Event type | Conditional? | Self-invoked? | Bucket | Notes |
|---|---|---|---|---|---|---|---|
| 1 | `SingleCellExpressionExperimentSubSetServiceImpl` | `:137` (`createSubSets`) | `SingleCellSubSetsCreatedEvent` | No (end-of-method) | No — public `@Transactional` | 2f (4-arg detail) | Details StringBuilder serialized to `DETAIL`. Blocked on `AuditEventPayload` Phase A. |
| 2 | `SingleCellExpressionExperimentAggregateServiceImpl` | `:325` (`aggregateVectors`) | `DataAddedEvent` | No (end-of-method) | No — public `@Transactional` | 2f (4-arg detail) | Details StringBuilder for aggregate composition. Blocked on payload Phase A. |
| 3 | `PreprocessorServiceImpl` | `:135` (`batchCorrect`) | `BatchCorrectionEvent` | No (early-return at top, then unconditional emit) | No — package-private but called from public `process()` via `this` | 2b (typed-from-result) — but self-invoke caveat | Note interpolates `replaced` int. Method is `private`; needs hoist or `@Audited` on the public `process()` ancestor. Bucket 2g overlap. |
| 4 | `BatchInfoPopulationHelperServiceImpl` | `:95` (`createRnaSeqBatchFactor`) | `UninformativeFASTQHeadersForBatchingEvent` | Yes (in `catch(FASTQHeadersPresentButNotUsableException)`) | No — public `@Transactional` | 2d (multi-event) + 2c-ii (multi-branch) | One of two failure branches in the same method. Needs branch extraction. |
| 5 | `BatchInfoPopulationHelperServiceImpl` | `:99` (`createRnaSeqBatchFactor`) | `SingletonBatchInvalidEvent` | Yes (in `catch(SingletonBatchesException)`) | No — public `@Transactional` | 2d + 2c-ii | Second of the two failure branches. Same method as #4; one extraction unblocks both. |
| 6 | `DifferentialExpressionAnalyzerServiceImpl` | `:301` (`persistAnalyses`/audit step) | `DifferentialExpressionAnalysisEvent` | No (end-of-method, wrapped in defensive try/catch) | No — public flow | 2f (4-arg detail) | `analysis.getDescription()` becomes `DETAIL`. Blocked on payload Phase A. |
| 7 | `OutlierFlaggingServiceImpl` | `:96` (`markAsMissing`) | `SampleRemovalEvent` | Yes (early-return on `!hasNewOutliers`) | No — public `@Transactional(NEVER)` | 2c-i + 2f | Note `count + " flagged as outliers"`, detail = joined bioAssay list. **Owned by `audit-2f-sweep` worktree.** |
| 8 | `OutlierFlaggingServiceImpl` | `:135` (`unmarkAsMissing`) | `SampleRemovalReversionEvent` | Yes (early-return on `!hasReversions`) | No — public `@Transactional(NEVER)` | 2c-i + 2f | Mirror of #7. **Owned by `audit-2f-sweep` worktree.** |
| 9 | `ExternalDatabaseServiceImpl` | `:107` (`updateReleaseDetails`) | `ReleaseDetailsUpdateEvent` | No (end-of-method; `detail` may be null) | No — public `@Transactional` | 2f (5-arg detail+date) | 5-arg form with explicit `lastUpdated` Date. Blocked on payload Phase A AND on per-event-date support in `@Audited`. |
| 10 | `ExternalDatabaseServiceImpl` | `:116` (`updateReleaseLastUpdated`) | `ReleaseDetailsUpdateEvent` | No (end-of-method) | No — public `@Transactional` | 2f (5-arg detail+date) | Same shape as #9; do as a pair. |
| 11 | `SingleCellExpressionExperimentServiceImpl` | `:1067` (`changePreferredCellTypeAssignment(ee, dim, …)`) | `PreferredCellTypeAssignmentChangedEvent` | Yes (early-return `UNCHANGED` at `:1059`) | No — public `@Transactional` | 2c-i | Message is dynamic but parameter-derived (uses `dimension`, `preferredCta`, `newPreferredCta`). Candidate for `@AuditedConditional(when="#result != UNCHANGED", …)` once the method returns the enum result on every path. |
| 12 | `SingleCellExpressionExperimentServiceImpl` | `:1540` (`createCellTypeFactor(ee, ctl, …)`) | `ExperimentalDesignUpdatedEvent` | No (end-of-method on the create branch) | **Yes** — `createCellTypeFactor` at `:1505` is `private` | 2g (private-helper) | Called from multiple sites (`:428`, `:498`, `:1016`, `:1075`, `:1501`). Hoist to package-private on a companion bean. |
| 13 | `SingleCellExpressionExperimentServiceImpl` | `:1567` (`removeCellTypeFactor(ee, ef)`) | `ExperimentalDesignUpdatedEvent` | No (end-of-method) | **Yes** — `removeCellTypeFactor` at `:1563` is `private` | 2g (private-helper) | Two call sites (`:1516`, `:1557`). Same hoist treatment as #12. |
| 14 | `PreboardedExperimentServiceImpl` | `:101` (`create(accession, ...)`) | `PreboardedCreatedEvent` | No (end-of-method, after `persist`+`flush`) | No — public `@Transactional` | 2a (typed-from-args) | Target (`skel`) is freshly created inside the method — `@Audited` needs a returned-`#result` target, but the SpEL infra supports `#result` so this is migratable today. **Clean candidate; should land next.** Note the `//noinspection deprecation` comment, implying the API was already flagged. |
| 15 | `ExpressionExperimentWriteServiceImpl` | `:246` (`updateQuantitationType`) | dynamic `Class<? extends PreferredDataChangedEvent>` | Yes (conditional on `qt.isPreferred(...)` + `eventType != null`) | No — public `@Transactional` | 2c-ii + 2i (dynamic-type) | Event class chosen at runtime from `vectorType`. **Permanently out of scope for `@Audited` unless `valueSpel` lands.** |
| 16 | `ExpressionExperimentWriteServiceImpl` | `:256` (`updateQuantitationType`) | dynamic, same as #15 | Yes (the "cleared" branch) | No — public `@Transactional` | 2c-ii + 2i | Sibling of #15. |
| 17 | `ProcessedExpressionDataVectorServiceImpl` | `:112` (`computeProcessedExpressionData`) | `ProcessedVectorComputationEvent` | No (end-of-method) | No — public `@Transactional` | 2f (4-arg detail) | Companion comment at `:110-:111` already calls out "retained pending the `AuditEventPayload` refactor (bucket 2f). The failure path is now handled by `@AuditedOnError` above." **Explicit Phase A blocker.** **Owned by `audit-2f-sweep` worktree.** |
| 18 | `ProcessedExpressionDataVectorServiceImpl` | `:152` (`reorderByDesign`) | (no event class — 2-arg note-only form) | No (end-of-method) | No — public `@Transactional` | special — no-type 2-arg | `addUpdateEvent(ee, "Reordered the data vectors …")`. The `@Audited` annotation requires a class literal. Skip per `AUDIT_PHASE_C_RECCE.md` line 90. **Owned by `audit-2f-sweep` worktree.** |

## Summary

- **18 genuine callsites** inventoried (down from the 22 raw `grep` hits;
  the four excluded hits are javadoc inside the `*AuditServiceImpl` /
  `PreprocessorHelperServiceImpl` companion-bean headers).
- **In flight (do not touch):** #7, #8, #17, #18 are owned by the
  `audit-2f-sweep` worktree this session.

### Bucket breakdown

| Bucket | Count | Sites |
|---|---|---|
| 2a typed-from-args (clean) | 1 | #14 |
| 2b typed-from-result (clean) | 1 | #3 (with 2g overlap) |
| 2c-i conditional early-return | 3 | #7, #8, #11 |
| 2c-ii multi-branch | 4 | #4, #5, #15, #16 |
| 2d multi-event per method | 2 | #4, #5 (same method) |
| 2e catch-block / Throwable | 0 | (all migrated to `@AuditedOnError`) |
| 2f 4-arg / 5-arg detail | 7 | #1, #2, #6, #7, #8, #9, #10, #17 |
| 2g private-helper | 3 | #3, #12, #13 |
| 2i dynamic-type | 2 | #15, #16 |
| no-type 2-arg (skip) | 1 | #18 |

(Sites appear in multiple buckets — e.g. #7 is 2c-i AND 2f.)

### Blockers

- **`AuditEventPayload` Phase A** blocks the bulk: #1, #2, #6, #7, #8,
  #9, #10, #17 (8 sites) all rely on the `DETAIL` column today.
  Migrating to `@Audited` without the payload landing means losing the
  per-event detail string — coordinate before dropping.
- **`valueSpel` on `@Audited`** would be needed to migrate #15 / #16
  (dynamic event class). No infra in flight; treat as permanent
  imperative residue unless someone commits to the annotation work.
- **Private-helper hoist** required for #3, #12, #13 — mechanical,
  bucket 2g playbook applies, but each touches multiple call sites so
  the hoist commit is non-trivial.
- **Branch extraction** required for #4 / #5 (paired) and #11.

### Cleanest next candidate (zero-blocker)

**#14** (`PreboardedExperimentServiceImpl.create:101`) is the only
inventoried site that:
- emits at end-of-method,
- uses a compile-time literal event class,
- has no `DETAIL` payload to preserve,
- is not currently owned by another worktree,
- has no early-return short-circuit, no multi-branch dispatch, no
  private-helper indirection.

The target is `skel`, which is the local `@return` value of `create`, so
`@Audited(value = PreboardedCreatedEvent.class, messageSpel = "'Preboarded created for accession ' + #accession")` on `create` would
preserve the existing note exactly. Worth landing in isolation as the
canonical "Phase C clean migration" example.

### Sites NOT in this inventory

The five `*AuditServiceImpl` and `PreprocessorHelperServiceImpl`
companion beans contain javadoc-only references to
`auditTrailService.addUpdateEvent` documenting the imperative shape that
each helper replaced. They are not callsites and need no action:

- `DataUpdaterAuditServiceImpl.java` (lines 37, 49)
- `ArrayDesignMergeAuditServiceImpl.java` (lines 34, 42)
- `GeoUpdateAuditServiceImpl.java` (line 31)
- `GeeqAuditServiceImpl.java` (line 34)
- `PreprocessorHelperServiceImpl.java` (line 39)

`DataUpdaterImpl.java:396` is also javadoc-only ("// `@AuditedOnError`
replaces two imperative `auditTrailService.addUpdateEvent(...)` …") and
is the receipt that the DataUpdater catch-block migration already
landed.

`gemma-web` (4 callsites) and `gemma-cli` (12 callsites) are out of
scope for this inventory per the brief.
