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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class DifferentialExpressionResultDaoImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDaoBase {

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
            + " from DifferentialExpressionAnalysisImpl a " + " inner join a.experimentAnalyzed e  "
            + " inner join a.resultSets rs inner  join  rs.results r inner join fetch r.probe p "
            + " where rs in (:resultsAnalyzed)"; // no order by clause, we add it later; 'e' is not used in this query.

    private static final String fetchResultsByResultSetAndGeneQuery = "select dear.CORRECTED_PVALUE "
            + " from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear, GENE2CS g2s FORCE KEY(GENE), PROBE_ANALYSIS_RESULT par "
            + " where g2s.CS = par.PROBE_FK and par.ID = dear.ID and  "
            + " dear.EXPRESSION_ANALYSIS_RESULT_SET_FK = :rs_id and g2s.GENE = :gene_id "
            + " order by dear.CORRECTED_P_VALUE_BIN DESC";

    // private static final String fetchBatchProbeAnalysisResultsByResultSetsAndGeneQuery =
    // "SELECT SQL_NO_CACHE dear.EXPRESSION_ANALYSIS_RESULT_SET_FK, dear.CORRECTED_P_VALUE_BIN, dear.ID"
    // + " from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear, GENE2CS g2s , PROBE_ANALYSIS_RESULT par" //FORCE KEY(GENE)
    // + " where par.ID = dear.ID and g2s.CS = par.PROBE_FK and "
    // + " dear.EXPRESSION_ANALYSIS_RESULT_SET_FK IN (:rs_ids) and "
    // + " g2s.AD in (:ad_ids) and "
    // +
    // " g2s.GENE IN (:gene_ids) GROUP BY dear.EXPRESSION_ANALYSIS_RESULT_SET_FK, dear.CORRECTED_P_VALUE_BIN ORDER BY dear.CORRECTED_P_VALUE_BIN DESC";

    private static final String fetchBatchProbeAnalysisResultsByResultSetsAndGeneQuery = "SELECT g2s.GENE, dear.CORRECTED_P_VALUE_BIN, dear.ID"
            + " from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear, GENE2CS g2s FORCE KEY(GENE), PROBE_ANALYSIS_RESULT par"
            + " where par.ID = dear.ID and g2s.CS = par.PROBE_FK and "
            + " dear.EXPRESSION_ANALYSIS_RESULT_SET_FK = :rs_id and "
            + " g2s.AD in (:ad_ids) and "
            + " g2s.GENE IN (:gene_ids) ";//GROUP BY g2s.GENE, dear.CORRECTED_P_VALUE_BIN ORDER BY dear.CORRECTED_P_VALUE_BIN DESC";

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
    public Map<BioAssaySet, List<ProbeAnalysisResult>> find( Gene gene ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Map<BioAssaySet, List<ProbeAnalysisResult>> results = new HashMap<BioAssaySet, List<ProbeAnalysisResult>>();
        if ( gene == null ) return results;

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        tpl.setCacheQueries( true );

        List<?> qresult = tpl.findByNamedParam( fetchResultsByGene, "gene", gene );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            BioAssaySet ee = ( BioAssaySet ) oa[0];
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<ProbeAnalysisResult>() );
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
    public Map<BioAssaySet, List<ProbeAnalysisResult>> find( Gene gene, Collection<BioAssaySet> experimentsAnalyzed ) {

        Map<BioAssaySet, List<ProbeAnalysisResult>> results = new HashMap<BioAssaySet, List<ProbeAnalysisResult>>();

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
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<ProbeAnalysisResult>() );
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
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            java.util.Collection<ubic.gemma.model.expression.experiment.BioAssaySet> experiments,
            double qvalueThreshold, Integer limit ) {

        Map<BioAssaySet, List<ProbeAnalysisResult>> results = new HashMap<BioAssaySet, List<ProbeAnalysisResult>>();

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
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<ProbeAnalysisResult>() );
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
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
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

        Map<BioAssaySet, List<ProbeAnalysisResult>> results = new HashMap<BioAssaySet, List<ProbeAnalysisResult>>();

        String[] paramNames = { "gene", "threshold" };
        Object[] objectValues = { gene, threshold };

        List<?> qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            BioAssaySet ee = ( BioAssaySet ) oa[0];
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<ProbeAnalysisResult>() );
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
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
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

        Map<BioAssaySet, List<ProbeAnalysisResult>> results = new HashMap<BioAssaySet, List<ProbeAnalysisResult>>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        String[] paramNames = { "gene", "experimentsAnalyzed", "threshold" };
        Object[] objectValues = { gene, experimentsAnalyzed, threshold };

        List<?> qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            BioAssaySet ee = ( BioAssaySet ) oa[0];
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<ProbeAnalysisResult>() );
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

    public List<Double> findGeneInResultSets( Gene gene, ExpressionAnalysisResultSet resultSet,
            Collection<Long> arrayDesignIds, Integer limit ) {

        StopWatch timer = new StopWatch();
        timer.start();

        List<Double> results = null;

        try {
            Session session = super.getSession();
            org.hibernate.SQLQuery queryObject = session.createSQLQuery( fetchResultsByResultSetAndGeneQuery );

            queryObject.setLong( "gene_id", gene.getId() );
            queryObject.setLong( "rs_id", resultSet.getId() );
            // queryObject.setParameterList( "array_ids", arrayDesignIds );
            // queryObject.setLong( "array_ids", arrayDesignIds.iterator().next() );

            if ( limit != null ) {
                queryObject.setMaxResults( limit );
            }

            queryObject.addScalar( "CORRECTED_PVALUE", new DoubleType() );
            results = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        timer.stop();
        if ( log.isDebugEnabled() )
            log.debug( "Fetching probeResults from resultSet " + resultSet.getId() + " for gene " + gene.getId()
                    + "and " + arrayDesignIds.size() + "arrays took : " + timer.getTime() + " ms" );

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
    public java.util.Map<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit ) {

        Map<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>> results = new HashMap<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>>();

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
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new ArrayList<ProbeAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return results;
    }

    public static class DiffExprGeneSearchResult {
        private long probeAnalysisResultId;
        private int numberOfProbes = 0;
        private int numberOfProbesDiffExpressed = 0;
        
        public int getNumberOfProbesDiffExpressed() {
            return numberOfProbesDiffExpressed;
        }
        public void setNumberOfProbesDiffExpressed( int numberOfProbesDiffExpressed ) {
            this.numberOfProbesDiffExpressed = numberOfProbesDiffExpressed;
        }
        public long getProbeAnalysisResultId() {
            return probeAnalysisResultId;
        }
        public void setProbeAnalysisResultId( long probeAnalysisResultId ) {
            this.probeAnalysisResultId = probeAnalysisResultId;
        }
        public int getNumberOfProbes() {
            return numberOfProbes;
        }
        public void setNumberOfProbes( int numberOfProbes ) {
            this.numberOfProbes = numberOfProbes;
        }
    }
    
    public Map<Long, DiffExprGeneSearchResult> findProbeAnalysisResultIdsInResultSet( Long resultSetId, Collection<Long> geneIds,
            Collection<Long> adUsed ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Map<Long,DiffExprGeneSearchResult> results = new HashMap<Long,DiffExprGeneSearchResult>();

        Map<Long, Integer> best_p_value = new HashMap<Long, Integer>();

        Session session = super.getSession();
        try {
            org.hibernate.SQLQuery queryObject = session
                    .createSQLQuery( fetchBatchProbeAnalysisResultsByResultSetsAndGeneQuery );

            queryObject.setLong( "rs_id", resultSetId );
            queryObject.setParameterList( "gene_ids", geneIds );
            queryObject.setParameterList( "ad_ids", adUsed );

            List<?> queryResult = queryObject.list();

            log.warn( "Got " + queryResult.size() + " results" );

            if ( queryResult.isEmpty() ) return results;

            // Get probe result with the best pValue.            
            for ( Object o : queryResult ) {
                Object[] row = ( Object[] ) o;
                BigInteger geneId = ( BigInteger ) row[0];
                Integer p_value_bin = ( Integer ) row[1];
                BigInteger probe_analysis_id = ( BigInteger ) row[2];

                // Count diff expressed probes per gene.
                if (results.get( geneId.longValue() ) != null) {
                    DiffExprGeneSearchResult r = results.get( geneId.longValue() );
                    r.setNumberOfProbes( r.getNumberOfProbes() + 1 );
                    if (p_value_bin != null && p_value_bin > 0) {
                        r.setNumberOfProbesDiffExpressed( r.getNumberOfProbesDiffExpressed() + 1 );                        
                    }                    
                }
                
                if ( best_p_value.get( geneId.longValue() ) == null ) { // first encounter
                    best_p_value.put( geneId.longValue(), p_value_bin );
                    
                    DiffExprGeneSearchResult r =  new DiffExprGeneSearchResult();
                    r.setProbeAnalysisResultId( probe_analysis_id.longValue() );
                    r.setNumberOfProbes( r.getNumberOfProbes() + 1 );
                    if (p_value_bin != null && p_value_bin > 0) {
                        r.setNumberOfProbesDiffExpressed( r.getNumberOfProbesDiffExpressed() + 1 );                        
                    }                    
                    
                    results.put( geneId.longValue(), r );                    
                } else {
                    if ( p_value_bin != null && best_p_value.get( geneId.longValue() ) < p_value_bin) {
                        // replace   
                        best_p_value.put( geneId.longValue(), p_value_bin );
                        
                        DiffExprGeneSearchResult r = results.get( geneId.longValue() );
                        r.setProbeAnalysisResultId( probe_analysis_id.longValue() );
                        //results.put( geneId.longValue(),r );                    
                    }                    
                }                
            }

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        } finally {
            super.releaseSession( session );
        }
        
        timer.stop();
       // if ( log.isDebugEnabled() )
            log.info( "Fetching ProbeResults for geneIds " + StringUtils.join( geneIds, "," ) + " and result set "
                    + resultSetId + " ad used " +  StringUtils.join( adUsed, "," ) + " took : " + timer.getTime() + " ms" );

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase#handleGetExperimentalFactors
     * (java.util.Collection)
     */
    @Override
    protected Map<ProbeAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            Collection<ProbeAnalysisResult> differentialExpressionAnalysisResults ) throws Exception {
        StopWatch timer = new StopWatch();
        timer.start();
        Map<ProbeAnalysisResult, Collection<ExperimentalFactor>> factorsByResult = new HashMap<ProbeAnalysisResult, Collection<ExperimentalFactor>>();
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
            ProbeAnalysisResult res = ( ProbeAnalysisResult ) ar[1];

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
            ProbeAnalysisResult differentialExpressionAnalysisResult ) throws Exception {

        final String queryString = "select ef from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r inner join rs.experimentalFactors ef where r=:differentialExpressionAnalysisResult";

        String[] paramNames = { "differentialExpressionAnalysisResult" };
        Object[] objectValues = { differentialExpressionAnalysisResult };

        return this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDaoBase#load(java.lang.Long)
     */
    public ProbeAnalysisResult load( Long id ) {
        return this.getHibernateTemplate().get( ProbeAnalysisResultImpl.class, id );
    }

    public Collection<ProbeAnalysisResult> loadAll() {
        throw new UnsupportedOperationException( "Sorry, that would be nuts" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#thaw(java.util.Collection)
     */
    public void thaw( final Collection<ProbeAnalysisResult> results ) {
        HibernateTemplate templ = this.getHibernateTemplate();

        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {

            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( ProbeAnalysisResult result : results ) {
                    session.buildLockRequest( LockOptions.NONE ).lock( result );
                    Hibernate.initialize( result );

                    CompositeSequence cs = result.getProbe();
                    Hibernate.initialize( cs );
                }

                return null;
            }
        } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao#thaw(ubic.gemma.model.analysis.expression
     * .diff.ProbeAnalysisResult)
     */
    public void thaw( final ProbeAnalysisResult result ) {
        HibernateTemplate templ = this.getHibernateTemplate();

        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {

            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
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

                return null;
            }
        } );
    }

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

}