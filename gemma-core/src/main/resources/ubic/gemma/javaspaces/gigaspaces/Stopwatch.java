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
package ubic.gemma.javaspaces.gigaspaces;

/**
 * Class that enables developers to forego writing their own, similar timer classes. Way too many people here are
 * rolling their own timer classes... Usage: <blockquote>
 * 
 * <pre>
 * 	Stopwatch sw = new Stopwatch();
 *   ...
 *   sw.start();
 *   ... // do stuff
 *   System.err.println(&quot;Stuff took: &quot; + sw.stop().getElapsedTime() + &quot; ms&quot;);
 *   or
 *   System.err.println(&quot;Stuff took: &quot; + sw.stop().getElapsedTime()/1000. + &quot; secs&quot;);
 * 
 *  Some methods return a reference to the Stopwatch object for convenience.
 * </pre>
 * 
 * </blockquote>
 * 
 * @author keshav
 * @version $Id$
 * @since 5.1
 */

public class Stopwatch {
    private long startTime = -1;
    private long stopTime = -1;
    private boolean running = false;

    /**
     * Start the timer
     * 
     * @param None
     */
    public Stopwatch start() {
        startTime = System.currentTimeMillis();
        running = true;
        return this;
    }

    /**
     * Stop the timer
     * 
     * @param None
     */
    public Stopwatch stop() {
        stopTime = System.currentTimeMillis();
        running = false;
        return this;
    }

    /**
     * If stop() was called previously, return the number of milliseconds between the call to start() and stop() If
     * stop() was not called previously, return the number of milliseconds between the call to start() and this call to
     * getElapsedTime()
     * 
     * @param None
     */
    public long getElapsedTime() {
        if ( startTime == -1 ) return 0;

        if ( running )
            return System.currentTimeMillis() - startTime;
        else
            return stopTime - startTime;
    }

    /**
     * Stop and reset the timer, but doesn't start().
     * 
     * @param None
     */
    public Stopwatch reset() {
        startTime = stopTime = -1;
        running = false;
        return this;
    }
}
