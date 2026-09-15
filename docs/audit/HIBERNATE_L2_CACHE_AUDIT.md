# Hibernate L2 cache region audit + tuning recommendations

Branch: `worktree-hibernate-l2-tune` (baseline `08e760bdaf` =
`phase2-acl-migrate` HEAD; ehcache-CacheManager fix on
`worktree-ehcache-cachemanager-fix` is NOT yet merged here, so the local
`EhcacheConfig.java` is still the `ConcurrentMapCacheManager` stub).

This is a recce. No source modifications. All file/line references are
absolute to this worktree unless otherwise noted.

---

## 1. Cache provider configuration

From
`gemma-core/src/main/resources/ubic/gemma/applicationContext-hibernate.xml`
(lines 79-109):

```
hibernate.cache.region.factory_class      = jcache
hibernate.javax.cache.provider            = org.ehcache.jsr107.EhcacheCachingProvider
hibernate.javax.cache.missing_cache_strategy = create
hibernate.cache.use_query_cache           = true
hibernate.cache.use_second_level_cache    = true
```

What this means for Hibernate 6.4 + Ehcache 3.10 (jakarta):

- `jcache` resolves to
  `org.hibernate.cache.jcache.internal.JCacheRegionFactory`.
- Hibernate constructs its OWN `javax.cache.CacheManager` from the
  Ehcache JSR-107 provider. It is a separate `javax.cache.CacheManager`
  instance from the one Spring's application-cache layer uses
  (`EhcacheConfig#jCacheCacheManager()` on the unmerged fix branch); both
  resolve to the same provider but the instances are distinct.
- `missing_cache_strategy=create` means: when Hibernate asks the JCache
  provider for a cache region that hasn't been pre-declared, the
  provider creates one **with default settings**. For Ehcache 3 with no
  XML or programmatic config, "default" means **unbounded heap, no
  TTL, no eviction**. This is the same latent leak vector that the
  application-level `ConcurrentMapCacheManager` stub had; for L2 it is
  still present today.
- No `hibernate.javax.cache.uri` / `javax.cache.cacheManager.uri` is set,
  and `EhcacheCachingProvider#getDefaultURI()` does NOT scan the
  classpath for the legacy v2 `ehcache.xml`. It is the same provider
  for app + L2 but with no config URI.

### 1.1 The stale `ehcache.xml`

`gemma-core/src/main/resources/ehcache.xml` (468 lines) is still on the
classpath. It is **Ehcache 2.x XML format** (`<ehcache>` root,
`maxElementsInMemory`, `timeToLiveSeconds`, `overflowToDisk`,
`eternal="true"`). Ehcache 3 / JCache cannot parse this schema, and no
code reads it. It is dead config but a useful historical reference for
sizing — pre-migration this is where every L2 region had a heap cap +
TTL.

(See `worktree-aspectj-ehcache-audit/ASPECTJ_EHCACHE_AUDIT.md`
section 2.3 for the same finding from the upstream audit.)

---

## 2. @Cache inventory (HBM XML)

Zero `@Cache` / `@Cacheable` annotations on Java entities — all
Hibernate L2 caching is declared via HBM XML `<cache usage="...">`
elements. Cache regions for entities default to the FQCN; collection
regions default to FQCN + "." + role.

### 2.1 Cached entities (52 total)

Strategy distribution:

| strategy | count |
|---|---|
| read-only | 18 |
| read-write | 27 |
| nonstrict-read-write | 7 |
| transactional | 0 |

Full list (region name = FQCN unless noted):

**read-only** (immutable entities; safest + fastest L2 strategy)

| region | mutable | hbm file |
|---|---|---|
| `ubic.gemma.model.analysis.AnalysisResultSet` | false | AnalysisResultSet.hbm.xml |
| `ubic.gemma.model.blacklist.BlacklistedEntity` | false | BlacklistedEntity.hbm.xml |
| `ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix` | false | SampleCoexpressionMatrix.hbm.xml |
| `ubic.gemma.model.analysis.expression.diff.HitListSize` | false | HitListSize.hbm.xml |
| `ubic.gemma.model.analysis.expression.diff.PvalueDistribution` | false | PvalueDistribution.hbm.xml |
| `ubic.gemma.model.analysis.expression.pca.Eigenvalue` | false | Eigenvalue.hbm.xml |
| `ubic.gemma.model.analysis.expression.pca.Eigenvector` | false | Eigenvector.hbm.xml |
| `ubic.gemma.model.association.Gene2GOAssociation` | false | Gene2GOAssociation.hbm.xml |
| `ubic.gemma.model.common.auditAndSecurity.AuditEvent` | false | AuditEvent.hbm.xml |
| `ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType` | false | AuditEventType.hbm.xml |
| `ubic.gemma.model.common.description.DatabaseEntry` | false | DatabaseEntry.hbm.xml |
| `ubic.gemma.model.common.measurement.Unit` | false | Unit.hbm.xml |
| `ubic.gemma.model.common.protocol.Protocol` | false | Protocol.hbm.xml |
| `ubic.gemma.model.expression.bioAssayData.BioAssayDimension` | false | BioAssayDimension.hbm.xml |
| `ubic.gemma.model.expression.bioAssayData.GenericCellLevelCharacteristics` | false | CellLevelCharacteristics.hbm.xml |
| `ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation` | false | MeanVarianceRelation.hbm.xml |
| `ubic.gemma.model.expression.bioAssayData.SingleCellDimension` | false | SingleCellDimension.hbm.xml |
| `ubic.gemma.model.expression.biomaterial.Compound` | false | Compound.hbm.xml |
| `ubic.gemma.model.genome.Chromosome` | false | Chromosome.hbm.xml |
| `ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult` | false | SequenceSimilaritySearchResult.hbm.xml |

**read-write** (mutable + needs serializable isolation through L2)

`Investigation`, `ExpressionExperimentSet`,
`BioSequence2GeneProduct`, `Contact`, `GroupAuthority`, `JobInfo`,
`UserGroup`, `CurationDetails`, `BibRefAnnotation`, `Measurement`,
`AlternateName`, `ArrayDesign`, `BioAssay`, `BioMaterial`, `Treatment`,
`ExperimentalDesign`, `ExperimentalFactor`, `FactorValue`, `Geeq`,
`PhysicalLocation`, `GeneAlias`, `GeneSet`, `GeneSetMember`,
`Multifunctionality`. (24 plus the joined-subclasses that inherit from
some of these.)

**nonstrict-read-write** (very stale-tolerant; cheaper than read-write
but doesn't lock — only correct when the application can tolerate
out-of-date L2 reads after a write)

| region |
|---|
| `ubic.gemma.model.analysis.Analysis` |
| `ubic.gemma.model.common.description.BibliographicReference` |
| `ubic.gemma.model.common.description.ExternalDatabase` |
| `ubic.gemma.model.common.quantitationtype.QuantitationType` |
| `ubic.gemma.model.expression.designElement.CompositeSequence` |
| `ubic.gemma.model.genome.ChromosomeFeature` |
| `ubic.gemma.model.genome.Taxon` |
| `ubic.gemma.model.genome.biosequence.BioSequence` |

### 2.2 Cached collections (39 total)

Collection regions are auto-named `<owner-fqcn>.<role>` and are
SEPARATE from the entity region. Each gets its own JCache cache.

`read-only` collections (mutable=false owners):

- `Analysis.resultSets`, `Analysis.eigenValues`, `Analysis.eigenVectors`
- `AnalysisResultSet.hitListSizes`
- `SingleCellDimension.bioAssays`

`read-write` collections (mutable owners): all others — see the parser
output earlier in the session for the exhaustive list. Notable
high-cardinality ones:

- `ArrayDesign.compositeSequences` — one row per probe (~50k for a
  typical platform; ~600k for some Affy clariom arrays). The OWNING
  collection cache stores IDs only, not the CompositeSequence entities,
  so cardinality is bounded by `#platforms * mean-probes-per-platform`.
- `BioMaterial.factorValues`, `BioMaterial.characteristics`,
  `BioMaterial.bioAssaysUsedIn` — small per-EE but ~1M biomaterials in
  prod = a lot of regions populated.
- `ExperimentalDesign.experimentalFactors`,
  `ExperimentalFactor.factorValues`, `FactorValue.characteristics` —
  small per-EE; in the design-editor path these are hot reads.
- `BibliographicReference.{annotations,meshTerms,keywords,chemicals}` —
  per-publication metadata.
- `ChromosomeFeature.{accessions,products,aliases}` — gene-level
  metadata; hot for every gene-page render.

---

## 3. Region names Hibernate will use

Hibernate's `JCacheRegionFactory` will request, from the JCache
provider, the following region names (one cache per name):

- One per cached entity: the FQCN. ~52 entities. Subclasses share their
  root class's region (single table inheritance through `<class>` +
  `<joined-subclass>` / `<subclass>` chains rooted on the cached
  parent), unless the subclass declares its own `<cache>`.
- One per cached collection role: `<owner-fqcn>.<role>`. ~39 roles.
- The query cache: `org.hibernate.cache.internal.StandardQueryCache`
  (one region for all `setCacheable(true)` queries; Hibernate 6 still
  uses this single region by default).
- The query-cache invalidation index:
  `org.hibernate.cache.spi.TimestampsRegion` (sometimes still called
  `UpdateTimestampsCache` in older docs; Hibernate 6 normalizes to
  the new spi name internally, but JCache may see either depending on
  which Hibernate release is on the classpath).

Total: ~93 distinct L2 regions, **all currently auto-created with
unbounded heap + no TTL** because `missing_cache_strategy=create` and
no programmatic L2 configuration exists.

---

## 4. Query cache usage

`hibernate.cache.use_query_cache=true` and 49 distinct
`setCacheable(true)` call sites across the codebase. Concentration:

| file | sites |
|---|---|
| `persistence/service/expression/experiment/ExpressionExperimentDaoImpl.java` | 24 |
| `persistence/service/expression/arrayDesign/ArrayDesignDaoImpl.java` | 9 |
| `persistence/service/analysis/expression/diff/DifferentialExpressionResultDaoImpl.java` | 4 |
| `persistence/service/analysis/expression/diff/ExpressionAnalysisResultSetDaoImpl.java` | 3 |
| `persistence/service/AbstractQueryFilteringVoEnabledDao.java` | 7 (parameterized via `cacheable`) |
| `persistence/util/CommonQueries.java` | 1 |
| `persistence/service/common/description/CharacteristicDaoImpl.java` | 1 |

Patterns observed:

- The biggest cluster is the EE filtering / VO loading path
  (`AbstractQueryFilteringVoEnabledDao` + `ExpressionExperimentDaoImpl`)
  — every list/count under filters/sort/paging is cached. These flow
  through the catalog / homepage / search.
- `ArrayDesignDaoImpl` caches per-platform metadata (composite
  sequences, taxa, etc.) — hot on platform pages.
- `setCacheRegion(...)` is NOT used anywhere — every query goes to the
  default `StandardQueryCache` region. That means one cache with
  thousands of unrelated query-result entries, all sharing the same
  size cap and TTL.

---

## 5. Sizing recommendations (per-region)

These are conservative starting points seeded from `ehcache.xml`
(Ehcache 2.x — see Section 1.1) and adjusted where the legacy values
looked suspicious. Tune from prod cache-hit metrics once
`MeterRegistryJCacheConfigurer` is wired (the binding the ehcache
fix unblocks; see `EhcacheConfig.java` Javadoc on the fix branch).

**Convention:** "eternal" = no TTL, rely on Hibernate write
invalidations; safe for `read-only` + immutable entities. "TTL=20m" =
`Duration.ofMinutes(20)` (matches the legacy `timeToLiveSeconds="1200"`
default).

### 5.1 High-priority L2 caches (catalog / homepage / search hot path)

| region | heap | TTL | notes |
|---|---|---|---|
| `ubic.gemma.model.expression.experiment.ExpressionExperiment` | 10000 | 20m | catalog + every EE page |
| `...ExpressionExperiment.characteristics` | 10000 | 20m | EE annotations |
| `...ExpressionExperiment.quantitationTypes` | 10000 | 20m | |
| `...ExpressionExperiment.bioAssays` | 10000 | 20m | |
| `...ExpressionExperiment.otherParts` | 10000 | 20m | split-EE relation |
| `...arrayDesign.ArrayDesign` | 10000 | 20m | one per platform; ~1k platforms in prod |
| `...arrayDesign.ArrayDesign.compositeSequences` | 10000 | 20m | IDs only |
| `...designElement.CompositeSequence` | 200000 | 20m | probes; legacy already had 200k |
| `...genome.Gene` | 50000 | 20m | (Gene has no `<cache>` directly — see Open Questions) |
| `...genome.Taxon` | 100 | eternal | ~20 taxa; immutable in practice |

### 5.2 Immutable / read-only entities (eternal, modest heap)

| region | heap | TTL |
|---|---|---|
| `...analysis.AnalysisResultSet` | 10000 | eternal |
| `...analysis.expression.diff.PvalueDistribution` | 10000 | eternal |
| `...analysis.expression.diff.HitListSize` | 100000 | eternal |
| `...analysis.expression.pca.Eigenvalue` | 100000 | eternal |
| `...analysis.expression.pca.Eigenvector` | 100000 | eternal |
| `...analysis.expression.coexpression.SampleCoexpressionMatrix` | 1000 | eternal |
| `...association.Gene2GOAssociation` | 10000 | eternal |
| `...common.auditAndSecurity.AuditEvent` | 10000 | eternal |
| `...common.auditAndSecurity.eventType.AuditEventType` | 1000 | eternal |
| `...common.description.DatabaseEntry` | 200000 | eternal |
| `...common.measurement.Unit` | 1000 | eternal |
| `...common.protocol.Protocol` | 10000 | eternal |
| `...expression.bioAssayData.BioAssayDimension` | 10000 | eternal |
| `...expression.bioAssayData.MeanVarianceRelation` | 10000 | eternal |
| `...expression.bioAssayData.SingleCellDimension` | 10000 | eternal |
| `...expression.bioAssayData.GenericCellLevelCharacteristics` | 10000 | eternal |
| `...expression.biomaterial.Compound` | 1000 | eternal |
| `...genome.Chromosome` | 1200 | eternal |
| `...genome.sequenceAnalysis.SequenceSimilaritySearchResult` | 100000 | eternal |
| `...blacklist.BlacklistedEntity` | 1000 | eternal |

### 5.3 Mutable entities (read-write / nonstrict, 20m TTL, modest heap)

10k heap + 20m TTL each unless noted. Full list of region names is in
Section 2.1; this covers all read-write + nonstrict-read-write entities
above.

Exceptions where the legacy ehcache.xml had a larger heap (keep the
larger value):

| region | heap |
|---|---|
| `...expression.experiment.FactorValue` | 100000 |
| `...expression.bioAssay.BioAssay` | 100000 |
| `...genome.biosequence.BioSequence` | 200000 |
| `...genome.gene.GeneAlias` | 50000 |
| `...genome.gene.GeneSetMember` | 50000 |
| `...genome.gene.Multifunctionality` | 50000 |
| `...genome.ChromosomeFeature` | 200000 |
| `...common.description.Characteristic` | 200000 |
| `...association.BioSequence2GeneProduct` | 100000 |

### 5.4 Collection caches

Default each cached collection to 10000 heap + 20m TTL. Exceptions
(matching legacy):

| region | heap | TTL |
|---|---|---|
| `...BioMaterial.characteristics` | 20000 | 20m |
| `...BioMaterial.treatments` | 1000 | 20m |
| `...GeneSet.characteristics` | 1000 | 20m |
| `...GeneSet.members` | 1000 | 20m |
| `...Investigation.characteristics` | 1000 | 20m |
| `...AnalysisResultSet.hitListSizes` | 1000 | eternal |
| `...SingleCellDimension.bioAssays` | 10000 | eternal |
| `...ExternalDatabase.externalDatabases` | 100 | 20m |

### 5.5 Query cache + timestamps

| region | heap | TTL |
|---|---|---|
| `org.hibernate.cache.internal.StandardQueryCache` | 5000 | 20m |
| `org.hibernate.cache.spi.TimestampsRegion` | 5000 | eternal |

The legacy ehcache.xml had `5000 / 1200s` and `5000 / eternal`; keep
these. The TimestampsRegion MUST be eternal (eviction would let stale
query results survive a write).

---

## 6. Recommendation: extend `EhcacheConfig` with L2 region declarations

After the unmerged ehcache fix (`worktree-ehcache-cachemanager-fix`)
lands, `EhcacheConfig` will pre-declare 13 named **application** caches
on Spring's `JCacheCacheManager`. That manager and Hibernate's L2
JCache manager share the same `EhcacheCachingProvider`, so adding the
L2 regions to the same `EhcacheConfig` is the natural extension point.

Two implementation options:

**Option A (preferred): expand the existing `APP_CACHES` map.**
Add the ~93 L2 region names to the LinkedHashMap with their (heap,
TTL) tuples. Pro: one config class to read, one config style, immune
to provider-instance separation since the JCache provider returns the
same `javax.cache.CacheManager` for the same URI.
Con: `EhcacheConfig` becomes 200+ lines. Acceptable.

**Option B: split out `HibernateL2CacheConfig`.** New
`@Configuration` class that fetches the same JCache `CacheManager`
(via `Caching.getCachingProvider(...).getCacheManager()`) and
pre-declares the L2 regions there. Pro: tidier separation. Con: easier
to forget which file has which region; double-bookkeeping.

In both cases, with regions pre-declared, the
`missing_cache_strategy=create` setting becomes a safety net rather
than the operating mode — any region the audit missed still works,
but the named ones are bounded.

**DO NOT** revive `ehcache.xml`. The v2 schema is unparseable by
Ehcache 3 and the file is dead config; delete it as part of the next
ehcache-touching commit.

**ALTERNATIVE if XML is preferred**: ship an Ehcache 3 config XML
(`http://www.ehcache.org/v3` namespace, `<cache alias="...">`) and
point Hibernate at it via
`hibernate.javax.cache.uri=classpath:ehcache3.xml`. The programmatic
route in `EhcacheConfig` is more maintainable for our size and matches
the existing pattern.

---

## 7. Open questions

1. **`Gene` has no `<cache>` element.**
   `gemma-core/src/main/resources/ubic/gemma/model/genome/Gene.hbm.xml`
   (NOT scanned in Section 2) — confirm. The legacy ehcache.xml has
   `ubic.gemma.model.genome.Gene` + `Gene.products` + `Gene.aliases` +
   `Gene.accessions` + `Gene.phenotypeAssociations` regions, suggesting
   either (a) `Gene` IS cached and the cache element lives in a
   superclass hbm that the parser didn't follow (ChromosomeFeature is
   the parent — IS cached), or (b) Gene relies on ChromosomeFeature's
   cache region. Hibernate's behaviour for subclasses of a cached root
   class with `<joined-subclass>` is: the subclass uses the root's
   region UNLESS it declares its own. Confirm at config-time which
   regions Hibernate actually creates by enabling Hibernate stats and
   dumping `SessionFactory.getStatistics().getSecondLevelCacheRegionNames()`.

2. **`Characteristic` cache.** Looks like its HBM has a `<cache>`
   inside a `<joined-subclass>` rather than at the top — confirm with
   `grep -n "<cache\|<class\|<joined-subclass" gemma-core/src/main/resources/ubic/gemma/model/common/description/Characteristic.hbm.xml`.

3. **Read vs write frequency in prod.** All the heap-sizing values
   above are best-guesses from legacy + entity semantics. Real
   prioritization needs the
   `io.micrometer.core.instrument.binder.cache.JCacheMetrics` binder
   (the `MeterRegistryJCacheConfigurer` follow-up unblocked by the
   ehcache fix). With per-region hit/miss/evict counters in Prometheus,
   the next pass on this file can drop dead regions and boost hot ones.

4. **Query cache sharding.** Today every `setCacheable(true)` query
   shares `StandardQueryCache`. The catalog filter / VO path issues
   thousands of distinct query keys; consider splitting via
   `.setCacheRegion("ExpressionExperiment.filteredVo")` to isolate
   eviction pressure. Cheap win if cache-hit metrics show eviction
   churn in the default region.

5. **`hibernate.javax.cache.*` vs `hibernate.jakarta.cache.*` keys.**
   Hibernate 6.4 accepts both prefixes but `jakarta.cache.*` is
   preferred. Out of scope for this audit — flag for a follow-up
   property-key rename when the L2 region declarations land.

6. **`nonstrict-read-write` correctness.** Eight entities use
   `nonstrict-read-write` (Section 2.1) — explicitly stale-tolerant.
   Confirm none of them are read in a context that requires
   read-after-write consistency (e.g., `Analysis` results during a
   freshly-run differential expression pipeline). If any do, downgrade
   to `read-write`.

7. **Subclass inheritance of cache regions.** Many entity classes
   above (`Analysis`, `Investigation`, `ChromosomeFeature`,
   `BlacklistedEntity`, `Contact`) are abstract root classes with
   `<joined-subclass>` descendants. The parser in Section 2 did not
   walk those — verify whether subclasses (e.g.,
   `ExpressionExperiment` extending `Investigation`,
   `Gene` extending `ChromosomeFeature`,
   `User` extending `Contact`) inherit the root's region or shadow it
   with their own. Easiest verification: at startup, log
   `SessionFactory.getStatistics().getSecondLevelCacheRegionNames()`.

---

## 8. Worked example — what to add to `EhcacheConfig` (illustrative)

(Don't edit `EhcacheConfig` on this branch; ehcache fix is unmerged.
This is what the eventual delta would look like.)

```java
// In APP_CACHES static initializer, after the existing 13 app caches:

// --- Hibernate L2: query cache + timestamps ---
APP_CACHES.put( "org.hibernate.cache.internal.StandardQueryCache",
                new CacheSpec( 5000, Duration.ofMinutes( 20 ) ) );
APP_CACHES.put( "org.hibernate.cache.spi.TimestampsRegion",
                new CacheSpec( 5000, /*eternal*/ null ) );

// --- L2 entity regions: immutable / read-only ---
APP_CACHES.put( "ubic.gemma.model.genome.Taxon",
                new CacheSpec( 100, null ) );
APP_CACHES.put( "ubic.gemma.model.common.measurement.Unit",
                new CacheSpec( 1000, null ) );
// ... (one entry per region from Section 5.2 + 5.3 + 5.4)
```

A "null TTL = eternal" overload in `CacheSpec` keeps the API uniform.

---

## 9. Out of scope here (tracked elsewhere)

- The unmerged ehcache-CacheManager fix
  (`worktree-ehcache-cachemanager-fix`) — wait for merge.
- `MeterRegistryJCacheConfigurer` for per-region metrics — already
  filed against `MetricsConfig`.
- `ehcache.xml` deletion — bundle with the L2 region declarations
  commit when it lands.
- `hibernate.jakarta.cache.*` property rename — separate Phase 3
  cleanup.

---

## 10. Sources

- `gemma-core/src/main/resources/ubic/gemma/applicationContext-hibernate.xml`
  lines 79-109 (Hibernate properties; L2 + query cache enablement).
- `gemma-core/src/main/resources/**/*.hbm.xml` (52 cached entities, 39
  cached collections — Section 2).
- `gemma-core/src/main/resources/ehcache.xml` (legacy Ehcache 2.x —
  size baseline only; not parsed at runtime).
- `gemma-core/src/main/java/ubic/gemma/persistence/cache/EhcacheConfig.java`
  (stub on this branch; real impl on `worktree-ehcache-cachemanager-fix`
  commits `6b17850441` + `08cc312b86`).
- 49 `setCacheable(true)` call sites; concentration in
  `ExpressionExperimentDaoImpl` and `AbstractQueryFilteringVoEnabledDao`
  (Section 4).
- `.claude/worktrees/agent-aspectj-ehcache-audit/ASPECTJ_EHCACHE_AUDIT.md`
  sections 2.3, 2.4, 3.x (upstream rationale for the JCache provider
  choice + `missing_cache_strategy=create` discussion).
