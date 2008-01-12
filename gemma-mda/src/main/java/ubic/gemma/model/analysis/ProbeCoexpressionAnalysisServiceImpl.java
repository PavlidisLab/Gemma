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
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.ProbeCoexpressionAnalysisService
 * @version $Id$
 * @author paul
 */
public class ProbeCoexpressionAnalysisServiceImpl extends
        ubic.gemma.model.analysis.ProbeCoexpressionAnalysisServiceBase {

    /**
     * @see ubic.gemma.model.analysis.ProbeCoexpressionAnalysisService#create(ubic.gemma.model.analysis.ProbeCoexpressionAnalysis)
     */
    protected ubic.gemma.model.analysis.ProbeCoexpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.ProbeCoexpressionAnalysis probeCoexpressionAnalysis ) throws java.lang.Exception {
        return ( ProbeCoexpressionAnalysis ) this.getProbeCoexpressionAnalysisDao().create( probeCoexpressionAnalysis );
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().findByInvestigations( investigations );
    }

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().findByTaxon( taxon );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ProbeCoexpressionAnalysis handleFindByUniqueInvestigations( Collection investigations ) throws Exception {

        Map<Investigation, Collection<ProbeCoexpressionAnalysis>> anas = this.getProbeCoexpressionAnalysisDao()
                .findByInvestigations( investigations );

        /*
         * Find an analysis that uses all the investigations.
         */

        for ( ExpressionExperiment ee : ( Collection<ExpressionExperiment> ) investigations ) {

            if ( !anas.containsKey( ee ) ) {
                return null; // then there can be none meeting the criterion.
            }

            Collection<ProbeCoexpressionAnalysis> analyses = anas.get( ee );
            for ( ProbeCoexpressionAnalysis a : analyses ) {
                if ( a.getExperimentsAnalyzed().size() == investigations.size()
                        && a.getExperimentsAnalyzed().containsAll( investigations ) ) {
                    return a;
                }
            }
        }

        return null;
    }

}