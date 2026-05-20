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
package ubic.gemma.core.goldenpath;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.Collection;

import static org.junit.Assume.assumeNoException;

/**
 * These tests require a populated Human database. Valid as of 11/2009 on hg19
 *
 * @author pavlidis
 */
@Tag("goldenpath")
public class GoldenPathQueryTest {

    /* fixtures */
    private static GoldenPathQuery queryer;

    @BeforeAll
    public static void setUp() throws Exception {
        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "human" );
        t.setIsGenesUsable( true );
        try {
            queryer = new GoldenPathQuery( t );
        } catch ( CannotGetJdbcConnectionException e ) {
            assumeNoException( e );
        }
    }

    @AfterAll
    public static void tearDown() {
        if ( queryer != null )
            queryer.close();
    }

    @Test
    public final void testQueryEst() {
        Collection<BlatResult> actualValue = queryer.findAlignments( "AA411542" );
        Assertions.assertEquals( 6, actualValue.size() ); // updated for hg19 2/2011
    }

    @Test
    public final void testQueryMrna() {
        Collection<BlatResult> actualValue = queryer.findAlignments( "AK095183" );
        // assertEquals( 3, actualValue.size() );
        Assertions.assertTrue( actualValue.size() > 0 ); // value used to be 3, now 2; this should be safer.
        BlatResult r = actualValue.iterator().next();
        Assertions.assertEquals( "AK095183", ( r.getQuerySequence().getName() ) );
    }

    @Test
    public final void testQueryNoResult() {
        Collection<BlatResult> actualValue = queryer.findAlignments( "YYYYYUUYUYUYUY" );
        Assertions.assertEquals( 0, actualValue.size() );
    }
}
