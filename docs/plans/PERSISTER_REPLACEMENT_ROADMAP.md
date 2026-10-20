# persisterHelper retirement: replacement roadmap

**Date:** 2026-05-18
**Branch baseline:** `phase2-acl-migrate` HEAD `08e760bdaf`
**Status:** recce only — no code changed in this commit.

---

## 1. Why retire `persisterHelper`

`PersisterHelper` is a deep five-level class hierarchy that hand-walks domain
object graphs and inserts them one node at a time, with manual `findOrCreate`
calls for every association. It was load-bearing under Hibernate 3/4, where the
JPA `cascade={PERSIST, MERGE}` semantics were either unimplemented or
unreliable; you had to walk the graph yourself or risk `TransientObjectException`.

Two things have changed that make the hierarchy redundant:

1. **Hibernate 6 + the HBM cascade declarations are already in place.** The
   `.hbm.xml` files (e.g. `Investigation.hbm.xml`, `ArrayDesign.hbm.xml`,
   `ChromosomeFeature.hbm.xml`) already declare `cascade="all"` (or
   `save-update`/`all-delete-orphan` where appropriate) on every parent→child
   association we hand-walk inside the persisters. Hibernate 6 honours those
   cascades correctly. The persister code is doing on the Java side what
   Hibernate is also doing on flush — duplicated work, with subtle ordering
   bugs (the `setHibernateFlushMode(MANUAL)` + manual flush dance in
   `AbstractPersister` exists precisely to paper over those races).

2. **`BusinessKey` already exists.** `gemma-core/src/main/java/ubic/gemma/
   persistence/util/BusinessKey.java` (816 LoC) is a static utility with
   `find(Session, T)` methods for 14 entity types. It is already invoked from
   the DAOs' `find(T)` methods (e.g. `BioMaterialDaoImpl.find` calls
   `BusinessKey.find(session, bioMaterial)`). The find-or-create logic that
   persisters claim to own is already factored out; the persisters just call
   `dao.find()` then `dao.create()` in sequence.

The only behaviour the persisters add on top of "JPA cascade + DAO
findOrCreate" is:

- Per-call caching of `Taxon`/`ExternalDatabase`/`Chromosome`/`QuantitationType`/
  `BioAssayDimension` in a `Caches` value object, to avoid round-trips when the
  same business-key probe appears many times in one graph.
- A pre-pass (`ExpressionExperimentPrePersistService.prepare`) that hydrates
  every `ArrayDesign` referenced by every `RawExpressionDataVector` so they
  can be matched by `shortName` instead of object identity.
- Domain-specific tangles in `GenomePersister.updateGene` /
  `handleGeneProductChangedGIs` that aren't really persistence — they're
  business logic for "NCBI changed the GI on this gene product, reconcile it"
  and belong in `NcbiGeneLoader`, not in a persister.

All three are addressable without preserving the hierarchy.

---

## 2. Inventory

Nine `*Persister*.java` files in `gemma-core/src/main`, totalling 2203 LoC
(the 2335 figure in the project memory included the two test files). Note
the brief's "11 files" was approximate — there are 9 main-source files plus
2 test files.

| File | LoC | Role |
|---|---:|---|
| `Persister.java` | 80 | Public interface (`persist`/`persistOrUpdate` + collection variants). |
| `PersisterHelper.java` | 25 | Subinterface adding `persist(ExpressionExperiment, ArrayDesignsForExperimentCache)` + `prepare()`. |
| `AbstractPersister.java` | 190 | Top of chain: `FlushMode.MANUAL` dance, `Caches` value object, error logging. |
| `CommonPersister.java` | 211 | Handles `AuditTrail`, `Person`, `Contact`, `Unit`, `QuantitationType`, `ExternalDatabase`, `Protocol`, `Characteristic`, `BibliographicReference`, `DatabaseEntry`. |
| `GenomePersister.java` | 920 | Handles `Gene`, `GeneProduct`, `BioSequence`, `Taxon`, `Chromosome`, `BlatResult`, `BlatAssociation`, `BioSequence2GeneProduct`, `SequenceSimilaritySearchResult`. Contains the gnarly `updateGene` / `handleGeneProductChangedGIs` business logic. |
| `ArrayDesignPersister.java` | 143 | Handles `ArrayDesign` (find by `shortName`, then create new) and walks `CompositeSequence` → `BioSequence`. |
| `ExpressionPersister.java` | 507 | Handles `ExpressionExperiment`, `BioAssay`, `BioMaterial`, `BioAssayDimension`, `Compound`, `ExperimentalFactor`, `FactorValue`, `ExperimentalDesign`, `ExpressionExperimentSubSet`, raw data vectors. |
| `RelationshipPersister.java` | 80 | Handles `Gene2GOAssociation`, `ExpressionExperimentSet`. |
| `PersisterHelperImpl.java` | 47 | The `@Service` bean. Extends `RelationshipPersister`, adds `AuditTrail` priming for `Auditable` entities. |
| `ArrayDesignsForExperimentCache.java` | 127 | Not a persister — the pre-prepared array-design cache passed in. Stays. |

Chain: `PersisterHelperImpl → RelationshipPersister → ExpressionPersister →
ArrayDesignPersister → GenomePersister → CommonPersister → AbstractPersister`.
Every level overrides `doPersist(T, Caches)` with an `instanceof` cascade.

---

## 3. Caller map

**81 call sites total**: **26 in production** (`gemma-core/src/main`,
`gemma-cli/src/main`, `gemma-web/src/main`) and **55 in tests** (mostly
`PersistentDummyObjectHelper` and friends).

### Production callers (26 call sites across 18 files)

| Call site | What it's trying to do | Replacement intent |
|---|---|---|
| `GeoServiceImpl` (4 sites: AD, EE, BS, BibRef) | Persist a whole-experiment graph after parsing GEO; persist platforms before persisting EEs | `EeWriteService.create(ee, cachedArrays)`; AD persist via `ArrayDesignService.create(ad)` |
| `ExpressionExperimentPrePersistServiceImpl` (2 sites: AD, BS) | Resolve probes/sequences to persistent IDs before EE persist | Stays — uses `ArrayDesignDao.find()` directly post-migration |
| `SimpleExpressionDataLoaderServiceImpl` | Persist user-uploaded expression matrix as EE | Same as Geo: a new `EeWriteService.create(ee)` |
| `NCBIGene2GOAssociationLoader` | Batch-persist `Gene2GOAssociation` rows | `Gene2GOAssociationDao.create(Collection)` directly (already exists) |
| `ArrayDesignProbeMapperServiceImpl` | Persist a single `BlatAssociation` after mapping | `BlatAssociationDao.create(ba)` — `cascade=all` handles `BlatResult`+`GeneProduct` lookup via the existing `BusinessKey` resolvers |
| `ArrayDesignSequenceAlignmentServiceImpl` | Persist a `Collection<BlatResult>` after alignment | `BlatResultDao.create(brs)` (find-or-create flavour) |
| `ArrayDesignSequenceProcessingServiceImpl` | `persistOrUpdate(BioSequence)` for a probe sequence | `BioSequenceService.findOrUpdate(bs)` (new thin method) |
| `NcbiGeneLoader` | `persistOrUpdate(Gene)` for every gene in NCBI dump | `GeneWriteService.upsert(gene)` — wraps the `updateGene` business logic at its proper home |
| `ExternalFileGeneLoaderServiceImpl` | Same as NcbiGeneLoader but for custom files | Same target |
| `TaxonLoader`, `PubMedService`, `PubMedSearcher`, `ExpressionExperimentPrimaryPubCli`, `UpdatePubMedCli` | Persist `Taxon`, batches of `BibliographicReference` | Direct DAO `create()` after `find()` — these have one-line `BusinessKey` resolvers already |
| `DifferentialExpressionAnalysisHelperServiceImpl` (2 sites: Protocol, BioAssaySet) | Persist analysis-attached protocol + ensure experiment is persistent | Direct DAO calls; both targets already have explicit ID checks |
| `BibliographicReferenceController` (gemma-web) | Persist a manually-entered bibref | gemma-web is walking dead per project memory; rewrite happens in `gemma-curation-ui` |
| `ExpressionExperimentController`, `ExpressionExperimentEditController` (gemma-web) | Persist edited `BioMaterial`/`BibliographicReference` | Same — gemma-web replacement |

### Test callers (55 sites)

Concentrated in `PersistentDummyObjectHelper` (29 sites) and
`BaseSpringContextTest` (1 site, used widely transitively). The Phase 3
fixture migration already in flight (`ExperimentFactory`, etc., see
project memory) replaces these test helpers with proper factory beans that
go directly through DAOs / write-services. **No test-side caller needs to
be rewritten just for persister retirement — the in-flight factory
migration carries the load.**

---

## 4. Business-key inventory

`BusinessKey` (816 LoC, `gemma-core/src/main/java/ubic/gemma/persistence/
util/BusinessKey.java`) is the existing source of truth. For each domain
type that flows through a persister:

| Entity | Business key | `BusinessKey.find()` exists? | `@NaturalId`? | `equals/hashCode` aware of BK? | Notes |
|---|---|:-:|:-:|:-:|---|
| `Taxon` | `ncbiId` (preferred), `scientificName`, `commonName` | yes | no | yes (NCBI ID + scientific name) | Cleanest case. |
| `ExternalDatabase` | `name` | (uses `find()` in `ExternalDatabaseDaoImpl`) | no | yes (ID-equality fallback to ID-eq only) | Single-field BK. |
| `Gene` | `ncbiGeneId` (preferred), then `(officialSymbol, taxon)`, then `(officialSymbol, taxon, physicalLocation)` | yes | no | yes (4-way fallback in `Gene.equals`) | Three-tier resolution; matches `updateGene` semantics. |
| `GeneProduct` | `ncbiGi`, `name` | yes | no | name-based | Multiple GIs per gene cause cruft handling. |
| `BioSequence` | `(name, taxon)` + optional `sequenceDatabaseEntry.accession` | yes | no | yes | DB-entry overrides name when present. |
| `ArrayDesign` | `shortName` | yes | no | (Identifiable equality — relies on ID) | One-field BK; the resolver could be a 4-liner. |
| `BioMaterial` | `(name, sourceTaxon, externalAccession)` | yes | no | name-equality | Race-condition retry loop in current DAO. |
| `BioAssay` | `(name, accession)` | yes | no | partial | Tightly coupled to its EE. |
| `Compound` | name-based via `findOrCreate` | (only via DAO) | no | no | Simple, but rarely exercised. |
| `Unit` | name-based via `findOrCreate` | yes | no | no | Simple. |
| `Characteristic` | value+category | yes | no | no | Cascaded-only in `CommonPersister`. |
| `FactorValue` | composite over `(experimentalFactor, measurement, characteristics)` | yes | no | composite | Most expensive resolver. |
| `ExpressionExperiment` | `shortName` | (via `ExpressionExperimentDao.findByShortName`) | no | mostly ID | Cleanest of the experiment graph entities. |
| `Contact`, `Person` | name + email (Person extends Contact) | yes (`Contact`) | no | no | Used in audit trail. |
| `Gene2GOAssociation` | `(gene, ontologyTerm)` | yes | no | no | Pure relationship. |
| `Chromosome` | `(name, taxon)` | (not in BusinessKey — handled inline by `GenomePersister.persistChromosome`) | no | no | **Gap.** |
| `QuantitationType` | `(name, description)` hash | (not in BusinessKey — inline in `CommonPersister`) | no | no | **Gap.** Note: `persistQuantitationType` deliberately uses `create`, not findOrCreate — QTs are per-experiment. |
| `BioAssayDimension` | hashCode of `BioAssay` list | (not in BusinessKey — inline in `ExpressionPersister`) | no | no | **Gap.** Hash key built from list contents. |
| `ExperimentalFactor`, `ExperimentalDesign` | always create-new in current code | (not in BusinessKey) | n/a | n/a | Composition-owned by EE; no BK needed if we rely on cascade. |
| `Protocol`, `BibliographicReference` | name; pubMed accession | (BibRef via DAO) | no | no | Bib has explicit `findOrCreate` via `pubAccession`. |

### Gaps and inconsistencies

- **No JPA `@NaturalId` annotations anywhere.** All BK info is encoded in
  the `BusinessKey` utility's static `find()` methods, which use the JPA
  Criteria API against the live `Session`. Adding `@NaturalId` is optional
  for the migration; doing so would let us use `session.bySimpleNaturalId(...)`
  but is a cosmetic improvement, not a blocker.
- `Chromosome`, `QuantitationType`, `BioAssayDimension` have their BK
  logic inline in the persisters, not in `BusinessKey`. **The migration
  must lift these three into `BusinessKey` first** before the calling
  persister can be retired.
- `Gene.equals/hashCode` is heavy — `hashCode` mutates with attached
  products, which makes Gene risky to use as a key in caches. Worth
  fixing while we're in there but not on the critical path.

---

## 5. Target architecture

### 5.1 `BusinessKeyResolver<T>` interface

```java
package ubic.gemma.persistence.bk;

import java.util.Optional;
import ubic.gemma.model.common.Identifiable;

/** Resolve an entity by its business (natural) key. Distinct from
 * primary-key load (which is what BaseDao.load(id) does). */
public interface BusinessKeyResolver<T extends Identifiable> {
    /** Returns Optional.empty() if no match; throws if the probe has no
     * BK (caller has to provide enough to identify). */
    Optional<T> findByBusinessKey(T probe);
}
```

### 5.2 Example: `TaxonBusinessKeyResolver`

Thin wrapper over the existing `BusinessKey.find(Session, Taxon)` —
no DB-layer code is moving, just the Spring boundary.

```java
@Component
public class TaxonBusinessKeyResolver implements BusinessKeyResolver<Taxon> {
    @Autowired private SessionFactory sessionFactory;

    @Override
    public Optional<Taxon> findByBusinessKey(Taxon probe) {
        if (probe.getNcbiId() == null
                && probe.getScientificName() == null
                && probe.getCommonName() == null) {
            throw new IllegalArgumentException(
                "Taxon probe needs at least one of NCBI ID, scientific name, common name");
        }
        return Optional.ofNullable(
            BusinessKey.find(sessionFactory.getCurrentSession(), probe));
    }
}
```

### 5.3 Example: `GeneBusinessKeyResolver`

```java
@Component
public class GeneBusinessKeyResolver implements BusinessKeyResolver<Gene> {
    @Autowired private SessionFactory sessionFactory;

    @Override
    public Optional<Gene> findByBusinessKey(Gene probe) {
        // BusinessKey.find already does the (ncbiGeneId | symbol+taxon |
        // symbol+taxon+location) cascade.
        return Optional.ofNullable(
            BusinessKey.find(sessionFactory.getCurrentSession(), probe));
    }
}
```

### 5.4 Call-site pattern

The 3-line replacement for `T result = persisterHelper.persist(probe)`:

```java
T persistent = resolver.findByBusinessKey(probe)
    .orElseGet(() -> {
        entityManager.persist(probe);
        return probe;
    });
```

For "must already exist" sites (e.g. `BioAssay.arrayDesignUsed`), the
`.orElseThrow(() -> new IllegalStateException(...))` flavour is correct
and clarifies intent — the current persister silently no-ops when an ID
is already set, which masks real bugs.

For the deep graphs the pattern relies on HBM cascade declarations:

- **EE graph** (`Investigation.hbm.xml` already has `cascade="all"` on
  `auditTrail`, `accession`, `bioAssays`, `quantitationTypes`,
  `meanVarianceRelation`, `geeq`, `curationDetails`): just
  `entityManager.persist(ee)` after resolving `taxon`, `primaryPublication`,
  and `arrayDesignsUsedByBioAssays` to persistent instances.
- **ArrayDesign graph** (`ArrayDesign.hbm.xml` already has `cascade="all"`
  on `externalReferences`, `compositeSequences`, `alternateNames`):
  `entityManager.persist(ad)` after resolving `primaryTaxon`,
  `designProvider`, and each `compositeSequence.biologicalCharacteristic`
  to persistent `BioSequence` via `BioSequenceBusinessKeyResolver`.
- **Gene graph** (`ChromosomeFeature.hbm.xml` has `cascade="all"` on
  `physicalLocation`, `accessions`, `products`, `aliases`):
  `entityManager.persist(gene)` after resolving `taxon`, then for each
  product resolve its `chromosome` if any.

### 5.5 Where cascade does NOT suffice

Cases where a thin walk is still needed (these become small per-entity
write services, not a generic helper):

1. **`updateGene` / `handleGeneProductChangedGIs` business logic** — this
   is not persistence, it's domain logic about NCBI ID switches and
   GP-to-Gene reattachment. It lives in `NcbiGeneLoader`'s sibling, a new
   `GeneWriteService.upsert(Gene)`.
2. **`ExperimentalDesign` pre-creation** to dodge premature cascade of
   `FactorValue` — current code creates the design before its factors so
   factor values get their ACLs. Once
   `BaseAclAdvice` is fully off AOP (Phase 3 listener cutover already
   landed per project memory), the workaround is unnecessary and a plain
   cascade works. Verify on dev before retiring.
3. **`QuantitationType` per-experiment freshness** — the persister
   intentionally calls `create`, not `findOrCreate`, because QTs are
   per-EE. That's a positive design decision; keep it via
   `QuantitationTypeWriteService.create(qt)` and do not write a resolver.
4. **`ArrayDesignsForExperimentCache` prefill** — must run in its own
   transaction before the EE persist so probe→AD lookups are cheap. The
   `ExpressionExperimentPrePersistService` already encapsulates this; it
   stays.
5. **`audit trail` priming** in `PersisterHelperImpl.doPersist` — needs a
   spring boundary so new `Auditable`s get their `AuditTrail` created
   before they cascade. Move to a JPA `@PrePersist` listener on
   `Auditable`, or a thin `AuditingAspect`.

---

## 6. Migration order

Replace bottom-up so each step is independently shippable and reduces
the surface for the next:

1. **Lift `Chromosome`, `QuantitationType`, `BioAssayDimension` BK
   logic into `BusinessKey`.** Pure refactor — no behaviour change.
   Unblocks every downstream step. **~1 agent-session.**

2. **Add `@PrePersist` audit-trail listener** so `Auditable` entities get
   their `AuditTrail` populated automatically. Unblocks removal of
   `PersisterHelperImpl.doPersist`. Tested under existing audit-trail
   integration tests. **~1 agent-session.**

3. **Replace `CommonPersister`** — `Contact`, `Person`, `Unit`,
   `Protocol`, `BibliographicReference`, `DatabaseEntry`,
   `ExternalDatabase`, `QuantitationType`. Each gets a thin
   `XxxBusinessKeyResolver` (or none, where create-only is wanted) and
   call sites move to the relevant `XxxService`. This is the lowest-risk
   tier — no graph depth, no business logic. **~1 agent-session.**

4. **Replace `RelationshipPersister`** — only two entities
   (`Gene2GOAssociation`, `ExpressionExperimentSet`); both already have
   DAOs and `BusinessKey` resolvers. **~0.5 agent-session.**

5. **Replace `ArrayDesignPersister`** — single entity (`ArrayDesign`),
   one BK (`shortName`), cascade already declared. Migrate the AD
   call sites (`GeoServiceImpl`, `ExpressionExperimentPrePersistServiceImpl`,
   `ArrayDesignSequenceProcessingServiceImpl`). **~1 agent-session.**

6. **Replace `GenomePersister`** — biggest single file (920 LoC) and
   hosts the `updateGene`/`handleGeneProductChangedGIs` business logic.
   Strategy: lift that logic into a new `GeneWriteService` (consumed
   only by `NcbiGeneLoader` + `ExternalFileGeneLoaderServiceImpl`),
   then the persister-side of `Gene`/`GeneProduct`/`BioSequence` reduces
   to "resolve by BK, else cascade-persist". **~2 agent-sessions.**

7. **Replace `ExpressionPersister`** — biggest behavioural impact
   (`EE` + `BioAssay` + `BioMaterial` + factors + designs + data
   vectors). Needs a new `ExpressionExperimentWriteService.create(EE,
   cachedArrays)` matching the existing `prepare()` contract so
   `GeoServiceImpl` and `SimpleExpressionDataLoaderServiceImpl` are a
   one-line change. **~2 agent-sessions.**

8. **Delete `AbstractPersister`, `PersisterHelper`, `PersisterHelperImpl`,
   `Persister` interface.** Migrate the remaining test sites (largely
   covered by the in-flight `ExperimentFactory` fixture migration).
   **~1 agent-session.**

Total: **~9.5 agent-sessions**, roughly two weeks of focused work.

---

## 7. Risk + estimates

Risk 1 = trivial drop-in; 5 = touches many call sites and exercises
fragile loader code paths.

| Step | Persister | Risk | Sessions | Why |
|---|---|:---:|:---:|---|
| 1 | BusinessKey lift (Chromosome/QT/BAD) | 1 | 1 | Pure refactor, no callers change. |
| 2 | AuditTrail `@PrePersist` listener | 2 | 1 | New listener path; covered by existing audit tests but watch for double-create races. |
| 3 | `CommonPersister` | 2 | 1 | Many entities but each is small and unrelated. Audit-trail interaction is the only hazard, mitigated by step 2. |
| 4 | `RelationshipPersister` | 1 | 0.5 | Two entities, both with DAOs already wired. |
| 5 | `ArrayDesignPersister` | 3 | 1 | The probe/BioSequence walk is real; `GeoServiceImpl` and three ArrayDesignSequence* services hit it. Risk is in BioSequence ID propagation. |
| 6 | `GenomePersister` | **5** | 2 | 920 LoC of which ~350 are NCBI-GI-reconciliation business logic. Touched only by `NcbiGeneLoader` + `ExternalFileGeneLoaderServiceImpl` but those are critical loaders. The `updateGene` rewrite needs careful test coverage including the rare bicistronic/duplicated-GI edge cases the existing code calls out. |
| 7 | `ExpressionPersister` | **5** | 2 | 507 LoC, every EE that enters Gemma goes through this. The `processBioAssays` / `fillInDesignElementDataVectorAssociations` / `processExperimentalDesign` triad runs in a specific order that the cascade declarations must preserve. The `ArrayDesignsForExperimentCache` contract must be exactly preserved. |
| 8 | Delete the chain | **4** | 1 | Mechanical, but the Spring bean wiring is a single `@Service` shared by many transactional callers — failure mode is "context fails to start." Mitigated by doing steps 3–7 first; by step 8 nothing should be injecting `PersisterHelper`. |

### The three highest-risk replacement candidates

1. **`GenomePersister`** (risk 5): 920 LoC, NCBI reconciliation business
   logic that has been hardened over a decade of NCBI screw-ups
   (drosophila bicistronic gene products, GI rotations). Replacement must
   preserve all of that semantics in `GeneWriteService.upsert`.
2. **`ExpressionPersister`** (risk 5): 507 LoC on the hottest write path
   in Gemma; every GEO load and SimpleEE upload passes through it. The
   `FlushMode.MANUAL` dance exists because pre-cascade flushes broke ACL
   creation; verify the ACL listener cutover from the 2026-05-18 session
   makes that workaround unnecessary before removing it.
3. **`ArrayDesignPersister`** (risk 3): the lowest of the top three but
   still notable — moving array-design persistence breaks the
   `ArrayDesignsForExperimentCache` contract if you're not careful, and
   re-importing the same platform after the change must produce
   identical row counts.

---

## 8. Open questions for Paul

1. **Does the ACL listener cutover (Phase 3, landed 2026-05-18) make
   `FlushMode.MANUAL` in `AbstractPersister` redundant?** If yes, that
   simplifies the EE write service. If no, the new write service has to
   replicate the manual-flush dance and we should document why.

2. **`updateGene` rehome target.** Two reasonable destinations:
   (a) a new `GeneWriteService` adjacent to `GeneServiceImpl`, or
   (b) inside `NcbiGeneLoader` itself (the only real client). Option (b)
   keeps NCBI-specific logic in NCBI-specific code; option (a) is more
   reusable. Preference?

3. **`@NaturalId` annotations: yes/no?** They make resolvers a one-liner
   (`session.bySimpleNaturalId(Taxon.class).load(ncbiId)`) but add
   bytecode-enhancement requirements at runtime. Worth the trade?

4. **Should the new `BusinessKeyResolver<T>` interface live in
   `gemma-core`, or be promoted to a shared `gemma-spi` module?** The
   in-flight fixture-factory work (`ExperimentFactory` etc.) would also
   benefit from a clean interface. If it's just for internal use, leave
   it in core.

5. **Test fixture migration timing.** The in-flight `ExperimentFactory`
   work (agent `a719cad6c20a655be`) is replacing `PersistentDummyObjectHelper`
   callers. Should persister retirement wait for that to finish (cleaner
   test surface), or proceed in parallel and let the fixture migration
   pick up the new write services?

6. **`gemma-web` controllers.** Four gemma-web call sites
   (`BibliographicReferenceController`, `ExpressionExperimentController` x2,
   `ExpressionExperimentEditController`). Per project memory gemma-web is
   "walking dead" — do we patch these (1 hour) or leave them and let
   `gemma-curation-ui` consume the new write services natively?
