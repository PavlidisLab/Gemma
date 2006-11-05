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

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.web.util.MockLongJobController;

/**
 * Test of long job control.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TaskRunningTest extends BaseSpringWebTest {

    AbstractController controller;
    TaskCompletionController taskCheckController;

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

    }

    public final void testSuccessfulRun() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/mock.html" );

        // goes to the progress page...
        ModelAndView mv = controller.handleRequest( request, response );
        assertTrue( mv.getView() instanceof RedirectView );
        assertTrue( ( ( RedirectView ) mv.getView() ).getUrl().startsWith( "processProgress.html?taskId=" ) );
        String taskId = ( String ) request.getSession().getAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        assertNotNull( taskId );

        // wait for job to run
        Thread.sleep( ( int ) ( MockLongJobController.JOB_LENGTH * 1.5 ) );

        // check on the job.
        MockHttpServletRequest afterRequest = newPost( "/checkJobProgress.html" );
        afterRequest.getSession().setAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE, taskId );

        ModelAndView result = null;

        long timeout = 60000;
        long startTime = System.currentTimeMillis();
        while ( result == null ) {
            Thread.sleep( 1000 );
            try {
                result = taskCheckController.handleRequest( afterRequest, response );
            } catch ( Exception e ) {
                fail( "Got an exception: " + e );
            }
            if ( System.currentTimeMillis() - startTime > timeout ) fail( "Test timed out" );
        }

        assertEquals( "view", result.getViewName() );
        assertEquals( "42", result.getModel().get( "answer" ) );

    }

    public final void testFailedRun() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/mock.html" );
        request.setAttribute( "throw", "true" );

        // goes to the progress page...
        ModelAndView mv = controller.handleRequest( request, response );
        assertTrue( mv.getView() instanceof RedirectView );
        assertTrue( ( ( RedirectView ) mv.getView() ).getUrl().startsWith( "processProgress.html?taskId=" ) );

        String taskId = ( String ) request.getSession().getAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        assertNotNull( taskId );

        // wait for job to throw exception.
        Thread.sleep( 2000 );

        MockHttpServletRequest afterRequest = newPost( "/checkJobProgress.html" );
        afterRequest.setAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE, taskId );

        try {
            taskCheckController.handleRequest( afterRequest, response );
            fail( "Should have seen an exception" );
        } catch ( Exception e ) {
            // that's what we expected
            assertTrue( "Got a " + e.getClass(), e instanceof IllegalArgumentException );
        }

    }

    public final void testCancelledRun() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/mock.html" );
        request.setAttribute( "cancel", "true" );

        // goes to the progress page...
        ModelAndView mv = controller.handleRequest( request, response );
        assertTrue( mv.getView() instanceof RedirectView );
        assertTrue( ( ( RedirectView ) mv.getView() ).getUrl().startsWith( "processProgress.html?taskId=" ) );

        String taskId = ( String ) request.getSession().getAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE );
        assertNotNull( taskId );

        // let it go a little while
        Thread.sleep( 1000 );

        MockHttpServletRequest afterRequest = newPost( "/checkJobProgress.html" );
        afterRequest.getSession().setAttribute( BackgroundProcessingFormController.JOB_ATTRIBUTE, taskId );
        afterRequest.setAttribute( TaskCompletionController.CANCEL_ATTRIBUTE, "true" );

        // should have been cancelled...
        ModelAndView result = taskCheckController.handleRequest( afterRequest, response );
        assertNotNull( result );

    }

}
