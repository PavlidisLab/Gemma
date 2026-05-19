# persisterHelper retirement -- cache-lift recce

**Date:** 2026-05-19
**Branch baseline:** `phase2-acl-migrate` HEAD `732be5240c`
**Status:** doc-only -- no code changes.
**Predecessors:**
- `PERSISTER_BK_STEP1_RECCE.md` (Sec 4.2, Sec 6.1, Sec 6.2.quater) -- flagged
  the cache-lifetime question as the blocker for sweeping the four remaining
  `CommonPersister.persistXxx` methods.
- `PERSISTER_DELETION_PLAN.md` -- the wider persister-chain teardown plan.

This recce answers a single question: **where do the per-call `Caches`
maps live after the persister chain is deleted?** It is a prerequisite
for sweeping `persistExternalDatabase`, `persistDatabaseEntry`,
`persistQuantitationType`, and `persistBibliographicReference` out of
`CommonPersister`.

The short answer up front: **adopt the
`GeneWriteServiceImpl`-style local-cache pattern -- a per-call
`Map<String, ExternalDatabase>` (or equivalent) passed down the
fillIn helper stack. It is already in main, already exercises the
exact pattern we want, and trivialises the sweep.**

---

## 1. The four remaining methods and their cache use

All four live in `CommonPersister` (gemma-core
`ubic/gemma/persistence/persister/CommonPersister.java`). All four
take a `Caches caches` parameter threaded from `persist(...)` via
`AbstractPersister.Caches.empty(null)` (`AbstractPersister.java`
L101).

### 1.1 `persistExternalDatabase(ExternalDatabase, Caches)` (L94-110)

```java
Map<String, ExternalDatabase> seenDatabases = caches.getExternalDatabaseCache();
String name = database.getName();
if ( seenDatabases.containsKey( name ) ) {
    return seenDatabases.get( name );
}
ExternalDatabase existingDatabase = externalDatabaseDao.find( database );
if ( existingDatabase == null ) {
    database = externalDatabaseDao.create( database );
} else {
    database = existingDatabase;
}
seenDatabases.put( database.getName(), database );
return database;
```

**Use:** within-import memoisation, keyed by `ExternalDatabase.name`
(the only field in the BK). Avoids re-running
`externalDatabaseDao.find` (which is
`findOneByProperty("name", ...)` -- a Hibernate Criteria over
`EXTERNAL_DATABASE` table) for every BioAssay accession, every gene
product accession, every BLAT searched-database reference. A typical
EE import sees `GEO`, `PubMed`, and one or two NCBI databases
referenced hundreds-to-thousands of times.

**Lifetime:** per call to `persisterHelper.persist(ee, caches)` --
i.e. one full EE-graph persist. The `Caches` instance is built fresh
at `AbstractPersister.persist` entry (line 101) and dropped on return.

### 1.2 `persistDatabaseEntry(DatabaseEntry, Caches)` (L112-121, private)

```java
entity.setExternalDatabase( this.persistExternalDatabase( entity.getExternalDatabase(), caches ) );
return databaseEntryDao.create( entity );
```

**Use:** indirect -- only consumes the cache via
`persistExternalDatabase`. The DatabaseEntry itself is never cached
(accession strings are per-entry and only unique within an external
database; create-only by design).

**Lifetime:** same as 1.1.

### 1.3 `persistQuantitationType(QuantitationType, Caches)` (L123-142)

```java
int key = qType.getName().hashCode();
if ( qType.getDescription() != null )
    key += qType.getDescription().hashCode();
Map<Integer, QuantitationType> quantitationTypeCache = caches.getQuantitationTypeCache();
if ( quantitationTypeCache.containsKey( key ) ) {
    return quantitationTypeCache.get( key );
}
QuantitationType qt = quantitationTypeDao.create( qType );
quantitationTypeCache.put( key, qt );
return qt;
```

**Use:** within-import deduplication keyed by `(name, description)`
hash, matching the `BusinessKey.matches(QT, QT)` semantics. QTs are
*deliberately* per-experiment: a QT named "Signal" in EE A is a
distinct row from a QT named "Signal" in EE B. The cache stops a
single import from creating ten thousand identical "Signal" QT rows
when ten thousand data vectors share the same QT.

This is the most behaviourally load-bearing of the four caches.
Without it, every data vector that references the same QT
would create a duplicate QT row, which would break the EE.

**Lifetime:** per EE-graph persist. Cache is empty at the start of a
new import; intentionally not shared across EEs.

Caller: `EeWriteServiceImpl.fillInDesignElementDataVectorAssociations`
(L359), once per `BulkExpressionDataVector`. The same QT instance is
passed in many times across the data-vector collection of an EE.

### 1.4 `persistBibliographicReference(BibliographicReference, Caches)` (L144-151, private)

```java
this.fillInDatabaseEntry( reference.getPubAccession(), caches );
BibliographicReference existing = bibliographicReferenceDao.find( reference );
return existing != null ? existing : bibliographicReferenceDao.create( reference );
```

**Use:** indirect -- only consumes the cache via
`fillInDatabaseEntry` -> `persistExternalDatabase`. The
BibRef itself is *not* cached; the DAO `find(BibliographicReference)`
queries by `pubAccession.accession` (L183-192 of `BibliographicReferenceDaoImpl`).

**Lifetime:** same as 1.1.

### 1.5 Summary

| method | cache directly read/written | cache used indirectly | behavioural function |
|---|---|---|---|
| `persistExternalDatabase` | `externalDatabaseCache` | -- | dedupe `find` round-trips for repeat names within one import |
| `persistDatabaseEntry` | -- | `externalDatabaseCache` (via 1.1) | dedupe `find` round-trips on the entry's externalDatabase |
| `persistQuantitationType` | `quantitationTypeCache` | -- | **prevent duplicate QT rows for the same (name, description) within one EE** |
| `persistBibliographicReference` | -- | `externalDatabaseCache` (via fillInDatabaseEntry) | dedupe `find` round-trips on the BibRef's pubAccession externalDatabase |

Only `persistQuantitationType` has behaviour that *materially*
depends on the cache; the other three are pure performance
optimisations (avoid repeat `findOneByProperty("name", "GEO")` calls).

---

## 2. The `Caches` value object

`AbstractPersister.Caches` (`AbstractPersister.java` L68-90) is a
Lombok `@Value` immutable holder for six maps:

```java
@With
@Value(staticConstructor = "empty")
protected static class Caches {
    @Nullable
    ArrayDesignsForExperimentCache arrayDesignCache;
    Map<String, ExternalDatabase> externalDatabaseCache = new HashMap<>();
    Map<Object, Taxon> taxonCache = new HashMap<>();
    Map<Integer, Chromosome> chromosomeCache = new HashMap<>();
    Map<Integer, QuantitationType> quantitationTypeCache = new HashMap<>();
    Map<Integer, BioAssayDimension> bioAssayDimensionCache = new HashMap<>();
}
```

Each map has the same lifetime: built at the entry of
`AbstractPersister.persist`, dropped at return. Threaded through
every `doPersist`/`persistXxx`/`fillInXxx` call in the chain.

The four we care about for this recce are only two of those six
maps: `externalDatabaseCache` and `quantitationTypeCache`. The other
four (`arrayDesignCache`, `taxonCache`, `chromosomeCache`,
`bioAssayDimensionCache`) are consumed by `GenomePersister` /
`EeWriteServiceImpl` and are already on independent migration
tracks.

---

## 3. The fundamental tension

If we naively inline `dao.find / dao.create` at the call sites (the
Contact pilot pattern, Sec 6.2.bis of the BK recce), three things
happen:

1. **Repeat `find` round-trips.** Every BioAssay accession in a
   large EE triggers a `findOneByProperty("name", "GEO")` against
   the `EXTERNAL_DATABASE` table -- a few hundred extra Criteria
   queries per import in the typical case.
2. **Hibernate L1 + L2 partially mitigate this** -- but not
   completely. `ExternalDatabase` is mapped
   `<cache usage="nonstrict-read-write"/>` (verified in
   `ExternalDatabase.hbm.xml` L6), so a hit on the secondary cache
   returns the entity by ID. But the query path is a *Criteria
   on `name`*, not a load by ID, so the L2 entity cache does not
   short-circuit it -- only the L2 query cache could, and that is
   off by default. So each repeat `find` is a real SQL `SELECT ...
   WHERE name=?`, even though it returns the same row.
3. **Duplicate QT rows.** For `persistQuantitationType`,
   inlining `dao.find / dao.create` *without* a cache breaks
   correctness, not just performance. The DAO `find(QT)` matches
   any existing QT with the same `(name, description)` -- which
   means within a single EE import, the first call creates a row,
   and subsequent calls find that row. **But only if Hibernate
   has flushed it**, and `AbstractPersister.persist` sets
   `FlushMode.MANUAL` (L99) -- so the create is not visible to
   subsequent finds *until end of transaction*. Without the
   cache, every data vector that references the same QT object
   would attempt a new `create`, producing N duplicate rows for
   N vectors.

The QT case is the load-bearing one. The ExternalDatabase case is
purely a perf optimisation.

---

## 4. Proposed solutions

### 4.1 Option A: per-method memoisation at the call site (recommended)

Push the cache one level up -- from the `Caches` value object into
the caller (`EeWriteServiceImpl` for QT + most XDB references;
`GeneWriteServiceImpl` for the rest of the XDB references;
`GeoServiceImpl` for BibRef).

The pattern already exists in main: **`GeneWriteServiceImpl` already
does exactly this** (`GeneWriteServiceImpl.java` L622-651):

```java
private void fillInDatabaseEntry( DatabaseEntry databaseEntry ) {
    this.fillInDatabaseEntry( databaseEntry, new HashMap<String, ExternalDatabase>() );
}

private void fillInDatabaseEntry( DatabaseEntry databaseEntry, Map<String, ExternalDatabase> externalDbCache ) {
    // ...
    ExternalDatabase persistedDb = this.persistExternalDatabase( tempExternalDb, externalDbCache );
    // ...
}

private ExternalDatabase persistExternalDatabase( ExternalDatabase database, Map<String, ExternalDatabase> externalDbCache ) {
    String name = database.getName();
    if ( name != null && externalDbCache.containsKey( name ) ) {
        return externalDbCache.get( name );
    }
    ExternalDatabase existingDatabase = externalDatabaseDao.find( database );
    ExternalDatabase resolved = existingDatabase != null ? existingDatabase : externalDatabaseDao.create( database );
    if ( name != null ) {
        externalDbCache.put( name, resolved );
    }
    return resolved;
}
```

Pros:

- **Already in main.** This is not a hypothetical pattern -- it's
  the proven copy that `GeneWriteServiceImpl` adopted when the
  Phase 3 sweep peeled gene-write off the persister chain. The
  comment at L618-620 explicitly calls it out as the migration
  pattern ("These are cache-free copies. Hibernate L1 covers
  within-transaction identity. Once the cutover lands,
  GenomePersister's copies go away.").
- **Cache lifetime is explicit.** A local `Map` in the top-level
  service method has obvious lifetime: one method call. No more
  `Caches` value object threading through five layers.
- **Same correctness guarantees as today.** Within one import,
  QT and XDB lookups still hit the cache; across imports the
  cache is fresh.
- **Easy to delete later** if Hibernate's L2 query cache ever
  gets turned on for `ExternalDatabase`/`QuantitationType`.

Cons:

- **Code duplication.** Each consuming service ends up with its
  own copy of `persistExternalDatabase(XDB, Map)`. Today there
  are exactly three consumers (EeWriteService, GeneWriteService,
  and any future BibRef caller). The duplication has not been
  judged a problem in the GeneWriteService cutover; unlikely to
  be a problem here.
- **Spreads the cache key contract.** The `(name, description)`
  hash key for QT and the `name` key for XDB get copy-pasted.
  Mitigation: the keys are simple enough to inline; the BK
  contract lives in `BusinessKey.matches(QT, QT)` and the DAO
  `find()`, both of which are unchanged.

### 4.2 Option B: DAO-level `findOrCreate` with caching

Push the cache down into the DAO. Add (or use existing)
`findOrCreate(T)` on `ExternalDatabaseDao` and
`QuantitationTypeDao`, backed by Hibernate's L2 query cache
(turn it on for these entities).

Pros:

- Single source of truth for the find-or-create semantics.
- Cache is automatic; callers see a clean `dao.findOrCreate(...)`.

Cons:

- **L2 query cache is currently off.** Turning it on globally
  is a configuration change with non-trivial blast radius
  (cache invalidation semantics, memory pressure, integration-test
  flakiness). Turning it on for just two entities means a
  selective `@QueryCacheable` annotation pass plus
  `@Cacheable` query hints in the DAO -- moderate effort.
- **`FlushMode.MANUAL` problem persists for QT.** The
  manual-flush window is set by `AbstractPersister.persist`;
  once the persister is deleted, the caller (`EeWriteService.persist`)
  has to set the same flush mode. Even then, the DAO-level
  `findOrCreate` doesn't see the in-flight `create` until
  flush, so it would still produce duplicate QT rows under the
  current flush regime. To fix it, `findOrCreate(QT)` would
  need its own in-memory cache *inside the DAO* -- which is
  Option A re-implemented in the wrong layer.
- **Wrong layer for the QT semantics.** A DAO is conceptually
  stateless across calls; smuggling per-transaction state in
  via a `ThreadLocal` or an injected `Caches` re-creates today's
  problem with extra ceremony.

### 4.3 Option C: drop the cache entirely

Replace the four methods with inline `dao.find / dao.create`
two-liners. Accept the extra `find` round-trips. For QT,
either rely on the entity-identity-via-object-reference
(callers re-use the same `QuantitationType` instance across
data vectors, which is already the case in `EeWriteServiceImpl`)
or rely on the QT's id being set after the first `create` (it
is, by Hibernate's `save`-generates-id semantics, even under
`MANUAL` flush).

Pros:

- Minimal code. No cache maps anywhere.

Cons:

- **QT correctness depends on object-identity discipline at
  the caller.** Today's code already mostly does this -- the
  QT cache key is hash-of-(name,description), so it works
  even when distinct-but-equal QT instances are passed in. If
  any caller ever constructs two distinct QT objects with the
  same (name, description) within one import, dropping the
  cache produces duplicate rows. A grep of the codebase
  suggests this doesn't happen today, but it's a foot-gun for
  future imports.
- **Measurable perf regression on large imports.**
  Hundreds-to-thousands of extra `findOneByProperty("name", ...)`
  queries per EE. Each query is cheap, but they add up; on a
  large GEO import this could be 10-30 seconds of pointless
  round-trips. Not catastrophic, but visible.

### 4.4 Option D: Hibernate L2 + restructure (long-game)

Turn on Hibernate L2 query caching for `ExternalDatabase` and
`QuantitationType` queries, configure the cache region, and
rely on Hibernate to do the memoisation transparently.

Pros:

- Architecturally clean; uses the framework's built-in
  mechanism.

Cons:

- **Doesn't solve the QT-during-MANUAL-flush problem** (see
  4.2).
- **Scope explosion.** Touching the L2 query cache requires a
  Phase 4+ effort -- it would interact with the new entity
  caches we just configured for ACL/security, with the
  hot-path read services, and with test isolation. Out of
  scope for persisterHelper retirement.

---

## 5. Recommendation

**Adopt Option A (per-method memoisation at the call site, using
the `GeneWriteServiceImpl` pattern).**

Rationale:

1. The pattern already exists in main, is already exercised on
   production code paths (gene + gene-product imports), and has
   already been validated by the Phase 3 sweep. Adopting it for
   the four remaining methods is a 1:1 copy of an in-production
   pattern, not a new design.
2. It preserves today's correctness (QT dedup works) and today's
   performance (XDB find round-trips dedupe) without introducing
   any new framework layer.
3. It dissolves the `Caches` value object cleanly. Once all four
   methods are migrated, `externalDatabaseCache` and
   `quantitationTypeCache` can be removed from `Caches`; the
   remaining four maps stay until their owning persister is
   deleted, and the whole class disappears with the persister
   chain.
4. It is straightforwardly revertable. If a perf regression
   surfaces on a real import, we can re-introduce the map at any
   level of the call stack.
5. Option B (DAO-level) re-creates today's problem in the wrong
   layer; Option C trades 10-30 seconds of round-trip time for a
   future-foot-gun; Option D is out of scope.

---

## 6. Concrete next steps

The actual cache-lift implementation (separate PRs from this recce):

### 6.1 `persistExternalDatabase` + `persistDatabaseEntry` lift

Callers of `persistExternalDatabase` outside `CommonPersister`:

- `ArrayDesignPersister.persistNewArrayDesign` (L118) -- one call site.
- `GenomePersister` -- four call sites (L183, L456, L659, L680, L827,
  L846), all via `fillInDatabaseEntry` or directly on a BLAT result's
  searched-database.
- `EeWriteServiceImpl.fillInBioAssayAssociations` (L315),
  `EeWriteServiceImpl.persistExpressionExperiment` (L189 via
  `fillInDatabaseEntry`), `EeWriteServiceImpl` L482 (via
  `fillInDatabaseEntry`).
- `GeneWriteServiceImpl` -- already has its own copy (L634-651).

Steps:

1. Move `persistExternalDatabase` + `fillInDatabaseEntry` into
   `EeWriteServiceImpl` as private helpers, mirroring the
   `GeneWriteServiceImpl` shape. Use a local
   `Map<String, ExternalDatabase>` instantiated at
   `persistExpressionExperiment` entry and threaded down.
2. Move `persistExternalDatabase` + `fillInDatabaseEntry` into
   `GenomePersister` (or its successor write service) the same
   way. (GenomePersister still has not been fully cut over; this
   lift may end up living in the successor.)
3. Update `ArrayDesignPersister.persistNewArrayDesign` to use a
   local map (single call site, single XDB per call -- a
   no-arg overload taking `new HashMap<>()` works).
4. Delete `CommonPersister.persistExternalDatabase`,
   `CommonPersister.persistDatabaseEntry`,
   `CommonPersister.fillInDatabaseEntry`, the `instanceof ExternalDatabase`
   and `instanceof DatabaseEntry` arms of `doPersist`, and the
   `ExternalDatabaseDao` + `DatabaseEntryDao` `@Autowired` fields.
5. Delete `Caches.externalDatabaseCache` field.
6. `mvn -pl gemma-core,gemma-cli,gemma-rest test-compile` clean.
7. Smoke: `LoadExperimentCli` on a small GEO fixture (e.g.
   GSE40191) against gemdtest, verify identical row counts in
   `EXTERNAL_DATABASE`, `DATABASE_ENTRY`.

### 6.2 `persistQuantitationType` lift

Single caller: `EeWriteServiceImpl.fillInDesignElementDataVectorAssociations`
(L359).

Steps:

1. Add a private `findOrCreateQuantitationType(QT, Map<Integer, QT>)`
   helper in `EeWriteServiceImpl`, mirroring the existing
   `CommonPersister` logic (key = name.hashCode() +
   description.hashCode()).
2. Instantiate the map at `persistExpressionExperiment` entry
   (alongside the XDB map from 6.1).
3. Thread the map down to
   `fillInDesignElementDataVectorAssociations` (one extra
   parameter on the call chain).
4. Replace the `persister().persistQuantitationType(...)` call
   with `findOrCreateQuantitationType(...)`.
5. Delete `CommonPersister.persistQuantitationType`, the
   `instanceof QuantitationType` arm of `doPersist`, and the
   `QuantitationTypeDao` `@Autowired` field.
6. Delete `Caches.quantitationTypeCache` field.
7. `mvn -pl gemma-core,gemma-cli,gemma-rest test-compile` clean.
8. Smoke: `LoadExperimentCli` against gemdtest, verify exactly N
   QT rows for an EE with N distinct QT names (not N * vectors).

### 6.3 `persistBibliographicReference` lift

Single caller path: `GeoServiceImpl` -> `persisterHelper.persist(BibRef)`
-> `CommonPersister.doPersist` -> `persistBibliographicReference`.

Steps:

1. Inline at the GEO call site:
   `BibliographicReference existing = bibliographicReferenceDao.find(ref);`
   `BibliographicReference resolved = existing != null ? existing : bibliographicReferenceDao.create(ref);`
2. Resolve `ref.getPubAccession().getExternalDatabase()` via
   the local XDB cache (from 6.1) -- or, if `GeoServiceImpl`
   only resolves one BibRef per call, a fresh
   `new HashMap<>()` is fine.
3. Delete `CommonPersister.persistBibliographicReference`, the
   `instanceof BibliographicReference` arm of `doPersist`, and the
   `BibliographicReferenceDao` `@Autowired` field.

### 6.4 Final cleanup

After all three migrations:

- `CommonPersister.doPersist` is down to two arms: `User`
  (throws) and `Characteristic` (no-op return null). Both
  degenerate; class can be deleted, dispatch arms removed
  from the chain.
- `AbstractPersister.Caches` keeps `arrayDesignCache`,
  `taxonCache`, `chromosomeCache`, `bioAssayDimensionCache` --
  all consumed by `GenomePersister` and `EeWriteServiceImpl`.
  Final removal of `Caches` blocks on `GenomePersister`
  cutover (out of scope here).

### 6.5 Acceptance checks

- `grep -rn 'persistExternalDatabase\|persistDatabaseEntry\|persistQuantitationType\|persistBibliographicReference' gemma-core/src/main gemma-cli/src/main`
  returns only the in-`EeWriteServiceImpl` (and possibly
  `GenomePersister` successor) private helpers, no
  cross-persister calls.
- `grep -rn 'caches\.getExternalDatabaseCache\|caches\.getQuantitationTypeCache'` returns zero.
- One smoke run of `LoadExperimentCli` on a small GEO fixture
  produces identical `EXTERNAL_DATABASE`, `DATABASE_ENTRY`,
  `QUANTITATION_TYPE`, and `BIBLIOGRAPHIC_REFERENCE` row counts to
  the pre-change baseline.

---

## 7. Effort estimate

| Task | Effort |
|---|---:|
| 6.1 ExternalDatabase + DatabaseEntry lift (3 call sites, ~50 LoC moved) | 0.75 session |
| 6.2 QuantitationType lift (1 call site, ~25 LoC moved) | 0.5 session |
| 6.3 BibliographicReference lift (1 call site, ~10 LoC moved) | 0.25 session |
| 6.4 Cleanup of `CommonPersister`, `Caches`, dispatch arms | 0.25 session |
| Validation (test-compile, smoke run against gemdtest) | 0.25 session |
| **Total** | **~2 sessions** |

Risk: low. The pattern is in production today
(`GeneWriteServiceImpl`); the four call sites are well-isolated; the
only behavioural correctness concern (QT dedup) is preserved by the
local-map pattern. Rollback is trivial -- revert the per-method
commit.

---

## 8. Bottom line

- Of the four remaining `CommonPersister.persistXxx` methods, only
  `persistQuantitationType` has behaviourally load-bearing cache
  semantics. The other three are pure perf optimisations.
- The `GeneWriteServiceImpl` private-helper pattern (local
  `Map<String, ExternalDatabase>` threaded into a per-call helper)
  is already in production and is the right shape for the lift.
- **Recommended: Option A**, executed in the order
  ExternalDatabase + DatabaseEntry -> QuantitationType ->
  BibliographicReference, then cleanup.
- Total effort: **~2 sessions**, low risk, trivially revertable.
- After the lift, `CommonPersister.doPersist` is down to two
  degenerate arms (User-throws, Characteristic-no-op); the class
  is deletable and the persister chain shrinks to
  `GenomePersister` + `ArrayDesignPersister` + `RelationshipPersister`
  + the thin `ExpressionPersister` delegate.
