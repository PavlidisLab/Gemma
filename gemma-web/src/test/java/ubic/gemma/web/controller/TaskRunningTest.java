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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.job.progress.ProgressData;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.web.job.progress.ProgressStatusService;
import ubic.gemma.web.util.BaseSpringWebTest;
import ubic.gemma.web.util.MockLongJobController;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test of long job control.
 *
 * @author pavlidis
 */
public class TaskRunningTest extends BaseSpringWebTest {

    @Autowired
    private ProgressStatusService progressStatusService;

    @Autowired
    private TaskRunningService taskRunningService;

    @Autowired
    private MockLongJobController mockLongJobController;

    @Test
    public final void testCancelledRun() throws Exception {

        // goes to the progress page...handleRequest
        TaskCommand taskCommand = new TaskCommand();
        String taskId = mockLongJobController.runJob( taskCommand );

        // let it go a little while
        Thread.sleep( 100 );

        SubmittedTask task = taskRunningService.getSubmittedTask( taskId );
        assertNotNull( task );

        // cancel it.
        task.requestCancellation();
        assertEquals( SubmittedTask.Status.CANCELLING, task.getStatus() );
    }

    @Test
    public final void testFailedRun() throws Exception {

        // goes to the progress page...handleRequest
        TaskCommand taskCommand = new TaskCommand();
        taskCommand.setPersistJobDetails( false ); // we use this for 'die' in this test.

        String taskId = mockLongJobController.runJob( taskCommand );
        assertNotNull( taskId );

        // wait for job to run
        ProgressData lastResult = null;
        long timeout = 5000;
        long startTime = System.currentTimeMillis();
        wait:
        while ( true ) {
            Thread.sleep( 500 );
            List<ProgressData> result = progressStatusService.getProgressStatus( taskId );
            if ( result.size() > 0 ) {

                for ( ProgressData lr : result ) {
                    lastResult = lr;
                    if ( lr.isFailed() ) {
                        return; // yay
                    }
                    if ( lr.isDone() )
                        break wait;
                }
            }
            log.info( "Waiting .." );

            if ( System.currentTimeMillis() - startTime > timeout )
                fail( "Test timed out" );
        }

        assertNotNull( lastResult );
        assertTrue( lastResult.isFailed() );
    }

    @Test
    @Category(SlowTest.class)
    public final void testSuccessfulRun() throws Exception {

        TaskCommand taskCommand = new TaskCommand();
        String taskId = mockLongJobController.runJob( taskCommand );

        // wait for job to run
        long timeout = 5000;
        ProgressData lastResult = null;
        long startTime = System.currentTimeMillis();
        wait:
        while ( true ) {
            Thread.sleep( 500 );
            List<ProgressData> result = progressStatusService.getProgressStatus( taskId );
            if ( result.size() > 0 ) {
                for ( ProgressData lr : result ) {
                    lastResult = lr;
                    if ( lr.isDone() )
                        break wait;
                }
            }
            log.info( "Waiting .." );

            if ( System.currentTimeMillis() - startTime > timeout )
                fail( "Test timed out" );
        }
        assertNotNull( lastResult );
        assertTrue( !lastResult.isFailed() );
    }

}
