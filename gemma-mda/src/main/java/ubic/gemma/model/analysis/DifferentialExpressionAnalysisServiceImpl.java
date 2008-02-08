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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @author keshav
 * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisService
 * @version $Id$
 */
public class DifferentialExpressionAnalysisServiceImpl extends
        ubic.gemma.model.analysis.DifferentialExpressionAnalysisServiceBase {

    /**
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisService#create(ubic.gemma.model.analysis.DifferentialExpressionAnalysis)
     */
    protected ubic.gemma.model.analysis.DifferentialExpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.DifferentialExpressionAnalysis analysis ) throws java.lang.Exception {
        return ( DifferentialExpressionAnalysis ) this.getDifferentialExpressionAnalysisDao().create( analysis );
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigations( investigations );
    }

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByTaxon( taxon );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DifferentialExpressionAnalysis handleFindByUniqueInvestigations( Collection investigations )
            throws Exception {

        Map<Investigation, Collection<DifferentialExpressionAnalysis>> anas = this
                .getDifferentialExpressionAnalysisDao().findByInvestigations( investigations );

        /*
         * Find an analysis that uses all the investigations.
         */

        for ( ExpressionExperiment ee : ( Collection<ExpressionExperiment> ) investigations ) {

            if ( !anas.containsKey( ee ) ) {
                return null; // then there can be none meeting the criterion.
            }

            Collection<DifferentialExpressionAnalysis> analyses = anas.get( ee );
            for ( DifferentialExpressionAnalysis a : analyses ) {
                if ( a.getExperimentsAnalyzed().size() == investigations.size()
                        && a.getExperimentsAnalyzed().containsAll( investigations ) ) {
                    return a;
                }
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisServiceBase#handleThaw(java.util.Collection)
     */
    @Override
    protected void handleThaw( Collection expressionAnalyses ) throws Exception {
        this.getDifferentialExpressionAnalysisDao().thaw( expressionAnalyses );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisServiceBase#handleFind(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFind( Gene gene ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().find( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisServiceBase#handleFind(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Collection handleFind( Gene gene, ExpressionExperiment experimentAnalyzed ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().find( gene, experimentAnalyzed );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisServiceBase#handleDelete(java.lang.Long)
     */
    @Override
    protected void handleDelete( Long idToDelete ) throws Exception {
        this.getDifferentialExpressionAnalysisDao().remove( idToDelete );
    }

}