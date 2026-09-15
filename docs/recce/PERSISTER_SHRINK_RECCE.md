# PERSISTER_SHRINK_RECCE.md — shortest path to retiring `persisterHelper` post-Caches

**Baseline:** `phase2-acl-migrate` @ `0033d7ec0fe5c1a99ce700d57e31f7374e21b34d`
**Context:** `Caches` POJO deleted (`199ee5b840`); all per-call caches are now explicit `Map<KEY, VALUE>` parameters threaded through helper signatures. `BibliographicReference`, `Contact`, `Person`, `Unit`, `Protocol`, `AuditTrail`, `ExternalDatabase`, `DatabaseEntry`, `QuantitationType` already lifted to write services / DAO-level `findOrCreate`. Audit-trail priming moved to `AuditTrailEventListener` (`PersisterHelperImpl` now empty subclass).

This recce supersedes the per-step planning in `PERSISTER_REPLACEMENT_ROADMAP.md` and `PERSISTER_DELETION_PLAN.md` with a smaller, post-Caches view: now that the cache POJO is gone, each remaining `persistXxx` is already a self-contained "lookup-by-BK, else create" with all state passed in by parameter. The chain is no longer carrying any hidden state — only polymorphic dispatch.

---

## 1. File inventory (12 files, 2,636 LOC)

| File | LOC | Responsibility (post-Caches) |
|---|---|---|
| `Persister.java` | 80 | Public interface: `persist(T)`, `persist(Collection<T>)`, `persistOrUpdate(T)`. `@Secured("GROUP_USER")`. |
| `PersisterHelper.java` | 25 | EE-specific extension: `persist(EE, ArrayDesignsForExperimentCache)`, `prepare(EE)`. |
| `PersisterHelperImpl.java` | 45 | Concrete bean (`@Service`). **Empty body** — fully inherits `RelationshipPersister.doPersist`. The audit-trail priming that justified its existence has moved to `AuditTrailEventListener`. |
| `AbstractPersister.java` | 164 | Public `persist/persistOrUpdate` entry points: open Hibernate session, set `FlushMode.MANUAL`, allocate fresh `Map<String, ExternalDatabase>`, call `doPersist`, flush, restore `FlushMode.AUTO`. Default `doPersist`/`doPersistOrUpdate` throw `UnsupportedOperationException`. |
| `CommonPersister.java` | 142 | `doPersist` arms: `User` (throws), `Characteristic` (no-op, cascaded). Helpers: `fillInDatabaseEntry`, `persistExternalDatabase`, `persistDatabaseEntry`, `persistBibliographicReference`. **Helpers are `protected`** and called from `EeWriteServiceImpl` + `GenomePersister` + `ArrayDesignPersister`. |
| `GenomePersister.java` | 974 | `doPersist` arms: `Gene`, `GeneProduct`, `BioSequence`, `Taxon`, `BioSequence2GeneProduct`, `SequenceSimilaritySearchResult`, `Chromosome`. `doPersistOrUpdate` arms: `BioSequence`, `Gene`, `GeneProduct`. **Largest remaining body of logic** — NCBI gene update, gene-product GI reconciliation, BlatResult/BlatAssociation, BioSequence updates, chromosome dedupe. |
| `ArrayDesignPersister.java` | 161 | `doPersist` arm: `ArrayDesign`. BK-lookup + new-AD persist (composite sequences, BioSequence resolution, design provider, primary taxon). |
| `ExpressionPersister.java` | 166 | `@Deprecated`. Thin delegating shim: `doPersist` arms `EE`, `BioAssayDimension`, `BioMaterial`, `BioAssay`, `Compound`, `EESubSet` all bounce into `EeWriteServiceImpl` via an AOP-unwrap helper. The `persist(EE, cache)` + `prepare(EE)` `PersisterHelper` interface methods also delegate. |
| `RelationshipPersister.java` | 107 | `doPersist` arms: `Gene2GOAssociation`, `ExpressionExperimentSet`. |
| `EeWriteServiceImpl.java` | 640 | **Already a write service** (`@Component`, implements `EeWriteService`). Lives in the persister package only to reach `protected` helpers (`persistTaxon`, `persistExternalDatabase`, `fillInDatabaseEntry`, `persistBibliographicReference`) via an AOP-unwrap of the injected `PersisterHelper`. |
| `ArrayDesignsForExperimentCache.java` | 126 | Per-EE cache populated by `ExpressionExperimentPrePersistServiceImpl` and `GeoServiceImpl`. **Not part of the dispatch chain** — passed in explicitly. Keeps. |
| `package-info.java` | 6 | — |

---

## 2. Live callers vs dead code

### 2.1 External `Persister` / `PersisterHelper` injection sites (13 production files)

Direct field declarations of `Persister` / `PersisterHelper` outside the package:

| File | Type | Call | Entity routed |
|---|---|---|---|
| `core/loader/genome/taxon/TaxonLoader.java` | `Persister` | `persist(taxon)` | Taxon |
| `apps/TaxonLoaderCli.java` | `PersisterHelper` | wires `TaxonLoader` | — |
| `core/loader/genome/gene/ncbi/NcbiGeneLoader.java` | `Persister` | (callsite already cut to `GeneWriteService.upsert`; comment-only ref) | — |
| `apps/NcbiGeneLoaderCLI.java` | `Persister` | wires `NcbiGeneLoader` | — |
| `core/loader/genome/gene/ExternalFileGeneLoaderServiceImpl.java` | `Persister` | `persistOrUpdate(gene)` | Gene |
| `core/loader/association/NCBIGene2GOAssociationLoader.java` | `Persister` | `persist(itemsToPersist)` (x2) | Gene2GOAssociation |
| `apps/NCBIGene2GOAssociationLoaderCLI.java` | `Persister` | wires loader | — |
| `core/loader/expression/arrayDesign/ArrayDesignSequenceProcessingServiceImpl.java` | `Persister` | `persistOrUpdate(sequence)` | BioSequence |
| `core/loader/expression/arrayDesign/ArrayDesignSequenceAlignmentServiceImpl.java` | `Persister` | `persist(brs)` | BlatResult collection |
| `core/loader/expression/arrayDesign/ArrayDesignProbeMapperServiceImpl.java` | `Persister` | `persist(bacs.ba)` | BlatAssociation |
| `core/loader/expression/arrayDesign/ArrayDesignMergeHelperServiceImpl.java` | `Persister` (field `arrayDesignPersiter`) | `persist(result)` | ArrayDesign |
| `core/loader/expression/geo/service/GeoServiceImpl.java` | `PersisterHelper` | `persist(bioSeq)`, `persist(arrayDesigns)` | BioSequence, ArrayDesign |
| `core/loader/expression/cellxgene/CellXGeneDataLoaderServiceImpl.java` | `Persister` | `persist(ee)` | EE — **already a deprecated path; should route to `EeWriteService.create`** |
| `core/analysis/preprocess/SplitExperimentServiceImpl.java` | `Persister` | `persist(split)` | EE (same) |
| `core/analysis/expression/diff/DifferentialExpressionAnalysisHelperServiceImpl.java` | `Persister` | `persist(experimentAnalyzed)` | BioAssaySet (= EE / EESubSet) |
| `service/expression/experiment/ExpressionExperimentPrePersistServiceImpl.java` | `Persister` | `persist(bioSeq)`, `persist(arrayDesign)` | BioSequence, ArrayDesign |
| `core/loader/util/ParserAndLoaderTools.java` | `Persister` (parameter) | utility wrapper | passthrough |

**Distinct routed entity types:** Taxon, Gene, BioSequence, BlatResult, BlatAssociation, Gene2GOAssociation, ArrayDesign, ExpressionExperiment (+ subset/biomaterial/bioassay via internal `ExpressionPersister` dispatch).

### 2.2 Dead-or-trivial code

- `PersisterHelperImpl.java` — empty body. Pure dispatch facade.
- `ExpressionPersister.persist(EE, cache)` and `prepare(EE)` — already `@Deprecated`, only retained for the `PersisterHelper` interface contract; both delegate one line to `EeWriteService` / `ExpressionExperimentPrePersistService`.
- `ExpressionPersister.doPersist` — all 6 arms delegate one line to `eeWriteServiceImpl()` (an AOP-unwrap helper). Pure routing.
- `RelationshipPersister.persistExpressionExperimentSet` — 1 internal call from `doPersist`; no external caller for `Persister.persist(ExpressionExperimentSet)` in production found. Likely test-only — confirm before deletion.
- `GenomePersister.doPersist` arm for `Chromosome` — no external `persist(chromosome)` caller in production. Internal-helper only (called from `persistChromosome`, `fillChromosomeLocationAssociations`, etc.).
- `GenomePersister.doPersist` arm for `BioSequence2GeneProduct` / `SequenceSimilaritySearchResult` — same: only routed internally via `persistBioSequenceAssociations`. No external caller routes raw `BioSequence2GeneProduct` through `Persister.persist`. (`BlatResult` is routed by `ArrayDesignSequenceAlignmentServiceImpl` as a `Collection<BlatResult>`, which hits the `SequenceSimilaritySearchResult` arm via subtype.)
- `AbstractPersister.doPersistOrUpdate` default + `CommonPersister`/`ArrayDesignPersister`/`ExpressionPersister`/`RelationshipPersister` not overriding it — only `GenomePersister.doPersistOrUpdate` actually does work. The `persistOrUpdate` entry point can collapse to a `GenomePersister`-flavoured dispatch with 3 arms.

---

## 3. Shortest path to deletion (4–5 steps)

The dispatch chain is now only doing two things: (a) polymorphic routing by `instanceof`, (b) opening a `FlushMode.MANUAL` window around the call. Both are cheap to inline.

### Step S1 — cut over the 3 still-EE-bound callers to `EeWriteService.create`
Three production callers still go through `persister.persist(ee)` instead of the modern API:
- `CellXGeneDataLoaderServiceImpl` line 123
- `SplitExperimentServiceImpl` line 301
- `DifferentialExpressionAnalysisHelperServiceImpl` line 62

Each is a one-line swap (`persister.persist(ee)` → `eeWriteService.create(ee)`). After this, `ExpressionPersister` has **zero external EE callers** and can be deleted along with the EE / BioMaterial / BioAssay / BAD / Compound / EESubSet arms in `ExpressionPersister.doPersist`. `EeWriteServiceImpl` moves out of the `persister` package to `service.expression.experiment` once it no longer needs the `protected` helpers (Step S3 fixes that).

**Effort:** half-session. Three callsites + delete `ExpressionPersister.java` + delete `PersisterHelper.java` interface + its mention in `PersisterHelperImpl`.

### Step S2 — convert each remaining persister into a `@Component` "WriteService"-shaped helper
For each of `CommonPersister`, `GenomePersister`, `ArrayDesignPersister`, `RelationshipPersister`:
- Promote the `protected` `persistXxx` helpers to `public` on a new concrete `@Component` of the same name (or rename to `*WriteService` / `*Persister` as a leaf bean — naming is bikeshed).
- The `instanceof`-based `doPersist` arm becomes a public method named for the entity (e.g. `persistGene`, `persistBlatResult`, `persistGene2GOAssociation`).
- All helper-to-helper calls inside the chain (e.g. `GenomePersister.persistBlatAssociation` calls `persistGeneProduct`, `persistBioSequence`) stay intra-class.

Cross-class helper calls become bean-to-bean autowires:
- `GenomePersister` → autowire `CommonPersister` (for `persistExternalDatabase`, `fillInDatabaseEntry`, `persistDatabaseEntry`).
- `ArrayDesignPersister` → autowire `GenomePersister` (for `persistTaxon`, `persistBioSequence`) and `CommonPersister`.
- `RelationshipPersister` → autowire `GenomePersister` (for `persistGene`).
- `EeWriteServiceImpl` → autowire `CommonPersister` + `GenomePersister` instead of unwrapping AOP. The AOP-unwrap helpers (`eeWriteServiceImpl()` in `ExpressionPersister`, `persister()` in `EeWriteServiceImpl`) go away.

**Effort:** ~2 sessions, mostly mechanical refactor + chasing autowires through test contexts.

### Step S3 — inline `instanceof` dispatch at the 16 external call sites
At each external `persister.persist(x)` / `persistOrUpdate(x)` site (Section 2.1), replace with the typed bean call:

```java
// Before
persisterHelper.persistOrUpdate( sequence );      // BioSequence
persisterHelper.persist( taxon );                 // Taxon
persisterHelper.persist( itemsToPersist );        // Collection<Gene2GOAssociation>

// After
genomePersister.persistOrUpdateBioSequence( sequence, newXdbMap(), newTaxonMap(), newChromMap() );
genomePersister.persistTaxon( taxon, newTaxonMap() );
for ( Gene2GOAssociation a : itemsToPersist ) relationshipPersister.persistGene2GOAssociation( a, newXdbMap() );
```

The per-call map allocation is the same noise `AbstractPersister.persist` is doing today. Wrap as a small static factory if the noise is annoying (`PersisterCaches.fresh()` returning a record of empty maps), or move the fresh-map allocation inside each new public method.

The `FlushMode.MANUAL` ↔ `FlushMode.AUTO` window in `AbstractPersister.persist` is the only piece of behaviour the dispatcher contributes beyond routing. Each leaf public method needs to either: (a) inherit the flush-mode handling via a shared `@Transactional` wrapper utility, or (b) accept that Spring's `@Transactional` already handles flush-on-commit and the `MANUAL` mode is a legacy guard against partial cascades (the `PERSISTER_DELETION_PLAN.md` calls this out — needs verification; risk callout below).

For collection callers (`NCBIGene2GOAssociationLoader` does `persist(Collection)`), wrap in a small batch method on the target bean.

**Effort:** ~2 sessions across 13 production files + CLI wiring.

### Step S4 — delete the chain
Delete in order:
1. `ExpressionPersister.java` (after S1)
2. `PersisterHelper.java` interface (after S1)
3. `PersisterHelperImpl.java` (after S2 + S3; nothing references it)
4. `AbstractPersister.java` (after S2 promotes leaf beans to `@Component` directly)
5. `Persister.java` interface (after S3 cuts every external injection)

`CommonPersister`, `GenomePersister`, `ArrayDesignPersister`, `RelationshipPersister` survive as `@Component` beans (probably renamed `*WriteHelper` to reflect that they're no longer in a "persister" chain). `ArrayDesignsForExperimentCache` survives unchanged. `EeWriteServiceImpl` moves to `service.expression.experiment` package.

**Effort:** half-session including final test-context cleanup.

### Step S5 (optional) — flatten leaf beans into `*WriteService` proper
The four surviving beans can each merge with their natural sibling write service:
- `CommonPersister.persistExternalDatabase` etc. → `ExternalDatabaseService` (which already has `findOrCreate`).
- `CommonPersister.persistBibliographicReference` → `BibliographicReferenceService.findOrCreate` (already exists).
- `GenomePersister` → `GeneWriteService` (already exists) + new `BioSequenceWriteService` + `TaxonWriteService` + `ChromosomeWriteService` + `BlatResultWriteService`.
- `ArrayDesignPersister` → new `ArrayDesignWriteService` (or merge with `ArrayDesignService`).
- `RelationshipPersister.persistGene2GOAssociation` → new `Gene2GOAssociationWriteService` (or `Gene2GOAssociationService.findOrCreate`).

This is the same target architecture as `PERSISTER_REPLACEMENT_ROADMAP.md` §5 — but it's a *clean-up*, not a prerequisite for deleting `persisterHelper`. Defer.

**Effort:** 2+ sessions, can be done incrementally per entity over the next quarter.

---

## 4. Effort estimate

| Step | Sessions | Risk |
|---|---|---|
| S1 — 3 EE callers to `EeWriteService.create`, delete `ExpressionPersister` + `PersisterHelper` | 0.5 | Low — already deprecated, single-line cutovers |
| S2 — promote 4 abstract classes to `@Component` + autowire | 2.0 | Medium — Spring context wiring, test fixtures |
| S3 — inline dispatch at 13 production caller files + CLI wiring | 2.0 | Medium — touches CLI loaders, GEO import |
| S4 — delete `AbstractPersister`, `Persister`, `PersisterHelperImpl` | 0.5 | Low — by this point no references remain |
| S5 — optional flattening into per-entity `*WriteService` | 2.0+ | Low (cleanup) |
| **Total to "persisterHelper gone"** | **~5.0** | — |
| **Total including S5** | **~7.0+** | — |

Compared to `PERSISTER_DELETION_PLAN.md`'s ~10-session estimate, the Caches lift cut ~50% of the work: the per-call-state plumbing that dominated steps E3/E4 is already done. What's left is dispatcher-removal + caller-cutover, which is mechanical.

---

## 5. Risks

1. **`FlushMode.MANUAL` semantics** — `AbstractPersister.persist` sets `FlushMode.MANUAL` for the duration of the persist, then restores `FlushMode.AUTO`. Comment on the class says this is "for some reason we cannot afford to let Hibernate flush changes to the database until the whole operation is completed." The reason is likely cascade ordering through transient associations (a flush mid-cascade can hit a not-null constraint on a column that's about to be filled in two lines later). **Before deleting `AbstractPersister`, confirm whether each leaf method actually needs this guard, or whether modern `@Transactional` (which flushes on commit) suffices.** Likely a per-method judgement; the GenomePersister update paths are the riskiest (`updateGene` mutates 5+ entities). Mitigation: keep a small `FlushModeGuard` AutoCloseable utility that the leaf methods open at the top of any multi-entity update.

2. **`ExpressionPersister.doPersist`'s `prepare()` synthesis** — when an external caller hits the polymorphic dispatch with a fresh EE (no `ArrayDesignsForExperimentCache`), `ExpressionPersister.doPersist` calls `prepare(ee)` in the same transaction (with a `log.warn` to "consider doing the 'prepare' step in a separate transaction"). The 3 EE callers being cut over in S1 (CellXGene, SplitExperiment, DifferentialExpressionAnalysisHelper) may rely on this synthesis. Verify each callsite either passes a pre-built cache or accepts the in-transaction `prepare`. The modern `EeWriteService.create(ee)` (no-cache overload) already handles this — confirm equivalence.

3. **Cross-class helper calls during S2** — `ArrayDesignPersister.persistNewArrayDesign` calls `persistTaxon` (inherited from `GenomePersister`) and `persistExternalDatabase` (inherited from `CommonPersister`). Promoting these to autowired beans means cycles (ArrayDesign → Genome → Common). Spring handles this fine via setter injection but watch for `@Lazy` requirements. Mitigation: structure the autowires bottom-up (Common is leaf, Genome depends on Common, ArrayDesign on both).

4. **`PersisterHelper.prepare` and `ArrayDesignsForExperimentCache`** — survives the chain deletion. The interface method can move to `ExpressionExperimentPrePersistService` (which already declares an identical `prepare(EE)` method). Two callers reach it through `PersisterHelper`: `GeoServiceImpl.fetchAndLoad` and the `ExpressionPersister.prepare` shim. Both can switch to `ExpressionExperimentPrePersistService` directly. The cache class itself stays.

5. **NCBI gene update path (`GenomePersister.updateGene` + `handleGeneProductChangedGIs`)** — both already `@Deprecated` with the canonical implementation in `GeneWriteService.upsert`. `GenomePersister.doPersistOrUpdate(Gene)` is the only remaining call site. After S3 cuts `ExternalFileGeneLoaderServiceImpl` to `geneWriteService.upsert(gene)`, the `doPersistOrUpdate` Gene arm and its `updateGene` / `handleGeneProductChangedGIs` helpers can be deleted outright (~250 LOC). **Risk: NCBI loader (`NcbiGeneLoader.java:208`) — comment says it already routes to `GeneWriteService.upsert`; confirm before deletion.**

6. **Recursive `persist` calls** — `GenomePersister.persistGene` calls `persistGeneProduct`; `persistGeneProduct` may call `persistGene` if the gene is transient. Inside the chain this is fine (same `this`). Across beans (S2) it's still fine (same bean references). But during the partial-cut state between S2 and S3, external callers might inject the old `Persister` bean and hit a method that delegates to the new typed bean — keep both injections alive until S3 is done.

7. **`Persister.persist(Collection)` semantics** — `AbstractPersister.persist(Collection)` opens *one* `FlushMode.MANUAL` window for the whole batch. After deletion, batch callers (`NCBIGene2GOAssociationLoader` persists ~1000 associations per chunk) need to either: (a) loop and open one window per call (slow), or (b) get a batch-shaped method on the typed bean that preserves the single-window semantics. Cheap to add; flag during S3.

8. **Test fixtures** — `gemma-core/src/test` has many `persisterHelper.persist(...)` calls (junit5-batch9 agent territory). Per `PERSISTER_REPLACEMENT_ROADMAP.md` §3, ~55 test sites. These are *not* doc-only's problem but they block the final delete in S4. Coordinate with junit5-batch9.

---

## 6. Recommendation

Go S1 → S2 → S3 → S4 in that order. Skip S5 until after the next merge to `development`. ~5 sessions of agent work, vs the `PERSISTER_DELETION_PLAN.md` ~10. The Caches lift was the load-bearing simplification; what's left is mechanical.

The single highest-value step is **S1** (half-session): it deletes `ExpressionPersister.java` + `PersisterHelper.java` outright, removes the AOP-unwrap helpers in `EeWriteServiceImpl` and `ExpressionPersister`, and decouples the EE path from the persister chain entirely. Do S1 first as a standalone branch, then re-recce S2–S4 against the further-simplified tree.
