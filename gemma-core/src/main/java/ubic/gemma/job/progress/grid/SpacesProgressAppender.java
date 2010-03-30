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

import net.jini.core.lease.Lease;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.job.grid.util.SpacesUtil;
import ubic.gemma.job.progress.ProgressAppender;

/**
 * This appender is used by workers to send progress notifications to the space. The information for these notifications
 * is retrieved from the {@link LoggingEvent}. This information comes from regular logging statements inlined in the
 * source code (ie. log.info("the text")).
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesProgressAppender extends ProgressAppender {

    private SpacesProgressEntry entry = null;

    private GigaSpacesTemplate gigaSpacesTemplate = null;

    private String threadName; 

    public SpacesProgressAppender( GigaSpacesTemplate gigaSpacesTemplate, String taskId ) {
        assert gigaSpacesTemplate != null;
        assert taskId != null;

        /*
         * This has to be set at construction time.
         */
        this.threadName = Thread.currentThread().getName();

        this.gigaSpacesTemplate = gigaSpacesTemplate;
        this.entry = new SpacesProgressEntry();
        this.entry.taskId = taskId;
        gigaSpacesTemplate.write( entry, Lease.FOREVER );
    }

    @Override
    protected void append( LoggingEvent event ) {

        /*
         * Important: this method gets events from all threads.
         */

        /*
         * Thus we handle the case where there are multiple workers in the same JVM, and they are all logginig things.
         * Each 'worker' runs in its own thread, and if we don't check this, we get logging going across workers since
         * there is nothing else to distinguish them within the JVM - all logging events go here. This works but it
         * isn't perfect: logging from child threads will be lost.
         */
        if ( !event.getThreadName().equals( this.threadName ) ) {
            // System.err.println( "Wrong thread: " + event.getMessage() + " [" + this.threadName + " != "
            // + event.getThreadName() + "]" );
            return;
        }

        if ( event.getLevel().isGreaterOrEqual( Level.INFO ) && event.getMessage() != null ) {
            this.entry = ( SpacesProgressEntry ) gigaSpacesTemplate.read( entry, SpacesUtil.WAIT_TIMEOUT );

            if ( entry != null ) {
                // System.err.println( entry.taskId + " >>>" + event.getMessage() );
                entry.setMessage( event.getMessage().toString() );
                gigaSpacesTemplate.update( entry, Lease.FOREVER, SpacesUtil.WAIT_TIMEOUT );
            }
        }
    }

    /**
     * Remove the progress entry from the space.
     */
    public void removeProgressEntry() {
        if ( entry != null ) gigaSpacesTemplate.clear( entry );
    }
}
