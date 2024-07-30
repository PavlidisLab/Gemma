/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.analysis.expression.diff;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.distribution.Histogram;
import ubic.basecode.util.SQLUtils;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.QueryUtils;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.batchParameterList;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * This is a key class for queries to retrieve differential expression results (as well as standard CRUD aspects of
 * working with DifferentialExpressionResults).
 *
 * @author keshav
 */
@Repository
public class DifferentialExpressionResultDaoImpl extends AbstractDao<DifferentialExpressionAnalysisResult>
        implements DifferentialExpressionResultDao {

    /*
     * Temporary. For mimicking the effect of storing only 'significant' results.
     */
    private static final Double CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX = 1.0;

    private static final String DIFF_EX_RESULTS_BY_GENE_QUERY = "select distinct e, r from DifferentialExpressionAnalysis a, BioSequence2GeneProduct bs2gp "
            + "join a.experimentAnalyzed e "
            + "join a.resultSets rs join rs.results r join r.probe p join p.biologicalCharacteristic bs "
            + "join bs2gp.geneProduct gp join gp.gene g "
            + "where g=:gene";

    private final DifferentialExpressionResultCache differentialExpressionResultCache;

    @Autowired
    public DifferentialExpressionResultDaoImpl( SessionFactory sessionFactory,
            DifferentialExpressionResultCache differentialExpressionResultCache ) {
        super( DifferentialExpressionAnalysisResult.class, sessionFactory );
        this.differentialExpressionResultCache = differentialExpressionResultCache;
    }

    @Override
    public Collection<DifferentialExpressionAnalysisResult> create( Collection<DifferentialExpressionAnalysisResult> entities ) {
        throw new UnsupportedOperationException( "Results cannot be created directly, use DifferentialExpressionAnalysisDao.create() instead." );
    }

    @Override
    public void remove( DifferentialExpressionAnalysisResult entity ) {
        throw new UnsupportedOperationException( "Results cannot be removed directly, use DifferentialExpressionAnalysisDao.remove() instead." );
    }

    @Override
    public List<DifferentialExpressionAnalysisResult> findByGeneAndExperimentAnalyzed(
            Gene gene,
            Collection<Long> experimentAnalyzedIds,
            boolean includeSubsets,
            @Nullable Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap,
            @Nullable Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap,
            @Nullable Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap,
            double threshold,
            boolean keepNonSpecificProbes,
            boolean initializeFactorValues ) {
        Assert.notNull( gene.getId(), "The gene must have a non-null ID." );
        Assert.isTrue( threshold >= 0.0 && threshold <= 1.0, "Threshold must be in the [0, 1] interval." );
        if ( experimentAnalyzedIds.isEmpty() ) {
            return Collections.emptyList();
        }
        StopWatch timer = StopWatch.createStarted();
        StopWatch retrieveProbesTimer = StopWatch.createStarted();
        //noinspection unchecked
        List<Long> probeIds = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select CS from GENE2CS where GENE = :geneId"
                        // only retain probes that map to a single gene in the platform
                        + ( keepNonSpecificProbes ? "" : " and (select count(distinct gene2cs2.GENE) from GENE2CS gene2cs2 where gene2cs2.AD = GENE2CS.AD and gene2cs2.CS = GENE2CS.CS) = 1" ) )
                .addScalar( "CS", StandardBasicTypes.LONG )
                .setParameter( "geneId", gene.getId() )
                .list();
        retrieveProbesTimer.stop();
        if ( probeIds.isEmpty() ) {
            log.warn( String.format( "%s has no associated probes in the GENE2CS table, no differential expression results will be returned.", gene ) );
            return Collections.emptyList();
        }
        StopWatch retrieveBioAssayIdsTimer = StopWatch.createStarted();
        Set<Long> bioAssaySetIds = new HashSet<>( experimentAnalyzedIds );
        Map<Long, Long> subsetIdToExperimentId = null;
        // create a mapping of subset ID to source experiment ID
        if ( sourceExperimentIdMap != null ) {
            subsetIdToExperimentId = QueryUtils.streamByBatch( getSessionFactory().getCurrentSession()
                            .createQuery( "select eess.id, eess.sourceExperiment.id from ExpressionExperimentSubSet eess"
                                    + " where eess.sourceExperiment.id in :eeIds or eess.id in :eeIds" ), "eeIds", experimentAnalyzedIds, 2048, Object[].class )
                    .collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[1] ) );
            if ( includeSubsets ) {
                bioAssaySetIds.addAll( subsetIdToExperimentId.keySet() );
            }
        } else if ( includeSubsets ) {
            List<Long> subsetIds = QueryUtils.listByBatch( getSessionFactory().getCurrentSession()
                    .createQuery( "select eess.id from ExpressionExperimentSubSet eess"
                            + " where eess.sourceExperiment.id in :eeIds or eess.id in :eeIds" ), "eeIds", experimentAnalyzedIds, 2048 );
            bioAssaySetIds.addAll( subsetIds );
        }
        retrieveBioAssayIdsTimer.stop();
        StopWatch retrieveResultsTimer = StopWatch.createStarted();
        Query query = getSessionFactory().getCurrentSession()
                .createQuery( "select dear, dea.experimentAnalyzed.id" + ( baselineMap != null ? ", b.id, be.type" : "" ) + " from DifferentialExpressionAnalysisResult dear "
                        + "join dear.resultSet dears "
                        + "join dears.analysis dea "
                        + ( baselineMap != null ? "left join dears.baselineGroup b left join b.experimentalFactor be " : "" )
                        + "where dear.probe.id in :probeIds and dea.experimentAnalyzed.id in :bioAssaySetIds and dear.correctedPvalue <= :threshold "
                        // if more than one probe is found, pick the one with the lowest corrected p-value
                        + "group by dears "
                        // ascending, nulls last
                        + "order by -dear.correctedPvalue desc" )
                .setParameterList( "probeIds", optimizeParameterList( probeIds ) )
                .setParameter( "threshold", threshold );
        List<Object[]> result = QueryUtils.listByBatch( query, "bioAssaySetIds", bioAssaySetIds, 2048 );
        retrieveResultsTimer.stop();
        List<DifferentialExpressionAnalysisResult> rs = new ArrayList<>( result.size() );
        int warns = 0;
        // using separate loops ensure that hibernate can batch-initialize without interleaving queries
        StopWatch probeInitializationTimer = StopWatch.createStarted();
        for ( Object[] row : result ) {
            DifferentialExpressionAnalysisResult r = ( DifferentialExpressionAnalysisResult ) row[0];
            Hibernate.initialize( r.getProbe() );
        }
        probeInitializationTimer.stop();

        StopWatch contrastInitializationTimer = StopWatch.createStarted();
        for ( Object[] row : result ) {
            DifferentialExpressionAnalysisResult r = ( DifferentialExpressionAnalysisResult ) row[0];
            Hibernate.initialize( r.getContrasts() );
        }
        contrastInitializationTimer.stop();

        if ( initializeFactorValues ) {
            for ( Object[] row : result ) {
                DifferentialExpressionAnalysisResult r = ( DifferentialExpressionAnalysisResult ) row[0];
                for ( ContrastResult c : r.getContrasts() ) {
                    Hibernate.initialize( c.getFactorValue() );
                    Hibernate.initialize( c.getSecondFactorValue() );
                }
            }
        }

        for ( Object[] row : result ) {
            DifferentialExpressionAnalysisResult r = ( DifferentialExpressionAnalysisResult ) row[0];
            Long bioAssaySetId = ( Long ) row[1];
            rs.add( r );
            if ( sourceExperimentIdMap != null ) {
                sourceExperimentIdMap.put( r, subsetIdToExperimentId.getOrDefault( bioAssaySetId, bioAssaySetId ) );
            }
            if ( experimentAnalyzedIdMap != null ) {
                experimentAnalyzedIdMap.put( r, bioAssaySetId );
            }
            if ( baselineMap != null ) {
                // TODO: add support for interaction of factors, requires https://github.com/PavlidisLab/Gemma/issues/1122
                Long baselineId = ( Long ) row[2];
                FactorType baselineType = ( FactorType ) row[3];
                if ( baselineId != null ) {
                    // create a proxy, cheap
                    if ( baselineType.equals( FactorType.CATEGORICAL ) ) {
                        FactorValue baseline = ( FactorValue ) getSessionFactory().getCurrentSession().load( FactorValue.class, baselineId );
                        baselineMap.put( r, Baseline.categorical( baseline ) );
                    } else {
                        // we have a few experiments with continuous factors with a baseline set in the result set, this
                        // is incorrect and is being tracked in https://github.com/PavlidisLab/GemmaCuration/issues/530
                        String msg = String.format( "Unexpected factor type for baseline FactorValue Id=%d of %s: %s, it should be categorical.",
                                baselineId, r, baselineType );
                        if ( warns < 5 ) {
                            log.warn( msg );
                            warns++;
                        } else {
                            if ( warns == 5 ) {
                                log.warn( "Only showing first 5 warnings, additional warnings will be emitted in debug logs." );
                                warns++;
                            }
                            log.debug( msg );
                        }
                    }
                }
            }
        }
        if ( baselineMap != null && initializeFactorValues ) {
            for ( Baseline baseline : baselineMap.values() ) {
                Hibernate.initialize( baseline.getFactorValue() );
                Hibernate.initialize( baseline.getSecondFactorValue() );
            }
        }
        // because of batching, results must be resorted
        rs.sort( Comparator.comparing( DifferentialExpressionAnalysisResult::getCorrectedPvalue, Comparator.nullsLast( Comparator.naturalOrder() ) ) );
        if ( timer.getTime() > 1000 ) {
            log.warn( String.format( "Retrieving %d diffex results for %s took %d ms (retrieving probes from genes: %d ms, retrieving subsets: %d ms, retrieving results: %d ms, initializing contrasts: %d ms, initializing probes: %d ms)",
                    rs.size(), gene, timer.getTime(),
                    retrieveProbesTimer.getTime(),
                    retrieveBioAssayIdsTimer.getTime(),
                    retrieveResultsTimer.getTime(),
                    contrastInitializationTimer.getTime(), probeInitializationTimer.getTime() ) );
        }
        return rs;
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            Collection<Long> experimentsAnalyzed, double threshold, int limit ) {
        if ( experimentsAnalyzed.isEmpty() ) {
            return Collections.emptyMap();
        }

        StopWatch timer = StopWatch.createStarted();
        List<?> qResult = getSessionFactory().getCurrentSession()
                .createQuery( DIFF_EX_RESULTS_BY_GENE_QUERY
                        + " and e.id in (:experimentAnalyzed) "
                        + "and r.correctedPvalue <= :threshold"
                        // no need for the hack, nulls are filtered by the threshold
                        + ( limit > 0 ? " order by r.correctedPvalue" : "" ) )
                .setParameter( "gene", gene )
                .setParameterList( "experimentsAnalyzed", optimizeParameterList( experimentsAnalyzed ) )
                .setParameter( "threshold", threshold )
                .setMaxResults( limit )
                .setCacheable( true )
                .setCacheRegion( "diffExResult" )
                .list();

        Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> results = groupDiffExResultVos( qResult );
        AbstractDao.log.info( String.format( "Num experiments with probe analysis results (with limit = %d) : %d. Number of probes returned in total: %d",
                limit, results.size(), qResult.size() ) );

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.warn( "Diff ex results: " + timer.getTime() + " ms" );
        }

        return results;
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> findByExperimentAnalyzed(
            Collection<Long> experiments, double qvalueThreshold, int limit ) {
        if ( experiments.isEmpty() ) {
            return Collections.emptyMap();
        }
        StopWatch timer = StopWatch.createStarted();
        List<?> qResult = getSessionFactory().getCurrentSession()
                .createQuery( "select e, r from DifferentialExpressionAnalysis a "
                        + "join a.experimentAnalyzed e  "
                        + "join a.resultSets rs "
                        + "join rs.results r "
                        + "where e.id in (:experimentsAnalyzed) "
                        + "and r.correctedPvalue <= :threshold"
                        + ( limit > 0 ? " order by r.correctedPvalue" : "" ) )
                .setParameterList( "experimentsAnalyzed", optimizeParameterList( experiments ) )
                .setParameter( "threshold", qvalueThreshold )
                .setMaxResults( limit )
                .setCacheable( true )
                .setCacheRegion( "diffExResult" )
                .list();
        try {
            return groupDiffExResultVos( qResult );
        } finally {
            timer.stop();
            if ( timer.getTime() > 1000 ) {
                AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
            }
        }
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene ) {
        Assert.notNull( gene );
        StopWatch timer = StopWatch.createStarted();
        List<?> qResult = getSessionFactory().getCurrentSession()
                .createQuery( DIFF_EX_RESULTS_BY_GENE_QUERY )
                .setParameter( "gene", gene )
                .setCacheable( true )
                .list();
        try {
            return groupDiffExResultVos( qResult );
        } finally {
            timer.stop();
            if ( timer.getTime() > 1000 ) {
                AbstractDao.log.warn( "Diff ex results: " + timer.getTime() + " ms" );
            }
        }
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            Collection<Long> experimentsAnalyzed ) {
        Assert.notNull( gene );
        if ( experimentsAnalyzed.isEmpty() ) {
            return Collections.emptyMap();
        }
        StopWatch timer = StopWatch.createStarted();
        List<?> qResult = this.getSessionFactory().getCurrentSession()
                .createQuery( DIFF_EX_RESULTS_BY_GENE_QUERY + " and e.id in (:experimentsAnalyzed)" )
                .setParameter( "gene", gene )
                .setParameterList( "experimentsAnalyzed", optimizeParameterList( experimentsAnalyzed ) )
                .list();
        try {
            return groupDiffExResultVos( qResult );
        } finally {
            timer.stop();
            if ( timer.getTime() > 1000 ) {
                AbstractDao.log.warn( "Diff ex results: " + timer.getTime() + " ms" );
            }
        }
    }

    private Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> groupDiffExResultVos( List<?> qResult ) {
        Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> results = new HashMap<>();
        for ( Object o : qResult ) {
            Object[] oa = ( Object[] ) o;
            ExpressionExperimentValueObject ee = ( ( BioAssaySet ) oa[0] ).createValueObject();
            DifferentialExpressionAnalysisResult dear = ( DifferentialExpressionAnalysisResult ) oa[1];
            Hibernate.initialize( dear.getProbe() );
            DifferentialExpressionValueObject probeResult = new DifferentialExpressionValueObject( dear );
            results.computeIfAbsent( ee, k -> new ArrayList<>() ).add( probeResult );
        }
        return results;
    }

    @Override
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findDiffExAnalysisResultIdsInResultSets(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds ) {

        Map<Long, Map<Long, DiffExprGeneSearchResult>> results = new HashMap<>();

        Session session = this.getSessionFactory().getCurrentSession();

        Map<Long, DiffExResultSetSummaryValueObject> resultSetIdsMap = resultSets.stream()
                .collect( Collectors.toMap( DiffExResultSetSummaryValueObject::getResultSetId, rs -> rs, ( a, b ) -> b ) );

        Map<Long, Collection<Long>> foundInCache = this.fillFromCache( results, resultSetIdsMap.keySet(), geneIds );

        if ( !foundInCache.isEmpty() ) {
            AbstractDao.log.info( "Results for " + foundInCache.size() + " resultsets found in cache" );
        } else {
            AbstractDao.log.info( "No results were in the cache" );
        }

        Collection<Long> resultSetsNeeded = this
                .stripUnneededResultSets( foundInCache, resultSetIdsMap.keySet(), geneIds );

        // Are we finished?
        if ( resultSetsNeeded.isEmpty() ) {
            AbstractDao.log.info( "All results were in the cache." );
            return results;
        }

        AbstractDao.log.info( foundInCache.size() + "/" + resultSetIdsMap.size()
                + " resultsSets had at least some cached results; still need to query " + resultSetsNeeded.size() );

        assert !resultSetsNeeded.isEmpty();

        SQLQuery queryObject = session.createSQLQuery(
                "SELECT dear.PROBE_FK, dear.ID,"
                        + " dear.RESULT_SET_FK, dear.CORRECTED_PVALUE, dear.PVALUE  "
                        + " FROM DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear FORCE INDEX (probeResultSets) WHERE dear.RESULT_SET_FK IN (:rs_ids) AND "
                        + " dear.PROBE_FK IN (:probe_ids) " );

        /*
         * These values have been tweaked to probe for performance issues.
         */
        int resultSetBatchSize = 50;
        int geneBatchSize = 100;

        if ( resultSetsNeeded.size() > geneIds.size() ) {
            resultSetBatchSize = Math.min( 500, resultSetsNeeded.size() );
            AbstractDao.log
                    .info( "Batching by result sets (" + resultSetsNeeded.size() + " resultSets); " + geneIds.size()
                            + " genes; batch size=" + resultSetBatchSize );

        } else {
            geneBatchSize = Math.min( 200, geneIds.size() );
            AbstractDao.log.info( "Batching by genes (" + geneIds.size() + " genes); " + resultSetsNeeded.size()
                    + " resultSets; batch size=" + geneBatchSize );
        }

        final int numResultSetBatches = ( int ) Math.ceil( ( double ) resultSetsNeeded.size() / ( double ) resultSetBatchSize );

        queryObject.setFlushMode( FlushMode.MANUAL );

        StopWatch timer = new StopWatch();
        timer.start();
        int numResults = 0;
        long timeForFillingNonSig = 0;

        Map<Long, Map<Long, DiffExprGeneSearchResult>> resultsFromDb = new HashMap<>();

        int numResultSetBatchesDone = 0;

        // Iterate over batches of resultSets
        for ( Collection<Long> resultSetIdBatch : batchParameterList( resultSetsNeeded, resultSetBatchSize ) ) {

            if ( AbstractDao.log.isDebugEnabled() )
                AbstractDao.log.debug( "Starting batch of resultsets: " + StringUtils
                        .abbreviate( StringUtils.join( resultSetIdBatch, "," ), 100 ) );

            /*
             * Get the probes using the CommonQueries gene2cs. Otherwise we (in effect) end up doing this over and over
             * again.
             */
            Map<Long, Collection<Long>> cs2GeneIdMap = this
                    .getProbesForGenesInResultSetBatch( session, geneIds, resultSetIdsMap, resultSetIdBatch );

            queryObject.setParameterList( "rs_ids", resultSetIdBatch );

            int numGeneBatchesDone = 0;
            final int numGeneBatches = ( int ) Math.ceil( ( double ) cs2GeneIdMap.size() / ( double ) geneBatchSize );

            StopWatch innerQt = new StopWatch();

            // iterate over batches of probes (genes)
            for ( Collection<Long> probeBatch : batchParameterList( cs2GeneIdMap.keySet(), geneBatchSize ) ) {

                if ( AbstractDao.log.isDebugEnabled() )
                    AbstractDao.log.debug( "Starting batch of probes: " + StringUtils
                            .abbreviate( StringUtils.join( probeBatch, "," ), 100 ) );

                queryObject.setParameterList( "probe_ids", probeBatch );

                innerQt.start();
                List<?> queryResult = queryObject.list();
                innerQt.stop();

                if ( innerQt.getTime() > 2000 ) {
                    // show the actual query with params.
                    AbstractDao.log.info( "Query time: " + innerQt.getTime() + "ms:\n " + queryObject.getQueryString()
                            .replace( ":probe_ids", StringUtils.join( probeBatch, "," ) )
                            .replace( ":rs_ids", StringUtils.join( resultSetIdBatch, "," ) ) );
                }
                innerQt.reset();

                /*
                 * Each query tuple are the probe, result, resultsSet, qvalue, pvalue.
                 */
                for ( Object o : queryResult ) {
                    // Long resultSetId = ( ( BigInteger )((Object[])o)[2] ).longValue();
                    // if (!resultSetId.equals)
                    numResults += this.processResultTuple( o, resultsFromDb, cs2GeneIdMap );
                }

                if ( timer.getTime() > 5000 && AbstractDao.log.isInfoEnabled() ) {
                    AbstractDao.log.info( "Batch time: " + timer.getTime() + "ms; Fetched DiffEx " + numResults
                            + " results so far. " + numResultSetBatchesDone + "/" + numResultSetBatches
                            + " resultset batches completed. " + numGeneBatchesDone + "/" + numGeneBatches
                            + " gene batches done." );
                    timer.reset();
                    timer.start();
                }

                // Check if task was cancelled.
                if ( Thread.currentThread().isInterrupted() ) {
                    throw new RuntimeException( "Search was cancelled" ) {
                        private static final long serialVersionUID = 7343146551545342910L;
                    };
                }

                numGeneBatchesDone++;

                if ( DifferentialExpressionResultDaoImpl.CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX < 1.0 ) {
                    timeForFillingNonSig += this
                            .fillNonSignificant( probeBatch, resultSetIdsMap, resultsFromDb, resultSetIdBatch, cs2GeneIdMap,
                                    session );
                }
            } // over probes.

            // Check if task was cancelled.
            if ( Thread.currentThread().isInterrupted() ) {
                throw new RuntimeException( "Search was cancelled" ) {
                    private static final long serialVersionUID = 7343146551545342910L;
                };
            }

            numResultSetBatchesDone++;

        }

        if ( timer.getTime() > 1000 && AbstractDao.log.isInfoEnabled() ) {
            AbstractDao.log
                    .info( "Fetching DiffEx from DB took total of " + timer.getTime() + " ms : geneIds=" + StringUtils
                            .abbreviate( StringUtils.join( geneIds, "," ), 50 ) + " result set="
                            + StringUtils
                            .abbreviate( StringUtils.join( resultSetsNeeded, "," ), 50 ) );
            if ( timeForFillingNonSig > 100 ) {
                AbstractDao.log.info( "Filling in non-significant values: " + timeForFillingNonSig + "ms in total" );
            }
        }

        // Add the DB results to the cached results.
        this.addToCache( resultsFromDb, resultSetsNeeded, geneIds );

        for ( Long resultSetId : resultsFromDb.keySet() ) {
            Map<Long, DiffExprGeneSearchResult> geneResults = resultsFromDb.get( resultSetId );
            if ( results.containsKey( resultSetId ) ) {
                results.get( resultSetId ).putAll( geneResults );
            } else {
                results.put( resultSetId, geneResults );
            }
        }

        return results;
    }

    @Override
    public List<DifferentialExpressionValueObject> findInResultSet( ExpressionAnalysisResultSet resultSet,
            double threshold, int limit, int minNumberOfResults ) {
        Assert.notNull( resultSet, "The result set must not be null." );
        Assert.isTrue( minNumberOfResults > 0, "Minimum number of results must be greater than zero." );

        // check for top hits in cache
        List<DifferentialExpressionValueObject> cachedResults = differentialExpressionResultCache.getTopHits( resultSet );
        if ( cachedResults != null && cachedResults.size() >= minNumberOfResults ) {
            AbstractDao.log.info( "Top hits already in cache" );
            return cachedResults;
        }

        // retrieve top hits from database
        StopWatch timer = new StopWatch();
        timer.start();
        String qs = "select r from DifferentialExpressionAnalysisResult r "
                + "where r.resultSet = :resultSet and r.correctedPvalue <= :threshold "
                // nulls are filtered out by the threshold, no need for the hack
                + "order by r.correctedPvalue";

        List<?> qResult = getSessionFactory().getCurrentSession().createQuery( qs )
                .setParameter( "resultSet", resultSet )
                .setParameter( "threshold", threshold )
                .setMaxResults( limit )
                .list();

        // If too few probes meet threshold, redo and just get top results.
        if ( qResult.size() < minNumberOfResults ) {
            // FIXME this is kind of dumb. If we always return the top minimum, why not just always get that?
            AbstractDao.log.info( "Too few results met threshold, repeating to just get the top hits" );
            qs = "select r from DifferentialExpressionAnalysisResult r "
                    + "where r.resultSet = :resultSet "
                    // ascending, nulls last
                    + "order by -r.correctedPvalue desc";
            qResult = getSessionFactory().getCurrentSession().createQuery( qs )
                    .setParameter( "resultSet", resultSet )
                    .setMaxResults( minNumberOfResults )
                    .list();
        }

        List<DifferentialExpressionValueObject> results = new ArrayList<>();
        for ( Object o : qResult ) {
            DifferentialExpressionAnalysisResult dear = ( DifferentialExpressionAnalysisResult ) o;
            Hibernate.initialize( dear.getProbe() );
            DifferentialExpressionValueObject probeResult = new DifferentialExpressionValueObject( dear );
            results.add( probeResult );
        }

        differentialExpressionResultCache.addToTopHitsCache( resultSet, results );

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        return results;
    }

    /**
     * Key method for getting contrasts associated with results.
     */
    @Override
    public Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids ) {
        //language=SQL

        Map<Long, ContrastsValueObject> probeResults = new HashMap<>();

        if ( ids.isEmpty() ) {
            return probeResults;
        }

        SQLQuery query = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "SELECT DISTINCT c.ID, c.LOG_FOLD_CHANGE, c.FACTOR_VALUE_FK,"
                        + " c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK, c.PVALUE FROM CONTRAST_RESULT c"
                        + " WHERE c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK IN (:ids)  " );

        int BATCH_SIZE = 2000; // previously: 500, then 1000. New optimized query is plenty fast.
        StopWatch timer = new StopWatch();
        for ( Collection<Long> batch : batchParameterList( ids, BATCH_SIZE ) ) {
            timer.reset();
            timer.start();

            query.setParameterList( "ids", batch );

            List<?> batchR = query.list();

            for ( Object o : batchR ) {
                Object[] ol = ( Object[] ) o;

                Long resultId = ( ( BigInteger ) ol[3] ).longValue();

                if ( !probeResults.containsKey( resultId ) ) {
                    probeResults.put( resultId, new ContrastsValueObject( resultId ) );
                }

                ContrastsValueObject cvo = probeResults.get( resultId );

                Long contrastId = ( ( BigInteger ) ol[0] ).longValue();

                Double logFoldChange = ol[1] == null ? null : ( Double ) ol[1];

                Long factorValueId = ol[2] == null ? null : ( ( BigInteger ) ol[2] ).longValue();

                Double pvalue = ol[4] == null ? null : ( Double ) ol[4];

                cvo.addContrast( contrastId, factorValueId, logFoldChange, pvalue, null );

            }

            if ( timer.getTime() > 2000 ) {
                AbstractDao.log.info( "Fetch " + batch.size() + " results with contrasts: " + timer.getTime()
                        + "ms; query was\n " + ( "SELECT DISTINCT c.ID, c.LOG_FOLD_CHANGE, c.FACTOR_VALUE_FK,"
                        + " c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK, c.PVALUE FROM CONTRAST_RESULT c"
                        + " WHERE c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK IN (:ids)  " ).replace( ":ids", StringUtils.join( batch, "," ) ) );
            }
        }

        return probeResults;
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            double threshold, int limit ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Session session = this.getSessionFactory().getCurrentSession();
        String sql = "select d.ID, d.PVALUE as D_PVALUE, d.CORRECTED_PVALUE, d.PROBE_FK, d.RESULT_SET_FK, c.FACTOR_VALUE_FK, c.PVALUE as C_PVALUE, c.LOG_FOLD_CHANGE, c.SECOND_FACTOR_VALUE_FK, c.ID as C_ID "
                + "from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT d, GENE2CS g2s, CONTRAST_RESULT c "
                + "where g2s.CS = d.PROBE_FK and c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK = d.ID and g2s.GENE = :gene_id";

        if ( threshold > 0.0 ) {
            sql = sql + " and d.CORRECTED_PVALUE <= :threshold ";
        }

        if ( limit > 0 ) {
            sql = sql + " order by d.PVALUE";
        }

        SQLQuery query = session.createSQLQuery( sql );

        query.setParameter( "gene_id", gene.getId() );
        query.setMaxResults( limit );
        if ( threshold > 0.0 ) {
            query.setParameter( "threshold", threshold );
        }

        Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> results = new HashMap<>();

        //noinspection unchecked
        List<Object[]> qResult = query.list();

        Set<Long> resultSets = new HashSet<>();

        Map<Long, DifferentialExpressionValueObject> resultsInter = new HashMap<>();
        Set<Long> probeIds = new HashSet<>();
        GeneValueObject genevo = new GeneValueObject( gene );

        for ( Object[] rec : qResult ) {
            Long id = SQLUtils.asId( rec[0] );

            if ( !resultsInter.containsKey( id ) ) {

                DifferentialExpressionValueObject vo = new DifferentialExpressionValueObject( id );
                vo.setP( ( Double ) rec[1] );
                vo.setCorrP( ( Double ) rec[2] );
                vo.setProbeId( SQLUtils.asId( rec[3] ) ); // fill in probe name later.
                probeIds.add( vo.getProbeId() );
                vo.setResultSetId( SQLUtils.asId( rec[4] ) );
                if ( threshold > 0 )
                    vo.setMetThreshold( true );

                vo.setGene( genevo );

                // gather up result sets so we can fetch the experiments, experimental factors
                resultSets.add( vo.getResultSetId() );

                resultsInter.put( id, vo );

            }

            resultsInter.get( id ).addContrast( SQLUtils.asId( rec[9] ), SQLUtils.asId( rec[5] ), ( Double ) rec[6],
                    ( Double ) rec[7], SQLUtils.asId( rec[8] ) );
        }

        // gather up probe information
        Map<Long, String> probeNames = new HashMap<>();
        if ( !probeIds.isEmpty() ) {
            //noinspection unchecked
            for ( Object[] rec : ( List<Object[]> ) session
                    .createQuery( "select id,name from CompositeSequence where id in (:ids)" )
                    .setParameterList( "ids", optimizeParameterList( probeIds ) ).list() ) {
                probeNames.put( ( Long ) rec[0], ( String ) rec[1] );
            }
        }

        /*
         * load the result set information.
         */
        if ( resultSets.isEmpty() )
            return results;

        //noinspection unchecked
        List<Object[]> ees = session
                .createQuery( "select ee, rs from ExpressionAnalysisResultSet rs "
                        + "join fetch rs.experimentalFactors join rs.analysis a join a.experimentAnalyzed ee "
                        + "where rs.id in (:rsids)" )
                .setParameterList( "rsids", optimizeParameterList( resultSets ) ).list();

        /*
         * Finish populating the objects
         */
        for ( Object[] oa : ees ) {
            ExpressionAnalysisResultSet rs = ( ExpressionAnalysisResultSet ) oa[1];
            ExpressionExperimentValueObject evo = ( ( BioAssaySet ) oa[0] ).createValueObject();

            if ( !results.containsKey( evo ) ) {
                results.put( evo, new ArrayList<>() );
            }

            for ( Iterator<DifferentialExpressionValueObject> it = resultsInter.values().iterator(); it.hasNext(); ) {

                DifferentialExpressionValueObject dvo = it.next();
                dvo.setExpressionExperiment( evo );
                dvo.setProbe( probeNames.get( dvo.getProbeId() ) );
                dvo.getExperimentalFactors().clear();

                for ( ExperimentalFactor ef : rs.getExperimentalFactors() ) {
                    dvo.getExperimentalFactors().add( new ExperimentalFactorValueObject( ef ) );

                }

                if ( dvo.getResultSetId().equals( rs.getId() ) ) {
                    results.get( evo ).add( dvo );
                    it.remove();
                }

            }
        }

        AbstractDao.log
                .debug( "Num experiments with probe analysis results (with limit = " + limit + ") : " + results.size()
                        + ". Number of results (probes x contrasts) returned in total: " + qResult.size() );

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        return results;
    }

    @Override
    public Histogram loadPvalueDistribution( Long resultSetId ) {

        List<?> pvds = this.getSessionFactory().getCurrentSession()
                .createQuery( "select rs.pvalueDistribution from ExpressionAnalysisResultSet rs where rs.id=:rsid " )
                .setParameter( "rsid", resultSetId )
                .list();
        if ( pvds.isEmpty() ) {
            return null;
        }

        assert pvds.size() == 1;

        PvalueDistribution pvd = ( PvalueDistribution ) pvds.get( 0 );
        ByteArrayConverter bac = new ByteArrayConverter();
        double[] counts = bac.byteArrayToDoubles( pvd.getBinCounts() );

        Integer numBins = pvd.getNumBins();
        assert numBins == counts.length;

        Histogram hist = new Histogram( resultSetId.toString(), numBins, 0.0, 1.0 );
        for ( int i = 0; i < numBins; i++ ) {
            hist.fill( i, ( int ) counts[i] );
        }

        return hist;

    }

    @Override
    public Collection<DifferentialExpressionAnalysisResult> loadAll() {
        throw new UnsupportedOperationException( "Sorry, that would be nuts" );
    }

    /**
     * @return how many results were added. Either 1 or 0.
     */
    private int processResultTuple( Object resultRow, Map<Long, Map<Long, DiffExprGeneSearchResult>> resultsFromDb,
            Map<Long, Collection<Long>> cs2GeneIdMap ) {
        Object[] row = ( Object[] ) resultRow;
        Long probeId = ( ( BigInteger ) row[0] ).longValue();
        Long resultId = ( ( BigInteger ) row[1] ).longValue();
        Long resultSetId = ( ( BigInteger ) row[2] ).longValue();
        Double correctedPvalue = ( Double ) row[3];
        Double pvalue = ( Double ) row[4];

        if ( pvalue == null || correctedPvalue == null ) {
            return 0;
        }

        if ( !resultsFromDb.containsKey( resultSetId ) ) {
            resultsFromDb.put( resultSetId, new HashMap<>() );
        }

        assert cs2GeneIdMap.containsKey( probeId );

        /*
         * This is a hack to mimic the effect of storing only 'good' results.
         */
        if ( correctedPvalue > DifferentialExpressionResultDaoImpl.CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX ) {
            return 0;
        }

        for ( Long geneId : cs2GeneIdMap.get( probeId ) ) {
            this.processDiffExResultHit( resultsFromDb.get( resultSetId ), resultSetId, geneId, resultId,
                    correctedPvalue, pvalue );
        }

        if ( AbstractDao.log.isDebugEnabled() )
            AbstractDao.log.debug( "resultset=" + resultSetId + " probe=" + probeId + " qval=" + String
                    .format( "%.2g", correctedPvalue ) );

        return 1;
    }

    /**
     * This is to be called after a query for diff ex results is finished for a set of resultSets and genes. It assumes
     * that if a gene is missing from the results, there are none for that resultSet for that gene. It then stores a
     * dummy entry.
     *
     * @param results - which might be empty.
     * @param resultSetids - the ones which we searched for in the database. Put in dummy results if we have to.
     */
    private void addToCache( Map<Long, Map<Long, DiffExprGeneSearchResult>> results, Collection<Long> resultSetids,
            Collection<Long> geneIds ) {
        StopWatch timer = new StopWatch();
        timer.start();
        int i = 0;
        for ( Long resultSetId : resultSetids ) {
            Map<Long, DiffExprGeneSearchResult> resultSetResults = results.get( resultSetId );

            for ( Long geneId : geneIds ) {

                if ( resultSetResults != null && resultSetResults.containsKey( geneId ) ) {
                    this.differentialExpressionResultCache.addToCache( resultSetResults.get( geneId ) );
                } else {
                    // put in a dummy, so we don't bother searching for it later.
                    this.differentialExpressionResultCache.addToCache( new MissingResult( resultSetId, geneId ) );
                }
                i++;
            }
        }

        if ( timer.getTime() > 10 ) {
            AbstractDao.log.info( "Add " + i + " results to cache: " + timer.getTime() + "ms" );
        }
    }

    /**
     * Get results that are already in the cache.
     */
    private Map<Long, Collection<Long>> fillFromCache( Map<Long, Map<Long, DiffExprGeneSearchResult>> results,
            Collection<Long> resultSetIds, Collection<Long> geneIds ) {

        Map<Long, Collection<Long>> foundInCache = new HashMap<>();

        boolean useCache = true;
        //noinspection ConstantConditions // for debugging ... disable cache.
        if ( !useCache ) {
            AbstractDao.log.warn( "Cache is disabled" );
            return foundInCache;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        int totalFound = 0;
        for ( Long r : resultSetIds ) {

            Collection<DiffExprGeneSearchResult> cached = differentialExpressionResultCache.get( r, geneIds );

            if ( cached.isEmpty() )
                continue;

            foundInCache.put( r, new HashSet<>() );

            for ( DiffExprGeneSearchResult result : cached ) {
                if ( !results.containsKey( r ) ) {
                    results.put( r, new HashMap<>() );
                }

                results.get( r ).put( result.getGeneId(), result );
                foundInCache.get( r ).add( result.getGeneId() );
                totalFound++;
            }

        }

        if ( timer.getTime() > 100 && totalFound > 0 ) {
            AbstractDao.log.info( "Fill " + totalFound + " results from cache: " + timer.getTime() + "ms" );
        }

        return foundInCache;

    }

    /**
     * For any genes for which the result did not meet the threshold, but where the gene was tested, add a dummy value.
     *
     * @return ms taken
     */
    private long fillNonSignificant( Collection<Long> pbL, Map<Long, DiffExResultSetSummaryValueObject> resultSetIds,
            Map<Long, Map<Long, DiffExprGeneSearchResult>> resultsFromDb, Collection<Long> resultSetIdBatch,
            Map<Long, Collection<Long>> cs2GeneIdMap, Session session ) {

        if ( pbL.isEmpty() )
            return 0;
        int d = 0;
        StopWatch t = new StopWatch();
        t.start();
        for ( Long resultSetId : resultSetIdBatch ) {

            /*
             * only include a dummy for probes which are from this result set.
             */
            Collection<Long> arrayDesignIds = resultSetIds.get( resultSetId ).getArrayDesignsUsed();
            Collection<Long> probesForResultSet = CommonQueries.filterProbesByPlatform( pbL, arrayDesignIds, session );

            for ( Long probeId : probesForResultSet ) {
                for ( Long geneId : cs2GeneIdMap.get( probeId ) ) {

                    Map<Long, DiffExprGeneSearchResult> resultsForResultSet = resultsFromDb.get( resultSetId );
                    if ( resultsForResultSet == null || resultsForResultSet.containsKey( geneId ) ) {
                        continue;
                    }

                    DiffExprGeneSearchResult dummy = new NonRetainedResult( resultSetId, geneId );

                    resultsForResultSet.put( geneId, dummy );
                    d++;
                }
            }
        }
        if ( t.getTime() > 100 ) {
            AbstractDao.log.info( "Fill in " + d + " non-significant values: " + t.getTime() + "ms" );
        }
        return t.getTime();
    }

    /**
     * @return map of probe to genes.
     */
    private Map<Long, Collection<Long>> getProbesForGenesInResultSetBatch( Session session, Collection<Long> geneIds,
            Map<Long, DiffExResultSetSummaryValueObject> resultSetIds, Collection<Long> resultSetIdBatch ) {
        Collection<Long> adUsed = new HashSet<>();
        for ( Long rsid : resultSetIdBatch ) {
            assert resultSetIds.containsKey( rsid );
            Collection<Long> arrayDesignsUsed = resultSetIds.get( rsid ).getArrayDesignsUsed();
            assert arrayDesignsUsed != null;
            adUsed.addAll( arrayDesignsUsed );
        }
        return CommonQueries.getCs2GeneIdMap( geneIds, adUsed, session );
    }

    /**
     * @param results map of gene id to result, which the result gets added to.
     * @param resultId the specific DifferentialExpressionAnalysisResult, corresponds to an entry for one probe in the
     *        resultSet
     */
    private void processDiffExResultHit( Map<Long, DiffExprGeneSearchResult> results, Long resultSetId, Long geneId,
            Long resultId, double correctedPvalue, double uncorrectedPvalue ) {
        DiffExprGeneSearchResult r = results.get( geneId );

        if ( r == null ) { // first encounter
            r = new DiffExprGeneSearchResult( resultSetId, geneId );
            r.setResultId( resultId );
            r.setNumberOfProbes( r.getNumberOfProbes() + 1 );

            if ( correctedPvalue <= DifferentialExpressionResultDaoImpl.CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX ) {
                // This check is only useful if we are only storing 'significant' results.
                r.setNumberOfProbesDiffExpressed( r.getNumberOfProbesDiffExpressed() + 1 );
            }
            r.setCorrectedPvalue( correctedPvalue );
            r.setPvalue( uncorrectedPvalue );
            results.put( geneId, r );
        } else if ( r.getCorrectedPvalue() == null || r.getCorrectedPvalue() > correctedPvalue ) {
            // replace with the better value
            r.setResultId( resultId ); // note this changes the hashcode of r.
            r.setCorrectedPvalue( correctedPvalue );
            r.setNumberOfProbes( r.getNumberOfProbes() + 1 );
            r.setPvalue( uncorrectedPvalue );
            r.setNumberOfProbesDiffExpressed( r.getNumberOfProbesDiffExpressed() + 1 );
        }
    }

    /**
     * Identify resultSets that are still need to be queried. Those would be the ones which don't have information for
     * all the genes requested in the cache. Note that those results could be 'dummies' that are represent missing
     * results.
     */
    private Collection<Long> stripUnneededResultSets( Map<Long, Collection<Long>> foundInCache,
            Collection<Long> resultSetIds, Collection<Long> geneIds ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<Long> needToQuery = new HashSet<>();
        for ( Long resultSetId : resultSetIds ) {

            if ( !foundInCache.containsKey( resultSetId ) || !foundInCache.get( resultSetId ).containsAll( geneIds ) ) {
                needToQuery.add( resultSetId );
            }

        }
        if ( timer.getTime() > 1 ) {
            AbstractDao.log.info( "Checking cache results: " + timer.getTime() + "ms; " + needToQuery.size()
                    + " result sets must be queried" );
        }
        return needToQuery;
    }
}