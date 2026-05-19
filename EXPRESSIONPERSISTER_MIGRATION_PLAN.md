# ExpressionPersister retirement: migration plan (risk-5 recce)

**Date:** 2026-05-18
**Branch baseline:** `worktree-expressionpersister-recce` HEAD
`a1ca482301` (CommonPersister BK-conversion present as the pattern to
mimic; ACL listener cutover landed in `21e4fc412e8f` — included).
**Status:** recce only — no code changed in this commit.
**Predecessor:** `PERSISTER_REPLACEMENT_ROADMAP.md` (commit
`753c258481`), step 7. Sibling recce: `GENOMEPERSISTER_MIGRATION_PLAN.md`
(commit `baf8a6c919`).

---

## 1. Baseline: LoC + method count

`gemma-core/src/main/java/ubic/gemma/persistence/persister/ExpressionPersister.java`
— **507 LoC, 20 methods**, 1 public (`prepare`), 1 public override
(`persist`), 1 protected entry (`persistExpressionExperiment`), 1
protected dispatch (`doPersist`), 16 private helpers. Nine `@Autowired`
DAO fields: `bioAssayDimensionDao`, `bioAssayDao`, `bioMaterialDao`,
`compoundDao`, `experimentalDesignDao`, `experimentalFactorDao`,
`expressionExperimentDao`, `expressionExperimentSubSetDao`,
`factorValueDao`, plus `expressionExperimentPrePersistService`.

The entry point (`persist(EE, cachedArrays)`) wraps
`persistExpressionExperiment` in a `setHibernateFlushMode(MANUAL)` /
`flush()` / restore-`AUTO` block — redundant with the identical wrap
already in `AbstractPersister.persist(T)` (lines 97–107). See §4.

**Important context: vectors are cascade-driven, not bulk-written.**
The roadmap's per-line risk note mentions
`persistDesignElementDataVectors` / raw-vector bulk write, but in the
current code there is no such method. `RawExpressionDataVector` is
persisted via `cascade="all"` on `ExpressionExperiment.rawExpressionDataVectors`
(verified `Investigation.hbm.xml:141-142`). The persister only walks
each vector to *fill in associations* (BAD + QT) before the cascade
fires on `expressionExperimentDao.create(ee)`. The hot loop is
`fillInExpressionExperimentDataVectorAssociations` (lines 295–325) but
it never calls a vector DAO. This re-frames the migration: there is no
"bulk write path that must stay native SQL" — the whole class is graph
preparation, and Hibernate cascade does the writing.

---

## 2. Method categorization

### 2a. Simple find-or-create — drops in directly (low risk, 5 methods, ~85 LoC)

These map 1:1 onto the CommonPersister pattern. Each becomes a 4–6 line
method in the new service.

| Method | LoC | BK resolver | Notes |
|---|---:|---|---|
| `persistCompound` | 3 | `compoundDao.findOrCreate(c)` already wraps BK. | Trivial pass-through; inline into the EE write service or delete. |
| `persistBioMaterial` | 19 | `BusinessKey.find(s, BioMaterial)` (line 174). | After taxon + external-accession resolution, becomes `bioMaterialDao.findOrCreate(bm)`. The dao impl is the BK consumer. |
| `persistFactorValue` | 11 | `BusinessKey.find(s, FactorValue)` (line 322). | Already `factorValueDao.findOrCreate` after parent-EF fill-in. Trivial. |
| `persistExperimentalFactor` | 4 | n/a — `create`-only (comment: "uses 'create', not 'findOrCreate'"). | EF is composition-owned by the EE's `ExperimentalDesign`. Drops to direct `experimentalFactorDao.create(ef)`. |
| `persistExpressionExperimentSubSet` | 9 | `expressionExperimentSubSetDao.findOrCreate(s)`. | Two argument checks, then DAO call. Inline trivially. |

### 2b. BioAssayDimension reuse — preserve verbatim (1 method, ~17 LoC)

| Method | Why it stays |
|---|---|
| `persistBioAssayDimension` | Walks `bioAssays` list, persists each via `persistBioAssay`, then calls `bioAssayDimensionDao.findOrCreate(bad)`. BAD's `find()` uses `BusinessKey.find(s, BioAssayDimension)` (lines 593–612) which matches on the *exact ordered* bioassay list — this is what prevents N×M dimension explosion across vector groups within an EE. The walk-then-find order is load-bearing: BAs must be ID'd before the BAD lookup. Lift into the new write service unchanged. |

### 2c. BioAssay association fill — relocate intact (2 methods, ~70 LoC)

| Method | LoC | Disposition |
|---|---:|---|
| `persistBioAssay` | 4 | Calls `fillInBioAssayAssociations` then `bioAssayDao.create(ba)`. Trivial wrapper — inline. |
| `fillInBioAssayAssociations` | 53 | Resolves AD (from cache), walks `sampleUsed.factorValues` to persist each, fills `accession.externalDatabase`, then persists `sampleUsed` via `persistBioMaterial`. **Load-bearing**: the AD resolution uses the per-prepare cache (line 232) and falls back to `session.load(ArrayDesign, id)` to ensure the BA points at an attached AD proxy. Without this, the cascade-driven create would fail because BAs reference *transient* ADs returned from the parser. Survives in the new `EeWriteService` as a private helper. |

### 2d. ExperimentalDesign / cascade-override (3 methods, ~70 LoC)

| Method | LoC | Disposition |
|---|---:|---|
| `processExperimentalDesign` | 50 | The "override cascade" comment on line 467 + line 481 reveals the intent: this method exists because `EE.experimentalDesign` cascade=all would otherwise prematurely flush half-built EF/FV graphs *before* their ACLs are attached. With ACL maintenance on `EntityInsert` (post-21e4fc41), this is no longer required: `session.persist(ee)` will cascade through ED→EF→FV and the listener fires `EntityInsert` for each, attaching the ACL at insert time. **Open question 1**: confirm with a black-box test (an EE with experimental factors goes through `expressionExperimentDao.create(ee)` *without* `processExperimentalDesign`, ACLs verified on every FV). If confirmed, **delete the method.** If a corner case turns up (e.g. FV self-reference, EF→ED back-reference), reduce to a 6-line cascade-prep helper. |
| `fillInExperimentalFactorAssociations` | 3 | One-liner that walks `ef.annotations`. Inline or delete. |
| `fillInFactorValueAssociations` | 10 | Walks `fv.experimentalFactor` (recursing) and `fv.measurement.unit`. Survives as a private helper in `EeWriteService`. |

### 2e. Vector association fill — preserve, restructure (3 methods, ~85 LoC)

| Method | LoC | Disposition |
|---|---:|---|
| `fillInExpressionExperimentDataVectorAssociations` | 31 | The hot loop. Walks every `RawExpressionDataVector`, fills its BAD + QT, accumulates BAs touched. **Survives intact** as `EeWriteService.prepareVectors(ee, caches)`. No DAO calls — pure object-graph manipulation. The stopwatch logging stays. |
| `fillInDesignElementDataVectorAssociations` | 15 | Per-vector BAD+QT resolution. Calls `getBioAssayDimensionFromCacheOrCreate` (which is the cache key by hashCode on the BAD's bioAssays list — see quirks). Survives intact. |
| `getBioAssayDimensionFromCacheOrCreate` | 14 | The hashCode-keyed BAD cache. Survives intact. |

### 2f. EE top-level orchestration — collapse into write service entry (4 methods, ~155 LoC)

| Method | LoC | Disposition |
|---|---:|---|
| `persist(EE, cachedArrays)` public override | 12 | The FlushMode.MANUAL wrap is **redundant** (already on AbstractPersister.persist). With the new entry point it becomes a `@Transactional` method on `EeWriteService` that delegates to `persistExpressionExperiment` and `flush()`s. The redundant MANUAL/AUTO wrap is dropped (only one layer needed). |
| `persistExpressionExperiment` | 46 | The body of `EeWriteService.create(EE ee, ArrayDesignsForExperimentCache cache)`. Linear: `findByShortName` short-circuit → persist publications/owner/taxon/QTs → fill EA → `processExperimentalDesign` (or its replacement) → `checkExperimentalDesign` → `processBioAssays` → `expressionExperimentDao.create(ee)`. |
| `doPersist(T, Caches)` dispatch override | 23 | Disappears with the persister chain. The new service exposes typed entry points: `create(EE)`, `findOrCreateSubSet(...)`. Callers that pass loose `Identifiable` (none of the production callers do — they all pass `EE` directly) move to the typed methods. |
| `prepare(EE)` | 3 | Pure delegate to `expressionExperimentPrePersistService`. Stays — or callers move to call the prepare service directly (it already is `@Service`). |

### 2g. Sanity checks — keep (1 method, ~55 LoC)

| Method | LoC | Disposition |
|---|---:|---|
| `checkExperimentalDesign` | 55 | Validates every FV has at least one BM using it (warning only — see FIXME on line 213). Pure validation, no DAO calls. **Stays unchanged** as a private helper in `EeWriteService.create`. |

**Bucket totals:** 5 trivial + 1 BAD-reuse + 2 BA-fill + 3 ED-cascade + 3
vector-fill + 4 top-level + 1 sanity = **19 methods**. The 20th is the
4-LoC `prepare()` delegator. The persister chain dispatch glue
(`doPersist`) disappears with the chain.

---

## 3. Caller traffic

ExpressionPersister is reached only via
`persisterHelper.persist(EE, cache)` / `persisterHelper.persist(EE)`
— the `instanceof ExpressionExperiment` branch in `doPersist`. Five
production call sites across **3 files** (the other gemma-web sites are
walking-dead — see project memory).

### Production callers (5 sites in production code; 3 hot)

| Site | What it does | Hot? | Replacement |
|---|---|---|---|
| `GeoServiceImpl:276` — `ee = persisterHelper.persist(ee, c)` | The whole-EE write that finishes a GEO load. Cache `c` is the ArrayDesignsForExperimentCache produced by `prepare()` in a prior transaction. | **HOTTEST** — every GEO experiment load. | `eeWriteService.create(ee, c)`. One-line change. |
| `SimpleExpressionDataLoaderServiceImpl:97` — `experiment = persisterHelper.persist(experiment, persisterHelper.prepare(experiment))` | User-uploaded SimpleEE. Prepares in the *same* tx (the comment on line 141 of ExpressionPersister.doPersist says "Consider doing the 'prepare' step in a separate transaction"). | **Hot** — every SimpleEE upload via the curation UI. | `eeWriteService.create(experiment, prePersistService.prepare(experiment))`. One-line change. |
| `GeoServiceImpl:141` — `persisterHelper.persist(cs.getBiologicalCharacteristic())` | Persists a single `BioSequence`. Routed to `GenomePersister` via the dispatch, not ExpressionPersister. | Hot, but not our problem — covered by `GENOMEPERSISTER_MIGRATION_PLAN.md`. | N/A — sibling recce. |
| `GeoServiceImpl:208` — `return persisterHelper.persist(arrayDesigns)` | Collection of `ArrayDesign`. Routes to `ArrayDesignPersister`, not us. | Hot, not our problem. | N/A. |
| `GeoServiceImpl:384` — bib-ref persist | Routes to `CommonPersister`. | Low. | N/A. |

### Test callers

Tests that traffic `ExpressionExperiment` through `persisterHelper`:
~6 files (`AclAdviceTest`, `AclAuthorizationTest`,
`ProcessedExpressionDataVectorServiceTest`,
`CompositeSequenceGeneMapperServiceTest`,
`PersistentDummyObjectHelper.getTestExpressionExperiment*`,
`ExpressionDataSVDTest`, `TwoChannelMissingValuesTest`). All flow
through `PersistentDummyObjectHelper`, which is itself a candidate for
migration to `ExperimentFactory` (in-flight per `MEMORY.md`
`project_phase3_progress.md`).

---

## 4. FlushMode.MANUAL analysis + recommendation

### History

- **2012-05-20** (commit `4df4b89e`, bug 2888): introduced as
  `FlushMode.COMMIT` to "hold off flushes until the whole EE graph is
  built". Same pattern simultaneously added to `ArrayDesignPersister`.
- **2022-11-09** (commit `de9fa4d2`, "Cleanups for Persister"): cleanup
  pass left the wrap in place; mode upgraded to `MANUAL` (stronger
  guarantee than `COMMIT`).
- **2026-05-17** (commit `b5aa51cb`, "Align worktree to phase2 state"):
  no behavioural change — `setFlushMode` → `setHibernateFlushMode`
  rename only.
- **2026-05-18** (commit `21e4fc41`): **ACL listener cutover** — the
  `@AfterReturning` AOP advice on `dao.create()` that previously
  required an explicit `create()` call (and therefore *forbade*
  cascade-only persistence) is now disabled. ACL maintenance is fired
  by Hibernate's `EntityInsert` event listener instead — so a cascade
  insert and an explicit `dao.create()` both produce identical ACL
  results.

### Original purposes (two of them)

1. **Memory: prevent auto-flush mid-build of a huge graph.** With AUTO,
   any HQL `find()` during the walk would trigger a partial flush of
   the half-built object graph, blowing up the L1 cache and (more
   importantly) writing partial state on transient FKs. MANUAL holds
   everything until the explicit `flush()` at the end. **Still valid
   post-Phase-3.**
2. **ACL: force explicit `dao.create()` calls.** The comment on line
   496 — *"this cascades from updates to the factor, but because
   auto-flush is off, we have to do this here to get ACLs populated"* —
   reveals the second reason. The old `@AfterReturning("execution(*
   *Dao.create(..))")` advice ran on the *method call*, not on the
   `EntityInsert` event, so a cascade-only persist wouldn't fire it.
   The flush had to be deferred AND the `create()` call made
   explicitly. **Post-listener cutover, no longer valid.** The listener
   sees `EntityInsert` events from cascades too.

### Verdict

**KEEP MANUAL at one layer — the `AbstractPersister`/new-service
boundary — and DELETE the redundant inner wrap on
`ExpressionPersister.persist`.**

Concretely, in the new `EeWriteService.create(ee, cache)`:

```java
@Transactional
public ExpressionExperiment create( ExpressionExperiment ee,
        ArrayDesignsForExperimentCache cache ) {
    Session s = sessionFactory.getCurrentSession();
    FlushMode prev = s.getHibernateFlushMode();
    s.setHibernateFlushMode( FlushMode.MANUAL );
    try {
        ExpressionExperiment persisted = doCreate( ee, cache );
        s.flush();
        return persisted;
    } finally {
        s.setHibernateFlushMode( prev );
    }
}
```

The memory reason (1) survives Phase 3 and is independent of which
ACL mechanism is in play. We keep it.

The ACL reason (2) is gone: the cascade-override gymnastics in
`processExperimentalDesign` (the explicit `factorValueDao.create` loop
on lines 491–501) can be replaced by trusting the cascade.

**Cleanup wins from removing reason (2):**
- `processExperimentalDesign` collapses from 50 LoC to ~6 LoC (or
  disappears entirely if cascade-only works).
- The `// Withhold to avoid premature cascade` dance on lines 460–471
  disappears.
- Inside `persistExpressionExperiment`, the explicit
  `experimentalFactorDao.update(experimentalFactor)` at line 503 is
  no longer needed (cascade=all on EE→ED→EF handles inserts).

**Validation gate before deleting the explicit-create loop:** run
`AclAdviceTest`, `AclAuthorizationTest`, and a fresh
`EeWriteServiceTest` that persists an EE with experimental factors
*without* calling `processExperimentalDesign`; verify ACLs exist on
every persisted FactorValue + ExperimentalFactor + ExperimentalDesign.

---

## 5. Quirks inventory

### 5.1 FlushMode.MANUAL wrap (see §4)
Memory-control reason still valid. ACL reason obsolete.

### 5.2 BioAssayDimension reuse by hashCode

`getBioAssayDimensionFromCacheOrCreate` (line 338) caches BADs in a
`Map<Integer, BioAssayDimension>` keyed on
`vector.getBioAssayDimension().hashCode()`. This prevents N×M dimension
explosion when multiple `RawExpressionDataVector`s share the same BAD
(the common case). `BioAssayDimension.hashCode` is content-based (over
the ordered bioAssay list — confirmed by inspecting the BK matcher).
**Quirk**: the cache lives inside the `Caches` value object passed
through the call chain; it has *no* identity beyond the within-call
lifetime. Two different EEs being persisted in two transactions get
different caches. Survives intact in `EeWriteService`. Document the
hashCode contract dependency in a Javadoc.

### 5.3 BAD persistence ordering: BAs first, then BAD

`persistBioAssayDimension` (line 359) explicitly walks `bioAssayDimension.getBioAssays()`
and persists each BA *before* calling `bioAssayDimensionDao.findOrCreate(bad)`.
This is required because (a) BAD is `mutable="false"` with no cascade
(verified `BioAssayDimension.hbm.xml:6,7`), and (b) `BusinessKey.find(s, BAD)`
matches on persistent BA IDs (lines 597–612 of `BusinessKey.java`),
so the BAs must have IDs assigned before the BAD lookup runs. **Cannot
be deleted, cannot be reordered.** This is the load-bearing piece of
the BioAssay walk.

### 5.4 ArrayDesign cache → `session.load` rehydration

`fillInBioAssayAssociations` (line 237) does
`session.load(ArrayDesign.class, arrayDesignUsed.getId())` after
fetching from the cache. This is because the cache's AD is the entity
returned by a *prior* `prepare()` transaction; when this transaction
opens, the cache's AD is detached. Hibernate's `load()` returns a proxy
attached to the current session. Without this, the eventual
`expressionExperimentDao.create(ee)` cascade would throw
`NonUniqueObjectException` or `LazyInitializationException`. **Cannot
be deleted** — this is what makes the two-transaction prepare/create
flow work.

### 5.5 cascade-override on ExperimentalDesign

(See §2d.) The current code creates ED *without* its EFs (line 465–468)
to "avoid premature cascade", then re-attaches and updates. With ACL
listener-driven maintenance this should become a single
`session.persist(ee)` that cascades through ED→EF→FV.
**Open question 1** (see §9) — needs explicit black-box test before
deleting.

### 5.6 No BioMaterial dedup-by-name in baseline

The roadmap-mentioned "BioMaterial dedup by name within an EE" quirk
(commits `0795755c` "prevent addition of samples with the same name"
and `1d6d8c45` "orphan protection") is **not** in our baseline
`a1ca482301`. Both are on the upstream `development` branch
(`OganM`, April 2026). When this work eventually merges back into
development, the new `EeWriteService.create` will need to absorb that
dedup logic in `fillInBioAssayAssociations` / `persistBioMaterial`.
**Action**: track on the open-question list; coordinate the merge
direction with Paul before the chunk-2 cutover.

### 5.7 Audit-trail wiring — already not our concern

The `AuditAdvice` (`gemma-core/.../security/audit/AuditAdvice.java`)
listener wires `AuditTrail` priming on `Auditable` entity inserts via
the same Hibernate event mechanism the ACL cutover uses. The persister
does not directly touch `auditTrail` — `cascade="all"` on the EE
mapping (`Investigation.hbm.xml:13`) handles the cascade and the
listener creates the trail. Nothing to migrate.

---

## 6. Proposed chunked migration order

### Chunk E1 — lift cascade-override + delete redundant FlushMode wrap (~0.5 session)
1. Verify (with new `EeWriteServiceTest`) that `processExperimentalDesign`'s
   "withhold + cascade-override" gymnastics can be replaced by a single
   `session.persist(ee)` + ACL-listener verification.
2. Replace the body of `processExperimentalDesign` with a thin
   `experimentalDesignDao.create(ed)` (cascade does the rest), or
   delete the method outright if `EE.experimentalDesign` cascade=all
   covers it.
3. Delete the explicit `factorValueDao.create` loop on lines 491–501.
4. Delete the inner `setHibernateFlushMode(MANUAL)` block on
   `ExpressionPersister.persist` (keep the AbstractPersister wrap).

This is **doable inside the existing persister class** — no new service
yet. Validates the FlushMode + ACL hypothesis before further structural
change. Tests: `AclAdviceTest`, `AclAuthorizationTest`,
`ProcessedExpressionDataVectorServiceTest`, plus the standard EE-load
sweep.

### Chunk E2 — extract `EeWriteService` skeleton (~0.5 session)
1. Create `gemma-core/.../service/expression/experiment/EeWriteService.java`
   (interface) + `EeWriteServiceImpl` (impl). Define
   `create(EE, ArrayDesignsForExperimentCache)` matching the contract
   of the current `persisterHelper.persist(EE, cache)`.
2. Move trivial methods (5 in bucket 2a) directly from
   `ExpressionPersister` into `EeWriteServiceImpl` as private helpers.
3. Keep the old `ExpressionPersister.persist` as a *delegate* to the
   new service. No caller changes yet.

### Chunk E3 — relocate the rest of the persister body (~0.7 session)
4. Move `fillInBioAssayAssociations`, `persistBioAssay`,
   `persistBioAssayDimension`, `persistBioMaterial`,
   `persistFactorValue`, `processBioAssays`, `checkExperimentalDesign`,
   `fillInExpressionExperimentDataVectorAssociations` and the
   per-vector helpers into `EeWriteServiceImpl`.
5. Inline the public `persistExpressionExperiment` as
   `EeWriteServiceImpl.create`'s body.

### Chunk E4 — cut over the two hot callers (~0.3 session)
6. `GeoServiceImpl:276` → `eeWriteService.create(ee, c)`.
7. `SimpleExpressionDataLoaderServiceImpl:97` → same.

### Chunk E5 — delete ExpressionPersister + migrate tests (~0.5 session)
8. `PersistentDummyObjectHelper.getTestExpressionExperiment*` →
   `eeWriteService.create` (or `ExperimentFactory` if that fixture
   migration has landed).
9. Delete `ExpressionPersister.java`. Tighten the persister chain in
   `PersisterHelperImpl` (or delete it outright if no other classes
   depend on the chain — coordinate with `GenomePersister` recce
   chunks).

**Total: 2.5 agent-sessions.** Slightly under the roadmap's "~2
agent-sessions" estimate because the FlushMode+ACL hypothesis collapse
in Chunk E1 buys back the ED-cascade complexity.

---

## 7. Risk callouts

### R1 — FlushMode/ACL hypothesis wrong (Chunk E1)
Risk: ACL listener doesn't fire on cascade-inserted FVs, leading to
silent ACL gaps on FactorValues in newly-loaded EEs. Detection: the
`AclAdviceTest` sweep + a new explicit "EE with EFs has ACLs on every
FV" assertion. If broken, fall back to keeping the explicit
`factorValueDao.create` loop (which means keeping the inner FlushMode
wrap too, since that's why the loop exists). The hypothesis is grounded
in the cutover commit's claim "23/23 of ACL test sweep pass" but EE+EF
isn't called out specifically — verify it.

### R2 — BAD reuse fails across two EEs in the same JVM tx (Chunk E3)
Risk: the per-`Caches` BAD cache is keyed by hashCode. If two
concurrent EE loads share a session (unusual but possible in CLI
batch loaders), one's cache pollutes the other. **Pre-existing**, not
introduced by the migration — but the new service should re-allocate
`Caches` per `create()` call. Mitigation: make `Caches` a
method-local in `create`, never a field.

### R3 — `ArrayDesignsForExperimentCache` lifecycle (Chunk E4)
Risk: callers may forget that `prepare()` runs in a separate tx and
the returned cache must be passed to `create()` *within an outer tx*
that opens after `prepare()` commits. Mitigation: keep the existing
two-method API (`prepare` + `create`); document the lifecycle in a
class-level Javadoc on `EeWriteService`. Don't merge the two into a
single `createAndPrepare` — it would tie the user to a single huge tx.

### R4 — `BioMaterial` dedup merge conflict (Chunk E3)
Risk: when upstream `development` merges in (with commits `0795755c` +
`1d6d8c45`), the new `EeWriteServiceImpl` needs to absorb the
dedup-by-name + orphan-protection logic. **Mitigation**: do the
upstream merge first, before Chunks E2/E3, so the migration starts
from the development-tip behaviour. Or: explicitly port the logic
during Chunk E3 and add tests for both cases.

### R5 — `processedExpressionDataVectors` not in the create path
Risk: only `rawExpressionDataVectors` are filled in by
`fillInExpressionExperimentDataVectorAssociations` — processed vectors
are computed downstream by `processedExpressionDataVectorService` after
the EE is persisted. The cascade does pick them up if present, but our
test scope must not assume they exist at create time. Pre-existing
contract; don't break it.

### R6 — Vector volume + memory at flush time
The single `s.flush()` at the end of `persist` writes all
RawExpressionDataVectors for the EE in one shot. For a large EE
(~50k probes × 100 samples = 5M vector rows), this hits MySQL hard.
This is the *real* "hot write path" mentioned in the roadmap.
Pre-existing behaviour; not affected by the migration. If we ever
want to chunk the flush, that's a separate JdbcTemplate / batch-insert
project independent of the persister retirement.

---

## 8. Effort estimate

**~2.5 agent-sessions total.**

| Chunk | Risk | Effort |
|---|:-:|---|
| E1 — FlushMode/ACL collapse + redundant wrap delete | medium-high | 0.5 |
| E2 — Skeleton `EeWriteService` + trivial methods | low | 0.5 |
| E3 — Relocate body + BAD/BA helpers + sanity checks | medium | 0.7 |
| E4 — Cut over `GeoServiceImpl` + Simple loader | low | 0.3 |
| E5 — Delete persister + migrate tests | low | 0.5 |

The risk-5 rating from the roadmap is correct: the hottest write path
(every GEO load + SimpleEE upload) routes through this class, and the
graph-build orchestration has at least three pieces of load-bearing
non-obvious logic (BAD ordering, AD cache rehydration, FlushMode wrap).
But none of those *change* in this migration — they get relocated to
a new home with the same semantics, plus one collapse (ED cascade
override) gated on a clean test.

---

## 9. Open questions for Paul

1. **ED-cascade collapse.** Does ACL listener-driven maintenance
   correctly fire `EntityInsert` for every cascade-inserted
   `FactorValue` + `ExperimentalFactor` + `ExperimentalDesign` during
   an EE create? The cutover commit (`21e4fc41`) claims 23/23 ACL test
   sweep passes but doesn't break down EE+EF specifically. Chunk E1
   needs an explicit pre-cutover test. If the answer is "no", we keep
   the explicit `factorValueDao.create` loop *and* keep the inner
   FlushMode wrap, and the net win shrinks from ~80 LoC to ~20 LoC.

2. **`BioMaterial` dedup merge direction.** When does upstream
   `development` (with `0795755c` + `1d6d8c45`) merge back into our
   phase-3 branch? If "soon", do the merge before Chunk E2 so the new
   service starts from development-tip behaviour. If "after Phase 3
   lands", port the logic during Chunk E3.

3. **`prepare()` two-transaction contract.** The current persister
   warns *"Consider doing the 'prepare' step in a separate
   transaction"* (line 141) but `SimpleExpressionDataLoaderServiceImpl`
   ignores the warning and does it in-line. Should the new
   `EeWriteService` enforce this (two `@Transactional` methods, fail
   loudly if called from inside an existing tx) or preserve the
   permissive current behaviour?

4. **`Caches` per call.** Confirm the new service should allocate
   `Caches` per `create()` invocation (Risk R2). The current persister
   already does this via `Caches.empty(cachedArrays)` in
   `persist(EE, ArrayDesignsForExperimentCache)`, but the chain
   passing makes it less obvious. The new service makes it explicit.

5. **Removing `PersisterHelper` interface.** With both `ExpressionPersister`
   and `GenomePersister` retired, the only remaining consumers of the
   `Persister`/`PersisterHelper` interfaces are `CommonPersister`-typed
   things already migrated and the dispatch chain itself. Schedule the
   final interface deletion (roadmap step 8) right after Chunk E5 of
   this plan + the equivalent terminal chunk of the Genome plan.

---

## Appendix: source pointers

- **Subject:** `gemma-core/src/main/java/ubic/gemma/persistence/persister/ExpressionPersister.java` (507 LoC)
- **Base class:** `gemma-core/src/main/java/ubic/gemma/persistence/persister/AbstractPersister.java` (FlushMode wrap on lines 97–135)
- **EE mapping:** `gemma-core/src/main/resources/ubic/gemma/model/analysis/Investigation.hbm.xml` (cascade declarations on lines 13, 95, 102, 107, 111, 123, 128, 135, 141, 148)
- **BAD mapping:** `gemma-core/src/main/resources/ubic/gemma/model/expression/bioAssayData/BioAssayDimension.hbm.xml` (mutable="false", no cascade)
- **BAD BK matcher:** `gemma-core/src/main/java/ubic/gemma/persistence/util/BusinessKey.java` lines 586–620
- **ACL listener cutover:** commit `21e4fc41` (2026-05-18)
- **FlushMode origin:** commit `4df4b89e` (2012-05-20, bug 2888)
- **FlushMode COMMIT→MANUAL upgrade:** commit `de9fa4d2` (2022-11-09)
- **Hot callers:** `gemma-core/src/main/java/ubic/gemma/core/loader/expression/geo/service/GeoServiceImpl.java:276`, `gemma-core/src/main/java/ubic/gemma/core/loader/expression/simple/SimpleExpressionDataLoaderServiceImpl.java:97`
- **Roadmap:** `PERSISTER_REPLACEMENT_ROADMAP.md` (commit `753c258481`)
- **Sibling recce:** `GENOMEPERSISTER_MIGRATION_PLAN.md` (commit `baf8a6c919`)
