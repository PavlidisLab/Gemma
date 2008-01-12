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
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.ProbeCoexpressionAnalysis
 */
public class ProbeCoexpressionAnalysisDaoImpl extends ubic.gemma.model.analysis.ProbeCoexpressionAnalysisDaoBase {

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select distinct poa from ProbeCoexpressionAnalysisImpl as poa inner join poa.experimentsAnalyzed  as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        Map<Investigation, Collection<ProbeCoexpressionAnalysis>> results = new HashMap<Investigation, Collection<ProbeCoexpressionAnalysis>>();
        for ( ExpressionExperiment ee : ( Collection<ExpressionExperiment> ) investigations ) {
            Collection<ProbeCoexpressionAnalysis> ae = this.findByInvestigation( ee );
            results.put( ee, ae );
        }
        return results;
    }

    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        final String queryString = "select distinct a from ProbeCoexpressionAnalysisImpl a where :e in elements (a.experimentsAnalyzed)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "e", investigation );
    }
}