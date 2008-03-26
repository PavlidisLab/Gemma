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

import org.apache.commons.lang.StringUtils;
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
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.search.SearchService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * For creating and managing virtual gene link analyses.
 * 
 * @spring.bean id="geneLinkAnalysisManagerController"
 * @spring.property name="geneCoexpressionService" ref="geneCoexpressionService"
 * @spring.property name="geneCoexpressionAnalysisService" ref="geneCoexpressionAnalysisService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name = "searchService" ref="searchService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @author paul
 * @version $Id$
 */
public class GeneLinkAnalysisManagerController extends BaseFormController {
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;
    private GeneCoexpressionService geneCoexpressionService;
    private ExpressionExperimentService expressionExperimentService;
    private TaxonService taxonService;
    private PersisterHelper persisterHelper;
    private SearchService searchService = null;

    /**
     * @param obj
     * @return the id of the newwly created analysis object.
     */
    @SuppressWarnings("unchecked")
    public Long create( CannedAnalysisValueObject obj ) {

        if ( obj.getId() != null ) {
            throw new IllegalArgumentException( "Should not provide an id for 'create': " + obj.getId() );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        Analysis existing = geneCoexpressionAnalysisService.findByName( obj.getName() );
        if ( existing != null ) {
            throw new IllegalArgumentException( "There is already an analysis with the name '" + obj.getName() + "'" );
        }

        Long viewedAnalysisId = obj.getViewedAnalysisId();

        if ( viewedAnalysisId == null ) {
            throw new IllegalArgumentException(
                    "Can only create views of existing analyses; provide the ID of an existing one" );
        }

        GeneCoexpressionAnalysis analysis = ( GeneCoexpressionAnalysis ) geneCoexpressionAnalysisService
                .load( viewedAnalysisId );
        Collection<ExpressionExperiment> datasetsInViewed = geneCoexpressionAnalysisService
                .getDatasetsAnalyzed( analysis );

        GeneCoexpressionVirtualAnalysis va = GeneCoexpressionVirtualAnalysis.Factory.newInstance();
        va.setName( obj.getName() );
        va.setDescription( obj.getDescription() );
        va.setStringency( obj.getStringency() );
        va.setViewedAnalysis( analysis );
        va.setTaxon( taxonService.load( obj.getTaxonId() ) );
        Collection<ExpressionExperiment> datasetsAnalyzed = expressionExperimentService
                .loadMultiple( obj.getDatasets() );

        if ( !datasetsInViewed.containsAll( datasetsAnalyzed ) ) {
            throw new IllegalArgumentException(
                    "Some of the datasets in the new virtual analysis aren't in the original" );
        }

        va.setExperimentsAnalyzed( new HashSet<ExpressionExperiment>( datasetsAnalyzed ) );

        GeneCoexpressionVirtualAnalysis newAnalysis = ( GeneCoexpressionVirtualAnalysis ) persisterHelper.persist( va );
        return newAnalysis.getId();
    }

    /**
     * @param obj
     */
    @SuppressWarnings("unchecked")
    public void update( CannedAnalysisValueObject obj ) {

        if ( obj.getId() == null ) {
            throw new IllegalArgumentException( "Can only update an existing analysis (passed id=" + obj.getId() + ")" );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        Analysis existing = geneCoexpressionAnalysisService.findByName( obj.getName() );
        if ( existing != null && !existing.getId().equals( obj.getId() ) ) {
            throw new IllegalArgumentException( "There is already another analysis with the name '" + obj.getName()
                    + "'" );
        }

        GeneCoexpressionAnalysis toUpdate = ( GeneCoexpressionAnalysis ) geneCoexpressionAnalysisService.load( obj
                .getId() );

        if ( !( toUpdate instanceof GeneCoexpressionVirtualAnalysis ) ) {
            throw new IllegalArgumentException( "Can only edit 'virtual' analyses" );
        }

        GeneCoexpressionVirtualAnalysis toUpdateV = ( GeneCoexpressionVirtualAnalysis ) toUpdate;
        geneCoexpressionAnalysisService.thaw( toUpdateV );
        toUpdateV.setName( obj.getName() );
        toUpdateV.setDescription( obj.getDescription() );

        /*
         * TODO:Ensure we aren't adding data sets which are not in the viewed.
         */

        Collection<ExpressionExperiment> datasetsAnalyzed = expressionExperimentService
                .loadMultiple( obj.getDatasets() );
        toUpdateV.getExperimentsAnalyzed().retainAll( datasetsAnalyzed );
        toUpdateV.getExperimentsAnalyzed().addAll( datasetsAnalyzed );

        geneCoexpressionAnalysisService.update( toUpdateV );

        log.info( "Updated " + obj.getName() );

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
     * Get the set of available canned analyses (for all taxa)
     * 
     * @return
     */
    public Collection<CannedAnalysisValueObject> getCannedAnalyses() {
        return geneCoexpressionService.getCannedAnalyses( true, true );
    }

    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentValueObject> getExperimentsInAnalysis( Long id ) {
        Collection<Long> eeids = getExperimentIdsInAnalysis( id );
        return expressionExperimentService.loadValueObjects( eeids );
    }

    /**
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<Long> getExperimentIdsInAnalysis( Long id ) {
        GeneCoexpressionAnalysis analysis = ( GeneCoexpressionAnalysis ) geneCoexpressionAnalysisService.load( id );
        Collection<ExpressionExperiment> datasetsAnalyzed = geneCoexpressionAnalysisService
                .getDatasetsAnalyzed( analysis );
        Collection<Long> eeids = new HashSet<Long>();
        for ( ExpressionExperiment ee : datasetsAnalyzed ) {
            eeids.add( ee.getId() );
        }
        return eeids;
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

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setGeneCoexpressionService( GeneCoexpressionService geneCoexpressionService ) {
        this.geneCoexpressionService = geneCoexpressionService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    @SuppressWarnings("unchecked")
    public void updateExperimentsInAnalysis( Long analysisId, Collection<Long> eeIds ) {
        Analysis a = geneCoexpressionAnalysisService.load( analysisId );
        if ( !( a instanceof GeneCoexpressionVirtualAnalysis ) ) {
            throw new IllegalArgumentException( "'Real' analyses cannot be edited in this way." );
        }
        GeneCoexpressionVirtualAnalysis analysis = ( GeneCoexpressionVirtualAnalysis ) a;
        geneCoexpressionAnalysisService.thaw( analysis );
        Collection<ExpressionExperiment> datasetsAnalyzed = expressionExperimentService.loadMultiple( eeIds );
        analysis.getExperimentsAnalyzed().retainAll( datasetsAnalyzed );
        analysis.getExperimentsAnalyzed().addAll( datasetsAnalyzed );
        geneCoexpressionAnalysisService.update( analysis );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings( { "unchecked", "unused" })
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        return new ModelAndView( this.getFormView() );
    }

}
