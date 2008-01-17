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

import java.util.List;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressStatusService;

/**
 * Test of long job control.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TaskRunningTest extends BaseSpringWebTest {

    AbstractController controller;
    TaskCompletionController taskCheckController;
    ProgressStatusService progressStatusService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringWebTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        controller = ( AbstractController ) this.getBean( "mockController" );

        taskCheckController = ( TaskCompletionController ) this.getBean( "taskCompletionController" );

        progressStatusService = ( ProgressStatusService ) this.getBean( "progressStatusService" );
    }

    /**
     * @throws Exception
     */
    public final void testSuccessfulRun() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/mock.html" );

        // goes to the progress page...
        ModelAndView mv = controller.handleRequest( request, response );
        assertTrue( mv.getView() instanceof RedirectView );
        assertTrue( "Got " + ( ( RedirectView ) mv.getView() ).getUrl(), ( ( RedirectView ) mv.getView() ).getUrl()
                .startsWith( "/Gemma/processProgress.html?taskid=" ) );

        Object taskId = mv.getModel().get( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        assertNotNull( taskId );

        // wait for job to run
        long timeout = 5000;
        ProgressData lastResult = null;
        long startTime = System.currentTimeMillis();
        wait: while ( true ) {
            Thread.sleep( 500 );
            List<ProgressData> result = progressStatusService.getProgressStatus( ( String ) taskId );
            if ( result.size() > 1 ) {
                for ( ProgressData lr : result ) {
                    lastResult = lr;
                    if ( lr.isDone() ) break wait;
                }
            }
            log.info( "Waiting .." );

            if ( System.currentTimeMillis() - startTime > timeout ) fail( "Test timed out" );
        }
        assertNotNull( lastResult );
        assertTrue( !lastResult.isFailed() );
    }

    /**
     * @throws Exception
     */
    public final void testFailedRun() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/mock.html" );
        request.setAttribute( "throw", "true" );

        // goes to the progress page...
        ModelAndView mv = controller.handleRequest( request, response );
        assertTrue( mv.getView() instanceof RedirectView );
        assertTrue( "Got " + ( ( RedirectView ) mv.getView() ).getUrl(), ( ( RedirectView ) mv.getView() ).getUrl()
                .startsWith( "/Gemma/processProgress.html?taskid=" ) );

        Object taskId = mv.getModel().get( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        assertNotNull( taskId );

        // wait for job to run
        ProgressData lastResult = null;
        long timeout = 5000;
        long startTime = System.currentTimeMillis();
        wait: while ( true ) {
            Thread.sleep( 500 );
            List<ProgressData> result = progressStatusService.getProgressStatus( ( String ) taskId );
            if ( result.size() > 1 ) {

                for ( ProgressData lr : result ) {
                    lastResult = lr;
                    if ( lr.isFailed() ) {
                        return; // yay
                    }
                    if ( lr.isDone() ) break wait;
                }
            }
            log.info( "Waiting .." );

            if ( System.currentTimeMillis() - startTime > timeout ) fail( "Test timed out" );
        }

        assertNotNull( lastResult );
        assertTrue( lastResult.isFailed() );

    }

    /**
     * @throws Exception
     */
    public final void testCancelledRun() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/mock.html" );
        request.setAttribute( "cancel", "true" );

        // goes to the progress page...
        ModelAndView mv = controller.handleRequest( request, response );
        assertTrue( mv.getView() instanceof RedirectView );
        assertTrue( "Got " + ( ( RedirectView ) mv.getView() ).getUrl(), ( ( RedirectView ) mv.getView() ).getUrl()
                .startsWith( "/Gemma/processProgress.html?taskid=" ) );

        Object taskId = mv.getModel().get( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        assertNotNull( taskId );

        // let it go a little while
        Thread.sleep( 100 );

        // cancel it.
        boolean cancelJob = progressStatusService.cancelJob( ( String ) taskId );
        assertTrue( cancelJob );

    }

}
