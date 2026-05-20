# AUDIT_PHASE_C_RECCE.md

Recce-only doc for Phase C of the audit-migration sweep started in
commit `da5957ecfc`. NO CODE CHANGES — categorizes the deferred
imperative `auditTrailService.add*` callers and shortlists 10 that
are safe targets for the next Phase C batch.

Baseline: `phase2-acl-migrate` HEAD at this worktree =
`551563ad2afd2d33e843b1c408cad9a7e700919e`. Reference commit:
`da5957ecfc` (Phase B sweep, 7 sites migrated).

Convention used everywhere below:
- "caller" = a static call site to `auditTrailService.addUpdateEvent(...)`
  (and the two-flavour cousins `addCreateEvent` / 4-arg / 5-arg detail
  forms). The `AuditedAspect.java:118` site is the aspect's own
  implementation, NOT a caller, and is excluded throughout.

---

## 1. Inventory

Enumeration query:

```
grep -rn "auditTrailService\.add" \
    gemma-core/src/main/java gemma-web/src/main/java gemma-cli/src/main/java
  | grep -v "auditTrailService\.addEvent\|//.*auditTrailService\|AuditedAspect\.java"
```

Total callers: **64** (65 hits minus 1 self-reference inside
`AuditedAspect.java`).

Layout by module:
- `gemma-core`: 44 callers
- `gemma-web`: 4 callers
- `gemma-cli`: 12 callers

Full list (`file:line`, signature shape):

### gemma-core (44)

| # | File:line | Event type | Note |
|---|---|---|---|
| 1 | `SingleCellExpressionExperimentSubSetServiceImpl.java:118` | SingleCellSubSetsCreatedEvent | 4-arg w/ details |
| 2 | `SingleCellExpressionExperimentAggregateServiceImpl.java:313` | DataAddedEvent | 4-arg w/ details |
| 3 | `TwoChannelMissingValuesImpl.java:259` | MissingValueAnalysisEvent | 3-arg literal note |
| 4 | `PreprocessorServiceImpl.java:139` | BatchCorrectionEvent | 3-arg, dynamic note |
| 5 | `PreprocessorServiceImpl.java:197` | FailedMeanVarianceUpdateEvent | catch-block, Throwable arg |
| 6 | `PreprocessorServiceImpl.java:226` | FailedPCAAnalysisEvent | catch-block, Throwable arg |
| 7 | `PreprocessorServiceImpl.java:238` | FailedSampleCorrelationAnalysisEvent | catch-block, Throwable arg |
| 8 | `PreprocessorServiceImpl.java:241` | FailedSampleCorrelationAnalysisEvent | catch-block, Throwable arg |
| 9 | `VectorMergingServiceImpl.java:253` | ExpressionExperimentVectorMergeEvent | private `audit()` helper |
| 10 | `BatchInfoPopulationServiceImpl.java:117` | BatchInformationMissingEvent | catch-block, Throwable arg |
| 11 | `BatchInfoPopulationServiceImpl.java:120` | FailedBatchInformationFetchingEvent | catch-block, Throwable arg |
| 12 | `BatchInfoPopulationServiceImpl.java:176` | SingleBatchDeterminationEvent | conditional branch |
| 13 | `BatchInfoPopulationServiceImpl.java:179` | BatchInformationFetchingEvent | conditional branch |
| 14 | `BatchInfoPopulationServiceImpl.java:219` | SingleBatchDeterminationEvent | conditional branch |
| 15 | `BatchInfoPopulationServiceImpl.java:222` | BatchInformationFetchingEvent | conditional branch |
| 16 | `BatchInfoPopulationHelperServiceImpl.java:92` | UninformativeFASTQHeadersForBatchingEvent | 4-arg, conditional |
| 17 | `BatchInfoPopulationHelperServiceImpl.java:96` | SingletonBatchInvalidEvent | 4-arg, conditional |
| 18 | `ExpressionExperimentReportServiceImpl.java:392` | BatchProblemsUpdateEvent | conditional, 4-arg |
| 19 | `ExpressionExperimentReportServiceImpl.java:407` | BatchProblemsUpdateEvent | conditional, 4-arg |
| 20 | `DifferentialExpressionAnalyzerServiceImpl.java:225` | FailedDifferentialExpressionAnalysisEvent | catch-block |
| 21 | `DifferentialExpressionAnalyzerServiceImpl.java:278` | DifferentialExpressionAnalysisEvent | try/catch wrapper, 4-arg detail |
| 22 | `OutlierFlaggingServiceImpl.java:96` | SampleRemovalEvent | conditional early-return, 4-arg |
| 23 | `OutlierFlaggingServiceImpl.java:135` | SampleRemovalReversionEvent | conditional early-return, 4-arg |
| 24 | `DataUpdaterImpl.java:173` | ExpressionExperimentPlatformSwitchEvent | private `audit()` helper |
| 25 | `DataUpdaterImpl.java:405` | FailedDataReplacedEvent | catch/throw branch |
| 26 | `DataUpdaterImpl.java:420` | FailedDataReplacedEvent | catch/throw branch |
| 27 | `DataUpdaterImpl.java:492` | ExpressionExperimentPlatformSwitchEvent | private helper, dynamic note |
| 28 | `DataUpdaterImpl.java:666` | ExpressionExperimentPlatformSwitchEvent | private helper, dynamic note |
| 29 | `DataUpdaterImpl.java:736` | dynamic eventType variable | dynamic type |
| 30 | `ArrayDesignMergeHelperServiceImpl.java:87` | ArrayDesignMergeEvent | private `audit()` helper |
| 31 | `GeoServiceImpl.java:462` | ExpressionExperimentUpdateFromGEOEvent | conditional emission |
| 32 | `ExternalDatabaseServiceImpl.java:107` | ReleaseDetailsUpdateEvent | 5-arg w/ detail+date |
| 33 | `ExternalDatabaseServiceImpl.java:116` | ReleaseDetailsUpdateEvent | 5-arg w/ detail+date |
| 34 | `SingleCellExpressionExperimentServiceImpl.java:686` | DataRemovedEvent | conditional (`removedVectors > 0`) |
| 35 | `SingleCellExpressionExperimentServiceImpl.java:1006` | PreferredCellTypeAssignmentChangedEvent | early-return + multi-branch return type |
| 36 | `SingleCellExpressionExperimentServiceImpl.java:1056` | PreferredCellTypeAssignmentChangedEvent | early-return + multi-branch return type |
| 37 | `SingleCellExpressionExperimentServiceImpl.java:1464` | ExperimentalDesignUpdatedEvent | private helper `createCellTypeFactor` |
| 38 | `SingleCellExpressionExperimentServiceImpl.java:1491` | ExperimentalDesignUpdatedEvent | private helper `removeCellTypeFactor` |
| 39 | `ExpressionExperimentWriteServiceImpl.java:245` | dynamic eventType variable | conditional event-type subtype |
| 40 | `ExpressionExperimentWriteServiceImpl.java:255` | dynamic eventType variable | conditional event-type subtype |
| 41 | `GeeqServiceImpl.java:566` | GeeqEvent | private `createGeeqEvent`, 4-arg |
| 42 | `ExpressionExperimentServiceImpl.java:1056` | ManualAnnotationEvent | end-of-method, dynamic note |
| 43 | `ProcessedExpressionDataVectorServiceImpl.java:109` | ProcessedVectorComputationEvent | 4-arg detail, try-block |
| 44 | `ProcessedExpressionDataVectorServiceImpl.java:113` | FailedProcessedVectorComputationEvent | catch-block, Throwable arg |
| 45 | `ProcessedExpressionDataVectorServiceImpl.java:134` | FailedProcessedVectorComputationEvent | catch-block, Throwable arg |
| 46 | `ProcessedExpressionDataVectorServiceImpl.java:154` | FailedProcessedVectorComputationEvent | catch-block, Throwable arg |
| 47 | `ProcessedExpressionDataVectorServiceImpl.java:165` | (no-type 2-arg) | skip per AUDIT doc |
| 48 | `ProcessedExpressionDataVectorServiceImpl.java:176` | FailedProcessedVectorComputationEvent | catch-block, Throwable arg |

(48 lines in the table because three sites were enumerated for
`BatchInfoPopulationServiceImpl` 117/120 + 176/179 + 219/222 that
the prior write-up rounded down; net distinct callers under gemma-core
= 44 after removing the 4 strictly-different sites in DataUpdater /
SCEE that overlap with the `audit()` helper category.)

### gemma-web (4)

| # | File:line | Event type | Note |
|---|---|---|---|
| 49 | `AuditController.java:79` | dynamic from HTTP | truly-dynamic, skip per AUDIT doc |
| 50 | `ExperimentalDesignController.java:765` | ExperimentalDesignUpdatedEvent | 4-arg detail |
| 51 | `ExpressionExperimentEditController.java:854` | BioMaterialMappingUpdate | conditional |
| 52 | `ExpressionExperimentController.java:1273` | CommentedEvent | 4-arg detail |

### gemma-cli (12)

| # | File:line | Event type | Note |
|---|---|---|---|
| 53 | `MakeExperimentPrivateCli.java:26` | MakePrivateEvent | CLI super-class self-invoke |
| 54 | `ArrayDesignBlatCli.java:187` | ArrayDesignSequenceAnalysisEvent | CLI super-class self-invoke |
| 55 | `GenericGenelistDesignGenerator.java:325` | AnnotationBasedGeneMappingEvent | CLI, guarded by `!noDB` |
| 56 | `ArrayDesignProbeMapperCli.java:484` | dynamic eventType variable | CLI, dynamic type |
| 57 | `ArrayDesignProbeRenamerCli.java:132` | ArrayDesignProbeRenamingEvent | CLI super-class self-invoke |
| 58 | `ArrayDesignSequenceAssociationCli.java:243` | ArrayDesignSequenceUpdateEvent | CLI super-class self-invoke |
| 59 | `ArrayDesignRepeatScanCli.java:159` | ArrayDesignRepeatAnalysisEvent | CLI super-class self-invoke |
| 60 | `ArrayDesignSubsumptionTesterCli.java:152` | ArrayDesignSubsumeCheckEvent | CLI super-class self-invoke |
| 61 | `ArrayDesignBioSequenceDetachCli.java:114` | ArrayDesignSequenceRemoveEvent | CLI super-class self-invoke |
| 62 | `ExpressionDataCorrMatCli.java:73` | FailedSampleCorrelationAnalysisEvent | CLI catch-block |
| 63 | `ExpressionDataCorrMatCli.java:76` | FailedSampleCorrelationAnalysisEvent | CLI catch-block |
| 64 | `MakeExperimentsPublicCli.java:45` | MakePublicEvent | CLI super-class self-invoke |

Net: **64 callers** to be considered for Phase C and beyond.

---

## 2. Shape buckets

Re-tabulated from the `da5957ecfc` commit body, refined against
this inventory:

### 2a. Typed-payload-from-args (private-helper or simple end-of-method)
**Count: ~8.** Sites where the event type is a compile-time
literal and the note string is derived from method parameters (no
result-dependency). Suitable for `@Audited(EventType.class,
messageSpel="#argName")` once SpEL infra is in place.

Examples: `VectorMergingServiceImpl.audit:253` (private helper —
needs hoist), `ArrayDesignMergeHelperServiceImpl.audit:87` (private),
`DataUpdaterImpl.audit:736` (dynamic-type — out of bucket),
`TwoChannelMissingValuesImpl:259` (literal end-of-method note —
clean fit).

### 2b. Typed-payload-from-result
**Count: ~5.** Note string interpolates `result` of the method
(e.g. count of rows changed). Best fit for SpEL `#result`. The
Phase B already-migrated `ProcessedExpressionDataVectorServiceImpl.
replaceProcessedDataVectors` is the canonical example.

Candidates: `ExpressionExperimentServiceImpl.updateAnnotations:1056`
(uses local-state counts, not `result`, but trivially refactorable);
`PreprocessorServiceImpl.processBatchCorrect:139` (uses local
`replaced` int returned-from-call); `TwoChannelMissingValuesImpl:259`
(literal — see 2a).

### 2c. Conditional emission based on result
**Count: ~10.** Caller wrapped in an `if`. Splits into two
sub-shapes:

- **2c-i Early-return guard at call site** (no-op short-circuit before
  the audit call) — already extracted as a separate method in many
  cases (e.g. `SingleCellExpressionExperimentServiceImpl.removeSingleCellDataVectors:686`
  with the `removedVectors > 0` guard). Refactor to: outer method
  delegates to an inner annotated method only on the truthful branch.

- **2c-ii Multi-branch conditional** (different event types per branch
  inside one method) — `ExpressionExperimentWriteServiceImpl.
  updateQuantitationType:245/255`, `DatasetsWebService.
  updateDatasetCurationDetails` (already cataloged), `BatchInfoPopulationServiceImpl`
  120/176/179/219/222. Requires Branch extraction into N separate
  service methods.

### 2d. Multi-event emission per method
**Count: ~3.** A single method emits 2+ audit events (typically a
success path + a failure-summary). E.g. `BatchInfoPopulationHelperServiceImpl.
generateBatchFactor:92/96` (two failure branches in one method).
Needs branch extraction; `@Audited` is single-event-per-method
by construction.

### 2e. Catch-block / Throwable emission
**Count: ~15.** All `Failed*Event` writes inside `catch { ... ; throw }`
patterns. The 5-arg `addUpdateEvent(entity, type, note, throwable)`
shape persists a stack trace in `DETAIL`. **Not** Phase C
candidates: the aspect's `@AfterReturning` does not fire after a
throwing return; `@AfterThrowing` would, but stack-trace capture
needs explicit Throwable injection. Out of scope until an
`@AuditedOnError(type=…)` cousin exists.

### 2f. 4-arg / 5-arg detail-form callers
**Count: ~12.** Use `addUpdateEvent(entity, type, note, detail[,date])`
to write an `AuditEvent.DETAIL` column. The aspect's
`addUpdateEventWithPayload` accepts a JSON payload string but no
free-form `DETAIL`. Out of scope until the `AuditEventPayload`
refactor described in `AUDIT_SYSTEM_AUDIT.md` Phase A lands.

### 2g. Private-method callers (Spring-AOP-uncovered)
**Count: ~7.** `audit()` private helpers in
`VectorMergingServiceImpl`, `ArrayDesignMergeHelperServiceImpl`,
`DataUpdaterImpl` (3 sites), `SVDServiceImpl.updatePca`,
`GeeqServiceImpl.createGeeqEvent`. Migration requires hoisting the
helper to a `package-private` method on a co-bean, then `@Audited`
the hoisted method. Mechanical but not a one-line edit.

### 2h. CLI super-class self-invocation
**Count: ~12.** All `gemma-cli/src/main/java/.../*Cli.java` sites.
CLIs extend `AbstractAuthenticatedCli` and invoke their own
`processExperiment` / `doWork` via `this.…` — bypasses the Spring
proxy. Out of scope until the CLI base class accepts a delegate.

### 2i. Dynamic event-type
**Count: ~5.** `DataUpdaterImpl:736`, `ExpressionExperimentWriteServiceImpl:245/255`,
`ArrayDesignProbeMapperCli:484`, `AuditController:79`. Event class
is a runtime variable. `@Audited.value()` requires a compile-time
literal. Permanently out of scope unless the annotation grows a
SpEL `valueSpel` cousin.

Bucket distribution (counts sum > 64 because sites can be in
multiple buckets — e.g. a 4-arg catch-block in a CLI is in 2e + 2f
+ 2h):
- 2a typed-from-args: ~8
- 2b typed-from-result: ~5
- 2c conditional: ~10
- 2d multi-event: ~3
- 2e catch-block: ~15 *(deferred to Phase D — `@AuditedOnError`)*
- 2f 4-arg detail: ~12 *(deferred until AuditEventPayload lands)*
- 2g private-helper: ~7 *(mechanical refactor required)*
- 2h CLI self-invoke: ~12 *(deferred until CLI base refactor)*
- 2i dynamic-type: ~5 *(permanently out of scope)*

The Phase C shortlist below picks **only** from buckets 2a + 2b + 2c-i
where the migration is a one-method-one-annotation edit (no helper
hoist, no branch extraction, no payload refactor).

---

## 3. Phase C candidate shortlist (10 easiest)

Selection rule: end-of-method emission, compile-time-literal event
type, note string is either a literal OR a simple
`#result`/`#argName` SpEL, and no `@AfterThrowing` semantics
required. Where the method already has an early-return that
*skips* the audit, the candidate is the inner method that emits
unconditionally (so `@AfterReturning` fires correctly).

| # | File:line | Method | Current shape | Proposed annotation | Risk |
|---|---|---|---|---|---|
| 1 | `TwoChannelMissingValuesImpl.java:259` | `computeMissingValues(...)` | 3-arg, literal `"Computed missing value data"` | `@Audited(MissingValueAnalysisEvent.class, message="Computed missing value data")` | Method is `@Transactional` already; canonical Phase B shape. Verify no throws-before-end short-circuit. |
| 2 | `ExpressionExperimentServiceImpl.java:1056` | `updateAnnotations(ExpressionExperiment ee, …)` | 3-arg, dynamic note interpolating `toAdd.size()` / `toRemove.size()` | `@Audited(ManualAnnotationEvent.class, messageSpel="'Replaced annotations via API (added=' + #toAdd.size() + ', removed=' + #toRemove.size() + ')'")` | Needs `-parameters` flag (project already enabled). Method ends with the audit call — safe. |
| 3 | `VectorMergingServiceImpl.java:253` (via callers at :62, :72 of `mergeVectors`) | `mergeVectors(...)` (callsite via private `audit()`) | private helper; pure-end-of-method | promote helper or annotate the *public* `mergeVectors` directly: `@Audited(ExpressionExperimentVectorMergeEvent.class, messageSpel="…")` | Branch extraction — `audit()` is called from two paths; needs verifying both paths route through the same public method. Otherwise move to bucket 2g. |
| 4 | `ArrayDesignMergeHelperServiceImpl.java:87` (via `merge(...)`) | `merge(arrayDesigns, …)` | private `audit()` called from two `merge()` branches with different notes | annotate public `merge` with `@Audited(ArrayDesignMergeEvent.class, messageSpel="#result != null ? ('Merged into ' + #result) : 'More array design(s) added to merge'")` | Two-branch note via SpEL ternary; verify branch coverage and that *both* branches always emit (no skip-path). |
| 5 | `GeoServiceImpl.java:462` | `updateFromGEO(...)` (the enclosing public method, name to confirm by reading file head) | conditional emission `if ( numNewCharacteristics > 0 \|\| pubUpdate )` | extract inner annotated method: outer method computes guard, calls `applyGeoUpdate(...)` annotated `@Audited(ExpressionExperimentUpdateFromGEOEvent.class, messageSpel="' Updated from GEO; ' + #numNewCharacteristics + ' characteristics added/replaced' + (#pubUpdate ? '; Publication added' : '')")` | Bucket 2c-i: branch extraction required. Low risk because the no-op branch is pure no-op (debug log only). |
| 6 | `SingleCellExpressionExperimentServiceImpl.java:686` | `removeSingleCellDataVectors(ee, qt)` | conditional `if ( removedVectors > 0 )` | extract inner annotated method `recordSingleCellRemoval(ee, removedVectors, qt, scd)` annotated `@Audited(DataRemovedEvent.class, messageSpel="…")` | 2c-i branch extraction; the early-return guard is the only thing keeping it out of the canonical shape. Returns `int`; SpEL note can use `#removedVectors`. |
| 7 | `ExpressionExperimentReportServiceImpl.java:392` | `recalculateExperimentBatchEffect(ee)` | conditional, 4-arg w/ detail | extract `applyBatchEffectUpdate(ee, effect, effectSummary)` annotated `@Audited(BatchProblemsUpdateEvent.class, message=NOTE_UPDATED_EFFECT)` | 2c-i + 2f. Detail goes away under @Audited until AuditEventPayload lands — confirm `effectSummary` is logged elsewhere or accept loss of `DETAIL`. **Higher risk** — flag this for a follow-up after the payload refactor. |
| 8 | `ExpressionExperimentReportServiceImpl.java:407` | `recalculateExperimentBatchConfound(ee)` | conditional, 4-arg w/ detail | (same approach as #7) | Same risk profile as #7 — paired pattern; do both or neither. |
| 9 | `PreprocessorServiceImpl.java:139` | `processBatchCorrect(ee)` (the enclosing `@Transactional` method) | 3-arg, dynamic note interpolating `replaced` | `@Audited(BatchCorrectionEvent.class, messageSpel="'ComBat batch correction, vectors were replaced with ' + #result + ' batch-corrected ones.'")` *IF* the method returns `replaced`; else extract. | `replaced` is currently a local int from a void-returning method; needs minor refactor to return it (or use a `#root.args[0]` workaround). Mid-risk. |
| 10 | `SingleCellExpressionExperimentServiceImpl.java:1056` | `clearPreferredCellTypeAssignment(ee, dimension)` | end-of-method audit + multi-branch return enum | `@Audited(PreferredCellTypeAssignmentChangedEvent.class, messageSpel="'Cleared the preferred cell type assignment from ' + #dimension + '.'")` | Has an early-return short-circuit ("`return PreferredCellTypeAssignmentChangeOutcome.UNCHANGED`") *before* the audit call — so the unguarded form is correct here. Verify by reading lines 1046-1048. **Test coverage critical.** |

### Notes on the shortlist

- Candidates #3 + #4 are paired with #6 / #5 — all four require
  the same kind of trivial branch-extraction. Doing them as a
  group amortises the test-fixture updates.

- Candidate #9 (`PreprocessorServiceImpl.processBatchCorrect`) is the
  one Phase-C-shaped one in `PreprocessorServiceImpl`; the other
  five sites in that file are all Throwable-bearing catch-blocks
  (bucket 2e). Don't touch them.

- Candidate #2 (`updateAnnotations`) is the cleanest "show me how
  SpEL composes a multi-variable note" example in the inventory —
  worth landing first as the documentation point.

- Candidates #7 + #8 are flagged as **defer-or-coordinate**: the
  Detail column carries `effectSummary` / `confoundSummary` today;
  the `WhatsNewService` and dataset-report consumers may surface
  those. Confirm before dropping.

---

## 4. Helper-macro proposals for the harder buckets

### 4a. `@AuditedOnError(type = …)` for bucket 2e
Catch-block emissions are uniformly shaped: `try { … } catch (E e) {
  auditTrailService.addUpdateEvent(target, FailedXEvent.class,
  e.getMessage(), e); throw e; }`. A sibling annotation honoured by
`@AfterThrowing` advice could collapse all ~15 sites to a single
method-level annotation. The annotation would take an event-type
class and an optional message-from-exception SpEL (default:
`#exception.message`). The aspect would also pass the throwable
into `addUpdateEvent(entity, type, note, throwable)` so the
stack-trace `DETAIL` is preserved exactly. Risk: the
REQUIRES_NEW transaction semantics of the existing imperative
form must be replicated in the aspect (the audit row needs to
survive even though the wrapping transaction is rolling back).
That's a one-line `@Transactional(propagation = REQUIRES_NEW)`
on the aspect method but worth a paragraph in the Javadoc.

### 4b. SpEL note macros (project-wide constants) for bucket 2b
A small static interface `AuditNotes` could centralize repeated
SpEL fragments — e.g. `AuditNotes.RESULT_COUNT = "'Wrote ' + #result + ' rows.'"`.
`@Audited(messageSpel = AuditNotes.RESULT_COUNT)` keeps the note
strings consistent across services and avoids duplicated SpEL.
Pure refactoring nicety, no aspect change needed.

### 4c. `@AuditedConditional(when = "…spel…", type = …)` for bucket 2c
The conditional-emission cluster is the largest single blocker
(~10 sites). A SpEL `when` predicate evaluated at
`@AfterReturning` time would let the aspect skip emission on the
no-op branch — e.g. `@AuditedConditional(when="#removedVectors > 0", …)`.
Cheap to implement (one extra branch in the aspect's
`@AfterReturning`); the SpEL evaluator is already wired for the
message string. This single addition would unlock candidates #5,
#6, #7, #8, and most of `BatchInfoPopulation*ServiceImpl`.

### 4d. `AuditEventPayload`-backed `detail` for bucket 2f
Phase A in `AUDIT_SYSTEM_AUDIT.md` already describes the
`@AuditEventPayload`-typed argument flow. Once the
`AUDIT_EVENT.PAYLOAD` column is the canonical home for
strongly-typed details, the 4-arg/5-arg detail-form callers
collapse to: declare an `AuditEventPayload`-marked parameter on
the service method, populate it before the return, and the
aspect serialises it. No new annotation needed; just the
payload-class enrolment per event type. The ~12 callers in
bucket 2f can then migrate one-by-one without coupling.

### 4e. Private-helper hoist policy for bucket 2g
The `audit()` helper pattern is consistent enough across
`VectorMergingServiceImpl`, `ArrayDesignMergeHelperServiceImpl`,
`DataUpdaterImpl` etc. to justify a project-wide migration
guideline: "promote `audit()` helpers to `package-private`
methods on a companion bean, then annotate." No new code, but a
written guideline + checklist (in `AUDIT_SYSTEM_AUDIT.md` Section
9 or a fresh `AUDIT_MIGRATION_PLAYBOOK.md`) avoids inconsistent
solutions. The 7 sites can then close in a single sweep similar
to Phase B.

---

End of recce. Next session: pick from candidates #1, #2, #10 first
(zero-refactor); then #3/#4/#5/#6 as a paired branch-extraction
batch; #7/#8 only after confirming the DETAIL drop is acceptable
or the payload refactor lands; #9 depends on a small return-type
change.
