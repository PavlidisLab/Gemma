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
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.util.progress.TaskRunningService;

/**
 * Generic controller that looks for a finished job.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Controller
public class TaskCompletionController extends BaseFormController {

     @Autowired
    TaskRunningService taskRunningService;

    /**
     * If this has a value in the request, the job given will be cancelled (instead of checked).
     */
    public static final String CANCEL_ATTRIBUTE = "cancel";

    /**
     * AJAX
     * 
     * @param taskId
     * @return
     */
    public Object checkResult( String taskId ) throws Exception {
        Object result = taskRunningService.checkResult( taskId );

        if ( result == null ) return null;

        if ( result instanceof ModelAndView ) {
            View view = ( ( ModelAndView ) result ).getView();
            if ( view instanceof RedirectView ) {
                return ( ( RedirectView ) view ).getUrl();
            }
            return null;
        }
        return result;
    }

    /**
     * @param taskRunningService the taskRunningService to set
     */
    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    /*
     * The normal way this is reached is via a call with the task id. Usually this should be called only when the job is
     * done and the results are needed.
     */
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        String taskId = request.getParameter( TaskRunningService.JOB_ATTRIBUTE );
        if ( taskId == null ) {
            this.saveMessage( request, "Can not monitor a task with a null task Id" );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // have to replace this...
        }

        // todo: is this redundant code? see ProcessDeleteController. remove?
        if ( request.getAttribute( CANCEL_ATTRIBUTE ) != null ) {
            log.debug( "Cancelling " + taskId );
            taskRunningService.cancelTask( taskId );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // have to replace this...
        }

        log.debug( "Checking for job " + taskId );

        ModelAndView returnedView = null;
        try {
            int tries = 0;
            while ( tries < 3 ) {
                returnedView = ( ModelAndView ) taskRunningService.checkResult( taskId );
                if ( returnedView != null ) {
                    log.debug( "Got result for " + taskId + ":" + returnedView );
                    return returnedView;
                }
                tries++;
                Thread.sleep( 100 );
            }

            this.saveMessage( request, "No task found with id, or still running: " + taskId );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // have to replace this...

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
