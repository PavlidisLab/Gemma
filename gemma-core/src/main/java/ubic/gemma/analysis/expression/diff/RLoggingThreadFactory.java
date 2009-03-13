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

/**
 * Creates and starts threads {@link RLoggingThread}s, which log the elapsed time of R analyses.
 * 
 * @author keshav
 * @version $Id$
 */
public class RLoggingThreadFactory {

    /**
     * Creates and starts an {@link RLoggingThread}. The thread is set as a daemon.
     * 
     * @return the {@link RLoggingThread}.
     */
    public static RLoggingThread createRLoggingThread() {
        RLoggingThread rLogging = new RLoggingThread();
        rLogging.setDaemon( true );
        rLogging.start();
        return rLogging;
    }

}
