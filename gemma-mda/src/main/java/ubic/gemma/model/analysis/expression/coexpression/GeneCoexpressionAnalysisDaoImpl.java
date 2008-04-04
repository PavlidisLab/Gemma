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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.GeneCoexpressionAnalysis
 * @$Id$
 */
public class GeneCoexpressionAnalysisDaoImpl extends
        ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDaoBase {

    private static Log log = LogFactory.getLog( GeneCoexpressionAnalysisDaoImpl.class.getName() );

    String[] linkClasses = new String[] { "HumanGeneCoExpressionImpl", "MouseGeneCoExpressionImpl",
            "RatGeneCoExpressionImpl", "OtherGeneCoExpressionImpl" };

    @Override
    public Collection /* analyses */findByName( String name ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select a from GeneCoexpressionAnalysisImpl as a where a.name = :name", "name", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.GeneCoexpressionAnalysisDaoBase#remove(ubic.gemma.model.analysis.GeneCoexpressionAnalysis)
     */
    @Override
    public void remove( final GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {

        for ( String clazz : linkClasses ) {
            // delete the links first.
            String deleteLinkString = "delete link from " + clazz + " link inner join link.sourceAnalysis a where a=?";
            int numdeleted = this.getHibernateTemplate().bulkUpdate( deleteLinkString, geneCoexpressionAnalysis );
            if ( numdeleted > 0 ) {
                log.info( "Deleted " + numdeleted + " gene2gene links" );
                break;
            }
        }

        this.remove( geneCoexpressionAnalysis );

    }

    @Override
    protected void handleThaw( final GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( geneCoexpressionAnalysis, LockMode.NONE );
                Hibernate.initialize( geneCoexpressionAnalysis );
                Hibernate.initialize( geneCoexpressionAnalysis.getExperimentsAnalyzed() );
                return null;
            }
        } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoImpl#handleFindByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        final String queryString = "select distinct a from GeneCoexpressionAnalysisImpl a where :e in elements (a.experimentsAnalyzed)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "e", investigation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoImpl#handleFindByInvestigations(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        Map<Investigation, Collection<GeneCoexpressionAnalysis>> results = new HashMap<Investigation, Collection<GeneCoexpressionAnalysis>>();
        for ( ExpressionExperiment ee : ( Collection<ExpressionExperiment> ) investigations ) {
            Collection<GeneCoexpressionAnalysis> ae = this.findByInvestigation( ee );
            results.put( ee, ae );
        }
        return results;
    }

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select distinct goa from GeneCoexpressionAnalysisImpl as goa inner join goa.experimentsAnalyzed  as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected int handleGetNumDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) throws Exception {
        final String queryString = "select count(e) from GeneCoexpressionAnalysisImpl g inner join g.experimentsAnalyzed e where g=:g";
        List list = getHibernateTemplate().findByNamedParam( queryString, "g", analysis );
        return ( ( Long ) list.iterator().next() ).intValue();
    }

    @Override
    protected Collection handleGetDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) throws Exception {
        final String queryString = "select e from GeneCoexpressionAnalysisImpl g inner join g.experimentsAnalyzed e where g=:g";
        return getHibernateTemplate().findByNamedParam( queryString, "g", analysis );
    }

}