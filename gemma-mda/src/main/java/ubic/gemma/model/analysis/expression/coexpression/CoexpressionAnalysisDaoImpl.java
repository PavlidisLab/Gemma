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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.CoexpressionAnalysis
 */
@Repository
public class CoexpressionAnalysisDaoImpl extends CoexpressionAnalysisDaoBase {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog( CoexpressionAnalysisDaoImpl.class.getName() );

    @Autowired
    public CoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public CoexpCorrelationDistribution getCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment ) {

        String q = "select ccd from CoexpressionAnalysisImpl pca "
                + "join pca.coexpCorrelationDistribution ccd where pca.experimentAnalyzed = :ee";

        return ( CoexpCorrelationDistribution ) this.getSessionFactory().getCurrentSession().createQuery( q )
                .setParameter( "ee", expressionExperiment ).uniqueResult();

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#getExperimentsWithAnalysis(java.util
     * .Collection)
     */
    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        return this
                .getSessionFactory()
                .getCurrentSession()
                .createQuery(
                        "select experimentAnalyzed.id from CoexpressionAnalysisImpl where experimentAnalyzed.id in (:ids)" )
                .setParameterList( "ids", idsToFilter ).list();
    }

    @Override
    protected Collection<CoexpressionAnalysis> handleFindByInvestigation( Investigation investigation ) {
        final String queryString = "select distinct a from CoexpressionAnalysisImpl a where :e = a.experimentAnalyzed";
        return this.getHibernateTemplate().findByNamedParam( queryString, "e", investigation );
    }

    @Override
    protected Map<Investigation, Collection<CoexpressionAnalysis>> handleFindByInvestigations(
            Collection<Investigation> investigations ) {
        Map<Investigation, Collection<CoexpressionAnalysis>> results = new HashMap<Investigation, Collection<CoexpressionAnalysis>>();
        for ( Investigation ee : investigations ) {
            Collection<CoexpressionAnalysis> ae = this.findByInvestigation( ee );
            results.put( ee, ae );
        }
        return results;
    }

    /*
     * FIXME not used and broken/ (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByParentTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection<CoexpressionAnalysis> handleFindByParentTaxon( Taxon taxon ) {
        final String queryString = "select distinct an from CoexpressionAnalysisImpl an"
                + " inner join an.experimentAnalyzed ee " + "inner join ee.bioAssays ba "
                + "inner join ba.sampleUsed sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected Collection<CoexpressionAnalysis> handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select distinct an from CoexpressionAnalysisImpl an"
                + " inner join an.experimentAnalyzed ee " + "inner join ee.bioAssays ba "
                + "inner join ba.sampleUsed sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }
}