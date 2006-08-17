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
package ubic.gemma.analysis.sequence;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.externalDb.GoldenPath;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;
import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperTest extends TestCase {

    private static Log log = LogFactory.getLog( ProbeMapperTest.class.getName() );
    Collection<BlatResult> blatres;
    private String databaseHost;
    private String databaseUser;
    private String databasePassword;

    protected void setUp() throws Exception {
        super.setUp();
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/col8a1.blatresults.txt" );
        BlatResultParser brp = new BlatResultParser();
        brp.parse( is );
        blatres = brp.getResults();

        assert blatres != null && blatres.size() > 0;

        databaseHost = ConfigUtils.getString( "gemma.testdb.host" );
        databaseUser = ConfigUtils.getString( "gemma.testdb.user" );
        databasePassword = ConfigUtils.getString( "gemma.testdb.password" );

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // public void testProcessGbId() {
    // // fail( "Not yet implemented" );
    // }

    public void testProcessBlatResults() throws Exception {
        ProbeMapper pm = new ProbeMapper();

        try {
            GoldenPath gp = new GoldenPath( 3306, "mm8", databaseHost, databaseUser, databasePassword );
            if ( gp == null ) {
                log.warn( "Could not get Goldenpath database connection, skipping test" );
                return;
            }

            Map<String, Collection<BlatAssociation>> res = pm.processBlatResults( gp, blatres );

            // This test will fail if the database changes :)
            assertTrue( "No results", res.values().size() > 0 );
            assertTrue( "No results", res.values().iterator().next().size() > 0 );
            assertEquals( "Col8a1", res.values().iterator().next().iterator().next().getGeneProduct().getGene()
                    .getOfficialSymbol() );
        } catch ( java.sql.SQLException e ) {
            if ( e.getMessage().contains( "Unknown database" ) ) {
                log.warn( "Test skipped due to missing mm8 database" );
                return;
            } else if ( e.getMessage().contains( "Access denied" ) ) {
                log.warn( "Test skipped due to database authentication problem - check username and password in test" );
                return;
            }
            throw e;
        }

    }
}
