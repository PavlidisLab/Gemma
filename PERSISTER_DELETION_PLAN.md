# PersisterHelper retirement: deletion + dispatch-facade plan

**Date:** 2026-05-18
**Branch baseline:** `phase2-acl-migrate` HEAD `08e760bdaf`
**Status:** plan only -- no code changed in this commit.
**Predecessor:** `PERSISTER_REPLACEMENT_ROADMAP.md` (commit `753c258481` on
`worktree-persister-recce`).

This document picks up where the original roadmap left off. Multiple persister
branches have landed find-or-create rewires and write-service extractions; the
question now is: how do we finally delete the persister classes? Two strategies
are laid out, with a recommendation at the end.

---

## 1. Current state (per-persister)

State is measured against the `phase2-acl-migrate` baseline plus the
un-merged conversion branches. Nothing below this line has landed on
`phase2-acl-migrate` yet.

| Persister | LoC (base) | Conversion branch | Conversion commit | What landed | Remaining work |
|---|---:|---|---|---|---|
| `AbstractPersister` | 190 | -- | -- | Top of chain; `FlushMode.MANUAL` dance + `Caches` value object. No body conversion needed. | After ACL-listener cutover (already done, Phase 3) the manual-flush dance should be verifiable as redundant; remove on deletion. |
| `Persister` (iface) | 80 | -- | -- | Public surface. | Decide A vs B (see Sec. 2). |
| `PersisterHelper` (iface) | 25 | -- | -- | Subinterface adding `persist(EE, cache)` + `prepare()`. | Same. |
| `PersisterHelperImpl` | 47 | -- | -- | `@Service` bean. AuditTrail priming on `Auditable`. | Move audit-trail priming to a `@PrePersist` listener (Step 2 in original roadmap) OR keep as facade (Strategy B). |
| `CommonPersister` | 211 | `worktree-persister-step2` | `a1ca482301` | All 10 `persistXxx` methods rewired to `BusinessKey.find` + `dao.create` pattern. Logic still in CommonPersister. | Extract methods into per-entity write services OR delete inline (Strategy A). |
| `ArrayDesignPersister` | 143 | `worktree-persister-step3-ad` | `72a2c1016d` | `findOrPersistArrayDesign` uses `BusinessKey.find`; `persistNewArrayDesign` collapsed to single `dao.create` (cascade=all carries CSes / extRefs). | Move into `ArrayDesignService.create` (already exists) OR keep inline. |
| `GenomePersister` 5.1 | 920 | `worktree-genome-chunk-51` | `7a476582e9` | `persistBioSequence`, `persistChromosome` rewired to `BusinessKey.find`. | Methods still inline. |
| `GenomePersister` 5.2 | -- | `worktree-genome-chunk-52` | `200bc90166` | `persistTaxon` rewired to `BusinessKey.find` (inline; was DAO `findOrCreate`). | Methods still inline. |
| `GenomePersister` 5.3 (prep) | -- | `worktree-genome-chunk-53-prep` | `08c73d0bc8` (+ `74e607c32d` quirk-pinning tests) | `GeneWriteService` interface + skeleton impl added with method bodies copied from `GenomePersister` (`updateGene`, `handleGeneProductChangedGIs`, `updateGeneProduct`, `persistGene`). **Not yet invoked.** | 5.4: cut persister methods over to delegate. 5.5: caller cutover (`NcbiGeneLoader`, `ExternalFileGeneLoaderServiceImpl`). |
| `RelationshipPersister` | 80 | `worktree-relationshippersister` | `bff8828d06` | `persistGene2GOAssociation` -> `BusinessKey.find`. `persistExpressionExperimentSet` SKIPPED (no static BK resolver; documented). | Methods still inline. |
| `ExpressionPersister` E1 | 507 | `worktree-expression-chunk-e1` | `0e3e26f694` | `processExperimentalDesign` cascade-override + per-FV create collapsed (depends on Phase 3 ACL listener attaching ACLs from Hibernate insert events). | -- |
| `ExpressionPersister` E2 | -- | `worktree-expression-chunk-e2` | `41f5c981d9` | `EeWriteService` interface + `@Service` impl added; 5 simple find-or-create methods delegate to it (`Compound`, `BioMaterial`, `FactorValue`, `ExperimentalFactor`, `ExpressionExperimentSubSet`). | -- |
| `ExpressionPersister` E3 | -- | `worktree-expression-chunk-e3` | `7060457261` (+ skeleton `cf1a833eb4`) | EeWriteServiceImpl owns 9 DAOs + helper bodies. `ExpressionPersister` is 89 LoC; `persist(EE, cache)` + every `doPersist` arm delegate to `eeWriteService`. **E3 documents the next steps explicitly: E4 = caller cutover, E5 = delete class.** | E4: migrate 26 production callers off `persisterHelper`. E5: delete the chain. |

**Net effect of the in-flight branches if merged together:**
- All persister bodies (except `AbstractPersister` + the two interfaces +
  `PersisterHelperImpl`) become thin delegates to:
  - `EeWriteServiceImpl` (EE/BAD/BM/BA/Compound/EESubSet)
  - `GeneWriteService` (Gene/GeneProduct/BioSequence Gene-side)
  - `CommonPersister` itself for the small `Common` entities (Step 2
    landed only the BK rewiring, not the extraction)
  - `ArrayDesignPersister` itself for `ArrayDesign` (Step 3 landed only
    the cascade collapse, not the extraction)
  - `RelationshipPersister` itself for `Gene2GOAssociation` and
    `ExpressionExperimentSet`
  - Inline `GenomePersister` for `BioSequence`/`Chromosome`/`Taxon`/BLAT
    family (`persistBlatResult`, `persistBlatAssociation` skipped per the
    original roadmap -- explicit create-only).

So the EE half is fully extracted (E2+E3), the Gene half has a skeleton
but no cutover (5.3-prep), and the Common/AD/Relationship halves are
rewired-but-not-extracted.

---

## 2. Two strategies

### Strategy A: delete all persisters, callers move to per-entity write services

Mirror the EeWriteService model across every persister.

**Surface after migration:**
- `EeWriteService` (already exists on E2/E3) -- owns EE/BAD/BM/BA/Compound/EESubSet
- `GeneWriteService` (skeleton exists on 5.3-prep) -- owns Gene/GeneProduct/BioSequence/Taxon/Chromosome/BLAT
- `ArrayDesignService.create(ArrayDesign)` (already exists; absorb the
  ArrayDesignPersister logic)
- `CommonPersister` content split into:
  - `BibliographicReferenceService.findOrCreate` (already exists)
  - `ContactService` / `PersonService` -- thin wrappers over DAOs
  - `UnitService`, `ProtocolService`, `QuantitationTypeService`,
    `ExternalDatabaseService` -- where they exist already, just call them;
    where they don't, add thin services
  - `AuditTrailService` already owns audit trails
  - `DatabaseEntryService` for `DatabaseEntry`
- `Gene2GOAssociationService.create(Collection)` -- already exists
- `ExpressionExperimentSetService.create` -- already exists

**Migration order (continuation of the original roadmap step numbers):**
1. **Merge in-flight branches in dependency order.** See Sec. 5.
2. **Step E4 (already named in commit `7060457261`):** Migrate 26 production
   callers off `persisterHelper.persist(...)` to the appropriate write service.
3. **Step E5 (already named in commit `7060457261`):** Delete the persister
   chain leaf-up (Relationship -> Expression -> ArrayDesign -> Genome ->
   Common -> Abstract -> Persister/PersisterHelper interfaces ->
   PersisterHelperImpl).
4. **AuditTrail priming:** Move from `PersisterHelperImpl.doPersist` to a
   `@PrePersist` JPA listener on `Auditable` OR a one-line `entity.setAuditTrail(...)`
   inside each `EeWriteServiceImpl.create` / `GeneWriteServiceImpl.upsert`.
   Listener is cleaner.
5. **Test fixture migration carries the test-side load** (Phase 3
   `ExperimentFactory` work in flight). No new test-side migration needed
   for persister retirement.

**Pros:**
- Per-entity write services are the eventual target architecture (already
  matches `EeWriteService`).
- Each call site reads as "what is this code doing": `geneService.upsert(g)`
  not `persisterHelper.persistOrUpdate(g)`.
- Fully eliminates the `Persister` hierarchy -- no facade left to maintain.
- Generic `persist(Object)` dispatch is gone; type errors caught at compile
  time.

**Cons:**
- 26 production caller migrations need to know which write service to call.
  Most are trivial (BibRef -> BibRefService) but a few are subtle:
  `DifferentialExpressionAnalysisHelperServiceImpl` persists a `BioAssaySet`
  whose runtime type is `ExpressionExperiment` or `ExpressionExperimentSubSet`
  -- needs a runtime instance check at the call site.
- Bulk-collection calls (`NCBIGene2GOAssociationLoader` passing
  `Collection<Gene2GOAssociation>` to `persist`) need a list-shaped overload
  on each write service, or the caller does the loop. Mild boilerplate.
- gemma-web has 5 call sites in walking-dead controllers; per project memory
  they're being replaced by `gemma-curation-ui`. Either patch (1 hour) or
  carve out gemma-web from the cleanup.

### Strategy B: keep `PersisterHelperImpl` as a thin dispatcher facade

Delete 8 of 9 source files, keep `PersisterHelperImpl` (and the `Persister` /
`PersisterHelper` interfaces) as a dispatch shim that constructs the right
`*WriteService` call for each entity type. Callers don't change.

**Surface after migration:**
- `PersisterHelperImpl` collapses to a single class containing the union of
  all `instanceof` arms, delegating to the write services. No inheritance,
  no `Caches` value object (push that into `EeWriteService` where it's
  actually used).
- All other persister classes deleted.
- Per-entity write services are introduced as in Strategy A, just less
  aggressively (only where the persister body is non-trivial: EeWriteService,
  GeneWriteService).

**Pros:**
- Zero caller-side churn. 26 production + 52 test call sites unchanged.
- `persisterHelper.persist(obj)` remains a useful "I don't care which
  service" dispatch for loader code that genuinely doesn't know the entity
  type until runtime (`NCBIGene2GOAssociationLoader` is borderline; the
  CLI publication-update tools are arguably honest about it).
- Phase 3 deletion is a single drop -- low risk of cascading caller bugs.

**Cons:**
- Keeps a generic-dispatch facade that the original roadmap argued was the
  problem in the first place (loses static typing, hides which service
  owns which entity, makes the dependency graph fuzzy).
- The `Caches` value object has to live somewhere; either it stays on the
  facade (then the facade carries non-trivial state) or it moves into
  EeWriteService (then non-EE callers can't supply caches). The latter is
  fine in practice -- the only caller that supplies a `Caches` is the
  EE pre-persist service.
- Future cleanups (e.g. removing `FlushMode.MANUAL`) still need to live
  somewhere; PersisterHelperImpl becomes a small but non-trivial class.

---

## 3. Recommendation: Strategy A with a small B-flavoured concession

**Recommend A.** The whole point of the retirement is to make ownership
of each write path explicit. Keeping a `PersisterHelperImpl` dispatcher
preserves the very thing that motivated retirement (anonymous polymorphic
dispatch hiding ownership).

**One concession from B:** for the **5 gemma-web call sites**, do not
migrate them. Per project memory gemma-web is walking dead and being
replaced by `gemma-curation-ui`. Patching them is throw-away work. Two
options for handling them during the deletion window:

(a) **Bridge bean.** Leave a tiny `@Service("persisterHelper")` bean in
gemma-web's Spring config that exposes only the `persist(BibliographicReference)`
and `persist(BioMaterial)` methods those controllers use, delegating
through to `BibliographicReferenceService` / `BioMaterialService`. Survives
until gemma-web is deleted.

(b) **Carve gemma-web out of Phase 3 deletion.** Leave PersisterHelperImpl
+ ExpressionPersister + CommonPersister + their bean wiring intact ONLY
to satisfy gemma-web. When gemma-web is retired, do a single sweep to
remove them too. Cleaner from gemma-core's perspective but extends the
persister chain's lifetime by 1-2 months.

Either is fine. (a) makes gemma-core fully clean today; (b) defers a
small amount of work. Picking (a) lets the post-deletion gemma-core
verify "zero `extends *Persister`, zero `import ubic.gemma.persistence.persister.*`"
as the acceptance criterion.

---

## 4. Migration order (Strategy A, the recommended path)

This is the concrete sequence that runs after this plan is approved.
Steps assume each prior step has merged into `phase2-acl-migrate` and
green-CI'd on a focused integration test.

### 4.1 Prerequisite -- merge the in-flight branches

In topological order, with conflict checks at each step:

1. `worktree-bk-consolidation` (commit `ffe6b0293e`) -- lifts
   Chromosome/QT/BAD BK lookups into `BusinessKey`. Original-roadmap
   Step 1.
2. `worktree-persister-step2` (`a1ca482301`) -- CommonPersister BK rewire.
3. `worktree-persister-step3-ad` (`72a2c1016d`) -- ArrayDesignPersister
   collapse.
4. `worktree-genome-chunk-51` (`7a476582e9`) -- GenomePersister BS/Chrom.
5. `worktree-genome-chunk-52` (`200bc90166`) -- GenomePersister Taxon.
6. `worktree-relationshippersister` (`bff8828d06`) -- RelationshipPersister
   G2GO.
7. `worktree-expression-chunk-e1` (`0e3e26f694`) -- ExpressionPersister
   cascade collapse.
8. `worktree-expression-chunk-e2` (`41f5c981d9`) -- EeWriteService skeleton
   + 5 delegated methods.
9. `worktree-expression-chunk-e3` (`7060457261`) -- ExpressionPersister
   collapse to delegate.
10. `worktree-genome-chunk-53-prep` (`08c73d0bc8` + `74e607c32d`) --
    GeneWriteService skeleton + quirk tests.

These are independent enough that 2-10 can be ordered loosely, but
expressionchunk-e3 depends on e2 depends on e1, and genome-53-prep
depends on 51+52. The bk-consolidation step is a true prerequisite for
everything.

### 4.2 Step 5.4-5.5 -- finish GenomePersister

After 5.3-prep is merged, the GeneWriteService skeleton exists but the
persister methods aren't yet delegating to it.

- **5.4:** Replace `GenomePersister.persistGene` body with `return
  geneWriteService.persistGene(...);`. Same for `updateGene`,
  `handleGeneProductChangedGIs`, `updateGeneProduct`. Persister keeps the
  `doPersist` instanceof dispatch.
- **5.5:** Migrate the 2 production callers
  (`NcbiGeneLoader.persistOrUpdate(gene)`,
  `ExternalFileGeneLoaderServiceImpl.persistOrUpdate(gene)`) to
  `geneWriteService.upsert(gene)`.

### 4.3 Step E4 -- caller cutover (the big one)

Migrate the 26 production callers. Target service per call site:

| Caller | What it persists | Target write-service call |
|---|---|---|
| `ExpressionExperimentPrimaryPubCli` :180 | `BibliographicReference` | `bibliographicReferenceService.findOrCreate(ref)` |
| `PubMedSearcher` :82 | `Collection<BibliographicReference>` | `bibliographicReferenceService.findOrCreate(refs)` (add list overload if missing) |
| `UpdatePubMedCli` :178 | `BibliographicReference` | same as above |
| `DifferentialExpressionAnalysisHelperServiceImpl` :51 | `Protocol` | `protocolService.create(p)` (Protocol is create-only per PP2017 comment in CommonPersister) |
| `DifferentialExpressionAnalysisHelperServiceImpl` :57 | `BioAssaySet` (runtime: EE or EESubSet) | runtime `instanceof` check then `eeWriteService.create(ee, null)` or `eeWriteService.persistSubSet(sub)` |
| `NCBIGene2GOAssociationLoader` :152, :177 | `Collection<Gene2GOAssociation>` | `gene2GoAssociationDao.create(coll)` (DAO already takes a collection) -- no service layer needed for this batch loader |
| `PubMedService` :72 | `Collection<BibliographicReference>` | `bibliographicReferenceService.findOrCreate(refs)` |
| `ArrayDesignProbeMapperServiceImpl` :445 | `BioAssociation` (BlatAssociation) | `blatAssociationDao.create(ba)` -- cascade=all carries BlatResult + GeneProduct via BK resolvers |
| `ArrayDesignSequenceAlignmentServiceImpl` :392 | `Collection<BlatResult>` | `blatResultDao.create(brs)` find-or-create flavour |
| `ArrayDesignSequenceProcessingServiceImpl` :973 | `BioSequence` (persistOrUpdate) | `bioSequenceService.findOrCreate(bs)` (already exists?) or new `geneWriteService.persistOrUpdateBioSequence(bs)` |
| `GeoServiceImpl` :141 | `BioSequence` | `geneWriteService.persistBioSequence(bs)` |
| `GeoServiceImpl` :208 | `Collection<ArrayDesign>` | `arrayDesignService.findOrCreate(ads)` (already in service) |
| `GeoServiceImpl` :276 | `ExpressionExperiment` + cache | `eeWriteService.create(ee, c)` (this is the E3-named target) |
| `GeoServiceImpl` :384 | `BibliographicReference` | `bibliographicReferenceService.findOrCreate(pub)` |
| `SimpleExpressionDataLoaderServiceImpl` :97 | `EE` + prepared cache | `eeWriteService.create(ee, eePrePersistService.prepare(ee))` |
| `ExternalFileGeneLoaderServiceImpl` :144 | `Gene` (persistOrUpdate) | `geneWriteService.upsert(gene)` (see 5.5 above) |
| `NcbiGeneLoader` :191 | `Gene` (persistOrUpdate) | same |
| `TaxonLoader` :81 | `Taxon` | `taxonService.findOrCreate(taxon)` |
| `ExpressionExperimentPrePersistServiceImpl` :191 | `BioSequence` | `geneWriteService.persistBioSequence(bs)` |
| `ExpressionExperimentPrePersistServiceImpl` :262 | `ArrayDesign` | `arrayDesignService.findOrCreate(ad)` |
| `BibliographicReferenceController` (gemma-web) :174 | `BibliographicReference` | **bridge bean** (per Sec. 3 concession) |
| `ExpressionExperimentController` (gemma-web) :1211 | `BioMaterial` | bridge bean |
| `ExpressionExperimentController` (gemma-web) :1643 | `BibliographicReference` | bridge bean |
| `ExpressionExperimentController` (gemma-web) :1684 | `BibliographicReference` | bridge bean |
| `ExpressionExperimentEditController` (gemma-web) :835 | `BioMaterial` | bridge bean |

**Note on counts:** 21 gemma-core/cli call sites + 5 gemma-web sites = 26.
The original recce's 26 included `ArrayDesignSequenceProcessingServiceImpl`
+ `NcbiGeneLoader` + `ExternalFileGeneLoaderServiceImpl` which use the
`persistOrUpdate` flavour (3 sites). Total verified by `grep -rn
'persisterHelper\.\(persist\|update\)' --include='*.java' gemma-core/src/main
gemma-cli/src/main gemma-web/src/main` = 26.

For each migration: change the field type from `PersisterHelper` to the
target service interface; rename the field; update the call. None of
these is a deep change -- the wiring is already in place because every
target service is already a `@Service` in the same Spring context.

### 4.4 Test caller cutover

Per the original recce: 52 test sites concentrated in
`PersistentDummyObjectHelper` + `BaseSpringContextTest`. The Phase 3
`ExperimentFactory` work (agent `a719cad6c20a655be`) replaces these as a
side effect of fixture modernization. **Plan: do not touch test sites
directly; let the in-flight factory migration absorb them.** Track the
last `persisterHelper` test-call site as a Phase 3 fixture-migration
acceptance criterion.

If the fixture work doesn't finish before Step E5 is otherwise ready, an
escape hatch: keep `PersisterHelperImpl` alive as a test-scope bean (move
its `@Service` to `@Service` + `@Profile("test")` or expose it only in
the test Spring context). The main code stops depending on it; only
legacy tests do.

### 4.5 Step E5 -- delete the chain

Leaf-up:

1. Delete `RelationshipPersister.java` (80 LoC).
2. Delete `ExpressionPersister.java` (89 LoC at E3; was 507).
3. Delete `ArrayDesignPersister.java` (143 LoC, slightly shorter after
   step3-ad).
4. Delete `GenomePersister.java` (920 LoC; thinned to delegate by 5.4).
5. Delete `CommonPersister.java` (211 LoC; could shrink if extraction
   happens, but acceptable to delete as-is after E4 since no callers
   remain).
6. Delete `AbstractPersister.java` (190 LoC).
7. Delete `Persister.java`, `PersisterHelper.java`,
   `PersisterHelperImpl.java`.
8. Move `ArrayDesignsForExperimentCache.java` out of the
   `persistence.persister` package (it's not a persister; it's a value
   object) and into `persistence.service.expression.experiment` next to
   `ExpressionExperimentPrePersistService` which owns it.
9. Delete the now-empty package and `package-info.java`.
10. Delete the bridge bean from gemma-web's Spring config when gemma-web
    itself is retired.

Each delete is a single commit. Compile must pass at each step; the order
above guarantees no inheritance dangler.

### 4.6 AuditTrail priming

PersisterHelperImpl's only unique behaviour is priming `AuditTrail` on
`Auditable`s. Two options:

- **Option I (preferred):** JPA `@PrePersist` listener on `Auditable`. One
  file, one annotation, runs automatically. Tested by existing audit-trail
  integration tests.
- **Option II:** explicit `entity.setAuditTrail(AuditTrailDao.create(...))`
  at the top of each `EeWriteServiceImpl.create` / `GeneWriteServiceImpl.upsert`.
  More boilerplate but explicit.

Pick I unless the listener interacts badly with the Phase 3 ACL listener
ordering (test before committing).

### 4.7 `FlushMode.MANUAL` removal

`AbstractPersister.persist(...)` flips `FlushMode.MANUAL` before calling
`doPersist`. The original roadmap flagged this as a paper-over for
ACL-creation races. The Phase 3 ACL listener cutover (already landed,
2026-05-18, see `21e4fc41`) attaches ACLs from Hibernate insert events,
removing the race. **Verify** by running `EeWriteServiceImpl.create`
against gemdtest without the manual-flush wrapper before deleting it.

---

## 5. Caller migration cost (Strategy A)

| Tier | Count | Effort (agent-sessions) | Risk |
|---|---:|---:|:---:|
| `gemma-core` production callers | 18 | 1.0 | 2 |
| `gemma-cli` production callers | 3 | 0.25 | 1 |
| `gemma-web` production callers (via bridge bean) | 5 | 0.25 | 1 |
| Test callers (absorbed by `ExperimentFactory` migration) | 52 | 0.0 (rides on existing work) | 1 |
| **Subtotal: caller cutover** | **78** | **1.5** | -- |
| GeneWriteService 5.4 + 5.5 (persister delegate + 2 callers) | -- | 1.0 | 3 |
| Deletion sweep (Step E5, 7 source files) | 9 files | 0.5 | 2 |
| AuditTrail listener (Sec. 4.6) | 1 file | 0.5 | 2 |
| FlushMode.MANUAL removal + verification | -- | 0.25 | 2 |
| **Total** | -- | **~3.75** | -- |

That's on top of the ~7 agent-sessions already invested in the in-flight
conversion branches. Cumulative effort: ~11 agent-sessions.

For Strategy B the caller-cutover cost vanishes (0 sessions) but is
replaced by ~0.5 session writing the dispatcher facade -- net ~1
session saved at the cost of carrying the facade indefinitely. Not
worth it.

---

## 6. Risk callouts

1. **`FlushMode.MANUAL` invariants.** The persister chain flips flush
   mode for the duration of `persist(...)`. If callers had implicit
   assumptions about flush ordering, the new write services have to
   replicate that or break those assumptions cleanly. Mitigation: run
   the EeWriteServiceImpl smoke tests without flush-mode flipping
   *before* the AbstractPersister deletion lands.

2. **`Caches` lifetime contract.** The `Caches` value object is built per
   call to `persist(...)`. Some callers (`GeoServiceImpl`) pass a
   pre-populated `ArrayDesignsForExperimentCache`. The EeWriteService.create
   contract must preserve this -- the cache lives for the duration of
   *one* EE persist; not across.

3. **GenomePersister `updateGene` / `handleGeneProductChangedGIs` business
   logic.** This is the highest-risk transfer because it encodes a decade
   of NCBI screw-up handling (drosophila bicistronic genes, GI rotation,
   gene-history merges). 5.3-prep landed quirk-pinning tests
   (`74e607c32d`) -- these MUST pass after the persister-side cutover in
   5.4. If they don't, do not delete `GenomePersister`.

4. **`ArrayDesignsForExperimentCache` package move.** Step E5.8 moves a
   class to a different package, which is binary-incompatible with any
   serialized form. Verify no Hibernate cache layer / Quartz job / RPC
   payload references it by package name before moving.

5. **Hidden `PersisterHelper` injections.** Grep `@Autowired.*Persister`
   AND `<bean.*Persister` (XML wiring) before deletion. Any missed
   injection point causes Spring context start to fail. Acceptance: zero
   compile errors AND zero startup errors against gemdtest after Step
   E5.7 completes.

6. **AuditTrail listener ordering vs ACL listener.** Both run on
   Hibernate events. The audit trail must exist before the ACL listener
   touches the entity; verify event ordering when adding the `@PrePersist`
   listener.

7. **Test-side gap.** If `ExperimentFactory` migration stalls and Step
   E5 lands first, ~52 test sites break simultaneously. Pre-flight
   check before E5: zero compile errors in `gemma-core/src/test` with
   the persister classes removed. If non-zero, gate E5 on fixture-
   migration completion or accept the test-scope bean escape hatch
   (Sec. 4.4).

---

## 7. Estimated total effort

| Phase | Work | Sessions |
|---|---|---:|
| Already done | Steps 1 + 2 + 3 (BK lift, CommonPersister, AD, Genome 5.1/5.2/5.3-prep, Relationship, Expression E1/E2/E3) | ~7 (sunk) |
| Step 5.4 + 5.5 | GenomePersister delegate + 2 caller cutover | 1.0 |
| Step E4 | 21 gemma-core + cli caller cutover | 1.0 |
| Step E4 web | Bridge bean for gemma-web (5 sites) | 0.25 |
| AuditTrail listener | JPA `@PrePersist` for `Auditable` | 0.5 |
| FlushMode.MANUAL removal + verification | -- | 0.25 |
| Step E5 | Delete 7 source files + ArrayDesignsForExperimentCache repackage | 0.5 |
| **Remaining total** | -- | **~3.5** |

Realistic calendar: 1 week of focused agent-sessions to clear the
remaining 3.5 sessions, assuming the in-flight branches merge cleanly
into `phase2-acl-migrate` (they were all branched from the same baseline,
so conflicts should be confined to Maven dep tweaks and unrelated test
files).

---

## 8. Open question to Paul

**The core question: Strategy A (per-entity write services, delete all
persisters) vs Strategy B (keep PersisterHelperImpl as dispatcher
facade)?**

The roadmap above assumes A with the gemma-web bridge-bean concession.
Switching to B reduces caller-cutover effort to ~0 sessions but keeps a
generic `Object`-dispatching facade in the codebase indefinitely. The
recommendation is A; please confirm or correct.

**Secondary questions** (carried forward from the original recce,
restated here for closure):

a. **AuditTrail priming via `@PrePersist` listener vs explicit
   write-service step?** Listener is cleaner and matches the Phase 3 ACL
   listener pattern; recommend listener (Option I).

b. **Does the Phase 3 ACL listener cutover make `FlushMode.MANUAL`
   redundant?** Highly likely yes -- ACLs no longer rely on the explicit
   create-per-entity pattern that the manual flush protected. Verify on
   gemdtest before deletion.

c. **gemma-web bridge bean vs carve-out?** Recommend bridge bean
   (Sec. 3a). Defer no work; let gemma-web walk its own dead path.

d. **`ArrayDesignsForExperimentCache` package move.** Cosmetic but
   improves locality. Skip if any external caller exists that we don't
   control; otherwise do it as part of E5.

---

## 9. Acceptance criteria for "persisters fully deleted"

- [ ] Zero source files matching `find . -name '*Persister*.java' -path
  '*/src/main/*'` (excluding `ArrayDesignsForExperimentCache.java` which
  has been repackaged).
- [ ] Zero matches for `persisterHelper\.` in `gemma-core/src/main`,
  `gemma-cli/src/main`, `gemma-web/src/main` (bridge bean exception in
  gemma-web until gemma-web retirement).
- [ ] Zero matches for `extends.*Persister` in `gemma-core/src/main`.
- [ ] Zero matches for `<bean.*Persister` in `gemma-core/src/main/resources`.
- [ ] `mvn verify` green against gemdtest.
- [ ] Smoke: one full GEO import via `LoadExperimentCli` against gemdtest
  produces an identical row count to the pre-deletion baseline.
- [ ] Smoke: one full NCBI gene reload via `NcbiGeneLoader` against
  gemdtest produces an identical row count (and identical
  `updateGene`-quirk handling per the 5.3-prep quirk tests).
- [ ] `notable_cases.md` updated with the deletion case + any
  surprises found during E4/E5.
