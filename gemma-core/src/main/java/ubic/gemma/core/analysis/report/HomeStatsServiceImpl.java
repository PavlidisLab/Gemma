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
import org.hibernate.SessionFactory;
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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellDimensionExperimentDao;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.QueryUtils;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
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
    private SingleCellDimensionExperimentDao singleCellDimensionExperimentDao;
    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private ManualAuthenticationService manualAuthenticationService;

    /** URI prefix marking CHEBI chemical-entity terms. Used to narrow the "drug count" tile
     *  to actual chemicals (vs the broader `treatment` category-label, which also captures
     *  radiation exposure, behavioural interventions, etc.). */
    private static final String CHEBI_URI_PREFIX = "http://purl.obolibrary.org/obo/CHEBI_";

    /** Maximum number of factor-value-by-category rows to retain. Sorted desc by value. */
    private static final int FACTOR_VALUE_CATEGORY_LIMIT = 50;

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
        stats.setSampleCount( countRootBioMaterials() );
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

        // Single-cell experiment count — distinct EEs that have at least one SingleCellDimension
        // recorded. Counted against the full SCD link table without ACL filtering: an EE either
        // is or is not single-cell; the public/private partition only matters when surfacing
        // the EE itself, not when measuring corpus shape.
        stats.setSingleCellCount( singleCellDimensionExperimentDao.countDistinctExperiments() );

        // DEA result-set count — the per-contrast output unit. Uses the existing filterable count;
        // FilteringService.count applies the ACL EXISTS clause on the underlying EE so the number
        // reflects what an anonymous caller can actually retrieve via /resultSets.
        stats.setDeaResultSetCount( expressionAnalysisResultSetService.count( Filters.empty() ) );

        // Drug count — CHEBI-anchored characteristics only. Narrower than the `treatment`
        // category-label count which captures non-drug treatments too.
        stats.setDrugCount( countDistinctValueUrisByPrefix( CHEBI_URI_PREFIX ) );

        // Manipulated-gene count — distinct gene URIs annotating experiments as
        // perturbation targets. Genes carry the NCBI gene-record namespace, see Gene#NCBI_URI_PREFIX.
        stats.setGeneManipulatedCount( countDistinctValueUrisByPrefix( Gene.NCBI_URI_PREFIX ) );

        // Companion: count of experiments touched by any gene-URI annotation (vs the
        // distinct-genes count above, which counts the genes themselves).
        stats.setGeneManipulatedExperimentCount( countDistinctExperimentsByCharacteristicUriPrefix( Gene.NCBI_URI_PREFIX ) );

        // Total cells across single-cell experiments — sum of BioAssay.numberOfCells for
        // assays attached to EEs with a SingleCellDimension. Reported in millions on the
        // home page tile; this is the raw cell-level count.
        stats.setTotalCells( computeTotalCellsInSingleCellExperiments() );

        // Sample (biomaterial) counts split by tech bucket: single_cell / rna_seq / microarray.
        // Mutually exclusive — a single-cell RNA-seq study counts in single_cell only.
        // Companion to the corpus-wide sampleCount above; this is the tech-axis breakdown.
        Map<String, Long> samplesByTech = new LinkedHashMap<>();
        samplesByTech.put( "single_cell", countBioMaterialsInSingleCellExperiments() );
        samplesByTech.put( "rna_seq", countBioMaterialsByTechExcludingSingleCell(
                TechnologyType.SEQUENCING, TechnologyType.GENELIST ) );
        samplesByTech.put( "microarray", countBioMaterialsByTech(
                TechnologyType.ONECOLOR, TechnologyType.TWOCOLOR, TechnologyType.DUALMODE ) );
        stats.setSamplesByTech( samplesByTech );

        // Factor-value distribution by EF category — "how many distinct disease-state factor
        // values exist", "how many distinct genotypes", etc. Reflects the range of experimental
        // axes Gemma has measured along.
        stats.setFactorValuesByCategory( computeFactorValuesByCategory() );

        // Datasets-by-accession-source distribution — for the home page Datasets-tile
        // nested footnote ("GEO 22000 · ArrayExpress 800 · CELLxGENE 150 · none 599").
        stats.setDatasetsByAccessionSource( computeDatasetsByAccessionSource() );

        long elapsed = System.currentTimeMillis() - t0;
        log.info( "HomeStats: snapshot recomputed in " + elapsed + " ms — "
                + stats.getDatasetCount() + " datasets ("
                + stats.getSingleCellCount() + " single-cell), "
                + stats.getPlatformCount() + " platforms, "
                + stats.getSampleCount() + " samples (" + stats.getTotalCells() + " cells), "
                + stats.getByTaxon().size() + " taxa, "
                + stats.getOntologyTermCount() + " ontology terms, "
                + stats.getDrugCount() + " drugs, "
                + stats.getGeneManipulatedCount() + " manipulated genes ("
                + stats.getGeneManipulatedExperimentCount() + " experiments), "
                + stats.getDeaResultSetCount() + " DEA result sets, "
                + stats.getFactorValuesByCategory().size() + " FV-category rows, "
                + stats.getDatasetsByAccessionSource().size() + " accession sources, "
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

    /**
     * Count distinct {@code Characteristic.valueUri} values that start with the given prefix,
     * across all characteristics in the database. Used for the CHEBI-drug count and the
     * manipulated-gene count.
     * <p>
     * Not ACL-filtered — the count reflects the corpus, not what's visible to the
     * anonymous viewer. The cost of ACL-filtering this would be a per-characteristic
     * EE-lookup; for a stat tile that's not worth the precision (the public/private
     * partition has a tiny effect on a count like "distinct drugs in Gemma").
     */
    private long countDistinctValueUrisByPrefix( String uriPrefix ) {
        Long n = ( Long ) sessionFactory.getCurrentSession()
                .createQuery( "select count(distinct c.valueUri) from Characteristic c "
                        + "where c.valueUri like :prefix" )
                .setParameter( "prefix", uriPrefix + "%" )
                .setCacheable( true )
                .uniqueResult();
        return n != null ? n : 0L;
    }

    /**
     * Count distinct experiments touched by at least one characteristic whose {@code valueUri}
     * starts with the given prefix. Walks the {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC}
     * denormalization table (the same surface {@code getAnnotationsUsageFrequency} reads from),
     * so the count includes tags, sample-level annotations, and factor-value characteristics
     * uniformly. Not ACL-filtered — tile semantics are corpus shape, not anonymous visibility.
     */
    private long countDistinctExperimentsByCharacteristicUriPrefix( String uriPrefix ) {
        Number n = ( Number ) sessionFactory.getCurrentSession()
                .createNativeQuery( "select count(distinct EXPRESSION_EXPERIMENT_FK) "
                        + "from EXPRESSION_EXPERIMENT2CHARACTERISTIC "
                        + "where VALUE_URI like :prefix" )
                .setParameter( "prefix", uriPrefix + "%" )
                .setCacheable( true )
                .uniqueResult();
        return n != null ? n.longValue() : 0L;
    }

    /**
     * Distinct factor-value count grouped by ExperimentalFactor.category.category. Reflects
     * the range of experimental conditions Gemma has measured along each axis.
     * <p>
     * Joins {@code ExperimentalFactor → factorValues} and counts distinct FVs grouped by
     * the category Characteristic.category label. EFs with a null category are folded into
     * a single null-keyed bucket. Top {@link #FACTOR_VALUE_CATEGORY_LIMIT} buckets retained,
     * sorted descending by count.
     */
    private List<HomeStats.FactorValueCategoryStat> computeFactorValuesByCategory() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = sessionFactory.getCurrentSession()
                .createQuery( "select ef.category.category, ef.category.categoryUri, count(distinct fv.id) "
                        + "from ExperimentalFactor ef "
                        + "join ef.factorValues fv "
                        + "group by ef.category.category, ef.category.categoryUri "
                        + "order by count(distinct fv.id) desc" )
                .setCacheable( true )
                .setMaxResults( FACTOR_VALUE_CATEGORY_LIMIT )
                .list();
        List<HomeStats.FactorValueCategoryStat> out = new ArrayList<>( rows.size() );
        for ( Object[] row : rows ) {
            String category = ( String ) row[0];
            String categoryUri = ( String ) row[1];
            Long count = ( Long ) row[2];
            out.add( new HomeStats.FactorValueCategoryStat( category, categoryUri, count != null ? count : 0L ) );
        }
        return out;
    }

    /**
     * Datasets grouped by their external-database source (GEO, ArrayExpress, CELLxGENE,
     * etc.), with a {@code "none"} bucket for datasets without an external accession.
     * Sorted descending by count.
     * <p>
     * ACL-filtered to public EEs only — we pre-fetch the public-readable id set via
     * {@code expressionExperimentService.loadIdsWithCache(Filters.empty(), null)} (which
     * applies the EE2C ACL EXISTS clause) and then restrict the group-by to that set. The
     * sum of all buckets equals {@link HomeStats#getDatasetCount()}; if it doesn't the
     * count is broken, not the data. Two queries instead of one because HQL can't COALESCE
     * across a left-join — the grouped query naturally drops null-accession rows, so we
     * fold the no-accession bucket in via a separate count.
     */
    private List<HomeStats.AccessionSourceStat> computeDatasetsByAccessionSource() {
        Collection<Long> publicEeIds = QueryUtils.optimizeParameterList(
                expressionExperimentService.loadIdsWithCache( Filters.empty(), null ) );
        if ( publicEeIds.isEmpty() ) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = sessionFactory.getCurrentSession()
                .createQuery( "select ed.name, count(distinct ee.id) "
                        + "from ExpressionExperiment ee "
                        + "join ee.accession a "
                        + "join a.externalDatabase ed "
                        + "where ee.id in (:eeIds) "
                        + "group by ed.name "
                        + "order by count(distinct ee.id) desc" )
                .setParameterList( "eeIds", publicEeIds )
                .setCacheable( true )
                .list();
        List<HomeStats.AccessionSourceStat> out = new ArrayList<>( rows.size() + 1 );
        for ( Object[] row : rows ) {
            String name = ( String ) row[0];
            Long count = ( Long ) row[1];
            out.add( new HomeStats.AccessionSourceStat( name, count != null ? count : 0L ) );
        }
        Long noneCount = ( Long ) sessionFactory.getCurrentSession()
                .createQuery( "select count(distinct ee.id) from ExpressionExperiment ee "
                        + "where ee.accession is null and ee.id in (:eeIds)" )
                .setParameterList( "eeIds", publicEeIds )
                .setCacheable( true )
                .uniqueResult();
        if ( noneCount != null && noneCount > 0 ) {
            out.add( new HomeStats.AccessionSourceStat( "none", noneCount ) );
            out.sort( ( a, b ) -> Long.compare( b.getCount(), a.getCount() ) );
        }
        return out;
    }

    /**
     * Total individual cells across all single-cell experiments — sum of
     * {@code BioAssay.numberOfCells} for assays attached to EEs with a SingleCellDimension.
     */
    private long computeTotalCellsInSingleCellExperiments() {
        Number n = ( Number ) sessionFactory.getCurrentSession()
                .createQuery( "select sum(ba.numberOfCells) "
                        + "from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "where ba.numberOfCells is not null "
                        + "and exists ( "
                        + "  select 1 from SingleCellDimensionExperiment scde "
                        + "  where scde.expressionExperiment = ee "
                        + ")" )
                .setCacheable( true )
                .uniqueResult();
        return n != null ? n.longValue() : 0L;
    }

    /**
     * Count distinct ROOT biomaterials (sample-level, not cell-level / sub-) across the corpus.
     * <p>
     * The selector is {@code coalesce(bm.sourceBioMaterial.id, bm.id)} — collapses each
     * {@code ba.sampleUsed} BioMaterial to its parent if it's a sub-BM, otherwise itself.
     * This is the "biomaterials, not sub-biomaterials" semantic Paul flagged: in single-cell
     * studies each (sample, cell-type) pseudo-sample is stored as a derived BM with
     * {@code sourceBioMaterial} pointing back at the actual donor sample, and counting
     * {@code ba.sampleUsed} directly inflates the number by the cell-type cardinality.
     * One-level collapse only — chains deeper than (root → sub) are not walked, which is
     * fine for Gemma's current import pattern.
     */
    private long countRootBioMaterials() {
        Long n = ( Long ) sessionFactory.getCurrentSession()
                .createQuery( "select count(distinct coalesce(bm.sourceBioMaterial.id, bm.id)) "
                        + "from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.sampleUsed bm" )
                .setCacheable( true )
                .uniqueResult();
        return n != null ? n : 0L;
    }

    /**
     * Distinct root biomaterials (samples, not cell-type sub-BMs) in single-cell experiments.
     */
    private long countBioMaterialsInSingleCellExperiments() {
        Long n = ( Long ) sessionFactory.getCurrentSession()
                .createQuery( "select count(distinct coalesce(bm.sourceBioMaterial.id, bm.id)) "
                        + "from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.sampleUsed bm "
                        + "where exists ( "
                        + "  select 1 from SingleCellDimensionExperiment scde "
                        + "  where scde.expressionExperiment = ee "
                        + ")" )
                .setCacheable( true )
                .uniqueResult();
        return n != null ? n : 0L;
    }

    /**
     * Distinct root biomaterials on assays whose platform sits in any of the given technology
     * types. Used for the microarray bucket where there's no need to exclude single-cell
     * (single-cell EEs aren't on ONECOLOR/TWOCOLOR/DUALMODE platforms). For bulk data the
     * source-BM collapse is a no-op — ba.sampleUsed is already root — so the result matches
     * the natural sample count.
     */
    private long countBioMaterialsByTech( TechnologyType... techs ) {
        Long n = ( Long ) sessionFactory.getCurrentSession()
                .createQuery( "select count(distinct coalesce(bm.sourceBioMaterial.id, bm.id)) "
                        + "from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.sampleUsed bm "
                        + "join ba.arrayDesignUsed ad "
                        + "where ad.technologyType in (:techs)" )
                .setParameterList( "techs", techs )
                .setCacheable( true )
                .uniqueResult();
        return n != null ? n : 0L;
    }

    /**
     * Variant of {@link #countBioMaterialsByTech} that excludes single-cell EEs. Used for
     * the rna_seq bucket so a single-cell RNA-seq study isn't double-counted across the
     * single_cell and rna_seq tiles.
     */
    private long countBioMaterialsByTechExcludingSingleCell( TechnologyType... techs ) {
        Long n = ( Long ) sessionFactory.getCurrentSession()
                .createQuery( "select count(distinct coalesce(bm.sourceBioMaterial.id, bm.id)) "
                        + "from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.sampleUsed bm "
                        + "join ba.arrayDesignUsed ad "
                        + "where ad.technologyType in (:techs) "
                        + "and not exists ( "
                        + "  select 1 from SingleCellDimensionExperiment scde "
                        + "  where scde.expressionExperiment = ee "
                        + ")" )
                .setParameterList( "techs", techs )
                .setCacheable( true )
                .uniqueResult();
        return n != null ? n : 0L;
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
