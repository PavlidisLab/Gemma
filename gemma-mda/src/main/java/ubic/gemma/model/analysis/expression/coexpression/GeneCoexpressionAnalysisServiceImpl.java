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
import java.util.Map;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.GeneCoexpressionAnalysisService
 * @author paul
 * @version $Id$
 */
public class GeneCoexpressionAnalysisServiceImpl extends
        ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisServiceBase {

    /*
     * @see ubic.gemma.model.analysis.GeneCoexpressionAnalysisService#create(ubic.gemma.model.analysis.GeneCoexpressionAnalysis)
     */
    protected ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis )
            throws java.lang.Exception {
        return ( GeneCoexpressionAnalysis ) this.getGeneCoexpressionAnalysisDao().create( analysis );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisServiceImpl#handleLoadAll()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getGeneCoexpressionAnalysisDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.GeneCoexpressionAnalysisServiceBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().findByTaxon( taxon );
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().findByInvestigations( investigations );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected GeneCoexpressionAnalysis handleFindByUniqueInvestigations( Collection investigations ) throws Exception {

        Map<Investigation, Collection<GeneCoexpressionAnalysis>> anas = this.getGeneCoexpressionAnalysisDao()
                .findByInvestigations( investigations );

        /*
         * Find an analysis that uses all the investigations.
         */

        for ( ExpressionExperiment ee : ( Collection<ExpressionExperiment> ) investigations ) {

            if ( !anas.containsKey( ee ) ) {
                return null; // then there can be none meeting the criterion.
            }

            Collection<GeneCoexpressionAnalysis> analyses = anas.get( ee );
            for ( GeneCoexpressionAnalysis a : analyses ) {
                if ( a.getExperimentsAnalyzed().size() == investigations.size()
                        && a.getExperimentsAnalyzed().containsAll( investigations ) ) {
                    return a;
                }
            }
        }

        return null;
    }

    @Override
    protected void handleUpdate( GeneCoexpressionAnalysis geneCoExpressionAnalysis ) throws Exception {
        this.getGeneCoexpressionAnalysisDao().update( geneCoExpressionAnalysis );
    }

    @Override
    protected int handleGetNumDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().getNumDatasetsAnalyzed( analysis );
    }

    @Override
    protected Collection handleGetDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().getDatasetsAnalyzed( analysis );
    }

    @Override
    protected GeneCoexpressionAnalysis handleFindMostRecentByName( String name ) throws Exception {
        return ( GeneCoexpressionAnalysis ) this.getGeneCoexpressionAnalysisDao().findMostRecentWithName( name );
    }

    @Override
    protected void handleThaw( GeneCoexpressionAnalysis geneCoexpressionAnalysis ) throws Exception {
        this.getGeneCoexpressionAnalysisDao().thaw( geneCoexpressionAnalysis );

    }

}