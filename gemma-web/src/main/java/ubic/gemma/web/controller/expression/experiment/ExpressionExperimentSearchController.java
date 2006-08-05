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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.visualization.ExpressionDataMatrix;
import ubic.gemma.visualization.ExpressionDataMatrixVisualization;
import ubic.gemma.web.controller.BaseFormController;

/**
 * <hr>
 * <p>
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentSearchController" name="/expressionExperiment/searchExpressionExperiment.html"
 * @spring.property name = "commandName" value="expressionExperimentSearchCommand"
 * @spring.property name = "commandClass"
 *                  value="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSearchCommand"
 * @spring.property name = "formView" value="searchExpressionExperimentForm"
 * @spring.property name = "successView" value="showExpressionExperimentSearchResults"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class ExpressionExperimentSearchController extends BaseFormController {
    private static Log log = LogFactory.getLog( ExpressionExperimentSearchController.class.getName() );

    ExpressionExperimentService expressionExperimentService = null;
    CompositeSequenceService compositeSequenceService = null;
    String[] searchIds = null;
    Collection<DesignElement> designElements = null;
    Long id = null;

    // private final String messagePrefix = "Expression experiment with id";

    public ExpressionExperimentSearchController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    protected Object formBackingObject( HttpServletRequest request ) {

        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            // return an error.
        }
        log.debug( id );

        ExpressionExperiment ee = null;
        ExpressionExperimentSearchCommand eesc = new ExpressionExperimentSearchCommand();

        if ( !"".equals( id ) )
            ee = expressionExperimentService.findById( id );

        else
            ee = ExpressionExperiment.Factory.newInstance();

        eesc.setId( ee.getId() );
        eesc.setDescription( ee.getDescription() );
        eesc.setName( ee.getName() );
        eesc.setSearchString( "36936_at" );
        eesc.setStringency( 1 );

        return eesc;

    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        id = ( ( ExpressionExperimentSearchCommand ) command ).getId();

        if ( request.getParameter( "cancel" ) != null ) {
            return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":8080"
                    + request.getContextPath() + "/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
        }

        // more searchString validation - see also validation.xml
        String searchString = ( ( ExpressionExperimentSearchCommand ) command ).getSearchString();
        searchIds = StringUtils.tokenizeToStringArray( searchString, ",", true, true );
        designElements = new HashSet();
        for ( int i = 0; i < searchIds.length; i++ ) {
            log.debug( "searching for " + searchIds[i] );

            DesignElement de = compositeSequenceService.findByName( searchIds[i] );
            if ( de != null ) designElements.add( de );
        }

        log.debug( designElements.size() );
        if ( designElements.size() == 0 ) {
            errors.addError( new ObjectError( command.toString(), null, null, "Requested probe sets do not exist." ) );
        }

        // more searchCriteria validation - see also validation.xml
        if ( ( ( ExpressionExperimentSearchCommand ) command ).getSearchCriteria().equalsIgnoreCase( "gene symbol" ) )
            errors.addError( new ObjectError( command.toString(), null, null,
                    "Search by gene symbol unsupported at this time." ) );

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        String searchCriteria = ( ( ExpressionExperimentSearchCommand ) command ).getSearchCriteria();
        boolean suppressVisualizations = ( ( ExpressionExperimentSearchCommand ) command ).isSuppressVisualizations();

        // TODO allow filename to be entered from form
        String filename = ( ( ExpressionExperimentSearchCommand ) command ).getFilename();
        if ( filename == null ) filename = "visualization.png";
        String visualDir = getServletContext().getRealPath( "/resources" ) + "/" + request.getRemoteUser() + "/";

        File dirPath = new File( visualDir );

        // Create the directory if it doesn't exist
        if ( !dirPath.exists() ) {
            dirPath.mkdirs();
        }

        filename = visualDir + filename;
        log.info( "filename: " + filename );

        ExpressionDataMatrix expressionDataMatrix = null;
        ExpressionDataMatrixVisualization matrixVisualization = null;
        if ( searchCriteria.equalsIgnoreCase( "probe set id" ) ) {
            ExpressionExperiment ee = expressionExperimentService.findById( id );
            expressionDataMatrix = new ExpressionDataMatrix( ee, designElements );

            matrixVisualization = new ExpressionDataMatrixVisualization();
            matrixVisualization.setExpressionDataMatrix( expressionDataMatrix );
            matrixVisualization.setOutfile( filename );
            matrixVisualization.setSuppressVisualizations( suppressVisualizations );

        } else {
            log.debug( "search by official gene symbol" );
            // call service which produces expression data image based on gene symbol search criteria
        }

        return new ModelAndView( getSuccessView() )
                .addObject( "expressionDataMatrixVisualization", matrixVisualization );
    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unchecked")
    protected Map referenceData( HttpServletRequest request ) {
        Collection searchCategories = new HashSet();
        searchCategories.add( "gene symbol" );
        searchCategories.add( "probe set id" );

        Map searchByMap = new HashMap();

        searchByMap.put( "searchCategories", searchCategories );
        return searchByMap;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param compositeSequenceService
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }
}
