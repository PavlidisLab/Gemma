package ubic.gemma.persistence.cache;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring application-cache configuration backed by Ehcache 3 (jakarta classifier) via JCache (JSR-107).
 * <p>
 * History: Phase 1 retired the old Ehcache 2.x stack. Until this commit the bean named
 * {@code ehcache} was a {@link org.springframework.cache.concurrent.ConcurrentMapCacheManager}
 * stub — a {@code HashMap}-backed CacheManager that auto-creates caches on demand with
 * <em>no</em> heap cap, TTL, or eviction policy. Every consumer
 * ({@link ubic.gemma.core.security.acl.GemmaAclConfiguration#aclCache aclCache},
 * {@code OutlierDetailsCache}, {@code ExpressionExperimentReportsCache}, the
 * {@code OntologyService.*} caches, {@code GeneOntologyService.*}, {@code Gene2GoServiceCache},
 * {@code DiffExResultCache}, {@code TopDiffExResultCache}, and the two
 * {@code ProcessedExpressionDataVector*} caches) silently received unbounded HashMap-backed
 * stores — a latent memory-leak vector in production.
 * <p>
 * This config replaces the stub with a real {@link JCacheCacheManager}. Each named
 * application cache is declared programmatically with a bounded heap and a TTL via
 * {@link CacheConfigurationBuilder} (wrapped in {@link Eh107Configuration} for JSR-107).
 * Hibernate's L2 cache is unaffected — Hibernate has its own
 * {@code org.hibernate.cache.spi.RegionFactory} (configured in
 * {@code applicationContext-hibernate.xml} as {@code jcache} with the same
 * {@code EhcacheCachingProvider}) and uses {@code missing_cache_strategy=create} for
 * its L2 regions.
 * <p>
 * Sizes and TTLs below are conservative starting points sourced from the legacy
 * (Ehcache 2.x) {@code ehcache.xml} where overlap exists, and from common-sense
 * defaults otherwise. TODO: per-cache tuning informed by production traffic.
 * See {@code ASPECTJ_EHCACHE_AUDIT.md} (audit branch
 * {@code worktree-aspectj-ehcache-audit}) for the upstream rationale.
 * <p>
 * Follow-up unblocked by this change: a {@code MeterRegistryJCacheConfigurer}
 * binding {@code io.micrometer.core.instrument.binder.cache.JCacheMetrics} per
 * named cache, restoring the cache-metrics flow that was dropped in Phase 2 when
 * {@code MeterRegistryEhcacheConfigurer} (Ehcache 2.x API) was deleted. The
 * JCache {@link javax.cache.CacheManager} produced here is the right target for
 * that binder; that work is tracked separately in {@code MetricsConfig}.
 */
@Configuration
public class EhcacheConfig {

    /** Cache definitions: name -> (heap entries, TTL). Order is preserved for diagnostics. */
    private static final Map<String, CacheSpec> APP_CACHES = new LinkedHashMap<>();

    static {
        // Spring Security ACL cache — read-heavy, evictions driven by AclService mutations.
        APP_CACHES.put( "aclCache", new CacheSpec( 5000, Duration.ofHours( 1 ) ) );

        // OutlierDetectionServiceImpl: caches outlier-detail blobs keyed by EE id.
        APP_CACHES.put( "OutlierDetailsCache", new CacheSpec( 1000, Duration.ofHours( 2 ) ) );

        // ExpressionExperimentReportServiceImpl: per-EE stats summaries.
        APP_CACHES.put( "ExpressionExperimentReportsCache", new CacheSpec( 5000, Duration.ofHours( 6 ) ) );

        // OntologyServiceImpl: search / hierarchy lookups against external ontology sources.
        APP_CACHES.put( "OntologyService.search", new CacheSpec( 5000, Duration.ofHours( 1 ) ) );
        APP_CACHES.put( "OntologyService.parents", new CacheSpec( 10000, Duration.ofHours( 6 ) ) );
        APP_CACHES.put( "OntologyService.children", new CacheSpec( 10000, Duration.ofHours( 6 ) ) );

        // GeneOntologyServiceImpl: GO term metadata.
        APP_CACHES.put( "GeneOntologyService.goTerms", new CacheSpec( 50000, Duration.ofHours( 12 ) ) );
        APP_CACHES.put( "GeneOntologyService.term2Aspect", new CacheSpec( 50000, Duration.ofHours( 12 ) ) );

        // Gene2GOAssociationServiceImpl: gene -> GO term annotations.
        APP_CACHES.put( "Gene2GoServiceCache", new CacheSpec( 20000, Duration.ofHours( 6 ) ) );

        // DifferentialExpressionResultCacheImpl
        APP_CACHES.put( "DiffExResultCache", new CacheSpec( 5000, Duration.ofHours( 2 ) ) );
        APP_CACHES.put( "TopDiffExResultCache", new CacheSpec( 5000, Duration.ofHours( 2 ) ) );

        // ProcessedDataVectorCache / ByGene — historically large; bound the heap and let
        // the LRU prune the long tail. TTL kept generous since recompute is expensive.
        APP_CACHES.put( "ProcessedExpressionDataVectorCache", new CacheSpec( 1000, Duration.ofHours( 6 ) ) );
        APP_CACHES.put( "ProcessedExpressionDataVectorByGeneCache", new CacheSpec( 1000, Duration.ofHours( 6 ) ) );
    }

    /**
     * The single JCache {@link javax.cache.CacheManager} shared by Spring's application-cache
     * abstraction. Hibernate constructs its own JCache CacheManager internally via the same
     * provider (see {@code applicationContext-hibernate.xml}), so the two are not the same
     * instance but share the underlying Ehcache provider.
     */
    @Bean(destroyMethod = "close")
    public javax.cache.CacheManager jCacheCacheManager() {
        javax.cache.CacheManager mgr = Caching
                .getCachingProvider( "org.ehcache.jsr107.EhcacheCachingProvider" )
                .getCacheManager();
        for ( Map.Entry<String, CacheSpec> e : APP_CACHES.entrySet() ) {
            String name = e.getKey();
            if ( mgr.getCache( name ) == null ) {
                mgr.createCache( name, buildConfig( e.getValue() ) );
            }
        }
        return mgr;
    }

    /**
     * Spring's facade over the JCache CacheManager. Preserves the bean name
     * {@code ehcache} so legacy XML wiring ({@code depends-on="ehcache"}) and existing
     * field injections resolve unchanged.
     */
    @Bean(name = "ehcache")
    public CacheManager ehcache( javax.cache.CacheManager jcache ) {
        return new JCacheCacheManager( jcache );
    }

    private static javax.cache.configuration.Configuration<Object, Object> buildConfig( CacheSpec spec ) {
        return Eh107Configuration.fromEhcacheCacheConfiguration(
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder( Object.class, Object.class,
                                ResourcePoolsBuilder.heap( spec.heapEntries ) )
                        .withExpiry( ExpiryPolicyBuilder.timeToLiveExpiration( spec.ttl ) )
                        .build()
        );
    }

    private static final class CacheSpec {
        final long heapEntries;
        final Duration ttl;

        CacheSpec( long heapEntries, Duration ttl ) {
            this.heapEntries = heapEntries;
            this.ttl = ttl;
        }
    }
}
