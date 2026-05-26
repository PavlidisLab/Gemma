/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.analysis.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Computes + caches the public home-page statistics snapshot. The computation runs
 * from the anonymous-user perspective (same security pattern as {@link WhatsNewServiceImpl})
 * so private datasets do not leak into the public counts.
 * <p>
 * Persistence: JSON file at {@code ${gemma.appdata.home}/HomeStats/home_stats.json}.
 * JSON, not Java serialization — survives class-shape changes across Gemma versions
 * and is human-readable for ops debugging.
 */
@Component("homeStatsService")
public class HomeStatsServiceImpl implements HomeStatsService, InitializingBean {

    private static final Log log = LogFactory.getLog( HomeStatsServiceImpl.class );

    private static final String HOME_STATS_DIR = "HomeStats";
    private static final String HOME_STATS_FILE = "home_stats.json";

    /** How many recent experiments to keep in the snapshot for the scrolling widget. */
    private static final int RECENT_EXPERIMENTS_LIMIT = 50;

    /**
     * Per-category breakdowns we expose on the home page. Keys are stable lowercase-snake-case
     * tile labels; values are the canonical Gemma category labels that {@code Characteristic.category}
     * carries. (Could also be URIs, but the labels are what the data is loaded with — switching to
     * URIs would force a per-deployment lookup of the categoryURI-for-this-label.) Insertion order
     * controls the order of the {@code byAnnotationCategory} map in the JSON payload.
     */
    private static final Map<String, String> ANNOTATION_CATEGORIES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put( "disease", "disease" );
        m.put( "organism_part", "organism part" );
        m.put( "cell_type", "cell type" );
        m.put( "treatment", "treatment" );
        m.put( "strain", "strain" );
        m.put( "cell_line", "cell line" );
        ANNOTATION_CATEGORIES = Collections.unmodifiableMap( m );
    }

    /** How many top categories to retain in the {@code categoryDistribution} surface field. */
    private static final int CATEGORY_DISTRIBUTION_LIMIT = 25;

    /** Reverse lookup — Gemma canonical category label → home-page tile key. Used so the
     *  category-distribution entries know whether they correspond to one of the tile buckets. */
    private static final Map<String, String> CATEGORY_LABEL_TO_KEY;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        for ( Map.Entry<String, String> e : ANNOTATION_CATEGORIES.entrySet() ) {
            m.put( e.getValue(), e.getKey() );
        }
        CATEGORY_LABEL_TO_KEY = Collections.unmodifiableMap( m );
    }

    /** Per-category annotation-count timeout. Tight enough that a runaway query doesn't stall
     *  the daily refresh; generous enough that the four category queries plus the total all
     *  finish in the typical refresh window. */
    private static final long ANNOTATION_COUNT_TIMEOUT_MS = 60_000L;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private ManualAuthenticationService manualAuthenticationService;

    @Value("${gemma.appdata.home}")
    private String homeDir;

    private final ObjectMapper json = new ObjectMapper()
            .disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );

    private final AtomicReference<HomeStats> cache = new AtomicReference<>();

    /**
     * Load any persisted snapshot on bean startup so that {@link #getCached()} doesn't
     * have to hit disk on the first REST request. A missing / unreadable file is treated
     * as "no snapshot yet" — the daily refresher will populate it.
     * <p>
     * {@code jakarta.annotation.PostConstruct} isn't on the gemma-core classpath
     * (dropped in the Phase 3 Jakarta cleanup) so Spring's {@link InitializingBean}
     * provides the lifecycle slot — same pattern as {@code AclClassIdInitializer}.
     */
    @Override
    public void afterPropertiesSet() {
        Path file = snapshotFile();
        if ( !Files.exists( file ) ) {
            log.info( "HomeStats: no cached snapshot at " + file + ", waiting for first refresh" );
            return;
        }
        try {
            HomeStats stats = json.readValue( file.toFile(), HomeStats.class );
            cache.set( stats );
            log.info( "HomeStats: loaded cached snapshot from " + file + " (generated " + stats.getGeneratedAt() + ")" );
        } catch ( IOException e ) {
            log.warn( "HomeStats: could not deserialize " + file + "; will recompute on next refresh", e );
        }
    }

    @Override
    public HomeStats getCached() {
        return cache.get();
    }

    @Override
    @Transactional(readOnly = true)
    public HomeStats refresh() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication( manualAuthenticationService.authenticateAnonymously() );
        try {
            HomeStats stats = new DelegatingSecurityContextCallable<>( this::compute, context ).call();
            cache.set( stats );
            persist( stats );
            return stats;
        } catch ( Exception e ) {
            throw new RuntimeException( "HomeStats refresh failed", e );
        }
    }

    private HomeStats compute() {
        long t0 = System.currentTimeMillis();
        HomeStats stats = new HomeStats();
        stats.setGeneratedAt( new Date() );

        Filters empty = Filters.empty();

        stats.setDatasetCount( expressionExperimentService.countWithCache( empty, null ) );
        stats.setPlatformCount( arrayDesignService.count( empty ) );
        stats.setSampleCount( expressionExperimentService.countBioMaterials( empty ) );
        stats.setGeneCount( geneService.countAll() );

        // Taxon breakdown — already ACL-aware via the underlying ee2c/ee2ad query plumbing.
        Map<Taxon, Long> taxa = expressionExperimentService.getTaxaUsageFrequency( empty, null );
        stats.setByTaxon( taxa.entrySet().stream()
                .sorted( Map.Entry.<Taxon, Long>comparingByValue().reversed() )
                .map( e -> new HomeStats.TaxonStat(
                        e.getKey().getId(),
                        e.getKey().getCommonName(),
                        e.getKey().getScientificName(),
                        e.getValue() ) )
                .collect( Collectors.toList() ) );

        // Platform technology-type breakdown — frontend rolls ONECOLOR+TWOCOLOR+DUALMODE → microarray,
        // SEQUENCING+GENELIST → RNA-seq. We ship the raw enum-keyed map so the frontend can also
        // surface individual tech types in tooltips.
        Map<TechnologyType, Long> tts = expressionExperimentService.getTechnologyTypeUsageFrequency( empty, null );
        Map<String, Long> ttMap = new LinkedHashMap<>();
        for ( TechnologyType tt : TechnologyType.values() ) {
            ttMap.put( tt.name(), tts.getOrDefault( tt, 0L ) );
        }
        stats.setByPlatformType( ttMap );

        // Recent experiments — sort by curationDetails.lastUpdated desc.
        Slice<ExpressionExperimentValueObject> recent = expressionExperimentService.loadValueObjectsWithCache(
                empty,
                Sort.by( null, "curationDetails.lastUpdated", Sort.Direction.DESC, Sort.NullMode.LAST, "lastUpdated" ),
                0, RECENT_EXPERIMENTS_LIMIT );
        stats.setRecentExperiments( recent.stream()
                .map( vo -> new HomeStats.RecentExperiment(
                        vo.getId(),
                        vo.getShortName(),
                        vo.getName(),
                        vo.getTaxonObject() != null ? vo.getTaxonObject().getCommonName() : null,
                        vo.getLastUpdated() ) )
                .collect( Collectors.toList() ) );

        // Distinct ontology-term count + per-category breakdown — same semantics as
        // /datasets/annotations/count?excludeFreeText=true. We pre-seed excludedTermUris with
        // the FREE_TEXT sentinel so getAnnotationsUsageFrequencyInternal drops rows with null
        // valueUri (free-text characteristics like `lung tissue` / `Lung` would otherwise
        // each count as a distinct "term" — the 482K-result bug bro reported).
        List<String> freeTextSentinel = Collections.singletonList( ExpressionExperimentService.FREE_TEXT );
        stats.setOntologyTermCount( countAnnotationTerms( empty, null, freeTextSentinel ) );

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for ( Map.Entry<String, String> e : ANNOTATION_CATEGORIES.entrySet() ) {
            byCategory.put( e.getKey(), countAnnotationTerms( empty, e.getValue(), freeTextSentinel ) );
        }
        stats.setByAnnotationCategory( byCategory );

        // Category distribution — top-N annotation categories actually used across public
        // datasets, with their experiment counts. Reflects the range of experimental
        // conditions / annotation dimensions represented in the corpus (factor-value
        // categories ride here too — getCategoriesUsageFrequency aggregates across
        // experiment tags, samples and factor values per its operation description).
        Map<Characteristic, Long> categoryUsage = expressionExperimentService.getCategoriesUsageFrequency(
                empty, null, null, null, null, CATEGORY_DISTRIBUTION_LIMIT );
        List<HomeStats.CategoryStat> categoryDistribution = categoryUsage.entrySet().stream()
                .sorted( Map.Entry.<Characteristic, Long>comparingByValue().reversed() )
                .map( e -> {
                    String label = e.getKey().getCategory();
                    return new HomeStats.CategoryStat(
                            label != null ? CATEGORY_LABEL_TO_KEY.get( label ) : null,
                            label,
                            e.getKey().getCategoryUri(),
                            e.getValue() );
                } )
                .collect( Collectors.toList() );
        stats.setCategoryDistribution( categoryDistribution );

        // Still on the v2 wishlist (need new HQL aggregates): single-cell EE count and
        // distinct-DEA-condition count. See HOME_STATS_WISHLIST.md.

        long elapsed = System.currentTimeMillis() - t0;
        log.info( "HomeStats: snapshot recomputed in " + elapsed + " ms — "
                + stats.getDatasetCount() + " datasets, "
                + stats.getPlatformCount() + " platforms, "
                + stats.getSampleCount() + " samples, "
                + stats.getByTaxon().size() + " taxa, "
                + stats.getOntologyTermCount() + " ontology terms, "
                + stats.getRecentExperiments().size() + " recent" );
        return stats;
    }

    /**
     * Count distinct ontology-backed annotation terms via the existing usage-frequency
     * service method with {@code maxResults=0} (the DAO "no-limit" sentinel). The result
     * list size is the distinct-term count — we throw the per-term details away. Wraps
     * the {@link TimeoutException} as a logged zero so a single slow category doesn't
     * abort the whole daily snapshot.
     */
    private long countAnnotationTerms( Filters filters, String category, List<String> excludedTermUris ) {
        try {
            return ( long ) expressionExperimentService.getAnnotationsUsageFrequency(
                    filters,
                    null,
                    category,
                    null,
                    excludedTermUris,
                    1,           // minFrequency — at least one experiment uses the term
                    null,
                    0,           // maxResults — unlimited; we count rather than render
                    false, false,
                    ANNOTATION_COUNT_TIMEOUT_MS, TimeUnit.MILLISECONDS ).size();
        } catch ( TimeoutException e ) {
            log.warn( "HomeStats: annotations-usage count timed out for category=" + category + "; reporting 0", e );
            return 0L;
        }
    }

    private Path snapshotFile() {
        return Paths.get( homeDir, HOME_STATS_DIR, HOME_STATS_FILE );
    }

    private void persist( HomeStats stats ) {
        Path target = snapshotFile();
        try {
            Files.createDirectories( target.getParent() );
            // Atomic write via temp file + ATOMIC_MOVE so a crashed refresh doesn't leave
            // a half-written JSON for the next REST request to choke on.
            Path tmp = Files.createTempFile( target.getParent(), "home_stats-", ".json.tmp" );
            json.writerWithDefaultPrettyPrinter().writeValue( tmp.toFile(), stats );
            Files.move( tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );
            log.info( "HomeStats: persisted snapshot to " + target );
        } catch ( IOException e ) {
            log.error( "HomeStats: could not persist snapshot to " + target + "; in-memory cache is still updated", e );
        }
    }
}
