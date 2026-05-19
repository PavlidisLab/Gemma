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
 * <p>
 * <b>Hibernate L2 regions</b> are also pre-declared here. The same JCache provider
 * ({@code EhcacheCachingProvider}) backs both the Spring application caches and
 * Hibernate's {@code JCacheRegionFactory} (see {@code applicationContext-hibernate.xml}).
 * Although Hibernate constructs its own {@code javax.cache.CacheManager} instance, the
 * Ehcache provider returns the same underlying CacheManager for the same URI — so caches
 * pre-created here are visible to Hibernate's L2 lookups. Without pre-declaration, Hibernate's
 * {@code missing_cache_strategy=create} would lazily create each region with the JCache
 * provider's defaults, which for Ehcache 3 means <em>unbounded heap with no TTL</em> — the
 * same latent memory-leak vector as the App-cache stub had. The {@code missing_cache_strategy=create}
 * setting is retained as a safety net: any region not enumerated here still works, just
 * unbounded; the enumerated ones are bounded.
 * <p>
 * Sizes and TTLs below are conservative starting points sourced from the legacy
 * (Ehcache 2.x) {@code ehcache.xml} where overlap exists, the Hibernate L2 cache audit
 * (see {@code HIBERNATE_L2_CACHE_AUDIT.md} on {@code worktree-hibernate-l2-tune}), and
 * common-sense defaults otherwise. TODO: per-cache tuning informed by production traffic.
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

    /** Application cache definitions: name -> (heap entries, TTL). Order is preserved for diagnostics. */
    private static final Map<String, CacheSpec> APP_CACHES = new LinkedHashMap<>();

    /**
     * Hibernate L2 cache region definitions: name -> (heap entries, TTL).
     * Kept separate from {@link #APP_CACHES} so the App-cache vs L2-cache distinction
     * stays visible in source. Region names follow Hibernate's default naming: the
     * entity FQCN for entity regions, and {@code <owner-fqcn>.<role>} for collection
     * regions. Enumerated from {@code <cache>} elements in {@code *.hbm.xml} as of
     * the {@code worktree-hibernate-l2-tune} audit.
     */
    private static final Map<String, CacheSpec> L2_CACHES = new LinkedHashMap<>();

    /** Read-only entity defaults: small heap, eternal-ish TTL. */
    private static final CacheSpec L2_READ_ONLY = new CacheSpec( 1000, Duration.ofHours( 24 ) );
    /** Read-write entity defaults: larger heap (mutable, more churn), shorter TTL. */
    private static final CacheSpec L2_READ_WRITE = new CacheSpec( 5000, Duration.ofHours( 1 ) );
    /** Nonstrict-read-write entity defaults: small heap, short TTL (high churn, stale-tolerant). */
    private static final CacheSpec L2_NONSTRICT = new CacheSpec( 1000, Duration.ofMinutes( 5 ) );
    /** Collection region defaults: small heap, medium TTL. */
    private static final CacheSpec L2_COLLECTION = new CacheSpec( 1000, Duration.ofMinutes( 30 ) );
    /**
     * Default sizing for sharded query-cache regions (i.e. named alternatives to
     * {@code StandardQueryCache}). Matches the StandardQueryCache sizing so a sharded
     * region behaves the same as the shared one, just isolated from other-query eviction.
     */
    private static final CacheSpec L2_QUERY = new CacheSpec( 5000, Duration.ofMinutes( 30 ) );

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

    static {
        // --- Hibernate standard regions (query cache + invalidation timestamps) ---
        // StandardQueryCache: holds every setCacheable(true) query result that has NOT been
        // sharded into a named region via setCacheRegion(...). Hot DAOs (notably
        // ExpressionExperimentDaoImpl) shard their cacheable queries into the
        // ExpressionExperiment.* regions declared below to keep their eviction pressure off
        // this shared region. See HIBERNATE_L2_CACHE_AUDIT.md recommendation #4.
        L2_CACHES.put( "org.hibernate.cache.internal.StandardQueryCache", new CacheSpec( 5000, Duration.ofMinutes( 30 ) ) );
        // UpdateTimestampsCache / TimestampsRegion: tracks last-write timestamps per table
        // for query-cache invalidation. MUST be eternal (TTL=null) - eviction here would
        // let stale query results survive a write.
        L2_CACHES.put( "org.hibernate.cache.spi.UpdateTimestampsCache", new CacheSpec( 5000, null ) );
        // Hibernate 6 internal alias for the same region; declare both to be safe.
        L2_CACHES.put( "org.hibernate.cache.spi.TimestampsRegion", new CacheSpec( 5000, null ) );

        // --- Sharded query-cache regions (named alternatives to StandardQueryCache) ---
        // Names match the constants in ExpressionExperimentDaoImpl
        // (FILTERED_VO_CACHE_REGION, ANNOTATIONS_CACHE_REGION, USAGE_FREQ_CACHE_REGION,
        // QUERIES_CACHE_REGION). The EE-VO filter path is the hottest contributor in
        // gemma-core (24 setCacheable calls in ExpressionExperimentDaoImpl out of 49
        // gemma-core-wide per the L2 audit) and is given its own dedicated region so the
        // catalog/filter traffic does not evict unrelated cached queries (taxon lookups,
        // platform lookups, ACL helpers, etc).
        L2_CACHES.put( "ExpressionExperiment.filteredVo", L2_QUERY );
        L2_CACHES.put( "ExpressionExperiment.annotations", L2_QUERY );
        L2_CACHES.put( "ExpressionExperiment.usageFrequency", L2_QUERY );
        L2_CACHES.put( "ExpressionExperiment.queries", L2_QUERY );

        // --- Read-only entity regions (immutable configuration / reference data) ---
        L2_CACHES.put( "ubic.gemma.model.analysis.AnalysisResultSet", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.diff.HitListSize", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.diff.PvalueDistribution", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.pca.Eigenvalue", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.pca.Eigenvector", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.association.Gene2GOAssociation", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.blacklist.BlacklistedEntity", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.AuditEvent", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.common.description.DatabaseEntry", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.common.measurement.Unit", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.common.protocol.Protocol", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.expression.bioAssayData.BioAssayDimension", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.expression.bioAssayData.GenericCellLevelCharacteristics", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.expression.bioAssayData.SingleCellDimension", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.expression.biomaterial.Compound", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.genome.Chromosome", L2_READ_ONLY );
        L2_CACHES.put( "ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult", L2_READ_ONLY );

        // --- Read-write entity regions (mutable; need serializable isolation through L2) ---
        L2_CACHES.put( "ubic.gemma.model.analysis.Investigation", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.ExpressionExperimentSet", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.association.BioSequence2GeneProduct", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.Contact", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.GroupAuthority", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.JobInfo", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.UserGroup", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.common.description.BibRefAnnotation", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.common.description.Characteristic", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.common.measurement.Measurement", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.arrayDesign.AlternateName", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.arrayDesign.ArrayDesign", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.bioAssay.BioAssay", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.biomaterial.BioMaterial", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.biomaterial.Treatment", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExperimentalDesign", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExperimentalFactor", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.FactorValue", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.Geeq", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.genome.PhysicalLocation", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.genome.gene.GeneAlias", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.genome.gene.GeneSet", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.genome.gene.GeneSetMember", L2_READ_WRITE );
        L2_CACHES.put( "ubic.gemma.model.genome.gene.Multifunctionality", L2_READ_WRITE );

        // --- Nonstrict-read-write entity regions (high churn, stale-tolerant) ---
        L2_CACHES.put( "ubic.gemma.model.analysis.Analysis", L2_NONSTRICT );
        L2_CACHES.put( "ubic.gemma.model.common.description.BibliographicReference", L2_NONSTRICT );
        L2_CACHES.put( "ubic.gemma.model.common.description.ExternalDatabase", L2_NONSTRICT );
        L2_CACHES.put( "ubic.gemma.model.common.quantitationtype.QuantitationType", L2_NONSTRICT );
        L2_CACHES.put( "ubic.gemma.model.expression.designElement.CompositeSequence", L2_NONSTRICT );
        L2_CACHES.put( "ubic.gemma.model.genome.ChromosomeFeature", L2_NONSTRICT );
        L2_CACHES.put( "ubic.gemma.model.genome.Taxon", L2_NONSTRICT );
        L2_CACHES.put( "ubic.gemma.model.genome.biosequence.BioSequence", L2_NONSTRICT );

        // --- Collection regions (one per cached collection role) ---
        L2_CACHES.put( "ubic.gemma.model.analysis.Investigation.characteristics", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis.resultSets", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet.hitListSizes", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis.eigenValues", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis.eigenVectors", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.User.jobs", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.common.auditAndSecurity.UserGroup.authorities", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.common.description.BibliographicReference.annotations", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.common.description.BibliographicReference.chemicals", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.common.description.BibliographicReference.keywords", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.common.description.BibliographicReference.meshTerms", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.common.description.ExternalDatabase.externalDatabases", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.common.description.MedicalSubjectHeading.qualifiers", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.arrayDesign.ArrayDesign.alternateNames", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.arrayDesign.ArrayDesign.compositeSequences", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.arrayDesign.ArrayDesign.externalReferences", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.arrayDesign.ArrayDesign.mergees", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.arrayDesign.ArrayDesign.subsumedArrayDesigns", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.bioAssayData.BioAssayDimension.bioAssays", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.bioAssayData.SingleCellDimension.bioAssays", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.biomaterial.BioMaterial.bioAssaysUsedIn", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.biomaterial.BioMaterial.characteristics", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.biomaterial.BioMaterial.factorValues", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.biomaterial.BioMaterial.treatments", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExperimentalDesign.experimentalFactors", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExperimentalDesign.types", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExperimentalFactor.annotations", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExperimentalFactor.factorValues", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExpressionExperiment.bioAssays", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExpressionExperiment.otherParts", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.ExpressionExperiment.quantitationTypes", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.expression.experiment.FactorValue.characteristics", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.genome.Gene.accessions", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.genome.Gene.aliases", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.genome.Gene.products", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.genome.biosequence.BioSequence.bioSequence2GeneProduct", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.genome.gene.GeneProduct.accessions", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.genome.gene.GeneSet.characteristics", L2_COLLECTION );
        L2_CACHES.put( "ubic.gemma.model.genome.gene.GeneSet.members", L2_COLLECTION );
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
        // Pre-declare Hibernate L2 regions so they are bounded. Hibernate's
        // missing_cache_strategy=create remains as a safety net for any region not
        // enumerated here (those will be created unbounded by the JCache provider).
        for ( Map.Entry<String, CacheSpec> e : L2_CACHES.entrySet() ) {
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
        CacheConfigurationBuilder<Object, Object> b = CacheConfigurationBuilder
                .newCacheConfigurationBuilder( Object.class, Object.class,
                        ResourcePoolsBuilder.heap( spec.heapEntries ) );
        // null TTL = eternal (no expiry). Required for UpdateTimestampsCache so query-cache
        // invalidation tombstones do not expire prematurely.
        if ( spec.ttl != null ) {
            b = b.withExpiry( ExpiryPolicyBuilder.timeToLiveExpiration( spec.ttl ) );
        } else {
            b = b.withExpiry( ExpiryPolicyBuilder.noExpiration() );
        }
        return Eh107Configuration.fromEhcacheCacheConfiguration( b.build() );
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
