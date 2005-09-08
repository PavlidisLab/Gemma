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
package edu.columbia.gemma.loader.loaderutils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractFetcher implements Fetcher {

    protected static Log log = LogFactory.getLog( AbstractFetcher.class.getName() );
    protected String localBasePath = null;
    protected String baseDir = null;
    protected boolean success = false;
    protected boolean force = false;

    /**
     * Set to true if downloads should proceed even if the file already exists.
     * 
     * @param force
     */
    public void setForce( boolean force ) {
        this.force = force;
    }

    /**
     * Create a directory according to the current accession number and set path information.
     * 
     * @param accession
     * @return
     * @throws IOException
     */
    protected File mkdir( String accession ) throws IOException {
        assert localBasePath != null;
        File newDir = new File( localBasePath + "/" + accession );
        if ( !newDir.exists() ) {
            success = newDir.mkdir();
            if ( !success ) {
                throw new IOException( "Could not create output directory " + newDir );
            }
            log.info( "Created directory " + newDir.getAbsolutePath() );
        }

        if ( !newDir.canWrite() ) {
            throw new IOException( "Cannot write to target directory " + newDir.getAbsolutePath() );
        }

        return newDir;
    }

    /**
     * @return Returns the localBasePath.
     */
    public String getLocalBasePath() {
        return this.localBasePath;
    }

}
