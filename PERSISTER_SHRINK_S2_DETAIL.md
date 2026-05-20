# PERSISTER_SHRINK_S2_DETAIL.md — promote the abstract persister chain to @Component beans

**Baseline:** `phase2-acl-migrate` @ `931bbf514effdac0ea79031c82a06e6d711da02a` (post-S1).
**Status of this document:** detail recce only; no code changes in this commit.
**Supersedes:** Section 3, Step S2 of `PERSISTER_SHRINK_RECCE.md`.

---

## 1. Why a detail recce instead of code

The recce's S2 paragraph is one sentence per persister; on actual file inspection
the conversion is not a single mechanical pass. The conservative-scope hint in
the S2 agent brief ("just ADD `@Component` annotations and `@Autowired`
references and prove they're consistent") is a no-op as written, because Spring
component-scan ignores `@Component` on abstract classes — they cannot be
instantiated. To make S2 valuable we have to actually break the inheritance
chain, and that touches six files with several non-obvious interlocks. This
document walks the interlocks and stages the change so a follow-on agent can
execute it cleanly in 1–2 sessions of focused work.

Files in scope:
- `gemma-core/src/main/java/ubic/gemma/persistence/persister/AbstractPersister.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/persister/CommonPersister.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/persister/GenomePersister.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/persister/ArrayDesignPersister.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/persister/RelationshipPersister.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/persister/PersisterHelperImpl.java`
- `gemma-core/src/main/java/ubic/gemma/persistence/persister/EeWriteServiceImpl.java`  *(consumer)*

---

## 2. Current shape (post-S1)

```
PersisterHelperImpl  (@Service, concrete)
        extends RelationshipPersister  (abstract)
        extends ArrayDesignPersister   (abstract)
        extends GenomePersister        (abstract)
        extends CommonPersister        (abstract)
        extends AbstractPersister      (abstract; implements Persister)
```

`AbstractPersister.persist(T)` / `persistOrUpdate(T)` / `persist(Collection<T>)`
open a `FlushMode.MANUAL` window, call `doPersist(entity, new HashMap<>())`,
flush, restore `FlushMode.AUTO`. Each subclass overrides `doPersist` with an
`instanceof` arm + `super.doPersist(...)` chain. Five levels of `super.doPersist`
end at `AbstractPersister.doPersist` which throws `UnsupportedOperationException`.

Helpers `protected` on the chain that are reached by `EeWriteServiceImpl` via an
AOP-unwrap (`persister()`):
- `AbstractPersister.getSessionFactory()` — used for FlushMode + raw session ops.
- `AbstractPersister.doPersist(Collection<T>, Map)` — used to cascade-persist
  `Characteristic` collections (ExperimentalDesign.types,
  ExperimentalFactor.annotations).
- `CommonPersister.persistExternalDatabase`, `persistBibliographicReference`,
  `fillInDatabaseEntry`.
- `GenomePersister.persistTaxon`.

`PersisterHelperImpl` also reaches into `EeWriteServiceImpl` via the symmetric
AOP-unwrap (`eeWriteServiceImpl()`) to call `persistExpressionExperiment`,
`persistBioAssay`, `persistBioMaterial`, `persistBioAssayDimension`,
`persistCompound`, `persistExpressionExperimentSubSet` from `doPersist`.

---

## 3. Target shape

Each persister becomes a concrete `@Component`. No inheritance among them; all
collaboration goes through `@Autowired` references. The polymorphic dispatch
lives only in `PersisterHelperImpl` (the bean external callers see via the
`Persister` / `PersisterHelper` interfaces).

```
PersisterHelperImpl       (@Service, dispatcher; implements PersisterHelper)
    └── owns FlushMode window
    └── @Autowired CommonPersister, GenomePersister,
                   ArrayDesignPersister, RelationshipPersister, EeWriteService
    └── doPersist(T): typed dispatch table over the four leaf beans + EE arms

CommonPersister           (@Component)
    └── @Autowired SessionFactory, ExternalDatabaseDao,
                   DatabaseEntryDao, BibliographicReferenceDao
    └── public persistExternalDatabase, persistDatabaseEntry,
                fillInDatabaseEntry, persistBibliographicReference
    └── public doCommon(T, xdbCache) — User throws, Characteristic returns null,
                otherwise returns null to signal "not mine"

GenomePersister           (@Component)
    └── @Autowired CommonPersister + DAOs (Gene, Chromosome, GeneProduct,
                   BioSequence, Taxon, BlatAssociation, BlatResult,
                   AnnotationAssociation)
    └── public persistGene, persistGeneProduct, persistBioSequence,
                persistTaxon, persistBioSequence2GeneProduct,
                persistSequenceSimilaritySearchResult, persistChromosome,
                persistOrUpdateBioSequence, persistOrUpdateGene,
                persistOrUpdateGeneProduct
    └── (intra-class helpers stay private)

ArrayDesignPersister      (@Component)
    └── @Autowired CommonPersister, GenomePersister, ArrayDesignDao, ContactDao
    └── public persistArrayDesign(ArrayDesign, xdbCache, taxonCache, chromCache)

RelationshipPersister     (@Component)
    └── @Autowired GenomePersister (for persistGene)
                   + @Lazy PersisterHelperImpl (for recursive EE dispatch — see §5.3)
                   + Gene2GOAssociationDao, ExpressionExperimentSetDao
    └── public persistGene2GOAssociation, persistExpressionExperimentSet
```

`AbstractPersister` is deleted.

---

## 4. Step-by-step plan

The cheapest sequence is **bottom-up**: start at the leaf (`CommonPersister`,
no upward deps) and work up. After each step the project compiles and tests
pass; the inheritance chain shrinks one level per step.

### Step S2a — peel `CommonPersister` off `AbstractPersister`

1. `CommonPersister`: drop `extends AbstractPersister`. Add `@Component`. Class
   becomes non-abstract.
2. Move the `SessionFactory` autowire down from `AbstractPersister` into
   `CommonPersister` (also into `GenomePersister`, `ArrayDesignPersister`, and
   `PersisterHelperImpl` for now — duplicate fields with the same name; they all
   resolve to the same Spring bean and Hibernate cares about the session, not
   the holder).
3. Make `persistExternalDatabase`, `persistDatabaseEntry`,
   `persistBibliographicReference`, `fillInDatabaseEntry` `public` (they're
   already needed by `EeWriteServiceImpl`).
4. Replace the `doPersist(T, xdbCache)` override with a public typed
   `doCommon(T, xdbCache)` returning `@Nullable T`: handles User (throws),
   Characteristic (returns null = cascaded). Returns the entity unchanged for
   anything not recognised.
5. `GenomePersister` still `extends CommonPersister` for now — keep
   `super.doPersist` chains compiling. The new `doCommon` is additive.
6. **Watch:** `EeWriteServiceImpl.persister().persistExternalDatabase(...)`
   etc. continue to work via the inherited chain. No EE caller change yet.
7. **Compile check:** `mvn -pl gemma-core test-compile -q`.

Commit: `Persister shrink S2a: lift CommonPersister to @Component`.

### Step S2b — peel `GenomePersister` off `CommonPersister`

1. `GenomePersister`: drop `extends CommonPersister`. Add `@Component`. Add
   `@Autowired CommonPersister common;`.
2. Replace every inherited helper call (`this.persistExternalDatabase(...)`,
   `this.persistDatabaseEntry(...)`, `this.fillInDatabaseEntry(...)`) with
   `common.persistExternalDatabase(...)`.
3. Promote the `doPersist`/`doPersistOrUpdate` dispatch arms to a public
   `doGenome(T, xdbCache) -> @Nullable T` / `doGenomeUpdate(T, xdbCache) ->
   @Nullable T` returning null if no arm matches. The Gene/GeneProduct/
   BioSequence/Taxon/etc. typed helpers (already private) become `public`.
4. `ArrayDesignPersister` still `extends GenomePersister`; switch its
   `super.persistTaxon(...)` calls to `genome.persistTaxon(...)` via a new
   `@Autowired GenomePersister genome;` field (introducing the dep ahead of the
   actual `extends` removal — both work simultaneously).
5. **Compile check.**

Commit: `Persister shrink S2b: lift GenomePersister to @Component`.

### Step S2c — peel `ArrayDesignPersister` off `GenomePersister`

1. Drop `extends GenomePersister`. Add `@Component`. Confirm the
   `@Autowired CommonPersister common; @Autowired GenomePersister genome;`
   pair (added in S2b).
2. Replace `super.doPersist(...)` fallback in the AD `doPersist` arm with
   direct calls to `genome.doGenome(...)` and `common.doCommon(...)`. AD only
   handles `ArrayDesign` itself; everything else routes to Genome/Common.
3. Public method: `persistArrayDesign(ArrayDesign, xdbCache, taxonCache,
   chromCache)`.
4. `RelationshipPersister` still `extends ArrayDesignPersister`; switch its
   `super.persistGene(...)` to `genome.persistGene(...)` ahead of the
   `extends` removal.
5. **Compile check.**

Commit: `Persister shrink S2c: lift ArrayDesignPersister to @Component`.

### Step S2d — peel `RelationshipPersister` off `ArrayDesignPersister`

1. Drop `extends ArrayDesignPersister`. Add `@Component`. Inject
   `@Autowired GenomePersister genome;` (already present from S2c).
2. Public methods: `persistGene2GOAssociation(Gene2GOAssociation, xdbCache)`,
   `persistExpressionExperimentSet(ExpressionExperimentSet, xdbCache)`.
3. **Handle the EE recursion in `persistExpressionExperimentSet`** — this
   method recursively calls `this.doPersist(baSet, xdbCache)` on each EE
   member whose id is null. Replace with `dispatcher.doPersist(baSet, xdbCache)`
   where `dispatcher` is a `@Lazy @Autowired PersisterHelperImpl dispatcher;`
   (lazy to break the cycle Spring would otherwise see). See §5.3.
4. **Compile check.**

Commit: `Persister shrink S2d: lift RelationshipPersister to @Component`.

### Step S2e — collapse `PersisterHelperImpl`

1. Drop `extends RelationshipPersister`. Implement `Persister` and
   `PersisterHelper` directly. `@Service` stays.
2. Pull the `persist(T)`/`persistOrUpdate(T)`/`persist(Collection<T>)` entry
   points down from `AbstractPersister`: own the FlushMode.MANUAL window and
   the per-call `new HashMap<>()` allocation here.
3. New `doPersist(T, xdbCache)` is a single dispatch table — try EE arms first
   (BioAssay, BioMaterial, BAD, Compound, EESubSet, EE itself) via
   `eeWriteService` / `eeWriteServiceImpl()`, then Relationship arms
   (Gene2GOAssociation, ExpressionExperimentSet) via `relationship.*`, then
   AD (`arrayDesign.persistArrayDesign`), then `genome.doGenome(...)`, then
   `common.doCommon(...)`, then throw UnsupportedOperationException.
4. **`AbstractPersister` is now unreferenced.** Delete the file.
5. **Compile check.**

Commit: `Persister shrink S2e: collapse PersisterHelperImpl + delete AbstractPersister`.

### Step S2f — `EeWriteServiceImpl` cleanup (optional same-PR follow-on)

1. Replace the `@Autowired PersisterHelper persisterHelper;` +
   `persister()` AOP-unwrap with direct typed injections:
   ```java
   @Autowired private SessionFactory sessionFactory;
   @Autowired private CommonPersister commonPersister;
   @Autowired private GenomePersister genomePersister;
   @Autowired private PersisterHelper persisterHelper; // KEEP for doPersist(Collection<Characteristic>)
   ```
2. Rewrite all `persister().X(...)` call sites:
   - `persister().getSessionFactory()` → `sessionFactory`.
   - `persister().persistBibliographicReference` → `commonPersister.persistBibliographicReference`.
   - `persister().persistTaxon` → `genomePersister.persistTaxon`.
   - `persister().persistExternalDatabase` → `commonPersister.persistExternalDatabase`.
   - `persister().fillInDatabaseEntry` → `commonPersister.fillInDatabaseEntry`.
   - `persister().doPersist( experimentalDesign.getTypes(), xdbCache )` →
     stays as `persisterHelper.persist(...)` (uses the public collection path
     on the dispatcher — no protected reach needed).
3. Delete the `persister()` helper method.
4. **Compile check.**

Commit: `Persister shrink S2f: EeWriteServiceImpl direct autowires`.

---

## 5. Risks and gotchas

### 5.1 BeanNameGenerator name collisions
`gemma-core/src/main/java/ubic/gemma/core/context/BeanNameGenerator.java` strips
trailing `Impl` from class names. `CommonPersister`, `GenomePersister`,
`ArrayDesignPersister`, `RelationshipPersister` all become beans named
`commonPersister`, `genomePersister`, `arrayDesignPersister`,
`relationshipPersister`. `PersisterHelperImpl` is already `persisterHelper`.
None of the new names collide with the dispatcher or with existing beans —
verified via `grep -rn "commonPersister\|genomePersister\|arrayDesignPersister\|relationshipPersister" --include="*.xml" --include="*.java"` (zero hits in production). Test contexts in
`gemma-core/src/test/resources` may declare bare-name beans; check XML test
configs before the S2a commit.

### 5.2 FlushMode window ownership
`AbstractPersister.persist(T)` is the only place that toggles
`FlushMode.MANUAL ↔ AUTO`. After deletion this responsibility must live in
exactly one place per public entry point:

- `PersisterHelperImpl.persist(T)` etc. — wraps the dispatcher.
- `EeWriteServiceImpl.create(EE, ?)` already does it (lines 158–166); keep.

Internal calls from one leaf bean to another (e.g.
`relationship.persistGene2GOAssociation` calls
`genome.persistGene`) must **not** re-toggle FlushMode — they execute inside
the dispatcher's window. The current code achieves this implicitly because the
helpers are `private`/`protected` and aren't reached through `persist(T)`. The
new public typed methods on the bean must preserve this: they assume the
caller has already opened the FlushMode window. Document this in each public
method's Javadoc.

### 5.3 RelationshipPersister recursive dispatch (the only hard cycle)
`RelationshipPersister.persistExpressionExperimentSet` calls
`this.doPersist(baSet, xdbCache)` where `baSet` is an `ExpressionExperiment`.
That dispatches to the EE arm, which lives in `PersisterHelperImpl`
(post-S2). So `RelationshipPersister → PersisterHelperImpl →
RelationshipPersister` is a Spring DI cycle.

Two options:
- (preferred) `@Lazy @Autowired PersisterHelperImpl dispatcher;` on
  `RelationshipPersister`. Spring resolves with a proxy, breaks the eager
  cycle, and the recursive call works as today.
- (alternative) delete the recursive path. Per the recce §2.2, no production
  caller routes a raw `ExpressionExperimentSet` through `persist`. If only
  test fixtures exercise it, the path can require all set members to be
  pre-persisted (throw if `baSet.getId() == null`). Cheaper at runtime but
  breaks any test fixture relying on the current behaviour; verify before
  committing.

### 5.4 PersisterHelperImpl + EeWriteServiceImpl cycle
`PersisterHelperImpl.doPersist` calls `eeWriteServiceImpl().persistExpressionExperiment` etc.
`EeWriteServiceImpl` autowires `persisterHelper`. This is already a cycle and
Spring already resolves it (both are `@Service`/`@Component`, neither uses
constructor injection — the field-injection cycle is fine). Preserve this in
S2: keep the `eeWriteService` autowire on the dispatcher and the
`persisterHelper` autowire on EE (post-S2f the EE side can drop it; keep until
then to minimise risk).

### 5.5 AOP-unwrap helpers go away
Both `PersisterHelperImpl.eeWriteServiceImpl()` and
`EeWriteServiceImpl.persister()` exist solely to reach package-private /
protected helpers across the `@Transactional` JDK proxy. Post-S2 there are no
protected helpers reaching across beans; all collaboration is on public
methods of typed beans, and the JDK proxy is transparent for public calls.
Both methods can be deleted in S2e (dispatcher) and S2f (EE) respectively.

### 5.6 Test contexts and PersistentDummyObjectHelper
`PersistentDummyObjectHelper` and `TwoChannelMissingValuesTest` (per
`PersisterHelperImpl` Javadoc) still hit the polymorphic `persist(T)` /
`persistOrUpdate(T)` path with EE / BioAssay / BioMaterial / BAD / Compound /
EESubSet entities. The dispatcher must keep all six EE arms in its dispatch
table until those tests migrate to `EeWriteService.create` directly (tracked
by the junit5-batch agents). S2 must not delete those arms.

### 5.7 `@Transactional` placement
`AbstractPersister.persist(T)` carries `@Transactional`. The new
`PersisterHelperImpl.persist(T)` etc. must carry it (or rely on the existing
`@Transactional` already on `PersisterHelperImpl.persist(EE, cache)` and on
`EeWriteServiceImpl.create`). Verify after S2e: every `Persister` interface
method on the dispatcher must be `@Transactional` or be in a class with
class-level `@Transactional`.

### 5.8 doPersist(Collection<T>) cascade path
`AbstractPersister.doPersist(Collection<T>, Map)` is `protected final` and is
called by `EeWriteServiceImpl` twice (for ExperimentalDesign.types and
ExperimentalFactor.annotations — both Characteristic collections). Per §5,
this becomes `persisterHelper.persist(Collection<T>)` on the public interface.
The semantic difference: `persist(Collection<T>)` opens its own FlushMode
window, whereas the inherited `doPersist(Collection<T>, Map)` did not. Since
`EeWriteServiceImpl.create` already has FlushMode.MANUAL open at the outer
level, calling the public `persist(Collection)` would nest a second window;
Hibernate is fine with nested FlushMode sets but it's noise. Cleaner
alternative: make the dispatcher expose a public `doPersist(Collection<T>,
Map<String, ExternalDatabase>)` that *does not* manage FlushMode, just
delegates to a loop of `doPersist(T, xdbCache)`. Document the contract.

### 5.9 Don't try to flatten `Persister` interface methods in this S2
Step S5 (flatten into per-entity `*WriteService`) is explicitly deferred. S2's
goal is only to break the inheritance chain and replace it with autowired
collaboration. The public `Persister.persist(T)` interface stays unchanged;
external callers see no API difference.

---

## 6. Compile-and-test bar

After each commit:
```
cd <worktree> && mvn -pl gemma-core,gemma-cli,gemma-rest test-compile -q
```
After S2e (full chain broken):
```
mvn -pl gemma-core verify -q -DfailIfNoTests=false -Dtest='*Persister*Test,*EeWriteService*Test,PersisterHelperTest'
```
After S2f (consumer cleanup):
```
mvn -pl gemma-core verify -q  # full test suite — EE persistence is exercised by many tests
```

---

## 7. Effort

| Step | Files touched | Risk | Estimate |
|---|---|---|---|
| S2a — CommonPersister | 2 (Common, Abstract) | Low | 30 min |
| S2b — GenomePersister | 3 (Common, Genome, Abstract) | Low | 45 min |
| S2c — ArrayDesignPersister | 2 (AD, Genome) | Low | 30 min |
| S2d — RelationshipPersister | 2 (Rel, PersisterHelperImpl @Lazy) | Medium (cycle) | 45 min |
| S2e — PersisterHelperImpl + delete AbstractPersister | 2 (PHI, Abstract) | Medium (FlushMode ownership) | 1 hr |
| S2f — EeWriteServiceImpl direct autowires | 1 | Low | 45 min |
| **Total** | 6 files | — | **~4 hours / 1 session** |

The original recce estimated 2 sessions for S2. This detail breakdown suggests
1 focused session is realistic if all six sub-steps land cleanly; budget 2
sessions if test-context fallout shows up in S2a/S2e.

---

## 8. Recommendation

Execute S2a → S2b → S2c → S2d → S2e in one session. Defer S2f to a follow-on
commit (it's a pure consumer cleanup with no behavioural effect once S2a–e
have shipped). After S2e the chain is gone and `AbstractPersister` is deleted;
`EeWriteServiceImpl` continues to work through its existing `persisterHelper`
indirection until S2f lands.

The single critical gotcha is §5.3 (RelationshipPersister recursive
dispatch). Confirm the `@Lazy` approach with a smoke test before committing
S2d — start the Spring context and call
`persisterHelper.persist(ExpressionExperimentSet)` with a single transient
member. If `@Lazy` doesn't break the cycle cleanly, fall back to the
"throw if member id null" alternative in §5.3.
