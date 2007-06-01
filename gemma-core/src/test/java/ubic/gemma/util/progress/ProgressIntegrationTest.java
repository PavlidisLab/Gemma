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
package ubic.gemma.util.progress;

import java.util.Collection;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * Itegration test for progress monitoring. Tests static classes in ProgressManager. Some of these tests rely on Spring
 * and this greatly slows the speed of integration test. Tests for 1 user, running 1 job, with 1 observer TODO: test for
 * 1 user with 2 jobs, 3 jobs TODO: test for 2 users monitoring the same job TODO: test for monitoring all jobs todo
 * need to add test cases to test thread local variable todo need to add test case to test proggress jobs that happen in
 * series and nested
 * 
 * @author klc
 * @version $Id$
 */
public class ProgressIntegrationTest extends BaseSpringContextTest {

    private ProgressJob pJob;
  //  private HttpProgressObserver pObserver;

    /**
     * Tests 1 user with 1 job with 1 observer
     */
    public void testSingleUse() {

        SimpleMockProcess mp = new SimpleMockProcess( ConfigUtils.getString( "gemma.admin.user" ), "A run of tests" );
        MockClient mc = new MockClient();
     //   ProgressManager.addToNotification( ConfigUtils.getString( "gemma.admin.user" ), mc );
        mp.start();

        try {
            Thread.sleep( 3500 );
        } catch ( InterruptedException e ) {
            e.printStackTrace();

        }
        assertEquals( 101, mc.upDateTimes() );

    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressManager.CreateProgressJob(String, String)'
     */
    public void testCreateProgressJob() {

        pJob = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
                "Testing the Progress Manager" );
        assertEquals( pJob.getUser(), ConfigUtils.getString( "gemma.admin.user" ) );
   //     assertEquals( pJob.getProgressData().getDescription(), "Testing the Progress Manager" );

        ProgressManager.destroyProgressJob( pJob, true ); // clean up so this test won't affect next tests
        pJob = null;

    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressManager.CreateProgressJob(String, String)' Tests creating a
     * progress Job for an invalid user/anonymous user
     */
    // this test doesn't make sense as since the test enviroment has a user associated with it the progress manager
    // automatically
    // assisngs it to the job...
    // public void testCreateAnonymousProgressJob() {
    //
    // pJob = ProgressManager.createProgressJob( "123456", "123456", "Testing the Progress Manager in anonymous ways" );
    // assertEquals( "test",pJob.getUser() );
    // assertEquals( pJob.getProgressData().getDescription(), "Testing the Progress Manager in anonymous ways" );
    //
    // ProgressManager.destroyProgressJob( pJob ); // clean up so this test won't affect next tests
    // pJob = null;
    //
    // }
    /*
     * Tests the destruction of a progress job. todo add testing for deletion of a user with more than just 1 job.
     */

    public void testDestroyProgressJob() {
   //     pObserver = new HttpProgressObserver( ConfigUtils.getString( "gemma.admin.user" ) );
        pJob = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
                "Testing the Progress Manager" );

        // single case
    //    ProgressManager.addToNotification( pJob.getUser(), pObserver );
        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );

        ProgressManager.destroyProgressJob( pJob, true );
     //   assertEquals( ProgressManager.addToNotification( ConfigUtils.getString( "gemma.admin.user" ), pObserver ),
     //           false );

        pJob = null;
    }

    /*
     * Test the simple case of the user only have one job. Test the more complex case of the user haveing several jobs.
     */

    public void testAddToNotificationStringObserver() {

    //    pObserver = new HttpProgressObserver( ConfigUtils.getString( "gemma.admin.user" ) );
        String taskID = "123FakeTaskId";
        pJob = ProgressManager.createProgressJob( taskID, ConfigUtils.getString( "gemma.admin.user" ),
                "Testing the Progress Manager" );

        // single case
     //   ProgressManager.addToNotification( taskID, pObserver );
        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );

     //   assertEquals( pObserver.getProgressData().iterator().next().getPercent(), 88 );
     //   assertEquals( pObserver.getProgressData().iterator().next().getDescription(), "Another test" );
     //   assert ( pObserver.getProgressData().iterator().next().isDone() );
        ProgressManager.destroyProgressJob( pJob, true );
        pJob = null;

        // multiple case
        ProgressJob pJob1 = ProgressManager.createProgressJob( "123FakeId",
                ConfigUtils.getString( "gemma.admin.user" ), "Test1 of Notify" );
        ProgressJob pJob2 = ProgressManager.createProgressJob( "321FakeId",
                ConfigUtils.getString( "gemma.admin.user" ), "Test2 of Notify" );

        MockClient mClient = new MockClient();
   //     ProgressManager.addToNotification( ConfigUtils.getString( "gemma.admin.user" ), mClient );
        pJob1.nudgeProgress();
        assertEquals( mClient.upDateTimes(), 1 );
        pJob2.nudgeProgress();
        assertEquals( mClient.upDateTimes(), 2 );
        assertEquals( mClient.getProgressData().size(), 1 );

        ProgressManager.destroyProgressJob( pJob1, true );
        ProgressManager.destroyProgressJob( pJob2, true );

    }

    /*
     * Test adding to notify list when list contains 1 job of correct type Test adding to notify list when list contains
     * multiple jobs of difernt types test addint to notify list when list contains mutiple jobs of different and same
     * type
     */
    // public void testAddToNotificationStringIntObserver() {
    //
    // pObserver = new HttpProgressObserver( ConfigUtils.getString( "gemma.regular.user" ) );
    // pJob = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ), "Testing the Progress
    // Manager" );
    //
    // // single case
    // ProgressManager.addToNotification( pJob.getUser(), pObserver );
    // pJob.updateProgress( new ProgressData( 88, "Another test", true ) );
    //
    // assertEquals( pObserver.getProgressData().getPercent(), 88 );
    // assertEquals( pObserver.getProgressData().getDescription(), "Another test" );
    // assert ( pObserver.getProgressData().isDone() );
    // ProgressManager.destroyProgressJob( pJob );
    // pJob = null;
    //
    // // multiple case
    // ProgressJob pJob1 = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DATABASE_PROGRESS,
    // "Test1 of Notify" );
    // ProgressJob pJob2 = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DOWNLOAD_PROGRESS,
    // "Test2 of Notify" );
    //
    // MockClient mClient = new MockClient();
    // ProgressManager.addToNotification( ConfigUtils.getString( "gemma.regular.user" ), ProgressJob.DATABASE_PROGRESS,
    // mClient );
    // pJob1.updateProgress();
    // assertEquals( mClient.upDateTimes(), 1 );
    // pJob2.updateProgress();
    // assertEquals( mClient.upDateTimes(), 1 );
    // assertEquals( mClient.getProgressData().size(), 1 );
    //
    // ProgressJob pJob3 = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DATABASE_PROGRESS,
    // "Test3 of Notify" );
    // ProgressJob pJob4 = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.PARSING_PROGRESS,
    // "Test4 of Notify" );
    //
    // ProgressManager.addToNotification( ConfigUtils.getString( "gemma.regular.user" ), ProgressJob.DATABASE_PROGRESS,
    // mClient );
    // pJob3.updateProgress();
    // assertEquals( mClient.upDateTimes(), 2 );
    // pJob4.updateProgress();
    // assertEquals( mClient.upDateTimes(), 2 );
    // assertEquals( mClient.getProgressData().size(), 2 );
    //
    // ProgressManager.destroyProgressJob( pJob1 );
    // ProgressManager.destroyProgressJob( pJob2 );
    // ProgressManager.destroyProgressJob( pJob3 );
    // ProgressManager.destroyProgressJob( pJob4 );
    //
    // }
    /*
     * Test adding to notify list when list contains 1 job Test adding to notify list when list contains multiple jobs
     */
//    public void testAddToRecentNotificationStringObserver() {
//
//        pObserver = new HttpProgressObserver( ConfigUtils.getString( "gemma.admin.user" ) );
//        pJob = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
//                "Testing the Progress Manager" );
//
//        // single case
//        ProgressManager.addToNotification( pJob.getUser(), pObserver );
//        pJob.updateProgress( new ProgressData( 88, "Another test", true ) );
//
//        assertEquals( pObserver.getProgressData().iterator().next().getPercent(), 88 );
//        assertEquals( pObserver.getProgressData().iterator().next().getDescription(), "Another test" );
//        assert ( pObserver.getProgressData().iterator().next().isDone() );
//        ProgressManager.destroyProgressJob( pJob, true );
//        pJob = null;
//
//        // multiple case
//        ProgressJob pJob1 = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
//                "Test1 of Notify" );
//        ProgressJob pJob2 = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
//                "Test2 of Notify" );
//        ProgressJob pJob3 = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
//                "Test3 of Notify" );
//        ProgressJob pJob4 = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
//                "Test4 of Notify" );
//
//        MockClient mClient = new MockClient();
//        ProgressManager.addToRecentNotification( ConfigUtils.getString( "gemma.admin.user" ), mClient );
//        pJob1.nudgeProgress();
//        assertEquals( mClient.upDateTimes(), 1 );
//        pJob2.nudgeProgress();
//        assertEquals( mClient.upDateTimes(), 2 );
//        pJob3.nudgeProgress();
//        assertEquals( mClient.upDateTimes(), 3 );
//        pJob4.nudgeProgress();
//        assertEquals( mClient.upDateTimes(), 4 );
//
//        assertEquals( mClient.getProgressData().size(), 1 );
//
//        ProgressManager.destroyProgressJob( pJob1, true );
//        ProgressManager.destroyProgressJob( pJob2, true );
//        ProgressManager.destroyProgressJob( pJob3, true );
//        ProgressManager.destroyProgressJob( pJob4, true );
//
//    }

    /*
     * Test adding to notify list when list contains 1 job of correct type todo: Test adding to notify list when list
     * contains multiple jobs of difernt types todo: test addint to notify list when list contains mutiple jobs of
     * different and same type
     */
    // public void testAddToRecentNotificationStringIntObserver() {
    //
    // pObserver = new HttpProgressObserver( ConfigUtils.getString( "gemma.regular.user" ) );
    // pJob = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DOWNLOAD_PROGRESS,
    // "Testing the Progress Manager" );
    //
    // // single case
    // ProgressManager.addToRecentNotification( pJob.getUser(), ProgressJob.DOWNLOAD_PROGRESS, pObserver );
    // pJob.updateProgress( new ProgressData( 88, "Another test", true ) );
    //
    // assertEquals( pObserver.getProgressData().getPercent(), 88 );
    // assertEquals( pObserver.getProgressData().getDescription(), "Another test" );
    // assert ( pObserver.getProgressData().isDone() );
    // ProgressManager.destroyProgressJob( pJob );
    // pJob = null;
    //
    // // multiple case
    // ProgressJob pJob1 = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DATABASE_PROGRESS,
    // "Test1 of Notify" );
    // ProgressJob pJob2 = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DOWNLOAD_PROGRESS,
    // "Test2 of Notify" );
    //
    // MockClient mClient = new MockClient();
    // ProgressManager.addToRecentNotification( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DATABASE_PROGRESS, mClient );
    // pJob1.updateProgress();
    // assertEquals( mClient.upDateTimes(), 1 );
    // pJob2.updateProgress();
    // assertEquals( mClient.upDateTimes(), 1 );
    // assertEquals( mClient.getProgressData().size(), 1 );
    //
    // mClient = new MockClient();
    // ProgressJob pJob3 = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DATABASE_PROGRESS,
    // "Test3 of Notify" );
    // ProgressJob pJob4 = ProgressManager.createProgressJob( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.PARSING_PROGRESS,
    // "Test4 of Notify" );
    //
    // ProgressManager.addToRecentNotification( ConfigUtils.getString( "gemma.regular.user" ),
    // ProgressJob.DATABASE_PROGRESS, mClient );
    //
    // pJob1.updateProgress();
    // assertEquals( mClient.upDateTimes(), 0 );
    // pJob2.updateProgress();
    // assertEquals( mClient.upDateTimes(), 0 );
    // pJob3.updateProgress();
    // assertEquals( mClient.upDateTimes(), 1 );
    // pJob4.updateProgress();
    // assertEquals( mClient.upDateTimes(), 1 );
    // assertEquals( mClient.getProgressData().size(), 1 );
    //
    // ProgressManager.destroyProgressJob( pJob1 );
    // ProgressManager.destroyProgressJob( pJob2 );
    // ProgressManager.destroyProgressJob( pJob3 );
    // ProgressManager.destroyProgressJob( pJob4 );
    //
    // }
    /*
     * Tests if the thread local variable gets inherited to new threads
     */
    public void testMultipleThreads() {
        ProgressJob pj = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
                "i luve tests" );
  //      MockProgress mProgress = new MockProgress( 3, ConfigUtils.getString( "gemma.admin.user" ), "test runs", pj
  //              .getId() );
 //       mProgress.start();
    }

    public void updateCurrentThreadsProgressJobTest() {

        ProgressManager.dump();
        assertEquals( false, ProgressManager.nudgeCurrentThreadsProgressJob() );

        ProgressJob pj = ProgressManager.createProgressJob( null, ConfigUtils.getString( "gemma.admin.user" ),
                "i luve happie tests" );

        FakeProgress fProgres = new FakeProgress();
        MockClient mc = new MockClient();
  //      ProgressManager.addToNotification( ConfigUtils.getString( "gemma.admin.user" ), mc );
        fProgres.start();

        ProgressManager.destroyProgressJob( pj, true );

        assertEquals( mc.upDateTimes(), 100 );

    }

    class FakeProgress extends Thread {
        private static final int DELAY = 30;

        @Override
        public void run() {

            for ( int i = 0; i < 100; i++ ) {
                assertEquals( true, ProgressManager.nudgeCurrentThreadsProgressJob() );
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
     * Just a mockProcess inner class to ease testing
     * <p>
     * Copyright (c) 2006 UBC Pavlab
     * 
     * @author klc
     * @version $Id$
     */
    class SimpleMockProcess extends Thread {
        private static final int DELAY = 30;

        private String userName;
        private String description;
        private ProgressJob simpleJob;

        public SimpleMockProcess( String fakeName, String fakeDescription ) {

            userName = fakeName;
            description = fakeDescription;
            simpleJob = ProgressManager.createProgressJob( userName, userName, description );

        }

        @Override
        public void run() {

            for ( int i = 0; i < 100; i++ ) {
                simpleJob.nudgeProgress();

                try {
                    Thread.sleep( DELAY );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }

            }

            ProgressManager.destroyProgressJob( simpleJob, true );

        }

     //   public Long getJobId() {
      //      return simpleJob.getId();
      //  }
    }

    class MockProgress extends Thread {

        int times;
        String userName;
        String description;
        Long jobId;

        public MockProgress( int times, String userName, String description, Long jobId ) {
            this.times = times;
            this.userName = userName;
            this.description = description;
            this.jobId = jobId;
        }

        @Override
        public void run() {
            SimpleMockProcess mp;
            for ( int i = 0; i < times; i++ ) {
                mp = new SimpleMockProcess( userName, description );
             //   assertEquals( jobId, mp.getJobId() );

                mp.start();

                mp = null;
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
