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
package ubic.gemma.job.progress.grid;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This appender is used by workers to send progress notifications to the space. The information for these notifications
 * is retrieved from the {@link LoggingEvent}. This information comes from regular logging statements inlined in the
 * source code (ie. log.info("the text")).
 * 
 * @author keshav
 * @version $Id$
 */
public class RemoteProgressAppender extends AppenderSkeleton {

    private final String taskId;
    private final ProgressUpdateCallback progressUpdatesCallback;

    public RemoteProgressAppender( String taskId, ProgressUpdateCallback progressUpdatesCallback ) {
        assert taskId != null;

        this.taskId = taskId;
        this.progressUpdatesCallback = progressUpdatesCallback;
    }

    @Override
    protected void append( LoggingEvent event ) {

        if ( event.getMDC( "taskId" ) == null || !event.getMDC( "taskId" ).equals( this.taskId ) ) {
            return;
        }

        if ( event.getLevel().isGreaterOrEqual( Level.INFO ) && event.getMessage() != null ) {
            progressUpdatesCallback.addProgressUpdate( event.getMessage().toString() );
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
