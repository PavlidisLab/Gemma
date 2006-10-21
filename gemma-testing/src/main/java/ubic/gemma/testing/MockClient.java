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
package ubic.gemma.testing;

import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressManager;

/**
 * Just a mock client inner class to ease testing
 * 
 * @author klc
 * @version $Id$
 */
public class MockClient implements Observer {

    private static Log log = LogFactory.getLog( MockClient.class.getName() );

    private static final long TIMEOUT = 600000;
    private int update;

    private ProgressData pData;

    public MockClient() {
        super();
        this.update = 0;
    }

    @SuppressWarnings("unused")
    public void update( Observable o, Object pd ) {
        pData = ( ProgressData ) pd;
        update++;
    }

    public int upDateTimes() {
        return this.update;

    }

    public ProgressData getProgressData() {
        return pData;
    }

    /**
     * monitors the a background (test) progress that was started and returns after 60 seconds or when it is finished.
     * 
     * @return ProgressData the last progress data that the load sent
     */
    public static ProgressData monitorLoad() {
        MockClient mc = new MockClient();
        long start = System.currentTimeMillis();
        long elapsed = 0;
        boolean done = false;

        // Need a short pause to make sure the job is started before we try and monitor it
        // try {
        // long numMillisecondsToSleep = 3000; // 3 seconds
        // Thread.sleep( numMillisecondsToSleep );
        // } catch ( InterruptedException e ) {
        // }

        // fixme: I'm not sure why the user is set to 'test'. If this changes this test will break
        boolean ok = ProgressManager.addToNotification( "test", mc );

        if ( !ok ) {
            // throw new IllegalStateException();
        }

        while ( !done && !( TIMEOUT < elapsed ) ) {
            if ( mc.getProgressData() != null ) {
                done = mc.getProgressData().isDone();
            }

            try {
                long numMillisecondsToSleep = 2000;
                Thread.sleep( numMillisecondsToSleep );
                if ( log.isDebugEnabled() ) log.debug( mc.getProgressData().getDescription() );
                if ( log.isInfoEnabled() ) log.info( "Waiting...Elapsed time: " + elapsed );
            } catch ( InterruptedException e ) {
            }

            elapsed = System.currentTimeMillis() - start;
        }

        // forwardURL.getChars( srcBegin, srcEnd, dst, dstBegin )forwardURL.charAt('=');
        // long id = forwardURL. todo: get the id of the EE and load it to really see if it worked. Can get the id from
        // end of the fowarding url

        if ( !done ) {
            throw new IllegalStateException( "Test timed out" );
        }
        return mc.getProgressData();
    }
}