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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * <hr>
 * <p>
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="experimentalDesignFormController" name="/experimentalDesign/editExperimentalDesign.html"
 * @spring.property name = "commandName" value="experimentalDesign"
 * @spring.property name = "formView" value="experimentalDesign.edit"
 * @spring.property name = "successView" value="redirect:/expressionExperiment/showAllExpressionExperiments.html"
 * @spring.property name = "experimentalDesignService" ref="experimentalDesignService"
 */
public class ExperimentalDesignFormController extends BaseFormController {
    private ExperimentalDesignService experimentalDesignService = null;
    private final String messagePrefix = "ExperimentalDesign with id";
    
    /**
     * 
     *
     */
    public ExperimentalDesignFormController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
        setCommandClass( ExperimentalDesign.class );
    }
    
    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    protected Object formBackingObject( HttpServletRequest request ) {

        Long id = Long.parseLong( request.getParameter( "id" ) );

        ExperimentalDesign ed = null;

        log.debug( id );

        if ( !"".equals( id ) )
            ed = experimentalDesignService.findById( id );

        else
            ed = ExperimentalDesign.Factory.newInstance();

        saveMessage( request, getText( "object.editing", new Object[] { messagePrefix, ed.getId() }, request
                .getLocale() ) );

        return ed;
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

        ExperimentalDesign ed = ( ExperimentalDesign ) command;

        experimentalDesignService.update( ed );

        saveMessage( request, getText( "object.saved", new Object[] { messagePrefix, ed.getId() }, request.getLocale() ) );

        return new ModelAndView( getSuccessView() );
    }
    
    /**
     * @return
     */
    public ExperimentalDesignService getExperimentalDesignService() {
        return experimentalDesignService;
    }

    /**
     * @param experimentalDesignService
     */
    public void setExperimentalDesignService( ExperimentalDesignService experimentalDesignService ) {
        this.experimentalDesignService = experimentalDesignService;
    }

    

}
