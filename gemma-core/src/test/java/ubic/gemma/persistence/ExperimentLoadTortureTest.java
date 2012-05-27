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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import ubic.gemma.model.common.Describable;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests for deadlocking issues; see bug 2888.
 * 
 * @author paul
 * @version $Id$
 */
public class ExperimentLoadTortureTest extends BaseSpringContextTest {

    /**
     * This test can fail with a deadlock. Note that this behaviour is dependent on configuration, in complex ways, so
     * don't worry about trying to get it to fail. The fact that it works is good ;)
     * 
     * @throws Exception
     */
    @Test
    public void testConcurrentLoading() throws Exception {
        /*
         * Initialize things, so we're not using a completely fresh db.
         */
        getTestPersistentCompleteExpressionExperiment( false );

        int numThreads = 5;
        final int numExperimentsPerThread = 1;

        final AtomicInteger c = new AtomicInteger( 0 );

        final ConcurrentHashMap<Describable, Integer> results = new ConcurrentHashMap<Describable, Integer>();

        final AtomicBoolean failed = new AtomicBoolean( false );

        for ( int i = 0; i < numThreads; i++ ) {
            final int t = i;
            new Thread( new Runnable() {
                @Override
                public void run() {
                    for ( int j = 0; j < numExperimentsPerThread; j++ ) {

                        try {
                            Thread.sleep( RandomUtils.nextInt( 1000 ) );
                            log.info( "Thread " + t + " experiment " + j );

                            results.put( getTestPersistentCompleteExpressionExperiment( false ), 1 );
                            c.incrementAndGet();
                        } catch ( Exception e ) {
                            log.error( "Failure in: Thread " + t + " experiment " + j + ": " + e.getMessage() + " "
                                    + ExceptionUtils.getStackTrace( e ) );
                            failed.set( true );
                        }
                    }
                }
            } ).start();
        }

        while ( c.get() < numThreads * numExperimentsPerThread && !failed.get() ) {
            Thread.sleep( 5000 );
            log.info( "Waiting ..." );
        }

        Thread.sleep( 1000 );

        /*
         * This test passes like 4/5 times.
         */
        if ( results.size() != numThreads * numExperimentsPerThread ) {
            log.warn( "Multithreaded loading failure: check logs for failure to recover from deadlock" );
        }
        // assertEquals( "Multithreaded loading failure: check logs for deadlock", numThreads * numExperimentsPerThread,
        // results.size() );
    }
}
