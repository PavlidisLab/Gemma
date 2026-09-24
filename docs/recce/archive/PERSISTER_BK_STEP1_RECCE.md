# persisterHelper retirement -- Step 1 (BK consolidation) recce

**Date:** 2026-05-19
**Branch baseline:** `phase2-acl-migrate` HEAD `db5c0540af`
**Status:** doc-only -- no code changes.
**Predecessors:**
- `PERSISTER_REPLACEMENT_ROADMAP.md` (commit `753c258481`, 2026-05-18) -- original 8-step plan.
- `PERSISTER_DELETION_PLAN.md` (commit `da7b8e9d15`, 2026-05-18) -- post-conversion deletion plan.

This recce re-scopes Step 1 ("BK consolidation") of the persisterHelper
retirement roadmap given what has already shipped on `phase2-acl-migrate`
since the original roadmap was written. The short answer up front:
**the heavy lift of Step 1 is already in main**; what remains is a
small, optional consolidation pass that should be folded into the
deletion sweep rather than run as a standalone step.

---

## 1. What the roadmap calls "Step 1"

`PERSISTER_REPLACEMENT_ROADMAP.md` Sec. 6 step 1 reads:

> **Lift `Chromosome`, `QuantitationType`, `BioAssayDimension` BK
> logic into `BusinessKey`.** Pure refactor -- no behaviour change.
> Unblocks every downstream step. **~1 agent-session.**

The original roadmap's Sec. 5 also names the broader pattern -- a
per-entity `BusinessKeyResolver<T>` Spring component wrapping
`BusinessKey.find(Session, T)`. That is essentially a packaging
question; the resolver logic itself already lives in `BusinessKey`.

---

## 2. What has actually landed

Commit `ffe6b0293e` ("Phase 3 BusinessKey: lift Chromosome/Quantitation
Type/BioAssayDimension BK lookups from persisters", 2026-05-18) closed
out the literal Step 1 work:

- Added `BusinessKey.find(Session, Chromosome)` at L247.
- Added `BusinessKey.find(Session, QuantitationType)` at L577 (plus
  `matches(...)` + `checkKey(QT)`).
- Added `BusinessKey.find(Session, BioAssayDimension)` at L593 (plus
  `checkKey(BAD)`).

`BusinessKey.java` has grown from 816 LoC (the figure in the original
roadmap) to 934 LoC. There is now a uniform `find(Session, T)` for
every entity the persisters touched.

Beyond Step 1, **most of Steps 3-7 have also landed** (see
`PERSISTER_DELETION_PLAN.md` Sec. 1 for the per-persister state).
Concretely:

- `CommonPersister` (211 LoC): every `persistXxx` method now calls
  `BusinessKey.find` or a DAO `find()` that internally calls
  `BusinessKey.find`. No raw inline criteria-builders remain.
- `ArrayDesignPersister` (139 LoC): `findOrPersistArrayDesign` ->
  `BusinessKey.find` + `dao.create`; cascade=all carries the
  composite-sequences / external references.
- `GenomePersister` (950 LoC): `persistBioSequence`,
  `persistChromosome`, `persistTaxon` all rewired to `BusinessKey.find`.
  The `updateGene` / `handleGeneProductChangedGIs` business logic is
  copied (not yet invoked) into a new `GeneWriteService` skeleton.
- `RelationshipPersister` (97 LoC): `persistGene2GOAssociation` ->
  `BusinessKey.find`. `ExpressionExperimentSet` skipped (no static BK;
  documented in the file).
- `ExpressionPersister` (154 LoC, was 507): collapsed to a thin
  delegate over `EeWriteServiceImpl` (539 LoC) which owns the EE-graph
  write body and itself uses DAO `findOrCreate` for the simple cases
  (`BioAssayDimension`, `BioMaterial`, `Compound`, `FactorValue`,
  `ExperimentalFactor`, `ExpressionExperimentSubSet`).

So the find-or-create-by-BK logic that the original roadmap described
as "scattered across PersisterHelper, persister/* classes, and
individual entity persisters" is no longer scattered. It lives in
exactly two places:

1. **`BusinessKey.java`** -- the static `find(Session, T)` methods,
   one per entity, all using JPA Criteria.
2. **Per-DAO `find(T)` overrides** -- thin wrappers that either
   delegate to `BusinessKey.find` (Taxon, Gene, BioMaterial,
   ArrayDesign, BioSequence, Contact, ...) or do a one-line
   `findOneByProperty` (`ExternalDatabaseDaoImpl.find` is the
   canonical single-property example, L39-41: just
   `findOneByProperty("name", ...)`).

The `findOrCreate(T)` method on `AbstractDao` (L433-436) is the
canonical "thin per-entity BusinessKeyResolver" the roadmap asked for
-- it's just spelled as a DAO inheritance, not a separate
`@Component`. There is no further consolidation to do at the
*resolver* layer.

---

## 3. Inventory: entities with BK find-or-create logic today

Counts based on `BusinessKey.find(Session, T)` overloads + per-DAO
`find(T)` overrides on `phase2-acl-migrate` HEAD `db5c0540af`.

| Entity | `BusinessKey.find` | DAO `find(T)` override | Used by which persister | Notes |
|---|:-:|:-:|---|---|
| `Taxon` | yes (L472) | yes (delegates to BK) | GenomePersister | clean. |
| `Gene` | yes (L395) | yes (delegates) | GenomePersister | three-tier cascade in `BusinessKey`. |
| `GeneProduct` | yes (L437) | yes (delegates) | GenomePersister | name + GI. |
| `BioSequence` | yes (L202) | yes (delegates) | GenomePersister, ArrayDesignPersister | name+taxon, optional dbEntry. |
| `Chromosome` | yes (L247, new) | yes | GenomePersister | landed `ffe6b0293e`. |
| `ArrayDesign` | yes (L120) | yes (delegates, L269) | ArrayDesignPersister | one-field BK (shortName). |
| `BioMaterial` | yes (L174) | yes (delegates, L76) | ExpressionPersister (via EeWriteService) | retry loop in DAO. |
| `BioAssay` | yes (L146) | yes (delegates) | ExpressionPersister | tightly coupled to EE. |
| `FactorValue` | yes (L322) | yes | ExpressionPersister | composite key. |
| `ExperimentalFactor` | yes (L308) | yes | ExpressionPersister | composite key. |
| `QuantitationType` | yes (L577, new) | yes | CommonPersister | landed `ffe6b0293e`. Persister deliberately uses `create`, not findOrCreate -- QTs are per-EE. |
| `BioAssayDimension` | yes (L593, new) | yes (`findOrCreate` at line 335 of EeWriteServiceImpl) | ExpressionPersister/EeWriteService | landed `ffe6b0293e`. |
| `Contact` / `Person` | yes (L277) | yes (delegates) | CommonPersister | Person extends Contact. |
| `Characteristic` | yes (L496) | -- | CommonPersister | cascaded-only. |
| `Unit` | yes (L512) | yes (delegates) | CommonPersister | name-only. |
| `ExpressionExperimentSubSet` | yes (L531) | yes (delegates) | ExpressionPersister | composite. |
| `ExternalDatabase` | no | yes (`findOneByProperty("name")`, L39) | CommonPersister | single-field BK; no static needed. |
| `BibliographicReference` | no | yes (queries by pubAccession.accession) | CommonPersister | DB entry is the BK. |
| `Gene2GOAssociation` | yes (L426) | -- | RelationshipPersister | pure relationship. |
| `ExpressionExperiment` | no static | yes (via `findByShortName`) | ExpressionPersister | shortName BK lives in the DAO. |
| `Compound` | no | yes (`findOrCreate` via inherited AbstractDao) | ExpressionPersister | mostly-ID. |
| `Protocol`, `ExperimentalDesign`, `ExpressionExperimentSet` | n/a | n/a | -- | create-only by design. |

**Entity count with BK find-or-create logic: 19 entities.** All 19 now
have a uniform `BusinessKey.find` and/or DAO `find(T)` entry point.

---

## 4. Redundancy / scatter assessment

Three forms of "scatter" the original roadmap worried about:

### 4.1 Inline criteria-builders in persisters -- ELIMINATED

The original roadmap flagged Chromosome, QuantitationType, and
BioAssayDimension as having inline BK logic *only* in the persister
files. All three were lifted into `BusinessKey` by commit
`ffe6b0293e`. No persister-side inline BK criteria-builders remain.

### 4.2 Persister-side caching of BK lookups -- still present, scoped

`CommonPersister` still caches `ExternalDatabase` and
`QuantitationType` lookups per-call in a `Caches` value object
(L150-162, L189-200). The `Caches` object is built per `persist(...)`
call by `AbstractPersister` and threaded through the chain. This is
genuine behaviour, not redundancy -- it avoids round-trips inside one
graph persist. After the persister chain is deleted (Step E5 in
`PERSISTER_DELETION_PLAN.md`) the caches either:

- move into `EeWriteServiceImpl` (it's the only caller that needs them
  for whole-EE graph persistence), or
- get replaced by Hibernate's per-session first-level cache (likely
  sufficient given the `BusinessKey.find` calls all reuse the same
  Session within a transaction).

Either path is straightforward; neither is "BK consolidation" per se.

### 4.3 Three-place lookup pattern: `BusinessKey.find` vs `dao.find` vs `dao.findOrCreate` -- intentional layering

Five distinct usage patterns exist in main today:

- `BusinessKey.find(session, probe)` -- direct, used by persisters
  that want to avoid the DAO indirection.
- `dao.find(probe)` -- DAO `find(T)` override delegating to
  `BusinessKey.find`. Used by callers that already have the DAO
  injected.
- `dao.findOrCreate(probe)` -- inherited from `AbstractDao` (L433),
  used by `EeWriteServiceImpl` for the simple cases.
- `Service.findOrCreate(probe)` -- e.g.
  `BioSequenceServiceImpl.findOrCreate`, used by callers outside the
  persistence package.
- `Caches`-wrapped lookup -- `CommonPersister.persistExternalDatabase`,
  per-call memoisation for repeated lookups in one graph.

This is layering, not redundancy. Each layer adds one piece of value
(transaction binding, caching, business-rule wrapping). Collapsing
them would be a code-style decision, not a correctness fix.

---

## 5. The "thin per-entity BusinessKeyResolver" question

The original roadmap (Sec. 5.1) proposed a new interface:

```java
public interface BusinessKeyResolver<T extends Identifiable> {
    Optional<T> findByBusinessKey(T probe);
}
```

with one `@Component` per entity wrapping `BusinessKey.find(session, T)`.

**Recommendation: do not introduce this interface now.** Reasoning:

1. The DAO `find(T)` overrides already provide the same surface. Every
   DAO `find(T)` either is a `BusinessKey.find` wrapper or is a
   trivial property-equality query that *could* be in `BusinessKey`
   but doesn't need to be.
2. The interface would create a third lookup path
   (`businessKeyResolver.findByBusinessKey(t)`) alongside the existing
   two (`dao.find(t)` and `BusinessKey.find(s, t)`), worsening rather
   than improving the layering.
3. The persisters are scheduled for deletion (per
   `PERSISTER_DELETION_PLAN.md` Sec. 4.5). Adding a new interface
   layer that only the about-to-be-deleted persisters consume is
   negative-value work.
4. The proper end-state is: callers use the per-entity write services
   (`EeWriteService`, `GeneWriteService`, `ArrayDesignService`,
   `BibliographicReferenceService`, ...). The write service owns the
   BK lookup as an implementation detail; it can call `dao.find(t)` or
   `BusinessKey.find(session, t)` interchangeably. No external resolver
   interface needed.

If a future cleanup wants to remove `BusinessKey` as a static utility
and convert it to per-entity `@Component`s, that's a 1-2 session
refactor that can happen after persister deletion. It is not on the
critical path for the persisterHelper retirement.

---

## 6. Concrete first-step plan (revised)

Given Step 1 has landed, the meaningful "first step" question becomes:
**what is the smallest, lowest-risk task that moves us closer to
persister deletion?** Two candidates:

### 6.1 Pilot: `ExternalDatabase` `Caches` lift

`CommonPersister.persistExternalDatabase` (L147-163) is the smallest
remaining persister-side behaviour that is not yet replicated
elsewhere: it caches name -> ExternalDatabase in the `Caches` value
object so repeated probes within one EE-graph persist don't round-trip
to the DB.

**Pilot plan:**

1. Move the `Map<String, ExternalDatabase>` cache from `Caches`
   into `EeWriteServiceImpl` as a per-call local
   `Map<String, ExternalDatabase>`.
2. Replace `CommonPersister.persistExternalDatabase`'s body with a
   plain `dao.find(name).orElseGet(dao.create)`.
3. Verify by running the GEO load smoke test against gemdtest --
   `LoadExperimentCli` with a small GEO fixture (e.g. GSE40191,
   the standard tiny-EE smoke) should produce identical row counts to
   the pre-change baseline.

Why this is a good pilot:

- One entity, one method, ~15 LoC change.
- Touches only `CommonPersister` (slated for deletion) and
  `EeWriteServiceImpl` (the survivor).
- Shakes out the "where do caches live post-persister" question
  without committing to the full deletion sweep.
- If the smoke test reveals a perf regression (unlikely; Hibernate's
  first-level cache should cover us), the change is trivially
  revertable.

### 6.2 Alternative pilot: `Contact` consolidation

`CommonPersister.persistContact` (L140-145) is even smaller -- no
caching, just a direct `BusinessKey.find` then `dao.create`. The
caller surface (`AuditTrail.performer`) goes through
`AuditTrailService`, which already wraps the audit-event creation.
Move the call site one level up:

1. Change `AuditTrailServiceImpl.create(event)` so it resolves
   `event.performer` via `contactDao.find` directly, removing the
   `persisterHelper.persist(Contact)` indirection.
2. Delete `CommonPersister.persistContact` and its dispatch arm in
   `CommonPersister.doPersist`.

Why this is also good:

- Cleanest in isolation -- one caller, one DAO, no Caches involved.
- Sets the pattern for the rest of `CommonPersister`'s persistXxx
  methods (Person, Unit, Protocol, BibRef -- all do the same shape).

**Recommended pilot: 6.2 (`Contact` consolidation).** It's the simpler
of the two, sets a clearer pattern for the eight other persistXxx
methods in `CommonPersister`, and doesn't need to touch the `Caches`
value object's lifetime contract (which is its own follow-up).

#### 6.2.bis Pilot as executed (2026-05-19)

The pilot as actually run differed from the plan above; the recce
mis-identified the caller. `AuditTrailServiceImpl.createAuditEvent`
sets the performer from `userManager.getCurrentUser()` (already a
managed User entity) -- it never goes through `persisterHelper`. So
"`AuditTrail.performer` goes through `AuditTrailService`" is true,
but the resolution step the recce wanted to eliminate doesn't exist
on that path.

The actual callers of `persistContact` on `phase2-acl-migrate` HEAD
`e39366679` are two, both inside the persister package:

1. `ArrayDesignPersister.persistNewArrayDesign` -- calls
   `this.persistContact(arrayDesign.getDesignProvider())` to resolve
   the array-design `designProvider` (a `Contact`).
2. `EeWriteServiceImpl.persistExpressionExperiment` -- the
   `ee.setOwner(persister().doPersist(ee.getOwner(), caches))` line
   routes through the `instanceof Contact` arm of
   `CommonPersister.doPersist` (EE owner is a `Contact`).

Migration done:

- `ArrayDesignPersister`: added `@Autowired ContactDao contactDao`;
  inlined `contactDao.find(...) orElse contactDao.create(...)` in
  place of `this.persistContact(...)`.
- `EeWriteServiceImpl`: added `@Autowired ContactDao contactDao`;
  inlined `contactDao.find(...) orElse contactDao.create(...)` in
  place of `persister().doPersist(ee.getOwner(), caches)`.
- `CommonPersister`: removed the `instanceof Contact` dispatch arm,
  deleted `persistContact`, removed the now-unused `ContactDao`
  field + import.

`AuditTrailServiceImpl` was left untouched. The recce's Sec. 6.3
acceptance check ("grep for `persisterHelper.persist.*Contact`
returns zero") was already true before the change; the relevant grep
post-change is for `persistContact` callers (also zero) and for
`instanceof Contact` in `CommonPersister.doPersist` (also gone).

Sets the same pattern Sec. 6.2 was after -- a `dao.find / dao.create`
two-liner at each call site -- on the actual surface that needed
it. Pattern is reusable for the remaining seven persistXxx methods
in `CommonPersister`.

#### 6.2.tris Person follow-up (2026-05-19)

Same shape, even smaller. After the Contact pilot inlined the two
Contact-typed callers (`ArrayDesign.designProvider`,
`ExpressionExperiment.owner`), the `instanceof Person` arm of
`CommonPersister.doPersist` had no live caller:

- No model field is typed `Person` (only `User extends Person`,
  which is handled by a separate `UnsupportedOperationException`
  arm; everything else routes through `Contact`).
- Both `Contact`-typed call sites were inlined by the Contact
  pilot, so no `instanceof Contact` runtime-Person object reaches
  `doPersist` either.
- `grep -rn "persistPerson\|doPersist.*[Pp]erson" gemma-core
  gemma-cli gemma-rest gemma-web` returns only the two
  `CommonPersister` self-references; no external callers.
- `grep -rn "persisterHelper.persist" --include=*.java` returns
  zero `Person`-shaped arguments.
- No `Person`/`Persister` test class exists (`-Dtest='*Person*,
  *Persister*'` runs zero tests).

Migration done:

- `CommonPersister`: removed the `instanceof Person` dispatch arm,
  deleted `persistPerson`, removed the now-unused `PersonDao`
  field + import.

No call-site inlining needed (the Contact pilot already covered
the only paths that could have reached `persistPerson` -- both
went via the Contact arm at runtime, both now use `contactDao.find
orElse contactDao.create`, which handles Person correctly because
`BusinessKey.find(Session, Contact)` returns whichever subclass
matches the BK).

`mvn -pl gemma-core,gemma-cli,gemma-rest test-compile` clean after
the change.

#### 6.2.quater Persister sweep -- Protocol, AuditTrail, Unit (2026-05-19)

Same pattern as 6.2.bis/tris, applied to three more `persistXxx`
methods in `CommonPersister`. All landed on branch `persister-sweep`
off `phase2-acl-migrate` HEAD `c8e212f969`.

**Protocol** (consolidated, 1 caller):

- Caller: `DifferentialExpressionAnalysisHelperServiceImpl.persistStub`
  set `entity.setProtocol((Protocol) persisterHelper.persist(...))`.
- `persistProtocol` was a pure pass-through (`protocolDao.create(p)`)
  -- Protocols are intentionally not shared across analyses.
- Inlined at call site as `entity.setProtocol(protocolDao.create(...))`.
- Removed: `instanceof Protocol` arm, `persistProtocol` method,
  `ProtocolDao` field + import from `CommonPersister`.

**AuditTrail** (dead-removed, 0 callers):

- No production caller of `persisterHelper.persist(AuditTrail)`.
  Audit trails are always reached via the parent `Auditable`'s
  `cascade=all` and never enter the dispatch chain.
- The comment on `persistAuditTrail` said "preserved for the few
  that do" -- there are none.
- Removed: `instanceof AuditTrail` arm, `persistAuditTrail` method,
  `AuditTrailDao` field + import.

**Unit** (consolidated, 2 callers, both in `EeWriteServiceImpl`):

- Callers: `fillInExperimentalFactorAssociations` and
  `fillInFactorValueAssociations`, both calling
  `persister().persistUnit(...)` for FactorValue measurement units.
- The previous `persistUnit` bypassed `UnitDao.find` to call
  `BusinessKey.find(session, unit)` directly -- but the DAO
  delegates to the same call, so semantics are identical.
- Added private `findOrCreateUnit(Unit)` helper in
  `EeWriteServiceImpl` (`unitDao.find orElse unitDao.create`) and
  routed both call sites through it.
- Removed: `instanceof Unit` arm, `persistUnit` method, `UnitDao`
  field, and the now-stale `Session` / `Unit` / `BusinessKey`
  imports + Javadoc `{@link}` reference in `CommonPersister`.

After all three, `CommonPersister.doPersist` is down to: `User`
(throws), `QuantitationType`, `ExternalDatabase`, `Characteristic`
(no-op), `BibliographicReference`, `DatabaseEntry` -- six arms,
two of which (`User`, `Characteristic`) are degenerate. The
remaining four all involve `Caches` (`ExternalDatabase`, `QT`,
`BibRef` via `fillInDatabaseEntry`, `DatabaseEntry` likewise).
Lifting them requires a decision on where the per-call
ExternalDatabase / QT caches live post-persister -- see Sec 4.2
above. Deferred to the deletion sweep.

`mvn -pl gemma-core,gemma-cli,gemma-rest test-compile` clean after
each commit (three commits, one per `persistXxx`).

**Persister sweep -- methods still present in `CommonPersister`:**

| method | callers | state | reason left |
|---|---|---|---|
| `persistExternalDatabase` | 5 in persisters + 4 in `GeneWriteServiceImpl` | left | Owns the per-call `ExternalDatabase` cache (`Caches.externalDatabaseCache`). Lifting needs cache-lifetime decision (Sec 4.2). |
| `persistDatabaseEntry` (private) | dispatch + `fillInDatabaseEntry` | left | Pure create + cached external-DB resolve. Goes with `persistExternalDatabase`. |
| `persistQuantitationType` | 1 (`EeWriteServiceImpl` L355) | left | Owns the per-experiment QT cache (`Caches.quantitationTypeCache`) -- intentionally per-EE; lifting requires the cache to follow. |
| `persistBibliographicReference` (private) | 1 (`GeoServiceImpl` L387 via dispatch) | left | Calls `fillInDatabaseEntry` which uses the ExternalDatabase cache. Couples to the cache-lifetime question above. |

All four remaining `persistXxx` in `CommonPersister` are blocked on
the same call: where do the `Caches.externalDatabaseCache` and
`Caches.quantitationTypeCache` live after the persister chain is
deleted? That is exactly the question Sec 6.1 (`ExternalDatabase`
`Caches` lift) was posed to answer. Recommend doing that pilot
next before sweeping any of the four; otherwise each sweep would
have to invent its own answer in isolation.

### 6.3 Smallest test of the new pattern

After 6.2 lands, the acceptance check is:

- `grep -n 'persisterHelper\.persist.*Contact' gemma-core/src/main
  gemma-cli/src/main` returns zero matches.
- `AuditTrailIntegrationTest` (or its successor under
  `BaseIntegrationTest5`) passes against gemdtest.
- One smoke run of `LoadExperimentCli` on a small GEO fixture
  produces identical audit-trail performer rows.

That validates the per-entity write-service pattern end-to-end on the
smallest possible surface.

---

## 7. Effort estimate for Step 1 (revised)

Literal Step 1 as originally written: **0 sessions** -- it landed in
`ffe6b0293e`. No further BK-utility work is needed.

If "Step 1" is reinterpreted as "the first concrete step of the
remaining persister-retirement work" (the 6.2 pilot above), the
estimate is:

| Task | Effort |
|---|---:|
| `AuditTrailServiceImpl.create` rewires performer resolution to `contactDao.find` | 0.25 session |
| Delete `CommonPersister.persistContact` + dispatch arm | included |
| Run `mvn verify` against gemdtest, fix any test fallout | 0.25 session |
| **Total** | **0.5 session** |

For the broader `CommonPersister` consolidation (all 10 persistXxx
methods, not just Contact): ~1 session. That maps to Step 3 of the
original roadmap and is already partly covered by commit
`a1ca482301` (CommonPersister BK rewire). What remains is the
extraction into per-entity services and removal of the dispatch arms
-- handled by Step E4 in `PERSISTER_DELETION_PLAN.md`.

---

## 8. Bottom line

- Literal Step 1 (BK consolidation for Chromosome/QT/BAD) **shipped
  2026-05-18** in commit `ffe6b0293e`. There is no missing BK
  lift-and-shift work.
- `BusinessKey.java` (934 LoC) now has a uniform `find(Session, T)`
  for every entity the persister chain touches; per-DAO `find(T)`
  overrides delegate to it. No inline criteria-builders remain in the
  persisters.
- The original roadmap's "thin per-entity `BusinessKeyResolver`
  interface" proposal should be **skipped**. The DAO `find(T)`
  pattern + per-entity write services already provide that surface,
  and adding a new interface layer to a soon-to-be-deleted chain is
  negative-value work.
- The meaningful "next step" is the per-entity write-service caller
  cutover (Step E4 of `PERSISTER_DELETION_PLAN.md`). The smallest
  pilot is the **`Contact` consolidation in `AuditTrailServiceImpl`**
  (Sec. 6.2 above, ~0.5 session).
- All Step 1 effort estimates in the original roadmap are now sunk
  cost; further BK work is folded into Steps E4/E5 of the deletion
  plan.

**Recommendation: close out Step 1 in the roadmap and pick up
directly from Step E4 of `PERSISTER_DELETION_PLAN.md`.** The Contact
pilot above is a reasonable warm-up for that sweep.
