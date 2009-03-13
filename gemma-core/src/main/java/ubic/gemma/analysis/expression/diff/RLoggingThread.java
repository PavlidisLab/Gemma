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
import org.mortbay.log.LogFactory;

/**
 * A {@link Thread} that logs the elapsed time of an R analysis.
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
        long previous = 0;
        while ( !done ) {
            long current = watch.getTime();
            if ( ( current - previous ) > ONE_MIN_IN_MILLISEC ) {
                log.info( ( current / ONE_MIN_IN_MILLISEC ) + " min elapsed" );
                previous = current;
            }
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
        this.done = true;
    }
}
