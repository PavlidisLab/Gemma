package edu.columbia.gemma.loader.arraydesign;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;


import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffymetrixProbeTest extends TestCase {

    AffymetrixProbe[] testProbes;
    int[] expectedResults;

    Sequence seq;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        testProbes = new AffymetrixProbe[] {
                new AffymetrixProbe( "foo", "foo", DNATools.createDNASequence( "ccc", "dna_2" ), 0 ),
                new AffymetrixProbe( "foo", "foo", DNATools.createDNASequence( "cccccccaaaaaaaaa", "dna_2" ), 0 ),
                new AffymetrixProbe( "foo", "foo", DNATools.createDNASequence( "atttttttttt", "dna_2" ), 0 ),
                new AffymetrixProbe( "foo", "foo", DNATools.createDNASequence( "aaaaaaaaaaccc", "dna_2" ), 0 ),
                new AffymetrixProbe( "foo", "foo", DNATools.createDNASequence( "ccaaacccccccccccaaa", "dna_2" ), 0 ),
                new AffymetrixProbe( "foo", "foo", DNATools.createDNASequence( "ggg", "dna_2" ), 0 ),
                new AffymetrixProbe( "foo", "foo", DNATools.createDNASequence( "ggggggg", "dna_2" ), 0 ) };

        expectedResults = new int[] { 3, 5, 0, 0, 2, 0, 0 };

        seq = DNATools.createDNASequence( "ccccccgccccc", "dna_1" );
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for int overlap(Sequence)
     */
    public final void testOverlapSequence() throws Exception {

        for ( int i = 0; i < testProbes.length; i++ ) {
            int actualReturn = testProbes[i].rightHandOverlap( seq );

            int expectedReturn = expectedResults[i];
            assertEquals( expectedReturn, actualReturn );
        }

    }
}
