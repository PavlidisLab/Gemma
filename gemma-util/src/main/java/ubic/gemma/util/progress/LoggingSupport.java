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

import org.apache.commons.logging.Log;

/**
 * Used to combine logging with progress updates.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LoggingSupport {

    /**
     * Combines logging at the "INFO" level with a progress update.
     * 
     * @param log to be logged to
     * @param message to be logged, and also used to update progress.
     */
    public static void progressLog( Log log, String message ) {
        log.info( message );
        ProgressManager.updateCurrentThreadsProgressJob( message );
    }

    /**
     * Combines logging at the "INFO" level with a progress update.
     * 
     * @param log
     * @param message
     * @param nudge If true, the progress percentage complete is increased by one.
     */
    public static void progressLog( Log log, String message, boolean nudge ) {
        log.info( message );
        if ( nudge ) {
            ProgressManager.nudgeCurrentThreadsProgressJob( message );
        } else {
            ProgressManager.updateCurrentThreadsProgressJob( message );
        }
    }

    /**
     * Combines logging at the "INFO" level with a progress update.
     * 
     * @param log
     * @param message
     * @param percent Percentage done
     */
    public static void progressLog( Log log, String message, int percent ) {
        log.info( message );
        ProgressManager.updateCurrentThreadsProgressJob( new ProgressData( percent, message ) );
    }

}
