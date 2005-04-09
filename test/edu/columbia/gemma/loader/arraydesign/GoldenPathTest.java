package edu.columbia.gemma.loader.arraydesign;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.tools.GoldenPath;
import edu.columbia.gemma.tools.GoldenPath.ThreePrimeData;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPathTest extends TestCase {
    protected static final Log log = LogFactory.getLog( GoldenPathTest.class );
    GoldenPath gp;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        gp = new GoldenPath( 3306, "hg17", "localhost", "pavlidis", "toast" );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        gp = null;
    }

    public final void testFindRefGenesByLocation() {
        List actualResult = gp.findRefGenesByLocation( "11", 100000, 300000 );
        Collections.sort( actualResult, new Comparator() {
            public int compare( Object a, Object b ) {
                Gene ga = ( Gene ) a;
                Gene gb = ( Gene ) b;
                return ga.getOfficialSymbol().compareTo( gb.getOfficialSymbol() );
            };
        } );
        for ( Iterator iter = actualResult.iterator(); iter.hasNext(); ) {
            Gene gene = ( Gene ) iter.next();
            assertEquals( "BET1L", gene.getOfficialSymbol() );
            break;
        }

    }

    // TODO: put these values in an external file. These are based on the may 2004 hg17.
    public final void testGetThreePrimeDistance() {

        // gene >>>>, location contained within the gene.
        int location = 1439902 - 100;
        int actualResult = ( ( ThreePrimeData ) gp.getThreePrimeDistances( "11", location, location + 2 , null, null).get( 0 ) )
                .getDistance();
        int expectedResult = 100 - 2;
        assertEquals( expectedResult, actualResult );

        // gene >>>>, location overhangs
        location = 1439902 - 100;
        actualResult = ( ( ThreePrimeData ) gp.getThreePrimeDistances( "11", location, location + 200, null, null ).get( 0 ) )
                .getDistance();
        expectedResult = 0;
        assertEquals( expectedResult, actualResult );

        // gene >>>>, location does not overlap
        location = 1439902 - 100;

        List actualResultL = gp.getThreePrimeDistances( "11", location + 101, location + 200, null, null );
        List expectedResultL = null;
        assertEquals( expectedResultL, actualResultL );

        // gene >>>>, location does not overlap but barely missed (test for off-by-one)
        location = 1439902 - 100;
        actualResultL = gp.getThreePrimeDistances( "11", location + 100, location + 200, null, null );
        expectedResultL = null;
        assertEquals( expectedResultL, actualResultL );

        // gene <<<<<, location does not overlap but matches exactly
        location = 206138 + 100;
        actualResult = ( ( ThreePrimeData ) gp.getThreePrimeDistances( "11", location + 100, location + 200 , null, null).get( 0 ) )
                .getDistance();
        expectedResult = 200;
        assertEquals( expectedResult, actualResult );

        // gene <<<<<, location overhangs on left.
        location = 206138 + 100;
        actualResult = ( ( ThreePrimeData ) gp.getThreePrimeDistances( "11", location - 200, location + 200 , null, null).get( 0 ) )
                .getDistance();
        expectedResult = 0;
        assertEquals( expectedResult, actualResult );

        // gene <<<<<, no overlap
        location = 206138 + 100;
        actualResultL = gp.getThreePrimeDistances( "11", location - 200, location - 199 , null, null);
        expectedResultL = null;
        assertEquals( expectedResultL, actualResultL );

        // gene <<<<<, region contains gene entirely
        location = 206138; // start of the gene.
        actualResult = ( ( ThreePrimeData ) gp.getThreePrimeDistances( "11", location - 200, location + 10000 , null, null).get( 0 ) )
                .getDistance();
        expectedResult = 0;
        assertEquals( expectedResult, actualResult );

        // gene <<<<<, region contains more than one gene. We allow this.
        location = 206138; // start of the gene.
        actualResult = gp.getThreePrimeDistances( "11", location - 200, location + 100000, null, null ).size();
        expectedResult = 7;
        assertEquals( expectedResult, actualResult );

    }
}
