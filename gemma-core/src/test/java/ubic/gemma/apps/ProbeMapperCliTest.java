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
package ubic.gemma.apps;

import java.io.File;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperCliTest extends AbstractCLITestCase {

    File tempFile;

    ProbeMapperCli p;
    private String user;
    private String password;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempFile = File.createTempFile( "cli", ".txt" );
        p = new ProbeMapperCli();

        user = ConfigUtils.getString( "gemma.admin.user" );
        password = ConfigUtils.getString( "gemma.admin.password" );

    }

    @Override
    protected void tearDown() throws Exception {
        tempFile.delete();
    }

    public final void testMainBadPort() throws Exception {
        Exception result = p.doWork( new String[] { "-testing", "-v", "3", "-P", "c", "-u", user, "-p", password, "-o",
                tempFile.getAbsolutePath() } ); // should result in an
        // exception
        if ( result == null ) {
            fail( "Expected bad port" );
        }
    }

    public void testBlatHandling() throws Exception {

        String basePath = this.getTestFileBasePath();

        String blatFile = basePath + File.separatorChar
                + "/gemma-core/src/test/resources/data/loader/genome/blatResult.noheader.txt";

        assert ( new File( blatFile ) ).canRead() : "Input blat result file not readable from " + blatFile;

        Exception result = p.doWork( new String[] { "-v", "3", "-u", user, "-p", password, "-o",
                tempFile.getAbsolutePath(), "-b", blatFile, "-d", "hg18", "-testing" } );
        if ( result != null ) {
            result.printStackTrace();

            throw ( result );
        }
    }

    public void testGbHandling() throws Exception {

        String basePath = this.getTestFileBasePath();

        String gbFile = basePath + System.getProperty( "file.separator" )
                + "/gemma-core/src/test/resources/data/loader/genome/ncbiGenes.test.txt";

        assert ( new File( gbFile ) ).canRead();

        Exception result = p.doWork( new String[] { "-testing", "-v", "3", "-u", user, "-p", password, "-o",
                tempFile.getAbsolutePath(), "-g", gbFile, "-d", "hg18" } );
        if ( result != null ) {
            throw ( result );
        }
    }

    public void testSingleGb() throws Exception {
        Exception result = p.doWork( new String[] { "-testing", "-v", "3", "-u", user, "-p", password, "-o",
                tempFile.getAbsolutePath(), "-d", "hg18", "AF015731", "BX473803" } );
        if ( result != null ) {
            throw ( result );
        }
    }

    public void testBadFile() throws Exception {
        String basePath = this.getTestFileBasePath();

        String blatFile = basePath + System.getProperty( "file.separator" )
                + "/gemma-core/src/test/resources/data/loader/genome/blatresult.doesntexist.noheader.txt";

        Exception result = p.doWork( new String[] { "-testing", "-u", user, "-p", password, "-o",
                tempFile.getAbsolutePath(), "-b", blatFile, "-d", "hg18" } );
        if ( result == null ) {
            fail( "Expected bad file" );
        }
    }
    // This test requires a running gfServer with java client. Only works under linux.
    // public void testSequenceHandling() throws Exception {
    // String basePath = this.getTestFileBasePath();
    //
    // String file = basePath + System.getProperty( "file.separator" )
    // + "/gemma-core/src/test/resources/data/loader/genome/testsequence.fa";
    //
    // assert ( new File( file ) ).canRead();
    //
    // Exception result = p.doWork( new String[] { "-v", "3", "-u", "pavlidis", "-p", "toast", "-o",
    // tempFile.getAbsolutePath(), "-f", file, "-d", "mm8" } );
    // if ( result != null ) {
    // fail( result.getMessage() );
    // }
    // }
}