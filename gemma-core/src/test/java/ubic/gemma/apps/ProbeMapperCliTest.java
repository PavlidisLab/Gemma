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

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperCliTest extends AbstractCLITestCase {

    File tempFile;

    ProbeMapperCli p;
    private String databaseHost;
    private String databaseUser;
    private String databasePassword;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        tempFile = File.createTempFile( "cli", ".txt" );
        p = new ProbeMapperCli();

        // fixme - factor this out so it can be reused.
        try {
            config = new CompositeConfiguration();
            config.addConfiguration( new SystemConfiguration() );
            config.addConfiguration( new PropertiesConfiguration( "build.properties" ) );
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }

        databaseHost = config.getString( "gemma.testdb.host" );
        databaseUser = config.getString( "gemma.testdb.user" );
        databasePassword = config.getString( "gemma.testdb.password" );
    }

    protected void tearDown() throws Exception {
        tempFile.delete();
    }

    public final void testMainBadPort() throws Exception {
        Exception result = p.doWork( new String[] { "-v", "3", "-P", "c", "-u", databaseUser, "-p", databasePassword,
                "-H", databaseHost, "-o", tempFile.getAbsolutePath() } ); // should result in an exception
        assertTrue( result.getMessage(), result != null );
    }

    public void testBlatHandling() throws Exception {

        String basePath = this.getTestFileBasePath();

        String blatFile = basePath + System.getProperty( "file.separator" )
                + "/gemma-core/src/test/resources/data/loader/genome/blatresult.noheader.txt";

        assert ( new File( blatFile ) ).canRead();

        Exception result = p.doWork( new String[] { "-v", "3", "-u", databaseUser, "-p", databasePassword, "-H",
                databaseHost, "-o", tempFile.getAbsolutePath(), "-b", blatFile, "-d", "hg18" } );
        if ( result != null ) {
            fail( result.getMessage() );
        }
    }

    public void testGbHandling() throws Exception {

        String basePath = this.getTestFileBasePath();

        String gbFile = basePath + System.getProperty( "file.separator" )
                + "/gemma-core/src/test/resources/data/loader/genome/ncbiGenes.test.txt";

        assert ( new File( gbFile ) ).canRead();

        Exception result = p.doWork( new String[] { "-v", "3", "-u", databaseUser, "-p", databasePassword, "-H",
                databaseHost, "-o", tempFile.getAbsolutePath(), "-g", gbFile, "-d", "hg18" } );
        if ( result != null ) {
            fail( result.getMessage() );
        }
    }

    public void testSingleGb() throws Exception {
        Exception result = p.doWork( new String[] { "-v", "3", "-u", "testuser", "-p", "toast", "-o",
                tempFile.getAbsolutePath(), "-d", "hg18", "AF015731", "BX473803" } );
        if ( result != null ) {
            result.printStackTrace();
            fail( result.getMessage() );
        }
    }

    public void testBadFile() throws Exception {
        String basePath = this.getTestFileBasePath();

        String blatFile = basePath + System.getProperty( "file.separator" )
                + "/gemma-core/src/test/resources/data/loader/genome/blatresult.doesntexist.noheader.txt";

        Exception result = p.doWork( new String[] { "-u", databaseUser, "-p", databasePassword, "-H", databaseHost,
                "-o", tempFile.getAbsolutePath(), "-b", blatFile, "-d", "hg18" } );
        assertTrue( result.getMessage(), result != null );
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