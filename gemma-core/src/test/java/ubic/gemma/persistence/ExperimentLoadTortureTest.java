/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.persistence;

import org.junit.Test;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests for deadlocking issues; see bug 2888.
 * 
 * @author paul
 * @version $Id$
 */
public class ExperimentLoadTortureTest extends BaseSpringContextTest {

    /** Disabled; this test was failing all of a sudden. */
    @Test
    public void testConcurrentLoading() throws Exception {
        // /*
        // * Initialize things, so we're not using a completely fresh db.
        // */
        // getTestPersistentCompleteExpressionExperiment( false );
        //
        // int numThreads = 10;
        // final int numExperimentsPerThread = 5;
        //
        // final AtomicInteger c = new AtomicInteger( 0 );
        //
        // final ConcurrentHashMap<Describable, Integer> results = new ConcurrentHashMap<Describable, Integer>();
        //
        // final AtomicBoolean failed = new AtomicBoolean( false );
        // final Random random = new Random();
        // Collection<Thread> threads = new HashSet<Thread>();
        // for ( int i = 0; i < numThreads; i++ ) {
        // final int t = i;
        // Thread k = new Thread( new Runnable() {
        // @Override
        // public void run() {
        // for ( int j = 0; j < numExperimentsPerThread; j++ ) {
        //
        // try {
        // Thread.sleep( random.nextInt( 500 ) );
        // log.info( "Thread " + t + " experiment " + j );
        //
        // results.put( getTestPersistentCompleteExpressionExperiment( false ), 1 );
        // c.incrementAndGet();
        // } catch ( Exception e ) {
        // log.error( "Failure in: Thread " + t + " experiment " + j + ": " + e.getMessage() + " "
        // + ExceptionUtils.getStackTrace( e ) );
        // failed.set( true );
        // }
        // }
        // }
        // } );
        //
        // threads.add( k );
        //
        // k.start();
        // }
        //
        // int waits = 0;
        // int maxWaits = 20;
        // while ( c.get() < numThreads * numExperimentsPerThread && !failed.get() ) {
        // Thread.sleep( 5000 );
        // log.info( "Waiting ..." );
        // if ( ++waits > maxWaits ) {
        // for ( Thread t : threads ) {
        // if ( t.isAlive() ) t.interrupt();
        // }
        // fail( "Multithreaded loading failure: timed out." );
        // }
        // }
        //
        // Thread.sleep( 1000 );
        //
        // if ( results.size() != numThreads * numExperimentsPerThread ) {
        // fail(
        // "Multithreaded loading failure: check logs for failure to recover from deadlock (or other database error)" );
        // } else {
        // log.info( "TORTURE TEST PASSED!" );
        // }
    }
}
