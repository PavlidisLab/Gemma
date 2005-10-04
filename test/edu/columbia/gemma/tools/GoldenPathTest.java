package edu.columbia.gemma.tools;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;

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
        List<Gene> actualResult = gp.findRefGenesByLocation( "11", 100000, 300000, null );
        Collections.sort( actualResult, new Comparator<Gene>() {
            public int compare( Gene a, Gene b ) {
                return a.getOfficialSymbol().compareTo( b.getOfficialSymbol() );
            };
        } );

        for ( Gene gene : actualResult ) {
            assertEquals( "BET1L", gene.getOfficialSymbol() );
            break;
        }

    }

    // We should put these hard-coded values in an external file. These are based on the may 2004 hg17.
    // These locations on chromosome 11 were just chosen at random.
    // http://genome.ucsc.edu/cgi-bin/hgTracks?hgsid=41239384&hgt.out1=1.5x&position=chr11%3A206144-206244
    // http://genome.ucsc.edu/cgi-bin/hgTracks?hgsid=41239384&hgt.out3=10x&position=chr11%3A1439902-1439903
    public final void testGetThreePrimeDistanceA() {

        // gene >>>>, location contained within the gene.
        int location = 1439902 - 100;
        int actualResult = gp.getThreePrimeDistances( "11", location, location + 2, null, null, null,
                GoldenPath.RIGHTEND ).get( 0 ).getDistance();
        int expectedResult = 100 - 2;
        assertEquals( expectedResult, actualResult );

    }

    public final void testGetThreePrimeDistanceB() {
        // gene >>>>, location overhangs
        int location = 1439902 - 100;
        int actualResult = gp.getThreePrimeDistances( "11", location, location + 200, null, null, "+",
                GoldenPath.RIGHTEND ).get( 0 ).getDistance();
        int expectedResult = 0;
        assertEquals( expectedResult, actualResult );
    }

    public final void testGetThreePrimeDistanceC() {
        // gene >>>>, location does not overlap
        int location = 1439902 - 100;

        List actualResultL = gp.getThreePrimeDistances( "11", location + 101, location + 200, null, null, "+",
                GoldenPath.RIGHTEND );
        List expectedResultL = null;
        assertEquals( expectedResultL, actualResultL );
    }

    public final void testGetThreePrimeDistanceD() {
        // gene >>>>, location does not overlap but barely missed (test for off-by-one)
        int location = 1439902 - 100;
        List actualResultL = gp.getThreePrimeDistances( "11", location + 100, location + 200, null, null, "+",
                GoldenPath.RIGHTEND );
        List expectedResultL = null;
        assertEquals( expectedResultL, actualResultL );
    }

    public final void testGetThreePrimeDistanceE() {
        // Gene in <<<<<< direction.
        int location = 206144 + 100;
        int actualResult = gp.getThreePrimeDistances( "11", location + 100, location + 200, null, null, "-",
                GoldenPath.RIGHTEND ).get( 0 ).getDistance();
        int expectedResult = 206; // this is a funny case, as it turns out.
        assertEquals( expectedResult, actualResult );
    }

    public final void testGetThreePrimeDistanceF() {
        // gene <<<<<, location overhangs on left.
        int location = 206144 + 100;
        int actualResult = gp.getThreePrimeDistances( "11", location - 200, location + 200, null, null, "-",
                GoldenPath.RIGHTEND ).get( 0 ).getDistance();
        int expectedResult = 0;
        assertEquals( expectedResult, actualResult );
    }

    public final void testGetThreePrimeDistanceG() {
        // gene <<<<<, no overlap
        int location = 206144 + 100;
        List actualResultL = gp.getThreePrimeDistances( "11", location - 200, location - 199, null, null, "+",
                GoldenPath.RIGHTEND );
        List expectedResultL = null;
        assertEquals( expectedResultL, actualResultL );
    }

    public final void testGetThreePrimeDistanceH() {
        // gene <<<<<, region contains gene entirely
        int location = 206144; // start of the gene.
        int actualResult = gp.getThreePrimeDistances( "11", location - 200, location + 10000, null, null, "-",
                GoldenPath.RIGHTEND ).get( 0 ).getDistance();
        int expectedResult = 0;
        assertEquals( expectedResult, actualResult );
    }

    public final void testGetThreePrimeDistanceI() {
        // gene <<<<<, region contains more than one gene. We allow this.
        int location = 206144; // start of the gene.
        int actualResult = gp.getThreePrimeDistances( "11", location - 200, location + 100000, null, null, "-",
                GoldenPath.RIGHTEND ).size();
        int expectedResult = 3; // number of genes detected
        assertEquals( expectedResult, actualResult );

    }
}
