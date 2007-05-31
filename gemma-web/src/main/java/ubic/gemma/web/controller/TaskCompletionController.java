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

import java.util.concurrent.CancellationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Generic controller that looks for a finished job.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="taskCompletionController"
 * @spring.property name="taskRunningService" ref="taskRunningService"
 */
public class TaskCompletionController extends BaseFormController {

    private static Log log = LogFactory.getLog( TaskCompletionController.class.getName() );

    TaskRunningService taskRunningService;

    /**
     * If this has a value in the request, the job given will be cancelled (instead of checked).
     */
    public static final String CANCEL_ATTRIBUTE = "cancel";

    /**
     * @param taskRunningService the taskRunningService to set
     */
    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    @SuppressWarnings("unused")
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        //String taskId = ( String ) request.getSession().getAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        String taskId =  request.getParameter( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        if ( taskId == null ) {
            
            this.saveMessage( request, "Can not monitor a task with a null task Id" );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // have to replace this...
        }

        //todo: is this redundant code?  see ProcessDeleteController. remove? 
        if ( request.getAttribute( CANCEL_ATTRIBUTE ) != null ) {
            log.info( "Cancelling " + taskId );
            taskRunningService.cancelTask( taskId );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // have to replace this...
        }

        log.debug( "Checking for job " + taskId );

        ModelAndView returnedView;
        try {
            returnedView =  (ModelAndView) taskRunningService.checkResult( taskId );
            if (returnedView == null){
                this.saveMessage( request, "No task found with id: " + taskId );
                return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // have to replace this...
            }
              
            //returnedView.setView( new RedirectView(returnedView.getViewName()) );
              return returnedView;
            
        } catch ( CancellationException e ) {
            log.debug( "Job was cancelled" );
            
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // have to replace this...
        } catch ( Throwable e ) {
            log.debug( "Got an exception: " + e );
            if ( e instanceof Exception ) throw ( Exception ) e;
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // have to replace this...
        }
        
    }
        
}
