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
package ubic.gemma.job.progress;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Logging appender for log4j that puts messages in the current thread progress monitor.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LocalProgressAppender extends AppenderSkeleton {

    public LocalProgressAppender(String taskId, Queue<String> progressUpdates) {
        super();
        /*
         * This has to be set at construction time.
         */
        this.taskId = taskId;
        MDC.put( "taskId", taskId );

        this.progressUpdates = progressUpdates;
    }

    protected String taskId;
    private Queue<String> progressUpdates;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append( LoggingEvent event ) {

        if ( event.getMDC( "taskId" ) == null || !event.getMDC( "taskId" ).equals( this.taskId ) ) {
            return;
        }

        /*
         * 'Remote task' logs are events that are duplicates of logging events from the worker. This appender should not
         * be configured for spaces jobs but it can happen...not sure how yet
         */
        if ( event.getLevel().isGreaterOrEqual( Level.INFO ) && event.getMessage() != null
                && !event.getMessage().toString().contains( "[Remote task:" ) ) {
            progressUpdates.add( event.getMessage().toString() );
            //ProgressManager.updateJob( this.taskId, event.getMessage().toString() );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.Appender#close()
     */
    @Override
    public void close() {
        // no-op
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.Appender#requiresLayout()
     */
    @Override
    public boolean requiresLayout() {
        return true;
    }

}
