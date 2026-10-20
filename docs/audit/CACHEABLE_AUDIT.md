# `@Cacheable` Annotation Audit — Phase 3

**Branch:** `worktree-cacheable-audit` (forked from `phase2-acl-migrate` HEAD `08e760bdaf`).
**Date:** 2026-05-18.
**Scope:** Verify that the 13 named application caches declared in
`EhcacheConfig` (unmerged branch `worktree-ehcache-cachemanager-fix`,
commits `6b17850441` + `08cc312b86`) cover every Spring-application-cache
consumer in the codebase, and that none of those consumers reference a
cache name that the config does not declare.

This audit is **recce only** — no code changes.

---

## 1. Inventory totals

The codebase uses **zero Spring `@Cacheable`-family annotations**.
All Spring application caching is wired **programmatically**, via
`CacheManager.getCache(<name>)` plus direct `Cache.put` / `Cache.evict`
/ `Cache.clear` calls.

| Annotation       | Occurrences |
|------------------|------------:|
| `@Cacheable`     |           0 |
| `@CacheEvict`    |           0 |
| `@CachePut`      |           0 |
| `@Caching`       |           0 |
| `@CacheConfig`   |           0 |
| JPA `@Cacheable` |           0 |

Hibernate-query `Query.setCacheable(true)` calls exist in DAOs
(`ExpressionExperimentDaoImpl`, `ArrayDesignDaoImpl`,
`DifferentialExpressionResultDaoImpl`, etc.) — those drive Hibernate's
**L2 query cache**, which is configured separately in `ehcache.xml` /
`applicationContext-hibernate.xml`. They are **out of scope** for this
audit (Spring's `CacheManager` and Hibernate's `RegionFactory` are
distinct, per the EhcacheConfig docstring).

---

## 2. Cache-name registry

### 2a. Declared in `EhcacheConfig` (the unmerged-branch fix)

| # | Cache name                                  | Heap entries | TTL       |
|---|---------------------------------------------|-------------:|-----------|
| 1 | `aclCache`                                  |        5,000 | 1 h       |
| 2 | `OutlierDetailsCache`                       |        1,000 | 2 h       |
| 3 | `ExpressionExperimentReportsCache`          |        5,000 | 6 h       |
| 4 | `OntologyService.search`                    |        5,000 | 1 h       |
| 5 | `OntologyService.parents`                   |       10,000 | 6 h       |
| 6 | `OntologyService.children`                  |       10,000 | 6 h       |
| 7 | `GeneOntologyService.goTerms`               |       50,000 | 12 h      |
| 8 | `GeneOntologyService.term2Aspect`           |       50,000 | 12 h      |
| 9 | `Gene2GoServiceCache`                       |       20,000 | 6 h       |
|10 | `DiffExResultCache`                         |        5,000 | 2 h       |
|11 | `TopDiffExResultCache`                      |        5,000 | 2 h       |
|12 | `ProcessedExpressionDataVectorCache`        |        1,000 | 6 h       |
|13 | `ProcessedExpressionDataVectorByGeneCache`  |        1,000 | 6 h       |

### 2b. Referenced by application code (via `CacheManager.getCache(...)`)

Each reference resolves to a declared name; the table cross-references
the call site to row #2a.

| File:line                                                                                                          | Cache name                                  | Source form                                       | Declared? |
|--------------------------------------------------------------------------------------------------------------------|---------------------------------------------|---------------------------------------------------|:---------:|
| `gemma-core/.../core/security/acl/GemmaAclConfiguration.java:185`                                                  | `aclCache`                                  | literal                                           |    Yes    |
| `gemma-core/.../core/analysis/preprocess/OutlierDetectionServiceImpl.java:48`                                      | `OutlierDetailsCache`                       | literal                                           |    Yes    |
| `gemma-core/.../core/analysis/report/ExpressionExperimentReportServiceImpl.java:99` (`EESTATS_CACHE_NAME` :70)     | `ExpressionExperimentReportsCache`          | `private static final String EESTATS_CACHE_NAME`  |    Yes    |
| `gemma-core/.../core/ontology/OntologyServiceImpl.java:152` (constants :90–92)                                     | `OntologyService.search` / `.parents` / `.children` | `SEARCH_CACHE_NAME`, `PARENTS_CACHE_NAME`, `CHILDREN_CACHE_NAME` |    Yes    |
| `gemma-core/.../core/ontology/providers/GeneOntologyServiceImpl.java:105–106`                                      | `GeneOntologyService.goTerms` / `.term2Aspect` | literals                                       |    Yes    |
| `gemma-core/.../persistence/service/association/Gene2GOAssociationServiceImpl.java:63` (`G2G_CACHE_NAME` :49)      | `Gene2GoServiceCache`                       | `private static final String G2G_CACHE_NAME`      |    Yes    |
| `gemma-core/.../persistence/service/analysis/expression/diff/DifferentialExpressionResultCacheImpl.java:141–142`   | `DiffExResultCache` / `TopDiffExResultCache` | `DIFF_EX_RESULT_CACHE_NAME`, `TOP_HITS_CACHE_NAME` |    Yes    |
| `gemma-core/.../persistence/service/expression/bioAssayData/ProcessedDataVectorCacheImpl.java:30`                  | `ProcessedExpressionDataVectorCache`        | `VECTOR_CACHE_NAME` (this class)                  |    Yes    |
| `gemma-core/.../persistence/service/expression/bioAssayData/ProcessedDataVectorByGeneCacheImpl.java:57`            | `ProcessedExpressionDataVectorByGeneCache`  | `VECTOR_CACHE_NAME` (this class)                  |    Yes    |

Generic iterators (do not pin a cache name):

* `CacheMonitorImpl.java` walks `cacheManager.getCacheNames()` — diagnostic only.
* `CacheUtils.getCache(...)` throws on missing — used by all the consumers above.

### 2c. Test-only references

* `gemma-core/src/test/.../GeneOntologyService2Test.java:87` and
  `GeneOntologyServiceTest.java:93` construct their own
  `ConcurrentMapCacheManager("GeneOntologyService.goTerms", "GeneOntologyService.term2Aspect")`.
  Production wiring is unaffected; these tests bypass `EhcacheConfig`.

---

## 3. Mismatches

### 3a. Referenced but NOT declared

**None.** Every cache name reached via `CacheManager.getCache(...)` in
production code is declared in `EhcacheConfig.APP_CACHES`.

### 3b. Declared but NOT referenced

**None.** All 13 declared cache names have at least one production
consumer (table 2b).

### 3c. Latent silent-fall-through guard in `GemmaAclConfiguration`

`GemmaAclConfiguration.java:185–192` defends against the cache being
absent by constructing a local `ConcurrentMapCache("aclCache")`:

```java
Cache cache = cacheManager.getCache( "aclCache" );
if ( cache == null ) {
    // Defensive fallback for test contexts where the CacheManager
    // hasn't pre-registered an "aclCache" region.
    cache = new org.springframework.cache.concurrent.ConcurrentMapCache( "aclCache" );
}
```

With the fixed `EhcacheConfig` in place this branch becomes
**unreachable in production** (the cache is always declared). It is
**still useful for tests** that wire a vanilla `ConcurrentMapCacheManager`
without the production config — leave it as-is.

---

## 4. Eviction-pattern hot spots

Three pairs of caches are evicted together; the pairing is consistent
and there is no obvious drop-too-much / drop-too-little issue.

| Caches                                                                       | Eviction trigger                                                                 | Pattern |
|------------------------------------------------------------------------------|----------------------------------------------------------------------------------|---------|
| `DiffExResultCache` + `TopDiffExResultCache`                                  | `DifferentialExpressionResultCacheImpl.clearCache()` clears both                  | Paired clear: OK |
| `ProcessedExpressionDataVectorCache` + `ProcessedExpressionDataVectorByGeneCache` | `CachedProcessedExpressionDataVectorServiceImpl.evict(ee)` evicts both per-EE | Paired evict-by-EE: OK |
| `GeneOntologyService.goTerms` + `GeneOntologyService.term2Aspect`            | `GeneOntologyServiceImpl.clearCaches()` clears both                              | Paired clear: OK |
| `OntologyService.search` / `.parents` / `.children`                          | `OntologyCache` clears each independently (per-ontology refresh)                  | OK |
| `ExpressionExperimentReportsCache`                                            | Per-EE `evict(id)` and `put(id,vo)` on report-VO refresh                          | OK |
| `Gene2GoServiceCache`                                                         | Per-gene `put(gene, ...)`; no explicit eviction (relies on TTL = 6 h)             | OK; TTL bounded |
| `OutlierDetailsCache`                                                         | `getCache().clear()` on demand; no per-EE evict                                   | Coarse but acceptable |
| `aclCache`                                                                    | Spring Security ACL service drives evictions via `AclService` mutations           | Out of scope (ACL framework owns this) |

No annotation-driven eviction patterns exist, so the typical
"`@CacheEvict` everywhere on the wrong key" failure mode does not
apply here.

---

## 5. Recommendations for `EhcacheConfig`

### 5a. Cache definitions to add

**None.** The declared set exactly matches the production reference set
on the current `phase2-acl-migrate` HEAD.

### 5b. Cache definitions to remove

**None.** Every declared cache has at least one production consumer.

### 5c. Follow-ups (not required for the EhcacheConfig commit)

1. **Re-attach metrics.** `applicationContext-serviceBeans.xml:70`
   still wires `ubic.gemma.core.metrics.MeterRegistryEhcacheConfigurer`,
   whose Phase-2 deletion is acknowledged in the `EhcacheConfig`
   docstring. Either delete the XML bean or replace it with the
   planned `MeterRegistryJCacheConfigurer` so the JCache CacheManager
   produced by `EhcacheConfig#jCacheCacheManager()` gets bound to
   Micrometer. (Outside this audit; tracked under `MetricsConfig`.)
2. **Per-cache TTL tuning.** EhcacheConfig flags the current TTLs as
   conservative starting points. Once metrics are restored, revisit
   `Gene2GoServiceCache` (no explicit eviction, relies entirely on
   6 h TTL) and `OutlierDetailsCache` (2 h TTL, only coarse clear).
3. **Consider migrating to Spring `@Cacheable` annotations.** The
   `*CacheImpl` boilerplate around `CacheManager.getCache` + manual
   `put` / `evict` could be replaced with `@Cacheable` / `@CacheEvict`
   for a future readability pass, but that is **not** required and is
   firmly out of Phase 3 scope.

### 5d. Verdict

`EhcacheConfig` as committed on `worktree-ehcache-cachemanager-fix` is
**complete and correct** with respect to the application-cache surface
on `phase2-acl-migrate` HEAD. Merge it as-is.

---

## Appendix — search commands used

```bash
# Inventory annotations (zero hits)
grep -rn --include='*.java' "@Cacheable\|@CacheEvict\|@CachePut\|@Caching\|@CacheConfig" .

# Direct cache lookups
grep -rn --include='*.java' "cacheManager\.getCache\|CacheUtils\.getCache" .

# Resolve cache-name string constants
grep -rn --include='*.java' -E "EESTATS_CACHE_NAME|G2G_CACHE_NAME|DIFF_EX_RESULT_CACHE_NAME|TOP_HITS_CACHE_NAME|VECTOR_CACHE_NAME|SEARCH_CACHE_NAME|PARENTS_CACHE_NAME|CHILDREN_CACHE_NAME" .

# Pull EhcacheConfig from unmerged branch
git show worktree-ehcache-cachemanager-fix:gemma-core/src/main/java/ubic/gemma/persistence/cache/EhcacheConfig.java
```
