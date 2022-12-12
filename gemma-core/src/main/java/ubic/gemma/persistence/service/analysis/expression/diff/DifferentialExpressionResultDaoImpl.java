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
import org.hibernate.type.DoubleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.distribution.Histogram;
import ubic.basecode.util.BatchIterator;
import ubic.basecode.util.SQLUtils;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.TaskCancelledException;

import java.math.BigInteger;
import java.util.*;

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
     * Temporary. For mimicing the effect of storing only 'significant' results.
     */
    private static final Double CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX = 1.0;
    /*
     * This is a key query: get all results for a set of genes in a set of resultssets (basically, experiments)
     */
    private static final String fetchBatchDifferentialExpressionAnalysisResultsByResultSetsAndGeneQuery = "SELECT dear.PROBE_FK, dear.ID,"
            + " dear.RESULT_SET_FK, dear.CORRECTED_PVALUE, dear.PVALUE  "
            + " FROM DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear FORCE INDEX (probeResultSets) WHERE dear.RESULT_SET_FK IN (:rs_ids) AND "
            + " dear.PROBE_FK IN (:probe_ids) ";

    private static final String fetchResultsByExperimentsQuery = "select distinct e, r"
            + " from DifferentialExpressionAnalysis a, BioSequence2GeneProduct bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "left join p.biologicalCharacteristic bs left join bs2gp.geneProduct gp left join gp.gene g"
            + " where bs2gp.bioSequence=bs and e.id in (:experimentsAnalyzed) and r.correctedPvalue < :threshold order by r.correctedPvalue";

    private static final String fetchResultsByGene = "select distinct e, r" + " from DifferentialExpressionAnalysis a, BioSequence2GeneProduct bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join r.probe  p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where g=:gene"; // no order by clause, we add it later

    private static final String fetchResultsByGeneSQL = "select d.ID, d.PVALUE as D_PVALUE, d.CORRECTED_PVALUE, d.PROBE_FK, d.RESULT_SET_FK, "
            + " c.FACTOR_VALUE_FK, c.PVALUE as C_PVALUE, c.LOG_FOLD_CHANGE, c.SECOND_FACTOR_VALUE_FK, c.ID as C_ID "
            + " from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT d, GENE2CS g2s, CONTRAST_RESULT c "
            + " where g2s.CS = d.PROBE_FK and c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK = d.ID and g2s.GENE = :gene_id ";

    private static final String fetchResultsByGeneAndExperimentsQuery = "select distinct e, r"
            + " from DifferentialExpressionAnalysis a, BioSequence2GeneProduct bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and g=:gene and e.id in (:experimentsAnalyzed)"; // no order by clause, we
    // add
    // it later

    private static final String fetchResultsByResultSetAndGeneQuery = "SELECT dear.CORRECTED_PVALUE "
            + " FROM DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear, GENE2CS g2s FORCE KEY(GENE)  "
            + " WHERE g2s.CS = dear.PROBE_FK  AND dear.RESULT_SET_FK = :rs_id AND g2s.GENE = :gene_id "
            + " ORDER BY dear.CORRECTED_P_VALUE_BIN DESC";

    /**
     * No constraint on gene
     */
    private static final String fetchResultsByResultSetQuery = "select distinct rs, r "
            + " from DifferentialExpressionAnalysis a inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner  join  rs.results r inner join fetch r.probe p "
            + " where rs in (:resultsSets)"; // no order by clause, we add it later; 'e' is not used in this query.

    private static final String fetchResultsBySingleResultSetQuery = "select distinct r "
            + " from DifferentialExpressionAnalysis a inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner  join  rs.results r inner join fetch r.probe p "
            + " where rs = :resultSet"; // no order by clause, we add it later; 'e' is not used in this query.

    private final DifferentialExpressionResultCache differentialExpressionResultCache;

    @Autowired
    public DifferentialExpressionResultDaoImpl( SessionFactory sessionFactory,
            DifferentialExpressionResultCache differentialExpressionResultCache ) {
        super( DifferentialExpressionAnalysisResult.class, sessionFactory );
        this.differentialExpressionResultCache = differentialExpressionResultCache;
        // previously: 500
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed, double threshold, int limit ) {

        StopWatch timer = new StopWatch();
        timer.start();
        String qs = DifferentialExpressionResultDaoImpl.fetchResultsByGeneAndExperimentsQuery
                + " and r.correctedPvalue < :threshold";

        if ( limit > 0 ) {
            qs += " order by r.correctedPvalue";
        }

        Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> results = new HashMap<>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        List<?> qResult = getSessionFactory().getCurrentSession()
                .createQuery( qs )
                .setParameter( "gene", gene )
                .setParameterList( "experimentsAnalyzed", experimentsAnalyzed )
                .setParameter( "threshold", threshold )
                .setMaxResults( limit )
                .setCacheable( true )
                .setCacheRegion( "diffExResult" )
                .list();

        for ( Object o : qResult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionExperimentValueObject ee = ( ( BioAssaySet ) oa[0] ).createValueObject();
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionValueObject>() );
            }

            results.get( ee ).add( new DifferentialExpressionValueObject( probeResult ) );
        }

        AbstractDao.log
                .warn( "Num experiments with probe analysis results (with limit = " + limit + ") : " + results.size()
                        + ". Number of probes returned in total: " + qResult.size() );

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        return results;
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find(
            Collection<Long> experiments, double qvalueThreshold, int limit ) {

        Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> results = new HashMap<>();

        if ( experiments.size() == 0 ) {
            return results;
        }

        StopWatch timer = new StopWatch();
        timer.start();

        List<?> qResult = getSessionFactory().getCurrentSession()
                .createQuery( DifferentialExpressionResultDaoImpl.fetchResultsByExperimentsQuery )
                .setParameterList( "experimentsAnalyzed", experiments )
                .setParameter( "threshold", qvalueThreshold )
                .setMaxResults( limit )
                .setCacheable( true )
                .setCacheRegion( "diffExResult" )
                .list();

        for ( Object o : qResult ) {

            Object[] oa = ( Object[] ) o;
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) oa[1];
            ExpressionExperimentValueObject eevo = ( ( BioAssaySet ) oa[0] ).createValueObject();
            if ( !results.containsKey( eevo ) ) {
                results.put( eevo, new ArrayList<DifferentialExpressionValueObject>() );
            }

            results.get( eevo ).add( new DifferentialExpressionValueObject( probeResult ) );
        }

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        return results;
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> results = new HashMap<>();
        if ( gene == null )
            return results;

        List<?> qResult = getSessionFactory().getCurrentSession().createQuery( DifferentialExpressionResultDaoImpl.fetchResultsByGene )
                .setParameter( "gene", gene )
                .setCacheable( true )
                .list();

        for ( Object o : qResult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionExperimentValueObject ee = ( ( BioAssaySet ) oa[0] ).createValueObject();
            DifferentialExpressionValueObject probeResult = new DifferentialExpressionValueObject(
                    ( DifferentialExpressionAnalysisResult ) oa[1] );

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionValueObject>() );
            }

            results.get( ee ).add( probeResult );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return results;
    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            Collection<Long> experimentsAnalyzed ) {

        Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> results = new HashMap<>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        StopWatch timer = new StopWatch();
        timer.start();

        List<?> qResult = this.getSessionFactory().getCurrentSession()
                .createQuery( DifferentialExpressionResultDaoImpl.fetchResultsByGeneAndExperimentsQuery )
                .setParameter( "gene", gene )
                .setParameterList( "experimentsAnalyzed", experimentsAnalyzed )
                .list();

        for ( Object o : qResult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionExperimentValueObject ee = new ExpressionExperimentValueObject( ( ExpressionExperiment ) oa[0] );
            DifferentialExpressionValueObject probeResult = new DifferentialExpressionValueObject(
                    ( DifferentialExpressionAnalysisResult ) oa[1] );
            probeResult.setExpressionExperiment( ee );

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionValueObject>() );
            }

            results.get( ee ).add( probeResult );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return results;
    }

    @Override
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findDiffExAnalysisResultIdsInResultSets(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds ) {

        Map<Long, Map<Long, DiffExprGeneSearchResult>> results = new HashMap<>();

        Session session = this.getSessionFactory().getCurrentSession();

        Map<Long, DiffExResultSetSummaryValueObject> resultSetIdsMap = EntityUtils
                .getPropertyMap( resultSets, "resultSetId" );

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

        org.hibernate.SQLQuery queryObject = session.createSQLQuery(
                DifferentialExpressionResultDaoImpl.fetchBatchDifferentialExpressionAnalysisResultsByResultSetsAndGeneQuery );

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

        final int numResultSetBatches = ( int ) Math.ceil( resultSetsNeeded.size() / resultSetBatchSize );

        queryObject.setFlushMode( FlushMode.MANUAL );

        StopWatch timer = new StopWatch();
        timer.start();
        int numResults = 0;
        long timeForFillingNonSig = 0;

        Map<Long, Map<Long, DiffExprGeneSearchResult>> resultsFromDb = new HashMap<>();

        int numResultSetBatchesDone = 0;

        // Iterate over batches of resultSets
        for ( Collection<Long> resultSetIdBatch : new BatchIterator<>( resultSetsNeeded, resultSetBatchSize ) ) {

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
            final int numGeneBatches = ( int ) Math.ceil( cs2GeneIdMap.size() / geneBatchSize );

            StopWatch innerQt = new StopWatch();

            // iterate over batches of probes (genes)
            for ( Collection<Long> probeBatch : new BatchIterator<>( cs2GeneIdMap.keySet(), geneBatchSize ) ) {

                if ( AbstractDao.log.isDebugEnabled() )
                    AbstractDao.log.debug( "Starting batch of probes: " + StringUtils
                            .abbreviate( StringUtils.join( probeBatch, "," ), 100 ) );

                // would it help to sort the probeBatch/
                List<Long> pbL = new Vector<>( probeBatch );
                Collections.sort( pbL );

                queryObject.setParameterList( "probe_ids", pbL );

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
                    throw new TaskCancelledException( "Search was cancelled" );
                }

                numGeneBatchesDone++;

                if ( DifferentialExpressionResultDaoImpl.CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX < 1.0 ) {
                    timeForFillingNonSig += this
                            .fillNonSignificant( pbL, resultSetIdsMap, resultsFromDb, resultSetIdBatch, cs2GeneIdMap,
                                    session );
                }
            } // over probes.

            // Check if task was cancelled.
            if ( Thread.currentThread().isInterrupted() ) {
                throw new TaskCancelledException( "Search was cancelled" );
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
    public List<Double> findGeneInResultSets( Gene gene, ExpressionAnalysisResultSet resultSet,
            Collection<Long> arrayDesignIds, int limit ) {

        StopWatch timer = new StopWatch();
        timer.start();

        List<Double> results;

        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session
                .createSQLQuery( DifferentialExpressionResultDaoImpl.fetchResultsByResultSetAndGeneQuery );

        queryObject.setLong( "gene_id", gene.getId() );
        queryObject.setLong( "rs_id", resultSet.getId() );

        queryObject.setMaxResults( limit );

        queryObject.addScalar( "CORRECTED_PVALUE", new DoubleType() );
        //noinspection unchecked
        results = queryObject.list();

        timer.stop();
        if ( AbstractDao.log.isDebugEnabled() )
            AbstractDao.log
                    .debug( "Fetching probeResults from resultSet " + resultSet.getId() + " for gene " + gene.getId()
                            + "and " + arrayDesignIds.size() + "arrays took : " + timer.getTime() + " ms" );

        return results;
    }

    @Override
    public List<DifferentialExpressionValueObject> findInResultSet( ExpressionAnalysisResultSet resultSet,
            Double threshold, int limit, int minNumberOfResults ) {

        if ( minNumberOfResults < 1 ) {
            throw new IllegalArgumentException( "Minimum number of results must be greater than one" );
        }

        List<DifferentialExpressionValueObject> results = new ArrayList<>();

        if ( resultSet == null ) {
            return results;
        }

        results = differentialExpressionResultCache.getTopHits( resultSet );
        if ( results != null && results.size() >= minNumberOfResults ) {
            AbstractDao.log.info( "Top hits already in cache" );
            return results;
        }

        results = new ArrayList<>();

        // get it.

        StopWatch timer = new StopWatch();
        timer.start();
        String qs = DifferentialExpressionResultDaoImpl.fetchResultsBySingleResultSetQuery
                + " and r.correctedPvalue <= :threshold order by r.correctedPvalue";

        List<?> qResult = getSessionFactory().getCurrentSession().createQuery( qs )
                .setParameter( "resultSet", resultSet )
                .setParameter( "threshold", threshold )
                .setMaxResults( limit )
                .list();

        // If too few probes meet threshold, redo and just get top results.
        if ( qResult.size() < minNumberOfResults ) {
            AbstractDao.log.info( "No results met threshold, repeating to just get the top hits" );
            qs = DifferentialExpressionResultDaoImpl.fetchResultsBySingleResultSetQuery + " order by r.correctedPvalue";
            qResult = getSessionFactory().getCurrentSession().createQuery( qs )
                    .setParameter( "resultSet", resultSet )
                    .setMaxResults( minNumberOfResults )
                    .list();
        }

        for ( Object o : qResult ) {
            DifferentialExpressionValueObject probeResult = new DifferentialExpressionValueObject(
                    ( DifferentialExpressionAnalysisResult ) o );
            results.add( probeResult );
        }

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        differentialExpressionResultCache.addToTopHitsCache( resultSet, results );

        return results;
    }

    /**
     * Given a list of result sets finds the results that met the given threshold
     *
     * @param limit - max number of results to return.
     */
    @Override
    public Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> findInResultSets(
            Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, int limit ) {

        Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> results = new HashMap<>();

        if ( resultsAnalyzed.size() == 0 ) {
            return results;
        }

        // Integer bin = Math.log10(threshold);

        StopWatch timer = new StopWatch();
        timer.start();
        String qs = DifferentialExpressionResultDaoImpl.fetchResultsByResultSetQuery
                + " and r.correctedPvalue < :threshold order by r.correctedPvalue";

        List<?> qResult = getSessionFactory().getCurrentSession().createQuery( qs )
                .setParameterList( "resultsAnalyzed", resultsAnalyzed )
                .setParameter( "threshold", threshold )
                .setMaxResults( limit )
                .list();

        for ( Object o : qResult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionAnalysisResultSet ee = ( ExpressionAnalysisResultSet ) oa[0];
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return results;
    }

    @Override
    public DifferentialExpressionAnalysis getAnalysis( ExpressionAnalysisResultSet rs ) {
        return ( DifferentialExpressionAnalysis ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct a from DifferentialExpressionAnalysis a join a.resultSets r where r=:r" )
                .setParameter( "r", rs )
                .uniqueResult();
    }

    @Override
    public Collection<ExperimentalFactor> getExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select ef from ExpressionAnalysisResultSet rs"
                        + " inner join rs.results r inner join rs.experimentalFactors ef where r=:differentialExpressionAnalysisResult" )
                .setParameter( "differentialExpressionAnalysisResult", differentialExpressionAnalysisResult ).list();

    }

    @Override
    public Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> factorsByResult = new HashMap<>();
        if ( differentialExpressionAnalysisResults.isEmpty() ) {
            return factorsByResult;
        }

        //language=HQL
        final String queryString = "select rs.experimentalFactors, r from ExpressionAnalysisResultSet rs"
                + " inner join rs.results r where r in (:differentialExpressionAnalysisResults)";

        List<?> qr = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameterList( "differentialExpressionAnalysisResults", differentialExpressionAnalysisResults )
                .list();

        if ( qr == null || qr.isEmpty() )
            return factorsByResult;

        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            ExperimentalFactor f = ( ExperimentalFactor ) ar[0];
            DifferentialExpressionAnalysisResult res = ( DifferentialExpressionAnalysisResult ) ar[1];

            assert differentialExpressionAnalysisResults.contains( res );

            if ( !factorsByResult.containsKey( res ) ) {
                factorsByResult.put( res, new HashSet<ExperimentalFactor>() );
            }

            factorsByResult.get( res ).add( f );

            if ( AbstractDao.log.isDebugEnabled() )
                AbstractDao.log.debug( res );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "factors by results: " + timer.getTime() + " ms" );
        }
        return factorsByResult;

    }

    /**
     * Key method for getting contrasts associated with results.
     */
    @Override
    public Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids ) {
        //language=SQL
        final String queryString = "SELECT DISTINCT c.ID, c.LOG_FOLD_CHANGE, c.FACTOR_VALUE_FK,"
                + " c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK, c.PVALUE FROM CONTRAST_RESULT c"
                + " WHERE c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK IN (:ids)  ";

        Map<Long, ContrastsValueObject> probeResults = new HashMap<>();

        if ( ids.isEmpty() ) {
            return probeResults;
        }

        SQLQuery query = this.getSessionFactory().getCurrentSession().createSQLQuery( queryString );

        int BATCH_SIZE = 2000; // previously: 500, then 1000. New optimized query is plenty fast.
        StopWatch timer = new StopWatch();
        for ( Collection<Long> batch : new BatchIterator<>( ids, BATCH_SIZE ) ) {
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
                        + "ms; query was\n " + queryString.replace( ":ids", StringUtils.join( batch, "," ) ) );
            }
        }

        return probeResults;
    }

    @Override
    public void thaw( final Collection<DifferentialExpressionAnalysisResult> results ) {
        Session session = this.getSessionFactory().getCurrentSession();
        for ( DifferentialExpressionAnalysisResult result : results ) {
            reattach( result );
            Hibernate.initialize( result );
            CompositeSequence cs = result.getProbe();
            Hibernate.initialize( cs );
            Hibernate.initialize( result.getContrasts() );
        }

    }

    @Override
    public void thaw( final DifferentialExpressionAnalysisResult result ) {
        Session session = this.getSessionFactory().getCurrentSession();

        reattach( result );
        Hibernate.initialize( result );

        CompositeSequence cs = result.getProbe();
        Hibernate.initialize( cs );

        Collection<ContrastResult> contrasts = result.getContrasts();
        for ( ContrastResult contrast : contrasts ) {
            FactorValue f = contrast.getFactorValue();
            Hibernate.initialize( f );
            //noinspection ResultOfMethodCallIgnored
            f.getIsBaseline();
        }

    }

    @Override
    public Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> find( Gene gene,
            double threshold, int limit ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Session session = this.getSessionFactory().getCurrentSession();
        String sql = DifferentialExpressionResultDaoImpl.fetchResultsByGeneSQL;

        if ( threshold > 0.0 ) {
            sql = sql + " and d.CORRECTED_PVALUE < :threshold ";
        }

        if ( limit > 0 ) {
            sql = sql + "  order by d.PVALUE ASC ";
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
                    .setParameterList( "ids", probeIds ).list() ) {
                probeNames.put( ( Long ) rec[0], ( String ) rec[1] );
            }
        }

        /*
         * load the result set information.
         */
        if ( resultSets.isEmpty() )
            return results;

        //noinspection unchecked
        List<Object[]> ees = session.createQuery(
                        "select ee, rs from  ExpressionAnalysisResultSet rs join fetch rs.experimentalFactors join rs.analysis a join a.experimentAnalyzed ee"
                                + " where rs.id in (:rsids)" )
                .setParameterList( "rsids", resultSets ).list();

        /*
         * Finish populating the objects
         */
        for ( Object[] oa : ees ) {
            ExpressionAnalysisResultSet rs = ( ExpressionAnalysisResultSet ) oa[1];
            ExpressionExperimentValueObject evo = ( ( BioAssaySet ) oa[0] ).createValueObject();

            if ( !results.containsKey( evo ) ) {
                results.put( evo, new ArrayList<DifferentialExpressionValueObject>() );
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

    @Override
    public void remove( Collection<DifferentialExpressionAnalysisResult> entities ) {
        if ( entities == null || entities.size() < 1 ) return;
        Collection<Long> cIds = new HashSet<>();

        // Read contrast ids and wipe references
        for ( DifferentialExpressionAnalysisResult r : entities ) {
            cIds.addAll( EntityUtils.getIds( r.getContrasts() ) );
            r.setContrasts( new HashSet<ContrastResult>() );
        }

        // Remove contrasts
        if ( cIds.size() > 0 ) {
            AbstractDao.log.info( "Removing contrasts..." );
            this.getSessionFactory().getCurrentSession().createQuery( "delete from ContrastResult e where e.id in (:ids)" )
                    .setParameterList( "ids", cIds )
                    .executeUpdate();

        }

        // Remove results
        AbstractDao.log.info( "Removing DEA results" );
        this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from DifferentialExpressionAnalysisResult e where e.id in (:ids)" )
                .setParameterList( "ids", EntityUtils.getIds( entities ) ).executeUpdate();
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
            resultsFromDb.put( resultSetId, new HashMap<Long, DiffExprGeneSearchResult>() );
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

            foundInCache.put( r, new HashSet<Long>() );

            for ( DiffExprGeneSearchResult result : cached ) {
                if ( !results.containsKey( r ) ) {
                    results.put( r, new HashMap<Long, DiffExprGeneSearchResult>() );
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
    private long fillNonSignificant( List<Long> pbL, Map<Long, DiffExResultSetSummaryValueObject> resultSetIds,
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
            Long resultId, Double correctedPvalue, Double uncorrectedPvalue ) {

        assert correctedPvalue != null;
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