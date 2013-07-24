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
package ubic.gemma.model.analysis.expression.diff;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.DoubleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.distribution.Histogram;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.CommonQueries;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.NativeQueryUtils;
import ubic.gemma.util.TaskCancelledException;

import java.math.BigInteger;
import java.util.*;

/**
 * This is a key class for queries to retrieve differential expression results (as well as standard CRUD aspects of
 * working with DifferentialExpressionResults).
 * 
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult
 */
@Repository
public class DifferentialExpressionResultDaoImpl extends DifferentialExpressionResultDaoBase {

    /*
     * Temporary. For mimicing the effect of storing only 'significant' results.
     */
    private static final Double CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX = 1.0;
    /*
     * This is a key query: get all results for a set of genes in a set of resultssets (basically, experiments)
     */
    private static final String fetchBatchDifferentialExpressionAnalysisResultsByResultSetsAndGeneQuery = "SELECT dear.PROBE_FK, dear.ID,"
            + " dear.RESULT_SET_FK, dear.CORRECTED_PVALUE, dear.PVALUE  "
            + " from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear where dear.RESULT_SET_FK in (:rs_ids) and "
            + " dear.PROBE_FK IN (:probe_ids) ";

    private static final String fetchResultsByExperimentsQuery = "select distinct e, r"
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "left join p.biologicalCharacteristic bs left join bs2gp.geneProduct gp left join gp.gene g"
            + " where bs2gp.bioSequence=bs and e in (:experimentsAnalyzed) and r.correctedPvalue < :threshold order by r.correctedPvalue";

    private static final String fetchResultsByGene = "select distinct e, r"
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe  p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and g=:gene"; // no order by clause, we add it later

    private static final String fetchResultsByGeneAndExperimentsQuery = "select distinct e, r"
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and g=:gene and e in (:experimentsAnalyzed)"; // no order by clause, we add
    // it later

    private static final String fetchResultsByResultSetAndGeneQuery = "select dear.CORRECTED_PVALUE "
            + " from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear, GENE2CS g2s FORCE KEY(GENE)  "
            + " where g2s.CS = dear.PROBE_FK  and dear.RESULT_SET_FK = :rs_id and g2s.GENE = :gene_id "
            + " order by dear.CORRECTED_P_VALUE_BIN DESC";

    /**
     * No constraint on gene
     */
    private static final String fetchResultsByResultSetQuery = "select distinct rs, r "
            + " from DifferentialExpressionAnalysisImpl a inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner  join  rs.results r inner join fetch r.probe p "
            + " where rs in (:resultsAnalyzed)"; // no order by clause, we add it later; 'e' is not used in this query.

    private static final String fetchResultsBySingleResultSetQuery = "select distinct r "
            + " from DifferentialExpressionAnalysisImpl a inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner  join  rs.results r inner join fetch r.probe p "
            + " where rs in (:resultsAnalyzed)"; // no order by clause, we add it later; 'e' is not used in this query.

    @Autowired
    private DifferentialExpressionResultCache differentialExpressionResultCache;

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    public DifferentialExpressionResultDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#countNumberOfDifferentiallyExpressedProbes
     * (long, double)
     */
    @Override
    public Integer countNumberOfDifferentiallyExpressedProbes( long resultSetId, double threshold ) {
        DetachedCriteria criteria = DetachedCriteria.forClass( HitListSize.class );

        criteria.add( Restrictions.eq( "id", resultSetId ) );
        criteria.add( Restrictions.eq( "thresholdQValue", threshold ) );

        List<?> results = this.getHibernateTemplate().findByCriteria( criteria );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + HitListSize.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
                return ( ( HitListSize ) result ).getNumberOfProbes();
            }
            return 0;
        }
        return 0;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findResultsForGeneInExperiments(ubic
     * .gemma.model.genome.Gene )
     */
    @Override
    public Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> find( Gene gene ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = new HashMap<BioAssaySet, List<DifferentialExpressionAnalysisResult>>();
        if ( gene == null ) return results;

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        tpl.setCacheQueries( true );

        List<?> qresult = tpl.findByNamedParam( fetchResultsByGene, "gene", gene );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            BioAssaySet ee = ( BioAssaySet ) oa[0];
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findResultsForGeneInExperiments(ubic
     * .gemma.model.genome.Gene, java.util.Collection)
     */
    @Override
    public Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> find( Gene gene,
            Collection<BioAssaySet> experimentsAnalyzed ) {

        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = new HashMap<BioAssaySet, List<DifferentialExpressionAnalysisResult>>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        StopWatch timer = new StopWatch();
        timer.start();

        String[] paramNames = { "gene", "experimentsAnalyzed" };
        Object[] objectValues = { gene, experimentsAnalyzed };

        List<?> qresult = this.getHibernateTemplate().findByNamedParam( fetchResultsByGeneAndExperimentsQuery,
                paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionExperiment ee = ( ExpressionExperiment ) oa[0];
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#find(java.util.Collection, double,
     * java.lang.Integer)
     */
    @Override
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<DifferentialExpressionAnalysisResult>> find(
            java.util.Collection<ubic.gemma.model.expression.experiment.BioAssaySet> experiments,
            double qvalueThreshold, Integer limit ) {

        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = new HashMap<BioAssaySet, List<DifferentialExpressionAnalysisResult>>();

        if ( experiments.size() == 0 ) {
            return results;
        }

        StopWatch timer = new StopWatch();
        timer.start();

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        tpl.setQueryCacheRegion( "diffExResult" );
        tpl.setCacheQueries( true );

        if ( limit != null ) {
            tpl.setMaxResults( limit );
        }

        String[] paramNames = { "experimentsAnalyzed", "threshold" };
        Object[] objectValues = { experiments, qvalueThreshold };

        List<?> qresult = tpl.findByNamedParam( fetchResultsByExperimentsQuery, paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            BioAssaySet ee = ( BioAssaySet ) oa[0];
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#
     * findResultsForGeneInExperimentsMetThreshold(ubic.gemma.model.genome.Gene, double, integer)
     */
    @Override
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<DifferentialExpressionAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene, double threshold, Integer limit ) {

        StopWatch timer = new StopWatch();
        timer.start();
        String qs = fetchResultsByGene;

        if ( threshold > 0 ) qs = qs + " and r.correctedPvalue < :threshold";

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        tpl.setQueryCacheRegion( "diffExResult" );
        tpl.setCacheQueries( true );

        if ( limit != null ) {
            tpl.setMaxResults( limit );
            qs += " order by r.correctedPvalue";
        }

        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = new HashMap<BioAssaySet, List<DifferentialExpressionAnalysisResult>>();

        String[] paramNames = { "gene", "threshold" };
        Object[] objectValues = { gene, threshold };

        List<?> qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            BioAssaySet ee = ( BioAssaySet ) oa[0];
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }

        log.debug( "Num experiments with probe analysis results (with limit = " + limit + ") : " + results.size()
                + ". Number of probes returned in total: " + qresult.size() );

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#
     * findResultsForGeneInExperimentsMetThreshold(ubic.gemma.model.genome.Gene, java.util.Collection, double, Integer)
     */
    @Override
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<DifferentialExpressionAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.BioAssaySet> experimentsAnalyzed,
            double threshold, Integer limit ) {

        StopWatch timer = new StopWatch();
        timer.start();
        String qs = fetchResultsByGeneAndExperimentsQuery + " and r.correctedPvalue < :threshold";

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        tpl.setQueryCacheRegion( "diffExResult" );
        tpl.setCacheQueries( true );

        if ( limit != null ) {
            tpl.setMaxResults( limit );
            qs += " order by r.correctedPvalue";
        }

        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = new HashMap<BioAssaySet, List<DifferentialExpressionAnalysisResult>>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        String[] paramNames = { "gene", "experimentsAnalyzed", "threshold" };
        Object[] objectValues = { gene, experimentsAnalyzed, threshold };

        List<?> qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            BioAssaySet ee = ( BioAssaySet ) oa[0];
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<DifferentialExpressionAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }

        log.warn( "Num experiments with probe analysis results (with limit = " + limit + ") : " + results.size()
                + ". Number of probes returned in total: " + qresult.size() );

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        return results;
    }

    /*
     * Key method for getting diff ex results 'in bulk' (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#
     * findDifferentialExpressionAnalysisResultIdsInResultSet(java.lang.Long, java.util.Collection,
     * java.util.Collection)
     */
    @Override
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findDifferentialExpressionAnalysisResultIdsInResultSet(
            Map<ExpressionAnalysisResultSet, Collection<Long>> resultSetIdsToArrayDesignsUsed, Collection<Long> geneIds ) {

        Map<Long, Map<Long, DiffExprGeneSearchResult>> results = new HashMap<Long, Map<Long, DiffExprGeneSearchResult>>();

        Session session = super.getSessionFactory().getCurrentSession();

        Map<Long, ExpressionAnalysisResultSet> resultSetIds = EntityUtils.getIdMap( resultSetIdsToArrayDesignsUsed
                .keySet() );
        Map<Long, Collection<Long>> foundInCache = fillFromCache( results, resultSetIds.keySet(), geneIds );

        if ( !foundInCache.isEmpty() ) {
            log.info( "Results for " + foundInCache.size() + " resultsets found in cache" );
        } else {
            log.info( "No results were in the cache" );
        }

        Collection<Long> resultSetsNeeded = stripUnneededResultSets( foundInCache, resultSetIds.keySet(), geneIds );

        // Are we finished?
        if ( resultSetsNeeded.isEmpty() ) {
            log.info( "All results were in the cache." );
            return results;
        }

        log.info( foundInCache.size() + "/" + resultSetIds.size()
                + " resultsSets had at least some cached results; still need to query " + resultSetsNeeded.size() );

        assert !resultSetsNeeded.isEmpty();

        org.hibernate.SQLQuery queryObject = session
                .createSQLQuery( fetchBatchDifferentialExpressionAnalysisResultsByResultSetsAndGeneQuery );

        /*
         * These values have been tweaked to probe for performance issues.
         */
        int resultSetBatchSize = 50;
        int geneBatchSize = 50;

        if ( resultSetsNeeded.size() > geneIds.size() ) {
            resultSetBatchSize = Math.min( 500, resultSetsNeeded.size() );
            log.info( "Batching by result sets (" + resultSetsNeeded.size() + " resultSets); " + geneIds.size()
                    + " genes; batch size=" + resultSetBatchSize );

        } else {
            geneBatchSize = Math.min( 200, geneIds.size() );
            log.info( "Batching by genes (" + geneIds.size() + " genes); " + resultSetsNeeded.size()
                    + " resultSets; batch size=" + geneBatchSize );
        }

        final int numResultSetBatches = ( int ) Math.ceil( resultSetsNeeded.size() / resultSetBatchSize );

        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        StopWatch timer = new StopWatch();
        timer.start();
        int numResults = 0;
        long timeForFillingNonSig = 0;

        Map<Long, Map<Long, DiffExprGeneSearchResult>> resultsFromDb = new HashMap<Long, Map<Long, DiffExprGeneSearchResult>>();

        int numResultSetBatchesDone = 0;

        // Iterate over batches of resultSets
        for ( Collection<Long> resultSetIdBatch : new BatchIterator<Long>( resultSetsNeeded, resultSetBatchSize ) ) {

            if ( log.isDebugEnabled() )
                log.debug( "Starting batch of resultsets: "
                        + StringUtils.abbreviate( StringUtils.join( resultSetIdBatch, "," ), 100 ) );

            /*
             * Get the probes using the CommonQueries gene2cs. Otherwise we (in effect) end up doing this over and over
             * again.
             */
            Map<Long, Collection<Long>> cs2GeneIdMap = getProbesForGenesInResultSetBatch( session, geneIds,
                    resultSetIds, resultSetIdsToArrayDesignsUsed, resultSetIdBatch );

            queryObject.setParameterList( "rs_ids", resultSetIdBatch );

            int numGeneBatchesDone = 0;
            final int numGeneBatches = ( int ) Math.ceil( cs2GeneIdMap.size() / geneBatchSize );

            StopWatch innerQt = new StopWatch();

            // iterate over batches of probes (genes)
            for ( Collection<Long> probeBatch : new BatchIterator<Long>( cs2GeneIdMap.keySet(), geneBatchSize ) ) {

                if ( log.isDebugEnabled() )
                    log.debug( "Starting batch of probes: "
                            + StringUtils.abbreviate( StringUtils.join( probeBatch, "," ), 100 ) );

                // would it help to sort the probeBatch/
                List<Long> pbL = new Vector<Long>( probeBatch );
                Collections.sort( pbL );

                queryObject.setParameterList( "probe_ids", pbL );

                innerQt.start();
                List<?> queryResult = queryObject.list();
                innerQt.stop();

                if ( innerQt.getTime() > 2000 ) {
                    // show the actual query with params.
                    log.info( "Query time: "
                            + innerQt.getTime()
                            + "ms:\n "
                            + queryObject.getQueryString().replace( ":probe_ids", StringUtils.join( probeBatch, "," ) )
                                    .replace( ":rs_ids", StringUtils.join( resultSetIdBatch, "," ) ) );
                }
                innerQt.reset();

                /*
                 * Each query tuple are the probe, result, resultsSet, qvalue, pvalue.
                 */
                for ( Object o : queryResult ) {
                    numResults += processResultTuple( o, resultsFromDb, cs2GeneIdMap );
                }

                if ( timer.getTime() > 5000 && log.isInfoEnabled() ) {
                    log.info( "Batch time: " + timer.getTime() + "ms; Fetched DiffEx " + numResults
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

                if ( CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX < 1.0 ) {
                    timeForFillingNonSig += fillNonSignificant( pbL, resultSetIds, resultSetIdsToArrayDesignsUsed,
                            resultsFromDb, resultSetIdBatch, cs2GeneIdMap, session );
                }
            }// over probes.

            // Check if task was cancelled.
            if ( Thread.currentThread().isInterrupted() ) {
                throw new TaskCancelledException( "Search was cancelled" );
            }

            numResultSetBatchesDone++;

        }

        if ( timer.getTime() > 1000 && log.isInfoEnabled() ) {
            log.info( "Fetching DiffEx from DB took total of " + timer.getTime() + " ms : geneIds="
                    + StringUtils.abbreviate( StringUtils.join( geneIds, "," ), 50 ) + " result set="
                    + StringUtils.abbreviate( StringUtils.join( resultSetsNeeded, "," ), 50 ) );
            if ( timeForFillingNonSig > 100 ) {
                log.info( "Filling in non-significant values: " + timeForFillingNonSig + "ms in total" );
            }
        }

        // Add the DB results to the cached results.
        addToCache( resultsFromDb, resultSetsNeeded, geneIds );

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

    /**
     * @param resultRow
     * @param resultsFromDb
     * @param cs2GeneIdMap
     * @param numResults
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
         * This is a hack to mimic the effect of storing only 'good' results. FIXME this can be deleted if we store only
         * 'good' results.
         */
        if ( correctedPvalue > CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX ) {
            return 0;
        }

        for ( Long geneId : cs2GeneIdMap.get( probeId ) ) {
            /*
             * FIXME We might want to skip probes that have more than one gene.
             */
            processDiffExResultHit( resultsFromDb.get( resultSetId ), resultSetId, geneId, resultId, correctedPvalue,
                    pvalue );
        }

        if ( log.isDebugEnabled() )
            log.debug( "resultset=" + resultSetId + " probe=" + probeId + " qval="
                    + String.format( "%.2g", correctedPvalue ) );

        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#findGeneInResultSets(ubic.gemma.model
     * .genome.Gene, ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet, java.util.Collection,
     * java.lang.Integer)
     */
    @Override
    public List<Double> findGeneInResultSets( Gene gene, ExpressionAnalysisResultSet resultSet,
            Collection<Long> arrayDesignIds, Integer limit ) {

        StopWatch timer = new StopWatch();
        timer.start();

        List<Double> results = null;

        Session session = super.getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( fetchResultsByResultSetAndGeneQuery );

        queryObject.setLong( "gene_id", gene.getId() );
        queryObject.setLong( "rs_id", resultSet.getId() );

        if ( limit != null ) {
            queryObject.setMaxResults( limit );
        }

        queryObject.addScalar( "CORRECTED_PVALUE", new DoubleType() );
        results = queryObject.list();

        timer.stop();
        if ( log.isDebugEnabled() )
            log.debug( "Fetching probeResults from resultSet " + resultSet.getId() + " for gene " + gene.getId()
                    + "and " + arrayDesignIds.size() + "arrays took : " + timer.getTime() + " ms" );

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#findInResultSet(ubic.gemma.model.analysis
     * .expression.diff.ExpressionAnalysisResultSet, java.lang.Double, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<DifferentialExpressionAnalysisResult> findInResultSet( ExpressionAnalysisResultSet resultSet,
            Double threshold, Integer limit, Integer minNumberOfResults ) {

        if ( minNumberOfResults == null ) {
            throw new IllegalArgumentException( "Minimum number of results cannot be null" );
        }

        List<DifferentialExpressionAnalysisResult> results = new ArrayList<DifferentialExpressionAnalysisResult>();

        if ( resultSet == null ) {
            return results;
        }

        results = differentialExpressionResultCache.getTopHits( resultSet );
        if ( results != null && results.size() >= minNumberOfResults ) {
            log.info( "Top hits already in cache" );
            return results;
        }

        results = new ArrayList<DifferentialExpressionAnalysisResult>();

        // get it.

        Collection<ExpressionAnalysisResultSet> resultsAnalyzed = new ArrayList<ExpressionAnalysisResultSet>();
        resultsAnalyzed.add( resultSet );

        StopWatch timer = new StopWatch();
        timer.start();
        String qs = fetchResultsBySingleResultSetQuery
                + " and r.correctedPvalue < :threshold order by r.correctedPvalue";

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );

        if ( limit != null ) {
            tpl.setMaxResults( limit );
        }

        String[] paramNames = { "resultsAnalyzed", "threshold" };
        Object[] objectValues = { resultsAnalyzed, threshold };

        List<?> qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

        // If too few probes meet threshold, redo and just get top minNumberOfResults.
        if ( qresult.size() < minNumberOfResults ) {

            // FIXME why not just do it this way, in the first place.
            log.info( "No results met threshold, repeating to just get the top hits" );
            qs = fetchResultsBySingleResultSetQuery + " order by r.correctedPvalue";

            tpl = new HibernateTemplate( this.getSessionFactory() );
            tpl.setMaxResults( minNumberOfResults );

            String[] paramName = { "resultsAnalyzed" };
            Object[] objectValue = { resultsAnalyzed };

            qresult = tpl.findByNamedParam( qs, paramName, objectValue );
        }

        for ( Object o : qresult ) {
            DifferentialExpressionAnalysisResult probeResult = ( DifferentialExpressionAnalysisResult ) o;
            results.add( probeResult );
        }

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }

        differentialExpressionResultCache.addToTopHitsCache( resultSet, results );

        return results;
    }

    /**
     * Given a list of result sets finds the results that met the given threshold
     * 
     * @param resultsAnalyzed
     * @param threshold
     * @param limit - max number of results to return.
     * @return
     */
    @Override
    public Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> findInResultSets(
            Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit ) {

        Map<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>> results = new HashMap<ExpressionAnalysisResultSet, List<DifferentialExpressionAnalysisResult>>();

        if ( resultsAnalyzed.size() == 0 ) {
            return results;
        }

        // Integer bin = Math.log10(threshold);

        StopWatch timer = new StopWatch();
        timer.start();
        String qs = fetchResultsByResultSetQuery + " and r.correctedPvalue < :threshold order by r.correctedPvalue";

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );

        if ( limit != null ) {
            tpl.setMaxResults( limit );
        }

        String[] paramNames = { "resultsAnalyzed", "threshold" };
        Object[] objectValues = { resultsAnalyzed, threshold };

        List<?> qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

        for ( Object o : qresult ) {

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
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return results;
    }

    @Override
    public DifferentialExpressionAnalysis getAnalysis( ExpressionAnalysisResultSet rs ) {
        return ( DifferentialExpressionAnalysis ) this
                .getHibernateTemplate()
                .findByNamedParam( "select a from DifferentialExpressionAnalysisImpl a join a.resultSets r where r=:r",
                        "r", rs ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#loadMultiple(java.util.Collection)
     */
    @Override
    public Collection<? extends DifferentialExpressionAnalysisResult> load( Collection<Long> ids ) {
        final String queryString = "from DifferentialExpressionAnalysisResultImpl dea where dea.id in (:ids)";

        Collection<? extends DifferentialExpressionAnalysisResult> probeResults = new HashSet<DifferentialExpressionAnalysisResult>();

        if ( ids.isEmpty() ) {
            return probeResults;
        }

        int BATCH_SIZE = 1000; // previously: 500.

        for ( Collection<Long> batch : new BatchIterator<Long>( ids, BATCH_SIZE ) ) {
            StopWatch timer = new StopWatch();
            timer.start();
            probeResults.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
            if ( timer.getTime() > 1000 ) {
                log.info( "Fetch " + batch.size() + "/" + ids.size() + " results with contrasts: " + timer.getTime()
                        + "ms; query was\n " + NativeQueryUtils.toSql( getHibernateTemplate(), queryString ) );
            }
        }

        return probeResults;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDaoBase#load(java.lang.Long)
     */
    @Override
    public DifferentialExpressionAnalysisResult load( Long id ) {
        return this.getHibernateTemplate().get( DifferentialExpressionAnalysisResultImpl.class, id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#loadAll()
     */
    @Override
    public Collection<DifferentialExpressionAnalysisResult> loadAll() {
        throw new UnsupportedOperationException( "Sorry, that would be nuts" );
    }

    /*
     * Key method for getting contrasts associated with results.
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#loadEagerContrasts(java.util.Collection
     * )
     */
    @Override
    public Map<Long, ContrastsValueObject> loadContrastDetailsForResults( Collection<Long> ids ) {
        // this is too slow.
        // final String queryString =
        // "from DifferentialExpressionAnalysisResultImpl dea left join fetch dea.contrasts where dea.id in (:ids)";

        final String queryString = "SELECT DISTINCT c.ID, c.LOG_FOLD_CHANGE, c.FACTOR_VALUE_FK, c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK, c.PVALUE from CONTRAST_RESULT c"
                + " WHERE c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK IN (:ids)  ";

        Map<Long, ContrastsValueObject> probeResults = new HashMap<Long, ContrastsValueObject>();

        if ( ids.isEmpty() ) {
            return probeResults;
        }

        SQLQuery query = this.getSessionFactory().getCurrentSession().createSQLQuery( queryString );

        int BATCH_SIZE = 2000; // previously: 500, then 1000. New optimized query is plenty fast.
        StopWatch timer = new StopWatch();
        for ( Collection<Long> batch : new BatchIterator<Long>( ids, BATCH_SIZE ) ) {
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

                cvo.addContrast( contrastId, factorValueId, logFoldChange, pvalue );

            }

            if ( timer.getTime() > 2000 ) {
                log.info( "Fetch " + batch.size() + " results with contrasts: " + timer.getTime() + "ms; query was\n "
                        + queryString.replace( ":ids", StringUtils.join( batch, "," ) ) );
            }
        }

        return probeResults;
    }

    @Override
    public Histogram loadPvalueDistribution( Long resultSetId ) {

        List<?> pvds = this.getHibernateTemplate().findByNamedParam(
                "select rs.pvalueDistribution from ExpressionAnalysisResultSetImpl rs where rs.id=:rsid ", "rsid",
                resultSetId );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final Collection<DifferentialExpressionAnalysisResult> results ) {
        Session session = this.getSessionFactory().getCurrentSession();
        for ( DifferentialExpressionAnalysisResult result : results ) {
            session.buildLockRequest( LockOptions.NONE ).lock( result );
            Hibernate.initialize( result );
            CompositeSequence cs = result.getProbe();
            Hibernate.initialize( cs );
            Hibernate.initialize( result.getContrasts() );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#thaw(ubic.gemma.model.analysis.expression
     * .diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    public void thaw( final DifferentialExpressionAnalysisResult result ) {
        Session session = this.getSessionFactory().getCurrentSession();

        session.buildLockRequest( LockOptions.NONE ).lock( result );
        Hibernate.initialize( result );

        CompositeSequence cs = result.getProbe();
        Hibernate.initialize( cs );

        Collection<ContrastResult> contrasts = result.getContrasts();
        for ( ContrastResult contrast : contrasts ) {
            FactorValue f = contrast.getFactorValue();
            Hibernate.initialize( f );
            f.getIsBaseline();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase#handleGetExperimentalFactors
     * (java.util.Collection)
     */
    @Override
    protected Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults ) throws Exception {
        StopWatch timer = new StopWatch();
        timer.start();
        Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> factorsByResult = new HashMap<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>>();
        if ( differentialExpressionAnalysisResults.isEmpty() ) {
            return factorsByResult;
        }

        final String queryString = "select rs.experimentalFactors, r from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r where r in (:differentialExpressionAnalysisResults)";

        String[] paramNames = { "differentialExpressionAnalysisResults" };
        Object[] objectValues = { differentialExpressionAnalysisResults };

        List<?> qr = this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

        if ( qr == null || qr.isEmpty() ) return factorsByResult;

        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            ExperimentalFactor f = ( ExperimentalFactor ) ar[0];
            DifferentialExpressionAnalysisResult res = ( DifferentialExpressionAnalysisResult ) ar[1];

            assert differentialExpressionAnalysisResults.contains( res );

            if ( !factorsByResult.containsKey( res ) ) {
                factorsByResult.put( res, new HashSet<ExperimentalFactor>() );
            }

            factorsByResult.get( res ).add( f );

            if ( log.isDebugEnabled() ) log.debug( res );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "factors by results: " + timer.getTime() + " ms" );
        }
        return factorsByResult;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase#handleGetExperimentalFactors
     * (ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult)
     */
    @Override
    protected Collection<ExperimentalFactor> handleGetExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) throws Exception {

        final String queryString = "select ef from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r inner join rs.experimentalFactors ef where r=:differentialExpressionAnalysisResult";

        String[] paramNames = { "differentialExpressionAnalysisResult" };
        Object[] objectValues = { differentialExpressionAnalysisResult };

        return this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

    }

    /**
     * This is to be called after a query for diff ex results is finished for a set of resultSets and genes. It assumes
     * that if a gene is missing from the results, there are none for that resultSet for that gene. It then stores a
     * dummy entry.
     * 
     * @param results - which might be empty.
     * @param resultSetids - the ones which we searched for in the database. Put in dummy results if we have to.
     * @param geneIds2
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
            log.info( "Add " + i + " results to cache: " + timer.getTime() + "ms" );
        }
    }

    /**
     * Get results that are already in the cache.
     * 
     * @param resultSetIds
     * @param geneIds
     * @return
     */
    private Map<Long, Collection<Long>> fillFromCache( Map<Long, Map<Long, DiffExprGeneSearchResult>> results,
            Collection<Long> resultSetIds, Collection<Long> geneIds ) {

        Map<Long, Collection<Long>> foundInCache = new HashMap<Long, Collection<Long>>();

        // for debugging ... disable cache.
        boolean useCache = true;
        if ( !useCache ) {
            log.warn( "Cache is disabled" );
            return foundInCache;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        int totalFound = 0;
        for ( Long r : resultSetIds ) {

            Collection<DiffExprGeneSearchResult> cached = differentialExpressionResultCache.get( r, geneIds );

            if ( cached.isEmpty() ) continue;

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
            log.info( "Fill " + totalFound + " results from cache: " + timer.getTime() + "ms" );
        }

        return foundInCache;

    }

    /**
     * For any genes for which the result did not meet the threshold, but where the gene was tested, add a dummy value.
     * 
     * @param pbL
     * @param resultSetIds
     * @param resultSetIdsToArrayDesignsUsed
     * @param resultsFromDb
     * @param resultSetIdBatch
     * @param cs2GeneIdMap
     * @param session
     * @return ms taken
     */
    private long fillNonSignificant( List<Long> pbL, Map<Long, ExpressionAnalysisResultSet> resultSetIds,
            Map<ExpressionAnalysisResultSet, Collection<Long>> resultSetIdsToArrayDesignsUsed,
            Map<Long, Map<Long, DiffExprGeneSearchResult>> resultsFromDb, Collection<Long> resultSetIdBatch,
            Map<Long, Collection<Long>> cs2GeneIdMap, Session session ) {

        if ( pbL.isEmpty() ) return 0;
        int d = 0;
        StopWatch t = new StopWatch();
        t.start();
        for ( Long resultSetId : resultSetIdBatch ) {

            /*
             * only include a dummy for probes which are from this result set.
             */
            Collection<Long> arrayDesignIds = resultSetIdsToArrayDesignsUsed.get( resultSetIds.get( resultSetId ) );

            // FIXME SLOW?
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
        if ( t.getTime() > 200 ) {
            log.info( "Fill in " + d + " non-significant values: " + t.getTime() + "ms" );
        }
        return t.getTime();
    }

    /**
     * @param session
     * @param geneIds
     * @param resultSetIds
     * @param resultSetIdsToArrayDesignsUsed
     * @param resultSetIdBatch
     * @return map of probe to genes.
     */
    private Map<Long, Collection<Long>> getProbesForGenesInResultSetBatch( Session session, Collection<Long> geneIds,
            Map<Long, ExpressionAnalysisResultSet> resultSetIds,
            Map<ExpressionAnalysisResultSet, Collection<Long>> resultSetIdsToArrayDesignsUsed,
            Collection<Long> resultSetIdBatch ) {
        Collection<Long> adUsed = new HashSet<Long>();
        for ( Long rsid : resultSetIdBatch ) {
            adUsed.addAll( resultSetIdsToArrayDesignsUsed.get( resultSetIds.get( rsid ) ) );
        }
        Map<Long, Collection<Long>> cs2GeneIdMap = CommonQueries.getCs2GeneIdMap( geneIds, adUsed, session );
        return cs2GeneIdMap;
    }

    /**
     * @param results map of gene id to result, which the result gets added to.
     * @param resultSetId
     * @param geneId
     * @param resultId the specific DiffernetialExpressionAnalysisResult, corresponds to an entry for one probe in the
     *        resultset
     * @param correctedPvalue
     * @param uncorrectedPvalue
     */
    private void processDiffExResultHit( Map<Long, DiffExprGeneSearchResult> results, Long resultSetId, Long geneId,
            Long resultId, Double correctedPvalue, Double uncorrectedPvalue ) {

        assert correctedPvalue != null;
        DiffExprGeneSearchResult r = results.get( geneId );

        if ( r == null ) { // first encounter
            r = new DiffExprGeneSearchResult( resultSetId, geneId );
            r.setResultId( resultId );
            r.setNumberOfProbes( r.getNumberOfProbes() + 1 );

            if ( correctedPvalue <= CORRECTED_PVALUE_THRESHOLD_TO_BE_CONSIDERED_DIFF_EX ) {
                // This check is only useful if we are only storing 'significant' results.
                r.setNumberOfProbesDiffExpressed( r.getNumberOfProbesDiffExpressed() + 1 );
            }
            r.setCorrectedPvalue( correctedPvalue );
            r.setPvalue( uncorrectedPvalue );
            results.put( geneId, r );
        } else if ( r.getCorrectedPvalue() == null || r.getCorrectedPvalue() > correctedPvalue ) {
            // replace with the better value FIXME we might consider this too anticonservative.
            r.setResultId( resultId.longValue() ); // note this changes the hashcode of r.
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
     * 
     * @param foundInCache
     * @param resultSetIds
     * @param geneIds
     * @return
     */
    private Collection<Long> stripUnneededResultSets( Map<Long, Collection<Long>> foundInCache,
            Collection<Long> resultSetIds, Collection<Long> geneIds ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<Long> needToQuery = new HashSet<Long>();
        for ( Long resultSetId : resultSetIds ) {

            if ( !foundInCache.containsKey( resultSetId ) || !foundInCache.get( resultSetId ).containsAll( geneIds ) ) {
                needToQuery.add( resultSetId );
            }

        }
        if ( timer.getTime() > 1 ) {
            log.info( "Checking cache results: " + timer.getTime() + "ms; " + needToQuery.size()
                    + " result sets must be queried" );
        }
        return needToQuery;
    }

}