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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
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
            + " inner join a.expressionExperimentSetAnalyzed eesa inner join   eesa.experiments e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and g=:gene and e in (:experimentsAnalyzed)"; // no order by clause, we add
    // it later

    private static final String fetchResultsByGene = "select distinct e, r"
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.expressionExperimentSetAnalyzed eesa inner join   eesa.experiments e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe  p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and g=:gene"; // no order by clause, we add it later

    private static final String fetchResultsByExperimentsQuery = "select distinct e, r"
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.expressionExperimentSetAnalyzed eesa inner join   eesa.experiments e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and e in (:experimentsAnalyzed) and r.correctedPvalue < :threshold order by r.correctedPvalue";

    private static final String fetchResultsByResultSetQuery = "select distinct rs, r "
            + " from DifferentialExpressionAnalysisImpl a, BioSequence2GeneProductImpl bs2gp"
            + " inner join a.expressionExperimentSetAnalyzed eesa inner join   eesa.experiments e  "
            + " inner join a.resultSets rs inner join rs.results r inner join fetch r.probe p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and rs in (:resultsAnalyzed)"; // no order by clause, we add it later

    @Autowired
    public DifferentialExpressionResultDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findResultsForGeneInExperiments(ubic
     * .gemma.model.genome.Gene )
     */
    @SuppressWarnings("unchecked")
    public Map<BioAssaySet, List<ProbeAnalysisResult>> find( Gene gene ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Map<BioAssaySet, List<ProbeAnalysisResult>> results = new HashMap<BioAssaySet, List<ProbeAnalysisResult>>();
        if ( gene == null ) return results;

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        tpl.setCacheQueries( true );

        List qresult = tpl.findByNamedParam( fetchResultsByGene, "gene", gene );

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
    @SuppressWarnings("unchecked")
    public Map<BioAssaySet, List<ProbeAnalysisResult>> find( Gene gene, Collection<BioAssaySet> experimentsAnalyzed ) {

        Map<BioAssaySet, List<ProbeAnalysisResult>> results = new HashMap<BioAssaySet, List<ProbeAnalysisResult>>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        StopWatch timer = new StopWatch();
        timer.start();

        String[] paramNames = { "gene", "experimentsAnalyzed" };
        Object[] objectValues = { gene, experimentsAnalyzed };

        List qresult = this.getHibernateTemplate().findByNamedParam( fetchResultsByGeneAndExperimentsQuery, paramNames,
                objectValues );

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

    /**
     * 
     */
    @SuppressWarnings("unchecked")
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

        List qresult = tpl.findByNamedParam( fetchResultsByExperimentsQuery, paramNames, objectValues );

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
    @SuppressWarnings("unchecked")
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

        List qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

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
    @SuppressWarnings("unchecked")
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

        List qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

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

    /**
     * Given a list of result sets finds the results that met the given threshold
     * 
     * @param resultsAnalyzed
     * @param threshold
     * @param limit - max number of results to return.
     * @return
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit ) {

        Map<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>> results = new HashMap<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>>();

        if ( resultsAnalyzed.size() == 0 ) {
            return results;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        String qs = fetchResultsByResultSetQuery + " and r.correctedPvalue < :threshold order by r.correctedPvalue";

        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );

        if ( limit != null ) {
            tpl.setMaxResults( limit );
        }

        String[] paramNames = { "resultsAnalyzed", "threshold" };
        Object[] objectValues = { resultsAnalyzed, threshold };

        List qresult = tpl.findByNamedParam( qs, paramNames, objectValues );

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

    public Collection<ProbeAnalysisResult> loadAll() {
        throw new UnsupportedOperationException( "Sorry, that would be nuts" );
    }

    /**
     * 
     */
    public void thaw( final ProbeAnalysisResult result ) {
        HibernateTemplate templ = this.getHibernateTemplate();

        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {

            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( result, LockMode.NONE );
                Hibernate.initialize( result );

                CompositeSequence cs = result.getProbe();
                Hibernate.initialize( cs );

                return null;
            }
        } );
    }

    public void thaw( final Collection<ProbeAnalysisResult> results ) {
        HibernateTemplate templ = this.getHibernateTemplate();

        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {

            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( ProbeAnalysisResult result : results ) {
                    session.lock( result, LockMode.NONE );
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
     * ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResultDaoBase#handleGetExperimentalFactors
     * (java.util.Collection)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Map handleGetExperimentalFactors( Collection<ProbeAnalysisResult> differentialExpressionAnalysisResults )
            throws Exception {
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

        List qr = this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExperimentalFactor> handleGetExperimentalFactors(
            ProbeAnalysisResult differentialExpressionAnalysisResult ) throws Exception {

        final String queryString = "select ef from ExpressionAnalysisResultSetImpl rs"
                + " inner join rs.results r inner join rs.experimentalFactors ef where r=:differentialExpressionAnalysisResult";

        String[] paramNames = { "differentialExpressionAnalysisResult" };
        Object[] objectValues = { differentialExpressionAnalysisResult };

        return this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );

    }
}