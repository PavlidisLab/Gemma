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
package ubic.gemma.analysis.expression.diff;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Thread} that logs the elapsed time of an R analysis (every minute).
 * 
 * @author keshav
 * @version $Id$
 */
public class RLoggingThread extends Thread {

    private Log log = LogFactory.getLog( this.getClass() );

    private static final long ONE_MIN_IN_MILLISEC = 60000;

    private boolean done = false;

    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {
        StopWatch watch = new StopWatch();
        watch.start();

        int count = 1;
        while ( !done ) {
            try {
                Thread.sleep( ONE_MIN_IN_MILLISEC );
            } catch ( InterruptedException e ) {
                log.debug( "Thread interrupted.  R Analysis must have finished." );
                break;
            }
            log.info( "R is still running.  " + count + " min elapsed." );
            count++;
        }
        watch.stop();
        return;
    }

    /**
     * Stops the thread from executing.
     * 
     * @param done
     */
    public void done() {
        this.interrupt();
        this.done = true;
    }
}
