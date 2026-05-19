# AspectJ + ehcache + JCache audit (Phase 3 infra)

Branch baseline: `phase2-acl-migrate` @ `08e760bdaf`.
Audit performed: 2026-05-18.
Worktree: `.claude/worktrees/agent-aspectj-ehcache-audit`.

This report inventories the two infrastructure layers that quietly underpin
Spring 6 + Hibernate 6 on this codebase: AOP weaving (AspectJ) and L2 /
application caching (ehcache + JCache). Both have had Phase 2 / Phase 3
churn; both are currently load-bearing.

---

## 1. AspectJ inventory + usage shape

### 1.1 Maven coordinates

| artifact | version | scope |
|---|---|---|
| `org.aspectj:aspectjweaver` | **1.9.25.1** | compile (declared explicitly in `pom.xml:274-278`) |
| `org.springframework:spring-aspects` | `${spring.version}` (6.1.20) | runtime — `aspectjweaver` excluded so the explicit 1.9.25.1 wins |
| `org.aspectj:aspectjrt` | _not declared_ | (subsumed by `aspectjweaver`) |
| `org.aspectj:aspectjtools` | _not declared_ | |
| `aspectj-maven-plugin` / `dev.aspectj:aspectj-maven-plugin` | _not declared_ | — no compile-time weaving |

### 1.2 Style: Spring AOP (proxy-based), not full AspectJ

Confirmed by:

1. No `aspectj-maven-plugin` anywhere in the multi-module reactor.
2. Three Spring XML files declare `<aop:aspectj-autoproxy/>` — the Spring
   bean-factory directive that turns on proxy-based AOP using AspectJ
   *annotations* (not bytecode weaving):
   - `gemma-core/src/main/resources/ubic/gemma/applicationContext-security.xml:104`
   - `gemma-core/src/main/resources/ubic/gemma/applicationContext-serviceBeans.xml:75`
   - `gemma-core/src/main/resources/ubic/gemma/applicationContext-hibernate.xml:24`
3. Two test contexts use the Java-config equivalent
   `@EnableAspectJAutoProxy` (test-only):
   - `gemma-core/src/test/java/ubic/gemma/core/metrics/GenericMeterRegistryConfigurerTest.java:32`
   - `gemma-core/src/test/java/ubic/gemma/persistence/util/PointcutsTest.java:29`

In short: AspectJ jars are on the classpath only for the annotation API
(`@Aspect`, `@Before`, `@Pointcut`, etc.) and the reflective join-point
types Spring AOP exposes at runtime. No `.aj` files, no compile-time or
load-time weaving.

### 1.3 Actual aspects in main code

Only two aspects exist in main:

| file | role |
|---|---|
| `gemma-core/src/main/java/ubic/gemma/core/security/audit/AuditAdvice.java` | `@Aspect` with four `@Before` advices (creator / updater / saver / deleter) that populate audit events before CRUD operations. |
| `gemma-core/src/main/java/ubic/gemma/persistence/util/Pointcuts.java` | Library of named `@Pointcut`s (e.g. `daoMethod()`, `serviceMethod()`, `transactionalMethod()`) referenced by FQN from `AuditAdvice`. Not itself an `@Aspect`. |

Plus the Spring-supplied `io.micrometer.core.aop.TimedAspect` wired in the
metrics profile (`applicationContext-serviceBeans.xml:76`).

Historical note: `AclAdvice.java` and adjacent files still carry `@AfterReturning`
references in *comments*, but the actual AOP wiring there was replaced
in Phase 2/3 with a direct `org.hibernate.event` listener
(`AclEventListenerConfig.java`) — no advice fires.

### 1.4 AspectJ 1.9.25.1 — JDK status

- 1.9.20 added Java 20 support; 1.9.21 added Java 21 support; 1.9.22+ added
  Java 22+; **1.9.25 is current as of late 2025** and explicitly supports
  JDK 21, 22, 23 (and ships JDK 17 byte-code).
- 1.9.25.1 is the patch release fixing a regression in 1.9.25.
- **Verdict:** AspectJ is already JDK-21-ready. No bump needed for the
  Java-21 readiness milestone. The version is recent (released Q1 2026
  per the Maven Central record); no action.

---

## 2. ehcache + JCache wiring map

### 2.1 Maven coordinates

| artifact | version | notes |
|---|---|---|
| `org.ehcache:ehcache` `classifier=jakarta` | **3.10.8** | the only ehcache jar; `net.sf.ehcache:ehcache-core` (2.x) was removed in Phase 2 and is now banned via `extra-enforcer-rules` (`pom.xml:795`) |
| `org.hibernate.orm:hibernate-jcache` | `${hibernate.version}` (6.4.10.Final) | replaces the dropped `hibernate-ehcache` (2.x) |
| `javax.cache:cache-api` | 1.1.1 (in `<dependencyManagement>`) | JCache JSR-107 API, transitively pulled by `hibernate-jcache` |
| `net.sf.ehcache:ehcache-core` | **banned** | enforcer rule `pom.xml:795` |
| `org.hibernate:hibernate-ehcache` | **banned** | enforcer rule `pom.xml:800` |

### 2.2 Spring CacheManager bean

`gemma-core/src/main/java/ubic/gemma/persistence/cache/EhcacheConfig.java`:

```java
@Bean(name = "ehcache")
public CacheManager ehcache() {
    return new ConcurrentMapCacheManager();
}
```

This is **a stub**, despite the bean name. It is a Spring
`ConcurrentMapCacheManager` (in-memory `HashMap` backing, no TTL, no size
cap, no statistics) — **NOT** an `EhcacheCacheManager` wired to the
Ehcache 3 jakarta jar. The class-level javadoc acknowledges this:

> EhCache 2 was retired during the Spring 6 / Hibernate 6 migration; this
> stub exposes a Spring `ConcurrentMapCacheManager` keyed by the bean
> name `ehcache` so that legacy XML wiring that declares
> `depends-on="ehcache"` still resolves. A proper EhCache 3 (via JCache)
> or Caffeine integration will replace this in a follow-up phase.

The `ConcurrentMapCacheManager` auto-creates caches on demand
(`createConcurrentMapCache` lazily), so every call to
`cacheManager.getCache("...")` succeeds — but every cache loses its
configured size / TTL / eternal semantics.

**Consumers that use the Spring CacheManager** (i.e., still expect those
semantics from `ehcache.xml` but currently get unbounded HashMaps):

| consumer | cache name |
|---|---|
| `core/security/acl/GemmaAclConfiguration.java:182` | `aclCache` (gracefully falls back to a hand-rolled `ConcurrentMapCache` if not found — see "Gotcha" below) |
| `core/analysis/preprocess/OutlierDetectionServiceImpl.java:48` | `OutlierDetailsCache` |
| `core/analysis/report/ExpressionExperimentReportServiceImpl.java:99` | `EESTATS_CACHE_NAME` |
| `core/ontology/OntologyServiceImpl.java:140` | several |
| `web/controller/monitoring/CacheMonitorImpl.java` | iterates *all* — so the cache monitor UI lies about cache contents |

**Gotcha**: because `ConcurrentMapCacheManager` auto-creates caches, the
fallback path in `GemmaAclConfiguration#aclCache` is unreachable in
production today — the `cache != null` branch always wins, but with the
wrong (unbounded) semantics.

### 2.3 `ehcache.xml`

`gemma-core/src/main/resources/ehcache.xml` is still in tree and is still
in the Ehcache **2.x** XML format (root element `<ehcache>`, schema
`http://ehcache.org/ehcache.xsd`, attributes like `maxElementsInMemory`,
`overflowToDisk`, `eternal`). It declares ~100 named caches.

Status: **dead config file**. Nothing reads it.

- `ConcurrentMapCacheManager` doesn't parse XML.
- Hibernate's JCache region factory consults the JCache provider's
  default config (Ehcache 3 expects `ehcache3.xml` or a programmatic
  `CacheManagerBuilder`), not the v2 schema.
- The `hibernate.javax.cache.missing_cache_strategy=create` setting means
  Hibernate creates caches with default settings on demand, which is why
  L2 caching still works at all.

This file should either be migrated to Ehcache 3 XML
(`http://www.ehcache.org/v3` namespace, `<cache alias="...">` syntax) or
deleted. See "Recommendations" below.

### 2.4 Hibernate L2 cache provider configuration

From `applicationContext-hibernate.xml:90-94`:

```xml
<prop key="hibernate.cache.region.factory_class">jcache</prop>
<prop key="hibernate.javax.cache.provider">org.ehcache.jsr107.EhcacheCachingProvider</prop>
<prop key="hibernate.javax.cache.missing_cache_strategy">create</prop>
<prop key="hibernate.cache.use_query_cache">true</prop>
<prop key="hibernate.cache.use_second_level_cache">true</prop>
```

- `jcache` resolves to `org.hibernate.cache.jcache.internal.JCacheRegionFactory` (Hibernate 6 short alias).
- `org.ehcache.jsr107.EhcacheCachingProvider` ships with the Ehcache 3
  `jakarta` jar, so this is correct.
- `missing_cache_strategy=create` papers over the absence of a
  programmatic Ehcache 3 config (caches get default settings).

Hibernate 6.4 still accepts both `hibernate.javax.cache.*` and
`hibernate.jakarta.cache.*` setting keys (the latter is preferred in
6.x but the javax prefix is documented as still working).

---

## 3. MetricsConfig EhcacheConfigurer breakage status

### 3.1 What was broken

On `phase2-acl-migrate` HEAD (`08e760bdaf`):

- `applicationContext-serviceBeans.xml:70` declared:
  ```xml
  <bean class="ubic.gemma.core.metrics.MeterRegistryEhcacheConfigurer">
      <constructor-arg ref="meterRegistry"/>
      <constructor-arg ref="cacheManager"/>
  </bean>
  ```
- The backing class `gemma-core/src/main/java/ubic/gemma/core/metrics/MeterRegistryEhcacheConfigurer.java`
  was **deleted** in commit `ab94b884a4` ("WIP: Phase 2 (Spring 6 /
  Hibernate 6 / jakarta) — in-progress, does NOT compile") because it
  referenced `net.sf.ehcache.Ehcache` and the sibling helper
  `ubic.gemma.core.metrics.binder.cache.EhCache24Metrics`, both of which
  belong to the retired Ehcache 2.x stack.
- Net effect: **the `metrics` Spring profile fails to start** with
  `ClassNotFoundException: ubic.gemma.core.metrics.MeterRegistryEhcacheConfigurer`.
  Outside the `metrics` profile, no symptom.
- The unmerged `worktree-xml-config-kickoff` branch (`dcb758a615`)
  already documented this in a follow-up `MetricsConfig.java` javadoc.

### 3.2 Fix applied in this audit

Removed the dead bean from `applicationContext-serviceBeans.xml` and
replaced it with an explanatory comment pointing back at this audit doc.
The `metrics` profile now starts cleanly; it just lacks
ehcache-specific binders (JVM / Hibernate / Hikari / executor binders
still wire up).

### 3.3 Proposed full replacement (deferred — non-trivial)

Reintroducing cache metrics under Ehcache 3 / JCache is a non-trivial
ask because:

1. The Spring `CacheManager` bean is a `ConcurrentMapCacheManager` stub
   that has zero relationship to the JCache provider Hibernate uses.
   Wrapping its `Cache`s in a `JCacheMetrics` binder would surface nothing.
2. Hibernate constructs its own JCache `CacheManager` internally via
   `JCacheRegionFactory#getCacheManager()` — that field is package-private
   in `org.hibernate.cache.jcache.internal.JCacheRegionFactory` and not
   directly exposed via the public Hibernate API.
3. The "right" architectural fix is to *first* replace the
   `ConcurrentMapCacheManager` stub with a `JCacheCacheManager` (Spring's
   adapter over the JSR-107 manager) that shares one
   `javax.cache.CacheManager` with Hibernate; *then* wire metrics off
   that shared manager.

Sketch of the proper replacement, once a shared JCache `CacheManager`
exists (call it `jCacheCacheManager`):

```java
package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.util.Collections;

/**
 * Bind every cache in the supplied {@link CacheManager} (JSR-107) to the
 * Micrometer registry. Replaces the Phase-1 MeterRegistryEhcacheConfigurer
 * which assumed Ehcache 2.x APIs.
 */
public class MeterRegistryJCacheConfigurer extends AbstractMeterRegistryConfigurer {

    private final CacheManager cacheManager;

    public MeterRegistryJCacheConfigurer( MeterRegistry registry, CacheManager cacheManager ) {
        super( registry );
        this.cacheManager = cacheManager;
    }

    @Override
    protected void configure( MeterRegistry registry ) {
        for ( String name : cacheManager.getCacheNames() ) {
            Cache<?, ?> cache = cacheManager.getCache( name );
            if ( cache != null ) {
                JCacheMetrics.monitor( registry, cache, Collections.<Tag>emptyList() );
            }
        }
    }
}
```

Wire-up plan (deferred, requires a coordinated cache-manager redesign):

1. Replace `EhcacheConfig.ehcache()` with a `JCacheCacheManager` bean
   backed by a `javax.cache.CacheManager` produced from `Caching.getCachingProvider().getCacheManager(uri, classloader)`.
   Same URI / classloader Hibernate uses (or pass the URI via Hibernate
   property so both share).
2. Either (a) declare each named cache programmatically (an Ehcache 3
   `CacheConfigurationBuilder` per region; reads cleaner than XML), or
   (b) author an `ehcache3.xml` and point the JCache provider at it via
   the URI.
3. Restore `MeterRegistryJCacheConfigurer` (above) as a bean in
   `MetricsConfig` / the metrics-profile XML.
4. Delete the dead `ehcache.xml` (2.x format) at the end.

Total estimate: 1-2 days of focused work plus an integration-test pass
to make sure cache eviction semantics match prior behaviour for
`OutlierDetailsCache`, `EE Stats Cache`, `aclCache`, the ontology caches,
and the ~25 manually-managed app caches.

---

## 4. Combined recommendations

| # | item | priority | effort |
|---|---|---|---|
| 1 | **Done in this audit:** remove dead `MeterRegistryEhcacheConfigurer` bean from XML so the `metrics` profile starts. | applied | — |
| 2 | Replace the `ConcurrentMapCacheManager` stub in `EhcacheConfig.java` with a `JCacheCacheManager` sharing the same `javax.cache.CacheManager` Hibernate uses. Restores TTL / size / eternal semantics for all ~25 Gemma caches currently silently unbounded. | high | 1-2d |
| 3 | After #2, restore cache metrics via `JCacheMetrics.monitor(...)` (see code sketch in §3.3). | medium | 0.5d (trivial once #2 done) |
| 4 | Migrate `ehcache.xml` to Ehcache 3 schema (or replace with programmatic config) and delete the v2-format file. | medium | 0.5d |
| 5 | Remove the no-op AOP wiring chatter: three XML files declare `<aop:aspectj-autoproxy/>`. After XML→Java migration, consolidate into one `@EnableAspectJAutoProxy` on a central `@Configuration`. | low | 0.25d |
| 6 | Keep AspectJ 1.9.25.1 — already current and JDK-21 ready. No action. | none | — |
| 7 | Consider load-time-weaving for the audit aspect only if profiler data shows proxy overhead matters (DAO methods are hot paths). Currently nothing suggests it does. | speculative | — |

---

## 5. Quick fact sheet (for handoff)

- **AspectJ:** 1.9.25.1, Spring AOP style (proxy-based, no
  `aspectj-maven-plugin`, no `.aj`), already JDK-21 ready.
- **ehcache:** 3.10.8 jakarta classifier; old `net.sf.ehcache:ehcache-core` 2.x banned via enforcer.
- **JCache provider:** `org.ehcache.jsr107.EhcacheCachingProvider`.
- **Hibernate L2:** `hibernate.cache.region.factory_class=jcache`,
  using the Ehcache 3 caching provider; `missing_cache_strategy=create`.
- **Spring CacheManager (`@Bean name="ehcache"`):** a
  `ConcurrentMapCacheManager` stub — **not** ehcache 3. Drifts from
  `ehcache.xml`'s declared sizes / TTLs (which file is no longer read).
- **`MeterRegistryEhcacheConfigurer`:** dead reference removed from
  XML in this commit; replacement deferred until the Spring CacheManager
  is upgraded to a real JCache adapter.
- **Affected commit (deletion):** `ab94b884a4`.
- **Audit applied on:** `phase2-acl-migrate` @ `08e760bdaf`.
