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
import org.apache.log4j.spi.LoggingEvent;

/**
 * Logging appender for log4j that puts messages in the current thread progress monitor.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProgressAppender extends AppenderSkeleton {

    public ProgressAppender() {
        super();
        /*
         * This has to be set at construction time.
         */
        this.threadName = Thread.currentThread().getName();
    }

    private String threadName;

    /**
     * @return the threadName
     */
    public String getThreadName() {
        return threadName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append( LoggingEvent event ) {
        if ( !event.getThreadName().equals( this.threadName ) ) {
            System.err.println( this.threadName + " !=" + event.getThreadName() );
            return;
        }

        if ( event.getLevel().isGreaterOrEqual( Level.INFO ) && event.getMessage() != null ) {
            ProgressManager.updateCurrentThreadsProgressJob( event.getMessage().toString() );
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
