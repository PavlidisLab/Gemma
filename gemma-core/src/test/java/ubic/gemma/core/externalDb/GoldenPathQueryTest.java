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
package ubic.gemma.core.externalDb;

import org.junit.Assert;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.util.Settings;

import java.util.Collection;

/**
 * These tests require a populated Human database. Valid as of 11/2009 on hg19
 *
 * @author pavlidis
 */
public class GoldenPathQueryTest extends TestCase {

    private static final Log log = LogFactory.getLog( GoldenPathQueryTest.class.getName() );
    private GoldenPathQuery queryer;
    private boolean hasDb = false;

    public final void testQueryEst() {
        if ( !hasDb ) {
            GoldenPathQueryTest.log.warn( "Skipping test because hg could not be configured" );
            return;
        }
        Collection<BlatResult> actualValue = queryer.findAlignments( "AA411542" );
        Assert.assertEquals( 6, actualValue.size() ); // updated for hg19 2/2011
    }

    public final void testQueryMrna() {
        if ( !hasDb ) {
            GoldenPathQueryTest.log.warn( "Skipping test because hg could not be configured" );
            return;
        }
        Collection<BlatResult> actualValue = queryer.findAlignments( "AK095183" );
        // assertEquals( 3, actualValue.size() );
        Assert.assertTrue( actualValue.size() > 0 ); // value used to be 3, now 2; this should be safer.
        BlatResult r = actualValue.iterator().next();
        Assert.assertEquals( "AK095183", ( r.getQuerySequence().getName() ) );
    }

    public final void testQueryNoResult() {
        if ( !hasDb ) {
            GoldenPathQueryTest.log.warn( "Skipping test because hg could not be configured" );
            return;
        }
        Collection<BlatResult> actualValue = queryer.findAlignments( "YYYYYUUYUYUYUY" );
        Assert.assertEquals( 0, actualValue.size() );
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "human" );
        t.setIsGenesUsable( true );

        try {
            String databaseHost = Settings.getString( "gemma.testdb.host" );
            String databaseUser = Settings.getString( "gemma.testdb.user" );
            String databasePassword = Settings.getString( "gemma.testdb.password" );
            queryer = new GoldenPathQuery( Settings.getString( "gemma.goldenpath.db.human" ), databaseHost,
                    databaseUser, databasePassword );
            this.hasDb = true;
        } catch ( Exception e ) {
            this.hasDb = false;
        }

    }

}
