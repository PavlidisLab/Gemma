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

import java.util.Collection;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

/**
 * <hr>
 * Itegration test for progress monitoring. Tests static classes in ProgressManager. Some of these tests rely on Spring
 * and this greatly slows the speed of integration test. Tests for 1 user, running 1 job, with 1 observer TODO: test for
 * 1 user with 2 jobs, 3 jobs TODO: test for 2 users monitoring the same job TODO: test for monitoring all jobs
 * <p>
 * 
 * @author klc
 * @version $Id$
 */

public class ProgressIntegrationTest extends TestCase {

    private ProgressJob pJob;
    private HttpProgressObserver pObserver;

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressManager.CreateProgressJob(String, String)'
     */
    public void testCreateProgressJob() {

        pJob = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Testing the Progress Manager" );
        assertEquals( pJob.getUser(), "TestUser" );
        assertEquals( pJob.getProgressData().getDescription(), "Testing the Progress Manager" );
        assertEquals( pJob.getProgressType(), ProgressJob.DOWNLOAD_PROGRESS );

        ProgressManager.destroyProgressJob( pJob ); // clean up so this test won't affect next tests
        pJob = null;

    }

    /*
     * Tests the destruction of a progress job. todo add testing for deletion of a user with more than just 1 job.
     */

    public void testDestroyProgressJob() {
        pObserver = new HttpProgressObserver( "TestUser" );
        pJob = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Testing the Progress Manager" );

        // single case
        ProgressManager.addToNotification( pJob.getUser(), pObserver );
        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );

        ProgressManager.destroyProgressJob( pJob );
        assertEquals( ProgressManager.addToNotification( "TestUser", pObserver ), false );

        pJob = null;
    }

    /*
     * Test the simple case of the user only have one job. Test the more complex case of the user haveing several jobs.
     */

    public void testAddToNotificationStringObserver() {

        pObserver = new HttpProgressObserver( "TestUser" );
        pJob = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Testing the Progress Manager" );

        // single case
        ProgressManager.addToNotification( pJob.getUser(), pObserver );
        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );

        assertEquals( pObserver.getProgressData().getPercent(), 88 );
        assertEquals( pObserver.getProgressData().getDescription(), "Another test" );
        assert ( pObserver.getProgressData().isDone() );
        ProgressManager.destroyProgressJob( pJob );
        pJob = null;

        // multiple case
        ProgressJob pJob1 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DATABASE_PROGRESS,
                "Test1 of Notify" );
        ProgressJob pJob2 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Test2 of Notify" );

        MockClient mClient = new MockClient();
        ProgressManager.addToNotification( "TestUser", mClient );
        pJob1.updateProgress();
        assertEquals( mClient.upDateTimes(), 1 );
        pJob2.updateProgress();
        assertEquals( mClient.upDateTimes(), 2 );
        assertEquals( mClient.getProgressData().size(), 2 );

        ProgressManager.destroyProgressJob( pJob1 );
        ProgressManager.destroyProgressJob( pJob2 );

    }

    /*
     * Test adding to notify list when list contains 1 job of correct type Test adding to notify list when list contains
     * multiple jobs of difernt types test addint to notify list when list contains mutiple jobs of different and same
     * type
     */
    public void testAddToNotificationStringIntObserver() {

        pObserver = new HttpProgressObserver( "TestUser" );
        pJob = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Testing the Progress Manager" );

        // single case
        ProgressManager.addToNotification( pJob.getUser(), ProgressJob.DOWNLOAD_PROGRESS, pObserver );
        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );

        assertEquals( pObserver.getProgressData().getPercent(), 88 );
        assertEquals( pObserver.getProgressData().getDescription(), "Another test" );
        assert ( pObserver.getProgressData().isDone() );
        ProgressManager.destroyProgressJob( pJob );
        pJob = null;

        // multiple case
        ProgressJob pJob1 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DATABASE_PROGRESS,
                "Test1 of Notify" );
        ProgressJob pJob2 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Test2 of Notify" );

        MockClient mClient = new MockClient();
        ProgressManager.addToNotification( "TestUser", ProgressJob.DATABASE_PROGRESS, mClient );
        pJob1.updateProgress();
        assertEquals( mClient.upDateTimes(), 1 );
        pJob2.updateProgress();
        assertEquals( mClient.upDateTimes(), 1 );
        assertEquals( mClient.getProgressData().size(), 1 );

        ProgressJob pJob3 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DATABASE_PROGRESS,
                "Test3 of Notify" );
        ProgressJob pJob4 = ProgressManager.createProgressJob( "TestUser", ProgressJob.PARSING_PROGRESS,
                "Test4 of Notify" );

        ProgressManager.addToNotification( "TestUser", ProgressJob.DATABASE_PROGRESS, mClient );
        pJob3.updateProgress();
        assertEquals( mClient.upDateTimes(), 2 );
        pJob4.updateProgress();
        assertEquals( mClient.upDateTimes(), 2 );
        assertEquals( mClient.getProgressData().size(), 2 );

        ProgressManager.destroyProgressJob( pJob1 );
        ProgressManager.destroyProgressJob( pJob2 );
        ProgressManager.destroyProgressJob( pJob3 );
        ProgressManager.destroyProgressJob( pJob4 );

    }

    /*
     * Test adding to notify list when list contains 1 job Test adding to notify list when list contains multiple jobs
     */
    public void testAddToRecentNotificationStringObserver() {

        pObserver = new HttpProgressObserver( "TestUser" );
        pJob = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Testing the Progress Manager" );

        // single case
        ProgressManager.addToNotification( pJob.getUser(), pObserver );
        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );

        assertEquals( pObserver.getProgressData().getPercent(), 88 );
        assertEquals( pObserver.getProgressData().getDescription(), "Another test" );
        assert ( pObserver.getProgressData().isDone() );
        ProgressManager.destroyProgressJob( pJob );
        pJob = null;

        // multiple case
        ProgressJob pJob1 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DATABASE_PROGRESS,
                "Test1 of Notify" );
        ProgressJob pJob2 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Test2 of Notify" );
        ProgressJob pJob3 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DATABASE_PROGRESS,
                "Test3 of Notify" );
        ProgressJob pJob4 = ProgressManager.createProgressJob( "TestUser", ProgressJob.PARSING_PROGRESS,
                "Test4 of Notify" );

        MockClient mClient = new MockClient();
        ProgressManager.addToRecentNotification( "TestUser", mClient );
        pJob1.updateProgress();
        assertEquals( mClient.upDateTimes(), 0 );
        pJob2.updateProgress();
        assertEquals( mClient.upDateTimes(), 0 );
        pJob3.updateProgress();
        assertEquals( mClient.upDateTimes(), 0 );
        pJob4.updateProgress();
        assertEquals( mClient.upDateTimes(), 1 );

        assertEquals( mClient.getProgressData().size(), 1 );

        ProgressManager.destroyProgressJob( pJob1 );
        ProgressManager.destroyProgressJob( pJob2 );
        ProgressManager.destroyProgressJob( pJob3 );
        ProgressManager.destroyProgressJob( pJob4 );

    }

    /*
     * Test adding to notify list when list contains 1 job of correct type todo: Test adding to notify list when list
     * contains multiple jobs of difernt types todo: test addint to notify list when list contains mutiple jobs of
     * different and same type
     */
    public void testAddToRecentNotificationStringIntObserver() {

        pObserver = new HttpProgressObserver( "TestUser" );
        pJob = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Testing the Progress Manager" );

        // single case
        ProgressManager.addToRecentNotification( pJob.getUser(), ProgressJob.DOWNLOAD_PROGRESS, pObserver );
        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );

        assertEquals( pObserver.getProgressData().getPercent(), 88 );
        assertEquals( pObserver.getProgressData().getDescription(), "Another test" );
        assert ( pObserver.getProgressData().isDone() );
        ProgressManager.destroyProgressJob( pJob );
        pJob = null;

        // multiple case
        ProgressJob pJob1 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DATABASE_PROGRESS,
                "Test1 of Notify" );
        ProgressJob pJob2 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DOWNLOAD_PROGRESS,
                "Test2 of Notify" );

        MockClient mClient = new MockClient();
        ProgressManager.addToRecentNotification( "TestUser", ProgressJob.DATABASE_PROGRESS, mClient );
        pJob1.updateProgress();
        assertEquals( mClient.upDateTimes(), 1 );
        pJob2.updateProgress();
        assertEquals( mClient.upDateTimes(), 1 );
        assertEquals( mClient.getProgressData().size(), 1 );

        mClient = new MockClient();
        ProgressJob pJob3 = ProgressManager.createProgressJob( "TestUser", ProgressJob.DATABASE_PROGRESS,
                "Test3 of Notify" );
        ProgressJob pJob4 = ProgressManager.createProgressJob( "TestUser", ProgressJob.PARSING_PROGRESS,
                "Test4 of Notify" );

        ProgressManager.addToRecentNotification( "TestUser", ProgressJob.DATABASE_PROGRESS, mClient );

        pJob1.updateProgress();
        assertEquals( mClient.upDateTimes(), 0 );
        pJob2.updateProgress();
        assertEquals( mClient.upDateTimes(), 0 );
        pJob3.updateProgress();
        assertEquals( mClient.upDateTimes(), 1 );
        pJob4.updateProgress();
        assertEquals( mClient.upDateTimes(), 1 );
        assertEquals( mClient.getProgressData().size(), 1 );

        ProgressManager.destroyProgressJob( pJob1 );
        ProgressManager.destroyProgressJob( pJob2 );
        ProgressManager.destroyProgressJob( pJob3 );
        ProgressManager.destroyProgressJob( pJob4 );

    }

    /**
     * Tests 1 user with 1 job with 1 observer
     */
    public void testSingleUse() {

        MockProcess mp = new MockProcess( "TestRun", "A run of tests" );
        MockClient mc = new MockClient();
        ProgressManager.addToNotification( "TestRun", mc );
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
            simpleJob = ProgressManager.createProgressJob( userName, ProgressJob.DOWNLOAD_PROGRESS, description );

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
    class MockClient implements Observer {

        private int update;
        private Map<Observable, ProgressData> jobs = new ConcurrentHashMap<Observable, ProgressData>();

        public MockClient() {

            this.update = 0;

        }

        @SuppressWarnings("unused")
        public void update( Observable o, Object pd ) {
            jobs.put( o, ( ProgressData ) pd );
            update++;

        }

        public int upDateTimes() {
            return this.update;

        }

        public Collection<ProgressData> getProgressData() {
            return jobs.values();
        }

    }

}
