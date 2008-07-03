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
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
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

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select distinct doa from DifferentialExpressionAnalysisImpl as doa inner join doa.expressionExperimentSetAnalyzed eesa inner join eesa.experiments as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    public Collection /* DifferentialExpressionAnalysis */findByName( String name ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select a from DifferentialExpressionAnalysisImpl as a where a.name = :name", "name", name );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        // I don't know how to do this in a single query.
        Map<Investigation, Collection<DifferentialExpressionAnalysis>> results = new HashMap<Investigation, Collection<DifferentialExpressionAnalysis>>();

        for ( ExpressionExperiment ee : ( Collection<ExpressionExperiment> ) investigations ) {
            Collection<DifferentialExpressionAnalysis> ae = this.findByInvestigation( ee );
            results.put( ee, ae );
        }
        return results;

    }

    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        final String queryString = "select distinct a from DifferentialExpressionAnalysisImpl a where :e in elements (a.expressionExperimentSetAnalyzed.experiments)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "e", investigation );
    }

    /*
     * (non-Javadoc)
     * 
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
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase#handleFind(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindExperimentsWithAnalyses( Gene gene ) throws Exception {

        Collection<CompositeSequence> probes = CommonQueries.getCompositeSequences( gene, this.getSession() );

        final String queryString = "select distinct e from DifferentialExpressionAnalysisImpl a "
                + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs where cs in (:probes)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "probes", probes );
    }

    final String fetchResultsByGeneAndExperimentQuery = "select distinct r from DifferentialExpressionAnalysisImpl a"
            + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
            + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join "
            + "bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene g"
            + " inner join a.resultSets rs inner join rs.results r where r.probe=cs and g=:gene and e=:experimentAnalyzed";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase#handleFind(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Collection handleFind( Gene gene, ExpressionExperiment experimentAnalyzed ) throws Exception {

        String[] paramNames = { "gene", "experimentAnalyzed" };
        Object[] objectValues = { gene, experimentAnalyzed };

        return this.getHibernateTemplate().findByNamedParam( fetchResultsByGeneAndExperimentQuery, paramNames,
                objectValues );
    }

    @Override
    protected Collection handleFind( Gene gene, ExpressionExperiment expressionExperiment, double threshold ) {
        final String queryString = fetchResultsByGeneAndExperimentQuery + " and r.correctedPvalue < :threshold";

        String[] paramNames = { "gene", "experimentAnalyzed", "threshold" };
        Object[] objectValues = { gene, expressionExperiment, threshold };

        return this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );
    }

    @Override
    protected Collection handleFind( Gene gene, ExpressionAnalysisResultSet resultSet, double threshold )
            throws Exception {
        final String findByResultSet = "select distinct r from DifferentialExpressionAnalysisImpl a"
                + " inner join a.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join "
                + "bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene g"
                + " inner join a.resultSets rs inner join rs.results r where r.probe=cs and g=:gene and rs=:resultSet and r.correctedPvalue < :threshold";

        String[] paramNames = { "gene", "resultSet", "threshold" };
        Object[] objectValues = { gene, resultSet, threshold };

        return this.getHibernateTemplate().findByNamedParam( findByResultSet, paramNames, objectValues );
    }

    @Override
    protected Collection handleGetResultSets( ExpressionExperiment expressionExperiment ) throws Exception {
        final String query = "select r from ExpressionAnalysisResultSet r inner join r.analysis a"
                + " inner join a.expressionExperimentSetAnalyzed eeset inner join eeset.experiments ee where ee=:expressionExperiment ";
        return this.getHibernateTemplate().findByNamedParam( query, "expressionExperiment", expressionExperiment );
    }

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

}