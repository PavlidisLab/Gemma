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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class FtpArchiveFetcher extends AbstractFetcher implements ArchiveFetcher {
    protected FTPClient f;
    protected static Log log = LogFactory.getLog( FtpArchiveFetcher.class.getName() );
    protected String localBasePath = null;
    protected String baseDir = null;
    protected boolean success = false;

    protected boolean doDelete = false;

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.ArchiveFetcher#deleteAfterUnpack(boolean)
     */
    public void setDeleteAfterUnpack( @SuppressWarnings("hiding")
    boolean doDelete ) {
        this.doDelete = doDelete;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Fetcher#setForce(boolean)
     */
    public void setForce( boolean force ) {
        this.force = force;
    }

}
