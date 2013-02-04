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

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This appender is used by remote tasks to send progress notifications to the webapp. The information for these notifications
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
        super();
        assert taskId != null;
        assert progressUpdatesCallback != null;

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

    public void initialize() {
        MDC.put( "taskId", taskId );
        Logger logger = LogManager.getLogger( "ubic.gemma" );
        Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
        logger.addAppender( this );
        baseCodeLogger.addAppender( this );
    }

    /*
    * @see org.apache.log4j.Appender#close()
    */
    @Override
    public void close() {
        Logger logger = LogManager.getLogger( "ubic.gemma" );
        Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
        logger.removeAppender( this );
        baseCodeLogger.removeAppender( this );
        MDC.remove( "taskId" );
    }

    /*
     * @see org.apache.log4j.Appender#requiresLayout()
     */
    @Override
    public boolean requiresLayout() {
        return true;
    }
}
