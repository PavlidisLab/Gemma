/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.AbstractDao;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
@Repository
public class GeneDiffExMetaAnalysisDaoImpl extends AbstractDao<GeneDifferentialExpressionMetaAnalysis> implements
        GeneDiffExMetaAnalysisDao {

    @Autowired
    public GeneDiffExMetaAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( GeneDifferentialExpressionMetaAnalysis.class );
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigation( Investigation investigation ) {
        Long id = investigation.getId();
        return findByInvestigationId( id );
    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> findByInvestigationId( Long id ) {
        final String queryString = "select distinct e, a from GeneDifferentialExpressionMetaAnalysisImpl a"
                + "  inner join a.resultSetsIncluded rs inner join rs.analysis ra where ra.experimentAnalyzed.id = :eeId";

        List<GeneDifferentialExpressionMetaAnalysis> qresult = this.getHibernateTemplate().findByNamedParam(
                queryString, "eeId", id );
        return qresult;
    }

    @Override
    public Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        Map<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>> results = new HashMap<Investigation, Collection<GeneDifferentialExpressionMetaAnalysis>>();

        for ( Investigation i : investigations ) {
            results.put( i, this.getAnalyses( i ) );
        }

        return results;

    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByName( String name ) {
        return this.getHibernateTemplate()
                .find( "from GeneDifferentialExpressionMetaAnalysisImpl where name = ?", name );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByParentTaxon( Taxon taxon ) {
        final String queryString = "select distinct e, a from DifferentialExpressionAnalysisImpl a"
                + "   inner join a.resultSetsIncluded rs inner join rs.analysis ra inner join ra.experimentAnalyzed ee inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample "
                + "inner join sample.sourceTaxon as childtaxon where childtaxon.parentTaxon  = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    public Collection<GeneDifferentialExpressionMetaAnalysis> findByTaxon( Taxon taxon ) {
        final String queryString = "select goa from GeneDifferentialExpressionMetaAnalysisImpl as goa where goa.taxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /**
     * @param investigation
     * @return
     */
    private Collection<GeneDifferentialExpressionMetaAnalysis> getAnalyses( Investigation investigation ) {

        Long id = investigation.getId();

        return getAnalysesForExperiment( id );

    }

    private Collection<GeneDifferentialExpressionMetaAnalysis> getAnalysesForExperiment( Long id ) {
        Collection<GeneDifferentialExpressionMetaAnalysis> results = findByInvestigationId( id );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        results.addAll( this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct a from ExpressionExperimentSubSetImpl subset, GeneDifferentialExpressionMetaAnalysisImpl a"
                                + " join subset.sourceExperiment see "
                                + "   inner join a.resultSetsIncluded rs  join rs.analysis ra inner join ra.experimentAnalyzed eeanalyzed where see.id=:ee and subset=eeanalyzed",
                        "ee", id ) );

        return results;
    }

}
