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
package ubic.gemma.util.progress.grid.javaspaces;

import net.jini.core.lease.Lease;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.grid.javaspaces.SpacesUtil;
import ubic.gemma.util.grid.javaspaces.entry.SpacesProgressEntry;
import ubic.gemma.util.progress.ProgressAppender;

/**
 * This appender is used to send progress notifications to the space. The information for these notifications is
 * retrieved from the {@link LoggingEvent}. This information comes from the logging statement inlined in the source code
 * (ie. log.info("the text")).
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesProgressAppender extends ProgressAppender {

    private SpacesProgressEntry entry = null;

    private GigaSpacesTemplate gigaSpacesTemplate = null;

    private String taskId = null;

    public SpacesProgressAppender( GigaSpacesTemplate gigaSpacesTemplate, String taskId ) {
        this.gigaSpacesTemplate = gigaSpacesTemplate;
        this.taskId = taskId;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.progress.ProgressAppender#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append( LoggingEvent event ) {

        if ( gigaSpacesTemplate == null )
            throw new RuntimeException( "Cannot log tasks executing on the compute server.  GigaSpacesTemplate "
                    + "has not been added to the application context." );

        if ( event.getLevel().isGreaterOrEqual( Level.INFO ) && event.getMessage() != null ) {
            if ( entry == null ) {
                entry = new SpacesProgressEntry();
                entry.taskId = taskId;
                entry.message = "Logging Server Task";
                gigaSpacesTemplate.clear( entry );
                gigaSpacesTemplate.write( entry, Lease.FOREVER, 5000 );// FIXME use UpdateModifiers.WRITE_ONLY?
            } else {
                try {
                    entry = ( SpacesProgressEntry ) gigaSpacesTemplate.read( entry, SpacesUtil.WAIT_TIMEOUT );
                    entry.setMessage( event.getMessage().toString() );
                    gigaSpacesTemplate.update( entry, Lease.FOREVER, SpacesUtil.WAIT_TIMEOUT );
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        }
    }
}
