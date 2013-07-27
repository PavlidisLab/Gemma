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
package ubic.gemma.analysis.util;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.r.RClient;
import ubic.basecode.util.r.RConnectionFactory;
import ubic.basecode.util.r.RServeClient;
import ubic.gemma.util.Settings;

/**
 * A class that encapsulates a connection to R
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class RCommander {

    protected static Log log = LogFactory.getLog( RCommander.class.getName() );

    protected RClient rc;

    /**
     * Gets a connection using configured host (default is localhost). Modify by setting gemma.rserve.hostname.
     * 
     * @throws IOException
     */
    public RCommander() throws IOException {
        String hostname = Settings.getString( "gemma.rserve.hostname", "localhost" );
        this.init( hostname );
    }

    /**
     * @param hostName
     * @throws IOException
     */
    protected void init( String hostName ) throws IOException {
        rc = RConnectionFactory.getRConnection( hostName );
        if ( rc == null || !rc.isConnected() ) {
            throw new IOException( "Could not get an R connection" );
        }
        log.debug( "Got R connection: " + rc.getClass() );
    }

    /**
     * Users should call this method when they are ready to release this object. Otherwise memory leaks and other
     * badness can occur.
     */
    public void cleanup() {
        if ( rc == null || !rc.isConnected() ) {
            return;
        }

        rc.voidEval( " rm(list=ls())" ); // attempt to release all memory used by this connection.

        if ( rc instanceof RServeClient ) {
            log.debug( "Disconnecting from R..." );
            ( ( RServeClient ) rc ).disconnect();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        cleanup();
    }

    public RClient getRCommandObject() {
        return rc;
    }

}
