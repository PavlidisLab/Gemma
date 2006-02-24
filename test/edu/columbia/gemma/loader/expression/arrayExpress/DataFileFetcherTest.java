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
package edu.columbia.gemma.loader.expression.arrayExpress;

import java.io.File;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;

import edu.columbia.gemma.loader.expression.arrayExpress.util.ArrayExpressUtil;
import edu.columbia.gemma.loader.loaderutils.FtpArchiveFetcher;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DataFileFetcherTest extends TestCase {

    private static Log log = LogFactory.getLog( DataFileFetcherTest.class.getName() );

    /*
     * Test method for 'edu.columbia.gemma.loader.expression.arrayExpress.DataFileFetcher.fetch(String)'
     */
    public void testFetch() throws Exception {
        FtpArchiveFetcher f = new DataFileFetcher();

        try {
            ArrayExpressUtil.connect( FTP.BINARY_FILE_TYPE );
        } catch ( Exception e ) {
            log.warn( "Cannot connect to ArrayExpress FTP site, skipping test." );
            return;
        }

        if ( !( new File( f.getLocalBasePath() ).canWrite() ) ) {
            log.warn( "Path to copy files not reachable, skipping test." );
            return;
        }
        f.fetch( "SMDB-14" );
        // FIMXE no good fail condition!

    }
}
