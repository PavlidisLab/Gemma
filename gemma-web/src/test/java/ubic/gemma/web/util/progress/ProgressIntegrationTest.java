/* Copyright (c) 2006 University of British Columbia
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


package ubic.gemma.web.util.progress;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * 
 * 
 *
 * <hr>
 * Itegration test for progress monitoring.  Tests static classes in ProgressManager.
 * Some of these tests rely on Spring and this greatly slows the speed of integration test.
 *   
 * Tests for 1 user, running 1 job, with 1 observer
 * TODO:  test for 1 user with 2 jobs, 3 jobs
 * TODO:  test for 2 users monitoring the same job
 * TODO:  test for monitoring all jobs
 * 
 * <p>
 * @author klc
 * @version $Id$
 */

public class ProgressIntegrationTest extends BaseSpringContextTest {

    private ProgressJob pJob;
    private HttpProgressMonitor pObserver;

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressManager.CreateProgressJob(String, String)'
     */
    public void testCreateProgressJob() {
        pJob = ProgressManager.createProgressJob( "TestUser", "Testing the Progress Manager" );
        assertEquals( pJob.getUser(), "TestUser" );
        assertEquals( pJob.getProgressData().getDescription(), "Testing the Progress Manager" );
    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressManager.Notify(ProgressJob)'
     */
    public void testNotify() {
        pObserver = new HttpProgressMonitor();
        pJob = ProgressManager.createProgressJob( "TestUser", "Testing the Progress Manager" );

        ProgressManager.addToNotification( pJob.getUser(), pObserver );
        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );
        // ProgressManager.notify( pJob );
        assertEquals( pObserver.getProgressStatus().getPercent(), 88 );
        assertEquals( pObserver.getProgressStatus().getDescription(), "Another test" );
        assert ( pObserver.getProgressStatus().isDone() );

    }

    /**
     * Tests 1 user with 1 job with 1 observer
     */
    public void testSingleUse() {

        MockProcess mp = new MockProcess( "TestRun", "A run of tests" );
        MockClient mc = new MockClient( "TestRun" );
        mp.run();

        try {
            Thread.sleep( 3000 );
        } catch ( InterruptedException e ) {
            e.printStackTrace();

        }
        assertEquals( mc.upDateTimes(), 100 );
    }

    /**
     * <hr>
     * Just a mockProcess inner class to ease testing
     * <p>
     * Copyright (c) 2006 UBC Pavlab
     * 
     * @author klc
     * @version $Id$
     */
    class MockProcess implements Runnable {
        private static final int DELAY = 30;

        private String userName;
        private String description;
        private ProgressJob simpleJob;

        public MockProcess( String fakeName, String fakeDescription ) {

            userName = fakeName;
            description = fakeDescription;
            simpleJob = ProgressManager.createProgressJob( userName, description );

        }

        public void run() {

            for ( int i = 0; i < 100; i++ ) {
                simpleJob.updateProgress();

                try {
                    Thread.sleep( DELAY );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }

            }

        }

    }

    /**
     * <hr>
     * Just a mock client inner class to ease testing
     * <p>
     * Copyright (c) 2006 UBC Pavlab
     * 
     * @author klc
     * @version $Id$
     */
    class MockClient implements ProgressObserver {

        private String userName;
        private int update;

        public MockClient( String uName ) {

            this.update = 0;
            this.userName = uName;
            ProgressManager.addToNotification( this.userName, this );

        }

        @SuppressWarnings("unused")
        public void progressUpdate( ProgressData pd ) {
            this.update = this.update + 1;

        }

        public int upDateTimes() {
            return this.update;

        }

    }

}
