# GenomePersister retirement: migration plan (risk-5 recce)

**Date:** 2026-05-18
**Branch baseline:** `worktree-persister-genome` HEAD `a1ca482301`
(CommonPersister BK-conversion present as the pattern to mimic)
**Status:** recce only — no code changed in this commit.
**Predecessor:** `PERSISTER_REPLACEMENT_ROADMAP.md` (commit
`753c258481`), step 6.

---

## 1. Baseline: LoC + method count

`gemma-core/src/main/java/ubic/gemma/persistence/persister/GenomePersister.java`
— **920 LoC, 22 methods**, all `private`/`protected` (no public surface;
all dispatch enters via `doPersist` / `doPersistOrUpdate` overrides from
the parent chain). Eight `@Autowired` DAO fields: `geneDao`,
`chromosomeDao`, `geneProductDao`, `bioSequenceDao`, `taxonDao`,
`blatAssociationDao`, `blatResultDao`, `annotationAssociationDao`.

Pattern proven in commit `a1ca482301` (CommonPersister): each
`persistXxx(...)` reduces to two lines —
`BusinessKey.find(session, probe)` (or DAO `find()` where it already
wraps BK) followed by `dao.create(probe)` on miss. Five of the resolvers
GenomePersister needs (`Taxon`, `Gene`, `GeneProduct`, `BioSequence`,
`Chromosome`) are *already* in `BusinessKey.java`. No BK lift required
for this persister (unlike step 1 of the overall roadmap, which lifted
Chromosome/QT/BAD for downstream consumers — Chromosome lift is already
done).

---

## 2. Method categorization

### 2a. Simple find-or-create — drops in directly (low risk, 8 methods, ~120 LoC)

These map 1:1 onto the CommonPersister pattern. Each becomes a 4–6 line
method.

| Method | LoC | BK resolver | Notes |
|---|---:|---|---|
| `persistTaxon` | 35 | `BusinessKey.find(s, Taxon)` | Drops the per-call `seenTaxa` map only if we keep the `Caches` plumbing; otherwise trivially drop the cache (Hibernate L1 covers it within a tx). |
| `persistBioSequence` | 12 | `BusinessKey.find(s, BioSequence)` | Cleanest case. The `persistNewBioSequence` walk drops to `session.persist(bs)` once `taxon` + `sequenceDatabaseEntry.externalDatabase` are resolved. |
| `persistChromosome` | 48 | `BusinessKey.find(s, Chromosome)` | Already largely BK-shaped. The hash-keyed cache is per-`Caches`; consider deleting (Hibernate L1 covers within-tx). |
| `persistBlatResult` | 14 | n/a — `create`-only | Comment says "not a regular persist": always-new. Trivially `blatResultDao.create(br)` after resolving `querySequence` / `targetChromosome` / `searchedDatabase`. |
| `persistBlatAssociation` | 12 | n/a — `create`-only | Same pattern. |
| `persistBioSequence2GeneProduct` | 6 | dispatch-only | Pure delegator — delete. |
| `persistSequenceSimilaritySearchResult` | 6 | dispatch-only | Pure delegator — delete. |
| `persistGeneProduct` (simple branch) | 28 | `BusinessKey.find(s, GeneProduct)` | Trivial *only* when the gene is already persistent; otherwise interacts with `persistGene` (see 2c). |

### 2b. Cascade-handled — delete the method (1 method, ~30 LoC)

| Method | Why it can go |
|---|---|
| `fillInGeneProductAssociations` | Walks `physicalLocation.chromosome` + `accessions.externalDatabase`. `ChromosomeFeature.hbm.xml` declares `cascade="all"` on `physicalLocation` and `accessions` (verified lines 24, 38); a JPA cascade from `Gene`/`GeneProduct` to `Chromosome` does the BK-resolution work via the `Chromosome` natural-id path if we add it, OR via an explicit pre-step in `GeneWriteService.upsert`. Likely move logic, not delete outright — see 5.3. |

`fillChromosomeLocationAssociations`, `fillPhysicalLocationAssociations`,
`fillInBioSequenceTaxon`, `persistBioSequenceAssociations` are
walk-helpers that survive in trimmed form inside the new write services
(see 5).

### 2c. NCBI-GI reconciliation — relocate intact (1 method, ~110 LoC)

| Method | LoC | Landing target |
|---|---:|---|
| `handleGeneProductChangedGIs` | 110 | **`GeneWriteService.upsert(Gene)` (new)**. Pure domain logic about NCBI GI rotations — three nested cases (orphan, same-gene duplicate, cross-gene reattach). Zero persistence concerns; takes an attached `Gene` + `Map<gi, transient GeneProduct>` and returns `Collection<GeneProduct>` to remove. Lift verbatim. |

This is the load-bearing 110 LoC that justifies the risk-5 rating.
**Test coverage today: zero in `GenomePersisterTest`.** Before lifting,
write 4 unit tests against the existing private method via reflection
(or extract to a package-private `static` helper first): (1) routine GI
swap; (2) `LUZP6/MTPN`-style bicistronic; (3) duplicated GI within same
gene; (4) cross-gene reattach.

### 2d. `updateGene` + `updateGeneProduct` — distinct semantics, relocate (5 methods, ~250 LoC)

| Method | LoC | Landing target |
|---|---:|---|
| `updateGene` | 190 | `GeneWriteService.upsert(Gene)` private helper. Does the NCBI-ID-change check (with `previousNcbiGeneId` comma-list), the official-symbol/name/aliases swap, the `physicalLocation` walk, and the gene-product reconciliation loop that triggers `handleGeneProductChangedGIs`. |
| `updateGeneProduct` | 22 | `GeneWriteService` private helper. Copy of fields + `addAnyNewAccessions`. |
| `addAnyNewAccessions` | 13 | Same, private helper. |
| `persistOrUpdateGene` | 17 | Becomes `GeneWriteService.upsert(Gene)`'s public entry: load by ID or BK, branch to `create` vs `updateGene`. |
| `persistOrUpdateGeneProduct` | 16 | `GeneWriteService.upsertGeneProduct(GeneProduct)`. Called only from inside `updateGene`/`persistGeneProduct`, not externally — could be inlined. |
| `removeGeneProducts` | 38 | `GeneWriteService` private helper. Cleans BLAT + annotation associations, then removes; relies on `bioSequenceDao.findByAccession` to avoid releasing DB entries that other sequences still reference. |

**These are not persistence patterns; they are gene-curation business
logic.** They have no analogue in `CommonPersister` and the BK-find
conversion is irrelevant to them. They survive as the body of the new
service.

### 2e. `persistGene` (create-new path) — split (2 methods, ~80 LoC)

| Method | LoC | Disposition |
|---|---:|---|
| `persistGene(Gene, Caches)` overload | 2 | Delete (trivial pass-through to 3-arg form). |
| `persistGene(Gene, boolean checkFirst, Caches)` | 74 | Split: the `checkFirst` find moves to `GeneWriteService.upsert`'s entry; the rest (resolve taxon, walk accessions, walk products, cascade-create products, `geneDao.update`) becomes `GeneWriteService.create(Gene)`. The `geneProductDao.create(collection)` workaround — the comment says it exists because cascade doesn't fire auditing — needs to be re-checked against the Phase-3 audit-trail listener cutover (open question 1 in roadmap). If the listener fires for cascade-persisted children, this whole loop reduces to `session.persist(gene)`. |

**Bucket totals:** 8 trivial + 1 cascade-delete + 1 GI-reconciliation +
6 update-flavoured + 2 create-flavoured = **18 methods**. The other 4
(`doPersist`, `doPersistOrUpdate`, two `persistGene` overloads) are
dispatch glue that disappears with the persister chain.

---

## 3. Caller traffic

GenomePersister is reached only via `persisterHelper.persist(...)` /
`persistOrUpdate(...)` — the chain dispatches on `instanceof Gene` etc.
Production sites that traffic a Genome-handled type:

| File | Sites | Entity flavour | Hot? |
|---|:-:|---|---|
| `gemma-core/.../loader/genome/gene/ncbi/NcbiGeneLoader.java:191` | 1 | `Gene` (persistOrUpdate) | **Yes — every NCBI gene-info refresh** (~80k human + 80k mouse + … genes per run, batched). The only consumer of `updateGene` / `handleGeneProductChangedGIs`. |
| `gemma-core/.../loader/genome/gene/ExternalFileGeneLoaderServiceImpl.java:144` | 1 | `Gene` (persistOrUpdate) | Low — admin operation for custom gene lists. Same `updateGene` path. |
| `gemma-core/.../loader/genome/taxon/TaxonLoader.java:81` | 1 | `Taxon` (persist) | Low — admin bootstrap; runs at install/upgrade. |
| `gemma-core/.../persistence/service/expression/experiment/ExpressionExperimentPrePersistServiceImpl.java:191` | 1 | `BioSequence` (persist) | **Yes — every EE/AD prepare()**. Reaches GenomePersister only when an AD has unresolved probe sequences. |
| `gemma-core/.../loader/expression/arrayDesign/ArrayDesignSequenceAlignmentServiceImpl.java:392` | 1 | `Collection<BlatResult>` (persist) | Medium — runs per platform alignment. |
| `gemma-core/.../loader/expression/arrayDesign/ArrayDesignSequenceProcessingServiceImpl.java:973` | 1 | `BioSequence` (persistOrUpdate) | Medium — runs per array-design sequence-attachment. |
| `gemma-core/.../loader/expression/geo/service/GeoServiceImpl.java:141` | 1 | `BioSequence` (persist) | Medium — runs once per probe during GEO import. |
| `gemma-core/.../loader/expression/arrayDesign/ArrayDesignProbeMapperServiceImpl.java:445` | 1 | `BlatAssociation` (persist) | Medium — runs per probe-mapping operation. |

**Top 3 callers by hotness:**

1. **`NcbiGeneLoader`** — the *only* production caller of
   `updateGene` / `handleGeneProductChangedGIs`. Defines the contract
   the new `GeneWriteService` must preserve.
2. **`ExpressionExperimentPrePersistServiceImpl`** — every EE/AD
   prepare. BioSequence-only path; trivial migration.
3. **`GeoServiceImpl`** — every GEO import. BioSequence-only path;
   trivial migration. (Note: `GeoServiceImpl` also touches AD and EE,
   but those don't go through GenomePersister.)

Test sites (out of scope for migration — covered by in-flight fixture
factories): `PersistentDummyObjectHelper`, `GenomePersisterTest`,
`BioSequencePersistTest`, `GeneSearchTest`, `GeneServiceTest`,
`CuratableValueObjectTest`, `ProcessedExpressionDataVectorServiceTest`.

---

## 4. Quirks inventory

### 4.1 Drosophila bicistronic genes (LUZP6/MTPN-style)

**Where:** `updateGene` lines 211–276 + `handleGeneProductChangedGIs`
lines 750–773. Triggered when a transcript is annotated to two genes in
NCBI gene2accession (commented examples: human `LUZP6`/`MTPN`,
drosophila `Lcp65Ab1`/`Lcp65Ab2` for GenBank `BT099970`
GI:289666832→1108657489).

**Contract:** when a found `GeneProduct` is currently owned by
`oldGene` ≠ `newGene`, switch it to `newGene` and remove it from
`oldGene.products`. If `oldGene` ends up empty, log a warning (Hibernate
flush has historically failed here for the bicistronic-during-update
case, with the `--restart` workaround documented in line 257's comment).

**Landing target:** `GeneWriteService.upsert` private helper
`reattachOrphanedProduct(existingGene, existingGp, newGp)`. The
`--restart` workaround is documented (comment-block lines 251–258) but
the underlying flush failure should be retested under Hibernate 6 + the
new listener wiring — it may already be fixed by the
`AbstractPersister.formatEntity` change in commit `63bffbf6fc` (Phase 2:
"tolerates transient/detached entities"). **Open question for Paul.**

### 4.2 NCBI GI rotation

**Where:** `handleGeneProductChangedGIs` whole method (lines 683–792).
Three sub-cases by current owner of the GI being claimed:

- **No current owner** → just update the existing GP's `ncbiGi`. Log
  warn. The common case after an NCBI sequence-version bump.
- **Orphan owner (no gene)** → take the GI; delete the orphan. Log warn.
- **Same-gene duplicate** → drop the outdated GP. Log warn. Caused by
  legacy cruft.
- **Cross-gene owner** → reattach to *this* gene; un-attach from old.
  Log warn. Rare.

**Contract:** the method returns `Collection<GeneProduct>` to remove;
the caller (`updateGene`) finalizes the `existingGene.getProducts()`
removal and invokes `removeGeneProducts` to clean BLAT/annotation
associations.

**Landing target:** `GeneWriteService` private; identical signature.
Side effects on `existingGene.getProducts()` must remain inside the same
transactional unit.

### 4.3 NCBI ID change with previous-ID comma-list

**Where:** `updateGene` lines 113–154. NCBI sometimes merges two
previously distinct gene records (example: `MTUS2-AS1` merged
`LOC728437` + `LOC731614`). The `previousNcbiGeneId` field can carry a
*comma-separated* list of prior IDs; we accept the update if *any* of
them matches our current `existingGene.ncbiGeneId`.

**Contract:** if the new info's `previousNcbiGeneId` is blank, throw
`IllegalStateException` (we refuse to silently overwrite an ID we
didn't expect). If non-blank, split on comma, accept on first match.
Swap: `existingGene.previousNcbiGeneId = existingGene.ncbiGeneId`;
`existingGene.ncbiGeneId = newGene.ncbiGeneId`.

**Landing target:** `GeneWriteService.upsert` first pass. Wholly
self-contained; no DAO calls. Trivial unit-testable.

### 4.4 Probe→Gene mapping edge case (orphan rescue)

**Where:** `persistGene` create-path lines 454–473. When creating a
brand-new gene whose transient `GeneProduct` already exists in the DB
(by BK match), the existing product gets *moved* from its old gene to
the new gene. This is the inverse direction from 4.1.

**Contract:** for each transient `GeneProduct` in the new gene's
`products`, do a BK find; if a persistent match exists, transfer it
(removing from `previousGeneForProduct.products`) onto the new gene
instead of creating a duplicate. Log warn.

**Landing target:** `GeneWriteService.create(Gene)` body. Stays
inline — it's the create-path mirror of `updateGene`'s update-path
reattachment, and the two together are the gene-product-allocation
policy.

### 4.5 The `geneProductDao.create(collection)` cascade workaround

**Where:** `persistGene` lines 481–494, comment "we do a separate
create because the cascade doesn't trigger auditing correctly".

**Contract:** after `geneDao.create(gene)`, explicitly call
`geneProductDao.create(gene.getProducts())` and then `geneDao.update`.
The reason given is that auditing didn't fire on cascade-persisted
children.

**Landing target:** **Retest under Phase-3 listener wiring before
keeping.** If `AuditableEntityListener` (or whatever replaced the AOP
advice) fires on cascade-persisted entities, this whole block is dead
code. If it doesn't, document the reason in `GeneWriteService.create`
and keep the explicit collection-create. **Open question for Paul.**

---

## 5. Proposed migration order (small-and-safe first)

The order intentionally inverts the LoC distribution: the cheap entities
go first to shrink the persister and prove the pattern, then the
gene-graph (high-risk) lands as a single dedicated chunk.

### Chunk 5.1 — BioSequence + Chromosome (low risk, ~1 agent-session)

- `persistBioSequence`, `persistOrUpdateBioSequence`,
  `persistNewBioSequence`, `persistBioSequenceAssociations`,
  `fillInBioSequenceTaxon`, `persistChromosome`,
  `fillChromosomeLocationAssociations`,
  `fillPhysicalLocationAssociations`.
- Rewire `persistBioSequence` to two lines (BK find else
  `bioSequenceDao.create`).
- Move `persistOrUpdateBioSequence`'s field-by-field update logic into
  `BioSequenceService.findOrUpdate(BioSequence)` (new thin method).
- Callers (`GeoServiceImpl`, `ExpressionExperimentPrePersistServiceImpl`,
  `ArrayDesignSequenceProcessingServiceImpl`) move to
  `BioSequenceService.findOrUpdate` / `BioSequenceDao` directly.
- Delete the methods from `GenomePersister`.
- Result: ~250 LoC out, 8 methods gone, 920 → ~670 LoC.

### Chunk 5.2 — Taxon + BLAT-family (low risk, ~0.5 agent-session)

- `persistTaxon` → `TaxonService.findOrCreate(Taxon)` (already has a
  DAO `findOrCreate`).
- `persistBlatResult`, `persistBlatAssociation`,
  `persistBioSequence2GeneProduct`,
  `persistSequenceSimilaritySearchResult` → callers move to the DAOs
  directly.
- Result: ~100 LoC out, 5 methods gone, ~670 → ~570 LoC.

### Chunk 5.3 — Lift `updateGene` + `handleGeneProductChangedGIs` to `GeneWriteService` (HIGH risk, ~1.5 agent-sessions)

- **Step 1 (before any code moves):** write unit tests for the four
  quirks (4.1–4.4) against the existing private methods. Use
  reflection or temporarily make them package-private. **These tests
  are the contract** the new service must satisfy. Reuse fixtures
  from `GenomePersisterTest` where possible.
- **Step 2:** create `GeneWriteService` (interface +
  `@Service` impl) in
  `gemma-core/src/main/java/ubic/gemma/persistence/service/genome/gene/`.
  Public surface: `Gene upsert(Gene)`, `Gene create(Gene)`. Wire
  `GeneDao`, `GeneProductDao`, `BioSequenceDao`,
  `BlatAssociationDao`, `AnnotationAssociationDao`, `SessionFactory`.
- **Step 3:** copy `updateGene`, `handleGeneProductChangedGIs`,
  `removeGeneProducts`, `updateGeneProduct`, `addAnyNewAccessions`,
  `persistGene(Gene, boolean, Caches)` private body, and the
  ID-change check into the new service. Drop the `Caches` parameter
  — Hibernate L1 covers what the per-call cache was protecting (and
  the `Taxon`/`Chromosome` cases are already handled in chunk 5.1).
- **Step 4:** rewire the two callers
  (`NcbiGeneLoader:191`, `ExternalFileGeneLoaderServiceImpl:144`) to
  call `geneWriteService.upsert(gene)` directly. Both already have
  the gene fully populated; no setup change.
- **Step 5:** delete `persistGene`, `persistGeneProduct`,
  `persistOrUpdateGene`, `persistOrUpdateGeneProduct`,
  `updateGene`, `updateGeneProduct`, `addAnyNewAccessions`,
  `handleGeneProductChangedGIs`, `removeGeneProducts` from
  `GenomePersister`.
- **Step 6:** retest the `geneProductDao.create(collection)` audit
  workaround (4.5) against the Phase-3 listener wiring; document the
  result.
- Result: ~400 LoC out, 9 methods gone, ~570 → ~170 LoC.

### Chunk 5.4 — Delete `GenomePersister.java` (low risk, ~0.5 agent-session)

After chunks 5.1–5.3, only `doPersist` / `doPersistOrUpdate` dispatch
remain. With nothing left to dispatch to, those overrides become
no-ops; the file can be deleted and `ArrayDesignPersister` (next in the
chain) can extend `CommonPersister` directly. (Note: this depends on
roadmap step 5 — `ArrayDesignPersister` replacement — having landed, or
on `ArrayDesignPersister` being temporarily rewired to skip the
GenomePersister layer.)

---

## 6. Risk callouts

1. **The four quirks are largely untested today.** `GenomePersisterTest`
   exercises happy-path persistence but not the GI-rotation,
   bicistronic, or NCBI-ID-change branches. The risk-5 rating exists
   *because* a regression here would only surface on the next NCBI gene
   refresh (typically weekly), and only as a single warn-log line —
   easy to miss. **Test-first migration is non-negotiable for chunk
   5.3.**

2. **Transaction-boundary changes.** `GenomePersister` is invoked
   inside a `@Transactional` propagation from `NcbiGeneLoader`'s batch
   loop. `GeneWriteService.upsert` must run inside the same transaction
   (use `Propagation.MANDATORY` or `REQUIRED` matching the caller); if
   it accidentally opens a new transaction, the rare cross-transaction
   GI-reattach case (4.2 cross-gene) will fail because the `oldGene`
   update won't be visible to the next batch.

3. **`AbstractPersister.FlushMode.MANUAL` interaction.** The dance in
   `AbstractPersister.doPersist` (sets `FlushMode.MANUAL`, then
   manually flushes at end) exists to avoid premature flushes during
   graph walks. `GeneWriteService` won't extend `AbstractPersister`,
   so it'll run under default `AUTO`. Verify under integration test
   that `updateGene` doesn't trigger a mid-walk flush that breaks the
   GI-reattach (this is the `--restart`-workaround scenario from 4.1).

4. **Cascade=all on `ChromosomeFeature.hbm.xml products` set.** Line
   71 declares `cascade="all"` from Gene→GeneProduct. That means
   simple `session.persist(gene)` will cascade-create products. But
   `updateGene` deliberately walks products one at a time to do the
   GI-reconciliation. We must NOT rely on cascade for the update path;
   we must explicitly manage the collection (as the current code
   does). Create path *can* use cascade once we verify the auditing
   workaround (4.5) is no longer needed.

5. **`removeGeneProducts` BLAT/AnnotationAssociation cleanup.** This
   cascade is NOT declared in HBM (verify); the current code walks it
   manually. Even after migration this stays manual unless we add
   `cascade="all-delete-orphan"` to the GeneProduct↔BlatAssociation
   relation (likely a bad idea — blat associations have lifecycle
   independent of gene-product churn).

---

## 7. Effort estimate

| Chunk | Effort | Risk |
|---|:-:|:-:|
| 5.1 BioSequence + Chromosome | 1.0 session | 2 |
| 5.2 Taxon + BLAT-family | 0.5 session | 1 |
| 5.3 Lift updateGene + GI logic to GeneWriteService | 1.5 sessions | **5** |
| 5.4 Delete GenomePersister.java | 0.5 session | 2 |
| **Total** | **3.5 sessions** | (overall risk 5 due to 5.3) |

This matches the roadmap's "~2 agent-sessions" estimate for step 6 if
you treat chunks 5.1 + 5.2 as prep work that could roll into the
broader BusinessKey lift, and chunk 5.4 as fall-out from chunk 5.3.
Standalone, 3.5 sessions is the more honest figure once you include
writing the four contract tests (the bulk of chunk 5.3's effort).

---

## 8. Open questions for Paul

1. **`GeneWriteService` location.** Roadmap question 2 asked: (a) new
   `GeneWriteService` adjacent to `GeneServiceImpl`, or (b) inside
   `NcbiGeneLoader` itself? Recommendation here: **(a) — new service**.
   `ExternalFileGeneLoaderServiceImpl` is the second caller and lives
   in a sibling package; option (b) would require duplication or a
   loader-to-loader call that obscures the contract. Confirm?

2. **The `geneProductDao.create(collection)` cascade workaround
   (quirk 4.5).** Has the Phase-3 audit-trail listener cutover (or
   the `AbstractPersister.formatEntity` fix in commit `63bffbf6fc`)
   made this redundant? If yes, chunk 5.3 deletes ~15 lines and
   `GeneWriteService.create` reduces to plain `session.persist(gene)`.
   If no, the explicit collection-create must be preserved verbatim
   with a load-bearing comment.

3. **Drosophila flush-failure `--restart` workaround (quirk 4.1
   comment, lines 251–258).** Is this still observed in 2026, or did
   the Hibernate 6 + DTYPE migration fix it? If unobserved for the
   last N gene refreshes, the comment can be downgraded from "this
   is how we live with it" to "historical note." A confirmation that
   the last 6 months of NCBI refreshes succeeded without `--restart`
   would be sufficient.

4. **`Caches` parameter on the new write service.** The persister
   threads a per-call `Caches` object that holds Taxon/Chromosome/QT
   maps. For `GeneWriteService` post-chunk 5.1 (Taxon+Chromosome already
   resolved before entry), this cache is empty / unused. Drop the
   parameter? Recommendation: yes — Hibernate L1 covers within-tx
   identity, and the cache was a Hibernate-3 workaround.

5. **Should the four contract tests live in `GeneWriteServiceTest`
   (new) or be carried over from `GenomePersisterTest`?** Recommendation:
   new test class, kept alongside `GeneWriteService` itself; let
   `GenomePersisterTest` shrink to the entities still routed through
   the persister (i.e. delete it once chunk 5.4 lands).

---

## 9. Out of scope for this recce

- Actually doing any of the rewrite.
- Touching `ArrayDesignPersister` or `ExpressionPersister`.
- Lifting `QuantitationType` or `BioAssayDimension` BK logic (roadmap
  step 1; not needed for GenomePersister).
- Adding `@NaturalId` annotations (roadmap question 3; orthogonal to
  this chunk).
- Test fixture migration (in-flight under `ExperimentFactory`).
