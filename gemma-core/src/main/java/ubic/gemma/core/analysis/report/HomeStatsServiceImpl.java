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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
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

        // Wishlist for future passes (deliberately omitted from v1 to keep the daily-refresh
        // budget tight): single-cell EE count, distinct-ontology-term count, distinct
        // DEA-condition count, drug-annotation count, disease/tissue/cell-type term counts.
        // These need new HQL aggregates on Characteristic / FactorValue / SingleCellDimension —
        // see HOME_STATS_WISHLIST.md.

        long elapsed = System.currentTimeMillis() - t0;
        log.info( "HomeStats: snapshot recomputed in " + elapsed + " ms — "
                + stats.getDatasetCount() + " datasets, "
                + stats.getPlatformCount() + " platforms, "
                + stats.getSampleCount() + " samples, "
                + stats.getByTaxon().size() + " taxa, "
                + stats.getRecentExperiments().size() + " recent" );
        return stats;
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
