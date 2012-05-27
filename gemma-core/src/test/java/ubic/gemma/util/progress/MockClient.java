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
package ubic.gemma.util.progress;

import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.job.progress.ProgressData;

/**
 * Just a mock client inner class to ease testing
 * 
 * @author klc
 * @version $Id$
 */
public class MockClient implements Observer {

    private static Log log = LogFactory.getLog( MockClient.class.getName() );

    private static final long TIMEOUT = 600000;

    /**
     * monitors the a background (test) progress that was started and returns after 60 seconds or when it is finished.
     * 
     * @return ProgressData the last progress data that the was sent
     */
    public static ProgressData monitorTask( String taskId ) {
        MockClient mc = new MockClient();
        long start = System.currentTimeMillis();
        long elapsed = 0;
        boolean done = false;

        // boolean ok = ProgressManager.addToNotification( taskId, mc );

        try {
            Thread.sleep( 2000 );
        } catch ( InterruptedException e1 ) {
            //
        }

        // if ( !ok ) {
        // throw new IllegalStateException( "No task " + taskId );
        // maybe it's already done.
        // return mc.getProgressData();
        // }
        //
        while ( !done && !( TIMEOUT < elapsed ) ) {
            if ( mc.getProgressData() != null ) {
                done = mc.getProgressData().isDone();
            }

            try {
                long numMillisecondsToSleep = 2000;
                Thread.sleep( numMillisecondsToSleep );
                if ( log.isDebugEnabled() ) log.debug( mc.getProgressData().getDescription() );
                if ( log.isInfoEnabled() ) log.info( "Waiting for job " + taskId + " ...Elapsed time: " + elapsed );
            } catch ( InterruptedException e ) {
            }

            elapsed = System.currentTimeMillis() - start;
        }

        if ( !done ) {
            throw new IllegalStateException( "Test timed out" );
        }
        return mc.getProgressData();
    }

    private int update;

    private ProgressData pData;

    public MockClient() {
        super();
        this.update = 0;
    }

    public ProgressData getProgressData() {
        return pData;
    }

    @Override
    public void update( Observable o, Object pd ) {
        pData = ( ProgressData ) pd;
        update++;
    }

    public int upDateTimes() {
        return this.update;

    }
}