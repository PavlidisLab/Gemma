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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
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
 * @spring.property name = "successView"
 *                  value="redirect:/expressionExperiment/showExpressionExperimentSearchResults.html"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 */
public class ExpressionExperimentSearchController extends BaseFormController {// TODO put in validator
    private static Log log = LogFactory.getLog( ExpressionExperimentSearchController.class.getName() );

    ExpressionExperimentService expressionExperimentService = null;
    ContactService contactService = null;

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

        Long id = Long.parseLong( request.getParameter( "id" ) );

        log.debug( id );

        ExpressionExperiment ee = null;
        ExpressionExperimentSearchCommand eecc = new ExpressionExperimentSearchCommand();

        if ( !"".equals( id ) )
            ee = expressionExperimentService.findById( id );

        else
            ee = ExpressionExperiment.Factory.newInstance();

        eecc.setExpressionExperiment( ee );
        eecc.setDescription( ee.getDescription() );
        eecc.setName( ee.getName() );
        eecc.setSearchString( "NAT1" );
        eecc.setStringency( 1 );

        request.setAttribute( "command", eecc );// must manually put the object back in the request scope.
        return eecc;

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
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        // saveMessage( request, getText( "object.saved", new Object[] { messagePrefix, ee.getId() },
        // request.getLocale() ) );

        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unchecked")
    protected Map referenceData( HttpServletRequest request ) {
        Collection searchCategories = new HashSet();
        searchCategories.add( "probe set id" );
        searchCategories.add( "gene symbol" );

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
}
