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
import java.util.Iterator;
import java.util.Map;

import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.GeneCoexpressionAnalysisService
 * @author paul
 * @version $Id$
 */
@Service
public class GeneCoexpressionAnalysisServiceImpl extends
        ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisService#loadMyAnalyses()
     */
    public Collection<GeneCoexpressionAnalysis> loadMyAnalyses() {
        return loadEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisService#loadMySharedAnalyses()
     */
    public Collection<GeneCoexpressionAnalysis> loadMySharedAnalyses() {
        return loadEnabled();
    }

    /*
     * @see
     * ubic.gemma.model.analysis.GeneCoexpressionAnalysisService#create(ubic.gemma.model.analysis.GeneCoexpressionAnalysis
     * )
     */
    @Override
    protected ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis )
            throws java.lang.Exception {
        return this.getGeneCoexpressionAnalysisDao().create( analysis );
    }

    @Override
    protected void handleDelete( GeneCoexpressionAnalysis toDelete ) throws Exception {
        this.getGeneCoexpressionAnalysisDao().remove( toDelete );
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().findByInvestigations( investigations );
    }

    @Override
    protected Collection handleFindByName( String name ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().findByName( name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.GeneCoexpressionAnalysisServiceBase#handleFindByParentTaxon(ubic.gemma.model.genome
     * .Taxon)
     */
    @Override
    protected Collection handleFindByParentTaxon( Taxon taxon ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().findByParentTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.GeneCoexpressionAnalysisServiceBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().findByTaxon( taxon );
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
                if ( a.getExpressionExperimentSetAnalyzed().getExperiments().size() == investigations.size()
                        && a.getExpressionExperimentSetAnalyzed().getExperiments().containsAll( investigations ) ) {
                    return a;
                }
            }
        }

        return null;
    }

    @Override
    protected Collection handleGetDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().getDatasetsAnalyzed( analysis );
    }

    @Override
    protected int handleGetNumDatasetsAnalyzed( GeneCoexpressionAnalysis analysis ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().getNumDatasetsAnalyzed( analysis );
    }

    @Override
    protected GeneCoexpressionAnalysis handleLoad( Long id ) throws Exception {
        return this.getGeneCoexpressionAnalysisDao().load( id );
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

    @Override
    protected void handleThaw( GeneCoexpressionAnalysis geneCoexpressionAnalysis ) throws Exception {
        this.getGeneCoexpressionAnalysisDao().thaw( geneCoexpressionAnalysis );

    }

    @Override
    protected void handleUpdate( GeneCoexpressionAnalysis geneCoExpressionAnalysis ) throws Exception {
        this.getGeneCoexpressionAnalysisDao().update( geneCoExpressionAnalysis );
    }

    /**
     * @return
     */
    private Collection<GeneCoexpressionAnalysis> loadEnabled() {
        Collection<GeneCoexpressionAnalysis> all = ( Collection<GeneCoexpressionAnalysis> ) this
                .getGeneCoexpressionAnalysisDao().loadAll();

        for ( Iterator<GeneCoexpressionAnalysis> it = all.iterator(); it.hasNext(); ) {
            if ( !it.next().getEnabled() ) {
                it.remove();
            }
        }

        return all;
    }

    @Override
    public GeneCoexpressionAnalysis findCurrent( Taxon taxon ) {
        Collection<GeneCoexpressionAnalysis> analyses = null;
        if ( taxon.getIsSpecies() ) {
            analyses = findByTaxon( taxon );
        } else {
            analyses = findByParentTaxon( taxon );
        }

        if ( analyses.size() == 0 ) {
            return null;
        }

        for ( GeneCoexpressionAnalysis a : analyses ) {
            if ( a.getEnabled() ) {
                return a;
            }
        }

        return null;

    }

}