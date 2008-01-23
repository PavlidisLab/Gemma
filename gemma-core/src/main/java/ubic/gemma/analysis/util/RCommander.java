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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.RClient;
import ubic.basecode.util.RConnectionFactory;

/**
 * A class that encapsulates a connection to R
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class RCommander {

    protected static Log log = LogFactory.getLog( RCommander.class.getName() );

    protected RClient rc;

    public RCommander() {
        this.init();
    }

    protected void init() {
        rc = RConnectionFactory.getRConnection();
    }

    /**
     * Users should call this method when they are ready to release this object. Otherwise memory leaks and other
     * badness can occur.
     */
    public void cleanup() {
        if ( rc == null ) {
            log.warn( "Cleanup called, but no connection" );
            return;
        }
        rc.voidEval( " rm(list=ls())" ); // attempt to release all memory used by this connection.
        log.debug( "Disconnecting from RServer..." );
        rc.disconnect();
        log.debug( "...disconnected" );
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
