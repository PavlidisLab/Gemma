package ubic.gemma.web.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.search.SearchService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 * @spring.bean id="generalSearchController"
 * @spring.property name="formView" value="generalSearch"
 * @spring.property name="successView" value="generalSearch"
 * @spring.property name="searchService" ref="searchService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 */

public class GeneralSearchController extends BaseFormController {

    protected SearchService searchService;
    protected ExpressionExperimentService expressionExperimentService;
    protected ArrayDesignService arrayDesignService;

    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        String searchString = request.getParameter( "searchString" );

        // first check - searchString should allow searches of 3 characters or more ONLY
        // this is to prevent a huge wildcard search

        if ( !searchStringValidator( searchString ) ) {
            this.saveMessage( request, "Must use at least three characters for search" );
            log.info( "User entered an invalid search" );
            return new ModelAndView( getSuccessView() );
        }

        List<Gene> foundGenes = searchService.geneSearch( searchString );
        List<ExpressionExperiment> foundEEs = searchService.compassExpressionSearch( searchString );
        List<ArrayDesign> foundADs = searchService.compassArrayDesignSearch( searchString );

        ModelAndView mav = new ModelAndView( getSuccessView() );
        log.info( "Attempting general search" );
            
            mav.addObject( "SearchString", searchString );
 
            mav.addObject( "geneList", foundGenes );
            mav.addObject( "numGenes", foundGenes.size() );
 
            mav.addObject( "expressionList", expressionExperimentService
                    .loadValueObjects( generateEEIdList( foundEEs ) ) );
            mav.addObject( "numEEs", foundEEs.size() );
 
            mav.addObject( "arrayList", arrayDesignService.loadValueObjects( generateADIdList( foundADs ) ) );
            mav.addObject( "numADs", foundADs.size() );
        
        return mav;
    }

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        if ( request.getParameter( "cancel" ) != null ) {
            this.saveMessage( request, "Cancelled Search" );
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "searchString" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }

        return super.showForm( request, response, errors );
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    protected boolean searchStringValidator( String query ) {

        if ( StringUtils.isBlank( query ) ) return false;

        if ( ( query.charAt( 0 ) == '%' ) || ( query.charAt( 0 ) == '*' ) ) return false;

        return true;
    }

    protected Collection<Long> generateEEIdList( List<ExpressionExperiment> searchResults ) {
        Collection<Long> list = new ArrayList<Long>();

        for ( ExpressionExperiment ee : searchResults )
            list.add( ee.getId() );

        return list;
    }

    protected Collection<Long> generateADIdList( List<ArrayDesign> searchResults ) {
        Collection<Long> list = new ArrayList<Long>();

        for ( ArrayDesign ad : searchResults )
            list.add( ad.getId() );

        return list;
    }

    /**
     * @return the arrayDesignService
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}
