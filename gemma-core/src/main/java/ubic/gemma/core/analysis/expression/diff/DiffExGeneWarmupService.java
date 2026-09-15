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
package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Periodic warm-up of the DEA "find results by gene" path. The hot endpoint
 * {@code /datasets/analyses/differential/results/genes/{id}} (in
 * {@code DatasetsWebService}) calls
 * {@link DifferentialExpressionResultService#findByGeneAndExperimentAnalyzedIds(Gene, boolean, boolean, Collection, boolean, java.util.Map, java.util.Map, java.util.Map, double, boolean)}.
 * <p>
 * Cold-cache latency is ~4 s; warm latency ~0.55 s (round-3 perf probe, see
 * {@code DEA_FINDBYGENE_COLDCACHE_RECCE.md}). The gap is InnoDB-page and
 * Hibernate query-cache cold-start, not a plan or index problem. This service
 * re-runs the call for a seed list of high-traffic genes on a fixed cadence so
 * curator-facing requests land on a warm buffer pool.
 * <p>
 * The seed list and scheduling cadence are config-overridable
 * ({@code gemma.diffex.warmup.*}). The discarded result intentionally streams
 * through the same DAO path the endpoint uses; we are paying the I/O so a user
 * doesn't have to.
 *
 * @author pavlidis-lab (perf round-3, Strategy A)
 */
@Service
public class DiffExGeneWarmupService {

    private static final Log log = LogFactory.getLog( DiffExGeneWarmupService.class );

    /**
     * Default seed list — curator-favourite genes that turn up in most DEA
     * lookups. Override via {@code gemma.diffex.warmup.genes}. Kept as a
     * compile-time constant so it can be substituted into the {@code @Value}
     * annotation default below.
     */
    static final String DEFAULT_SEED_SYMBOLS = ""
            + "TP53,BRCA1,BRCA2,KRAS,EGFR,MYC,TNF,IL6,TGFB1,GAPDH,"
            + "ACTB,CD4,CD8A,FOXP3,INS,LEP,APOE,MAPT,APP,SNCA,"
            + "PARK2,PINK1,LRRK2,HTT,DRD2,COMT,BDNF,NGF,VEGFA,HIF1A,"
            + "AKT1,MTOR,PIK3CA,PTEN,RB1,CDKN2A,MDM2,CCND1,CDK4,ERBB2,"
            + "ESR1,AR,PGR,FOXA1,GATA3,KRT8,KRT18,VIM,CDH1,SNAI1";

    /** Cap total wall time for one warm-up pass. */
    private static final long DEFAULT_MAX_PASS_MILLIS = 5L * 60L * 1000L;

    @Autowired
    private GeneService geneService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Value("${gemma.diffex.warmup.genes:" + DEFAULT_SEED_SYMBOLS + "}")
    private String seedSymbolsCsv;

    @Value("${gemma.diffex.warmup.maxPassMillis:300000}")
    private long maxPassMillis;

    /** Whether the warm-up is enabled. Defaults to on; ops can flip it off. */
    @Value("${gemma.diffex.warmup.enabled:true}")
    private boolean enabled;

    /**
     * Trigger one warm-up pass.
     * <p>
     * Initial delay 5 minutes (avoid boot tax); then re-run every 6 hours
     * because InnoDB pages do cycle out of the buffer pool under load.
     * Schedule values are config-driven so ops can tune without a redeploy.
     */
    @Scheduled(
            initialDelayString = "${gemma.diffex.warmup.initialDelay:300000}",
            fixedDelayString = "${gemma.diffex.warmup.fixedDelay:21600000}" )
    public void warmTopGenes() {
        if ( !enabled ) {
            log.debug( "DEA gene warm-up disabled (gemma.diffex.warmup.enabled=false)" );
            return;
        }
        List<String> symbols = parseSeedSymbols( seedSymbolsCsv );
        if ( symbols.isEmpty() ) {
            log.info( "DEA gene warm-up: empty seed list, nothing to do" );
            return;
        }

        long started = System.currentTimeMillis();
        long deadline = started + Math.max( 1000L, maxPassMillis );
        log.info( "Warming DEA cache for " + symbols.size() + " genes" );

        // Resolve the full EE id space once per pass — the warm-up wants to
        // touch the same pages real traffic does.
        List<Long> allEeIds;
        try {
            allEeIds = new ArrayList<>( expressionExperimentService.loadIdsWithCache( ( Filters ) null,
                    expressionExperimentService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ) ) );
        } catch ( Exception e ) {
            log.warn( "DEA gene warm-up aborted: could not resolve EE ID list", e );
            return;
        }
        if ( allEeIds.isEmpty() ) {
            log.info( "DEA gene warm-up: no experiments to warm against, skipping" );
            return;
        }

        int warmed = 0;
        int failures = 0;
        int skipped = 0;
        for ( String symbol : symbols ) {
            if ( System.currentTimeMillis() >= deadline ) {
                log.info( "DEA gene warm-up: deadline reached after " + warmed + " genes, stopping" );
                break;
            }
            try {
                int hits = warmOneSymbol( symbol, allEeIds );
                if ( hits == 0 ) {
                    skipped++;
                } else {
                    warmed++;
                }
            } catch ( Exception e ) {
                failures++;
                log.warn( "DEA gene warm-up failed for symbol '" + symbol + "': " + e.getMessage(), e );
            }
        }

        long elapsedSec = ( System.currentTimeMillis() - started ) / 1000L;
        log.info( "Warmed " + warmed + " genes in " + elapsedSec + "s; " + failures + " failures, " + skipped + " unresolved symbols" );
    }

    /**
     * Returns the number of {@link Gene} entities resolved for the symbol and
     * driven through the warmer. Zero means the symbol did not resolve.
     */
    private int warmOneSymbol( String symbol, List<Long> allEeIds ) {
        Collection<Gene> genes = geneService.findByOfficialSymbol( symbol );
        if ( genes == null || genes.isEmpty() ) {
            log.debug( "DEA gene warm-up: symbol '" + symbol + "' did not resolve" );
            return 0;
        }
        for ( Gene gene : genes ) {
            long start = System.currentTimeMillis();
            // Mirror DatasetsWebService:2320 — same flags, same map shape.
            differentialExpressionResultService.findByGeneAndExperimentAnalyzedIds(
                    gene,
                    /* useGene2Cs */ true,
                    /* keepNonSpecific */ false,
                    allEeIds,
                    /* includeSubSets */ true,
                    /* sourceExperimentIdMap */ new HashMap<>(),
                    /* experimentAnalyzedIdMap */ new HashMap<>(),
                    /* baselineMap */ new HashMap<DifferentialExpressionAnalysisResult, Baseline>(),
                    /* threshold */ 1.0,
                    /* initializeFactorValues */ true );
            if ( log.isDebugEnabled() ) {
                log.debug( "DEA warm-up: " + symbol + " (gene id=" + gene.getId() + ", taxon="
                        + ( gene.getTaxon() != null ? gene.getTaxon().getCommonName() : "?" )
                        + ") in " + ( System.currentTimeMillis() - start ) + "ms" );
            }
        }
        return genes.size();
    }

    static List<String> parseSeedSymbols( String csv ) {
        List<String> out = new ArrayList<>();
        if ( csv == null ) {
            return out;
        }
        for ( String s : csv.split( "," ) ) {
            String t = s.trim();
            if ( !t.isEmpty() ) {
                out.add( t );
            }
        }
        return out;
    }

    // --- Test seams (package-private) --------------------------------

    void setSeedSymbolsCsv( String csv ) {
        this.seedSymbolsCsv = csv;
    }

    void setMaxPassMillis( long maxPassMillis ) {
        this.maxPassMillis = maxPassMillis;
    }

    void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }
}
