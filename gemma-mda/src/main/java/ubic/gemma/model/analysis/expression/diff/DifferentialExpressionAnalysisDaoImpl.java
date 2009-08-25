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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.CommonQueries;

/**
 * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysis
 * @version $Id$
 * @author paul
 */
public class DifferentialExpressionAnalysisDaoImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase {

    private final String fetchResultsByGeneAndExperimentsQuery = "select e, r"
            + " from DifferentialExpressionAnalysisImpl a, BlatAssociationImpl bs2gp"
            + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e  "
            + " inner join a.resultSets rs inner join rs.results r inner join r.probe p "
            + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " where bs2gp.bioSequence=bs and g=:gene and e in (:experimentsAnalyzed)";

    
    private final String fetchResultsByExperimentsQuery = "select e, r"
        + " from DifferentialExpressionAnalysisImpl a, BlatAssociationImpl bs2gp"
        + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e  "
        + " inner join a.resultSets rs inner join rs.results r inner join r.probe p "
        + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
        + " where bs2gp.bioSequence=bs and e in (:experimentsAnalyzed)";

    private final String fetchResultsByResultSetQuery = "select rs, r"
        + " from DifferentialExpressionAnalysisImpl a, BlatAssociationImpl bs2gp"
        + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e  "
        + " inner join a.resultSets rs inner join rs.results r inner join r.probe p "
        + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
        + " where bs2gp.bioSequence=bs and rs in (:resultsAnalyzed)";

    
    
    private Log log = LogFactory.getLog( this.getClass() );

    @SuppressWarnings("unchecked")
    @Override
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select a from DifferentialExpressionAnalysisImpl as a where a.name = :name", "name", name );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findResultsForGeneInExperiments(ubic
     * .gemma.model.genome.Gene, java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    public Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> findResultsForGeneInExperiments( Gene gene,
            Collection<ExpressionExperiment> experimentsAnalyzed ) {

        Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> results = new HashMap<ExpressionExperiment, Collection<ProbeAnalysisResult>>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        String[] paramNames = { "gene", "experimentsAnalyzed" };
        Object[] objectValues = { gene, experimentsAnalyzed };

        List qresult = this.getHibernateTemplate().findByNamedParam( fetchResultsByGeneAndExperimentsQuery, paramNames,
                objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionExperiment ee = ( ExpressionExperiment ) oa[0];
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new HashSet<ProbeAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }
        log.info( "Num experiments with probe analysis results: " + results.size() );

        return results;
    }

    /**
     * Given a list of result sets finds the results that met the given threshold
     * 
     * @param resultsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ExpressionAnalysisResultSet, java.util.Collection<ProbeAnalysisResult>> findGenesInResultSetsThatMetThreshold(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed,
            double threshold,
            Integer limit) {

        String qs = fetchResultsByResultSetQuery + " and r.correctedPvalue < :threshold";

        
        int oldmax = getHibernateTemplate().getMaxResults();        
        if (limit != null ){
            getHibernateTemplate().setMaxResults( limit );
            qs += " order by r.correctedPvalue";
        }

        
        Map<ExpressionAnalysisResultSet, Collection<ProbeAnalysisResult>> results = new HashMap<ExpressionAnalysisResultSet, Collection<ProbeAnalysisResult>>();

        if ( resultsAnalyzed.size() == 0 ) {
            return results;
        }

        String[] paramNames = { "resultsAnalyzed", "threshold" };
        Object[] objectValues = { resultsAnalyzed, threshold };

        List qresult = this.getHibernateTemplate().findByNamedParam( qs, paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionAnalysisResultSet ee = ( ExpressionAnalysisResultSet ) oa[0];
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new HashSet<ProbeAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }

        if (limit != null ){
            getHibernateTemplate().setMaxResults( oldmax );
        }

        return results;
    }

    
    /*
     * (non-Javadoc)
     * @seeubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#
     * findResultsForGeneInExperimentsMetThreshold(ubic.gemma.model.genome.Gene, java.util.Collection, double)
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> findResultsForGeneInExperimentsMetThreshold(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed,
            double threshold ) {

        final String qs = fetchResultsByGeneAndExperimentsQuery + " and r.correctedPvalue < :threshold";

        Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> results = new HashMap<ExpressionExperiment, Collection<ProbeAnalysisResult>>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        String[] paramNames = { "gene", "experimentsAnalyzed", "threshold" };
        Object[] objectValues = { gene, experimentsAnalyzed, threshold };

        List qresult = this.getHibernateTemplate().findByNamedParam( qs, paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionExperiment ee = ( ExpressionExperiment ) oa[0];
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new HashSet<ProbeAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }
        log.info( "Num experiments with probe analysis results: " + results.size() );

        return results;
    }

    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> findGenesInExperimentsThatMetThreshold(
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed,
            double threshold, Integer limit ) {

        String qs = fetchResultsByExperimentsQuery + " and r.correctedPvalue < :threshold";

        int oldmax = getHibernateTemplate().getMaxResults();        
        if (limit != null ){
            getHibernateTemplate().setMaxResults( limit );
            qs += " order by r.correctedPvalue";
        }

        Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> results = new HashMap<ExpressionExperiment, Collection<ProbeAnalysisResult>>();

        if ( experimentsAnalyzed.size() == 0 ) {
            return results;
        }

        String[] paramNames = { "experimentsAnalyzed", "threshold" };
        Object[] objectValues = { experimentsAnalyzed, threshold };

        List qresult = this.getHibernateTemplate().findByNamedParam( qs, paramNames, objectValues );

        for ( Object o : qresult ) {

            Object[] oa = ( Object[] ) o;
            ExpressionExperiment ee = ( ExpressionExperiment ) oa[0];
            ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) oa[1];

            if ( !results.containsKey( ee ) ) {
                results.put( ee, new HashSet<ProbeAnalysisResult>() );
            }

            results.get( ee ).add( probeResult );
        }

        if (limit != null){
            getHibernateTemplate().setMaxResults( oldmax );
        }

        return results;
    }

    
    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase#handleThaw(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void handleThaw( final Collection expressionAnalyses ) throws Exception {
        for ( DifferentialExpressionAnalysis ea : ( Collection<DifferentialExpressionAnalysis> ) expressionAnalyses ) {
            DifferentialExpressionAnalysis dea = ea;
            thaw( dea );
        }

    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleFind(ubic.gemma.model.genome
     * .Gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFind( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) throws Exception {
        final String findByResultSet = "select distinct r from DifferentialExpressionAnalysisImpl a"
                + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join "
                + "bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene g"
                + " inner join a.resultSets rs inner join rs.results r where r.probe=cs and g=:gene and rs=:resultSet and r.correctedPvalue < :threshold";

        String[] paramNames = { "gene", "resultSet", "threshold" };
        Object[] objectValues = { gene, resultSet, threshold };

        return this.getHibernateTemplate().findByNamedParam( findByResultSet, paramNames, objectValues );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByInvestigation( Investigation investigation )
            throws Exception {
        final String queryString = "select distinct a from DifferentialExpressionAnalysisImpl a where :e in elements (a.expressionExperimentSetAnalyzed.experiments)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "e", investigation );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleFindByInvestigationIds(
     * java.util.Collection)
     */
    @Override
    protected Map<Long, DifferentialExpressionAnalysis> handleFindByInvestigationIds( Collection<Long> investigationIds )
            throws Exception {
        Map<Long, DifferentialExpressionAnalysis> results = new HashMap<Long, DifferentialExpressionAnalysis>();
        final String queryString = "select distinct e, a from DifferentialExpressionAnalysisImpl a"
                + " inner join a.expressionExperimentSetAnalyzed eeSet inner join eeSet.experiments e where e.id in (:eeIds)";
        List qresult = this.getHibernateTemplate().findByNamedParam( queryString, "eeIds", investigationIds );
        for ( Object o : qresult ) {
            Object[] oa = ( Object[] ) o;
            BioAssaySet bas = ( BioAssaySet ) oa[0];
            DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) oa[1];
            results.put( bas.getId(), dea );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByInvestigations(java.util.Collection)
     */
    @Override
    protected Map<Investigation, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigations(
            Collection investigations ) throws Exception {
        Map<Investigation, Collection<DifferentialExpressionAnalysis>> results = new HashMap<Investigation, Collection<DifferentialExpressionAnalysis>>();

        final String queryString = "select distinct e, a from DifferentialExpressionAnalysisImpl a"
                + " inner join a.expressionExperimentSetAnalyzed eeSet inner join eeSet.experiments e where e in (:investigations)";
        List qresult = this.getHibernateTemplate().findByNamedParam( queryString, "investigations", investigations );
        for ( Object o : qresult ) {
            Object[] oa = ( Object[] ) o;
            BioAssaySet bas = ( BioAssaySet ) oa[0];
            DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) oa[1];
            if ( !results.containsKey( bas ) ) {
                Collection<DifferentialExpressionAnalysis> deas = new HashSet<DifferentialExpressionAnalysis>();
                results.put( bas, deas );
            }
            results.get( bas ).add( dea );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select distinct doa from DifferentialExpressionAnalysisImpl as doa inner join doa.expressionExperimentSetAnalyzed eesa inner join eesa.experiments as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleFindExperimentsWithAnalyses
     * (ubic.gemma.model.genome.Gene)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExpressionExperiment> handleFindExperimentsWithAnalyses( Gene gene ) throws Exception {

        Collection<CompositeSequence> probes = CommonQueries.getCompositeSequences( gene, this.getSession() );
        Collection<ExpressionExperiment> result = new HashSet<ExpressionExperiment>();
        if ( probes.size() == 0 ) {
            return result;
        }

        /*
         * The constraint on taxon is required because of the potential for array designs that use sequences from the
         * "wrong" taxon, like GPL560. This way we ensure that we only get expression experiments for the same taxon as
         * the gene.
         */
        final String queryString = "select distinct e from DifferentialExpressionAnalysisImpl a "
                + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e inner join e.bioAssays ba"
                + " inner join ba.samplesUsed sa inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs where cs in (:probes) and sa.sourceTaxon.id = "
                + gene.getTaxon().getId();

        int batchSize = 1000;

        /*
         * If 'probes' is too large, query will fail so we have to batch. Yes, it can happen!
         */

        Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();
        for ( CompositeSequence probe : probes ) {
            batch.add( probe );

            if ( batch.size() == batchSize ) {
                result.addAll( this.getHibernateTemplate().findByNamedParam( queryString, "probes", batch ) );
                batch.clear();
            }

        }

        if ( batch.size() > 0 ) {
            result.addAll( this.getHibernateTemplate().findByNamedParam( queryString, "probes", batch ) );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleGetResultSets(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExpressionAnalysisResultSet> handleGetResultSets( ExpressionExperiment expressionExperiment )
            throws Exception {
        final String query = "select r from ExpressionAnalysisResultSetImpl r inner join r.analysis a"
                + " inner join a.expressionExperimentSetAnalyzed eeset inner join eeset.experiments ee where ee=:expressionExperiment ";
        return this.getHibernateTemplate().findByNamedParam( query, "expressionExperiment", expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleThaw(ubic.gemma.model.analysis
     * .expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    protected void handleThaw( final DifferentialExpressionAnalysis differentialExpressionAnalysis ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();

        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {

            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( differentialExpressionAnalysis, LockMode.NONE );
                Hibernate.initialize( differentialExpressionAnalysis );
                Collection<ExpressionAnalysisResultSet> ears = differentialExpressionAnalysis.getResultSets();
                Hibernate.initialize( ears );
                for ( ExpressionAnalysisResultSet ear : ears ) {
                    session.update( ear );
                    Hibernate.initialize( ear );
                    Collection<DifferentialExpressionAnalysisResult> ders = ear.getResults();
                    Hibernate.initialize( ders );
                    for ( DifferentialExpressionAnalysisResult der : ders ) {
                        session.update( der );
                        Hibernate.initialize( der );
                        if ( der instanceof ProbeAnalysisResult ) {
                            ProbeAnalysisResult par = ( ProbeAnalysisResult ) der;
                            CompositeSequence cs = par.getProbe();
                            // session.update( cs );
                            Hibernate.initialize( cs );
                        }
                    }
                }
                return null;
            }
        } );
    }

    
    public long countProbesMeetingThreshold(ExpressionAnalysisResultSet ears, double threshold){
        
      String query = "select count(r) "
        + " from DifferentialExpressionAnalysisImpl a, BlatAssociationImpl bs2gp"
        + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e  "
        + " inner join a.resultSets rs inner join rs.results r inner join r.probe p "
        + "inner join p.biologicalCharacteristic bs inner join bs2gp.geneProduct gp inner join gp.gene g"
        + " where bs2gp.bioSequence=bs and rs = :resultAnalyzed and r.correctedPvalue < :threshold";
        
      
      String[] paramNames = { "resultAnalyzed", "threshold" };
      Object[] objectValues = { ears, threshold };

      List qresult = this.getHibernateTemplate().findByNamedParam( query, paramNames, objectValues );
      
      Long count = null;
      for ( Object o : qresult ) {

          count = ( Long ) o;
          log.info( "Found " + count + " differentially expressed genes in result set (" + ears.getId() + ") at a threshold of " + threshold );

      }
          return count;
    }
    
    
    
}