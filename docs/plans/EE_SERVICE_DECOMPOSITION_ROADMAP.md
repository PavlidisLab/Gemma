# `ExpressionExperimentService` Decomposition Roadmap

**Status:** Phase 3 architecture-debt recce — planning document, no code changes.
**Baseline:** `phase2-acl-migrate` @ `08e760bdaf`.
**Target file:** `gemma-core/src/main/java/ubic/gemma/persistence/service/expression/experiment/ExpressionExperimentServiceImpl.java` (2 073 LOC, 168 `public` methods on the impl, ~160 declared on the interface).

---

## 1. Why decompose

`ExpressionExperimentServiceImpl` (`EESI`) is the single largest service class in `gemma-core` and the load-bearing entry point for ~629 call sites across 121 caller files in 4 modules. Today it conflates **at least six** distinct responsibilities (retrieval, mutation, vector I/O, subset/dimension lookup, lifecycle / `remove`, taxonomy/metadata reporting), wires **18 collaborators** via field `@Autowired`, and participates in **2 confirmed dependency cycles**.

Concrete pain points blocking the Spring 6+ modernization:

1. **Constructor injection is impossible today** without a 19-arg constructor. The one true constructor takes only the DAO; everything else is field-injected. This is a direct obstacle to the broader phase-2/3 move to constructor injection + `final` fields + Spring's preferred wiring style. Splitting the class lets each sub-service take a sane constructor (≤ 7 deps).
2. **Cyclic dependencies** force Spring to use proxy-late binding. `ExpressionExperimentSetService` and `SampleCoexpressionAnalysisService` both autowire `ExpressionExperimentService` *and* are autowired by it. With constructor injection these cycles fail-fast (which is the right behavior but breaks the world). A decomposition where the cycle endpoints (`getTaxon`, `thawLite`) live in a read-only sub-service breaks both cycles cleanly — `EESetService` and `SampleCoex...Service` would depend only on the read service, never the write/lifecycle one.
3. **Test scaffolding bloat.** 90 test files reference `ExpressionExperimentService`; mocking it requires stubbing dozens of unrelated methods. Specialty interfaces let tests mock only what they need.
4. **AOP / `@Secured` clarity.** With ~50 distinct `@Secured` attribute sets on the interface, splitting along ACL semantics (anonymous-readable vs `GROUP_USER`-only writes vs `GROUP_ADMIN`-only) makes the security envelope self-documenting and reviewable per file.
5. **Transactional boundary clarity.** 141 of 159 transactional methods are `readOnly=true`; the 18 writers are spread thinly. A read-service / write-service split makes the transactional contract obvious at the type level.

---

## 2. Current state

| Metric | Value |
|---|---|
| File size | 2 073 LOC |
| `public` methods (impl) | 168 |
| Methods declared on interface | ~160 |
| `@Autowired` collaborators | 18 (1 ctor-injected DAO + 17 field-injected services) |
| `@Transactional(readOnly=true)` methods | 141 |
| `@Transactional` (write) methods | 18 |
| Distinct caller files | 121 (`gemma-core` 79, `gemma-web` 29, `gemma-rest` 9, `gemma-cli` 4) |
| Total call sites | 629 |
| Confirmed dependency cycles | 2 (`ExpressionExperimentSetServiceImpl`, `SampleCoexpressionAnalysisServiceImpl`) |
| `this.<other-method>(...)` self-invocations | 1 (`remove` → `getSubSetsWithBioAssays`) |

### Responsibility table (method counts)

| Responsibility group | Method count | Tx flavour | Notes |
|---|---:|---|---|
| **A. Pure retrieval (load / find / exists)** | ~58 | mostly `readOnly` | `loadReference`, `loadWithAuditTrail`, `findByAccession`, `findByBioMaterial`, `findByShortName*`, `existsByShortName`, `loadAndThaw*`, `loadValueObjects*`, `loadDetailsValueObjects*`, etc. ACLs: `IS_AUTHENTICATED_ANONYMOUSLY` + `AFTER_ACL_*`. |
| **B. Counts / reporting / VOs** | ~24 | `readOnly` | `getAnnotationCountsByIds`, `getBioMaterialCount`, `getPerTaxonCount`, `getPopulatedFactorCounts*`, `getLastArrayDesignUpdate`, `getLastLinkAnalysis`, `getLastMissingValueAnalysis`, `getLastProcessedDataUpdate`, `getSampleRemovalEvents`, `loadAllIdentifiersAndName`, `getExperimentalDesignValueObject`. Includes audit-event aggregations. |
| **C. Subset / dimension / QT lookup** | ~30 | `readOnly` | `getSubSets*` family (10 overloads), `getBioAssayDimension*` family (8 overloads), `getQuantitationType*` family (7 overloads), `getArrayDesignsUsed` (3 overloads). These are all "structural read" — never writes. |
| **D. Vector I/O (raw + processed)** | ~14 | mixed (5 read, 9 write) | `getRawDataVectors`, `getPreferredRawDataVectors`, `getMissingValuesVectors`, `addRawDataVectors`, `replaceRawDataVectors`, `replaceAllRawDataVectors`, `removeAllRawDataVectors`, `removeRawDataVectors` (2 overloads), `getProcessedDataVectors` (2), `createProcessedDataVectors`, `replaceProcessedDataVectors`, `removeProcessedDataVectors`. ACL: `GROUP_USER` + `ACL_SECURABLE_EDIT` on writes. |
| **E. Design mutation (factors / chars / tags)** | ~7 | write | `addFactor`, `addFactorValue`, `addFactorValues`, `addCharacteristic`, `removeCharacteristics`, `updateQuantitationType`, `updateMeanVarianceRelation`. |
| **F. Lifecycle (remove)** | 2 | write | `remove(EE)`, `remove(Collection<EE>)`. Both heavy — `remove(EE)` orchestrates DEA / sample-coex / PCA / set / subset cleanup. |
| **G. Filter / search infrastructure** | ~8 | `readOnly` | `filter`, `filterByTaxon`, `getEnhancedFilters`, `getFilterablePropertyDescription`, `getFilterablePropertyConfigAttributes`, `loadIdsWithCache`, `countWithCache`, `loadValueObjectsWithCache`. |
| **H. Annotation / characteristic VO** | ~6 | `readOnly` | `getAnnotations` (2 overloads), `getCategoriesUsageFrequency`, `getAnnotationsUsageFrequency`, `getTechnologyTypeUsageFrequency`, `getArrayDesignUsedOrOriginalPlatformUsageFrequency`, `getTaxaUsageFrequency`. |
| **I. Predicates / taxonomy** | ~7 | `readOnly` | `isSingleCell`, `isRNASeq`, `isTwoChannel`, `isTroubled`, `isSuitableForDEA`, `isBlackListed`, `hasProcessedExpressionData`, `getTaxon`, `getTaxa`. |
| **J. Thaw helpers** | 3 | `readOnly` | `thaw`, `thawLite`, `thawLiter`. |

**Note:** there is **no** curation / GEEQ writing on this class today — curation is already in `ExpressionExperimentDetailsValueObject` populators and a separate `CuratableDao`. The hypothetical "curation service" suggested in the recce prompt is *not* a real candidate. Likewise there is **no** "analysis dispatch" code here — async pipeline kick-off lives in `*PreprocessorService` / `*AnalysisService` classes that *consume* `EE`. Strike those from the target list.

---

## 3. Caller analysis — top hot methods

Counts are by call-site occurrences across the whole repo (`grep -rhoE 'expressionExperimentService\.[a-zA-Z_]+'`), not distinct caller files. The top of the distribution is dominated by retrieval / thaw / load:

| Rank | Method | Call sites | Group |
|---:|---|---:|---|
| 1 | `thawLite` | 52 | J |
| 2 | `load` (inherited from base) | 44 | A |
| 3 | `thaw` | 39 | J |
| 4 | `remove` | 36 | F |
| 5 | `update` (inherited) | 32 | F/E |
| 6 | `loadAndThawLiteOrFail` | 30 | A |
| 7 | `loadOrFail` (inherited) | 28 | A |
| 8 | `getFilter*` (filter helpers) | 19 | G |
| 9 | `getArrayDesignsUsed` | 14 | C |
| 10 | `loadValueObjects` | 11 | A/B |

**Hot-method takeaway:** the load + thaw + find APIs (groups A, J) account for the overwhelming majority of call sites. If the decomposition keeps these on a single thin `ExpressionExperimentReadService`, ~85% of caller files only need to migrate *one import*.

Long tail: ~50 methods have only 1–2 call sites. Several are candidates for **inlining** rather than re-homing — e.g. `findIdByMeanVarianceRelation`, `findIdByFactor`, `findByQuantitationType`, `loadWithMeanVarianceRelation`, `findByShortNameWithPrimaryPublication` — but that is a separate cleanup pass, not part of the decomposition.

---

## 4. Dependency tangle

### 4.1 Autowired collaborators (18 total)

```
ExpressionExperimentDao              (ctor — the only one)
AuditEventService
AuditTrailService
BioAssayDimensionService
BioMaterialService
BlacklistedEntityService
CharacteristicService
DifferentialExpressionAnalysisService
ExperimentalFactorService
ExpressionExperimentFilterRewriteHelperService
ExpressionExperimentSetService          *** CYCLE ***
ExpressionExperimentSubSetService
FactorValueService
OntologyService
PrincipalComponentAnalysisService
QuantitationTypeService
SampleCoexpressionAnalysisService       *** CYCLE ***
SearchService
SecurityService
```

18 dependencies is roughly 3× the conventional "code smell" threshold of 6–7. The pure-read group alone (A+B+C+G+H+I+J ≈ 140 methods) only actually uses 10 of these collaborators (`expressionExperimentDao`, `auditEventService`, `auditTrailService`, `bioAssayDimensionService`, `characteristicService`, `expressionExperimentSubSetService`, `ontologyService`, `quantitationTypeService`, `searchService`, `filterRewriteService`); the write-heavy groups (D+E+F) need the rest.

### 4.2 Cyclic dependencies (confirmed)

| Direction A → B | Direction B → A | Methods called |
|---|---|---|
| `EESI` → `ExpressionExperimentSetService` | `EESetServiceImpl` → `EESI` | B→A uses `expressionExperimentService.getTaxon(ee)` only |
| `EESI` → `SampleCoexpressionAnalysisService` | `SampleCoexAnalysisServiceImpl` → `EESI` | B→A uses `expressionExperimentService.thawLite(ee)` (×2) only |

**Both cycles resolve naturally under the decomposition**: `getTaxon` and `thawLite` are both pure-read methods that belong on `ExpressionExperimentReadService`. If `EESetService` and `SampleCoexAnalysisService` depend only on the read service, and the (heavier) lifecycle / write services depend on those collaborators, the cycle is broken: read service has no back-edge.

### 4.3 Self-invocation

Only **one** intra-class call that crosses prospective service boundaries:

```java
// remove(ExpressionExperiment ee) at line 1916
Collection<ExpressionExperimentSubSet> subsets = this.getSubSetsWithBioAssays( ee );
```

In a decomposed world, `remove(...)` (in `LifecycleService`) would inject and call `ReadService.getSubSetsWithBioAssays(...)`. Since `remove(...)` is the proxy-entrypoint, this is correct — the call goes through Spring's proxy and `@Transactional` boundaries compose properly. **Low risk.**

### 4.4 XML wiring

Only one production XML still references the bean:

- `gemma-core/src/main/resources/ubic/gemma/applicationContext-schedule.xml:48` — `<entry key="expressionExperimentService" value-ref="expressionExperimentService"/>`

This is the Quartz job factory map. If the facade is retained (Option A below), this needs **no change**. If the facade is dropped, this entry must be split or removed.

---

## 5. Target architecture

Recommend a **3-service split + retained thin facade**:

### 5.1 `ExpressionExperimentReadService` (interface + impl)

Houses groups **A + B + C + G + H + I + J** — all read-only operations.

- ~140 methods.
- All `@Transactional(readOnly = true)`.
- ACL annotations: `IS_AUTHENTICATED_ANONYMOUSLY` + `AFTER_ACL_*` or `ACL_SECURABLE_READ` throughout.
- Collaborators: `ExpressionExperimentDao`, `AuditEventService`, `BioAssayDimensionService`, `CharacteristicService`, `ExpressionExperimentSubSetService`, `OntologyService`, `QuantitationTypeService`, `SearchService`, `ExpressionExperimentFilterRewriteHelperService`, `Thaws` (utility, not a bean) — **9 collaborators**, all already non-cyclic with respect to this service.
- **This is the cycle-breaker.** `ExpressionExperimentSetService` and `SampleCoexpressionAnalysisService` migrate to depend on this service instead of the old monolith.

### 5.2 `ExpressionExperimentVectorService` (interface + impl)

Houses group **D** — raw + processed vector I/O.

- ~14 methods.
- Mixed read/write transactional.
- ACL: `ACL_SECURABLE_READ` on reads, `GROUP_USER` + `ACL_SECURABLE_EDIT` on writes.
- Collaborators: `ExpressionExperimentDao`, `QuantitationTypeService`, `AuditTrailService` — **3 collaborators**.

Optionally folded into `LifecycleService` if we want only 3 services total — vectors are conceptually "write to experiment" but they're high-volume enough (data-loader CLI, replace-vectors workflows) that keeping them separate is cleaner.

### 5.3 `ExpressionExperimentLifecycleService` (interface + impl)

Houses groups **E + F** — design mutations + delete.

- ~9 methods.
- All `@Transactional` (write).
- ACL: `GROUP_USER` + `ACL_SECURABLE_EDIT`.
- Collaborators: `ExpressionExperimentDao`, `ExperimentalFactorService`, `FactorValueService`, `CharacteristicService`, `QuantitationTypeService`, `AuditTrailService`, `SecurityService`, `DifferentialExpressionAnalysisService`, `SampleCoexpressionAnalysisService`, `PrincipalComponentAnalysisService`, `ExpressionExperimentSetService`, `ExpressionExperimentSubSetService`, and `ExpressionExperimentReadService` (for the one `this.getSubSetsWithBioAssays` call in `remove`) — **13 collaborators**.

This service is still wide but it's *honestly* wide — `remove` genuinely needs all those services to cascade-delete. The wiring is finally legible.

### 5.4 Facade — `ExpressionExperimentService` (RETAIN for backward compat)

Keeps the existing interface and bean name. Its impl becomes a ~150-line composite that delegates each method to one of the three sub-services. This means:

- **All 629 call sites and 90 test files continue to compile and pass unchanged.**
- The XML wiring in `applicationContext-schedule.xml` continues to work.
- Migration can proceed callsite-by-callsite in subsequent passes ("hot" callers migrate to specialty interfaces; cold callers stay on the facade).
- Eventually the facade can be deprecated and removed, but that is **out of scope for the Spring 6+ migration**.

---

## 6. Migration approach

### Option A — Strangler fig with retained facade (RECOMMENDED)

Phases:

1. **Extract `ExpressionExperimentReadService`** as a new interface + impl. Move the ~140 read methods bodily. `ExpressionExperimentServiceImpl` autowires the new read service and delegates each read method (one-line forwarding bodies). Tests + callers unchanged. **Single PR.**
2. **Cycle-break:** migrate `ExpressionExperimentSetServiceImpl` and `SampleCoexpressionAnalysisServiceImpl` to autowire `ExpressionExperimentReadService` instead of `ExpressionExperimentService`. Now `EESI`'s autowire of those two services is non-cyclic. **Single small PR.**
3. **Extract `ExpressionExperimentVectorService`** the same way. **Single PR.**
4. **Extract `ExpressionExperimentLifecycleService`** the same way. **Single PR.**
5. **(Optional, much later) Migrate hot callers** off the facade onto the specialty interfaces. Per-module PRs.
6. **(Optional, much later) Delete the facade** once it has zero remaining direct callers.

Pros:
- Each step is small, reversible, and reviewable.
- Tests stay green at every step (delegation preserves behavior).
- The Spring 6+ modernization (constructor injection on the *new* services) lands in steps 1, 3, 4 — exactly where we want it.
- Cycle is broken in step 2, unblocking constructor injection on the facade in step 4.

Cons:
- Facade impl carries 168 delegation methods — visually ugly, but mechanically trivial. Lombok `@Delegate` could shrink this to ~20 LOC per sub-service if we accept the magic.
- Net LOC goes UP before it goes down (until the facade is retired).

### Option B — Direct split, no facade

Rename `ExpressionExperimentService` out of existence in one PR; rewrite every caller to import the new specialty interfaces.

Pros: ends up cleaner, faster.
Cons: 121 caller files in 4 modules + 90 test files in a single diff. Merge conflicts during the rest of phase-3 would be unmanageable. **Not recommended.**

### Option C — Hybrid

Extract `ReadService` only (step 1 of Option A), leaving D+E+F on the old class. Stop there.

This gets us 80% of the cycle / wiring benefit for 20% of the work. **Plausible compromise** if phase-3 budget is tight — the remaining 18-method monolith is no longer cyclic and the bulk of the read surface lives on a cleanly-constructed service.

---

## 7. Risks

### 7.1 Spring AOP / `@Transactional` self-invocation

Only one cross-group `this.` call exists (`remove` → `getSubSetsWithBioAssays`). After extraction, the call becomes `readService.getSubSetsWithBioAssays(...)` going through Spring's proxy, which correctly applies `@Transactional(readOnly=true)` semantics nested inside the outer `@Transactional` write transaction. **Behaviorally equivalent** — but worth a unit test that exercises `remove(...)` end-to-end. **Low risk.**

### 7.2 ACL `@Secured` annotations

The interface uses ~7 distinct `@Secured` attribute sets (`IS_AUTHENTICATED_ANONYMOUSLY`+`AFTER_ACL_READ`, `IS_AUTHENTICATED_ANONYMOUSLY`+`AFTER_ACL_READ_QUIET`, `IS_AUTHENTICATED_ANONYMOUSLY`+`AFTER_ACL_COLLECTION_READ`, `IS_AUTHENTICATED_ANONYMOUSLY`+`ACL_SECURABLE_READ`, `GROUP_USER`+`ACL_SECURABLE_EDIT`, `GROUP_ADMIN`+`AFTER_ACL_READ`, `GROUP_AGENT`+`AFTER_ACL_COLLECTION_READ`). All annotations are method-level and move with the method when extracted; no `this`-bound SpEL exists. The post-`AfterInvocationProvider` work (now in `phase2-acl-migrate` HEAD) operates on the *attribute strings*, not the class, so it transparently follows the methods. **Low risk.**

### 7.3 Test fixtures

90 test files reference `ExpressionExperimentService`. Under Option A (retained facade) **none of them need to change**. Under Option B all need rework. Under Option C tests of read paths optionally migrate to the new interface but compile against the facade if not. **Mitigated by Option A.**

### 7.4 Cycle removal exposing latent deadlocks

`ExpressionExperimentSetService`'s only callback into `EESI` is `getTaxon` — a single read on an already-loaded entity. `SampleCoexpressionAnalysisService`'s callbacks are `thawLite` (×2) inside compute methods. Once moved to `ReadService`, neither call should trigger a transactional reentry that would deadlock. Worth a smoke test on a real DEA + sample-coex computation run. **Low–medium risk.**

### 7.5 Bean name collision / XML wiring

The Quartz job map in `applicationContext-schedule.xml` references `expressionExperimentService` by bean name. Under Option A the facade keeps that name, so the XML is unaffected. Under Option B or C with full facade removal, the XML key must be removed (the scheduled task that uses it is verifiable by searching `expressionExperimentService` in scheduled-job code). **Mitigated by Option A.**

### 7.6 Constructor injection ordering during extraction

When the facade is rebuilt as a delegator with three sub-services injected via constructor, Spring will instantiate the three sub-services before the facade. Each sub-service constructor takes ≤ 13 args (Lifecycle is the widest). Lombok `@RequiredArgsConstructor` + `final` fields handles this. **No risk** beyond normal "did you forget a `@Service`" bugs.

---

## 8. Effort estimate

Assuming one agent-session ≈ 2–4 h of focused work:

| Phase | Sessions | Notes |
|---|---:|---|
| 1 — Extract `ReadService` | 2 | ~140 method moves + facade delegation + verify all tests pass + interface JavaDoc carry-over |
| 2 — Break cycles (`EESetService`, `SampleCoexAnalysisService`) | 0.5 | Two trivial autowire swaps |
| 3 — Extract `VectorService` | 1 | ~14 methods; less mechanical than read because mixed tx |
| 4 — Extract `LifecycleService` | 1.5 | Includes the heavy `remove` method; needs e2e smoke test |
| 5 — Constructor-injection cleanup on facade | 0.5 | Once cycles are gone, switch `@Autowired` fields → ctor + `final` |
| **Total (Option A, through facade rewrite)** | **5.5 agent-sessions** | |
| 6 (optional) — Migrate hot callers off facade | 2–4 | Per-module PRs, callsite-driven |
| 7 (optional) — Retire facade | 1 | Only after step 6 reaches zero callers |

**Recommended scope for the Spring 6+ modernization: phases 1–5 (~5.5 sessions). Phases 6–7 are nice-to-have and not blocking.**

---

## 9. Open questions

1. **Should `VectorService` be its own thing, or folded into `LifecycleService`?** Decision driver: do the data-loader CLIs deserve a focused write API surface, or is the facade adequate forever for them? Recommend: separate, because vector I/O methods are the most volatile API (raw → processed → single-cell migration in flight) and benefit from their own evolution.
2. **Lombok `@Delegate` for the facade?** Saves ~150 LOC of one-line forwarders but adds a Lombok dependency at compile-time and slightly obscures the bean wiring. Recommend: skip — write the delegators explicitly so the facade is grep-friendly.
3. **Where does `getEnhancedFilters` go?** It uses `ontologyService` heavily and is read-only but it's also strongly tied to the filter-rewrite helper. Either Read or its own `FilterService`. Recommend: Read, since the helper already exists as a separate bean.
4. **Naming.** `ExpressionExperimentReadService` is verbose. Alternatives: `EERetrievalService`, `ExperimentReadService`, `ExpressionExperimentQueryService`. Recommend: `ExpressionExperimentReadService` for consistency with the matching `…Lifecycle` / `…Vector` siblings.
5. **Phase-3 ordering.** This roadmap assumes phase-3 fixture migration and the AfterInvocation refactor land first. If those slip, the extraction PRs may need rebasing. Coordinate with the phase-3 progress snapshot.

---

*Recce produced 2026-05-18 on branch `worktree-ee-svc-decomp-recce` (worktree fresh from `phase2-acl-migrate` @ `08e760bdaf`). No code changes in this branch — doc only.*
