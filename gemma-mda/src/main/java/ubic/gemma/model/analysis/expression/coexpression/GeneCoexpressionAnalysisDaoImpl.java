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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.GeneCoexpressionAnalysis
 * @$Id$
 */
@Repository
public class GeneCoexpressionAnalysisDaoImpl extends
        ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDaoBase {

    private static Log log = LogFactory.getLog( GeneCoexpressionAnalysisDaoImpl.class.getName() );

    String[] linkClasses = new String[] { "HumanGeneCoExpressionImpl", "MouseGeneCoExpressionImpl",
            "RatGeneCoExpressionImpl", "OtherGeneCoExpressionImpl" };

    @Autowired
    public GeneCoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Collection<GeneCoexpressionAnalysis> findByName( String name ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select a from GeneCoexpressionAnalysisImpl as a where a.name = :name", "name", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.GeneCoexpressionAnalysisDaoBase#remove(ubic.gemma.model.analysis.GeneCoexpressionAnalysis
     * )
     */
    @Override
    public void remove( final GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        /*
         * Note that we don't worry about taxon here since the foreign key is the analysis. We could have bothered to
         * figure out which taxon and thus which table we need to do.
         */
        for ( String clazz : linkClasses ) {
            String deleteLinkString = "delete from " + clazz + " where sourceAnalysis =?";
            int numdeleted = this.getHibernateTemplate().bulkUpdate( deleteLinkString, geneCoexpressionAnalysis );
            if ( numdeleted > 0 ) {
                log.info( "Deleted " + numdeleted + " gene2gene links" );
                break;
            }
        }

        this.getHibernateTemplate().delete( geneCoexpressionAnalysis );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoImpl#handleFindByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    @Override
    protected Collection<GeneCoexpressionAnalysis> handleFindByInvestigation( Investigation investigation )
            throws Exception {
        final String queryString = "select distinct a from GeneCoexpressionAnalysisImpl a where :e in elements (a.expressionExperimentSetAnalyzed.experiments)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "e", investigation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoImpl#handleFindByInvestigations(java.util.Collection)
     */
    @Override
    protected Map<Investigation, Collection<GeneCoexpressionAnalysis>> handleFindByInvestigations(
            Collection<Investigation> investigations ) {
        Map<Investigation, Collection<GeneCoexpressionAnalysis>> results = new HashMap<Investigation, Collection<GeneCoexpressionAnalysis>>();
        for ( Investigation ee : investigations ) {
            Collection<GeneCoexpressionAnalysis> ae = this.findByInvestigation( ee );
            results.put( ee, ae );
        }
        return results;
    }

    /*
     * * If a taxon is not a species check if it has child taxa and if so retrieve the expression experiments for the
     * child taxa
     */
    @Override
    protected Collection<GeneCoexpressionAnalysis> handleFindByParentTaxon( Taxon taxon ) {
        final String queryStringParent = "select distinct goa from GeneCoexpressionAnalysisImpl as goa inner join goa.expressionExperimentSetAnalyzed"
                + " as eesa inner join eesa.experiments as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon.parentTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryStringParent, "taxon", taxon );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection<GeneCoexpressionAnalysis> handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select goa from GeneCoexpressionAnalysisImpl as goa where goa.taxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected Collection<ExpressionExperiment> handleGetDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) {
        final String queryString = "select e from GeneCoexpressionAnalysisImpl g inner join g.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e where g=:g";
        return getHibernateTemplate().findByNamedParam( queryString, "g", analysis );
    }

    @Override
    protected int handleGetNumDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) {
        final String queryString = "select count(e) from GeneCoexpressionAnalysisImpl g inner join g.expressionExperimentSetAnalyzed eesa inner join eesa.experiments e where g=:g";
        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "g", analysis );
        return ( ( Long ) list.iterator().next() ).intValue();
    }

    @Override
    protected void handleThaw( final GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( geneCoexpressionAnalysis );
                Hibernate.initialize( geneCoexpressionAnalysis );
                Hibernate.initialize( geneCoexpressionAnalysis.getExpressionExperimentSetAnalyzed() );
                return null;
            }
        } );
    }

    @Override
    public Collection<? extends GeneCoexpressionAnalysis> load( Collection<Long> ids ) {
        if ( ids.isEmpty() ) return new HashSet<GeneCoexpressionAnalysis>();
        return this.getHibernateTemplate().findByNamedParam(
                "select a from GeneCoexpressionAnalysisImpl as a where a.id in (:ids)", "ids", ids );
    }

}