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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.Hibernate;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.analysis.ProbeAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysis
 * @version $Id$
 * @author paul
 */
public class DifferentialExpressionAnalysisDaoImpl extends
        ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase {

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select distinct doa from DifferentialExpressionAnalysisImpl as doa inner join doa.experimentsAnalyzed  as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
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
        final String queryString = "select distinct a from DifferentialExpressionAnalysisImpl a where :e in elements (a.experimentsAnalyzed)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "e", investigation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase#handleThaw(java.util.Collection)
     */
    @Override
    public void handleThaw( final Collection expressionAnalyses ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                Iterator iter = expressionAnalyses.iterator();
                while ( iter.hasNext() ) {
                    ExpressionAnalysis ea = ( ExpressionAnalysis ) iter.next();
                    if ( ea instanceof DifferentialExpressionAnalysis ) {
                        DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) ea;
                        session.update( dea );
                        Hibernate.initialize( dea );
                        Collection<ExpressionAnalysisResultSet> ears = dea.getResultSets();
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

                    }
                }
                return null;
            }
        }, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase#handleFind(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFind( Gene gene ) throws Exception {
        final String queryString = "select distinct e from DifferentialExpressionAnalysisImpl a, BlatAssociationImpl bla"
                + " inner join a.experimentsAnalyzed e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene gene where bla.bioSequence=bs and gene = :gene";
        return this.getHibernateTemplate().findByNamedParam( queryString, "gene", gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase#handleFind(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Collection handleFind( Gene gene, ExpressionExperiment experimentAnalyzed ) throws Exception {
        final String queryString = "select distinct r from DifferentialExpressionAnalysisImpl a"
                + " inner join a.experimentsAnalyzed e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene g"
                + " inner join a.resultSets rs inner join rs.results r where r.probe=cs and g=:gene and e=:experimentAnalyzed";

        String[] paramNames = { "gene", "experimentAnalyzed" };
        Object[] objectValues = { gene, experimentAnalyzed };

        return this.getHibernateTemplate().findByNamedParam( queryString, paramNames, objectValues );
    }
}