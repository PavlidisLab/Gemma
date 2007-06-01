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

package ubic.gemma.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author klc
 * @version $Id$
 * @spring.bean id="processDeleteController"
 * @spring.property name="formView" value="mainMenu"
 * @spring.property name="successView" value="mainMenu"
 * @spring.property name="taskRunningService" ref="taskRunningService"
 * @deprecated Please use ajax calls to cancel jobs via the ProgressStatusService.
 */
public class ProcessDeleteController extends BaseFormController {

    TaskRunningService taskRunningService;

    /**
     * @param taskRunningService the taskRunningService to set
     */

    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    /**
     * 
     */
    @Override
    @SuppressWarnings("unused")
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        // String taskId = ( String ) request.getSession().getAttribute(
        // BackgroundProcessingFormController.JOB_ATTRIBUTE );
        String taskId = request.getParameter( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        if ( taskId == null ) {
            log.warn( "No thread in session.  Can't stop process" + this.getClass() );
            return new ModelAndView( new RedirectView( "/mainMenu.html" ) );
        }

        taskRunningService.cancelTask( taskId, true );
        this.saveMessage( request, "Job cancelled." );

        return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) );

    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    @SuppressWarnings("unused")
    protected Object formBackingObject( 
    HttpServletRequest request ) {

        // FIXME do you need this method at all?

        Object dummy = new Object();

        return dummy;
    }

    /**
     * Cancels the job in progress by terminating the thread.
     * 
     * @param request
     * @param response
     * @return ModelAndView
     */

    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        return this.processFormSubmission( request, response, null, null );
    }

}
