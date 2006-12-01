/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.util.MessageUtil;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "expressionExperimentSubSetService" ref="expressionExperimentSubSetService"
 * @spring.property name="methodNameResolver" ref="expressionExperimentActions"
 * @spring.property name="searchService" ref="searchService"
 */
public class ExpressionExperimentController extends BackgroundProcessingMultiActionController {

    private ExpressionExperimentService expressionExperimentService = null;
    private ExpressionExperimentSubSetService expressionExperimentSubSetService = null;
    private SearchService searchService;

    private final String messagePrefix = "Expression experiment with id";
    private final String identifierNotFound = "Must provide a valid ExpressionExperiment identifier";

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param expressionExperimentSubSetService
     */
    public void setExpressionExperimentSubSetService(
            ExpressionExperimentSubSetService expressionExperimentSubSetService ) {
        this.expressionExperimentSubSetService = expressionExperimentSubSetService;
    }

    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String filter = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            this.saveMessage( request, "No search critera provided" );
            return showAll( request, response );
        }

        List<ExpressionExperiment> searchResults = searchService.compassExpressionSearch( filter );

       if ((searchResults == null) || (searchResults.size() == 0)) {
           this.saveMessage( request, "Your search yielded no results.");
           return showAll(request, response);
       }
           
        String list = "";
        for ( ExpressionExperiment ee : searchResults )
            list += ee.getId() + ",";
        
        this.saveMessage( request, "Search Criteria: " + filter );
        this.saveMessage( request, searchResults.size() + " Expression Experiments matched your search." );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=" + list ));
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        if ( request.getParameter( "id" ) == null ) {
            // should be a validator error on submit
            return redirectToList( request );
        }

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validator error on submit
            return redirectToList( request );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            return redirectToList( request );
        }
        request.setAttribute( "id", id );
        ModelAndView mav = new ModelAndView( "expressionExperiment.detail" ).addObject( "expressionExperiment",
                expressionExperiment );

        Set s = expressionExperimentService.getQuantitationTypeCountById( id ).entrySet();
        mav.addObject( "qtCountSet", expressionExperimentService.getQuantitationTypeCountById( id ).entrySet() );

        // add arrayDesigns used, by name
        Collection<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        for ( BioAssay assay : bioAssays ) {
            ArrayDesign design = assay.getArrayDesignUsed();
            if ( !arrayDesigns.contains( design ) ) {
                arrayDesigns.add( design );
            }
        }

        mav.addObject( "arrayDesigns", arrayDesigns );
        long num = expressionExperimentService.getDesignElementDataVectorCountById( id );
        // add count of designElementDataVectors
        mav.addObject( "designElementDataVectorCount", new Long( expressionExperimentService
                .getDesignElementDataVectorCountById( id ) ) );
        return mav;
    }

    private ModelAndView redirectToList( HttpServletRequest request ) {
        this.addMessage( request, "errors.objectnotfound", new Object[] { "Expression Experiment " } );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) );
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showBioAssays( HttpServletRequest request, HttpServletResponse response ) {
        String idStr = request.getParameter( "id" );

        if ( idStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }
        Long id = Long.parseLong( idStr );

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        Map m = expressionExperimentService.getQuantitationTypeCountById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", expressionExperiment.getBioAssays() );
    }

    /**
     * shows a list of BioAssays for an expression experiment subset
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showSubSet( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperimentSubSet subset = expressionExperimentSubSetService.load( id );
        if ( subset == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        // request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", subset.getBioAssays() );
    }

    /**
     * Shows a bioassay view of a single expression experiment subset.
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showExpressionExperimentSubSet( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", expressionExperiment.getBioAssays() );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<ExpressionExperimentValueObject> expressionExperiments = new ArrayList<ExpressionExperimentValueObject>();
        // if no IDs are specified, then load all expressionExperiments
        if ( sId == null ) {
            this.saveMessage( request, "Displaying all Expression Experiments" );
            expressionExperiments.addAll( expressionExperimentService.loadAllValueObjects() );
        }

        // if ids are specified, then display only those expressionExperiments
        else {
            Collection ids = new ArrayList<Long>();

            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                ids.add( new Long( idList[i] ) );
            }
            expressionExperiments.addAll( expressionExperimentService.loadValueObjects( ids ) );
        }
        Long numExpressionExperiments = new Long( expressionExperiments.size() );
        ModelAndView mav = new ModelAndView( "expressionExperiments" );
        mav.addObject( "expressionExperiments", expressionExperiments );
        mav.addObject( "numExpressionExperiments", numExpressionExperiments );
        return mav;

    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {

        Long id = null;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            throw new EntityNotFoundException( "There was no valid identifier." );
        }

        if ( id == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( expressionExperiment + " not found" );
        }

        String taskId = startJob( expressionExperiment, request );
        return new ModelAndView( new RedirectView( "/Gemma/processProgress.html?taskid=" + taskId ) );
        // return doDelete( request, response, expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseBackgroundProcessingFormController#getRunner(org.acegisecurity.context.SecurityContext,
     *      java.lang.Object, java.lang.String)
     */
    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
            HttpServletRequest request, Object command, MessageUtil messenger ) {

        return new BackgroundControllerJob<ModelAndView>( taskId, securityContext, request, command, messenger ) {

            @SuppressWarnings("unchecked")
            public ModelAndView call() throws Exception {

                SecurityContextHolder.setContext( securityContext );

                ExpressionExperiment ee = ( ExpressionExperiment ) command;
                expressionExperimentService.thawLite( ee );
                ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext
                        .getAuthentication().getName(), "Deleting expression experiment: " + ee.getId() );

                expressionExperimentService.delete( ee );
                saveMessage( "Expression Experiment " + ee.getShortName() + " removed from Database" );
                ee = null;

                ProgressManager.destroyProgressJob( job );
                return new ModelAndView( new RedirectView(
                        "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) );
            }
        };
    }

    /**
     * @return the searchService
     */
    public SearchService getSearchService() {
        return searchService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

}