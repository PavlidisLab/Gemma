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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.DoubleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult
 */
@Repository
public class DifferentialExpressionResultDaoImpl extends DifferentialExpressionResultDaoBase {

    public static class DiffExprGeneSearchResult {
        private long analysisResultId;
        private int numberOfProbes = 0;
        private int numberOfProbesDiffExpressed = 0;

        public long getDifferentialExpressionAnalysisResultId() {
            return analysisResultId;
        }

        public int getNumberOfProbes() {
            return numberOfProbes;
        }

        public int getNumberOfProbesDiffExpressed() {
            return numberOfProbesDiffExpressed;
        }

        public void setDifferentialExpressionAnalysisResultId( long DifferentialExpressionAnalysisResultId ) {
            this.analysisResultId = DifferentialExpressionAnalysisResultId;
        }

        public void setNumberOfProbes( int numberOfProbes ) {
            this.numberOfProbes = numberOfProbes;
        }

        public void setNumberOfProbesDiffExpressed( int numberOfProbesDiffExpressed ) {
            this.numberOfProbesDiffExpressed = numberOfProbesDiffExpressed;
        }
    }

    private Log log = LogFactory.getLog( this.getClass() );
    private static final String fetchResultsByGeneAndExperimentsQuery = "select distinct e, r"
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and g=:gene and e in (:experimentsAnalyzed)"; // no order by clause, we add
    // it later

    private static final String fetchResultsByGene = "select distinct e, r"
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe  p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and g=:gene"; // no order by clause, we add it later

    private static final String fetchResultsByExperimentsQuery = "select distinct e, r"
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "left join p.biologicalCharacteristic bs left join bs2gp.geneProduct gp left join gp.gene g"
            + " where bs2gp.bioSequence=bs and e in (:experimentsAnalyzed) and r.correctedPvalue < :threshold order by r.correctedPvalue";

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

    private static final String fetchResultsByResultSetAndGeneQuery = "select dear.CORRECTED_PVALUE "
            + " from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear, GENE2CS g2s FORCE KEY(GENE)  "
            + " where g2s.CS = dear.PROBE_FK  and dear.EXPRESSION_ANALYSIS_RESULT_SET_FK = :rs_id and g2s.GENE = :gene_id "
            + " order by dear.CORRECTED_P_VALUE_BIN DESC";

    /*
     * This is a key query: get all results for a set of genes in a set of resultssets (basically, experiments)
     */
    private static final String fetchBatchDifferentialExpressionAnalysisResultsByResultSetsAndGeneQuery = "SELECT g2s.GENE, dear.CORRECTED_P_VALUE_BIN, dear.ID,"
            + " dear.EXPRESSION_ANALYSIS_RESULT_SET_FK"
            + " from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear, GENE2CS g2s "
            + " where  g2s.CS = dear.PROBE_FK and dear.EXPRESSION_ANALYSIS_RESULT_SET_FK in (:rs_ids) and "
            + "g2s.AD in (:ad_ids) and  g2s.GENE IN (:gene_ids) "; 

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
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#
     * findDifferentialExpressionAnalysisResultIdsInResultSet(java.lang.Long, java.util.Collection,
     * java.util.Collection)
     */
    @Override
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findDifferentialExpressionAnalysisResultIdsInResultSet(
            Collection<Long> resultSetIds, Collection<Long> geneIds, Collection<Long> adUsed ) {

        Map<Long, Map<Long, DiffExprGeneSearchResult>> results = new HashMap<Long, Map<Long, DiffExprGeneSearchResult>>();

        Map<Long, Map<Long, Integer>> goodPvalue = new HashMap<Long, Map<Long, Integer>>();

        Session session = super.getSession();

        org.hibernate.SQLQuery queryObject = session
                .createSQLQuery( fetchBatchDifferentialExpressionAnalysisResultsByResultSetsAndGeneQuery );
        queryObject.setParameterList( "ad_ids", adUsed );

        int RS_BATCH_SIZE = 100;
        int GENE_BATCH_SIZE = 20;
        // queryObject.setFetchSize( RS_BATCH_SIZE * GENE_BATCH_SIZE );
        queryObject.setReadOnly( true );
        // queryObject.setCacheable( true );
        // queryObject.setCacheRegion( "diffExResultQueryCache" );
        queryObject.setFlushMode( FlushMode.MANUAL );

        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Long> resultSetIdBatch : new BatchIterator<Long>( resultSetIds, RS_BATCH_SIZE ) ) {

            if ( log.isDebugEnabled() )
                log.debug( "Starting batch of resultsets: "
                        + StringUtils.abbreviate( StringUtils.join( resultSetIdBatch, "," ), 100 ) );
            queryObject.setParameterList( "rs_ids", resultSetIdBatch );

            for ( Collection<Long> geneBatch : new BatchIterator<Long>( geneIds, GENE_BATCH_SIZE ) ) {

                if ( log.isDebugEnabled() )
                    log.debug( "Starting batch of genes: "
                            + StringUtils.abbreviate( StringUtils.join( geneBatch, "," ), 100 ) );

                queryObject.setParameterList( "gene_ids", geneBatch );

                List<?> queryResult = queryObject.list();

                // Get probe result with the best pValue, in the give result set.
                for ( Object o : queryResult ) {
                    Object[] row = ( Object[] ) o;
                    Long geneId = ( ( BigInteger ) row[0] ).longValue();
                    Integer pValueBin = ( Integer ) row[1];
                    Long probeAnalysisId = ( ( BigInteger ) row[2] ).longValue();
                    Long resultSetId = ( ( BigInteger ) row[3] ).longValue();

                    if ( !goodPvalue.containsKey( resultSetId ) ) {
                        goodPvalue.put( resultSetId, new HashMap<Long, Integer>() );
                        results.put( resultSetId, new HashMap<Long, DiffExprGeneSearchResult>() );
                    }

                    processDiffExResultHit( results.get( resultSetId ), resultSetId, geneId, probeAnalysisId,
                            goodPvalue.get( resultSetId ), pValueBin );
                }

                if ( timer.getTime() > 1000 ) {
                    log.info( "Fetching DiffEx for batch " + timer.getTime() + " ms : geneIds="
                            + StringUtils.abbreviate( StringUtils.join( geneBatch, "," ), 50 ) + " result set="
                            + StringUtils.abbreviate( StringUtils.join( resultSetIdBatch, "," ), 50 ) + " adused="
                            + StringUtils.abbreviate( StringUtils.join( adUsed, "," ), 50 ) + " took : " );
                    timer.reset();
                    timer.start();
                }

            }

        }

        return results;
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

        Session session = super.getSession();
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
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#loadMultiple(java.util.Collection)
     */
    @Override
    public Map<Long, DifferentialExpressionAnalysisResult> loadMultiple( Collection<Long> ids ) {
        final String queryString = "select dea from DifferentialExpressionAnalysisResultImpl dea where dea.id in (:ids)";

        Map<Long, DifferentialExpressionAnalysisResult> probeResults = new HashMap<Long, DifferentialExpressionAnalysisResult>();

        if ( ids.size() == 0 ) {
            return probeResults;
        }

        int BATCH_SIZE = 100;

        Collection<Long> batch = new HashSet<Long>();

        for ( Long probeResultId : ids ) {
            batch.add( probeResultId );
            if ( batch.size() == BATCH_SIZE ) {
                Collection<DifferentialExpressionAnalysisResult> batchResults = getHibernateTemplate()
                        .findByNamedParam( queryString, "ids", batch );
                for ( DifferentialExpressionAnalysisResult par : batchResults ) {
                    probeResults.put( par.getId(), par );
                }
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            Collection<DifferentialExpressionAnalysisResult> batchResults = getHibernateTemplate().findByNamedParam(
                    queryString, "ids", batch );
            for ( DifferentialExpressionAnalysisResult par : batchResults ) {
                probeResults.put( par.getId(), par );
            }
        }

        return probeResults;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final Collection<DifferentialExpressionAnalysisResult> results ) {
        Session session = this.getSession();
        for ( DifferentialExpressionAnalysisResult result : results ) {
            session.buildLockRequest( LockOptions.NONE ).lock( result );
            Hibernate.initialize( result );
            CompositeSequence cs = result.getProbe();
            Hibernate.initialize( cs );
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
        Session session = this.getSession();

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
        if ( differentialExpressionAnalysisResults.size() == 0 ) {
            return factorsByResult;
        }

        final String queryString = "select ef, r from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r inner join rs.experimentalFactors ef where r in (:differentialExpressionAnalysisResults)";

        String[] paramNames = { "differentialExpressionAnalysisResults" };
        Object[] objectValues = { differentialExpressionAnalysisResults };

        List<?> qr = this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

        if ( qr == null || qr.isEmpty() ) return factorsByResult;

        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            ExperimentalFactor f = ( ExperimentalFactor ) ar[0];
            DifferentialExpressionAnalysisResult res = ( DifferentialExpressionAnalysisResult ) ar[1];

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
     * @param results
     * @param resultSetId
     * @param geneId
     * @param probeAnalysisId
     * @param goodPvalue
     * @param pValueBin
     */
    private void processDiffExResultHit( Map<Long, DiffExprGeneSearchResult> results, Long resultSetId, Long geneId,
            Long probeAnalysisId, Map<Long, Integer> goodPvalue, Integer pValueBin ) {
        // Count diff expressed probes per gene.
        if ( results.get( geneId.longValue() ) != null ) {
            DiffExprGeneSearchResult r = results.get( geneId.longValue() );
            r.setNumberOfProbes( r.getNumberOfProbes() + 1 );
            if ( pValueBin != null && pValueBin > 0 ) {
                r.setNumberOfProbesDiffExpressed( r.getNumberOfProbesDiffExpressed() + 1 );
            }
        }

        if ( goodPvalue.get( geneId.longValue() ) == null ) { // first encounter
            goodPvalue.put( geneId.longValue(), pValueBin );

            DiffExprGeneSearchResult r = new DiffExprGeneSearchResult();
            r.setDifferentialExpressionAnalysisResultId( probeAnalysisId.longValue() );
            r.setNumberOfProbes( r.getNumberOfProbes() + 1 );
            if ( pValueBin != null && pValueBin > 0 ) {
                r.setNumberOfProbesDiffExpressed( r.getNumberOfProbesDiffExpressed() + 1 );
            }

            results.put( geneId.longValue(), r );
        } else {
            // found a better pvalue for the gene in this result set.
            if ( pValueBin != null && goodPvalue.get( geneId.longValue() ) < pValueBin ) {
                // replace
                goodPvalue.put( geneId.longValue(), pValueBin );

                DiffExprGeneSearchResult r = results.get( geneId.longValue() );
                r.setDifferentialExpressionAnalysisResultId( probeAnalysisId.longValue() );
                // results.put( geneId.longValue(), r ); // why was this commented out?
            }
        }
    }

}