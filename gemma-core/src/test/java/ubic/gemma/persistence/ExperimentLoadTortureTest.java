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
import static org.junit.Assert.fail;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests for deadlocking issues; see bug 2888.
 * 
 * @author paul
 * @version $Id$
 */
public class ExperimentLoadTortureTest extends BaseSpringContextTest {

    /**
     * This test will fail with a deadlock on ACL table.
     * 
     * @throws Exception
     */
    // @Test
    public void testConcurrentLoading() throws Exception {
        /*
         * Initialize things, so we're not using a completely fresh db.
         */
        getTestPersistentCompleteExpressionExperiment( false );

        // I was playing around with larger values, but this has worked every time (exposing bug)
        int numThreads = 2;
        final int numExperimentsPerThread = 1;

        final AtomicInteger c = new AtomicInteger( 0 );

        final ConcurrentHashMap<ExpressionExperiment, Integer> results = new ConcurrentHashMap<ExpressionExperiment, Integer>();

        final AtomicBoolean failed = new AtomicBoolean( false );

        for ( int i = 0; i < numThreads; i++ ) {
            final int t = i;
            new Thread( new Runnable() {
                @Override
                public void run() {
                    for ( int j = 0; j < numExperimentsPerThread; j++ ) {
                        log.info( "Thread " + t + " experiment " + j );
                        try {
                            ExpressionExperiment ee = getTestPersistentCompleteExpressionExperiment( false );
                            results.put( ee, 1 );
                        } catch ( Exception e ) {
                            log.error( "Failure in: Thread " + t + " experiment " + j, e );
                            failed.set( true );
                        } finally {
                            c.incrementAndGet();
                        }
                    }
                }
            } ).start();
        }

        while ( c.get() < numThreads * numExperimentsPerThread && !failed.get() ) {
            Thread.sleep( 2000 );
            log.info( "Waiting ..." );
        }

        assertEquals( "Multithreaded loading failure: check logs for deadlock", numThreads * numExperimentsPerThread,
                results.size() );
    }
}
