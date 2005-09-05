/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.util.RCommand;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class RCommander {

    protected static Log log = LogFactory.getLog( RCommander.class.getName() );

    protected RCommand rc;

    public RCommander() {
        this.init();
    }

    /**
     * @param rc2
     */
    public RCommander( RCommand connection ) {
        if ( connection != null && connection.isConnected() ) {
            this.rc = connection;
        }
    }

    protected void init() {
        rc = RCommand.newInstance();
    }

    public void finalize() {
        rc.disconnect();
    }

    public RCommand getRCommandObject() {
        return rc;
    }

}
