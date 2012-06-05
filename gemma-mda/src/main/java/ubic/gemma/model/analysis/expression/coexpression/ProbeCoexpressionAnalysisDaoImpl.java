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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.ProbeCoexpressionAnalysis
 */
@Repository
public class ProbeCoexpressionAnalysisDaoImpl extends
        ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDaoBase {

    private static Log log = LogFactory.getLog( ProbeCoexpressionAnalysisDaoImpl.class.getName() );

    @Autowired
    public ProbeCoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Collection<CompositeSequence> getAssayedProbes( ExpressionExperiment experiment ) {
        Collection<ProbeCoexpressionAnalysis> analyses = this.findByInvestigation( experiment );

        if ( analyses.size() == 0 ) {
            log.warn( "No analyses available for " + experiment );
            return new HashSet<CompositeSequence>();
        }

        final String queryString = "select distinct c from  ProbeCoexpressionAnalysisImpl poa inner join poa.probesUsed c where poa in (:analyses)";

        return this.getHibernateTemplate().findByNamedParam( queryString, "analyses", analyses );

    }

    @Override
    protected Collection<ProbeCoexpressionAnalysis> handleFindByInvestigation( Investigation investigation )
            throws Exception {
        final String queryString = "select distinct a from ProbeCoexpressionAnalysisImpl a where :e = a.experimentAnalyzed";
        return this.getHibernateTemplate().findByNamedParam( queryString, "e", investigation );
    }

    @Override
    protected Map<Investigation, Collection<ProbeCoexpressionAnalysis>> handleFindByInvestigations(
            Collection<Investigation> investigations ) {
        Map<Investigation, Collection<ProbeCoexpressionAnalysis>> results = new HashMap<Investigation, Collection<ProbeCoexpressionAnalysis>>();
        for ( Investigation ee : investigations ) {
            Collection<ProbeCoexpressionAnalysis> ae = this.findByInvestigation( ee );
            results.put( ee, ae );
        }
        return results;
    }

    @Override
    protected Collection<ProbeCoexpressionAnalysis> handleFindByParentTaxon( Taxon taxon ) {
        final String queryString = "select distinct poa from ProbeCoexpressionAnalysisImpl as"
                + " p  inner join as.experimentAnalyzed  as ee " + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected Collection<ProbeCoexpressionAnalysis> handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select distinct poa from ProbeCoexpressionAnalysisImpl as"
                + " inner join as.experimentAnalyzed  as ee " + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }
}