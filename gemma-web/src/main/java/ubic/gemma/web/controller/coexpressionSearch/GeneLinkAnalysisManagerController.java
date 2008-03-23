/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.expression.coexpression.CannedAnalysisValueObject;
import ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionVirtualAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * For creating and managing virtual gene link analyses.
 * 
 * @spring.bean id="geneLinkAnalysisManagerController"
 * @spring.property name="geneCoexpressionService" ref="geneCoexpressionService"
 * @spring.property name="geneCoexpressionAnalysisService" ref="geneCoexpressionAnalysisService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "searchService" ref="searchService"
 * @author paul
 * @version $Id$
 */
public class GeneLinkAnalysisManagerController extends BaseFormController {
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;
    private GeneCoexpressionService geneCoexpressionService;
    private ExpressionExperimentService expressionExperimentService;
    private SearchService searchService = null;

    /**
     * Get the set of available canned analyses (for all taxa)
     * 
     * @return
     */
    public Collection<CannedAnalysisValueObject> getCannedAnalyses() {
        return geneCoexpressionService.getCannedAnalyses();
    }

    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentValueObject> getExperimentsInAnalysis( Long id ) {
        GeneCoexpressionAnalysis analysis = ( GeneCoexpressionAnalysis ) geneCoexpressionAnalysisService.load( id );
        Collection<ExpressionExperiment> datasetsAnalyzed = geneCoexpressionAnalysisService
                .getDatasetsAnalyzed( analysis );
        Collection<Long> eeids = new HashSet<Long>();
        for ( ExpressionExperiment ee : datasetsAnalyzed ) {
            eeids.add( ee.getId() );
        }
        return expressionExperimentService.loadValueObjects( eeids );
    }

    @SuppressWarnings("unchecked")
    public void updateExperimentsInAnalysis( Long analysisId, Collection<Long> eeIds ) {
        Analysis a = geneCoexpressionAnalysisService.load( analysisId );
        if ( !( a instanceof GeneCoexpressionVirtualAnalysis ) ) {
            throw new IllegalArgumentException( "'Real' analyses cannot be edited in this way." );
        }
        GeneCoexpressionVirtualAnalysis analysis = ( GeneCoexpressionVirtualAnalysis ) a;
        Collection<ExpressionExperiment> datasetsAnalyzed = expressionExperimentService.loadMultiple( eeIds );
        analysis.getExperimentsAnalyzed().retainAll( datasetsAnalyzed );
        analysis.getExperimentsAnalyzed().addAll( datasetsAnalyzed );
        geneCoexpressionAnalysisService.update( analysis );
    }

    public void create( CannedAnalysisValueObject obj ) {
        // TODO Auto-generated method stub
        // geneCoexpressionAnalysisService.create( analysis );
    }

    public void update( CannedAnalysisValueObject obj ) {
        // TODO Auto-generated method stub
        // geneCoexpressionAnalysisService.update( analysis );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        return new ModelAndView( this.getFormView() );
    }

    /**
     * Find expression experiments among the ones given (by id) that meet the query criteria.
     * 
     * @param query search string, e.g. "age"
     * @param taxonId this is only passed in for convenience.
     * @param toFilter
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<Long> filterExpressionExperiments( String query, Long taxonId, Collection<Long> toFilter ) {
        Collection<Long> results = searchService.searchExpressionExperiments( query, taxonId );
        results.retainAll( toFilter );
        return results;
    }

    /**
     * @param query
     * @param taxonId
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentValueObject> loadExpressionExperiments( Collection<Long> ids ) {
        if ( ids == null || ids.isEmpty() ) {
            return expressionExperimentService.loadAllValueObjects();
        }
        return expressionExperimentService.loadValueObjects( ids );
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setGeneCoexpressionService( GeneCoexpressionService geneCoexpressionService ) {
        this.geneCoexpressionService = geneCoexpressionService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

}
